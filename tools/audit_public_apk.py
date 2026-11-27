#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Build-time scan for known private test identifiers, not a universal PII detector.

Inspect tracked production inputs and decompressed APK members (including DEX,
Android binary XML, native code and assets). Also inspect the whole APK for
signing-block strings. Never output matched values or modify files.
"""
import argparse
from pathlib import Path
import subprocess
import sys
import zipfile

# Build-only denylist fixtures, not runtime or compatibility conditions.
IDENTIFIERS = tuple(
    word.encode(encoding)
    for word in ("reno10", "reno 10", "reno-10", "reno_10",
                 "nirvana ion", "nirvana-ion", "nirvana_ion")
    for encoding in ("utf-8", "utf-16le", "utf-16be")
)
CHUNK = 1024 * 1024
OVERLAP = max(map(len, IDENTIFIERS)) - 1


def stream_has_identifier(stream):
    tail = b""
    while True:
        block = stream.read(CHUNK)
        if not block:
            return False
        data = (tail + block).lower()
        if any(identifier in data for identifier in IDENTIFIERS):
            return True
        tail = data[-OVERLAP:]


def source_paths(root):
    listed = subprocess.check_output(
        ["git", "ls-files", "-z", "--", "app", "buildSrc"],
        cwd=root).split(b"\0")
    for raw in listed:
        if not raw:
            continue
        path = Path(raw.decode("utf-8"))
        if (path.parts[:2] == ("app", "src") and len(path.parts) > 2
                and path.parts[2] in ("test", "androidTest", "testFixtures")):
            continue
        yield path


def scan_sources(root):
    scanned, hits = 0, []
    for path in source_paths(root):
        file = root / path
        if not file.is_file():
            raise RuntimeError("Tracked production input missing")
        scanned += 1
        with file.open("rb") as stream:
            if stream_has_identifier(stream):
                hits.append(str(path))
    return scanned, hits


def scan_apk(path):
    scanned, hits = 0, []
    with zipfile.ZipFile(path) as apk:
        for entry in apk.infolist():
            if entry.is_dir():
                continue
            scanned += 1
            with apk.open(entry) as stream:
                if stream_has_identifier(stream):
                    hits.append(entry.filename)
    with path.open("rb") as stream:
        if stream_has_identifier(stream):
            hits.append("<raw APK / signing block>")
    return scanned, hits


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    count, source_hits = scan_sources(root)
    members, apk_hits = scan_apk(args.apk)
    print(f"Privacy identifier scan: {count} tracked production inputs, {members} APK members")
    for kind, hits in (("source", source_hits), ("APK", apk_hits)):
        for path in hits:
            print(f"::error::Known private test identifier in {kind}: {path}")
    if source_hits or apk_hits:
        return 1
    print("Known test-device identifier scan passed (not a comprehensive anonymity proof).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
