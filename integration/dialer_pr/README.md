### Ultimate Dialer PR: Call Recording + Settings (non-invasive payload)

This directory contains all files and snippets needed to integrate the `DialerBridge` library into your Dialer app with:
- Automatic/manual call recording
- Foreground service notification
- Settings UI for rules and audio config
- Safe storage via MediaStore on Android 10+

It is designed as a drop-in PR payload. No changes in this frameworks repo build are required.

### Steps (AOSP/Soong Dialer)
1) Copy `packages/DialerBridge` from this repo into your source tree if not already present, and depend on it from your Dialer app `Android.bp`:
```
static_libs: [
	"DialerBridge",
	// ...
],
```
2) Copy all files from `integration/dialer_pr/src` and `integration/dialer_pr/res` into your Dialer app module, preserving package paths.
3) Merge `integration/dialer_pr/AndroidManifest.additions.xml` into your app manifest.
4) Add the permissions and foreground service type gates as shown in the manifest additions.
5) Build and grant runtime permissions (RECORD_AUDIO, notifications on 33+).

### Steps (Gradle Dialer)
1) Add DialerBridge as a module or as sources.
2) Add `implementation project(":DialerBridge")` to your app module.
3) Copy `src`/`res` from this PR payload into your app.
4) Merge AndroidManifest additions.

### Features integrated
- Auto-record rules
- Configurable audio source (MIC or VOICE_COMMUNICATION), sample rate
- InCallService wiring
- Foreground service with notification
- Settings UI (`RecordingSettingsActivity`)

### Files
- src/com/example/dialer/integration/
  - RecordingInCallService.java
  - RecordingForegroundService.java
  - RecordingSettingsActivity.java
  - RecordingSettingsFragment.java
  - RulesRepository.java
- res/xml/recording_preferences.xml
- res/values/strings.xml
- AndroidManifest.additions.xml
- Soong.additions.txt (example `Android.bp` snippet)
- Gradle.additions.md (example Gradle snippet)

After applying, test with incoming/outgoing calls. The default auto-record settings are off; enable them in settings.