OS4 custom barrage display length: complete production callback regression

From the repository root (choose new output paths; see tests/PORTABILITY.md):
 python3 "$HYPERCEILER_TEST_DATA/acceptance/java-test12/barrage-v1/rom-v1/extract_rom.py"
 python3 tests/os4-barrage-length/verify_rom.py --output <new-rom-review>
 python3 tests/os4-barrage-length/run.py --output <new-host> --negative-controls
 python3 tests/os4-barrage-length/compile_android.py --output <new-sdk-compile>

External inputs: exact OTA MiuiBarrage.apk at the recorded extracted path, official Android37 SDK and baksmali/dexlib jars, JDK25, Kotlin2.4.10, real EzHook1.3.0/API102 dependencies, and frozen test10 compiled peer classes. The artifact/source release does not redistribute ROM/SDK binaries.

run.py compiles the full current production CustomBarrageLength.kt and drives its callbacks using actual EzHook/API102. The platform Notification/Bundle/Spannable and vendor business bodies are explicit host fixtures. It does not run vendor DEX, ART or a phone. 140 positives include all six style-selector values, both notification orientations, boundary/emoji/newline cases, actual captured WeChat return, two cache fields, icon/extra spans, original business gates/exceptions/skips, independent threads, nesting, repeat/foreign objects and five-stage rollback. Fourteen in-memory production mutants must produce concrete failing tests. A separate OS3 check resolves no OS4 target class and registers exactly the original two String hooks; original OS3 body equality is also verified statically except the corrected preference key and private method name.

compile_android.py compiles full Kotlin plus the Java app route against actual Android37/EzHook/API102 and frozen peer bytecode, with no platform stubs. This is not D8/whole-APK/ART proof. verify_rom.py checks 41 finite anchors and complete-body hashes from the actual OTA, supplementing chain-review.txt; neither keyword anchors nor host fixtures alone prove full runtime compatibility.

Selection is captured at Barrage process startup. Existing 36 means system default (actual notification 31/11), not an enforced 36. Valid non-default 12..108 count normalized String UTF-16 units before ellipsis, without icon prefix; valid surrogate pairs are not split. Original input filters, scheduling and all style business branches run unchanged. ZWJ/grapheme and actual geometry remain device acceptance work.

Latest intended positive/negative result: acceptance/java-test12/barrage-v1/host-v5.
Earlier v1 compile failure, v4 ambiguous mutation fixture failure, and rom-review-v1 parser failures are retained honestly; see explanatory files. Device status remains NOT_TESTED.
