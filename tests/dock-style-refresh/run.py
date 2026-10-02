from pathlib import Path
import subprocess, sys, tempfile
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[2]
tests=Path(__file__).resolve().parent
jdk=Path(r'D:/build-tools/jdk-25.0.4.1+1/bin')
dock=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
failed=0
with tempfile.TemporaryDirectory(prefix='hc-dock-style-') as out:
    sources=list((tests/'stubs').rglob('*.java'))+[tests/'PrefsBridgeNotificationTest.java',root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/utils/PrefsBridge.java',root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/utils/prefs/PrefType.java']
    gate=dock/'DockRevealStyleRefreshGate.java'
    if gate.exists():sources += [gate,tests/'DockRevealStyleRefreshGateTest.java']
    z=subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',out]+[str(x) for x in sources],capture_output=True,text=True)
    if z.returncode:print(z.stderr);sys.exit(z.returncode)
    classes=['PrefsBridgeNotificationTest']
    if gate.exists():classes.append('com.sevtinge.hyperceiler.libhook.rules.home.dock.DockRevealStyleRefreshGateTest')
    else:print('FAIL style refresh gate absent');failed+=1
    for cls in classes:
        z=subprocess.run([str(jdk/'java.exe'),'-ea','-cp',out,cls],capture_output=True,text=True);print(z.stdout,end='');print(z.stderr,end='');failed+=int(z.returncode!=0)
client=(dock/'DockGlassClient.kt').read_text(encoding='utf8')
window=(dock/'HomeDockWindow.kt').read_text(encoding='utf8')
checks={
 'provider style callback independent of remote listener': 'object : ContentObserver(Handler(worker.looper))' in client and 'refreshRevealStyle(force = true)' in client,
 'new launcher window forces refresh after context binding': 'if (revealStyleWindow.get() !== window)' in window and 'glassClient.refreshRevealStyle(force = true)' in window,
 'disabled dock never resets new-window latch': 'revealStyleWindow.clear()' not in window,
 'first bind bootstraps style': 'refreshRevealStyle()' in client[client.index('fun bindDiagnostics'):client.index('private fun watchRevealStyle')],
 'style observer unregisters on close': 'unregisterContentObserver(observer)' in client[client.index('fun close()'):],
 'fallback remains 30 seconds': 'STYLE_QUERY_INTERVAL_MS = 30_000L' in client,
}
for n,ok in checks.items():print(('PASS ' if ok else 'FAIL ')+n);failed+=int(not ok)
print('Dock style refresh suite: '+('PASS' if failed==0 else 'FAIL'))
sys.exit(int(failed!=0))
