# OS4 desktop render-thread overhead regressions

This change follows third-party reports of interrupted-animation stutter,
delayed back gestures and accidental back-long-press actions. October 26 logs
from launcher 8593 show two input ANRs with the main thread in `property_get`,
HyperCeiler native code and file reads, during back-arrow resource creation or
buffer import. The anonymous offsets cannot be attributed to a specific helper
without that APK's matching Build ID. The checks below prove avoidable work on
that entry path, not that every reported gesture symptom has been eliminated.

## Repeated first-render installation

The system-property hook calls `prime_home_layout_first_render`. Its former
ready condition required every optional folder/widget/drawer family to become
live. A missing runtime feature or a backend installation failure prevented that
condition forever. Each later property query could repeat configuration reads
and `ensure_slots_live` on the caller, which may be the render/UI thread.

Using the actual pre-fix bootstrap body and the counted slot-host fixture, a
missing widget feature produced **500 configuration reads and 500 installation
attempts for 500 calls**, despite executable discovery itself being cached.

The first-render pass now makes one synchronous attempt per image once the image
and configuration are available. Unsupported families and partial install
failures do not turn property reads into a retry loop. Registered slots remain
available to the existing worker for recovery. A busy/missing configuration
publication preserves scalars and defers binding, rather than publishing a
neutral value. Late image arrival can retry, and child/image state reset clears
the one-attempt flag.

The compiled production bootstrap regression covers the supported path, each
missing optional family, failed installs followed by worker adoption, busy
configuration, late image availability and a new generation. **500 repeated
calls produce one installation attempt** for each completed attempt scenario.
This is an invocation-count regression, not an Android frame-time benchmark.

## Native columns / back-region primers

`start_home_layout_hooks` also called the early native columns and back-region
primers before its ready/already-started checks. Missing contracts in a readable
Rust image could therefore trigger repeat scans on property queries (columns
were throttled to 200 ms, not stopped). The primers now make one decode/install
attempt once the complete image is readable. Their registered slots remain for
worker adoption; missing images/configuration can still retry. Once the worker
has taken ownership, the columns primer also stops entering the bootstrap path.

The actual production primer bodies are compiled against counted image/parser/
slot boundaries. 500 repeated requests past the columns throttle now produce
one columns scan and one back-region scan for an unsupported image. Tests cover
late images/configuration, disabled -> enabled settings, failed installation,
worker handoff, main-thread gating and image-generation reset. The existing
columns bootstrap suite additionally uses the real NHK bank and verifies original
container attribution, settings publication, no duplicate install and rejection
of a foreign image generation (53 checks).

## Process identity I/O

The property path also called `is_launcher_process`, which opened/read/closed
`/proc/self/cmdline` on every query. The native scalar cache now uses PID and a
rename generation. PID prevents inheriting a parent's answer after fork; the
existing `setprogname` hook invalidates before/after renaming. Atomic generation
comparison prevents an older read from republishing across invalidation.

Transient spawner names and failed reads are not cached. If the rename hook is
not installed, the original uncached behavior is preserved. The original NUL
terminated first cmdline argument semantics are retained.

The production cache test covers 10,000 repeated queries -> one name read,
positive/negative process identities, fork PID changes, same-PID renaming,
invalidation during a read, NUL/argv data, transient names, failed reads and
hook-unavailable fallback. No Dart pointer, object allocation or ABI addressing
was added to either path.

## Property dispatch and engine ownership

The earlier identity cache alone missed a second read: `StartHomeLayoutEngine`
tested the command line before its already-started flag. It now checks the
worker's PID first. Forked children still reset inherited state; failed thread
creation still allows a retry. Direct launcher injection also installs the
existing rename observer, so identity caching can become safe in that path.

Property dispatch now prevents recursive initialization while preserving every
property override and the original getter. After a layout worker starts,
property notifications no longer retry the first-render or native-region
discovery paths when images/configuration are missing. The existing worker
handles late Dart mappings; genuine library-load first-frame priming and a cold
property fallback before worker ownership remain available.

`property_path.py` compiles the actual shipping dispatch, engine-start and
layout-start bodies with counted OS boundaries. After startup, 10,000 property
queries cause no command-line reads, scans, installation retries or additional
threads. It also covers fork ownership, failed thread creation, recursion,
property overrides, exhausted retry budgets and library/cold fallback. This
does not assert zero file I/O during the one cold fallback or when a rename
observer cannot be installed.

## Cold raster-thread property reads (7755 report)

The October 26 14:10 ANR from launcher 7755 shows the platform thread waiting in
`shell_holder_surface_changed` while `1.raster` enters module code and a file read
during Impeller snapshot initialization. Its native Build ID differs from the
available binary, so the anonymous frames are not assigned to an exact helper.
The current production dispatch nevertheless reproduced a remaining avoidable
path: before worker startup, a background property read could perform the cold
first-render fallback and create workers. The earlier warm-only test missed it.

Property-triggered startup is now restricted to the process main thread, before
identity reads, specialization, discovery or installation. The first-render and
layout-start property helpers defend the same boundary. Background callers still
receive the original getter result and all existing scalar overrides. No attempt
or ready state is consumed by a deferred background request. Native entry,
setprogname and genuine library-load startup remain intact, as does the existing
main-thread cold first-frame path; no new thread, polling or per-query log is added.

The new production-body regression failed before the guard was added. It now
verifies 10,000 **cold** raster queries with no module initialization/discovery
attempt, including preserved prestart, device-level and blur property values,
followed by successful main-thread first-frame/worker startup. This eliminates
that render-thread entry path; it does not prove every cold main-thread operation
is cheap or that the reporter's device frame times are fixed.

## Run and limits

```sh
python3 tests/home-render-performance/run.py
```

This runs the actual cache implementation and actual production bootstrap body
with a counted host slot backend. The existing independent-object-scale host
and ARM64 suites verify unchanged scale routing and allocation behavior.

Release assembly and APK signing verification must also pass. Device acceptance
still needs launch -> return interruption, return -> relaunch interruption and
rapid switching with the reporter's settings. Use frame timelines/Perfetto and
compare the same workload before/after; hook readiness and host call counts do
not prove smooth device rendering. The original offline investigation did not
operate a phone. A later authorized installation and launcher-8584 check is
recorded in [device-20261026.md](device-20261026.md), including the partial scaled
run and the remaining device limitations. Code publication is tracked separately
in the branch histories.

## 2026-10-27 missing contracts and update preflight

`loader_lookup.py` compiles the production symbol lookup, symbol span and image-source accessors. The old callback path waited on the worker mutex when symbol parsing had merely started, and span/source accessors also performed discovery from the loader callback. The regression fails against that path with a busy decoder lock. Loader consumers now use try-lock, consume only a ready index and never invoke discovery or EnsureLoaded, including its deferred cache persistence. Worker parsing remains available.

`binding_retry.py` compiles the actual startup fragment and auxiliary discovery prefix. An unsupported fixture formerly attempted knobs 201 times and animation discovery 200 times, spending 10000 ms in deliberate sleeps. Startup now makes one attempt per family with no artificial delay. In 1200 half-second worker passes the auxiliary prefix ran 21600 binding calls before the fix and 414 after it. These are discovery calls, not a count of backend Hook installs or a device frame-rate result.

Discovery phases back off through 500 ms, 2 s, 10 s and 30 s. A changed feature request, newly completed executable inventory or process/image reset clears their cooldown. Late dependencies keep a recovery path. Existing slot health checks, required rollback/repair, original-code validation and dynamic preference publishing remain outside this discovery gate. Missing image/config uses the existing 1500 ms monitor cadence rather than an indefinite 50 ms loop, with target library/config notification retaining immediate wakeup. Experimental field captures are not retried while their explicit field-write gate is off.

`hot_reload.py` compiles the actual update preflight. A launcher with native loading already claimed owns hooks and permanent threads with no retirement interface, so permitting old-generation retirement is unsafe even outside the existing SDK 37 resource compatibility gate. Such transitions now return false before Java cleanup, logging automatic_restart=false. Native loading is claimed synchronously by the module-entry probe and the AtomicBoolean is never cleared in this process. Unloaded and unrelated older-platform targets retain the original transaction. No reboot, shell restart or automatic process kill was added.

These fixes preserve existing allocation ABI checks and runtime feature identification. The full suite and Release build passed; no device installation, reboot or frame-time measurement was performed for this audit. See ../../docs/investigations/os4-performance-update-20261027.md for evidence and remaining device checks.
