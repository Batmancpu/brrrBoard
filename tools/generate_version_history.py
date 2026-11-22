#!/usr/bin/env python3
"""Create an offline in-app changelog from signed releases and this build's notes.

Only the signed Obtainium pipeline stamps a new production version. No network
permission is added to the Android app; the workflow supplies GitHub release
metadata to this deterministic, dependency-free generator.
"""
# SPDX-License-Identifier: GPL-3.0-only

import argparse
import datetime as dt
import json
from pathlib import Path
import re

MARKER_START = "<!-- brrboard-version-history-v1"
MARKER_END = "-->"
MARKER_PATTERN = re.compile(re.escape(MARKER_START) + r"\s*(\{.*?\})\s*" + re.escape(MARKER_END), re.S)
VERSION = re.compile(r"1\.0\.([0-9]+)\Z")
COMMIT = re.compile(r"[0-9a-f]{40}\Z")
MAX_HISTORY_RELEASES = 10


def utc_timestamp(value):
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", value):
        raise ValueError("Release timestamp must be YYYY-MM-DDTHH:MM:SSZ")
    dt.datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
    return value


def validate_notes(payload):
    if not isinstance(payload, dict) or payload.get("schema") != 1:
        raise ValueError("Expected release-note schema 1")
    summary = payload.get("summary")
    changes = payload.get("changes")
    if not isinstance(summary, str) or not 3 <= len(summary.strip()) <= 500:
        raise ValueError("Provide an accurate summary of 3–500 characters")
    if not isinstance(changes, list) or not 1 <= len(changes) <= 30:
        raise ValueError("Release notes require 1–30 concrete changes")
    cleaned = []
    for change in changes:
        if not isinstance(change, dict) or not isinstance(change.get("title"), str) or not isinstance(change.get("detail"), str):
            raise ValueError("Every change requires a title and detail")
        title = change["title"].strip()
        detail = change["detail"].strip()
        if not 3 <= len(title) <= 100 or not 8 <= len(detail) <= 1600:
            raise ValueError("Change title/detail are missing or too long")
        cleaned.append({"title": title, "detail": detail})
    return {"summary": summary.strip(), "changes": cleaned}


def validate_entry(entry):
    if not isinstance(entry, dict) or not VERSION.fullmatch(entry.get("version", "")):
        raise ValueError("Invalid signed version")
    if not COMMIT.fullmatch(entry.get("commit", "")):
        raise ValueError("Invalid commit identity")
    utc_timestamp(entry.get("releaseTimeUtc"))
    if entry.get("timeSource") not in ("release_cut", "github_published"):
        raise ValueError("Unrecognized timestamp provenance")
    notes = validate_notes({"schema": 1, "summary": entry.get("summary"), "changes": entry.get("changes")})
    return {"version": entry["version"], "releaseTimeUtc": entry["releaseTimeUtc"],
            "timeSource": entry["timeSource"], "commit": entry["commit"], **notes}


def flatten_releases(payload):
    if not isinstance(payload, list):
        raise ValueError("GitHub release response must be a JSON list")
    if payload and isinstance(payload[0], list):
        return [release for page in payload for release in page]
    return payload


def previous_entries(releases, verified):
    legacy = {entry["version"]: validate_entry(entry) for entry in verified.get("releases", [])}
    result = {}
    for release in flatten_releases(releases):
        if not isinstance(release, dict) or release.get("draft") or release.get("prerelease"):
            continue
        tag = release.get("tag_name", "")
        if not isinstance(tag, str) or not tag.startswith("v") or not VERSION.fullmatch(tag[1:]):
            continue  # Never mix debug/test artifacts or arbitrary version names.
        version = tag[1:]
        if version in legacy:
            # The old releases lacked structured notes; only these backfills
            # are manually verified against exact published GitHub metadata.
            if (utc_timestamp(release.get("published_at")) == legacy[version]["releaseTimeUtc"]
                    and release.get("target_commitish") == legacy[version]["commit"]):
                result[version] = legacy[version]
            continue
        body = release.get("body") or ""
        marker = MARKER_PATTERN.search(body) if isinstance(body, str) else None
        if not marker:
            continue  # Never fabricate missing release descriptions.
        try:
            note = json.loads(marker.group(1))
            if note.get("version") != version:
                continue
            entry = validate_entry({**note, "timeSource": "github_published",
                                    "releaseTimeUtc": utc_timestamp(release.get("published_at"))})
            if release.get("target_commitish") != entry["commit"]:
                continue
            result[version] = entry
        except (ValueError, TypeError, KeyError):
            continue
    # When generating a local debug build without a GitHub response, keep only
    # independently verified baseline records. Production always supplies API data.
    if not releases:
        result.update(legacy)
    return result


def generate(notes, verified, releases, version=None, timestamp=None, commit=None):
    history = previous_entries(releases, verified)
    if version is not None:
        if not VERSION.fullmatch(version) or not isinstance(commit, str) or not COMMIT.fullmatch(commit):
            raise ValueError("A production build needs a valid version and SHA-1 commit")
        changes = validate_notes(notes)
        history[version] = validate_entry({"version": version,
            "releaseTimeUtc": utc_timestamp(timestamp), "timeSource": "release_cut",
            "commit": commit, **changes})
    latest = sorted(history.values(),
        key=lambda entry: int(VERSION.fullmatch(entry["version"]).group(1)), reverse=True)
    return {"schema": 1, "releases": latest[:MAX_HISTORY_RELEASES]}


def release_notes(entry):
    # The hidden marker keeps future version pickers precise and remains simple
    # plaintext inside GitHub Release descriptions; no Android network needed.
    payload = {"schema": 1, "version": entry["version"], "commit": entry["commit"],
               "summary": entry["summary"], "changes": entry["changes"]}
    human = ["### What's new", "", entry["summary"], ""]
    human.extend(f"- **{change['title']}** — {change['detail']}" for change in entry["changes"])
    human.extend(["", MARKER_START, json.dumps(payload, ensure_ascii=False, separators=(",", ":")), MARKER_END, ""])
    return "\n".join(human)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--notes", type=Path, default=Path("docs/releases/notes.json"))
    ap.add_argument("--verified", type=Path, default=Path("docs/releases/legacy_verified.json"))
    ap.add_argument("--releases-json", type=Path)
    ap.add_argument("--version")
    ap.add_argument("--timestamp")
    ap.add_argument("--commit")
    ap.add_argument("--output", type=Path, required=True)
    ap.add_argument("--release-notes-output", type=Path)
    args = ap.parse_args()
    notes = json.loads(args.notes.read_text(encoding="utf-8"))
    verified = json.loads(args.verified.read_text(encoding="utf-8"))
    releases = json.loads(args.releases_json.read_text(encoding="utf-8")) if args.releases_json else []
    result = generate(notes, verified, releases, args.version, args.timestamp, args.commit)
    if args.release_notes_output and args.version is None:
        ap.error("--release-notes-output requires --version")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if args.release_notes_output:
        args.release_notes_output.write_text(release_notes(result["releases"][0]), encoding="utf-8")
    print(f"Validated {len(result['releases'])} signed version histories; latest={result['releases'][0]['version']}")


if __name__ == "__main__":
    main()
