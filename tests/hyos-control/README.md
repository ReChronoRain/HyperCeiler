# Hyos control and authoritative preference regressions

These tests compile actual production sources. They never inject the launcher, communicate with a device, or replace production algorithms with copied ones.

From the source directory:

```sh
python3 tests/hyos-control/run_native.py --sdk /path/to/pinned/zygisk-next-api --output /new/output/native
python3 tests/hyos-control/run_joint.py --sdk /path/to/pinned/zygisk-next-api --output /new/output/joint
python3 tests/hyos-control/run_java.py --output /new/output/java
python3 tests/hyos-control/run_legacy_java.py --output /new/output/legacy
```

The native runner requires a C++20 host compiler and the pinned official Zygisk Next API header. The Java runners use JDK 21 or newer; `JAVA_HOME` selects it. Every runner writes production file hashes, command/output, and a nonzero exit on failed checks.

`control_test.cpp` includes actual `hyos_control.cpp`. Binder transport, `native_init`, report installation, Android logging, pthread creation, and the monotonic clock are controlled fixtures. Actual `std::thread` threads execute the production poll and expiry functions with deterministic gates. The infinite production loops are not started on the host: worker scheduling is controlled so blocked transactions, initialization and publication can be released and joined. Coverage includes bounded application startup, loader recording without IPC/thread creation, queued image notifications, disabled/unavailable/recovered snapshots, all 64 flag combinations, malformed replies, parcel and service reference release, initialization retries, failed worker creation, stalled Binder, stale reply rejection, lease expiration and publication/watchdog races. The latest completed native suite has 110 cases, including five report-retry cases that verify exact feature authorization, ordering after library events, no publication lock, and no new thread.

`run_joint.py` links actual `hyos_entry.cpp` and actual `hyos_control.cpp` as separate translation units. Its 11 cases execute the production loader callbacks, body and runtime guards, specialization, immutable concurrent binding, authenticated prepare function, and worker/control path together. Loader/dlsym paths must create no worker and perform no Binder query. A blocked first query must still let prepare return the original entry pointer in about 150ms; repeated entry calls cannot create replacement polling threads. Two 64-thread cases verify competing targets/runtime images cannot mix the published pair. Actual host `safe_read` is used. The assembly entry is only a marker in this joint fixture; the real ARM64 tail-call/register ABI is separately executed by `tests/hyos-entry/run_arm64.py`. Neither suite proves the real Hyos spawner or launcher lifecycle.

`HyosEndpointTest.java` compiles the actual endpoint, actual `HomeLayoutPrefsSnapshot` provider row builder, and shared framework gate. The ContentResolver fixture delegates row construction to that actual builder using one `SharedPreferences.getAll()` snapshot. Tests cover schema/typed fields, freshness, identity binding, gates and metadata, no stale restored authorization, caller checks, failed provider reads, and revision/retirement races. The extended Java suite is maintained with the shared framework gate; use its output for the current count.

`run_legacy_java.py` runs the three pre-existing layout tests. The legacy fixture was extended to serve text/long metadata with real types; all-key fault injection remains enabled. Its provider-shape assertion now follows the actual schema's exact number of keys rather than the old hard-coded 37-column size. The legacy endpoint test, ten idle snapshot cases and ten atomic reload cases must still pass.

The first implementation passed 91 ordinary native cases but failed the separately added stalled-Binder case: after 5.1 seconds it still reported `allowed=1` and options `30`. That historical failure is preserved in `validation/hyos-control-blocking-v1/`; ordinary passing counts were not treated as proof against that defect. The revised controller uses one Binder worker and one independent lease watchdog, with at most 150ms first-poll waiting at application entry. No Binder, native initialization, or worker creation occurs in the image-recording path.

The local lease uses CLOCK_MONOTONIC and lasts five seconds. Feature getters reject expiration immediately. A watchdog runs at nominal 250ms intervals to clear raw Dart/native option booleans; actual scheduling delays can add latency. Publication and expiry serialize under a short lock, whose runtime-options sink must contain only atomic operations. A separate completion lock bounds startup waiting even while publication is stalled. Replies from a query taking at least five seconds are discarded. A stuck Binder call retains one worker; repeated entry calls do not create replacement polling threads.

Both native threads intentionally live for the Home process lifetime. There is no supported live unload/dlclose of the companion; process exit reclaims its threads and storage. A startup timeout means not ready, and does not prove that startup-only Dart layout sites were patched. The root entry/hook tests and subsequent device acceptance must establish hook installation, LSPosed/Zygisk authorization, actual Binder transport, UI behavior and long-running stability separately.
