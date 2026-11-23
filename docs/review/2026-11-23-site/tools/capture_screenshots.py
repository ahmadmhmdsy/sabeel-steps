#!/usr/bin/env python3
"""Capture and check the Sabeel static site with headless Chrome (Playwright).

Precondition: a static server for site/ is running at http://127.0.0.1:8766/sabeel-steps/
(Pages-like base path, so root-absolute URLs resolve exactly as in production.)

Run from the repository root:
    python docs/review/2026-11-23-site/tools/capture_screenshots.py

Writes PNGs and runtime-report.json under docs/review/2026-11-23-site/.
"""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[4]
OUT = ROOT / "docs" / "review" / "2026-11-23-site"
SHOTS = OUT / "screenshots"
SHOTS.mkdir(parents=True, exist_ok=True)
BASE = "http://127.0.0.1:8766/sabeel-steps"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

PAGES = [
    ("en-desktop", "/", 1440, 1000),
    ("ar-desktop", "/ar/", 1440, 1000),
    ("privacy-desktop", "/privacy/", 1280, 1000),
    ("404-desktop", "/404.html", 1280, 900),
    ("en-mobile", "/", 390, 844),
    ("ar-mobile", "/ar/", 390, 844),
    ("en-narrow", "/", 320, 800),
    ("ar-narrow", "/ar/", 320, 800),
]


def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


STATE_JS = """() => ({
    title: document.title,
    description: document.querySelector('meta[name="description"]')?.content ?? null,
    lang: document.documentElement.lang,
    dir: document.documentElement.dir || '',
    scrollWidth: document.documentElement.scrollWidth,
    innerWidth: window.innerWidth,
    scrollHeight: document.documentElement.scrollHeight,
    h1: document.querySelectorAll('h1').length,
    imgs: Array.from(document.images).map(i => ({src: i.currentSrc || i.src, w: i.naturalWidth, h: i.naturalHeight, complete: i.complete})),
    links: Array.from(document.querySelectorAll('a[href]')).map(a => a.getAttribute('href')),
    stylesheets: Array.from(document.styleSheets).map(s => s.href || '(inline)'),
})"""

PREPARE_JS = """async () => {
    document.documentElement.style.scrollBehavior = 'auto';
    // Force lazy images to load now, then visit every screenful.
    Array.from(document.images).forEach(i => { i.loading = 'eager'; });
    const h = document.documentElement.scrollHeight;
    for (let y = 0; y < h; y += 500) {
        window.scrollTo(0, y);
        await new Promise(r => setTimeout(r, 15));
    }
    window.scrollTo(0, 0);
    await new Promise(r => setTimeout(r, 300));
    const pending = Array.from(document.images).filter(i => !i.complete);
    if (pending.length) {
        await Promise.race([
            Promise.all(pending.map(i => new Promise(res => { i.addEventListener('load', res, {once: true}); i.addEventListener('error', res, {once: true}); }))),
            new Promise(r => setTimeout(r, 8000)),
        ]);
    }
    return document.images.length;
}"""


def capture(p, name: str, path: str, width: int, height: int) -> dict:
    log(f"{name}: opening {BASE + path} at {width}x{height}")
    browser = p.chromium.launch(executable_path=CHROME, headless=True)
    ctx = browser.new_context(viewport={"width": width, "height": height}, device_scale_factor=1)
    page = ctx.new_page()
    page.set_default_timeout(30000)
    page.set_default_navigation_timeout(45000)
    requests: list[str] = []
    failures: list[str] = []
    console: list[str] = []
    page.on("request", lambda req: requests.append(req.url))
    page.on("requestfailed", lambda req: failures.append(f"{req.url} :: {req.failure}"))
    page.on("console", lambda msg: console.append(f"{msg.type}: {msg.text}") if msg.type in ("error", "warning") else None)
    url = BASE + path
    status = None
    try:
        response = page.goto(url, wait_until="domcontentloaded", timeout=45000)
        status = response.status if response else None
        page.wait_for_load_state("load", timeout=30000)
        loaded = page.evaluate(PREPARE_JS)
        log(f"{name}: prepared, {loaded} images in DOM")
        page.wait_for_timeout(300)
    except Exception as exc:  # noqa: BLE001 - report and continue
        log(f"{name}: navigation/prepare problem: {exc}")
    shot = SHOTS / f"{name}.png"
    try:
        page.screenshot(path=str(shot), full_page=True, timeout=60000)
        log(f"{name}: screenshot {shot.stat().st_size} bytes")
    except Exception as exc:  # noqa: BLE001
        log(f"{name}: screenshot failed: {exc}")
        failures.append(f"screenshot failed: {exc}")
    metrics = {}
    try:
        metrics = page.evaluate(STATE_JS)
    except Exception as exc:  # noqa: BLE001
        log(f"{name}: metrics failed: {exc}")
    ctx.close()
    browser.close()

    external = [u for u in requests if not u.startswith(BASE)]
    broken_imgs = [i for i in metrics.get("imgs", []) if not i["complete"] or i["w"] == 0]
    overflow = metrics.get("scrollWidth", 0) - metrics.get("innerWidth", 0)
    entry = {
        "name": name,
        "url": url,
        "status": status,
        "viewport": {"width": width, "height": height},
        "screenshot": str(shot.relative_to(ROOT)),
        "screenshot_bytes": shot.stat().st_size if shot.exists() else None,
        "metrics": metrics,
        "requests": requests,
        "external_requests": external,
        "request_failures": failures,
        "console_errors_or_warnings": console,
        "broken_images": broken_imgs,
        "horizontal_overflow_px": overflow,
    }
    log(f"{name}: status={status} height={metrics.get('scrollHeight')} overflow={overflow} "
        f"requests={len(requests)} external={len(external)} broken_images={len(broken_imgs)}")
    return entry


def main() -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    report: dict = {"base": BASE, "pages": [], "failures": []}
    with sync_playwright() as p:
        for name, path, width, height in PAGES:
            try:
                entry = capture(p, name, path, width, height)
            except Exception as exc:  # noqa: BLE001
                log(f"{name}: aborted: {exc}")
                report["failures"].append(f"{name}: aborted: {exc}")
                continue
            report["pages"].append(entry)
            if entry["external_requests"]:
                report["failures"].append(f"{name}: {len(entry['external_requests'])} external request(s)")
            if [f for f in entry["request_failures"] if "screenshot failed" not in f]:
                report["failures"].append(f"{name}: request failure(s): {entry['request_failures']}")
            if entry["broken_images"]:
                report["failures"].append(f"{name}: {len(entry['broken_images'])} broken image(s)")
            if entry["horizontal_overflow_px"] > 1:
                report["failures"].append(f"{name}: horizontal overflow {entry['horizontal_overflow_px']}px")
            if entry["console_errors_or_warnings"]:
                report["failures"].append(f"{name}: console errors/warnings: {entry['console_errors_or_warnings']}")
            (OUT / "runtime-report.json").write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    if not report["pages"]:
        report["failures"].append("no pages captured")
    (OUT / "runtime-report.json").write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print("")
    print(f"report: {(OUT / 'runtime-report.json').relative_to(ROOT)}")
    if report["failures"]:
        print("FAILURES:")
        for f in report["failures"]:
            print(" -", f)
        return 1
    print("Runtime checks passed: zero external requests, zero broken images, no horizontal overflow, no console errors.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
