#!/usr/bin/env python3
"""Original secondary fixed calculation and spacing; bounded synthetic environment."""
import argparse
import hashlib
import json
import os
import resource
import struct
import subprocess
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


W = Path(__file__).resolve().parent
R = test_paths.TEST_DATA_ROOT
ARM = test_paths.BUILD_CACHE / 'arm64-emulation/root'
OBJ = test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump'
ROM = test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
PIN = 'a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--positive-only', action='store_true')
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(exist_ok=False, parents=True)
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    catalog_path = test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/dart-v5/extraction.json'
    inputs = [Path(__file__).resolve(), W / 'quantized_fixture.cpp', test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S', catalog_path,W/'quantized_gateway.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/fixture.cpp']
    report = dict(passed=False, device_status='NOT_TESTED', artifact_runtime_approval=False,
                  inputs_before={str(test_paths.report_path(p, R)): sha(p) for p in inputs}, runs=[])
    save = lambda: (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    save()
    assert sha(ROM) == PIN
    report['rom'] = dict(path=str(test_paths.report_path(ROM, R)), sha256=PIN)
    data = ROM.read_bytes()
    phoff = struct.unpack_from('<Q', data, 32)[0]
    entsize, count = struct.unpack_from('<HH', data, 54)

    def read(va, size):
        for ix in range(count):
            typ, flags, off, base, _, filesz, _, _ = struct.unpack_from('<IIQQQQQQ', data, phoff + entsize * ix)
            if typ == 1 and flags & 1 and base <= va and va + size <= base + filesz:
                return data[off + va - base:off + va - base + size]
        raise AssertionError('not file-backed executable bytes')

    functions = []
    catalog = json.loads(catalog_path.read_text())['symbols']
    starts = [0xa49adc, 0xa49e60, 0xa49f64, 0xa4a890, 0xa4a698, 0xa4a7b8,
              0xa4a83c, 0xa4a674, 0xa4a650, 0xa4a584, 0xa4418c, 0x17d0f8c,
              0xa4ab18, 0xa4ab38, 0xa4ab58, 0xa4ac00, 0xa4ac58, 0xa4ac78,
              0xa4ac98, 0xa4acb8, 0xa4acd8]
    for va in starts:
        info = catalog[hex(va)]
        raw = read(va, info['size'])
        (out / f'{va:x}.bin').write_bytes(raw)
        result = subprocess.run([str(OBJ), '-D', f'--start-address={va}', f'--stop-address={va + info["size"]}', str(ROM)], capture_output=True, text=True, check=True)
        (out / f'{va:x}.asm.txt').write_text(result.stdout)
        functions.append(dict(va=hex(va), **info, sha256=hashlib.sha256(raw).hexdigest()))
    # Additional non-catalogued complete fast/lazy getter window, independently
    # read as 68 bytes through its return, not assigned a guessed symbol name.
    (out / 'a3d210.bin').write_bytes(read(0xa3d210, 68))
    result = subprocess.run([str(OBJ), '-D', '--start-address=10736144', '--stop-address=10736212', str(ROM)], capture_output=True, text=True, check=True)
    (out / 'a3d210.asm.txt').write_text(result.stdout)
    report['original_functions'] = functions
    report['extraction_scope'] = 'Catalogued complete bodies extracted for review; extraction alone is not execution. Navigation getter 0xa4ac00 is a declared zero provider in the fixture.'
    report['declared_adapters'] = [
        {'va': '0x1b57c08', 'kind': 'bounded synthetic array allocation'},
        {'va': '0x18aa0e0', 'kind': 'bounded synthetic allocation'},
        {'va': '0xe1d028', 'kind': 'synthetic view lookup, real alignPixel chooses CID2390 branch'},
        {'va': '0xb19564', 'kind': 'Rx setter storage/count only; no stream/equality execution'},
        {'va': '0x1781f28', 'kind': 'centering condition fixed to bit4-set false branch'},
        {'va': '0x18a363c', 'kind': 'boxed subtraction for selected zero/Double operands'},
        {'va': '0x18a3aa4', 'kind': 'boxed addition for selected zero/Double operands'},
        {'va': '0x868b8c', 'kind': 'log string interpolation ignored'},
        {'va': '0x86af0c', 'kind': 'logging ignored'},
        {'va': '0xa4ac00', 'kind': 'navigation-height provider fixed to zero'},
    ]
    for va, word in [(0xa49dec, 0x9400001d), (0xa49f04, 0x94033d98), (0xa49f34, 0x1e612802)]:
        assert read(va, 4) == struct.pack('<I', word)
    report['target_instruction_guards'] = True
    report['tool_inputs'] = {str(p): sha(p) for p in [OBJ, ARM / 'usr/bin/aarch64-linux-gnu-g++-13', ARM / 'usr/bin/qemu-aarch64-static']}
    env = dict(os.environ, LD_LIBRARY_PATH=str(ARM / 'usr/lib/x86_64-linux-gnu'))

    def run(label, command, expected=0, witness=None):
        command = list(map(str, command))
        try:
            result = subprocess.run(command, env=env, capture_output=True, text=True, timeout=60)
        except subprocess.TimeoutExpired as error:
            def decode(x): return x.decode(errors='replace') if isinstance(x,bytes) else (x or '')
            (out / (label + '.stdout')).write_text(decode(error.stdout))
            (out / (label + '.stderr')).write_text(decode(error.stderr)+'\nTIMEOUT: 60 seconds\n')
            report['runs'].append(dict(label=label,command=command,timeout_seconds=60,passed=False))
            save()
            raise
        (out / (label + '.stdout')).write_text(result.stdout)
        (out / (label + '.stderr')).write_text(result.stderr)
        okay = result.returncode == expected and (witness is None or witness in result.stderr)
        report['runs'].append(dict(label=label, command=command, exit_code=result.returncode, expected=expected, witness=witness, passed=okay))
        save()
        assert okay, (label, result.returncode, result.stderr[-3000:])
        return result

    binary = out / 'probe'
    run('compile', [ARM / 'usr/bin/aarch64-linux-gnu-g++-13', '--sysroot=' + str(ARM), '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror', W / 'quantized_fixture.cpp', test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',W/'quantized_gateway.S', '-o', binary])
    command = [ARM / 'usr/bin/qemu-aarch64-static', '-L', ARM / 'usr/aarch64-linux-gnu', binary, ROM]
    report['positive'] = json.loads(run('positive', command).stdout)
    assert report['positive']['passed'] and report['positive']['cases'] == 3360
    if not args.positive_only:
        for name,witness in {'missing-dpr-capture':'candidate cell is pixel-truncated before original publication','round-instead-of-trunc':'candidate cell is pixel-truncated before original publication','old-row-cache':'row Rx retains original selected Smi publication','lost-first-store':'candidate cell is pixel-truncated before original publication'}.items():
            run(name,command+[name],3,witness)
    report['inputs_after'] = {str(test_paths.report_path(p, R)): sha(p) for p in inputs}
    report['inputs_unchanged'] = report['inputs_before'] == report['inputs_after']
    assert report['inputs_unchanged'] and sha(ROM) == PIN
    report['binary_sha256'] = sha(binary)
    report['passed'] = True
    report['scope'] = [
        'Candidate rewrites 8 exact fixedRows words (4 selected dimension reads/capture, 3 DPR capture/subtract words, one direct B) and a 24-byte no-call splice. All original fixedRows downstream spacing/math/Rx-value calls retain their exact callsites and full original bodies.',
        'Original alignPixel executes with synthetic CID2390 view/DPR provider and host C trunc/round imports. Selected DPR values are not measured phone DPR.',
        'Original Rx.value executes with dependency tracking inactive. Rx.value= is explicit store/count adapter; real stream notifications/equality/disposal not executed.',
        'Allocation, log interpolation/logging, boxed arithmetic, zero navigation-height provider and centering condition have declared adapters; no Dart VM/GC, screen UI, database, FRB or production patch installed.',
        'Original negative-margin baseline is separately frozen. Candidate tests two synthetic geometries, all 70 UI pairs, three raw layout values and eight DPR inputs including four invalid values; this does not approve all view or database states or a production installer.',
    ]
    save()
    print(json.dumps(dict(passed=True, report=str(out / 'report.json'), result=report['positive'])))


if __name__ == '__main__':
    main()
