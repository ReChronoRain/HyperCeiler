Weather shared controller regression, including card routing

Run from project root into a new evidence directory:
  python3 -B source/tests/weather-runtime/run.py --negative-controls --output <new-output>

The source includes complete production weather_runtime.cpp, HWS1 Cache/decode
and DeviceLevel policy. All prior 92 controller cases remain and are asserted
before adding 25 card-routing cases. Those117 cases are retained unchanged,
then four early-lifetime-residue admission cases bring the expected total to121.
All19 prior mutations remain; one new mutation removes the real early gate.

weather_card_runtime is explicitly replaced by card_adapter.h for this suite.
Its configurable prepare/install result and cancellation state let tests check
the actual controller calls, independent fallback, worker count, late/missed
window refusal and propagation of a card Residue. The adapter only reads the
actual callback supplied by production and records fresh snapshots. It does
not implement production card code installation, ARM gateways, lease
publication, image identity, cached object recovery, or rendering. Material
installation is also an adapter in this controller-only suite. Production card
runtime and complete three-runtime integration are separate evidence.

Binder, BOOTTIME, poll sleeps and binding resolution are explicit adapters.
Real pthread startup/wait, mapped memory, process_vm_readv, mprotect/proc-maps
permissions, pointer CAS and the exact builtin return-PC condition execute.
Each case forks. A stuck Binder worker is not cancelled; startup remains
bounded and no replacement worker is created. The tests do not use a phone,
Android Binder service, actual Android linker or Dart VM. Device NOT_TESTED.

Card raw 2/3, default 0 and malformed values are tested through the actual HWS
decoder. A raw card default never forces either native feature. Freshness tests
show the values supplied to the adapter after expiry/new publication, not that
already consumed Dart objects automatically revert or refresh. Tests that
refresh material authorization do so before its first consuming query.

A negative control only counts if it compiles, completes with semantic failure,
and emits its named witness; compile failures/timeouts do not count. Input
hashes are collected before compilation and checked after all runs. Evidence
counts describe regression cases, never settings or feature coverage.

The separate source/tests/weather-card-joint-runtime suite includes all three
actual production translation units and real card binding/ELF mapped bytes.
Refer to its own fresh report and adapters; this suite does not inherit that
result and its card_adapter.h remains an explicit substitute.

lifetime_adapter.h supplies only a residue boolean and query count. The new
cases verify actual controller refusal before any worker/Binder/GOT/card work,
default-mode refusal, terminal refusal after the adapter clears, and concurrent
attempts creating no worker. They do not execute the production lifetime
observer, proc-task parser or page reclamation guard; those have separate tests.
