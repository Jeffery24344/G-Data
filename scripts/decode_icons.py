#!/usr/bin/env python3
"""Decode brand icon and generate launcher PNGs with proper square padding (no gray bars)."""
import base64
import pathlib
import subprocess

for xml in pathlib.Path("app/src/main/res").rglob("ic_launcher.xml"):
    xml.unlink()
    print("Removed", xml)

b64 = pathlib.Path("scripts/icon.jpg.b64").read_text().strip()
raw = base64.b64decode(b64)
src = pathlib.Path("/tmp/gdata_icon_src.jpg")
src.write_bytes(raw)
print("Source bytes", len(raw))

# Full-bleed square on brand dark blue so launcher is not squished / letterboxed gray
bg = "#0A1628"
for folder, px in [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]:
    out = pathlib.Path(f"app/src/main/res/{folder}/ic_launcher.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    conflict = out.with_suffix(".xml")
    if conflict.exists():
        conflict.unlink()
    # Resize to fit, center on square canvas of brand color (no gray bars)
    subprocess.check_call([
        "convert", str(src),
        "-resize", f"{px}x{px}^",
        "-gravity", "center",
        "-background", bg,
        "-extent", f"{px}x{px}",
        str(out),
    ])
    print("Wrote", out)

print("Done")
