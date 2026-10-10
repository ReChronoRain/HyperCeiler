Four actual Weather runtimes: early page observer and card/level/material control

Reproduce from source checkout:
  python3 tests/weather-card-lifetime-joint/run.py --output <new-absolute-directory> --negative-controls

The fixture includes complete production weather_card_lifetime.cpp,
weather_card_runtime.cpp, weather_runtime.cpp and weather_material_runtime.cpp.
It does not use a bool-state replacement for lifetime APIs or their GOT slot.

Actual pinned Dart, Flutter and Android libc ELF PT_LOAD bytes are mapped before
capture. Parent observe_parent_load executes once, then each isolated case uses
real fork, getpid/getppid/gettid and /proc/self/task enumeration. Child code
really changes the Flutter madvise GOT with CAS and mprotect, and parent memory
is checked unchanged after all cases. All original bytes are from the three
ROM ELF files, not values copied from generated guard arrays.

Host mappings are anonymous, so the test syscall adapter changes only the
pathname of the actual first Flutter /proc/self/maps line to the pinned Android
path. Addresses, permissions and other mapping lines remain from real procfs.
process_vm_readv, page ownership reads, mprotect and all GOT/code-word CAS are
real. The selected failure injection and slow-original controls are explicit.

The four runtimes share one real pthread worker and production HWS decode/cache.
Binder transport, BOOTTIME and old level/material identity resolution remain
external fixtures inherited from the three-runtime test. Their installers,
wrappers and policies execute. Callback admission from the real Android loader
is not exercised; on_first_card_snapshot is the explicit fixture boundary.

Calls through the actually changed Flutter GOT enter the production observer.
Its original A64 libc target address is verified, then the adapter calls host
madvise with the original parameters. Thus MADV_DONTNEED really discards
unprotected anonymous pages while both patched pages survive. This is not
execution of the original Flutter A64 release function: that has separate
selected ARM replay evidence. This suite does not claim to emulate all vendor
page-reclamation callers, file-backed Android reload behavior or VM semantics.

Both complete linked ARM64 gateway templates are compared to compiled-v2 bytes
that were independently executed, then embedded as data in the host test.
The host does not execute their ARM instructions. The special x86 callsites
provide the reviewed exact level/material return-PC relationship only.

v3 runs 29 positive cases and eight source-copy negative controls. In addition
to successful one-worker installation, it checks real fork/thread rejection,
foreign ownership, permission residue, actual three-way discard splitting,
in-flight-reader admission refusal, rollback with an active filtered reader,
foreign code refusal, lease expiry/revocation and concurrent GOT readers. Every
negative must compile and fail its designated named behavioral witness; a
compile error or missing RESULT is not counted as detection. Input files are
hashed before and after each complete batch.

The source ancestry lists reused external-fixture scaffolding. The old lifetime
adapter and old three-runtime scenario list were removed. Historical v1/v2
reports are preserved. v3 adds the active-reader rollback, foreign-code residue
and eight-reader cases; it does not overwrite earlier evidence.

All real-phone results remain NOT_TESTED: Android seccomp/SELinux, actual
Hyos/LSPosed injection, first-snapshot lifecycle, Dart GC/VM concurrency,
rendered card appearance and long-term stability. Single-thread scanning is an
observation at the required specialization boundary, not a thread-creation lock.
