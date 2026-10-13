#!/usr/bin/env python3
"""Execute full original Dart grid arithmetic with declared allocation/log/pixel adapters."""
import argparse,hashlib,json,os,resource,struct,subprocess,sys
from pathlib import Path
from pixel_provenance import prove,ENGINE

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
W=Path(__file__).resolve().parent;R=test_paths.TEST_DATA_ROOT
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so';PIN='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
FUNCTIONS=[(0x152228c,1244,'PhoneCellSizeHandler.calGridSizeByVariable'),(0x15228e8,640,'PhoneCellSizeHandler.calGridSizeByFixedRows'),(0x1520f40,2392,'PhoneCellSizeHandler.calVariableValues'),(0x1521898,276,'PhoneCellSizeHandler.correctWorkspaceCellSide'),(0xa4418c,560,'PixelPerfect|alignPixel')]
def extract(out):
 data=ROM.read_bytes();phoff=struct.unpack_from('<Q',data,32)[0];phsize,phnum=struct.unpack_from('<HH',data,54)
 def read(va,n):
  for ix in range(phnum):
   kind,flags,off,base,_,filesize,_,_=struct.unpack_from('<IIQQQQQQ',data,phoff+ix*phsize)
   if kind==1 and flags&1 and base<=va and va+n<=base+filesize:return data[off+va-base:off+va-base+n]
  raise AssertionError('range outside file-backed RX segment')
 catalog=json.loads((test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/dart-v5/extraction.json').read_text())['symbols'];result=[]
 for va,size,name in FUNCTIONS:
  assert catalog[hex(va)]=={'size':size,'name':name}
  raw=read(va,size);(out/f'{va:x}.bin').write_bytes(raw)
  result.append({'va':hex(va),'size':size,'name':name,'sha256':hashlib.sha256(raw).hexdigest(),'extent':'complete original function','execution':'selected normal branches; VM slow/GC paths not exercised'})
 for va,expected in [(0x15224ac,0xcb040005),(0x1521488,0xf847b004),(0x1521478,0xfc40b003),(0x15229f4,0x4ee0f422),(0xa442e0,0xfc413260),(0xa44340,0xf943ab50)]:assert int.from_bytes(read(va,4),'little')==expected
 pool=json.loads((test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/snapshot-parser/pool-scanned.json').read_text())['entries'];constants=[]
 for off,bits in [(0x27708,4607569728367971271),(0x27710,4596373779694328216),(0x27718,4590861373750426736)]:
  entry=pool[(off-16)//8];assert entry['bits']==16 and entry['value']==bits
  constants.append({'offset':hex(off),'bits':bits,'double':struct.unpack('<d',struct.pack('<Q',bits))[0]})
 objects=json.loads((test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/snapshot-parser/objects.json').read_text());layouts=[]
 for off,expected,name in [(0x15648,1,'fourColumnsMaxRowsLayout'),(0x15650,0,'fourColumnsMaxRowsMinusOneLayout'),(0x15658,2,'fiveColumnsLayout')]:
  item=pool[(off-16)//8];obj=objects[str(item['value'])];refs=obj['refs']
  assert obj['cid']==10233 and objects[str(refs[2])]['text']==name
  assert refs[3]['wordparts']==[expected,0] and refs[4]['wordparts']==[0,0]
  layouts.append({'pool_offset':hex(off),'object_ref':item['value'],'name':name,'tagged_raw_field_0x13':expected,'snapshot_refs':refs})
 return {'layout_identity_fields':layouts,'functions':result,'pool_constants':constants,'tested_body_adapter_overlap':False,'mutations_have_exact_original_instruction_guards':True}
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 resource.setrlimit(resource.RLIMIT_CORE,(0,0));assert sha(ROM)==PIN
 sources=[Path(__file__).resolve(),W/'spacing_v3_fixture.cpp',W/'spacing_v3_probe.S',W/'pixel_provenance.py',ENGINE,test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/dart-v5/extraction.json',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/snapshot-parser/objects.json',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/snapshot-parser/pool-scanned.json',test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/home/home_startup.cpp',test_paths.SOURCE_ROOT / 'library/core/src/main/res/xml/home_layout.xml']
 inputs={str(test_paths.report_path(x, R)):sha(x)for x in sources};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':inputs,'rom':{'path':str(test_paths.report_path(ROM, R)),'sha256':PIN},'runs':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 report['provenance']=extract(out);report['pixel_native_binding']=prove();save()
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=60);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append({'label':label,'command':cmd,'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':okay});save();assert okay,(label,r.returncode,r.stderr);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'spacing_v3_fixture.cpp',W/'spacing_v3_probe.S','-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==2527 and report['positive']['checks']>0 and report['positive']['passed'] is True
 for name,witness in {'lost-tail-row':'final copyWith receives original handler row as Smi','lost-tail-cell':'final copyWith receives calculated cell side','skip-shrink':'fixed policy retains OEM min width-height shrink through tail','ignore-dpr':'fixed policy retains OEM min width-height shrink through tail','truncate-as-round':'fixed policy retains OEM min width-height shrink through tail'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=report['inputs_after']==inputs and sha(ROM)==PIN;assert report['inputs_unchanged']
 report['binary_sha256']=sha(binary);report['passed']=True;report['scope']=['Original fixedRows or old patched variable function followed by complete original calVariableValues and correctWorkspaceCellSide. These are two explicit fixture calls, not the full production pipeline.','Final and intermediate ComputeContext.copyWith and Insets.copyWith are explicit identity/recording adapters; final named arguments are captured at the exact descriptor. Original intermediate horizontal correction arithmetic executes, but its cloned context and Insets results are not modeled.','Allocation, logging, boxed addition and view provider are explicit adapters; the complete original alignPixel and its descriptor/DPR/rounding selection execute. C math imports implement traced trunc and round contracts. No actual screen/provider lifecycle, GC, Dart VM, Flutter drawing, FRB, database migration or LSPosed injection.','2520 fixed-policy scenarios cover 70 UI dimensions, three enum layout identities, three synthetic viewport sizes and four DPR values. Four invalid-DPR fallback cases and three row-only counterexamples include the complete vertical tail. These counts do not assert all actual screen/window modes or all launcher settings.'];save()
if __name__=='__main__':main()
