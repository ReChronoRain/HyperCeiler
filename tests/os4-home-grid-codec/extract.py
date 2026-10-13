#!/usr/bin/env python3
import argparse,hashlib,json,subprocess,sys,struct,bisect
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT
sys.path.insert(0,str(test_paths.SOURCE_ROOT / 'tests/hyos-entry'));from extract_runtime_guard import Elf
sys.path.insert(0,str(test_paths.SOURCE_ROOT / 'tests/home-layout-native'));from fixture_symbols import symbols
OBJ=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump'
IMAGES={'dart':('libapp.so','a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992',[0x8bb7bc,0x8bb864,0x86ea54,0x86ead4,0x86ed9c,0x18a92c4,0x86da14]),'rust':('libapp_launcher.so','4dc9fa82371dda8a0a8b9eff48301469d2f84752b52b71635b84e5fc0c47fa6e',[0x84152c,0x965f98,0x965ffc,0x6c46e0,0x6c567c,0x6c44d8,0x6c4584,0x9aa6f8,0xaa40f8,0x7ed3b8])}
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);result={'device_status':'NOT_TESTED','functions':{},'images':{}}
 for kind,(name,h,vas)in IMAGES.items():
  path=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a'/name;e=Elf(path,h);syms=symbols(path);result['images'][kind]={'path':str(path),'sha256':h}
  for va in vas:
   found=[(size,n)for at,size,n in syms if at==va];origin='GNU embedded debug function symbol'
   if not found and kind=='dart':
    table=e.read(0x62d150,16+102289*8);header=struct.unpack_from('<IIII',table);assert header==(1920452,102289,89682,0)
    starts=[0x866940+struct.unpack_from('<I',table,16+i*8)[0]for i in range(header[1])];idx=starts.index(va);size=starts[idx+1]-va;assert size>0;found=[(size,'anonymous InstructionsTable entry '+str(idx))];origin='Actual snapshot InstructionsTable next entry boundary';result['instruction_table_sha256']=hashlib.sha256(table).hexdigest()
   assert found,(kind,hex(va));sizes={n[0]for n in found};assert len(sizes)==1,sizes;size=sizes.pop();assert size>0
   raw=e.read(va,size,True);prefix=kind+'-'+hex(va)[2:];(a.output/(prefix+'.bin')).write_bytes(raw)
   cmd=[str(OBJ),'-D','--start-address='+hex(va),'--stop-address='+hex(va+size),str(path)];asm=subprocess.check_output(cmd,text=True);(a.output/(prefix+'.asm.txt')).write_text(asm)
   result['functions'][prefix]={'va':hex(va),'size':size,'extent_source':origin,'aliases':[n for _,n in found],'full_symbol_sha256':hashlib.sha256(raw).hexdigest(),'command':cmd}
 result['inputs']={str(p):hashlib.sha256(p.read_bytes()).hexdigest()for p in [Path(__file__).resolve(),test_paths.SOURCE_ROOT / 'tests/hyos-entry/extract_runtime_guard.py',test_paths.SOURCE_ROOT / 'tests/home-layout-native/fixture_symbols.py',OBJ]};result['passed']=True;(a.output/'report.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({n:{'size':v['size'],'aliases':v['aliases']}for n,v in result['functions'].items()},indent=2))
if __name__=='__main__':main()
