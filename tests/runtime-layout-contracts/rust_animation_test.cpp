/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_layout_elf_targets.h"
#include <cassert>
#include <fstream>
#include <iterator>
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==2);std::ifstream file(argv[1],std::ios::binary);assert(file);
    const std::string raw(std::istreambuf_iterator<char>{file},{});
    std::vector<std::byte> bytes(raw.size());std::memcpy(bytes.data(),raw.data(),raw.size());
    const auto owner=home_layout::elf_targets::resolve_animation_duration_update(bytes);assert(owner);
    const auto image=home_layout::elf_targets::parse(bytes);assert(image);
    const auto bounds=image->bounds(*owner);assert(bounds&&bounds->begin==owner);
    unsigned stores=0;size_t offset=0;
    for(uint64_t at=bounds->begin;at<bounds->end;at+=4) {
        const auto word=image->word(at);assert(word);
        if((*word&0xffc003ffu)==0xbd000260u) {offset=*image->file_offset(at,4);++stores;}
    }
    assert(stores==1);
    const auto store=nhk::load_le32(bytes.data()+offset);
    const auto put=[&](uint32_t word){for(unsigned i=0;i<4;++i)bytes[offset+i]=std::byte((word>>(i*8))&255);};
    put((store&~0x003ffc00u)|(19<<10)); // receiver field relocates to byte 76
    assert(home_layout::elf_targets::resolve_animation_duration_update(bytes)==owner);
    put((store&~0x003ffc00u)|(1<<10)); // field overlaps object header
    assert(!home_layout::elf_targets::resolve_animation_duration_update(bytes));
    put(0xd503201fu); // semantic store missing, literal still present
    assert(!home_layout::elf_targets::resolve_animation_duration_update(bytes));
    std::cout<<"Rust animation owner: original ELF / moved state field / invalid and missing stores: PASS\n";
}
