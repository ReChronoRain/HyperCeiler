from pathlib import Path
import subprocess,sys,tempfile
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[2]
tests=Path(__file__).resolve().parent
jdk=Path(r'D:/build-tools/jdk-25.0.4.1+1/bin')
with tempfile.TemporaryDirectory(prefix='hc-activation-') as out:
 sources=[str(x) for x in (tests/'stubs').rglob('*.java')]+[str(tests/'ActivationDialogTest.java'),str(root/'app/src/main/java/com/sevtinge/hyperceiler/utils/XposedActivateHelper.java')]
 z=subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',out]+sources,capture_output=True,text=True)
 if z.returncode:print(z.stderr);sys.exit(z.returncode)
 z=subprocess.run([str(jdk/'java.exe'),'-ea','-cp',out,'ActivationDialogTest'],capture_output=True,text=True);print(z.stdout,end='');print(z.stderr,end='');sys.exit(z.returncode)
