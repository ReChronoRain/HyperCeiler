/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_folder_feature_resolver.h"
#include "home_runtime_layout_contracts.h"
namespace home_layout::features {
struct MediumFolderFeature {
    uint32_t producer=0,base=0,span_site=0,preview=0;
    BigFolderScaleSite geometry{};
};
// Prove a folder's span -> size tuple -> background/preview geometry -> padding
// graph. The original boxed span arguments are observed, never a CID/version.
inline std::optional<MediumFolderFeature> medium_folder_feature(const Code& code,uint32_t config) {
    using namespace home_dynamic;using namespace folder_detail;
    std::optional<MediumFolderFeature> result;
    for(const auto& function:code.functions()) {
        const auto& b=function.words;if(!frame(b))continue;
        SmallFolderScaleSite common;unsigned backgrounds=0,span_count=0,base_count=0;
        uint32_t base=0,span_site=0;
        for(size_t i=0;i+10<b.size();++i) {
            if(std_(b[i],2,1)&&std_(b[i+1],2,0)&&std_(b[i+2],2,1)&&std_(b[i+3],2,0)) {
                for(unsigned j=0;j<4;++j)common.dimensions[j]=imm9(b[i+j]);++backgrounds;
            }
            uint32_t size=0;Body source;
            if(b[i]==0xaa0203e1u&&b[i+1]==0xaa0003e2u&&b[i+2]==0x910082c3u
                &&(b[i+3]&0xffc003ffu)==0xf9400364u
                &&target(b[i+4],function.va+uint32_t((i+4)*4),size)
                &&ldw(b[i+5],0,1)&&b[i+6]==0x8b1c8021u&&stx(b[i+7],29,1)
                &&frame_slot(b,imm9(b[i+7]))&&ldw(b[i+8],0,2)&&b[i+9]==0x8b1c8042u
                &&stx(b[i+10],29,2)&&frame_slot(b,imm9(b[i+10]))&&code.body(size,source)) {
                // Both returned record fields must be written by the size producer.
                // Its consumers unbox them as doubles before writing the folder lengths.
                bool first=false,second=false;
                for(auto w:source)if((w&0xffe00c00u)==0xb8000000u&&(rn(w)==0||rn(w)==1)) {
                    first|=imm9(w)==imm9(b[i+5]);second|=imm9(w)==imm9(b[i+8]);
                }
                if(first&&second&&imm9(b[i+5])!=imm9(b[i+8])){span_site=uint32_t(i*4);++span_count;}
            }
        }
        for(size_t i=0;i+4<b.size();++i)if(tail(b,i)&&target(b[i],function.va+uint32_t(i*4),base))++base_count;
        if(backgrounds!=1||span_count!=1||base_count!=1)continue;
        Body padding;
        if(!code.body(base,padding)||!frame(padding)||!frame_size(padding)
            ||!stx(padding[3],29,1)||!frame_slot(padding,imm9(padding[3])))continue;
        std::vector<uint32_t> candidates;
        for(size_t i=0;i<b.size();++i){uint32_t to=0;Body body;
            if(target(b[i],function.va+uint32_t(i*4),to)&&to!=base&&to!=config
                &&code.body(to,body)&&frame(body)&&!stores(body).empty()
                &&std::find(candidates.begin(),candidates.end(),to)==candidates.end())candidates.push_back(to);
        }
        for(size_t a=0;a<candidates.size();++a)for(size_t c=a+1;c<candidates.size();++c) {
            uint32_t helpers_va[]={candidates[a],candidates[c]};std::array<Body,2> helpers;
            if(!code.body(helpers_va[0],helpers[0])||!code.body(helpers_va[1],helpers[1]))continue;
            auto fields=stores(b);
            for(const auto& helper:helpers){auto f=stores(helper);fields.insert(f.begin(),f.end());}
            if(fields.size()!=13)continue;
            for(auto field:common.dimensions)fields.erase(field);
            if(fields.size()!=9)continue;
            for(const auto& preview:code.functions()) {
                if(!frame(preview.words)||!preview_owner(code,preview,fields))continue;
                BigFolderScaleSite shape;
                if(!big_folder_scale_site(b,function.va,helpers,helpers_va,preview.words,base,common,shape))continue;
                if(result)return {};
                result=MediumFolderFeature{function.va,base,span_site,preview.va,shape};
            }
        }
    }
    return result;
}
// Smi ABI is independent of the current image's class/field layout. Unknown or
// boxed arguments are not classified. Horizontal and vertical never share a gate.
inline unsigned medium_folder_kind(uint64_t width,uint64_t height) {
    if((width|height)&1)return 0;
    if(width==6&&height==2)return 1;
    if(width==2&&height==6)return 2;
    return 0;
}
}
