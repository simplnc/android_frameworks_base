/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.trickystore;

import android.security.keystore.KeyProperties;
import android.util.Log;

import android.security.trickystore.SecurityAuditor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages dynamic key generation for on-demand attestation
 * Reduces security risks by generating keys instead of using static certificates
 * @hide
 */
public final class DynamicKeyManager {
    private static final String TAG = "TrickyStore.DynamicKeyManager";

    private static volatile DynamicKeyManager sInstance;

    // Cache for generated key pairs (limited lifetime)
    private final ConcurrentHashMap<String, CachedKeyPair> mKeyCache = new ConcurrentHashMap<>();

    // Cache expiration time (30 minutes)
    private static final long CACHE_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(30);

    private DynamicKeyManager() {}

    public static DynamicKeyManager getInstance() {
        if (sInstance == null) {
            synchronized (DynamicKeyManager.class) {
                if (sInstance == null) {
                    sInstance = new DynamicKeyManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Generate a dynamic key pair for attestation
     */
    public KeyPair generateDynamicKeyPair(String alias) {
        if (alias == null) return null;

        try {
            // Check cache first
            CachedKeyPair cached = mKeyCache.get(alias);
            if (cached != null && !cached.isExpired()) {
                Log.d(TAG, "Using cached key pair for alias: " + alias);
                return cached.keyPair;
            }

            // Generate new key pair
            KeyPair keyPair = generateEcKeyPair();

            // Cache the key pair
            CachedKeyPair newCached = new CachedKeyPair(keyPair);
            mKeyCache.put(alias, newCached);

            Log.d(TAG, "Generated new dynamic key pair for alias: " + alias);

            // Audit key generation
            SecurityAuditor.getInstance().logKeyGeneration(
                getPackageFromAlias(alias), alias, true);

            return keyPair;

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate dynamic key pair for alias: " + alias, e);

            // Audit failed key generation
            SecurityAuditor.getInstance().logKeyGeneration(
                getPackageFromAlias(alias), alias, false);

            return null;
        }
    }

    /**
     * Generate a complete attestation certificate chain dynamically
     */
    public X509Certificate[] generateDynamicAttestationChain(String alias, String subject,
            int[] attestationApplicationId, byte[] attestationChallenge,
            int deviceIdAttestationVersion) {

        if (alias == null || subject == null) return null;

        try {
            KeyPair attestedKeyPair = generateDynamicKeyPair(alias);
            if (attestedKeyPair == null) {
                Log.w(TAG, "Failed to generate attested key pair");
                return null;
            }

            // Generate attestation key pair (CA key)
            KeyPair attestationKeyPair = generateEcKeyPair();
            if (attestationKeyPair == null) {
                Log.w(TAG, "Failed to generate attestation key pair");
                return null;
            }

            // Generate dynamic attestation certificate chain
            X509Certificate[] chain = CertificateGenerator.generateAttestationCertificateChain(
                attestedKeyPair, attestationKeyPair.getPrivate(), subject,
                attestationApplicationId, attestationChallenge, deviceIdAttestationVersion);

            if (chain != null && chain.length > 0) {
                Log.d(TAG, "Generated dynamic attestation chain for alias: " + alias +
                      " (chain length: " + chain.length + ")");
            }

            return chain;

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate dynamic attestation chain for alias: " + alias, e);
            return null;
        }
    }

    /**
     * Generate EC key pair
     */
    private KeyPair generateEcKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Clear expired keys from cache
     */
    public void cleanupExpiredKeys() {
        long currentTime = System.currentTimeMillis();
        mKeyCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                Log.d(TAG, "Removing expired key pair for alias: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Clear all cached keys (for security)
     */
    public void clearAllKeys() {
        int cleared = mKeyCache.size();
        mKeyCache.clear();
        Log.i(TAG, "Cleared " + cleared + " cached key pairs");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        cleanupExpiredKeys(); // Clean up before reporting stats
        return new CacheStats(mKeyCache.size());
    }

    /**
     * Cached key pair with expiration
     */
    private static class CachedKeyPair {
        final KeyPair keyPair;
        final long creationTime;

        CachedKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
            this.creationTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - creationTime) > CACHE_EXPIRATION_MS;
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final int cachedKeys;

        CacheStats(int cachedKeys) {
            this.cachedKeys = cachedKeys;
        }

        @Override
        public String toString() {
            return "CacheStats{cachedKeys=" + cachedKeys + "}";
        }
    }

    /**
     * Generate a unique alias for dynamic keys based on package and timestamp
     */
    public static String generateDynamicAlias(String packageName, String baseAlias) {
        long timestamp = System.currentTimeMillis();
        return "dynamic_" + packageName + "_" + baseAlias + "_" + timestamp;
    }

    /**
     * Check if an alias is dynamically generated
     */
    public static boolean isDynamicAlias(String alias) {
        return alias != null && alias.startsWith("dynamic_");
    }

    /**
     * Extract package name from dynamic alias
     */
    private String getPackageFromAlias(String alias) {
        if (alias == null) return null;

        if (isDynamicAlias(alias)) {
            // Format: dynamic_package_alias_timestamp
            String[] parts = alias.split("_", 4);
            if (parts.length >= 4) {
                return parts[1]; // package name
            }
        }

        return null;
    }
}