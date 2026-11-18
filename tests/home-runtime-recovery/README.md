# Root runtime client recovery

Run `JAVA_HOME=<JDK 25> python3 run.py` in this directory.

The test compiles the actual `HomeConfigRootClient` and wire constants. Android
Handler scheduling, libsu binding, Binder death notifications and the remote
agent are transport fixtures because these APIs are unavailable on the host.
The actual client runs its real background executor; the tests prove that SDK
initialization leaves the caller thread, duplicate notifications do not create
extra retries, and stale initialization cannot refresh a disabled session.

Cases include initial denial, revocation, duplicate starts, process death,
already-dead Binder, deferred cleanup after libsu connection-map iteration, capped backoff, connection timeout, null binding,
incompatible protocol, one-shot probe, explicit disable, cancellation during
backoff, late replies and partial framework availability. The tests do not
duplicate the recovery implementation and do not establish Android reboot,
permission-manager or process-restart behavior.

Recovery currently depends on the module client process remaining alive to
receive the Binder death event. Recovering after both the client and root daemon
have exited requires a separate authenticated bootstrap path and is not claimed
by these tests. Repeated Root permission requests are not an automatic retry
strategy; recovery uses an existing known grant and pauses after denial or an
incompatible/null endpoint.

The initial 15-second deadline diagnoses a pending Binder bootstrap without
repeatedly adding bindings to libsu. libsu 6.0.0 keeps `pendingTasks` and
`DAEMON_EN_ROUTE` until bootstrap, and `unbind` only removes registered
connections. Retain that single pending session so a late callback can initialize
it. A delivered Binder gets a fresh 15-second initialization deadline; metadata
failures and confirmed deaths still use bounded backoff. If bootstrap never
arrives, this is still unavailable, not successful recovery. Independently
restarting a lost bootstrap is not implemented.

Cancelled bindings can arrive later and must be unbound outside libsu's callback
iteration, without initializing an obsolete session or stopping a newer owner.
The production service's `onUnbind` body is compiled into the host lifecycle
fixture: active modes retain the daemon, disabled modes stop after dispatch,
and a re-enabled mode is checked before stopping. Other service methods and
process teardown are not thereby verified.

Run `python3 sdk_manager_test.py` in this directory for four integration cases.
It loads the actual `RootServiceManager` class and nested classes from the pinned
Gradle 6.0.0 AAR, recording its hash. Only Android scheduling/collections, remote
Binder peers, and the RootService API adapter are fixtures; the manager's pending
list, flags, refcounts, callback traversal, and retry interaction run unchanged.
It never executes Shell tasks or accesses a device. Resolve the Gradle service
dependency first. Tests cover a delayed bootstrap, a callback after disable,
a callback overlapping a new owner, and actual manager death dispatch.

An optional `HC_ROOT_CLIENT_SOURCE=/path/to/HomeConfigRootClient.java` override
allows running the same manager integration against an earlier client source.
The pre-fix client reproduces the pending-binding regression. Upstream contract:
[RootServiceManager at 6.0.0](https://github.com/topjohnwu/libsu/blob/6.0.0/service/src/main/java/com/topjohnwu/superuser/internal/RootServiceManager.java).
Host passes do not establish whether HyperOS delivers or defers bootstrap, Root
authorization behavior, cold startup, freezer recovery, or actual process restart.
