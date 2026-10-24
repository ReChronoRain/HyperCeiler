"""Real-image source resolver tests; no device access."""
from pathlib import Path
import sys,subprocess,struct
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/gestures-os4-20261014/device/systemui-coexist/workspace-source-20261016';D.mkdir(parents=True,exist_ok=True)
header=R/'app/src/main/cpp/targets/home/home_workspace_gesture.h'
if not header.is_file():print('WORKSPACE_SOURCE=missing resolver; failed=1');raise SystemExit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
images=[('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')),('8244',W/'handover/folder-return-8244-20261009')]
code=r'''
#include <algorithm>
#include "targets/home/home_workspace_gesture.h"
#include <iostream>
using namespace home_workspace_gesture;using B=home_dynamic::Body;
int checks=0,failed=0;void ok(bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\n";}}
int main(){
'''
for version,root in images:
 e=Elf(root/f'launcher-{version}-libapp.so');s=Symbols(root/f'launcher-{version}-mini.elf')
 names=['WorkspaceGetxController.isDoubleTapLock','_DoubleTapGestureDetectorState._handlePointerDown','LauncherModeController.isElderlyManMode','Inst|isRegistered','_DoubleTapGestureDetectorState.isConsideredDoubleTap','_DoubleTapGestureDetectorState._resetState','RxObjectMixin.value']
 va=[s.by_name[n][0] for n in names];bodies=[]
 for n in names[:2]:
  a,z=s.by_name[n];z=next(v for v,_,_ in s.entries if v>a)-a;bodies.append(list(struct.unpack('<'+'I'*(z//4),e.read(a,z))))
 p,d=bodies
 code+=' {B p={'+','.join(map(str,p))+'};B d={'+','.join(map(str,d))+'};\n'
 args=','.join(map(str,va[2:]));base=f'resolve(p,{va[0]},d,{va[1]},{args},x)'
 code+=f'DoubleTapSites x;ok({base});std::cout<<"IMAGE={version} policy="<<x.policy<<" callback="<<x.callback<<" resume="<<x.resume<<"\\n";\n'
 code+=f'ok(x.policy>{va[0]}&&x.policy<{va[0]+len(p)*4}&&x.callback>{va[1]}&&x.resume==x.callback+20);\n'
 code+=f'''{{auto before=p;size_t i=(x.policy-{va[0]})/4;
 p[i]-=8<<12;ok({base});ok(x.frame==-16);p=before;p[i+1]+=4<<12;ok({base});p=before;
 p[i]^=1<<5;ok(!{base});p=before;p[i+2]^=1<<5;ok(!{base});p=before;
 p[i+3]=0xd503201f;ok(!{base});p=before;p[i+5]=0xd503201f;ok(!{base});p=before;
 for(auto &w:p)if(call(w,{va[0]}+(&w-p.data())*4,{va[2]}))w=0xd503201f;ok(!{base});p=before;
 p[0]=0x94000000u|(((int64_t({va[2]})-{va[0]})/4)&0x3ffffffu);ok(!{base});p=before;}}
 {{auto before=d;size_t i=(x.callback-{va[1]})/4;d[i+2]+=8<<10;ok({base});d=before;d[i+3]+=8<<12;ok({base});d=before;
 d[i+5]-=8<<12;ok({base});d=before;d[i+4]=0xd503201f;ok(!{base});d=before;
 d[i+3]^=1<<5;ok(!{base});d=before;d[i+5]^=1<<5;ok(!{base});d=before;
 for(auto &w:d)if(call(w,{va[1]}+(&w-d.data())*4,{va[5]}))w=0xd503201f;ok(!{base});d=before;
 auto copy=d;d.insert(d.end(),copy.begin(),copy.end());ok(!{base});d=before;}}
 }}
'''
code+=r'''std::cout<<"WORKSPACE_SOURCE checks="<<checks<<" failed="<<failed<<"\n";return failed?1:0;}'''
out=D/('host-'+R.name);out.mkdir(exist_ok=True);cpp=out/'verify.cpp';cpp.write_text(code)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';exe=out/'verify.exe'
c=[str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),str(cpp),'-o',str(exe)]
r=subprocess.run(c,capture_output=True,text=True);print(r.stdout,end='');print(r.stderr,end='')
if r.returncode:raise SystemExit(r.returncode)
r=subprocess.run([str(exe)],capture_output=True,text=True);print(r.stdout,end='');print(r.stderr,end='');raise SystemExit(r.returncode)
