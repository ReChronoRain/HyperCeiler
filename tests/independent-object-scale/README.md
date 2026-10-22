# Independent folder/widget scaling and close-animation dimensions

The settings page carries three independent enable/percent pairs, defaulting to
`false/100`: small folder, large folder, widget. An optional trailing HCS1 block
preserves the old ordinary-icon wire layout; cache v14 appends the same block and
continues reading v4–v13 with neutral object scales.

`launcher-7751.json` records the original named ARM64 functions from
RELEASE-8.01.02.7751. Production resolves each site, field, allocator tag, instance
size, pool constant and nursery pair from the current image, never these addresses.

Run host configuration, provider, instruction-shape and allocation checks:

```sh
python3 tests/independent-object-scale/run.py
```

With `capstone`/`unicorn` and an Android NDK, execute the production assembly:

```sh
python3 tests/independent-object-scale/verify_arm.py /path/to/android-ndk
python3 tests/folder-icon-scale/verify.py --ndk /path/to/android-ndk
```

The latter two cover 56 cases including distinct small/large factors, unrelated
callers, gates, SIMD/flags, original frame replay and native ABI preservation.
The host checks verify old/missing config, separate revisions, reset/range guards,
close ratios matching scaled icon/preview dimensions (including selected-icon Rect
correction), centered cached origins, and the widget wrapper's header, fields,
bounds and nursery fallback. The cached-origin resolver follows the same icon
fields as `_initScale`, validates both boxed writes and their half-difference
consumers, and rejects missing/changed producers or consumers. The five animation
banks are enabled together so dimensions and origins cannot use different units.

Widget scaling wraps the completed widget container with the launcher's own
HyperTransform; cell counts and occupied rectangles remain unchanged. It uses only
the nursery fast path, performs no Dart call or GC and leaves the original widget
on allocation refusal. Folder captions retain their independent font-size setting.

On launcher 7751, the cached-origin fix was built as a signed release, installed
non-incrementally and loaded by restarting the launcher. The installed APK hash
matched `a4f0c91bae60243ddd64fe7cf6afc9f0c8a750555067ee3dd7d9a4757600f3e4`.
Native diagnostics confirmed `dimensions=3 cached_origin=2` and active hooks.
Recordings covered 79% ordinary icons, a small folder at 100% and 80%, and a large
folder at 80%, with Back and outside-tap close paths. Folder strategy caches were
rebuilt by restarting after changing the independent scales. Frame inspection
confirmed the former roughly 6 px diagonal end jump was removed. All five
temporary preferences and the debug-write property were restored, followed by a
launcher restart to restore the original rendered geometry.

That earlier device check did not cover widget scaling. The follow-up below
records its actual failure and verification. The offline tests do not exercise Dart GC.

## 2026-10-21 widget allocation regression

On launcher 7751 the setting reached native code and `WidgetContainerWidget.build`
was hooked, but the wrapper returned the original widget on every call. The
guard incorrectly required a 16-byte-aligned nursery top. Dart's valid new-space
top was `8 mod 16`; native stack alignment is a different contract. The check now
requires word alignment, as the existing color-clone path does. Space exhaustion
still returns the original object without a GC-capable Dart call from a C frame.

The host allocation test now exercises both alignments, 60/70/100/125/140 percent,
misaligned tops, exact-fit/exhausted space, unchanged surrounding memory, and all
constructed fields. Widget resolver tests relocate the image by 2 MiB and change
field, frame and constant-pool operands. The folder-return suite also changes
the ordinary icon's config/double fields and relocates its owning function.
A temporary header copy with the old `top & 15` guard fails the new allocation
assertion, confirming this regression test detects the original bug.

Device verification on October 21–22 used a temporary calendar date card: 70%
visibly shrank it, 125% enlarged it around its center, and tapping the enlarged
card opened `com.android.calendar.AllInOneActivity`. Disabling the switch restored
its original geometry. Before the fix, diagnostics reported
`factor=0.7000 state=nursery-unavailable hits=1 applied=0`; after the fix they
reported `factor=0.7000 state=wrapped hits=1 applied=1 top_align=8` and then
`factor=1.2500 state=wrapped hits=1 applied=1 top_align=8` after a launcher restart.
Existing widgets need a rebuild (the device check restarted the launcher) to
pick up a new wrapper factor; swiping between cached pages alone is insufficient.
The temporary card was removed and both tested preference keys restored exactly;
the preference debug-write gate was turned off. A native Android widget sample
could not be added through the picker in this run, so it is not counted as
device-tested. The common container/transform path was audited in code.

The final Release was built, signature-verified, installed non-incrementally and
the installed APK SHA-256 matched
`48cf9c986cf03b30c9d38639d49802735dfe406a9d6e383932881d5f5c329b8b`.
Production assembly checks passed: independent animation/widget 32 cases,
small/large folder 24, app return 72, ordinary icon 108 and progress icon 72.
The older icon harness was run with host tool/output paths adapted in memory and
its renamed scale symbol mapped to the current `hc_icon_scale_bits`; its replay
and GC limitations still apply. Logcat parser/database import checks also passed.

Runtime diagnostics distinguish `wrapped`, neutral scale, invalid layout/input/
scale, unavailable nursery and unavailable constants. Counts and scalar memory
alignment/room are logged on state changes, capped at 16 records per process.
The existing `LogcatLogLoader`/`LogcatParser` imports `HyperCeiler.HomeLayout` into
the built-in log database on refresh/export, including INFO and WARN records.

## Dynamic addressing audit

| Active scale path | Derived from the current launcher image | Deliberate contracts / limits |
| --- | --- | --- |
| Ordinary icon, desktop and Dock | Named `AppIcon.build`/`GridController.currentConfig`, actual width/height inputs and config/double fields; `ShortcutIconWidget` typed getters identify the correct mixin copy and its normal/Dock branches | Unique instruction shapes, register ABI, bounded complete function bodies |
| Download/progress icon | Typed `ProgressShortcutIconGetxController.onInit` callee chain, linked back to the canonical icon config fields | Bounded chain, original prologue/epilogue and scalar bank ABI |
| Small folder | `FolderLayout1x1.calItemSize` and base strategy; four background/preview fields, current-config and preview-height producers, caller return | Validated matching stores/calls; independent small-folder factor |
| Large folder | `BigFolderCommonStrategy.calItemSize`, width/height helpers and preview consumer; 13 geometry fields and caller return | Validated producer/consumer relationship; independent large-folder factor |
| Folder close and app return | `_initScale`, `_refreshCachedGridParams`, canonical icon fields and verified app-return caller; source dimensions and cached center origins | Instruction windows/frames must match; ordinary icon and folder factors keep their separate roles |
| Widget | `WidgetContainerWidget.build` plus the launcher's default `HyperTransform` construction; child/scalar/reference fields, frame, allocator tag/size, pool constants and nursery load/store offsets | Dart compressed references, `+1` object tag, 8-byte words, tag size encoding, canonical false relation and register ABI; fast allocation only |

No active scale path uses a remembered absolute function address or a table of
per-version field offsets. Runtime VAs are added to the loaded image's base for
ASLR. Symbol caches are keyed by image identity and registry identity, then
revalidated for executable ownership, span, and current function shape; cache
hits do not bypass each scale binder's instruction/field validation.

The older `ht_plan.cpp` feature-21 calculator scanner remains in the tree, with
boxed-double payload byte offset `7` as a VM ABI contract. The active
`make_tweaks_config` deliberately does **not** enable that feature: rendering
scalar banks own ordinary icon scaling, avoiding double scaling. The byte offset
is relative to a tagged Dart double, not a fixed launcher function address.

Thus addresses and image-specific fields are dynamically resolved, but the code
is not independent of the Dart VM/compiler ABI. An OTA that changes symbols,
registers or instruction shapes can make a binder refuse its hook and report the
reason. Relocation/operand-mutation tests and this device check do not prove
compatibility with every launcher build or exercise a Dart GC slow path.
