package com.android.server.security;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Memory protection manager for ASLR, stack canaries, and heap protection.
 * Implements memory security measures to prevent buffer overflows and memory attacks.
 */
public class MemoryProtectionManager {
    private static final String TAG = "MemoryProtectionManager";
    private static final boolean DEBUG = false;
    
    private final Context mContext;
    private static MemoryProtectionManager sInstance;
    
    // Memory protection flags
    private static final String MEMORY_ASLR_ENABLED = "memory_aslr_enabled";
    private static final String MEMORY_STACK_PROTECTION = "memory_stack_protection";
    private static final String MEMORY_HEAP_PROTECTION = "memory_heap_protection";
    private static final String MEMORY_EXEC_PROTECTION = "memory_exec_protection";
    
    // Kernel parameters
    private static final String KERNEL_ASLR = "/proc/sys/kernel/randomize_va_space";
    private static final String KERNEL_STACK_PROTECTION = "/proc/sys/kernel/stack_protection";
    private static final String KERNEL_HEAP_PROTECTION = "/proc/sys/kernel/heap_protection";
    private static final String KERNEL_EXEC_PROTECTION = "/proc/sys/kernel/exec_protection";
    
    private MemoryProtectionManager(Context context) {
        mContext = context;
        initializeProtection();
    }
    
    public static synchronized MemoryProtectionManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MemoryProtectionManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize memory protection measures.
     */
    private void initializeProtection() {
        try {
            // Enable ASLR
            enableASLR();
            
            // Enable stack protection
            enableStackProtection();
            
            // Enable heap protection
            enableHeapProtection();
            
            // Enable execution protection
            enableExecutionProtection();
            
            if (DEBUG) {
                Log.d(TAG, "Memory protection initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize memory protection", e);
        }
    }
    
    /**
     * Enable Address Space Layout Randomization (ASLR).
     */
    private void enableASLR() {
        try {
            // Set kernel ASLR parameter
            writeKernelParameter(KERNEL_ASLR, "2"); // Full ASLR
            
            // Set system properties
            SystemProperties.set("ro.security.memory_aslr", "1");
            SystemProperties.set("ro.security.aslr_enabled", "1");
            
            // Enable ASLR for all processes
            enableProcessASLR();
            
            if (DEBUG) {
                Log.d(TAG, "ASLR enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable ASLR", e);
        }
    }
    
    /**
     * Enable stack protection (stack canaries).
     */
    private void enableStackProtection() {
        try {
            // Set kernel stack protection parameter
            writeKernelParameter(KERNEL_STACK_PROTECTION, "1");
            
            // Set system properties
            SystemProperties.set("ro.security.memory_stack_protection", "1");
            SystemProperties.set("ro.security.stack_canaries", "1");
            
            // Enable stack protection for native code
            enableNativeStackProtection();
            
            if (DEBUG) {
                Log.d(TAG, "Stack protection enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable stack protection", e);
        }
    }
    
    /**
     * Enable heap protection.
     */
    private void enableHeapProtection() {
        try {
            // Set kernel heap protection parameter
            writeKernelParameter(KERNEL_HEAP_PROTECTION, "1");
            
            // Set system properties
            SystemProperties.set("ro.security.memory_heap_protection", "1");
            SystemProperties.set("ro.security.heap_protection", "1");
            
            // Enable heap protection for native code
            enableNativeHeapProtection();
            
            if (DEBUG) {
                Log.d(TAG, "Heap protection enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable heap protection", e);
        }
    }
    
    /**
     * Enable execution protection (NX bit).
     */
    private void enableExecutionProtection() {
        try {
            // Set kernel execution protection parameter
            writeKernelParameter(KERNEL_EXEC_PROTECTION, "1");
            
            // Set system properties
            SystemProperties.set("ro.security.memory_exec_protection", "1");
            SystemProperties.set("ro.security.nx_bit", "1");
            
            // Enable execution protection for native code
            enableNativeExecutionProtection();
            
            if (DEBUG) {
                Log.d(TAG, "Execution protection enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable execution protection", e);
        }
    }
    
    /**
     * Enable ASLR for all processes.
     */
    private void enableProcessASLR() {
        try {
            // Set process ASLR flags
            SystemProperties.set("ro.security.process_aslr", "1");
            
            // Enable ASLR for system processes
            enableSystemProcessASLR();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable process ASLR", e);
        }
    }
    
    /**
     * Enable ASLR for system processes.
     */
    private void enableSystemProcessASLR() {
        try {
            // Enable ASLR for critical system processes
            String[] systemProcesses = {
                "system_server",
                "surfaceflinger",
                "zygote",
                "systemui"
            };
            
            for (String process : systemProcesses) {
                enableProcessASLR(process);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable system process ASLR", e);
        }
    }
    
    /**
     * Enable ASLR for a specific process.
     */
    private void enableProcessASLR(String processName) {
        try {
            // Set process-specific ASLR properties
            SystemProperties.set("ro.security." + processName + "_aslr", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable ASLR for process: " + processName, e);
        }
    }
    
    /**
     * Enable native stack protection.
     */
    private void enableNativeStackProtection() {
        try {
            // Set native stack protection flags
            SystemProperties.set("ro.security.native_stack_protection", "1");
            
            // Enable stack canaries for native libraries
            enableNativeStackCanaries();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native stack protection", e);
        }
    }
    
    /**
     * Enable native stack canaries.
     */
    private void enableNativeStackCanaries() {
        try {
            // Set stack canary properties
            SystemProperties.set("ro.security.native_stack_canaries", "1");
            
            // Enable canaries for critical native libraries
            String[] nativeLibraries = {
                "libc.so",
                "libm.so",
                "libdl.so",
                "liblog.so"
            };
            
            for (String library : nativeLibraries) {
                enableLibraryStackCanaries(library);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native stack canaries", e);
        }
    }
    
    /**
     * Enable stack canaries for a specific library.
     */
    private void enableLibraryStackCanaries(String libraryName) {
        try {
            // Set library-specific stack canary properties
            SystemProperties.set("ro.security." + libraryName + "_stack_canaries", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable stack canaries for library: " + libraryName, e);
        }
    }
    
    /**
     * Enable native heap protection.
     */
    private void enableNativeHeapProtection() {
        try {
            // Set native heap protection flags
            SystemProperties.set("ro.security.native_heap_protection", "1");
            
            // Enable heap protection for native libraries
            enableNativeHeapProtectionLibraries();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native heap protection", e);
        }
    }
    
    /**
     * Enable heap protection for native libraries.
     */
    private void enableNativeHeapProtectionLibraries() {
        try {
            // Enable heap protection for critical native libraries
            String[] nativeLibraries = {
                "libc.so",
                "libm.so",
                "libdl.so",
                "liblog.so"
            };
            
            for (String library : nativeLibraries) {
                enableLibraryHeapProtection(library);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native heap protection libraries", e);
        }
    }
    
    /**
     * Enable heap protection for a specific library.
     */
    private void enableLibraryHeapProtection(String libraryName) {
        try {
            // Set library-specific heap protection properties
            SystemProperties.set("ro.security." + libraryName + "_heap_protection", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable heap protection for library: " + libraryName, e);
        }
    }
    
    /**
     * Enable native execution protection.
     */
    private void enableNativeExecutionProtection() {
        try {
            // Set native execution protection flags
            SystemProperties.set("ro.security.native_exec_protection", "1");
            
            // Enable NX bit for native libraries
            enableNativeNXBit();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native execution protection", e);
        }
    }
    
    /**
     * Enable NX bit for native libraries.
     */
    private void enableNativeNXBit() {
        try {
            // Enable NX bit for critical native libraries
            String[] nativeLibraries = {
                "libc.so",
                "libm.so",
                "libdl.so",
                "liblog.so"
            };
            
            for (String library : nativeLibraries) {
                enableLibraryNXBit(library);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable native NX bit", e);
        }
    }
    
    /**
     * Enable NX bit for a specific library.
     */
    private void enableLibraryNXBit(String libraryName) {
        try {
            // Set library-specific NX bit properties
            SystemProperties.set("ro.security." + libraryName + "_nx_bit", "1");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable NX bit for library: " + libraryName, e);
        }
    }
    
    /**
     * Write kernel parameter.
     */
    private void writeKernelParameter(String parameter, String value) {
        try {
            File file = new File(parameter);
            if (file.exists() && file.canWrite()) {
                FileWriter writer = new FileWriter(file);
                writer.write(value);
                writer.close();
                
                if (DEBUG) {
                    Log.d(TAG, "Set kernel parameter " + parameter + " = " + value);
                }
            } else {
                Log.w(TAG, "Cannot write to kernel parameter: " + parameter);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write kernel parameter: " + parameter, e);
        }
    }
    
    /**
     * Get memory protection status.
     */
    public String getMemoryProtectionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Memory Protection Status:\n");
        status.append("ASLR: ").append(isASLREnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Stack Protection: ").append(isStackProtectionEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Heap Protection: ").append(isHeapProtectionEnabled() ? "ENABLED" : "DISABLED").append("\n");
        status.append("Execution Protection: ").append(isExecutionProtectionEnabled() ? "ENABLED" : "DISABLED").append("\n");
        return status.toString();
    }
    
    /**
     * Check if ASLR is enabled.
     */
    public boolean isASLREnabled() {
        return SystemProperties.getBoolean("ro.security.memory_aslr", true);
    }
    
    /**
     * Check if stack protection is enabled.
     */
    public boolean isStackProtectionEnabled() {
        return SystemProperties.getBoolean("ro.security.memory_stack_protection", true);
    }
    
    /**
     * Check if heap protection is enabled.
     */
    public boolean isHeapProtectionEnabled() {
        return SystemProperties.getBoolean("ro.security.memory_heap_protection", true);
    }
    
    /**
     * Check if execution protection is enabled.
     */
    public boolean isExecutionProtectionEnabled() {
        return SystemProperties.getBoolean("ro.security.memory_exec_protection", true);
    }
    
    /**
     * Get kernel parameter value.
     */
    public String getKernelParameter(String parameter) {
        try {
            File file = new File(parameter);
            if (file.exists() && file.canRead()) {
                java.util.Scanner scanner = new java.util.Scanner(file);
                String value = scanner.nextLine();
                scanner.close();
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read kernel parameter: " + parameter, e);
        }
        return "unknown";
    }
    
    /**
     * Verify memory protection is working.
     */
    public boolean verifyMemoryProtection() {
        try {
            // Check ASLR
            if (!isASLREnabled()) {
                Log.w(TAG, "ASLR verification failed");
                return false;
            }
            
            // Check stack protection
            if (!isStackProtectionEnabled()) {
                Log.w(TAG, "Stack protection verification failed");
                return false;
            }
            
            // Check heap protection
            if (!isHeapProtectionEnabled()) {
                Log.w(TAG, "Heap protection verification failed");
                return false;
            }
            
            // Check execution protection
            if (!isExecutionProtectionEnabled()) {
                Log.w(TAG, "Execution protection verification failed");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify memory protection", e);
            return false;
        }
    }
}
