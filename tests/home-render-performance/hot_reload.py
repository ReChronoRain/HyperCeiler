#!/usr/bin/env python3
"""The actual update preflight must keep a live native launcher generation intact."""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];path=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/XposedInitEntry.java';s=path.read_text()
a=s.index('    public boolean onHotReloading(');start=s.index('{',a);end=start+1;depth=1
while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
body=s[a:end]
source=r'''
import java.lang.annotation.*;
@interface NonNull{}
class Build{static class VERSION{static int SDK_INT=36;}}
class DeviceHelper{static class System{static boolean isHyperOSVersion(float v){return true;}}}
class HotReloadCompatibility{static boolean requiresProcessRestart(int sdk,boolean os){return sdk==37&&os;}}
class ModuleLog{static void w(String a,String b){}}
class XposedLog{static void w(String a,String b,String c){}}
class PackageTarget{boolean isSystemServer(){return false;}String getPackageName(){return "com.miui.home";}}
class BaseLoad{static int queries;static PackageTarget getTarget(){queries++;return new PackageTarget();}}
class HomeLayoutNativeEndpointOS4{static boolean pauseForHotReload(){return true;}static void resumeAfterRejectedHotReload(){}}
class AmbientNativeColorOS4{static boolean pauseForHotReload(){return true;}static void resumeAfterRejectedHotReload(){}}
class NativeHomeHooksOS4{static NativeHomeHooksOS4 INSTANCE=new NativeHomeHooksOS4();boolean started=true;boolean hasNativeLoadStarted(){return started;}}
public class ReloadAudit{static final String LAUNCHER_PACKAGE="com.miui.home";String processName="com.miui.home",TAG="fixture";int prepares;static class HotReloadingParam{}
boolean prepareHotReloadState(HotReloadingParam p){prepares++;return true;}
'''+body+r'''
 public static void main(String[] args){
  for(int sdk:new int[]{35,36,38}){Build.VERSION.SDK_INT=sdk;ReloadAudit e=new ReloadAudit();BaseLoad.queries=0;
   NativeHomeHooksOS4.INSTANCE.started=true;assert !e.onHotReloading(new HotReloadingParam()):"live native threads cannot survive a permitted generation retirement";
   assert e.prepares==0&&BaseLoad.queries==0;
   NativeHomeHooksOS4.INSTANCE.started=false;assert e.onHotReloading(new HotReloadingParam());assert e.prepares==1;
   e.processName="com.android.systemui";NativeHomeHooksOS4.INSTANCE.started=true;assert e.onHotReloading(new HotReloadingParam());
  }
  Build.VERSION.SDK_INT=37;ReloadAudit e=new ReloadAudit();assert !e.onHotReloading(new HotReloadingParam())&&e.prepares==0;
  java.lang.System.out.println("Update preflight PASS: native launcher generation stays live on every SDK, unloaded/unrelated targets retain original transaction, no restart action");
 }
}
'''
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
with tempfile.TemporaryDirectory(prefix='hc-update-preflight-')as tmp:
 p=Path(tmp);(p/'ReloadAudit.java').write_text(source)
 subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-d',tmp,str(p/'ReloadAudit.java')],check=True)
 subprocess.run([str(jdk/'java'),'-ea','-cp',tmp,'ReloadAudit'],check=True)
