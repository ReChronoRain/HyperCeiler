# Early launcher transition protocol

HCU1 is a module-owned control protocol carried by the platform WindowManager Binder. It does not change HCL4 or HCPC configuration framing.

- Request transaction `0x00484355`, interface `android.view.IWindowManager`
- Operation zero queries availability, one registers a strong listener Binder, two unregisters
- Registration is synchronous and restricted to the UID and PID of the bound real launcher window
- Reply uses the Android exception header, then three int32 values, ACK `0x48435531`, version one, availability
- Availability minus one means the platform start source is unsupported, zero means the window is not ready, one means ready

Optional operation three requests native Dock ownership and four releases it, both carry a positive int64 renderer epoch after the operation. Their reply appends the echoed epoch after the original three int32 fields. Zero means pending or unavailable, one means admitted or released. Operations zero through two retain their original wire layout. A registered listener owns the lease, and its death, unregistration or window retirement releases it. Requests and callbacks from a previous epoch cannot release a later admission.

The listener descriptor is `com.sevtinge.hyperceiler.home.UnlockTransition`. All callbacks are one way and only system UID 1000 is accepted.

| Callback | Payload |
| --- | --- |
| One | int64 uptime milliseconds, int32 original transition flags |
| Two | Empty, the window binding was retired |
| Three, optional extension | Boolean known, boolean app visible |
| Four, optional renderer extension | int64 renderer epoch, boolean native owner |

The first three callback layouts and operation zero through two request/reply layouts remain unchanged. An older client may ignore callback three. When an older server does not publish visibility, the new client leaves visibility unknown and preserves the existing capture behavior. Unknown with visible true is invalid. No event body permits extra trailing fields. Window removal invalidates the registered listener and its cached visibility. An authenticated retirement advances a local window generation. The material control worker clears its frame target and releases the old base, host and window before acquiring the current window; this does not add a protocol field or per-frame query. Inactive listeners and non-system callers cannot advance that generation. Epochs must be fresh, monotonic and not in the future.

The server captures the early timestamp before asynchronous dispatch, keeps all remote Binder work out of the WM lock, and emits visibility only on real platform edges. It does not query preferences, draw, poll traversal or own a frame clock. The native callback publishes scalars and wakes a control worker. Material pause, resume and fresh capture run on that worker.

`run.py` compiles the actual Java endpoint against the Android SDK and produces `android-tests.jar`, containing DEX rather than JVM bytecode. Set the device copy read only before running under the actual launcher UID, Android rejects writable DEX. `receiver_device_test.cpp` compiles the actual NDK production receiver. Run it under UID 1000 and a non-system UID to cover both acceptance and refusal. Its optional wire mode emits eight typed unlock Bundles, which the Java device test decodes using the real Android Bundle implementation.

These independent tests do not install a system Hook or register a real launcher window. Real system installation, callback execution, material lifecycle and visual/performance validation remain separate device checks.

The renderer coordinator suppresses legacy creation while retirement is pending and admits native display only after the legacy compositor transaction commits. Native detachment must commit before requesting release. `BackendCoordinatorTest` exercises the production coordinator with its deferred completion port; it does not establish physical compositor retirement. The independent Android endpoint and NDK receiver tests cover admission payloads, epoch binding, lifecycle cleanup and sender authentication. Physical retirement, no duplicate visible layers and restoration require the opt-in `app-native` device test. The legacy backend remains the default. The unmigrated Xiaoheizi capsule currently prevents native admission.
