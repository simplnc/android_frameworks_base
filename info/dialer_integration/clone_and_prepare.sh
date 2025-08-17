#!/usr/bin/env bash
set -euo pipefail

# Clone upstream repos for analysis under external_sources/
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
EXTERNAL_DIR="$ROOT_DIR/external_sources"
mkdir -p "$EXTERNAL_DIR"

clone_repo() {
	local url="$1"; shift
	local dest="$1"; shift
	if [ -d "$dest/.git" ]; then
		echo "[skip] $dest already cloned"
		return
	fi
	echo "[clone] $url -> $dest"
	git clone --depth=1 "$url" "$dest"
}

# BCR (Basic Call Recorder)
mkdir -p "$EXTERNAL_DIR/BCR"
clone_repo "https://github.com/chenxiaolong/BCR" "$EXTERNAL_DIR/BCR/src"

# Fossify Phone (Dialer)
mkdir -p "$EXTERNAL_DIR/FossifyPhone"
clone_repo "https://github.com/FossifyOrg/Phone" "$EXTERNAL_DIR/FossifyPhone/src"

# LineageOS VoiceDialer (ref only)
mkdir -p "$EXTERNAL_DIR/VoiceDialer"
clone_repo "https://github.com/LineageOS/android_packages_apps_VoiceDialer" "$EXTERNAL_DIR/VoiceDialer/src"

# Generate a quick report of key components
REPORT_DIR="$ROOT_DIR/info/dialer_integration/analysis"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/snapshot.txt"

{
	echo "== BCR key files =="
	grep -RIn "class .*Recorder\|InCall\|AccessibilityService" "$EXTERNAL_DIR/BCR/src" 2>/dev/null | head -n 200 || true
	echo
	echo "== Fossify Phone key files =="
	grep -RIn "InCallService\|TelecomManager\|CallRecording\|CallService" "$EXTERNAL_DIR/FossifyPhone/src" 2>/dev/null | head -n 200 || true
	echo
	echo "== VoiceDialer (legacy) key files =="
	grep -RIn "VoiceDial\|Recognizer\|Intent.ACTION_VOICE_COMMAND" "$EXTERNAL_DIR/VoiceDialer/src" 2>/dev/null | head -n 200 || true
} > "$REPORT_FILE"

echo "Prepared analysis snapshot at: $REPORT_FILE"