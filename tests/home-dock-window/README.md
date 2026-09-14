# HyperOS 4 Dock window regression checks

Current native implementation: [v31 semantic dynamic motion resolution,
concurrent runtime banks, persistent acknowledged recovery, and suspend/frame-channel
recovery](NATIVE_DYNAMIC_RESOLUTION.md). Java hook diagnostic version 31 also fixes
remote glass surface lifetime and immediate recovery after a live renderer is force-stopped:
attach and detach are serialized on the IPC worker, never deferred in WMS's sync
transaction. Each generation is explicitly reparented to null before releasing
its SurfacePackage. Late attachment requests are rejected after retirement; a
failed detach retains the handle and prevents creation until bounded recovery
can clean it up. Parent visibility and recents motion remain WMS-controlled.
`DockGlassSurfaceLeaseTest` covers repeated renderer generations, late attaches,
partial attachment, failed-detach retry, cancellation and idempotent release.
The lease separately controls readiness opacity with `setReady`: attachment always
shows the host at alpha `1/255` during warmup, then readiness raises it to `1`.
The common WMS parent still owns actual launcher/rotation visibility. Hiding the
capture root while waiting for its first texture can starve the capture; alpha zero
can also exclude buffered layers from composition. The compositor fallback remains
active throughout warmup. Tests cover late readiness without reattachment, repeated
writes, failed-alpha retry and rejection of retired/unattached leases.
Device verification must additionally check that repeated renderer recovery does
not accumulate old SurfaceControlViewHost / Dock glass layers in SurfaceFlinger.
An unstable provider reference and a system-server service binding both allow
OS4 to freeze the rendering process, returning BR_FROZEN_REPLY and triggering
provider-death recovery. While at least one verified HyperCeiler glass lease is
active, the hook filters only HyperCeiler's exact UID/current PID out of OS4
Greeze freeze batches. The same live-PID verification adds HyperCeiler only to
the per-request whitelist of OS4 policy-1 OneKeyClean, so clearing recents cannot
destroy an active native-glass buffer. Direct force-stop and every other clean
policy remain untouched. It first verifies exclusive package ownership and checks
PID-to-UID identity to prevent PID reuse. The lease is removed on disposal,
mode change and hot reload; other UIDs are never altered and no persistent
whitelist, global setting, started service or foreground service is created.
Long process freezes and deep sleep are detected by comparing `CLOCK_BOOTTIME` with
`CLOCK_MONOTONIC`; the native worker rebuilds its dynamically acquired WindowManager
Binder before forwarding the next coalesced launcher sample. A five-second idle health
packet exercises the frame channel without extending the lifetime of stale motion.
Native hook bytes and the complete executable `libapp.so` mapping inventory are checked
independently. Each file identity/load bias receives its own immutable hook bank, so a
runtime generation change is covered without moving shared layout/trampoline state away
from a still-active older generation.
The probe/v7 sections below are historical investigation notes; their address
profiles and opt-in probe have been removed and are not used by current builds.

The September 4 device log shows `Loaded HyperOS Runtime native module` for
HyperCeiler, but no launcher Java hook entry. An `Activity.onCreate` hook cannot
create the Dock view in this dex-free launcher. `HomeDockWindow` instead runs in
the existing `system` scope and owns two child SurfaceControl layers under the
exact launcher window: a background blur effect and a separate translucent tint.
No host surface, input window or system property is changed by this hook.

OS4 now offers only system material (saved value 1, localized as 高级材质)
and soft-light glass (saved value 3). The removed solid value 0, legacy custom
value 2 and invalid values resolve to system material in both the settings UI
and system hook. Opening settings persists that migration; the hook also
handles it without requiring a settings visit. Older OS versions keep their
existing options. The solid color picker is hidden only on OS4.

## Host test (JDK 25)

From the repository root:

```sh
dock_test_dir=$(mktemp -d)
javac -d "$dock_test_dir" \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockWindowPolicy.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassPreset.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockRecentsMotion.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockUnlockReveal.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassRetryPolicy.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassSurfaceLease.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassProcessPolicy.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockWallpaperEndpoint.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockNativeMotion.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockNativeMotionEndpoint.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassRecoveryGate.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockRotationPolicy.java \
  library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/DockGlassGeometry.java \
  tests/home-dock-window/stubs/android/os/IBinder.java \
  tests/home-dock-window/stubs/android/os/Binder.java \
  tests/home-dock-window/stubs/android/os/Parcel.java \
  tests/home-dock-window/DockWindowPolicyTest.java \
  tests/home-dock-window/DockGlassPresetTest.java \
  tests/home-dock-window/DockRecentsMotionTest.java \
  tests/home-dock-window/DockUnlockRevealTest.java \
  tests/home-dock-window/DockGlassRetryPolicyTest.java \
  tests/home-dock-window/DockGlassSurfaceLeaseTest.java \
  tests/home-dock-window/DockGlassProcessPolicyTest.java \
  tests/home-dock-window/DockWallpaperEndpointTest.java \
  tests/home-dock-window/DockNativeMotionTest.java \
  tests/home-dock-window/DockNativeMotionEndpointTest.java \
  tests/home-dock-window/DockGlassRecoveryGateTest.java \
  tests/home-dock-window/DockRotationPolicyTest.java \
  tests/home-dock-window/DockGlassGeometryTest.java
for test in DockWindowPolicy DockGlassPreset DockRecentsMotion DockUnlockReveal DockGlassRetryPolicy DockGlassSurfaceLease DockGlassProcessPolicy DockWallpaperEndpoint DockNativeMotion DockNativeMotionEndpoint DockGlassRecoveryGate DockRotationPolicy DockGlassGeometry; do
  java -cp "$dock_test_dir" "com.sevtinge.hyperceiler.tests.dock.${test}Test"
done
```

## Required device verification (not covered by host tests)

Unlock reveal uses the early `keyguardGoingAway(int)` epoch. A late-created/visible
layer joins that epoch instead of replaying from zero. Repeated transition callbacks
use the same 1221ms restart guard for both existing layers and the pending epoch.
In direct mode, already-visible layers keep animation position/alpha/matrix writes
on the frame clock; ordinary WMS material traversals must not queue an older pose.
First-show and actual geometry changes still carry their pose in WMS's transaction.
`DockUnlockRevealTest` checks epoch alignment, duplicate events, expiry while hidden,
rapid successive unlocks, and uptime-zero boundaries. It does not validate SurfaceFlinger
transaction order or the actual Flutter icon trajectory.
Clock expiry is separate from final-pose submission: a persistent pending-pose flag
survives repeated lost callbacks and is cleared only after submitting the resting pose.
The tests also cover rise-only residue after opacity reaches one, cancelled reveals,
and retrying the terminal frame without falsely acknowledging an unsubmitted pose.

The `lift-settle-v3` reveal rises from 96dp below the Dock, overshoots by about
5.5dp once, and lands with zero velocity within the same 821ms window. It holds
the start pose for a measured 10ms icon lead, because the launcher's own
`_showPresent` follows the keyguard epoch by 9-10ms, so the background starts on
the same phase as the dock icons. Opacity uses an independent 180ms smoothstep
fade so the return cannot pulse the glass.
The parent stays at scale 1; material presets and render surfaces are unchanged.
Tests bound the full trajectory, opacity, number of reversals, exact resting
endpoint, and a late-created surface joining the landing after skipped frames.

After loading the new system hook, verify
`phase=going-away pose=single-clock-v2 style=lift-settle-v3`.
Each transition-start also records `riseDp=96.0 style=lift-settle-v3`, so a
missed initialization log does not hide which curve is running. On the September 12
trace, native icon starts followed the keyguard epoch by 9–10ms and lasted about
0.8s. Verify the launcher's `_showPresent` actually occurs: a stuck
`isWorkspaceLoading=true` state can suppress icon fly-in while the background's
keyguard-triggered animation still runs.
Test fingerprint unlock from doze and unlock from the lit lock screen, including
repeated unlocks and a launcher-surface recreation. Record whether the Dock flashes at
rest before moving or abruptly jumps during the intended gentle landing. Host tests and an APK install alone
do not prove these visual results. The 821ms rise/fade remains a local approximation,
not per-frame sampling of Flutter's staggered 3D unlock animation.

1. Install the APK, enable the System Framework scope, and reboot the device.
2. Enable Dock background; select system material. Confirm `HomeDockWindow` logs contain
   `WMS dock hook ready`, a matching launcher window, and `Dock surface created`.
3. Confirm background appears behind icons, with no touch interception. Verify
   height, margin, radius, and dark-mode changes.
4. Open apps, recents, folders and the drawer; lock/unlock and rotate. Check no
   background leaks into other apps and the launcher buffer does not obscure it.
5. Disable the feature, restart the launcher, and hot-reload the module. Verify
   no old `HyperCeiler Dock` SurfaceFlinger layers remain.

Unknown window titles are logged (at most twelve launcher-only titles) and are
not guessed. If no title matches, attach the new logs. If layers are created but
remain invisible, inspect the launcher SurfaceFlinger tree on-device before
changing Z order; an opaque Flutter buffer may require a different attachment.

## Native glass (separate style value 3)

`DockGlassHost` creates an input-transparent, windowless HWUI View in the
HyperCeiler process. `DockGlassClient` performs provider IPC on its own worker
with an unstable provider client, never under the WM lock. The system hook
parents only the returned HyperCeiler surface, underneath the launcher buffer.
The original compositor blur is retained until the host commits a frame **and**
reports a nonzero vendor background-texture timestamp. Unsupported APIs,
rejected pass-window background, missing textures, and renderer death fall back
to compositor blur. An unsupported ticket uses delayed retries of 2, 4, 8, 16
and 30 seconds. Once a ticket has successfully rendered native glass, the first
loss of its live renderer is recreated immediately; only a failed recreation
resumes the bounded backoff, capped at 30 seconds. Each attempt checks the vendor texture
twenty times at 500ms intervals. An attached host whose producer is still active then
keeps waiting on the same generation: ten checks at 3s, then checks at 15s. Missing or
inactive producers still enter bounded recovery. A live silent source no longer burns
the compatibility budget or undergoes speculative refreshes that discard its texture.
Successful readiness stops polling. Hiding the parent cancels readiness immediately,
including a create completing after the hide; returning opens a new loop. Disabling
or removal cancels the ticket, stale
generation callbacks cannot affect a newer attempt, and hot reload stops the
worker. Retries never run on the WMS thread or at frame rate.
After a launcher-home preset or parent visibility return, the renderer first
checks its own vendor ViewRoot producer state (`mLastSfState`, `mTextureVis` and
the live `SurfaceTexture`). A healthy producer is left untouched; static wallpaper
timestamps are valid and need not advance. Only a stale producer activates compositor
fallback, restarts its own pass-window background, reapplies the native material,
and rechecks readiness; the native output is limited to the warmup opacity during restart.

`DockGlassRetryPolicyTest` verifies the backoff/readiness limits on the host JDK.
Actual boot-time recovery and cancellation still require device verification.

### Rotation return verification

The new build logs `glass capture warmup=positive-alpha-v1 idleWait=retain-producer-v1`.
These changes have host coverage; capture recovery on the vendor compositor still needs
a device run. After a genuinely landscape app returns to the portrait launcher, verify:

1. Rotation suspend/resume advances the epoch and creates a new host.
2. The remote glass root is shown at positive warmup alpha beneath the fallback; it
   reaches a nonzero `textureTimestamp` and `glass ready` without another gesture.
3. If the source is still silent, `glass awaiting first texture ... retaining live host`
   appears at the 3s/15s cadence changes. Host id/count stays stable across several minutes;
   no `background texture timeout` creation loop or exhausted budget occurs.
4. Leaving the launcher stops checks, returning resumes them, and renderer death still
   recovers. Repeat rotations and check that SurfaceFlinger layers do not accumulate.

The positive-alpha rationale follows AOSP's
[LayerSnapshot visibility predicate](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/main/services/surfaceflinger/FrontEnd/LayerSnapshot.cpp).
It is supporting evidence, not proof that Xiaomi's background producer uses the same gate.

### Preserve ready glass through rotation

`retained-capture-v2` pauses only the own ViewRoot producer with
`updateTextureState(backdrop, false)` when the launcher hides. The inspected ROM keeps
its SurfaceTexture/native consumer in state 2 (undraw); unregistering pass-window blur
would instead release them. A successful pause with a nonzero texture timestamp lets
the existing glass follow the visible launcher through its rotated return. Its rotation
epoch is acknowledged without destroying the host or selecting the compositor fallback.
Sampling resumes immediately when the display returns to portrait, with the same material
and source, so it follows the wallpaper return animation. The new-host 800ms settle guard
does not apply to this already-ready source. Missing,
unready, unsupported or dead sources retain the guarded rebuild path above.

Verify `glass rotation return=retained-capture-v2 resume=portrait-immediate`, then `glass capture paused ... retained=true`,
`glass rotation reused retained texture` and `glass capture resumed with retained texture`.
The host id/create count should not change on a normal rotation, and there should be no
intervening `native=false` material switch. The resumed event (`timing=portrait-return`)
should follow the portrait return without the former ~940ms delay. Blocked landscape
requests do not consume the refresh deduplication window; a queued request aborted by
rotation reversal cannot throttle the next portrait return. Visually check colour correctness throughout
return, including fast reversals and a long game where the launcher window may be removed.
The current retained path does not transfer a host across a removed WMS Layer.
Host policy tests cover eligibility and stale pause rejection, not vendor texture contents.

Every asynchronous glass entry point runs behind one boundary that catches
`Exception` only; `Error` and other VM-fatal throwables are never swallowed, so
the renderer path can no longer escape into the system-server uncaught handler.
The same restriction applies to the recoverable-failure helper used for
attachment, probe, refresh and cleanup: Kotlin's `runCatching` catches
`Throwable`, so it is deliberately not used here.
A temporarily unavailable renderer package is treated as a retryable dependency
outage rather than a hard failure: `PackageManager.NameNotFoundException` fails
the attempt safely and schedules a bounded 1/2/4/8/16/30s retry that never
exhausts and does not consume the compatibility budget. The resolved renderer UID
is cached and reused only while `getPackagesForUid` still attributes it to the
package; no `Context`, `ClassLoader` or file reference is cached across upgrades.
Recovery is single-flight: a duplicate request while one is pending is ignored and
counted, retired tickets reject stale generation/readiness/refresh callbacks, and
`release` is idempotent. `DockGlassRecoveryGateTest` covers the outage, the
single-flight latch, the exhausted budget, stale generations, idempotent
retirement, refresh deduplication and closed-client admission.

The diagnostic build records a bounded history (96 metadata events) in
HyperCeiler's private device-protected storage, independently of release logging
preferences. It includes hook endpoint resolution, glass attempt/failure/readiness,
up to twelve launcher wallpaper command samples, and limited motion-position
samples. No pixels, icons, user input or other application data are collected.
Provider history reads allow only app/system/shell; journal writes only app/system.
All IPC and diagnostic storage access happen away from WMS callbacks.

```sh
adb shell content call \
  --uri content://com.sevtinge.hyperceiler.provider.sharedprefs \
  --method dock_glass_history
```

The system hook must load the new code before it can produce these events;
installation alone does not guarantee that. Check for the versioned hook-init
event rather than assuming a reload occurred. Rebooting requires separate
permission. An empty history before reload does not prove observer failure.

Before testing the real desktop, ADB can run a fixed **unparented** host smoke
test through HyperCeiler (no screenshot or third-party app data):

```sh
adb shell content call \
  --uri content://com.sevtinge.hyperceiler.provider.sharedprefs \
  --method dock_glass_self_test
```

This does not prove that wallpaper delivery or final glass appearance works.
It releases its own host after the call. Other host-management methods accept
only system UID or the app UID, not arbitrary apps or ADB shell.

Additional device checks: glass day/night appearance and backdrop motion;
disable/re-enable; switch between system material and glass; rapid dimension changes;
HyperCeiler process death while glass is active; lock/unlock; launcher/window
removal; module hot reload. Verify touch still reaches icons and no host survives
removal. Device reboot/launcher restart and inspecting data outside HyperCeiler
require separate permission under this task's phone restrictions.

## Recents background lift

The OS4 native launcher animates its icons inside Flutter, independently of the
WindowState surface. The Dock observes the launcher's existing, window-scoped
`miui.wallpaper.animation` command without changing its arguments or result.
The APK's `WallpaperSceneAnimator` distinguishes recent (scene ratio `1.06`),
home (`1.0`), app (`1.14`) and home-hide (`1.18`). Its bIa presets also contain
a base factor `1.05`: actual device `scale_to` values are `1.113`, `1.05`,
`1.197` and `1.239`, respectively. The recent preset is `bIa@e88cd1`; home/app/
home-hide presets are at `pp+0x394a0`, `pp+0x394a8`, `pp+0x39490`.
Version-2 device history confirmed home `1.0499999523162842` and recent
`1.1129999160766602` as Float values. Both explicit base profiles (1 and 1.05)
are supported, with tight float tolerance; arbitrary scale ranges are not used.
`action=startAnim` animates the target and `setTo` finishes it immediately,
including interrupting an in-flight move toward the same target. Other commands,
scales and non-launcher windows are ignored.
Local APK evidence: the decoded Dart object pool has the recent-scene log at
`pp+0x393b8`; ARM64 code at `0x1a74184` writes `scale_to`, at `0x1a74194`
writes `action=startAnim`, and at `0x1a7420c` sends `miui.wallpaper.animation`.
The already-exported device `framework.jar` defines the Android 37 interface
`IWindowSession.sendWallpaperCommand(IBinder, String, int, int, int, Bundle): void`.
The old `sync` boolean is absent. The former seven-parameter-only resolver failed
on this device, as confirmed by its diagnostic history.

`DockWallpaperEndpoint` now checks exact six- and seven-parameter WindowState
overloads of WallpaperController's `sendWindowWallpaperCommandUnchecked` and
`sendWindowWallpaperCommand`. If unavailable, it selects the exact Session
`sendWallpaperCommand` overload with an IBinder source token, never a global
wallpaper overload. The Session fallback requires both the owning Session and
client binder to match an existing module-owned Dock layer's launcher window.
It never reads or moves unrelated windows. Missing fields disable only this
optional fallback. WM-global-lock then layer-lock ordering is preserved even
when a Session callback arrives without WMS holding its lock. If all scoped
endpoints are unavailable the static background remains usable.

Endpoint tests cover modern/legacy controller and Session signatures, rejection
of unscoped methods and incompatible return types, and rejection of foreign or
missing Session/token pairs. These tests do not establish on-device gesture
delivery; the version-3 hook must report both observer readiness and actual
motion-position events during user interaction.

The version-3 initial motion was a bounded 20dp lift over 250ms, reversed on a home/app
scene. This is a conservative approximation, **not** the native icon transform
or an exact gesture-progress match. Repeated targets do not restart it; a
reversal starts from the current position. Only the HyperCeiler effect parent's
position changes, in the WMS sync transaction. Glass host size, preset, key,
texture and child positions stay unchanged. A Choreographer callback requests
traversal while the visible background is moving; there is no idle polling.
Hidden layers finish at their latest endpoint. Removal/disabling discards the
layer's motion and hot reload cancels the pending callback.

After reloading the system hook (reboot requires user permission), verify:

1. From home, swipe up into recents: icons and background both lift; returning
   home restores the original bottom margin, without glass flashing/recreation.
2. Cancel halfway, rapidly enter/exit, and open an app from recents: no jumping,
   accumulated offset or background leaking over another application.
3. Try button-navigation recents and recents entered from an app. Confirm the
   native launcher emits the same recognized scene command in those paths.
4. Repeat for system material and glass styles, rotation, screen lock/unlock, and
   toggling the feature. Validate magnitude/timing visually on the device;
   host tests alone do not prove that this build receives the runtime signal.

September 4 validation: all three host tests above passed, `git diff --check`
passed, the final `:app:assembleRelease --offline` build succeeded, and the
APK passed v2 signature verification. `adb install -r` returned `Success` on
the user's connected device, preserving module settings. No reboot or launcher
restart was performed. Reloading the system hook and observing actual scene
events/visual motion remain pending user permission and device verification.

## September 4 diagnostic build verification

The retry-policy and existing three host tests passed. The final release build
passed assembly and APK v2 signature verification. `adb install -r` succeeded;
the installed APK hash matches the local artifact:
`21493f3b98292a6321277b094520bffed73057241acfa318fa1b916e636c18cd`.
No reboot, launcher restart or other-app data operation was performed.

New-hook events appeared after installation without an agent-requested reboot:

```text
hook init diagnosticVersion=1 enabled=true mode=3
motion observer unsupported=IllegalStateException: No window-scoped wallpaper command endpoint
glass create ... attempt=1
glass ready ... attempt=1 check=1
glass applied native=true fallbackBlur=0 visible=true
```

This confirms the current glass host and compositor fallback transition, not a
successful cold-boot retry. It also directly confirms that the animation
observer was not installed: neither exact WindowState endpoint signature was
found. The vendor method signature still needs inspection before changing the
observer. The user explicitly deferred reboot; cold-boot testing remains pending.

## Six-argument animation endpoint repair

The subsequent version-2 build passed all five host tests, release assembly and
APK-v2 verification and was installed with settings preserved. Its APK SHA-256
was `8ad653cd24f3f90f334e09291d7fa28fd2abd5f3768469d0d8dab69b839fd9ed`.
The module-only history confirmed:

```text
hook init diagnosticVersion=2 enabled=true mode=3
motion observer ready endpoint=void com.android.server.wm.WallpaperController.sendWindowWallpaperCommandUnchecked(com.android.server.wm.WindowState,java.lang.String,int,int,int,android.os.Bundle)
motion window identity bound
```

This establishes that the six-argument controller method exists and is hooked;
the Session fallback is not needed on this device. User gesture diagnostics then
exposed the additional 1.05 wallpaper base factor described above. Version 3
therefore adds the scaled endpoints and instant `setTo` handling. No device reboot,
launcher restart, services.jar export, or other-app data access was performed.

The final version-3 build passed all five host tests, release assembly and APK-v2
verification and was installed successfully. APK SHA-256:
`86c8af528879fb43b6e8f374877575081a5e33c631fc52263f1396a7705a97a9`.
The user was asked to repeat the home/recents gesture after this installation to
verify both actual position updates and visible lift/restoration.

Version-3 device logs recorded repeated moves from offset 0 to -65 pixels (20dp
at this device's density), and back to exactly 0 over approximately 250ms. The
user confirmed visible movement, but reported it begins only after the
background has already blurred. This initially suggested a late trigger, but
module-only logs could not distinguish that from a curve mismatch. The later
time-aligned launcher diagnostics below supersede that initial interpretation.

## Earlier recents-transition trigger (version 4)

This was an unsuccessful experiment, removed in version 5.

The same previously exported framework defines
`WindowOrganizer.startMiuiNewTransition(int type, WindowContainerTransaction t,
boolean isRecentsTransition)`. The third parameter name is preserved in its DEX.
The new observer runs before this system entry, without changing its arguments
or result. It only lifts an existing visible Dock if the Binder caller UID
matches that launcher's own WindowState Session UID. Ordinary transitions,
missing ownership and hidden Docks are ignored. Wallpaper scene commands still
confirm, reverse or immediately reset the target without restarting duplicates.
An unconfirmed early start restores the Dock after one second; stale timeout
callbacks check the layer identity, generation, feature state and cleanup flag.
WM-global-lock then layer-lock order is preserved. This timing change requires
fresh device verification; it is not a claim of exact Flutter gesture progress.

During version-3 stress gestures, module-only process exit records reported
`unstable content provider`; scoped Java crash logs were empty. That reason alone
does not establish the original cause of process death. Version 4 holds an
UNSTABLE provider client for each active glass ticket and closes it on disposal,
rather than reopening the renderer reference for each status check. It never
creates a stable dependency from system_server. Diagnostic writes are batched
at 250ms and return only an empty acknowledgement, not the entire history.
Renderer stability still requires device verification, independently of motion.

The final version-4 build passed all five host tests, release assembly and APK-v2
signature verification. `adb install -r` succeeded without a reboot or launcher
restart, preserving settings. The installed APK SHA-256 matches the local build:
`0a8974df28f07a5c51b84bf27d355495f4eddae0fe24ea2734c59bec9f5696e7`.
Module-only history confirmed `hook init diagnosticVersion=4` and
`early recents observer ready` for the exact three-argument
`WindowOrganizerController.startMiuiNewTransition` endpoint. This confirms hook
installation, not that a real gesture has reached the earlier path.

The subsequent 20:32 device test did not validate this fix: the user still saw
movement begin after the backdrop blurred. Module-only logs show repeated
wallpaper-driven lifts/restores, but no `early recents start` event. The hook
exists, yet the tested path did not pass its trigger/ownership/visibility guards;
the current evidence cannot distinguish a missing call from a rejected guard.
Do not relax ownership checks or infer recents from unrelated transitions.
The native launcher library also routes recents through the SystemUI shared
proxy, so the framework entry cannot be assumed to run under the launcher UID.
Focused launcher animation logs require the user's separate permission under
this task's phone data restriction before investigating that live path further.

Renderer death recurred during this test (attempts 1 and 2), followed by a
`DeadObjectException` on attempt 3. Holding the unstable provider client has not
established a renderer-stability fix. Cold-boot recovery remains untested; no
reboot, launcher restart or other-app diagnostic access was performed.

## Scene-synchronized spring (version 5)

With explicit user permission for focused launcher animation logs, the 20:40:47
gesture established the following sequence (same device wall clock):

- Shortcut/icon drag signals already occur at 20:40:47.056 and earlier.
- Home hold starts at 20:40:47.120; the blur spring starts at 20:40:47.122.
- The scoped wallpaper command sets the Dock target at 20:40:47.122, not after
  blur completion. The first two surface offsets are -0.15px and -1.55px.
- At 20:40:47.201 the native blur is already approximately 49% complete.
- A second gesture confirms only a 2ms difference between blur start and the
  Dock target. No early WindowOrganizer trigger was recorded.

The old 250ms smoothstep moves only 21.6% in its first 75ms. Version 5 replaces
it with an analytic damped spring using the observed **backdrop** scene presets:
enter response 0.3s / damping ratio 0.9; exit 0.45s / 0.95. Enter progress is
approximately 50% at 75ms. The lift remains 20dp, clamped to its endpoints, with
position and velocity preserved across interruptions. Duplicate commands do not
restart it. Endpoint/low-speed settling stops frame requests; a 1.5s hard limit
bounds every transition. Hidden windows and `setTo` discard momentum. The first
six position samples plus completion are recorded for timing verification.

The ineffective WindowOrganizer hook, caller-UID dependency and speculative
one-second prestart timeout have been removed. Runtime animation continues to
use only the exact launcher's scoped wallpaper commands. No logs are parsed at
runtime and no input events are intercepted. This improves **hold/overview**
backdrop timing; it does not implement the earlier Flutter finger-drag phase or
claim the exact native icon transform. That distinction still needs visual
device testing. Renderer stability and cold-boot recovery remain separate,
unresolved validation items.

Version 5 passed all five host tests, release assembly and APK-v2 verification.
`adb install -r` succeeded and the installed APK hash matches the local artifact:
`f05d2b22b7419f09be7ce846408b1366989808cd976580a10740e0fa81dbf956`.
At 20:47:36 the module reported `hook init diagnosticVersion=5` and the scoped
wallpaper observer ready. At 20:47:43 the glass reported ready on attempt 1 and
`native=true fallbackBlur=0`. No reboot or launcher restart was performed. Fresh
post-install gesture timing and subjective synchronization remain pending.

The user subsequently confirmed reduced delay, but not full synchronization.
Version-5 module logs show successive position updates mostly 16-17ms apart;
the previous launcher gesture logs contain native scene updates about 8ms apart.
This does not by itself prove the display refresh rate or visual frame delivery.

## Direct compositor-frame motion (version 6)

The prior animation callback only posted a WMS traversal, which then sampled the
motion in `prepareSurfaces` and posted another animation callback. Version 6
removes this round trip for position-only motion. A compositor Choreographer
callback now samples the existing spring using its frame timestamp, submits a
module-owned transaction containing only Dock-effect positions, and schedules
the next frame only while movement remains active. The transaction carries the
Choreographer VSync ID. The already-exported device framework confirms
`getSfInstance`, `getVsyncId`, `setAnimationTransaction`, and
`setFrameTimelineVsync` signatures. Fallback uses the prior traversal path if
direct submission is unavailable; neither path changes host surfaces.

WM-global-lock then layer-lock ordering protects visibility, removal and cached
geometry. WMS remains responsible for initial parenting, crop, material,
visibility and geometry changes; an early wallpaper command explicitly requests
initial layout submission. Unchanged layout does not enqueue stale animation
positions while direct motion is running. Position caches update only after a
successful direct submission. Hidden/removed/disabled layers do not continue
animation, and hot reload removes the callback and closes the owned transaction.
Frame timestamps cannot rewind past a newer scene or layout timestamp; the host
test covers that monotonicity guard. Position samples identify `source=vsync`
or `source=layout` for device verification.

The 20dp lift and version-5 spring presets are unchanged to isolate scheduling.
This still does not implement the earlier Flutter finger-drag transform. Device
checks must establish actual direct submissions, cadence, correct restoration,
rotation/visibility cleanup and subjective synchronization before claiming an
improvement. Renderer recovery is separate from this scheduling change.

The final version-6 build passed all five host tests, `git diff --check`, release
assembly and APK-v2 verification. Installation preserved settings and the phone
APK hash matched `2f53d9b1dce9ebfd06aacfbf7be3f85b50e78738116852c7fbe58819368c7e89`.
At 20:57:29 module-only logs confirmed `hook init diagnosticVersion=6` and the
scoped wallpaper observer. No reboot or launcher restart was performed. Direct
frame-path activation and user gesture/cancellation verification remain pending.

### Version-6 live result and remaining source mismatch

The subsequent 21:50 module logs confirm `source=vsync` submissions about 8ms
apart (for example 21:50:31.297, .305, .313, .321, .329, .337), followed by -65px
and exactly 0px endpoints. The user still reports desynchronization. Thus the
direct path works, but higher submission cadence does not resolve the remaining
source mismatch; this is not a verified complete synchronization fix.

Explicitly authorized launcher animation logs (PID 31911) show a different
trajectory than the blur presets used by the Dock. At 21:50:35.116 the icon
layer already targets scale 0.993731, then repeatedly updates that target down
toward 0.95 with response 0.15 / damping 0.86. The Dock's overview command only
arrives at 21:50:35.296. On gesture release the icon layer switches to scale 0.95
with response 0.5 / damping 1.0; returning home uses scale 1.0 with the same
critical-damping preset. This requires continuous icon-layer samples, not just
replacing the two endpoint spring constants or adding a fixed time offset.

Local AOT inspection identifies a candidate continuous scale callback:
`rNb` closure at 0xe2a1a8 calls 0xe2a1d0, which writes the same boxed-double
argument to both receiver fields at tagged offsets 0x43 and 0x47 via 0xab4794.
Its adjacent registration strings are `shortcut_menu_layer_scale_config` and
`shortcut_menu_layer_alpha_config`. This is a static candidate, not a validated
runtime hook or proof of the final hotseat transform/pivot.
The inspected libapp.so Build ID is `4f1bdaedba4ed60ade7d3ebc0fbbdc89`, SHA-256
`80cdbc61aa3e155c320999edaa94fa89fe623f8302cef574dadf3c06ff04feb8`.
Never apply an AOT offset without exact build/instruction and Dart ABI checks.

No log parsing, global touch interception, new native instrumentation or
additional guessed animation tuning has been enabled. An eventual native
bridge must preserve Dart registers/GC safety, validate source ownership and
module opt-in, transport bounded scale samples without blocking the render
thread, and fall back safely when unavailable. Loading/validating a changed
launcher native module requires separate permission to restart the launcher;
the existing authorization only permits animation-log reads, not that restart.

### Opt-in native probe and authorized restart

The user subsequently authorized one launcher restart for native diagnostics.
A probe is now available only with `-PdockNativeProbe=true`; ordinary builds pass
the CMake probe option OFF. It pins the 16-byte Build ID above, a readable/executable
mapping, and the entire 80-byte scale callback. A mismatch prevents hook installation.
The ARM64 observer uses the pinned Dart ABI (x15 stack, x16/x17 temporaries),
preserves borrowed registers/NZCV, checks the boxed Double class ID, and copies
only scalar bits to lock-free native storage. It makes no C/Java calls, retains
no heap pointer and changes no animation arguments. A detached diagnostic worker
waits at most 15s for the library, then observes for at most 180s with at most 60
aggregate reports. Sampling is disabled afterwards; no live trampoline removal
is attempted. No IPC or Dock animation integration is enabled by this probe.
The [Dart ARM64 register definitions](https://raw.githubusercontent.com/dart-lang/sdk/main/runtime/vm/constants_arm64.h)
and the pinned callback disassembly were checked before building.

`DockNativeProbeProfileTest.cpp` checks exact/mismatched/truncated profiles and,
when passed the inspected libapp.so path, verifies the actual artifact bytes.
The host C++ test passed against that artifact; the ARM64 object disassembly was
reviewed for stack/flag restoration and absence of call instructions. Release
assembly and APK-v2 verification passed. The installed APK hash is
`69877275f142c2b8d2665484899e1987f714f1f58eac1fc9110f6d5fcc21c994`.
The previous v6 APK was preserved at
`/private/tmp/hyperceiler-before-native-probe.vrtKxf/HyperCeiler-v6.apk`.

At 22:05:54 the authorized force-stop/start changed the launcher PID from 31911
to 32647. The new native module loaded, but at 22:05:55 reported
`probe not installed: build/instructions mismatch`. PID 32647 remained running.
No scale hook was installed and no new synchronization improvement was enabled.
The local base.apk extracts to the expected libapp.so SHA-256, and its code
matches the profile; this does not establish which runtime check failed on the
phone. The current combined diagnostic cannot distinguish a different build
from changed instructions. Do not bypass the guard. Exporting the currently
installed launcher APK for comparison requires separate permission under the
phone-data restriction. The one permitted launcher restart has been used; no
phone reboot or app-data clearing was performed.

### Current launcher 6236 profile (probe v2)

With separate permission, only the installed launcher APK was exported to
`/private/tmp/hyperceiler-current-launcher-20260904/base.apk`; no launcher user
data was read. Its version is `RELEASE-8.01.02.6236-260818-09031543-R`, whereas
the original sample is `RELEASE-8.01.02.6179-260818-08292132-R`. APK SHA-256:
`5aab4adbda52c0f046b5851bb702edc41e0c438099e3d4f81f84e6b00df25cd3`.
The current libapp.so Build ID is `4f1bdaed2dd40323de7d3ebcec64d4b3`, SHA-256
`98a3b9b7891ddf06eb4625846e490c31a8bab738e645428d1cb20dbedb66536f`.
This explains the v1 guard rejection; the guard must remain strict.

Current AOT registration ties `shortcut_menu_layer_scale_config` to closure
0xe25718, calling 0xe25740. The 80-byte callback writes its boxed Double to
the same Rx fields at tagged offsets 0x43 and 0x47, now calling 0x86dd24.
The Dart register convention and Double class/tag layout match the original
sample. Probe v2 selects either reviewed profile by exact Build ID and then
checks all 20 instructions. Unknown IDs, mapping failures and changed code now
have separate rejection messages. No fallback address scan is performed.

The host C++ test passes against both real library artifacts and checks null,
truncated, mutated and cross-profile inputs. This is static validation only:
the 6236 callback has not yet been verified on-device, and the probe still
does not drive Dock motion. The earlier one-time launcher restart permission
has been used; loading v2 needs another explicit restart authorization.

Probe v2 release assembly, APK-v2 signature verification, all five Java host
tests and the C++ profile test passed. The rebuilt ARM64 observer still has no
call instructions and restores its borrowed registers/flags. Installation
completed with settings preserved; APK SHA-256:
`b85ff532908d30f5925f7b5c255d68943843c266a09c572d116d698e5977f514`.
No second launcher restart or phone reboot was performed in this step.

### Probe v2 loaded on the device

The user then authorized another launcher restart and ongoing read-only system
app inspection. The interrupted attempt did not restart the launcher: after ADB
reconnected the PID was still 32647. The authorized restart was performed at
23:41:41, changing the PID to 25310. At 23:41:42 the module reported
`probe v2 ready: launcher=6236`; at 23:41:43 its first callback sample was 1.0.
Thus the exact-build/code guards and observer installation succeeded on 6236.
Manual recents/return sampling is pending; successful installation alone does
not prove continuous gesture coverage or synchronization. No phone reboot,
app-data clearing, or additional launcher restart was performed.

During the subsequent user gestures, PID 25310 produced 2302 callback samples
by 23:42:33.253, with many intermediate values and repeated exact 0.95 / 1.0
endpoints. Examples: 23:42:06.743 count=34 value=0.9552578;
23:42:07.745 count=174 value=0.9458601; 23:42:08.245 count=227 value=1.0;
23:42:32.252 count=2257 value=0.95; 23:42:33.253 count=2302 value=1.0.
This verifies repeated native callback delivery, not just startup notification.
The 500ms aggregate log does not establish exact first-sample latency or
per-frame pairing; counts can exceed the display frame rate. Values below 0.95
also show that blindly converting an assumed fixed range is insufficient.
The follow-up must establish recents-only scene ownership, transform/pivot
mapping and bounded nonblocking transport before driving the owned Dock surface.
The installed probe still does not change Dock motion.

### Version 7: native-scale-driven Dock motion (implementation, device pairing pending)

The ordinary build now includes the native motion observer, gated to the exact
6236 Build ID and scale body. Additional guards fingerprint the entire animTo
body at 0xdeecd4 (0x660 bytes), setTo at 0xe24488 (0x334 bytes), the parameter
toString mapping at 0x155b334 and the allocation stub at 0x1900a30. The last two
establish aPb/CID 1777, alpha at tagged offset 7, scaleX at 0xf,
isSurfaceChange at 0x2f, and shouldTryDisableByRecents at 0x33. All mutations
in each guarded region are rejected by the host artifact test. Unsupported
builds retain the wallpaper-driven fallback; no address scanning is used.

Two scene observers and the already-validated scale callback publish only a
scalar plus a two-bit scene into lock-free native storage. Eligible recents
targets require alpha 1, isSurfaceChange false, the recents flag, and target
scale in [0.94,1). Target 1 is a return, even with a default-true recents flag;
other scenes clear the latch. setTo also publishes its immediate scale because
it bypasses the animation callback. This combination needs actual folder/app/
gesture cancellation testing; the flag alone is not a unique scene ID.

The ARM64 wrapper preserves borrowed registers/NZCV and the x15 Dart stack.
It calls no C/Java functions and retains no heap pointer. When subscribed it
uses a raw nonblocking write to a permanent eventfd to wake the worker, which
blocks in poll when idle. The [Linux eventfd contract](https://man7.org/linux/man-pages/man2/eventfd.2.html)
and [arm64 syscall convention](https://man7.org/linux/man-pages/man2/syscall.2.html)
were checked. The event descriptor is never recycled while a callback could
reference it; it is reclaimed at process exit. No high-frequency idle polling.

The worker owns an abstract Unix socket named with the launcher PID and accepts
only UID 1000. The WMS client checks SO_PEERCRED UID **and PID** against the
already-matched launcher's WindowState Session, not package-name input from a
packet. Both socket I/O and connection retries are off the WMS thread. Fixed
32-byte packets carry magic, version, increasing sequence, monotonic worker
timestamp, packed scale/scene. Invalid or older-than-150ms packets close the
connection. The sender has a small socket buffer, uses nonblocking sends, and
disconnects on a short write/backpressure instead of constructing a stale queue.
The receiver retains just its latest immutable sample. Hidden/removed/disabled
windows and hot reload close the client; startup/reconnection attempts are bounded.
No SELinux rule, host surface or input handling changes are made.

WMS version 7 uses the received scale directly to move its own common Dock parent
on the next SF VSync, without applying another spring. Existing nominal travel
is retained: scale 1 -> 0dp, scale .95 -> -20dp; a measured small overshoot is
allowed up to -24dp. Home return only follows an existing recents latch and
clears it at scale 1. Unrelated scene/out-of-range samples reset it. Transport
loss resumes the old scene fallback from the current position. This maps the
native curve, not a proven physical hotseat pivot/translation matrix. Native
sample-to-visible-frame phase matching still requires real device verification.

Six Java host tests pass, covering packet rejection, replay, early drag,
return/cancel, unrelated zoom, overshoot bounds, geometry and fallback continuity.
The C++ artifact profile test passes against both 6179 and 6236. The independent
`DockNativeArm64Test.cpp` plus `DockNativeArm64Harness.S` was cross-compiled and
run successfully on the phone against the production observer assembly, using
only synthetic fixtures and its own eventfd. It checks preserved x0-x15, NZCV,
Dart stack, untouched fixtures, class/Smi/scene guards and subscribed/unsubscribed
notifications. The temporary phone test binary/directory were removed; sources
and the local binary remain available. No real launcher restart was needed for
this isolated test. Release assembly and APK-v2 signature verification pass.
APK SHA-256: `3acfb0099190a8f57f5c01219342238ee442fe6211f3b7072d6f69de777249bc`.

Required next device checks: version-7 WMS load, native motion-ready after a
separately authorized launcher restart, socket peer acceptance (including any
SELinux denial), native-vsync position samples before the wallpaper endpoint,
return restoration, gesture cancellation/reversal, folder/app isolation and
no idle frame submissions. Do not claim complete synchronization from build
or isolated assembly tests alone.

Installation succeeded with settings preserved. At September 5 00:02:17 WMS
PID 3586 reported `hook init diagnosticVersion=7 enabled=true mode=3`, confirming
the Java side hot-reloaded. The new native side has not been loaded into the
existing launcher process yet; no additional launcher restart or phone reboot
was performed during implementation/installation.
