#!/usr/bin/env python3
"""Validate grid/autofit discovery without symbol or section evidence."""
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
f=json.loads((HERE/'grid-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-grid-features-') as tmp:
    d=Path(tmp)
    h='static Functions grid_fixture(){return {\n'
    for row in f['functions']:
        h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\nstatic const std::map<std::string,uint32_t> grid_roots={\n'
    for k,v in f['roots'].items(): h+='{'+json.dumps(k)+','+str(v)+'},\n'
    h+='};\n'
    (d/'grid_fixture.h').write_text(h)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(d),str(HERE/'grid_test.cpp'),'-o',str(d/'test')],check=True)
    subprocess.run([str(d/'test')]+([str(args.image)] if args.image else []),check=True)
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
for name,next_name in [('bool bind_grid_autofit()','// Correlate the folder'),('void resolve_grid_fields(const char *)','home_layout::DropGeometryContract g_drop'),('void bind_drop_geometry()','size_t bind_knobs()')]:
    body=source[source.index(name):source.index(next_name,source.index(name))]
    assert 'ensure_runtime_features()' in body,name
    assert 'HomeTweaksFindSymbol' not in body and 'dartscan::' not in body,name
assert 'if(feature_geometry)' in source and 'feature.patches[index-2]' in source
print('Production grid integration PASS: autofit/field/drop binders and three default geometry knobs use runtime descriptors')
