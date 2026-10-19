#!/usr/bin/env python3
"""Compile the real native decoder/cache and consume real Java-writer fixture bytes."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', type=Path, required=True)
p.add_argument('--java-corpus', type=Path, required=True)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=False)
base = Path(__file__).resolve().parent
source = base.parents[1]
checks = []

with tempfile.TemporaryDirectory(prefix='hc-hcs1-') as temp:
    binary = Path(temp) / 'snapshot-test'
    cmd = ['g++', '-std=c++20', '-O2', '-Wall', '-Wextra', '-Werror', '-pthread',
           '-I' + str(source / 'app/src/main/cpp'), str(base / 'snapshot_test.cpp'), '-o', str(binary)]
    build = subprocess.run(cmd, capture_output=True, text=True)
    (a.output / 'build.txt').write_text(build.stdout + build.stderr)
    if build.returncode:
        print(build.stdout + build.stderr)
        raise SystemExit(build.returncode)
    run = subprocess.run([binary], capture_output=True, text=True, timeout=60)
    (a.output / 'tests.txt').write_text(run.stdout + run.stderr)
    print(run.stdout, end='')
    checks.append({'name': 'actual production decoder/cache regressions', 'passed': run.returncode == 0})
    corpus = []
    for expected_path in sorted(a.java_corpus.glob('*.json')):
        expected = json.loads(expected_path.read_text())
        path = expected_path.with_suffix('.bin')
        result = subprocess.run([binary, path, str(expected['observed_at_boottime_nanos'])],
                                capture_output=True, text=True, check=True)
        observed = json.loads(result.stdout)
        enabled = expected['available'] == 1 and expected['flags'] != 0
        expected_valid = not enabled or (0 < expected['deadline'] - expected['observed_at_boottime_nanos'] <= 5_000_000_000)
        valid = observed['valid'] == expected_valid
        if expected_valid:
            valid = valid and all(observed[key] == expected[key]
                                  for key in ['available', 'flags', 'epoch', 'revision', 'deadline'])
            if enabled:
                valid = valid and observed['grid'] == [expected['grid_enabled'], expected['cell_x'], expected['cell_y']]
                valid = valid and observed['knobs'] == [[e, d if e else 0] for e, d in zip(expected['knob_enabled'], expected['knob_delta_dp'])]
                valid = valid and observed['tweaks'] == expected['tweaks']
            else:
                valid = valid and observed['grid'] == [0, 4, 6] and observed['knobs'] == [[0, 0]] * 8
        checks.append({'name': 'Java production writer to native decoder: ' + path.stem, 'passed': valid})
        corpus.append({'path': str(path.resolve()), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                       'expected_json_sha256': hashlib.sha256(expected_path.read_bytes()).hexdigest(),
                       'observed': observed, 'expected_valid': expected_valid})
    checks.append({'name': 'six Java writer corpus cases present', 'passed': len(corpus) == 6})
    files = [base / 'snapshot_test.cpp', Path(__file__).resolve(),
             source / 'app/src/main/cpp/targets/hyos/hyos_snapshot.h',
             source / 'app/src/main/cpp/targets/home/home_layout_config.h',
             source / 'app/src/main/cpp/targets/home/home_layout_knobs.h']
    report = {'passed': all(item['passed'] for item in checks), 'checks': checks,
              'compiler_command': cmd, 'corpus': corpus,
              'files': {str(f): hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
              'boundary': 'Actual C++ decoder/cache with real std::thread concurrency and supplied BOOTTIME values; Java corpus comes from production writer through a host Parcel adapter. No Android Parcel/Binder integration, loader execution or device proof.'}
    (a.output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'passed': report['passed'], 'checks': checks}, indent=2))
    raise SystemExit(0 if report['passed'] else 1)
