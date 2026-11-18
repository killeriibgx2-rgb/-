import json
import struct
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/eternalsnow'

class ConfectionerAssetsTest(unittest.TestCase):
    def test_model_texture_references_exist(self):
        model=json.loads((ASSETS/'models/block/confectioner_table.json').read_text())
        for ref in model['textures'].values():
            self.assertTrue((ASSETS/'textures'/f'{ref.split(":")[1]}.png').is_file(),ref)
        for cube in model['elements']:
            self.assertTrue(all(a<b for a,b in zip(cube['from'],cube['to'])))
            for face in cube['faces'].values():
                self.assertIn(face['texture'][1:],model['textures'])
    def test_four_facings(self):
        states=json.loads((ASSETS/'blockstates/confectioner_table.json').read_text())['variants']
        self.assertEqual(set(states),{f'facing={x}' for x in ['north','east','south','west']})
    def test_both_profession_layers_have_vanilla_size(self):
        for entity in ['villager','zombie_villager']:
            data=(ASSETS/f'textures/entity/{entity}/profession/confectioner.png').read_bytes()
            self.assertEqual(struct.unpack('>II',data[16:24]),(64,64))
    def test_recipe_and_poi_tags(self):
        recipe=json.loads((RES/'data/eternalsnow/recipes/confectioner_table.json').read_text())
        self.assertEqual(recipe['ingredients'].count({'item':'minecraft:sugar'}),2)
        self.assertIn({'item':'minecraft:crafting_table'},recipe['ingredients'])
        self.assertIn({'tag':'eternalsnow:ordinary_candies'},recipe['ingredients'])
        candies=json.loads((RES/'data/eternalsnow/tags/items/ordinary_candies.json').read_text())['values']
        self.assertEqual(len(candies),5)
        self.assertNotIn('eternalsnow:candy_rainbow',candies)
        for name in ['acquirable_job_site','village']:
            tag=json.loads((RES/f'data/minecraft/tags/point_of_interest_type/{name}.json').read_text())
            self.assertFalse(tag['replace'])
            self.assertIn('eternalsnow:confectioner',tag['values'])
    def test_translations(self):
        for language in ['ru_ru','en_us']:
            lang=json.loads((ASSETS/f'lang/{language}.json').read_text())
            self.assertIn('block.eternalsnow.confectioner_table',lang)
            self.assertIn('entity.minecraft.villager.eternalsnow.confectioner',lang)
