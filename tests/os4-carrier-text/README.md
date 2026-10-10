# OS4 carrier text regression

Run from the project root:

```sh
python3 source/tests/os4-carrier-text/run.py --output acceptance/java-post-test8/carrier-host-NEW --negative-controls
python3 source/tests/os4-carrier-text/compile_android.py --output acceptance/java-post-test8/carrier-compile-NEW
```

The complete Kotlin production object runs through actual EzHook 1.3.0/libxposed API102 chains. Version/preferences/property reads, Android views, target classes and hook registration backend are explicit host adapters. SDK37 compilation separately uses the real Android SDK and frozen test8 peer classes, without those Android adapters. This is not ART, ROM DEX, LSPosed or phone execution.

The actual callback is `(availableCarrierCount: int, text: String, physicalSlot: int)`. `mCurrentCarrierStatus` belongs to the separate two-argument callback. The old rule's argument positions were wrong. The new OS4 branch only edits the String argument; count and slot remain unchanged. Counts outside 0–2 and slots outside 0–1 are preserved.

Mode 3 displays one device name in the fixed left slot while at least one carrier is available, including when the active SIM is physically in slot1. The right text becomes empty. Null/empty original left text is allowed here, because positive count already establishes an available carrier. Count0 preserves the original fallback text. Empty/missing device name preserves both original texts. This fixed display slot is required because the real producer's per-slot dedup uses a shared count updated by the first dispatched slot. A count-dependent display slot can lose the second refresh. The fixture executes that exact cache/dedup order and slot-filtering sequence, including dual/single/fallback/rename/late-registration cases. Shared producer caches and other listeners are not hooked or rewritten.

Mode1 preserves the complete ROM measure/accessibility path then makes the two actual separator variants invisible. Measured separator spacing is retained. Mode2 preserves the complete original privacy update then hides the carrier layout. Existing GONE state and original exceptions are preserved. HD policy is not changed on OS4. The old OS3 branch remains source-equivalent.

Mode and device-name setting changes should be followed by a SystemUI restart; no preference or property observer is added. Earlier host-v1/v2/v3 reports are historical partial checks: they lacked the real producer dedup transition coverage and must not be treated as final feature validation. The final v4 suite adds it and retains both earlier erroneous transformations as behavioral negative controls.
