/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
#include <set>
#include "home_recent_feature_resolver.h"
namespace home_layout::features::drawer {
using namespace home_dynamic;
struct Plan {uint32_t owner=0,site=0,width=0,caller=0,closure=0,empty_site=0;int32_t child_frame=0;uint32_t holder=0,blank_factory=0,bottom=0,content=0,color=0,context_allocator=0;};
inline std::optional<Plan> resolve(const Code& code) {
    std::optional<Plan> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        int32_t a=0,c=0,d=0;
        if(!receiver_slot(b,1,a)||!receiver_slot(b,2,c)||!receiver_slot(b,3,d)||a==c||a==d||c==d)continue;
        unsigned contexts=0;uint32_t context_allocator=0;
        for(size_t j=0;j+1<b.size();++j) {
            uint32_t allocator=0;
            if(movz(b[j],1)&&zimm(b[j])==4&&target(b[j+1],f.va+uint32_t((j+1)*4),allocator)){++contexts;context_allocator=allocator;}
        }
        if(contexts!=1)continue;
        for(size_t i=1;i+12<b.size();++i) {
            uint32_t width=0;Body source;
            if(!target(b[i],f.va+uint32_t(i*4),width)||!code.body(width,source)||!frame_size(source)
                ||b[i+1]!=0xa9460740u||b[i+2]!=0x91004000u||b[i+3]!=0xeb00003fu
                ||(b[i+4]&0xff00001fu)!=0x54000009u||b[i+5]!=0xf9003340u
                ||b[i+6]!=0xd1003c00u||!movz(b[i+7],1)||(b[i+8]&0xffe0001fu)!=0xf2a00001u
                ||b[i+9]!=0xf81ff001u||!std_(b[i+10],0,0)||!field(imm9(b[i+10]))
                ||!ldx(b[i+11],29,2)||!frame_slot(b,imm9(b[i+11]))
                ||!mem(b[i+12],0xb8000000u,2,0)||!field(imm9(b[i+12])))continue;
            // Numeric boxing ABI: allocation + publication must agree. The
            // actual class tag is decoded; it is never a target-selection RVA.
            const uint32_t tag=zimm(b[i+7])|(zimm(b[i+8])<<16);
            if(((tag>>8)&15)*16!=16)continue;
            unsigned siblings=0,callers=0;uint32_t caller=0;
            for(const auto& other:code.functions()) {
                const auto& w=other.words;
                for(size_t k=0;k+5<w.size();++k) {
                    uint32_t to=0;
                    if(!target(w[k],other.va+uint32_t(k*4),to))continue;
                    if(to==width&&other.va!=f.va&&std_(w[k+1],29,0)&&frame_slot(w,imm9(w[k+1])))++siblings;
                    if(to==f.va&&stx(w[k+1],29,0)&&frame_slot(w,imm9(w[k+1]))
                        &&(w[k+2]&0xfc000000u)==0x94000000u&&w[k+3]==0xaa0003e1u
                        &&ldx(w[k+4],29,0)&&imm9(w[k+4])==imm9(w[k+1])
                        &&mem(w[k+5],0xb8000000u,1,0)&&field(imm9(w[k+5]))) {++callers;caller=other.va;}
                }
            }
            if(!siblings||callers!=1)continue;
            if(result)return {};
            result=Plan{f.va,f.va+uint32_t((i+1)*4),width,caller};result->context_allocator=context_allocator;
        }
    }
    if(!result)return {};
    Body producer;if(!code.body(result->owner,producer))return {};
    const auto capture_width=imm9(producer[(result->site-result->owner)/4+11]);
    unsigned terminals=0;
    for(const auto& f:code.functions()) {
        const auto& w=f.words;if(!frame_size(w)||w.size()<12)continue;
        // Verified closure ABI: its context arrives via the closure argument.
        int32_t context=0;unsigned roots=0;
        for(size_t j=0;j+3<w.size();++j)
            if(ux(w[j],29,0)&&uoff(w[j])==16&&ldw(w[j+1],0,2)
                &&w[j+2]==0x8b1c8042u&&stx(w[j+3],29,2)&&frame_slot(w,imm9(w[j+3])))
                {context=imm9(w[j+3]);++roots;}
        if(roots!=1)continue;
        for(size_t j=2;j+3<w.size();++j) {
            if(!ldx(w[j-2],29,2)||imm9(w[j-2])!=context||!ldx(w[j-1],29,0)||!frame_slot(w,imm9(w[j-1]))||!ldw(w[j],2,1)||imm9(w[j])!=capture_width
                ||w[j+1]!=0x8b1c8021u||!stx(w[j+2],29,1)||!frame_slot(w,imm9(w[j+2])))continue;
            const auto width_frame=imm9(w[j+2]);
            for(size_t k=j+3;k+6<w.size();++k) {
                uint32_t allocator=0;
                if(!target(w[k-1],f.va+uint32_t((k-1)*4),allocator)
                    ||!ldx(w[k],29,1)||imm9(w[k])!=width_frame
                    ||!mem(w[k+1],0xb8000000u,0,1)||!field(imm9(w[k+1]))
                    ||!ldx(w[k+2],29,1)||!frame_slot(w,imm9(w[k+2]))||imm9(w[k+2])==width_frame
                    ||!stx(w[k-2],29,0)||imm9(w[k-2])!=imm9(w[k+2])
                    ||!mem(w[k+3],0xb8000000u,0,1)||!field(imm9(w[k+3]))
                    ||imm9(w[k+1])==imm9(w[k+3])
                    ||w[k+4]!=0xaa1d03efu||w[k+5]!=0xa8c179fdu||w[k+6]!=0xd65f03c0u)continue;
                // Width and nullable child of the final size wrapper, directly
                // returned. No call or later use of the child stack root remains.
                if(++terminals!=1)return {};
                result->closure=f.va;result->empty_site=f.va+uint32_t(k*4);
                result->child_frame=imm9(w[k+2]);result->blank_factory=allocator;
            }
        }
    }
    if(terminals!=1)return {};
    uint32_t tag_value=0,top=0,end=0;
    if(!recent::allocator(code,result->blank_factory,tag_value,top,end))return {};
    // Verify the stock allocator initializes payloads with compressed null. The
    // managed tail route below uses this exact allocator, including its original
    // slow path and stack maps, only after the native gateway has unwound.
    Body stub,allocation;uint32_t common=0;
    if(!code.prefix(result->blank_factory,3,stub)||!target(stub[2]|0x80000000u,result->blank_factory+8,common)
        ||!code.prefix(common,16,allocation)||allocation[7]!=0xf9000002u
        ||allocation[8]!=0x91002004u||allocation[9]!=0x92407ed0u||allocation[10]!=0xaa108210u
        ||allocation[11]!=0xa8814090u||allocation[12]!=0xeb03009fu||allocation[13]!=0x54ffffc3u
        ||allocation[14]!=0x91000400u||allocation[15]!=0xd65f03c0u)return {};
    std::set<uint32_t> width_users;
    for(const auto& f:code.functions())for(size_t i=0;i+1<f.words.size();++i){uint32_t to=0;
        if(target(f.words[i],f.va+uint32_t(i*4),to)&&to==result->width)width_users.insert(f.va);}
    unsigned holders=0;
    for(const auto& parent:code.functions()) {
        if(!width_users.count(parent.va))continue;
        bool sized=false;std::set<uint32_t> calls;
        for(size_t i=0;i<parent.words.size();++i){uint32_t to=0;
            if(target(parent.words[i],parent.va+uint32_t(i*4),to)){calls.insert(to);sized|=to==result->blank_factory;}}
        if(!sized)continue;
        for(auto va:calls) {
            Body b;if(!code.body(va,b)||!frame_size(b))continue;
            uint32_t holder=0,color=0;unsigned terminal=0;
            for(size_t i=0;i+3<b.size();++i){uint32_t to=0;
                if(!target(b[i],va+uint32_t(i*4),to))continue;
                if(b[i+1]==0xaa1d03efu&&b[i+2]==0xa8c179fdu&&b[i+3]==0xd65f03c0u){holder=to;++terminal;}
                if(width_users.count(to))color=to;
            }
            if(terminal!=1||!color||!holder)continue;
            unsigned forwards=0;for(size_t i=0;i<b.size();++i){uint32_t to=0;
                if(target(b[i],va+uint32_t(i*4),to)&&to==holder)++forwards;}
            Body h;int32_t receiver=0;if(forwards!=2||!code.body(holder,h)||!receiver_slot(h,1,receiver))continue;
            unsigned captures=0;for(size_t i=0;i+1<h.size();++i){uint32_t to=0;
                if(movz(h[i],1)&&zimm(h[i])==1&&target(h[i+1],holder+uint32_t((i+1)*4),to)
                    &&to==result->context_allocator)++captures;}
            if(captures!=1)continue;
            if(++holders!=1)return {};
            result->holder=holder;result->bottom=parent.va;result->content=va;result->color=color;
        }
    }
    return result;
}
}
