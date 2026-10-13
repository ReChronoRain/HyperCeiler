from pathlib import Path
import subprocess,sys,tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[2]
tests=Path(__file__).resolve().parent
jdk=test_paths.JAVA_HOME / 'bin'
with tempfile.TemporaryDirectory(prefix='hc-activation-') as out:
 sources=[str(x) for x in (tests/'stubs').rglob('*.java')]+[str(tests/'ActivationDialogTest.java'),str(root/'app/src/main/java/com/sevtinge/hyperceiler/utils/XposedActivateHelper.java')]
 z=subprocess.run([str(test_paths.java_tool('javac')),'-encoding','UTF-8','-d',out]+sources,capture_output=True,text=True)
 if z.returncode:print(z.stderr);sys.exit(z.returncode)
 z=subprocess.run([str(test_paths.java_tool('java')),'-ea','-cp',out,'ActivationDialogTest'],capture_output=True,text=True);print(z.stdout,end='');print(z.stderr,end='');sys.exit(z.returncode)
