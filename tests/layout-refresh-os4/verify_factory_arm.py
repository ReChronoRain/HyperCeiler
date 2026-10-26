#!/usr/bin/env python3
"""Execute the recognized stock Dart empty-wrapper allocator, not a C clone."""
import json,struct
from pathlib import Path
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn import arm64_const as A
HERE=Path(__file__).resolve().parent
checks=0
for version in ('7751','8583','8584'):
    d=json.loads((HERE/f'drawer-{version}.json').read_text());byva={f['va']:f['words'] for f in d['functions']}
    factory,common=d['blank_factory'],d['common'];stub=byva[factory][:3];words=byva[common][:19]
    tag=((stub[0]>>5)&65535)|(((stub[1]>>5)&65535)<<16);size=((tag>>8)&15)*16
    assert size>=24 and size<=256
    top_field=((words[2]>>15)&127)*8;end_field=top_field+8
    for alignment in (0,8):
        for available in (True,False):
            u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
            for page in {factory&~4095,common&~4095}:u.mem_map(page,4096)
            u.mem_write(factory,struct.pack('<III',*stub));u.mem_write(common,struct.pack('<'+'I'*len(words),*words))
            thread,heap,stack,dart,stop,slow=0x70000000,0x71000000,0x72008000,0x73008000,0x74000000,0x74001000
            for address,length in [(thread,4096),(heap,4096),(stack-32768,65536),(dart-32768,65536),(stop,8192)]:u.mem_map(address,length)
            top=heap+128+alignment;end=top+size+(64 if available else 0)
            u.mem_write(thread+top_field,struct.pack('<Q',top));u.mem_write(thread+end_field,struct.pack('<Q',end))
            slow_field=((words[17]>>10)&4095)*8
            u.mem_write(thread+slow_field,struct.pack('<Q',slow))
            u.reg_write(A.UC_ARM64_REG_SP,stack);u.reg_write(A.UC_ARM64_REG_X15,dart);u.reg_write(A.UC_ARM64_REG_X29,dart+128)
            u.reg_write(A.UC_ARM64_REG_X30,stop);u.reg_write(A.UC_ARM64_REG_X26,thread);u.reg_write(A.UC_ARM64_REG_X22,0x12340001)
            reached=[]
            def hook(uc,pc,size,_):
                if pc!=slow:return
                # The register gateway must have restored this same caller
                # context before tail routing here. No native C frame may own GC.
                assert uc.reg_read(A.UC_ARM64_REG_SP)==stack and uc.reg_read(A.UC_ARM64_REG_X15)==dart
                assert uc.reg_read(A.UC_ARM64_REG_X29)==dart+128 and uc.reg_read(A.UC_ARM64_REG_X30)==stop
                reached.append(1);uc.emu_stop()
            u.hook_add(UC_HOOK_CODE,hook);u.emu_start(factory,stop,count=200)
            assert bool(reached)==(not available)
            got_top=int.from_bytes(u.mem_read(thread+top_field,8),'little')
            if available:
                assert got_top==top+size and u.reg_read(A.UC_ARM64_REG_X0)==top+1
                assert int.from_bytes(u.mem_read(top,8),'little')==tag
                assert all(int.from_bytes(u.mem_read(top+i,4),'little')==0x12340001 for i in range(8,size,4))
            else:assert got_top==top
            checks+=1
print(f'stock empty-wrapper ARM64 PASS: {checks} cases; all 3 images, nursery 0/8 mod16, null child/dimensions, original slow tail and Dart caller context')
