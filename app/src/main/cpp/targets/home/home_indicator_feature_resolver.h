/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_indicator_pair.h"
#include "home_runtime_layout_contracts.h"
#include <set>
namespace home_layout::features {
struct IndicatorPolicyFeature {uint32_t owner=0,gate=0,result=0,editing=0,size=0,empty_pool=0;};
struct IndicatorSlideFeature {uint32_t owner=0,patch=0,idle_caller=0,refresh=0,type=0,animate=0,size=0;};
inline bool indicator_inbound_safe(const home_dynamic::Body& b,uint32_t at) {
    if(uint64_t(at)+16>b.size()*4)return false;
    for(size_t i=0;i<b.size();++i) {
        const auto w=b[i];int32_t delta=0;bool branch=false;
        if((w&0xfc000000u)==0x14000000u){delta=int32_t(w<<6)>>6;branch=true;}
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u){delta=int32_t(w<<8)>>13;branch=true;}
        else if((w&0x7e000000u)==0x36000000u){delta=int32_t(w<<13)>>18;branch=true;}
        const int64_t to=int64_t(i*4)+int64_t(delta)*4;
        if(branch&&to>at&&to<at+16)return false;
    }
    return true;
}
inline bool indicator_bool_comparison(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;
    if(!code.body(va,b)||!frame_size(b))return false;
    std::vector<size_t> values;
    for(size_t i=0;i+8<b.size();++i)
        if(ldx(b[i],29,1)&&frame_slot(b,imm9(b[i]))&&b[i+1]==0x6b00003fu
            &&b[i+2]==0x910082d0u&&b[i+3]==0x9100c2d1u&&b[i+4]==0x9a911202u
            &&b[i+5]==0xaa0203e0u&&runtime_contract::epilogue(b,i+6))values.push_back(i+4);
    return values.size()==1&&passes_return_points(b,values);
}
inline bool indicator_editing_bool(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;int32_t receiver=0;
    if(!code.body(va,b)||!receiver_slot(b,1,receiver))return false;
    std::vector<size_t> values;unsigned flags=0,toggles=0;
    for(size_t i=0;i+5<b.size();++i) {
        if(ldx(b[i],29,1)&&imm9(b[i])==receiver&&ldw(b[i+1],1,2)&&field(imm9(b[i+1]))
            &&b[i+2]==0x8b1c8042u&&(b[i+3]&0xfff8001fu)==0x37200002u
            &&b[i+4]==0x9100c2c0u&&runtime_contract::epilogue(b,i+5)) {
            const int64_t to=int64_t(i+3)+(int32_t(b[i+3]<<13)>>18);
            if(to>=1&&to+3<int64_t(b.size())&&ldx(b[to-1],29,1)&&imm9(b[to-1])==receiver
                &&ldw(b[to],1,2)&&field(imm9(b[to]))&&imm9(b[to])!=imm9(b[i+1])
                &&b[to+1]==0x8b1c8042u&&b[to+2]==0xaa0203e1u&&b[to+3]==0xaa0003e2u) {
                values.push_back(i+4);++flags;
            }
        }
        uint32_t comparison=0;
        if(target(b[i],va+uint32_t(i*4),comparison)&&indicator_bool_comparison(code,comparison)
            &&b[i+1]==0xd27c0001u&&b[i+2]==0xaa0103e0u&&runtime_contract::epilogue(b,i+3)) {
            values.push_back(i);++toggles;
        }
    }
    return flags==1&&toggles==1&&passes_return_points(b,values);
}
inline std::optional<IndicatorPolicyFeature> indicator_policy_feature(const Code& code) {
    using namespace home_dynamic;
    constexpr uint32_t gate[]={0xf100041f,0x540001ec,0xf85e83a3,0x362001a3,
        0xf85f83a1,0xb840f024,0x8b1c8084,0xaa0403e1};
    constexpr uint32_t result[]={0x362000e0,0xf85e03a0,0x362000a0,0xf9712b60,
        0xaa1d03ef,0xa8c179fd,0xd65f03c0,0xf85c83a2,0xf85c03a0,0xf85e83a1};
    std::optional<IndicatorPolicyFeature> found;
    for(const auto& fn:code.functions()) {
        const auto& b=fn.words;uint32_t g=0,r=0,editing=0;
        if(!frame_size(b)||!unique(b,gate,8,g)||!unique(b,result,10,r)||r!=g+36
            ||!target(b[g/4+8],fn.va+g+32,editing)||!indicator_editing_bool(code,editing))continue;
        const auto tb=[&](size_t at){return int64_t(at*4)+(int32_t(b[at]<<13)>>18)*4;};
        const auto cond=[&](size_t at){return int64_t(at*4)+(int32_t(b[at]<<8)>>13)*4;};
        if(tb(r/4)!=r+28||tb(r/4+2)!=r+28||tb(g/4+3)!=r+28||cond(g/4+1)!=r+28
            ||!indicator_inbound_safe(b,g)||!indicator_inbound_safe(b,r))continue;
        bool slots=true;
        for(auto at:{g/4+2,g/4+4,r/4+1,r/4+7,r/4+8,r/4+9})slots&=frame_slot(b,imm9(b[at]));
        if(!slots)continue;
        if(found)return {};
        found=IndicatorPolicyFeature{fn.va,g,r,editing,uint32_t(b.size()*4),uoff(b[r/4+3])};
    }
    return found;
}
// Actual raw enum return ABI, as consumed by _showIndicator's x2 argument.
inline bool indicator_raw_type(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;
    if(!code.body(va,b)||!frame_size(b))return false;
    std::set<uint32_t> values;std::vector<size_t> definitions;
    for(size_t i=0;i+3<b.size();++i)
        if(movz(b[i],0)&&zimm(b[i])<=2&&runtime_contract::epilogue(b,i+1)) {
            if(!values.insert(zimm(b[i])).second)return false;definitions.push_back(i);
        }
    return values==std::set<uint32_t>{0,1,2}&&passes_return_points(b,definitions);
}
inline bool indicator_animate(const Code& code,uint32_t va,int32_t type_field) {
    using namespace home_dynamic;Body b;int32_t receiver=0,argument=0;
    if(!code.body(va,b)||!receiver_slot(b,1,receiver)||!receiver_slot(b,2,argument)||receiver==argument)return false;
    constexpr uint32_t fallback[]={0x7100103f,0x54000081,0xb846b061,0x8b1c8021,0x14000003,0xb8463061,0x8b1c8021};
    uint32_t at=0;if(!unique(b,fallback,7,at))return false;
    const auto a=at/4;
    if((int32_t(b[a+1]<<8)>>13)!=4||(int32_t(b[a+4]<<6)>>6)!=3
        ||imm9(b[a+2])==imm9(b[a+5])||!field(imm9(b[a+2]))||!field(imm9(b[a+5])))return false;
    unsigned updates=0,boxed=0;
    for(size_t i=0;i+4<b.size();++i) {
        if(ldx(b[i],29,2)&&imm9(b[i])==argument&&b[i+1]==0xd37ff843u
            &&mem(b[i+2],0xb8000000u,1,3)&&field(imm9(b[i+2])))++boxed;
        if(ldw(b[i],0,1)&&field(imm9(b[i]))&&b[i+1]==0x93417c22u
            &&ldx(b[i+2],29,3)&&imm9(b[i+2])==receiver&&stx(b[i+3],3,2)&&imm9(b[i+3])==type_field)++updates;
    }
    return boxed==1&&updates==1;
}
inline std::optional<IndicatorSlideFeature> indicator_slide_feature(const Code& code) {
    using namespace home_dynamic;
    constexpr uint32_t show[]={0xf85f83a0,0xf81f03a3,0xf841b002,0xeb03005f};
    std::optional<IndicatorSlideFeature> found;
    for(const auto& fn:code.functions()) {
        const auto& b=fn.words;uint32_t patch=0;int32_t receiver=0,argument=0;
        if(!unique(b,show,4,patch)||!receiver_slot(b,1,receiver)||!receiver_slot(b,2,argument)
            ||receiver==argument||imm9(b[patch/4])!=receiver||imm9(b[patch/4+1])!=argument
            ||!indicator_inbound_safe(b,patch))continue;
        const auto type_field=imm9(b[patch/4+2]);if(!field(type_field))continue;
        uint32_t animate=0;unsigned animations=0;
        for(size_t i=patch/4+4;i+2<b.size();++i) {
            uint32_t to=0;
            if(ldx(b[i],29,1)&&imm9(b[i])==receiver&&ldx(b[i+1],29,2)&&imm9(b[i+1])==argument
                &&target(b[i+2],fn.va+uint32_t((i+2)*4),to)&&indicator_animate(code,to,type_field)) {
                animate=to;++animations;
            }
        }
        if(animations!=1)continue;
        for(const auto& caller:code.functions()) {
            const auto& c=caller.words;int32_t owner=0;
            if(!receiver_slot(c,1,owner))continue;
            uint32_t type=0,idle=0;unsigned resets=0,values=0;
            for(size_t i=2;i<c.size();++i) {
                uint32_t to=0;
                if(!target(c[i],caller.va+uint32_t(i*4),to)||to!=fn.va||!ldx(c[i-2],29,1)
                    ||imm9(c[i-2])!=owner)continue;
                if(c[i-1]==0xd2800002u)++resets;
                if(i>=3&&c[i-1]==0xaa0003e2u&&target(c[i-3],caller.va+uint32_t((i-3)*4),to)
                    &&indicator_raw_type(code,to)) {type=to;idle=caller.va+uint32_t((i+1)*4);++values;}
            }
            if(resets!=1||values!=1)continue;
            if(found)return {};
            found=IndicatorSlideFeature{fn.va,patch,idle,caller.va,type,animate,uint32_t(b.size()*4)};
        }
    }
    return found;
}
struct IndicatorMarginFeature {
    uint32_t owner=0,patch=0,size=0,deflate=0,constructor=0,insets_allocator=0,container_allocator=0;
    hc::indicator::InsetsLayout layout{};
};
inline bool indicator_allocation(const Code& code,uint32_t va,uint32_t minimum,uint32_t& cid,uint32_t& bytes) {
    using namespace home_dynamic;Body b;uint32_t header=0;
    if(!code.prefix(va,3,b)||!tag(b,header)||(b[2]&0xfc000000u)!=0x14000000u)return false;
    bytes=((header>>8)&15)*16;cid=(header>>12)&0xfffff;
    // Verified Dart allocation-tag flags; class and object extent are outputs.
    return (header&255)==0x1c&&cid&&bytes>=minimum&&bytes<=240;
}
inline bool indicator_container_constructor(const Code& code,uint32_t va,uint32_t bytes) {
    using namespace home_dynamic;Body b;int32_t receiver=0;
    if(!code.body(va,b)||!receiver_slot(b,1,receiver)||b[3]!=0xaa0103e2u)return false;
    std::set<int32_t> references;unsigned returns=0;
    for(size_t i=0;i<b.size();++i) {
        const auto w=b[i];
        if(mem(w,0xb8000000u,2,0)) {
            if(!field(imm9(w))||uint32_t(imm9(w)+5)>bytes||!references.insert(imm9(w)).second)return false;
        }
        if(w==0xd65f03c0u) {
            if(i<3||b[i-3]!=0xaa1603e0u||!runtime_contract::epilogue(b,i-2))return false;
            ++returns;
        }
    }
    return references.size()>=4&&returns==1;
}
// Identify the fresh four-sided Insets through actual Rect deflation roles and
// its original rooted argument forwarding to a bounded widget constructor.
// The zero/zero/zero/bottom producer alone has a real decoy elsewhere in this
// image. No named parent span or Capsule/Container function name is consulted.
inline std::optional<IndicatorMarginFeature> indicator_margin_feature(const Code& code) {
    using namespace home_dynamic;
    std::optional<std::pair<uint32_t,std::array<int32_t,4>>> deflate;
    for(const auto& f:code.functions()) {
        std::array<int32_t,4> fields;
        if(!hc::indicator::inset_fields(f.words,fields))continue;
        if(deflate)return {};deflate=std::pair{f.va,fields};
    }
    if(!deflate)return {};
    std::optional<IndicatorMarginFeature> found;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        for(size_t i=1;i+9<b.size();++i) {
            uint32_t allocation=0,cid=0,bytes=0;
            if(!stx(b[i],29,0)||!frame_slot(b,imm9(b[i]))||!ldd(b[i+4],29,0)||!frame_slot(b,imm9(b[i+4]))
                ||!std_(b[i+5],0,0)||imm9(b[i+5])!=deflate->second[3]||!ux(b[i+6],26,1)
                ||!target(b[i-1],f.va+uint32_t((i-1)*4),allocation)
                ||!indicator_allocation(code,allocation,48,cid,bytes))continue;
            unsigned zeroes=0;
            for(unsigned axis=0;axis<3;++axis)for(unsigned j=1;j<=3;++j)
                if(stx(b[i+j],0,31)&&imm9(b[i+j])==deflate->second[axis])zeroes|=1u<<axis;
            hc::indicator::InsetsLayout layout{cid,bytes,deflate->second,imm9(b[i]),imm9(b[i+4])};
            if(zeroes!=7||!layout.usable()||layout.root==layout.source
                ||(b[i+7]&0xffc003ffu)!=0xf9400021u||b[i+8]!=0x6b16003fu
                ||(b[i+9]&0xff00001fu)!=0x54000001u)continue;
            const int64_t destination=int64_t(i+9)+(int32_t(b[i+9]<<8)>>13);
            if(destination<int64_t(i+10)||destination>=int64_t(b.size())
                ||!indicator_inbound_safe(b,uint32_t((i+6)*4)))continue;
            unsigned consumers=0;uint32_t constructor=0,container_allocator=0;
            for(size_t j=i+7;j+7<b.size();++j) {
                if(stx(b[j],29,rd(b[j]))&&imm9(b[j])==layout.root)break;
                uint32_t factory=0,identity=0,extent=0,callee=0;
                if(j<4||!ldx(b[j],29,30)||imm9(b[j])!=layout.root||b[j+1]!=0xa900c1feu
                    ||!ldx(b[j+2],29,16)||!frame_slot(b,imm9(b[j+2]))||b[j+3]!=0xf90001f0u
                    ||b[j+4]!=0xaa0003e1u||!stx(b[j-3],29,0)||!frame_slot(b,imm9(b[j-3]))
                    ||!target(b[j-4],f.va+uint32_t((j-4)*4),factory)
                    ||!indicator_allocation(code,factory,64,identity,extent)
                    ||!target(b[j+7],f.va+uint32_t((j+7)*4),callee)
                    ||!indicator_container_constructor(code,callee,extent))continue;
                ++consumers;constructor=callee;container_allocator=factory;
            }
            if(consumers!=1)continue;
            if(found)return {};
            found=IndicatorMarginFeature{f.va,uint32_t((i+6)*4),uint32_t(b.size()*4),deflate->first,
                constructor,allocation,container_allocator,layout};
        }
    }
    return found;
}
} // namespace home_layout::features
