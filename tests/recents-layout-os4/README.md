# OS4 recents layout

The settings covered here start at card corner radius and include header distance,
header horizontal inset, animation duration ratio, tablet card center height,
horizontal portrait/landscape and vertical card scale, title font size/color and
freeform card background color. The OS4 layout screen removes the old search-bar
category; earlier OS pages retain it and saved choices are not erased.

`home_recent_feature_resolver.h` inventories only executable PT_LOAD words. No
private launcher symbol, debug section, saved RVA, saved pool displacement or
saved object/frame field participates in target selection. The current ARM64
Dart code-generator register/opcode contracts are deliberately narrow; this does
not promise universal firmware compatibility. Padding's FP register differs
between 7751 and 8583 and is itself decoded.

Admission proves the four-way card-width dispatcher, common dynamic rectangle
ratio calculator, real fixed/dynamic scale exits, rounded-corner factory and its
thumbnail/animation consumers, scalar animation corner sources, both header
style paths, header metrics and its device-dependent padding publication,
header-height boxing and linked inlined consumers, tablet dimension consumers,
the nullable freeform/theme getter, the three live inlined title Text stores, actual outer header Insets,
and the current freeform-mode background-to-clipped-container path. Color writes are additionally checked
against the actual A/R/G/B getter and byte-packing consumer chain. Style/Color
class tags, sizes, fields, pool roles and nursery offsets come from those actual
producers, consumers and allocators. Missing or ambiguous core graphs decline
the bank and retain OEM behavior. No Dart allocator or GC call/return PC is patched.

The shipping native callback captures one immutable 11-value preference revision
per call. Style and color replacements allocate only from the proved bounded
nursery fast path, admit both 0 and 8 modulo 16 tops, never retain a movable Dart
pointer, and never call the Dart GC slow allocator. Shared const styles/colors
are not modified. An insufficient nursery retains the original style/background
and emits a rate-limited reason through `HyperCeiler.HomeLayout` (imported into
the built-in log). The non-null/transparent background arm stays original.

The bank enables only after all continuations are ready. INFO diagnostics report
requests, admission, per-site actual hits/applies and readiness. This is distinct
from claiming a screenshot or interaction was verified. Resetting settings makes
retained hooks transparent. The recents-page animation slider composes with the
existing animation-page ratio through its already executable-discovered Dart
producer; its original 10..500 range is retained, without changing HCS1's fixed
188-byte authorization reply or any Rust initialization protocol.

HCRC v1 is an optional 52-byte HCL4 tail: magic, count 11, then the values in
`RecentConfig::Index` order. The provider schema advances to 3. The writer and
reader reject partial/wrong-count/out-of-range packets, while old endpoints
produce neutral defaults. Cache v16 includes the same extension; v15/v14 remain
readable. The endpoint revision/capability diagnostic identifies an old running
system_server after APK replacement. A restart of the launcher alone cannot
load a new system_server class.

Run from the repository root:

```sh
python3 tests/recents-layout-os4/run.py
# Optional captured complete images, independently SHA-verified against fixtures:
python3 tests/recents-layout-os4/run.py --image7751 /path/to/7751.so --image8583 /path/to/8583.so
/path/to/python-with-unicorn tests/recents-layout-os4/verify_arm.py /path/to/android-ndk
python3 tests/dock-title-os4/run.py
python3 tests/independent-object-scale/run.py
```

Native regression executes 26 (7751) / 24 (8583) banks' callbacks and clone/allocation policies on
both captured image graphs. It also removes symbols/sections/non-code data,
relocates functions and their interior allocator tails independently, shifts
frame locals, rejects ambiguous/missing core graphs, checks foreign scale
callers and covers nursery alignment/space/defaults. JVM regression uses the
shipping provider/endpoint/writer, including concurrent publication/queries,
defensive snapshots, old constructors and reset/range behavior. The cache and
wire regression compiles the shipping serializer/parser. ARM64 emulation executes
the production gateway, including native clobbers, terminal and original routes,
all GPR/SIMD registers, SP alignment, NZCV and FPCR/FPSR. It does not emulate GC,
real Android input, rendering, or an actual inline-hook backend.

Device validation (2026-10-24, Xiaomi 15 / OS4, launcher 8583): the Release was
installed and its on-device APK hash matched the signed build. The new endpoint
revision and capabilities loaded after reboot. Visual comparisons confirmed card
radius, portrait horizontal and vertical card scale, header height and horizontal position, title size
and color, and freeform background color. Live execution exposed three omissions:
the standalone title builder and legacy background getter were bypassed, and
the first horizontal-position candidate changed inner metrics without moving
the title. The bank now covers actual inlined title stores, the freeform
background producer and outer left/right Insets consumers. Screenshots and
per-site applied counts verified all three corrections.
The freeform-only predicate preserves ordinary cards. The animation hook's hits
increased and its published ratio was read as 1.8 for a 180% request; this was not
a frame-by-frame timing measurement. Built-in Room logs contained INFO execution
records, including the new background site's applied count. The resume-time
preferences were restored exactly, rotation restored to `lock 0` / `default`,
temporary freeform closed, and the debug preference writer disabled.

Tablet height is not device-verified. Forcing this phone's display orientation
produced an invalid mixed-orientation scene; the landscape bank executed, but
this does not count as a successful visual landscape test. Real landscape and
tablet checks, and exhaustive animation timing/transition checks, remain needed. After a launcher
restart, wait for bank readiness and re-enter recents before comparing a cached
header; an early first frame is not sufficient evidence of a rendering failure.
