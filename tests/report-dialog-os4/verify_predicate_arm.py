#!/usr/bin/env python3
"""Execute the three real predicates with synthetic compressed Dart objects.

This validates the original bool/list semantics and byte offsets, not a device
or a live Dart GC. The production hook/gateway has separate C++/ARM64 tests.
"""
from pathlib import Path
import json,struct
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn import arm64_const as A
HERE=Path(__file__).resolve().parent
checks=0
for version in ('7751','8583','8584'):
 data=json.loads((HERE/(version+'.json')).read_text());body=next(f['words']for f in data['functions']if f['va']==data['entry'])[:35]
 field=lambda w:((w>>12)&511)-(512 if w&(1<<20) else 0)
 for length in (0,1,3):
  for eligible in (False,True):
   u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
   text,pool,dispatch,callback,stop,stack,heap=0x100000,0x2000000,0x30000000,0x400000,0x401000,0x608000,0x100000000
   for address,size in [(text,4096),(pool,0x2000000),(dispatch,4096),(callback,8192),(0x600000,65536),(heap,65536)]:u.mem_map(address,size)
   u.mem_write(text,struct.pack('<35I',*body));u.mem_write(callback,struct.pack('<I',0xd65f03c0))
   null=heap+0x801;true,false=null+0x20,null+0x30
   linked=[heap+0x1001,heap+0x2001,heap+0x3001,heap+0x4001]
   u.mem_write(linked[0]+field(body[3]),struct.pack('<I',length*2))
   for pointer,target,w in zip(linked,linked[1:],(body[14],body[16],body[18])):u.mem_write(pointer+field(w),struct.pack('<I',target-heap))
   cid=5;u.mem_write(linked[-1]-1,struct.pack('<Q',cid<<12))
   descriptor=pool+((body[22]>>10)&4095)*4096+((body[23]>>10)&4095)*8
   u.mem_write(descriptor,struct.pack('<Q',0x12345671));u.mem_write(dispatch+cid*8,struct.pack('<Q',callback))
   regs={15:stack,21:dispatch,22:null,27:pool,28:1,29:0x123450,30:stop,2:linked[0]}
   for i,v in regs.items():u.reg_write(getattr(A,f'UC_ARM64_REG_X{i}'),v)
   u.reg_write(A.UC_ARM64_REG_SP,0x601000)
   calls=[]
   def typed_predicate(uc,address,size,user):
    if address==callback:calls.append(1);uc.reg_write(A.UC_ARM64_REG_X0,true if eligible else false)
   u.hook_add(UC_HOOK_CODE,typed_predicate);u.emu_start(text,stop,count=100)
   expected=length==1 and not eligible
   assert u.reg_read(A.UC_ARM64_REG_X0)==(true if expected else false),(version,length,eligible)
   assert len(calls)==int(length==1)
   for i in (15,29,30):assert u.reg_read(getattr(A,f'UC_ARM64_REG_X{i}'))==regs[i]
   assert u.reg_read(A.UC_ARM64_REG_SP)==0x601000
   checks+=1
print(f'report original ARM64 predicate PASS: {checks} zero/single/multiple app cases, typed predicate polarity, compressed-reference byte fields, original Dart/native frame return')
