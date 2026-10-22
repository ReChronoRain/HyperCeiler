# OS4 status bar signal order regression

This suite compiles the actual production hook with a minimal hook API double and dispatches its registered factory callback. The input names come from the supplied popsicle OS4.0.0.44 SystemUI's `config_statusBarIcons` resource. The real target DEX has `provideStatusBarIconList(Context)`, `mSlots`, and `Slot.mName`; it has no `StatusBarIconList(String[])` constructor.

```sh
python3 tests/os4-statusbar-slot-order/run.py --java /path/to/java --kotlin-home /path/to/kotlinc
```

`--production /path/to/original.kt` runs the same test against the original source. Tests check ordering, original object/list identity, missing and duplicate names, unfamiliar slots, idempotence and OS3 route retention. They do not install hooks in LSPosed or verify device rendering. Full APK compilation and device testing remain separate requirements.
