# OS4 recents card vibration

Run `JAVA_HOME=/path/to/jdk python3 run.py` here. Fixtures are independently compacted executable
instructions from launchers 8583 and 8584. Production discovery uses no names,
ELF sections or stored image addresses. It proves four recents-only notification
owners sharing the same scroll begin/end controller and class IDs, the actual
boxed metrics pixel consumer, grid/single-card calculator links and spacing,
the horizontal extent producer, and the stacked scroll/index inverse fields
with their two common viewport callers. Unknown or ambiguous graphs stay stock.
The actual ScrollStart producer and allocator prove its nullable drag-details
field and class, so programmatic entry positioning cannot start a feedback session.

The callback borrows only current Dart arguments, records numeric card extents
and observes scroll notifications. It never retains movable objects, allocates
Dart objects or calls Dart/GC. Only a crossed card boundary writes a small atomic
nonblocking pipe request. Binder work runs on a dedicated native worker and
Vibrator executes through an authenticated WMS endpoint, so a dex-less launcher
does not need a Java VM or an injected Java callback. Physical preferences are
read as one snapshot and each dispatched vibration uses the latest profile.
Disabled/not-ready banks take the original continuation without a native callback.

Tests run the shipping resolver and observation/state code under ASAN/UBSAN,
with relocation, missing/ambiguous dependencies, instruction corruption, changed
class IDs/spacing, null/wrong-class/nested notifications, all extent paths,
stationary/jitter/reverse/fling/cancel/settings changes and invalid samples.
The ARM64 gateway regression also executes the on/off path for all seven banks.
HCV1 reply parsing rejects truncated/malformed/unavailable responses. The actual
Java pattern builder is tested across 5824 combinations, including four distinct
rhythms without composition primitives or amplitude support. Earlier JNI worker
tests were removed with that unusable launcher-side Java path.

Device acceptance must cover all recents layouts, one pulse per card boundary,
fling/reverse/no vibration at rest or entry/exit, the four types and strength,
actual delivered counters, system haptic settings and a motor without every
Android primitive or amplitude control. The new independent HCV1 endpoint must
load in system_server after installation, so initial device testing needs a reboot.
HCL4, HCS1 and gesture wire contracts are unchanged.

## Independent upward-dismiss feedback

The close switch is independent of scroll feedback. Both use the existing type
and strength controls and their same waveform builder and WMS vibrator route.
The default is off. A full guarded manual vertical-drag graph proves the accepted
branch, animation/status-tracker relation and the original light-impact caller.
Two optional recents-bank sites queue one close event only after acceptance and
skip the stock haptic call only when the close channel and both sites are ready.
Canceled/locked-card swipes and clear-all retain their original control flow.
No original Dart BL is replayed through a native trampoline.

HCV1 (0x00484356, ACK 0x48435631) stays six words unchanged. HCV2 uses
0x00484357 and ACK 0x48435632. Requests are operation i32, monotonic timestamp
u64, count i32. Operations are query 0 with count 0, scroll 1 with count 1–8,
and accepted close 2 with count exactly 1. Flags, timestamp age 0–200 ms, caller
identity and trailing bytes retain the same checks. The seven reply words are
ACK, available, scroll enabled, close enabled, type, strength, result.
Query success may return result 1 even when both switches are off. Unavailable
replies have both switches and result zero, type zero and strength 50.

The provider keeps the four HCV1 columns; versioned cursor extras carry the
close flag from the same physical getAll snapshot. Config epoch, both switches,
shared profile and protocol version are published as one native atomic word.
Queued events whose epoch changed or age exceeds 160 ms are dropped. Callbacks
only write the existing bounded nonblocking queue; no Binder or logging runs on
the gesture thread. Close and scroll readiness and delivery counters are separate.
A server without HCV2 falls back to HCV1 scroll and retries HCV2 at most every
30 seconds. Close feedback requires reloading system_server after installing
this update; this implementation has not yet been installed/device-tested.

The 8584 close fixture tests every instruction corruption, independent function
relocation, tagged-field/frame changes, shared producer/consumer mismatch,
missing/ambiguous graphs, no-BL replay windows and atomic two-site admission.
This does not establish close-graph compatibility with 8583 or other launchers.
Host tests additionally validate both protocol versions, the four switch
combinations and single-snapshot provider publication with shared parameters.
Device acceptance must verify one selected waveform per confirmed upward close,
no extra stock vibration, no pulse on cancellation/locked cards/clear-all,
close-only and scroll-only settings, and all supported recents layouts.

## Native worker regression, 2026-10-29

Run `python3 runtime_test.py` here for the shipping runtime with only the
unavailable host Android Binder/log surface substituted. Real refresh,
profile publication, scroll observations and crossing state, nonblocking pipe,
worker dispatch, request serialization, reply parser and delivery counters run
unchanged. Five fresh processes cover both channels, scroll-only, close-only,
both off and an old HCV1 server. They assert operation 1/count 1 for one scroll
crossing, operation 2/count 1 for close, operation 0/count 0 only for queries,
and no extra event during 120 stationary updates. The full run.py includes this.

This test reproduced the actual regression before the fix: the former reserved
queue field remained 0 on scroll requests, but now meant query, so WMS rejected
query/count 1 and no vibration was delivered. The private request now uses a
typed Scroll/Close enum and defaults to Scroll; arbitrary integer 0 cannot be
supplied as feedback. Android hardware waveform perception still needs a phone.
