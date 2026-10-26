#!/usr/bin/env python3
"""Execute the shipping two scalar banks plus the original 7751 replay windows."""
from pathlib import Path
import json, os, re, struct, subprocess, sys, tempfile
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM
from unicorn import arm64_const as A
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf
ndk=Path(os.environ.get('ANDROID_NDK_HOME','/opt/homebrew/share/android-commandlinetools/ndk/28.2.13676358'))
clang=next(ndk.glob('toolchains/llvm/prebuilt/*/bin/aarch64-linux-android35-clang'))
shipping=(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S').read_text()
source='.text\n'
for name in ['dart_save','dart_restore','icon_render_bank']:
    source+=re.search(r'^\.macro '+name+r'\b.*?^\.endm',shipping,re.M|re.S).group()+'\n'
body=json.loads(Path(__file__).with_name('launcher-7751.json').read_text())['functions'][0]['words']
for i,kind in enumerate(['width','height']):
    source+=next(line for line in shipping.splitlines() if line.startswith('icon_render_bank hc_folder_return_'+kind+'_entry'))+'\n'
    bank=[0x448,0x4b8 if '--old-height-window' in sys.argv else 0x4bc][i]//4
    source+='replay_'+kind+':\n.word '+','.join(hex(w) for w in body[bank:bank+4])+'\n'
    # Model the inline backend's x17 continuation jump, then execute the first
    # original instruction outside the replay (the old height site crashed here).
    source+='ldr x17, =continuation_'+kind+'\nbr x17\ncontinuation_'+kind+':\n.word '+hex(body[bank+4])+'\ndone_'+kind+': b done_'+kind+'\n'
source+='''.data
.globl hc_folder_return_scale_enabled
hc_folder_return_scale_enabled: .word 0
.p2align 3
.globl hc_icon_scale_bits
hc_icon_scale_bits: .double 1.0
.globl hc_folder_return_scale_hits
hc_folder_return_scale_hits: .quad 0
.globl hc_folder_return_width_original
hc_folder_return_width_original: .quad replay_width
.globl hc_folder_return_height_original
hc_folder_return_height_original: .quad replay_height
'''
with tempfile.TemporaryDirectory(prefix='hc-folder-return-arm-') as folder:
    folder=Path(folder);asm=folder/'banks.S';asm.write_text(source);exe=folder/'banks.elf'
    subprocess.run([str(clang),'-nostdlib','-static','-Wl,-Ttext=0x100000','-Wl,-e,hc_folder_return_width_entry',str(asm),'-o',str(exe)],check=True)
    elf=Elf(exe);symbols={};table=elf.sections[elf.section_names['.symtab']];strings=elf.sections[table[6]]
    strings=elf.data[strings[4]:strings[4]+strings[5]]
    for at in range(table[4],table[4]+table[5],24):
        n,info,other,section,va,size=struct.unpack_from('<IBBHQQ',elf.data,at)
        symbols[strings[n:strings.find(b'\0',n)].decode()]=va
    checks=0
    for kind in ['width','height']:
        for enabled in [0,1]:
            for factor in [.6875,1.,1.375]:
                for edit in [.85,1.,1.2]:
                    for flags in [0xa0000000,0x40000000]:
                        uc=Uc(UC_ARCH_ARM64,UC_MODE_ARM);uc.mem_map(0x100000,0x600000)
                        for va,off,size in elf.loads:uc.mem_write(va,elf.data[off:off+size])
                        frame=0x608000;stack=0x607000;thread=0x300000
                        regs={i:0x400000+i*128 for i in range(31)};regs.update({15:stack,26:thread,29:frame})
                        simd={i:((i+1)<<80)|i for i in range(32)}
                        for i,v in regs.items():uc.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
                        for i,v in simd.items():uc.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),v)
                        uc.reg_write(A.UC_ARM64_REG_SP,0x606000);uc.reg_write(A.UC_ARM64_REG_NZCV,flags)
                        uc.reg_write(A.UC_ARM64_REG_D0,struct.unpack('<Q',struct.pack('<d',64.))[0])
                        uc.reg_write(A.UC_ARM64_REG_D1,struct.unpack('<Q',struct.pack('<d',edit))[0])
                        uc.mem_write(frame-248,struct.pack('<d',edit));uc.mem_write(frame-256,struct.pack('<d',88.))
                        uc.mem_write(thread+0x78,struct.pack('<Q',0x340000))
                        uc.mem_write(0x340000+0x2718,struct.pack('<Q',0x456789))
                        uc.mem_write(symbols['hc_folder_return_scale_enabled'],struct.pack('<I',enabled))
                        uc.mem_write(symbols['hc_icon_scale_bits'],struct.pack('<d',factor))
                        uc.emu_start(symbols['hc_folder_return_'+kind+'_entry'],symbols['done_'+kind],count=1000)
                        result=struct.unpack('<d',struct.pack('<Q',uc.reg_read(A.UC_ARM64_REG_D2)))[0]
                        assert result==64*(factor if enabled else 1)*edit
                        if kind=='width':assert struct.unpack('<d',uc.mem_read(frame-256,8))[0]==result
                        else:
                            assert struct.unpack('<d',struct.pack('<Q',uc.reg_read(A.UC_ARM64_REG_D0)))[0]==88.
                            assert struct.unpack('<d',uc.mem_read(frame-264,8))[0]==result
                        for i,v in regs.items():
                            if i not in [16,17] and not (i==0 and kind=='width'):
                                assert uc.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v
                        if kind=='width':assert uc.reg_read(A.UC_ARM64_REG_X0)==0x456789
                        for i,v in simd.items():
                            if i not in ([0,1,2] if kind=='width' else [0,1,2,3]):assert uc.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))==v
                        assert uc.reg_read(A.UC_ARM64_REG_SP)==0x606000 and uc.reg_read(A.UC_ARM64_REG_NZCV)==flags
                        assert struct.unpack('<Q',uc.mem_read(symbols['hc_folder_return_scale_hits'],8))[0]==enabled
                        checks+=1
    print('folder app return ARM64: %d cases passed (shipping banks, original replay, on/off/scales/edit, registers/SIMD/flags)'%checks)
