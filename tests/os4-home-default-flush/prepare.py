#!/usr/bin/env python3
"""Fresh Home7719 snapshot roots and exact consumed pool/dispatch bindings.
Format reference is official Dart3.10.1; acceptance additionally requires exact
vendor image and parse ending at the actual vendor snapshot declared end.
"""
import argparse,contextlib,hashlib,json,struct,sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
import snapshot_parser as parser

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);a=ap.parse_args();a.output.mkdir(parents=True,exist_ok=False)
 rom=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so';src=test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/snapshot-parser';inputs=[rom,Path(__file__),Path(parser.__file__),parser.BASE/'official-dart-3.10.1/runtime_vm_class_id.h',parser.BASE/'official-dart-3.10.1/runtime_vm_object_store.h']
 before={str(p):sha(p)for p in inputs};assert before[str(rom)]=='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992';b=rom.read_bytes()
 with (a.output/'parser.log').open('w')as log,contextlib.redirect_stdout(log):
  allocation=parser.alloc(b,0x4ac0);objects,spans=parser.first_fills(b,allocation,len(allocation['clusters']));roots=parser.roots(b,allocation,spans)
 assert roots['end']==0x4ac0+struct.unpack_from('<Q',b,0x4ac4)[0]+4==0x62d0d8
 assert roots['dispatch_length']==181460 and roots['dispatch_first_code']==159411
 tab=0x62d150;assert struct.unpack_from('<4I',b,tab)[1:]==(102289,89682,0)
 pcs=[0x866940+struct.unpack_from('<I',b,tab+16+j*8)[0]for j in range(102289)];assert all(x<=y for x,y in zip(pcs,pcs[1:]));pool=objects[256653]['entries'];bindings=[]
 for off,expected in [(0x2e210,0x99a79c),(0x2e298,0x8e0b24),(0x2e2a0,0x8f6f24),(0x2e3a8,0xaef838)]:
  ref=pool[(off-16)//8]['value'];f=objects[ref];assert f['name']=='Function';index=f['codeindex']-1;va=pcs[index];assert va==expected;bindings.append({'pool_offset':hex(off),'object_ref':ref,'function_code_index':index+1,'entry':hex(va),'extent':pcs[index+1]-va})
 dispatch=[]
 for cid,expected in [(2021,0x1852c34),(2030,0x18525f8)]:
  index=4096+cid-0xfbb;code=roots['dispatch'][index];va=pcs[code-1];assert va==expected
  clazz=next(v for v in objects.values()if v.get('classid')==cid)
  dispatch.append({'cid':cid,'origin':4096,'callsite':'0x99a7b8','selector_delta':-0xfbb,'array_index':index,'code_index':code,'entry':hex(va),'extent':pcs[code]-va,'actual_class_metadata':clazz})
 keys=[]
 for off,expected in [(0x16a00,'mamlConfigSpanX'),(0x16a08,'mamlConfigSpanY')]:
  ref=pool[(off-16)//8]['value'];text=objects[ref]['text'];assert text==expected;keys.append({'pool_offset':hex(off),'ref':ref,'text':text})
 after={str(p):sha(p)for p in inputs};assert before==after
 report={'passed':True,'device_status':'NOT_TESTED','inputs_before':before,'inputs_after':after,'snapshot_start':'0x4ac0','declared_and_parsed_end':hex(roots['end']),'allocation_clusters':len(allocation['clusters']),'parsed_objects':len(objects),'dispatch_length':roots['dispatch_length'],'bindings':bindings,'copy_dispatch':dispatch,'bundle_keys':keys,'limits':['Format reference does not prove a vendor VM ABI universally. Exact Home image, complete parser termination, consumed typed CIDs and target bodies are pinned here.','Only selected dispatch/pool entries are used; parsing other entries is not semantic approval.','Generic collection/scheduler adapters in the execution fixture do not instantiate a Dart VM.']}
 (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'passed':True,'pool_closures':len(bindings),'copy_targets':len(dispatch),'parser_end':hex(roots['end'])}))
if __name__=='__main__':main()
