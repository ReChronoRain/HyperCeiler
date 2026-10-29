#!/usr/bin/env python3
"""Run the production executable resolver and full-register predicate tests."""
import json
from pathlib import Path
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
for version in ('8583', '8584'):
    data = json.loads((HERE / f'{version}.json').read_text())
    with tempfile.TemporaryDirectory(prefix='hc-shortcuts-') as work:
        out = Path(work)
        fixture = 'static Functions fixture(){return {\n'
        for f in data['functions']:
            fixture += '{' + str(f['va']) + ',{' + ','.join(map(str, f['words'])) + '}},\n'
        fixture += '};}\nstatic constexpr uint32_t expected_entry=' + str(data['entry']) + ';\n'
        (out / 'fixture.h').write_text(fixture)
        subprocess.run(['c++', '-std=c++20', '-O1', '-g', '-Wall', '-Wextra', '-Werror',
                        '-fsanitize=address,undefined', '-fno-omit-frame-pointer',
                        '-I' + str(ROOT / 'app/src/main/cpp'), '-I' + work,
                        str(HERE / 'test.cpp'), '-o', str(out / 'test')], check=True)
        subprocess.run([str(out / 'test')], check=True)
