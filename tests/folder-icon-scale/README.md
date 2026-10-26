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
