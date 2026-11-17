#!/usr/bin/env python3
"""
Draws the Eternal Snow furnace GUI texture. Deterministic: every pixel is placed by an
explicit rule, there is no generated art anywhere in this file.

Run from the repository root:

    python3 tools/draw_cozy_furnace.py

Requires Pillow. Output:
    src/main/resources/assets/eternalsnow/textures/gui/cozy_furnace.png
    ../../docs/furnace-gui-preview.png   (3x nearest-neighbour, for review)

Why a script instead of a PNG in a paint program: the layout has to match the slot
coordinates in the menu to the pixel, and it has to be re-derivable when those coordinates
move. Change a constant below and the texture follows.

Two traps this file documents, both hit during development:

  * ImageDraw on an RGBA image does NOT alpha-blend — it overwrites the pixel and its alpha.
    Translucent shadows drawn directly on RGBA came out as solid bright bars. The canvas is
    therefore RGB and every translucent effect goes through blend().
  * A periodic formula for "paper grain" (`(y*3 + x//11) % 7`) produced visible diagonal
    stripes across the whole window. Texture that repeats every few pixels reads as a moire
    pattern, not as material, so the grain below is either integrated into a gradient or
    left out entirely.

Geometry (texture pixels, 176x166 — the vanilla container canvas):

    niche        58..102 x 24..78      centre (80, 51)
    input slot   71..88  x 26..43      one portion only, no result slot
    embers       73..86  x 46..52      firebox mouth between the two slots
    fuel slot    71..88  x 55..72
    progress     108..119 x 24..73     iron-framed vertical track, 8 wide
    group        58..119               centre 88.5 frames the 176px canvas
    inventory    8..169  x 84..141     9x3, 18px pitch, vanilla offsets
    hotbar       8..169  x 142..159
"""
from pathlib import Path

from PIL import Image, ImageDraw

W, H = 176, 166

# ---- palette: warm throughout. Nothing blue-grey: the mod is a winter mod, but the
# ---- furnace interior is the one warm place in it, and cool greys killed that.
FRAME_OUT = (42, 26, 17)
FRAME_DK  = (64, 42, 27)
FRAME_MID = (102, 68, 44)
FRAME_HI  = (138, 98, 65)

PARCH     = (212, 191, 156)
PARCH_HI  = (231, 213, 180)
PARCH_LO  = (184, 160, 124)

NICHE     = (172, 141, 108)
NICHE_DK  = (124, 96, 70)
NICHE_HI  = (194, 166, 134)

SLOT      = (126, 104, 78)
SLOT_EDGE = (72, 56, 40)
SLOT_HI   = (150, 128, 100)

IRON      = (72, 69, 74)
IRON_HI   = (114, 111, 118)
IRON_DK   = (46, 44, 48)

EMBER     = (224, 142, 62)
EMBER_HI  = (255, 208, 132)
EMBER_LO  = (168, 96, 44)
ASH       = (52, 40, 34)

img = Image.new("RGB", (W, H), PARCH)
d = ImageDraw.Draw(img)
px = img.load()


def blend(x, y, c, a):
    """Manual alpha blend — ImageDraw would overwrite instead."""
    if not (0 <= x < W and 0 <= y < H):
        return
    r, g, b = px[x, y]
    px[x, y] = (int(r + (c[0] - r) * a), int(g + (c[1] - g) * a), int(b + (c[2] - b) * a))


def rect(x0, y0, x1, y1, c):
    d.rectangle([x0, y0, x1, y1], fill=c)


def vgrad(x0, y0, x1, y1, top, bottom):
    """Vertical gradient — one colour per row, no per-pixel call."""
    span = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        t = (y - y0) / span
        d.line([(x0, y), (x1, y)],
               fill=(int(top[0] + (bottom[0] - top[0]) * t),
                     int(top[1] + (bottom[1] - top[1]) * t),
                     int(top[2] + (bottom[2] - top[2]) * t)))


# =============================================================== wooden frame
rect(0, 0, W - 1, H - 1, FRAME_OUT)
rect(1, 1, W - 2, H - 2, FRAME_DK)
rect(2, 2, W - 3, H - 3, FRAME_MID)
rect(2, 2, W - 3, 2, FRAME_HI)              # lit outer edge
rect(2, H - 3, W - 3, H - 3, FRAME_OUT)     # shaded outer edge
rect(3, 3, W - 4, H - 4, FRAME_OUT)         # inner shadow line
rect(5, 5, W - 6, H - 6, PARCH)

# grain on the wooden band only, sparse and irregular
for y in list(range(1, 4)) + list(range(H - 4, H - 1)):
    for x in range(1, W - 1):
        if (x + y * 5) % 13 == 0:
            blend(x, y, (0, 0, 0), 0.16)

# the page darkens toward the frame, so it does not read as one flat plane
for y in range(5, H - 5):
    for x in range(5, W - 5):
        edge = min(x - 5, y - 5, W - 6 - x, H - 6 - y)
        if edge < 16:
            blend(x, y, (120, 94, 66), (16 - edge) / 16.0 * 0.16)

# ====================================================================== rivets
for cx, cy in [(4, 4), (W - 5, 4), (4, H - 5), (W - 5, H - 5)]:
    d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=IRON_DK)
    d.ellipse([cx - 3, cy - 3, cx + 2, cy + 2], fill=IRON)
    d.ellipse([cx - 2, cy - 2, cx, cy], fill=IRON_HI)

# ======================================================================= niche
NX0, NX1, NY0, NY1 = 58, 102, 24, 78

# contact shadow under the niche, light catching its left edge
for i, a in enumerate((0.30, 0.18, 0.09)):
    for x in range(NX0 + 3 + i, NX1 - 2 - i):
        blend(x, NY1 + 1 + i, (92, 68, 46), a)
for y in range(NY0 + 3, NY1 + 1):
    blend(NX1 + 1, y, (92, 68, 46), 0.22)
    blend(NX0 - 1, y, (255, 246, 226), 0.38)

rect(NX0, NY0, NX1, NY1, NICHE_DK)
rect(NX0 + 1, NY0 + 1, NX1 - 1, NY1 - 1, NICHE)
vgrad(NX0 + 1, NY0 + 1, NX1 - 1, NY0 + 6, NICHE_DK, NICHE)   # it is a recess: dark at the top
vgrad(NX0 + 1, NY1 - 5, NX1 - 1, NY1 - 1, NICHE, NICHE_HI)   # lit lip at the bottom

# clay masonry: 6px courses, joints staggered, contrast kept low so the slots stay
# the darkest thing in the panel
for y in range(NY0 + 2, NY1 - 1):
    course = (y - (NY0 + 2)) // 6
    if (y - (NY0 + 2)) % 6 == 0:
        for x in range(NX0 + 2, NX1 - 1):
            blend(x, y, NICHE_DK, 0.85)
        continue
    for x in range(NX0 + 2, NX1 - 1):
        if (x - (NX0 + 2) + (course % 2) * 7) % 14 == 0:
            blend(x, y, NICHE_DK, 0.85)
        elif (y - (NY0 + 2)) % 6 == 1:
            blend(x, y, NICHE_HI, 0.45)

# firelight pooling in the recess, strongest just above the fuel slot
for y in range(NY0 + 2, NY1 - 1):
    for x in range(NX0 + 2, NX1 - 1):
        t = 1.0 - abs(y - (NY1 - 13)) / 22.0
        if t > 0:
            blend(x, y, (255, 186, 118), t * 0.16)


def slot(x, y, size=18):
    # drop shadow first, so the grid sits ON the page instead of floating
    for i, a in enumerate((0.34, 0.18)):
        for xx in range(x + 2 + i, x + size):
            blend(xx, y + size + i, (104, 78, 52), a)
        for yy in range(y + 2 + i, y + size):
            blend(x + size + i, yy, (104, 78, 52), a)
    rect(x, y, x + size - 1, y + size - 1, SLOT_EDGE)
    rect(x + 1, y + 1, x + size - 2, y + size - 2, SLOT)
    rect(x + 1, y + 1, x + size - 2, y + 1, SLOT_HI)
    rect(x + 1, y + 1, x + 1, y + size - 2, SLOT_HI)
    rect(x + size - 2, y + 1, x + size - 2, y + size - 2, SLOT_EDGE)
    rect(x + 1, y + size - 2, x + size - 2, y + size - 2, SLOT_EDGE)


SX = 71
slot(SX, 26)                     # input  — one portion, no result slot
slot(SX, 55)                     # fuel

# firebox mouth between the slots
rect(SX + 2, 46, SX + 15, 52, ASH)
for ex, ey, c in [(SX + 3, 51, EMBER), (SX + 5, 50, EMBER_HI), (SX + 8, 51, EMBER_LO),
                  (SX + 10, 50, EMBER), (SX + 13, 51, EMBER_HI), (SX + 14, 50, EMBER_LO)]:
    blend(ex, ey, c, 1.0)
d.line([(SX + 4, 49), (SX + 13, 49)], fill=(96, 60, 42))
for x in range(SX + 2, SX + 16):        # warm haze rising off the coals
    blend(x, 48, (255, 170, 100), 0.10)

# ================================================================ progress bar
BX, BY, BW, BH = 110, 26, 8, 46
rect(BX - 2, BY - 2, BX + BW + 1, BY + BH + 1, IRON_DK)
rect(BX - 1, BY - 1, BX + BW, BY + BH, IRON)
rect(BX - 1, BY - 1, BX + BW, BY - 1, IRON_HI)
rect(BX, BY, BX + BW - 1, BY + BH - 1, (68, 51, 37))
for i in range(1, 7):
    d.line([(BX + 1, BY + i * 6), (BX + BW - 2, BY + i * 6)], fill=(54, 40, 29))
for i in range(4):                      # arrow cap pointing down into the track
    d.line([(BX - i, BY - 4 + i), (BX + BW - 1 + i, BY - 4 + i)], fill=IRON_DK)

# ==================================================================== inventory
INV_X, INV_Y, HOT_Y = 8, 84, 142
for row in range(3):
    for col in range(9):
        slot(INV_X + col * 18, INV_Y + row * 18)
for col in range(9):
    slot(INV_X + col * 18, HOT_Y)
for col in range(9):
    blend(INV_X + col * 18 + 8, HOT_Y - 4, (150, 126, 92), 0.55)

# ========================================================================= output
here = Path(__file__).resolve().parent
root = here.parent

texture = root / "src/main/resources/assets/eternalsnow/textures/gui/cozy_furnace.png"
texture.parent.mkdir(parents=True, exist_ok=True)
img.convert("RGBA").save(texture)

docs = root.parent / "docs"
docs.mkdir(exist_ok=True)
img.resize((W * 3, H * 3), Image.NEAREST).save(docs / "furnace-gui-preview.png")

print(f"wrote {texture.relative_to(root.parent)}")
print(f"wrote docs/furnace-gui-preview.png")

# ---- geometry self-check: fails loudly if a constant above is nudged out of line ----
assert (NX0 + NX1) / 2 == 80, "niche is not centred on x=80"
assert (NX0 + BX + BW - 1) / 2 == (W - 1) / 2, "cooking group is not centred on the canvas"
assert INV_X + 8 * 18 + 17 <= W - 6, "inventory runs into the frame"
assert HOT_Y + 17 <= H - 6, "hotbar runs into the frame"
assert BX - 2 > NX1, "progress bar overlaps the niche"
print("geometry ok")
