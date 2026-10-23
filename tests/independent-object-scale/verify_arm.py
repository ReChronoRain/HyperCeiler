#!/usr/bin/env python3
"""Execute the production animation/widget banks; pass the Android NDK as argv[1]."""
import sys,struct,subprocess,tempfile,json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(root/'tests/home-layout-native'))
from dart_dump import Elf
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn import arm64_const as A
clang=next((Path(sys.argv[1])/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
with tempfile.TemporaryDirectory(prefix='hc-scale-arm-') as temp:
    obj=Path(temp)/'banks.o'
    subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(root/'app/src/main/cpp'),'-c',str(root/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(obj)],check=True)
    elf=Elf(obj);sym=elf.sections[elf.section_names['.symtab']];strings=elf.sections[sym[6]]
    strings=elf.data[strings[4]:strings[4]+strings[5]];symbols=[]
    for at in range(sym[4],sym[4]+sym[5],24):
        n,info,other,section,value,size=struct.unpack_from('<IBBHQQ',elf.data,at)
        symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,value))
    text_index=elf.section_names['.text'];section=elf.sections[text_index]
    original=elf.data[section[4]:section[4]+section[5]]
    text_base,data,replay,heap,stack,helper=0x100000,0x300000,0x400000,0x500000,0x608000,0x401000
    names=['hc_folder_anim_scale_enabled','hc_icon_scale_bits','hc_folder_anim_scale_hits']
    names += [f'hc_folder_anim_scale{i}_original' for i in range(5)]
    names += ['hc_widget_scale_enabled','hc_widget_scale_original','hc_widget_scale_bits','hc_widget_scale_hits','hc_widget_scale_applied','hc_widget_scale_applied_bits','hc_widget_dispatch_entry','hc_widget_parent_frame','hc_widget_closure_entry_field','hc_widget_child_field','hc_widget_scale_build']
    targets={n:data+i*128 for i,n in enumerate(names)}
    targets['hc_widget_scale_build']=helper
    def bank(name):
        start=next(v for n,s,v in symbols if n==name)
        end=(min(v for n,s,v in symbols if s==text_index and v>start and n.startswith('hc_')) if name=='hc_widget_scale_entry' else next(i.address+4 for i in Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(original[start:],start) if i.mnemonic=='br'))
        code=bytearray(original)
        for sec in elf.sections.values():
            if sec[1]!=4 or sec[7]!=text_index:continue
            for at in range(sec[4],sec[4]+sec[5],24):
                off,info,addend=struct.unpack_from('<QQq',elf.data,at)
                if not start<=off<end:continue
                name=symbols[info>>32][0];address,kind=targets[name]+addend,info&0xffffffff
                w=struct.unpack_from('<I',code,off)[0]
                if kind==275:
                    pages=(address>>12)-((text_base+off)>>12)
                    w=(w&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
                elif kind==277:w=(w&~(4095<<10))|((address&4095)<<10)
                elif kind==286:w=(w&~(4095<<10))|(((address&4095)//8)<<10)
                elif kind==283:w=(w&0xfc000000)|(((address-(text_base+off))//4)&0x3ffffff)
                else:raise AssertionError((name,kind))
                struct.pack_into('<I',code,off,w)
        u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
        for addr,size in [(text_base,(len(code)+4095)&~4095),(data,4096),(replay,8192),(heap,4096),(0x600000,65536),(0x700000,65536)]:u.mem_map(addr,size)
        u.mem_write(text_base,bytes(code));u.mem_write(helper,struct.pack('<I',0xd65f03c0))
        return u,text_base+start
    def init(u):
        regs={i:0x10000+i*256 for i in range(31)};regs.update({15:stack,29:stack+512})
        simd={i:((i+1)<<80)|i for i in range(32)}
        for i,v in regs.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
        for i,v in simd.items():u.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),v)
        u.reg_write(A.UC_ARM64_REG_SP,0x708000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000)
        return regs,simd
    def put(u,n,v,fmt='<Q'):u.mem_write(targets[n],struct.pack(fmt,v))
    for i in range(5):
        for enabled in [0,1]:
            for factor in [.6875,1.,1.375]:
                u,start=bank(f'hc_folder_anim_scale{i}_entry');regs,simd=init(u)
                u.reg_write(A.UC_ARM64_REG_D0,struct.unpack('<Q',struct.pack('<d',64.))[0])
                put(u,'hc_folder_anim_scale_enabled',enabled);put(u,'hc_icon_scale_bits',factor,'<d')
                put(u,f'hc_folder_anim_scale{i}_original',replay)
                u.emu_start(start,replay,count=1000)
                value=struct.unpack('<d',struct.pack('<Q',u.reg_read(A.UC_ARM64_REG_D0)))[0]
                assert value==64*(factor if enabled else 1)
                for j,v in regs.items():
                    if j not in [16,17]:assert u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(j)))==v
                for j,v in simd.items():
                    if j:assert u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(j)))==v
                assert u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
    cases=0
    # Original allocator has returned: exercise different volatile-register
    # states for its fast and slow return, without relocating either GC path.
    for moved in [0,8]:
      for slow_return in [0,1]:
       for enabled in [0,1]:
        for matching in [0,1]:
         for handled in [0,1]:
            u,start=bank('hc_widget_scale_entry');regs,simd=init(u)
            closure=heap+1;parent=heap+129;entry=0x481000
            fields={'hc_widget_parent_frame':-8-moved,'hc_widget_child_field':11+moved,
                'hc_widget_closure_entry_field':31+moved}
            for n,v in fields.items():put(u,n,v,'<q')
            put(u,'hc_widget_scale_enabled',enabled);put(u,'hc_widget_dispatch_entry',entry)
            for r,v in {0:closure,1:closure,4:0x123 if slow_return else 0x481000,30:replay}.items():
                regs[r]=v;u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(r)),v)
            u.mem_write(closure+31+moved,struct.pack('<Q',entry if matching else entry+4))
            u.mem_write(regs[29]-8-moved,struct.pack('<Q',parent))
            final=0x482000;result=heap+385
            u.mem_write(regs[29],struct.pack('<QQ',0xdeadbeef,final))
            calls=[]
            def run_helper(uc,address,size,user):
                if address!=helper:return
                calls.append(1)
                args=[uc.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i))) for i in range(6)]
                assert args==[closure,regs[29],replay,regs[26],regs[27],regs[22]],args
                for i in range(19):uc.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),0xaaa000+i)
                for i in list(range(8))+list(range(16,32)):uc.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),0xbbb000+i)
                uc.reg_write(A.UC_ARM64_REG_NZCV,0xf0000000)
                uc.reg_write(A.UC_ARM64_REG_X0,result)
                uc.reg_write(A.UC_ARM64_REG_X1,handled)
            u.hook_add(UC_HOOK_CODE,run_helper)
            routed=bool(enabled and matching and handled)
            u.emu_start(start,final,count=2000)
            assert len(calls)==int(bool(enabled and matching))
            assert u.reg_read(A.UC_ARM64_REG_X0)==(result if routed else parent)
            assert u.reg_read(A.UC_ARM64_REG_X1)==closure
            assert int.from_bytes(u.mem_read(parent+11+moved,4),'little')==closure&0xffffffff
            for i,v in regs.items():
                if i in [0,16,17,15,29,30]:continue
                assert u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v,(i,hex(v))
            for i,v in simd.items():assert u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))==v,(i,hex(v))
            assert u.reg_read(A.UC_ARM64_REG_X15)==regs[29]+16
            assert u.reg_read(A.UC_ARM64_REG_X29)==0xdeadbeef
            assert u.reg_read(A.UC_ARM64_REG_X30)==final
            assert u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
            assert u.reg_read(A.UC_ARM64_REG_SP)==0x708000
            cases+=1
    print(f'production ARM64 animation and Observer routing: PASS (30 scalar cases + {cases} creation cases; original publication/epilogue, fast/slow allocator return states, disabled/unrelated/unknown callers, moved fields, GPR/SIMD/flags)')
