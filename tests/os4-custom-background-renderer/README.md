# OS4 custom media background renderer

This suite compiles the complete new renderer. The shared OS3 processors, factory,
HostExecutor, and test13 AmbientColorResolver are not edited.

Run from the project root:

```sh
python3 source/tests/os4-custom-background-renderer/run.py --output <new-directory> --negative-controls
python3 source/tests/os4-custom-background-renderer/compile_android.py --output <new-directory>
python3 source/tests/os4-custom-background-renderer/extract_rom.py --output <new-directory>
```

The host fixture supplies Android graphics and target palette boundary adapters. It
checks exact dependency shapes, all four palette indices, per-request options,
private immutable output, cross-user arguments, interruption, GPU error results,
partial acquisition cleanup, and continued cleanup after one close throws. It does
not execute Android graphics. Source mutation controls must compile and fail real
assertions; compiler errors are not accepted as a witness.

The separate Android probe contains complete original OTA DEX classes as private
analysis assets, not a redistributable module. It runs production Kotlin with real
API37 Bitmap/Canvas/HardwareRenderer/WallpaperColors and original ColorScheme,
MediaData and NotificationUtil. Build output stays under `_local-only`:

```sh
python3 source/tests/os4-custom-background-renderer/android/build.py --output validation/_local-only/<new-build>
python3 source/tests/os4-custom-background-renderer/android/with_hidden_policy.py --build validation/_local-only/<new-build> --output <new-evidence-directory>
```

Only run the latter on the dedicated `hyperceiler_os4_api37` emulator at 5580 after
coordinating foreground ownership. The wrapper temporarily permits hidden framework
API access for the ordinary app probe and restores/readbacks the original value in
finally. This is not SystemUI/LSPosed permission evidence. Fancy construction is
bypassed only to give its actual read-only quiet getter a static image; no live MAML
renderer is created or exercised.

## Rendering contract

`create(loader, selectedStyles)` accepts a nonempty subset of 1..4 and validates
`MediaData.artwork`, `NotificationUtil.acquireApplicationIcon(Context,MediaData)`,
`ColorScheme(WallpaperColors,boolean,int)` and all four final palette fields plus
`TonalPalette.allShades`. `render` is background-only, exact receiver, stateless,
and takes separate target options. It has no preference, holder or cache globals.

* Art: accent1 indices 2/8, five rotated/reflected mosaic tiles, brightness
  correction, translucent tint and 40px blur.
* Blurred cover: neutral1[1], neutral2[3], accent2[9], accent1[9], radial mask,
  configured percent of target height, then a square background. Zero bypasses
  the GPU blur entirely; it still applies the intended radial color mask.
* Radial: the same four palette indices, centered square with palette padding;
  the presentation owns its radial gradient.
* Linear: accent1[2]/[8], or [8]/[3] when this target's inverse flag is set and
  sampled brightness is at least 192; square center crop and right-aligned
  presentation gradient.

All palettes come from actual integer CONTENT style 6 with dark scheme true and
require all 13 values to be Integers. Brightness samples the whole image at 5-pixel
steps using the actual sample count, correcting the old left-strip/divisor bias.
The four styles retain their user-facing operations, not byte-for-byte random
mosaic or uninitialized/default pixel equivalence.

Artwork is drawn into a private software snapshot, preserving aspect ratio within
1024 pixels per dimension. Dimensionless static drawables use 112x112. Hardware
sources are scaled then copied to software without recycling borrowed pixels.
Only proven static framework leaf states are cloned; exact MAML quiet/static
getters are read before cloning and no shared Fancy RendererCore is ticked or
registered. Static quiet content is not the animated frame. Unsupported/missing
artwork tries the real cross-user application icon method; unavailable inputs
return null, not transparent success. Valid transparent input can succeed.

`Frame` publishes one immutable background bitmap and immutable color values.
Every holder receives a different Drawable, while immutable pixels can be shared.
The original album view remains managed by the OEM bind. Task intermediates are
recycled; published pixels follow ordinary GC as long as any presentation holds
them. GPU image/buffer/renderer/node/reader cleanup is attempted independently on
all success/error exits. AOSP ImageReader.close owns Surface release; the exact
reference and vendor/framework boundary are in framework-close evidence.

A session should render once and distribute the same Frame to both DI holders so
random Art mosaics match and rendering work is not duplicated. Each holder keeps
its own bounds and animation lifetime. The lifecycle implementation and new
presentation drawable have separate authors and tests.

All physical phone feature/injection/reboot/stability states remain NOT_TESTED.
Historical compile failures and the pre-removal extra album-copy Android probe
remain visible; only the final explicitly bound reports describe current source.
