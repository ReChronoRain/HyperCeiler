from pathlib import Path
import sys,subprocess,re
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/resource-hot-reload-fix-20261018'/('host-'+R.name);D.mkdir(parents=True,exist_ok=True)
P='com/sevtinge/hyperceiler/libhook/base/'
s=(R/'library/libhook/src/main/java'/P/'XposedInitEntry.java').read_text('utf8')
def method(s,start):
 a=s.index(start);i=s.index('{',a);depth=1;j=i+1
 while depth:
  if s[j]=='{':depth+=1
  if s[j]=='}':depth-=1
  j+=1
 return s[a:j]
body=method(s,'public boolean onHotReloading(')
files=[]
def write(n,s):
 p=D/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8');files.append(str(p))
write('android/os/Build.java','package android.os;public class Build{public static class VERSION{public static int SDK_INT=37;}}')
write('android/util/Log.java','package android.util;public class Log{public static int warnings;public static String last;public static int w(String tag,String msg){warnings++;last=tag+":"+msg;return 0;}}')
write('androidx/annotation/NonNull.java','package androidx.annotation;public @interface NonNull{}')
write('com/sevtinge/hyperceiler/libhook/utils/api/DeviceHelper.java','package com.sevtinge.hyperceiler.libhook.utils.api;public class DeviceHelper{public static class System{public static float os=4;public static boolean isHyperOSVersion(float f){return os==f;}}}')
write(P+'EntryUnderTest.java','''package com.sevtinge.hyperceiler.libhook.base;
import android.os.Build;import android.util.Log;import androidx.annotation.NonNull;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
public class EntryUnderTest {
static String TAG="HyperCeiler",processName;static int prepares;static boolean prepared=true,throwPrepare;
static class HotReloadingParam{}
private boolean prepareHotReloadState(HotReloadingParam p){prepares++;if(throwPrepare)throw new IllegalStateException("fixture");return prepared;}
'''+body+'''
}
class PackageTarget{boolean system;PackageTarget(boolean s){system=s;}boolean isSystemServer(){return system;}}
class BaseLoad{static PackageTarget target;static int reads;static boolean throwRead;static PackageTarget getTarget(){reads++;if(throwRead)throw new AssertionError("preflight must run first");return target;}}
class HomeLayoutNativeEndpointOS4 {static boolean paused,pauseOk=true;static int pauses,resumes;static boolean pauseForHotReload(){pauses++;paused=pauseOk;return pauseOk;}static void resumeAfterRejectedHotReload(){resumes++;paused=false;}}
class XposedLog{static void w(String a,String b,String c){} }
''')
policy=R/'library/libhook/src/main/java'/P/'HotReloadCompatibility.java'
if policy.exists():files.append(str(policy))
write(P+'ReloadTest.java',r'''package com.sevtinge.hyperceiler.libhook.base;
import android.os.Build;import android.util.Log;import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
public class ReloadTest {
static int checks,failed;static void ok(boolean b,String n){checks++;if(!b){failed++;System.out.println("FAIL="+n);}}
static void reset(int sdk,float os,Boolean system,String process){Build.VERSION.SDK_INT=sdk;DeviceHelper.System.os=os;BaseLoad.target=system==null?null:new PackageTarget(system);BaseLoad.reads=0;BaseLoad.throwRead=false;EntryUnderTest.processName=process;EntryUnderTest.prepares=0;EntryUnderTest.prepared=true;EntryUnderTest.throwPrepare=false;HomeLayoutNativeEndpointOS4.pauses=0;HomeLayoutNativeEndpointOS4.resumes=0;HomeLayoutNativeEndpointOS4.paused=false;HomeLayoutNativeEndpointOS4.pauseOk=true;Log.warnings=0;Log.last="";}
public static void main(String[] args){EntryUnderTest e=new EntryUnderTest();EntryUnderTest.HotReloadingParam p=new EntryUnderTest.HotReloadingParam();
for(String host:new String[]{"system_server","com.android.systemui","com.miui.home","com.android.nfc","com.example.app"})for(Boolean t:new Boolean[]{null,false,true}){
 reset(37,4,t,host);boolean result=e.onHotReloading(p);ok(!result,"reject affected "+host+"/"+t);ok(BaseLoad.reads==0,"before target read");ok(EntryUnderTest.prepares==0,"no capture/cleanup");ok(HomeLayoutNativeEndpointOS4.pauses==0&&HomeLayoutNativeEndpointOS4.resumes==0&&!HomeLayoutNativeEndpointOS4.paused,"live publisher unchanged");ok(Log.warnings==1&&Log.last.contains("RESTART_REQUIRED")&&Log.last.contains(host),"unconditional restart diagnosis");
}
reset(37,4,true,"system_server");BaseLoad.throwRead=true;try{ok(!e.onHotReloading(p),"reject before failed metadata");}catch(Throwable x){ok(false,"unsafe metadata touched");}
for(int[] platform:new int[][]{{36,4},{35,4},{37,3},{37,2},{37,0},{38,4}})for(Boolean t:new Boolean[]{null,false,true}){
 reset(platform[0],platform[1],t,"legacy");ok(e.onHotReloading(p),"other platform original result");ok(EntryUnderTest.prepares==1&&BaseLoad.reads==1,"original transaction retained");ok(HomeLayoutNativeEndpointOS4.pauses==(Boolean.TRUE.equals(t)?1:0),"original publisher pause");ok(Log.warnings==0,"no new platform gate");
 reset(platform[0],platform[1],t,"legacy");EntryUnderTest.prepared=false;ok(!e.onHotReloading(p),"original rejection");ok(HomeLayoutNativeEndpointOS4.resumes==(Boolean.TRUE.equals(t)?1:0),"resume original rejection");
}
reset(36,4,true,"system_server");HomeLayoutNativeEndpointOS4.pauseOk=false;ok(!e.onHotReloading(p),"busy provider rejection");ok(EntryUnderTest.prepares==0&&HomeLayoutNativeEndpointOS4.resumes==1&&!HomeLayoutNativeEndpointOS4.paused,"busy provider restoration");
reset(36,4,true,"system_server");EntryUnderTest.throwPrepare=true;try{e.onHotReloading(p);ok(false,"fixture throw");}catch(IllegalStateException x){ok(HomeLayoutNativeEndpointOS4.resumes==1&&!HomeLayoutNativeEndpointOS4.paused,"finally on original exception");}
System.out.println("RESOURCE_RELOAD_PREFLIGHT checks="+checks+" failed="+failed+"; production onHotReloading + policy; Android/framework boundaries substituted; not native resource emulation");if(failed>0)System.exit(1);
}}
''')
J=Path('D:/build-tools/jdk-25.0.4.1+1/bin')
for command in [[str(J/'javac.exe'),'-encoding','UTF-8','-d',str(D)]+files,[str(J/'java.exe'),'-cp',str(D),'com.sevtinge.hyperceiler.libhook.base.ReloadTest']]:
 p=subprocess.run(command,capture_output=True,text=True,encoding='utf8');print(p.stdout+p.stderr,end='')
 if p.returncode:raise SystemExit(p.returncode)
