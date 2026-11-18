# Native Dock policy

Run `python3 run.py` in this directory. The golden producer invokes the original
Java `DockWindowPolicy.layout`, including negative/nonfinite/extreme density,
integer saturation and normal physical layouts. The C++ test executes the real
native policy under ASan/UBSan. This covers geometry parity, not window bounds
acquisition, rendering, motion, ownership or device appearance.


`python3 motion.py` compares the shipping Java DockNativeMotion and
DockRecentsMotion against the C++ policy in home_dock_motion.h, using 200
stateful sessions. It includes stale packets, exact sample replay after overview
authentication, separate unlock samples, NaN/Infinity/negative scales, invalid
density, backwards VSync timestamps, interrupted springs and hard deadlines.
ASan/UBSan run on the real C++ implementation. This validates policy equivalence;
this header is not yet wired to visible native rendering.
