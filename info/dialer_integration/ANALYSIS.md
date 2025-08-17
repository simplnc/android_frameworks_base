### Integration overview (non-invasive)

- BCR (Basic Call Recorder):
  - Likely integrates via an AccessibilityService or InCallService+AudioRecord. GPLv3.
  - For AOSP Dialer integration, prefer a small, independent recording service bound from Dialer, keeping GPL code out of core proprietary-licensed modules.
  - Key hooks: android.telecom.InCallService, Call.Details, CallAudioState, TelecomManager start/stop call recording triggers.

- Fossify Phone:
  - Modern Dialer app structure. Good reference for permissions (READ_CALL_LOG, RECORD_AUDIO), runtime flows, and call UI state handling.
  - Useful patterns: handling of TelecomManager, role manager for default dialer, incall UI.

- VoiceDialer (LineageOS legacy):
  - Outdated voice command flow. Use only as reference for voice intents. Prefer modern SpeechRecognizer APIs.

### Minimal bridge approach

- Create a separate bridge library module (not referenced in this tree), exposing:
  - CallRecordingController: start/stop, permission checks, file naming/storage policies.
  - CallEventsListener: mapping Telecom events to controller.
- Keep the bridge license-compatible. If you adopt BCR code, isolate it in a separately licensed optional module.

### Steps (safe, build-neutral)

1) Use `info/dialer_integration/clone_and_prepare.sh` to fetch upstreams under `external_sources/`.
2) Review `analysis/snapshot.txt` to locate concrete classes for recording and call state handling.
3) Implement your Dialer changes in your Dialer repo, wiring:
   - InCallService.onCallAdded()/onCallRemoved() to start/stop recording via the bridge.
   - Runtime permissions flow (RECORD_AUDIO, READ/WRITE media, POST_NOTIFICATIONS if targetSdk 33+).
   - Foreground service for ongoing recording + notification channel.
4) Test on-device across BT/earpiece/speaker, dual-SIM, VoIP.

This plan avoids touching this frameworks build and keeps additions non-invasive.