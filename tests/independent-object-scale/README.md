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

The latter two cover 44 cases including distinct small/large factors, unrelated
callers, gates, SIMD/flags, original frame replay and native ABI preservation.
The host checks verify old/missing config, separate revisions, reset/range guards,
close ratios matching scaled icon/preview dimensions (including selected-icon Rect
correction), and the widget wrapper's header, fields, bounds and nursery fallback.

Widget scaling wraps the completed widget container with the launcher's own
HyperTransform; cell counts and occupied rectangles remain unchanged. It uses only
the nursery fast path, performs no Dart call or GC and leaves the original widget
on allocation refusal. Folder captions retain their independent font-size setting.

Device validation still needs different icon/folder/widget scales, small/large
folder close/reopen (including a tapped icon), and widget touch/drag testing. These
offline checks do not establish visual results or exercise Dart GC.
