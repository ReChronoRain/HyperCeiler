# Launcher-owned configuration transport

Run `JAVA_HOME=<JDK 25> python3 run.py` in this directory.

The Java publisher is experimental and disabled by default, selected only by
`debug.hyperceiler.home_config_backend=java`. A module Java entry in a wrapper process
does not prove the actual HYOS renderer has ART. The 2026-10-29 device sample
executes `/system_ext/bin/hyos_spawner` without an ART mapping; this Java
publisher was not executed there. The `root-socket` experiment instead uses a
libsu daemon, one physical preference-file revision and the same HCL4 writer.
Both paths are disabled by default, with the existing `wms` path retained.

The Java test compiles the production Provider snapshot builder, endpoint decoder
and shared HCL4 writer against the existing host Android transport fixtures.
It generates enabled and empty-reset packets without starting the WMS refresher.

The native test compiles the actual reply decoder, serializer, local publication
and configuration-query functions extracted from `home_layout_config.cpp`.
It consumes those Java packets and checks independent object scales, geometry,
UTF-16 surrogate pairs, complete resets, every truncated packet prefix, trailing
data, stale revisions, failed publications, deduplicated persistence and exclusive
ownership with zero live WMS queries after local takeover. AddressSanitizer and
UndefinedBehaviorSanitizer are enabled.

The framing test exercises the actual root channel decoder, including every
split point, duplicate sequences, reset on reconnection, malformed headers,
coalesced messages and the maximum allowed payload. The root framing header is
HCPC/version/length/sequence in network byte order, followed by the feedback
profile (flags/type/strength), a v3 selection and unchanged Android Parcel bytes, and
HCAC/sequence/status acknowledgements. The Binder control interface is v6;
its version is independent of socket v4 and the frozen HCL4 payload. Endpoint metadata and
all HCL4 extension fields remain mandatory for a local publication.

Transport and storage fixtures provide Android APIs unavailable on the host.
These tests do not prove Android JNI Parcel conversion, Application lifecycle,
ContentObserver delivery, actual Dart execution or displayed results. Those need
device validation, including root peer credentials, SELinux socket access,
Android Parcel unmarshalling and helper lifecycle, separately recorded under
local `handover/`.

The Java-generated root frames also pass through the production native decoder
and publication function, checking atomic layout/profile publication, neutral
reset, malformed feedback metadata, source ownership and unavailable local state.

`android_action_test.cpp` links the entire production socket transport and runs
on Android as root. It creates private test sockets, never operates the desktop
or vibrates hardware. A peer supplies feedback acknowledgements while actual
`send`, `recv`, `poll`, clocks and `SO_PEERCRED` remain in use. It covers positive,
negative, fragmented and malformed replies, a 200 ms deadline, reconnection and
a child process dropping to uid 2000 to verify rejection. Build against the
project NDK with `-static-libstdc++ -llog`, push the executable to a temporary
path, run under root and remove the executable afterwards.

Actual SDK feedback additionally needs module-owned framework shared memory on
Android 17. The control interface transfers a read-only descriptor. When
ActivityThread has already closed that descriptor, the framework's own nonce
block address selects the caller's existing read-only ApplicationSharedMemory
mapping, and root opens its `map_files` handle. It accepts no caller-supplied PID
or path. It does not allocate substitute memory or alter system process state.
These platform and hardware requirements are validated on-device, not simulated
by the host protocol tests.

## Native recent-card migration groundwork

HCPC v3 adds a network-order selection after the 12-byte feedback profile:
`u32 count`, then `count * (u32 ASCII-byte-length, package bytes)`, followed by
unchanged HCL4. Count `0xffffffff` means unavailable; zero is a complete reset.
Packages are sorted and unique, capped at 1024 entries and 255 ASCII bytes each.
The physical preference reader preserves the existing Provider framing limit and
global authorization gates. An invalid preference selection or an extension
exceeding the combined frame budget publishes unavailable without suppressing
unrelated layout/profile updates. Malformed wire data rejects the whole packet.

The native reader also accepts v2 frames, which explicitly clear selection
availability. Snapshot publication includes layout, feedback and selection under
one lock. Selection identity changes on every accepted publication, even if its
values repeat. The production commit entry checks that identity under the
publication lock, using try-lock so callbacks retain the original response
instead of waiting for cache I/O. A real contending publisher thread is covered. Malformed HCL4 and stale/source-mismatched publications retain the
previous complete snapshot. Selection is absent from disk serialization.

`cards_test.cpp` compiles the production classification and compaction core under
ASan/UBSan. It checks realActivity/component/package precedence, whole-group
removal, retained order/identities, null groups, full validation before mutation,
aliased owned pointers, member/group budgets, neutral settings and 10000 calls.
The access adapter is a typed host fixture; it does not independently prove
SDK layout or displayed results. The separate `home-recent-cards-sdk` suite
covers actual SDK producer/consumer/allocation contracts and owned reply access.

HCPC v4 retains the v3 payload, and adds a native-ready bit to accepted ACK flags.
Flags are 0 rejected, 1 accepted, 3 accepted/native-ready. A repeated sequence may
only change that capability bit while remaining accepted. The service reads the
published sequence after the blocking read, rejects regressions and duplicate
ACKs, and changes the unique execution-owner token only at real transitions.
The complete production Android socket test checks capability changes without
another publication or idle duplicate ACK. A live publication requires an
unexpired generation-scoped 3-second lease; rejected data cannot revive it.
`inspect_local_recent_cards` distinguishes lock contention from unavailable data,
so a busy publisher does not falsely notify the launcher to restore cards.

No platform-library binary is committed. Host, Android isolated-process, and
actual launcher UI results have separate evidence in ignored local handover.


HCPC v5 appends a canonical 40-byte Dock block between card selection and frozen
HCL4: available, enabled, mode, ARGB, heightDp, marginDp, bottomDp, radiusDp,
nightMode, revealStyle, all big-endian words. Unavailable requires the following
nine words to be zero. The real physical Map produces layout, haptics, selection
and appearance together; native publishes one Config. Geometry is clamped once
at the preference boundary and malformed wire data rejects the whole publication.
Dock state is excluded from HCL4 disk serialization. Legacy v2/3/4 decode with
appearance unavailable and preserve all prior fields.

V6 uses the second bit of the Dock enabled word for the live capsule preference.
V5 retains the original boolean word. A V5 snapshot leaves capsule state unknown,
so the exclusive native Dock defers to legacy while other settings still apply.
The native Dock detaches and waits for its compositor commit before releasing
legacy traversal when a V6 snapshot enables the capsule.

The config7, config6, config5 and legacy config sockets share one authenticated peer.
Root connects to the highest available version and emits that version's format.
Control V10 retires the known V5 through V9 helpers through the existing typed
stop operation. Unknown control versions stay unavailable. The ACK layout and
flags are unchanged. The Android transport test exercises all four sockets and
their publication leases independently of the Root service lifecycle.


V7 appends a waveform count after the unchanged Dock block. Zero leaves
the existing per-event fallback available. An enabled scroll profile may
supply all eight pulse counts, and a close-only profile may supply one.
Each entry is a bounded, aligned Android Parcel payload for the same
authenticated configuration frame. Payloads are memory-only and are
omitted when the existing frame budget would otherwise be exceeded.
