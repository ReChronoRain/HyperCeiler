#!/usr/bin/env python3
"""Validate widget move discovery without symbol or section evidence."""
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
f=json.loads((HERE/'widget-move-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-widget-move-features-') as tmp:
    d=Path(tmp)
    h='static Functions widget_move_fixture(){return {\n'
    for row in f['functions']:
        h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\nstatic const std::map<std::string,uint32_t> widget_move_roots={\n'
    for k,v in f['roots'].items(): h+='{'+json.dumps(k)+','+str(v)+'},\n'
    h+='};\n'
    (d/'widget_move_fixture.h').write_text(h)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(d),str(HERE/'widget_move_test.cpp'),'-o',str(d/'test')],check=True)
    subprocess.run([str(d/'test')]+([str(args.image)] if args.image else []),check=True)
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
a=source.index('bool sync_widget_move(bool enabled)');b=source.index('bool bind_dock_title_model()',a);body=source[a:b]
assert 'ensure_runtime_features()' in body
assert 'HomeLayoutEngineFindSymbol' not in body and 'dartscan::' not in body
assert 'g_runtime_features.widget_move' in body
header=(ROOT/'app/src/main/cpp/targets/home/home_widget_move.h').read_text()
assert 'imm9(code[c+1])!=closure' in header and 'imm9(code[p])!=grid' in header
assert 'code[c+1] != 0xf85e83a2U' not in header and 'code[p] != 0xf85f03a0U' not in header
print('Production widget move integration PASS: executable-only roots, actual context/grid frame producers and unchanged source-word publication')
