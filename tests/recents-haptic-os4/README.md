# OS4 recents card vibration

The tracker-based notification path is checked by
`python tests/recents-haptic-os4/verify_tracker_grid.py` (C++20 compiler in `CXX`).
This portable suite also executes both original 8583/8584 resolver and observation
fixtures. The 9239 fixtures carry the source image SHA-256; their addresses are
test inputs, never production lookup tables.

The new path proves the phone/pad notification class dispatch and its shared
pooled scenario tracker, tracker begin/end singleton source, actual boxed metrics
consumer, real drag-details producer and grid calculator links. A separate
admission proves the merged list/stack notification owner against those same
class IDs and tracker calls, the linear extent/padding call and the stack
offset/index inverse relation with its paired viewport callers. The merged
callback receives updates even though its original body handles only start/end.
List and stack share one feedback lane and publish their actual rendered strides
to that lane; grid stride/state remains separate. The plan publishes a site mask,
so an unavailable/ambiguous list family cannot disable a fully proven grid family.
Only admitted slots participate in binding and readiness. The old complete
seven-site graph is attempted first and retains its original separate lanes.

The portable checks pass 685 grid and 878 shared-family instruction corruptions,
independent relocation, missing/ambiguous owners, invalid notification/depth/
pixel class, programmatic entry and unsupported-slot rejection. Both legacy
fixtures also pass their existing 882 corruption and crossing/observation checks.
Release builds successfully. Device validation on 2026-11-15 used the installed
Release on 24129PN74C, launcher process 11123, with the existing root-socket
configuration transport. The final graph bound mask `0x77`, ready=1, and received
enabled=1/type=2/strength=100. Four horizontal stack-layout swipes recorded 785
notification samples and 1508 extent observations, stride=274.847, queued=10,
delivered=10, dropped=0. Android vibrator_manager independently recorded matching
`com.miui.home` TOUCH waveforms (32 ms at amplitude 1.0) with status `finished`.
This establishes actual stack gesture consumption and system vibration execution;
subjective strength/sensation, the other layouts on-device, custom effect IDs and
default WMS transport were not revalidated in this run. The earlier grid-only
diagnostic build's mask `0x13` was insufficient for this device's stack layout.

The settings page has one Vibration effects entry combining the four existing
waveform presets and the device inventory. The redesigned picker has a full-width
search field and no manual ID entry. Purpose categories appear once as group
headings. Presets use compact single-line rows; device rows show a readable name
and secondary decimal ID. Support-status labels and hexadecimal duplicates are absent.
System actions are ordered before music/game effects. The native dialog list
owns scrolling and text uses MIUIX dialog attributes. Search explicitly clears
ALT_FOCUSABLE_IM because the editor is in the custom title, not setView.

Vendor names come from current numbered RTP firmware filenames in ODM/vendor
firmware directories. The application enumerates readable directories first,
then uses its existing root helper on a background thread when necessary. No
user path or command enters that read. Names are cached for the process; localized
translations are keyed by semantic firmware stems, never vendor numerical ID
lookup tables. Conflicting aliases decline naming. Unknown future stems retain
their actual vendor name, while missing metadata is labeled honestly. Standard
Android names remain the fallback for IDs in its own namespace.

`JAVA_HOME=/path/to/jdk python tests/recents-haptic-os4/verify_firmware_names.py`
checks the production filename index, changed IDs, duplicates, conflicts,
malformed/path-like names, and aligned English/Chinese/category resources. The
checked-in filename fixture was read from /odm/firmware on 24129PN74C; it contains
203 numbered aliases. All 181 nonstandard stems have localized names.

The latest Release was installed on 2026-11-15. Device UI checks confirmed the
full-width search and absence of Input ID, with searches returning Card scrolling
(ID 402), Camera shutter (217) and Acoustic guitar (521), their Chinese names
and purpose categories. Android recorded the ID 521 preview starting; it was
superseded by subsequent button feedback when canceling. The configuration
observed before and after the final cancel-only check remained
custom=true/effectId=118/type=2. Earlier picker acceptance also verified scrolling,
search-filtered scrolling, preset/device selection and mode persistence. Manual
input was tested in that earlier revision and has now been removed at user request.
Other device font scales and on-device dark mode were not exercised.

Search/previews do not save. Use writes the chosen value before selecting its
mode using the existing custom/type/effect_id keys; native and endpoint consumers
retain the original schema. The picker has only Cancel/Use actions.

The follow-up search-field adjustment uses 16 dp title-panel side gutters instead
of the library's larger title padding. On-device bounds changed from x=156..1044
to x=91..1109 (888 to 1018 screen pixels). The preview instruction and its
TextView/padding were subsequently removed at user request, leaving the search
directly above the list. The installed width adjustment was checked through the live UI hierarchy.
The IME Search action hides the keyboard without dismissing the picker.

The subsequent layout pass removes duplicate title padding, aligns search/list
gutters and uses MIUIX secondary text colors for category headings and IDs.
Group headings are nonselectable and exposed as accessibility headings. Use is
disabled when the current selection is absent from the filtered results.
The installed Release was checked for grouped scrolling, ID 402 filtering,
selection enabling Use, and empty results disabling Use. Cancel preserved
custom=true/effectId=118/type=2 in physical preferences. This pass did not assess
subjective preview sensation, other font scales, dark mode or screen readers.

The searchable picker uses a nonimmersive bottom-anchored panel. A full-height
window receives IME insets without platform panning. While the IME is visible,
the native list gets an explicit height after reserving the measured title and
action chrome; the panel retains its resting top where space permits, with a
minimum useful keyboard viewport on shorter windows. Filtering keeps that
viewport stable. Hiding the IME restores wrap-content sizing at the bottom.
This replaces the earlier top-anchored revision, which kept the editor stable
but incorrectly moved the resting picker to the top of the screen.
The installed Release was checked with the title at y=814..953 and editor at
y=953..1109, unchanged after IME opening and ID 402 filtering. The shortened
native list scrolled to show the complete Card scrolling name and ID. Both
actions remained visible above the IME. Hiding the keyboard restored the
filtered panel to the bottom (action bounds y=2364..2527). Cancel preserved
custom=true/effectId=118/type=2. Other keyboard heights and orientations were
not checked on-device.

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

The runtime test also covers the opt-in local helper backend for scroll-only,
close-only, both enabled, unavailable and failed playback. Local ownership
remains authoritative even if the debug property changes; the fixture fails
if any WMS lookup occurs. The actual nonblocking callback queue, worker, profile
checks and delivery/drop counters run unchanged.

The optional launcher playback backend uses the authenticated root configuration
channel with `debug.hyperceiler.home_haptic_backend=launcher`. The helper checks
the current physical profile and screen state, then serializes the platform
waveform in memory. The launcher submits the bytes with its own Binder token.
A prepared request and a legacy play request use separate wire commands, and a
failed prepared request is never replayed through the legacy player.

The SDK dispatch probe takes a read-only copy of the device's
`libhyper_os_aidl.dylib.so`. Build it with C++20 and the app native include
directory, then pass the library path as its only argument. The Android probe
additionally links Binder NDK and calls the dynamically resolved read-only
vibrator inventory operation. It does not play a vibration.

The `launcher-cached` experiment reuses a successfully submitted waveform
within the same authenticated configuration revision, profile and pulse count.
A cache miss uses the prepared playback path. On a hit, two read-only system
queries verify interactivity and keyguard state on the action worker. The live
SDK proxy instructions supply both transaction numbers. A known blocked state
drops the event, and an unavailable query leaves the decision to the helper.
