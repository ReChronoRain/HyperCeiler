#!/usr/bin/env python3
"""Validate layout scalar discovery without symbol or section evidence."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile
ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image',type=Path)
args=parser.parse_args()
f=json.loads((HERE/'scalars-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-scalar-features-') as tmp:
    d=Path(tmp)
    h='static Functions scalar_fixture(){return {\n'
    for row in f['functions']:
        h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\n'
    h+='static Functions scalar_scope_fixture(){return {\n'
    for row in f['scope_functions']:h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\nstatic const std::map<std::string,uint32_t> scalar_roots={\n'
    for k,v in f['roots'].items(): h+='{'+json.dumps(k)+','+str(v)+'},\n'
    h+='};\n'
    (d/'scalar_fixture.h').write_text(h)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(d),str(HERE/'scalar_test.cpp'),'-o',str(d/'test')],check=True)
    subprocess.run([str(d/'test')]+([str(args.image)] if args.image else []),check=True)
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
knobs=source[source.index('size_t bind_knobs()'):source.index('// The page-dot companion')]
assert 'if(feature_scalar)' in knobs
assert 'g_runtime_features.dock_margin:g_runtime_features.folder_row' in knobs
for name in ('GridController.screenMarginBottom','WidgetPositionUtil._getCellRectInHotSeatPad','FolderGridView._buildScrollableGrid'):
    assert name not in knobs,name
print('Production scalar integration PASS: both default scalar knobs use runtime feature descriptors')
