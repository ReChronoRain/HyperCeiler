#!/usr/bin/env python3
"""Re-read pinned OTA inputs and complete manually reviewed smali methods.
Static provenance/contract checks; never a ROM DEX execution or device verdict.
"""
from pathlib import Path
import argparse,hashlib,json,re,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
sha=lambda b:hashlib.sha256(b).hexdigest()
def methods(text):return re.findall(r'^\.method .*?^\.end method',text,re.M|re.S)
def check_method(text,row):
 found=[b for b in methods(text) if b.splitlines()[0]==row['method']]
 return len(found)==1 and sha(found[0].encode())==row['sha256'] and len(found[0].encode())==row['bytes']
def run(output):
 output.mkdir(parents=True,exist_ok=False);checks=[]
 def check(name,ok,detail=None):checks.append({'name':name,'passed':bool(ok),'detail':detail})
 review=test_paths.TEST_DATA_ROOT / 'acceptance/java-test10/wifi-v1/rom-reviewed-methods.json';r=json.loads(review.read_text());extract=test_paths.TEST_DATA_ROOT / 'acceptance/java-test10/candidates-v1/rom-extraction.json';e=json.loads(extract.read_text());apk=ROOT/r['apk'];check('exact_current_OTA_SystemUI_APK',sha(apk.read_bytes())==r['apk_sha256'])
 record=next(x for x in e['apks']if x['tag']=='systemui');check('extraction_binds_same_APK',record['sha256']==r['apk_sha256'])
 with zipfile.ZipFile(apk)as z:
  for d in record['extractions']:check('actual_DEX_'+d['dex'],sha(z.read(d['dex']))==d['sha256'])
 negative=[]
 for row in r['methods']:
  p=ROOT/row['file'];text=p.read_text();check(row['method'],check_method(text,row),row['reviewed_semantics']);relative=str(p.relative_to(extract.parent));check('full_class_'+p.name,sha(p.read_bytes())==e['files'][relative])
  block=next(x for x in methods(text)if x.splitlines()[0]==row['method']);lines=block.splitlines();index=next(i for i,line in enumerate(lines)if line.strip() and not line.strip().startswith(('.', '#', ':')));mutated=block.replace(lines[index],lines[index]+' # independent full-body change',1);negative.append({'method':row['method'],'rejected':not check_method(text.replace(block,mutated,1),row)})
 source=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/v/WifiStandard.kt';baseline=test_paths.TEST_DATA_ROOT / 'acceptance/java-test10/wifi-v1/WifiStandard-before.kt';new=source.read_text();old=baseline.read_text()
 old_init=old.split('    override fun init() {\n',1)[1].rsplit('\n}',1)[0]
 new_legacy=new.split('    override fun init() {\n',1)[1].split('        try {',1)[1].split('\n\n    private fun hookOS4()',1)[0]
 check('OS3_complete_legacy_init_text_unchanged','        try {'+new_legacy==old_init)
 check('OS4_does_not_require_old_DexKit_lookup','override fun useDexKit() = !isMoreHyperOSVersion(4f)'in new and 'if (!isMoreHyperOSVersion(4f)) makeWifiStandardZero'in new)
 ui=test_paths.SOURCE_ROOT / 'library/core/src/main/res/xml/system_ui_status_bar_icon_manage_new.xml';route=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemUI/SystemUIB.java'
 check('existing_UI_key','android:key="'+r['setting_key']+'"'in ui.read_text())
 check('existing_SystemUIB_registration', 'initHook(WifiStandard.INSTANCE, PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);'in route.read_text())
 inputs=[Path(__file__),review,extract,source,baseline,ui,route]+[ROOT/x['file']for x in r['methods']]
 report={'passed':all(x['passed']for x in checks)and all(x['rejected']for x in negative),'checks':checks,'full_method_negative_controls':negative,'inputs':{str(test_paths.report_path(p, ROOT)):sha(p.read_bytes())for p in inputs},'apk':{'path':r['apk'],'sha256':sha(apk.read_bytes())},'device_status':'NOT_TESTED','scope':'Static exact OTA/Dex/full-method reviewed contract and unchanged legacy init body. Semantic review descriptions are manual findings; no original ROM code is executed here.'}
 (output/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('ROM',sum(x['passed']for x in checks),'/',len(checks),'negative',sum(x['rejected']for x in negative));return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();raise SystemExit(0 if run(a.output)['passed']else 1)
