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

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Security auditor for TrickyStore operations
 * Provides comprehensive audit logging to address trust chain compromise concerns
 * @hide
 */
public final class SecurityAuditor {
    private static final String TAG = "TrickyStore.SecurityAuditor";

    private static final String AUDIT_LOG_PATH = "/data/misc/trickystore/security_audit.log";
    private static final int MAX_LOG_ENTRIES = 1000;
    private static final long MAX_LOG_AGE_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    private static volatile SecurityAuditor sInstance;

    private final ConcurrentLinkedQueue<AuditEntry> mAuditQueue = new ConcurrentLinkedQueue<>();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private volatile boolean mInitialized = false;

    private SecurityAuditor() {
        initialize();
    }

    public static SecurityAuditor getInstance() {
        if (sInstance == null) {
            synchronized (SecurityAuditor.class) {
                if (sInstance == null) {
                    sInstance = new SecurityAuditor();
                }
            }
        }
        return sInstance;
    }

    private void initialize() {
        try {
            // Ensure audit log directory exists
            File auditDir = new File("/data/misc/trickystore");
            if (!auditDir.exists()) {
                auditDir.mkdirs();
            }

            // Clean up old audit entries
            cleanupOldEntries();

            mInitialized = true;
            logEvent(AuditEventType.SYSTEM_START, "SecurityAuditor", "initialized", null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize security auditor", e);
            mInitialized = false;
        }
    }

    /**
     * Log a security event
     */
    public void logSecurityEvent(AuditEventType eventType, String component, String action,
            String details, String packageName, String alias) {

        if (!mInitialized) return;

        AuditEntry entry = new AuditEntry(eventType, component, action, details, packageName, alias);
        mAuditQueue.offer(entry);

        // Immediate logging for critical events
        if (eventType == AuditEventType.CERTIFICATE_SPOOFING ||
            eventType == AuditEventType.KEY_GENERATION) {
            writeAuditEntry(entry);
        }

        // Async batch writing for performance
        if (mAuditQueue.size() >= 10) {
            flushAuditLog();
        }
    }

    /**
     * Log certificate spoofing event
     */
    public void logCertificateSpoofing(String packageName, String alias, String profile) {
        logSecurityEvent(AuditEventType.CERTIFICATE_SPOOFING, "KeyStore", "spoof_certificate",
            "Certificate chain spoofed for security alias", packageName, alias);
    }

    /**
     * Log dynamic key generation
     */
    public void logKeyGeneration(String packageName, String alias, boolean success) {
        logSecurityEvent(AuditEventType.KEY_GENERATION, "DynamicKeyManager",
            success ? "generate_key_success" : "generate_key_failed",
            "Dynamic key pair generated", packageName, alias);
    }

    /**
     * Log profile changes
     */
    public void logProfileChange(String packageName, String action, String details) {
        logSecurityEvent(AuditEventType.PROFILE_CHANGE, "ProfileManager", action,
            details, packageName, null);
    }

    /**
     * Log system events
     */
    public void logSystemEvent(String component, String action, String details) {
        logSecurityEvent(AuditEventType.SYSTEM_EVENT, component, action, details, null, null);
    }

    /**
     * Get audit statistics
     */
    public AuditStats getAuditStats() {
        return new AuditStats(mAuditQueue.size(), getLogFileSize());
    }

    /**
     * Flush pending audit entries to disk
     */
    public void flushAuditLog() {
        if (!mInitialized || mAuditQueue.isEmpty()) return;

        AuditEntry entry;
        while ((entry = mAuditQueue.poll()) != null) {
            writeAuditEntry(entry);
        }
    }

    /**
     * Clear audit log (admin function)
     */
    public void clearAuditLog() {
        try {
            File auditFile = new File(AUDIT_LOG_PATH);
            if (auditFile.exists()) {
                auditFile.delete();
                logSystemEvent("SecurityAuditor", "clear_audit_log", "Audit log cleared by system");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear audit log", e);
        }
    }

    private void writeAuditEntry(AuditEntry entry) {
        if (!mInitialized) return;

        try (FileWriter writer = new FileWriter(AUDIT_LOG_PATH, true)) {
            writer.write(formatAuditEntry(entry) + "\n");
        } catch (IOException e) {
            Log.e(TAG, "Failed to write audit entry", e);
        }
    }

    private String formatAuditEntry(AuditEntry entry) {
        return String.format("%s|%s|%s|%s|%s|%s|%s",
            mDateFormat.format(new Date(entry.timestamp)),
            entry.eventType.name(),
            entry.component != null ? entry.component : "N/A",
            entry.action != null ? entry.action : "N/A",
            entry.details != null ? entry.details : "N/A",
            entry.packageName != null ? entry.packageName : "N/A",
            entry.alias != null ? entry.alias : "N/A"
        );
    }

    private void cleanupOldEntries() {
        try {
            File auditFile = new File(AUDIT_LOG_PATH);
            if (!auditFile.exists()) return;

            // Simple cleanup - if file is too large, truncate it
            if (auditFile.length() > 1024 * 1024) { // 1MB limit
                Log.i(TAG, "Audit log too large, truncating");
                try (FileWriter writer = new FileWriter(auditFile, false)) {
                    writer.write("# Audit log truncated at " + mDateFormat.format(new Date()) + "\n");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to cleanup audit entries", e);
        }
    }

    private long getLogFileSize() {
        try {
            File auditFile = new File(AUDIT_LOG_PATH);
            return auditFile.exists() ? auditFile.length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Audit event types
     */
    public enum AuditEventType {
        CERTIFICATE_SPOOFING,
        KEY_GENERATION,
        PROFILE_CHANGE,
        SYSTEM_EVENT,
        SECURITY_VIOLATION
    }

    /**
     * Audit entry
     */
    private static class AuditEntry {
        final AuditEventType eventType;
        final String component;
        final String action;
        final String details;
        final String packageName;
        final String alias;
        final long timestamp;

        AuditEntry(AuditEventType eventType, String component, String action, String details,
                String packageName, String alias) {
            this.eventType = eventType;
            this.component = component;
            this.action = action;
            this.details = details;
            this.packageName = packageName;
            this.alias = alias;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Audit statistics
     */
    public static class AuditStats {
        public final int pendingEntries;
        public final long logFileSize;

        AuditStats(int pendingEntries, long logFileSize) {
            this.pendingEntries = pendingEntries;
            this.logFileSize = logFileSize;
        }

        @Override
        public String toString() {
            return String.format("AuditStats{pending=%d, logSize=%d bytes}",
                pendingEntries, logFileSize);
        }
    }
}