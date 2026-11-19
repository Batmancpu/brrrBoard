#!/usr/bin/env bash
# Reproducible arm64 voice runtime for the privately hosted brrBoard Parakeet engine.
# Native libraries are not checked into Git; the GitHub Actions build verifies their hashes.
set -euo pipefail

VERSION="1.13.3"
ARCHIVE_SHA256="243ad797a3b6e75ebbeaf7a2ab4aec0777e7d71b730685abb762a120940b07b6"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}/sherpa-onnx-${VERSION}.aar"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$ROOT/app/libs/sherpa-onnx-${VERSION}.jar"
NATIVE="$ROOT/app/src/main/jniLibs/arm64-v8a"

if [[ -s "$JAR" && -s "$NATIVE/libsherpa-onnx-jni.so" && -s "$NATIVE/libonnxruntime.so" && "${1:-}" != "--force" ]]; then
  exit 0
fi

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
curl -fSL --retry 3 --connect-timeout 30 -o "$TEMP_DIR/sherpa.aar" "$URL"
echo "$ARCHIVE_SHA256  $TEMP_DIR/sherpa.aar" | sha256sum -c -

mkdir -p "$ROOT/app/libs" "$NATIVE"
unzip -p "$TEMP_DIR/sherpa.aar" classes.jar > "$JAR"
unzip -p "$TEMP_DIR/sherpa.aar" jni/arm64-v8a/libsherpa-onnx-jni.so > "$NATIVE/libsherpa-onnx-jni.so"
unzip -p "$TEMP_DIR/sherpa.aar" jni/arm64-v8a/libonnxruntime.so > "$NATIVE/libonnxruntime.so"
test -s "$JAR"
test -s "$NATIVE/libsherpa-onnx-jni.so"
test -s "$NATIVE/libonnxruntime.so"
echo "Offline speech runtime ready for arm64-v8a (sherpa-onnx $VERSION)"
