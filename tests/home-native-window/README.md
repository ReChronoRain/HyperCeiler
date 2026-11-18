# Native window lifecycle probe

This opt-in passive probe intercepts only the launcher's SDK callers of the standard NDK `AIBinder_Class_define` C API. It validates the live relocation slots against the actual platform function and preserves the original constructors, destructors, callback arguments, Parcel cursor and return value. It records callback identity and counters only, without decoding messages, consuming input, submitting surfaces or selecting a renderer backend.

`android_callback_test.cpp` compiles the production implementation and uses real Android NDK Binder objects and Parcels. Run the binary under an application UID. It checks original dispatch, bad-payload status, construction/destruction, disabled pass-through and multiple-window/multiple-dispatcher diagnostics. It does not register any window with the system. This test does not prove that the factory can be captured before the real launcher's window initialization; that requires a separate launcher process test.

The property `debug.hyperceiler.home_window_probe=1` is for bounded device experiments only. Its default is disabled. No private Rust callback ABI or vendor offset is supplied by the module.
