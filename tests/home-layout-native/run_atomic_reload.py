from pathlib import Path
import subprocess, sys, tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

workspace = Path(__file__).resolve().parents[2]
root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else workspace
tests = workspace / 'tests/home-layout-native'
jdk = test_paths.JAVA_HOME / 'bin'
with tempfile.TemporaryDirectory(prefix='hc-atomic-reload-') as out:
    sources = list((tests/'stubs').rglob('*.java')) + list((workspace/'tests/home-dock-window/stubs').rglob('*.java'))
    sources += [root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java', tests/'HomeLayoutAtomicReloadTest.java']
    helper = root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java'
    if helper.exists(): sources.append(helper)
    gate = root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/FrameworkHookGate.java'
    if gate.exists(): sources.append(gate)
    z = subprocess.run([str(test_paths.java_tool('javac')), '-encoding','UTF-8','-d',out]+[str(s) for s in sources], capture_output=True,text=True)
    if z.returncode: print(z.stderr); sys.exit(z.returncode)
    z = subprocess.run([str(test_paths.java_tool('java')),'-ea','-cp',out,'com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutAtomicReloadTest'],capture_output=True,text=True)
    print(z.stdout,end=''); print(z.stderr,end=''); sys.exit(z.returncode)
