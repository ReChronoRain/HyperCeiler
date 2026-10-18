from pathlib import Path
import subprocess,sys,tempfile,struct
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
H=R/'app/src/main/cpp/targets/home';J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
if not (H/'home_dock_edit.h').exists() or not (J/'DockEditState.java').exists():
 print('DOCK_EDIT=missing default edit policy; failed=1');sys.exit(1)
D=W/'handover/dock-edit-20261009/test';D.mkdir(parents=True,exist_ok=True)
def run(c):
 p=subprocess.run([str(x) for x in c],capture_output=True,text=True);print(p.stdout+p.stderr,end='')
 if p.returncode:sys.exit(p.returncode)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
fixtures=[W/'handover/folder-return-8244-20261009/launcher-8244',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/launcher-7722')]
code='#include "home_dock_edit.h"\n#include <iostream>\nint n=0,f=0;void check(bool b){++n;if(!b)++f;}\nint main(){\n'
for prefix in fixtures:
 e=Elf(str(prefix)+'-libapp.so');s=Symbols(str(prefix)+'-mini.elf');names=['EditModeGetxController.isInNormalEditing','RxObjectMixin.value','EditModeGetxController._updateGridState'];vec=[];vas=[]
 for name in names:
  va,size=s.by_name[name];vas.append(va);vec.append(list(struct.unpack('<'+'I'*(size//4),e.read(va,size))))
 code+=' {\n'+''.join('std::vector<uint32_t> '+name+'{'+','.join(hex(w) for w in words)+'};\n' for name,words in zip(['g','r','u'],vec))
 code+=f'home_layout::DockEditFields out; uint32_t gv={vas[0]},rv={vas[1]};auto test=[&](){{return home_layout::dock_edit_fields(g,gv,r,rv,u,out);}};check(test());check(out.controller>0&&out.value>0);\n'
 code+='''for(int i=0;i<9;++i){auto w=g[i];g[i]^=1;check(!test());g[i]=w;}
for(int i=0;i<5;++i){auto w=u[i];u[i]^=1;check(!test());u[i]=w;}
for(size_t i=0;i+7<r.size();++i)if(r[i+1]==0x8b1c8000&&r[i+2]==0xf9402370){for(int j=0;j<8;++j){auto w=r[i+j];r[i+j]^=1;check(!test());r[i+j]=w;}}
g[2]+=4<<12;check(test());check(out.controller==''' +str(((vec[0][2]>>12)&511)+4)+''');g[2]-=4<<12;gv+=0x1000;rv+=0x1000;check(test());
g[2]=(g[2]&~(511<<12))|(1<<12);check(!test());
}'''
code+='std::cout<<"DOCK_EDIT_FIELDS="<<n<<" checks; failed="<<f<<"\\n";return f?1:0;}';(D/'fields.cpp').write_text(code)
run([W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe','c++','-std=c++20','-I'+str(H),D/'fields.cpp','-o',D/'fields.exe']);run([D/'fields.exe'])
(D/'DockEditTest.java').write_text('''import com.sevtinge.hyperceiler.libhook.rules.home.dock.*;
import android.os.*;import java.util.concurrent.atomic.AtomicInteger;
public class DockEditTest {
 static int n;static void c(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 static Parcel p(long seq,long value,long rev){return new Parcel("android.view.IWindowManager",seq,System.nanoTime(),value,rev,rev);}
 public static void main(String[] a){
  DockEditState s=new DockEditState();long now=10000000000L;
  c(!s.hidden(10139,10));c(!s.accept(10139,10,1,now,1,1,1,now));c(!s.hidden(10139,10));c(s.bind(10139,10));c(s.hidden(10139,10));
  c(!s.accept(10139,10,1,now,0,2,2,now));c(s.hidden(10139,10));
  c(!s.accept(10139,10,2,now,0,1,1,now));c(s.hidden(10139,10));
  for(long[] x:new long[][]{{2,now,2,2,2},{2,now+1,0,2,2},{2,1,0,2,2},{2,now,0,3,2},{2,now,0,0,0},{0,now,0,2,2}})c(!s.accept(10139,10,x[0],x[1],x[2],x[3],x[4],now));
  c(!s.accept(10140,10,2,now,0,2,2,now));c(s.hidden(10139,10));
  c(s.accept(10139,10,2,now,0,2,2,now));c(!s.hidden(10139,10));c(!s.accept(10139,10,3,now,0,2,2,now));
  c(s.accept(10139,10,4,now,1,3,3,now));c(!s.accept(10139,11,1,now,0,1,1,now));c(s.hidden(10139,10));s.bind(10139,11);c(!s.hidden(10139,11));c(!s.hidden(10139,10));
  AtomicInteger changes=new AtomicInteger();DockNativeMotionEndpoint e=new DockNativeMotionEndpoint(changes::incrementAndGet,x->{});e.bindIdentity(10139,10);Binder.setCallingIdentityForTest(10139,10);
  c(DockNativeMotionEndpoint.handles(DockNativeMotionEndpoint.EDIT_TRANSACTION_CODE));
  c(e.receive(DockNativeMotionEndpoint.EDIT_TRANSACTION_CODE,p(1,1,1),0)==DockNativeMotionEndpoint.ACK);c(e.editing(10139,10));c(changes.get()==1);
  e.receive(DockNativeMotionEndpoint.EDIT_TRANSACTION_CODE,p(2,0,2),IBinder.FLAG_ONEWAY);c(e.editing(10139,10));
  e.receive(DockNativeMotionEndpoint.AUTO_AIM_TRANSACTION_CODE,p(2,0,2),0);c(e.editing(10139,10));
  e.receive(DockNativeMotionEndpoint.EDIT_TRANSACTION_CODE,p(2,0,2),0);c(!e.editing(10139,10));c(changes.get()==2);
  e.bindIdentity(10139,11);c(!e.editing(10139,11));
  System.out.println("DOCK_EDIT_STATE="+n+" checks; failed=0");
 }
}''')
jdk=Path('D:/build-tools/jdk-25.0.4.1+1/bin');old=W/'tests/home-dock-window';sources=list((old/'stubs').rglob('*.java'))+[J/x for x in ['DockEditState.java','DockNativeMotion.java','DockNativeMotionEndpoint.java','DockRecentsMotion.java']]+[D/'DockEditTest.java',old/'DockNativeMotionEndpointTest.java',old/'DockNativeMotionTest.java']
run([jdk/'javac.exe','-encoding','UTF-8','-d',D,*sources]);run([jdk/'java.exe','-ea','-cp',D,'DockEditTest']);run([jdk/'java.exe','-ea','-cp',D,'com.sevtinge.hyperceiler.tests.dock.DockNativeMotionEndpointTest']);run([jdk/'java.exe','-ea','-cp',D,'com.sevtinge.hyperceiler.tests.dock.DockNativeMotionTest'])
window=(J/'HomeDockWindow.kt').read_text(encoding='utf8');hooks=(H/'home_layout_hooks.cpp').read_text(encoding='utf8');motion=(H/'dock_native_motion.cpp').read_text(encoding='utf8');asm=(H/'home_layout_dart_arm64.S').read_text(encoding='utf8');a=asm[asm.index('hc_dock_edit_entry:'):]
checks=[ 'dock_edit_packed.store(0, std::memory_order_release)' in hooks, '0x0048434F' in (J/'DockNativeMotionEndpoint.java').read_text(encoding='utf8') and '0x0048434F' in motion, 'windowVisible && !overlayShowing && !editing' in window,'requestTraversal() // Editing' in window,'!kDockEditPolicy && !config.grid_enabled' in hooks,motion.index('const uint64_t editing =')<motion.index('if (!dock_motion_feature_enabled.load',motion.index('void run_dock_motion')),'bl ' not in a and 'blr ' not in a,'ldr w10, [x1, x9]' in a and 'stlr x11, [x9]' in a,'br x16' in a,'Dock 编辑状态原消费域' in (H/'tweaks/symtab.cpp').read_text(encoding='utf8')]
failed=sum(not x for x in checks);print(f'DOCK_EDIT_WIRING={len(checks)} checks; failed={failed}');sys.exit(int(failed>0))
