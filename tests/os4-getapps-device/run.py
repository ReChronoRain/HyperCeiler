#!/usr/bin/env python3
"""Actual production config/UI/route/rule through the real API102/EzHook callback chain."""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile,xml.etree.ElementTree as ET

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin'
FILES=[SOURCE/'library/common/src/main/java/com/sevtinge/hyperceiler/common/utils/GetAppsDeviceConfig.java',SOURCE/'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/GetAppsFragment.java',SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/GetApps.java',SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/getapps/DeviceModify.java']
XML=SOURCE/'library/core/src/main/res/xml/getapps.xml'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','api-102.0.0.jar']]+[test_paths.KOTLIN_HOME / 'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return{str(test_paths.report_path(p, ROOT)):sha(p)for p in [*FILES,XML,Path(__file__),*sorted((BASE/'fixtures').rglob('*.java')),*sorted((test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/getapps-v1/original-source').glob('*.java'))]}
MUTANTS={
 'legacy_enable_override':(0,'enabled = Boolean.TRUE.equals(values.get(ENABLED));','enabled = Boolean.TRUE.equals(values.get(ENABLED)) || "111".equals(values.get(LEGACY));'),
 'wrong_boolean_key':(0,'values.get(ENABLED));','values.get(LEGACY));'),
 'blank_overwrites_original':(0,'return trimmed.isEmpty() ? null : trimmed;','return trimmed;'),
 'migration_auto_enables':(0,'changes.put(ENABLED, false);','changes.put(ENABLED, true);'),
 'migration_overwrites_existing':(0,'if (!values.containsKey(keys[i])) {','if (true) {'),
 'legacy_preset_wrong_model':(0,'"popsicle", "25098PN5AC", "Xiaomi"','"popsicle", "WRONG", "Xiaomi"'),
 'late_ui_type_cleanup':(1,'GetAppsDeviceConfig.prepareUi(PrefsBridge.getAll()).forEach(PrefsBridge::putByApp);\n        super.onCreatePreferencesBefore(savedInstanceState, rootKey);','super.onCreatePreferencesBefore(savedInstanceState, rootKey);\n        GetAppsDeviceConfig.prepareUi(PrefsBridge.getAll()).forEach(PrefsBridge::putByApp);'),
 'hide_custom_inputs':(1,'mModel.setVisible(true);','mModel.setVisible(false);'),
 'wrong_active_handle':(2,'GetAppsDeviceConfig.from(PrefsBridge.getSharedPreferences().getAll())','GetAppsDeviceConfig.from(PrefsBridge.getAll())'),
 'old_route_key':(2,'deviceConfig.enabled && deviceConfig.hasOverrides()','PrefsBridge.getStringAsInt("market_device_modify_new", 0) != 0'),
 'constructor_after':(3,'public void before(HookParam param)','public void after(HookParam param)'),
 'wrong_identity_order':(3,'String[] values = {config.device, config.model, config.manufacturer};','String[] values = {config.model, config.device, config.manufacturer};'),
 'drop_write_readback':(3,'if (!Objects.equals(getStaticObjectField(Build.class, names[i]), values[i])) {','if (false) {'),
 'drop_rollback':(3,'for (int i = attempted; i >= 0; i--) {','for (int i = -1; i >= 0; i--) {'),
 'rollback_foreign_overwrite':(3,'if (Objects.equals(getStaticObjectField(Build.class, names[i]), values[i])) {','if (true) {'),
 'ignore_null_install':(3,') == null) throw new IllegalStateException("GetApps constructor hook was not installed");',') == null) { /* silently ignored */ }'),
}
def run(out,mutation=None,original=False):
 out.mkdir(parents=True,exist_ok=False);before=inputs()
 with tempfile.TemporaryDirectory(prefix='hc-getapps-')as folder:
  work=Path(folder);sources=[]
  for i,p in enumerate(FILES):
   body=p.read_text()
   if original and i>0:body=(test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/getapps-v1/original-source'/p.name).read_text()
   if mutation and i==MUTANTS[mutation][0]:
    _,a,b=MUTANTS[mutation];assert body.count(a)==1,(mutation,body.count(a));body=body.replace(a,b)
   dest=work/p.name;dest.write_text(body);sources.append(dest)
  extras=[]
  if original:
   dest=work/'DropDownPreference.java';dest.write_text('package fan.preference; public class DropDownPreference {}\n');extras.append(dest)
  cp=os.pathsep.join(map(str,DEPS));cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(work/'classes'),*map(str,sources),*map(str,extras),*map(str,sorted((BASE/'fixtures').rglob('*.java')))]
  p=subprocess.run(cmd,capture_output=True,text=True);(out/'compile.txt').write_text(p.stdout+p.stderr);assert p.returncode==0,p.stdout+p.stderr
  p=subprocess.run([str(JAVA/'java'),'-ea','-cp',str(work/'classes')+os.pathsep+cp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(p.stdout+p.stderr);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',p.stdout);assert m,p.stdout+p.stderr
  after=inputs();r={'passed':p.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':p.returncode,'mutation':mutation,'original_source':original,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(p)for p in DEPS},'scope':'Four whole production Java classes, real EzHook1.3.0/API102. Android widgets/DataStore initialization, Build-field backend, MarketApp constructor and consumer projection are explicitly host adapters; complete original OTA methods are independently extracted/read. No original ROM DEX or ART/device execution. Persistent rollback failures are reported and do not establish full restoration.','device_status':'NOT_TESTED'};(out/'report.json').write_text(json.dumps(r,indent=2)+'\n');print(out.name,m[1],m[2],flush=True);return r
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--negative-controls',action='store_true');p.add_argument('--original',action='store_true');a=p.parse_args()
 nodes={n.attrib.get('{http://schemas.android.com/apk/res/android}key'):n for n in ET.parse(XML).getroot().iter()}
 for k in ['device','model','manufacturer']:assert nodes['prefs_key_market_device_modify_'+k].attrib['{http://schemas.android.com/apk/res/android}dependency']=='prefs_key_market_device_modify_new1'
 assert ET.parse(XML).getroot().attrib['{http://schemas.android.com/apk/res-auto}quick_restart']=='com.xiaomi.market'
 r=run(a.output,original=a.original)
 if a.original:assert not r['passed'] and r['tests']['failed']>0
 else:assert r['passed']
 if a.negative_controls:
  checks=[]
  for name in MUTANTS:
   q=run(a.output/name,name);checks.append({'mutation':name,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  n={'passed':all(x['rejected']for x in checks),'checks':checks,'device_status':'NOT_TESTED','scope':'Source-copy faults rejected by production callback/UI/route fixtures; not APK mutations or device evidence.'};(a.output/'negative-controls.json').write_text(json.dumps(n,indent=2)+'\n');assert n['passed']
