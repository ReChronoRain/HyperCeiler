from pathlib import Path
import subprocess, sys, zipfile, shutil, hashlib, json, os
O=Path(__file__).resolve().parent; W=O.parents[1]
B=Path(os.environ.get('HC_NDK_BIN','D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin'))
mode=sys.argv[1]; root=W
if mode in ('baseline','rollback'):
    root=O/(mode+'-tree')
    shutil.copytree(W/'app/src/main/cpp/targets/home',root/'app/src/main/cpp/targets/home',dirs_exist_ok=True)
    if mode=='baseline':
        with zipfile.ZipFile(O/'ORIGINAL_FILE.zip') as z:z.extractall(root)
    else:
        for n in json.loads((O/'modified-hashes.json').read_text()):
            p=root/n;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(W/n,p)
        p=subprocess.run([shutil.which('bash') or 'D:/Git/bin/bash.exe',str(O/'ROLLBACK.sh'),root.as_posix()],capture_output=True,text=True)
        print(p.stdout,end=''); assert p.returncode==0,p.stderr
        hashes=json.loads((O/'original-hashes.json').read_text())
        assert all(hashlib.sha256((root/n).read_bytes()).hexdigest()==s for n,s in hashes.items())

# The link inputs are rebuilt from source on every run rather than committed as
# binaries. They used to be read from an absolute path under a scratch directory
# outside the repo, which meant a fresh clone could not run this suite at all.
#   crt.obj      freestanding PE entry (host-link/crt.c) - no CRT is linked
#   *.lib        import libraries synthesised from the .def files next to them
# Every symbol the freestanding tests actually use is in those .def files: the
# shim has no stdio beyond puts/printf and no allocation, so the surface is tiny.
L=O/'host-link'
def build(name,cmd):
    p=subprocess.run(cmd,capture_output=True,text=True)
    assert p.returncode==0,p.stderr
build('crt',[str(B/'clang-cl.exe'),'--target=x86_64-pc-windows-msvc','-c',str(L/'crt.c'),'-o',str(L/'crt.obj')])
for stem in ('msvcrt','kernel32'):
    build(stem,[str(B/'llvm-dlltool.exe'),'-d',str(L/(stem+'.def')),'-l',str(L/(stem+'.lib'))])
build('printf',[str(B/'llvm-dlltool.exe'),'-d',str(L/'printf.def'),'-l',str(L/'printf.lib')])

sources=[O/'regression.cpp']
if mode=='modified':sources += [W/'tests/home-layout-native'/(n+'.cpp') for n in ['workspace_geometry_test','folder_geometry_test','folder_render_snapshot_test','drop_render_snapshot_test']]
# The freestanding host build keeps -nostdinc++ and links lld/msvcrt directly, so the
# C++ standard library has to come from somewhere. host-shim/ only ever carried the six
# headers this test needed when it was written; home_dart_fields.h has since taken a
# std::vector<uint32_t> parameter, and a six-file shim turned that into a "file not
# found" abort that looked like a code regression. A real libcxx is not an option -
# it wants the MSVC CRT headers, which are not installed on this machine - so the shim
# grew a declaration-only vector. Nothing in it has a definition: a real call site
# would fail at link time instead of quietly computing wrong numbers.
for source in sources:
    obj=O/(mode+'-'+source.stem+'.obj');exe=obj.with_suffix('.exe')
    p=subprocess.run([str(B/'clang++.exe'),'--target=x86_64-pc-windows-msvc','-std=c++20','-nostdinc++','-O2','-Wall','-Wextra','-Werror','-fno-exceptions','-fno-rtti','-fno-stack-protector','-I',str(O/'host-shim'),'-I',str(L),'-I',str(root/'app/src/main/cpp'),'-c',str(source),'-o',str(obj)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=subprocess.run([str(B/'ld.lld.exe'),'-flavor','link','/entry:entry','/subsystem:console','/nodefaultlib','/out:'+str(exe),str(obj),str(L/'crt.obj'),str(L/'msvcrt.lib'),str(L/'kernel32.lib'),str(L/'printf.lib')],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=subprocess.run([str(exe)],capture_output=True,text=True); print(p.stdout,end='');print('TEST_EXIT='+str(p.returncode))
    assert p.returncode==(1 if mode!='modified' else 0)
print(mode.upper()+'=VERIFIED; EXIT=0')
