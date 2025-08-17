# DialerBridge

A small, self-contained library providing call recording plumbing for a Dialer app via Telecom/InCallService hooks.

- No external dependencies beyond AndroidX annotations
- High-verbosity code for clarity
- Does not modify the current build unless explicitly referenced by your Dialer app

## Modules
- CallRecordingController: starts/stops recording and handles file output and a foreground notification
- CallEventsListener: maps InCallService callbacks to the controller
- CallNumberExtractor: utility to get display number from a Call

## Integration (in your Dialer app repo)
- Add `implementation project(":DialerBridge")` (Gradle) or add `DialerBridge` as a static dependency in your app blueprint if using Soong
- In your `InCallService` subclass: construct `CallRecordingController` and `CallEventsListener`
- Forward `onCallAdded`/`onCallRemoved` to `CallEventsListener`
- Request runtime permissions (RECORD_AUDIO, storage as required)

## Notes
- This baseline records MIC. Device/ROM support determines whether uplink+downlink can be captured. Consider integrating device-specific policies or adopting strategies from BCR where licensing allows.