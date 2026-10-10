Weather card native transaction tests

Run from source/:
  python3 tests/weather-card-runtime/run.py --output ../acceptance/weather-card-next/NEW-TRANSACTION-RUN --negative-controls

The runner refuses to overwrite an existing evidence directory. It compiles the
entire production card runtime, control, binding and guard headers. Actual pinned
Weather Dart and vendor Flutter ELF PT_LOAD bytes are mapped in the host process.
The production code performs real private near-page mmap, bounded /proc/self/maps
reads, process_vm_readv, mprotect, aligned instruction CAS, complete image and
gateway comparisons, reverse rollback and atomic lease publication.

Twenty-seven separately forked scenarios cover successful two-site installation,
light/dark/default updates, unavailable/expired configuration, clock errors,
permanent cancellation, missing/foreign code, foreign gateway bytes and page
permissions, first full-image refusal, denied initial configuration, all eight
near-placement failures, a kernel ignoring the mapping flag, second-site failure,
recoverable and persistent permission restoration faults, cancellation before
commit/publication, foreign CAS ownership and untouched-tail changes, a busy
publisher, maps parsing, lease packing and signed B-instruction boundaries.
The four new scenarios cover an absent early lifetime observer, a busy protection
handshake, observer loss after installation and observer loss after the first write.

Eleven source mutations must compile and be rejected by the corresponding scenario.
Compilation failure is not a detected runtime negative control. The production
runtime is also compiled with the pinned Android NDK for arm64/API35, the module's
native minimum API; the phone target remains Android17/API37.

The host does not execute ARM instructions. It first links weather_card_arm64.S as
an Android shared ELF with no dynamic relocations, extracts both 296-byte templates
by their linked symbols, and compares all 568 instruction bytes with the separate
independently reviewed and ARM-executed gateway evidence. Only then are these bytes
embedded as data in the host program. Unlinked .o bytes are insufficient because
the two LDR-literal relocations in each template are unresolved there.

History: transaction-host-v1 records an actual host fixture warning and NDK's
missing std::atomic_ref implementation. Production now uses lock-free __atomic
builtins. v2 records 23 successful scenarios but an unused-function compilation
error in one negative mutation. v3 fixed that negative fixture; its embedded
unlinked template bytes were subsequently found incomplete by independent review.
v4 links and binds the real template bytes. v5 also binds the final ACQ_REL CAS
success ordering. Earlier records are retained, not relabelled or overwritten.
v6 adds lifetime integration admission tests; v7 binds the final lifetime API header.

The lifetime API is an explicit state/refusal adapter in this component suite.
It does not execute the parent capture, early GOT observer or madvise splitting.
The real lifetime implementation and its joint execution are separate suites.

This test does not prove the actual loader caller/handle/TID first-use window,
Binder transport, Android seccomp or SELinux, real Dart VM/GC, phone injection or
long-term stability. Template ARM execution and loader/controller integration are
separate evidence. Cancellation prevents future publications after it completes;
it cannot rewind a gateway already past its final lease sample or cached Dart data.
