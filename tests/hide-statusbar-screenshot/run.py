from pathlib import Path
import subprocess, sys, tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
tests = Path(__file__).resolve().parent
libhook = root / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
rule = libhook / 'rules/systemui/statusbar/HideStatusBarBeforeScreenshot.java'
failed = 0


with tempfile.TemporaryDirectory(prefix='hc-hide-statusbar-') as out:
    sources = list((tests / 'stubs').rglob('*.java'))
    sources += [rule, tests / 'HideStatusBarScreenshotRuleTest.java']
    z = subprocess.run([str(test_paths.java_tool('javac')), '-J-Duser.language=en', '-encoding', 'UTF-8', '-d', out]
                       + [str(x) for x in sources], capture_output=True, text=True,
                       encoding='utf-8', errors='replace')
    if z.returncode:
        print(z.stdout, end='')
        print(z.stderr, end='')
        sys.exit(z.returncode)
    z = subprocess.run([str(test_paths.java_tool('java')), '-ea', '-cp', out,
                        'com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar'
                        '.HideStatusBarScreenshotRuleTest'],
                       capture_output=True, text=True, encoding='utf-8', errors='replace')
    print(z.stdout, end='')
    print(z.stderr, end='')
    failed += int(z.returncode != 0)

system_ui = (libhook / 'app/SystemUI/SystemUIV.java').read_text(encoding='utf8')
system_ui_b = (libhook / 'app/SystemUI/SystemUIB.java').read_text(encoding='utf8')
base_load = (libhook / 'base/BaseLoad.java').read_text(encoding='utf8')
rule_text = rule.read_text(encoding='utf8')
checks = {
    'SystemUIV registers hide-status-bar rule once':
        system_ui.count('initHook(new HideStatusBarBeforeScreenshot()') == 1,
    'SystemUIB registers hide-status-bar rule once':
        system_ui_b.count('initHook(new HideStatusBarBeforeScreenshot()') == 1,
    'BaseLoad rejects duplicated hook registration':
        'sInitializedHookClasses.add(hookClass)' in base_load
        and 'sInitializedHookClasses.clear()' in base_load,
    'rule keeps process-wide state instead of instance fields':
        'private static final Map<View, HiddenStatusBar>' in rule_text
        and 'mVisibilityBeforeScreenshot' not in rule_text,
    'rule restores every hidden status bar on finish':
        'restoreHiddenStatusBars();' in rule_text,
}
for name, ok in checks.items():
    print(('PASS ' if ok else 'FAIL ') + name)
    failed += int(not ok)

print('Hide status bar screenshot suite: ' + ('PASS' if failed == 0 else 'FAIL'))
sys.exit(int(failed != 0))
