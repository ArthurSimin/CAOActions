import argparse
import pathlib
import shutil
import struct
import subprocess
import tempfile
import zlib

from PIL import Image

MAGIC = b"CAOV"
VERSION = 2
PALETTE_COLORS = 64
PALETTE_SAMPLES = 80


def extract_frames(video, workdir, width, height, fps):
    subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(video),
         "-vf", f"fps={fps},scale={width}:{height}:flags=lanczos",
         str(workdir / "%05d.png"), "-y"],
        check=True,
    )
    return sorted(workdir.glob("*.png"))


def build_palette(images, colors):
    step = max(1, len(images) // PALETTE_SAMPLES)
    sample = images[::step][:PALETTE_SAMPLES]
    montage = Image.new("RGB", (160, 120 * len(sample)))
    for i, image in enumerate(sample):
        montage.paste(image.resize((160, 120), Image.LANCZOS), (0, i * 120))
    return montage.quantize(colors=colors, method=Image.MEDIANCUT)


def encode(video, out, width, height, fps, colors):
    if not 1 <= colors <= 256:
        raise SystemExit("colors must be between 1 and 256")
    if not 1 <= width <= 0xFFFF or not 1 <= height <= 0xFFFF:
        raise SystemExit("width and height must fit in a u16")
    if not 1 <= fps <= 255:
        raise SystemExit("fps must fit in a u8")

    workdir = pathlib.Path(tempfile.mkdtemp(prefix="cvid-"))
    try:
        paths = extract_frames(video, workdir, width, height, fps)
        if not paths:
            raise SystemExit("ffmpeg produced no frames")
        if len(paths) > 0xFFFF:
            raise SystemExit(f"{len(paths)} frames exceeds the 65535 the header can hold")

        images = [Image.open(p).convert("RGB") for p in paths]
        palette = build_palette(images, colors)
        raw = b"".join(
            image.quantize(palette=palette, dither=Image.NONE).tobytes() for image in images
        )

        table = palette.getpalette()[: colors * 3]
        table += [0] * (colors * 3 - len(table))

        header = (MAGIC + struct.pack(">BHHBHH", VERSION, width, height, fps, len(images), colors)
                  + bytes(table))
        out.write_bytes(header + zlib.compress(raw, 9))

        print(f"frames  {len(images)} ({len(images) / fps:.2f}s @ {fps}fps)")
        print(f"raw     {len(raw) / 1048576:.1f} MB")
        print(f"packed  {out.stat().st_size / 1048576:.2f} MB -> {out}")
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


def main():
    parser = argparse.ArgumentParser(description="Pack a video into a CAOV frame pack.")
    parser.add_argument("video", type=pathlib.Path)
    parser.add_argument("out", type=pathlib.Path)
    parser.add_argument("--width", type=int, default=320)
    parser.add_argument("--height", type=int, default=240)
    parser.add_argument("--fps", type=int, default=20)
    parser.add_argument("--colors", type=int, default=PALETTE_COLORS,
                        help="1-256; the header carries it, so FramePack adapts")
    args = parser.parse_args()
    encode(args.video, args.out, args.width, args.height, args.fps, args.colors)


if __name__ == "__main__":
    main()
