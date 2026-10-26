# OS4 application process prelaunch

The OS4 Other page admits the existing `home_other_disable_prestart` switch.
The old native property filter remains for the legacy property reads, but is
not considered proof that the current Dart process-prelaunch path is blocked.

Production discovery inventories executable PT_LOAD instructions from the
verified current Home image. It admits one synchronous nullable-void consumer,
its four-argument process bridge and five-field FRB tuple, the shared tuple
allocator, and the boolean-property/await initializer publishing the same
static-table flag consumed by that function. The two captured images resolve
different function addresses and flag offsets. Private symbols, debug sections,
fixed function addresses, cached instruction offsets and pool displacements do
not participate in selection. Missing or ambiguous graphs retain original
behavior and emit a rate-limited binding reason.

The entry intercept is before the managed frame and displaces no Dart call.
When authorized and ready, it returns the exact canonical null used by all
proved original normal returns. Otherwise it resumes the original body through
the existing checked hook bank. It creates no Future, writes no managed object,
allocates no Dart heap, calls no Dart slow path and retains no managed pointer.
This blocks application **process prelaunch**, including calls from the original
pointer-acceptance path; it does not intercept the separate visual pre-start
transition, real application launch, cancellation or animation cleanup APIs.

The setting uses the existing preference key and authorization bit 4. Binder
packet lengths, provider schema and cache formats are unchanged. The Hyos
backend checks the current authoritative lease at every intercepted call;
expired/unavailable authorization cannot be revived by an old JNI value. The
ordinary LSPosed-native backend follows its existing JNI configuration path
(restart the launcher after changing the switch). No new polling worker is
created. Late image discovery and hook repair use the existing layout worker.

`HyperCeiler.HomeLayout` INFO records report graph binding plus `requested`,
`ready`, actual `hits` and actual `blocked` counts on the existing maintenance
cadence. WARN records explain missing/ambiguous graphs or unavailable binding.
This tag is already imported by HyperCeiler's built-in log collector. Installed
and ready are distinct from actual interception, rendering and interaction.

Run from the repository root, entirely offline after providing the optional ROM
images and SDK/tool dependencies:

```sh
python3 tests/prestart-os4/run.py
python3 tests/prestart-os4/run.py --image7751 /path/to/7751.so --image8583 /path/to/8583.so
/path/to/python-with-unicorn tests/recents-layout-os4/verify_arm.py /path/to/ndk
JAVA_HOME=/path/to/jdk python3 tests/hyos-control/run_java.py --output /tmp/prestart-endpoint
python3 tests/hyos-control/run_native.py --sdk /path/to/pinned/zygisk-next-api --output /tmp/prestart-control
```

The captured-image tests remove all symbols/sections, independently relocate
owners, move frame/static fields, reject missing members, inconsistent flag
producers, value-returning decoys and ambiguous roots. They run the shipping
entry policy for all readiness/authorization combinations and repeated toggles,
and compile the actual request getter for both native backends. The shared
ARM64 gateway regression executes both prestart routes with adversarial native
register clobbers and checks all GPR/SIMD registers (except continuation scratch
x16), native SP alignment, Dart stack preservation, flags and FP control state.
The endpoint/control tests cover authoritative preference delivery, retirement,
lease expiration and stalled Binder separately.

Local validation on 2026-10-24 passed both complete images (7751 and 8583), entry
policy and UI checks, 42 ARM64 gateway cases, 65 JVM endpoint checks, the recents
regression and log diagnostics. Release APK compilation passed. The endpoint
suite's obsolete schema-2/first-key-type assertions were updated to the current
provider schema; production protocol was not changed for this feature.
The control/lease suite passed 181 cases, including a restored loader-bootstrap
route using the same neutral-on-unavailable combined snapshot. Its Darwin
clock/thread fixtures adapt host ABI differences without changing Android code.

The separate experimental Hyos companion was also cross-compiled. This exposed
an existing incomplete conditional configuration branch, now repaired without
reinstating a disk/JNI authorization fallback. All its changed translation units
compile, but the complete companion still fails linking on the existing missing
`home_startup::detail::{binding,initialize_consumers,publish_consumers,clear_consumers}`
bridge. No placeholder was added and no successful companion build is claimed.
The ordinary APK Release builds successfully; it is a separate build target.

Device status: **NOT_TESTED**, as requested. Before claiming effectiveness,
compare disabled/enabled states after restarting Home, confirm actual hits and
blocked counts and the presence/absence of process-prelaunch requests, exercise
desktop/folder/drawer launches and long press/drag/cancel, confirm original
behavior returns when disabled, and check built-in logs. This intercept does
not disable Android's independent system-wide process-pool service.
