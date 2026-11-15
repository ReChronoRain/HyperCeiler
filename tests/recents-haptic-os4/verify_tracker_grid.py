"""Portable tracker-grid contracts and both legacy resolver/observation fixtures."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
for version in ('8583','8584','tracker-grid-9239','tracker-shared-9239'):
    data=json.loads((here/f'{version}.json').read_text(encoding='utf8'))
    with tempfile.TemporaryDirectory(prefix='hc-haptic-grid-') as work:
        out=Path(work)
        fixture='static Functions fixture(){return {\n'
        for f in data['functions']:
            fixture+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
        (out/'fixture.h').write_text(fixture+'};}\n',encoding='utf8')
        source='tracker_grid_test.cpp' if version.startswith('tracker-') else 'test.cpp'
        defines=['-DHC_SHARED_LIST'] if version.startswith('tracker-shared-') else []
        binary=out/'test.exe'
        subprocess.run([*shlex.split(os.environ.get('CXX','c++')),*defines,'-std=c++20','-O2','-UNDEBUG',
            '-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+work,
            str(here/source),'-o',str(binary)],check=True)
        subprocess.run([str(binary)],check=True)
