# Back-region height and icon-scale startup regression

Run `python3 tests/back-gesture-icon-scale/run.py` with a C++20 compiler.

The harness extracts the shipping worker's periodic refresh block and its slot
indices/types. Image discovery, preference transport and individual binders are
host boundaries; the hook lifecycle uses the production `nativehook/hook_bank.h`.
It starts with only Rust back-region hooks, then makes Dart available after the
early loader callback has been missed. The pre-fix worker never retries Dart
discovery and leaves icon rendering inactive.

Coverage includes default/custom heights and widths, three icon scale codes,
startup with/without native slots, unavailable preferences, delayed symbol-index
availability, preserved Rust image metadata, repeated height updates and absence
of duplicate hook installations or repeated successful image scans.

Run the existing independent-object-scale and runtime-layout-contracts suites
as well for actual dimension/allocation parsers and callbacks. This host test
does not establish device animation, GC or real touch-region behavior.
