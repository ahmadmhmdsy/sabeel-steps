#!/usr/bin/env python3
"""Static verification for the Sabeel GitHub Pages site.

Run from the repository root:  python docs/review/2026-11-23-site/tools/verify_static.py
Checks structure, metadata lengths, JSON-LD validity, sitemap/manifest validity,
external references, internal links and placeholder text.
Exit code 0 = all checks pass.
"""
from __future__ import annotations

import html as html_lib
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[4]
SITE = ROOT / "site"

ALLOWED_EXTERNAL_HOSTS = {
    "ahmadmhmdsy.github.io": "/sabeel-steps/",
    "github.com": "/ahmadmhmdsy/sabeel-steps",
    "www.apache.org": "/licenses/LICENSE-2.0",
    "schema.org": "/",
    "www.sitemaps.org": "/schemas/sitemap/0.9",
}

PAGES = [
    ("site/index.html", "en", "ltr"),
    ("site/ar/index.html", "ar", "rtl"),
    ("site/privacy/index.html", "en", "ltr"),
    ("site/404.html", "en", "ltr"),
]

problems: list[str] = []
notes: list[str] = []


def problem(msg: str) -> None:
    problems.append(msg)


def parse_html(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    if not text.lstrip().lower().startswith("<!doctype html>"):
        problem(f"{path}: missing <!doctype html>")
    return text


def get_attr(tag: str, name: str) -> str | None:
    m = re.search(r"\b" + re.escape(name) + r"\s*=\s*\"([^\"]*)\"", tag)
    return html_lib.unescape(m.group(1)) if m else None


def check_urls(path: Path, text: str) -> None:
    """External references: only allowlisted hosts; subresources must be local."""
    allowed: list[str] = []
    for m in re.finditer(r"(?:href|src|srcset|content)\s*=\s*\"(?P<url>[^\"]+)\"", text):
        raw_tag_context = text[max(0, m.start() - 120) : m.start()]
        url = html_lib.unescape(m.group("url"))
        if not url:
            continue
        for piece in [p.strip().split(" ")[0] for p in url.split(",")] if "srcset" in m.group(0)[:12] else [url]:
            if piece.startswith("http://") or piece.startswith("https://"):
                parts = urlsplit(piece)
                prefix = ALLOWED_EXTERNAL_HOSTS.get(parts.hostname or "")
                if prefix is None:
                    problem(f"{path}: external URL on a non-allowlisted host: {piece}")
                    continue
                if not (parts.path + ("?" + parts.query if parts.query else "")).startswith(prefix):
                    problem(f"{path}: external URL outside the project path: {piece}")
                    continue
                allowed.append(piece)

    # Detect external subresources specifically (images, scripts, styles, fonts, media, frames).
    for m in re.finditer(r"<(img|script|link|iframe|video|audio|source|embed|object)\b[^>]*>", text, re.I):
        tag = m.group(0)
        tag_name = m.group(1).lower()
        for attr in ("src", "href", "data"):
            val = get_attr(tag, attr)
            if val and re.match(r"https?://", val):
                if tag_name == "link" and re.search(r"\brel\s*=\s*\"(canonical|alternate|manifest|icon|apple-touch-icon|preload|stylesheet)\"", tag):
                    # canonical/alternate are references, not loads; manifest/icon/preload/stylesheet must be local
                    rel = re.search(r"\brel\s*=\s*\"([^\"]+)\"", tag)
                    rel_value = rel.group(1) if rel else ""
                    if rel_value in {"canonical", "alternate"}:
                        continue
                problem(f"{path}: external {tag_name} subresource: {val}")

    if "srcset" in text:
        for m in re.finditer(r"srcset\s*=\s*\"([^\"]+)\"", text):
            for cand in m.group(1).split(","):
                url = cand.strip().split(" ")[0]
                if url.startswith("http"):
                    problem(f"{path}: external srcset candidate: {url}")


def check_page(rel: str, lang: str, direction: str) -> None:
    path = ROOT / rel
    if not path.exists():
        problem(f"{rel}: file missing")
        return
    text = parse_html(path)
    page_tag = re.search(r"<html\b[^>]*>", text)
    if not page_tag:
        problem(f"{rel}: no <html> tag")
    else:
        tag = page_tag.group(0)
        if get_attr(tag, "lang") != lang:
            problem(f"{rel}: html lang is {get_attr(tag, 'lang')!r}, expected {lang!r}")
        dir_value = get_attr(tag, "dir")
        if dir_value is None:
            if direction == "rtl":
                problem(f"{rel}: RTL page without dir attribute")
        elif dir_value != direction:
            problem(f"{rel}: html dir is {dir_value!r}, expected {direction!r}")

    # Title / description / canonical.
    indexed = "404" not in rel
    title_m = re.search(r"<title>(.*?)</title>", text, re.S)
    if not title_m:
        problem(f"{rel}: no <title>")
    else:
        title = html_lib.unescape(title_m.group(1)).strip()
        notes.append(f"{rel}: title {len(title)} chars — {title}")
        if indexed and not (25 <= len(title) <= 65):
            problem(f"{rel}: title length {len(title)} outside 25..65")
    desc_m = re.search(r"<meta\s+name=\"description\"\s+content=\"([^\"]*)\"", text)
    if not desc_m:
        problem(f"{rel}: no meta description")
    else:
        desc = html_lib.unescape(desc_m.group(1)).strip()
        notes.append(f"{rel}: description {len(desc)} chars")
        if indexed and not (100 <= len(desc) <= 170):
            problem(f"{rel}: description length {len(desc)} outside 100..170")
    canon = re.search(r"<link\s+rel=\"canonical\"\s+href=\"([^\"]*)\"", text)
    if not canon and "404" not in rel:
        problem(f"{rel}: no canonical link")
    elif canon and canon.group(1) not in {
        "https://ahmadmhmdsy.github.io/sabeel-steps/",
        "https://ahmadmhmdsy.github.io/sabeel-steps/ar/",
        "https://ahmadmhmdsy.github.io/sabeel-steps/privacy/",
    }:
        problem(f"{rel}: unexpected canonical {canon.group(1)}")

    # Exactly one <h1>.
    h1_count = len(re.findall(r"<h1\b", text))
    if h1_count != 1:
        problem(f"{rel}: {h1_count} <h1> elements, expected 1")

    # Images need alt attributes.
    for m in re.finditer(r"<img\b[^>]*>", text):
        tag = m.group(0)
        if "alt=" not in tag:
            problem(f"{rel}: <img> without alt: {tag[:90]}")

    # JSON-LD must be valid JSON.
    ld_blocks = re.findall(r"<script\s+type=\"application/ld\+json\">(.*?)</script>", text, re.S)
    for i, block in enumerate(ld_blocks):
        try:
            data = json.loads(block)
        except json.JSONDecodeError as exc:
            problem(f"{rel}: JSON-LD block {i} is not valid JSON: {exc}")
            continue
        notes.append(f"{rel}: JSON-LD block {i} parsed, @graph types=" +
                     ",".join(n.get("@type", "?") for n in data.get("@graph", [])))
    if rel in {"site/index.html", "site/ar/index.html"} and not ld_blocks:
        problem(f"{rel}: no JSON-LD")
    if "site/404" in rel:
        if "noindex" not in text:
            problem(f"{rel}: 404 page should be noindex")

    # No other scripts at all (zero-JS site).
    scripts = re.findall(r"<script\b([^>]*)>", text, re.I)
    for attrs in scripts:
        if "application/ld+json" not in attrs:
            problem(f"{rel}: unexpected <script> element: {attrs.strip()}")

    # Placeholder / draft text.
    lowered = text.lower()
    for needle in ("lorem ipsum", "todo", "tbd", "fixme", "xxx", "coming soon", "placeholder"):
        if needle in lowered:
            problem(f"{rel}: placeholder-like text found: {needle}")
    for m in re.finditer(r"\[[A-Z][A-Z _/]{2,}\]", text):
        problem(f"{rel}: unfilled placeholder token {m.group(0)}")

    check_urls(path, text)


def check_internal_links() -> None:
    """Every local href/src in every page must resolve to a real file (anchors checked too)."""
    docs: dict[Path, str] = {}
    for rel, _, _ in PAGES:
        p = ROOT / rel
        if p.exists():
            docs[p] = p.read_text(encoding="utf-8")

    def anchors(text: str) -> set[str]:
        return {m.group(1) for m in re.finditer(r"\bid=\"([^\"]+)\"", text)} | {
            m.group(1) for m in re.finditer(r"\bname=\"([^\"]+)\"", text)
        }

    base_prefix = "/sabeel-steps/"
    for rel, _, _ in PAGES:
        path = ROOT / rel
        if path not in docs:
            continue
        text = docs[path]
        for m in re.finditer(r"<(a|img|link|source)\b[^>]*>", text, re.I):
            tag = m.group(0)
            name = m.group(1).lower()
            url = get_attr(tag, "href") if name in {"a", "link"} else get_attr(tag, "src")
            if not url:
                continue
            if url.startswith(("http://", "https://", "mailto:", "tel:", "data:")):
                continue
            if url.startswith("/"):
                if not url.startswith(base_prefix):
                    problem(f"{rel}: root-absolute URL outside site base: {url}")
                    continue
                target_rel = url[len(base_prefix):]
                target = SITE / target_rel
                if target_rel.endswith("/") or target_rel == "":
                    target = SITE / target_rel / "index.html"
                if not target.exists():
                    problem(f"{rel}: root-absolute link target missing: {url}")
                continue
            split = urlsplit(url)
            frag = split.fragment
            target_rel = split.path
            if not target_rel:
                target = path
            else:
                target = (path.parent / target_rel).resolve()
                if target_rel.endswith("/"):
                    target = target / "index.html"
            if not target.exists():
                problem(f"{rel}: internal link target missing: {url}")
                continue
            if frag and target in docs:
                if frag not in anchors(docs[target]):
                    problem(f"{rel}: anchor #{frag} not found in {target.relative_to(ROOT)}")


def check_sitemap() -> None:
    path = SITE / "sitemap.xml"
    if not path.exists():
        problem("sitemap.xml missing")
        return
    tree = ET.parse(path)
    ns = {"sm": "http://www.sitemaps.org/schemas/sitemap/0.9"}
    urls = tree.findall("sm:url", ns)
    notes.append(f"sitemap.xml: {len(urls)} URLs parsed by ElementTree")
    expected = {
        "https://ahmadmhmdsy.github.io/sabeel-steps/": SITE / "index.html",
        "https://ahmadmhmdsy.github.io/sabeel-steps/ar/": SITE / "ar/index.html",
        "https://ahmadmhmdsy.github.io/sabeel-steps/privacy/": SITE / "privacy/index.html",
    }
    found = set()
    for url in urls:
        loc = url.find("sm:loc", ns)
        lastmod = url.find("sm:lastmod", ns)
        if loc is None or not loc.text:
            problem("sitemap.xml: URL without <loc>")
            continue
        found.add(loc.text)
        if lastmod is None or lastmod.text != "2026-11-23":
            problem(f"sitemap.xml: {loc.text} has lastmod {lastmod.text if lastmod is not None else None!r}")
        if loc.text not in expected:
            problem(f"sitemap.xml: unexpected URL {loc.text}")
        elif not expected[loc.text].exists():
            problem(f"sitemap.xml: target file missing for {loc.text}")
    for missing in set(expected) - found:
        problem(f"sitemap.xml: missing {missing}")


def check_manifest() -> None:
    path = SITE / "manifest.webmanifest"
    if not path.exists():
        problem("manifest.webmanifest missing")
        return
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        problem(f"manifest.webmanifest: not valid JSON: {exc}")
        return
    for key in ("name", "short_name", "start_url", "scope", "display", "theme_color", "icons"):
        if key not in data:
            problem(f"manifest.webmanifest: missing {key!r}")
    for icon in data.get("icons", []):
        src = icon.get("src", "")
        target = SITE / src.replace("/sabeel-steps/", "")
        if not target.exists():
            problem(f"manifest.webmanifest: icon missing on disk: {src}")
    notes.append("manifest.webmanifest: valid JSON, icons exist")


def check_robots() -> None:
    path = SITE / "robots.txt"
    if not path.exists():
        problem("robots.txt missing")
        return
    text = path.read_text(encoding="utf-8")
    if "Sitemap: https://ahmadmhmdsy.github.io/sabeel-steps/sitemap.xml" not in text:
        problem("robots.txt: sitemap line missing or unexpected")
    if "Allow: /" not in text:
        problem("robots.txt: Allow: / missing")


def check_privacy_policy_specifics() -> None:
    path = SITE / "privacy/index.html"
    text = path.read_text(encoding="utf-8")
    csp_m = re.search(r'http-equiv="Content-Security-Policy"\s+content="([^"]+)"', text)
    if not csp_m:
        problem("privacy: CSP meta tag missing")
    else:
        csp = csp_m.group(1)
        for directive in ("default-src 'none'", "style-src 'unsafe-inline'", "img-src 'self'",
                          "base-uri 'none'", "form-action 'none'", "script-src 'none'"):
            if directive not in csp:
                problem(f"privacy: CSP missing {directive!r}")
    for needle, label in [
        ("ahmad2002bc@gmail.com", "contact email"),
        ("Ahmad Mahmoud", "developer name (EN)"),
        ("أحمد محمود", "developer name (AR)"),
        ("2026-11-23", "dates"),
        ("Health Connect", "Health Connect language"),
        ("GPX", "session export"),
        ("saved places", "places"),
        ("native Arabic", "native review note"),
    ]:
        if needle.lower() not in text.lower():
            problem(f"privacy: missing {label} ({needle!r})")
    for token in ("[POSTAL ADDRESS]", "[DEVELOPER LEGAL NAME]", "[CONTACT EMAIL]", "[DATE OF FIRST PUBLICATION]"):
        if token in text:
            problem(f"privacy: unfilled placeholder {token} still present")
    if "docs/legal" in text or "docs/privacy-site" in text:
        problem("privacy: links into internal docs trees")
    if "deploy from /docs" in text:
        problem("privacy: mentions deploying /docs")


def check_faq_matches_jsonld() -> None:
    """Visible <details> Q/A text must match the FAQPage JSON-LD, page by page."""
    for rel in ("site/index.html", "site/ar/index.html"):
        path = ROOT / rel
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        blocks = re.findall(r"<script\s+type=\"application/ld\+json\">(.*?)</script>", text, re.S)
        faq = None
        for block in blocks:
            data = json.loads(block)
            for node in data.get("@graph", []):
                if node.get("@type") == "FAQPage":
                    faq = node
        if faq is None:
            problem(f"{rel}: no FAQPage in JSON-LD")
            continue
        qa = [(html_lib.unescape(re.sub(r"<[^>]+>", "", q)), html_lib.unescape(re.sub(r"<[^>]+>", "", a)))
              for q, a in re.findall(r"<summary>(.*?)</summary>\s*<p>(.*?)</p>", text, re.S)]
        json_qa = [(item["name"], item["acceptedAnswer"]["text"]) for item in faq["mainEntity"]]
        if len(qa) != len(json_qa):
            problem(f"{rel}: visible FAQ count {len(qa)} != JSON-LD count {len(json_qa)}")
        for i, ((vq, va), (jq, ja)) in enumerate(zip(qa, json_qa)):
            if vq.strip() != jq.strip():
                problem(f"{rel}: FAQ Q{i+1} text mismatch:\n  visible: {vq}\n  jsonld : {jq}")
            if va.strip() != ja.strip():
                problem(f"{rel}: FAQ A{i+1} text mismatch:\n  visible: {va[:120]}\n  jsonld : {ja[:120]}")
        notes.append(f"{rel}: FAQ visible == JSON-LD for {len(qa)} questions")


def check_assets_referenced_exist() -> None:
    for rel, _, _ in PAGES:
        path = ROOT / rel
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"(?:src|href)=\"((?:\.\./|assets/|\./)[^\"]+\.(?:png|svg|css|webmanifest|xml|txt))\"", text):
            url = m.group(1)
            target = (path.parent / url).resolve()
            if not target.exists():
                problem(f"{rel}: referenced asset missing: {url}")


def check_workflow() -> None:
    path = ROOT / ".github/workflows/pages.yml"
    if not path.exists():
        problem(".github/workflows/pages.yml missing")
        return
    text = path.read_text(encoding="utf-8")
    for needle in (
        "actions/checkout@v4",
        "actions/configure-pages@v5",
        "actions/upload-pages-artifact@v3",
        "actions/deploy-pages@v4",
        "path: site",
        "pages: write",
        "id-token: write",
        "contents: read",
        "github-pages",
    ):
        if needle not in text:
            problem(f"pages.yml: missing {needle!r}")
    if "\t" in text:
        problem("pages.yml: contains a tab character")
    if "docs" in text.replace("# ", "").replace("Publishes", ""):
        # 'docs' may only appear in comments we own; the publish path must not include it
        if "path: docs" in text or "docs/" in text:
            problem("pages.yml: publishes something from docs/")
    try:
        import yaml  # type: ignore

        data = yaml.safe_load(text)
        jobs = data.get("jobs", {})
        if "build" not in jobs or "deploy" not in jobs:
            problem("pages.yml: build/deploy jobs missing")
        steps = jobs.get("build", {}).get("steps", [])
        names = " ".join(step.get("name", "") + " " + str(step.get("uses", "")) for step in steps)
        if "upload-pages-artifact" not in names:
            problem("pages.yml: upload-pages-artifact not in build steps")
        notes.append("pages.yml: parsed by PyYAML")
    except ImportError:
        notes.append("pages.yml: PyYAML not installed, structure checked textually only")


def main() -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    for rel, lang, direction in PAGES:
        check_page(rel, lang, direction)
    check_internal_links()
    check_assets_referenced_exist()
    check_sitemap()
    check_manifest()
    check_robots()
    check_privacy_policy_specifics()
    check_faq_matches_jsonld()
    check_workflow()

    print("=" * 78)
    print("Sabeel static site verification")
    print("=" * 78)
    for note in notes:
        print("OK   ", note)
    print("-" * 78)
    if problems:
        for p in problems:
            print("FAIL ", p)
        print(f"\n{len(problems)} problem(s)")
        return 1
    print("All static checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
