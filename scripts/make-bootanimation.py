#!/usr/bin/env python3
"""Generate the XonolithOS Android boot animation as an uncompressed ZIP."""
from io import BytesIO
from math import cos, pi, sin
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'vendor/extra/bootanimation/bootanimation.zip'
WIDTH, HEIGHT, FPS = 600, 960, 30
FONT_DIR = next(path for path in (
    Path('/usr/share/fonts/TTF'), Path('/usr/share/fonts/truetype/dejavu'))
    if (path / 'DejaVuSans-Bold.ttf').is_file())
FONT = ImageFont.truetype(str(FONT_DIR / 'DejaVuSans-Bold.ttf'), 108)
SUBFONT = ImageFont.truetype(str(FONT_DIR / 'DejaVuSans.ttf'), 32)


def frame(progress, phase):
    # Render at half device resolution; the boot animation service scales it.
    picture = Image.new('RGB', (WIDTH, HEIGHT), '#080b12')
    draw = ImageDraw.Draw(picture)
    for y in range(HEIGHT):
        glow = int(10 * (1 - abs(y - 400) / HEIGHT))
        draw.line((0, y, WIDTH, y), fill=(8 + glow // 3, 11 + glow // 2, 18 + glow))

    alpha = min(1, progress * 1.7)
    pulse = 0.5 + 0.5 * sin(phase * 2 * pi)
    center_x, center_y = WIDTH // 2, 375
    mark = Image.new('RGBA', picture.size)
    shape = ImageDraw.Draw(mark)
    # Three offset glass slabs form a monolith/X, with a subtle moving halo.
    cyan = (64, 222, 243, int(245 * alpha))
    violet = (166, 132, 255, int(220 * alpha))
    shape.polygon([(220, 274), (253, 274), (383, 477), (350, 477)], fill=cyan)
    shape.polygon([(348, 274), (381, 274), (251, 477), (218, 477)], fill=violet)
    shape.rounded_rectangle((286, 232, 314, 518), radius=10,
                            fill=(235, 245, 255, int(238 * alpha)))
    halo = mark.filter(ImageFilter.GaussianBlur(26))
    halo.putalpha(halo.getchannel('A').point(lambda v: int(v * (0.36 + pulse * 0.22))))
    picture.paste(halo, (0, 0), halo)
    picture.paste(mark, (0, 0), mark)

    draw = ImageDraw.Draw(picture)
    title = 'xOS'
    bounds = draw.textbbox((0, 0), title, font=FONT)
    draw.text(((WIDTH - bounds[2]) / 2, 548), title, font=FONT,
              fill=(226, 244, 252))
    subtitle = 'XonolithOS'
    bounds = draw.textbbox((0, 0), subtitle, font=SUBFONT)
    draw.text(((WIDTH - bounds[2]) / 2, 695), subtitle, font=SUBFONT,
              fill=(126, 165, 188))
    # An active, quiet progress streak anchors the composition.
    draw.rounded_rectangle((225, 784, 375, 787), radius=2, fill=(35, 54, 70))
    lead = 225 + int(114 * (0.5 + 0.5 * sin(phase * 2 * pi)))
    draw.rounded_rectangle((lead, 784, min(lead + 35, 375), 787), radius=2,
                           fill=(70, 213, 238))
    return picture


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(OUTPUT, 'w', compression=ZIP_STORED) as archive:
        archive.writestr('desc.txt', f'{WIDTH} {HEIGHT} {FPS}\np 1 0 part0\np 0 0 part1\n')
        for part, count in (('part0', 24), ('part1', 30)):
            for index in range(count):
                progress = (index + 1) / 24 if part == 'part0' else 1
                phase = index / 30
                canvas = frame(progress, phase)
                data = BytesIO()
                canvas.save(data, format='PNG', optimize=True)
                archive.writestr(f'{part}/{index:04}.png', data.getvalue())
    print(OUTPUT)


if __name__ == '__main__':
    main()
