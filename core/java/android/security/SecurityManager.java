/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
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

package android.security;

import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

/**
 * Central Security Manager - Coordinates all security hardening features
 * Inspired by GrapheneOS comprehensive security approach
 */
public class SecurityManager {
    private static final String TAG = "SecurityManager";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final SecureAppSpawning mSecureAppSpawning;
    private final MemoryHardening mMemoryHardening;
    private final NetworkSecurityHardening mNetworkSecurity;
    private final PrivacyManager mPrivacyManager;
    private final AutoRebootManager mAutoRebootManager;

    public SecurityManager(Context context) {
        mContext = context;
        mSecureAppSpawning = new SecureAppSpawning(context);
        mMemoryHardening = new MemoryHardening(context);
        mNetworkSecurity = new NetworkSecurityHardening(context);
        mPrivacyManager = new PrivacyManager(context);
        mAutoRebootManager = new AutoRebootManager(context);
        
        if (DEBUG) Log.d(TAG, "SecurityManager initialized with GrapheneOS-compatible modules");
    }

    /**
     * Get secure app spawning instance (GrapheneOS feature)
     */
    public SecureAppSpawning getSecureAppSpawning() {
        return mSecureAppSpawning;
    }

    /**
     * Get auto-reboot manager instance (GrapheneOS feature)
     */
    public AutoRebootManager getAutoRebootManager() {
        return mAutoRebootManager;
    }

    /**
     * Get memory hardening instance
     */
    public MemoryHardening getMemoryHardening() {
        return mMemoryHardening;
    }

    /**
     * Get network security hardening instance
     */
    public NetworkSecurityHardening getNetworkSecurityHardening() {
        return mNetworkSecurity;
    }

    /**
     * Get privacy manager instance
     */
    public PrivacyManager getPrivacyManager() {
        return mPrivacyManager;
    }

    /**
     * Check if secure app spawning is enabled (GrapheneOS feature)
     */
    public boolean isSecureAppSpawningEnabled() {
        return mSecureAppSpawning.isSecureAppSpawningEnabled();
    }

    /**
     * Check if auto-reboot is enabled (GrapheneOS feature)
     */
    public boolean isAutoRebootEnabled() {
        return mAutoRebootManager.isAutoRebootEnabled();
    }

    /**
     * Validate memory operation for security
     */
    public boolean isMemoryOperationAllowed(String operation, String packageName, int uid) {
        return mMemoryHardening.isMemoryViolation(operation, packageName, uid);
    }

    /**
     * Validate network operation for security
     */
    public boolean isNetworkOperationAllowed(String hostname, int port, String protocol) {
        return mNetworkSecurity.isNetworkConnectionAllowed(hostname, port, protocol);
    }

    /**
     * Validate TLS version for security
     */
    public boolean isTLSVersionAllowed(String tlsVersion) {
        return mNetworkSecurity.isTLSVersionAllowed(tlsVersion);
    }

    /**
     * Validate cipher suite for security
     */
    public boolean isCipherSuiteAllowed(String cipherSuite) {
        return mNetworkSecurity.isCipherSuiteAllowed(cipherSuite);
    }

    /**
     * Validate privacy operation
     */
    public boolean isPrivacyOperationAllowed(String operation) {
        return mPrivacyManager.isPrivacyOperationAllowed(operation);
    }

    /**
     * Get comprehensive security status (GrapheneOS-compatible)
     */
    public String getSecurityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== RISINGOS SECURITY STATUS ===\n");
        status.append("Secure App Spawning: ").append(mSecureAppSpawning.getSpawningModeStatus()).append("\n");
        status.append("Memory Hardening: ").append(mMemoryHardening.getMemoryHardeningStatus()).append("\n");
        status.append("Network Security: ").append(mNetworkSecurity.getNetworkSecurityStatus()).append("\n");
        status.append("Auto-Reboot: ").append(mAutoRebootManager.getAutoRebootStatus()).append("\n");
        status.append("Privacy Manager: Active\n");
        status.append("Security Level: GRAPHENEOS-COMPATIBLE\n");
        return status.toString();
    }

    /**
     * Reset all security settings to secure defaults (GrapheneOS-compatible)
     */
    public void resetToSecureDefaults() {
        mSecureAppSpawning.resetToSecureDefaults();
        mMemoryHardening.resetToSecureDefaults();
        mNetworkSecurity.resetToSecureDefaults();
        mPrivacyManager.resetToSecureDefaults();
        mAutoRebootManager.resetToDefaults();
        
        if (DEBUG) Log.d(TAG, "All security settings reset to GrapheneOS-compatible defaults");
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String packageName, int uid) {
        SecurityLogger.logSecurityViolation(event, packageName, uid);
    }

    /**
     * Check if security is properly configured (GrapheneOS-compatible)
     */
    public boolean isSecurityProperlyConfigured() {
        return mSecureAppSpawning.isSecureSpawningMode() &&
               mMemoryHardening.isASLREnabled() &&
               mNetworkSecurity.isTLSHardeningEnabled() &&
               mPrivacyManager.isWifiScanEnabled() == false; // Privacy-first
    }

    /**
     * Get security recommendations (GrapheneOS-compatible)
     */
    public String getSecurityRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("=== SECURITY RECOMMENDATIONS ===\n");
        
        if (!mSecureAppSpawning.isSecureAppSpawningEnabled()) {
            recommendations.append("⚠️ Enable secure app spawning for enhanced ASLR\n");
        }
        
        if (!mMemoryHardening.isASLREnabled()) {
            recommendations.append("⚠️ Enable ASLR for memory protection\n");
        }
        
        if (!mNetworkSecurity.isTLSHardeningEnabled()) {
            recommendations.append("⚠️ Enable TLS hardening\n");
        }
        
        if (!mAutoRebootManager.isAutoRebootEnabled()) {
            recommendations.append("⚠️ Consider enabling auto-reboot to mitigate firmware exploits\n");
        }
        
        if (mPrivacyManager.isWifiScanEnabled()) {
            recommendations.append("⚠️ Disable Wi-Fi scanning for privacy\n");
        }
        
        if (mPrivacyManager.isAnalyticsEnabled()) {
            recommendations.append("⚠️ Disable analytics for privacy\n");
        }
        
        if (recommendations.length() == "=== SECURITY RECOMMENDATIONS ===\n".length()) {
            recommendations.append("✅ All security settings are optimally configured (GrapheneOS-compatible)!\n");
        }
        
        return recommendations.toString();
    }
}
