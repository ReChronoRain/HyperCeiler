#!/usr/bin/env python3
"""Symbol-free OS4 card-header discovery and renderer regression."""
import json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
for version in ('8583','8584'):
 data=json.loads((HERE/f'header-{version}.json').read_text())
 with tempfile.TemporaryDirectory(prefix='hc-recent-header-') as work:
  out=Path(work);header='static Functions header_fixture(){return {\n'
  for f in data['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  (out/'header_fixture.h').write_text(header+'};}\n')
  subprocess.run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-fno-omit-frame-pointer','-I'+str(ROOT/'app/src/main/cpp'),'-I'+work,str(HERE/'header_test.cpp'),'-o',str(out/'test')],check=True)
  subprocess.run([str(out/'test')],check=True)
