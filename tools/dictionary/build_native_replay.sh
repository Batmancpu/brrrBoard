#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
src="$root/app/src/main/jni/src"
jdk="${BRR_JDK_ROOT:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
mapfile -t sources < <(find "$src" -name '*.cpp' -print | sort)
"${CXX:-c++}" -std=c++17 -DNDEBUG -O2 -I"$src" -I"$jdk/include" -I"$jdk/include/linux" \
    "$root/tools/dictionary/native_replay.cpp" "${sources[@]}" -o "$1"
