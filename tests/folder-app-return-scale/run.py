#!/usr/bin/env python3
"""Production resolver against launcher 7751 instruction fixtures; C++20 required."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
fixture=json.loads((HERE/'launcher-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-folder-return-') as folder:
    out=Path(folder)
    header='constexpr uint32_t current='+str(fixture['current'])+';\n'
    for i,row in enumerate(fixture['functions']):
        header+='constexpr uint32_t va%d=%d; home_dynamic::Body body%d={%s};\n'%(i,row['va'],i,','.join(hex(w) for w in row['words']))
    (out/'fixture.h').write_text(header)
    subprocess.run(shlex.split(os.environ.get('CXX','clang++'))+['-std=c++20','-Wall','-Wextra',
        '-I'+str(ROOT/'app/src/main/cpp/targets/home'),'-I'+str(out),str(HERE/'test.cpp'),'-o',str(out/'test')],check=True)
    subprocess.run([str(out/'test')],check=True)
