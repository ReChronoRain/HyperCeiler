#!/usr/bin/env python3
"""Execute the complete OS4 rule + SystemUIB route via the real EzHook API102 chain.
Android registration/target methods are host adapters; this is not a device test.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent
SOURCE=BASE.parents[1]
ROOT=test_paths.TEST_DATA_ROOT
PKG=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
RULE=PKG/'rules/systemui/controlcenter/RemoveNotifNumLimitOS4.java'
ROUTE=PKG/'app/SystemUI/SystemUIB.java'
CACHE=test_paths.BUILD_CACHE
JAVA=test_paths.JAVA_HOME / 'bin'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','api-102.0.0.jar']]+list((CACHE/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'))+[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(w,name,text):
 p=w/(name.replace('.','/')+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=True);rule=RULE.read_text();route=ROUTE.read_text()
 if mutation=='drop_hook':rule=rule.replace('hookMethod(callback, (IReplaceHook) param -> null) == null','false')
 if mutation=='wrong_method':rule=rule.replace('getDeclaredMethod("onViewBound$1", entry)','getDeclaredMethod("onViewBound", entry)')
 if mutation=='invert_preference':route=route.replace('&& PrefsBridge.getBoolean("system_ui_control_center_remove_notif_num_limit")','&& !PrefsBridge.getBoolean("system_ui_control_center_remove_notif_num_limit")')
 if mutation=='drop_os4_gate':route=route.replace('com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion(4f)', 'true')
 if mutation=='remove_route':route=re.sub(r'        initHook\(new RemoveNotifNumLimitOS4\(\),.*?;','',route,flags=re.S)
 cp=os.pathsep.join(map(str,DEPS))
 with tempfile.TemporaryDirectory(prefix='hc-notif-count-')as tmp:
  w=Path(tmp)
  for p in (BASE/'fixtures').rglob('*.java'):
   dest=w/p.relative_to(BASE/'fixtures');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(p.read_bytes())
  write(w,'com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.RemoveNotifNumLimitOS4',rule)
  write(w,'com.sevtinge.hyperceiler.libhook.app.SystemUI.SystemUIB',route)
  write(w,'com.hchen.database.HookBase','package com.hchen.database; public @interface HookBase { String targetPackage(); int minSdk(); }')
  write(w,'com.sevtinge.hyperceiler.common.utils.PrefsBridge','''package com.sevtinge.hyperceiler.common.utils;
public class PrefsBridge {public static boolean enabled;
public static boolean getBoolean(String k){return k.equals("system_ui_control_center_remove_notif_num_limit")&&enabled;}
public static boolean getBoolean(String k,boolean d){return getBoolean(k);}
public static int getInt(String k,int d){return d;}public static int getStringAsInt(String k,int d){return d;}
public static String getString(String k,String d){return d;}public static java.util.Set<String>getStringSet(String k){return java.util.Set.of();}}''')
  write(w,'com.sevtinge.hyperceiler.libhook.base.BaseLoad','''package com.sevtinge.hyperceiler.libhook.base;
public abstract class BaseLoad {public abstract void onPackageLoaded();protected void initHook(BaseHook h){h.init();}protected void initHook(BaseHook h,boolean enabled){if(enabled)h.init();}}''')
  write(w,'com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper','package com.sevtinge.hyperceiler.libhook.utils.api; public class DeviceHelper {public static class Miui {public static boolean isPad(){return false;}}public static class System {public static float version=4f;public static boolean isMoreHyperOSVersion(float v){if(v!=4f)throw new AssertionError("unexpected gate threshold");return version>=v;}}}')
  for name in re.findall(r'^import (com\.[\w.]+);',route,re.M):
   if (w/(name.replace('.','/')+'.java')).exists():continue
   package,short=name.rsplit('.',1)
   write(w,name,f'package {package};public class {short} extends com.sevtinge.hyperceiler.libhook.base.BaseHook {{public static final {short} INSTANCE=new {short}();public void init(){{}}public static void createHook(){{}}}}')
  cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(w/'classes'),*map(str,w.rglob('*.java'))]
  z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(out/'build.txt').write_text(z.stdout)
  if z.returncode:print(z.stdout);raise RuntimeError('fixture compile failed')
  z=subprocess.run([str(JAVA/'java'),'-ea','-cp',str(w/'classes')+os.pathsep+cp,'TestMain'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(out/'tests.txt').write_text(z.stdout);print(z.stdout,end='');m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout)
  if not m:raise RuntimeError('fixture did not complete')
  report={'passed':z.returncode==0 and int(m[2])==0,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':z.returncode,'mutation':mutation,'sources':{str(test_paths.report_path(p, ROOT)):sha(p) for p in [RULE,ROUTE]},'test_inputs':{str(test_paths.report_path(p, ROOT)):sha(p)for p in [*sorted((BASE/'fixtures').rglob('*.java')),Path(__file__)]},'dependencies':{str(p):sha(p)for p in DEPS},'boundary':'Complete production rule and route; real EzHook Hooks.createHook/HookFactory/InvocationContext and API102 Chain; class resolver/backend and Android ROM methods are adapters. The simple quota fixture is a scope witness, not execution of ROM DEX. No ART, LSPosed, or device evidence.','device_status':'NOT_TESTED'}
  (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output)
 if not r['passed']:raise SystemExit(1)
 if a.negative_controls:
  mutants=[]
  for m in ['drop_hook','wrong_method','invert_preference','remove_route','drop_os4_gate']:
   result=run(a.output/m,m);mutants.append({'mutation':m,'rejected':not result['passed'],'failed_tests':result['tests']['failed']})
  report={'passed':all(x['rejected']for x in mutants),'mutations':mutants,'source_modified':False};(a.output/'negative-controls.json').write_text(json.dumps(report,indent=2)+'\n')
  if not report['passed']:raise SystemExit(1)
