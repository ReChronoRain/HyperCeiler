from pathlib import Path
import subprocess,sys
D=Path(__file__).resolve().parent;W=D.parents[1];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
out=D/'java-classes';out.mkdir(exist_ok=True)
jdk=Path('D:/build-tools/jdk-25.0.4.1+1/bin')
sources=list((W/'tests/home-layout-native/stubs').rglob('*.java'))+list((W/'tests/home-dock-window/stubs').rglob('*.java'))+[R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java',R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java',D/'FolderLayoutEndpointTest.java',W/'tests/home-layout-native/HomeLayoutNativeEndpointOS4Test.java']
r=subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(out)]+list(map(str,sources)),capture_output=True,text=True,encoding='utf8');print(r.stdout+r.stderr,end='')
if r.returncode:sys.exit(r.returncode)
for cls in ['HomeLayoutNativeEndpointOS4Test','FolderLayoutEndpointTest']:
 r=subprocess.run([str(jdk/'java.exe'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.libhook.rules.home.dock.'+cls],capture_output=True,text=True,encoding='utf8');print(r.stdout+r.stderr,end='')
 if r.returncode:sys.exit(r.returncode)
print('LEGACY_JAVA_ENDPOINT=PASS; NEW_FOLDER_ENDPOINT=PASS')
