import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'src/main/resources'
WOODS=['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped','iron']
class InsulatedAssetsTest(unittest.TestCase):
    def test_all_variants_preserve_vanilla_models(self):
        for kind,count in [('door',32),('trapdoor',16)]:
            template=(ROOT/f'tools/templates/vanilla_oak_{kind}.json').read_text()
            for wood in WOODS:
                name=f'insulated_{wood}_{kind}'
                data=json.loads((RES/f'assets/eternalsnow/blockstates/{name}.json').read_text())
                self.assertEqual(len(data['variants']),count)
                self.assertEqual(data,json.loads(template.replace(f'oak_{kind}',f'{wood}_{kind}')))
                item=json.loads((RES/f'assets/eternalsnow/models/item/{name}.json').read_text())
                self.assertEqual(item['parent'],f'minecraft:item/{wood}_{kind}')
    def test_recipes_and_single_door_drop(self):
        for kind in ['door','trapdoor']:
            for wood in WOODS:
                name=f'insulated_{wood}_{kind}'
                recipe=json.loads((RES/f'data/eternalsnow/recipes/{name}.json').read_text())
                self.assertEqual(''.join(recipe['pattern']).count('W'),8)
                self.assertEqual(recipe['key']['W'],{'tag':'minecraft:wool'})
                self.assertEqual(recipe['key']['D'],{'item':f'minecraft:{wood}_{kind}'})
                self.assertEqual(recipe['result']['count'],1)
                loot=json.loads((RES/f'data/eternalsnow/loot_tables/blocks/{name}.json').read_text())
                states=[c for c in loot['pools'][0]['conditions'] if c['condition']=='minecraft:block_state_property']
                self.assertEqual(len(states),1 if kind=='door' else 0)
                if states:self.assertEqual(states[0]['properties'],{'half':'lower'})
