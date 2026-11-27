#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
import io
from pathlib import Path
import tempfile
import unittest
import zipfile
from unittest.mock import patch

import audit_public_apk as audit


class PublicApkAuditTest(unittest.TestCase):
    def test_ascii_utf16_and_chunk_boundaries(self):
        for encoding in ("utf-8", "utf-16le", "utf-16be"):
            with patch.object(audit, "CHUNK", 31):
                self.assertTrue(audit.stream_has_identifier(io.BytesIO(
                    b"x" * 29 + "ReNo 10".encode(encoding))))
        self.assertFalse(audit.stream_has_identifier(io.BytesIO(b"public project name")))

    def test_compressed_member_is_inspected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "sample.apk"
            with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as apk:
                apk.writestr("assets/example.json", b'{"test":"Nirvana Ion"}')
                apk.writestr("classes.dex", b"public feature")
            count, hits = audit.scan_apk(path)
            self.assertEqual(2, count)
            self.assertIn("assets/example.json", hits)


if __name__ == "__main__":
    unittest.main()
