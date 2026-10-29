#!/usr/bin/env python3
"""Execute production flight radius discovery and pre-animation target routing."""
import json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
for version in ('8583','8584'):
 d=json.loads((HERE/f'flight-{version}.json').read_text())
 with tempfile.TemporaryDirectory(prefix='hc-flight-radius-')as work:
  out=Path(work);h='static Functions flight_fixture(){return {\n'
  for f in d['functions']:h+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  h+='};}\nstatic Plan flight_seed(){Plan p;'
  for key in ('radius_factory','border_factory','stack_fraction','width_selector'):h+='p.'+key+'='+str(d[key])+';'
  h+='return p;}\n';(out/'flight_fixture.h').write_text(h)
  subprocess.run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-fno-omit-frame-pointer','-I'+str(ROOT/'app/src/main/cpp'),'-I'+work,str(HERE/'flight_test.cpp'),'-o',str(out/'test')],check=True)
  subprocess.run([str(out/'test')],check=True)
