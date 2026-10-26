#!/usr/bin/env python3
"""Validate indicator policy and slide discovery without symbol or section evidence."""
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
f=json.loads((HERE/'indicator-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-indicator-features-') as tmp:
    d=Path(tmp)
    h='static Functions indicator_fixture(){return {\n'
    for row in f['functions']:
        h+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    h+='};}\nstatic const std::map<std::string,uint32_t> indicator_roots={\n'
    for k,v in f['roots'].items(): h+='{'+json.dumps(k)+','+str(v)+'},\n'
    h+='};\n'
    (d/'indicator_fixture.h').write_text(h)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(d),str(HERE/'indicator_test.cpp'),'-o',str(d/'test')],check=True)
    subprocess.run([str(d/'test')]+([str(args.image)] if args.image else []),check=True)
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
a=source.index('void bind_indicator_dot_target()');b=source.index('home_layout::GridFieldOffsets g_grid_field',a);body=source[a:b]
assert 'ensure_runtime_features()' in body
assert 'HomeLayoutEngineFindSymbol' not in body and 'dartscan::' not in body
assert 'hc_indicator_empty_pool=feature.empty_pool;' in body
knobs=source[source.index('size_t bind_knobs()'):source.index('// The page-dot companion')]
assert 'if(feature_margin)' in knobs
assert 'capsule_wrapper_layout_compatible(' not in knobs
assert 'if(feature_policy)' in knobs and 'if(feature_slide)' in knobs
assert 'indicator_policy_compatible(' not in knobs and 'indicator_slide_compatible(' not in knobs
print('Production indicator integration PASS: dot binder and all default indicator knobs use executable contracts including the anonymous owner')
