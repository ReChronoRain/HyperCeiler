"""Check the real 7751 producer and execute the production ARM64 scaling bank.

Requires a host C++ compiler, Android NDK clang, capstone and unicorn.
Run with --ndk /path/to/ndk; all generated files live in a temporary directory.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--ndk', type=Path, default=os.environ.get('ANDROID_NDK_HOME'))
args = parser.parse_args()
fixture = json.loads(Path(__file__).with_name('launcher-7751.json').read_text())

def words(key):
    return '{' + ','.join(fixture[key]['words']) + '}'

with tempfile.TemporaryDirectory(prefix='hc-folder-scale-') as temp:
    out = Path(temp)
    source = r'''
#include "targets/home/home_icon_render.h"
#include <cassert>
#include <iostream>
using namespace home_layout;
int main() {
    home_dynamic::Body b=SMALL, base=BASE;
    SmallFolderScaleSite shape;
    auto resolve=[&] { return small_folder_scale_site(b,SMALL_VA,base,BASE_VA,CURRENT,PREVIEW,shape); };
    assert(resolve());
    assert((shape.dimensions==std::array<int32_t,4>{15,23,39,31}));
    auto original=b; auto resolved=shape;
    home_dynamic::Body large=BIG, preview=BIG_PREVIEW;
    std::array<home_dynamic::Body,2> helpers={home_dynamic::Body HELPER_WIDTH,home_dynamic::Body HELPER_HEIGHT};
    uint32_t helper_va[]={HELPER_WIDTH_VA,HELPER_HEIGHT_VA};
    BigFolderScaleSite big_shape;
    auto big_resolve=[&] {return big_folder_scale_site(large,BIG_VA,helpers,helper_va,preview,BASE_VA,resolved,big_shape);};
    assert(big_resolve());
    assert(big_shape.dimensions.size()==13);
    auto original_large=large;
    auto saved_helpers=helpers;
    for(auto& helper:helpers)for(auto& w:helper)
        if((w&0xffe00c00u)==0xfc000000u&&home_dynamic::imm9(w)==127)w=0xd503201f;
    assert(!big_resolve());helpers=saved_helpers;
    auto empty=helpers;empty[0].clear();assert(!big_folder_scale_site(large,BIG_VA,empty,helper_va,preview,BASE_VA,resolved,big_shape));
    preview.clear();assert(!big_resolve());preview=BIG_PREVIEW;
    helper_va[0]+=4;assert(!big_resolve());helper_va[0]-=4;
    large.insert(large.end(),original_large.begin(),original_large.end());assert(!big_resolve());large=original_large;
    assert(big_resolve());
    auto saved_preview=preview;
    auto moved=resolved;
    for(auto& offset:moved.dimensions)offset+=8;
    for(auto* block:{&large,&helpers[0],&helpers[1]})for(auto& w:*block)
        if((w&0xffe00c00u)==0xfc000000u&&(home_dynamic::rn(w)==1||home_dynamic::rn(w)==2))w+=8u<<12;
    for(auto& w:preview)if((w&0xffe00c00u)==0xfc400000u&&home_dynamic::rn(w)!=29)w+=8u<<12;
    assert(big_folder_scale_site(large,BIG_VA,helpers,helper_va,preview,BASE_VA,moved,big_shape));
    large=original_large;helpers=saved_helpers;preview=saved_preview;

    // Reject corruption of every admitted dimension store and either semantic callee.
    unsigned mutations=0;
    for(size_t i=0;i<b.size();++i) {
        const auto w=b[i];
        if(home_dynamic::std_(w,1,0)||home_dynamic::std_(w,1,1)) {
            b[i]=0xd503201f; assert(!resolve()); b=original; ++mutations;
        }
    }
    assert(mutations==8);
    assert(!small_folder_scale_site(b,SMALL_VA,base,BASE_VA,CURRENT+4,PREVIEW,shape));
    assert(!small_folder_scale_site(b,SMALL_VA,base,BASE_VA,CURRENT,PREVIEW+4,shape));
    base[3]=0xd503201f;assert(!resolve());base=BASE;
    // Consistently relocated strategy fields remain supported.
    for(auto& w:b)if(home_dynamic::std_(w,1,0)||home_dynamic::std_(w,1,1))w+=8u<<12;
    assert(resolve());
    for(unsigned i=0;i<4;++i)assert(shape.dimensions[i]==resolved.dimensions[i]+8);
    b=original;
    // A duplicate producer cannot silently select one candidate.
    b.insert(b.end(),original.begin(),original.end());assert(!resolve());
    std::cout<<"FOLDER_SCALE_RESOLVER passed (7751, corrupt stores/callees/frame, relocation, ambiguity)\n";
}
'''
    replacements = {
        'SMALL_VA': str(fixture['small']['va']), 'BASE_VA': str(fixture['base']['va']),
        'BIG_VA': str(fixture['big']['va']), 'CURRENT': str(fixture['current']),
        'PREVIEW': str(fixture['preview']), 'SMALL': words('small'), 'BASE': words('base'),
        'BIG': words('big'), 'BIG_PREVIEW': words('big_preview'),
        'HELPER_WIDTH': words('helper_width'), 'HELPER_HEIGHT': words('helper_height'),
        'HELPER_WIDTH_VA': str(fixture['helper_width']['va']),
        'HELPER_HEIGHT_VA': str(fixture['helper_height']['va']),
    }
    for token in sorted(replacements, key=len, reverse=True):
        source = source.replace(token, replacements[token])
    (out / 'test.cpp').write_text(source)
    subprocess.run([shutil.which('c++'), '-std=c++20', '-I'+str(ROOT/'app/src/main/cpp'),
                    str(out/'test.cpp'), '-o', str(out/'test')], check=True)
    subprocess.run([str(out/'test')], check=True)

    assert args.ndk, 'Pass --ndk or set ANDROID_NDK_HOME for ARM64 execution'
    clang = next((args.ndk/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
    obj = out/'banks.o'
    subprocess.run([str(clang), '--target=aarch64-linux-android35',
                    '-I'+str(ROOT/'app/src/main/cpp'), '-c',
                    str(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),
                    '-o', str(obj)], check=True)
    sys.path.insert(0, str(ROOT/'tests/home-layout-native'))
    from dart_dump import Elf
    from capstone import Cs, CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN
    from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM
    from unicorn import arm64_const as A

    elf = Elf(obj)
    sym = elf.sections[elf.section_names['.symtab']]
    strings_section = elf.sections[sym[6]]
    strings = elf.data[strings_section[4]:strings_section[4]+strings_section[5]]
    symbols = []
    for at in range(sym[4], sym[4]+sym[5], 24):
        n, info, other, section, value, size = struct.unpack_from('<IBBHQQ', elf.data, at)
        symbols.append((strings[n:strings.find(b'\0', n)].decode(), section, value))
    text_index = elf.section_names['.text']
    section = elf.sections[text_index]
    code = bytearray(elf.data[section[4]:section[4]+section[5]])
    start = next(v for n, s, v in symbols if n=='hc_folder_icon_scale_entry')
    end = next(i.address+4 for i in Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN).disasm(code[start:], start)
               if i.mnemonic=='br')
    text_base, data, replay, stop, heap, stack = 0x100000, 0x300000, 0x400000, 0x401000, 0x500000, 0x608000
    names = ['hc_folder_icon_scale_enabled', 'hc_folder_icon_scale_caller',
             'hc_small_folder_scale_bits', 'hc_folder_icon_scale_fields', 'hc_folder_icon_scale_original',
             'hc_folder_icon_scale_big_caller', 'hc_folder_icon_scale_big_fields', 'hc_big_folder_scale_bits']
    targets = {n: data+i*128 for i, n in enumerate(names)}
    for sec in elf.sections.values():
        if sec[1]!=4 or sec[7]!=text_index:
            continue
        for at in range(sec[4], sec[4]+sec[5], 24):
            off, info, addend = struct.unpack_from('<QQq', elf.data, at)
            if not start<=off<end:
                continue
            name = symbols[info>>32][0]
            address, kind = targets[name]+addend, info&0xffffffff
            w = struct.unpack_from('<I', code, off)[0]
            if kind==275:
                pages = (address>>12)-((text_base+off)>>12)
                w = (w&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
            elif kind==277:
                w = (w&~(4095<<10))|((address&4095)<<10)
            elif kind==286:
                w = (w&~(4095<<10))|(((address&4095)//8)<<10)
            else:
                raise AssertionError((name, kind))
            struct.pack_into('<I', code, off, w)
    checks = 0
    for enabled in [0, 1]:
        for caller_kind in [0, 1, 2]:
            for scale in [0.59375, 0.6875, 1.0, 1.375]:
                u = Uc(UC_ARCH_ARM64, UC_MODE_ARM)
                for addr, size in [(text_base, (len(code)+4095)&~4095), (data, 4096),
                                   (replay, 8192), (heap, 4096), (0x600000, 65536), (0x700000, 65536)]:
                    u.mem_map(addr, size)
                u.mem_write(text_base, bytes(code))
                # Replay exactly the original base prologue, stopping before its fifth word.
                u.mem_write(replay, struct.pack('<4I', *[int(w, 16) for w in fixture['base']['words'][:4]]))
                caller = 0x12345678
                for name, value in [(names[0], enabled), (names[1], caller), (names[4], replay)]:
                    u.mem_write(targets[name], struct.pack('<Q', value))
                u.mem_write(targets[names[2]], struct.pack('<d', scale))
                u.mem_write(targets[names[7]], struct.pack('<d', 1.4 if scale==1.0 else 1.0))
                big_fields = [15, 23, 39, 31, 143, 95, 103, 111, 127, 79, 87, 119, 135]
                u.mem_write(targets[names[5]], struct.pack('<Q', caller+8))
                u.mem_write(targets[names[6]], struct.pack('<13q', *big_fields))
                fields = big_fields if caller_kind==2 else [15, 23, 39, 31]
                u.mem_write(targets[names[3]], struct.pack('<4q', 15, 23, 39, 31))
                owner = heap+1
                initial = bytearray([0x5a]*256)
                values = [64.0+i for i in range(len(fields))]
                for field, value in zip(fields, values):
                    struct.pack_into('<d', initial, 1+field, value)
                u.mem_write(heap, bytes(initial))
                regs = {i:0x10000+i*256 for i in range(31)}
                regs.update({1:owner, 15:stack, 30:caller if caller_kind==1 else caller+8 if caller_kind==2 else caller+4})
                for i, v in regs.items():u.reg_write(getattr(A, 'UC_ARM64_REG_X'+str(i)), v)
                simd = {i:((i+1)<<80)|i for i in range(32)}
                for i, v in simd.items():u.reg_write(getattr(A, 'UC_ARM64_REG_Q'+str(i)), v)
                u.reg_write(A.UC_ARM64_REG_SP, 0x708000)
                u.reg_write(A.UC_ARM64_REG_NZCV, 0xa0000000)
                u.emu_start(text_base+start, replay+16, count=500)
                assert u.reg_read(A.UC_ARM64_REG_PC)==replay+16
                expected = bytearray(initial)
                for field, value in zip(fields, values):
                    struct.pack_into('<d', expected, 1+field, value*((scale if caller_kind==1 else (1.4 if scale==1.0 else 1.0)) if enabled and caller_kind else 1))
                assert u.mem_read(heap, 256)==expected
                for i, v in regs.items():
                    if i not in [15, 16, 17, 29]:assert u.reg_read(getattr(A, 'UC_ARM64_REG_X'+str(i)))==v
                for i, v in simd.items():assert u.reg_read(getattr(A, 'UC_ARM64_REG_Q'+str(i)))==v
                assert u.reg_read(A.UC_ARM64_REG_X29)==stack-16
                assert u.reg_read(A.UC_ARM64_REG_X15)==stack-56
                assert u.reg_read(A.UC_ARM64_REG_SP)==0x708000
                assert u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
                checks += 1
    print(f'FOLDER_SCALE_ARM passed ({checks} cases, off/caller guard/shrink/default/enlarge, original prologue, registers/SIMD/flags)')
