#!/usr/bin/env python3
"""Run actual Hyos entry callbacks against a controlled runtime ABI and real fork."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile

p = argparse.ArgumentParser()
p.add_argument('--sdk', type=Path, required=True, help='Pinned external Zygisk Next API 4 SDK directory')
p.add_argument('--output', type=Path, help='New evidence directory; omitted for the original console runner')
args = p.parse_args()
base = Path(__file__).resolve().parent
root = base.parents[1]
if args.output:
    args.output.mkdir(parents=True, exist_ok=False)
inputs = [Path(__file__).resolve(), base / 'entry_test.cpp', args.sdk.resolve() / 'zygisk_next_api.h']
inputs += sorted((base / 'stubs').rglob('*.[hH]'))
inputs += [root / 'app/src/main/cpp' / path for path in (
    'targets/home/recents_runtime.h', 'targets/hyos/hyos_snapshot.h',
    'targets/hyos/hyos_entry.cpp', 'targets/hyos/hyos_entry.h', 'targets/hyos/hyos_control.h',
    'targets/hyos/hyos_runtime_guard.h', 'targets/hyos/hyos_snapshot_guard.h',
    'targets/hyos/hyos_startup_reason.h', 'targets/weather/weather_binding.h',
    'targets/weather/weather_guard.generated.h', 'targets/weather/weather_device_level.h',
    'targets/weather/weather_snapshot.h', 'targets/weather/weather_card_lifetime.h', 'targets/weather/weather_runtime.h',
    'nativehook/memory_io.h', 'nativehook/nhk_base.h')]
hashes = {str(path.resolve()): hashlib.sha256(path.read_bytes()).hexdigest() for path in inputs}
with tempfile.TemporaryDirectory(prefix='hc-hyos-entry-') as tmp:
    binary = Path(tmp) / 'test'
    command = ['g++', '-std=c++20', '-Wall', '-Wextra', '-Werror', '-pthread',
                    '-I' + str(args.sdk.resolve()), '-I' + str(base / 'stubs'),
                    '-I' + str(base.parents[1] / 'app/src/main/cpp'),
                    str(base / 'entry_test.cpp'), '-o', str(binary)]
    build = subprocess.run(command, capture_output=True, text=True)
    if args.output: (args.output / 'build.txt').write_text(build.stdout + build.stderr)
    if build.returncode: raise RuntimeError(build.stderr)
    result = subprocess.run([str(binary)], capture_output=True, text=True)
    print(result.stdout, end='')
    if args.output:
        (args.output / 'tests.txt').write_text(result.stdout + result.stderr)
        match = re.search(r'(\d+)/(\d+) PASS; production entry', result.stdout)
        unchanged = all(hashlib.sha256(Path(p).read_bytes()).hexdigest() == h for p, h in hashes.items())
        report = {'passed': result.returncode == 0 and match is not None and unchanged,
                  'tests': int(match[1]) if match else 0, 'expected': int(match[2]) if match else 0,
                  'returncode': result.returncode, 'inputs': hashes, 'inputs_unchanged': unchanged,
                  'command': command, 'binary_sha256': hashlib.sha256(binary.read_bytes()).hexdigest(),
                  'limits': 'Actual production loader callbacks, live readv guards and real fork; loader SDK, Home/Weather controller entries and snapshot observers are fixtures. No Android loader or phone injection.'}
        (args.output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if result.returncode: raise RuntimeError(result.stderr)
