/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "targets/home/home_recent_cards_group_access.h"
#include <cassert>
#include <fstream>
using namespace home_layout::cards::sdk;
using Bytes=std::vector<std::byte>;
using Corpus=std::vector<Bytes>;
static Graph graph(const Corpus& files) {
    std::vector<Image> images;
    for(const auto& file:files){auto image=Image::parse(file);assert(image);images.push_back(std::move(*image));}
    return Graph(std::move(images));
}
static size_t offset(const Corpus& files,Address address) {
    const auto image=home_layout::elf_targets::parse(files[address.image]);assert(image);
    auto off=image->file_offset(address.va,4);assert(off);return *off;
}
static void write(Corpus& files,Address address,uint32_t word) {
    const auto at=offset(files,address);
    for(unsigned i=0;i<4;++i)files[address.image][at+i]=std::byte((word>>(i*8))&255);
}
[[maybe_unused]] static uint32_t shift_memory(uint32_t word,int delta) {
    if((word&0x3b000000u)==0x39000000u) {
        const unsigned width=(word&(1u<<26))&&((word>>23)&1)?16:1u<<(word>>30);
        assert(delta%width==0);const int imm=int((word>>10)&4095)+delta/int(width);assert(imm>=0&&imm<4096);
        return (word&~(4095u<<10))|(uint32_t(imm)<<10);
    }
    if((word&0x3a000000u)==0x28000000u) {
        const unsigned width=(word&(1u<<26))?4u<<((word>>30)&3):((word>>31)?8:4);
        assert(delta%width==0);const int imm=(int32_t(word<<10)>>25)+delta/int(width);assert(imm>=-64&&imm<=63);
        return (word&~(127u<<15))|((uint32_t(imm)&127)<<15);
    }
    if((word&0x3b200000u)==0x38000000u) {
        const int imm=(int32_t(word<<11)>>23)+delta;assert(imm>=-256&&imm<=255);
        return (word&~(511u<<12))|((uint32_t(imm)&511)<<12);
    }
    assert(false);return 0;
}
