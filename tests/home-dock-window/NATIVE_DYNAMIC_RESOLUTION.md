# OS4 Dock: semantic dynamic resolution, concurrent recovery and live unlock projection

The permanent nonblocking eventfd coalesces the newest sample while Binder reconnects.
The transport reconnects after either a transaction failure or a real suspend gap found
by comparing `CLOCK_BOOTTIME` and `CLOCK_MONOTONIC`. Its five-second health packet does
not refresh an older published motion sample; it only validates transport and preheats
the system-side frame channel.

The native hook manager verifies target mapping identity and patch bytes in the
background. A complete executable-mapping inventory change or an explicit recovery
request triggers a fresh semantic scan. Every independently mapped runtime is grouped by
file identity and load bias, resolved on its own, and installed into an immutable symbol/
trampoline bank. Old and new generations stay hooked concurrently; a newly mapped idle
runtime can no longer steal the only trampoline from the active UI runtime. Banks are
never reused in the same process, so a delayed callback can never observe another
generation's layout or original trampoline.
AUTO_AIM adds one optional hook to each immutable bank. The resolver identifies a raw-double
`scaleValue=` setter through its ABI, records the return address of the `bl` that feeds it from
`conversionValueFrom3DTo2D`, and then derives the state → widget → CellLocationInfo field chain and
the Hotseat container set from current ARM64 code. The hook accepts only that exact call site and
publishes its `d0` as scene 3; it never authors a keyframe, interpolator or spring. On the current
launcher artifact that call site is a per-icon setup call that carries a constant, so the reveal
replays the launcher's own logged projection instead (see README); the channel stays wired because a
launcher build that does publish a real per-frame value is then picked up with no code change.
Failure to resolve or repair this optional hook leaves the established recents triple unchanged.
The system-side frame channel uses an owned Choreographer when OS4 exposes it. A real
motion sample detects a frame request stranded across suspend using elapsed realtime and
immediately replaces it. Transient SurfaceControl or Choreographer failures rebuild the
cached transaction and frame clock with bounded backoff.

The production observer has no launcher Build ID/address table, function address,
file offset, whole-function fingerprint, fixed class ID or launcher payload offset.
It intentionally retains bounded ARM64 compiler idioms and register/data-flow
relationships as semantic contracts. The old probe and profile-only tables were
removed (recoverable from Git history).

## Resolution and safety boundary

1. Parse only this process's explicit, file-backed, non-writable executable
   `/libapp.so` mappings from `/proc/self/maps`, including a retained `(deleted)`
   generation. Code is copied with a fault-reporting own-process read and the
   mapping inventory is checked again afterward. It never scans writable,
   anonymous, heap, or another process's memory.
2. Decode ARM64 opcodes and register/data-flow roles. Locate the parameter
   constructor through its allocation, scalar stores, paired scale stores and
   canonical-bool initialization. Locate the per-frame callback through its two
   calls to one setter, then locate `setTo` through three boxed-double transfers
   using that same setter.
3. Resolve `animateTo` by its dynamically shared generated prelude with `setTo`,
   then require its continuation cluster to use the constructor-derived alpha,
   scale/scaleY fields and boxed-double allocation tag. No saved instruction
   launcher address, file offset or whole-function hash participates in selection.
4. Decode the object-header offset, class-ID bit range, object-size bit range,
   allocation tags, bool singleton displacement and every payload field from the
   current process's instructions. Independently corroborate constructor, setter,
   callback and continuation relationships. Invalid, changed or ambiguous code
   fails closed.
5. Publish each generation's derived values through its own independently relocated
   module symbols before installing callbacks. Assembly has no launcher address or
   payload offset constants and does not retain or modify Dart heap pointers.

Only ARM64 opcode encodings and Dart calling registers remain compile-time
contracts. They are instruction-set/calling-convention definitions, not launcher
addresses or memory offsets. Edit-mode observation was removed because it had no
equally strong relationship to the motion graph and would otherwise require a
specific closure layout.

Failure retains the existing wallpaper-command animation fallback. A scene-0 sample
cannot initiate motion by itself, but a verified window-scoped overview target can
bridge a transient native scene gap while native scale remains in the recents band.
An expiry frame returns from the last native position if samples stop after overview
exit. The former experimental EditMode visibility path is not part of native motion.

LSPosed initializes the module's native entry in `/system_ext/bin/hyos_spawner`
(currently named `usap64`) before it forks MiuiHome. Version 17 hooks the spawner's
dynamic `setprogname` symbol; the inherited hook starts the worker only after a
child identifies itself exactly as `com.miui.home`. The loader callback and
property-read detection remain bounded fallback signals. HYOS maps its AOT
application outside the ordinary linker callback path, so the worker still waits
for and resolves only that process's executable `libapp.so` ranges.

Samples travel from a detached MiuiHome transport worker to system_server as custom
transactions on the launcher's existing `IWindowManager` Binder. Synchronous Binder
identity is required because this OS4 kernel reports PID 0 for one-way calls; the
launcher render callback only publishes to eventfd and never waits on Binder. WMS accepts the fixed-size payload
only from the UID/PID bound to the exact launcher window Session, validates sequence,
monotonic timestamp, age, scene and scale, then applies the latest value on the SF
frame clock. The previous system_server-to-launcher Unix socket was blocked by
SELinux and has been removed.

If a new launcher PID publishes before its WindowState is prepared, WMS retains the
sample without applying it and promotes it only after the exact UID/PID is independently
bound. This closes the launcher-restart race without trusting package data from the
payload.

The detached sender recreates its `IWindowManager` handle and retries every 500ms
after a transaction failure. This covers the temporary endpoint loss caused by a
module install/hot reload without polling while connected or touching the launcher
render thread. Only the first unavailable interval and first three disconnects are
logged; each successful connection immediately publishes the latest sample before
waiting on eventfd again. A semantic revalidation request remains acknowledged until
the receiver observes a publish counter newer than the request baseline. While it is
pending, the sender repeats the scan at a bounded one-second cadence; a transient or
premature scan therefore cannot remain stuck until another overview gesture.

## Verification (2026-09-10)

- The v31 artifact resolver passes against launcher 6241 and dynamically discovers
  `scale=e25740`, `animate=deecd4`, `set=e24488` and parameter CID 1777 from semantic
  relationships. Multiple unrelated constructor and scale candidates are rejected
  by their setter/continuation data flow.
- The current launcher artifact also resolves the unlock projection setter and, independently,
  the return address of the `bl` that feeds it (`scale=12d3e3c`, `call_return=12d37d8`); the
  required-artifact test verifies the dynamically derived pointer offsets, the call-site filter,
  five distinct Hotseat containers, relocation and four-word hook contract.
- Resolver/runtime tests relocate complete executable ranges, group independently
  loaded generations, reject duplicate matches and malformed mapping inventories,
  and pass AddressSanitizer plus UndefinedBehaviorSanitizer.
- Nine Java policy tests pass, including packet replay/freshness, exact UID/PID
  ownership, early PID rebinding, same-sample overview reinterpretation, fallback
  continuity, background-mode migration and glass presets.
- The production assembly was cross-compiled and executed on the connected
  phone using HyperCeiler-only synthetic fixtures. Registers x0-x15, NZCV,
  Dart stack, unchanged fixtures, all five Hotseat classifications, raw `d0` preservation,
  scene guards and eventfd notifications passed.
  A second fixture changed the class-ID bit shift, mask, tagged-header offset,
  bool-singleton displacement, class IDs and every payload field without changing
  assembly. It uses a second immutable runtime bank and verifies the first bank still
  works afterward. Temporary phone files were removed.
- The acknowledged versioned Binder payload carries transport sequence, monotonic
  timestamp, packed scene/scale, entry count and publish count. Every active endpoint
  restores the input Parcel and yields before the final reply, so callbacks retained by
  hot reload cannot starve their successor.

Host artifact test (pass paths to extracted ELF files, not APKs):

```sh
clang++ -std=c++20 -O2 -Wall -Wextra -Werror \
  tests/home-dock-window/DockNativeResolverTest.cpp -o /tmp/HyperCeilerResolverTest
/tmp/HyperCeilerResolverTest /path/to/6179/libapp.so /path/to/6236/libapp.so
```

## Codacy PR 1686

The public report for commit `8eb09d035` contained 81 newly added issues.
Changes address the obsolete sleep API, untyped eventfd read, runtime-selected
reflection inventory, Java field/package declarations, test packages, deeply
nested socket reads, large window callbacks, labeled returns, duplicated strings,
glass-host NPath complexity and interrupted-future handling. Window lifecycle,
lock ordering, cleanup and fail-closed fallback remain intact.

Local Detekt 1.23.8 recheck no longer reports the listed long methods, labeled
returns, duplicate strings, complex conditions, excessive function counts or
native-client generic catch/nesting in the changed Kotlin paths. All-rules mode
also reports rules not enabled in this Codacy report; this is not a claim that
the entire repository or remote quality gate is clean.

Deliberate boundaries retained for review:

- The loader-owned `NativeApiEntries.unhook_func` slot must remain ABI-compatible
  even though these permanent process-lifetime hooks do not unhook.
- WMS Session comparison requires object identity; replacing it with an arbitrary
  value equality implementation weakens the scope guard.
- Renderer IPC/hidden-vendor API recovery catches unexpected vendor failures off
  the WMS thread. Narrowing this to a guessed list must not let an OEM failure
  terminate the system process, so the broad recovery boundary is retained.
- Access to own-process vendor ViewRoot instance fields is necessary for texture
  readiness and the own-window blur filter. Its scoped PMD suppression documents
  that requirement; public APIs are used for ordinary inventory types.

The requested push target is `os4`. PR 1686 uses `os4-branch`, so pushing only
`os4` does not refresh that PR's Codacy report.
