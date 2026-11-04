# Third-party notices

## MiuiBackGestureHook status-bar follow

The status-bar-follow implementation under:

- `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/`
- `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbarfollow/RecentsOpenSession.java`
- `library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemframework/statusbarfollow/`

is derived from the MiuiBackGestureHook project and modified for HyperCeiler.

- Source project: <https://github.com/wxxsfxyzm/MiuiBackGestureHook>
- Reference commit: `004bc059e5821775917b771d89aa1d8c18a1e669`
- Baseline feature commit: `b6e582fc32f83bda3d153b12e3ded2d411acf7f3`
- Later fixes included in the reference logic: `6870c01`, `c256ec2`, `acd2048`, `004bc05`
- Copyright: Copyright (C) 2026 MiuiBackGestureHook contributors
- License: Apache License 2.0
- License text: <https://www.apache.org/licenses/LICENSE-2.0>
- Changes: package/import adaptation, HyperCeiler hook/log/preference/gate integration, default-off behavior, and compile-only hidden API accommodation.

The copied files retain the source/license header. No reference-project logging or diagnostic tools are copied.
