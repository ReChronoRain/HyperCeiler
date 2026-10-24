from pathlib import Path
import subprocess,struct,sys
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/full-dynamic-20261009'/('arm-policy-'+R.name);D.mkdir(exist_ok=True)
clang=Path('D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe')
p=subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(W/'app/src/main/cpp'),'-c',str(R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(D/'policy.o')],capture_output=True,text=True)
if p.returncode:print(p.stderr);sys.exit(p.returncode)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
e=Elf(D/'policy.o');sec=e.sections[e.section_names['.symtab']];st=e.sections[sec[6]];strings=e.data[st[4]:st[4]+st[5]];symbols={}
for a in range(sec[4],sec[4]+sec[5],24):
 name,_,_,section,value,_=struct.unpack_from('<IBBHQQ',e.data,a);name=strings[name:strings.find(b'\0',name)].decode();symbols[name]=(section,value)
section,at=symbols['hc_layout_indicator_slide_only_entry'];t=e.sections[section];ins=list(Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(e.data[t[4]+at:t[4]+at+200],at));start=next(i.address for i in ins if i.mnemonic=='cmp');code={i.address:i for i in ins};checks=failed=0
# Execute the actual assembled policy control-flow slice, supplying only the
# externally loaded mode and two Dart return-PC values. Not a CPU/GC emulator.
for mode in range(3):
 for request in range(5):
  for idle in [False,True]:
   regs={'w10':mode,'x3':request};pc=start;eq=False
   def val(v):return int(v[1:],0) if v.startswith('#') else regs[v]
   for step in range(40):
    i=code[pc];op=i.op_str.split(', ');pc+=4
    if i.mnemonic=='ldp':break # original-value/flags restore follows; x3 is not restored
    if i.mnemonic=='cmp':eq=val(op[0])==val(op[1])
    elif i.mnemonic=='b.ne':
     if not eq:pc=val(op[0])
    elif i.mnemonic=='b':pc=val(op[0])
    elif i.mnemonic=='cbnz':
     if val(op[0]):pc=val(op[1])
    elif i.mnemonic=='mov':regs[op[0]]=val(op[1])
    elif i.mnemonic=='ldr':
     if i.op_str=='x10, [x29, #8]':regs['x10']=0x1234 if idle else 0x5678
     elif i.op_str=='x9, [x9]':regs['x9']=0x1234
     else:raise RuntimeError(i.op_str)
    elif i.mnemonic in ('adrp','add'):pass
    else:raise RuntimeError((i.mnemonic,i.op_str))
   else:raise RuntimeError('policy loop did not return')
   expected=3 if request==0 and (mode==2 or mode==1 and idle) else request;checks+=1
   if regs['x3']!=expected:failed+=1;print(f'FAIL mode={mode} request={request} idle={idle}: {regs["x3"]} != {expected}')
print(f'ARM64_INDICATOR_POLICY={checks} checks; failed={failed}');sys.exit(bool(failed))
