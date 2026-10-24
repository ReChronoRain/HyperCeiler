#!/usr/bin/env python3
"""Test folder producer/consumer discovery with no symbol or section tables."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image', type=Path, help='Optional complete 7751 libapp.so, parsed without symbols')
args = parser.parse_args()
fixture = json.loads((ROOT/'tests/folder-icon-scale/launcher-7751.json').read_text())
fixture.update({k:v for k,v in json.loads((ROOT/'tests/runtime-feature-addressing/folder-allocators.json').read_text()).items() if isinstance(v,dict)})
constants = {'kSmall': 'small', 'kBase': 'base', 'kBig': 'big', 'kBigPreview': 'big_preview',
             'kWidth': 'helper_width', 'kHeight': 'helper_height'}
with tempfile.TemporaryDirectory(prefix='hc-folder-feature-') as temp:
    folder = Path(temp)
    header = '#pragma once\n'
    for name, key in constants.items():
        header += f'constexpr uint32_t {name}={fixture[key]["va"]};\n'
    header += f'constexpr uint32_t kCurrent={fixture["current"]};\n'
    header += 'static Functions folder_fixture(){return {\n'
    for value in fixture.values():
        if isinstance(value, dict):
            header += '{'+str(value['va'])+', {'+','.join(value['words'])+'}},\n'
    header += '};}\n'
    # The fixture is emitted after the Function aliases, with no production names
    # in its ELF. Tests use these original VA constants only as expected outputs.
    (folder/'folder_fixture.h').write_text(header.replace('Functions folder_fixture()', 'std::vector<home_layout::features::Function> folder_fixture()'))
    subprocess.run([shutil.which('c++'), '-std=c++20', '-O2', '-I'+str(ROOT/'app/src/main/cpp'),
                    '-I'+str(folder), str(Path(__file__).with_name('folder_test.cpp')),
                    '-o', str(folder/'test')], check=True)
    subprocess.run([str(folder/'test')]+([str(args.image)] if args.image else []), check=True)
