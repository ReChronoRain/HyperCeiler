/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_icon_feature_resolver.h"
#include "home_dock_title.h"
#include <optional>

namespace home_layout::features {
struct DockTitleBuilderFeature {
    uint32_t va=0;
    DockTitlePlan plan{};
    std::array<uint32_t,2> original{};
    std::array<int32_t,8> fields{};
};
struct DockTitleFeature {uint32_t scope=0;std::array<DockTitleBuilderFeature,2> builders{};};
inline bool dock_scope_bool(const home_dynamic::Body& b,uint32_t va,const Code& code) {
    uint32_t ancestor=0;home_dynamic::Body callee;
    return b.size()==12&&b[0]==0xa9bf79fdu&&b[1]==0xaa0f03fdu&&b[2]==0xaa0103e0u
        &&home_dynamic::target(b[3],va+12,ancestor)&&code.body(ancestor,callee)
        &&b[4]==0x6b16001fu&&b[5]==0x910082d0u&&b[6]==0x9100c2d1u&&b[7]==0x9a911201u
        &&b[8]==0xaa0103e0u&&b[9]==0xaa1d03efu&&b[10]==0xa8c179fdu&&b[11]==0xd65f03c0u;
}
inline std::optional<DockTitleFeature> dock_title_feature(const Code& code) {
    using namespace home_dynamic;
    std::vector<DockTitleBuilderFeature> builders;uint32_t common_scope=0;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        for(size_t i=5;i+11<b.size();++i) {
            uint32_t scope=0;
            if((b[i]&0xfff8001fu)!=0x36200001u||b[i+1]!=0xaa0003e1u
                ||(b[i+3]&0xfff8001fu)!=0x36200000u
                ||!target(b[i+5],f.va+uint32_t((i+5)*4),scope))continue;
            auto plan=resolve_dock_title(b,f.va,scope);Body getter,scope_body;
            // A frameless leaf can follow this getter without becoming a BL
            // destination. Validate its complete RET-terminated 20-word
            // contract, rather than mistaking that next leaf for getter code.
            if(!plan||plan->gates.size()!=2||!code.prefix(plan->hotseat_getter,20,getter)
                ||!dock_title_hotseat_getter(getter)||!code.body(scope,scope_body)||!dock_scope_bool(scope_body,scope,code))continue;
            if(common_scope&&scope!=common_scope)return {};
            DockTitleBuilderFeature next;next.va=f.va;next.plan=*plan;
            std::copy(plan->alpha_fields.begin(),plan->alpha_fields.end(),next.fields.begin());
            next.fields[6]=zimm(getter[0]);next.fields[7]=imm9(getter[3]);
            bool valid=true;
            for(unsigned j=0;j<3;++j)valid&=frame_slot(b,next.fields[j]);
            if(!valid)continue;
            for(unsigned j=0;j<2;++j)next.original[j]=b[plan->gates[j].offset/4];
            common_scope=scope;builders.push_back(std::move(next));
            if(builders.size()>2)return {};
        }
    }
    if(builders.size()!=2||builders[0].va==builders[1].va)return {};
    DockTitleFeature result;result.scope=common_scope;
    std::copy(builders.begin(),builders.end(),result.builders.begin());
    return result;
}

struct DrawerFontFeature {
    uint32_t owner=0,font=0,height=0;
    int32_t font_widget=0,font_frame=0,font_field=0,height_widget=0,height_frame=0,height_field=0;
};
inline bool title_double_source(const Code& code,uint32_t va,unsigned depth=0) {
    using namespace home_dynamic;
    if(depth>=4)return false;
    Body b;if(!code.body(va,b))return false;
    std::vector<size_t> values;
    for(size_t i=1;i<b.size();++i) {
        if(b[i]!=0xd65f03c0u)continue;
        size_t value=i-1;
        if(i>=3&&b[i-1]==0xa8c179fdu&&b[i-2]==0xaa1d03efu)value=i-3;
        const auto w=b[value];uint32_t callee=0;
        const bool load=(w&0xffe00c1fu)==0xfc400000u||(w&0xffc0001fu)==0xfd400000u
            ||(w&0xff00001fu)==0x5c000000u;
        const bool literal=(w&0xffe01fffu)==0x1e601000u;
        const auto binary=w&0xffe0fc1fu;
        const bool arithmetic=binary==0x1e602800u||binary==0x1e603800u
            ||binary==0x1e600800u||binary==0x1e601800u;
        if(!load&&!literal&&!arithmetic&&!(target(w,va+uint32_t(value*4),callee)
            &&title_double_source(code,callee,depth+1)))return false;
        values.push_back(value);
    }
    return !values.empty()&&passes_return_points(b,values);
}
inline std::optional<DrawerFontFeature> drawer_font_feature(const Code& code,const IconFeature& icon) {
    using namespace home_dynamic;
    Body b;if(!code.body(icon.app,b)||!frame_size(b))return {};
    std::optional<DrawerFontFeature> result;
    for(size_t i=1;i+3<b.size();++i) {
        uint32_t getter=0;
        if(!ldx(b[i],29,0)||!std_(b[i+1],29,0)||!ldw(b[i+2],0,1)
            ||b[i+3]!=0x8b1c8021u||!target(b[i-1],icon.app+uint32_t((i-1)*4),getter)
            ||getter==icon.current||!frame_slot(b,imm9(b[i]))||!frame_slot(b,imm9(b[i+1]))
            ||!field(imm9(b[i+2])))continue;
        const int32_t font_frame=imm9(b[i+1]);
        for(size_t j=3;j+3<b.size();++j) {
            uint32_t height=0;
            if(!ldx(b[j],29,0)||!std_(b[j+1],29,0)||!ldw(b[j+2],0,1)||b[j+3]!=0x8b1c8021u
                ||!ldx(b[j-3],29,2)||!ldd(b[j-2],29,0)||imm9(b[j-2])!=font_frame
                ||imm9(b[j-3])!=imm9(b[j])||!target(b[j-1],icon.app+uint32_t((j-1)*4),height)
                ||height==getter||!frame_slot(b,imm9(b[j]))||!frame_slot(b,imm9(b[j+1]))
                ||!field(imm9(b[j+2])))continue;
            // Both actual callees return unboxed scalar values to the same
            // typed widget/frame chain. No nominal getter name is consulted.
            if(!title_double_source(code,getter)||!title_double_source(code,height))continue;
            if(result)return {};
            result=DrawerFontFeature{icon.app,uint32_t(i*4),uint32_t(j*4),
                imm9(b[i]),font_frame,imm9(b[i+2]),imm9(b[j]),imm9(b[j+1]),imm9(b[j+2])};
        }
    }
    return result;
}
} // namespace home_layout::features
