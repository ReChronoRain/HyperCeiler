#!/usr/bin/env python3
"""Exercise the shipping dismissal resolver and renderer on executable-only fixtures."""
import json,subprocess,tempfile
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
for version in ('8583','8584'):
 data=json.loads((HERE/f'{version}.json').read_text())
 with tempfile.TemporaryDirectory(prefix='hc-dismiss-') as work:
  path=Path(work);header='static Functions fixture(){return {\n'
  for f in data['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  (path/'fixture.h').write_text(header+'};}\n')
  subprocess.run(['c++','-std=c++20','-O1','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-fno-omit-frame-pointer','-I'+str(ROOT/'app/src/main/cpp'),'-I'+work,str(HERE/'test.cpp'),'-o',str(path/'test')],check=True)
  subprocess.run([str(path/'test')],check=True)
