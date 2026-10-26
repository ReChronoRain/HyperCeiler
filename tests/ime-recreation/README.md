# IME service recreation

Run `python3 tests/ime-recreation/run.py` (JDK 17+ required). The test compiles
the production support installer, input-method config, and third-party package
dispatcher. Only Android services and the hook-registration boundary are replaced.

The stock OS4 `InputMethodBottomManager.onDestroy()` resets `sIsImeSupport` to -1.
This test repeats that lifecycle 100 times with one reused manager class, checks
the support predicate and an entry that reads the field directly, then checks
cleanup, older method/field variants, and bounded recovery diagnostics.
Routing tests simulate an unavailable system context: selected MIUI/AOSP input
methods still install, while disabled/unselected targets stay disabled. Legacy
selection remains supported. Actual ART hook interception and process restarts
require device verification; this fixture does not emulate ART.

On Xiaomi 15 / OS4 Android 17 with WeType 4.0.0, the final Release was checked
through service recreation in the same process (switch away/back) and three
SIGKILL/restart cycles on 2026-10-20. Every case restored the visible MIUI bottom
view and an 876 px input frame matching the 876 px keyboard content; the extra
65 px inset was removed on the first show, with the separate 179 px bottom area
preserved. The same-process case emitted the bounded support-cache recovery
diagnostic. Installed APK SHA-256 matched the local signed artifact:
`207f6008d5cc2431f1509b40ecb27702c9cfe22d9e598302b0de8dbfe47ab528`.
