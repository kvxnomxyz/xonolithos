#!/usr/bin/env python3
"""Build the xOS phone-to-wordmark animation as a stored Android boot ZIP."""
from io import BytesIO
from math import cos, pi, sin
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'vendor/extra/bootanimation/bootanimation.zip'
WIDTH, HEIGHT, FPS = 600, 960, 30
FONT_ROOT = next(path for path in (
    Path('/usr/share/fonts/TTF'), Path('/usr/share/fonts/truetype/dejavu'))
    if (path / 'DejaVuSans-Bold.ttf').is_file())
WORD_FONT = ImageFont.truetype(str(FONT_ROOT / 'DejaVuSans-Bold.ttf'), 57)
SUB_FONT = ImageFont.truetype(str(FONT_ROOT / 'DejaVuSans.ttf'), 20)
COLORS = ((94, 231, 247), (137, 163, 255), (218, 244, 255))


def clamp(value):
    return max(0.0, min(1.0, value))


def background():
    image = Image.new('RGB', (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(image)
    for y in range(HEIGHT):
        glow = 1 - abs(y - HEIGHT * 0.45) / HEIGHT
        draw.line((0, y, WIDTH, y), fill=(5 + int(5 * glow),
                  12 + int(9 * glow), 24 + int(14 * glow)))
    return image


def phone_layer(explode):
    layer = Image.new('RGBA', (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(layer)
    fade = int(255 * (1 - clamp(explode * 1.4)))
    if fade:
        draw.rounded_rectangle((250, 334, 350, 526), radius=22, width=5,
                               outline=(134, 235, 251, fade))
        draw.rounded_rectangle((260, 350, 340, 495), radius=10,
                               fill=(25, 74, 102, int(fade * .7)))
        draw.line((286, 343, 314, 343), fill=(188, 237, 251, fade), width=3)
        draw.ellipse((296, 507, 304, 515), fill=(188, 237, 251, fade))
        draw.line((275, 425, 325, 425), fill=(68, 212, 238, fade), width=3)
        draw.line((300, 400, 300, 450), fill=(68, 212, 238, fade), width=3)
    return layer


def shards_layer(explode):
    layer = Image.new('RGBA', (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(layer)
    fade = int(255 * sin(pi * clamp(explode)))
    for index in range(24):
        angle = index * 2.39996
        distance = (16 + (index % 5) * 5) + explode * (132 + index % 7 * 12)
        x = 300 + cos(angle) * distance
        y = 430 + sin(angle) * distance
        size = 3 + (index % 4) * 2
        color = COLORS[index % len(COLORS)] + (fade,)
        draw.polygon([(x, y-size), (x+size*.7, y+size),
                      (x-size*.8, y+size*.3)], fill=color)
    return layer


def word_layer(appear, pulse):
    layer = Image.new('RGBA', (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(layer)
    alpha = int(255 * clamp(appear))
    brightness = .82 + .18 * pulse
    word = 'XonolithOS'
    bounds = draw.textbbox((0, 0), word, font=WORD_FONT)
    x = (WIDTH - (bounds[2] - bounds[0])) / 2
    y = 405
    color = tuple(int(channel * brightness) for channel in (220, 247, 255))
    draw.text((x, y), word, font=WORD_FONT, fill=color + (alpha,))
    sub = 'ANDROID, REFORGED'
    bounds = draw.textbbox((0, 0), sub, font=SUB_FONT)
    draw.text(((WIDTH - (bounds[2] - bounds[0])) / 2, 495), sub,
              font=SUB_FONT, fill=(87, 189, 217, int(alpha * .8)))
    return layer


def frame(index, loop=False):
    canvas = background().convert('RGBA')
    if loop:
        explode, appear = 1.0, 1.0
        pulse = .5 + .5 * sin(index * 2 * pi / 38)
    else:
        explode = clamp((index - 11) / 23)
        appear = clamp((index - 25) / 13)
        pulse = 1.0
        canvas.alpha_composite(phone_layer(explode))
        canvas.alpha_composite(shards_layer(explode))
    word = word_layer(appear, pulse)
    glow = word.filter(ImageFilter.GaussianBlur(19))
    glow.putalpha(glow.getchannel('A').point(lambda a: int(a * (.14 + .16 * pulse))))
    canvas.alpha_composite(glow)
    canvas.alpha_composite(word)
    return canvas.convert('RGB')


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(OUTPUT, 'w', compression=ZIP_STORED) as archive:
        archive.writestr('desc.txt', f'{WIDTH} {HEIGHT} {FPS}\np 1 0 part0\np 0 0 part1\n')
        for part, count in (('part0', 39), ('part1', 38)):
            for index in range(count):
                image = frame(index, loop=(part == 'part1'))
                data = BytesIO()
                image.save(data, format='PNG', optimize=True)
                archive.writestr(f'{part}/{index:04}.png', data.getvalue())
    print(OUTPUT)


if __name__ == '__main__':
    main()
