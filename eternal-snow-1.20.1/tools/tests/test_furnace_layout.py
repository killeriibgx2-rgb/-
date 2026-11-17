"""Headless layout contract. Run: python3 -m unittest discover -s tools/tests -v.

Does not replace a Forge client smoke test: validates pixel geometry and slot coverage only.
"""
import re
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / 'src/main/java/com/eternalsnow/client/CozyFurnaceScreen.java'


class FurnaceLayoutTest(unittest.TestCase):
    def test_canvas_size(self):
        data = (ROOT / 'src/main/resources/assets/eternalsnow/textures/gui/cozy_furnace.png').read_bytes()
        self.assertEqual(data[:8], b'\x89PNG\r\n\x1a\n')
        self.assertEqual(struct.unpack('>II', data[16:24]), (176, 166))

    def test_slots_match_texture_interiors(self):
        source = SCREEN.read_text()
        fixed = {int(i): (int(x), int(y)) for i, x, y in
                 re.findall(r'move\((\d+), (\d+), (\d+)\)', source)}
        self.assertEqual(fixed, {0: (72, 27), 1: (72, 56), 2: (137, 44)})
        self.assertIn('move(3 + row * 9 + col, 9 + col * 18, 85 + row * 18)', source)
        self.assertIn('move(30 + col, 9 + col * 18, 143)', source)
        slots = dict(fixed)
        for row in range(3):
            for col in range(9):
                slots[3 + row * 9 + col] = (9 + col * 18, 85 + row * 18)
        for col in range(9):
            slots[30 + col] = (9 + col * 18, 143)
        self.assertEqual(set(slots), set(range(39)))
        for i, (x, y) in slots.items():
            self.assertGreaterEqual(x - 1, 5)
            self.assertGreaterEqual(y - 1, 5)
            self.assertLessEqual(x + 16, 170)
            self.assertLessEqual(y + 16, 160)
            for j, (xx, yy) in slots.items():
                if i != j:
                    self.assertTrue(x + 16 <= xx or xx + 16 <= x or
                                    y + 16 <= yy or yy + 16 <= y, (i, j))

    def test_full_progress_stays_in_track(self):
        for progress in range(25):
            height = progress * 46 // 24
            self.assertTrue(26 <= 72 - height <= 72)
        self.assertEqual(24 * 46 // 24, 46)


if __name__ == '__main__':
    unittest.main()
