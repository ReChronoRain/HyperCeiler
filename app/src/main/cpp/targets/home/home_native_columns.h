/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include "home_dynamic_layout.h"
namespace home_layout {
// PhoneRowColHandler converges its map/default arms at this scalar argument bank.
// Override the argument, not only the two fallback constants. The zero-row
// bypass arm remains stock; the phone arms explicitly carry raw row=-1.
struct DartColumns {uint32_t splice=0,field=0;int32_t frame=0;};
inline std::optional<DartColumns> resolve_dart_columns(
    const home_dynamic::Body& b,uint32_t owner,uint32_t copy_with) {
    using namespace home_dynamic;
    if(!owner||!copy_with||b.size()<16)return {};
    unsigned hits=0;DartColumns found;
    for(size_t i=4;i+7<b.size();++i){
        if(!ldw(b[i],0,2)||b[i+1]!=0xd37ff823u||b[i+2]!=0xa90009e3u
            ||!ldx(b[i+3],29,1)||!slot(imm9(b[i+3])))continue;
        uint32_t call=0;
        if(!target(b[i+6],owner+uint32_t((i+6)*4),call)||call!=copy_with)continue;
        // The copyWith named-argument descriptor is loaded into x4, not a second call.
        if((b[i+4]&0xffc003ffu)!=0x91400364u
            ||(b[i+5]&0xffc003ffu)!=0xf9400084u)continue;
        if(b[i-4]!=0x92800001u||b[i-2]!=0xaa0203e0u||b[i-1]!=0xd2800001u)continue;
        auto branch=nhk::arm64::branch_target(owner+uint32_t((i-3)*4),b[i-3],false);
        if(!branch||*branch!=owner+i*4)continue;
        const int32_t off=imm9(b[i]);if(!field(off))continue;
        unsigned writes=0;
        for(size_t q=0;q+1<i;++q)
            if((b[q]&0xffe00000u)==0xd2800000u && (b[q+1]&0xffe00fe0u)==0xb8000000u
                && rd(b[q])==rd(b[q+1])&&imm9(b[q+1])==off
                &&zimm(b[q])>=2&&zimm(b[q])<=16&&!(zimm(b[q])&1))++writes;
        if(writes!=2)continue;
        ++hits;found={uint32_t(i*4),uint32_t(off),imm9(b[i+3])};
    }
    return hits==1?std::optional<DartColumns>(found):std::nullopt;
}
// The original persistence boundary writes two currentConfig integer fields.
// A module override is transient: it must not replace the launcher's own defaults.
inline bool grid_persist_boundary(const home_dynamic::Body& b,uint32_t owner,
        uint32_t current,uint32_t put_int) {
    using namespace home_dynamic;
    if(b.size()<16||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu
        ||(b[2]&0xffc003ffu)!=0xd10001efu||!stx(b[3],29,1)||!slot(imm9(b[3])))return false;
    unsigned writes=0,reads=0,returns=0;int32_t first=-1;
    for(size_t i=0;i<b.size();++i){
        uint32_t call=0;
        if(target(b[i],owner+uint32_t(i*4),call)){
            if(call==current)++reads;
            if(call==put_int){
                if(i<4||!target(b[i-4],owner+uint32_t((i-4)*4),call)||call!=current
                    ||!ldx(b[i-3],0,2)||(imm9(b[i-3])&7)!=3)return false;
                // Named keys come from the original pool, never a guessed preference name.
                if((b[i-2]&0xffc003ffu)!=0x91400361u||(b[i-1]&0xffc003ffu)!=0xf9400021u)return false;
                if(!writes)first=imm9(b[i-3]);else if(imm9(b[i-3])!=first+8)return false;
                ++writes;
            }
        }
        if(b[i]==0xd65f03c0u){
            if(i<3||b[i-3]!=0xaa1603e0u||b[i-2]!=0xaa1d03efu||b[i-1]!=0xa8c179fdu)return false;
            ++returns;
        }
    }
    return writes==2&&reads==2&&returns>=2;
}
// saveAndUpdateLayoutType also updates the live native integer. Recreate only
// that original scalar store while suppressing its persistent default write.
inline std::optional<int32_t> grid_layout_save_field(const home_dynamic::Body& b,
        uint32_t owner,uint32_t put_int) {
    using namespace home_dynamic;
    if(b.size()<20||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu
        ||(b[2]&0xffc003ffu)!=0xd10001efu||!stx(b[3],29,1)
        ||!slot(imm9(b[3]))||!stx(b[4],29,2)||!slot(imm9(b[4])))return {};
    unsigned writes=0,returns=0;int32_t field_off=0;
    for(size_t i=0;i<b.size();++i){
        uint32_t call=0;
        if(target(b[i],owner+uint32_t(i*4),call)&&call==put_int){
            if(i<10||i+4>=b.size()||!ldx(b[i-4],29,0)
                ||imm9(b[i-4])!=imm9(b[3])||!stx(b[i-3],0,2)
                ||(imm9(b[i-3])&7)!=7||imm9(b[i-3])<=0
                ||(b[i-2]&0xffc003ffu)!=0x91400361u
                ||(b[i-1]&0xffc003ffu)!=0xf9400021u
                ||b[i-7]!=0x93417c22u||b[i-6]!=0x36000041u
                ||b[i-5]!=0xf8407022u||!ldx(b[i-10],29,0)||!slot(imm9(b[i-10]))||!ldw(b[i-9],0,1)
                ||b[i-8]!=0x8b1c8021u)return {};
            // Trace the saved raw x2 parameter through its original Smi/Mint
            // boxing into this exact closure field, then back through unboxing.
            // Without that proof an entry shortcut must not synthesize a store.
            unsigned inputs=0;
            for(size_t q=5;q+6<i-9;++q){
                uint32_t allocator=0;
                const int32_t rel=int32_t(b[q+3]<<8)>>13;
                const uint32_t join=owner+uint32_t((q+3)*4)+uint32_t(rel*4);
                if(!stx(b[q-2],29,3)||imm9(b[q-2])!=imm9(b[i-10])
                    ||!target(b[q+4],owner+uint32_t((q+4)*4),allocator)
                    ||!ldx(b[q],29,4)||imm9(b[q])!=imm9(b[4])
                    ||b[q+1]!=0x937f7880u||b[q+2]!=0xeb80049fu
                    ||(b[q+3]&0xff00001fu)!=0x54000000u
                    ||join!=owner+(q+6)*4||!stx(b[q+5],0,4)
                    ||imm9(b[q+5])!=7||(b[q+6]&0xffe00fffu)!=0xb8000060u
                    ||imm9(b[q+6])!=imm9(b[i-9]))continue;
                ++inputs;
            }
            if(inputs!=1)return {};
            field_off=imm9(b[i-3]);++writes;
        }
        if(b[i]==0xd65f03c0u){
            if(i<3||b[i-3]!=0xaa1603e0u||b[i-2]!=0xaa1d03efu||b[i-1]!=0xa8c179fdu)return {};
            ++returns;
        }
    }
    return writes==1&&returns==1?std::optional<int32_t>(field_off):std::nullopt;
}
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
