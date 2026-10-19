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

Tapped-icon return and widget touch/drag testing remain outside this device
check. The offline tests do not exercise Dart GC.
