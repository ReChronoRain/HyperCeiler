# OS4 folder auto-close regression

Production resolves `AppIconRemoteClick.handleRemoteClick` and its original close/Rx/registration callees from the loaded image's symbol index, then validates the control-flow shape. It modifies one mode-gate instruction; the original registered/open-folder checks and close implementation remain untouched. Disable restores the original word using the shared source-word ownership/recovery journal. No code VA, pool offset or object-field offset is a production profile.

Run from the repository root:

- `python tests/folder-auto-close-os4/verify.py` — recorded AOT fixture, instruction mutations, relocation, extension defaults and exact restoration.
- `python tests/folder-auto-close-os4/verify_cache.py` — extracted production cache serializer/parser; v12 round trips and v10/v11 compatibility.
- `python tests/folder-auto-close-os4/verify_java.py` — production provider/endpoint and preference gate with Android substitutes.
- `python tests/folder-auto-close-os4/verify_arm64.py` — original AOT close-path instructions under Unicorn, with mocked Dart callees; not a full launcher emulation.

`ZIG` may select the Zig executable; `JAVA_HOME` may select the JDK. The ARM test uses the existing `tests/back-gesture-os4/runtime` Unicorn installation. The default local tool paths match the existing native audit tests.

The recorded AOT addresses belong only to the test fixture. Phone captures, APKs and rollback preimages remain in the ignored repository-root `handover/` directory, not this test suite.
