#!/usr/bin/env python3
"""Bounded full STT_FUNC extraction for original default placement research."""
import argparse,hashlib,json,subprocess,sys,struct,bisect
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
sys.path.insert(0,str(test_paths.SOURCE_ROOT / 'tests/hyos-entry'));from extract_runtime_guard import Elf
sys.path.insert(0,str(test_paths.SOURCE_ROOT / 'tests/home-layout-native'));from fixture_symbols import symbols
IMAGES={'dart':('libapp.so','a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'),'rust':('libapp_launcher.so','4dc9fa82371dda8a0a8b9eff48301469d2f84752b52b71635b84e5fc0c47fa6e')}
OBJ=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--kind',choices=IMAGES,required=True);ap.add_argument('--output',type=Path,required=True);ap.add_argument('va',nargs='+');a=ap.parse_args();a.output.mkdir(parents=True,exist_ok=False)
 name,digest=IMAGES[a.kind];p=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a'/name;e=Elf(p,digest);ss=symbols(p);by={va:(size,name)for va,size,name in ss};rows=[]
 inputs=[p,Path(__file__),OBJ,test_paths.SOURCE_ROOT / 'tests/hyos-entry/extract_runtime_guard.py',test_paths.SOURCE_ROOT / 'tests/home-layout-native/fixture_symbols.py'];before={str(p):sha(p)for p in inputs}
 for va in [int(x,0)for x in a.va]:
  boundary='fresh actual ELF embedded debug STT_FUNC'
  if va in by:size,name=by[va]
  else:
   assert a.kind=='dart'
   # Home7719 image is pinned above. Independently consume its actual table,
   # never infer a complete body from a return instruction or next named symbol.
   table=0x62d150;th=struct.unpack_from('<4I',e.data,table);assert th[1:]==(102289,89682,0)
   pcs=[0x866940+struct.unpack_from('<I',e.data,table+16+i*8)[0]for i in range(th[1])]
   assert all(x<=y for x,y in zip(pcs,pcs[1:]));ix=bisect.bisect_right(pcs,va)-1;assert pcs[ix]==va
   size=pcs[ix+1]-va;name='unnamed actual InstructionsTable entry';boundary='actual InstructionsTable consecutive entries'
  raw=e.read(va,size,True);text=subprocess.check_output([str(OBJ),'-D',f'--start-address={va}',f'--stop-address={va+size}',str(p)],text=True);annotated=[]
  import re
  for l in text.splitlines():
   m=re.search(r'\bbl?\s+0x([0-9a-f]+)',l)
   if m and int(m[1],16)in by:l+=' // '+by[int(m[1],16)][1]
   annotated.append(l)
  (a.output/f'{va:x}.asm.txt').write_text('\n'.join(annotated)+'\n');(a.output/f'{va:x}.bin').write_bytes(raw)
  rows.append({'va':hex(va),'size':size,'name':name,'sha256':hashlib.sha256(raw).hexdigest(),'assembly_sha256':sha(a.output/f'{va:x}.asm.txt'),'boundary':boundary})
 after={str(p):sha(p)for p in inputs};assert before==after
 (a.output/'report.json').write_text(json.dumps({'passed':True,'kind':'BOUNDED_DEFAULT_PLACEMENT_EXTRACTION','device_status':'NOT_TESTED','image':str(p),'image_sha256':digest,'functions':rows,'inputs_before':before,'inputs_after':after,'scope':'Full original bytes, not automatic semantic or device approval.'},indent=2)+'\n');print('functions',len(rows))
if __name__=='__main__':main()
