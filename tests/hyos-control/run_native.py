#!/usr/bin/env python3
from pathlib import Path
import argparse,subprocess,tempfile,json,hashlib
p=argparse.ArgumentParser();p.add_argument('--sdk',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
base=Path(__file__).resolve().parent;root=base.parents[1]
files=[root/'app/src/main/cpp/targets/hyos'/n for n in ['hyos_control.cpp','hyos_control.h','hyos_entry.h','hyos_report_runtime.h','hyos_snapshot.h']]
files += [root/'app/src/main/cpp/targets/home'/n for n in ['home_startup.h','recents_runtime.h','home_layout_config.cpp','home_layout_config.h','home_layout_knobs.h','native_home_entry.h']]
files += [base/'control_test.cpp'] + sorted((base/'stubs').rglob('*.h'))
before={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files}
with tempfile.TemporaryDirectory(prefix='hc-hyos-control-') as t:
 binary=Path(t)/'control-test'
 cmd=['g++','-std=c++20','-Wall','-Wextra','-Werror','-pthread','-DHC_REAL_CONFIG','-DHYPERCEILER_HYOS_BACKEND','-I'+str(a.sdk.resolve()),'-I'+str(base/'stubs'),'-I'+str(root/'app/src/main/cpp'),str(base/'control_test.cpp'),str(root/'app/src/main/cpp/targets/home/home_layout_config.cpp'),'-o',str(binary)]
 build=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'build.txt').write_text(build.stdout)
 if build.returncode:print(build.stdout);raise SystemExit(build.returncode)
 run=subprocess.run([str(binary)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(a.output/'native.txt').write_text(run.stdout);print(run.stdout,end='')
 assert before=={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},'Inputs changed during build/test'
 (a.output/'native-report.json').write_text(json.dumps({'exit_code':run.returncode,'command':cmd,'build_test_inputs':before,'boundary':'actual production control.cpp; real std::thread interleavings; controlled worker scheduling, Binder, native_init and BOOTTIME clock and startup backend fixtures; production poll_flags/expire_lease execute; no Binder/device injection'},indent=2)+'\n')
 raise SystemExit(run.returncode)
