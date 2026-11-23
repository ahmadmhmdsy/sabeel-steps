"""Build site images for Sabeel: cropped screenshots, hero montages, favicons, OG card.

Inputs are the real emulator captures in docs/assets/screenshots/. The ONLY content
edit is removing the emulator's "No step sensor" notice band from the three Home
captures (flat card on flat background, stitched rows); source files are untouched.
"""
import base64
import io
import os
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[4] if "__file__" in globals() else Path(r"E:\android_apps\steps_app")
SRC = ROOT / "docs" / "assets" / "screenshots"
SITE = ROOT / "site"
OUT = SITE / "assets"
SHOTS_OUT = OUT / "screenshots"
TMP = ROOT / "docs" / "review" / "2026-11-23-site" / "_tmp"
for d in (OUT, SHOTS_OUT, TMP):
    d.mkdir(parents=True, exist_ok=True)

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

CREAM = (250, 247, 242)
TEAL = (15, 107, 92)
SAND = (233, 216, 184)
INK = (24, 45, 39)


def crop_notice(name: str, cuts: dict):
    """Remove the emulator notice band (rows cut_start..cut_end inclusive)."""
    src = SRC / name
    im = Image.open(src).convert("RGB")
    a, b = cuts[src.name]
    out = Image.new("RGB", (im.width, im.height - (b - a + 1)))
    out.paste(im.crop((0, 0, im.width, a)), (0, 0))
    out.paste(im.crop((0, b + 1, im.width, im.height)), (0, a))
    return out


def rounded(im: Image.Image, radius: int) -> Image.Image:
    im = im.convert("RGBA")
    mask = Image.new("L", im.size, 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius=radius, fill=255)
    im.putalpha(mask)
    return im


def add_shadow(canvas: Image.Image, size, radius, pos, blur=22, offset=(-4, 18), alpha=52):
    w, h = size
    sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(sh)
    x, y = pos[0] + offset[0], pos[1] + offset[1]
    d.rounded_rectangle((x, y, x + w, y + h), radius=radius, fill=(20, 40, 34, alpha))
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    canvas.alpha_composite(sh)


def gradient(size, top, bottom):
    im = Image.new("RGB", size)
    d = ImageDraw.Draw(im)
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        d.line(
            [(0, y), (size[0], y)],
            fill=(
                round(top[0] + (bottom[0] - top[0]) * t),
                round(top[1] + (bottom[1] - top[1]) * t),
                round(top[2] + (bottom[2] - top[2]) * t),
            ),
        )
    return im.convert("RGBA")


def stride_arcs(canvas, origin, scale, color, alpha, width):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    paths = [
        ((32, 74), (40, 67), (49, 64), (60, 64)),
        ((38, 63), (44, 57), (52, 54), (62, 54)),
        ((45, 52), (50, 47), (56, 45), (63, 45)),
    ]
    ox, oy = origin

    def bez(p0, p1, p2, p3, steps=80):
        pts = []
        for i in range(steps + 1):
            t = i / steps
            mt = 1 - t
            x = mt**3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t**3 * p3[0]
            y = mt**3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t**3 * p3[1]
            pts.append((ox + x * scale, oy + y * scale))
        return pts

    for p in paths:
        pts = bez(*p)
        d.line(pts, fill=color + (alpha,), width=max(1, round(width)))
        r = width / 2
        for pt in (pts[0], pts[-1]):
            d.ellipse((pt[0] - r, pt[1] - r, pt[0] + r, pt[1] + r), fill=color + (alpha,))
    canvas.alpha_composite(layer)


def hero(order, crops, filename):
    W, H = 1600, 1000
    canvas = gradient((W, H), (248, 243, 232), (240, 230, 210))
    # soft brand decoration
    deco = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(deco)
    d.ellipse((-260, 640, 360, 1260), fill=SAND + (70,))
    d.ellipse((1240, -230, 1860, 390), fill=SAND + (95,))
    canvas.alpha_composite(deco)
    stride_arcs(canvas, (60, 690), 2.6, TEAL, 40, 5)
    stride_arcs(canvas, (1180, 700), 2.2, TEAL, 30, 5)

    target_h = 760
    bottom = 900
    gap = 26
    cards = []
    for key in order:
        im = crops[key]
        print("hero input", filename, key, im.size)
        h = target_h
        w = round(im.width * h / im.height)
        im = im.resize((w, h), Image.LANCZOS)
        cards.append((im, w, h))

    total = sum(c[1] for c in cards) + gap * (len(cards) - 1)
    x = (W - total) // 2
    for im, w, h in cards:
        pos = (x, bottom - h)
        add_shadow(canvas, (w, h), 26, pos)
        canvas.alpha_composite(rounded(im, 26), pos)
        x += w + gap

    out = canvas.convert("RGB")
    out.save(OUT / filename, optimize=True)
    return out


def favicons():
    S = 2048
    scale = S / 108
    im = Image.new("RGB", (S, S), SAND)
    d = ImageDraw.Draw(im)
    paths = [
        ((32, 74), (40, 67), (49, 64), (60, 64)),
        ((38, 63), (44, 57), (52, 54), (62, 54)),
        ((45, 52), (50, 47), (56, 45), (63, 45)),
    ]

    def bez(p0, p1, p2, p3, steps=140):
        pts = []
        for i in range(steps + 1):
            t = i / steps
            mt = 1 - t
            x = mt**3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t**3 * p3[0]
            y = mt**3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t**3 * p3[1]
            pts.append((x * scale, y * scale))
        return pts

    width = max(2, round(5 * scale))
    for p in paths:
        pts = bez(*p)
        d.line(pts, fill=TEAL, width=width, joint="curve")
        r = width / 2
        for pt in (pts[0], pts[-1]):
            d.ellipse((pt[0] - r, pt[1] - r, pt[0] + r, pt[1] + r), fill=TEAL)
    cx, cy, r = 70 * scale, 36 * scale, 5 * scale
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=TEAL)
    for size, name in ((512, "favicon-512.png"), (180, "favicon-180.png"), (32, "favicon-32.png")):
        im.resize((size, size), Image.LANCZOS).save(OUT / name, optimize=True)
    shutil.copyfile(ROOT / "docs" / "assets" / "sabeel-mark.svg", OUT / "favicon.svg")


def b64(path):
    return base64.b64encode(Path(path).read_bytes()).decode("ascii")


def og_card(home_crop: Image.Image, insights: Image.Image):
    for im, name, width in ((home_crop, "home.png", 460), (insights, "insights.png", 400)):
        im.resize((width, round(im.height * width / im.width)), Image.LANCZOS).save(TMP / name, optimize=True)
    home_html = b64(TMP / "home.png")
    insights_html = b64(TMP / "insights.png")
    mark_svg = (ROOT / "docs" / "assets" / "sabeel-mark.svg").read_text(encoding="utf-8")
    mark_svg = mark_svg.replace('width="512" height="512"', 'width="64" height="64"')
    html = """<!doctype html><html lang="en"><head><meta charset="utf-8">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: 1200px; height: 630px; overflow: hidden; }
  body { font-family: "Segoe UI", system-ui, -apple-system, Roboto, Arial, sans-serif;
         background: #faf7f2; color: #1c2b26; position: relative; }
  .deco { position: absolute; border-radius: 50%; }
  .deco.sand1 { width: 520px; height: 520px; right: -170px; top: -210px; background: #e9d8b8; opacity: .55; }
  .deco.sand2 { width: 260px; height: 260px; left: -120px; bottom: -150px; background: #e9d8b8; opacity: .5; }
  .arcs { position: absolute; left: 36px; bottom: 18px; opacity: .5; }
  .wrap { position: relative; display: flex; height: 100%; }
  .left { width: 690px; padding: 62px 20px 48px 72px; display: flex; flex-direction: column; }
  .brand { display: flex; align-items: center; gap: 16px; }
  .word { font-family: Georgia, "Times New Roman", serif; font-size: 46px; letter-spacing: -0.5px; color: #0f6b5c; line-height: 1; }
  .ar { font-family: "Segoe UI", Tahoma, "Noto Naskh Arabic", sans-serif; font-size: 30px; color: #0f6b5c; }
  h1 { font-family: Georgia, "Times New Roman", serif; font-weight: 400; font-size: 47px; line-height: 1.1;
       letter-spacing: -0.8px; color: #18302a; margin-top: 34px; }
  h1 em { font-style: normal; color: #0f6b5c; }
  p.tag { margin-top: 18px; font-size: 22px; line-height: 1.4; color: #4f4a41; max-width: 520px; }
  .chips { margin-top: 26px; display: flex; gap: 10px; flex-wrap: wrap; }
  .chip { border: 1.5px solid rgba(15, 107, 92, .35); color: #0f6b5c; border-radius: 999px;
          padding: 7px 14px; font-size: 15px; font-weight: 600; letter-spacing: .2px; }
  .url { margin-top: auto; font-size: 15px; color: #7a746a; letter-spacing: .3px; }
  .right { position: relative; flex: 1; }
  .phone { position: absolute; border-radius: 22px; box-shadow: 0 24px 48px rgba(24, 48, 39, .22),
           0 2px 0 rgba(255,255,255,.7) inset; }
  .p1 { width: 232px; top: 92px; left: 40px; }
  .p2 { width: 204px; top: 168px; left: 236px; }
</style></head><body>
<div class="deco sand1"></div><div class="deco sand2"></div>
<svg class="arcs" width="240" height="150" viewBox="0 0 108 108" aria-hidden="true">
  <g fill="none" stroke="#0f6b5c" stroke-width="4.5" stroke-linecap="round" opacity=".22">
    <path d="M32,74 C40,67 49,64 60,64"/><path d="M38,63 C44,57 52,54 62,54"/><path d="M45,52 C50,47 56,45 63,45"/>
  </g><circle cx="70" cy="36" r="4.5" fill="#0f6b5c" opacity=".22"/>
</svg>
<div class="wrap">
  <div class="left">
    <div class="brand">__MARK__<span class="word">Sabeel</span><span class="ar">سبيل</span></div>
    <h1>An offline step counter that <em>keeps your steps to itself.</em></h1>
    <p class="tag">Quiet counting, honest limits, and no way for the app to send your data anywhere.</p>
    <div class="chips"><span class="chip">No ads</span><span class="chip">No account</span><span class="chip">No INTERNET permission</span></div>
    <div class="url">ahmadmhmdsy.github.io/sabeel-steps</div>
  </div>
  <div class="right">
    <img class="phone p1" src="data:image/png;base64,__HOME__" alt="">
    <img class="phone p2" src="data:image/png;base64,__INSIGHTS__" alt="">
  </div>
</div>
</body></html>"""
    html = html.replace("__MARK__", mark_svg.replace("<svg ", '<svg style="border-radius:12px" '))
    html = html.replace("__HOME__", home_html).replace("__INSIGHTS__", insights_html)
    template = TMP / "og.html"
    template.write_text(html, encoding="utf-8")
    out = OUT / "og-1200x630.png"
    subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-first-run",
         "--no-default-browser-check", "--user-data-dir=" + str(TMP / "chrome-profile"),
         "--force-device-scale-factor=1", "--window-size=1200,630",
         "--screenshot=" + str(out), template.as_uri()],
        check=True, capture_output=True, timeout=120,
    )
    return out


def main():
    cuts = {
        "home-en.png": (415, 736),
        "home-ar.png": (427, 766),
        "home-dark-en.png": (415, 736),
    }
    cropped = {}
    for name in cuts:
        im = crop_notice(name, cuts)
        cropped[name] = im
        print("cropped", name, im.size)
        im.save(TMP / ("crop-" + name), optimize=True)
        # sanity: the stitch seam should be flat background for the full width
        px = im.load()
        seam_rows = range(max(0, cuts[name][0] - 2), min(im.height, cuts[name][0] + 3))
        worst = 0
        for y in seam_rows:
            for x in range(0, im.width, 2):
                p = px[x, y]
                worst = max(worst, max(abs(p[0] - CREAM[0]), abs(p[1] - CREAM[1]), abs(p[2] - CREAM[2])))
        print("  seam max deviation from cream bg:", worst)

    # gallery copies at 540 px wide
    gallery = {
        "home-en": cropped["home-en.png"],
        "home-ar": cropped["home-ar.png"],
        "home-dark-en": cropped["home-dark-en.png"],
        "insights-en": Image.open(SRC / "insights-en.png").convert("RGB"),
        "history-en": Image.open(SRC / "history-en.png").convert("RGB"),
        "settings-en": Image.open(SRC / "settings-en.png").convert("RGB"),
        "walk-en": Image.open(SRC / "walk-en.png").convert("RGB"),
    }
    for key, im in gallery.items():
        w = 540
        h = round(im.height * w / im.width)
        im.resize((w, h), Image.LANCZOS).save(SHOTS_OUT / (key + ".png"), optimize=True)
        print("gallery", key, (w, h))

    full = {name: Image.open(SRC / name).convert("RGB") for name in
            ("history-en.png", "insights-en.png", "settings-en.png")}
    hero_sources = {
        "home-en": cropped["home-en.png"],
        "home-ar": cropped["home-ar.png"],
        "history-en": full["history-en.png"],
        "insights-en": full["insights-en.png"],
        "settings-en": full["settings-en.png"],
    }
    hero(["home-en", "history-en", "insights-en", "settings-en"], hero_sources, "hero.png")
    hero(["home-ar", "history-en", "insights-en", "settings-en"], hero_sources, "hero-ar.png")

    # responsive variants
    for base, widths in (("hero.png", (960, 640)), ("hero-ar.png", (960, 640))):
        im = Image.open(OUT / base)
        for w in widths:
            im.resize((w, round(im.height * w / im.width)), Image.LANCZOS).save(
                OUT / (base.replace(".png", f"-{w}.png")), optimize=True)

    favicons()
    og = og_card(cropped["home-en.png"], Image.open(SRC / "insights-en.png").convert("RGB"))
    print("og", og, Image.open(og).size)

    shutil.copyfile(OUT / "hero.png", ROOT / "docs" / "assets" / "sabeel-hero.png")
    shutil.copyfile(OUT / "og-1200x630.png", ROOT / "docs" / "assets" / "social-card.png")
    for p in sorted(OUT.glob("*")):
        if p.is_file():
            print("out", p.name, p.stat().st_size)
    for p in sorted(SHOTS_OUT.glob("*")):
        print("shot", p.name, p.stat().st_size)


if __name__ == "__main__":
    main()
