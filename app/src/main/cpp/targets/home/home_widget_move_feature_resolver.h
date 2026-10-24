/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_widget_move.h"
#include "home_runtime_layout_contracts.h"
#include <set>
namespace home_layout::features {
struct WidgetMoveFeature {
    uint32_t owner=0,span=0,rx=0,size=0,gate=0,editing=0;
    int32_t flag=0;
};
// Actual width/height predicate ABI. Argument/frame roots, independent raw
// dimension comparisons and the canonical boolean's final negation establish
// that the branch really consumes a span decision, not an arbitrary object.
// Supported span values are NOT a discovery table: the original predicate runs.
inline bool widget_span_predicate(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;int32_t width=0,height=0;
    if(!code.body(va,b)||!receiver_slot(b,1,width)||!receiver_slot(b,2,height)||width==height)return false;
    unsigned widths=0,heights=0,returns=0,booleans=0;
    for(size_t i=0;i<b.size();++i) {
        const auto w=b[i];
        if((w&0xffc003ffu)==0xf100003fu)++widths;
        if((w&0xffc003ffu)==0xf100005fu)++heights;
        if(rn(w)==22&&((w&0xfffffc00u)==0x91008000u||(w&0xfffffc00u)==0x9100c000u))++booleans;
        if(w==0xd65f03c0u) {
            if(i<3||b[i-3]!=0xd27c0020u||!runtime_contract::epilogue(b,i-2))return false;
            ++returns;
        }
    }
    return widths>=4&&heights>=2&&booleans>=2&&returns==1;
}
inline bool widget_reactive_source(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;int32_t receiver=0;
    if(!code.body(va,b)||!receiver_slot(b,1,receiver))return false;
    unsigned returns=0;
    for(size_t i=0;i+8<b.size();++i)
        if(ldx(b[i],29,1)&&imm9(b[i])==receiver&&ldw(b[i+1],1,0)&&field(imm9(b[i+1]))
            &&b[i+2]==0x8b1c8000u&&ux(b[i+3],27,16)&&b[i+4]==0x6b10001fu
            &&(b[i+5]&0xff00001fu)==0x54000000u&&runtime_contract::epilogue(b,i+6))++returns;
    return returns==1;
}
inline std::optional<WidgetMoveFeature> widget_move_feature(const Code& code) {
    using namespace home_dynamic;
    std::optional<WidgetMoveFeature> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b)||b.size()*4>0x4000)continue;
        bool flag=false;
        for(size_t i=2;i<b.size();++i)
            if(ldw(b[i-2],2,0)&&b[i-1]==0x8b1c8000u&&(b[i]&0xfff8001fu)==0x37200000u)flag=true;
        if(!flag)continue;
        const uint32_t bytes=uint32_t(b.size()*4);
        const auto read=[&](size_t size,Body& out){if(size!=bytes)return false;out=b;return true;};
        std::set<uint32_t> reactives;
        for(size_t i=0;i+2<b.size();++i) {
            uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&ldx(b[i+1],29,2)&&frame_slot(b,imm9(b[i+1]))
                &&mem(b[i+2],0xb8000000u,2,0)&&field(imm9(b[i+2]))&&widget_reactive_source(code,to))reactives.insert(to);
        }
        for(auto rx:reactives) {
            const auto editing=widget_move_editing_gate_offset(bytes,read,f.va,rx);
            if(editing==SIZE_MAX)continue;
            for(size_t i=1;i<b.size();++i) {
                uint32_t span=0;
                if((b[i]&0xfff8001fu)!=0x37200000u||!target(b[i-1],f.va+uint32_t((i-1)*4),span)
                    ||!widget_span_predicate(code,span))continue;
                int32_t field=0;const auto gate=widget_move_gate_offset(bytes,read,&field,f.va,span);
                if(gate==SIZE_MAX||gate==editing||gate+4>bytes||editing+4>bytes||!home_dynamic::field(field))continue;
                if(result)return {};
                result=WidgetMoveFeature{f.va,span,rx,bytes,uint32_t(gate),uint32_t(editing),field};
            }
        }
    }
    return result;
}
} // namespace home_layout::features
