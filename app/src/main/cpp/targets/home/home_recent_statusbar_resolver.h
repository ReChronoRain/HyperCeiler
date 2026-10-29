/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include "home_feature_code.h"

namespace home_recent_bar {
struct Target { uint64_t entry=0,flag=0; };
struct Targets { std::array<Target,3> values{}; };
inline bool writable_flag(const home_layout::elf_targets::Image& image,uint64_t va){
    unsigned count=0;
    for(const auto& p:image.segments)if(p.type==1&&(p.flags&2)&&!(p.flags&1)
        &&va>=p.vaddr&&va-p.vaddr<p.memsz)++count;
    return count==1;
}
// Log text identifies the scene, actual producer/store/CFG proves its bool ABI.
// No section, .gnu_debugdata, Rust symbol name, RVA or build profile is used.
inline std::optional<Targets> resolve(std::span<const std::byte> bytes){
    using namespace home_layout::elf_targets;
    const auto image=parse(bytes);if(!image)return {};
    constexpr const char* labels[]={"set_is_in_portrait_overview: value=",
        "set_is_in_landscape_overview: value=","set_recents_window_showing: "};
    Targets out;
    for(unsigned k=0;k<3;++k){
        auto text=unique_literal(*image,labels[k]);auto f=text?unique_function(*image,*text):std::nullopt;
        if(!f||f->end-f->begin<0x60||f->end-f->begin>0x300)return {};
        unsigned executable=0;
        for(const auto& p:image->segments)if(p.type==1 && (p.flags&1) && !(p.flags&2)
            && f->begin>=p.vaddr && f->end>=f->begin && f->end-p.vaddr<=p.filesz)++executable;
        if(executable!=1)return {};
        home_dynamic::Body body;for(auto pc=f->begin;pc<f->end;pc+=4){auto w=image->word(pc);if(!w)return {};body.push_back(*w);}
        // Verified AAPCS64 Rust bool argument W0, normal SP frame, return X30.
        if((body[0]&0xffc003ffu)!=0xd10003ffu||body.back()!=0xd65f03c0u)return {};
        unsigned input=32,moves=0;size_t input_at=0;
        for(size_t i=0;i<std::min(size_t(16),body.size());++i)
            if((body[i]&0xffffffe0u)==0x2a0003e0u){input=body[i]&31;input_at=i;++moves;}
        if(moves!=1||input==31)return {};
        uint64_t flag=0;size_t store=0;unsigned hits=0;
        if(k<2){
            if(input<19||input>28)return {}; // Preserved through the optional logger.
            for(size_t i=1;i<body.size();++i){
                const auto w=body[i];
                if((w&0xffc0001fu)!=(0x39000000u|input))continue;
                auto page=nhk::arm64::decode_adrp(f->begin+(i-1)*4,body[i-1]);
                if(!page||(body[i-1]&31)!=((w>>5)&31))continue;
                uint64_t at=*page+((w>>10)&4095);
                if(!writable_flag(*image,at))continue;flag=at;store=i;++hits;
            }
        }else{
            for(size_t i=0;i+6<body.size();++i){
                const std::array<uint32_t,2> pair{body[i],body[i+1]};
                auto address=nhk::arm64::decode_address_pair(pair,0,f->begin+i*4);
                if(!address||!writable_flag(*image,*address))continue;
                const auto base=body[i+1]&31;
                for(size_t j=i+2;j<std::min(i+7,body.size()-2);++j){
                    const auto load=body[j],write=body[j+1],retry=body[j+2];
                    if((load&0xfffffc00u)!=0x085f7c00u||(write&0xffe0fc00u)!=0x08007c00u
                        ||((load>>5)&31)!=base||((write>>5)&31)!=base||(write&31)!=input
                        ||(retry&0xff000000u)!=0x35000000u||(retry&31)!=((write>>16)&31))continue;
                    const int32_t delta=int32_t(retry<<8)>>13;
                    if(int64_t(j+2)+delta!=int64_t(j))continue;
                    flag=*address;store=j+1;++hits;
                }
            }
        }
        if(hits!=1||input_at>=store
            ||!home_layout::features::passes_return_points(body,std::span(&input_at,1))
            ||!home_layout::features::passes_return_points(body,std::span(&store,1)))return {};
        // Prove the saved bool reaches the store unchanged. Low registers cannot
        // survive a call; preserved registers must not be overwritten locally.
        for(size_t i=input_at+1;i<store;++i){
            const auto w=body[i];
            if((w&0xfc000000u)==0x94000000u||(w&0xfffffc1fu)==0xd63f0000u){if(input<19)return {};continue;}
            const bool pair=(w&0x3b000000u)==0x29000000u;
            if(pair && (w&0x00400000u) && (((w>>10)&31)==input))return {};
            if((w&31)!=input)continue;
            const bool scalar_store=(((w&0x3b000000u)==0x39000000u)
                ||((w&0x3b200000u)==0x38000000u)) && !(w&0x00400000u);
            if(scalar_store || (pair&&!(w&0x00400000u)))continue;
            // Direct/conditional branches and tests do not define their low bits as Rd.
            if((w&0x7c000000u)==0x14000000u||(w&0xff000010u)==0x54000000u
                ||(w&0x7e000000u)==0x34000000u||(w&0x7e000000u)==0x36000000u)continue;
            return {};
        }
        out.values[k]={f->begin,flag};
    }
    for(unsigned i=0;i<3;++i)for(unsigned j=i+1;j<3;++j)
        if(out.values[i].entry==out.values[j].entry||out.values[i].flag==out.values[j].flag)return {};
    return out;
}
}
