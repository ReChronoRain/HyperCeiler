# OS4 glass lifetime regression (2026-09-06)

## Observed failure

While the user reported missing blur in the minus-one screen, Control Center
and notifications, SurfaceFlinger retained 12 HyperCeiler SurfaceControlViewHost
roots and 11 Dock glass buffer layers under one Dock effect parent. Only one
renderer host was active. Most old roots had no remaining local handle.
The compositor logged `PassBlur.deqBuf failed`; affected system processes also
reported missing background snapshots. Global window blur remained enabled.

The old client released SurfacePackage references without detaching their
server-side roots. Renderer death/retry therefore accumulated attached layers.
WMS also used its deferred sync transaction to reparent the remote root; an
independent worker detach alone would leave a late-commit resurrection race.

## Fix

- One lease per renderer generation owns attachment/retirement state.
- The serial IPC worker performs both attachment and explicit null-parent
  detachment. WMS only transforms/shows/hides the owned Dock parent.
- Retirement rejects late attaches, including after partial attachment failure.
- Detachment precedes package release. A failed detach retains the handle and
  blocks replacement creation until bounded recovery can complete cleanup.
- Readiness requires attachment as well as a vendor background texture.

The host-JDK test covers twelve generations, attach-once, late requests,
partial attachment failure, cleanup retry, cancellation and repeated release.
All seven Dock host regression suites passed. Release APK signature verified.

## Device checks

The first installation loaded hook diagnostic version 9 through hot reload.
All 12 old roots / 11 old buffers disappeared. A fresh renderer reported
`backgroundReady=true`, and the user confirmed that blur was normal again.
Repeated renderer recovery did not accumulate old roots after this change.
Neither the phone nor launcher/SystemUI was restarted; only HyperCeiler-owned
surfaces were removed. One explicit HyperCeiler-only force-stop was used to
exercise renderer death, without clearing data.

Further testing exposed an existing, separate lifecycle issue: OS4 SmartPower
froze HyperCeiler while the windowless renderer was in use. Binder calls then
returned `BR_FROZEN_REPLY`; ActivityManager recorded `unstable content provider`
exits. Several such exits preceded the deliberate force-stop and also existed
in the pre-fix history. The retry budget could become exhausted.

Hook diagnostic version 14 replaces the ineffective service-binding experiment.
OS4 explicitly ignores system-server dependencies when freezing background
apps. The active generation now owns an in-memory freeze guard for the verified
HyperCeiler UID/current PID, removed before the generation is disposed. It does
not add a persistent whitelist or change any system/global policy.

Version 13 also keeps retrying at a capped cadence after a renderer that previously
reached native-glass readiness is killed. Unsupported configurations retain the
bounded startup budget. This prevents a launcher restart from being the only way
to recover after cumulative runtime deaths. Verification requires returning to
the desktop; no host is created while the launcher is invisible.

Version 16 distinguishes loss of an already-ready renderer from an initialization
failure. The former is recreated immediately, removing the fixed two-second gap
after OS4 OneKeyClean force-stops the HyperCeiler package. If that immediate
recreation does not become ready, subsequent failures return to the bounded
2/4/8/16/30-second backoff.

The active lease also contributes the package only to the temporary whitelist
constructed for OS4 policy-1 OneKeyClean. The check requires the stored renderer
PID to resolve to its previously verified UID at the instant of cleaning. It does
not alter a static whitelist and does not intercept direct force-stop, swipe-kill,
thermal, idle, lock-screen or other cleanup policies. This retains the live glass
buffer through the recents clear instead of merely shortening its reconstruction.

Version 20 also handles launcher-parent visibility transitions. Returning from a
cold-started application reasserts pass-window blur and invalidates only HyperCeiler's
own windowless glass views after allowing several display frames for the WMS show
transaction. The Surface
package is retained, so this refresh neither accumulates layers nor flashes through a
renderer recreation. Wallpaper scale is no longer allowed to hide the Dock as a
guessed edit-mode signal.

## Follow-up verification

1. With glass visible, confirm one bound DockGlassRenderService, one host root,
   one Dock glass buffer, and a nonzero background texture timestamp.
2. Leave the desktop visible past the previously observed freezing interval,
   visit Control Center/notifications/minus-one, then return. Verify stable PID
   and no new `BR_FROZEN_REPLY` or renderer-death retries.
3. Disable glass / change material / remove Dock: confirm the service binding
   and remote root disappear. Repeat creation and renderer recovery; counts
   must return to one active generation rather than growing.
4. Boot recovery and long-duration stability remain separate device checks;
   no phone reboot was authorized or performed for this repair.
