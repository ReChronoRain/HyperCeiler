#!/usr/bin/env python3
"""Exercise the actual Hyos-to-Home report adapter against the supplied ELF."""
import argparse, hashlib, json, subprocess
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--sdk',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
here=Path(__file__).resolve().parent;source=here.parents[1];root=test_paths.TEST_DATA_ROOT
cpp=source/'app/src/main/cpp';rom=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
assert hashlib.sha256(rom.read_bytes()).hexdigest()=='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
binary=out/'report_test'
checks=[]
def run(name,cmd):
 r=subprocess.run(list(map(str,cmd)),capture_output=True,text=True)
 (out/(name+'.txt')).write_text(r.stdout+r.stderr)
 checks.append(dict(name=name,passed=r.returncode==0,returncode=r.returncode))
 if r.returncode:raise RuntimeError(name+': '+r.stderr+r.stdout)
run('build',['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-I'+str(cpp),'-I'+str(a.sdk.resolve()),'-I'+str(here/'stubs'),here/'report_runtime_test.cpp',cpp/'targets/home/home_report_hooks.cpp','-o',binary])
scenarios=['disabled','null-api','not-loaded-then-retry','wrong-note','wrong-elf','outside-executable','wrong-size','duplicate','expires-during-discovery','exact-rom-installs-once','body-mutated']
for name in scenarios:run(name,[binary,name,rom])
files=[cpp/'targets/hyos/hyos_report_runtime.cpp',cpp/'targets/home/home_report_hooks.cpp',cpp/'targets/home/home_report_guard.h']
report=dict(passed=all(c['passed'] for c in checks),checks=checks,scenarios=len(scenarios),sources=[dict(path=str(f.relative_to(source)),sha256=hashlib.sha256(f.read_bytes()).hexdigest()) for f in files],device_status='NOT_TESTED',limits=['SDK symbol resolver and inline-hook operations are fixtures; actual ROM ELF and production guards/installer are exercised.'])
(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
print(f'{len(scenarios)}/{len(scenarios)} PASS')
