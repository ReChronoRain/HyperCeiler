#pragma once
#include <cassert>
#include <cstring>
#include <map>
#include <vector>
using namespace home_layout;
using namespace home_layout::features;
using Image=std::vector<std::byte>;
using Functions=std::vector<Function>;

static Image image(Functions functions) {
    assert(!functions.empty());
    std::sort(functions.begin(),functions.end(),[](auto& a,auto& b){return a.va<b.va;});
    const uint32_t begin=functions.front().va&~4095u;
    const uint32_t end=(functions.back().va+functions.back().words.size()*4+4095)&~4095u;
    Image out(4096+end-begin);
    auto put=[&](size_t at,auto value){std::memcpy(out.data()+at,&value,sizeof(value));};
    const unsigned char ident[]={0x7f,'E','L','F',2,1,1};
    std::memcpy(out.data(),ident,sizeof(ident));
    put(16,uint16_t{3});put(18,uint16_t{183});put(20,uint32_t{1});put(32,uint64_t{64});
    put(52,uint16_t{64});put(54,uint16_t{56});put(56,uint16_t{1});
    put(64,uint32_t{1});put(68,uint32_t{5});put(72,uint64_t{4096});
    put(80,uint64_t{begin});put(88,uint64_t{begin});put(96,uint64_t{end-begin});
    put(104,uint64_t{end-begin});put(112,uint64_t{4096});
    for(size_t i=0;i<functions.size();++i) {
        const auto& f=functions[i];
        std::memcpy(out.data()+4096+f.va-begin,f.words.data(),f.words.size()*4);
        const uint64_t finish=uint64_t(f.va)+f.words.size()*4;
        const uint64_t next=i+1<functions.size()?functions[i+1].va:end;
        // Sparse fixtures omit neighbouring functions. Supply a distinct
        // (non-matching) framed neighbour rather than inventing symbol bounds.
        if(finish+8<=next) {
            const uint32_t sentinel[]={0xa9bf79fdu,0xaa0f03fdu};
            std::memcpy(out.data()+4096+finish-begin,sentinel,sizeof(sentinel));
        }
    }
    return out;
}
static uint32_t relocate(uint32_t old,const std::map<uint32_t,uint32_t>& map) {
    const auto i=map.find(old);return i==map.end()?old:i->second;
}
static Functions relocated(const Functions& original,std::map<uint32_t,uint32_t>& addresses) {
    // Every owning function moves independently, in reverse address order. Each
    // BL is re-encoded from its current call PC to its actual destination.
    for(size_t i=0;i<original.size();++i)addresses[original[i].va]=0x05000000u+uint32_t((original.size()-i)*0x2000);
    Functions out=original;
    for(auto& f:out){const uint32_t previous=f.va;f.va=addresses.at(previous);
        for(size_t i=0;i<f.words.size();++i){uint32_t to=0;
            if(!home_dynamic::target(f.words[i],previous+uint32_t(i*4),to))continue;
            const int64_t delta=int64_t(relocate(to,addresses))-(f.va+i*4);
            assert(!(delta&3)&&delta>=-(1ll<<27)&&delta<(1ll<<27));
            f.words[i]=0x94000000u|(uint32_t(delta/4)&0x03ffffffu);
        }
    }
    return out;
}
