from pathlib import Path
import subprocess,sys,os
W=Path(__file__).resolve().parents[2];D=Path(__file__).resolve().parent
R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
header=R/'app/src/main/cpp/targets/home/home_folder_auto_close.h'
if not header.exists():print('FOLDER_AUTO_CLOSE=absent; OS4 control gated');sys.exit(1)
zig=os.environ.get('ZIG',str(W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'))
out=D/'run';out.mkdir(exist_ok=True)
subprocess.run([zig,'c++','-std=c++20','-I'+str(header.parent),str(D/'FolderAutoCloseTest.cpp'),'-o',str(out/'test.exe')],check=True)
subprocess.run([str(out/'test.exe')],check=True)
