"""Real image contracts and production recognizer; no phone access."""
from pathlib import Path
import sys,subprocess,struct
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/gestures-os4-20261014/device/systemui-coexist/workspace-swipe-20261016';D.mkdir(parents=True,exist_ok=True)
if not (R/'app/src/main/cpp/targets/home/home_workspace_swipe.h').is_file():
 print('WORKSPACE_SWIPE=missing resolver; failed=1');raise SystemExit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
images=[('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')),('8244',W/'handover/folder-return-8244-20261009')]
runtime=(R/'app/src/main/cpp/targets/home/home_gestures_os4.cpp').read_text()
a=runtime.index('extern "C" uintptr_t hc_workspace_status_result(');b=runtime.index('{',a)+1;depth=1;j=b
while depth:depth+=(runtime[j]=='{')-(runtime[j]=='}');j+=1
helper=runtime[a:j].replace('hc_workspace_status_result','test_status_result').replace('scope_contract','test_scope').replace('status_policy','test_policy')
code=r'''
#include "targets/home/home_workspace_swipe.h"
#include <iostream>
#include <map>
#include <fstream>
using namespace home_workspace_gesture;using B=home_dynamic::Body;
int checks=0,failed=0;void ok(bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\n";}}
#include <mutex>
#include <atomic>
std::mutex lock;std::atomic_bool probe{false};ScopeContract test_scope;StatusPolicy test_policy;
int64_t stock_block_start=0,stock_block_end=0;
#define ANDROID_LOG_INFO 4
int __android_log_print(int,const char*,const char*,...){return 0;}
''' + helper + r'''
int main(int argc,char**argv){
'''
for version,root in images:
 e=Elf(root/f'launcher-{version}-libapp.so');s=Symbols(root/f'launcher-{version}-mini.elf')
 names=['_DoubleTapGestureDetectorState._handlePointerDown','WorkspaceGetxController.isDoubleTapLock','_TransformedPointerEvent.timeStamp','SwipeDetector.onPointerMove','SwipeDetector._computeVelocity']
 va=[s.by_name[n][0] for n in names];bodies=[]
 for n in [names[0],names[2],names[3],names[4],'StatusBarSwipeController.onInterceptTouchEvent']:
  a,z=s.by_name[n];z=next(v for v,_,_ in s.entries if v>a)-a;bodies.append(list(struct.unpack('<'+'I'*(z//4),e.read(a,z))))
 code+=' {B d={'+','.join(map(str,bodies[0]))+'};B s={'+','.join(map(str,bodies[1]))+'};B m={'+','.join(map(str,bodies[2]))+'};\n'
 cv=s.by_name['StatusBarSwipeController.canInterceptTouch'][0];sv=s.by_name['StatusBarSwipeController.onInterceptTouchEvent'][0]
 code+='B status={'+','.join(map(str,bodies[4]))+'};StatusPolicy policy;ok(status_policy(status,'+str(sv)+','+str(cv)+',policy));std::cout<<"STATUS_POLICY='+version+' frame="<<policy.event_frame<<" bool="<<policy.bool_field<<" reg="<<policy.value_register<<"\\n";{auto old=status;status[6]-=8<<12;ok(status_policy(status,'+str(sv)+','+str(cv)+',policy));status=old;status[0]=0xd503201f;ok(!status_policy(status,'+str(sv)+','+str(cv)+',policy));}\n'
 code+='B v={'+','.join(map(str,bodies[3]))+'};ok(velocity_leaf(v));{auto old=v;v[1]+=4<<12;ok(!velocity_leaf(v));v=old;v[6]=0xd503201f;ok(!velocity_leaf(v));}\n'
 base=f'scope_contract(d,{va[0]},{va[1]},s,m,{va[3]},{va[4]},x)'
 code+=f'ScopeContract x;ok({base});std::cout<<"SCOPE={version} frame="<<x.event_frame<<" time="<<x.timestamp_field<<" duration="<<x.duration_field<<" selector="<<x.original_selector<<"\\n";\n'
 code+=f'''{{auto old=d;d[4]-=8<<12;ok({base});ok(x.event_frame==-24);d=old;d[4]^=1<<5;ok(!{base});d=old;
 for(size_t i=0;i<d.size();++i)if(call(d[i],{va[0]}+i*4,{va[1]}))d[i]=0xd503201f;ok(!{base});d=old;}}
 {{auto old=s;s[4]+=1<<5;ok({base});s=old;for(auto& w:s)if(home_dynamic::ldw(w,0,1))w+=4<<12;ok({base});s=old;s[2]=0xd503201f;ok(!{base});s=old;}}
 {{auto old=m;for(size_t i=0;i+2<m.size();++i)if(m[i+1]==0xd2807d00u&&m[i+2]==0x9ac00c22u)m[i]+=8<<12;ok({base});ok(x.duration_field==15);m=old;
 for(auto &v:m)if(v==0xd2807d00u)v=0xd503201f;ok(!{base});m=old;}}
 }}
'''
code+=r'''
 for(int a=1;a<argc;++a){std::ifstream f(argv[a],std::ios::binary);std::vector<char>b((std::istreambuf_iterator<char>(f)),{});
 auto t=home_gestures::resolve(std::span(reinterpret_cast<const std::byte*>(b.data()),b.size()));ok(t&&t->scene);if(t)std::cout<<"SCENE="<<a<<" "<<std::hex<<t->scene<<std::dec<<"\n";}
 for(int slot:{-16,-24})for(int field:{23,27})for(int underlying:{0,1}){
  std::map<uintptr_t,uint8_t> mem;auto put=[&](uintptr_t a,auto v){for(size_t i=0;i<sizeof(v);++i)mem[a+i]=(uint64_t(v)>>(8*i))&255;};
  auto read=[&](uintptr_t a,auto&v){uint64_t n=0;for(size_t i=0;i<sizeof(v);++i){if(!mem.count(a+i))return false;n|=uint64_t(mem[a+i])<<(i*8);}v=static_cast<std::remove_reference_t<decltype(v)>>(n);return true;};
  ScopeContract c{0x100100,0x200000,0x210000,slot,field,7,900};uintptr_t frame=0x300000,fp=0x301000,event=0x400001,base=underlying?0x401001:event,getter=0x200100,table=0x500000;
  put(frame,uint64_t(fp));put(frame+8,uint64_t(c.down_return));put(fp+slot,uint64_t(event));put(event-1,uint64_t(37)<<12);put(table+(37+900)*8,uint64_t(getter));
  if(underlying){put(getter,uint32_t(0xb840b020));put(getter+4,uint32_t(0x8b1c8000));put(getter+8,uint32_t(0xd65f03c0));put(event+11,uint32_t(base));}
  else {put(getter,uint32_t(0xaa0103e0));put(getter+4,uint32_t(0xd65f03c0));put(getter+8,uint32_t(0xd503201f));}
  put(base+field,uint32_t(0x402001));put(0x402001+7,int64_t(123456789));int64_t ms=0;
  ok(timestamp(frame,0,table,c,read,ms)&&ms==123456);put(frame+8,uint64_t(c.down_return+4));ok(!timestamp(frame,0,table,c,read,ms));put(frame+8,uint64_t(c.down_return));
  put(getter,uint32_t(0xd503201f));ok(!timestamp(frame,0,table,c,read,ms));
 }
 {
 std::vector<uint8_t> memory(65536);uintptr_t base=reinterpret_cast<uintptr_t>(memory.data());uint64_t heap=base>>32;
 auto put=[&](uintptr_t p,auto value){std::memcpy(reinterpret_cast<void*>(p),&value,sizeof(value));};
 uintptr_t frame=base+1024,event=base+4097,duration=base+8193,controller=base+12289,table=base+16384,getter=base+32768,null=(base&~uintptr_t(0xffffffffu))+0x10001;
 ok((base>>32)==((base+65535)>>32));test_scope={0,getter,getter+12,0,23,7,900};test_policy.event_frame=-24;test_policy.value_register=5;test_policy.base_register=1;test_policy.bool_field=87;
 put(frame-24,event);put(event-1,uint64_t(37)<<12);put(event+23,uint32_t(duration));put(duration+7,int64_t(123456789));put(table+(37+900)*8,getter);
 put(getter,uint32_t(0xaa0103e0));put(getter+4,uint32_t(0xd65f03c0));put(getter+8,uint32_t(0xd503201f));
 std::array<uintptr_t,32> saved{};saved[1]=controller;saved[5]=null+32;put(controller+87,uint32_t(null+32));
 for(unsigned kind=0;kind<3;++kind){stock_block_start=123000;stock_block_end=124000;ok(test_status_result(frame,heap,table,saved.data(),kind,null)==null+48);
 stock_block_start=124000;ok(test_status_result(frame,heap,table,saved.data(),kind,null)==null+32);}
 stock_block_start=123000;saved[5]=null+48;put(controller+87,uint32_t(null+48));
 ok(test_status_result(frame,heap,table,saved.data(),1,null)==null+48);ok(test_status_result(frame,heap,table,saved.data(),2,null)==null+48);
 }
 home_gestures::Settings settings{30,false,1,1200,1};SwipeRecognizer state;
 auto e=[&](int action,float dy=0,float dx=0,int n=1,int64_t time=1000,float spread=0){SwipeSample v;v.action=action;v.count=n;v.id={7,9};v.x={500+dx,700+dx+spread};v.y={1000+dy,1000+dy};v.time=time;v.owner=1;return state.accept(v,settings,false);};
 for(int fingers:{1,2})for(int dir:{-1,1}){
  e(0);ok(state.authorize(1000,1));if(fingers==2)e(5,0,0,2,1040);
  auto d=e(2,dir*80,0,fingers,1100);ok(d.cancel&&d.consume&&!d.gesture);
  ok(!e(2,dir*100,0,fingers,1120).cancel);
  if(fingers==2)e(6,dir*100,0,2,1150);
  d=e(1,dir*100,0,1,1200);ok(d.consume&&d.gesture==(dir<0?(fingers==2?4:2):(fingers==2?5:3)));ok(!e(1,dir*100,0,1,1201).consume);
 }
 e(0);ok(!state.authorize(999,1));ok(!state.authorize(1000,2));ok(!e(2,-100,0,1,1100).consume);e(1,-100,0,1,1200);
 e(0);state.authorize(1000,1);ok(!e(2,-20,100,1,1100).consume);e(1,-20,100,1,1200);
 e(0);state.authorize(1000,1);e(2,-20,100,1,1100);ok(!e(2,-400,100,1,1150).consume);e(1,-400,100,1,1200);
 e(0);state.authorize(1000,1);e(5,0,0,2,1040);ok(!e(2,-100,0,2,1100,150).consume);e(1,-100,0,1,1200);
 e(0);state.authorize(1000,1);e(2,-100,0,1,1100);ok(!e(3,-100,0,1,1200).gesture);ok(!e(1,-100,0,1,1201).consume);
 e(0);state.authorize(1000,1);e(2,-100,0,1,1100);settings.revision++;settings.mask=0;auto disabled=e(1,-100,0,1,1200);ok(disabled.consume&&!disabled.gesture);settings={30,false,1,1200,1};
 e(0);state.authorize(1000,1);e(5,0,0,3,1040);ok(!e(2,-100,0,2,1100).consume);e(1,-100,0,1,1200);
 e(0);state.authorize(1000,1);ok(!e(2,-100,0,1,1800).consume);e(1,-100,0,1,1900);
 e(0);state.authorize(1000,1);auto intent=e(2,8,0,1,1100);ok(intent.cancel&&intent.consume&&!intent.gesture);
 auto short_release=e(1,20,0,1,1200);ok(short_release.consume&&!short_release.gesture);
 std::cout<<"WORKSPACE_SWIPE checks="<<checks<<" failed="<<failed<<"; real image contracts + timestamp correlation + cancellation/pinch/config\n";return failed?1:0;}
'''
out=D/('host-'+R.name);out.mkdir(exist_ok=True);cpp=out/'verify.cpp';cpp.write_text(code)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';exe=out/'verify.exe'
r=subprocess.run([str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),str(cpp),'-o',str(exe)],capture_output=True,text=True);print(r.stdout+r.stderr,end='')
if r.returncode:raise SystemExit(r.returncode)
r=subprocess.run([str(exe),str(W/'tests/back-gesture-os4/libapp_launcher.so'),str(W/'handover/gestures-os4-20261014/libapp_launcher.so')],capture_output=True,text=True);print(r.stdout+r.stderr,end='');raise SystemExit(r.returncode)
