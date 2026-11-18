/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstddef>
#include <bit>
#include <cstdint>
#include <limits>
#include <optional>
#include <span>

namespace nhk::elf {
// ELF64 RELR, including Android's original dynamic-tag aliases. Entries are
// addresses or bitmaps; this reader enumerates slots without applying writes.
// Format: AOSP bionic/linker/linker.cpp, relocate_relr.
template<class Visitor>
bool relative_relocation_offsets(std::span<const std::byte> bytes,Visitor visit) {
    if(bytes.size()%8||bytes.size()/8>1048576)return false;
    uint64_t cursor=0;size_t emitted=0;
    for(size_t at=0;at<bytes.size();at+=8) {
        uint64_t word=0;
        for(unsigned byte=0;byte<8;++byte)word|=uint64_t(std::to_integer<unsigned>(bytes[at+byte]))<<(byte*8);
        if(!(word&1)) {
            if(word%8||word>UINT64_MAX-8||++emitted>1048576||!visit(word))return false;
            cursor=word+8;continue;
        }
        if(cursor>UINT64_MAX-63*8)return false;
        auto bits=word>>1;
        while(bits) {
            const auto bit=std::countr_zero(bits);
            if(++emitted>1048576||!visit(cursor+uint64_t(bit)*8))return false;
            bits&=bits-1;
        }
        cursor+=63*8;
    }
    return true;
}
struct AndroidRela {uint64_t offset=0,info=0;int64_t addend=0;};
// APS2 is Android's ELF relocation format, not a vendor address signature.
// Format reference: AOSP bionic/linker/linker_reloc_iterators.h. In particular,
// a grouped addend delta is applied ONCE per group, not once per relocation.
template<class Visitor>
bool android_relocations(std::span<const std::byte> bytes,Visitor visit) {
    if(bytes.size()<4||bytes[0]!=std::byte{'A'}||bytes[1]!=std::byte{'P'}
        ||bytes[2]!=std::byte{'S'}||bytes[3]!=std::byte{'2'})return false;
    size_t cursor=4;
    const auto read=[&]()->std::optional<int64_t> {
        uint64_t value=0;
        for(unsigned index=0;index<10;++index) {
            if(cursor>=bytes.size())return {};
            const auto byte=std::to_integer<unsigned>(bytes[cursor++]);
            const auto payload=byte&127u;
            if(index==9&&(payload!=0&&payload!=127))return {};
            value|=uint64_t(payload)<<(index*7);
            if(!(byte&128u)) {
                const unsigned bits=(index+1)*7;
                if(bits<64&&(byte&64u))value|=~uint64_t(0)<<bits;
                return static_cast<int64_t>(value);
            }
        }
        return {};
    };
    const auto count=read(),initial=read();
    // Work budget for untrusted image metadata, independent of any SDK ABI.
    if(!count||!initial||*count<0||*count>1048576)return false;
    int64_t offset=*initial,addend=0;uint64_t info=0;
    const auto add=[](int64_t& value,int64_t delta) {
        if((delta>0&&value>std::numeric_limits<int64_t>::max()-delta)
            ||(delta<0&&value<std::numeric_limits<int64_t>::min()-delta))return false;
        value+=delta;return true;
    };
    for(int64_t emitted=0;emitted<*count;) {
        const auto size=read(),flags=read();
        if(!size||!flags||*size<=0||*size>*count-emitted||*flags<0||(*flags&~15)
            ||((*flags&4)&&!(*flags&8)))return false;
        int64_t delta=0;
        if(*flags&2){const auto value=read();if(!value)return false;delta=*value;}
        if(*flags&1){const auto value=read();if(!value||*value<0)return false;info=static_cast<uint64_t>(*value);}
        if((*flags&12)==12){const auto value=read();if(!value||!add(addend,*value))return false;}
        else if(!(*flags&8))addend=0;
        for(int64_t item=0;item<*size;++item) {
            if(!(*flags&2)){const auto value=read();if(!value)return false;delta=*value;}
            if(!add(offset,delta)||offset<0)return false;
            if(!(*flags&1)){const auto value=read();if(!value||*value<0)return false;info=static_cast<uint64_t>(*value);}
            if((*flags&12)==8){const auto value=read();if(!value||!add(addend,*value))return false;}
            if(!visit(AndroidRela{static_cast<uint64_t>(offset),info,addend}))return false;
        }
        emitted+=*size;
    }
    return true;
}
}
