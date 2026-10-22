#!/usr/bin/env python3
"""Compile shipping runtime contracts against original 7751 instructions."""
from pathlib import Path
import json, subprocess, tempfile, sys
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
data=json.loads((HERE/'launcher-7751.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-runtime-layout-') as folder:
    out=Path(folder)
    header='std::map<std::string,home_dynamic::Body> images={\n'
    for name,row in data.items():
        header+='{'+json.dumps(name)+',{'+','.join(hex(w) for w in row['words'])+'}},\n'
    header+='};\nstd::map<std::string,uint32_t> addresses={\n'
    for name,row in data.items():header+='{'+json.dumps(name)+','+str(row['va'])+'},\n'
    header+='};\n'
    (out/'fixture.h').write_text(header)
    source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
    start=source.index('extern "C" int hc_grid_autofit_body(uintptr_t saved, uintptr_t frame, unsigned kind) {')
    end=source.index('\n}\n',start)+3
    (out/'autofit-body.inc').write_text(source[start:end])
    start=source.index('bool capsule_wrapper_layout_compatible(')
    end=source.index('\n}\n',start)+3
    (out/'indicator-binder.inc').write_text(source[start:end])
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror',
        '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'test.cpp'),'-o',str(out/'test')],check=True)
    subprocess.run([str(out/'test')],check=True)
    if len(sys.argv)>1:
        subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror',
            '-I'+str(ROOT/'app/src/main/cpp'),str(HERE/'rust_animation_test.cpp'),'-o',str(out/'rust')],check=True)
        subprocess.run([str(out/'rust'),sys.argv[1]],check=True)
