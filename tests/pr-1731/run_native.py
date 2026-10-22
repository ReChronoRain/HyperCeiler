#!/usr/bin/env python3
"""Compile production folder guards and run focused layout arithmetic regressions."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
cxx=shlex.split(os.environ.get('CXX','clang++'))
with tempfile.TemporaryDirectory(prefix='hc-pr1731-native-') as folder:
    out=Path(folder)
    fixture=json.loads((here/'folder-7751.json').read_text())
    text='struct Fixture {uint32_t va;home_dynamic::Body words;};\nstd::map<std::string,Fixture> images={\n'
    for name,row in fixture.items():
        text+='{'+json.dumps(name)+',{'+str(row['va'])+',{'+','.join(hex(w) for w in row['words'])+'}}},\n'
    (out/'folder-fixture.inc').write_text(text+'};\n')
    sources=[here/'folder_contract_test.cpp']
    sources += [root/'tests/home-layout-native'/(name+'.cpp') for name in
                ['folder_geometry_test','folder_render_snapshot_test','drop_render_snapshot_test']]
    for source in sources:
        binary=out/source.stem
        subprocess.run(cxx+['-std=c++20','-Wall','-Wextra','-Werror','-O2',
            '-I'+str(root/'app/src/main/cpp'),'-I'+str(out),str(source),'-o',str(binary)],check=True)
        subprocess.run([str(binary)],check=True)
