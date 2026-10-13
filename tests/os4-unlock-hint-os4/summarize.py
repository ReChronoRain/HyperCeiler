#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;SOURCE=test_paths.SOURCE_ROOT;OUT=test_paths.TEST_DATA_ROOT / 'acceptance/java-test15/unlock-hint-runtime'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def run():
 paths=[OUT/'host-v4/report.json',OUT/'host-v4/negative-controls.json',OUT/'rom-review-v1/report.json']
 inputs={str(Path(__file__).resolve()):sha(Path(__file__))};reports=[]
 for p in paths:
  j=json.loads(p.read_text());assert j['passed'] is True;inputs[str(p)]=sha(p);reports.append(j)
 host,negative,rom=reports
 assert host['tests']=={'passed':59,'failed':0}and host['inputs_unchanged']is True
 assert len(negative['checks'])==11 and all(x['rejected']is True and x['witness_observed']is True for x in negative['checks'])
 assert len(rom['checks'])==35 and all(x['passed']is True for x in rom['checks'])
 for p,h in host['inputs_after'].items():assert sha(ROOT/p)==h
 for p,h in rom['sources'].items():assert sha(SOURCE/p)==h
 for p in [OUT/'README.md',test_paths.SOURCE_ROOT / 'tests/os4-unlock-hint-os4/README.md',test_paths.SOURCE_ROOT / 'tests/os4-unlock-hint-os4/extract_rom.py',test_paths.SOURCE_ROOT / 'tests/os4-unlock-hint-os4/verify_rom.py']:
  inputs[str(p)]=sha(p)
 result=dict(passed=True,kind='test15_stock_type13_author_component',sources=rom['sources'],inputs=inputs,checks=[dict(name='production SDK37 compilation',passed=host['sdk37_compiled']),dict(name='59 real EzHook API102 host cases',passed=True),dict(name='11 compiled source controls fail their specified behavior witness',passed=True),dict(name='35 actual OTA/source contract checks and OS3 byte identity',passed=True)],test_counts={'host':59,'source_mutants':11,'rom_static':35,'complete_rom_methods':8},review_role='author component verification; peer independent and original OTA ART execution reports remain separately identified',setting_key='prefs_key_system_ui_lock_screen_unlock_tip',scope_status='PARTIAL_STOCK_SOFT_SLEEP_TYPE13_OFFLINE_CANDIDATE',physical_device_pass_count=0,device_status='NOT_TESTED',limitations=rom['limitations']+['This does not cover all ordinary/default-theme bottom unlock text or arbitrary MAML themes.','Host target body only records calls. The unmodified original OTA queue is exercised in the separate unlock-hint-oem ART report, not by this host fixture.','Existing SystemUI restart/reload is required for setting changes; no lockscreen authentication or state is changed.'])
 p=OUT/'report.json';assert not p.exists();p.write_text(json.dumps(result,indent=2)+'\n');print(sha(p))
if __name__=='__main__':run()
