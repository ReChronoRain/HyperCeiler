# OS4 lockscreen DND presentation

This test compiles the complete production `HideLockscreenZenMode` Kotlin object against the real EzHook 1.3.0 / libxposed API 102 callback implementation. Only the hook backend, ROM target classes, version lookup and view operations are host fixtures. It does not load the original ROM DEX, ART, Android animation code or LSPosed.

The existing key is `prefs_key_system_ui_lock_screen_not_disturb_mode` (隐藏勿扰模式通知). SystemUIB already gates this rule with that Boolean. The old OS3 controller is absent in the target OTA. OS3's existing source branch is preserved.

The OS4 implementation changes two display inputs and executes each complete original method once:

* `FlipNotifDataStore.merge(boolean, MediaData, List)` receives false only for its local tiny-lockscreen Zen entry. Its media handling, keyguard notification privacy filter, summary filter, focus wrappers, four-item capacity and returned list remain the original implementation. The original false branch resets its local dismiss flag and local `mZenModeOn`; neither is the global DND setting.
* `NotificationNumStateViewBinder$bind$1$1$1$1.emit(Object, Continuation)` receives false only for its exact R8 classId 0 Boolean consumer and a valid target view. The original method sets its view state, updates text, requests layout and posts the original hide animation. Other class IDs keep their input. The common ViewModel flow, Zen controller and DND settings remain original. The ROM emit has no suspension point; the caller's collection state machine and continuation are not replaced.

The selected OTA's full DEX scan finds six merge callers and only one writer of the ordinary view's `isZenModeEnabled` field. The original ordinary layout starts the Zen child invisible and alpha zero. The false animation also hides the dividing line and retains the notification-count path. The tiny row's click callback can turn DND off; this implementation never calls it. This is local lockscreen presentation scope, not removal of system DND functionality or every Zen icon/notification in all apps.

Run from the project root:

```
python3 -B source/tests/os4-zen-notification/run.py --output /tmp/zen-host-new --negative-controls
python3 -B source/tests/os4-zen-notification/compile_android.py --output /tmp/zen-sdk-new
python3 -B source/tests/os4-zen-notification/verify_rom.py --output /tmp/zen-rom-new
```

The 66 host scenarios cover both display branches; 16 combinations of Zen/media/privacy/dismissal; original ordered capacity and filtering; count/Zen queued interleaving; all other R8 branches; invalid values and null views; original exceptions/cancellation and return/continuation identity; pre-skipped real callbacks; exact shape rejection; two-hook installation failures; failed unhook with inactive residue; and original OS3 behavior. Twelve in-memory source mutations must fail behavioral assertions, not just compilation. View/animation and media/notification wrappers are explicit semantic transcriptions; their Android framework timing is not reproduced.

Both shapes resolve before registration. Hooks remain inactive until both registrations succeed. A partial failure attempts removal and leaves any callback residue inactive. This rule is configured at SystemUI initialization; change the switch and restart SystemUI to apply or restore it. The tests do not establish live setting hot reload, animation frame timing, third-party hook interoperability or behavior on another ROM shape.

All actual device, injection and long-duration statuses remain `NOT_TESTED`. Device validation must compare disabled/enabled states, DND already enabled at startup, subsequent DND toggles, privacy/notification-count/media changes, ordinary and available tiny lockscreen layouts, and reversal after restart. Confirm the DND tile/setting and actual sound suppression remain enabled while only the lockscreen presentation disappears.
