/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
#include <map>
namespace home_layout::features {
struct LayoutScalarFeature {
    uint32_t consumer=0,screen=0,size=0;
    runtime_contract::ScalarSource source{};
    std::vector<uint32_t> margin_callers; // Verified Dock return PCs, never shared workspace/config producers.
};
// Every returning arm must define unboxed D0 immediately before the epilogue,
// or branch directly to it. This also covers zero and the original typed tail
// getter; a nominal object-returning helper is never enough evidence.
inline bool scalar_double_return(const Code& code,uint32_t va,unsigned depth=0) {
    using namespace home_dynamic;using runtime_contract::epilogue;
    if(depth>=4)return false;
    Body b;if(!code.body(va,b))return false;
    std::vector<size_t> points;
    for(size_t i=0;i<b.size();++i) {
        const bool load=ldd(b[i],rn(b[i]),0)&&field(imm9(b[i]));
        const bool zero=b[i]==0x6e201c00u;
        uint32_t callee=0;
        if((load||zero||(target(b[i],va+uint32_t(i*4),callee)&&scalar_double_return(code,callee,depth+1)))
            &&epilogue(b,i+1))points.push_back(i);
        if((load||zero)&&i+1<b.size()&&(b[i+1]&0xfc000000u)==0x14000000u) {
            const int64_t to=int64_t(i+1)+(int32_t(b[i+1]<<6)>>6);
            if(to>=0&&to<int64_t(b.size())&&epilogue(b,size_t(to)))points.push_back(i);
        }
        if(ldd(b[i],rn(b[i]),1)&&field(imm9(b[i]))&&i+1<b.size()&&b[i+1]==0x4ea11c20u
            &&epilogue(b,i+2))points.push_back(i);
    }
    return !points.empty()&&passes_return_points(b,points);
}
// The folder row getter feeds the aspect of a fresh grid delegate, after its
// integer count and two distinct gap scalars. Division alone also appears in
// many unrelated layout functions and cannot identify this getter.
inline bool folder_row_consumer(const Code& code,const Function& f,size_t call) {
    using namespace home_dynamic;const auto& b=f.words;
    if(call+19>=b.size()||!std_(b[call+5],29,2)||!frame_slot(b,imm9(b[call+5])))return false;
    const auto aspect=imm9(b[call+5]);
    uint32_t allocator=0;Body stub;uint32_t header=0;
    if(!target(b[call+8],f.va+uint32_t((call+8)*4),allocator)||!code.prefix(allocator,3,stub)
        ||!tag(stub,header)||(stub[2]&0xfc000000u)!=0x14000000u||((header>>8)&15)*16<40
        ||b[call+6]!=0x93417c01u||!stx(b[call+7],29,1)||!frame_slot(b,imm9(b[call+7]))
        ||b[call+9]!=0xaa0003e1u||!ldx(b[call+10],29,0)||imm9(b[call+10])!=imm9(b[call+7])
        ||!stx(b[call+11],29,1)||!frame_slot(b,imm9(b[call+11]))||!stx(b[call+12],1,0))return false;
    const auto count=imm9(b[call+12]);std::array<int32_t,3> locals{};
    if(!field(count))return false;
    for(unsigned axis=0;axis<3;++axis) {
        const auto at=call+13+axis*2;
        if(!ldd(b[at],29,0)||!frame_slot(b,imm9(b[at]))||!std_(b[at+1],1,0)
            ||imm9(b[at+1])!=count+int32_t((axis+1)*8))return false;
        locals[axis]=imm9(b[at]);
    }
    return locals[2]==aspect&&locals[0]!=locals[1]&&locals[0]!=aspect&&locals[1]!=aspect
        &&uint32_t(count+33)<=((header>>8)&15)*16;
}
// The Dock controller combines the margin with its optional title height. Follow
// the saved margin through its fresh double box into the final subtraction;
// this is distinct from the shared getter's workspace/configuration consumers.
inline bool dock_total_consumer(const Function& f,size_t call) {
    using namespace home_dynamic;const auto& b=f.words;
    if(call+10>=b.size()||!std_(b[call+1],29,0)||!frame_slot(b,imm9(b[call+1]))
        ||(b[call+2]&0xfc000000u)!=0x94000000u
        ||(b[call+3]&0xfff8001fu)!=0x37200000u||b[call+4]!=0xd2800000u
        ||(b[call+5]&0xfc000000u)!=0x14000000u
        ||!ldx(b[call+6],29,1)||!ldx(b[call+7],29,2)
        ||(b[call+8]&0xfc000000u)!=0x94000000u||!std_(b[call+9],29,0))return false;
    unsigned returns=0;
    for(size_t i=call+10;i+16<b.size();++i) {
        if(!ldd(b[i],29,0)||imm9(b[i])!=imm9(b[call+1])
            ||b[i+1]!=0xa9460b41u||b[i+2]!=0x91004021u||b[i+3]!=0xeb01005fu
            ||(b[i+4]&0xff00001fu)!=0x54000009u||b[i+5]!=0xf9003341u
            ||b[i+6]!=0xd1003c21u||b[i+9]!=0xf81ff022u||b[i+10]!=0xfc007020u
            ||b[i+11]!=0xa90005e0u||(b[i+12]&0xfc000000u)!=0x94000000u
            ||b[i+13]!=0xfc407000u||!runtime_contract::epilogue(b,i+14))continue;
        ++returns;
    }
    return returns==1;
}
inline std::optional<LayoutScalarFeature> layout_scalar_feature(const Code& code,uint32_t config,bool margin) {
    using namespace home_dynamic;
    struct Source {uint32_t screen,size;runtime_contract::ScalarSource fields;};
    std::map<uint32_t,Source> sources;
    Body root;if(!code.body(config,root)||!frame_size(root))return {};
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        unsigned hits=0;Source candidate{};
        for(size_t j=0;j+6<b.size();++j) {
            uint32_t to=0;
            if(!target(b[j],f.va+uint32_t(j*4),to)||to!=config||!ldw(b[j+1],0,1)
                ||!field(imm9(b[j+1]))||b[j+2]!=0x8b1c8021u||!ldd(b[j+3],1,0)||!field(imm9(b[j+3])))continue;
            bool returns=!margin&&runtime_contract::epilogue(b,j+4);int32_t local=0;
            uint32_t screen=0;
            if(margin&&j+15<b.size()&&target(b[j+15],f.va+uint32_t((j+15)*4),screen)) {
                Body sb;bool shared=false;
                if(code.body(screen,sb))for(size_t i=0;i+2<sb.size();++i) {
                    uint32_t current=0;
                    if(target(sb[i],screen+uint32_t(i*4),current)&&current==config&&ldw(sb[i+1],0,1)
                        &&field(imm9(sb[i+1]))&&sb[i+2]==0x8b1c8021u)shared=true;
                }
                returns=shared&&scalar_double_return(code,screen)
                    &&runtime_contract::margin_return(b,j+4,f.va,screen,local);
            }
            const std::array<size_t,1> value_point{j+3};
            if(returns&&std::count(b.begin(),b.end(),0xd65f03c0u)==1&&passes_return_points(b,value_point)) {
                ++hits;candidate={screen,uint32_t(b.size()*4),{f.va,imm9(b[j+1]),imm9(b[j+3]),local}};
            }
        }
        if(hits==1)sources.emplace(f.va,candidate);
    }
    std::optional<LayoutScalarFeature> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        for(size_t i=0;i+4<b.size();++i) {
            uint32_t to=0;
            if(b[i+1]!=0x4ea01c01u||!ldd(b[i+2],29,0)||b[i+3]!=(margin?0x1e613802u:0x1e611802u)
                ||!target(b[i],f.va+uint32_t(i*4),to))continue;
            const auto source=sources.find(to);if(source==sources.end()||(!margin&&!folder_row_consumer(code,f,i)))continue;
            runtime_contract::ScalarSource proved;
            if(margin) {
                if(!frame_slot(b,imm9(b[i+2])))continue;
                proved=source->second.fields;
            } else if(!runtime_contract::layout_scalar(b,f.va,config,source->second.screen,false,
                    [&](uint32_t target,Body& out){return code.body(target,out);},proved))continue;
            if(result&&result->source.target!=proved.target)return {};
            // Multiple callers of one proved getter are supporting evidence,
            // never alternative hook targets or a first-match fallback.
            if(!result)result=LayoutScalarFeature{f.va,source->second.screen,source->second.size,proved,{}};
            else result->consumer=f.va;
            if(margin)result->margin_callers.push_back(f.va+uint32_t((i+1)*4));
        }
    }
    if(margin&&result) {
        unsigned totals=0;
        for(const auto& f:code.functions())for(size_t i=0;i<f.words.size();++i) {
            uint32_t to=0;
            if(target(f.words[i],f.va+uint32_t(i*4),to)&&to==result->source.target&&dock_total_consumer(f,i)) {
                result->margin_callers.push_back(f.va+uint32_t((i+1)*4));++totals;
            }
        }
        if(totals!=1)return {};
        // Derive the Dock row-height field from the same rectangle consumers.
        // Origin/center helpers must use that very field and the same config
        // producer, so hit targets and return animations follow the icon row.
        std::vector<int32_t> heights;
        for(const auto& f:code.functions()) {
            bool dock=false;
            for(auto pc:result->margin_callers)if(pc>f.va&&pc<=f.va+f.words.size()*4)dock=true;
            if(!dock)continue;
            const auto& b=f.words;
            for(size_t i=0;i+3<b.size();++i){uint32_t to=0;
                if(target(b[i],f.va+uint32_t(i*4),to)&&to==config&&ldd(b[i+1],0,0)
                    &&field(imm9(b[i+1]))&&ldd(b[i+2],29,1)&&frame_slot(b,imm9(b[i+2]))
                    &&b[i+3]==0x1e603822u)heights.push_back(imm9(b[i+1]));
            }
        }
        std::sort(heights.begin(),heights.end());heights.erase(std::unique(heights.begin(),heights.end()),heights.end());
        if(heights.size()==1)for(const auto& f:code.functions()) {
            const auto& b=f.words;
            for(size_t i=0;i+4<b.size();++i){uint32_t to=0;
                if(!target(b[i],f.va+uint32_t(i*4),to)||to!=result->source.target)continue;
                bool origin=false,center=false;
                if(b[i+1]==0x4ea01c01u&&ldd(b[i+2],29,0)&&frame_slot(b,imm9(b[i+2]))
                    &&b[i+3]==0x1e612802u&&std_(b[i+4],29,2)) {
                    for(size_t j=i+5;j+5<b.size();++j){uint32_t current=0;
                        if(target(b[j],f.va+uint32_t(j*4),current)&&current==config
                            &&ldd(b[j+1],0,1)&&imm9(b[j+1])==heights[0]&&ldd(b[j+2],29,2)
                            &&imm9(b[j+2])==imm9(b[i+4])&&b[j+3]==0x1e612840u
                            &&runtime_contract::epilogue(b,j+4))origin=true;
                    }
                }
                if(i+10<b.size()&&b[i+1]==0x4ea01c03u&&ldd(b[i+2],29,0)
                    &&frame_slot(b,imm9(b[i+2]))&&b[i+5]==0x6e211c21u&&b[i+6]==0x6e221c42u
                    &&(b[i+7]&0xfc000000u)==0x94000000u&&runtime_contract::epilogue(b,i+8)) {
                    for(size_t j=0;j+2<i;++j){uint32_t current=0;
                        if(target(b[j],f.va+uint32_t(j*4),current)&&current==config
                            &&ldd(b[j+1],0,0)&&imm9(b[j+1])==heights[0]&&std_(b[j+2],29,0)
                            &&imm9(b[j+2])==imm9(b[i+2]))center=true;
                    }
                }
                if(origin||center)result->margin_callers.push_back(f.va+uint32_t((i+1)*4));
            }
        }
        std::sort(result->margin_callers.begin(),result->margin_callers.end());
        result->margin_callers.erase(std::unique(result->margin_callers.begin(),result->margin_callers.end()),result->margin_callers.end());
        if(result->margin_callers.size()>32)return {};
    }
    return result;
}
} // namespace home_layout::features
