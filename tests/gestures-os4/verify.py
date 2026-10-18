"""Production resolver / recognizer and MRU policy. No phone operations."""
from pathlib import Path
import sys,subprocess,struct
W=Path(__file__).resolve().parents[2]
R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/gestures-os4-20261014'; out=D/('host-'+R.name);out.mkdir(exist_ok=True)
H=R/'app/src/main/cpp/targets/home';J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture'
if not (H/'home_gestures_os4.h').exists():
    print('OS4_GESTURES=missing native resolver/recognizer; failed=1');sys.exit(1)
code=r'''
#include "targets/home/home_gestures_os4.h"
#include <fstream>
#include <iostream>
using namespace home_gestures;
int checks=0,failed=0;void ok(bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\n";}}
int main(int argc,char**argv){
 for(int a=1;a<argc;++a){std::ifstream f(argv[a],std::ios::binary);std::vector<char>b((std::istreambuf_iterator<char>(f)),{});
 auto span=[&](auto &v){return std::span(reinterpret_cast<const std::byte*>(v.data()),v.size());};
 auto t=resolve(span(b));ok(bool(t));if(!t){std::cout<<"BIND_FAILED="<<argv[a]<<"\n";continue;}
 std::cout<<"BIND="<<a<<" home="<<std::hex<<t->home<<" back="<<t->back<<" mode="<<t->home_mode<<std::dec<<"\n";
 auto image=home_layout::elf_targets::parse(span(b));auto offset=[&](uint64_t va){return *image->file_offset(va,4);};
 auto moved=b;uint32_t w=nhk::load_le32(reinterpret_cast<const std::byte*>(moved.data()+offset(t->home)));
 w+=16<<10;std::memcpy(moved.data()+offset(t->home),&w,4);auto mt=resolve(span(moved));ok(mt&&mt->home_mode==t->home_mode+64);
 auto broken=b;broken[offset(t->home+4)]=0;ok(!resolve(span(broken)));
 auto imp=b;std::string name=kImports[0];auto at=std::search(imp.begin(),imp.end(),name.begin(),name.end());ok(at!=imp.end());if(at!=imp.end())*at='X';ok(!resolve(span(imp)));
 auto duplicate=b;std::string one=" Home on_touch_event , action=",two=" Home on_touch_event: event already handled by another handler";
 auto at2=std::search(duplicate.begin(),duplicate.end(),two.begin(),two.end());ok(at2!=duplicate.end());if(at2!=duplicate.end())std::copy(one.begin(),one.end(),at2);ok(!resolve(span(duplicate)));
 auto action=b;uint32_t nop=0xd503201f;
 for(uint64_t pc=t->home;pc<image->bounds(t->home)->end;pc+=4){auto ww=*image->word(pc);if((ww&0xfc000000)==0x94000000){
 auto target=uint64_t(int64_t(pc)+(int64_t(int32_t(ww<<6))>>4));if(target==t->imports[0])std::memcpy(action.data()+offset(pc),&nop,4);}}
 ok(!resolve(span(action)));
 auto no_drop=b,no_helper=b;
 for(uint64_t pc=t->back;pc<image->bounds(t->back)->end;pc+=4){auto ww=*image->word(pc);
  auto target=nhk::arm64::branch_target(pc,ww,true);if(!target)continue;
  if(*target==t->imports[8])std::memcpy(no_drop.data()+offset(pc),&nop,4);
  if(pc>=8&&*image->word(pc-8)==0xaa1403e0&&*image->word(pc-4)==0xaa1303e1)
   std::memcpy(no_helper.data()+offset(pc),&nop,4);
 }
 ok(!resolve(span(no_drop)));ok(!resolve(span(no_helper)));
 }
 Recognizer r;Settings s{31,true,1,1200,1};
 auto e=[&](int a,float x,float y,int64_t t,int p=1,uintptr_t owner=1,bool back=true){return r.accept({a,p,x,y,t,owner},s,back);};
 e(0,0,500,1000);e(2,100,500,1100);auto d=e(1,100,500,1500);ok(d.cancel&&d.consume&&d.gesture==Previous);ok(!e(1,100,500,1501).consume);
 e(0,1199,500,2000);e(2,1099,500,2100);ok(e(1,1099,500,2500).gesture==Previous);
 e(0,0,500,3000);e(2,100,500,3100);ok(!e(1,100,500,3150).consume); // ordinary fast back
 e(0,1199,500,4000);e(2,1299,500,4100);ok(!e(1,1299,500,4500).consume); // outward
 e(0,0,500,5000);e(2,100,650,5100);ok(!e(1,100,650,5500).consume); // vertical rejection
 e(0,0,500,6000);e(2,100,500,6100,2);ok(!e(1,100,500,6500).consume); // two-finger back
 e(0,0,500,7000);e(2,100,500,7100);e(3,100,500,7500);ok(!e(1,100,500,7501).consume);
 e(0,0,500,8000);e(2,100,500,8100);s.quick=false;s.revision++;ok(!e(1,100,500,8500).consume);
 s.quick=true;s.mask=0;s.revision++;e(0,0,500,9000);e(2,100,500,9100);ok(e(1,100,500,9500).gesture==Previous); // independent quick switch
 e(0,500,500,10000,1,1,false);e(2,500,400,10100,1,1,false);ok(!e(1,500,400,10200,1,1,false).consume); // master off
 s.mask=31;s.revision++;e(0,500,500,11000,1,1,false);d=e(2,500,400,11100,1,1,false);ok(d.cancel&&d.consume&&!d.gesture);
 d=e(1,500,400,11200,1,1,false);ok(!d.cancel&&d.consume&&d.gesture==Up);
 e(0,500,500,12000,1,1,false);e(5,500,500,12050,2,1,false);d=e(2,500,600,12100,2,1,false);ok(d.cancel);ok(e(1,500,600,12200,1,1,false).gesture==Down2);
 e(0,500,500,13000,1,1,false);e(1,500,500,13100,1,1,false);e(0,500,500,13200,1,1,false);ok(e(1,500,500,13300,1,1,false).gesture==DoubleTap);
 e(0,500,500,14000,1,1,false);ok(!e(1,500,500,14600,1,1,false).consume); // long press
 e(0,500,500,15000,1,1,false);e(1,500,500,15100,1,1,false);e(0,500,500,15200,1,2,false);ok(!e(1,500,500,15300,1,2,false).consume); // owner lifetime
 std::cout<<"NATIVE_CHECKS="<<checks<<" FAILED="<<failed<<"\n";return failed?1:0;
}
'''
runtime=(H/'home_gestures_os4.cpp').read_text()
a=runtime.index('template<class F> void forward(');b=runtime.index('\n}\n}',a)+2
forward=runtime[a:b].replace('emit(', 'fake_emit(')
a=runtime.index('template<class F> void forward_owned(');b=runtime.index('\nextern "C" void hc_back_touch',a)
forward+='\n'+runtime[a:b].replace('emit(', 'fake_emit(')
fake=r"""
+std::vector<int> trace;int action_value=0;
+bool clone_failure=false;int original_handle=0,copy_handle=3;
+void *clone_event(void*){trace.push_back(20);return clone_failure?nullptr:&copy_handle;}
+void drop_event(void*p){trace.push_back(p==&original_handle?21:22);}
+struct FakeApi {int(*action)(void*);void(*set_action)(void*,int);void*(*clone)(void*);void(*drop)(void*);};
+int get(void*){return action_value;}void set(void*,int a){action_value=a;trace.push_back(a);}
+FakeApi api{get,set,clone_event,drop_event};void fake_emit(int event,uint64_t){if(event)trace.push_back(100+event);}
+""".replace('\n+','\n')
code=code.replace('int main(int argc,char**argv){',fake+forward+'\nint main(int argc,char**argv){')
extra=r"""
+ trace.clear();action_value=0x101;
+ forward_owned([&](void*p){trace.push_back(p==&copy_handle?30:31);drop_event(p);},&original_handle,{true,true,Previous});
+ ok(trace==std::vector<int>({20,3,21,30,22,106}));
+ trace.clear();clone_failure=true;
+ forward_owned([&](void*p){trace.push_back(31);drop_event(p);},&original_handle,{true,true,Previous});
+ ok(trace==std::vector<int>({20,31,21}));clone_failure=false;
+ trace.clear();forward_owned([&](void*p){trace.push_back(31);drop_event(p);},&original_handle,{});
+ ok(trace==std::vector<int>({31,21}));
+ trace.clear();forward_owned([&](void*){trace.push_back(31);},&original_handle,{false,true,0});
+ ok(trace==std::vector<int>({21}));
+ trace.clear();action_value=0x101;forward([&]{trace.push_back(10+action_value);},nullptr,{true,true,Previous});
+ ok(action_value==0x101&&trace==std::vector<int>({3,13,0x101,106}));
+ trace.clear();action_value=2;forward([&]{trace.push_back(10+action_value);},nullptr,{true,true,0});
+ ok(action_value==2&&trace==std::vector<int>({3,13,2}));
+ trace.clear();forward([&]{trace.push_back(10+action_value);},nullptr,{false,true,Up});ok(trace==std::vector<int>({102}));
+ trace.clear();forward([&]{trace.push_back(10+action_value);},nullptr,{});ok(trace==std::vector<int>({12}));
+ Config cfg;std::vector<int32_t> packet={kMagic,1,1,31,520,1200};
+ auto read=[&](const auto&v,bool optional){size_t i=0;return read_config([&](int32_t&w){if(i==v.size())return false;w=v[i++];return true;},cfg,optional);};
+ ok(read(packet,false)&&cfg.enabled&&cfg.quick&&cfg.mask==31&&cfg.density_dpi==520&&cfg.width==1200);
+ ok(read(std::vector<int32_t>{},true)&&!cfg.enabled&&!cfg.quick);ok(!read(std::vector<int32_t>{},false));
+ for(auto [i,v]:std::vector<std::pair<int,int>>{{0,0},{1,2},{2,-1},{3,32},{4,79},{4,1281},{5,0},{5,32769}}){auto bad=packet;bad[i]=v;ok(!read(bad,false));}
+ for(size_t n=1;n<packet.size();n++){auto bad=packet;bad.resize(n);ok(!read(bad,false));}
+""".replace('\n+','\n')
code=code.replace(' std::cout<<"NATIVE_CHECKS=',extra+' std::cout<<"NATIVE_CHECKS=')

# Production queue functions, with only eventfd/clock/Binder boundaries adapted on Windows.
def extract_native(name):
 a=runtime.index(name);b=runtime.index('{',a);depth=1;i=b+1
 while depth:
  if runtime[i]=='{':depth+=1
  elif runtime[i]=='}':depth-=1
  i+=1
 return runtime[a:i]
queue_prefix=r"""
#include <mutex>
#include <cerrno>
#include <time.h>
std::mutex lock;Settings settings{31,true,1,1200,7};
struct Request {int event=0;int64_t time=0;uint64_t revision=0;};
std::array<Request,8> requests{};size_t head=0,count=0;int wake=1,reads=0,writes=0;int64_t clock_ms=1000;
std::vector<int> sent;
int fake_clock(int,timespec*t){t->tv_sec=clock_ms/1000;t->tv_nsec=(clock_ms%1000)*1000000;return 0;}
int fake_write(int,const void*,size_t n){++writes;return int(n);}
int fake_read(int,void*,size_t n){if(reads-->0)return int(n);errno=EBADF;return -1;}
void send(Request r){sent.push_back(r.event);}
#define CLOCK_MONOTONIC 1
"""
queue=queue_prefix+extract_native('void emit(int event,uint64_t revision)')+'\n'+extract_native('void *action_worker(void*)')
queue=queue.replace('clock_gettime(', 'fake_clock(').replace('read(wake,','fake_read(wake,').replace('write(fd,','fake_write(fd,')
code=code.replace('int main(int argc,char**argv){',queue+'\nint main(int argc,char**argv){')
queue_tests=r"""
 emit(0,7);ok(count==0&&writes==0);emit(6,8);ok(count==0);emit(6,7);ok(count==1&&writes==1);
 reads=1;action_worker(nullptr);ok(count==0&&sent==std::vector<int>({6}));sent.clear();
 for(int i=0;i<9;++i)emit(6,7);ok(count==8);reads=1;action_worker(nullptr);ok(count==0&&sent.size()==8);sent.clear();
 emit(6,7);settings.revision=8;reads=1;action_worker(nullptr);ok(sent.empty());settings.revision=7;
 emit(6,7);clock_ms+=751;reads=1;action_worker(nullptr);ok(sent.empty());
 emit(6,7);settings.quick=false;reads=1;action_worker(nullptr);ok(sent.empty());settings.quick=true;
 emit(Up,7);settings.mask=0;reads=1;action_worker(nullptr);ok(sent.empty());
"""
code=code.replace(' std::cout<<"NATIVE_CHECKS=',queue_tests+' std::cout<<"NATIVE_CHECKS=')

(out/'verify.cpp').write_text(code)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
cmd=[str(zig),'c++','-std=c++20','-O2','-I'+str(R/'app/src/main/cpp'),str(out/'verify.cpp'),'-o',str(out/'verify.exe')]
x=subprocess.run(cmd,capture_output=True,text=True);print(x.stdout+x.stderr,end='');
if x.returncode:sys.exit(x.returncode)
images=[W/'tests/back-gesture-os4/libapp_launcher.so',D/'libapp_launcher.so']
x=subprocess.run([str(out/'verify.exe'),*map(str,images)],capture_output=True,text=True);print(x.stdout+x.stderr,end='');failed=x.returncode
s=(J/'HomeQuickSwitchOS4.java').read_text();assert 'getField("isAvailable")' not in s and 'RECENT_IGNORE_UNAVAILABLE' in s
a=s.index('    static int selectPrevious(');b=s.index('    private static boolean switchPrevious(',a)
java='public class Policy {\n'+s[a:b]+'''
static int checks=0,failed=0;static void ok(boolean v){checks++;if(!v)failed++;}
public static void main(String[]a){
 int[] ids={7,8,9,10},users={0,0,0,0},types={1,2,3,1};boolean[] available={true,true,true,true};
 ok(selectPrevious(7,0,ids,users,types,available)==10);
 ok(selectPrevious(10,0,ids,users,types,available)==7);
 ok(selectPrevious(7,10,ids,users,types,available)==-1);
 available[3]=false;ok(selectPrevious(7,0,ids,users,types,available)==-1);
 ok(selectPrevious(7,0,ids,new int[0],types,available)==-1);
 ids[0]=-1;available[3]=true;ok(selectPrevious(1,0,ids,users,types,available)==10);
 System.out.println("TASK_POLICY_CHECKS="+checks+" FAILED="+failed);if(failed>0)System.exit(1);
}}
'''
(out/'Policy.java').write_text(java)
for cmd in [['javac',str(out/'Policy.java')],['java','-cp',str(out),'Policy']]:
 x=subprocess.run(cmd,capture_output=True,text=True);print(x.stdout+x.stderr,end='');failed|=x.returncode
# Exercise the actual reflective framework adapter, including TaskInfo WITHOUT isAvailable.
def function(src,name):
 a=src.index(name);b=src.index('{',a);depth=1;i=b+1
 while depth:
  if src[i]=='{':depth+=1
  elif src[i]=='}':depth-=1
  i+=1
 return src[a:i]
stubs={
'android/os/IBinder.java':'package android.os;public interface IBinder {int FLAG_ONEWAY=1;}',
'android/content/Context.java': 'package android.content; public class Context {public static final String ACTIVITY_SERVICE="activity";public Object getSystemService(String n){return new android.app.ActivityManager();}}',
'android/os/Bundle.java':'package android.os;public class Bundle {}',
'android/util/Log.java':'package android.util;public class Log {public static int i(String a,String b){return 0;}}',
'android/app/ActivityManager.java':'''package android.app;import java.util.*;public class ActivityManager {
public static int RECENT_IGNORE_UNAVAILABLE=2; public static class RunningTaskInfo {public int taskId=7,userId=0;}
public static List<RunningTaskInfo> running=new ArrayList<>(List.of(new RunningTaskInfo()));
public List<RunningTaskInfo> getRunningTasks(int n){return running;}}''',
'android/app/IActivityTaskManager.java':'''package android.app;import android.os.Bundle;public interface IActivityTaskManager {
ActivityTaskManager.Slice getRecentTasks(int a,int b,int c);int startActivityFromRecents(int id,Bundle b);}''',
'android/app/ActivityTaskManager.java':'''package android.app;import java.util.*;import android.os.Bundle;public class ActivityTaskManager {
public static class WC {public int type=1;public int getActivityType(){return type;}}
public static class Cfg {public WC windowConfiguration=new WC();}
public static class Task {public int taskId,userId=0;public Cfg configuration=new Cfg();public Task(int id,int type){taskId=id;configuration.windowConfiguration.type=type;}}
public static class Slice {public List<Task> getList(){return tasks;}}
public static List<Task> tasks=new ArrayList<>(List.of(new Task(7,1),new Task(8,2),new Task(9,3),new Task(10,1)));
public static int launched=-1,result=2,flags=-1,user=-1;
public static class Service implements IActivityTaskManager {public Slice getRecentTasks(int n,int f,int u){flags=f;user=u;return new Slice();}
public int startActivityFromRecents(int id,Bundle b){launched=id;return result;}}
public static Object getService(){return new Service();}}'''
}
for n,t in stubs.items():
 p=out/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(t)
adapter='import android.app.*;import android.content.Context;import android.os.Bundle;import java.util.List;public class Adapter {\n'
adapter+=function(s,'static boolean synchronous(')+'\n'+function(s,'static int selectPrevious(')+'\n'+function(s,'private static boolean switchPrevious(')
adapter+=r'''
static int checks=0,failed=0;static void ok(boolean v){checks++;if(!v)failed++;}
public static void main(String[]a)throws Exception {
 for(int flags:new int[]{0,2,16,18,32,48})ok(synchronous(flags));
 for(int flags:new int[]{1,3,17,19,33,49})ok(!synchronous(flags));
 Context ctx=new Context();ok(switchPrevious(ctx,-1)&&ActivityTaskManager.launched==10&&ActivityTaskManager.flags==2&&ActivityTaskManager.user==0);
 ok(!switchPrevious(ctx,8));ok(switchPrevious(ctx,7));
 ActivityManager.RECENT_IGNORE_UNAVAILABLE=42;ok(switchPrevious(ctx,-1)&&ActivityTaskManager.flags==42);
 ActivityTaskManager.result=-1;ok(!switchPrevious(ctx,-1));ActivityTaskManager.result=100;ok(!switchPrevious(ctx,-1));ActivityTaskManager.result=0;ok(switchPrevious(ctx,-1));
 ActivityTaskManager.tasks.get(3).userId=10;ok(!switchPrevious(ctx,-1));ActivityTaskManager.tasks.get(3).userId=0;
 ActivityTaskManager.tasks.clear();ok(!switchPrevious(ctx,-1));ActivityManager.running.clear();ok(!switchPrevious(ctx,-1));
 System.out.println("FRAMEWORK_ADAPTER_CHECKS="+checks+" FAILED="+failed+"; mocked framework, production reflective method");if(failed>0)System.exit(1);
}}
'''
(out/'Adapter.java').write_text(adapter)
cmd=['javac','-d',str(out),str(out/'Adapter.java'),*[str(out/n) for n in stubs]]
x=subprocess.run(cmd,capture_output=True,text=True);print(x.stdout+x.stderr,end='');failed|=x.returncode
if not x.returncode:
 x=subprocess.run(['java','-cp',str(out),'Adapter'],capture_output=True,text=True);print(x.stdout+x.stderr,end='');failed|=x.returncode
print('OS4_GESTURES_EXIT='+str(failed));sys.exit(failed)
