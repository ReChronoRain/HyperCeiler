#!/usr/bin/env python3
"""Exercise production APK image discovery with controlled maps/linker/live-read boundaries."""
import argparse
import struct
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--launcher-apk', type=Path, help='optional real arm64 launcher APK')
args = parser.parse_args()
with tempfile.TemporaryDirectory(prefix='hc-hyos-inventory-') as folder:
    temp = Path(folder)
    for name in ('android', 'sys', 'nativehook'):
        (temp/name).mkdir()
    (temp/'android/log.h').write_text('''#pragma once
enum {ANDROID_LOG_INFO=4,ANDROID_LOG_WARN=5,ANDROID_LOG_ERROR=6};
inline int __android_log_print(int,const char*,const char*,...){return 0;}
''')
    (temp/'sys/sysmacros.h').write_text('#pragma once\n#include <sys/types.h>\n')
    (temp/'link.h').write_text('''#pragma once
#include <elf.h>
struct dl_phdr_info {uintptr_t dlpi_addr;const char*dlpi_name;const Elf64_Phdr*dlpi_phdr;Elf64_Half dlpi_phnum;};
inline int dl_iterate_phdr(int(*)(dl_phdr_info*,size_t,void*),void*){return 0;}
''')
    (temp/'nativehook/memory_io.h').write_text('''#pragma once
#include <span>
namespace nhk {bool safe_read(uintptr_t,std::span<std::byte>);}
''')
    # macOS has no elf.h; provide only the ABI structures consumed by image.cpp.
    if not Path('/usr/include/elf.h').exists():
        (temp/'elf.h').write_text('''#pragma once
#include <stdint.h>
typedef uint16_t Elf64_Half;typedef uint32_t Elf64_Word;typedef uint64_t Elf64_Addr;typedef uint64_t Elf64_Off;typedef uint64_t Elf64_Xword;
struct Elf64_Ehdr {unsigned char e_ident[16];Elf64_Half e_type,e_machine;Elf64_Word e_version;Elf64_Addr e_entry;Elf64_Off e_phoff,e_shoff;Elf64_Word e_flags;Elf64_Half e_ehsize,e_phentsize,e_phnum,e_shentsize,e_shnum,e_shstrndx;};
struct Elf64_Phdr {Elf64_Word p_type,p_flags;Elf64_Off p_offset;Elf64_Addr p_vaddr,p_paddr;Elf64_Xword p_filesz,p_memsz,p_align;};
#define ELFMAG "\\177ELF"
#define SELFMAG 4
#define EI_CLASS 4
#define EI_DATA 5
#define EI_VERSION 6
#define ELFCLASS64 2
#define ELFDATA2LSB 1
#define EV_CURRENT 1
#define ET_DYN 3
#define EM_AARCH64 183
#define PT_LOAD 1
#define PF_R 4
#define PF_W 2
#define PF_X 1
''')
    for page in (4096, 16384):
        size = 130*page
        image = bytearray(size)
        image[:16] = b'\x7fELF\x02\x01\x01'+b'\0'*9
        struct.pack_into('<HHIQQQIHHHHHH', image, 16, 3, 183, 1, 0, 64, 0, 0, 64, 56, 3, 0, 0, 0)
        for i, (offset, length, flags) in enumerate(((0,page,4),(page,128*page,5),(129*page,page,6))):
            struct.pack_into('<IIQQQQQQ', image, 64+56*i, 1, flags, offset, offset, 0, length, length, page)
        filename = 'lib/arm64-v8a/libapp.so'
        apk = temp/f'home-{page}.apk'
        with zipfile.ZipFile(apk, 'w', compression=zipfile.ZIP_STORED) as z:
            info = zipfile.ZipInfo(filename)
            padding = (-(30+len(filename))) % page
            info.extra = struct.pack('<HH', 0xffff, padding-4)+b'\0'*(padding-4)
            z.writestr(info, image)
        (temp/f'second-{page}.apk').write_bytes(apk.read_bytes())
        for i in range(80):
            with zipfile.ZipFile(temp/f'other-{i}.apk','w'):
                pass
        command = ['c++','-std=c++20','-Wall','-Wextra','-Werror',f'-DNHK_HOST_PAGE_SIZE={page}',
                   '-I'+str(temp),'-I'+str(ROOT/'app/src/main/cpp'),
                   str(Path(__file__).with_name('test.cpp')),'-o',str(temp/'test')]
        subprocess.run(command, check=True)
        subprocess.run([str(temp/'test'),str(temp),str(page)], check=True)
        if args.launcher_apk:
            command[-3:] = [str(Path(__file__).with_name('real_image.cpp')), '-o', str(temp/'real-test')]
            subprocess.run(command, check=True)
            subprocess.run([str(temp/'real-test'), str(args.launcher_apk.resolve())], check=True)
