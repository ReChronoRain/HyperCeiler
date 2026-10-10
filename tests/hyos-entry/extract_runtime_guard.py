#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Reproduce exact OS4.0.0.44/7719 runtime entry guards from supplied ELF files.

These constants identify one inspected ROM, not a general Rust ABI. This tool
does not execute, modify or load the supplied binaries. The default operation
compares the generated text against the production header; --write-header is
an explicit developer operation. --output must name a new evidence directory.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

SOURCE = Path(__file__).resolve().parents[2]
HEADER = SOURCE / "app/src/main/cpp/targets/hyos/hyos_runtime_guard.h"
EXPECTED = {
    "runtime": "9e3ff7028cfcc2db3ca050767061880f2369f979e540841429712b00d0a8b20d",
    "home": "4dc9fa82371dda8a0a8b9eff48301469d2f84752b52b71635b84e5fc0c47fa6e",
    "dart": "a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992",
}


class Elf:
    def __init__(self, path, expected):
        self.path = path.resolve()
        self.data = path.read_bytes()
        self.sha = hashlib.sha256(self.data).hexdigest()
        if self.sha != expected:
            raise ValueError(f"Unrecognized {path.name}: {self.sha}")
        if self.data[:6] != b"\x7fELF\x02\x01" or struct.unpack_from("<H", self.data, 18)[0] != 183:
            raise ValueError("Expected little endian AArch64 ELF64")
        phoff = struct.unpack_from("<Q", self.data, 32)[0]
        phsize, phcount = struct.unpack_from("<HH", self.data, 54)
        if phsize != 56 or phoff + phsize * phcount > len(self.data):
            raise ValueError("Malformed program headers")
        self.phdrs = [struct.unpack_from("<IIQQQQQQ", self.data, phoff + i*phsize)
                      for i in range(phcount)]
        shoff = struct.unpack_from("<Q", self.data, 40)[0]
        shsize, shcount, shnames = struct.unpack_from("<HHH", self.data, 58)
        if shsize != 64 or shnames >= shcount or shoff + shsize*shcount > len(self.data):
            raise ValueError("Malformed section headers")
        self.sections = [struct.unpack_from("<IIQQQQIIQQ", self.data, shoff+i*shsize)
                         for i in range(shcount)]
        raw = self.section(self.sections[shnames])
        self.named = {self.cstr(raw, s[0]): s for s in self.sections}

    @staticmethod
    def cstr(raw, offset):
        end = raw.index(b"\0", offset)
        return raw[offset:end].decode("utf-8")

    def section(self, s):
        if s[4] + s[5] > len(self.data):
            raise ValueError("Section outside file")
        return self.data[s[4]:s[4]+s[5]]

    def read(self, va, size, executable=False):
        for typ, flags, off, start, _, filesz, _, _ in self.phdrs:
            if typ == 1 and start <= va and va + size <= start + filesz:
                if executable and not flags & 1:
                    raise ValueError("Non-executable guard region")
                return self.data[off+va-start:off+va-start+size]
        raise ValueError(f"Unmapped file VA {va:x}/{size:x}")

    def symbol(self, name):
        tab = self.named[".dynsym"]
        strings = self.section(self.sections[tab[6]])
        found = []
        for n, info, _, section, value, size in struct.iter_unpack("<IBBHQQ", self.section(tab)):
            if n and section and info & 15 == 2 and self.cstr(strings,n) == name:
                found.append((value,size))
        if len(found) != 1:
            raise ValueError(f"Expected exactly one exported {name}, got {found}")
        return found[0]

    def note(self, va, build_id):
        s = self.named[".note.gnu.build-id"]
        raw = self.section(s)
        expected = struct.pack("<III", 4, 16, 3) + b"GNU\0" + bytes.fromhex(build_id)
        if s[3] != va or raw != expected or self.read(va,32) != expected:
            raise ValueError("Build-id note differs from inspected ROM")
        return raw


def branch_target(va, word):
    if word & 0xfc000000 != 0x94000000:
        raise ValueError("Expected direct AArch64 BL")
    imm = word & 0x3ffffff
    if imm & 0x2000000:
        imm -= 0x4000000
    return va + 4*imm


def array(name, raw, byte=False):
    size = 1 if byte else 4
    values = list(raw) if byte else [x[0] for x in struct.iter_unpack("<I", raw)]
    typ = "uint8_t" if byte else "uint32_t"
    width = 2 if byte else 8
    rows = ["    " + ", ".join(f"0x{x:0{width}x}" + ("" if byte else "u")
            for x in values[i:i+8]) + "," for i in range(0,len(values),8)]
    return f"inline constexpr std::array<{typ}, {len(raw)//size}> {name} = {{\n" + "\n".join(rows) + "\n};\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in EXPECTED:
        parser.add_argument("--" + name, required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--write-header", action="store_true")
    args = parser.parse_args()
    images = {name: Elf(getattr(args,name), sha) for name,sha in EXPECTED.items()}
    runtime, home, dart = (images[k] for k in ("runtime","home","dart"))
    if home.symbol("app_entry_point") != (0x9b544c,960):
        raise ValueError("Unexpected Home export address/size")
    if runtime.symbol("launch_main_thread") != (0x83cfc,144):
        raise ValueError("Unexpected startup export")
    if runtime.symbol("run_application") != (0x84b5c,552):
        raise ValueError("Unexpected alternate startup export")
    dynamic = struct.iter_unpack("<QQ",runtime.section(runtime.named[".dynamic"]))
    if any(tag in (12,25,27) for tag,_ in dynamic):
        raise ValueError("Unexpected runtime DT_INIT/DT_INIT_ARRAY")
    runtime_note = runtime.note(0x2c0,"a51654af714371c53ce8d5a8db9a513a")
    dart_note = dart.note(0x1c8,"4f1bdaed74ab646bde7d3ebcdf821948")
    if runtime.read(0x3984c,15) != b"app_entry_point":
        raise ValueError("Unexpected requested symbol literal")
    # Independently decode the concrete direct and indirect call instructions.
    for pc, target in ((0x63e4c,0x87c30),(0x63678,0x87bd0),
                       (0x83d10,0x623c0),(0x84bc0,0x623c0)):
        if branch_target(pc,struct.unpack("<I",runtime.read(pc,4))[0]) != target:
            raise ValueError(f"Call target changed at {pc:x}")
    if runtime.read(0x63ec8,4) != struct.pack("<I",0xd63f0320):
        raise ValueError("Entry call is not BLR x25")
    ranges = [
        ("kDlsymWords","runtime",0x63e0c,0xac),
        ("kEntryCallWords","runtime",0x63eb8,0x30),
        ("kHomeEntryWords","home",0x9b544c,960),
    ]
    text = """// SPDX-License-Identifier: AGPL-3.0-or-later
// Generated by tests/hyos-entry/extract_runtime_guard.py from the supplied
// popsicle OS4.0.0.44.XPBCNXM / MiuiHome 801027719 ELF files.
// Exact byte guards only. No masked instructions or future-version fallback.
// app_entry_point writes a 24-byte result via x8; preserve original x0/x8/LR.
#pragma once

#include <array>
#include <cstdint>

namespace hyos_guard {
inline constexpr uintptr_t kDlsymReturnVa = 0x63e50;
inline constexpr uintptr_t kEntryReturnVa = 0x63ecc;
inline constexpr uintptr_t kHomeEntryVa = 0x9b544c;
inline constexpr uintptr_t kDlsymWindowVa = 0x63e0c;
inline constexpr uintptr_t kEntryWindowVa = 0x63eb8;
inline constexpr uintptr_t kRuntimeNoteVa = 0x2c0;
inline constexpr uintptr_t kDartNoteVa = 0x1c8;
inline constexpr uintptr_t kEntryNameVa = 0x3984c;
inline constexpr char kEntryName[] = "app_entry_point";

"""
    text += array("kRuntimeBuildIdNote",runtime_note,True) + "\n"
    text += array("kDartBuildIdNote",dart_note,True) + "\n"
    records = []
    for name, image, va, size in ranges:
        raw = images[image].read(va,size,True)
        text += array(name,raw) + "\n"
        records.append({"constant":name,"image":image,"va":va,"size":size,
                        "sha256":hashlib.sha256(raw).hexdigest(),"bytes":raw.hex()})
    text += "} // namespace hyos_guard\n"
    if args.write_header:
        HEADER.write_text(text)
    elif HEADER.read_text() != text:
        raise ValueError("Production header does not match exact ROM extraction")
    args.output.mkdir(parents=True,exist_ok=False)
    for record in records:
        (args.output/(record["constant"]+".bin")).write_bytes(bytes.fromhex(record["bytes"]))
    report = {"status":"PASS","header":str(HEADER),
              "header_sha256":hashlib.sha256(text.encode()).hexdigest(),
              "extractor_sha256":hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
              "images":{n:{"path":str(e.path),"sha256":e.sha} for n,e in images.items()},
              "ranges":records,"runtime_note":runtime_note.hex(),"dart_note":dart_note.hex(),
              "assertions":["Exact full-image hashes recognized", "Export identity/VA/size checked",
                            "Runtime has no DT_INIT/DT_INIT_ARRAY", "Symbol literal checked",
                            "Dlsym and startup direct BL targets decoded", "Entry BLR register checked",
                            "Generated production header byte-for-byte equal to extracted guards"],
              "limitations":["Static ROM identity and ABI evidence; no runtime injection or device execution",
                             "No assertion that every DT_NEEDED dependency is uninitialized before entry",
                             "Whole image SHA validates provenance offline; runtime must validate notes and all guard ranges safely"]}
    (args.output/"report.json").write_text(json.dumps(report,indent=2)+"\n")
    print(f"PASS exact ROM guards: {HEADER}; report: {args.output/'report.json'}")


if __name__ == "__main__":
    main()
