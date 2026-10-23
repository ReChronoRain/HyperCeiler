# Independent folder/widget scaling and close-animation dimensions

The settings page carries three independent enable/percent pairs, defaulting to
`false/100`: small folder, large folder, widget. An optional trailing HCS1 block
preserves the old ordinary-icon wire layout; cache v14 appends the same block and
continues reading v4–v13 with neutral object scales.

`launcher-7751.json` records the original named ARM64 functions from
RELEASE-8.01.02.7751. Production resolves each site, field, allocator tag, instance
size, pool constant and nursery pair from the current image, never these addresses.
The production widget hook now adjusts the launcher's existing outer transform
scalar without another allocation; see `../runtime-feature-addressing/README.md`.
The allocator helpers keep the alignment regression as historical coverage.

Run host configuration, provider, instruction-shape and allocation checks:

```sh
python3 tests/independent-object-scale/run.py
```

With `capstone`/`unicorn` and an Android NDK, execute the production assembly:

```sh
python3 tests/independent-object-scale/verify_arm.py /path/to/android-ndk
python3 tests/folder-icon-scale/verify.py --ndk /path/to/android-ndk
```

The latter two cover 64 cases including distinct small/large factors, unrelated
callers, gates, SIMD/flags, original frame replay and native ABI preservation.
The host checks verify old/missing config, separate revisions, reset/range guards,
close ratios matching scaled icon/preview dimensions (including selected-icon Rect
correction), centered cached origins, and the widget wrapper's header, fields,
bounds and nursery fallback. The cached-origin resolver follows the same icon
fields as `_initScale`, validates both boxed writes and their half-difference
consumers, and rejects missing/changed producers or consumers. The five animation
banks are enabled together so dimensions and origins cannot use different units.

Before the 2026-10-23 migration, widget scaling allocated an additional
HyperTransform around the completed container. The current path adjusts the
launcher's existing outer transform instead. It adds no allocation or GC call;
cell counts, occupied rectangles and independent folder captions retain their
original roles. The allocation regression below records the earlier implementation.

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

The 2026-10-21 runtime diagnostics distinguished `wrapped`, neutral scale, invalid layout/input/
scale, unavailable nursery and unavailable constants. Counts and scalar memory
alignment/room are logged on state changes, capped at 16 records per process.
The existing `LogcatLogLoader`/`LogcatParser` imports `HyperCeiler.HomeLayout` into
the built-in log database on refresh/export, including INFO and WARN records.

## Dynamic addressing audit (2026-10-23)

The object-scale binders now use executable-code feature discovery instead of
named roots or symbol spans. See `../runtime-feature-addressing/README.md` for
full-image tests with symbol/section/string/debug data removed, independently
relocated functions and fields, ambiguity/missing-link rejection and provenance.

| Active object-scale path | Current-image evidence | Contracts / limits |
| --- | --- | --- |
| Ordinary icon, desktop and Dock | Actual width/height consumer graph, config/double fields, matching mixin pair and its normal/Dock producers | Unique executable candidates, bounded frame roles, register ABI |
| Download/progress icon | Typed lifecycle dimensions linked to the canonical icon source and a distinct mixin pair | Original caller/callee relationship; repeated callers share one bank |
| Small/large folders | Common base padding, small producer's four fields, large producer/helpers' 13 fields, actual rectangle-list math/consumer and caller returns | Independent factors; unknown or ambiguous bank declines coherently |
| Folder close and app return | Canonical icon fields, dimension producers, cached half-difference origins and real stable-landing tail caller | All replay slots must validate; ordinary and folder factors stay independent |
| Widget | Actual three-way dispatcher/capture/producers, original closure-entry field, matching Observer consumers and canonical transform allocator/layout | Runtime closure entry + caller selection after either allocator return; bounded nursery wrapper, no VM/GC call relocation |

Only immutable numeric feature contracts are retained per validated image.
Generation reset invalidates them. Full discovery runs on the existing worker,
never in the loader callback; unknown inventories are negatively cached. Widget
runtime logs report `common-observer-wrapper`, observed factor, hits and
applications through the existing built-in-log import path. The 2026-10-23
record below covers the current path; earlier recordings do not validate it.

Other layout/title/tweaks paths still depend on names; their separate migration
status is listed in `../runtime-feature-addressing/AUDIT.md`. This is not a
repository-wide no-symbol claim.

The older `ht_plan.cpp` feature-21 calculator scanner remains in the tree. The
active `make_tweaks_config` does not enable that feature: rendering scalar banks
own ordinary icon scaling. Its boxed-double byte offset7 is a VM ABI contract
relative to a tagged double, not a saved function address.

The parser still requires the verified ARM64 Dart ABI and supported instruction
relationships. An OTA can change those and make an affected feature decline.
Offline tests do not establish universal launcher compatibility or device
appearance, inverse hit testing, phone/tablet behavior and long-run stability.

## Current widget device verification (2026-10-23)

Launcher RELEASE-8.01.02.8583 on OS4.0.0.22, phone 24129PN74C. A real
installation test found that the initial executable-only migration had selected
`_TransformedWidgetContainer.build`, which ordinary widgets bypassed. Hook-ready
status therefore did not imply execution: both the existing step card and a
new calendar card had zero scale hits/applications.

The replacement discovers the actual common dispatcher and its capture/render
producers. The image admits 23 equivalent Observer caller contracts; each final
publication/epilogue bank preserves stock behavior unless the freshly returned
closure entrypoint and the actual caller match the common-widget dispatcher.
The VM allocator remains completely unchanged, so both allocator return paths
reach the same bank. The native wrapper uses the verified nursery fast path
only, including valid 8-mod-16 tops; exhaustion keeps the original Observer.

Both step and calendar cards visibly rendered at 79%, with ordinary/folder
scaling disabled. Their center taps opened `com.mi.health` and
`com.android.calendar`, respectively. After narrowing the hook to caller banks,
the final APK was retested with the existing step card: INFO diagnostics in the
built-in log database reported factor 0.7900, hits=6 and applied=3 (the first
three hits used neutral settings). The final step-card center tap opened
`com.mi.health` again. The temporary calendar was removed; original preference
values/switches were restored, with only explicit 100% defaults added for the
three previously absent independent-scale keys.

Final APK SHA-256, equal on host and device:
`d6e9aa98c09fa1940417b69779b9654e1f3248e84e4e077eed2654b170a7ba0b`.
Release compilation and APK signature verification passed. Two full-image/
executable-only fixture regressions and 90 shipping completion/allocation cases
passed, along with 30 animation and 32 Observer ARM64 cases. The latter check
fast/slow allocator-return register states; they do not run an actual VM GC.
Native Android/third-party widget variants, edge hit testing, dragging/resizing,
phone/tablet differences and prolonged GC pressure remain unverified.
