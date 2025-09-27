package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * System-wide ad blocking manager inspired by DivestOS.
 * Implements DNS-based ad blocking and network filtering.
 */
public class AdBlockingManager {
    private static final String TAG = "AdBlockingManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static AdBlockingManager sInstance;
    
    // Ad blocking settings
    private static final String AD_BLOCKING_ENABLED = "ad_blocking_enabled";
    private static final String AD_BLOCKING_MODE = "ad_blocking_mode";
    private static final String AD_BLOCKING_DNS_SERVER = "ad_blocking_dns_server";
    
    // DNS servers for ad blocking
    private static final String DNS_ADGUARD = "94.140.14.14";
    private static final String DNS_CLOUDFLARE = "1.1.1.1";
    private static final String DNS_QUAD9 = "9.9.9.9";
    
    // Blocked domains (common ad servers)
    private static final Set<String> BLOCKED_DOMAINS = new HashSet<>();
    
    static {
        // Common ad domains to block
        BLOCKED_DOMAINS.add("googleadservices.com");
        BLOCKED_DOMAINS.add("googlesyndication.com");
        BLOCKED_DOMAINS.add("googletagmanager.com");
        BLOCKED_DOMAINS.add("doubleclick.net");
        BLOCKED_DOMAINS.add("google-analytics.com");
        BLOCKED_DOMAINS.add("facebook.com");
        BLOCKED_DOMAINS.add("connect.facebook.net");
        BLOCKED_DOMAINS.add("amazon-adsystem.com");
        BLOCKED_DOMAINS.add("amazonadvertising.com");
        BLOCKED_DOMAINS.add("adsystem.amazon.com");
    }
    
    private AdBlockingManager(Context context) {
        mContext = context;
        initializeAdBlocking();
    }
    
    public static synchronized AdBlockingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AdBlockingManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize ad blocking functionality.
     */
    private void initializeAdBlocking() {
        try {
            if (isAdBlockingEnabled()) {
                enableAdBlocking(getAdBlockingMode());
            }
            
            if (DEBUG) {
                Log.d(TAG, "Ad blocking manager initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ad blocking", e);
        }
    }
    
    /**
     * Check if ad blocking is enabled.
     */
    public boolean isAdBlockingEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_ENABLED, 1) != 0;
        } catch (Exception e) {
            return true; // Default to enabled
        }
    }
    
    /**
     * Get ad blocking mode.
     */
    public String getAdBlockingMode() {
        try {
            return Settings.Secure.getString(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_MODE);
        } catch (Exception e) {
            return "dns"; // Default to DNS blocking
        }
    }
    
    /**
     * Enable ad blocking with specified mode.
     */
    public void enableAdBlocking(String mode) {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_ENABLED, 1);
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_MODE, mode);
            
            switch (mode) {
                case "dns":
                    enableDNSAdBlocking();
                    break;
                case "hosts":
                    enableHostsAdBlocking();
                    break;
                case "hybrid":
                    enableHybridAdBlocking();
                    break;
                default:
                    enableDNSAdBlocking();
                    break;
            }
            
            if (DEBUG) {
                Log.d(TAG, "Ad blocking enabled with mode: " + mode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable ad blocking", e);
        }
    }
    
    /**
     * Disable ad blocking.
     */
    public void disableAdBlocking() {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_ENABLED, 0);
            
            // Restore default DNS
            restoreDefaultDNS();
            
            if (DEBUG) {
                Log.d(TAG, "Ad blocking disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable ad blocking", e);
        }
    }
    
    /**
     * Enable DNS-based ad blocking.
     */
    private void enableDNSAdBlocking() {
        try {
            // Set AdGuard DNS server
            setDNSAdBlockingServer(DNS_ADGUARD);
            
            // Set system properties
            SystemProperties.set("ro.security.ad_blocking", "1");
            SystemProperties.set("ro.security.ad_blocking_mode", "dns");
            
            if (DEBUG) {
                Log.d(TAG, "DNS-based ad blocking enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable DNS ad blocking", e);
        }
    }
    
    /**
     * Enable hosts file-based ad blocking.
     */
    private void enableHostsAdBlocking() {
        try {
            // Update hosts file with blocked domains
            updateHostsFile();
            
            // Set system properties
            SystemProperties.set("ro.security.ad_blocking", "1");
            SystemProperties.set("ro.security.ad_blocking_mode", "hosts");
            
            if (DEBUG) {
                Log.d(TAG, "Hosts-based ad blocking enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hosts ad blocking", e);
        }
    }
    
    /**
     * Enable hybrid ad blocking (DNS + hosts).
     */
    private void enableHybridAdBlocking() {
        try {
            // Enable both DNS and hosts blocking
            enableDNSAdBlocking();
            updateHostsFile();
            
            // Set system properties
            SystemProperties.set("ro.security.ad_blocking", "1");
            SystemProperties.set("ro.security.ad_blocking_mode", "hybrid");
            
            if (DEBUG) {
                Log.d(TAG, "Hybrid ad blocking enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hybrid ad blocking", e);
        }
    }
    
    /**
     * Set DNS server for ad blocking.
     */
    private void setDNSAdBlockingServer(String dnsServer) {
        try {
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_DNS_SERVER, dnsServer);
            
            // Set system properties
            SystemProperties.set("ro.security.ad_blocking_dns", dnsServer);
            
            if (DEBUG) {
                Log.d(TAG, "Ad blocking DNS server set to: " + dnsServer);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set DNS ad blocking server", e);
        }
    }
    
    /**
     * Update hosts file with blocked domains.
     */
    private void updateHostsFile() {
        try {
            File hostsFile = new File("/system/etc/hosts");
            if (hostsFile.exists() && hostsFile.canWrite()) {
                try (FileWriter writer = new FileWriter(hostsFile, true)) {
                    writer.write("\n# Ad blocking entries\n");
                    for (String domain : BLOCKED_DOMAINS) {
                        writer.write("127.0.0.1 " + domain + "\n");
                        writer.write("::1 " + domain + "\n");
                    }
                    writer.flush();
                }
                
                if (DEBUG) {
                    Log.d(TAG, "Hosts file updated with blocked domains");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to update hosts file", e);
        }
    }
    
    /**
     * Restore default DNS settings.
     */
    private void restoreDefaultDNS() {
        try {
            // Clear ad blocking DNS settings
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.AD_BLOCKING_DNS_SERVER, "");
            
            // Clear system properties
            SystemProperties.set("ro.security.ad_blocking", "0");
            SystemProperties.set("ro.security.ad_blocking_dns", "");
            
            if (DEBUG) {
                Log.d(TAG, "Default DNS settings restored");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore default DNS", e);
        }
    }
    
    /**
     * Check if a domain should be blocked.
     */
    public boolean isDomainBlocked(String domain) {
        try {
            if (!isAdBlockingEnabled()) {
                return false;
            }
            
            // Check against blocked domains list
            for (String blockedDomain : BLOCKED_DOMAINS) {
                if (domain.contains(blockedDomain)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get ad blocking status.
     */
    public String getAdBlockingStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Ad Blocking Status:\n");
        status.append("Enabled: ").append(isAdBlockingEnabled() ? "YES" : "NO").append("\n");
        
        if (isAdBlockingEnabled()) {
            String mode = getAdBlockingMode();
            status.append("Mode: ").append(mode.toUpperCase()).append("\n");
            
            if ("dns".equals(mode) || "hybrid".equals(mode)) {
                String dnsServer = Settings.Secure.getString(mContext.getContentResolver(),
                        Settings.Secure.AD_BLOCKING_DNS_SERVER);
                status.append("DNS Server: ").append(dnsServer != null ? dnsServer : "Not set").append("\n");
            }
            
            status.append("Blocked Domains: ").append(BLOCKED_DOMAINS.size()).append("\n");
        }
        
        return status.toString();
    }
}
