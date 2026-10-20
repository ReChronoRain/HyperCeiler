#!/usr/bin/env python3
"""Execute the shipping Dock title splice. Requires Android NDK and unicorn/capstone."""
from pathlib import Path
import os,struct,subprocess,sys,tempfile
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn.arm64_const import *
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf
ndk=Path(os.environ.get('ANDROID_NDK_HOME','/opt/homebrew/share/android-commandlinetools/ndk/28.2.13676358'))
clang=next(ndk.glob('toolchains/llvm/prebuilt/*/bin/aarch64-linux-android35-clang'))
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S').read_text()
source='.text\n'+source[source.index('.macro dock_title_model index'):]+'''
.data
.globl hc_dock_title_model_hits
hc_dock_title_model_hits: .quad 0,0
.globl hc_dock_title_model_hotseat_hits
hc_dock_title_model_hotseat_hits: .quad 0,0
.p2align 4
.globl hc_dock_title_model_fields
hc_dock_title_model_fields: .word -24,-40,-64,15,19,11,339,71,-16,-24,-40,15,19,11,311,71
.globl hc_dock_title_model_enabled
hc_dock_title_model_enabled: .word 0
.p2align 3
.globl hc_dock_title_model_continue
hc_dock_title_model_continue: .quad 0x200000,0x200000
'''
with tempfile.TemporaryDirectory(prefix='dock-title-arm64-') as work:
    folder=Path(work);asm=folder/'splice.S';asm.write_text(source);exe=folder/'splice.elf'
    subprocess.run([str(clang),'-nostdlib','-static','-Wl,-Ttext=0x100000','-Wl,-e,hc_dock_title_model_0_entry',str(asm),'-o',str(exe)],check=True)
    elf=Elf(exe);symbols={};checks=0
    section=elf.sections[elf.section_names['.symtab']];strings=elf.sections[section[6]]
    table=elf.data[strings[4]:strings[4]+strings[5]]
    for at in range(section[4],section[4]+section[5],24):
        name,info,other,index,va,size=struct.unpack_from('<IBBHQQ',elf.data,at)
        symbols[table[name:table.find(b'\0',name)].decode()]=(va,size)
    for builder in range(2):
        for enabled in (0,1):
            for container in (-101,-102,-103,-105,-106,-100,0,123):
                for flags in (0xa0000000,0x40000000):
                    uc=Uc(UC_ARCH_ARM64,UC_MODE_ARM);uc.mem_map(0x100000,0x400000)
                    for va,off,size in elf.loads:uc.mem_write(va,elf.data[off:off+size])
                    model=0x300001;metadata=0x310001;frame=0x328000;stack=0x327000
                    closure=0x330001;wrapper=0x331001;child=0x332001
                    modelslot=-24 if builder==0 else -16;wrapperslot=-40 if builder==0 else -24;childslot=-64 if builder==0 else -40
                    metafield=339 if builder==0 else 311
                    uc.mem_write(model+metafield,struct.pack('<I',metadata))
                    uc.mem_write(metadata+71,struct.pack('<q',container))
                    uc.mem_write(frame+modelslot,struct.pack('<Q',model));uc.mem_write(frame+childslot,struct.pack('<Q',child))
                    uc.mem_write(symbols['hc_dock_title_model_enabled'][0],struct.pack('<I',enabled))
                    for i in range(29):uc.reg_write(UC_ARM64_REG_X0+i,0x600000+i*0x100)
                    uc.reg_write(UC_ARM64_REG_X0,closure);uc.reg_write(UC_ARM64_REG_X1,wrapper)
                    uc.reg_write(UC_ARM64_REG_X28,0);uc.reg_write(UC_ARM64_REG_X29,frame)
                    uc.reg_write(UC_ARM64_REG_X30,0x12345678);uc.reg_write(UC_ARM64_REG_SP,stack)
                    uc.reg_write(UC_ARM64_REG_NZCV,flags)
                    before={UC_ARM64_REG_X0+i:uc.reg_read(UC_ARM64_REG_X0+i) for i in range(1,29) if i not in (16,17)}
                    saved=bytes(uc.mem_read(0x300000,0x20000))
                    uc.emu_start(symbols['hc_dock_title_model_%d_entry'%builder][0],0x200000,count=100)
                    assert uc.reg_read(UC_ARM64_REG_PC)==0x200000
                    expected=child if enabled and container in (-101,-102,-103,-105,-106) else wrapper
                    assert struct.unpack('<Q',uc.mem_read(frame+wrapperslot,8))[0]==expected
                    assert struct.unpack('<I',uc.mem_read(wrapper+15,4))[0]==closure
                    assert struct.unpack('<I',uc.mem_read(wrapper+19,4))[0]==child
                    assert uc.reg_read(UC_ARM64_REG_X0)==child
                    assert all(uc.reg_read(r)==v for r,v in before.items())
                    assert uc.reg_read(UC_ARM64_REG_NZCV)==flags and uc.reg_read(UC_ARM64_REG_SP)==stack
                    assert uc.reg_read(UC_ARM64_REG_X29)==frame and uc.reg_read(UC_ARM64_REG_X30)==0x12345678
                    assert bytes(uc.mem_read(0x300000,0x20000))==saved
                    checks+=1
    print('Dock alpha ARM64: %d cases passed (on/off, five hotseats, stock workspace wrapper, replay, flags/registers/frame, immutable model)'%checks)
