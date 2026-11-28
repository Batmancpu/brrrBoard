#!/usr/bin/env python3
"""Reject backup-derived and unapproved binary dictionaries in every APK."""
import hashlib
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
APPROVED_SOURCE = json.loads((ROOT / "tools/clean_dictionary_assets.json").read_text())
# Debug and Obtainium variants intentionally ship only these three dictionaries.
SHIPPED = ("assets/dicts/main_en-US.dict", "assets/dicts/main_en-GB.dict", "assets/dicts/emoji_en-IN.dict")
EXPECTED = {name: APPROVED_SOURCE[name] for name in SHIPPED}

def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()

def verify(apk):
    source = ROOT / "app/src/main"
    if {str(p.relative_to(source)) for p in (source / "assets/dicts").glob("*.dict")} != set(APPROVED_SOURCE):
        raise ValueError("unexpected or missing source dictionary assets")
    for name, digest in APPROVED_SOURCE.items():
        if git_blob((source / name).read_bytes()) != digest:
            raise ValueError("source dictionary changed: " + name)
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
        actual = {n for n in names if n.startswith("assets/dicts/") and n.endswith(".dict")}
        if actual != set(EXPECTED):
            raise ValueError("unexpected or missing dictionary assets")
        if len([n for n in names if n in actual]) != len(actual):
            raise ValueError("duplicate dictionary asset")
        if any(n.lower().endswith((".dict", "default_backup.zip")) and n not in actual for n in names):
            raise ValueError("unapproved dictionary or backup asset")
        for name, digest in EXPECTED.items():
            if git_blob(z.read(name)) != digest:
                raise ValueError("unapproved dictionary bytes: " + name)
    print("Verified pinned general dictionary inventory and absence of backup-derived dictionaries")

if __name__ == "__main__":
    verify(sys.argv[1])
