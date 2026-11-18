# Extra gesture snapshot wire

Run `python3 run.py` from this directory. The test serializes the production
Java `HomeExtraGestureWire` through a little-endian Writer, then feeds those
bytes to the production native `read_snapshot` under ASan/UBSan. It checks
131 frames across masks, rotations and optional geometry/handle states,
all 12,880 truncated prefixes, invalid headers, trailing data, future and
expired publications. No Android mocks or second protocol implementation.

HGE1 is a module-owned protocol separate from the frozen five-bit HG1 config.
It carries one immutable producer/revision/timing/mask/geometry snapshot.
Publication uses Android uptime / CLOCK_MONOTONIC, with a 1500ms maximum age.
A producer ID scopes window-owner generations; recognition must reset across
producer, revision or owner changes. Unavailable and closed are distinct.

The production integration is a default-off ten-second read-only probe using
`debug.hyperceiler.home_extra_probe=1`. SystemUI publishes actual navigation
view geometry; the module Provider only discovers its fixed typed endpoint.
The native reader queries on the SDK worker and exposes an immutable memory
snapshot. This does not authorize input ownership, pilfer or actions.

Device acceptance must observe actual accepted geometry samples, process
survival, restoration and absence of preference changes. A passing wire test
alone does not prove SDK transport, current geometry or gesture behavior.

`HandoffTest.java` uses the production handoff state machine, with explicit
monotonic timestamps, to check prepare/drain/confirm/renew/release, producer and
revision checks, stale completion, owner death, invalid configuration/geometry,
expiry and the bounded startup quarantine. Configuration loss denies renewal
immediately but does not restore the old backend before a granted lease has
ended or its native owner has explicitly retired/died.

`ControlGolden.java` serializes actual Java `writeGrant` output; the native
production reader checks 18 control frames and all 792 truncated prefixes,
status/metadata agreement, lease lifetime and complete length. Snapshot QUERY1
is unchanged. CONTROL2 is a separate module-owned protocol, only Home UID may
mutate it. The fixed owner token supplies lifetime, never arbitrary commands.

The default-off `home_extra_handoff` experiment retires the old receiver on Main
and waits for the old action worker FIFO barrier before confirming a lease.
Native retirement must finish the SDK receiver/timer/carrier before RELEASE.
Snapshot/renewal due times drive the event worker directly; a long layout scan
must not postpone them. No scheduled deadline means an indefinite event wait.
Configuration bootstrap still honors its own hold/backoff on these extra wakes.
The read-only control-plane experiment still finishes unhandled and never
calls pilfer or actions. Execution is separately opt-in. CONTROL V2 requires
an invalidatable owner and typed action broker; it deliberately refuses the
old control service, while snapshot QUERY1 and the WMS action protocol stay
unchanged. `LeaseGolden.java` and `lease_test.cpp` compare 19,200 actual Java/
native authorization cases and cover the live lease margin, peer death and
revocation preceding a late grant ACK. Root remains a read-only snapshot caller;
only the actual Home UID and owner token may request the fixed action codes.
