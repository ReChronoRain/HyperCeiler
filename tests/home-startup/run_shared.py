#!/usr/bin/env python3
"""Run actual gateway assembly in a PIC DSO, with old PLT binding as a negative control."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import resource
import subprocess

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT = pathlib.Path(__file__).resolve().parents[2]
ARM = test_paths.BUILD_CACHE / 'arm64-emulation/root'
TEST = ROOT / 'tests/home-startup'
ASM = ROOT / 'app/src/main/cpp/targets/home/home_startup_arm64.S'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    inputs = [ASM, pathlib.Path(__file__).resolve()] + [TEST / f for f in (
        'gateway_test.cpp', 'gateway_probe.S', 'gateway_shared_entry.cpp', 'gateway_shared_driver.cpp')]
    hashes = {str(test_paths.report_path(p, ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    env = os.environ.copy()
    env['LD_LIBRARY_PATH'] = str(ARM / 'usr/lib/x86_64-linux-gnu')
    cxx = ARM / 'usr/bin/aarch64-linux-gnu-g++-13'
    objdump = ARM / 'usr/bin/aarch64-linux-gnu-objdump'
    report = {'passed': False, 'source_files': hashes, 'results': []}

    def run(name, command, expected=0):
        result = subprocess.run([str(x) for x in command], capture_output=True, text=True, env=env)
        (out / (name + '.stdout')).write_text(result.stdout)
        (out / (name + '.stderr')).write_text(result.stderr)
        okay = result.returncode == expected
        report['results'].append({'name': name, 'command': [str(x) for x in command],
                                  'returncode': result.returncode, 'expected': expected, 'passed': okay})
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
        assert okay, (name, result.returncode, result.stderr)
        print(name, 'PASS', flush=True)
        return result

    driver = out / 'gateway-driver'
    common = [cxx, '--sysroot=' + str(ARM), '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror']
    run('driver-build', common + [TEST / 'gateway_shared_driver.cpp', '-ldl', '-o', driver])
    asm = ASM.read_text()
    assert asm.count('.hidden \\target') == 1
    baseline = out / 'old-plt-gateway.S'
    baseline.write_text(asm.replace('.hidden \\target', ''))
    for name, source, negative in [('fixed', ASM, False), ('old-plt', baseline, True)]:
        library = out / ('libgateway-' + name + '.so')
        run(name + '-build', common + ['-fPIC', '-shared', '-fvisibility=hidden', '-Dmain=gateway_test_main',
            TEST / 'gateway_test.cpp', TEST / 'gateway_probe.S', TEST / 'gateway_shared_entry.cpp',
            source, '-o', library])
        disassembly = run(name + '-disassembly', [objdump, '-d', library]).stdout
        branches = []
        for index in range(15):
            match = re.search(r'^([0-9a-f]+) <hc_home_startup_' + str(index) + r'>:\n(.*?)(?=\n\n)',
                              disassembly, re.M | re.S)
            assert match, index
            branch = re.search(r'^\s*([0-9a-f]+):\s+[0-9a-f]+\s+b\s+([0-9a-f]+) <([^>]+)>', match[2], re.M)
            assert branch, index
            assert branch[3].endswith('@plt') == negative, branch[3]
            branches.append({'gateway': index, 'va': '0x' + match[1], 'branch_va': '0x' + branch[1],
                             'target_va': '0x' + branch[2], 'target': branch[3]})
        execution = run(name + '-run', [ARM / 'usr/bin/qemu-aarch64-static', '-L',
            ARM / 'usr/aarch64-linux-gnu', driver, library], 1 if negative else 0)
        if negative:
            assert 'x17 mismatch: gateway=0 enabled=1' in execution.stderr
            # Inspect the linked PLT path actually executed by the negative control.
            plt = re.search(r'<hc_layout_dart_HotseatMargin_entry@plt>:\n(.*?)(?=\n\n)',
                            disassembly, re.S)
            assert plt and re.search(r'ldr\s+x17,', plt[1]) and re.search(r'br\s+x17', plt[1])
        else:
            summary = json.loads(execution.stdout.strip().splitlines()[-1])
            assert summary['gateway_cases'] == 360 and summary['passed'] == 360
        report[name] = {'sha256': hashlib.sha256(library.read_bytes()).hexdigest(),
                        'gateway_branches': branches, 'executed': True,
                        'negative_control_rejected': negative,
                        'result': 'x17 contract violation detected' if negative else '360/360'}
    report['inputs_unchanged'] = all(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == h
                                     for p, h in hashes.items())
    assert report['inputs_unchanged']
    report['limits'] = ['Actual production gateway assembly is dynamically loaded from a PIC ELF DSO.',
        'Destinations are controlled register-capture stubs, not the full Dart runtime or Android inline-hook backend.',
        'No -Bsymbolic or linker-wide local-binding workaround is used; only the production .hidden target controls binding.',
        'AArch64 execution uses QEMU/Linux; final Android shared ELF must separately show all 15 direct local branches.']
    report['passed'] = True
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'passed': True, 'fixed_cases': 360, 'old_plt_rejected': True}))


if __name__ == '__main__':
    main()
