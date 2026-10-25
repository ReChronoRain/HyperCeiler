# OS4 uninstall-dialog report option

The runtime resolver recognizes the original application's eligibility predicate
and verifies its three dialog consumers, including publication of the bool into
the dialog state. The hook returns the verified Dart false value before entering
that function's managed frame when the feature is enabled. Off/unready states
continue through the original function. It never invokes uninstall actions.

The setting is carried by the authenticated ordinary layout snapshot's HCPO
policy extension (count 2, low word first), not a separate preference listener.
Permission, framework fingerprint, provider revision, lease and policy bits
therefore share one immutable publication. CombinedSnapshot defensive copies
retain those bits. The experimental HCS1 wire has not been extended for them.

```sh
python3 tests/report-dialog-os4/run.py
# Python environment with Unicorn:
python3 tests/report-dialog-os4/verify_predicate_arm.py
# Existing full native register gateway, including the report terminal return:
python3 tests/recents-layout-os4/verify_arm.py "$ANDROID_NDK_HOME"
```

The three executable-only launcher fixtures (7751, 8583, 8584) test independent
relocation, moved byte fields, 35 instruction corruptions, missing/unknown/
ambiguous consumers and eight complete register cases each. The original ARM64
predicate runs with zero/single/multiple synthetic application objects and both
typed-predicate results. This verifies the bool polarity, compressed-reference
byte offsets and original frame return; it is not a running Dart VM/GC test.

The UI entry is exposed for OS4. Full device acceptance remains pending: load the
matching system_server endpoint, compare an originally eligible application's
uninstall dialog with the switch off/on, cancel the dialog, then restore the
setting. Do not equate source/ABI tests with a device result, and do not uninstall
an application merely to test hiding the report option.
