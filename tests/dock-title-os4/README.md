# OS4 Dock app titles

Run `python3 tests/dock-title-os4/run.py` from any directory. Requires a C++20
compiler (`CXX`, defaults to clang++) and JDK 17+ (`javac`/`java` on PATH).

The checked-in instruction fixture is from Xiaomi Home
`RELEASE-8.01.02.7751-260904-10100945-R` (versionCode 801027751).
It contains the two text builders, their actual hotseat getters, and the DockScope
semantic root. Production resolves symbols and operands from the installed image;
the fixture addresses are test expectations, not shipping patch addresses.

Coverage:

- Ordinary and downloading app titles, relocated code, explicit hide-title guard,
  all combinations of hotseat/DockScope and ordinary workspace rendering.
- Reject changed opcodes/registers, branch destinations, or hotseat semantics.
- Optional Binder extension; current cache round trip, truncation/corruption, and
  v13/v14 defaults with existing widget/folder settings preserved.
- Production provider and authenticated endpoint, on/off propagation, diagnostic
  summary, and backward-compatible Snapshot constructor.

The ARM64 splice test additionally requires the Android NDK (`ANDROID_NDK_HOME`)
and a Python environment with Unicorn and Capstone:
`python3 tests/dock-title-os4/check_model_arm64.py`.
It executes the shipping assembly for on/off and all five hotseat IDs, checks
instruction replay, frame-local selection, register/NZCV preservation, and that
controller/model data remain unchanged.

Xiaomi 15 / launcher 7751 was checked on-device on 2026-10-20: the four Dock app
titles appear when enabled, disappear after disable/restart, and reappear after
re-enabling. The glass background remains visible. Download-in-progress icons
and OS4 tablet variants have host coverage and still need device checks.
