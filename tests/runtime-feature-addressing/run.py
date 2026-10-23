#!/usr/bin/env python3
"""Exercise the production executable-only feature resolvers and binders."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image', type=Path, help='Optional complete launcher 7751 libapp.so')
args = parser.parse_args()
fixture = json.loads((HERE/'features-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-runtime-features-') as directory:
    folder = Path(directory)
    header = 'static Functions features_fixture(){return {\n'
    for row in fixture['functions']:
        header += '{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    header += '};}\nstatic const std::map<std::string,uint32_t> expected_features={\n'
    for key, value in fixture['roots'].items():
        header += '{'+json.dumps(key)+','+str(value)+'},\n'
    header += '};\n'
    (folder/'features_fixture.h').write_text(header)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(folder),
                    str(HERE/'features_test.cpp'),'-o',str(folder/'test')],check=True)
    subprocess.run([str(folder/'test')]+([str(args.image)] if args.image else []),check=True)

source = (ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
def body(name):
    start = source.index('bool '+name+'(')
    opening = source.index('{',start)
    level = 1
    at = opening+1
    while level:
        if source[at] == '{': level += 1
        if source[at] == '}': level -= 1
        at += 1
    return source[start:at]

for name in ('bind_icon_render','bind_desktop_icon_render','bind_progress_icon_render',
             'bind_folder_animation_scale','bind_folder_app_return_scale','bind_folder_icon_scale',
             'bind_animation_consumer','bind_magic_consumer','bind_widget_scale',
             'bind_drawer_title','bind_dock_title_model','sync_dock_titles'):
    implementation = body(name)
    assert 'ensure_runtime_features()' in implementation, name
    assert 'HomeTweaksFindSymbol' not in implementation, name
    assert 'dartscan::body' not in implementation, name
assert 'hometweaks::InLoaderCallback()' in body('ensure_runtime_features')
assert 'g_runtime_features_scanned=false;' in source[source.index('g_dart.reset();'):]
print('Production integration PASS: twelve binders use only runtime feature contracts; loader guard and image reset retained')
