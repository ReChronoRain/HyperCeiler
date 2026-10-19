# OS4 PIN layout host regression

Run from the project root:

```sh
python3 source/tests/os4-pin-layout/run.py --output acceptance/java-post-test8/pin-host-NEW --negative-controls
python3 source/tests/os4-pin-layout/compile_android.py --output acceptance/java-post-test8/pin-compile-NEW
```

The runner compiles the complete production `ScramblePIN.java` and executes real EzHook 1.3.0 / libxposed API 102 hook dispatch. Android hierarchy, original inflation, click listeners and the credential comparison are host fixtures. They model identities and observable operations from the complete extracted OS4 XML and DEX; they do not execute ART, the device's credential verifier, LSPosed or a real unlock.

The new branch selects the exact main `KeyguardPINView`. It only reorders ten existing `NumPadKey` view objects after validating the complete 0–9 set, unique identities, parent rows, slot layout parameters and the two animation matrices. The first three rows remain shared; both bottom numeric references update together. It preserves the bottom spacer/confirm container, other controls, labels, IDs, digit fields and attached listeners. Tiny subclasses and unknown layouts are unchanged. The OS3 branch remains byte-for-byte source-equivalent after removing the new version gate.

Tests include 50 random draws and repeated invocations, valid and invalid input through unchanged fixture listeners, disabled input, unknown layouts, registration failure, and one-shot hierarchy exceptions at each of the 20 remove/add operations both before and after its effect. Re-inflation that rebuilds ID-order matrices against an already shuffled hierarchy is declined. There is no requirement that every random draw change the order; the distribution itself is not a cryptographic audit.

Rollback is best effort when Android itself continually refuses hierarchy operations. The persistent-add-failure test explicitly demonstrates that digits can remain detached although the old matrices were not published over. This case passes only the diagnostic-boundary assertion; **it does not demonstrate restored UI**. Such an underlying framework failure requires recreating the affected view/SystemUI. No host test is labeled a physical unlock acceptance result.

Evidence `pin-host-v1` retains a failed negative-control compilation caused by a missing brace in the test mutation. It is not counted as a rejected behavioral mutation. Final `pin-host-v3` fixes that test and uses a deterministic malformed re-inflate fixture; production logic was unchanged between v1 and v3.
