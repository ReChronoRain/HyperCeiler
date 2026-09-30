from pathlib import Path
import subprocess,sys,tempfile
w=Path(__file__).resolve().parents[2];jdk=Path(r'D:/build-tools/jdk-25.0.4.1+1/bin');dock=w/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock';t=w/'tests/home-dock-window'
names='DockWindowPolicy DockGlassPreset DockRecentsMotion DockUnlockReveal DockGlassRetryPolicy DockGlassSurfaceLease DockGlassProcessPolicy DockWallpaperEndpoint DockNativeMotion DockNativeMotionEndpoint DockGlassRecoveryGate DockRotationPolicy DockGlassGeometry'.split()
with tempfile.TemporaryDirectory(prefix='hc-dock-existing-') as out:
 sources=[dock/(n+'.java') for n in names]+[t/(n+'Test.java') for n in names]+list((t/'stubs').rglob('*.java'))
 z=subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',out]+[str(x) for x in sources],capture_output=True,text=True)
 if z.returncode:print(z.stderr);sys.exit(z.returncode)
 for n in names:
  z=subprocess.run([str(jdk/'java.exe'),'-ea','-cp',out,'com.sevtinge.hyperceiler.tests.dock.'+n+'Test'],capture_output=True,text=True);print(z.stdout,end='');print(z.stderr,end='')
  if z.returncode:sys.exit(z.returncode)
 print('Existing Dock regression: 13 suites passed')
