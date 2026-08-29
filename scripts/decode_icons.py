#!/usr/bin/env python3
import base64, pathlib, subprocess
parts = sorted(pathlib.Path("scripts/icon_parts").glob("part*.txt"))
b64 = "".join(p.read_text().strip() for p in parts)
raw = base64.b64decode(b64)
src = pathlib.Path("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
src.parent.mkdir(parents=True, exist_ok=True)
src.write_bytes(raw)
print("Wrote xxxhdpi", len(raw))
for folder, px in [("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96), ("mipmap-xxhdpi", 144)]:
    out = pathlib.Path(f"app/src/main/res/{folder}/ic_launcher.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.check_call(["convert", str(src), "-resize", f"{px}x{px}", str(out)])
    print("Wrote", out)
print("Done")
