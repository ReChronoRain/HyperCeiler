#!/usr/bin/env python3
"""Exercise the actual first-snapshot routing through control and patch journal."""
import argparse, hashlib, json, subprocess, tempfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--sdk',type=Path,required=True)
p.add_argument('--rom-dart',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
base=Path(__file__).resolve().parent;root=base.parents[1];cpp=root/'app/src/main/cpp'
inputs=[base/n for n in ['joint_executor_test.cpp','joint_entry_fixture.cpp','control_test.cpp']]
inputs += list((base/'stubs').rglob('*.h'))+list((base.parent/'hyos-entry/stubs').rglob('*.h'))
inputs += [base.parent/'home-startup'/n for n in ['startup_fixture.h','startup_fixture.cpp']]
inputs += list((cpp/'targets/hyos').glob('*.h'))+[cpp/'targets/hyos'/n for n in ['hyos_entry.cpp','hyos_control.cpp']]
inputs += list((cpp/'targets/weather').glob('*.h'))
inputs += [cpp/'targets/home'/n for n in ['home_startup.cpp','recents_runtime.h','home_startup.h','home_startup_plan.generated.h','home_layout_config.h','home_layout_knobs.h','native_home_entry.h']]
inputs += [cpp/'nativehook'/n for n in ['elf_image.h','nhk_base.h']]
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
before={str(f):sha(f) for f in sorted(set(inputs))}
rom_sha=sha(a.rom_dart)
assert rom_sha=='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
with tempfile.TemporaryDirectory(prefix='hc-three-layer-') as t:
    binary=Path(t)/'test'
    command=['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-DHOME_STARTUP_TEST',
        '-I'+str(a.sdk.resolve()),'-I'+str(base/'stubs'),'-I'+str(base.parent/'hyos-entry/stubs'),'-I'+str(cpp),
        str(base/'joint_executor_test.cpp'),str(base/'joint_entry_fixture.cpp'),
        str(base.parent/'home-startup/startup_fixture.cpp'),str(cpp/'targets/home/home_startup.cpp'),'-o',str(binary)]
    build=subprocess.run(command,capture_output=True,text=True)
    (a.output/'build.log').write_text(build.stdout+build.stderr)
    if build.returncode:print(build.stdout+build.stderr);raise SystemExit(build.returncode)
    run=subprocess.run([str(binary),str(a.rom_dart.resolve())],capture_output=True,text=True,timeout=90)
    (a.output/'run.log').write_text(run.stdout+run.stderr);print(run.stdout+run.stderr)
    unchanged=before=={str(f):sha(f) for f in sorted(set(inputs))}
    report={'passed':run.returncode==0 and unchanged,'exit_code':run.returncode,'inputs_unchanged':unchanged,
        'command':command,'inputs':before,'rom_dart_sha256':rom_sha,
        'boundary':'Actual entry/controller/executor; fixture loader, Binder, clock, hook backend and Dart consumer ABI. Exact ROM mapped as host data, never executed. No Android/Hyos/LSPosed injection evidence.',
        'real_os4_injection':'NOT_TESTED','all_settings_on_device':'NOT_TESTED','long_term_stability':'NOT_TESTED'}
    (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    raise SystemExit(0 if report['passed'] else 1)
