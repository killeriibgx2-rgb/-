"""Reuse vanilla 1.20.1 models verbatim; no generated or modified textures.
Templates: InventivetalentDev/minecraft-assets, branch 1.20.1, assets/minecraft/blockstates.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
WOODS=['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped','iron']
RU=['Дубовая','Еловая','Берёзовая','Тропическая','Акациевая','Из тёмного дуба','Мангровая','Вишнёвая','Бамбуковая','Багровая','Искажённая','Железная']
def put(path,data):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def tag(name,values):
    p=f'data/minecraft/tags/blocks/{name}.json'
    file=RES/p
    d=json.loads(file.read_text()) if file.exists() else {'replace':False,'values':[]}
    d['values']=list(dict.fromkeys(d['values']+values));put(p,d)
def generate():
    langs={lang:json.loads((RES/f'assets/eternalsnow/lang/{lang}.json').read_text()) for lang in ['en_us','ru_ru']}
    for kind in ['door','trapdoor']:
        template=(ROOT/f'tools/templates/vanilla_oak_{kind}.json').read_text()
        for wood,ru in zip(WOODS,RU):
            base=f'{wood}_{kind}';name=f'insulated_{base}';ident=f'eternalsnow:{name}'
            put(f'assets/eternalsnow/blockstates/{name}.json',json.loads(template.replace(f'oak_{kind}',base)))
            put(f'assets/eternalsnow/models/item/{name}.json',{'parent':f'minecraft:item/{base}'})
            put(f'data/eternalsnow/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':['WWW','WDW','WWW'],'key':{'W':{'tag':'minecraft:wool'},'D':{'item':f'minecraft:{base}'}},'result':{'item':ident,'count':1}})
            entry={'type':'minecraft:item','name':ident}
            conditions=[{'condition':'minecraft:survives_explosion'}]
            if kind=='door':conditions.append({'condition':'minecraft:block_state_property','block':ident,'properties':{'half':'lower'}})
            put(f'data/eternalsnow/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'conditions':conditions}]})
            put(f'data/eternalsnow/advancements/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_base':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[f'minecraft:{base}']}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':ident}}},'requirements':[['has_base','has_recipe']],'rewards':{'recipes':[ident]}})
            tag(kind+'s',[ident])
            tag('mineable/'+('pickaxe' if wood=='iron' else 'axe'),[ident])
            if wood!='iron':tag('wooden_'+kind+'s',[ident])
            langs['en_us']['block.'+ident.replace(':','.')]='Insulated '+wood.replace('_',' ').title()+' '+('Door' if kind=='door' else 'Trapdoor')
            langs['ru_ru']['block.'+ident.replace(':','.')]=('Утеплённая дверь' if kind=='door' else 'Утеплённый люк')+' — '+ru.lower()
    for lang,d in langs.items():put(f'assets/eternalsnow/lang/{lang}.json',d)
if __name__=='__main__':generate()
