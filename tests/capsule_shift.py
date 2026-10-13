#!/usr/bin/env python3
"""Locate the assistant capsule in two launcher screenshots and report how far it moved.

The capsule is the dark rounded pill just above the dock. In a 1200x2670 frame that is the
bottom band, so the scan is restricted there and looks for rows whose dark-pixel run is wide
enough to be the pill but not the dock itself.
"""
import sys

from PIL import Image

BOTTOM_START = 0.72   # fraction of the frame height where the capsule band begins
DOCK_START = 0.885    # the dock's frosted panel starts here; stop before it
DARK = 110            # luminance threshold for "dark pill pixel"
MIN_RUN = 90          # a horizontal dark run shorter than this is not the pill


def luminance(pixel):
    r, g, b = pixel[0], pixel[1], pixel[2]
    return 0.299 * r + 0.587 * g + 0.114 * b


def capsule_rows(path):
    image = Image.open(path).convert("RGB")
    width, height = image.size
    pixels = image.load()
    rows = []
    start = int(height * BOTTOM_START)
    stop = int(height * DOCK_START)
    for y in range(start, stop):
        run = best = 0
        for x in range(int(width * 0.15), int(width * 0.85)):
            if luminance(pixels[x, y]) < DARK:
                run += 1
                best = max(best, run)
            else:
                run = 0
        rows.append((y, best))
    # The pill is the contiguous band of rows with a wide dark run; take its centre.
    band = [y for y, best in rows if best >= MIN_RUN]
    if not band:
        return None, width, height
    return (band[0], band[-1]), width, height


def main(argv):
    off_path, on_path = argv[1], argv[2]
    off_band, width, height = capsule_rows(off_path)
    on_band, _, _ = capsule_rows(on_path)
    print(f"frame {width}x{height}, dark run >= {MIN_RUN}px in the {BOTTOM_START:.0%}..{DOCK_START:.0%} band")
    print(f"  off (knob disabled) : capsule rows {off_band}")
    print(f"  on  (knob enabled)  : capsule rows {on_band}")
    if off_band and on_band:
        shift = (on_band[0] + on_band[1]) / 2 - (off_band[0] + off_band[1]) / 2
        print(f"  centre shift: {shift:+.1f} px ({shift / 3.25:+.2f} dp)")
        print("  RESULT: " + ("MOVED" if abs(shift) > 4 else "NO VISIBLE MOVE"))
    else:
        print("  RESULT: capsule not found in at least one frame")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
