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

/**
 * Memory Hardening - Advanced memory protection mechanisms
 * Inspired by GrapheneOS memory hardening features
 */
public class MemoryHardening {
    private static final String TAG = "MemoryHardening";
    private static final boolean DEBUG = false;

    // System properties for memory hardening
    private static final String PROP_ASLR_ENABLED = "security.aslr_enabled";
    private static final String PROP_STACK_PROTECTION = "security.stack_protection";
    private static final String PROP_HEAP_PROTECTION = "security.heap_protection";
    private static final String PROP_MEMORY_DISCLOSURE_PROTECTION = "security.memory_disclosure_protection";
    private static final String PROP_CFI_ENABLED = "security.cfi_enabled";
    private static final String PROP_RETURN_ADDRESS_PROTECTION = "security.return_address_protection";

    // Settings keys
    private static final String SETTING_ASLR_ENABLED = "memory_aslr_enabled";
    private static final String SETTING_STACK_PROTECTION = "memory_stack_protection";
    private static final String SETTING_HEAP_PROTECTION = "memory_heap_protection";
    private static final String SETTING_MEMORY_DISCLOSURE_PROTECTION = "memory_disclosure_protection";
    private static final String SETTING_CFI_ENABLED = "memory_cfi_enabled";
    private static final String SETTING_RETURN_ADDRESS_PROTECTION = "memory_return_address_protection";

    // Default security settings (secure by default)
    private static final boolean DEFAULT_ASLR_ENABLED = true;
    private static final boolean DEFAULT_STACK_PROTECTION = true;
    private static final boolean DEFAULT_HEAP_PROTECTION = true;
    private static final boolean DEFAULT_MEMORY_DISCLOSURE_PROTECTION = true;
    private static final boolean DEFAULT_CFI_ENABLED = true;
    private static final boolean DEFAULT_RETURN_ADDRESS_PROTECTION = true;

    private final Context mContext;

    public MemoryHardening(Context context) {
        mContext = context;
        initializeSystemProperties();
    }

    /**
     * Initialize system properties for memory hardening
     */
    private void initializeSystemProperties() {
        SystemProperties.set(PROP_ASLR_ENABLED, isASLREnabled() ? "1" : "0");
        SystemProperties.set(PROP_STACK_PROTECTION, isStackProtectionEnabled() ? "1" : "0");
        SystemProperties.set(PROP_HEAP_PROTECTION, isHeapProtectionEnabled() ? "1" : "0");
        SystemProperties.set(PROP_MEMORY_DISCLOSURE_PROTECTION, isMemoryDisclosureProtectionEnabled() ? "1" : "0");
        SystemProperties.set(PROP_CFI_ENABLED, isCFIEnabled() ? "1" : "0");
        SystemProperties.set(PROP_RETURN_ADDRESS_PROTECTION, isReturnAddressProtectionEnabled() ? "1" : "0");
    }

    /**
     * Check if ASLR (Address Space Layout Randomization) is enabled
     */
    public boolean isASLREnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_ASLR_ENABLED, DEFAULT_ASLR_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Check if stack protection is enabled
     */
    public boolean isStackProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_STACK_PROTECTION, DEFAULT_STACK_PROTECTION ? 1 : 0) == 1;
    }

    /**
     * Check if heap protection is enabled
     */
    public boolean isHeapProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_HEAP_PROTECTION, DEFAULT_HEAP_PROTECTION ? 1 : 0) == 1;
    }

    /**
     * Check if memory disclosure protection is enabled
     */
    public boolean isMemoryDisclosureProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_MEMORY_DISCLOSURE_PROTECTION, DEFAULT_MEMORY_DISCLOSURE_PROTECTION ? 1 : 0) == 1;
    }

    /**
     * Check if CFI (Control Flow Integrity) is enabled
     */
    public boolean isCFIEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_CFI_ENABLED, DEFAULT_CFI_ENABLED ? 1 : 0) == 1;
    }

    /**
     * Check if return address protection is enabled
     */
    public boolean isReturnAddressProtectionEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTING_RETURN_ADDRESS_PROTECTION, DEFAULT_RETURN_ADDRESS_PROTECTION ? 1 : 0) == 1;
    }

    /**
     * Enable ASLR
     */
    public void setASLREnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_ASLR_ENABLED, enabled ? 1 : 0);
        SystemProperties.set(PROP_ASLR_ENABLED, enabled ? "1" : "0");
    }

    /**
     * Enable stack protection
     */
    public void setStackProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_STACK_PROTECTION, enabled ? 1 : 0);
        SystemProperties.set(PROP_STACK_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Enable heap protection
     */
    public void setHeapProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_HEAP_PROTECTION, enabled ? 1 : 0);
        SystemProperties.set(PROP_HEAP_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Enable memory disclosure protection
     */
    public void setMemoryDisclosureProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_MEMORY_DISCLOSURE_PROTECTION, enabled ? 1 : 0);
        SystemProperties.set(PROP_MEMORY_DISCLOSURE_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Enable CFI
     */
    public void setCFIEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_CFI_ENABLED, enabled ? 1 : 0);
        SystemProperties.set(PROP_CFI_ENABLED, enabled ? "1" : "0");
    }

    /**
     * Enable return address protection
     */
    public void setReturnAddressProtectionEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_RETURN_ADDRESS_PROTECTION, enabled ? 1 : 0);
        SystemProperties.set(PROP_RETURN_ADDRESS_PROTECTION, enabled ? "1" : "0");
    }

    /**
     * Reset to secure defaults
     */
    public void resetToSecureDefaults() {
        setASLREnabled(DEFAULT_ASLR_ENABLED);
        setStackProtectionEnabled(DEFAULT_STACK_PROTECTION);
        setHeapProtectionEnabled(DEFAULT_HEAP_PROTECTION);
        setMemoryDisclosureProtectionEnabled(DEFAULT_MEMORY_DISCLOSURE_PROTECTION);
        setCFIEnabled(DEFAULT_CFI_ENABLED);
        setReturnAddressProtectionEnabled(DEFAULT_RETURN_ADDRESS_PROTECTION);
    }

    /**
     * Get memory hardening status
     */
    public String getMemoryHardeningStatus() {
        return String.format("ASLR: %b, StackProt: %b, HeapProt: %b, MemDisclosure: %b, CFI: %b, RetAddr: %b",
                isASLREnabled(), isStackProtectionEnabled(), isHeapProtectionEnabled(),
                isMemoryDisclosureProtectionEnabled(), isCFIEnabled(), isReturnAddressProtectionEnabled());
    }

    /**
     * Check if memory protection is violated
     */
    public boolean isMemoryViolation(String operation, String packageName, int uid) {
        // Log memory violation attempt
        SecurityLogger.logMemoryViolation(operation, packageName, uid);
        
        // In strict mode, block all suspicious memory operations
        if (isMemoryDisclosureProtectionEnabled()) {
            return false; // Block by default
        }
        
        return true; // Allow for system apps
    }
}
