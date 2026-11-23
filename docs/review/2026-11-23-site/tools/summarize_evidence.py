#!/usr/bin/env python3
"""Summarise the captured runtime report and compute site statistics for REPORT.md."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
report = json.loads((ROOT / "docs/review/2026-11-23-site/runtime-report.json").read_text(encoding="utf-8"))
print("pages captured:", len(report["pages"]))
for e in report["pages"]:
    m = e["metrics"]
    print(f'- {e["name"]}: viewport {e["viewport"]["width"]}x{e["viewport"]["height"]}, doc height {m["scrollHeight"]}px, '
          f'requests {len(e["requests"])}, external {len(e["external_requests"])}, images {len(m["imgs"])}, '
          f'broken {len(e["broken_images"])}, overflow {e["horizontal_overflow_px"]}px, h1 {m["h1"]}, dir "{m["dir"]}"')
print()
print("requests on en-desktop:")
for u in report["pages"][0]["requests"]:
    print("  ", u.replace(report["base"], ""))
print("stylesheets:", report["pages"][0]["metrics"]["stylesheets"])

# Internal link / anchor statistics across the four HTML pages.
pages = {
    "site/index.html": ROOT / "site/index.html",
    "site/ar/index.html": ROOT / "site/ar/index.html",
    "site/privacy/index.html": ROOT / "site/privacy/index.html",
    "site/404.html": ROOT / "site/404.html",
}
texts = {k: p.read_text(encoding="utf-8") for k, p in pages.items()}
def anchors(t):
    return set(re.findall(r'\bid="([^"]+)"', t))
local = anchors_checked = external = imgs = imgs_alt = 0
for rel, text in texts.items():
    path = pages[rel]
    for m in re.finditer(r"<(a|img|link|source)\b[^>]*>", text, re.I):
        tag = m.group(0)
        name = m.group(1).lower()
        url_m = re.search(r'\b(?:href|src)="([^"]+)"', tag)
        if not url_m:
            continue
        url = url_m.group(1)
        if name == "img":
            imgs += 1
            if re.search(r'\balt="', tag):
                imgs_alt += 1
        if url.startswith(("http://", "https://", "mailto:", "tel:", "data:")):
            if name in ("a", "link"):
                external += 1
            continue
        local += 1
        frag = url.split("#", 1)[1] if "#" in url else ""
        if frag:
            target = path
            anchors_checked += 1
            if frag not in anchors(texts[rel]):
                print("MISSING ANCHOR", rel, url)
print(f"\nlocal href/src references: {local}")
print(f"fragment anchors checked: {anchors_checked}")
print(f"external (link/allowlisted) hrefs: {external}")
print(f"images total: {imgs}, with alt: {imgs_alt}")
print(f"HTML pages: {len(pages)}")

# WCAG contrast ratios for the core palette.
def lum(hex_color):
    r, g, b = (int(hex_color[i:i+2], 16) / 255 for i in (1, 3, 5))
    def f(c):
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)

def ratio(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)

pairs = [
    ("teal #0F6B5C on cream #FAF7F2", "#0F6B5C", "#FAF7F2"),
    ("teal-deep #0B4F44 on cream #FAF7F2", "#0B4F44", "#FAF7F2"),
    ("muted #5E5A51 on cream #FAF7F2", "#5E5A51", "#FAF7F2"),
    ("ink #1D2B26 on cream #FAF7F2", "#1D2B26", "#FAF7F2"),
    ("cream #FAF7F2 on teal-deep #0B4F44", "#FAF7F2", "#0B4F44"),
    ("cream #FAF7F2 on teal #0F6B5C", "#FAF7F2", "#0F6B5C"),
    ("sand #E9D8B8 on teal-deep #0B4F44", "#E9D8B8", "#0B4F44"),
    ("accent #A2542F on paper #FFFDF9", "#A2542F", "#FFFDF9"),
]
print("\ncontrast ratios (WCAG 2.x):")
for label, a, b in pairs:
    print(f"  {label}: {ratio(a, b):.2f}:1")
