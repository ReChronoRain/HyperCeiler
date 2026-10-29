#!/usr/bin/env python3
"""Exercise the production executable-only closed-folder landing resolver."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image', action='append', type=Path, default=[],
                    help='Optional complete launcher libapp.so; may be repeated')
args = parser.parse_args()
fixtures = [json.loads(p.read_text()) for p in sorted(Path(__file__).parent.glob('launcher-*.json'))]
with tempfile.TemporaryDirectory(prefix='hc-folder-preview-return-') as temp:
    temp = Path(temp)
    header = 'static std::vector<Fixture> fixtures(){return {\n'
    for fixture in fixtures:
        functions = fixture['functions']
        header += '{"' + fixture['version'] + '",' + str(fixture['current']) + ',{\n'
        for key, function in functions.items():
            header += '{' + str(function['va']) + ',{' + ','.join(function['words']) + '}},\n'
        header += '},{'
        header += ','.join(str(functions[k]['va']) for k in
                           ('producer', 'getter', 'accessor', 'origin', 'wrapper', 'consumer', 'landing', 'item_allocator', 'rect_allocator'))
        header += '}},\n'
    header += '};}\n'
    (temp/'fixtures.h').write_text(header)
    subprocess.run([shutil.which('c++'), '-std=c++20', '-O2', '-Wall', '-Wextra',
                    '-I'+str(ROOT/'app/src/main/cpp'),
                    '-I'+str(ROOT/'tests/runtime-feature-addressing'), '-I'+str(temp),
                    str(Path(__file__).with_name('test.cpp')), '-o', str(temp/'test')], check=True)
    subprocess.run([str(temp/'test'), *map(str, args.image)], check=True)
