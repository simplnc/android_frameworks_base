#!/usr/bin/env bash
set -Eeuo pipefail

REMOTE="epic"
BRANCH="15"
CATEGORY="all"   # security|perf|all
DRY_RUN=1        # 1=dry-run (default), 0=apply
LIMIT=0          # 0=unlimited
SINCE=""         # e.g., 2024-01-01
SKIP_ON_CONFLICT=0 # 1=skip conflicting commits instead of stopping
INCLUDE_RISKY=0    # 1=include API/AIDL/Android.bp/etc. risky changes
OUTPUT_DIR="$(pwd)/_epic15_analysis"

usage() {
  cat <<USAGE
Usage: $0 [-r remote] [-b branch] [-c category] [-n] [-a] [-l limit] [-s since_date] [-S] [-R] [-o outdir]
  -r  remote name (default: epic)
  -b  branch name (default: 15)
  -c  category: security|perf|all (default: all)
  -n  dry-run only (default)
  -a  apply (turn off dry-run)
  -l  limit number of commits to consider (default: unlimited)
  -s  since date, e.g. 2024-01-01 (optional)
  -o  output dir for lists (default: ./_epic15_analysis)
  -S  skip commits that conflict (default: stop on conflict)
  -R  include risky changes (API/AIDL/Android.bp/etc.)
USAGE
}

while getopts ":r:b:c:l:s:o:nSaR" opt; do
  case "$opt" in
    r) REMOTE="$OPTARG" ;;
    b) BRANCH="$OPTARG" ;;
    c) CATEGORY="$OPTARG" ;;
    l) LIMIT="$OPTARG" ;;
    s) SINCE="$OPTARG" ;;
    o) OUTPUT_DIR="$OPTARG" ;;
    n) DRY_RUN=1 ;;
    a) DRY_RUN=0 ;;
    S) SKIP_ON_CONFLICT=1 ;;
    R) INCLUDE_RISKY=1 ;;
    *) usage; exit 1 ;;
  esac
done

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Run this inside your frameworks/base git repo" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

if ! git remote get-url "$REMOTE" >/dev/null 2>&1; then
  git remote add "$REMOTE" https://github.com/EpicROM-AOSP/android_frameworks_base.git
fi
# Fetch the target branch
git fetch "$REMOTE" "$BRANCH" --tags

REF="$REMOTE/$BRANCH"

SEC_GREP=(
  "security" "CVE" "vuln" "exploit" "permission" "privilege" "DoS" "denial" "overflow"
  "out[- ]of[- ]bounds" "\\boob\\b" "use[- ]after[- ]free" "\\buaf\\b" "null pointer" "NPE"
  "sanitize" "validate" "bounds" "SSRF" "XSS" "RCE" "sqli" "spoof" "bypass" "\\bleak\\b"
)
PERF_GREP=(
  "\\bperf\\b" "performance" "optimi[sz]" "speed" "fast" "latency" "jank" "startup" "cold start"
  "\\bANR\\b" "frame" "render" "jitter" "\\bGC\\b" "memory" "leak" "contention" "\\block\\b"
  "\\brace\\b" "deadlock" "Strict.?Mode" "cache" "throughput" "bandwidth" "scheduler" "dispatch"
  "binder" "zygote" "hot path" "micro-optim" "time to"
)

build_grep_args() {
  local -n arr=$1
  local args=()
  for pat in "${arr[@]}"; do
    args+=("-E" "--regexp-ignore-case" "--grep=${pat}")
  done
  printf '%s\n' "${args[@]}"
}

GREP_ARGS=()
case "$CATEGORY" in
  security) mapfile -t GREP_ARGS < <(build_grep_args SEC_GREP) ;;
  perf)     mapfile -t GREP_ARGS < <(build_grep_args PERF_GREP) ;;
  all)      mapfile -t GREP_ARGS < <( { build_grep_args SEC_GREP; build_grep_args PERF_GREP; } ) ;;
  *) echo "Invalid category: $CATEGORY" >&2; exit 1 ;;
esac

RANGE_ARGS=("$REF")
[ -n "$SINCE" ] && RANGE_ARGS+=("--since=$SINCE")
[ "$LIMIT" != "0" ] && RANGE_ARGS+=("--max-count=$LIMIT")

mapfile -t CANDIDATES < <(git log "${RANGE_ARGS[@]}" --no-merges --reverse \
  "${GREP_ARGS[@]}" --pretty=format:%H | sed '/^$/d' | sed '/[Rr]evert/d' | uniq)

is_risky_file() {
  local f="$1"
  [[ "$f" =~ ^(core|legacy-test)/api/ ]] && return 0
  [[ "$f" =~ (^|/)api/ ]] && return 0
  [[ "$f" =~ ^tools/metalava ]] && return 0
  [[ "$f" =~ (^|/)Android\.bp$ ]] && return 0
  [[ "$f" =~ (^|/)Android\.mk$ ]] && return 0
  [[ "$f" =~ ^core/res/AndroidManifest\.xml$ ]] && return 0
  [[ "$f" =~ ^core/java/.+\.aidl$ ]] && return 0
  [[ "$f" =~ (current\.txt|removed\.txt|system-current\.txt|test-current\.txt)$ ]] && return 0
  return 1
}

SAFE_SHAS=()
SEC_SHAS=()
PERF_SHAS=()

classify_subject() {
  local subj="$1"
  if echo "$subj" | grep -Eiq "$(IFS='|'; echo "${SEC_GREP[*]}")"; then
    echo security; return 0
  fi
  echo perf
}

for sha in "${CANDIDATES[@]}"; do
  subj=$(git log -1 --pretty=%s "$sha")
  cls=$(classify_subject "$subj")
  safe=1
  if [ "$INCLUDE_RISKY" -eq 0 ]; then
    while IFS= read -r f; do
      if is_risky_file "$f"; then
        safe=0; break
      fi
    done < <(git diff-tree --no-commit-id --name-only -r "$sha")
  fi
  if [ "$safe" -eq 1 ]; then
    SAFE_SHAS+=("$sha")
    if [ "$cls" = security ]; then
      SEC_SHAS+=("$sha")
    else
      PERF_SHAS+=("$sha")
    fi
  fi
done

SEC_LIST="$OUTPUT_DIR/epic_fb15_security_cherrypick_order.txt"
PERF_LIST="$OUTPUT_DIR/epic_fb15_perf_cherrypick_order.txt"
ALL_TSV="$OUTPUT_DIR/epic_fb15_candidates_filtered.tsv"
RAW_TSV="$OUTPUT_DIR/epic_fb15_candidates_raw.tsv"

git log "$REF" --no-merges --reverse "${GREP_ARGS[@]}" --date=short --pretty=format:$'%H\t%ad\t%s' \
  | sed '/[Rr]evert/d' > "$RAW_TSV"

: > "$ALL_TSV"
: > "$SEC_LIST"
: > "$PERF_LIST"

for sha in "${SAFE_SHAS[@]}"; do
  printf "%s\t%s\t%s\n" "$sha" "$(git log -1 --date=short --pretty=format:%ad "$sha")" "$(git log -1 --pretty=%s "$sha")" >> "$ALL_TSV"
  if printf '%s\n' "${SEC_SHAS[@]}" | grep -qx "$sha"; then
    echo "$sha" >> "$SEC_LIST"
  else
    echo "$sha" >> "$PERF_LIST"
  fi
done

echo "Security candidates (safe): ${#SEC_SHAS[@]}"
echo "Performance candidates (safe): ${#PERF_SHAS[@]}"
echo "Total candidates (safe): ${#SAFE_SHAS[@]}"
echo "Lists saved under: $OUTPUT_DIR"

apply_list() {
  local list_file="$1"
  while IFS= read -r sha; do
    [ -n "${sha:-}" ] || continue
    echo "Applying $sha ..."
    if ! git cherry-pick -x "$sha"; then
      echo "Conflict on $sha" >&2
      if [ "$SKIP_ON_CONFLICT" -eq 1 ]; then
        git cherry-pick --abort
        echo "Skipped $sha due to conflict" >&2
        continue
      else
        echo "Resolve conflicts, then run: git cherry-pick --continue" >&2
        exit 1
      fi
    fi
  done < "$list_file"
}

if [ "$DRY_RUN" -eq 1 ]; then
  echo "Dry-run mode: no commits applied. Review lists and rerun with -a to apply."
  exit 0
fi

if [ -s "$SEC_LIST" ]; then
  apply_list "$SEC_LIST"
fi
if [ -s "$PERF_LIST" ]; then
  apply_list "$PERF_LIST"
fi

echo "Done. Applied commits successfully."