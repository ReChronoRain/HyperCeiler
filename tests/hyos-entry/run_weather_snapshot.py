#!/usr/bin/env python3
"""Compile the real shared Home/Weather first-snapshot observer with controlled callbacks."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


HERE = Path(__file__).resolve().parent
SOURCE = HERE.parents[1]
CPP = SOURCE / 'app/src/main/cpp'
HYOS = CPP / 'targets/hyos'
SHA = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--sdk', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--arm64', action='store_true')
    p.add_argument('--mutations', action='store_true')
    a = p.parse_args()
    out = a.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    inputs = [HERE/n for n in ['run_weather_snapshot.py','weather_snapshot_test.cpp','snapshot_test.cpp','entry_test.cpp']]
    inputs += [HYOS/n for n in ['hyos_entry.cpp','hyos_entry.h','hyos_control.h','hyos_runtime_guard.h','hyos_snapshot_guard.h','hyos_startup_reason.h']]
    inputs += [CPP/'targets/weather'/n for n in ['weather_runtime.h','weather_guard.generated.h','weather_binding.h','weather_device_level.h','weather_snapshot.h','weather_card_lifetime.h']]
    inputs += [CPP/'nativehook'/n for n in ['memory_io.h','nhk_base.h']]
    inputs += [a.sdk.resolve()/'zygisk_next_api.h'] + sorted((HERE/'stubs').rglob('*.h'))
    inputs += [CPP/'targets/home/recents_runtime.h', HYOS/'hyos_snapshot.h']
    before = {str(p):SHA(p) for p in inputs}
    env = dict(os.environ)
    compiler = 'g++'
    runner = []
    flags = ['-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread']
    if a.arm64:
        arm = test_paths.BUILD_CACHE / 'arm64-emulation/root'
        compiler = str(arm/'usr/bin/aarch64-linux-gnu-g++-13')
        runner = [str(arm/'usr/bin/qemu-aarch64-static')]
        env['LD_LIBRARY_PATH'] = str(arm/'usr/lib/x86_64-linux-gnu')
        flags += ['--sysroot='+str(arm),'-static','-DHC_ARM64_FIXTURE_READS=1']
    flags += ['-I'+str(a.sdk.resolve()),'-I'+str(HERE/'stubs'),'-I'+str(CPP),'-I'+str(HYOS)]
    commands = []

    def run(name, argv):
        r = subprocess.run(list(map(str,argv)),env=env,capture_output=True,text=True,timeout=180)
        (out/(name+'.log')).write_text(r.stdout+r.stderr)
        commands.append(dict(name=name,argv=list(map(str,argv)),exit_code=r.returncode))
        return r

    built = run('build',[compiler,*flags,HERE/'weather_snapshot_test.cpp','-o',out/'observer-test'])
    assert built.returncode==0,built.stdout+built.stderr
    result = run('positive',[*runner,out/'observer-test'])
    m = re.search(r'(\d+)/(\d+) PASS; Weather card actual observer',result.stdout)
    assert result.returncode==0 and m and int(m[1])==int(m[2])==30,result.stdout+result.stderr
    assert 'rejected 6656 Weather identity bit mutations' in result.stdout
    negatives = []
    if a.mutations:
        original=(HYOS/'hyos_entry.cpp').read_text()
        mutations=[
            ('ignore_pending','g_startup_armed.store(weather_runtime::card_startup_pending(),','g_startup_armed.store(true,'),
            ('wrong_card_route','else weather_runtime::on_first_card_snapshot(dart, source - kSnapshotDlsymReturnVa);','else hyos_control::on_first_home_snapshot(dart);'),
            ('skip_weather_identity',': boundary_matches(result - vm_data_va, 0, weather_guard::kDartBytes0)',': true'),
            ('skip_thread','|| g_search_tid != static_cast<pid_t>(syscall(SYS_gettid))','|| false'),
            ('skip_handle','|| g_search_handle != handle','|| (g_search_handle != handle && false)'),
            ('skip_flutter_binding','|| g_search_flutter_base != source - kSnapshotDlsymReturnVa','|| false'),
            ('discard_weather_miss','else if (route == StartupRoute::WeatherCard) weather_runtime::on_card_snapshot_window_missed(reason);','else if (route == StartupRoute::WeatherCard) (void)reason;'),
            ('dispatch_after_cancel','if (g_snapshot_attempt.compare_exchange_strong(checking, 4, std::memory_order_acq_rel))','if ((void)checking, true)'),
            ('lose_errno','errno = saved_errno;','(void)saved_errno;'),
        ]
        with tempfile.TemporaryDirectory(prefix='weather-entry-mutations-') as tmp:
            base=Path(tmp);testdir=base/'tests/hyos-entry';prod=base/'app/src/main/cpp/targets/hyos'
            testdir.mkdir(parents=True);prod.mkdir(parents=True)
            for name in ['entry_test.cpp','snapshot_test.cpp','weather_snapshot_test.cpp']:
                (testdir/name).write_bytes((HERE/name).read_bytes())
            (prod/'hyos_control.h').write_bytes((HYOS/'hyos_control.h').read_bytes())
            for name,old,new in mutations:
                assert old in original,name
                changed=original.replace(old,new)
                (prod/'hyos_entry.cpp').write_text(changed)
                binary=out/(name+'-test')
                built=run(name+'-build',[compiler,*flags,testdir/'weather_snapshot_test.cpp','-o',binary])
                assert built.returncode==0,built.stdout+built.stderr
                result=run(name+'-rejected',[*runner,binary])
                detected=result.returncode!=0 and ('Assertion' in result.stderr or 'Assertion' in result.stdout)
                negatives.append(dict(name=name,detected=detected,exit_code=result.returncode,mutated_entry_sha256=hashlib.sha256(changed.encode()).hexdigest()))
    after={str(p):SHA(p) for p in inputs}
    passed=before==after and all(x['detected'] for x in negatives)
    report=dict(passed=passed,tests=30,failed=0,weather_identity_bits_rejected=6656,negative_controls=negatives,
        mode='ARM64_QEMU_BOUNDED_DECLARED_READ_FIXTURES' if a.arm64 else 'HOST_REAL_SELF_PROCESS_VM_READV',
        inputs_before=before,inputs_after=after,inputs_unchanged=before==after,commands=commands,
        device_status='NOT_TESTED',limitations=[
            'Compiles actual hyos_entry.cpp. Original loader/controller/Weather policy implementations are controlled fixtures.',
            'Host normal reads use actual process_vm_readv; denial and interruption are injected. ARM QEMU requires explicit readable-memory fixtures.',
            'Tests prove callback routing/cancellation notification, not complete installer rollback or absence of concurrent Dart consumption.',
            'Another successful same-name lookup may conservatively consume/cancel the only window; no loader-thread wait is introduced.',
            'No actual Hyos injection, SELinux/syscall policy admission, Dart VM, real startup order, UI or long-term phone behavior is proven.'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(passed=passed,tests=30,mutations=len(negatives),mode=report['mode'],report=str(out/'report.json')),indent=2))
    return 0 if passed else 1


if __name__=='__main__':
    raise SystemExit(main())
