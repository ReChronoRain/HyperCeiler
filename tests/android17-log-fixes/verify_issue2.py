"""Check the Android 17 SharedUserPatch diagnostic fallback."""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
source = Path(sys.argv[1]) if len(sys.argv) > 1 else (
    ROOT / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemframework/corepatch/SharedUserPatch.java"
)
text = source.read_text(encoding="utf-8")
block = text.split("if (CorePatchHelper.isSharedUserEnabled())", 1)[1].split("// Android 11+", 1)[0]
checks = {
    "Android 17 final-field failure is identified": bool(re.search(
        r"e instanceof IllegalAccessException.*?Build\.VERSION\.SDK_INT >= 37.*?targetSdk 37", block, re.S
    )),
    "failure is warning and does not claim bypass succeeded": "XposedLog.w(TAG, \"system\"" in block
    and "sharedUser bypass is inactive" in block,
    "reflection remains catchable and JNI is not used": "field.set(null, true)" in block
    and "SetStaticBooleanField" not in text,
    "other CorePatch hooks remain present": all(
        token in text for token in ("hookVerifySignatures(utilClass)", '"removePackage"', '"addPackage"')
    ),
}
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
sys.exit(0 if all(checks.values()) else 1)
