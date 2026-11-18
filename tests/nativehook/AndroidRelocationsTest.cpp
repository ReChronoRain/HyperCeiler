/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../../app/src/main/cpp/nativehook/android_relocations.h"
#include <array>
#include <cassert>
#include <iostream>
#include <vector>
using Bytes=std::vector<std::byte>;
static void sleb(Bytes& out,int64_t value) {
    for(;;) {
        const auto byte=uint8_t(value&127);value>>=7;
        const bool done=(value==0&&!(byte&64))||(value==-1&&(byte&64));
        out.push_back(std::byte(byte|(done?0:128)));if(done)return;
    }
}
static Bytes frame(std::initializer_list<int64_t> values) {
    Bytes out{std::byte{'A'},std::byte{'P'},std::byte{'S'},std::byte{'2'}};
    for(auto value:values)sleb(out,value);return out;
}
int main() {
    const auto relr=[](std::initializer_list<uint64_t> words) {
        Bytes out;for(auto word:words)for(unsigned byte=0;byte<8;++byte)out.push_back(std::byte(word>>(byte*8)));return out;
    };
    std::vector<uint64_t> slots;
    const auto collect=[&](uint64_t slot){slots.push_back(slot);return true;};
    assert(nhk::elf::relative_relocation_offsets(relr({0x100,11,3,0x800}),collect));
    assert((slots==std::vector<uint64_t>{0x100,0x108,0x118,0x300,0x800}));
    slots.clear();assert(nhk::elf::relative_relocation_offsets(relr({0x100,1,3}),collect));
    assert((slots==std::vector<uint64_t>{0x100,0x300}));
    slots.clear();assert(nhk::elf::relative_relocation_offsets(relr({0x100,UINT64_C(0x8000000000000001)}),collect));
    assert((slots==std::vector<uint64_t>{0x100,0x2f8}));
    const auto packed=relr({0x100,3});
    for(size_t cut=1;cut<packed.size();++cut)if(cut%8)
        assert(!nhk::elf::relative_relocation_offsets(std::span(packed).first(cut),[](auto){return true;}));
    for(auto invalid:{relr({2}),relr({UINT64_MAX-7}),relr({UINT64_MAX-511,3,3})})
        assert(!nhk::elf::relative_relocation_offsets(invalid,[](auto){return true;}));
    assert(!nhk::elf::relative_relocation_offsets(packed,[](auto){return false;}));
    assert(nhk::elf::relative_relocation_offsets({},[](auto){return true;}));
    const auto bytes=frame({3,0,2,15,8,257,100,1,8,-8,1027,-25});
    std::vector<nhk::elf::AndroidRela> found;
    assert(nhk::elf::android_relocations(bytes,[&](auto item){found.push_back(item);return true;}));
    assert(found.size()==3&&found[0].offset==8&&found[1].offset==16&&found[2].offset==8);
    assert(found[0].info==257&&found[1].info==257&&found[2].info==1027);
    assert(found[0].addend==100&&found[1].addend==100&&found[2].addend==75);
    // The grouped addend is applied once. A later non-addend group resets it.
    found.clear();const auto reset=frame({2,-8,1,15,16,257,200,1,3,8,257});
    assert(nhk::elf::android_relocations(reset,[&](auto item){found.push_back(item);return true;}));
    assert(found[0].offset==8&&found[0].addend==200&&found[1].offset==16&&!found[1].addend);
    for(size_t cut=0;cut<bytes.size();++cut)
        assert(!nhk::elf::android_relocations(std::span(bytes).first(cut),[](auto){return true;}));
    for(auto invalid:{frame({1,0,0,0}),frame({1,0,2,0}),frame({1,0,1,16}),frame({1,0,1,4}),
        frame({1048577,0}),frame({1,INT64_MAX,1,3,1,257}),frame({1,0,1,3,-1,257}),frame({1,0,1,3,8,-1})})
        assert(!nhk::elf::android_relocations(invalid,[](auto){return true;}));
    auto overflow=frame({1,0});for(unsigned i=0;i<10;++i)overflow.push_back(std::byte{0x81});
    assert(!nhk::elf::android_relocations(overflow,[](auto){return true;}));
    assert(!nhk::elf::android_relocations(bytes,[](auto){return false;}));
    const auto extreme=frame({2,0,1,15,8,257,INT64_MIN,1,15,8,257,INT64_MAX});
    found.clear();assert(nhk::elf::android_relocations(extreme,[&](auto item){found.push_back(item);return true;}));
    assert(found[0].addend==INT64_MIN&&found[1].addend==-1);
    std::cout<<"PASS APS2 grouped/per-item relocation state, signed deltas, all truncations and malformed/overflow refusal\n";
    std::cout<<"PASS ELF64 RELR addresses, consecutive/empty/high-bit bitmaps, truncation, alignment and overflow refusal\n";
}
