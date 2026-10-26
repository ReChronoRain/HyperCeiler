# App return into an open folder

`CheckRectHelper.deterministicFolderLandingRect` calls
`FolderClingGetxController.getStableIconRectInFolder`. Its width and height read
the canonical config directly, bypassing the scaled widget/mixin getters. This
made the closing app animation land at the stock size and snap to the rendered
size afterward. Two scalar banks apply the ordinary-icon factor before the
original edit/fold multiplier and centering math. Folder background scaling,
title height, scroll handling, and split-screen transforms keep their own logic.

`python3 tests/folder-app-return-scale/run.py` checks the production resolver
against launcher 7751 instructions, relocated symbols, and changed producer,
caller, and frame layouts. The fixture addresses are test expectations only.
Production matches both lengths to `AppIcon.build`'s actual config fields and
validates the deterministic folder caller before publishing either bank.

`python3 tests/folder-app-return-scale/verify_arm.py` additionally requires the
Android NDK (`ANDROID_NDK_HOME`) and Unicorn/Capstone. It executes the shipping
banks plus the original four displaced instructions, including register/SIMD/
flags preservation and independent edit and icon factors.
The test also models the backend's x17 continuation jump and executes the next
original instruction. The height replay keeps the MOVN x17/indexed store together;
splitting them would use the continuation address as a stack offset. The
`--old-height-window` option is a negative control and must fail with an invalid
memory write.

The final signed Release was installed on Xiaomi 15 / OS4 Android 17, launcher
7751, on 2026-10-20. Recordings covered a 1.25 icon factor with gesture-home return
and a 0.78125 factor with Back return into an open folder. Each execution hit both
new scalar banks and kept the launcher PID stable. Frame inspection showed a
continuous settle into the rendered icon size. Temporary icon-scale/auto-close
preferences and the debug-write property were restored afterward. Tablet and
split-screen returns still need device checks.
