from pathlib import Path
import subprocess,struct,sys,re,json
W=Path(__file__).resolve().parents[2];D=W/'handover/folder-title-20261011';H=W/'app/src/main/cpp/targets/home'
clang=Path('D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe')
p=subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(W/'app/src/main/cpp'),'-c',str(H/'home_layout_dart_arm64.S'),'-o',str(D/'folder-stubs.o')],capture_output=True,text=True);assert p.returncode==0,p.stderr
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
e=Elf(D/'folder-stubs.o');ss=e.sections[e.section_names['.symtab']];st=e.sections[ss[6]];strings=e.data[st[4]:st[4]+st[5]];symbols=[]
for a in range(ss[4],ss[4]+ss[5],24):
 n,_,_,section,value,_=struct.unpack_from('<IBBHQQ',e.data,a);symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,value))
rel={}
for sec in e.sections.values():
 if sec[1]==4:
  for a in range(sec[4],sec[4]+sec[5],24):
   off,info,addend=struct.unpack_from('<QQq',e.data,a);rel[off]=symbols[info>>32][0]
checks=0
bits=lambda f:struct.unpack('<Q',struct.pack('<d',f))[0]
flt=lambda b:struct.unpack('<d',struct.pack('<Q',b))[0]
for name,endreg in [('hc_folder_title_scale_entry','x2'),('hc_folder_title_scale4_entry','x4')]:
 _,section,at=next(x for x in symbols if x[0]==name);sec=e.sections[section];allcode=list(Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(e.data[sec[4]+at:sec[4]+at+180],at));code=allcode[:next(i for i,x in enumerate(allcode) if x.mnemonic=='br')+1]
 (D/(name+'.asm')).write_text('\n'.join(f'{i.address:x} {i.mnemonic} {i.op_str}' for i in code))
 for factor in [1.0,0.59375,1.1875,0.6875]:
  for room in [0,16,32]:
   r={f'x{i}':1000+i for i in range(31)};r['x29']=50000;r['x26']=60000;r['x15']=70000;r['q1']=123456789;r['d0']=0.0;r['d1']=flt(r['q1']&((1<<64)-1));original=r.copy()
   globals={'hc_folder_title_font_frame':-56,'hc_folder_title_scale_bits':bits(factor),'hc_folder_title_nursery':96,'hc_folder_title_allocation':16,'hc_folder_title_scale_continue':123456}
   address={n:80000+j*256 for j,n in enumerate(globals)};mem={address[n]:v for n,v in globals.items()};mem[50000-56]=bits(12.0);mem[60000+96]=200000;mem[60000+104]=200000+room
   def val(x):return int(x[1:],0) if x.startswith('#') else r[x]
   for i in code:
    ops=i.op_str.split(', ');op=i.mnemonic
    if op=='adrp':r[ops[0]]=address[rel[i.address]]
    elif op=='add':r[ops[0]]=address[rel[i.address]] if i.address in rel else val(ops[1])+val(ops[2])
    elif op in ('ldr','ldar'):
     dest=ops[0]
     if i.address in rel:v=mem[address[rel[i.address]]]
     else:
      m=re.search(r'\[(x\d+)(?:, (x\d+|#-?0x[\da-f]+|#-?\d+))?\]',i.op_str);a=r[m[1]]+(val(m[2]) if m[2] else 0);v=mem[a]
     if dest=='d0':r['d0']=flt(v)
     elif dest=='q1':r['q1']=v;r['d1']=flt(v&((1<<64)-1))
     else:r[dest]=v
     if i.op_str.endswith('], #0x10'):r['x15']+=16
    elif op=='str':assert i.op_str=='q1, [x15, #-0x10]!';r['x15']-=16;mem[r['x15']]=r['q1']
    elif op=='fmov':r['d1']=flt(r['x17']);r['q1']=r['x17']
    elif op=='fmul':r['d0']=r['d0']*r['d1']
    elif op=='ldp':m=re.search(r'\[(x\d+)\]',i.op_str);a=r[m[1]];r[ops[0]]=mem[a];r[ops[1]]=mem[a+8]
    elif op=='cmp':flags=(val(ops[0])<=val(ops[1]))
    elif op=='br':assert r['x16']==123456
    else:raise RuntimeError((op,i.op_str))
   assert r['d0']==12.0*factor and r['x1']==200016 and r[endreg]==200000+room
   assert flags==(room<=16) and r['q1']==original['q1'] and r['x15']==original['x15']
   for n,v in original.items():
    if n not in ['x1',endreg,'x16','x17','d0','d1']:assert r[n]==v,n
   checks+=1
print(f'ARM64_FOLDER_SCALE={checks} cases; failed=0; register/SIMD/stack/flags restored')
