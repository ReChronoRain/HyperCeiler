#!/usr/bin/env python3
"""Executable-only report predicate and register-contract regressions."""
from pathlib import Path
import json,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
for version in ('7751','8583','8584'):
 data=json.loads((HERE/(version+'.json')).read_text())
 with tempfile.TemporaryDirectory(prefix='hc-report-')as tmp:
  out=Path(tmp);h='static Functions fixture(){return {\n'
  for f in data['functions']:h+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  h+='};}\nstatic constexpr uint32_t oracle_entry='+str(data['entry'])+';\n'
  (out/'fixture.h').write_text(h)
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'test.cpp'),'-o',str(out/'test')],check=True)
  subprocess.run([str(out/'test')],check=True)
