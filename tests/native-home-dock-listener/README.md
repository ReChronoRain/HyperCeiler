# OS4 native Dock preference listener regression test

The runner compiles the production `NativeHomeHooksOS4.kt` with small Android and preference stubs, then invokes the real registration method and dispatches callbacks through the registered listener. It checks stored-key enable/disable delivery, null clear notifications, unrelated-key isolation, duplicate registration, a switch change during registration, unavailable preferences, registration exceptions, and listener lifetime with a weak registry.

Use a JDK 17+ and an unpacked Kotlin 2.4.10 compiler:

```sh
python3 tests/native-home-dock-listener/run.py --kotlin-home /path/to/kotlinc
```

`--java /path/to/java.exe` supports a Windows JDK from WSL. If needed, set `HC_TEST_TMPDIR` to a directory on a Windows-mounted drive for compiler output.

`--production /path/to/NativeHomeHooksOS4.kt` runs the same suite against an unmodified baseline. The recovery cases invoke the private registration method again through reflection; the patch makes registration retryable but adds no automatic retry scheduling.

JNI is intentionally unavailable in this host test. Production catches that unavailable-native failure; the test observes the actual preference reads and completed callback logs. It does not validate native hooking, a launcher binary, LSPosed transport delivery, or any device UI. The weak-registry test checks the Android API's permitted ownership behavior, not an assertion that current LSPosed remote preferences use weak references.
