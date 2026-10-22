#!/usr/bin/env python3
"""Exercise actual production material binding against pinned ROM PT_LOAD bytes."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, subprocess, sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


HERE = Path(__file__).resolve().parent
ROOT = test_paths.TEST_DATA_ROOT
CPP = test_paths.SOURCE_ROOT / 'app/src/main/cpp'
TARGET = CPP / 'targets/weather'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    headers = [TARGET / name for name in ['weather_material_binding.h',
        'weather_material_guard.generated.h', 'weather_material_support.h', 'weather_snapshot.h']]
    tests = [HERE / name for name in ['run.py', 'generate.py', 'binding_test.cpp', 'compile_probe.cpp']]
    spec = importlib.util.spec_from_file_location('material_generator', HERE / 'generate.py')
    generator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(generator)
    _, manifest = generator.generate()
    images = ['Weather', 'Public', 'Private', 'Dart']
    inputs = headers + tests + [Path(x['path']) for x in manifest['inputs'].values()] + [Path(p) for p in manifest['evidence_inputs']]
    before = {str(p): sha(p) for p in inputs}
    commands = []
    def command(cmd, stem, timeout=180):
        commands.append(cmd)
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                text=True, timeout=timeout)
        (output / (stem + '.stdout.txt')).write_text(result.stdout)
        (output / (stem + '.stderr.txt')).write_text(result.stderr)
        return result
    regen = command([sys.executable, '-B', str(HERE / 'generate.py'), '--check', '--output',
                     str(output / 'regeneration')], 'regeneration')
    assert regen.returncode == 0, regen.stdout + regen.stderr
    manifest = json.loads((output / 'regeneration/guard-manifest.json').read_text())
    assert [len(manifest['guards'][name]) for name in images] == [15, 5, 4, 17]
    regions = [(i, r['va'], r['size']) for i, name in enumerate(images) for r in manifest['guards'][name]]
    guard_bits = sum(r[2] for r in regions) * 8
    assert guard_bits == 88928
    lines = ['#pragma once', '#include <array>', '#include <span>', 'namespace fixture {',
        'struct Load {uint64_t va,offset,filesz,memsz;};', 'struct Import {uintptr_t slot,target;};',
        'struct Region {size_t image;uintptr_t va;size_t size;};',
        'inline constexpr std::array<uintptr_t,4> bases={0x100000000ULL,0x200000000ULL,0x300000000ULL,0x400000000ULL};',
        f'inline constexpr uint64_t guard_bits={guard_bits};']
    for name in images:
        loads = manifest['inputs'][name]['loads']
        lines.append(f'inline constexpr std::array<Load,{len(loads)}> {name.lower()}_loads={{{{')
        lines += ['{%s,%s,%s,%s},' % (r['va'], r['file_offset'], r['filesz'], r['memsz']) for r in loads]
        lines.append('}};')
    lines.append('inline constexpr std::array<std::span<const Load>,4> loads={weather_loads,public_loads,private_loads,dart_loads};')
    for name, key in [('weather_imports', 'weather_imports'), ('private_imports', 'private_imports'), ('vtables', 'weather_vtables')]:
        values = manifest[key]
        lines.append(f'inline constexpr std::array<Import,{len(values)}> {name}={{{{')
        lines += ['{%s,%s},' % tuple(row) for row in values]
        lines.append('}};')
    lines.append(f'inline constexpr std::array<Region,{len(regions)}> regions={{{{')
    lines += ['{%s,%s,%s},' % row for row in regions]
    lines += ['}};', '}']
    fixture = output / 'fixture.generated.h'
    fixture.write_text('\n'.join(lines) + '\n')

    def host(include, stem):
        exe = output / stem
        result = command(['g++', '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror',
            '-Wno-mismatched-new-delete', '-pthread', '-fsanitize=undefined', '-fno-sanitize-recover=all',
            '-I', str(include), '-I', str(output), str(HERE / 'binding_test.cpp'), '-o', str(exe)], stem + '-build')
        assert result.returncode == 0, result.stdout + result.stderr
        result = command([str(exe), *[manifest['inputs'][name]['path'] for name in images]], stem + '-tests')
        match = re.search(r'RESULT passes=(\d+) failures=(\d+) guard_bit_mutations=(\d+)', result.stdout)
        return result, dict(passed=int(match[1]), failed=int(match[2]), guard_bit_mutations=int(match[3])) if match else None
    positive, counts = host(CPP, 'positive')
    assert positive.returncode == 0 and counts and counts['failed'] == 0 and counts['guard_bit_mutations'] == guard_bits, positive.stdout + positive.stderr

    text = headers[0].read_text()
    mutations = [
        ('ignore_guard_bytes', 'std::memcmp(bytes.data(), guard.bytes.data() + offset, count) != 0', 'false', 'all guard bit flips rejected'),
        ('ignore_callback_identity', 'observed != expected', 'false', 'actual relocated callback pointer 0'),
        ('skip_final_import_reread', 'public_again != public_get', 'false', 'Weather import changed during validation'),
        ('allow_running_state', 'return state == 3;', 'return state == 3 || state == 1;', 'warm running poisoned and waiter states refused'),
        ('publish_output_early', 'if (!verify_weather(weather_base, read)) return false;', 'out = {};\n    if (!verify_weather(weather_base, read)) return false;', 'only final caller output publication'),
        ('skip_core_private_body', 'if (!detail::image(private_base, weather_material_guard::kPrivateImageSpan,\n                       weather_material_guard::kPrivateGuards, read)) return false;', 'if (false) return false;', 'all guard bit flips rejected'),
    ]
    negative = []
    for name, old, new, witness in mutations:
        assert text.count(old) == 1, name
        include = output / ('mutation-' + name)
        dest = include / 'targets/weather'
        dest.mkdir(parents=True)
        for header in headers:
            (dest / header.name).write_bytes(text.replace(old, new).encode() if header == headers[0] else header.read_bytes())
        result, test_counts = host(include, name)
        detected = result.returncode != 0 and test_counts is not None and test_counts['failed'] > 0 and ('FAIL ' + witness) in result.stdout
        negative.append(dict(name=name, detected=detected, witness=witness, exit_code=result.returncode,
                             tests=test_counts, mutated_header_sha256=sha(dest / headers[0].name)))
    compiler = test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang++'
    ndk = command([str(compiler), '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror', '-fno-exceptions',
        '-fno-rtti', '-I', str(CPP), '-c', str(HERE / 'compile_probe.cpp'), '-o', str(output / 'ndk-api35.o')], 'ndk-api35')
    after = {str(p): sha(p) for p in inputs}
    report = dict(passed=before == after and all(x['detected'] for x in negative) and ndk.returncode == 0,
        tests=counts, negative_controls=negative, ndk=dict(passed=ndk.returncode == 0, exit_code=ndk.returncode,
        compiler=str(compiler), compiler_sha256=sha(compiler), object_sha256=sha(output / 'ndk-api35.o') if ndk.returncode == 0 else None),
        inputs_before=before, inputs_after=after, inputs_unchanged=before == after,
        production_inputs={str(p):before[str(p)] for p in headers},
        generated_fixture=dict(path=str(fixture), sha256=sha(fixture)),
        extraction_report=dict(path=str(output / 'regeneration/guard-manifest.json'), sha256=sha(output / 'regeneration/guard-manifest.json')),
        commands=commands, limitations=[
        'Actual ROM ELF PT_LOAD bytes, complete production header and actual relocation targets; relocations and readable-memory responses are simulated.',
        'All 88928 guard bits are independently mutated; guarded instructions are not executed in this binding suite. ARM execution is separately reported in arm-policy-v5.',
        'Heap allocation forbidden while resolving/guarding, maximum 256-byte stack chunks; no Binder, JNI, symbol lookup or patching.',
        'cold_state==3 is an observation, not a reservation. Legitimate initializer running states include 1 and 5 with waiters.',
        'Binding output assigned only after rechecks within one call; caller still owns cross-thread publication, startup synchronization, RELRO/CAS handling and TOCTOU after validation.',
        'No real Android linker, property service or device validation. NOT_TESTED on the phone.'])
    (output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(dict(passed=report['passed'], tests=counts, negative_controls=len(negative), ndk=ndk.returncode,
                          inputs_unchanged=before == after, report=str(output / 'report.json')), indent=2))
    return 0 if report['passed'] else 1

if __name__ == '__main__':
    raise SystemExit(main())
