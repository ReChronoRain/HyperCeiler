# Activation dialog regression

Run `python tests/activation-dialog/run.py [SOURCE_TREE]` using the configured host JDK.
The harness compiles the real XposedActivateHelper with deterministic Android/UI test doubles.
It covers active startup, binding after the old 2s deadline, 10s inactive grace, binding after
warning display, main-thread dispatch, background cancellation, destroyed windows, rotation,
service recovery, ignored-dialog resume and non-Activity contexts. It performs no phone operations.

Production integration: volatile Application.isModuleActivated publishes Binder state;
setModuleActivated notifies the helper. HomePageActivity resumes checks only after its UI is
initialized, and cancels on pause/destroy. DialogHelper returns the actual AlertDialog so an
activation event can dismiss it. No persisted active-state cache or recurring poll is used.
A late or missing callback can still show a warning after 10s; an eventual live bind clears it.
