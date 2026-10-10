# Weather HWS1 Java bridge regression

This suite compiles the complete production `WeatherPrefsSnapshot`, `SharedPrefsProvider`, `WeatherNativeEndpointOS4`, `WeatherNativeBridgeOS4`, `FrameworkHookGate`, and the unchanged Home snapshot builder. Android interfaces, clock, PackageManager and hook installation are host adapters. Actual Linux `/proc/<pid>/cmdline` is exercised with two short-lived test-owned processes. This is not an Android Binder, ART, LSPosed injection or Weather UI test.

From the project directory above `source`:

```
python3 source/tests/weather-native-bridge/run.py --output <new-output-dir>
python3 source/tests/weather-native-bridge/negative_controls.py --output <new-output-dir>
python3 source/tests/weather-native-bridge/run_ezhook.py --output <new-output-dir>
python3 source/tests/weather-native-bridge/compile_android.py --output <new-output-dir>
```

`run.py` covers typed one-copy preference snapshots, all raw UI values, missing defaults, malformed cursors, shared framework fingerprint policy, absolute query-start BOOTTIME deadlines, whole-record invalidation, bounded read retries, exact caller identity, fixed 48-byte writer, and the actual registered WMS callback. A blocked provider fixture ignores interrupts while twenty retired endpoint generations yield to one classloader-local query worker; no replacement query starts until the old one exits. The suite also verifies Home schema 2 remains intact.

`run_ezhook.py` executes production callbacks with the real EzHook 1.3.0 `HookParam` and `InvocationContext` and libxposed API 102 `Chain` interface, proving before-result assignment skips the original and unrelated transactions still proceed. The physical hook installation and Android transport remain adapters.

`negative_controls.py` modifies separate copies of production source and requires runtime assertion failures, not compilation failures: caller verification, lease start, global permission, default level, raw level preservation, publication identity, retired query suppression and the single-worker exclusion.

`compile_android.py` uses Android 37, real API 102 / EzHook / Kotlin dependencies, frozen test5 peer class files and this project's hidden-API signatures. It compiles the full SystemFrameworkB and SharedPrefsProvider and saves SystemFrameworkB bytecode showing the separate OS4 installation. It does not build or validate an APK.

The corpus is emitted by the production `writeReply` method through the host Parcel adapter. Every binary has exactly 48 bytes. JSON identifies observed BOOTTIME for active, default, disabled, unavailable and delayed-expired envelopes. No frame was captured on a phone.

HWS1 transports raw device level (default 3; low/middle/high 0/1/2), card values 0/2/3 and blur 0/1. It makes no +1 conversion. Transporting card and blur does not claim native implementation. Native expiry cannot undo an already cached Dart decision; current ROM consumer evidence indicates Weather may need a process restart for changed device level to be read.

Retirement is permanent for an endpoint and does not preserve its Map or authorization. If a subsequent unrelated hot-reload cleanup rejects the reload, this retired Weather route remains unavailable until a replacement generation or process restart. A single static worker prevents accumulation only within the same module classloader. A framework that replaces the entire classloader can create another independent lane while an uninterruptible old Binder query remains; this is an explicit unresolved framework boundary, not a proven global thread bound.
