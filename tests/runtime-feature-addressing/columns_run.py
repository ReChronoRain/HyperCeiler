#!/usr/bin/env python3
"""Validate columns and persistence discovery without symbol or section evidence."""
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
f=json.loads((HERE/'columns-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-columns-features-') as tmp:
    d=Path(tmp)
    h='static Functions columns_fixture(){return {\n'
    for row in f['functions']:
        h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\nstatic const std::map<std::string,uint32_t> columns_roots={\n'
    for k,v in f['roots'].items(): h+='{'+json.dumps(k)+','+str(v)+'},\n'
    h+='};\n'
    (d/'columns_fixture.h').write_text(h)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(d),str(HERE/'columns_test.cpp'),'-o',str(d/'test')],check=True)
    subprocess.run([str(d/'test')]+([str(args.image)] if args.image else []),check=True)
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
a=source.index('bool bind_dart_columns()');b=source.index('// BOTH fixed-row',a);body=source[a:b]
assert 'ensure_runtime_features()' in body
assert 'HomeLayoutEngineFindSymbol' not in body and 'dartscan::' not in body
assert 'policy.resize' not in body and 'save_body.resize' not in body
assert 'hc_layout_save_field=feature.save_field;' in body
assert 'hc_dart_columns_frame=shape.frame;' in body
print('Production column integration PASS: argument/persistence/save targets and field descriptors from executable relationships only')
