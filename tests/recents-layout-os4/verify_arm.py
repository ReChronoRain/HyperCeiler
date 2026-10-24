#!/usr/bin/env python3
"""Execute the shipping recents register gateway with adversarial native clobbers."""
import sys,struct,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn import arm64_const as A
clang=next((Path(sys.argv[1])/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
with tempfile.TemporaryDirectory(prefix='hc-recents-arm-')as work:
 obj=Path(work)/'bank.o';subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(ROOT/'app/src/main/cpp'),'-c',str(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(obj)],check=True)
 elf=Elf(obj);sym=elf.sections[elf.section_names['.symtab']];strings=elf.sections[sym[6]];strings=elf.data[strings[4]:strings[4]+strings[5]];symbols=[]
 for at in range(sym[4],sym[4]+sym[5],24):
  n,info,other,section,value,size=struct.unpack_from('<IBBHQQ',elf.data,at);symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,value))
 text_index=elf.section_names['.text'];section=elf.sections[text_index];raw=elf.data[section[4]:section[4]+section[5]]
 code_base,callback,stop,stack,dart_frame=0x100000,0x400000,0x401000,0x608000,0x708000
 checks=0
 for family,index in [('recent',i) for i in (0,1,16,63)]+[('prestart',0),('drawer',0),('drawer',1)]:
  start=next(v for n,s,v in symbols if n==f'hc_{family}_{index}_entry')
  end=next((v for n,s,v in symbols if s==text_index and v>start and n.startswith('hc_') and n.endswith('_entry')),len(raw))
  for terminal in ((False,) if family=='drawer' else (False,True)):
   for changed_reg in ((0,1,3,18,29) if family=='recent' else (0,)):
    code=bytearray(raw)
    for sec in elf.sections.values():
     if sec[1]!=4 or sec[7]!=text_index:continue
     for at in range(sec[4],sec[4]+sec[5],24):
      off,info,addend=struct.unpack_from('<QQq',elf.data,at)
      if not start<=off<end:continue
      assert symbols[info>>32][0]==f'hc_{family}_body' and info&0xffffffff==283
      w=struct.unpack_from('<I',code,off)[0];w=(w&0xfc000000)|(((callback+addend-(code_base+off))//4)&0x3ffffff);struct.pack_into('<I',code,off,w)
    u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    for address,size in [(code_base,(len(code)+4095)&~4095),(callback,8192),(0x600000,65536),(0x700000,65536)]:u.mem_map(address,size)
    u.mem_write(code_base,bytes(code));u.mem_write(callback,struct.pack('<I',0xd65f03c0))
    regs={i:0x123000+i*32 for i in range(31)};regs.update({15:dart_frame-128,29:dart_frame,30:stop})
    q={i:(i+1)<<80|0x76540000+i for i in range(32)}
    for i,v in regs.items():u.reg_write(getattr(A,f'UC_ARM64_REG_X{i}'),v)
    for i,v in q.items():u.reg_write(getattr(A,f'UC_ARM64_REG_Q{i}'),v)
    u.reg_write(A.UC_ARM64_REG_SP,stack);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000);u.reg_write(A.UC_ARM64_REG_FPCR,0x400000);u.reg_write(A.UC_ARM64_REG_FPSR,0x10)
    calls=[];expected_x=dict(regs);expected_q=dict(q)
    def helper(uc,address,size,user):
     if address!=callback:return
     calls.append(1);saved=uc.reg_read(A.UC_ARM64_REG_X0);kind=uc.reg_read(A.UC_ARM64_REG_X1)
     assert kind==index and saved==stack-800 and saved%16==0
     for i,v in regs.items():assert int.from_bytes(uc.mem_read(saved+i*8,8),'little')==v
     for i,v in q.items():assert int.from_bytes(uc.mem_read(saved+256+i*16,16),'little')==v
     assert int.from_bytes(uc.mem_read(saved+248,8),'little')==0xa0000000
     assert int.from_bytes(uc.mem_read(saved+768,8),'little')==0x400000
     assert int.from_bytes(uc.mem_read(saved+776,8),'little')==0x10
     expected_x[changed_reg]=0xabc00000+changed_reg
     expected_q[3]=(0xaabbcc<<80)|0x112233
     if family=='drawer' and index==0:uc.mem_write(dart_frame-8,struct.pack('<Q',regs[22]))
     uc.mem_write(saved+changed_reg*8,struct.pack('<Q',expected_x[changed_reg]))
     uc.mem_write(saved+256+3*16,expected_q[3].to_bytes(16,'little'))
     if terminal and family=='prestart':
      expected_x[0]=regs[22];uc.mem_write(saved,struct.pack('<Q',regs[22]))
     elif terminal:
      expected_x.update({15:dart_frame+16,29:0xdeadbeef,30:stop})
      for i in (15,29,30):uc.mem_write(saved+i*8,struct.pack('<Q',expected_x[i]))
     uc.mem_write(saved+784,struct.pack('<Q',0 if terminal else stop))
     for i in range(19):uc.reg_write(getattr(A,f'UC_ARM64_REG_X{i}'),0xaaa000+i)
     for i in range(32):uc.reg_write(getattr(A,f'UC_ARM64_REG_Q{i}'),0xbbb000+i)
     uc.reg_write(A.UC_ARM64_REG_NZCV,0xf0000000);uc.reg_write(A.UC_ARM64_REG_FPCR,0);uc.reg_write(A.UC_ARM64_REG_FPSR,0)
    u.hook_add(UC_HOOK_CODE,helper);u.emu_start(code_base+start,stop,count=1000)
    assert len(calls)==1
    if family=='drawer' and index==0:assert int.from_bytes(u.mem_read(dart_frame-8,8),'little')==regs[22]
    for i,v in expected_x.items():
     if i==16:continue # Backend continuation scratch.
     assert u.reg_read(getattr(A,f'UC_ARM64_REG_X{i}'))==v,(index,terminal,i)
    for i,v in expected_q.items():assert u.reg_read(getattr(A,f'UC_ARM64_REG_Q{i}'))==v
    assert u.reg_read(A.UC_ARM64_REG_SP)==stack and u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
    assert u.reg_read(A.UC_ARM64_REG_FPCR)==0x400000 and u.reg_read(A.UC_ARM64_REG_FPSR)==0x10
    checks+=1
 print(f'recents/prestart/drawer ARM64 gateway PASS: {checks} cases; live/terminal routing, all GPR/Q registers, SP16, NZCV, FPCR/FPSR and native clobbers')
