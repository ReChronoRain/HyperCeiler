#!/usr/bin/env python3
"""Execute the production Dock getter bank for admitted and shared callers."""
import sys,struct,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn import arm64_const as A
clang=next((Path(sys.argv[1])/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
with tempfile.TemporaryDirectory(prefix='hc-margin-arm-')as tmp:
 obj=Path(tmp)/'bank.o'
 subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(ROOT/'app/src/main/cpp'),'-c',str(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(obj)],check=True)
 elf=Elf(obj);sec=elf.sections[elf.section_names['.symtab']];strings=elf.sections[sec[6]];strings=elf.data[strings[4]:strings[4]+strings[5]];syms=[]
 for at in range(sec[4],sec[4]+sec[5],24):
  n,_,_,section,value,_=struct.unpack_from('<IBBHQQ',elf.data,at);syms.append((strings[n:strings.find(b'\0',n)].decode(),section,value))
 ix=elf.section_names['.text'];text=elf.sections[ix];code=bytearray(elf.data[text[4]:text[4]+text[5]])
 name='hc_layout_dart_HotseatMargin_';start=next(v for n,s,v in syms if n==name+'entry');end=next(v for n,s,v in syms if s==ix and v>start and n.endswith('_entry'))
 base,data,original,stop=0x100000,0x300000,0x400000,0x401000
 targets={name+n:data+i*8 for i,n in enumerate(('caller','hits','original','last','enabled','delta'))}
 targets.update({'hc_hotseat_margin_caller_count':data+64,'hc_hotseat_margin_applied':data+72,'hc_hotseat_margin_shared':data+80,'hc_hotseat_margin_callers':data+128})
 for sec in elf.sections.values():
  if sec[1]!=4 or sec[7]!=ix:continue
  for at in range(sec[4],sec[4]+sec[5],24):
   off,info,addend=struct.unpack_from('<QQq',elf.data,at)
   if not start<=off<end:continue
   addr=targets[syms[info>>32][0]]+addend;kind=info&0xffffffff;w=struct.unpack_from('<I',code,off)[0]
   if kind==275:
    pages=(addr>>12)-((base+off)>>12);w=(w&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
   elif kind==277:w=(w&~(4095<<10))|((addr&4095)<<10)
   else:raise AssertionError(kind)
   struct.pack_into('<I',code,off,w)
 cases=0
 for enabled in (0,1):
  for count in (0,1,10,32):
   for matched in (False,True):
    for margin in (0.,70.,110.,150.):
     u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
     for a,z in ((base,(len(code)+4095)&~4095),(data,4096),(original,8192),(0x600000,65536)):u.mem_map(a,z)
     u.mem_write(base,bytes(code));u.mem_write(original,struct.pack('<II',0x1e641000,0xd65f03c0)) # original D0=8; RET
     for n,v in ((name+'enabled',enabled),(name+'original',original),('hc_hotseat_margin_caller_count',count)):
      u.mem_write(targets[n],struct.pack('<Q',v))
     u.mem_write(targets[name+'delta'],struct.pack('<d',margin))
     callers=[stop+4+i*4 for i in range(32)]
     if matched and count:callers[count-1]=stop
     u.mem_write(targets['hc_hotseat_margin_callers'],struct.pack('<32Q',*callers))
     regs={i:0x123000+i*32 for i in range(31)};regs[15]=0x608000;regs[30]=stop
     for i,v in regs.items():u.reg_write(getattr(A,f'UC_ARM64_REG_X{i}'),v)
     u.reg_write(A.UC_ARM64_REG_SP,0x609000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000)
     u.emu_start(base+start,stop,count=1000)
     got=struct.unpack('<d',struct.pack('<Q',u.reg_read(A.UC_ARM64_REG_D0)))[0]
     assert got==(margin if enabled and matched and count else 8.),(enabled,count,matched,margin,got)
     for i,v in regs.items():assert u.reg_read(getattr(A,f'UC_ARM64_REG_X{i}'))==v,(i,v)
     assert u.reg_read(A.UC_ARM64_REG_SP)==0x609000 and u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
     assert struct.unpack('<Q',u.mem_read(targets[name+'hits'],8))[0]==1
     assert struct.unpack('<d',u.mem_read(targets[name+'last'],8))[0]==8.
     assert struct.unpack('<Q',u.mem_read(targets['hc_hotseat_margin_applied'],8))[0]==bool(enabled and matched and count)
     assert struct.unpack('<Q',u.mem_read(targets['hc_hotseat_margin_shared'],8))[0]==bool(enabled and not matched and count)
     cases+=1
 print(f'Dock margin ARM64 PASS: {cases} cases; shared callers untouched, absolute 0/70/110/150, disabled, empty/full admission, original execution and all GPR/NZCV/SP preserved')
