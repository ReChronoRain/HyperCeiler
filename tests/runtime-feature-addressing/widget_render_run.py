#!/usr/bin/env python3
"""Verify both captured widget creation graphs, optionally their full images."""
import argparse,json,subprocess,tempfile,hashlib,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image7751',type=Path);parser.add_argument('--image8583',type=Path);args=parser.parse_args()
for version in ('7751','8583'):
 fixture=json.loads((HERE/f'widget-render-{version}.json').read_text());source=getattr(args,'image'+version)
 if source:
  raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()==fixture['source_sha256']
  off=struct.unpack_from('<Q',raw,32)[0];phsize,n=struct.unpack_from('<HH',raw,54)
  loads=[struct.unpack_from('<IIQQQQQQ',raw,off+i*phsize)for i in range(n)]
  for row in fixture['functions']:
   va=row['va'];size=len(row['words'])*4
   p=next(p for p in loads if p[0]==1 and p[3]<=va and va+size<=p[3]+p[5])
   assert list(struct.unpack_from('<'+'I'*len(row['words']),raw,p[2]+va-p[3]))==row['words']
 with tempfile.TemporaryDirectory(prefix='hc-widget-render-') as folder:
  folder=Path(folder);header='static Functions widget_render_fixture(){return {\n'
  for row in fixture['functions']:header+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
  header+='};}\nstatic const std::map<std::string,uint32_t> widget_render_roots={\n'
  for key,value in fixture['roots'].items():header+='{'+json.dumps(key)+','+str(value)+'},\n'
  (folder/'widget_render_fixture.h').write_text(header+'};\n')
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(folder),str(HERE/'widget_render_test.cpp'),'-o',str(folder/'test')],check=True)
  subprocess.run([str(folder/'test')]+([str(source)]if source else[]),check=True)
