# OS4 notification count callback

This suite compiles and executes the complete `RemoveNotifNumLimitOS4` and `SystemUIB` classes. The rule preserves `CountLimitCoordinator.attach` and replaces only the reviewed OS4 `CountLimitCoordinator$$ExternalSyntheticLambda0.onViewBound$1(NotificationEntry):void` callback. This callback selects groups exceeding Xiaomi's count quota, then dismisses an eligible notification from each such group. This does not remove system-server posting quotas or every cause of notification removal.

Run from the workspace project root:

```sh
python3 source/tests/os4-notification-count/run.py --output acceptance/remaining-features-next/notification-tests-new --negative-controls
python3 source/tests/os4-notification-count/compile_android.py --output acceptance/remaining-features-next/notification-compile-new
```

The 22 host cases cover OS3/OS4 × disabled/enabled routing, exact method/overload choice, skipping the original callback, preserving attach and other bind/dismiss methods, repeated dispatch, null arguments, missing and mismatched classes/methods/owner fields, and backend failures. The dependency path uses real EzHook 1.3.0 `Hooks.createHook`, `HookFactory`, and `InvocationContext`, with API102 `Chain`; a backend adapter captures the installed hooker. The BaseHook adapter delegates to the same dependency method as the actual production overload. Android registration, class resolution, and the original callback body are host adapters. The simple quota body is only a scope witness, not a JVM execution of the ROM's DEX.

Five source mutations are compiled in temporary directories: removing the hook, changing its method name, reversing the preference, removing the route, and dropping the HyperOS4 gate. Each must complete the fixture and fail meaningful assertions. They never modify production files.

The independent compile uses Android SDK37 and the real API102/EzHook dependency jars plus frozen test6 peer classes. It is not an APK build. ROM method/consumer evidence and hashes are under `acceptance/remaining-features-next/`. Physical-device injection, notification behavior, and long-term stability remain `NOT_TESTED`. The SDK35 legacy class and route are unchanged. Unknown future ROM shapes fail this rule instead of falling back to broader hooks; matching the Java shape is not a runtime bytecode identity check.
