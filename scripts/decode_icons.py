#!/usr/bin/env python3
"""
Build launcher icons with correct placement.

Android adaptive masks (circle / squircle) crop the outer ~18% of the canvas.
We place the artwork in the center ~72% so it is not cut off or pushed to a corner.
Background is solid brand blue (#0A1628) so there is no gray letterboxing.
"""
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

bg = "#0A1628"

# Densities: (folder, pixel size)
sizes = [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]

for folder, px in sizes:
    out_dir = pathlib.Path(f"app/src/main/res/{folder}")
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / "ic_launcher.png"

    # Inner size ~72% of canvas (adaptive safe zone)
    inner = max(int(px * 0.72), 1)

    # 1) Scale artwork to fit inside safe zone
    # 2) Center on square brand-blue canvas (no gray bars, correct placement)
    subprocess.check_call([
        "convert", str(src),
        "-resize", f"{inner}x{inner}",
        "-gravity", "center",
        "-background", bg,
        "-extent", f"{px}x{px}",
        str(out),
    ])
    print(f"Wrote {out} ({px}px, art={inner}px)")

print("Done — icons centered with adaptive safe-zone padding")
