# Hyos first-snapshot layout owner

The production owner is `app/src/main/cpp/targets/home/home_startup.cpp`, with
15 authorization gateways in `home_startup_arm64.S` and the atomic parameter
bridge at the end of `home_layout_hooks.cpp`. Hyos must keep the old layout and
tweaks writers disabled; ART still uses its original path. The new owner has its
own persistent fixed journal, so there is no partial transfer to the old bank.

`prepare_for_child()` runs before the controller publishes readiness. It obtains
the page size and initializes the fixed state. `execute(Request)` is called once
inside the separately verified first VM-data snapshot-symbol window. The
request borrows an immutable decoded configuration and a fresh authorization
check. It never retains either pointer. A competing writer returns `Busy`
immediately. Control polling later uses `maintenance`; lease cancellation uses
`request_revoke` followed by best-effort `service_revoke` outside its lock.

The window does not open files, query Binder, parse APK/XZ/ELF symbol tables,
create threads, allocate through the executor, sleep, or wait for another owner.
It does call `process_vm_readv`, `mprotect`, instruction-cache maintenance and the
framework's opaque inline-hook backend. Those operations are not guaranteed to
complete within a hard real-time bound. The backend's internal allocations and
locks are outside the no-allocation assertion made by the host owner fixture.

The generated plan includes 794 outputs of the actual production planner, 39
unique instruction locations, 15 detours, and 97 exact guard ranges. The 70 phone
and 450 Pad/Fold grid variants remain evidence only: these composite grids are
currently declined because Rust also controls GridConfig and workspace data creation. Rust
recents-animation timing is likewise unsupported. Folder columns include their
square-preview companion; a formerly missing precise symbol lookup was added
and has its own negative control. Supported groups use the existing production
Dart trampolines and full companion sites, not partial copies of their logic.

Code writes use aligned atomic 32-bit stores and instruction-cache maintenance.
A pending RX-protection record survives byte rollback. Every backend call is
followed by a complete guard read-back permitting only the recorded 16-byte
replacement spans; a backend that changes more is refused. Failure restores
owned constant words, asks the backend to unhook, and verifies all original guard
bytes. Unknown words and tails are not overwritten. Failed restoration is
terminal and retains evidence; a successful backend return alone is insufficient.

Authorization is checked between hooks/writes, around parameter publication,
and after opening the epoch gate. Revocation advances a monotonic generation;
stale writers cannot reopen it. Saturation stays denied. Parameter fields are
separate atomics, not one atomic structure. The checks do not turn a changing
external authorization source into a transaction with all running Dart code.
The opaque backend still has to provide a usable original continuation before a
patched function can execute; the API header does not promise installation
ordering. The accepted boundary establishes that the observed snapshot has not
been consumed, not that every possible thread in the real phone is stopped.

After first use, maintenance may update parameters of already installed dynamic
groups. It does not install a newly enabled detour or reapply a lost 16-byte
patch. New values for startup constant groups require a fresh launcher start.
Revocation restores owned constants word by word when the writer is available;
this is neither a multi-site atomic rollback nor reversal of values already
cached in Dart objects. Kernel or backend failure can delay or prevent code
restoration even though the gateway is closed. The implementation does not claim
instant visual undo or long-term phone stability from these offline tests.

Reproduce from the workspace root, using a fresh output directory each time:

```sh
python3 source/tests/home-startup/generate_plan.py \
  --output acceptance/home-startup-next/generated-plan-rerun \
  --header acceptance/home-startup-next/generated-plan-rerun/home_startup_plan.generated.h
cmp source/app/src/main/cpp/targets/home/home_startup_plan.generated.h \
  acceptance/home-startup-next/generated-plan-rerun/home_startup_plan.generated.h
python3 source/tests/home-startup/run.py \
  --output acceptance/home-startup-next/executor-rerun
python3 source/tests/home-startup/test_plan_symbol.py \
  --generated acceptance/home-startup-next/generated-plan-rerun \
  --output acceptance/home-startup-next/missing-symbol-rerun
```

The runner compiles actual production C++ with low-level fault fixtures, also
compiles the same C++ without the test macro for real host memory syscalls, and
executes actual production gateways under AArch64 QEMU. The ARM compiler/QEMU
location is the workspace's cached AArch64 environment in `run.py`. ROM
instructions themselves are mapped as data in owner tests. Gateway destinations
are controlled fixtures, not a complete Dart VM. Real Android process policy,
ZygiskNext injection, first-frame rendering and long-duration behavior still
require phone acceptance. `startup_fixture.*` is an isolated backend for the
separate real entry/control/owner integration tests; it is never linked into the
shipping module.
