"""Structural contracts for the 3.0 rewrite.

These guard the assumptions the CI build makes, so a half-finished
refactor fails loudly instead of producing a jar that is missing
something silently.
"""

import pathlib
import re
import unittest

MOD_ROOT = pathlib.Path(__file__).resolve().parents[2]


def read_gradle_properties() -> dict:
    values = {}
    for line in (MOD_ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


class GradlePropertiesTest(unittest.TestCase):
    def test_required_keys_present(self):
        props = read_gradle_properties()
        for key in ("mod_id", "mod_version", "minecraft_version", "forge_version"):
            self.assertIn(key, props, f"gradle.properties is missing {key}")

    def test_version_is_three_number_semver(self):
        version = read_gradle_properties()["mod_version"]
        self.assertRegex(
            version,
            r"^\d+\.\d+\.\d+$",
            "CI derives the jar name from mod_version; keep it plain x.y.z",
        )

    def test_mod_id_has_no_uppercase_or_spaces(self):
        mod_id = read_gradle_properties()["mod_id"]
        self.assertRegex(mod_id, r"^[a-z][a-z0-9_]*$")


class MainSourcesTest(unittest.TestCase):
    def test_every_java_file_declares_the_package_it_lives_in(self):
        source_root = MOD_ROOT / "src/main/java"
        if not source_root.exists():
            self.skipTest("no java sources yet")
        for path in source_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            expected = path.parent.relative_to(source_root).as_posix().replace("/", ".")
            match = re.search(r"^package\s+([\w.]+)\s*;", text, re.MULTILINE)
            self.assertIsNotNone(match, f"{path} has no package declaration")
            self.assertEqual(
                match.group(1),
                expected,
                f"{path} declares the wrong package",
            )


class ResourcesTest(unittest.TestCase):
    def test_mods_toml_uses_placeholder_for_mod_id(self):
        toml = (MOD_ROOT / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
        self.assertIn('modId="${mod_id}"', toml)

    def test_every_asset_json_parses(self):
        import json

        assets = MOD_ROOT / "src/main/resources/assets"
        if not assets.exists():
            self.skipTest("no assets yet")
        for path in assets.rglob("*.json"):
            with self.subTest(path=str(path.relative_to(MOD_ROOT))):
                json.loads(path.read_text(encoding="utf-8"))

    def test_every_java_class_has_a_lang_entry(self):
        """A missing translation is the classic silent bug: a raw key on screen."""
        import json

        source_root = MOD_ROOT / "src/main/java"
        lang_file = MOD_ROOT / "src/main/resources/assets/eternalsnow/lang/en_us.json"
        if not source_root.exists() or not lang_file.exists():
            self.skipTest("no sources or lang file yet")
        lang = json.loads(lang_file.read_text(encoding="utf-8"))
        mod_id = read_gradle_properties()["mod_id"]
        missing = []
        for path in source_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            for key in re.findall(rf'"(?:block|item|entity|effect)\.{mod_id}\.([a-z0-9_]+)"', text):
                full = f"block.{mod_id}.{key}"
                if full not in lang and f"item.{mod_id}.{key}" not in lang:
                    missing.append(full)
        self.assertEqual(missing, [], f"missing lang entries: {missing}")


if __name__ == "__main__":
    unittest.main()
