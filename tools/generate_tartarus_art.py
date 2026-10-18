#!/usr/bin/env python3
"""Original pixel art for Tartarus. Seeded pigments, hand-defined veins and silhouettes.

No game textures, photographs or generated concept images are sampled. The mod
ships these PNGs; Python's standard library reproduces them byte for byte.
--preview additionally needs Pillow and shows the exact sprites, without lighting.
"""
import argparse
import math
from pathlib import Path
import random
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'src/main/resources/assets/elysium/textures'


def png(w, h, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    raw = b''.join(b'\0' + bytes(c for p in pixels[y*w:(y+1)*w] for c in p) for y in range(h))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def mix(a, b, t):
    return tuple(round(x + (y-x)*max(0, min(1, t))) for x, y in zip(a, b))


def pigment(c, n):
    return tuple(max(0, min(255, round(v+n))) for v in c) + (255,)


def field(x, y, scale, seed=17):
    """Low-frequency pigment clumps, without the repeating sine-wave wood grain."""
    x/=scale; y/=scale; ix=math.floor(x); iy=math.floor(y); tx=x-ix; ty=y-iy
    tx=tx*tx*(3-2*tx); ty=ty*ty*(3-2*ty)
    def point(a,b):
        n=(a*374761393+b*668265263+seed*1274126177)&0xffffffff
        n=((n^(n>>13))*1274126177)&0xffffffff
        return (n^(n>>16))/0xffffffff
    a=point(ix,iy)*(1-tx)+point(ix+1,iy)*tx
    b=point(ix,iy+1)*(1-tx)+point(ix+1,iy+1)*tx
    return a*(1-ty)+b*ty


def texture(name, w, h):
    rng = random.Random('elysium-original-26-' + name)
    pixels = []
    for y in range(h):
        for x in range(w):
            grain = rng.choice((-4, -2, 0, 0, 2, 4))
            wave = math.sin(x*.53 + math.sin(y*.33)*2)
            if name == 'bronze_pinion':
                centre=13+(h-y)*.18; d=abs(x-centre)
                extent=7*math.sin(max(0,(y-2)/(h-6))*math.pi)**.6 if 2<=y<=h-4 else 0
                if d>extent and not (d<1 and y>h-12): pixels.append((0,0,0,0)); continue
                barb=(y+int(d*1.8))%5
                c=(205,149,66) if d<1 else (159,102,48) if barb<3 else (72,48,32)
                if y>h*.73 and d>1:c=mix(c,(51,73,60),.55)
            elif name == 'bronze_feather':
                # Long-axis quill and oblique barbs: different from a sheet of copper.
                d = abs(x-(w-1)/2)
                barb = (y + int(d*1.6)) % 7
                c = (67, 43, 32) if barb == 0 else (154, 105, 55) if barb < 3 else (111, 73, 43)
                if d < 1: c = (215, 166, 85)
                elif d < 2: c = (82, 55, 36)
                if y > h*.74: c = mix(c, (37, 36, 33), (y-h*.74)/(h*.26))
                if d > w*.34 and math.sin(y*.14+x*.32) > .65: c = mix(c, (49, 88, 77), .65)
            elif name == 'bronze_plate':
                patina = math.sin(x*.22+1)*math.cos(y*.26)+.25*math.sin((x+y)*.8)
                c = mix((139, 89, 48), (51, 88, 75), max(0, patina-.15))
                if x in (2, w-3) or y in (2, h-3): c = (80, 56, 37)
                if (x-5)**2+(y-5)**2 < 4 or (x-w+6)**2+(y-h+6)**2 < 4: c = (195, 148, 76)
                if (x+y*2) % 23 == 0: c = mix(c, (207, 156, 85), .4)
            elif name == 'horn':
                c = mix((28, 25, 25), (87, 75, 58), (wave+1)*.18)
                if (y+int(x*.4)) % 11 == 0: c = (21, 20, 21)
            elif name in ('cinder_hide', 'titan_skin'):
                # Fine ashen patches and healed fissures. The first material
                # looked like polished timber on the actual giant and was rejected.
                pigment_field=field(x,y,8)+field(x+51,y+19,3)*.22
                c=mix((32,30,30),(85,71,60),.15+pigment_field*.65)
                fissure=abs(field(x+13,y-7,9,91)-.49)
                if fissure<.025: c=mix(c,(26,24,25),.55)
                if name == 'titan_skin':
                    c=mix(c,(83,78,71),.22)
                    scar=abs(x-w*.46-2*math.sin(y*.19))
                    if 17<y<48 and scar<.8:c=(94,62,46)
                    elif 17<y<48 and scar<1.7:c=(32,28,28)
            elif name == 'ember':
                c = mix((117, 30, 16), (255, 194, 78), (.5+.5*math.sin(x*.39+y*.18))**3)
                if (x+int(3*math.sin(y*.3))) % 11 < 2: c = (67, 26, 22)
            elif name == 'mourner_cloth':
                c = mix((32, 34, 33), (83, 78, 64), (wave+1)*.27)
                if y > h*.55: c = mix(c, (31, 35, 30), (y/h-.55)*1.6)
                if x%13 == 3 and y%5 < 3: c = (96, 81, 65)
                if y > h-8 and (x*7)%11 > h-y: pixels.append((0,0,0,0)); continue
            elif name == 'moth_wing':
                # A complete paired wing silhouette, charcoal margins around amber cells.
                u=(x-(w-1)/2)/(w/2); v=(y-(h-1)/2)/(h/2)
                extent=math.sqrt(max(0, 1-v*v))*(.93-.15*math.cos(v*8))
                if abs(u)>extent or abs(u)<.04: pixels.append((0,0,0,0)); continue
                cell=abs(math.sin(v*8+abs(u)*4))
                c=mix((91, 48, 27), (229, 171, 81), cell*.8)
                if abs(u)>extent-.12 or cell<.13: c=(42, 34, 29)
                if (abs(u)-.53)**2+(v+.18)**2<.038: c=(45, 36, 30)
                if (abs(u)-.53)**2+(v+.18)**2<.008: c=(244, 196, 111)
            elif name == 'charred_bone':
                fibre=(x*17+x*x*3)%19-9
                c=(124+fibre,113+fibre,94+fibre)
                if (x+int(math.sin(y*.21)))%23==5 and y>18:c=(80,73,61)
                if rng.randrange(31)==0:c=(146,130,105)
            elif name == 'cinder_ash':
                c = mix((57, 48, 46), (115, 94, 77), .4+.2*math.sin((x+y)*.2)+.1*math.cos(y*.5))
                if rng.randrange(19)==0: c=(135, 109, 79)
            elif name in ('burnt_clinker', 'smouldering_clinker'):
                seam=(x+int(math.sin(y*.3)*4))%17 < 2 or (y+int(math.cos(x*.28)*3))%13==0
                c=(32, 27, 28) if seam else mix((48, 40, 39), (94, 68, 51), (wave+1)*.3)
                if name=='smouldering_clinker' and seam and math.sin(y*.3+x*.14)>.3: c=(174, 62, 27)
            else: raise ValueError(name)
            pixels.append(pigment(c, grain))
    return png(w, h, pixels)


SPECS = [('entity/bronze_feather',32,64),('entity/bronze_plate',32,32),('entity/horn',32,32),
         ('entity/cinder_hide',32,32),('entity/titan_skin',64,64),('entity/ember',16,16),
         ('entity/mourner_cloth',32,64),('entity/moth_wing',64,32),('block/charred_bone',32,32),
         ('block/cinder_ash',32,32),('block/burnt_clinker',32,32),('block/smouldering_clinker',32,32),
         ('item/bronze_pinion',32,32)]


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true');parser.add_argument('--preview',action='store_true')
    args=parser.parse_args()
    for path,w,h in SPECS:
        target=DEST/(path+'.png'); content=texture(path.split('/')[-1],w,h)
        if args.check: assert target.read_bytes()==content, 'Regenerate '+str(target)
        else: target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(content)
    if args.preview:
        from PIL import Image, ImageDraw, ImageFont
        sheet=Image.new('RGB',(1040,62+320*math.ceil(len(SPECS)/4)),'#191817');draw=ImageDraw.Draw(sheet)
        font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',17)
        draw.text((24,18),'TARTARUS / original material study — exact shipped pixels',font=font,fill='#e8c58e')
        for i,(path,w,h) in enumerate(SPECS):
            x=24+(i%4)*256;y=62+(i//4)*320
            tile=Image.open(DEST/(path+'.png')).convert('RGBA');scale=min(216//w,256//h)
            tile=tile.resize((w*scale,h*scale),Image.Resampling.NEAREST)
            sheet.paste(tile,(x,y),tile)
            draw.text((x,y+268),path.split('/')[-1].replace('_',' '),font=font,fill='#cfbda0')
            draw.text((x,y+291),f'{w} × {h} / no scene lighting',font=font,fill='#8c8171')
        target=ROOT/'docs/art/alpha26-materials.png';target.parent.mkdir(parents=True,exist_ok=True);sheet.save(target)
    print(('Verified' if args.check else 'Generated')+f' {len(SPECS)} original Tartarus textures.')


if __name__=='__main__': main()
