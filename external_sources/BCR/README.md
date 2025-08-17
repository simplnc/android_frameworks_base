# Upstream: Basic Call Recorder (BCR)

- URL: https://github.com/chenxiaolong/BCR
- Purpose: A stand-alone call recorder app providing recording engine, storage, and UI.
- Notes:
  - Licensed under GPLv3.
  - Uses Kotlin and AndroidX; Gradle-based project.
  - Integration approach for AOSP Dialer: extract a minimal recording engine and bind via InCallService hooks.

This directory is a placeholder to save and track the upstream. Actual code is not vendored here to avoid affecting the existing build. Use the script in `../../info/dialer_integration/clone_and_prepare.sh` to clone it locally under this path when needed.