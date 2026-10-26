# OS4 desktop render-thread overhead regressions

This change follows a third-party report of stutter when interrupting animations
and dropped desktop frames. No reporter log, launcher version or trace was
available. The checks below prove two avoidable code paths, not that the
reporter's complete symptom has been reproduced or eliminated.

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
not prove smooth device rendering. No phone operation, app installation or reboot was performed for this
investigation. Code publication is tracked separately in the branch histories.
