from pathlib import Path
import subprocess,sys,tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

w=Path(__file__).resolve().parents[2];jdk=test_paths.JAVA_HOME / 'bin';dock=w/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock';t=w/'tests/home-dock-window'
names='DockWindowPolicy DockGlassPreset DockRecentsMotion DockUnlockReveal DockGlassRetryPolicy DockGlassSurfaceLease DockGlassProcessPolicy DockWallpaperEndpoint DockNativeMotion DockNativeMotionEndpoint DockGlassRecoveryGate DockRotationPolicy DockGlassGeometry'.split()
with tempfile.TemporaryDirectory(prefix='hc-dock-existing-') as out:
 sources=[dock/(n+'.java') for n in names]+[t/(n+'Test.java') for n in names]+list((t/'stubs').rglob('*.java'))
 z=subprocess.run([str(test_paths.java_tool('javac')),'-encoding','UTF-8','-d',out]+[str(x) for x in sources],capture_output=True,text=True)
 if z.returncode:print(z.stderr);sys.exit(z.returncode)
 for n in names:
  z=subprocess.run([str(test_paths.java_tool('java')),'-ea','-cp',out,'com.sevtinge.hyperceiler.tests.dock.'+n+'Test'],capture_output=True,text=True);print(z.stdout,end='');print(z.stderr,end='')
  if z.returncode:sys.exit(z.returncode)
 print('Existing Dock regression: 13 suites passed')
