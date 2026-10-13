#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

R=test_paths.TEST_DATA_ROOT;S=test_paths.SOURCE_ROOT;base=test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/custom-background-runtime-v1'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();assert not a.output.exists()
paths={
'host':base/'host-v11/report.json','negative':base/'negative-parallel-v1/negative-controls.json',
'sdk':base/'sdk-v2/report.json','rom':base/'rom-v2/report.json',
'overlay_art':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/custom-overlay-art-v1/report.json',
'shader_art':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/seekbar-color-art-v1/report.json',
'renderer':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/renderer-v1/report.json'}
reports={k:json.loads(v.read_text())for k,v in paths.items()};checks=[]
def check(n,v):checks.append({'name':n,'passed':bool(v)});assert v,n
for k,r in reports.items():check(k+'_passed',r.get('passed')is True)
check('host_exact132',reports['host']['tests']=={'passed':132,'failed':0})
check('host_same_inputs',reports['host']['inputs_unchanged']and reports['host']['inputs_before']==reports['host']['inputs_after'])
for path,h in reports['host']['inputs_before'].items():check('host_input:'+path,sha(R/path)==h)
check('13_named_behavioural_negative_witnesses',len(reports['negative']['checks'])==13 and all(c['rejected']and c['witness_observed']for c in reports['negative']['checks']))
for row in reports['negative']['checks']:
 file=R/row['report'];x=json.loads(file.read_text());check('negative_report:'+row['mutation'],sha(file)==row['report_sha256']and x['inputs_before']==reports['host']['inputs_before'])
check('ROM55_complete21',len(reports['rom']['checks'])==55 and len(reports['rom']['complete_methods'])==21)
for path,h in reports['rom']['inputs'].items():check('rom_input:'+path,sha(Path(path))==h)
for path,h in reports['sdk']['sources'].items():check('sdk_source:'+path,sha(S/path)==h)
rule=S/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3'
sources={str((rule/n).relative_to(S)):sha(rule/n)for n in ['CustomBackground.kt','CustomBackgroundOS4.kt','MediaSeekBarColorOS4.kt']}
r={'passed':True,'review_role':'AUTHOR component verification, not independent artifact/device approval','device_status':'NOT_TESTED','sources':sources,'checks':checks,'reports':{k:{'path':str(test_paths.report_path(v, R)),'sha256':sha(v)}for k,v in paths.items()},'inputs':{str(v):sha(v)for v in [*paths.values(),Path(__file__).resolve(),Path(__file__).with_name('README.txt'),Path(__file__).with_name('run.py'),Path(__file__).with_name('compile_android.py'),Path(__file__).with_name('verify_rom.py'),base/'negative-parallel-v1/coordinator.py',base/'negative-parallel-v1/transition.json']},'scope':{'settings':'Existing NC and DI background modes1..4 with their independent color-animation/inverse/blur options; no new user feature counting.','registration':'NC6+DI5 lifecycle endpoints plus1 shared exact shader draw, one all-selected activation transaction.','evidence':'132 production callback host cases,13 compiled-copy source controls, SDK37 compilation of all5 real files,55 static checks/21 complete ROM bodies, separately supplied real Android compositing/shader and renderer reports.','non_OS4':'Removing only OS4 route/import produces byte-identical frozen test13 file; actual branch/default fixture compiled separately. Full OS3 device behavior not newly tested.','unverified':'No physical Xiaomi SystemUI/LSPosed/long-run testing. No claim of all vendor reflection, runtime resource overlays or third-party mutations. Value-equal transitionAlpha0 ownership cannot distinguish a foreign write. Permanent cleanup operation failure is reported, not guaranteed repaired.'}}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(r,indent=2)+'\n');print(sha(a.output),len(checks))
