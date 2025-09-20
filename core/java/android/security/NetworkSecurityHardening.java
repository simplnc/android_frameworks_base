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
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;

/**
 * Network Security Hardening - Advanced network security measures
 * Inspired by GrapheneOS network security hardening
 */
public class NetworkSecurityHardening {
    private static final String TAG = "NetworkSecurityHardening";
    private static final boolean DEBUG = false;

    // System properties for network security
    private static final String PROP_TLS_HARDENING = "security.tls_hardening";
    private static final String PROP_CERTIFICATE_PINNING = "security.certificate_pinning";
    private static final String PROP_DNS_SECURITY = "security.dns_security";
    private static final String PROP_NETWORK_MONITORING = "security.network_monitoring";
    private static final String PROP_TRAFFIC_ANALYSIS_PROTECTION = "security.traffic_analysis_protection";

    // Settings keys
    private static final String SETTING_TLS_HARDENING = "network_tls_hardening";
    private static final String SETTING_CERTIFICATE_PINNING = "network_certificate_pinning";
    private static final String SETTING_DNS_SECURITY = "network_dns_security";
    private static final String SETTING_NETWORK_MONITORING = "network_monitoring";
    private static final String SETTING_TRAFFIC_ANALYSIS_PROTECTION = "network_traffic_analysis_protection";

    // Default security settings (secure by default)
    private static final boolean DEFAULT_TLS_HARDENING = true;
    private static final boolean DEFAULT_CERTIFICATE_PINNING = true;
    private static final boolean DEFAULT_DNS_SECURITY = true;
    private static final boolean DEFAULT_NETWORK_MONITORING = true;
    private static final boolean DEFAULT_TRAFFIC_ANALYSIS_PROTECTION = true;

    // Allowed TLS versions (secure defaults)
    private static final Set<String> ALLOWED_TLS_VERSIONS = new HashSet<>();
    static {
        ALLOWED_TLS_VERSIONS.add("TLSv1.2");
        ALLOWED_TLS_VERSIONS.add("TLSv1.3");
    }

    // Blocked TLS versions
    private static final Set<String> BLOCKED_TLS_VERSIONS = new HashSet<>();
    static {
        BLOCKED_TLS_VERSIONS.add("SSLv2");
        BLOCKED_TLS_VERSIONS.add("SSLv3");
        BLOCKED_TLS_VERSIONS.add("TLSv1.0");
        BLOCKED_TLS_VERSIONS.add("TLSv1.1");
    }

    // Blocked cipher suites
    private static final Set<String> BLOCKED_CIPHER_SUITES = new HashSet<>();
    static {
        BLOCKED_CIPHER_SUITES.add("NULL");
        BLOCKED_CIPHER_SUITES.add("RC4");
        BLOCKED_CIPHER_SUITES.add("DES");
        BLOCKED_CIPHER_SUITES.add("3DES");
        BLOCKED_CIPHER_SUITES.add("MD5");
        BLOCKED_CIPHER_SUITES.add("SHA1");
    }

    private final Context mContext;

    public NetworkSecurityHardening(Context context) {
        mContext = context;
        initializeSystemProperties();
    }

    /**
     * Initialize system properties for network security
     */
    private void initializeSystemProperties() {
        SystemProperties.set(PROP_TLS_HARDENING, isTLSHardeningEnabled() ? "1" : "0");
        SystemProperties.set(PROP_CERTIFICATE_PINNING, isCertificatePinningEnabled() ? "1" : "0");
        SystemProperties.set(PROP_DNS_SECURITY, isDNSSecurityEnabled() ? "1" : "0");
        SystemProperties.set(PROP_NETWORK_MONITORING, isNetworkMonitoringEnabled() ? "1" : "0");
        SystemProperties.set(PROP_TRAFFIC_ANALYSIS_PROTECTION, isTrafficAnalysisProtectionEnabled() ? "1" : "0");
    }

    /**
     * Check if TLS hardening is enabled
     */
    public boolean isTLSHardeningEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_TLS_HARDENING, DEFAULT_TLS_HARDENING ? 1 : 0) == 1;
    }

    /**
     * Check if certificate pinning is enabled
     */
    public boolean isCertificatePinningEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_CERTIFICATE_PINNING, DEFAULT_CERTIFICATE_PINNING ? 1 : 0) == 1;
    }

    /**
     * Check if DNS security is enabled
     */
    public boolean isDNSSecurityEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_DNS_SECURITY, DEFAULT_DNS_SECURITY ? 1 : 0) == 1;
    }

    /**
     * Check if network monitoring is enabled
     */
    public boolean isNetworkMonitoringEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_NETWORK_MONITORING, DEFAULT_NETWORK_MONITORING ? 1 : 0) == 1;
    }

    /**
     * Check if traffic analysis protection is enabled
     */
    public boolean isTrafficAnalysisProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_TRAFFIC_ANALYSIS_PROTECTION, DEFAULT_TRAFFIC_ANALYSIS_PROTECTION ? 1 : 0) == 1;
    }

    /**
     * Validate TLS version for security
     */
    public boolean isTLSVersionAllowed(String tlsVersion) {
        if (!isTLSHardeningEnabled()) {
            return true;
        }

        // Block insecure TLS versions
        if (BLOCKED_TLS_VERSIONS.contains(tlsVersion)) {
            SecurityLogger.logNetworkSecurity("Blocked TLS version: " + tlsVersion, 
                    "system", Process.myUid());
            return false;
        }

        // Allow secure TLS versions
        return ALLOWED_TLS_VERSIONS.contains(tlsVersion);
    }

    /**
     * Validate cipher suite for security
     */
    public boolean isCipherSuiteAllowed(String cipherSuite) {
        if (!isTLSHardeningEnabled()) {
            return true;
        }

        // Block insecure cipher suites
        for (String blocked : BLOCKED_CIPHER_SUITES) {
            if (cipherSuite.contains(blocked)) {
                SecurityLogger.logNetworkSecurity("Blocked cipher suite: " + cipherSuite, 
                        "system", Process.myUid());
                return false;
            }
        }

        return true;
    }

    /**
     * Validate network connection for security
     */
    public boolean isNetworkConnectionAllowed(String hostname, int port, String protocol) {
        // Log network connection attempt
        SecurityLogger.logNetworkSecurity("Network connection: " + hostname + ":" + port + " " + protocol, 
                "system", Process.myUid());

        // Block cleartext connections in secure mode
        if (isTLSHardeningEnabled() && !protocol.equals("https") && port != 80) {
            SecurityLogger.logNetworkSecurity("Blocked cleartext connection: " + hostname, 
                    "system", Process.myUid());
            return false;
        }

        return true;
    }

    /**
     * Set TLS hardening enabled
     */
    public void setTLSHardeningEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_TLS_HARDENING, enabled ? 1 : 0);
        SystemProperties.set(PROP_TLS_HARDENING, enabled ? "1" : "0");
    }

    /**
     * Set certificate pinning enabled
     */
    public void setCertificatePinningEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_CERTIFICATE_PINNING, enabled ? 1 : 0);
        SystemProperties.set(PROP_CERTIFICATE_PINNING, enabled ? "1" : "0");
    }

    /**
     * Set DNS security enabled
     */
    public void setDNSSecurityEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_DNS_SECURITY, enabled ? 1 : 0);
        SystemProperties.set(PROP_DNS_SECURITY, enabled ? "1" : "0");
    }

    /**
     * Set network monitoring enabled
     */
    public void setNetworkMonitoringEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_NETWORK_MONITORING, enabled ? 1 : 0);
        SystemProperties.set(PROP_NETWORK_MONITORING, enabled ? "1" : "0");
    }

    /**
     * Set traffic analysis protection enabled
     */
    public void setTrafficAnalysisProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_TRAFFIC_ANALYSIS_PROTECTION, enabled ? 1 : 0);
        SystemProperties.set(PROP_TRAFFIC_ANALYSIS_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Reset to secure defaults
     */
    public void resetToSecureDefaults() {
        setTLSHardeningEnabled(DEFAULT_TLS_HARDENING);
        setCertificatePinningEnabled(DEFAULT_CERTIFICATE_PINNING);
        setDNSSecurityEnabled(DEFAULT_DNS_SECURITY);
        setNetworkMonitoringEnabled(DEFAULT_NETWORK_MONITORING);
        setTrafficAnalysisProtectionEnabled(DEFAULT_TRAFFIC_ANALYSIS_PROTECTION);
    }

    /**
     * Get network security status
     */
    public String getNetworkSecurityStatus() {
        return String.format("TLS: %b, CertPinning: %b, DNS: %b, Monitoring: %b, TrafficProt: %b",
                isTLSHardeningEnabled(), isCertificatePinningEnabled(), isDNSSecurityEnabled(),
                isNetworkMonitoringEnabled(), isTrafficAnalysisProtectionEnabled());
    }
}
