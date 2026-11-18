# Native extra-gesture state migration

Run `JAVA_HOME=<JDK 25> python3 run.py` from this directory.
The runner compiles the existing production Java HomeExtraGestureState as the
oracle. It generates 44500 state operations across 500 sessions and feeds the
same operations and original results into the production native state header
under ASan/UBSan. Expected behavior is not reimplemented in the test.

The comparison covers shake, both diagonal bottom-corner gestures, hint long
press and double tap, ownership/claim flags, cancellation, multi-pointer input,
window/revision/mask changes, invalid geometry, non-finite values and deadlines.
Geometry equality retains Java record float semantics, including signed zero.
Owner is a receiver/window generation scalar, never a retained Dart pointer.

The default-off native SDK receiver, live navigation snapshots and read-only
state integration have separate device acceptance. Production single execution
ownership and action dispatch remain pending; the current SystemUI gesture
backend remains active.

`observer_test.cpp` exercises the production snapshot-to-state adapter with
corner and double-tap sequences, publisher restarts reusing revision/owner IDs,
revision/rotation/window/timing changes, missing geometry, changed source/device/
pointer and multi-touch cancellation. It validates recognition decisions only;
the adapter cannot claim input or execute an action.

`timer_test.cpp` runs the actual monotonic timed-wait implementation, without
clock/SDK replacements. It checks single delivery, cancellation by UP/CANCEL/
movement, changed snapshot publisher/revision/window/rotation/timing/mask or
missing geometry, and teardown with a pending five-second wait. The same source
also runs as an independent Android executable. The opt-in device observer
starts this timer and joins it before publishing SDK owner-release counters.
Only memory snapshots and the state machine run on it, no Provider, Binder,
SDK invocation, pilfer or action. No timer wake occurs without a pending hold.
