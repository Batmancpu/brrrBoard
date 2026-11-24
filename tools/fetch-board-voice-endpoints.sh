#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fetch() {
  local url="$1" dest="$2" expected="$3" size="$4"
  mkdir -p "$(dirname "$dest")"
  if [[ -f "$dest" ]] && [[ "$(stat -c %s "$dest")" == "$size" ]] && echo "$expected  $dest" | sha256sum -c --status; then return; fi
  curl -fSL --retry 3 --connect-timeout 30 -o "$TMP/file" "$url"
  test "$(stat -c %s "$TMP/file")" == "$size"
  echo "$expected  $TMP/file" | sha256sum -c -
  install -m 0644 "$TMP/file" "$dest"
}
fetch https://raw.githubusercontent.com/snakers4/silero-vad/5cd7945676eb32225748052e2e6a0580e4686a08/src/silero_vad/data/silero_vad.onnx "$ROOT/app/src/main/assets/voice/silero_vad_v6_2_3.onnx" 1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3 2327524
fetch https://huggingface.co/pipecat-ai/smart-turn-v3/resolve/f766f81/smart-turn-v3.2-cpu.onnx "$ROOT/app/src/main/assets/voice/smart-turn-v3.2-cpu.onnx" 2bb026316b14a660486a75b1733cd3fbab8c2fd0314dc9af7be49f8cca967e4f 8679182
fetch https://github.com/csukuangfj/onnxruntime-libs/releases/download/v1.28.2/onnxruntime-android-1.28.2.zip "$TMP/ort.zip" 01518867f78241138b6aa25925802e843a4fa9085af8d303d49e35bbb52aff4d 33765715
mkdir -p "$ROOT/app/src/main/jni/voiceendpoint/ort_headers"
unzip -q "$TMP/ort.zip" 'headers/*' -d "$TMP/ort"
cp -R "$TMP/ort/headers/." "$ROOT/app/src/main/jni/voiceendpoint/ort_headers/"
test -s "$ROOT/app/src/main/jni/voiceendpoint/ort_headers/onnxruntime_c_api.h"
readelf --dyn-syms --wide "$ROOT/app/src/main/jniLibs/arm64-v8a/libonnxruntime.so" > "$TMP/symbols"
grep -q 'OrtGetApiBase' "$TMP/symbols"
