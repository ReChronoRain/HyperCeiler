# OS4 network refresh scheduling regression

Run `python3 tests/os4-network-spacing/run.py` from the source root. Use `--source`
to run the same tests against an earlier `NetworkSpeedSpacing.java`.

This compiles the actual production Java file and invokes its registered callbacks.
Fixtures provide a deterministic host Handler queue and Looper identity; three
cases also use two physical Java threads with latches/join. The fixtures model the
OS4 ROM's merged UI/background Handler class, UI visibility cancellation and
immediate restart, and BG `!mIsStatusBarHidden` condition. They do not execute ART,
Android MessageQueue, LSPosed, Xiaomi SystemUI, battery or long-duration workloads.

50 cases cover:

- Legal UI intervals 10..100 (100 ms units), invalid imported values and the older
  direct `postUpdateNetworkSpeedDelay(4000)` path. Delay 0 and other values remain.
- Exact Handler/controller identity and refusing the unverified separate-class
  fallback, unrelated messages, hidden/invisible cancellation, and the previous
  hasMessages/cancel/send race.
- UI generation changes before and after control messages 100001/2/3/5, preserving
  0 ms restarts, and BG work starting inside a UI control method.
- An original BG method that itself overwrites a concurrent 0 ms restart retains
  its original 4000 ms delay; the module declines stale retiming and does not
  replace the ROM scheduling implementation.
- Latest BG completion wins, pending UI tasks coalesce, elapsed queue waiting time
  is subtracted, and running BG work causes an older UI task to yield.
- Failed/throwing post, preference/field exceptions, failed/skipped original calls,
  before/after wrappers sharing the real InvocationContext argument array, nested
  unmatched after calls, and wrong-Looper execution.
- A second physical BG thread can complete while UI preferences are read; this
  detects preference IPC accidentally moved inside the state lock. Physical-thread
  cancellation and restart tests bracket original BG/after execution with latches.

Current UI-serialized implementation: 50 passes, 0 failures. Historical test2
candidate with only BG identity/hasMessages guarding: 31 passes, 19 failures under
this expanded suite. Earlier 20-pass evidence did not cover the cancellation gap.

ROM basis outside the source tree:
`acceptance/systemui-evidence/NetworkSpeedController.java`, full DEX cross references
`acceptance/network-cancellation-xrefs.json`, and `acceptance/network-spacing-evidence.json`.
The target lacks `postUpdateNetworkSpeedDelay`, uses `NetworkSpeedController$5` for
both handlers, and all statically visible cancellation/restart paths run on mHandler.

The guarantee assumes the audited ROM and normal paired callbacks from the pinned
EzHook 1.3.0 dependency. Arbitrary third-party reflective field/queue mutations,
forced missing after callbacks, real-framework execution, and timing measurements
remain outside these host tests. The fallback deliberately retains system timing
when UI and BG Handler runtime classes differ.
