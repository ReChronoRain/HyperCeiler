#!/usr/bin/env python3
"""Build graph regressions using executable-only fixtures from three launchers."""
from pathlib import Path
import argparse,json,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--corpus',type=Path);args=parser.parse_args()
if args.corpus:args.corpus.mkdir(parents=True,exist_ok=True)
for version in ('7751','8583','8584'):
 data=json.loads((HERE/(version+'.json')).read_text())
 with tempfile.TemporaryDirectory(prefix='hc-clear-graph-')as tmp:
  out=Path(tmp);h='static Functions fixture(){return {\n'
  for f in data['functions']:h+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  h+='};}\n'
  for key in ('sync','init','forward','reverse'):h+=f'static constexpr uint32_t oracle_{key}={data[key]};\n'
  (out/'fixture.h').write_text(h)
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'test.cpp'),'-o',str(out/'test')],check=True)
  subprocess.run([str(out/'test')],check=True)
for version in ('7751','8583','8584'):
 data=json.loads((HERE/('disable-'+version+'.json')).read_text())
 with tempfile.TemporaryDirectory(prefix='hc-clear-disable-')as tmp:
  out=Path(tmp);h='static Functions fixture(){return {\n'
  for f in data['functions']:h+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  h+='};}\n'
  for key in ('overlay','fold','query'):h+=f'static constexpr uint32_t oracle_{key}={data[key]};\n'
  h+='static const std::vector<uint32_t> oracle_calls={'+','.join(map(str,data['calls']))+'};\n'
  (out/'fixture.h').write_text(h)
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'disable_test.cpp'),'-o',str(out/'test')],check=True)
  subprocess.run([str(out/'test')],check=True)

  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(ROOT/'tests/home-layout-native/stubs'),'-I'+str(out),str(HERE/'plan_test.cpp'),str(ROOT/'app/src/main/cpp/targets/home/tweaks/ht_plan.cpp'),'-o',str(out/'planner')],check=True)
  destination=args.corpus/(version+'.json') if args.corpus else out/'patched-fold.json'
  subprocess.run([str(out/'planner'),str(destination)],check=True)
  subprocess.run([str(out/'planner'),str(destination.with_stem(destination.stem+'-relocated')),'--relocated'],check=True)
