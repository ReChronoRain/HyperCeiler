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
    names += [f'hc_folder_anim_scale{i}_original' for i in range(3)]
    names += ['hc_widget_scale_frame','hc_widget_scale_child_field','hc_widget_scale_enabled','hc_widget_scale_continue','hc_widget_scale_wrap']
    targets={n:data+i*128 for i,n in enumerate(names)};targets['hc_widget_scale_wrap']=helper
    def bank(name):
        start=next(v for n,s,v in symbols if n==name)
        end=next(i.address+4 for i in Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(original[start:],start) if i.mnemonic=='br')
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
    for i in range(3):
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
    for enabled in [0,1]:
        u,start=bank('hc_widget_scale_entry');regs,simd=init(u)
        owner=heap+1;frame=regs[29];oldfp,oldlr=0x12345678,0x23456789
        u.mem_write(frame,struct.pack('<QQ',oldfp,oldlr));u.mem_write(frame-8,struct.pack('<Q',owner))
        put(u,'hc_widget_scale_frame',-8,'<q');put(u,'hc_widget_scale_child_field',11,'<q')
        put(u,'hc_widget_scale_enabled',enabled);put(u,'hc_widget_scale_continue',replay)
        calls=[]
        def call(uc,address,size,user):
            if address!=helper:return
            calls.append(1);assert uc.reg_read(A.UC_ARM64_REG_X0)==owner
            assert uc.reg_read(A.UC_ARM64_REG_X1)==regs[26]
            assert uc.reg_read(A.UC_ARM64_REG_X2)==regs[27]
            assert uc.reg_read(A.UC_ARM64_REG_X3)==regs[22]
            ret=uc.reg_read(A.UC_ARM64_REG_X30)
            for j in range(19):uc.reg_write(getattr(A,'UC_ARM64_REG_X'+str(j)),j+900)
            for j in range(32):uc.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(j)),j+1000)
            uc.reg_write(A.UC_ARM64_REG_X0,owner+256);uc.reg_write(A.UC_ARM64_REG_PC,ret)
        u.hook_add(UC_HOOK_CODE,call)
        u.emu_start(start,replay,count=1000)
        assert u.reg_read(A.UC_ARM64_REG_X0)==owner+(256 if enabled else 0)
        assert len(calls)==enabled
        assert struct.unpack('<I',u.mem_read(owner+11,4))[0]==regs[1]
        for j,v in regs.items():
            if j not in [0,15,16,17,29,30]:assert u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(j)))==v,(j,v)
        for j,v in simd.items():assert u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(j)))==v
        assert u.reg_read(A.UC_ARM64_REG_X15)==frame+16
        assert u.reg_read(A.UC_ARM64_REG_X29)==oldfp and u.reg_read(A.UC_ARM64_REG_X30)==oldlr
        assert u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
    print('production ARM64 animation/widget banks: PASS (20 cases, independent gate/scalar, native ABI, SIMD/flags, original return)')
