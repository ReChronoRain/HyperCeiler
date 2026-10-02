from pathlib import Path
import sys

source = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2] / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeDockWindow.kt'
text = source.read_text(encoding='utf-8')
checks = {
    'state reply gates covered launcher': 'reply.writeInt(if (desktopDrawable()) 1 else 0)' in text,
    'desktop requires panel and visible window': 'private fun desktopDrawable(): Boolean = screenInteractive() && launcherWindowVisible()' in text,
    'visibility query uses WM then layer lock': 'synchronized(wm.getObjectFieldAs<Any>(WM_LOCK)) {\n            synchronized(layers)' in text,
    'startup and missing layer fail open': 'layers.isEmpty() || layers.keys.any { it.callMethod("isVisible") == true }' in text,
    'system sweep pauses when launcher covered': 'if (!desktopDrawable()) {\n                    sweepIntervalMs = SWEEP_DOZE_INTERVAL_MS' in text,
}
for name, passed in checks.items():
    print(('PASS' if passed else 'FAIL') + ' ' + name)
print(f'BACKGROUND_POWER_TEST: {sum(checks.values())} passed; {len(checks) - sum(checks.values())} failed')
raise SystemExit(0 if all(checks.values()) else 1)
