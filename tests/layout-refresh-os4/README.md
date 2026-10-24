# OS4 first-render and drawer search regression

The executable-only migration left icon size and Dock margin discovery on the
worker. The loader callback refused that discovery, allowing initial icon widgets
and Hotseat geometry to consume stock values before their scalar banks existed.
Returning from an application subsequently rebuilt that icon with the new bank.

The first-render pass binds the icon/progress dimensions, drawer search and Dock margin
contracts. It publishes the cached configuration before the original layout can
consume it, without Binder, symbol decoding, sleeps or waiting for the worker.
The worker adopts the same slots and shares their lease during maintenance and
late image adoption. General feature discovery stays on the worker.

This fixes the startup ordering, not general Flutter cache invalidation. Existing
screens expose the launcher quick-restart action; changing an already constructed
widget's scalar does not itself schedule a Flutter rebuild. Cache unavailability
still needs the authoritative worker snapshot; the bootstrap never invents one.

Drawer search discovery joins the drawer width producer, its capture, the owning
builder and the closure's final size wrapper. The bank clears the last-use child
stack root with Dart's canonical null (verified ABI register X22). The four
original instructions still copy the original width and nullable child into the
new wrapper. No Dart allocation/call is moved into a native frame, and no heap
pointer is retained. With the switch off, all instructions and stack roots remain
original. The Finder path used on 8584 additionally follows the shared width's bottom-bar
consumer into its search holder and separate color-filter builder. At the holder
entry, the restored managed context tail-branches to the verified stock empty-size
allocator. No native C frame is present when its original allocation or GC slow
path runs. Only the search holder is replaced; the color filter stays original.
Both banks are installed before first layout because the drawer caches its holder
before being opened. This removes the search subtree instead of relying on width
or opacity alone. It does not resize the drawer's independent surrounding padding.

The ordinary layout Binder protocol gains an optional `HCDR, boolean` tail after
`HCRC`; cache version 17 preserves version 16/15/14 defaults. Provider schema 4
and endpoint revision/capability diagnostics identify mixed module generations.
The independent experimental HYOS combined protocol is unchanged and does not
transport this new flag.

Run:

```sh
python3 tests/layout-refresh-os4/run.py --image7751 /path/to/7751/libapp.so --image8583 /path/to/8583/libapp.so --image8584 /path/to/8584/libapp.so
python3 tests/recents-layout-os4/run.py
python3 tests/dock-title-os4/run.py
python3 tests/log-diagnostics/run.py
python3 tests/recents-layout-os4/verify_arm.py /path/to/android/ndk
python3 tests/layout-refresh-os4/verify_factory_arm.py
```

Use Python with Unicorn installed for the ARM gateway test and JDK 21 for JVM
checks. Fixtures contain only executable words from all three images; the test
verifies their provenance when complete images are supplied. Tests cover
independent function relocation, ambiguous/missing links, first-consumer ordering,
failed installation, cached configuration unavailability, last-use child root
writes, register preservation and neutral switch behavior.

Device checks remain: restart launcher after loading the new module; verify all
icons on the initial workspace/Dock without first launching each application;
check minimum/70/maximum Dock margin and switch reset; open the drawer with search
hiding on/off and verify scrolling, keyboard focus, gestures and accessibility.
Compare first-render/config endpoint diagnostics and drawer hits/applied counters
in HyperCeiler's built-in log. Hook installation alone is not visual proof.

## Device verification (2026-10-24)

On Xiaomi 15, OS4.0.0.22, launcher 8584, the final signed Release was installed
non-incrementally and its installed APK SHA-256 matched
`9bb7a96c110da17de3fd4f2178418f41b6a8621dc27fd21efaf76c71273a3eb5`.

- Before opening any application, the initial workspace and Dock used 75% scaling.
  Reading only this module's native globals confirmed factor `0.75`, enabled `1`,
  desktop width hits `1524` and Dock width hits `12`; calendar icon raster width
  changed from 186 px at 100% to 138 px at 75%.
- Dock margin 0/70/110 changed actual icon positions. At unchanged 75% size,
  changing 70 to 110 moved the phone icon upwards by 123 physical pixels.
- The Finder holder was recognized from the 8584 image rather than private names.
  First-render publication reported `drawer=1`; runtime reported `hits=1 applied=1`.
  The first drawer opening omitted search, could scroll, and returned to Home
  without changing the launcher PID. Tapping the previous search location left
  `mInputShown=false`. Turning the switch off restored search and tapping it gave
  `mInputShown=true`.
- The drawer-page switch was present and enabled. HyperCeiler's built-in database
  contained the first-render and drawer-applied records after normal log import.
- All five temporary preference changes were restored exactly (the originally
  absent drawer key was removed), debug preference writing was disabled again,
  and the final workspace was visually checked.

The 7751 and 8583 Finder/legacy paths have image, relocation and ARM emulation
coverage, not separate physical-device verification. Color-filter preservation
is established by targeting only its sibling search holder; active color-filter
UI was not separately tested on the device.


## Dock margin isolation (2026-10-24)

The absolute bottom-margin getter is shared with workspace configuration, so a
process-wide override also moved the desktop search pill. The getter now admits
only runtime-derived return PCs from its rectangle/subtraction consumers and the
unique title/margin aggregator. Origin and center helpers are joined through the
same discovered config/row-height field. Both hit-rectangle branches are admitted.
Unrelated callers execute the original getter and keep its result. No caller RVA,
field offset, private name or section header is used for this admission list.
The original nursery instructions in the aggregator are classification evidence
for the verified Dart ARM64 ABI, not newly executed allocation code.

Both first-render binding and normal binding publish the admission table before
enabling the same scalar bank. The built-in log path receives `dock margin scope`
with admitted caller count, applied count and shared pass-through count. Tables
contain executable addresses only, never moving Dart pointers.

Additional checks:

```sh
python3 tests/runtime-feature-addressing/scalar_run.py --image /path/to/7751/libapp.so
python3 tests/layout-refresh-os4/verify_margin_arm.py /path/to/android/ndk
```

The 64 production-bank ARM cases cover absolute 0/70/110/150, disabled/empty/full
admission, a match in the last slot, shared-call pass-through, original execution,
register/flags/stack preservation and applied/pass-through counters. Native graph
tests include independent relocation, two consumers in one hit rectangle,
workspace exclusion and ambiguous title/margin aggregators. Complete 7751/8583/
8584 executable scans resolve ten admitted return PCs each. These are validated
instruction contracts, not a promise of compatibility with arbitrary future ABIs.

On the connected 8584 device, 70→110 moved the phone icon from Y=2211 to Y=2089
(122 physical pixels), while the unobscured search text remained Y=2155..2186.
Extreme values can intentionally overlap the independent search pill; the slider
does not automatically relocate the pill to avoid that overlap.
