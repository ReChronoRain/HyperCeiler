#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Emit an OFFLINE candidate Flutter guard; never edit a production header.

The caller boundary still requires a separate reviewed synchronization design.
An emitted guard does not make Binder/worker work safe inside every dlopen hook.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import sys

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
s = importlib.util.spec_from_file_location("runtime_extract",HERE/"extract_runtime_guard.py")
g=importlib.util.module_from_spec(s);s.loader.exec_module(g)
s = importlib.util.spec_from_file_location("fixture_symbols",HERE.parent/"home-layout-native/fixture_symbols.py")
elf=importlib.util.module_from_spec(s);s.loader.exec_module(elf)
EXPECTED_SHA="4eaa65d63731a5711d9ca05b35b493f4482ab4f5e31fa10c9057011a2deaa7cb"


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--flutter",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    f=g.Elf(a.flutter,EXPECTED_SHA)
    found=[(va,size) for va,size,name in elf.symbols(a.flutter)
           if name.startswith("_ZN7flutterL13SearchMappingE")]
    if found != [(0xe89a34,0x600)]:raise ValueError("SearchMapping identity/address/size differs")
    expected=struct.pack("<III",4,20,3)+b"GNU\0"+bytes.fromhex("4ae44cae83c5140c46697ecdf193338e17aa8668")
    sec=f.named[".note.gnu.build-id"]
    if sec[3]!=0x340 or f.section(sec)!=expected or f.read(0x340,36)!=expected:
        raise ValueError("Flutter build-id note differs")
    if g.branch_target(0xe89c48,struct.unpack("<I",f.read(0xe89c48,4,True))[0]) != 0x10b03a0:
        raise ValueError("Snapshot dlopen call differs")
    text="""// SPDX-License-Identifier: AGPL-3.0-or-later
// OFFLINE CANDIDATE ONLY. Not included by production or an APK build.
// Generated from popsicle OS4.0.0.44 libhyper_os_flutter.so by
// tests/hyos-entry/extract_flutter_guard.py. A synchronization design and
// independent regression are required before activating this boundary.
#pragma once
#include <array>
#include <cstdint>
namespace hyos_flutter_guard {
inline constexpr uintptr_t kNoteVa = 0x340;
inline constexpr uintptr_t kDlopenReturnVa = 0xe89c4c;
inline constexpr uintptr_t kSearchMappingVa = 0xe89a34;
inline constexpr uintptr_t kDlopenWindowVa = 0xe89c18;
"""
    text+=g.array("kBuildIdNote",expected,True)+"\n"
    records=[]
    for name,va,size in [("kSearchMappingWords",0xe89a34,0x600),("kDlopenWords",0xe89c18,0x3c)]:
        raw=f.read(va,size,True);text+=g.array(name,raw)+"\n"
        records.append({"name":name,"va":va,"size":size,"sha256":hashlib.sha256(raw).hexdigest()})
    text+="} // namespace hyos_flutter_guard\n"
    a.output.mkdir(parents=True,exist_ok=False)
    header=a.output/"hyos_flutter_guard.candidate.h"
    header.write_text(text)
    report={"status":"PASS_OFFLINE_CANDIDATE_ONLY","image":str(f.path),"image_sha256":f.sha,
            "header":str(header.resolve()),"header_sha256":hashlib.sha256(text.encode()).hexdigest(),
            "extractor_sha256":hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),"ranges":records,
            "limitations":["Production header untouched and candidate is not compiled into a module",
                           "No first-layout race fix, Binder-lock proof or real-device support claim"]}
    (a.output/"report.json").write_text(json.dumps(report,indent=2)+"\n")
    print("PASS offline Flutter candidate",header)


if __name__=="__main__":main()
