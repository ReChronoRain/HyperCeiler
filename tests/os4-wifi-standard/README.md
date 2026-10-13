# OS4 Wi-Fi standard number

This suite compiles the complete production `WifiStandard.kt` and executes its installed callbacks through the real EzHook 1.3.0 / libxposed API 102 chain. It uses the actual kotlinx-coroutines-core 1.9.0 library for suspension, resumption, two concurrent collectors, downstream exceptions, cancellation, `stateIn`, and derived Flow consumers. It does not execute the ROM DEX, ART, LSPosed injection, or Android views.

The selected OS4 ROM uses two different R8 discriminator values: the outer standard map is **3**, while the inner standard emitter is **2**. The outer method is shared with three other flows. Its upstream is a merged CombineUnsafe instance with discriminator **2**, exactly five Flow inputs, and the `WifiViewModelInject$wifiStandardFlow$1` transform. The suite supplies this exact field layout as a host adapter; the upstream scheduling is a real library Flow. Android Wi-Fi/network inputs and the producer's complete gate/mapping branches are explicitly transcribed fixtures, not an execution of the vendor implementation.

The module preference is `prefs_key_system_ui_status_bar_icon_wifi_standard`:

* `0`: original behavior. No OS4 hook is installed.
* `1`: preserve the original five-input producer (enabled, forceHidden, network, metered, slave state), and bypass only the later map that changes standards 4/5 into 0. The original upstream receives the same collector and continuation. A suspended result is returned unchanged; a synchronous result is normalized to target-loader `Unit`, as in the original outer method. Upstream exceptions and cancellation are unwrapped and propagated without re-collecting.
* `2`: the original standard producer runs completely; only a successful boxed Integer result becomes 0. This is before the common `stateIn` used by both the standard-number view and `wifiInoutLeft`. Independently metered arrow placement is retained. Other return types, the suspension marker, and original failures remain intact.

The setting changes require restarting SystemUI. No new live preference listener is installed. OS3 keeps its previous DexKit path and complete legacy init body. This does not assert OS3 physical-device validation.

## Reproduce

Run from the repository root, using a new output directory on every execution.
See `tests/PORTABILITY.md` for toolchain and external input configuration.

```sh
python3 tests/os4-wifi-standard/run.py --output acceptance/java-test10/wifi-v1/host-new --negative-controls
python3 tests/os4-wifi-standard/check_rom.py --output acceptance/java-test10/wifi-v1/rom-new
python3 tests/os4-wifi-standard/compile_android.py --output acceptance/java-test10/wifi-v1/compile-new
```

`check_rom.py` rechecks the pinned OTA APK / DEX hashes and twelve complete manually reviewed method bodies against `rom-reviewed-methods.json`, plus UI/registration linkage and the unchanged OS3 init body. The input full classes can be regenerated with `acceptance/java-test10/candidates-v1/extract_rom.py`. These static contracts are separate from the host callback suite. The twelve in-memory method changes test the static checker, not Android behavior.

The SDK compile uses the real Android 37 / API102 / EzHook / DexKit jars and frozen test9 peer classes, without Android stubs. The runtime suite has explicit adapters for hook installation, preferences, version selection, the R8 target shape, and Android producer inputs. The simulated prior skipped callback uses the actual HookParam/chain but an adapter injects the preceding result; it is not a multi-module LSPosed test.

The final author host batch has 81 cases and 15 source-copy negative controls. Earlier `host-v2` failed to compile a fixture that incorrectly declared checked Throwable on the callback interface; `host-v3` exposed an invalid test assumption that two `yield()` calls complete a derived stateIn. The fixed test waits on actual Flow emissions under a timeout and cancels its own SupervisorJob. Those attempts remain as history and are not counted as passing runs.

Physical device, LSPosed injection, actual status-bar visibility/layout, and long-duration stability remain **NOT_TESTED**. On the target phone, exercise modes 0/1/2 using Wi-Fi 4/5/6/7 when available; also test Wi-Fi disabled/disconnected, metered connections, slave Wi-Fi, carrier-merged/forced-hide cases when supported, screen lock/unlock, configuration changes, and repeated SystemUI restarts. Mode 1 retains the original producer's suppression in those states; it is not an unconditional number generator. Restore mode 0 and restart SystemUI to return to original behavior.
