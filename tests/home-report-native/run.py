#!/usr/bin/env python3
"""Build real Home report guard/installer and execute the actual ARM64 trampoline.

The copied ROM predicate executes with synthetic Dart objects and an equality
dispatch fixture. This is ABI execution evidence, not a running Dart VM/device.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


sys.dont_write_bytecode = True
SOURCE = Path(__file__).resolve().parents[2]
PROJECT = test_paths.TEST_DATA_ROOT
HERE = Path(__file__).parent
CPP = SOURCE / 'app/src/main/cpp'
HOME = CPP / 'targets/home'
RUNTIME = test_paths.BUILD_CACHE / 'arm64-emulation/root'
SCENARIOS = ['guard', 'not_loaded_then_retry', 'invalid_runtime', 'unreadable',
             'wrong_size', 'unaligned', 'mutated_body', 'wrong_name', 'duplicate_address',
             'duplicate_size', 'duplicate_same', 'failure_then_retry',
             'missing_original_then_retry', 'rollback_failure_terminal',
             'idempotence_and_switch', 'conflicting_image', 'concurrent_install']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--skip-arm64', action='store_true')
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    spec = importlib.util.spec_from_file_location('elf', SOURCE/'tests/home-layout-native/fixture_symbols.py')
    elf = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(elf)
    image = test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
    raw = image.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != 'a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992':
        raise ValueError('Fixture is not the supplied 7719 image')
    targets = [x for x in elf.symbols(image) if x[2]=='UninstallController._shouldShowReport']
    if len(targets)!=1:
        raise ValueError('Ambiguous fixture symbol')
    va, size, name = targets[0]
    _, sections = elf.sections(raw)
    containing = [s for s in sections.values() if s[2]&4 and s[3]<=va and va+size<=s[3]+s[5]]
    if len(containing)!=1:
        raise ValueError('Ambiguous executable section')
    section = containing[0]
    offset=section[4]+va-section[3]
    fixture=out/'predicate-7719.bin'
    fixture.write_bytes(raw[offset:offset+size])
    commands=[]
    def run(label, command, env=None):
        result=subprocess.run(list(map(str,command)),text=True,capture_output=True,env=env)
        (out/(label+'.log')).write_text('$ '+' '.join(map(str,command))+'\n'+result.stdout+result.stderr)
        commands.append({'label':label,'command':list(map(str,command)),'returncode':result.returncode})
        if result.returncode:
            print(result.stdout+result.stderr)
            raise RuntimeError(label+' failed')
        if result.stdout: print(result.stdout.strip())
    common=['-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-I'+str(CPP)]
    host=out/'install_test'
    run('host-build',['g++',*common,HERE/'install_test.cpp',HOME/'home_report_hooks.cpp','-o',host])
    for scenario in SCENARIOS:run('host-'+scenario,[host,scenario,fixture])
    if not args.skip_arm64:
        compiler=RUNTIME/'usr/bin/aarch64-linux-gnu-g++-13'
        binary=out/'arm64_test'
        compiler_env=os.environ.copy()
        compiler_env['LD_LIBRARY_PATH']=str(RUNTIME/'usr/lib/x86_64-linux-gnu')+(
            ':'+compiler_env['LD_LIBRARY_PATH'] if compiler_env.get('LD_LIBRARY_PATH') else '')
        run('arm64-build',[compiler,'--sysroot='+str(RUNTIME),*common,'-static',
             HERE/'arm64_test.cpp',HERE/'arm64_probe.S',HOME/'home_report_hooks.cpp',
             HOME/'home_report_dart_arm64.S','-o',binary],env=compiler_env)
        run('arm64-execute',[RUNTIME/'usr/bin/qemu-aarch64-static',binary,fixture])
    sources=[HOME/('home_report_'+suffix) for suffix in ['guard.h','hooks.h','hooks.cpp','dart_arm64.S']]
    report={'fixture':{'image':str(image),'sha256':digest,'symbol':name,'va':va,'size':size,
                       'body_sha256':hashlib.sha256(fixture.read_bytes()).hexdigest()},
            'production_sources':[{'path':str(p.relative_to(SOURCE)),
                                   'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sources],
            'host_scenarios':len(SCENARIOS),'guard_bit_mutations_rejected':size*8,
            'arm64_case_combinations':0 if args.skip_arm64 else 20,
            'commands':commands,'passed':all(x['returncode']==0 for x in commands),
            'device_status':'NOT_TESTED',
            'limits':['Host installer uses hook API fixtures, not ZygiskNext inline patching.',
                      'QEMU executes the production trampoline and unmodified ROM predicate bytes with synthetic Dart objects and a controlled equality dispatch.',
                      'Dart garbage collection, exception unwinding, actual UI visibility, Hyos injection and sustained device stability are not validated.']}
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print('PASS report:',out/'report.json')


if __name__=='__main__':main()
