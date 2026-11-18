#!/usr/bin/env python3
"""Hand-authored pixel art. No generated imagery, noise or random sampling.
Run with Pillow: python3 eternal-snow-1.20.1/tools/draw_confectioner.py
Outputs Minecraft textures/model and an illustrative orthographic preview (not a game screenshot).
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/eternalsnow'
DOC=ROOT.parent/'docs/art'
DOC.mkdir(parents=True,exist_ok=True)
P={'wood':'#815337','wood_hi':'#ac7951','wood_lo':'#51382d','edge':'#372b29',
   'cream':'#e8d5ad','light':'#fff0cf','fold':'#baa17b','red':'#9c3d44','red_hi':'#cc6562',
   'red_lo':'#622d39','gold':'#d9a14d','glass':'#a4c9ba','glass_lo':'#527d79','mint':'#638f79'}
def tile(c): return Image.new('RGBA',(16,16),c)
def save(img,name,folder='block'):
 p=ASSET/f'textures/{folder}/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);img.save(p)
def rect(im,box,c): ImageDraw.Draw(im).rectangle(box,fill=P.get(c,c))
# Dark walnut construction, deliberate grooves and knots.
wood=tile(P['wood'])
for x in [0,5,11,15]: rect(wood,(x,0,x,15),'wood_lo')
for x,y0,y1 in [(1,1,7),(6,3,14),(12,0,10),(3,10,14),(9,0,5)]:rect(wood,(x,y0,x,y1),'wood_hi')
for b in [(2,8,3,8),(3,9,4,9),(7,6,8,6),(8,7,9,7),(12,12,13,12)]:rect(wood,b,'wood_lo')
save(wood,'confectioner_wood')
# Front: two small drawers, brass knobs, a candy insignia on the lower panel.
front=tile(P['wood_lo'])
rect(front,(0,0,15,1),'wood_hi');rect(front,(0,2,15,3),'red_lo')
for x in [1,9]:
 rect(front,(x,4,x+5,8),'edge');rect(front,(x+1,5,x+4,7),'wood')
 rect(front,(x+1,5,x+4,5),'wood_hi');rect(front,(x+2,6,x+3,6),'gold')
rect(front,(1,10,14,14),'wood');rect(front,(2,10,13,10),'wood_hi')
rect(front,(6,11,9,13),'cream');rect(front,(7,11,7,13),'red')
for b in [(4,11,5,11),(4,13,5,13),(10,11,11,11),(10,13,11,13)]:rect(front,b,'cream')
save(front,'confectioner_front')
# Worktop: bordered textile with restrained alternating cream/red stripes.
top=tile(P['wood'])
for y in [0,15]:rect(top,(0,y,15,y),'wood_hi')
rect(top,(1,1,14,14),'cream')
for x in [2,6,10,14]:rect(top,(x,1,x,14),'red')
rect(top,(1,1,14,1),'light');rect(top,(1,14,14,14),'fold')
# Hand-placed cocoa crumb marks, no texture noise.
for x,y in [(12,10),(13,12),(11,13)]:rect(top,(x,y,x,y),'wood_lo')
save(top,'confectioner_top')
# Jar opaque pixel-glass treatment, robust on standard solid block render layer.
jar=tile(P['glass_lo']);rect(jar,(1,0,14,15),'glass');rect(jar,(2,1,3,13),'light')
rect(jar,(13,2,14,14),'mint');rect(jar,(4,4,12,12),'cream')
for x in [5,8,11]:
 rect(jar,(x,5,x,10),'red');rect(jar,(x,11,x+1,12),'gold')
rect(jar,(1,14,14,15),'glass_lo');save(jar,'confectioner_jar')
candy=tile(P['cream'])
for y in range(16):
 for x in range(16):
  if (x+y)%6<2:candy.putpixel((x,y),tuple(bytes.fromhex(P['red'][1:]))+(255,))
save(candy,'confectioner_candy')
board=wood.copy();rect(board,(1,1,14,14),'gold');rect(board,(2,2,13,13),'wood_hi')
for b in [(4,4,7,6),(9,4,12,6),(4,9,7,11),(9,9,12,11)]:
 rect(board,b,'gold');rect(board,(b[0],b[1],b[2],b[1]),'cream')
save(board,'confectioner_board')
# Model coordinates in Minecraft pixels. Bare cuboids; no GUI or profession registration.
textures={k:f'eternalsnow:block/confectioner_{v}' for k,v in {'wood':'wood','front':'front','top':'top','jar':'jar','candy':'candy','board':'board'}.items()}
textures['particle']=textures['wood']
elements=[]
def cube(lo,hi,tex,fronttex=None,toptex=None):
 faces={f:{'uv':[0,0,16,16],'texture':'#'+tex} for f in ['north','south','east','west','up','down']}
 if fronttex:faces['north']['texture']='#'+fronttex;faces['south']['texture']='#'+fronttex
 if toptex:faces['up']['texture']='#'+toptex
 elements.append({'from':lo,'to':hi,'faces':faces})
for x in [1,12]:
 for z in [1,12]:cube([x,0,z],[x+3,13,z+3],'wood')
cube([2,3,2],[14,5,14],'wood')
cube([0,10,0],[16,14,16],'wood','front')
cube([0,14,0],[16,16,16],'wood',None,'top')
cube([2,16,3],[6,21,7],'jar')
cube([1.5,21,2.5],[6.5,22,7.5],'wood')
# Three striped candy sticks in a little holder.
cube([10,16,10],[14,19,14],'jar')
for x,z,h in [(10.5,11,25),(12.5,12,24),(11.5,13,23)]:
 cube([x,19,z],[x+1,h,z+1],'candy')
cube([8,16,2],[14,16.5,8],'wood',None,'board')
model={'parent':'minecraft:block/block','textures':textures,'elements':elements,
 'display':{'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.6,.6,.6]},
 'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.5,.5,.5]}}}
p=ASSET/'models/block/confectioner_table.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(model,indent=2)+'\n')
p=ASSET/'models/item/confectioner_table.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps({'parent':'eternalsnow:block/confectioner_table'},indent=2)+'\n')
# Villager profession layer: vanilla 64x64 UV. Face/nose remain transparent.
v=Image.new('RGBA',(64,64),(0,0,0,0))
# Body base, top surfaces and four sides (8w,12h,6d; UV 16,20).
rect(v,(16,26,43,37),'red');rect(v,(22,20,37,25),'red_lo')
rect(v,(22,26,29,37),'cream')
for x in [22,29]:rect(v,(x,27,x,37),'fold')
rect(v,(24,26,27,27),'light');rect(v,(25,30,25,31),'gold')
# Outer apron/jacket UV 0,38. 8x20 front at (6,44), back at (20,44).
for box in [(0,44,5,63),(14,44,19,63),(20,44,27,63)]:rect(v,box,'red_lo')
rect(v,(6,44,13,63),'cream');rect(v,(6,44,6,63),'fold');rect(v,(13,44,13,63),'fold')
rect(v,(7,45,12,45),'light')
# Waist tie continues around coat; rear bow.
rect(v,(0,51,27,52),'red');rect(v,(7,51,12,52),'cream')
rect(v,(22,51,25,52),'gold');rect(v,(21,53,22,55),'cream');rect(v,(25,53,26,56),'fold')
# Apron pocket and two small caramel stains.
rect(v,(8,55,11,58),'fold');rect(v,(8,55,11,55),'light');rect(v,(9,56,11,57),'cream')
for box in [(7,59,8,59),(8,60,8,60),(12,48,12,49)]:rect(v,box,'gold')
rect(v,(7,61,12,61),'light');rect(v,(7,63,12,63),'red')
# Sleeves UV44,22 and folded arm bar UV40,38.
rect(v,(44,26,59,33),'red');rect(v,(48,22,55,25),'red_hi')
rect(v,(44,30,59,31),'cream');rect(v,(44,32,59,33),'fold')
rect(v,(40,42,63,45),'red');rect(v,(44,38,59,41),'red_hi')
rect(v,(44,42,51,45),'cream');rect(v,(44,45,51,45),'fold')
# Shoes/legs; front UV4,26 4x12, other faces around it.
rect(v,(0,26,15,37),'red_lo');rect(v,(0,34,15,37),'edge');rect(v,(4,34,7,34),'wood_hi')
# Hat UV32,0. Only top + first four side rows: short cap, not a face mask.
rect(v,(40,0,55,7),'red');rect(v,(41,1,46,5),'red_hi')
rect(v,(32,8,63,11),'red');rect(v,(32,11,63,11),'cream')
rect(v,(41,8,46,8),'red_hi');rect(v,(43,9,44,10),'gold')
save(v,'confectioner','entity/villager/profession')
p=ASSET/'textures/entity/villager/profession/confectioner.png.mcmeta';p.write_text(json.dumps({'villager':{'hat':'partial'}},indent=2)+'\n')
# Zombie-villager profession UV shares head/body/legs, but arms are 12px long.
z=v.copy()
rect(z,(40,38,63,45),(0,0,0,0))
rect(z,(44,26,59,33),'red')
rect(z,(44,34,59,35),'cream')
rect(z,(44,36,59,37),(0,0,0,0))
save(z,'confectioner','entity/zombie_villager/profession')
p=ASSET/'textures/entity/zombie_villager/profession/confectioner.png.mcmeta'
p.write_text(json.dumps({'villager':{'hat':'partial'}},indent=2)+'\n')
# Illustrative rendering from actual texture faces. Pixel raster, no blur or AA.
canvas=Image.new('RGB',(320,200),'#202b31');draw=ImageDraw.Draw(canvas)
def project(x,y,z,ox,oy,scale):return (ox+(x-z)*scale,oy+(x+z)*scale*.48-y*scale)
def face(corners,texture,ox,oy,scale,shade=1):
 # Raster each authored texel into its projected quadrilateral, nearest-neighbour.
 for yy in range(texture.height):
  for xx in range(texture.width):
   color=texture.getpixel((xx,yy))
   if len(color)==4 and color[3]==0:continue
   color=tuple(round(c*shade) for c in color[:3])
   pts=[]
   for u,w in [(xx/texture.width,yy/texture.height),((xx+1)/texture.width,yy/texture.height),((xx+1)/texture.width,(yy+1)/texture.height),(xx/texture.width,(yy+1)/texture.height)]:
    pt=tuple(corners[0][i]+u*(corners[1][i]-corners[0][i])+w*(corners[3][i]-corners[0][i]) for i in range(3))
    pts.append(project(*pt,ox,oy,scale))
   draw.polygon(pts,fill=color)
def box(lo,hi,front,side,top,ox,oy,sc):
 x,y,z=lo;X,Y,Z=hi
 face([(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],side,ox,oy,sc,.73)
 face([(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],front,ox,oy,sc,.9)
 face([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],top,ox,oy,sc,1)
tex={'wood':wood,'front':front,'top':top,'jar':jar,'candy':candy,'board':board}
for e in elements:
 f=e['faces'];box(e['from'],e['to'],tex[f['south']['texture'][1:]],tex[f['east']['texture'][1:]],tex[f['up']['texture'][1:]],79,130,3)
# Neutral hand-drawn face for preview only. Runtime uses vanilla face under profession layer.
faceimg=Image.new('RGBA',(8,10),'#ac8766');fd=ImageDraw.Draw(faceimg)
for b,c in [((1,5,6,5),'#483d32'),((1,6,2,6),'#ece6cc'),((5,6,6,6),'#ece6cc'),((2,6,2,6),'#5c815c'),((5,6,5,6),'#5c815c'),((3,7,4,7),'#75583f')]:fd.rectangle(b,fill=c)
skin=tile('#9c785a');nose=tile('#a68261');hair=tile('#a38362')
ox,oy,sc=248,158,3
for x in [-4,0]:box([x,0,-2],[x+4,12,2],v.crop((4,26,8,38)),v.crop((0,26,4,38)),tile(P['red_lo']),ox,oy,sc)
box([-4,4,-3],[4,24,3],v.crop((6,44,14,64)),v.crop((0,44,6,64)),tile(P['red']),ox,oy,sc)
box([-4,24,-4],[4,34,4],faceimg,skin,hair,ox,oy,sc)
box([-4.3,30,-4.3],[4.3,34.3,4.3],v.crop((40,8,48,12)),v.crop((32,8,40,12)),v.crop((40,0,48,8)),ox,oy,sc)
box([-1,23,4],[1,27,6],nose,skin,skin,ox,oy,sc)
# Folded arms in front of apron (preview approximate pose, not the game's animation).
box([-8,16,2],[8,20,6],v.crop((44,42,52,46)),v.crop((44,26,48,30)),v.crop((44,38,52,42)),ox,oy,sc)
# Preview labels on a dark editorial card.
font=ImageFont.load_default()
draw.text((15,12),'CONFECTIONER / PIXEL ART',font=font,fill='#ecd8ae')
draw.text((22,177),'WORKSTATION',font=font,fill='#a8b8b8')
draw.text((209,177),'PROFESSION',font=font,fill='#a8b8b8')
canvas.resize((1280,800),Image.Resampling.NEAREST).save(DOC/'confectioner-preview.png')
v.resize((512,512),Image.Resampling.NEAREST).save(DOC/'confectioner-uv.png')
# Basic asset contracts.
assert v.size==(64,64) and all(v.getpixel((x,y))[3]==0 for y in range(8,18) for x in range(8,16))
for e in elements:
 assert all(a<b for a,b in zip(e['from'],e['to']))
for texture in tex.values(): assert texture.size==(16,16)
print('Wrote 6 block textures, profession overlay, model and previews; UV/size checks passed.')
