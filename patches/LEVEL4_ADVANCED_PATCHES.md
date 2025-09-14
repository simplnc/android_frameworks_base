# LEVEL 4: ADVANCED SECURITY PATCHES
## These patches are advanced and may require kernel/network modifications

### 12. grapheneos-network-security-1.patch
- **Safety**: ⚠️ MODERATE-HIGH
- **Purpose**: Enhanced MAC randomization
- **Risk**: Moderate-High - Affects network connectivity
- **Apply Order**: 12
- **Testing Required**: Test WiFi connectivity and MAC randomization

### 13. grapheneos-network-security-2.patch
- **Safety**: ⚠️ MODERATE-HIGH
- **Purpose**: Disable captive portal checks
- **Risk**: Moderate-High - May affect network connectivity detection
- **Apply Order**: 13
- **Testing Required**: Test captive portal detection and network connectivity

### 14. grapheneos-memory-hardening-1.patch
- **Safety**: ⚠️ HIGH RISK
- **Purpose**: Enhanced stack protection and memory hardening
- **Risk**: High - Affects memory management and may cause instability
- **Apply Order**: 14
- **Testing Required**: Extensive testing for memory-related crashes

### 15. grapheneos-exec-spawning.patch
- **Safety**: ⚠️ HIGH RISK
- **Purpose**: Implement exec-based spawning
- **Risk**: High - Affects process creation and may cause boot issues
- **Apply Order**: 15
- **Testing Required**: Test boot process and app launching

### 16. grapheneos-privacy-hardening-4.patch
- **Safety**: ⚠️ HIGH RISK
- **Purpose**: Network privacy protection and DNS hardening
- **Risk**: High - NetworkMonitor.java may not exist in this build
- **Apply Order**: 16
- **Testing Required**: Verify NetworkMonitor.java exists before applying

**RECOMMENDATION**: Apply Level 4 patches only after extensive testing. Some may not be compatible with your LineageOS build.
