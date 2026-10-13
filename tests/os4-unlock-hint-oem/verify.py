#!/usr/bin/env python3
"""Re-read actual APK/DEX provenance and Android results; never upgrades to device PASS."""
from pathlib import Path
import argparse,hashlib,json,zipfile,io,importlib.util,sys,re

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
BASE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;SHA=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();H=lambda b:hashlib.sha256(b).hexdigest()
PROD=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockScreenHintOS4.kt'
ROT='com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController';IND='com.android.systemui.keyguard.KeyguardIndication';VC='com.android.systemui.util.ViewController'
def main():
 p=argparse.ArgumentParser();p.add_argument('--build',type=Path,required=True);p.add_argument('--android',type=Path,required=True);p.add_argument('--rom',type=Path,required=True);p.add_argument('--mutation-build',type=Path,required=True);p.add_argument('--mutation-android',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
 checks=[];inputs={};add=lambda n,b:checks.append(dict(name=n,passed=bool(b)))
 def read(p):p=p.resolve();inputs[str(p)]=SHA(p);return json.loads(p.read_text())
 inputs[str(Path(__file__).resolve())]=SHA(Path(__file__).resolve());inputs[str(PROD)]=SHA(PROD)
 b=read(a.build/'build-report.json');r=read(a.android/'report.json');android=read(a.android/'android-report.json');rom=read(a.rom/'extraction.json')
 add('build current production and fixture input bytes unchanged',b['production']['research_mutation']is False and b['production']['sha256']==SHA(PROD)and all(SHA(Path(n))==h for n,h in b['inputs'].items()))
 inputs.update(b['inputs']);inputs.update(b['dependencies']);add('actual API37 report checks all true',r['passed']is True and android['passed']is True and android['failed_checks']==0 and android['check_count']==len(android['checks'])and all(x['passed']is True for x in android['checks'])and android['sdk']==37)
 add('installed actual probe APK identity',r['installed_apk_sha256']==b['apk_sha256']==r['apk_sha256']==SHA(a.build/'unlock-hint-oem-probe.apk'))
 add('hidden API policy not changed',r['hidden_api_policy']==dict(before='null',after='null',changed=False))
 add('real phone status not upgraded',all(x['device_status']=='NOT_TESTED'for x in [r,android,b,rom]))
 modpath=test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py';inputs[str(modpath)]=SHA(modpath);sp=importlib.util.spec_from_file_location('unmodified_dex',modpath);dex=importlib.util.module_from_spec(sp);sp.loader.exec_module(dex)
 apk=a.build/'unlock-hint-oem-probe.apk';inputs[str(apk.resolve())]=SHA(apk)
 with zipfile.ZipFile(apk)as z:
  raw=z.read('assets/oem.apk');adapter=z.read('assets/adapters.jar');parents={}
  for n in z.namelist():
   if re.fullmatch(r'classes\d*\.dex',n):parents.update(dex.dex_classes(z.read(n)))
 add('full OTA APK asset exact original bytes',H(raw)==b['ota']['sha256']==rom['apk_sha256']==android['ota_sha256']=='7da817a4c65299f7362fd530d72c09c0a4785c98cf2732395c721f2740e7662a')
 with zipfile.ZipFile(io.BytesIO(raw))as z:
  orig={};dexes={}
  for n in z.namelist():
   if re.fullmatch(r'classes\d*\.dex',n):data=z.read(n);dexes[n]=H(data);orig.update({name:n for name in dex.dex_classes(data)})
 add('all original OTA DEX members exact fresh extraction hashes',dexes==b['ota']['dex']=={x['dex']:x['sha256']for x in rom['dex']})
 with zipfile.ZipFile(io.BytesIO(adapter))as z:
  adapters={}
  for n in z.namelist():adapters.update(dex.dex_classes(z.read(n)))
 target_names=sorted(n for n in orig if n.startswith((ROT,IND,VC)))
 add('all actual controller indication and base helper classes absent in parent and adapters',bool(target_names)and not set(target_names)&(set(parents)|set(adapters)))
 add('actual adapter dex inventoried',H(adapter)==b['adapters']['sha256']==android['adapter_sha256']and sorted(adapters)==b['adapters']['classes'])
 add('actual parent probe dex inventoried',sorted(parents)==b['adapters']['main_dex_classes'])
 executed={ROT,IND,ROT+'$1',ROT+'$ShowNextIndication',ROT+'$ShowNextIndication$$ExternalSyntheticLambda0',ROT+'$$ExternalSyntheticLambda1',VC,VC+'$1'}
 add('runtime exact original class origin records and adapter precedence closure',set(x['class']for x in android['class_origins'])==executed and executed<=set(orig)and not executed&(set(parents)|set(adapters)))
 methods={}
 for rel,h in rom['files'].items():
  f=a.rom/rel;add('fresh complete smali file '+rel,SHA(f)==h);inputs[str(f.resolve())]=SHA(f)
  owner=rel.removeprefix('smali/').removesuffix('.smali').replace('/','.')
  if owner in executed:
   methods[owner]=[{"signature":s.splitlines()[0][len('.method '):],"full_smali_sha256":H(s.encode())}for s in re.findall(r'(?ms)^\.method .*?^\.end method',f.read_text())]
 mb=read(a.mutation_build/'build-report.json');mr=read(a.mutation_android/'report.json');md=read(a.mutation_android/'android-report.json');mut=Path(mb['production']['path']);inputs[str(mut)]=SHA(mut)
 add('negative control changes exactly null substitution into original skip',mut.read_text()==PROD.read_text().replace('param.args[1] = null','param.result = null')and mb['production']['research_mutation']is True and SHA(mut)==mb['production']['sha256'])
 fail=[x['name']for x in md['checks']if x['passed']is False]
 add('actual Android skip-original mutant rejected by stale map and pending queue cases',mr['run_complete']is True and mr['passed']is False and len(fail)==3 and any('clears already current13' in n for n in fail)and any('removes queued13'in n for n in fail)and md['sdk']==37)
 add('negative control actual installed APK bound',mr['installed_apk_sha256']==mb['apk_sha256']==SHA(a.mutation_build/'unlock-hint-oem-probe.apk'))
 add('all persistent inputs unchanged at end',all(SHA(Path(n))==h for n,h in inputs.items()))
 report=dict(passed=all(x['passed']for x in checks),checks=checks,check_count=len(checks),sources={str(PROD.relative_to(test_paths.SOURCE_ROOT)):SHA(PROD)},inputs=inputs,original_class_locations={n:orig[n]for n in target_names},actual_adapter_classes=sorted(adapters),actual_methods=methods,android_positive_checks=android['check_count'],negative_controls=dict(count=1,detected=True,witnesses=fail,scope='One source mutation executed on real ART against unchanged original Rotate/indication/helper DEX. This earlier mutant uses explicitly recorded superclass adapter; final positive removes that adapter and runs original ViewController too.'),device_status='NOT_TESTED',physical_device_pass_count=0,scope='Original unmodified OEM consumer DEX + production before callback via real EzHook/API102 dispatch and original Method.invoke on API37 emulator; actual View init/remove/add/destroy dispatches original base attach listener.',limits=['No LSPosed injection or physical Xiaomi phone execution. The API102 registration/Chain adapter dispatches callbacks explicitly; it does not intercept internal invokes automatically.','Optimized classes have inlined constructors; Unsafe allocation and exact producer field initialization replace full KeyguardIndicationController.setIndicationArea execution.','TextView is a thin field-bearing child adapter extending real Android TextView; OEM KeyguardIndicationTextView animation/font/content-protection implementation is not executed.','Theme-state check, logger, status controller and delay executor are explicit dependency adapters. Delay values/cancellation and original task behavior execute; wall-clock Handler delivery is manually triggered.','The actual original ViewController init and attach listener run for the final lifecycle test; original Rotate onViewAttached/onViewDetached/show/update/doze tasks are not translated.','Only confirmed stock type13 resource is selected; no claim about custom themes or generic lockscreen wording.'])
 (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=report['passed'],checks=len(checks),android=android['check_count'],failures=[x for x in checks if not x['passed']])));assert report['passed']
if __name__=='__main__':main()
