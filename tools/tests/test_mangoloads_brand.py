"""Source-only MangoLoads identity contract checks; no SDK or private data needed."""

from __future__ import annotations

import struct
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "app/src/main"
BRAND = ROOT / "art/brand/mangoloads"


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as stream:
        header = stream.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise AssertionError(f"Not a PNG: {path}")
    return struct.unpack(">II", header[16:24])


def luminance(value: str) -> float:
    rgb = (int(value[index : index + 2], 16) / 255 for index in (1, 3, 5))
    linear = [channel / 12.92 if channel <= 0.04045 else ((channel + 0.055) / 1.055) ** 2.4 for channel in rgb]
    return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(fg: str, bg: str) -> float:
    upper, lower = sorted((luminance(fg), luminance(bg)), reverse=True)
    return (upper + 0.05) / (lower + 0.05)


class MangoLoadsBrandContractTest(unittest.TestCase):
    def test_exact_intended_raster_dimensions(self) -> None:
        sizes = {
            APP / "res/drawable-nodpi/mangoloads_brand_full.png": (768, 768),
            APP / "res/drawable-nodpi/mangoloads_brand_small.png": (192, 192),
            APP / "res/drawable-nodpi/mangoloads_launcher_foreground.png": (432, 432),
            APP / "res/drawable-nodpi/mangoloads_launcher_monochrome.png": (432, 432),
            APP / "ic_launcher-playstore.png": (512, 512),
            BRAND / "readme-hero-light.png": (1440, 520),
            BRAND / "readme-hero-dark.png": (1440, 520),
            BRAND / "github-social-preview.png": (1280, 640),
            ROOT / "fastlane/metadata/android/en-US/images/featureGraphic.png": (1024, 500),
        }
        for path, size in sizes.items():
            with self.subTest(path=path.name):
                self.assertEqual(png_size(path), size)

    def test_portable_master_and_export_script(self) -> None:
        data = (BRAND / "primary-alpha-lossless.webp").read_bytes()
        self.assertEqual(data[:4], b"RIFF")
        self.assertEqual(data[8:12], b"WEBP")
        script = (ROOT / "tools/export-mangoloads-brand.sh").read_text()
        self.assertIn("primary-alpha-lossless.webp", script)
        self.assertIn("mangoloads_launcher_foreground.png", script)
        self.assertIn("featureGraphic.png", script)

    def test_adaptive_resources_preserve_stable_ids(self) -> None:
        v24 = APP / "res/drawable-v24/ic_launcher_foreground.xml"
        mono = APP / "res/drawable/ic_launcher_monochrome.xml"
        adaptive = APP / "res/mipmap-anydpi-v26/ic_launcher.xml"
        round_icon = APP / "res/mipmap-anydpi-v26/ic_launcher_round.xml"
        for path in (v24, mono, adaptive, round_icon):
            ET.parse(path)
        self.assertIn("mangoloads_launcher_foreground", v24.read_text())
        self.assertIn("mangoloads_launcher_monochrome", mono.read_text())
        for path in (adaptive, round_icon):
            value = path.read_text()
            self.assertIn("@drawable/ic_launcher_foreground", value)
            self.assertIn("@drawable/ic_launcher_monochrome", value)

    def test_about_color_image_not_material_tinted(self) -> None:
        settings = (APP / "java/helium314/keyboard/settings/screens/MainSettingsScreen.kt").read_text()
        self.assertIn("icon = R.drawable.mangoloads_brand_small", settings)
        self.assertIn("brandIcon = true", settings)
        self.assertIn("Image(", settings)
        about = (APP / "java/helium314/keyboard/settings/screens/AboutScreen.kt").read_text()
        self.assertIn("R.mipmap.ic_launcher_round", about)

    def test_motion_finishes_without_infinite_repeat(self) -> None:
        icon = (APP / "java/helium314/keyboard/settings/AnimatedBoardIcon.kt").read_text()
        wizard = (APP / "java/helium314/keyboard/settings/WelcomeWizard.kt").read_text()
        self.assertIn("durationMillis = 780", icon)
        self.assertIn("targetValue = 1f", icon)
        self.assertNotIn("while (true)", icon)
        self.assertNotIn("infiniteRepeatable(", wizard)
        self.assertNotIn("rememberInfiniteTransition(", wizard)

    def test_app_package_and_store_copy(self) -> None:
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn('applicationId = "com.mangoloads.board"', gradle)
        metadata = ROOT / "fastlane/metadata/android/en-US"
        self.assertEqual((metadata / "title.txt").read_text().strip(), "brrBoard")
        self.assertLessEqual(len((metadata / "short_description.txt").read_text().strip()), 80)
        self.assertNotIn("Gemini", (metadata / "full_description.txt").read_text())

    def test_premium_text_pairs_pass_wcag_aa(self) -> None:
        for fg, bg in (
            ("#17263D", "#F7F9FC"),
            ("#52647F", "#F7F9FC"),
            ("#2D60C8", "#F7F9FC"),
            ("#F5F7FC", "#0C1527"),
            ("#AFC5E4", "#0C1527"),
            ("#89ACED", "#0C1527"),
        ):
            with self.subTest(fg=fg, bg=bg):
                self.assertGreaterEqual(contrast(fg, bg), 4.5)


if __name__ == "__main__":
    unittest.main()
