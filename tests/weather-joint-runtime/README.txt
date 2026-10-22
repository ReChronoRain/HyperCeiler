Joint Weather DeviceLevel + material runtime regression with card routing adapter

Run from the project root, always using a new evidence directory:
  python3 -B source/tests/weather-joint-runtime/run.py --negative-controls --output <new-output>

Previous sealed complete run:
  acceptance/weather-blur-next/joint-runtime-v2/report.json
  34 cases, 6 meaningful production-source negative controls, all passed.

The expanded runner preserves all 34 cases and six mutations, adds 20 card
routing cases and seven source mutations. All54 cases/13mutations remain;
four early-lifetime-residue admission cases and a real-gate deletion mutation
now require exactly58 cases and14 mutations when negative controls are requested.
Current new-run evidence is under acceptance/weather-card-next, always in a
new directory. Read the report result rather than inferring success here.

Both production translation units weather_runtime.cpp and
weather_material_runtime.cpp execute together in each forked process. Their
real HWS1 Cache/decoder and weather_device_level/weather_material_support
policies remain intact. No production installation or scheduling logic is
copied into the fixture.

The new card_adapter.h explicitly substitutes weather_card_runtime for this
suite. It only records prepare/install/service/cancel routing, configurable
results and the actual shared fresh-snapshot callback. No card installer,
ARM gateway, code lease publisher or renderer executes here. This is two real
production runtimes plus a card adapter, NOT three real production runtimes.
Independent card-component and three-runtime tests are separate evidence.

Other adapters cover Binder transport, BOOTTIME/poll scheduling/thread
counting, readonly image identities/binding resolution and mprotect fault
injection. Memory mapping, mprotect, proc-maps permissions, process_vm_readv,
atomic GOT compare/exchange, real worker pthread and condition-variable
initial wait all execute. Exactly one HWS worker is counted, even with both
features enabled or concurrent entry attempts.

calls.S uses actual x86 CALL instructions with return-address difference
0x5fc76c - 0x4402e4, so both production caller checks share one synthetic
Weather base. This is host callsite evidence, not an AArch64/Dart ABI claim.
The binding adapters still read real mapped GOT words. The actual complete
ROM byte guards are separately verified by the two binding test suites.

Coverage includes blur-only, level-only, both, independent identity failures,
clean rollback, foreign GOT owner retention, each component's protection
residue closing the startup batch, early/mismatched Dart observation, shared
lease expiry, missing/changed image identities, and a blocked original property
query crossing the final cold-state observation and installer commit. The
early-query case proves a late installer cannot authorize that query.

The first run joint-runtime-v1 is preserved as an unsuccessful test-suite
validation: 34/34 positives passed but one of six mutations was redundant.
Removing g_level_installed alone did not bypass the independent live GOT
ownership check after rollback. The final runner retains production and that
positive scenario, replacing the redundant mutation with bypassing live level
binding validation. Compilation failures and timeouts do not count as detected
negative controls.

This does not launch Android Weather, Binder service, actual loader, Dart VM,
GC or renderer. Each case forks and exits; exit ends detached workers. It does
not prove cancellation of an indefinitely blocked Binder call. Full native
Lazy execution and Dart branch evidence are in the separate ARM suite. Phone
status remains NOT_TESTED. Regression case counts are not feature coverage.

New card routing includes raw 2/3/default/invalid frames, card-only no GOT
writes, independent preparation failures, card Residue refusing both actual
native policy callbacks, early/wrong Dart cancellation, first-window miss,
shared snapshot refresh and exactly one worker under concurrent starts.
Refresh observations do not prove already cached Dart objects update.

The separate source/tests/weather-card-joint-runtime suite includes all three
actual production translation units and real card binding/ELF mapped bytes.
Refer to its own fresh report and adapters; this suite does not inherit that
result and its card_adapter.h remains an explicit substitute.

lifetime_adapter.h supplies only a residue boolean and query count. The four
new cases verify the actual controller keeps both native runtimes cold and
starts zero workers before any Binder/GOT/card work, including concurrent and
subsequent retries. No real lifetime observer/proc-task/page-protection algorithm
executes in this fixture; the separate lifetime component is not substituted
silently or credited as tested here.
