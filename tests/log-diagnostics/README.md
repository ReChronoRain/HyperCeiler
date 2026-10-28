# Failure diagnostics

```sh
python3 tests/log-diagnostics/run.py
python3 tests/log-diagnostics/lspd_load_failure.py
python3 tests/logcat-import/run.py
python3 tests/independent-object-scale/run.py
```

The tests compile the production diagnostic writer against Android/DAO stubs and
run it with no ordinary AndroidLog implementation on the classpath. They cover
Root denial/exception/recovery, deduplication, environment identity, database-failure
fallback without recursion, and diagnostic records surviving refresh/export SQL.

The C++ fixture uses the production binding-failure reporter and trailing endpoint
metadata reader. It checks per-feature/stage deduplication, old snapshots, missing,
malformed and future metadata, capability presence, and Java/native constants. The production
shell-copy method is compiled from the Java source and run in disposable directories for
empty/success/cp-failure/chmod-failure cases. The capture path tries direct root, `/proc/1/root`,
and both common `nsenter` syntaxes, and accepts both `modules_*.log`/`verbose_*.log` and legacy
`modules.log`/`verbose.log` names. Health checks accept both raw LSPosed timestamp headers and
HyperCeiler export page markers. A failed staged capture leaves the previous app-local copy intact.

In the built-in **Xposed** log tab, refresh with the level/tag filters set to All:

- `LogSync.Root`: usable Root, denial or check exception.
- `LogSync.Logcat`: collection success, command exit + bounded stderr, interruption or exception.
- `LogSync.HomeCoverage`: whether launcher-native records remain in the current buffers;
  absence does not prove that hooks never ran.
- `LogSync.LSPosed.*`: the successful access strategy, missing/empty directories and copy/chmod
  failures; failed staged captures preserve the previous database logs.
- `LSPosed.ModuleLoad`: APK open/parse failures from verbose logs, including their
  original severity and stack. These are scanned even when modules logs exist;
  ordinary module records are not imported twice.
- `LogSync.Environment`: installed module/launcher versions, update times and Android build;
  installed versions do not prove that running processes loaded them.
- `HyperCeiler.HomeConfig`: transport, accepted endpoint revision/capabilities/PID,
  missing old extensions, version mismatch and live/cache/fallback config source.
- `HyperCeiler.HomeLayout`: six scale binding families report the unavailable symbol,
  producer/consumer instruction guard or replay-window failure, only when the failure
  stage changes. Requests include switches/percentages even at neutral 100%.

Diagnostic collection events are persisted directly to Room, independently of the
ordinary logging-level preference. Native records are still imported from logcat;
expired native buffer records cannot be recovered. No reboot, hook-guard relaxation,
extra background poller or database schema migration is introduced.

## Device installation

Use `adb install --no-incremental -r <release.apk>` for module testing. On the
October 19 device boot, HyperCeiler's APK was on `incremental-fs` under
`/data/app`; LSPosed reported two early-boot APK parse failures with `No such
file or directory`. The WMS endpoint was absent and SystemUI reported
`Dock broker not installed`, leaving Dock icons visible without the background.
Launcher-side cached settings cannot restore an absent system_server hook.
Check the replacement APK's filesystem and, after an authorized reboot, verify
the WMS endpoint, glass layer and screenshot instead of relying on install success.

The non-incremental release (`0fd7557ff83e1575d89ad7d7ae357985a16936f9ee50512d60aad4d4490614d9`)
was installed with settings preserved. After the authorized reboot, its APK
remained on f2fs, the WMS state transaction returned `0x48435331 / 1`, and
SurfaceFlinger listed Dock tint/blur/glassLeash/glass layers. Native diagnostics
reported `ENDPOINT_READY` with revision `20261019`, PID `3694`, and live config.
The device screenshot confirmed the rounded glass Dock background was visible.
