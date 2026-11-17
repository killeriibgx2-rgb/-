"""Shelf model / click inverse regression checks; no Minecraft runtime required."""
import json
import math
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class ShelfLayoutTest(unittest.TestCase):
    def test_all_hooks_all_facings(self):
        model = json.loads((ROOT / 'src/main/resources/assets/eternalsnow/models/block/food_shelf.json').read_text())
        for i, element in enumerate(model['elements'][1:]):
            x = (element['from'][0] + element['to'][0]) / 32
            self.assertAlmostEqual(x, (1.6 + 3 * i) / 16)
            for facing, angle in [('north', 0), ('east', -90), ('south', -180), ('west', -270)]:
                t = math.radians(angle)
                wx = .5 + math.cos(t) * (x - .5) + math.sin(t) * .37
                wz = .5 - math.sin(t) * (x - .5) + math.cos(t) * .37
                along = {'north': wx, 'south': 1-wx, 'east': wz, 'west': 1-wz}[facing]
                self.assertEqual(round((along * 16 - 1.6) / 3), i)
                # Item stays at the wall, not on the opposite side of the block.
                depth = {'north': wz, 'south': 1-wz, 'east': 1-wx, 'west': wx}[facing]
                self.assertAlmostEqual(depth, .87)

    def test_texture_and_scale(self):
        model = json.loads((ROOT / 'src/main/resources/assets/eternalsnow/models/block/food_shelf.json').read_text())
        self.assertEqual(model['textures']['wood'], 'minecraft:block/spruce_planks')
        source = (ROOT / 'src/main/java/com/eternalsnow/client/FoodShelfRenderer.java').read_text()
        self.assertIn('ITEM_SCALE = 0.275F', source)
        self.assertNotIn('Axis.XP', source)
