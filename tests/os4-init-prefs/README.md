# Preference initialization and Home recovery regression

```sh
python3 tests/os4-init-prefs/run.py
python3 tests/os4-init-prefs/run_native.py
python3 tests/native-home-dock-listener/run.py \
  --kotlin-home "$KOTLIN_HOME" \
  --java "$JAVA_HOME/bin/java"
```

The Java suite compiles the **unmodified full production source files**
`XposedInitEntry`, `PrefsBridge`, `BaseLoad` and `PackageTarget`. It links the
project's actual libxposed API **102.0.0** JAR, including the final preference
forwarding method, and EzHookTool **1.3.0** `TargetReadyDispatcher`. No preference
retry algorithm or dispatcher state machine is reproduced in a test stub.

A Java proxy supplies a deterministic framework/remote preference transport.
Android services, logging, rule discovery and the EzXposed lifecycle adapter are
stubbed. A small fixture module replaces database rule discovery, then executes
through the real `BaseLoad.onLoad` and reads the real `PrefsBridge`. The hot
reload adapter tests callback rejection before returning a successful restore;
it does **not** emulate or prove the real framework's atomic hook replacement.

The 18 cases cover package/system-server transient failure, initial success and
cache preservation, ignored secondary packages, persistent failure, both
existing enable/framework denial paths, restored-target success/failure,
unexpected null transport, concurrent initialization, a valid empty map, failed
generation repeat callbacks, Home recovery routing behind both gates, and a
non-null preference handle whose bootstrap log-level read still throws. That
last case verifies the native probe receives the entry's explicit failed
bootstrap state; handle presence alone is insufficient. The log stub preserves
the real `getStringAsInt("log_level_v2", ...)` read and `IllegalStateException`
catch boundary; other runtime exceptions propagate through the production entry.

For the negative control, extract the original entry and run the same test:

```sh
git show 5e4686069:library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/XposedInitEntry.java \
  > /tmp/XposedInitEntry.java
python3 tests/os4-init-prefs/run.py --source /tmp/XposedInitEntry.java
```

`run_native.py` compiles the **full production Kotlin object** and a small host
JNI library that records calls and boolean arguments. It exercises actual
`System.loadLibrary`, native method resolution and preference listener callbacks.
It does not load or test the Android ARM64 module in a real launcher. Its 11 cases
cover fallback-to-recovered values, no duplicate load/configuration, dynamic
Dock state after recovery, early module-entry rejection, listener/config/read
failure retries, reentrancy, failed library load, healthy startup, still-missing
preferences, a non-null handle with explicitly failed bootstrap, rebinding to a
different preference handle whose events must reach Dock, and a controlled
late-concurrent caller. In the last case, one
thread is paused after reading `pending=true` but before acquiring the recovery
slot; another finishes recovery before it resumes. The late caller must not
reconfigure JNI or register another listener.

The unchanged older Dock listener suite should remain **8/8**. Each Java/native
case starts a fresh JVM to avoid state contamination between process lifecycles.
The scripts use existing local JDK, dependency JARs and Kotlin compiler caches;
they do not download dependencies, rebuild an APK, or connect to a phone.

Recovery is deliberately bounded by the real target-ready lifecycle. Once that
callback has failed, the dependency refuses to repeat it in the same generation.
No background retry or recovery without another legal callback is promised.
Native repair changes subsequent configuration and registers the missing Dock
listener; it cannot establish that a launcher value already cached during early
startup is recomputed. Real OS4 injection, hook effects and stability remain
separate device acceptance requirements.
