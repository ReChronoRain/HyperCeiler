/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_recent_render.h"
#include "home_runtime_layout_contracts.h"

namespace home_layout::features::shortcuts {
struct Plan { uint32_t entry=0, eligibility=0, local=0, global=0, prohibited=0, loader=0; };

// Narrow ARM64 Dart code-generator contracts. Image addresses, frame/object
// fields, static/pool displacements and dispatch selectors are NOT identities.
// Relative branches describe the validated in-function boolean control flow.
inline uint32_t instruction_mask(uint32_t w) {
    using namespace home_dynamic;
    if((w&0xfc000000u)==0x94000000u)return 0xfc000000u;
    if((w&0x3b200c00u)==0x38000000u)return 0xffe00fffu;
    if((w&0x3b000000u)==0x39000000u)return 0xffc003ffu;
    if((w&0xffc003e0u)==0x91400360u)return 0xffc003ffu;
    if((w&0xffc003ffu)==0xd10001efu||(w&0xffc003ffu)==0xd100001eu)return 0xffc003ffu;
    return 0xffffffffu;
}
template<size_t N> inline bool matches(const home_dynamic::Body& body,const uint32_t (&shape)[N]) {
    if(body.size()!=N)return false;
    for(size_t i=0;i<N;++i)if((body[i]&instruction_mask(body[i]))!=shape[i])return false;
    for(auto w:body)if((w&0x3b200c00u)==0x38000000u) {
        const auto off=home_dynamic::imm9(w);
        if(home_dynamic::rn(w)==29) { if(!home_dynamic::frame_slot(body,off))return false; }
        else if(off!=-1&&!home_dynamic::field(off))return false;
    }
    return true;
}
inline constexpr uint32_t getter[]={
    0xa9bf79fdu,0xaa0f03fdu,0xb8400020u,0x8b1c8000u,0x6b16001fu,0x54000160u,0xaa0003e1u,0x91400362u,
    0xf9400042u,0x94000000u,0x6b16001fu,0x54000041u,0x9100c2c0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
    0x9100c2c0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
};
inline constexpr uint32_t eligibility[]={
    0xa9bf79fdu,0xaa0f03fdu,0xd10001efu,0xf80003a1u,0xf80003a2u,0xf9400340u,0xf9400000u,0xf9400370u,
    0x6b10001fu,0x54000061u,0xf9400362u,0x94000000u,0xaa0003e1u,0xf80003a0u,0x94000000u,0xf84003a0u,
    0xb8400001u,0x8b1c8021u,0x372000a1u,0xf84003a1u,0x94000000u,0x36200120u,0x14000004u,0xf84003a1u,
    0x94000000u,0x362000a0u,0x9100c2c0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0xf84003a1u,0x94000000u,
    0x6b16001fu,0x54000120u,0xaa0003e1u,0xf84003a2u,0x94000000u,0x372000a0u,0x9100c2c0u,0xaa1d03efu,
    0xa8c179fdu,0xd65f03c0u,0x910082c0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
};
inline constexpr uint32_t controller[]={
    0xa9bf79fdu,0xaa0f03fdu,0xd10001efu,0xaa0203e3u,0xf80003a2u,0xaa0103e2u,0xf80003a1u,0xf9400340u,
    0xf9400000u,0x6b16001fu,0x54000181u,0xf9400340u,0xf9400000u,0xf9400370u,0x6b10001fu,0x54000061u,
    0xf9400362u,0x94000000u,0xf9400370u,0xf90001f0u,0xf9400364u,0x94000000u,0xb8400001u,0x8b1c8021u,
    0x372004c1u,0xf9400340u,0xf9400000u,0xf9400370u,0x6b10001fu,0x54000061u,0xf9400362u,0x94000000u,
    0xf80003a0u,0xf9400340u,0xf9400000u,0xf9400370u,0x6b10001fu,0x54000061u,0xf9400362u,0x94000000u,
    0xb8400001u,0x8b1c8021u,0x6b16003fu,0x54000640u,0xb8400020u,0x8b1c8000u,0xaa0003e1u,0xf84003a2u,
    0x94000000u,0x6b16001fu,0x54000061u,0xf9400362u,0x14000002u,0xaa0003e2u,0xf84003a1u,0xf84003a3u,
    0x94000000u,0x362000a0u,0x9100c2c0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0xf84003a1u,0x94000000u,
    0x6b16001fu,0x54000061u,0xf9400362u,0x14000002u,0xaa0003e2u,0xf84003a1u,0xf80003a2u,0x94000000u,
    0x6b16001fu,0x54000061u,0xaa1603e0u,0x14000003u,0xaa0003e1u,0x94000000u,0xf80003a0u,0x94000000u,
    0xaa0003e3u,0xf84003a0u,0xf80003a3u,0xb8000060u,0xaa0303e1u,0xf84003a2u,0x94000000u,0xf84003a1u,
    0xf84003a2u,0x94000000u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0x94000000u,0xaa0003e1u,0xf9400360u,
    0xb8000020u,0xaa0103e0u,0x94000000u,0xd4200000u,
};
inline constexpr uint32_t prohibited[]={
    0xa9bf79fdu,0xaa0f03fdu,0xb8400020u,0x8b1c8000u,0x6b16001fu,0x54000280u,0xaa0003e1u,0x91400362u,
    0xf9400042u,0x94000000u,0x6b16001fu,0x540001c0u,0xf8400001u,0xd34c7c21u,0xaa0003f0u,0xaa0103e0u,
    0xaa1003e1u,0x91400362u,0xf9400042u,0xd100001eu,0xf87e7abeu,0xd63f03c0u,0xaa1d03efu,0xa8c179fdu,
    0xd65f03c0u,0xaa1603e0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
};

inline std::optional<Plan> resolve(const Code& code) {
    using namespace home_dynamic;
    const auto callee=[&](const Function& f,size_t at) {
        uint32_t to=0;return at<f.words.size()&&target(f.words[at],f.va+uint32_t(at*4),to)?to:0;
    };
    std::optional<Plan> found;
    for(const auto& f:code.functions()) {
        if(!matches(f.words,controller))continue;
        Plan p;p.entry=f.va;p.eligibility=callee(f,89);
        Body e,l,g,h;
        if(!code.body(p.eligibility,e)||!matches(e,eligibility))continue;
        const Function ef{p.eligibility,e};
        p.global=callee(ef,20);p.local=callee(ef,24);p.prohibited=callee(ef,31);
        if(!p.local||p.local==p.global||!code.body(p.local,l)||!code.body(p.global,g)
            ||!code.body(p.prohibited,h)||!matches(l,getter)||!matches(g,getter)||!matches(h,prohibited))continue;
        // Both regional flags read the SAME fresh metadata bundle, with
        // different keys through the SAME bool bridge. The prohibited-activity
        // string getter reads that bundle too. Never target generic getters.
        uint32_t lb=0,gb=0;
        if(!target(l[9],p.local+36,lb)||!target(g[9],p.global+36,gb)||lb!=gb
            ||!lb||imm9(l[2])!=imm9(g[2])||imm9(l[2])!=imm9(h[2])
            ||imm9(f.words[83])!=imm9(l[2])
            ||(l[7]==g[7]&&l[8]==g[8]))continue;
        // Saved receivers/arguments must survive every nested call unchanged.
        const auto same_slot=[](const Body& b,std::initializer_list<size_t> at) {
            const auto field=imm9(b[*at.begin()]);
            return std::all_of(at.begin(),at.end(),[&](size_t i){return imm9(b[i])==field;});
        };
        if(!same_slot(e,{3,19,23,30})||!same_slot(e,{4,35})||!same_slot(e,{13,15})
            ||!same_slot(f.words,{4,55,88})||!same_slot(f.words,{6,47,62,69,78,81})
            ||!same_slot(f.words,{82,87}))continue;
        unsigned consumers=0;bool invalid=false;
        for(const auto& caller:code.functions())for(size_t i=0;i<caller.words.size();++i) {
            if(callee(caller,i)!=p.entry)continue;
            const auto& b=caller.words;
            // Async activity-list loop: await package lookup, reject null,
            // qualify package/activity, then query persisted hidden state.
            // This is the management list consumer, not the uninstall dialog.
            if(i<6||i+17>=b.size()||!frame_size(b)||b[i-6]!=0x6b16001fu
                ||(b[i-5]&0xff00001fu)!=0x54000001u||b[i-3]>>26!=5u
                ||b[i-2]!=0xaa0003e1u||!ldx(b[i-1],29,2)
                ||(b[i+1]&0xfff8001fu)!=0x36200000u||(int32_t(b[i+1]<<13)>>18)!=3
                ||!frame_slot(b,imm9(b[i-1]))||!frame_slot(b,imm9(b[i+12]))
                ||!frame_slot(b,imm9(b[i+13]))
                ||imm9(b[i-1])!=imm9(b[i+12])
                ||!ldw(b[i+5],0,1)||b[i+6]!=0x8b1c8021u
                ||!ldw(b[i+9],1,2)||b[i+10]!=0x8b1c8042u
                ||b[i+11]!=0xaa0203e1u||!ldx(b[i+12],29,2)||!ldx(b[i+13],29,3)
                ||b[i+14]!=0xd2800005u||!callee(caller,i+15)
                ||b[i+16]!=0xaa0003e1u)invalid=true;
            p.loader=caller.va;++consumers;
        }
        if(invalid||consumers!=1)continue;
        if(found)return {};found=p;
    }
    return found;
}

// The legacy feature admits all applications in this list. Actual hide/restore,
// confirmation, provider persistence and uninstall eligibility remain original.
// Called before the Dart entry, with no managed frame, allocation or GC call.
inline bool apply(recent_render::Registers& saved,bool ready,uintptr_t original) {
    saved.branch=original;
    if(!ready||!original)return false;
    saved.x[0]=saved.x[22]+0x20; // Verified canonical Dart true, not integer 1.
    saved.branch=0;
    return true;
}
} // namespace home_layout::features::shortcuts
