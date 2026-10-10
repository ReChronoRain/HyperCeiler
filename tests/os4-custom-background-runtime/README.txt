Test14 CustomBackground OS4 lifecycle and shader-tint regression

Production under test:
- CustomBackground.kt: OS4 early routing; remainder identical to frozen test13.
- CustomBackgroundOS4.kt: four selected styles, NC/DI owner generations, views,
  foreground journal, exact late layout writer, receiver and worker lifecycle.
- MediaSeekBarColorOS4.kt: exact current-owned high/middle draw scope only.

Reproduce from project root:
  python3 source/tests/os4-custom-background-runtime/run.py --output NEW_DIR --negative-controls
  python3 source/tests/os4-custom-background-runtime/compile_android.py --output NEW_DIR
  python3 source/tests/os4-custom-background-runtime/verify_rom.py --output NEW_DIR
Output directories must not already exist; reports bind inputs before/after.

The host suite compiles complete production Kotlin against Android37 first and
runs the real EzHook1.3.0/API102 before/after chain. Android views/Looper,
controller method bodies, renderer/drawer and asynchronous scheduling are
explicit stand-ins. It does not run Xiaomi SystemUI or LSPosed. A separately
compiled complete old/new route tests OS4 no-legacy lookup and OS3 legacy entry;
verify_rom additionally compares the full non-OS4 source with frozen test13.

The authoritative positive host-v11 suite has 132 cases. The 13 negative controls
compile modified copies, never production, and require a named behavioural
witness to fail. Final negative-parallel-v1 explicitly combines completed
host-v11 controls with remaining sibling runs of the same frozen runner; the
serial parent stopped at the eighth completed report boundary before any next
child existed. transition.json records the exact split; no completed report
was replaced. The parent process termination is not itself counted as a pass. The SDK test compiles all five real current production Kotlin
files with SDK37/API102 and frozen peer jars, without the Android/view/renderer
host stand-ins. Counts are cases, not independently adapted user features.

Actual ROM differences:
- NC Normal.DayNight style sets progressBarMode=0. Phone marketing tier does not
  imply this NC view selects the high-end shader.
- Both DI layouts use DayNight with no override; constructor default -1 selects
  DeviceUtils level. Primary setters accept 0/1; level2 ignores all four.
- The additional exact drawHighEndMode(Canvas) hook therefore temporarily colors
  only the current custom presentation's Paint. The original shader, uniforms,
  texture, geometry, alpha, original call and exception continue. SRC_IN changes
  the decorative head hue by design to the custom foreground palette. Level1's
  primary path remains outside the temporary draw scope. No global Paint or
  onDraw hook and no change to device level.
- An additional module SeekBar retains its own thumb/progress/background tint
  journal. No album field, original holder background, material callback or
  original view is removed.

Host coverage includes styles1..4, independent NC/DI options (including actual
control_control_color_anim keys), all12 registration failure positions,
original skip/throw and invocation counts, before/after args replacement,
nested attach/detach/bind, retained data at attach, null DI binds, stale jobs,
shared DI render with separate drawers, background bounds/outline/transforms,
paused/screen/AOD/drag state, current insertion order, weak-owner collection,
one receiver per group, partial cleanup faults, foreign background replacement,
primary and alternate seekbar colors, independent updateLayout$1 color writer,
transitionAlpha suppression/restoration, shader levels0/1/2, unknown/unowned
view passthrough, nested Paint scopes, foreign filter writes and cleanup.

Separate real Android prerequisites:
- ../custom-overlay-art-v1: real View/PixelCopy alpha composition.
- ../seekbar-color-art-v1: actual production helper, real API102 chain, original
  OTA shader/head bytes in a fixture SeekBar, actual RuntimeShader/PixelCopy.
- ../renderer-v1 and custom-drawable reports: actual rendering algorithm/drawer
  tests; the lifecycle host does not duplicate those implementations as proof.

Limits:
- Physical Xiaomi17ProMax OS4.0.0.44 UI, LSPosed injection and long-run behavior
  remain NOT_TESTED. Resource overlays, future ROMs, arbitrary reflection or
  unobserved third-party writers are not inferred safe by static seed scans.
- External writes of transitionAlpha=0 equal to the owned0 cannot be
  distinguished. Known actual callback writes transitionAlpha to the player
  parent, not this media background; parent transitions remain original.
- Cleanup attempts every owned action and reports failures. Permanent framework
  setter/removal failure is not represented as guaranteed recovery.
- Change mode/preferences with SystemUI restart; this suite checks current
  configuration activation and cleanup, not a new preference IPC mechanism.

Historical diagnostics remain present. host-v10 executed130/130 but correctly
failed its accidentally declared132 total; host-v11 adds prior-skip and busy
scope cases to reach132. rom-v1 stopped on the checker's singular pref_key typo;
rom-v2 verifies the actual prefs_key XML names. Other early directories retain
fixture/setup failures and are not final approval evidence.
