#!/usr/bin/env python3
"""Bounded actual ARM primary entry routing; no installer or Dart VM approval."""
import argparse, bisect, hashlib, importlib.util, json, os, resource, struct, subprocess, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
ROOT=test_paths.TEST_DATA_ROOT; HERE=Path(__file__).resolve().parent
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
PIN='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
SPACING=test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/spacing-review-test16/pixel-arm-final-v2/report.json'
SPACING_PIN='5c5186d5ccc603313a7554dcaf7b98f9752b678eab3533ff28b37025746a5b05'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def elf(path):
 b=path.read_bytes();assert b[:6]==b'\x7fELF\x02\x01' and struct.unpack_from('<H',b,18)[0]==183
 po,so=struct.unpack_from('<QQ',b,32);pe,pn,se,sn,si=struct.unpack_from('<5H',b,54)
 ph=[struct.unpack_from('<II6Q',b,po+i*pe) for i in range(pn)]
 sections=[struct.unpack_from('<II4QII2Q',b,so+i*se)for i in range(sn)]
 names=b[sections[si][4]:sections[si][4]+sections[si][5]]
 def z(d,o):return d[o:d.index(0,o)].decode()
 syms={}
 for s in sections:
  if s[1]!=2:continue
  st=sections[s[6]];strings=b[st[4]:st[4]+st[5]]
  for off in range(s[4],s[4]+s[5],s[9]):
   nm,info,other,index,va,n=struct.unpack_from('<IBBHQQ',b,off)
   if nm:syms[z(strings,nm)]=(va,n,info,other,index)
 def read(va,n):
  m=[p for p in ph if p[0]==1 and p[3]<=va and va+n<=p[3]+p[5]];assert len(m)==1
  p=m[0];off=p[2]+va-p[3];return b[off:off+n],off
 return b,syms,read,[(z(names,s[0]),s) for s in sections],ph

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);a=ap.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 resource.setrlimit(resource.RLIMIT_CORE,(0,0));assert sha(ROM)==PIN
 inputs=[HERE/'router.S',HERE/'probe.S',HERE/'fixture.cpp',Path(__file__).resolve(),test_paths.SOURCE_ROOT / 'tests/os4-home-grid-spacing/spacing_v3_probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-spacing/spacing_v3_fixture.cpp',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-spacing/pixel_provenance.py',SPACING,test_paths.ROM_ROOT / 'components/system_ext/lib64/libhyper_os_flutter.so',ROM,ARM/'usr/bin/aarch64-linux-gnu-g++-13',ARM/'usr/bin/qemu-aarch64-static',NDK/'aarch64-linux-android35-clang',NDK/'llvm-objdump']
 before={str(p):sha(p)for p in inputs};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':before,'runs':[],'checks':[],'mutations':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 def check(name,b):report['checks'].append({'name':name,'passed':b is True});save();assert b,name
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(name,cmd,expected=0,witness=None):
  r=subprocess.run(list(map(str,cmd)),env=env,cwd=ROOT,capture_output=True,text=True,timeout=90)
  (out/(name+'.stdout')).write_text(r.stdout);(out/(name+'.stderr')).write_text(r.stderr)
  good=r.returncode==expected and(witness is None or witness in r.stderr)
  report['runs'].append({'name':name,'command':list(map(str,cmd)),'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':good,'stdout_sha256':sha(out/(name+'.stdout')),'stderr_sha256':sha(out/(name+'.stderr'))});save();assert good,(name,r.returncode,r.stderr);print(name,'PASS',flush=True);return r
 try:
  gcc=ARM/'usr/bin/aarch64-linux-gnu-g++-13';binary=out/'fixture';so=out/'router.so';ndkso=out/'router-ndk.so'
  run('compile-gnu-dso',[gcc,'--sysroot='+str(ARM),'-shared','-nostdlib','-Wl,--no-undefined',HERE/'router.S','-o',so])
  run('compile-ndk-dso',[NDK/'aarch64-linux-android35-clang','-shared','-nostdlib','-Wl,--no-undefined',HERE/'router.S','-o',ndkso])
  run('compile-fixture',[gcc,'--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',HERE/'fixture.cpp',HERE/'probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-spacing/spacing_v3_probe.S','-o',binary,'-ldl'])
  rom,_,read,_,rom_ph=elf(ROM);prefix,_=read(0x15228e8,44)
  table=0x62d150;head=struct.unpack_from('<4I',rom,table)
  check('actual InstructionsTable expected header',head[1:]==(102289,89682,0))
  pcs=[0x866940+struct.unpack_from('<I',rom,table+16+i*8)[0] for i in range(head[1])]
  check('actual InstructionsTable monotonic',all(a<=b for a,b in zip(pcs,pcs[1:])))
  report['full_original_functions']=[]
  for v,n in [(0x15228e8,640),(0x152228c,1244),(0x1520f40,2392),(0x1521898,276),(0xa4418c,560),(0x1abeb94,176)]:
   ix=bisect.bisect_right(pcs,v)-1;check(hex(v)+' actual complete table extent',pcs[ix]==v and pcs[ix+1]-v==n)
   blob,_=read(v,n);(out/f'{v:x}.bin').write_bytes(blob)
   dis=run('disassemble-'+hex(v),[NDK/'llvm-objdump','-d','--start-address='+str(v),'--stop-address='+str(v+n),ROM])
   report['full_original_functions'].append({'va':hex(v),'bytes':n,'sha256':hashlib.sha256(blob).hexdigest(),'assembly_sha256':hashlib.sha256(dis.stdout.encode()).hexdigest()})
  check('none original caller exact BL',read(0x1abec30,4)[0]==bytes.fromhex('978de997'))
  # Only direct B/BLs in file-backed executable segments are enumerated. This
  # does not exclude arbitrary indirect function pointers or VM resume paths.
  entries=[]
  for p in rom_ph:
   if p[0]!=1 or not(p[1]&1):continue
   for off in range(p[2],p[2]+p[5]-3,4):
    w=struct.unpack_from('<I',rom,off)[0]
    if w&0x7c000000!=0x14000000:continue
    va=p[3]+off-p[2];imm=w&0x3ffffff;imm=imm-(1<<26) if imm&(1<<25) else imm
    dest=va+4*imm
    if dest in (0x152228c,0x15228e8):entries.append({'pc':hex(va),'target':hex(dest),'link':bool(w&0x80000000)})
  check('exact direct entry callers in original executable bytes',entries==[{'pc':'0x1abec20','target':'0x15228e8','link':True},{'pc':'0x1abec30','target':'0x152228c','link':True}])
  report['direct_entry_callers']=entries
  check('frozen original pixel provenance report',sha(SPACING)==SPACING_PIN)
  inherited=json.loads(SPACING.read_text());check('pixel provenance successful and stable',inherited['passed'] is True and inherited['inputs_unchanged'] is True)
  spec=importlib.util.spec_from_file_location('pixel_provenance',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-spacing/pixel_provenance.py');pixel=importlib.util.module_from_spec(spec);spec.loader.exec_module(pixel)
  report['pixel_native_binding']=pixel.prove();check('current engine import chain matches frozen proof',report['pixel_native_binding']==inherited['pixel_native_binding'])
  report['layout_identity_inherited']=inherited['provenance']['layout_identity_fields']
  check('fixture enum raw fields match original decoded instances',[r['tagged_raw_field_0x13'] for r in report['layout_identity_inherited']]==[1,0,2])
  artifacts=[]
  for path in [so,ndkso]:
   b,syms,sread,sections,ph=elf(path);va,n,*_=syms['primary_router'];code,offset=sread(va,n)
   check(path.name+' linked ET_DYN',struct.unpack_from('<H',b,16)[0]==3)
   check(path.name+' no runtime relocations',not any(s[1] in (4,9) and s[5] for _,s in sections))
   check(path.name+' complete original prefix exactly once',code.count(prefix)==1)
   words=struct.unpack('<'+'I'*(n//4),code)
   check(path.name+' two bounded acquire samples',sum(w==0xc8dffe30 for w in words)==2)
   check(path.name+' router has no BL or BLR',not any(w>>26==0x25 or w&0xfffffc1f==0xd63f0000 for w in words))
   artifacts.append({'path':str(path),'sha256':sha(path),'router_va':hex(va),'router_size':n,'router_sha256':hashlib.sha256(code).hexdigest(),'code_hex':code.hex()})
   result=json.loads(run('positive-'+path.stem,[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM,path]).stdout)
   check(path.name+' actual bounded original flow',result['passed'] is True and result['cases']>0 and result['checks']>0);report.setdefault('executions',[]).append(result);save()
   for case in ['null-natural-off','null-foreign-on']:run(path.stem+'-'+case,[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM,path,case],42)
  report['linked_artifacts']=artifacts
  # Mutate actual fully-linked GNU machine code, then execute the same fixture.
  # A compiler error, signal or wrong assertion is not an accepted rejection.
  b,syms,sread,_,_=elf(so);router_va,router_n,*_=syms['primary_router'];code,codeoff=sread(router_va,router_n)
  mutations=[
   ('ignore-epoch',44,0x36000450,0xd503201f,'epoch closed future call selects original variable or natural fixed',None),
   ('admit-foreign-caller',28,0xb50005b1,0xd503201f,'only exact two original callers can adopt config',None),
   ('disabled-none-to-fixed',200,0xf9401231,0xf9400a31,'epoch closed future call selects original variable or natural fixed',None),
   ('hardcode-row-six',164,0xaa1003e4,0xd28000c4,'enabled router selects rows for null Smi and Mint without rewriting object',None),
   ('clobber-live-x6',160,0x6b16007f,0xd2800006,'router preserves every live GPR including x6 LR heap pool and Thread','abi-only'),
   ('lose-original-cmp-flags',160,0x6b16007f,0xd503201f,'accepted prefix retains original CMP NZCV','abi-only'),
   ('ignore-high-config-bits',60,0xd350fe11,0xd2800011,'invalid packed key falls back instead of adopting a row',None),
   ('ignore-low-row-range',116,0x36000211,0xd503201f,'invalid packed key falls back instead of adopting a row',None),
  ]
  for name,delta,old,new,witness,mode in mutations:
   actual=struct.unpack_from('<I',code,delta)[0];check('mutation original instruction '+name,actual==old)
   path=out/(name+'.so');changed=bytearray(b);struct.pack_into('<I',changed,codeoff+delta,new);path.write_bytes(changed)
   check('mutation changes exactly one word '+name,changed[:codeoff+delta]==b[:codeoff+delta] and changed[codeoff+delta+4:]==b[codeoff+delta+4:])
   cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM,path]
   if mode:cmd.append(mode)
   r=run('negative-'+name,cmd,3,'FAILED: '+witness)
   report['mutations'].append({'name':name,'va':hex(router_va+delta),'file_offset':codeoff+delta,'old':hex(old),'new':hex(new),'artifact_sha256':sha(path),'exit_code':r.returncode,'witness':witness,'rejected':True,'mode':mode or 'full'});save()
  report['scope']={
   'original_bytes':5288,'full_original_functions':6,'entry_patch_bytes':16,'caller_redirect_bytes':4,'replayed_prefix_bytes':44,
   'original_resume':'0x1522924','nullable_decode_bypassed_only_when_admitted':['0x1522914','0x1522918','0x152291c','0x1522920'],
   'samples':'shared epoch LDAR odd first, then one feature key LDAR; high16 zero, X3..9 Y4..13; only two exact original LR values',
   'frame':'original x15/FP extent, original saved LR and typed object slots; no slot type repurposing or config object mutation',
   'inflight':'deterministic epoch/feature close at first original fixed alignPixel; admitted row remains while future none returns to original variable',
   'adapters':'synthetic compressed objects, allocator, string/log, add, two identity/recorder copyWiths, view/DPR provider, InitAsync and ReturnAsync; original null-throw target exits42 rather than executing VM unwinding',
   'limits':['No Dart VM/GC/stack-map/async scheduler/DB/real copyWith cloning proof.','No actual Hyos InlineHook backend, installer, concurrent owner publication or whole-grid revoke proof.','Linked DSO remains at its linked mapping; router bytes may not be copied separately from ADRP data.','Targets, epoch pointer and nonzero key dimensions must remain immutable while reachable; changing dimensions between idle fixture cases models independent configurations, not supported hot changes.','Gate closing is not a memory reclamation barrier; accepted frames and original code/trampolines must stay alive.','x16/x17 are branch scratch; other live GPR, full Q, NZCV, LR, nativeSP and original Dart frame are checked at bounded samples.','Column producer/context consistency is a separate contract; primary validates shared X but only selects rows.','Direct edge enumeration does not establish absence of arbitrary indirect entry/resume.'],
   'production_or_device_approved':False,
  }
  report['inputs_after']={str(p):sha(p)for p in inputs};check('inputs unchanged',before==report['inputs_after'])
  report['passed']=True;save()
 except Exception as e:report['error']=repr(e);save();raise
 print(json.dumps({'passed':report['passed'],'report':str(out/'report.json'),'sha256':sha(out/'report.json')}))
if __name__=='__main__':main()
