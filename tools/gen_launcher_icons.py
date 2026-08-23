#!/usr/bin/env python3
"""
Hamp Keyboard launcher-icon generator.

SOURCE OF TRUTH: /home/myvps/uploads/APP_LOGO.jpg and nothing else.
The file's SHA-256 is asserted at run time so a stale or swapped source can
never silently produce icons again.

This follows the user-supplied algorithm (navy detected from corner samples,
distance-from-navy plus luminance to build a soft content mask, ImageOps.multiply
to combine alphas) with three corrections that are required for this project:

  1. FOLDER. mipmap-anydpi-v26/ic_launcher.xml in this repo references
     @drawable/ic_launcher_background and @drawable/ic_launcher_foreground.
     Adaptive layers written only to mipmap-<dpi>/ are therefore NEVER READ, which
     is what made several earlier fixes appear to do nothing. Layers are written to
     drawable-<dpi>/ (authoritative) and mirrored to mipmap-<dpi>/ so the icon is
     correct regardless of which path a manifest or XML happens to reference.

  2. LEGACY PLATE. A legacy mipmap/ic_launcher.png consisting of white glyphs on a
     transparent canvas makes launchers composite the icon onto their OWN WHITE
     BACKING PLATE - the white square with white corners seen on device. Legacy
     icons must be self-contained: opaque navy plate, content on top, then the
     squircle/circle mask cut from that plate. This matches requirement 3
     ("navy plate + content + squircle mask").

  3. BOUNDING BOX BY MASS. Image.getbbox() keys off any non-zero pixel. This source
     carries JPEG speckle in its bottom and left edge strips (verified: 3 stray
     bright pixels along the bottom, 4 along the left), which stretches the measured
     box from the real ~645x515 artwork to 842x775 - a 31% error that oversizes the
     content and pushes it off centre. Rows/columns must therefore carry a real
     share of the artwork's ink to count.
"""

import hashlib
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

# ========== CONFIG ==========
SRC = "/home/myvps/uploads/APP_LOGO.jpg"
SRC_SHA256 = "96f3008ce48931f08f141aaf5139f8a6570be66d8daff93f442fbe7f8df9a3a0"
OUT_BASE = "java/res"

NAVY_TOLERANCE = 55             # how close to navy counts as background
CONTENT_SCALE_ADAPTIVE = 0.60   # inside the 66dp/108dp safe zone (0.611)
CONTENT_SCALE_LEGACY = 0.72     # optical fit inside the squircle plate
SQUIRCLE_RADIUS = 0.225         # Android's rounded-square convention
INK_FRACTION = 0.02             # row/col must carry >2% of peak ink to count
ALPHA_FLOOR = 3                 # alpha at or below this is clamped to 0
SS = 4                          # supersampling factor for masks and centring

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def assert_source():
    """Refuse to run unless the source file is byte-for-byte the expected one."""
    if not os.path.exists(SRC):
        sys.exit(f"FATAL: source not found: {SRC}")
    digest = hashlib.sha256(open(SRC, "rb").read()).hexdigest()
    print(f"source      : {SRC}")
    print(f"sha256      : {digest}")
    if digest != SRC_SHA256:
        sys.exit(f"FATAL: source hash mismatch.\n  expected {SRC_SHA256}\n  actual   {digest}\n"
                 "Update SRC_SHA256 only after confirming the new artwork is correct.")
    print("hash check  : OK (matches the approved APP_LOGO.jpg)")
    return Image.open(SRC).convert("RGBA")


def detect_navy(src):
    """Average the four corner pixels to recover the flat plate colour."""
    w, h = src.size
    px = src.load()
    samples = [px[x, y][:3] for x in (5, w - 6) for y in (5, h - 6)]
    navy = tuple(sum(c[i] for c in samples) // 4 for i in range(3))
    print(f"navy        : #{navy[0]:02X}{navy[1]:02X}{navy[2]:02X} {navy}")
    return navy


def build_content_mask(src, navy):
    """Soft alpha mask of the artwork, with the navy plate removed.

    Vectorised form of the supplied per-pixel loop: distance from navy selects
    content, luminance sets its strength, then a Gaussian blur softens the edges.
    """
    rgb = np.array(src.convert("RGB")).astype(np.float32)
    dist = np.sqrt(((rgb - np.array(navy, dtype=np.float32)) ** 2).sum(axis=2))
    lum = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    mask_arr = np.where(dist > NAVY_TOLERANCE, np.clip(lum * 1.25, 0, 255), 0)

    mask = Image.fromarray(mask_arr.astype(np.uint8), "L")
    mask = mask.filter(ImageFilter.GaussianBlur(radius=1.8))

    # Ink-mass bounding box (see header note 3). getbbox() would include the
    # JPEG speckle in the source's bottom/left edge strips.
    arr = np.array(mask).astype(np.float32) / 255.0
    rows, cols = arr.sum(axis=1), arr.sum(axis=0)
    keep_r = np.where(rows > rows.max() * INK_FRACTION)[0]
    keep_c = np.where(cols > cols.max() * INK_FRACTION)[0]
    if keep_r.size == 0 or keep_c.size == 0:
        sys.exit("FATAL: no content found - check NAVY_TOLERANCE")

    naive = mask.getbbox()
    box = (int(keep_c[0]), int(keep_r[0]), int(keep_c[-1]) + 1, int(keep_r[-1]) + 1)
    if naive:
        print(f"bbox naive  : {naive} -> {(naive[2]-naive[0], naive[3]-naive[1])} (speckle-inflated)")
    print(f"bbox by ink : {box} -> {(box[2]-box[0], box[3]-box[1])} (used)")
    return mask.crop(box)


def white_from(mask):
    """Pure white RGB carrying `mask` as its alpha channel."""
    out = Image.new("RGBA", mask.size, (255, 255, 255, 255))
    out.putalpha(mask)
    return out


def compose(canvas, mask, scale, plate=None):
    """Centre the content on a square canvas, composed at SS x then reduced.

    Working supersampled and reducing the finished canvas keeps mdpi/hdpi sharp,
    and rounding the offset (rather than flooring) removes the up-left bias that
    left small icons visibly off centre.
    """
    big = canvas * SS
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0) if plate is None else plate)
    tw = int(round(big * scale))
    art = white_from(mask.resize((tw, max(1, round(tw * mask.height / mask.width))),
                                 Image.LANCZOS))
    out.alpha_composite(art, (int(round((big - art.width) / 2)),
                              int(round((big - art.height) / 2))))
    return out.resize((canvas, canvas), Image.LANCZOS)


def shape(size, kind):
    """Anti-aliased squircle or circle alpha mask."""
    big = size * SS
    m = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(m)
    if kind == "squircle":
        d.rounded_rectangle([0, 0, big - 1, big - 1],
                            radius=int(big * SQUIRCLE_RADIUS), fill=255)
    else:
        d.ellipse([0, 0, big - 1, big - 1], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def apply_shape(img, mask):
    """Multiply the image's alpha by a shape mask.

    Note: multiply() lives in ImageChops, not ImageOps (PIL has no
    ImageOps.multiply - calling it raises AttributeError).

    LANCZOS reduction of the supersampled mask overshoots slightly and can leave
    an alpha of 1-3 in the masked-out corners (measured: alpha=2 at hdpi). That is
    invisible but it is not truly transparent, so anything below the threshold is
    clamped to exactly 0 - the corners outside the shape must be absolute zero.
    """
    r, g, b, a = img.split()
    combined = ImageChops.multiply(a, mask)
    arr = np.array(combined)
    arr[arr <= ALPHA_FLOOR] = 0
    return Image.merge("RGBA", (r, g, b, Image.fromarray(arr, "L")))


def main():
    src = assert_source()
    navy = detect_navy(src)
    content = build_content_mask(src, navy)
    print()

    # drawable/ fallback for densities we do not ship, at xxxhdpi fidelity.
    os.makedirs(f"{OUT_BASE}/drawable", exist_ok=True)
    compose(432, content, CONTENT_SCALE_ADAPTIVE) \
        .save(f"{OUT_BASE}/drawable/ic_launcher_foreground.png")

    for dpi, size in DENSITIES.items():
        adaptive = int(108 * size / 48)
        dw, mm = f"{OUT_BASE}/drawable-{dpi}", f"{OUT_BASE}/mipmap-{dpi}"
        os.makedirs(dw, exist_ok=True)
        os.makedirs(mm, exist_ok=True)

        # --- Adaptive BACKGROUND: solid navy, no artwork whatsoever ---
        bg = Image.new("RGBA", (adaptive, adaptive), (*navy, 255))

        # --- Adaptive FOREGROUND: content only, transparent canvas ---
        fg = compose(adaptive, content, CONTENT_SCALE_ADAPTIVE)

        # Authoritative location (what @drawable/... resolves to), plus a mirror
        # in mipmap-<dpi>/ so either reference style yields the same icon.
        for d in (dw, mm):
            bg.save(f"{d}/ic_launcher_background.png")
            fg.save(f"{d}/ic_launcher_foreground.png")

        # --- LEGACY: navy plate + content, then masked to shape ---
        plate = compose(size, content, CONTENT_SCALE_LEGACY, plate=(*navy, 255))
        apply_shape(plate, shape(size, "squircle")).save(f"{mm}/ic_launcher.png")
        apply_shape(plate, shape(size, "circle")).save(f"{mm}/ic_launcher_round.png")

        print(f"  {dpi:8s} adaptive={adaptive:3d}px legacy={size:3d}px")

    print("\ndone")


if __name__ == "__main__":
    main()
