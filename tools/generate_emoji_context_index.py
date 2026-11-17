#!/usr/bin/env python3
"""Regenerate the pinned Unicode 17 / CLDR 48.2 English Emoji search index.

This offline developer tool is NOT executed during app builds or at IME startup.
Supply the three exact upstream files independently; SHA-1 checks are Git blob
hashes from the pinned repository trees, not the file's raw SHA-1 digest.
"""

import argparse
import hashlib
import pathlib
import re
import xml.etree.ElementTree as ET


ANNOTATIONS_BLOB = "fbf0e693ebafbae2dedf90f97b7ad141769941d9"
DERIVED_BLOB = "8ba77741a2ff1e02e2be911b8a3766dc114f1d43"
EMOJI_TEST_BLOB = "e42fdef494399d2a05b9b75930a3231fe81b2436"


def checked_bytes(path: pathlib.Path, expected: str) -> bytes:
    payload = path.read_bytes()
    header = b"blob " + str(len(payload)).encode("ascii") + b"\0"
    actual = hashlib.sha1(header + payload).hexdigest()
    if actual != expected:
        raise ValueError(f"Unexpected upstream blob for {path}: {actual}")
    return payload


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--annotations", required=True, type=pathlib.Path)
    parser.add_argument("--derived", required=True, type=pathlib.Path)
    parser.add_argument("--emoji-test", required=True, type=pathlib.Path)
    parser.add_argument("--output", required=True, type=pathlib.Path)
    args = parser.parse_args()

    annotations = {}
    for path, expected in [(args.annotations, ANNOTATIONS_BLOB),
                           (args.derived, DERIVED_BLOB)]:
        root = ET.fromstring(checked_bytes(path, expected))
        for node in root.iter("annotation"):
            cp = (node.attrib.get("cp") or "").replace("\ufe0f", "")
            if not cp:
                continue
            record = annotations.setdefault(cp, {"name": "", "keywords": ""})
            kind = "name" if node.attrib.get("type") == "tts" else "keywords"
            record[kind] = (node.text or "").strip()

    text = checked_bytes(args.emoji_test, EMOJI_TEST_BLOB).decode("utf-8")
    rows = [
        "# SPDX-License-Identifier: Unicode-3.0",
        "# Generated offline from Unicode CLDR release-48-2, English annotations and derived annotations.",
        f"# Sources: unicode-org/cldr common/annotations/en.xml ({ANNOTATIONS_BLOB})",
        f"# common/annotationsDerived/en.xml ({DERIVED_BLOB})",
        f"# Filter: Unicode Emoji 17.0 emoji-test.txt fully-qualified sequences ({EMOJI_TEST_BLOB}).",
        "# Columns: exact emoji sequence, CLDR short name, pipe-separated broad search keywords.",
        "# Broad CLDR terms are metadata for search; they must never automatically trigger typing suggestions.",
    ]
    seen = set()
    for line in text.splitlines():
        match = re.match(r"^[0-9A-F ]+;\s*fully-qualified\s*#\s*(\S+)", line)
        if not match:
            continue
        emoji = match.group(1)
        entry = annotations.get(emoji.replace("\ufe0f", ""))
        if emoji in seen or not entry or not (entry["name"] or entry["keywords"]):
            continue
        seen.add(emoji)
        columns = (emoji, entry["name"], entry["keywords"])
        rows.append("\t".join(re.sub(r"[\t\r\n]", " ", item) for item in columns))

    args.output.write_text("\n".join(rows) + "\n", encoding="utf-8")
    print(f"Wrote {len(seen)} supported qualified sequences to {args.output}")


if __name__ == "__main__":
    main()
