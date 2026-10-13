#!/usr/bin/env python3
"""Run the complete card transaction over mapped pinned ROM images on the host."""
from pathlib import Path
import argparse, hashlib, json, re, struct, subprocess, tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


HERE = Path(__file__).resolve().parent
SOURCE = HERE.parents[1]
ROOT = test_paths.TEST_DATA_ROOT
CPP = SOURCE / 'app/src/main/cpp'
WEATHER = CPP / 'targets/weather'
NDK = test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
DART = test_paths.TEST_DATA_ROOT / 'acceptance/native-evidence/weather-libapp.so'
FLUTTER = test_paths.ROM_ROOT / 'components/system_ext/lib64/libhyper_os_flutter.so'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def linked_templates(path):
    data = path.read_bytes()
    eh = struct.unpack_from('<16sHHIQQQIHHHHHH', data)
    rows = [struct.unpack_from('<IIQQQQIIQQ', data, eh[6] + i * eh[11]) for i in range(eh[12])]
    assert eh[1] == 3  # ET_DYN: local literal relocations must already be resolved.
    assert all(not r[5] for r in rows if r[1] in (4, 9)), 'unexpected linked relocations'
    symbols = {}
    for row in rows:
        if row[1] != 2: continue
        strings = rows[row[6]]
        names = data[strings[4]:strings[4] + strings[5]]
        for off in range(row[4], row[4] + row[5], row[9]):
            name, _, _, _, value, _ = struct.unpack_from('<IBBHQQ', data, off)
            symbols[names[name:names.index(b'\0', name)].decode()] = value
    result = {}
    for kind in ['recompute', 'constructor']:
        name = 'hc_weather_card_' + kind + '_template'
        begin = symbols[name]
        assert symbols[name+'_code_end'] - begin == 284
        assert symbols[name+'_lease_literal'] - begin == 288
        assert symbols[name+'_end'] - begin == 296
        owners = [r for r in rows if r[1] == 1 and r[3] <= begin and begin + 296 <= r[3] + r[5]]
        assert len(owners) == 1
        row = owners[0]; offset = row[4] + begin - row[3]
        result[kind] = data[offset:offset+296]
    return result

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--negative-controls', action='store_true')
    a = p.parse_args()
    out = a.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    assert sha(DART) == '5b458c7a5a34bc64336b4895229f3a04e8a21edafd8c8b2c6d7926258f90b7fc'
    assert sha(FLUTTER) == '4eaa65d63731a5711d9ca05b35b493f4482ab4f5e31fa10c9057011a2deaa7cb'
    inputs = [*(WEATHER/name for name in ('weather_card_runtime.cpp', 'weather_card_runtime.h',
        'weather_card_control.h', 'weather_card_gateway.h', 'weather_card_arm64.S',
        'weather_card_binding.h', 'weather_card_guard.generated.h', 'weather_card_lifetime.h')),
        WEATHER/'weather_snapshot.h', HERE/'run.py', HERE/'runtime_test.cpp', DART, FLUTTER]
    before = {str(f): sha(f) for f in inputs}
    commands = []
    def command(cmd, prefix, timeout=90):
        cmd = list(map(str, cmd)); commands.append(cmd)
        run = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        prefix.with_suffix('.stdout').write_text(run.stdout)
        prefix.with_suffix('.stderr').write_text(run.stderr)
        return run
    with tempfile.TemporaryDirectory(prefix='hc-card-runtime-') as temp:
        temp = Path(temp)
        cross = command([NDK/'aarch64-linux-android35-clang', '-fPIC', '-shared', '-nostdlib', WEATHER/'weather_card_arm64.S', '-o', temp/'templates.so'], out/'arm-template-link')
        assert cross.returncode == 0, cross.stderr
        content = linked_templates(temp/'templates.so')
        approved_path = test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/production-gateway-v1/compiled-v2/report.json'
        approved = json.loads(approved_path.read_text())
        assert approved['passed']
        host_asm = ['.section .rodata', '.p2align 3']
        template_hashes = {}
        for kind in ['recompute', 'constructor']:
            symbol = 'hc_weather_card_' + kind + '_template'
            raw = content[kind]
            assert len(raw) == 296 and raw[288:] == bytes(8)
            inspected = next(row for row in approved['templates'] if row['name'] == symbol)
            assert raw[:284] == bytes.fromhex(inspected['code_hex'])
            blob = temp/(kind+'.bin'); blob.write_bytes(raw)
            template_hashes[kind] = sha(blob)
            host_asm += [f'.global {symbol}', f'{symbol}:', f'.incbin "{blob}",0,284', f'.global {symbol}_code_end', f'{symbol}_code_end:', f'.incbin "{blob}",284,4', f'.global {symbol}_lease_literal', f'{symbol}_lease_literal:', f'.incbin "{blob}",288,8', f'.global {symbol}_end', f'{symbol}_end:', '.p2align 3']
        host_asm += ['.section .note.GNU-stack,"",@progbits']
        asm = temp/'templates-host.S'; asm.write_text('\n'.join(host_asm)+'\n')
        def execute(directory, runtime=None):
            directory.mkdir(parents=True, exist_ok=True)
            exe = temp/(directory.name+'-test')
            cmd = ['g++', '-std=c++20', '-O1', '-g', '-Wall', '-Wextra', '-Werror', '-pthread', '-fsanitize=undefined', '-fno-sanitize-recover=all', '-I'+str(CPP), '-I'+str(WEATHER)]
            if runtime: cmd += ['-DHC_CARD_RUNTIME_SOURCE="'+str(runtime)+'"']
            cmd += [HERE/'runtime_test.cpp', asm, '-Wl,--wrap=mmap', '-Wl,--wrap=mprotect', '-Wl,--wrap=clock_gettime', '-o', exe]
            build = command(cmd, directory/'compile')
            result = dict(compile_exit=build.returncode)
            if build.returncode: return result
            run = command([exe, DART, FLUTTER], directory/'run')
            match = re.search(r'RESULT cases=(\d+) failed=(\d+)', run.stdout)
            result.update(exit_code=run.returncode, cases=int(match[1]) if match else None, failed=int(match[2]) if match else None,
                          failures=re.findall(r'^FAIL (.*)$', run.stderr, re.M), failed_cases=re.findall(r'^CASE_FAILED (.*?) status=', run.stderr, re.M))
            return result
        fixed = execute(out/'positive')
        mutations = [
            ('partial_install_survives', 'const bool restored = !attempted || rollback();', 'const bool restored = true; (void)attempted; (void)&rollback;', 'second site rollback'),
            ('renew_lease_from_receipt', 'return g_lease.publish(expected, snapshot, now());', 'snapshot.valid_until = now() + 4000000000LL; return g_lease.publish(expected, snapshot, now());', 'positive'),
            ('ignore_arena_bytes', 'return read_memory(arena, actual) && actual == g_templates;', 'return read_memory(arena, actual);', 'changed arena'),
            ('skip_live_image_guard', 'if (!image_state(3) || !arena_intact()', 'if (!arena_intact()', 'lost code'),
            ('ignore_image_readback', 'if (!image_state(mask) || !arena_intact() || !weather_card_lifetime::intact())', 'if (!arena_intact() || !weather_card_lifetime::intact())', 'foreign tail'),
            ('claim_permission_restored', 'return {changed, restored};', 'return {changed, true};', 'permission rollback'),
            ('clear_terminal_marker', 'g_lease.cancel();\n    // Retain Residue', 'g_lease.clear(g_lease.read());\n    // Retain Residue', 'terminal'),
            ('omit_second_site', 'for (unsigned index = 0; index < 2; ++index) {', 'for (unsigned index = 0; index < 1; ++index) {', 'positive'),
            ('omit_early_lifetime_gate', 'if (!weather_card_lifetime::ready()) return fail(Error::LifetimeRejected, false);', '/* mutation: omit early lifetime gate */', 'lifetime absent'),
            ('ignore_busy_lifetime_claim', 'if (!weather_card_lifetime::protect(dart_base, flutter_base, g_binding.epilogue))', 'if (false && !weather_card_lifetime::protect(dart_base, flutter_base, g_binding.epilogue))', 'positive'),
            ('ignore_lifetime_maintenance', 'if (!image_state(3) || !arena_intact() || !weather_card_lifetime::intact()', 'if (!image_state(3) || !arena_intact()', 'lifetime lost'),
        ]
        negative = []
        source = (WEATHER/'weather_card_runtime.cpp').read_text()
        if a.negative_controls and fixed.get('exit_code') == 0:
            for name, old, new, witness in mutations:
                assert source.count(old) == 1, name
                directory = out/'negative-controls'/name; directory.mkdir(parents=True)
                candidate = directory/'weather_card_runtime.cpp'; candidate.write_text(source.replace(old, new))
                row = execute(directory, candidate)
                row.update(name=name, expected_witness=witness, mutated_source_sha256=sha(candidate))
                row['detected'] = row['compile_exit'] == 0 and row.get('exit_code') == 1 and witness in row.get('failed_cases', [])
                negative.append(row)
        ndk = command([NDK/'aarch64-linux-android35-clang++', '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror', '-fPIC', '-fno-exceptions', '-fno-rtti', '-I'+str(CPP), '-c', WEATHER/'weather_card_runtime.cpp', '-o', temp/'runtime-android.o'], out/'ndk-runtime-compile')
    after = {str(f): sha(f) for f in inputs}
    passed = fixed.get('exit_code') == 0 and fixed.get('failed') == 0 and ndk.returncode == 0 and before == after and all(r['detected'] for r in negative)
    report = dict(passed=passed, fixed=fixed, negative_controls=negative, negative_controls_requested=a.negative_controls,
                  ndk_compile_exit=ndk.returncode, inputs_before=before, inputs_after=after, inputs_unchanged=before==after,
                  host_embedded_arm_template_sha256=template_hashes,
                  linked_template_reference=dict(path=str(approved_path), sha256=sha(approved_path), full_568_instruction_bytes_equal=True, linked_dynamic_relocations=0), commands=commands,
                  scope=[
                      'Complete production runtime, control, binding, generated guards and copy helper execute together. Actual pinned Dart and Flutter ELF PT_LOAD bytes are mapped with host RX/read protections.',
                      'Real mmap, mprotect, bounded raw proc-maps reads, process_vm_readv, aligned CAS, complete guard reads, arena readback, rollback and lease publication run. Fault wrappers deliberately change selected syscalls or config/clock observations.',
                      'Host embeds final NDK-linked ARM template bytes as data; both full instruction bodies match the independently reviewed/executed templates. It does not execute ARM instructions. Separate production-gateway ARM evidence covers their execution.',
                      'The new weather_card_lifetime API is an explicit refusal/takeover adapter in this component fixture. Early loader capture, single-thread admission, actual madvise GOT handling and reclaim exclusion require separate lifetime and joint evidence.',
                      'Caller/handle/TID/loader startup association, HWS Binder transport, real Dart VM/GC, Android seccomp/SELinux and phone installation are not exercised by this component test.',
                      'Component execution does not establish a correct loader/controller call site or a complete installation artifact; those require independent integration evidence.'
                  ], device_status='NOT_TESTED')
    (out/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(dict(passed=passed, fixed=fixed, negatives=len(negative), detected=sum(r['detected'] for r in negative), ndk_compile_exit=ndk.returncode)))
    return 0 if passed else 1

if __name__ == '__main__': raise SystemExit(main())
