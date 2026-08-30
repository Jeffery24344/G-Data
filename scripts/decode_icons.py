#!/usr/bin/env python3
"""Build launcher icons from complete base64 art."""
import base64
import pathlib
import subprocess

for xml in pathlib.Path("app/src/main/res").rglob("ic_launcher.xml"):
    # Keep anydpi adaptive; remove density xml only
    if "anydpi" not in str(xml):
        xml.unlink()
        print("Removed", xml)

src_b64 = pathlib.Path("scripts/icon.jpg.b64")
if not src_b64.exists():
    print("No icon.jpg.b64 — skipping PNG generation (vector adaptive will be used)")
    raise SystemExit(0)

raw = base64.b64decode(src_b64.read_text().strip())
src = pathlib.Path("/tmp/gdata_icon.jpg")
src.write_bytes(raw)
print("Decoded", len(raw), "bytes")

# Fail build if image is incomplete/corrupt
info = subprocess.check_output(["identify", "-format", "%w %h", str(src)], text=True).strip()
w, h = map(int, info.split())
print("Image size", w, "x", h)
if w < 64 or h < 64:
    raise SystemExit("Icon source too small / corrupt")

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
    subprocess.check_call([
        "convert", str(src),
        "-resize", f"{px}x{px}^",
        "-gravity", "center",
        "-background", bg,
        "-extent", f"{px}x{px}",
        str(out),
    ])
    sz = out.stat().st_size
    print(f"Wrote {out} ({sz} bytes)")
    if sz < 800:
        raise SystemExit(f"Generated icon too small: {out}")

print("Done")
