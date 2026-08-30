#!/usr/bin/env python3
"""Build launcher icons. Prefer complete brand art; never write a half-gray icon."""
import base64
import pathlib
import subprocess

for xml in pathlib.Path("app/src/main/res").rglob("ic_launcher.xml"):
    if "anydpi" in str(xml):
        continue
    xml.unlink()
    print("Removed", xml)

bg = "#0A1628"
sizes = [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]

def write_solid(folder: str, px: int) -> None:
    out = pathlib.Path(f"app/src/main/res/{folder}/ic_launcher.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    # Solid brand square — clean, never gray-half
    subprocess.check_call([
        "convert",
        "-size", f"{px}x{px}",
        f"xc:{bg}",
        str(out),
    ])
    print(f"Solid fallback {out}")

src_path = pathlib.Path("scripts/icon.jpg.b64")
use_art = False
src = pathlib.Path("/tmp/gdata_icon.jpg")

if src_path.exists():
    try:
        raw = base64.b64decode(src_path.read_text().strip())
        src.write_bytes(raw)
        info = subprocess.check_output(
            ["identify", "-format", "%w %h", str(src)], text=True
        ).strip()
        w, h = map(int, info.split())
        print(f"Art decoded {len(raw)} bytes, {w}x{h}")
        # Reject incomplete / tiny / gray-half style corrupt files
        if w >= 128 and h >= 128 and len(raw) >= 8000:
            use_art = True
        else:
            print("Art rejected (too small / likely truncated)")
    except Exception as e:
        print("Art decode failed:", e)

for folder, px in sizes:
    out = pathlib.Path(f"app/src/main/res/{folder}/ic_launcher.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    if use_art:
        subprocess.check_call([
            "convert", str(src),
            "-resize", f"{px}x{px}^",
            "-gravity", "center",
            "-background", bg,
            "-extent", f"{px}x{px}",
            str(out),
        ])
        print(f"Wrote art {out} ({out.stat().st_size} bytes)")
    else:
        write_solid(folder, px)

print("Done — API 26+ uses vector adaptive icon (always correct)")
