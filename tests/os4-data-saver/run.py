#!/usr/bin/env python3
"""Complete production rule and SystemUIB route through actual EzHook API102 hooks.
ROM-shaped targets/resources are explicit host fixtures, not execution of ROM DEX.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT
PKG=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
RULE=PKG/'rules/systemui/statusbar/icon/all/DataSaverIconOS4.java';ROUTE=PKG/'app/SystemUI/SystemUIB.java'
CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','api-102.0.0.jar']]+list((CACHE/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'))+[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p)for p in [RULE,ROUTE,Path(__file__),*sorted((BASE/'fixtures').rglob('*.java'))]}
def write(w,name,text):
 p=w/(name.replace('.','/')+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
def replace_once(s,old,new):
 if s.count(old)!=1:raise RuntimeError('mutation witness absent or ambiguous: '+old)
 return s.replace(old,new)
MUTANTS={
 'drop_route':('route','initHook(new DataSaverIconOS4(),','initHook(new DataSaverIconOS4(), false &&'),
 'drop_os4_gate':('route','initHook(new DataSaverIconOS4(),\n            com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion(4f));','initHook(new DataSaverIconOS4(), true);'),
 'force_region_modes':('rule','mode == 1 || (mode != 2 && (boolean) param.getArgs()[0])','mode != 2'),
 'remove_creation':('rule','setIcon.invoke(owner, resources.getString(description), slot, drawable);','/* missing icon holder intentionally not initialized */'),
 'swap_regions':('rule','if (value == qs) return 2;','if (value == qs) return 1;'),
 'drop_reactive_filter':('rule','param.getArgs()[0] = adjusted(input, slot, mode, region);','/* incoming block list left unchanged */'),
 'drop_initial_group':('rule','owned.addAll(replacement);','owned.addAll(input);'),
 'mutate_shared_input':('rule','ArrayList<Object> result = new ArrayList<>(input);','@SuppressWarnings("unchecked") ArrayList<Object> result = (ArrayList<Object>) input;'),
 'remove_slot_guard':('rule','if (!slot.equals(policySlot.get(target))) return;','/* accept unrelated slot */'),
 'remove_registration_rollback':('rule','try { handle.unhook(); } catch (RuntimeException rollback) { error.addSuppressed(rollback); }','/* leave earlier registrations installed */'),
}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();rule=RULE.read_text();route=ROUTE.read_text()
 if mutation:
  target,old,new=MUTANTS[mutation]
  if target=='rule':rule=replace_once(rule,old,new)
  else:route=replace_once(route,old,new)
 cp=os.pathsep.join(map(str,DEPS))
 with tempfile.TemporaryDirectory(prefix='hc-data-saver-')as tmp:
  w=Path(tmp)
  for p in (BASE/'fixtures').rglob('*.java'):
   dest=w/p.relative_to(BASE/'fixtures');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(p.read_bytes())
  write(w,'com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.DataSaverIconOS4',rule)
  write(w,'com.sevtinge.hyperceiler.libhook.app.SystemUI.SystemUIB',route)
  write(w,'com.hchen.database.HookBase','package com.hchen.database;public @interface HookBase{String targetPackage();int minSdk();}')
  write(w,'com.sevtinge.hyperceiler.common.utils.PrefsBridge','''package com.sevtinge.hyperceiler.common.utils;public class PrefsBridge{public static int mode;public static int getStringAsInt(String k,int d){return k.equals("system_ui_status_bar_icon_data_saver")?mode:d;}public static boolean getBoolean(String k){return false;}public static boolean getBoolean(String k,boolean d){return false;}public static int getInt(String k,int d){return d;}public static String getString(String k,String d){return d;}public static java.util.Set<String>getStringSet(String k){return java.util.Set.of();}}''')
  write(w,'com.sevtinge.hyperceiler.libhook.base.BaseLoad','package com.sevtinge.hyperceiler.libhook.base;public abstract class BaseLoad{public abstract void onPackageLoaded();protected void initHook(BaseHook h){h.init();}protected void initHook(BaseHook h,boolean b){if(b)h.init();}}')
  write(w,'com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper','package com.sevtinge.hyperceiler.libhook.utils.api;public class DeviceHelper{public static class Miui{public static boolean isPad(){return false;}}public static class System{public static float version=4f;public static boolean isMoreHyperOSVersion(float v){if(v!=4f)throw new AssertionError();return version>=v;}}}')
  for name in re.findall(r'^import (com\.[\w.]+);',route,re.M):
   if(w/(name.replace('.','/')+'.java')).exists():continue
   package,short=name.rsplit('.',1);write(w,name,f'package {package};public class {short} extends com.sevtinge.hyperceiler.libhook.base.BaseHook{{public static final {short} INSTANCE=new {short}();public void init(){{}}public static void createHook(){{}}}}')
  cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(w/'classes'),*map(str,w.rglob('*.java'))]
  z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(out/'build.txt').write_text(z.stdout)
  if z.returncode:print(z.stdout);raise RuntimeError('fixture compile failed')
  z=subprocess.run([str(JAVA/'java'),'-ea','-cp',str(w/'classes')+os.pathsep+cp,'TestMain'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(out/'tests.txt').write_text(z.stdout);print(z.stdout,end='');m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout)
  if not m:raise RuntimeError('fixture did not complete')
  after=inputs();report={'passed':z.returncode==0 and int(m[2])==0 and int(m[1])==93 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':z.returncode,'mutation':mutation,'production_inputs':{str(test_paths.report_path(p, ROOT)):sha(p)for p in [RULE,ROUTE]},'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(p)for p in DEPS},'limits':['Whole production rule and SystemUIB compiled/executed with real EzHook1.3.0/API102 hook chain.','ROM classes, resources and registration backend are host fixtures; not ART, actual SystemUI DEX, LSPosed, screenshot or device execution.','Reactive list tests execute successive original setter and refresh/group callbacks, not the Kotlin coroutine engine.'],'device_status':'NOT_TESTED'}
  (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output)
 if not r['passed']:raise SystemExit(1)
 if a.negative_controls:
  mutants=[]
  for m in MUTANTS:
   result=run(a.output/m,m);mutants.append({'mutation':m,'rejected':not result['passed'] and result['tests']['failed']>0 and result['inputs_unchanged'],'failed_tests':result['tests']['failed']})
  report={'passed':all(x['rejected']for x in mutants),'mutations':mutants,'source_modified':False};(a.output/'negative-controls.json').write_text(json.dumps(report,indent=2)+'\n')
  if not report['passed']:raise SystemExit(1)
