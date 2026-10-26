# OS4 workspace touch lifecycle regression

A third-party report described occasional loss of desktop swiping. This suite
reproduces a concrete event-chain defect in the shipping native scene adapter;
it does not establish that every reported incident has this cause.

With custom vertical workspace gestures enabled and scope-authorized:

1. DOWN reaches the original receiver; MOVE claims the custom gesture and sends
   the original receiver a synthetic CANCEL.
2. One source-bank readiness gate closes. A genuine CANCEL and the next physical
   DOWN can arrive while that gate is closed.
3. The old adapter bypassed its recognizer entirely while unready, so the old
   `active/cancelled` state survived even though the new DOWN reached the desktop.
4. When readiness recovered, MOVE and UP from the new stream were consumed by
   that old state. The original receiver retained a pressed sequence with no UP.

The pre-fix production-adapter reproduction produced original actions
`DOWN, CANCEL, CANCEL, DOWN` and left the stock receiver active after physical UP.
The fixed reproduction produces `DOWN, CANCEL, DOWN, MOVE, UP` and ends inactive.

Readiness now gates new claims rather than delivery of lifecycle events to an
existing recognizer. Already cancelled streams drain without dispatching a
stale custom action. DOWN received while unready resets the old sequence and
cannot become authorized halfway through the next stream. Its timestamp
suppression interval also clears on the new DOWN. When the event ABI is absent,
native sequence state is retired without touching the event. The hot path skips
all extra event decoding when no custom claim, old stream or suppression window
exists.

```sh
python3 tests/home-touch-lifecycle/run.py
```

The test compiles the actual native API/global state declarations, borrowed-event
forwarder, readiness methods and scene callback from `home_gestures_os4.cpp`,
with the production `SwipeRecognizer`. MotionEvent APIs and the original receiver
are observable host fixtures; action queue I/O is mocked. Eighteen sequences
cover readiness gaps with/without CANCEL, both readiness gates, unclaimed and
cancelled streams, settings/editing changes, two fingers, full action/index
restoration, unavailable ABI, ordinary paging, scope rejection, long gaps and
normal one-shot custom-gesture dispatch.

Additional existing checks exercise extra-navigation receiver ownership, region
selection and SystemUI quick-switch cleanup. Release compilation/signing must
also pass. No phone operation or install is performed by this suite. Device
acceptance still needs repeated swipe interruptions, app return, editing and
multi-finger gestures with the affected user's configuration; host lifecycle
checks do not prove hardware/input-dispatch behavior on that device.
