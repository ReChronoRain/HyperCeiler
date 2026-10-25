#!/usr/bin/env python3
"""Execute the exact four-word fold bypass emitted by the production planner."""
import argparse,json,struct
from pathlib import Path
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn import arm64_const as A
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--corpus',type=Path,required=True);args=parser.parse_args()
files=sorted(args.corpus.glob('*.json'));assert len(files)==6
checks=0
for file in files:
 data=json.loads(file.read_text());words=data['words'];assert words[:2]==[0xa9bf79fd,0xaa0f03fd]
 load=words[2];assert load&0xffc003ff==0xf9400360
 assert words[3:6]==[0xaa1d03ef,0xa8c179fd,0xd65f03c0]
 for alignment in (0,8):
  for widget in (0x12345671,0x87654321):
   u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
   for address,size in [(0x100000,4096),(0x200000,65536),(0x400000,8192),(0x600000,65536)]:u.mem_map(address,size)
   u.mem_write(0x100000,struct.pack('<'+'I'*len(words),*words));u.mem_write(0x200000+((load>>10)&4095)*8,struct.pack('<Q',widget))
   regs={i:0x123000+i*32 for i in range(31)};regs.update({15:0x608000+alignment,27:0x200000,30:0x401000})
   q={i:(i+1)<<80|0x76540000+i for i in range(32)}
   for i,v in regs.items():u.reg_write(getattr(A,f'UC_ARM64_REG_X{i}'),v)
   for i,v in q.items():u.reg_write(getattr(A,f'UC_ARM64_REG_Q{i}'),v)
   u.reg_write(A.UC_ARM64_REG_SP,0x601000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000)
   u.reg_write(A.UC_ARM64_REG_FPCR,0x400000);u.reg_write(A.UC_ARM64_REG_FPSR,0x10)
   u.emu_start(0x100000,0x401000,count=12)
   for i,v in regs.items():assert u.reg_read(getattr(A,f'UC_ARM64_REG_X{i}'))==(widget if i==0 else v),(file.name,i)
   for i,v in q.items():assert u.reg_read(getattr(A,f'UC_ARM64_REG_Q{i}'))==v
   assert u.reg_read(A.UC_ARM64_REG_SP)==0x601000 and u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
   assert u.reg_read(A.UC_ARM64_REG_FPCR)==0x400000 and u.reg_read(A.UC_ARM64_REG_FPSR)==0x10
   checks+=1
print(f'clear disable ARM64 PASS: {checks} actual planner executions; original and relocated graphs, pool operand, Dart frame return, GPR/Q/flags and separate native SP')
