# Weather material controller host regression

Run from the project root with a new output directory:

```sh
python3 source/tests/weather-material-runtime/run.py --output acceptance/weather-blur-next/runtime-host-NEW --negative-controls
```

The fixture includes the complete production `weather_material_runtime.cpp`, the real material policy and the real HWS1 cache. It supplies a genuine x86 return address through an assembly caller, executes anonymous `mmap`, page protection changes, atomic GOT compare/exchange, `process_vm_readv`, process isolation and controlled thread interleavings. UBSan and compiler warnings are enabled.

Image identity resolution, Dart identity and the initial cold-state observation are explicit adapters. Permission failure injection surrounds real `mprotect`. Authorization reads use the real cache with a controlled monotonic time value; this fixture does not execute Binder, Android lifecycle, the pinned ARM64 libraries or their renderer. The independent binding and ARM policy suites cover their separately stated scopes.

The 37 cases cover unchanged original call arguments and exactly one call, exact caller/property filtering, no override on missing/expired/disabled authorization, no late activation after a first missed material query, publication before the GOT becomes observable, conflicting slot ownership, permission restoration and rollback residue, one installation writer, and the `Preparing` race. In the latter case the first original getter blocks while the installer receives a deliberately stale final cold-state observation: it must roll back rather than commit `Installed` after the query consumed its window. Calling the wrapper during Rust Once's running state is allowed; this is not a test of contested Rust Once internals.

Nine source-copy negative controls must compile, complete the suite, and fail their named semantic witness. Compilation errors, a timeout or a signal are not accepted as negative-test success. Production source files are never modified. Input hashes are compared before and after the batch.

The wrapper never resets the application's native or Dart capability caches. Future override refusal does not undo values already cached by Weather. Device rendering, LSPosed/Hyos injection and long-duration stability remain `NOT_TESTED`.
