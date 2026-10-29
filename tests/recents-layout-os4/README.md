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

The shipping native callback captures one immutable 13-value preference revision
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
`RecentConfig::Index` order. The provider schema remains 4, header switches use optional cursor extras. The writer and
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


## OS4 header visibility, 2026-10-28

The existing Hide icon switch now targets the Dart header rather than the old
Android TaskViewHeader. An OS4-only Hide text switch independently removes app
names from card headers. Neither switch changes lock badges, card menus or
thumbnail content. Production selection uses executable instructions and linked
constructor/consumer contracts, without private names, saved addresses or CIDs.
The fixed ARM64 Dart code-generator shapes are compatibility bounds, not a
promise to support every launcher version.

The cached icon returns the header owner's original empty widget after
its keep-alive superclass call, unwinding its original managed frame. The
stateless transition icon returns that same current-pool widget at entry. Both
paths must match uniquely before either is published. Title hiding writes the
canonical empty String into the data field of the five already proved fresh
Text constructors. String CIDs/length, Text CID/data, frame and pool offsets
come from the current image's original allocators and stores. No shared const
object is mutated, no Dart call/GC or new allocation is used by visibility-only
requests, and no movable reference is retained. Missing or ambiguous optional
header contracts retain stock headers without disabling numeric card controls.

HCRC v1 stays exactly 52 bytes and HCS1 stays 188 bytes. HCRH v1 appends two
int32 words after the existing infinite-scroll tail, magic 0x48435248 and flags
bit 0 hide icon, bit 1 hide text. Unknown bits and partial packets are rejected.
An absent old-server tail defaults only those two switches off. Provider extras
are version 1 and come from the same getAll snapshot as the unchanged schema-4
columns. Both booleans live in the same immutable 13-value recent array, whose
first 11 elements retain HCRC order. Cache v21 adds the identical final trailer
and continues reading v20 and earlier caches. A new system_server class requires
a system reboot after installing, launcher restart alone cannot add this tail.

```sh
python3 tests/recents-layout-os4/run_header.py
```

Captured 8583 and 8584 executable-only fixtures exercise discovery and rendering
under ASan/UBSan, all four switch combinations, managed-frame returns, original
routes, fresh Text class and empty String validation, zero visibility allocation,
font customization at both heap alignments, relocated functions/calls and pool
slots, ambiguity and missing families. Existing numeric recents regressions,
wire/cache migration, exact schema, immutable/concurrent publication, preference
observer invalidation, absent extras and ARM64 gateway tests also pass.
The cache regression previously assumed a stale tail size and the JVM runner
included ModuleLog twice, both test harness problems were reproduced and fixed.

The initial build matched two standalone icon builders, but real 8584 cards
reuse a list of prebuilt icon Widgets and never executed those builders. The
live path now proves the bounded index-zero/index-one/dynamic-index reads of
that list inside the already title-proved owner. Only last-use child registers
are replaced, leaving the list, spacers, titles and lock children intact. One
window runs before the original heap-base ADD, so it writes a compressed
reference only after validating the current heap base. Its four extra sites
and the standalone pair publish as one optional family.

Device validation on 8584 confirmed all four combinations with matching native
HCRH flags, screenshots and actual live-icon applications. The latest two
switches were both true and were restored true, all other preferences matched
exactly. No exhaustive split-card, tablet, landscape or interrupted-transition
coverage is claimed. Header regression is 55 ASan/UBSan cases per fixture.


## Entering-recents corner continuity, 2026-10-28

Steady thumbnails already used the requested radius, but the Flutter flight
lerp and the real app-window flight had independent stock-20 targets. New
optional discovery links their shared stacked-radius producer back to the
proved width dispatcher, original Radius/BorderRadius allocators, and scalar
Tween allocator. It verifies four physical and four logical Tween end stores
on the app-window side. The requested radius is applied before the original
width fraction and display-density arithmetic, preserving screen-radius begin
values, asymmetric screen corners, scaling, progress and interpolation. No
surface-wide override, shared-object write, Dart call or native allocation is
introduced. A missing or ambiguous family publishes none of its four sites.
The same snapshot feeds these targets and the stable thumbnail radius.

```sh
python3 tests/recents-layout-os4/run_flight.py
```

37 ASan/UBSan cases per 8583/8584 fixture cover plain/stacked target routing,
neutral/zero/max values, original screen-begin registers, stack scale retention,
relocated owners/allocators, moved frame locals and missing/ambiguous/changed
semantic contracts. Numeric regressions now exercise 30 banks/91 cases (7751)
and 28 banks/87 cases (8583); complete 8584 discovery has 34 sites with optional
icons and both flight representations. No fixed address or private symbol is
used by production discovery. The final Release builds and verifies its
signature, device validation details and remaining limits are recorded locally
under root handover/ and /tmp/hc-recent-header-live-20261028.
