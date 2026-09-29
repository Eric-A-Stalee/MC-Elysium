#!/usr/bin/env python3
"""Original leaf sprite and quiet woodland breeze. No sampled or copied game media.

The sprite and synthesis use only Python's standard library. Regenerating the
committed Ogg needs ffmpeg/libvorbis; --check needs neither audio software nor a
Minecraft installation. The normal mod build uses the committed asset.
"""
import argparse
import math
from pathlib import Path
import random
import struct
import subprocess
import tempfile
import wave
import zlib

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/elysium"


def leaf_png():
    # Original grayscale mask; the client applies the regional foliage palette.
    rows = ("....y...", "...yyy..", "..yyvyy.", ".yyvvyy.", "..yvyy..", ".yvyy...", "..v.....", "..s.....")
    colors = {".": (0, 0, 0, 0), "y": (255, 255, 255, 255), "v": (190, 190, 190, 255), "s": (115, 115, 115, 255)}
    raw = b"".join(b"\0" + bytes(channel for char in row for channel in colors[char]) for row in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 8, 8, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def breeze_pcm():
    rate, seconds, overlap = 24000, 24, 2
    rng = random.Random(918421)
    samples, low, middle, fast = [], 0.0, 0.0, 0.0
    for i in range(rate * (seconds + overlap)):
        noise = rng.uniform(-1, 1)
        low += 0.007 * (noise - low)
        middle += 0.045 * (noise - middle)
        fast += 0.35 * (noise - fast)
        t = i / rate
        gust = 0.65 + 0.18 * math.sin(2 * math.pi * t / 12) + 0.10 * math.sin(2 * math.pi * t / 8 + 0.6)
        # Low wind with a little high-frequency leaf rustle, no tones or sudden calls.
        samples.append(gust * (low * 1.8 + (middle - low) * 0.6 + (fast - middle) * 0.09))
    length, fade = rate * seconds, rate * overlap
    for i in range(fade):
        weight = (1 - math.cos(math.pi * i / fade)) / 2
        samples[i] = samples[length + i] * (1 - weight) + samples[i] * weight
    samples = samples[:length]
    scale = 0.28 / max(abs(sample) for sample in samples)
    return rate, b"".join(struct.pack("<h", round(sample * scale * 32767)) for sample in samples)


def chain_png():
    """Quiet, tileable forged metal with sparse irregular mineral accretions."""
    rng = random.Random(813701245)
    rows = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            grain = rng.randrange(-5, 6)
            field = (math.sin(math.tau*x/16+0.4) + 0.6*math.cos(math.tau*y/16+1.1)
                     + 0.4*math.sin(math.tau*(x+y)/16))
            mineral = field > 1.55 and rng.randrange(4) != 0
            hammer = round(5*math.sin(math.tau*(x-y)/16))
            # No frame or inset square: the structure itself supplies the link
            # silhouette. The surface should read as one continuous material.
            base = (120, 116, 103) if mineral else (62+hammer, 65+hammer, 67+hammer)
            row.extend(max(0, min(255, c + grain)) for c in base)
            row.append(255)
        rows.append(b"\0" + row)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b""))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--textures-only", action="store_true", help="regenerate original sprites without rebuilding audio")
    args = parser.parse_args()
    sprite = ASSETS / "textures/particle/golden_leaf.png"
    chain = ASSETS / "textures/block/tartarus_chain.png"
    sound = ASSETS / "sounds/woodland_breeze.ogg"
    if args.check:
        assert sprite.read_bytes() == leaf_png(), "Regenerate the original leaf sprite"
        assert chain.read_bytes() == chain_png(), "Regenerate the original chain texture"
        encoded = sound.read_bytes()
        assert encoded.startswith(b"OggS") and b"\x01vorbis" in encoded[:100], "Missing Vorbis ambience asset"
        assert 10000 < len(encoded) < 1000000
        print("Checked original leaf/chain textures and packaged Vorbis ambience.")
        return
    sprite.parent.mkdir(parents=True, exist_ok=True)
    sound.parent.mkdir(parents=True, exist_ok=True)
    sprite.write_bytes(leaf_png())
    chain.parent.mkdir(parents=True, exist_ok=True)
    chain.write_bytes(chain_png())
    if args.textures_only:
        print("Generated original leaf and Chain of Tartarus textures.")
        return
    rate, pcm = breeze_pcm()
    with tempfile.TemporaryDirectory() as folder:
        wav = Path(folder) / "breeze.wav"
        with wave.open(str(wav), "wb") as stream:
            stream.setnchannels(1); stream.setsampwidth(2); stream.setframerate(rate); stream.writeframes(pcm)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "2",
                        "-fflags", "+bitexact", "-flags:a", "+bitexact", "-map_metadata", "-1", str(sound)], check=True)
    print(f"Generated original 8x8 leaf and 24-second woodland breeze ({sound.stat().st_size} bytes).")


if __name__ == "__main__":
    main()
