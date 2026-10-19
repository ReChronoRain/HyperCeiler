# Actual-ROM migration audit

This is a read-only audit of initialized OS3 phone-launcher hook families and all
Gallery/Weather entry hooks against the supplied OS4 ROM. It produces evidence
and unresolved gaps, not a runtime compatibility score. It does not modify any
APK, native image, production hook or preference protocol.

```sh
python3 tests/native-remaining-audit/audit.py \
  --project "$HYPERCEILER_TEST_DATA" \
  --objdump "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump"
```

Requires the verified `firmware/component-inventory.json`, the three extracted
APKs and `firmware/launcher-native/lib/arm64-v8a/` fixtures. It independently
checks APK SHA256, DEX entries, and extracted native SHA256 against each APK.
The generated `acceptance/native-remaining-audit.json` has every feature's
source hash, entry gate, preference references, status and unresolved evidence.
The text version is a readable companion. Exact symbol body bytes and ARM64
disassemblies are saved under `acceptance/native-evidence/`.

Gallery/Weather ELF exports and debug-section availability are read directly
from the supplied binaries. Launcher private symbols are independently decoded
from `.gnu_debugdata` by the existing Python ELF fixture reader. Function bodies
must lie inside an executable section; ambiguous names are retained as separate
candidates, never reduced to a guessed address.

`not_wired_in_reviewed_os4_home_entry` is deliberately scoped. Shared framework
hooks and external dynamically loaded classes require independent examination;
this status is not a claim of device-observed failure. An existing native path,
export name or preference whitelist entry is never labeled device-verified.

No firmware or derived native binary belongs in version control. Runtime
acceptance still requires the real phone, LSPosed logs, setting-level effects,
default restoration, app/reboot persistence and prolonged stability tests.
