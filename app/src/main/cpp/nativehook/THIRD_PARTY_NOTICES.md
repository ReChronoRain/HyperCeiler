# Third-party notices — nativehook/

This directory contains code derived from the
[MiuiBackGestureHook](https://github.com/wxxsfxyzm/MiuiBackGestureHook) project
(`miui-home-hyos-native/`), licensed under the **Apache License, Version 2.0**.

Source revision referenced for this port: `aab59e2b78561271d56ea10952373bd28718596b`
(local read-only checkout used for analysis; only `miui-home-hyos-native/` was
consulted, and nothing in that repository was modified).

## Apache-2.0 derived code

| File | Derivation |
|---|---|
| `elf_image.h` | Ported and generalized from `runtime_profile_resolver.cpp` (`ParseElf`, dynamic-table parsing) and `lsposed_hook_backend.cpp` (`BuildDynamicView`, symbol/relocation scanning). Deltas: byte-offset decoding (no `<elf.h>` dependency), GNU/SYSV hash table *lookup* (the source only derived symbol counts), snapshot-span based parsing instead of live memory reads. |
| `got_hook_backend.h` | Ported from `lsposed_hook_backend.cpp` (`WritePointer`, `CollectRelocationSlots`, `PltHookRaw`, RELRO handling, all-or-nothing rollback). Deltas: parameterized by an `ElfImage` instead of hard-coded launcher module identities; health model aligned with the project's hook bank semantics. |
| `page_guard.h` | Ported from `lsposed_hook_backend.cpp` (`AddProtectedPage`, `GuardedMadvise`, guard state machine). Deltas: the hooked library (originally the HyperOS Flutter runtime) is a caller decision; the guard is an optional policy, not a mandatory dependency. |
| `arm64_decode.h` | ADRP/ADD-pair, PLT-stub and conditional-branch decoding derived from `runtime_profile_resolver.cpp`; register/load-store/bitfield decoders abstracted from this project's own Dart resolver. |
| `inline_hook_backend.h` | The `NativeAPIEntries` (hookFunc/unhookFunc) boundary shape follows `native_api.h`; the null-continuation refusal and rollback logic come from this project's `targets/home/dock_native_hooks.cpp`. |
| `nhk_base.h` | `AddOverflows` / bounded-range discipline follows the source's overflow-guard style. |

## License text

Apache License 2.0 text: see the `LICENSE` file shipped with the upstream
MiuiBackGestureHook repository, or
https://www.apache.org/licenses/LICENSE-2.0.

## Attribution requirement

Per Apache-2.0 §4, the ported files carry a header notice naming the source
project and this file. HyperCeiler itself is AGPL-3.0-or-later; the derived
files above remain subject to Apache-2.0 with the notices preserved.
