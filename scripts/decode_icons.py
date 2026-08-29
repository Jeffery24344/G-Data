#!/usr/bin/env python3
import base64, pathlib, subprocess

# Remove any XML launcher stubs that would conflict with PNG resources
for xml in pathlib.Path("app/src/main/res").rglob("ic_launcher.xml"):
    xml.unlink()
    print("Removed", xml)

b64 = pathlib.Path("scripts/icon.jpg.b64").read_text().strip()
raw = base64.b64decode(b64)
src_jpg = pathlib.Path("app/src/main/res/mipmap-xxxhdpi/ic_launcher_src.jpg")
src_jpg.parent.mkdir(parents=True, exist_ok=True)
src_jpg.write_bytes(raw)
print("Wrote source jpg", len(raw))

for folder, px in [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]:
    out = pathlib.Path(f"app/src/main/res/{folder}/ic_launcher.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    # Remove conflicting xml in same folder if any
    conflict = out.with_suffix(".xml")
    if conflict.exists():
        conflict.unlink()
    subprocess.check_call([
        "convert", str(src_jpg), "-resize", f"{px}x{px}", str(out)
    ])
    print("Wrote", out)

src_jpg.unlink(missing_ok=True)
print("Done")
