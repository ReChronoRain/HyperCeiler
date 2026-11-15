Folder icon scaling regression
=============================

`launcher-7751.json` contains original ARM64 instruction words from launcher
RELEASE-8.01.02.7751 (801027751), covering small/large size producers, the shared
padding calculator and large-folder preview consumers. No runtime addresses are
used by production: fields and caller identities are resolved from the loaded image.

Run with Python packages `capstone` and `unicorn`, a host C++ compiler and an NDK:

```sh
python3 tests/folder-icon-scale/verify.py --ndk /path/to/android-ndk
```

The resolver checks real instruction bodies, malformed stores/callees, relocated
fields and ambiguous producers. Unicorn executes the production assembly bank and
the original padding-calculator prologue in 24 cases: disabled/enabled, small/large/
unrelated callers, distinct independent factors, shrink/default/enlarge, all scalar fields, neighboring memory,
GPRs, SIMD registers and flags. This does not simulate Dart GC or verify device UI.

The split width-accessor path is covered separately:

```sh
python tests/folder-icon-scale/run_small_runtime.py
```

Set `CXX` to a C++20 compiler command when necessary. The anonymous executable
fixture `small-9239.json` comes from the current device image. Newer code obtains
two background dimensions directly from current config, preview width from an
inlined copy of that config accessor, and preview height from a linked accessor.
The resolver compares the inlined executable prefix, resolved call destinations,
boxed-config dataflow and both final stores. It decodes all geometry fields and
the padding caller from that image. The original three-direct-read path remains
unchanged; missing or ambiguous evidence still rejects the bank.

The suite checks independent function/field relocation, missing callees,
ambiguous producers and 38 helper-instruction corruptions. The legacy folder
producer/consumer and medium-folder suites also passed after this change.

On 2026-11-15, physical settings had small-folder scaling enabled at 140%, and
the launcher had already consumed that request while the previous resolver
rejected its producer. After installing the Release and restarting only the
launcher, the closed folder visibly enlarged. Center and left-side taps (the
latter outside the old background) opened the expected folder. A recorded close
cycle returned to the enlarged resting geometry. This verifies that cycle and
those touch points; it does not establish every boundary, dock folder or every
animation mode. Existing folder scale settings were retained.
