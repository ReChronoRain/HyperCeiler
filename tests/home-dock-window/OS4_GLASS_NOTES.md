# HyperOS 4 native glass investigation

Status (2026-09-04): the user confirmed that the WMS Dock background is visible.
The existing blur mode still uses compositor blur. An experimental, separate
native-glass mode is implemented but **not yet visually verified on the desktop**.
The working effect/tint layers remain the fallback.

## Read-only reference files

With the user's explicit permission, only these two additional device files were
exported for local analysis. Do not check the proprietary binaries into Git.

| File | SHA-256 |
| --- | --- |
| `/system/framework/framework.jar` | `4f24636042ed6661e62660f0724f5c3a22b7e5f2fe73750593f2dcfc77fba613` |
| `/system/lib64/libhwui.so` | `4ddaf2217cb36127427052e95aa6e333d8d5e135ef234091554ccfd037dc1670` |

These are build-specific observations, not a stable Xiaomi API contract.

## Confirmed call path and gating

- `View.setMiGlass(float[])` checks `ViewRootImpl.getSupportedBionicMaterial()`,
  then calls `RenderNode.setMiGlass(float[])` and invalidates the View on change.
- The capability is initialized from `persist.sys.bionic_material_supported`.
  Do not force that property or substitute API presence for capability support.
- `RenderNode.nSetMiGlass(long, float[])` JNI registration is at file offset
  `0xe5cd70` in this libhwui; the function address is `0x984210`.
- The native function requires exactly **42 floats** (`cmp w4, #0x2a` at
  `0x984294`). A successful call alone does not prove that glass was rendered.
- `BackgroundBlurBlendProperties::setGlassFilterParam` at `0xa32034` compares
  the first `0xa8` bytes. Its internal struct also contains trailing geometry
  and state: do not pass its full native size as the Java array length.
- `SurfaceControl.Transaction` exposes blur, blend and stroke methods on this
  phone, but the API inventory found no equivalent direct glass setter.

## Partial native field mapping

The shader setup near `0xa45d14` loads the following fields from the glass
parameter struct into named uniforms. This is useful ABI evidence, **not a
complete validated preset**. In particular, mode values, supported ranges and
the full producer-to-renderer path still need a real framework client example.

| Struct float index (zero-based) | Uniform destination |
| --- | --- |
| 11–14 | `uGlassColor` |
| 15, 16, 33, 34 | `uInClrWhiteMix_bgClrSatBri` |
| 18, 19, 21, 22 | `uAlpha_shapeEdgeThickReflect` |
| 32, 24, 23, 17 | `uRefractIORStrengthLight_clrpow` |
| 8, 9, 7, constant zero | `uDarkRangeDark_hit` |
| 28, 29, 10, 4 | `uDirLightIntensityOpp_inBottom_lumiAmount` |

Do not guess the remaining entries or test speculative native parameters inside
system_server. Index 19 is the shape-edge field, **not alpha**; its use as a
validity threshold in the string formatter does not make it an enable flag.

## Cross-window background is a separate requirement

`View.setPassWindowBlurEnabled(true)` checks MiBlur support and a package filter
before registering the View with `ViewRootImpl.addTextureView`. The root then
uses `SetPassBlurSurface` and `setUpdateTextureFlag` on its own SurfaceControl to
request the background texture. It can return false when the View is unattached,
when unsupported/filtered, or when the requested state is already set.

A windowless HWUI host in the HyperCeiler process is a possible design, not yet
proven. It needs verification of background-texture delivery, correct surface
position/visibility, lifecycle cleanup and input transparency. Do not claim that
calling `setMiGlass` on a detached View adapts the native Flutter launcher.

## Next required evidence

The user subsequently authorized a targeted, read-only SystemUI APK export:
`/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk`, SHA-256
`22afac555f2df2a33749083fb2093393835cf7dbdeb3f27595c2d07bb88b5ff0`.
No SystemUI private data, settings or running process was modified.

`HeadsUpNotificationGlassEffect.apply` and its `GlassDarkEffect` counterpart
contain complete 42-float presets and call `setMiGlassCompat` followed by
`setMiViewMaterialTypeCompat(1, view)`. Clear uses 42 zero floats and material
type 0. Their preceding element-blur setup enables View blur mode 1. These
numeric presets are used by `DockGlassPreset`; they are notification glass
presets, not a claim of exact equivalence to the launcher's Flutter preset.

The app-side host additionally needs its own container/background blur and
pass-window texture setup because it has no SystemUI parent supplying those.
That cross-window path and the final rendering remain device-verification items.
The cached old Miuix dependency is not replaced, and SystemUI code is not loaded
into either the app or system_server.

## Device smoke-test result

The first app-only self-test failed with the HyperCeiler-process log:
`enablePassWindowBlur not set, because com.sevtinge.hyperceiler not allow!`.
Both capability getters returned true. `DockGlassHost` now extends only its own
windowless ViewRoot's instance field `mPassWindowBlurFilterData` with its own
package name when necessary. No static filter flag, persistent/global setting,
system property or other app's ViewRoot is changed.

After rebuilding and reinstalling HyperCeiler with data preserved, the fixed
unparented self-test returned `frameCommitted=true`. Host geometry and preset
tests passed, and APK v2 signature verification passed. This is not a screenshot
or a visual comparison, and does not test wallpaper delivery. The phone has not
been rebooted to activate the new WMS hook; reboot permission and desktop
verification remain outstanding. The existing blur selection was not changed.

## White/opaque appearance report

The user reported a white, opaque-looking Dock. ADB was disconnected, so whether
that observation was the native renderer or its compositor fallback is unknown.
Local shader inspection confirms `uGlassColor.a` interpolates toward tint RGB
(the light notification preset used 1.5), and background saturation 0 removes
wallpaper chroma. Dock-specific tuning now uses tint mixing 0.06 light / 0.10
dark, inner-color mixing 0.10, and background saturation 1. Output alpha and
refraction parameters remain unchanged. Glass fallback tint alpha is 0x18;
ordinary Blur mode retains its existing 0x66 tint. These adjustments still need
device/visual validation and do not prove background texture delivery is correct.

## Superseding preset: match Control Center

The user subsequently requested matching the pull-down Control Center, not a
hand-tuned notification preset. The relevant read-only reference is
`/product/app/MIUISystemUIPlugin/MIUISystemUIPlugin.apk`, SHA-256
`f07be6a32708b0205d5ecc91ed8ce1a38fd64a17de3d49dd88f6306f9d2dab99`.
No plugin private data or control-center settings were accessed or changed.

Verified producer/call chain:

1. `ControlCenterMaterialTokens$defaultContentBgMaterialToken$2` and
   `$mediaItemToken$2` obtain `MaterialTokenKt.getDefaultBionicsStyle()`.
2. That style uses `MiBackgroundStyle.DEFAULT_GLASS_TOKEN`, not
   `ACTIVATED_GLASS_TOKEN` (colored/active controls) or notification-row presets.
3. `BionicsToken$toBionicsParams$2` writes token fields to indices 0–36 of a
   copied 42-float default array; the remaining five entries are zero.
4. `BionicsStyle.apply` sets View blur mode 1. The bionics branch of
   `setMiBackgroundStyle` sets material type 1, then calls `setMiGlass`.
5. `setBackgroundBlurRadius$default` supplies small/big glass radii of 110/110.
6. `CCMaterialToken` clears the solid background. Where supported, its
   `ControlCenterUtils.updateBlurEnhanceClip` disables normal outline clipping
   and enables glass enhancement through flags/mask `0x2000`.

`DockGlassPreset.parameters` now returns the exact default Control Center array
for both day and night; that token has no day/night branch. The earlier
notification presets and white-tint tuning are superseded, not layered on top.
The host uses a transparent background, radii 110/110, corresponding material
setup order and optional native clipping enhancement. Unsupported enhancement
retains normal rounded-outline clipping. The overall Dock geometry and
compositor fallback remain independently managed by the WMS hook.

The regression test checks all 42 float bit patterns with `Arrays.hashCode ==
-1053074018`, independently calculated from the APK's default array payload,
as well as identical day/night tokens and blur/enhancement constants.

This aligns the **default neutral Control Center card material**, not active
buttons, pressed lighting animations, sliders, or the full-screen panel backdrop.
The Dock has different geometry and wallpaper input; matching parameters alone
does not establish pixel-identical rendering. On-device visual confirmation and
native background-texture delivery verification remain required.

The Control Center preset build was compiled, APK-v2 verified and installed with
`adb install -r`. The app-only smoke test returned `frameCommitted=true`; the
HyperCeiler PID log recorded a native glass frame commit without an enhancement
API fallback or AndroidRuntime error in the captured log. This still does not
verify the actual Dock's background texture or visual equivalence. Recreate the
Dock host by toggling its background off/on to pick up the new app-side renderer;
no additional device reboot was performed for this preset update.

## Gaussian-looking result: rendering-path investigation

After a user-triggered Dock toggle, HyperCeiler logged an actual (non-self-test)
host frame commit at 14:35:23.583. Thus the app-side renderer was reached; absence
of earlier host logs was not evidence of a permanently missing hook. The user
still reported Gaussian-looking output. At that point the logs did not expose
the actual host's background-texture timestamp, so fallback was not established.

The host is being tested with separate HWUI roles: a FrameLayout source
container owns pass-window background, container blur mode 1 and dual blur
radii; its child owns element blur mode 1 and the Control Center glass material.
Previously one View had both roles. This is a rendering-path hypothesis under
test, not a visually confirmed root cause. The exact Control Center preset is
unchanged in this experiment.

`content call --uri content://com.sevtinge.hyperceiler.provider.sharedprefs
--method dock_glass_diagnostics` is a read-only, app/system/ADB-shell-only
diagnostic. It reports this app process's host count, Dock create count,
last status/release and timestamps (never background pixels). It allows a real
host to be distinguished from an unparented smoke test and records missing
textures without modifying the global system log level or other applications.

After the next user-confirmed toggle, diagnostics from HyperCeiler PID 6398
reported `pipeline=container+glassChild`, one Dock create request, one active
host, no release, and `backgroundReady=true` with texture timestamp
`2008074045325`. This verifies background-texture delivery to a real Dock host
in the separated-role build, not just the offscreen smoke test. It does not
independently verify the system hook's final compositor state or visible glass
refraction; visual confirmation is still pending. The scoped process log read
returned no entries, so it provides no additional rendering evidence.

## Superseding preset: match the folder icon glass

The user's current target is the soft glass the launcher draws behind a folder
**icon**. This supersedes the Control Center card of the section above; it is not
the expanded folder panel either. The producer is
`FolderBlurUtils.buildFolderGlass` (libapp.so, offline sample only; Dart object
offsets are never runtime addresses).

Producer/call chain confirmed offline:

1. `FolderBlurUtils.buildFolderGlass` reads `WallpaperGetxController`
   (`safeAppliedLightWallpaper`), cross-checked against
   `LauncherStateManager._hasAppliedLightWallpaper`.
2. A light wallpaper selects `Glass_Common_Medium_Thin_High`; otherwise
   `Glass_Common_Medium_Thin_Low`. **This is wallpaper brightness, not
   `Configuration.UI_MODE_NIGHT_YES`.**
3. `WallpaperUtils.hasAppliedLightWallpaper` reads the wallpaper colour hints and
   tests the low bit (`& 1 == 1`), i.e. "supports dark text".
4. The token carries its own dual blur radii (36/500) and 42 floats.

`DockGlassPreset` now carries the two folder tokens and radii.
`DockGlassHost.applyMaterial` resolves the brightness on every (re)apply by
reading `WallpaperManager.getWallpaperColors(FLAG_SYSTEM).getColorHints() & 1`,
so the material follows a wallpaper change instead of being frozen at host
creation. `probe()` reports "not ready" while the resolved bit differs from the
one the live material was built from, which routes the launcher through the
existing `refresh()` path. `entry.dark` is retained only for the fallback tint
and diagnostics; it no longer selects the glass.

The regression test pins `Arrays.hashCode` `-616200191` (Medium_Thin_Low) and
`-1650047496` (Medium_Thin_High), the shared field groups, and the
wallpaper-branched refractive index and background field.

Open, unverified items: the Flutter `ImageFilter` sigma values and the HWUI
`setMiGlassBlurRadius` unit are **not** proven to be the same blur unit, so 36/500
is carried across unchanged and must be confirmed on-device. Matching parameters
alone does not establish pixel-identical rendering: the Dock is produced by the
same HWUI glass API but under different geometry and input than a folder icon, and
the whole chain still needs on-device visual confirmation.

