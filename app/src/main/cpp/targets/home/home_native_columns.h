/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
namespace home_layout {
struct NativeColumns {
    uint64_t splice=0, width=0;
    uint32_t slot=0, frame=0, context=0, bytes=0, reg=0;
};
inline std::optional<NativeColumns> resolve_native_columns(
    std::span<const std::byte> raw,const elf_targets::Targets& targets) {
    auto im=elf_targets::parse(raw);if(!im)return {};
    auto fn=im->bounds(targets.grid_config_initializer+4);
    if(!fn||fn->begin!=targets.grid_config_initializer||fn->end-fn->begin>0x8000)return {};
    auto read=[&](uint64_t p){return im->word(p).value_or(0);};
    uint32_t frameWord=read(fn->begin);
    if((frameWord&0xffc003ffu)!=0xd10003ffu)return {};
    NativeColumns out;out.frame=(frameWord>>10)&4095;
    uint64_t heightCall=0,widthCall=0;unsigned heightHits=0,inputs=0,copies=0;
    for(uint64_t p=fn->begin;p+4<=fn->end;p+=4){
        auto t=nhk::arm64::branch_target(p,read(p),true);
        if(t&&*t==targets.grid_height){heightCall=p;++heightHits;}
    }
    if(heightHits!=1)return {};
    // AAPCS arguments: width(screen width,padding,columns). Its second use is on
    // the far side of the independently identified height computation.
    for(uint64_t p=fn->begin;p+24<=heightCall;p+=4){
        uint32_t a=read(p),b=read(p+4),c=read(p+8),d=read(p+12),e=read(p+16);
        if((a&0xffc003ffu)!=0xb94003e0u||(b&0xffc003ffu)!=0xb94003e1u||
            (c&0xffe0ffffu)!=0x2a0003e2u)continue;
        const unsigned reg=(c>>16)&31;if(reg<19||reg>28)continue;
        // Same column is logged with two neighbouring native integer scalars.
        if((d&0xffc003ffu)!=(0x290003a0u|reg)||
            (e&0xffe00fe0u)!=0xb80003a0u)continue;
        auto width=nhk::arm64::branch_target(p+20,read(p+20),true);if(!width)continue;
        unsigned after=0;
        for(uint64_t q=heightCall+4;q+4<=fn->end;q+=4){
            auto call=nhk::arm64::branch_target(q,read(q),true);if(call&&*call==*width)++after;
        }
        if(after!=1)continue;
        uint64_t load=0;
        for(uint64_t q=fn->begin;q<p+8;q+=4)
            if((read(q)&0xffc003ffu)==(0xb94003e0u|reg))load=q;
        if(!load)continue;
        bool clobber=false;
        for(uint64_t q=load+4;q<p+8;q+=4){auto w=read(q);
            const bool store=(w&0x0a000000u)==0x08000000u && !(w&(1u<<22));
            const bool branch=(w&0x7c000000u)==0x14000000u;
            if((w&31)==reg&&!store&&!branch)clobber=true;
        }
        if(clobber)continue;
        ++inputs;out.splice=load;out.slot=((read(load)>>10)&4095)*4;
        out.reg=reg;out.width=*width;widthCall=p;
        out.context=((a>>10)&4095)*4;
    }
    if(inputs!=1||!out.frame||out.slot+4>out.frame||out.slot<out.context)return {};
    // Prove the scalar belongs to the result context copied to the caller, not
    // a logger local: two memcpy-like calls transfer the same complete context.
    for(uint64_t p=heightCall+4;p+32<=fn->end;p+=4){
        uint32_t a=read(p),b=read(p+4),c=read(p+8),e=read(p+16),f=read(p+20),g=read(p+24);
        if((a&0xffc003ffu)!=0x910003e0u||(b&0xffc003ffu)!=0x910003e1u||
            (c&0xffe0001fu)!=0x52800002u||
            (e&0xffc003ffu)!=0xf94003e0u||(f&0xffc003ffu)!=0x910003e1u||g!=c)continue;
        auto one=nhk::arm64::branch_target(p+12,read(p+12),true);
        auto two=nhk::arm64::branch_target(p+28,read(p+28),true);
        uint32_t count=(c>>5)&65535;
        if(!one||!two||*one!=*two||((b>>10)&4095)!=out.context||
            ((f>>10)&4095)!=((a>>10)&4095)||count<32||count>512||
            out.slot+4>out.context+count||widthCall>=p)continue;
        ++copies;out.bytes=count;
    }
    if(copies!=1)return {};
    return out;
}
// saved is the complete 784-byte native bank save area. Only a scalar in the
// current native frame is changed; no Rust/Dart object, reference or heap write.
inline bool apply_native_columns(std::byte* saved,const NativeColumns& shape,uint32_t request) {
    const uint32_t columns=request>>8;
    if(!(request&1)||columns<1||columns>8||shape.slot+4>shape.frame||!shape.splice)return false;
    std::memcpy(saved+784+shape.slot,&columns,4);return true;
}
}
