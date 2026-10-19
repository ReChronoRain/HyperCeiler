Module logcat import
====================

Run `python3 tests/logcat-import/run.py` with JDK 25 or newer. The tests execute the
production parser and the DAO's SQL, including repeated captures, identical events,
multiline exceptions, clear cutoff, reboot identity, LSPosed refresh and ZIP export grouping.

The existing log refresh/export worker reads available logcat buffers using the
app's existing root access. Only HyperCeiler-prefixed tags and `HomeTweaks` are
persisted; unrelated records are discarded while streaming. Direct Java/Kotlin
diagnostics use `ModuleLog` to identify previously generic tags. Native tags remain
unchanged. `AndroidLog` records already delivered to the App database are excluded
from reimport. Logcat records survive LSPosed refresh and are included in the
existing App/module views and exports. No database migration or persistent polling
thread is needed. Logs already evicted from Android's buffers cannot be recovered.
