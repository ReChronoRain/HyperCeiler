#!/usr/bin/env python3
"""Execute production Weather runtime with bounded, explicit host adapters.

Each test forks; no Weather app, Android Binder service or Dart VM is emulated.
Negative controls compile private copies of one production translation unit.
"""
import argparse
import hashlib
import json
import re
import subprocess
import tempfile
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--output', required=True, type=Path)
p.add_argument('--negative-controls', action='store_true')
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=False)
b = Path(__file__).resolve().parent
root = b.parents[1]
cpp = root / 'app/src/main/cpp'
weather = cpp / 'targets/weather'
stubs = b.parent / 'hyos-control/stubs'
source = weather / 'weather_runtime.cpp'
# The lifetime implementation is explicitly substituted by lifetime_adapter.h.
# Its API header is consumed; its private generated guard is not included.
files = sorted(p for p in weather.glob('*.h') if p.name != 'weather_card_lifetime_guard.generated.h') + [source, b / 'runtime_test.cpp', b / 'card_adapter.h', b / 'lifetime_adapter.h', Path(__file__).resolve()] + sorted(stubs.rglob('*.h')) + sorted((cpp / 'nativehook').glob('*.h'))
sha = lambda f: hashlib.sha256(f.read_bytes()).hexdigest()
before = {str(f): sha(f) for f in files}
original = source.read_text()
mutations = [
    ('ignore_early_lifetime_residue', 'if (weather_card_lifetime::has_residue()) {', 'if (false && weather_card_lifetime::has_residue()) {', 'early lifetime residue refuses all features before worker'),
    ('accept_non48_parcel', '&& AParcel_getDataSize(output) == weather_snapshot::kByteCount', '&& AParcel_getDataSize(output) >= 0', 'query rejects size 0'),
    ('ignore_stopped_late_reply', 'if (!timely || g_stop.load(std::memory_order_acquire)) g_cache.revoke();', 'if (!timely) g_cache.revoke();', 'poll stopped rejects valid reply'),
    ('allow_foreign_caller', 'caller != g_binding.weather_base + 0x5fc76c || dart == 0', '(caller == UINTPTR_MAX) || dart == 0', 'foreign caller pass through once'),
    ('late_binding_publication', 'g_binding_ready.store(true, std::memory_order_release);', '/* negative control: publish only after GOT write */', 'binding available before atomic write'),
    ('ignore_restore_failure', 'written.changed && written.protected_again && !g_dart_rejected.load(std::memory_order_acquire)\n        && nhk::read_protection(page) == protection', 'written.changed && !g_dart_rejected.load(std::memory_order_acquire)', 'first restore failure rollback clean'),
    ('accept_nonunderscore_snapshot', 'reject_dart();\n        return;\n    }\n    const bool vm_data', 'return;\n    }\n    const bool vm_data', 'wrong first successful snapshot kDartVmSnapshotData'),
    ('accept_mixed_snapshot_image', 'if (base == 0 || base > UINTPTR_MAX - offset || address != base + offset)', 'if (base == 0 || base > UINTPTR_MAX - offset)', 'different image following snapshot _kDartVmSnapshotInstructions'),
    ('allow_snapshot_address_wrap', 'if (base == 0 || base > UINTPTR_MAX - offset || address != base + offset)', 'if (base == 0 || address != base + offset)', 'overflow following snapshot _kDartVmSnapshotInstructions'),
    ('overwrite_foreign_slot', 'const bool changed = __atomic_compare_exchange_n', 'expected = __atomic_load_n(reinterpret_cast<uintptr_t *>(g_binding.slot), __ATOMIC_ACQUIRE);\n    const bool changed = __atomic_compare_exchange_n', 'CAS foreign owner retained'),
    ('material_also_installs_level', 'const bool level_installed = level_selected && install();', 'const bool level_installed = (level_selected || material_selected) && install();', 'blur only leaves device level import untouched'),
    ('ignore_material_residue', 'if (weather_material_runtime::has_residue())\n        g_phase.store(Phase::Residue, std::memory_order_release);', 'if (weather_material_runtime::has_residue()) { /* negative: residue ignored */ }', 'material residue closes installed level batch'),
    ('require_level_dart_for_material', 'if (!weather_binding::verify_dart(base, read_memory)\n        && !weather_material_binding::verify_dart(base, read_memory))', 'if (!weather_binding::verify_dart(base, read_memory))', 'material Dart image independent from device level guards'),
    ('ignore_card_raw3', '(snapshot.card_type == 2 || snapshot.card_type == 3)', '(snapshot.card_type == 2)', 'card only raw 3'),
    ('reject_card_only_admission', '(!level_installed && !material_installed && !card_prepared)', '(!level_installed && !material_installed && (card_prepared || !card_prepared))', 'card only raw 2'),
    ('card_depends_on_level', 'const bool card_prepared = card_selected', 'const bool card_prepared = card_selected && level_installed', 'card only raw 2'),
    ('omit_card_cancel_on_dart_rejection', 'g_dart_rejected.store(true, std::memory_order_release);\n    weather_card_runtime::cancel();', 'g_dart_rejected.store(true, std::memory_order_release);\n    /* negative: card remains prepared */', 'wrong first Dart cancels prepared card kDartVmSnapshotData'),
    ('ignore_card_residue', 'if (weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue)', 'if (false && weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue)', 'card residue closes all shared callbacks'),
    ('omit_card_service', 'weather_card_runtime::service();', '/* negative: no card maintenance after HWS poll */', 'shared poll services card after current publication'),
    ('omit_card_window_cancellation', 'void on_card_snapshot_window_missed(uint32_t) noexcept {\n    weather_card_runtime::cancel();', 'void on_card_snapshot_window_missed(uint32_t) noexcept {\n    /* negative: missed first-use window ignored */', 'card window missed cannot late install'),
]


def execute(directory, temporary, candidate=None):
    directory.mkdir(parents=True, exist_ok=True)
    exe = temporary / (directory.name + '-runtime-test')
    cmd = ['g++', '-std=c++20', '-O1', '-g', '-Wall', '-Wextra', '-Werror', '-pthread', '-fno-omit-frame-pointer', '-fsanitize=undefined', '-fno-sanitize-recover=all', '-I' + str(stubs), '-I' + str(cpp), '-I' + str(weather)]
    if candidate is not None:
        cmd += ['-DHC_RUNTIME_SOURCE="' + str(candidate) + '"']
    cmd += [str(b / 'runtime_test.cpp'), '-o', str(exe)]
    built = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    (directory / 'compile.stdout').write_text(built.stdout)
    (directory / 'compile.stderr').write_text(built.stderr)
    record = {'command': cmd, 'compile_exit': built.returncode}
    if built.returncode:
        return record
    ran = subprocess.run([str(exe)], capture_output=True, text=True, timeout=45)
    (directory / 'run.stdout').write_text(ran.stdout)
    (directory / 'run.stderr').write_text(ran.stderr)
    counts = re.search(r'RESULT cases=(\d+) failed=(\d+)', ran.stdout)
    record.update(exit_code=ran.returncode, cases=int(counts[1]) if counts else None, failed=int(counts[2]) if counts else None, failures=re.findall(r'^FAIL (.*)$', ran.stderr, re.M))
    return record


with tempfile.TemporaryDirectory(prefix='hc-weather-runtime-') as t:
    temp = Path(t)
    fixed = execute(a.output, temp)
    negatives = []
    if a.negative_controls:
        for name, old, new, witness in mutations:
            if original.count(old) != 1:
                raise RuntimeError('Mutation anchor not unique: ' + name)
            modified = original.replace(old, new)
            if name == 'late_binding_publication':
                marker = 'const auto written = exchange_slot(original, replacement, protection);'
                if modified.count(marker) != 1:
                    raise RuntimeError('Late-publication placement anchor not unique')
                modified = modified.replace(marker, marker + '\n    g_binding_ready.store(true, std::memory_order_release);')
            directory = a.output / 'negative-controls' / name
            directory.mkdir(parents=True)
            candidate = directory.resolve() / 'weather_runtime.cpp'
            candidate.write_text(modified)
            item = execute(directory, temp, candidate)
            item.update(name=name, expected_witness=witness, source_sha256=sha(candidate))
            item['detected'] = item['compile_exit'] == 0 and item.get('exit_code') == 1 and item.get('failed', 0) > 0 and any(f.startswith(witness + ':') for f in item.get('failures', []))
            negatives.append(item)
    after = {str(f): sha(f) for f in files}
    same = before == after
    result = {
        'passed': fixed['compile_exit'] == 0 and fixed.get('exit_code') == 0 and fixed.get('failed') == 0 and fixed.get('cases') == 121 and same and all(n['detected'] for n in negatives),
        'expected_cases': 121, 'preserved_prior_cases': 117, 'retained_original_controller_cases': 92, 'retained_card_routing_cases': 25, 'new_lifetime_admission_cases': 4, 'fixed': fixed, 'negative_controls': negatives, 'negative_controls_requested': a.negative_controls,
        'inputs': before, 'inputs_unchanged': same,
        'boundary': [
            'Actual production weather_runtime.cpp included directly; actual weather_snapshot Cache/decode and weather_device_level policy execute.',
            'Binder transport, BOOTTIME, native binding resolver and pthread/nanosleep scheduling use explicit adapters. Real worker pthreads, condition-variable startup wait, process_vm_readv, anonymous mmap, atomic pointer CAS and default mprotect/proc-maps permission checks execute.',
            'Card runtime is an explicit routing adapter: prepare/install/service/cancel record controller calls and configured outcomes, with no template/ARM/Dart code execution or lease publication. Actual card runtime is independently tested; this is NOT three actual runtimes together.',
            'Card lifetime has_residue is an explicit boolean adapter. New cases prove the actual controller refuses before any worker/Binder/GOT/card work and stays terminal; they do not test lifetime installation, proc-task enumeration or page reclamation protection.',
            'Material component installation and its image predicates are adapters in this shared-controller fixture. Feature selection, partial success, residual failure and supplied callback lifecycle execute in the actual controller; the material GOT wrapper itself is tested separately by weather-material-runtime.',
            'Pinned image guards are NOT proved by the resolver adapter; separately tested by weather-binding. Host x86 callsite wrapper verifies the actual builtin-return-address condition; it is not AArch64/Dart ABI evidence.',
            'Each case runs in a fresh fork; process exit ends detached workers. A blocked production Binder worker is not cancellable by this test or the runtime, but no replacement worker or late installer is started.',
            'Private mutations must compile, complete and fail the named semantic witness; compilation failure and timeout do not count as successful negative controls.',
            'This is not an Android Weather lifecycle, Binder service, Android linker, Dart VM or phone test.'
        ], 'device_status': 'NOT_TESTED'
    }
    (a.output / 'report.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'passed': result['passed'], 'fixed': {k:v for k,v in fixed.items() if k != 'command'}, 'negative_controls': [{k:v for k,v in n.items() if k in ('name','detected','failed','compile_exit')} for n in negatives], 'inputs_unchanged': same}, indent=2))
    raise SystemExit(0 if result['passed'] else 1)
