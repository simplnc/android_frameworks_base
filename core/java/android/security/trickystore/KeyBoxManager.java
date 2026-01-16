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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @hide
 */
public final class KeyBoxManager {
    private static final String TAG = "KeyBoxManager";
    private static final String KEYBOX_PATH = "/data/misc/keybox/keybox.xml";

    private static KeyBoxManager sInstance;
    private final Map<String, KeyBoxEntry> mKeyBoxEntries = new HashMap<>();

    private KeyBoxManager() {
        loadKeyBox();
    }

    public static synchronized KeyBoxManager getInstance() {
        if (sInstance == null) {
            sInstance = new KeyBoxManager();
        }
        return sInstance;
    }

    public KeyBoxEntry getKeyBoxEntry(String alias) {
        return mKeyBoxEntries.get(alias);
    }

    public void addKeyBoxEntry(String alias, KeyBoxEntry entry) {
        mKeyBoxEntries.put(alias, entry);
    }

    private void loadKeyBox() {
        File keyBoxFile = new File(KEYBOX_PATH);
        if (!keyBoxFile.exists()) {
            Log.i(TAG, "KeyBox file does not exist: " + KEYBOX_PATH);
            return;
        }

        try (FileInputStream fis = new FileInputStream(keyBoxFile)) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fis);

            NodeList keyNodes = doc.getElementsByTagName("Key");
            for (int i = 0; i < keyNodes.getLength(); i++) {
                Element keyElement = (Element) keyNodes.item(i);
                String alias = keyElement.getAttribute("alias");

                NodeList certNodes = keyElement.getElementsByTagName("CertificateChain");
                if (certNodes.getLength() > 0) {
                    Element certChainElement = (Element) certNodes.item(0);
                    NodeList certElements = certChainElement.getElementsByTagName("Certificate");

                    X509Certificate[] certChain = new X509Certificate[certElements.getLength()];
                    for (int j = 0; j < certElements.getLength(); j++) {
                        Element certElement = (Element) certElements.item(j);
                        String certData = certElement.getTextContent();
                        // Parse certificate from base64 or PEM format
                        certChain[j] = parseCertificate(certData);
                    }

                    KeyBoxEntry entry = new KeyBoxEntry(certChain);
                    mKeyBoxEntries.put(alias, entry);
                }
            }

            Log.i(TAG, "Loaded " + mKeyBoxEntries.size() + " keybox entries");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load keybox", e);
        }
    }

    private X509Certificate parseCertificate(String certData) {
        if (certData == null || certData.trim().isEmpty()) {
            Log.w(TAG, "Certificate data is null or empty");
            return null;
        }

        try {
            // Basic validation - check if it's base64-like
            if (!isValidBase64(certData)) {
                Log.w(TAG, "Certificate data is not valid base64");
                return null;
            }

            // Decode base64 certificate data
            byte[] certBytes = android.util.Base64.decode(certData, android.util.Base64.DEFAULT);

            // Create certificate from bytes
            java.security.cert.CertificateFactory certFactory =
                java.security.cert.CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(
                new java.io.ByteArrayInputStream(certBytes));

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse certificate", e);
            return null;
        }
    }

    private boolean isValidBase64(String data) {
        if (data == null) return false;
        // Basic validation - check for valid base64 characters
        return data.matches("^[A-Za-z0-9+/]+={0,2}$") && data.length() % 4 == 0;
    }

    public static class KeyBoxEntry {
        private final X509Certificate[] mCertificateChain;

        public KeyBoxEntry(X509Certificate[] certificateChain) {
            mCertificateChain = certificateChain;
        }

        public X509Certificate[] getCertificateChain() {
            return mCertificateChain;
        }
    }
}