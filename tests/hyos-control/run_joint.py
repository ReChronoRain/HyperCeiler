#!/usr/bin/env python3
"""Compile actual entry/control together, using controlled loader/Binder/worker fixtures."""
from pathlib import Path
import argparse,subprocess,tempfile,json,hashlib
p=argparse.ArgumentParser();p.add_argument('--sdk',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
base=Path(__file__).resolve().parent;root=base.parents[1]
with tempfile.TemporaryDirectory(prefix='hc-hyos-joint-') as t:
 binary=Path(t)/'joint-test'
 cmd=['g++','-std=c++20','-Wall','-Wextra','-Werror','-pthread','-I'+str(a.sdk.resolve()),'-I'+str(base/'stubs'),'-I'+str(base.parent/'hyos-entry/stubs'),'-I'+str(root/'app/src/main/cpp'),str(base/'joint_entry_fixture.cpp'),str(base/'joint_control_test.cpp'),'-o',str(binary)]
 build=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'build.txt').write_text(build.stdout)
 if build.returncode:print(build.stdout);raise SystemExit(build.returncode)
 run=subprocess.run([str(binary)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(a.output/'joint.txt').write_text(run.stdout);print(run.stdout,end='')
 files=[root/'app/src/main/cpp/targets/hyos'/n for n in ['hyos_entry.cpp','hyos_runtime_guard.h','hyos_control.cpp','hyos_control.h','hyos_entry.h','hyos_snapshot_guard.h','hyos_startup_reason.h','hyos_snapshot.h']]
 files += [root/'app/src/main/cpp/targets/home/recents_runtime.h']
 files+=list((root/'app/src/main/cpp/targets/weather').glob('*.h'))
 (a.output/'joint-report.json').write_text(json.dumps({'exit_code':run.returncode,'command':cmd,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},'test_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in [base/'joint_entry_fixture.cpp',base/'joint_control_test.cpp',base/'control_test.cpp',Path(__file__)]},'boundary':'actual entry.cpp and control.cpp linked as separate TUs; real std::thread interleavings and bounded process_vm_readv; actual HCS decoder/lease/phase with fixture loader/Binder/worker scheduling/clock/native_init/startup executor/report installer; ASM tail call is independently exercised by hyos-entry/run_arm64.py; no device or real Hyos injection'},indent=2)+'\n')
 raise SystemExit(run.returncode)
