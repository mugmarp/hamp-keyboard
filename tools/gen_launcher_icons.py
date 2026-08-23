#!/usr/bin/env python3
"""
Hamp Keyboard launcher-icon generator.

WHY THIS SCRIPT EXISTS
----------------------
Two subtle Android resource bugs produced visible artefacts on device and are
guarded against here:

  Bug A - wrong folder.
      mipmap-anydpi-v26/ic_launcher.xml references @drawable/ic_launcher_*.
      Writing layers into mipmap-<dpi>/ therefore has NO effect on the adaptive
      icon: the XML never reads those files. Every adaptive layer this script
      writes goes into drawable-<dpi>/ (plus a drawable/ fallback).

  Bug B - transparent legacy icon.
      A legacy mipmap/ic_launcher.png that is mostly transparent (bare white
      glyphs) makes launchers composite the icon onto their own WHITE plate,
      which is what produced the white square frame with white corners.
      Legacy icons must therefore be SELF-CONTAINED: opaque navy plate, content
      on top, then the rounded-square / circle mask cut out of that plate.

LAYER CONTRACT
--------------
  drawable-<dpi>/ic_launcher_background.png  solid navy, fully opaque, no art.
  drawable-<dpi>/ic_launcher_foreground.png  white content only, centred,
                                             transparent canvas, inside safe zone.
  mipmap-<dpi>/ic_launcher.png               navy plate + content, squircle mask.
  mipmap-<dpi>/ic_launcher_round.png         navy plate + content, circle mask.

The content is extracted from the source artwork by luminance, so the navy
plate is never baked into the foreground layer (that was the double-image bug:
the same full-bleed art in both background and foreground).
"""

import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SRC = "/home/myvps/uploads/APP_LOGO.jpg"
RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "java", "res")

# Density buckets: legacy launcher size (dp=48) and adaptive canvas (dp=108).
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

# Adaptive icons are 108dp with only the central 66dp guaranteed visible; the
# outer 18dp on each side can be cropped by any launcher mask. Keep content at
# or below that ratio so nothing is ever clipped.
SAFE_ZONE_RATIO = 66.0 / 108.0          # 0.611
FG_CONTENT_RATIO = 0.58                  # a touch inside the safe zone
LEGACY_CONTENT_RATIO = 0.62              # optical fit inside the squircle plate
SQUIRCLE_RADIUS_RATIO = 0.225            # Android's rounded-square convention
SS = 4                                   # supersampling factor for mask edges


def extract_content_and_plate(path):
    """Split the source art into (navy plate colour, white content alpha mask).

    The source is a dark navy square with light artwork on it. Sampling the
    corners gives the plate colour; thresholding luminance isolates the artwork
    without dragging any navy into the foreground layer.
    """
    rgb = np.array(Image.open(path).convert("RGB")).astype(np.float32)

    # Plate colour: median of the four corner patches, robust to JPEG ringing.
    h, w, _ = rgb.shape
    p = 24
    corners = np.concatenate([
        rgb[:p, :p].reshape(-1, 3),
        rgb[:p, w - p:].reshape(-1, 3),
        rgb[h - p:, :p].reshape(-1, 3),
        rgb[h - p:, w - p:].reshape(-1, 3),
    ])
    navy = tuple(int(v) for v in np.median(corners, axis=0))

    lum = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    plate_lum = 0.299 * navy[0] + 0.587 * navy[1] + 0.114 * navy[2]

    # Ramp alpha from just above the plate luminance to full white. Anything at
    # or below the plate becomes fully transparent, so no navy leaks through.
    lo = plate_lum + 18.0
    hi = 210.0
    alpha = np.clip((lum - lo) / (hi - lo), 0.0, 1.0) * 255.0

    # Kill isolated JPEG speckle before measuring the bounding box, otherwise a
    # few stray bright pixels in the margins inflate it (this is what made the
    # earlier attempts mis-centre the content).
    mask = Image.fromarray(alpha.astype(np.uint8), "L")
    mask = mask.filter(ImageFilter.MedianFilter(size=5))
    mask = mask.filter(ImageFilter.GaussianBlur(radius=0.6))

    # Bounding box by MASS, not by extremes. A median filter still leaves the
    # odd surviving speckle, and a single stray pixel in the margin stretched the
    # measured box from the real 643x513 artwork to 842x775 - a 31% error that
    # both oversized the content and pushed it off centre. Instead, require a row
    # or column to carry a meaningful share of the artwork's ink before it counts
    # as part of the content.
    arr = np.array(mask).astype(np.float32) / 255.0
    row_mass = arr.sum(axis=1)
    col_mass = arr.sum(axis=0)
    row_keep = np.where(row_mass > row_mass.max() * 0.02)[0]
    col_keep = np.where(col_mass > col_mass.max() * 0.02)[0]
    box = (int(col_keep[0]), int(row_keep[0]),
           int(col_keep[-1]) + 1, int(row_keep[-1]) + 1)
    return navy, mask.crop(box)


def white_from_mask(mask):
    """Pure white RGB carrying the mask as its alpha channel."""
    out = Image.new("RGBA", mask.size, (255, 255, 255, 255))
    out.putalpha(mask)
    return out


def fit(mask, box_w):
    """Scale the content mask so its width is box_w, preserving aspect."""
    w, h = mask.size
    return mask.resize((box_w, max(1, round(box_w * h / w))), Image.LANCZOS)


def shape_mask(size, kind):
    """Anti-aliased squircle or circle alpha mask, rendered supersampled."""
    big = size * SS
    m = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(m)
    if kind == "squircle":
        d.rounded_rectangle((0, 0, big - 1, big - 1),
                            radius=int(big * SQUIRCLE_RADIUS_RATIO), fill=255)
    else:
        d.ellipse((0, 0, big - 1, big - 1), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def centred(canvas_size, mask, content_ratio, plate=None):
    """Render content centred on a square canvas, composed at SS x then reduced.

    Composing at supersampled resolution and downscaling the *finished* canvas
    keeps small icons sharp and keeps the content geometrically centred. Placing
    an already-downscaled bitmap instead leaves it off-centre by up to a pixel
    at mdpi/hdpi and softens the glyph edges.

    plate: optional opaque RGBA fill drawn behind the content (legacy icons).
    """
    big = canvas_size * SS
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0) if plate is None else plate)
    art = white_from_mask(fit(mask, int(round(big * content_ratio))))
    # Round rather than floor the offset: flooring biases the artwork up-left by
    # up to a full supersampled pixel, which is visible once reduced to mdpi.
    out.alpha_composite(art, (int(round((big - art.width) / 2)),
                              int(round((big - art.height) / 2))))
    return out.resize((canvas_size, canvas_size), Image.LANCZOS)


def main():
    navy, content = extract_content_and_plate(SRC)
    print(f"plate navy   : #{navy[0]:02X}{navy[1]:02X}{navy[2]:02X} {navy}")
    print(f"content bbox : {content.size}")

    # drawable/ fallback for the adaptive foreground (used if a device asks for
    # a density we did not ship). Generated at xxxhdpi fidelity.
    fallback_canvas = 432
    centred(fallback_canvas, content, FG_CONTENT_RATIO) \
        .save(os.path.join(RES, "drawable", "ic_launcher_foreground.png"))

    for dpi, scale in DENSITIES.items():
        adaptive = int(round(108 * scale))   # adaptive layer canvas
        legacy = int(round(48 * scale))      # legacy launcher icon

        dw = os.path.join(RES, f"drawable-{dpi}")
        mm = os.path.join(RES, f"mipmap-{dpi}")
        os.makedirs(dw, exist_ok=True)
        os.makedirs(mm, exist_ok=True)

        # --- adaptive background: solid navy, no artwork whatsoever ---
        Image.new("RGBA", (adaptive, adaptive), (*navy, 255)) \
             .save(os.path.join(dw, "ic_launcher_background.png"))

        # --- adaptive foreground: white content only, transparent canvas ---
        centred(adaptive, content, FG_CONTENT_RATIO) \
            .save(os.path.join(dw, "ic_launcher_foreground.png"))

        # --- legacy: opaque navy plate + content, then masked to shape ---
        plate = centred(legacy, content, LEGACY_CONTENT_RATIO, plate=(*navy, 255))
        for name, kind in (("ic_launcher.png", "squircle"),
                           ("ic_launcher_round.png", "circle")):
            shaped = plate.copy()
            # Multiply the plate's alpha by the shape so corners become truly
            # transparent instead of being painted over.
            shaped.putalpha(shape_mask(legacy, kind))
            shaped.save(os.path.join(mm, name))

        # Remove the orphaned foreground copies an earlier pass left in mipmap/:
        # nothing references them and they only cause confusion.
        stale = os.path.join(mm, "ic_launcher_foreground.png")
        if os.path.exists(stale):
            os.remove(stale)

        print(f"  {dpi:8s} adaptive={adaptive:3d}px legacy={legacy:3d}px")

    print("done")


if __name__ == "__main__":
    main()
