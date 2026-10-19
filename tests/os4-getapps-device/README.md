This suite compiles all four production GetApps files: the typed configuration, preference fragment, app route and constructor hook. The hook runs through the actual EzHook 1.3.0 and libxposed API 102 callback chain. Android preference widgets, the `Build` write backend and MarketApp consumers are host adapters, with the initialization and consumer projections documented against complete extracted OTA methods. They are not execution of the original ROM DEX.

From the workspace root:

```text
python3 -B source/tests/os4-getapps-device/run.py --output <new-directory> --negative-controls
python3 -B source/tests/os4-getapps-device/run.py --output <another-new-directory> --original
python3 -B source/tests/os4-getapps-device/compile_android.py --output <new-compile-directory>
python3 -B acceptance/java-test12/getapps-v1/rom/extract.py
python3 -B acceptance/java-test12/getapps-v1/rom/review.py
```

The two ROM commands regenerate the author's extraction/review files; copy the evidence directory to a new location and adjust the workspace root before using them after archival. The three test commands require a previously nonexistent output directory and do not edit production. SDK compilation uses the frozen test11 peer bytecode and Android 37, not the host stubs. A full test12 APK build and actual DEX inspection are separate work.

The UI uses the Boolean `market_device_modify_new1`. It defaults to off. The provided APK's DataStore-backed preference initialization persists that default before `initPrefs`, so a stored false value cannot distinguish a first display from an explicit user choice. Migration never infers enablement from the obsolete String enum. All 24 previously implemented presets can prefill only missing text keys; existing Strings, including intentionally empty Strings, remain unchanged. Imported non-String text fields become empty before widget inflation; invalid switch values become false. The three fields stay visible, with enabled state controlled by the existing XML dependency.

Runtime reads one snapshot from the active preferences handle. Only Boolean true enables overrides. Each nonblank trimmed String overrides its corresponding `Build.DEVICE`, `MODEL` or `MANUFACTURER`; missing, blank and non-String values leave that field unchanged. No other Build fields or global OS identity are changed. The exact no-argument MarketApp constructor is hooked before its original body. The original constructor, Client PatchProxy branches and BasicMode privacy checks still run.

Tests cover all preset migrations, explicit off and corrupt types, two conflicting active/physical preference cases, field ordering, caches and startup snapshots, normal constructor exceptions, one-shot write/read failures, readback, foreign-value preservation and persistent rollback failure reporting. The old source failure run is retained. Source-copy negative controls must both compile and fail an actual assertion; compile errors alone are not rejection evidence.

Changes require restarting all GetApps processes via the existing quick-restart menu. Hot reload cannot reconstruct already cached user-agent strings, request maps, Shortplay configuration or static model-dependent lock selection. A persistent field-backend failure or another writer can prevent full rollback; such failures are logged, not called successful restoration. Static ROM evidence and host tests do not establish ART injection, server update eligibility, UI operation on a phone or long-term stability. Physical-device status remains NOT_TESTED.
