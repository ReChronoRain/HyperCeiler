# Native launcher input capability probe

The receiver callback inventory is also read from the actual factory argument and resolved through ELF relocation metadata. The reader supports ELF64 RELR and Android's original dynamic-tag aliases, including live load-base normalization. Its address/bitmap encoding follows the [AOSP linker decoder](https://android.googlesource.com/platform/bionic/+/3865c8f942ca7e8cbf9de56741edce8a97a865b1/linker/linker.cpp). This identifies an SDK-owned table, and does not yet authorize constructing a foreign callback object.

The carrier destructor contract additionally follows the constructor's actual foreign-carrier copies into the SDK allocation, then the SDK adapter's carrier to prefix to base-vtable to drop-thunk chain. Carrier size, field positions and forwarded argument position are derived from these producers and consumers. AAPCS64 pointer width and integer argument registers are explicit native ABI constraints, not Dart heap rules. Resumed traces assign identities only after proven pointer loads, discard untransferred stack values, and reject unknown instructions, non-stack writes, incompatible forwarding, zero-register pointer loads and overlapping carrier fields. Negative tests damage individual loads, the tail call and carrier copies. Positive tests shift actual base and drop fields, so accepting only the current numeric offsets cannot pass.

The event bridge contract discovers relocated methods until the adjacent carrier table, then identifies the two owned-event adapters by their proven identity conversion and four-argument forwarding. It derives the foreign prefix presence bit, flag byte and method entry offset, and checks that receiver and runtime arguments retain separate identities. The supported trait has five methods and two such event adapters; a changed trait is refused. Tests damage argument forwarding, presence branches, identity conversion and tail calls; positive tests relocate foreign method entries. This proves the adapter ABI, without deciding which incoming event belongs to a gesture or authorizing event completion on the manager handle.

Run from this directory, with the read-only SDK corpus as the argument:

```sh
python3 run.py ../../handover/artifacts/native-runtime-20261103/sdk-readonly
```

The receiver result contract also needs the actual Bundle and graphics private dependencies and the sys image, because its runtime dereference and owned error destructor are defined outside the original four images. Run its separate real-corpus checks from this directory:

```sh
python3 receiver_run.py ../../handover/artifacts/native-runtime-20261103/sdk-readonly ../../handover/artifacts/native-runtime-20261104/sdk-receiver-readonly
```

`home_native_input_receiver.h` resolves both result variants on all four returned paths, the reference to Shared Runtime, the complete owned-manager copy and matching public drop allocation, and the relocation-backed byte-vector error destructor. It rebases the actual post-format frame aliases onto the native Vec and checks that the returned pointer, length and capacity moves invert the selected error destructor's moves. A corrupted word order is refused even when the total result size is unchanged. The current corpus's result and manager sizes are parser results, not sizes supplied to an SDK allocator. Tests accept shifted discriminant fields and reject incompatible indexed writes, incomplete manager copies, changed error field moves and damaged destructors.

The channel contract proves the monitor's returned Option is borrowed and the receiver factory clones its present payload. The completion contract derives its nonzero success status and packed error flag; a successful finish must not be interpreted as integer zero. The dispatch contract enumerates bounded acyclic witnesses to the SDK's indirect call sites, rejects contradictory constant branches, and connects allocated/copied event types to matching SDK get-ID/drop interfaces and actual callback slots. Lock retries and input-draining back edges are not unrolled. The two other automatic-completion sites and the pending-batch byte distinguish scalar callback roles; actual boolean and float argument producers distinguish focus from drag. No callback slot or object-field address is fixed. A positive mutation swaps the key/motion dispatch slots and requires both bindings to follow that swap. Negative mutations corrupt borrowed-receiver arguments, completion packing/arguments, channel discriminant width and scalar boolean/float forwarding. Private linkage renaming is repeated on the seven real images.

`debug.hyperceiler.home_input_receive=1` is a separate opt-in, one-shot receiver probe. On the SDK worker it creates a named monitor, borrows its channel, transfers a module-owned listener carrier to the actual receiver factory and holds the returned manager for at most twelve seconds. Each owned event is queried, finished once through the callback-provided borrowed receiver/runtime, and dropped once through its matching public SDK destructor. Focus/drag completion remains with the SDK. Pending batches use the validated SDK consume-batches call and negative frame time; [AOSP InputConsumer::consumeBatch](https://android.googlesource.com/platform/frameworks/native/+/f3213d2ca8/libs/input/InputTransport.cpp) defines this as draining all queued samples. That source does not prove a vendor implementation works; live event/finish/drop acceptance is still required.

The read-only receiver probe never calls pilferPointers, injects input, invokes gesture actions, stores event objects, or logs event IDs/coordinates. It records only aggregate counts. Shutdown drops the SDK manager before the monitor, then the SDK foreign destructor deletes only module-owned carrier state and wakes the worker once. There is no owner-release polling or failure retry loop. Default routing stays unchanged. Device acceptance must require nonzero real motion events, equal event/finish/drop totals, zero completion errors, exactly one carrier-owner destructor, a surviving launcher, absent named input channel/surface and restored experiment properties/preferences.

The test compiles the production semantic resolver with address and undefined-behavior sanitizers. It checks the actual public SDK monitor factory, its success/error return paths, Box allocation and copy, and the matching public destructor. It rejects damaged copy size, error packing, factory return and destructor. It also repeats the checks after renaming private linkage and rebuilding GNU hashes. Public C anchors locate the interface, while instructions establish the supported ABI and ownership. Current corpus sizes are results of parsing, never runtime constants.

The opt-in `debug.hyperceiler.home_input_probe=1` path runs once on the existing SDK control worker after the real runtime Context is available. It first invokes the two actual SDK event adapters with distinct module-owned opaque tokens, then the actual SDK carrier destructor adapter using module-owned storage and a module-owned drop thunk. Both event callbacks must forward the correct arguments and the drop thunk must be called exactly once with the derived carrier argument. The identity conversion proof is required before passing tokens; they are never presented to SDK event getters/destructors. No SDK Box is fabricated or freed by our allocator. It then creates a named monitor and immediately destroys it through the validated SDK destructor. The success log includes `carrierDrop=verified eventBridge=verified`. It installs no input receiver, calls no pointer-pilfer operation and retains no monitor. Turning the property off resets the one-shot state. Default feature routing remains unchanged.

Offline checks and installed permission grants do not prove a real input API call succeeds. Device acceptance must confirm the create/release log, a surviving launcher process, no remaining named monitor/channel/surface, and restored properties and settings. Successful probing does not implement native gesture detection, pointer ownership or action routing. Those require their own receiver ABI contracts and backend exclusion before replacing the system implementation.

The extra-motion observer additionally requires `home_native_motion_view.h` to
prove that the SDK's public MotionEvent pointer-count adapter forwards exactly
the same pointer through identity-only helpers to the typed platform
`AMotionEvent_getPointerCount` import. A shifted/dereferenced pointer or damaged
tail is refused; a neutral instruction variation remains accepted. This permits
borrowing the existing object with NDK input getters, not freeing it through
NDK. Event completion and destruction still use the validated SDK interfaces.
No second private MotionEvent layout decoder or duplicated launcher PLT binding
is introduced. NDK timestamps are nanoseconds and are converted to uptime
milliseconds before the existing extra gesture state sees them. The monitor is
explicitly bound to display zero; this observer is not a general display bridge.

Only when both `home_input_receive` and `home_extra_probe` are selected does the
12-second observer feed actual samples and the current immutable geometry/config
snapshot into `home_extra_gesture_observer.h`. It counts observations/decisions
without logging coordinates or event IDs, pilfering, marking events handled or
emitting actions. A separate event-driven monotonic wait delivers long-press
observations; it never shares the Provider worker's blocking IPC. Timer delivery
rechecks the current memory snapshot and is cancelled on gesture invalidation.
The SDK owner destructor joins it before publishing final counters, and logs
aggregate timer/long-press/lateness counts. Decisions remain read-only evidence,
not full gesture acceptance. A replaced producer,
configuration/timing change, lost geometry, or changed input identity clears
recognition state. Production backend exclusion and action integration remain
separate work.

`python3 pilfer_run.py <sdk samples> <receiver dependency samples>` additionally
requires `libbinder_rs.dylib.so`. The public `input_InputMonitor_pilferPointers`
returns void and swallows failures. `home_native_input_pilfer.h` follows its
transparent adapter, derives the monitor data/vtable fields and actual virtual
slot within the proven owned monitor size, and checks the nullable X0 return
branch. Each non-null path must retain that same status word and unwrap it into
NDK `AStatus_delete`; success and absent-host paths cannot own an error status.
The optional log branch does not define success. Tests follow shifted data/slot
loads and refuse a removed call, inverted result branch, shifted status word or
damaged destructor tail, then repeat with private linkage renamed. These are
value/ownership contracts only. No checked runtime invocation or input backend
handoff is enabled, and this test never invokes pilfer.

`hc_native_input_receive_retire()` is called only by the SDK control worker.
It releases manager/monitor and reports success only once the foreign carrier
and its timer are gone. The handoff client invokes it before releasing an owned
extra-gesture lease, including uncertain transport or configuration loss.
`home_extra_handoff` is a default-off control-plane experiment; an acknowledged
lease alone does not make the read-only observer consume/pilfer input or execute
an action. Source/owner death and timed lease expiry have independent
fallback semantics; queued legacy touch actions are invalidated before the old
worker barrier is acknowledged. Shake remains with the existing sensor backend.

`home_extra_execute=1` adds the opt-in actuator to the active input/snapshot/
handoff probes. It takes a V2 owner lease before observing eligible input,
checks memory-only peer death/revocation/freshness and deadline before every
claim, and queues typed 9..12 actions on the SDK worker. `CheckedPilfer` reads
the actual monitor fields and live relocated virtual target from the verified
semantic contract on setup. The target must belong to a captured SDK executable
segment. A non-null status is consumed via `AStatus_delete` and rejects the
claim; the public void SDK adapter cannot authorize a gesture. The actuator is
closed before dropping the monitor even if carrier teardown is asynchronous.

`android_pilfer_test.cpp` calls the same live binding and invocation in an
independent Android process with a temporary receiverless monitor. A passing
null status proves the actual ABI for that owned monitor, without proving real
pointer ownership or the user-facing action. Run the phone probe only during
an authorized no-touch pause. Full device acceptance separately checks one
real claim, one privileged dispatch, actual visible effect, equal event/finish/
drop counts, immediate preference revocation and complete cleanup.
