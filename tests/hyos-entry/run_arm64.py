#!/usr/bin/env python3
"""Execute the production AAPCS/Rust entry wrapper with a clobbering helper."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
out = a.output.resolve()
out.mkdir(parents=True, exist_ok=False)
here = Path(__file__).resolve().parent
production = here.parents[1] / 'app/src/main/cpp/targets/hyos/hyos_app_entry_arm64.S'
runtime = test_paths.BUILD_CACHE / 'arm64-emulation/root'
env = dict(os.environ, LD_LIBRARY_PATH=str(runtime / 'usr/lib/x86_64-linux-gnu'))
compiler = runtime / 'usr/bin/aarch64-linux-gnu-g++-13'
emulator = runtime / 'usr/bin/qemu-aarch64-static'
checks = []
def run(label, command, expect=0):
    r = subprocess.run(list(map(str, command)), env=env, capture_output=True, text=True)
    (out / (label + '.txt')).write_text(r.stdout + r.stderr)
    passed = r.returncode == 0 if expect == 0 else r.returncode != 0
    checks.append(dict(name=label, passed=passed, returncode=r.returncode))
    if not passed:
        raise RuntimeError(label + ' failed: ' + r.stdout + r.stderr)
    return r
def build(label, asm):
    binary = out / label
    run(label + '-build', [compiler, '--sysroot=' + str(runtime), '-std=c++20', '-O2',
        '-Wall', '-Wextra', '-Werror', '-static', here / 'arm64_test.cpp',
        here / 'arm64_probe.S', asm, '-o', binary])
    return binary
binary = build('wrapper', production)
r = run('wrapper-execute', [emulator, binary])
print(r.stdout)
# Safe negative controls corrupt one register, so the fixture detects the ABI
# break with an assertion rather than accidentally writing through a bad x8.
for name, old, new in [
    ('lose-argument', 'ldp x2, x3, [sp, #16]', 'mov x2, #0\n    ldr x3, [sp, #24]'),
    ('lose-vector', 'ldp q0, q1, [sp, #160]', 'movi v0.16b, #0\n    ldr q1, [sp, #176]'),
    ('lose-flags', 'msr nzcv, x16', 'msr nzcv, xzr')]:
    mutated = out / (name + '.S')
    original = production.read_text()
    assert original.count(old) == 1
    mutated.write_text(original.replace(old, new))
    b = build(name, mutated)
    run(name + '-execute', [emulator, b], expect=1)
report = dict(passed=all(c['passed'] for c in checks), checks=checks,
    production_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
    arm64_executions=32, negative_controls_detected=3, device_status='NOT_TESTED',
    limits=['QEMU user-mode executes the actual assembly with a fixture helper and fixture Rust-shaped callee.',
            'No Hyos loader, Zygisk Next injection, Rust runtime or physical device is executed.'])
(out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
