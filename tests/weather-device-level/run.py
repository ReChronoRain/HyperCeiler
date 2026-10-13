#!/usr/bin/env python3
"""Execute pinned OS4 DeviceLevel/Rust allocator code and the real C++ policy in QEMU."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import resource
import subprocess
import tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT = Path(__file__).resolve().parents[2]
ARM = test_paths.BUILD_CACHE / 'arm64-emulation/root'
PUBLIC = test_paths.EXTRACTED_ROM_ROOT / 'system_ext/lib64/libhyper_os_system_public.so'
STANDARD = test_paths.EXTRACTED_ROM_ROOT / 'system/system/lib64/libstd.dylib.so'
EXPECTED = {
    PUBLIC: 'f1a4a3efc287207004fb487e5d525ca797d6a6e6616317a462d58115066c14f2',
    STANDARD: '1b656d82be749751208de1625dff0ca40f477009c368d639a792f467461528b1',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    cpp = ROOT / 'app/src/main/cpp'
    header = cpp / 'targets/weather/weather_device_level.h'
    inputs = list(EXPECTED) + [Path(__file__).resolve(), Path(__file__).with_name('device_level_test.cpp'),
                             header, cpp / 'targets/weather/weather_snapshot.h']
    hashes = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    for path, expected in EXPECTED.items():
        assert hashes[str(path)] == expected, path
    env = dict(os.environ, LD_LIBRARY_PATH=str(ARM / 'usr/lib/x86_64-linux-gnu'))
    compiler = ARM / 'usr/bin/aarch64-linux-gnu-g++-13'
    source = Path(__file__).with_name('device_level_test.cpp')
    report = {'passed': False, 'inputs': hashes, 'runs': []}
    with tempfile.TemporaryDirectory(prefix='hc-weather-owned-') as name:
        temporary = Path(name)
        variants = [('baseline', None),
                    ('wrong_ui_mapping', ('chosen.device_level + 1', 'chosen.device_level')),
                    ('skip_null_override', ('const bool created = object == nullptr;',
                                           'if (object == nullptr) return {nullptr, Outcome::Passthrough};\n    const bool created = object == nullptr;')),
                    ('changed_selector', ('api.get(runtime, selector);', 'api.get(runtime, selector ^ 1);')),
                    ('mixed_gpu', ('bytes[2] = native_level;', 'bytes[2] = 1;')),
                    ('stale_revision', ('|| current.revision != chosen.revision', '|| false'))]
        for variant, mutation in variants:
            includes = ['-I' + str(cpp)]
            if mutation:
                assert header.read_text().count(mutation[0]) == 1, variant
                directory = temporary / variant / 'targets/weather'
                directory.mkdir(parents=True)
                mutated = directory / header.name
                mutated.write_text(header.read_text().replace(*mutation))
                includes.insert(0, '-I' + str(temporary / variant))
                # Relative production include resolves to the unchanged header.
                (directory / 'weather_snapshot.h').write_bytes((header.parent / 'weather_snapshot.h').read_bytes())
            binary = temporary / ('probe-' + variant)
            command = [str(compiler), '--sysroot=' + str(ARM), '-std=c++20', '-O2',
                       '-Wall', '-Wextra', '-Werror', '-pthread', *includes, str(source), '-o', str(binary)]
            build = subprocess.run(command, env=env, capture_output=True, text=True, timeout=120)
            (out / (variant + '-build.txt')).write_text(build.stdout + build.stderr)
            assert build.returncode == 0, (variant, build.stderr)
            run = subprocess.run([str(ARM / 'usr/bin/qemu-aarch64-static'), '-L',
                                  str(ARM / 'usr/aarch64-linux-gnu'), str(binary), str(PUBLIC), str(STANDARD)],
                                 env=env, capture_output=True, text=True, timeout=60)
            (out / (variant + '-run.txt')).write_text(run.stdout + run.stderr)
            success = run.returncode == 0 if mutation is None else (
                run.returncode != 0 and 'Assertion' in run.stderr)
            row = {'name': variant, 'passed': success, 'returncode': run.returncode,
                   'binary_sha256': hashlib.sha256(binary.read_bytes()).hexdigest(), 'command': command,
                   'negative_control': mutation is not None}
            if mutation is None and run.returncode == 0:
                row['result'] = json.loads(run.stdout.strip().splitlines()[-1])
            report['runs'].append(row)
            (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
            assert success, (variant, run.stdout, run.stderr)
            print(variant, 'PASS', flush=True)
    report['inputs_unchanged'] = all(hashlib.sha256(Path(p).read_bytes()).hexdigest() == h
                                     for p, h in hashes.items())
    report['limits'] = [
        'Original ELF PT_LOAD bytes execute at their relative virtual addresses; no public function or allocator instruction is replaced.',
        'Fixture relocations bind Runtime conversion/private level provider and allocator libc malloc/free to controlled native functions.',
        'The production policy calls the pinned original DeviceLevel_get_device_level and supplemental Rust allocator/shim; cancellation invokes pinned original drop/deallocator.',
        'The fixture acts as the Weather caller, reading the result with all three pinned public getters and releasing it with pinned original drop/deallocator.',
        'This is AArch64 Linux/QEMU with synthetic runtime/authorization and no Android linker, Binder, Weather app lifecycle, LSPosed or phone execution.',
        'Production installation, image/import identity guards and first-use control remain separate required work.'
    ]
    report['passed'] = report['inputs_unchanged'] and all(r['passed'] for r in report['runs'])
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'passed': report['passed'], 'cases': report['runs'][0]['result']['cases'],
                      'negative_controls': len(report['runs']) - 1}))


if __name__ == '__main__':
    main()
