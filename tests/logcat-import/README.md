Module logcat import
====================

Run `python3 tests/logcat-import/run.py` with JDK 25 or newer. The tests execute the
production parser and the DAO's SQL, including repeated captures, identical events,
multiline exceptions, clear cutoff, reboot identity, LSPosed refresh and ZIP export grouping.

The existing log refresh/export worker reads available logcat buffers using the
app's existing root access. Only HyperCeiler-prefixed tags and `HomeLayoutEngine` are
persisted; unrelated records are discarded while streaming. Direct Java/Kotlin
diagnostics use `ModuleLog` to identify previously generic tags. Native tags remain
unchanged. `AndroidLog` records already delivered to the App database are excluded
from reimport. Logcat records survive LSPosed refresh and are included in the
existing App/module views and exports. No database migration or persistent polling
thread is needed. Logs already evicted from Android's buffers cannot be recovered.


The desktop patch engine now emits `HyperCeiler.HomeLayoutEngine`. Older bare tags are
accepted and canonicalized after the raw event identity is hashed, preserving
repeat-import IDs. `LogEntry` normalizes historical row getters for list/detail
and export without deleting the original diagnostic events. The legacy literals
exist only for read compatibility; native filesystem/debug inputs retain their
existing names so upgrades continue to find configuration and caches.
