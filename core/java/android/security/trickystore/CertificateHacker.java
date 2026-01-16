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

import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @hide
 */
public final class CertificateHacker {
    private static final String TAG = "CertificateHacker";

    // OID for Android key attestation
    private static final String KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17";

    static {
        // Ensure BouncyCastle is available
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CertificateHacker() {}

    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subject)
            throws Exception {
        X500Name issuer = new X500Name(subject);
        X500Name subjectName = new X500Name(subject);
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10);

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subjectName, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC")
            .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public static X509Certificate[] generateAttestationCertificateChain(
            KeyPair attestedKeyPair, PrivateKey attestationKey, String subject,
            int[] attestationApplicationId, byte[] attestationChallenge,
            int deviceIdAttestationVersion) throws Exception {

        // Input validation
        if (attestedKeyPair == null || attestationKey == null) {
            throw new IllegalArgumentException("Key pair and attestation key cannot be null");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }
        if (attestationChallenge == null || attestationChallenge.length == 0) {
            throw new IllegalArgumentException("Attestation challenge cannot be null or empty");
        }

        try {
            // Generate attested certificate with key description extension
            X509Certificate attestedCert = generateAttestedCertificate(
                attestedKeyPair, attestationKey, subject, attestationApplicationId,
                attestationChallenge, deviceIdAttestationVersion);

            // Generate batch certificate (intermediate CA)
            KeyPair batchKeyPair = CertificateGenerator.generateEcKeyPair();
            X509Certificate batchCert = generateBatchCertificate(batchKeyPair, attestationKey, subject);

            // Return chain: attested cert, batch cert
            return new X509Certificate[]{attestedCert, batchCert};
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate attestation certificate chain", e);
            throw new RuntimeException("Certificate chain generation failed", e);
        }
    }

    public static X509Certificate generateAttestedCertificate(
            KeyPair attestedKeyPair, PrivateKey attestationKey, String subject,
            int[] attestationApplicationId, byte[] attestationChallenge,
            int deviceIdAttestationVersion) throws Exception {

        X500Name issuer = new X500Name("CN=Android Keystore Key");
        X500Name subjectName = new X500Name(subject);
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subjectName, attestedKeyPair.getPublic());

        // Add key description extension for attestation
        byte[] keyDescription = createKeyDescription(attestationApplicationId, attestationChallenge, deviceIdAttestationVersion);
        certBuilder.addExtension(new ASN1ObjectIdentifier(KEY_DESCRIPTION_OID), false, keyDescription);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithECDSA").setProvider("BC")
            .build(attestationKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public static X509Certificate generateBatchCertificate(
            KeyPair batchKeyPair, PrivateKey caKey, String subject) throws Exception {

        X500Name issuer = new X500Name("CN=Android KeyStore");
        X500Name subjectName = new X500Name("CN=Android KeyStore");
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 25);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subjectName, batchKeyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC")
            .build(caKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    private static byte[] createKeyDescription(int[] attestationApplicationId,
            byte[] attestationChallenge, int deviceIdAttestationVersion) throws Exception {

        List<ASN1Encodable> keyDescriptionElements = new ArrayList<>();

        // Version
        keyDescriptionElements.add(new ASN1Integer(deviceIdAttestationVersion));

        // Attestation Challenge
        keyDescriptionElements.add(new DEROctetString(attestationChallenge));

        // Software enforced authorization list
        keyDescriptionElements.add(createAuthorizationList());

        // Tee enforced authorization list
        keyDescriptionElements.add(createAuthorizationList());

        // Unique ID
        keyDescriptionElements.add(new DEROctetString(AttestationUtils.getBootKey()));

        // App ID (attestationApplicationId)
        if (attestationApplicationId != null) {
            keyDescriptionElements.add(new DEROctetString(intArrayToByteArray(attestationApplicationId)));
        }

        return new DERSequence(keyDescriptionElements.toArray(new ASN1Encodable[0])).getEncoded();
    }

    private static ASN1Sequence createAuthorizationList() throws Exception {
        // Create empty authorization list for simplicity
        return new DERSequence();
    }

    private static byte[] intArrayToByteArray(int[] ints) {
        byte[] bytes = new byte[ints.length * 4];
        for (int i = 0; i < ints.length; i++) {
            int value = ints[i];
            bytes[i * 4] = (byte) (value >>> 24);
            bytes[i * 4 + 1] = (byte) (value >>> 16);
            bytes[i * 4 + 2] = (byte) (value >>> 8);
            bytes[i * 4 + 3] = (byte) value;
        }
        return bytes;
    }
}