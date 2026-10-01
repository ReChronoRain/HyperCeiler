from pathlib import Path
import subprocess, sys, tempfile
workspace = Path(__file__).resolve().parents[2]
root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else workspace
tests = workspace / 'tests/home-layout-native'
jdk = Path('D:/build-tools/jdk-25.0.4.1+1/bin')
with tempfile.TemporaryDirectory(prefix='hc-atomic-reload-') as out:
    sources = list((tests/'stubs').rglob('*.java')) + list((workspace/'tests/home-dock-window/stubs').rglob('*.java'))
    sources += [root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java', tests/'HomeLayoutAtomicReloadTest.java']
    helper = root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java'
    if helper.exists(): sources.append(helper)
    z = subprocess.run([str(jdk/'javac.exe'), '-encoding','UTF-8','-d',out]+[str(s) for s in sources], capture_output=True,text=True)
    if z.returncode: print(z.stderr); sys.exit(z.returncode)
    z = subprocess.run([str(jdk/'java.exe'),'-ea','-cp',out,'com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutAtomicReloadTest'],capture_output=True,text=True)
    print(z.stdout,end=''); print(z.stderr,end=''); sys.exit(z.returncode)
