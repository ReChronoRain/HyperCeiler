/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_folder_layout.h"

namespace home_layout::features {
struct FolderTitleCenter {
    uint32_t text=0,editor=0,center=0;
    int32_t enum_index=0;
    std::array<int32_t,2> axes{};
    FolderInnerContainer inner{};
};
// Roots are current executable bodies, not stored offsets. The anonymous text
// builder links to this header's own style method; parent ownership and its
// original Container/Insets producers are independently validated.
inline std::optional<FolderTitleCenter> folder_title_center(const Code& code,
    uint32_t text,uint32_t text_end,uint32_t editor,uint32_t style,
    const std::array<home_dynamic::Body,7>& roots) {
    using namespace home_dynamic;
    FolderTitleCenter out;
    uint32_t start=0,other=0,decoration=0;
    if(!folder_start_pool(roots[0],start)||!folder_center_pool(roots[1],out.center)
        ||!folder_text_alignment_center_pool(roots[0],other)||other!=out.center
        ||!folder_inner_alignment(roots[3],out.inner.alignment)
        ||!folder_inner_padding(roots[4],out.inner.padding,decoration))return {};
    unsigned gravity=0,axes=0,index=0;
    const auto& g=roots[2];
    for(size_t i=0;i+6<g.size();++i) {
        // Android Gravity.CENTER (0x11), followed by an original pool return.
        size_t count=0;uint32_t pool=0;
        if((g[i]&0xfffffc1fu)==0xf100441fu&&(g[i+1]&0xff00001fu)==0x54000001u
            &&folder_pool_load(g,i+2,0,pool,count)
            &&i+count+4<g.size()&&g[i+count+2]==0xaa1d03efu
            &&g[i+count+3]==0xa8c179fdu&&g[i+count+4]==0xd65f03c0u){out.inner.center_pool=pool;++gravity;}
        // The converter's original Alignment allocator consumes X and Y here.
        if(ldd(g[i],29,0)&&std_(g[i+1],0,0)&&ldd(g[i+2],29,0)
            &&std_(g[i+3],0,0)&&g[i+4]==0xaa1d03efu){
            out.axes={imm9(g[i+1]),imm9(g[i+3])};++axes;
        }
    }
    const auto& p=roots[5];
    for(size_t i=0;i+2<p.size();++i)
        if((p[i]&0xffe00fffu)==0xf8400022u&&p[i+1]==0x93407c42u&&p[i+2]==0xb801b002u)
            {out.enum_index=imm9(p[i]);++index;}
    if(gravity!=1||axes!=1||index!=1||out.enum_index<=0||out.enum_index>255||out.axes[0]==out.axes[1])return {};
    for(auto axis:out.axes)if(axis<=0||axis>255||((axis+1)&7))return {};
    uint32_t editor_site=0;
    if(!folder_text_align_store(roots[6],editor,start,editor_site))return {};
    out.editor=editor+editor_site;
    unsigned texts=0,parents=0;
    for(const auto& f:code.functions()) {
        unsigned styles=0,texts_called=0,editors_called=0;
        for(size_t i=0;i<f.words.size();++i){uint32_t to=0;
            if(!target(f.words[i],f.va+uint32_t(i*4),to))continue;
            styles+=to==style;texts_called+=to==text;editors_called+=to==editor;
        }
        uint32_t site=0;
        if(styles==1&&folder_text_align_store(f.words,f.va,start,site)){out.text=f.va+site;++texts;}
        // Symbol span establishes the closure's owner; instruction contracts
        // establish the feature. New output dispatches child builders through
        // closures instead of direct calls to the named text/editor methods.
        if((texts_called!=1||editors_called!=1)
            &&!(f.va>=text&&uint64_t(f.va)+f.words.size()*4<=text_end))continue;
        auto c=out.inner;
        if(!folder_inner_window(f.words,f.va,c.window,c.owner_slot)
            ||!folder_inner_frame(f.words,f.va,c.window,c.frame)||c.owner_slot>c.frame
            ||!folder_inner_owner_store(f.words,f.va,c.window,c.owner_slot)
            ||!folder_inner_inset(f.words,f.va,c.window,c.frame,c.inset,c.inset_slot)
            ||c.inset_slot==c.owner_slot||c.alignment==c.padding||c.alignment==c.inset||c.padding==c.inset
            ||!folder_inner_directions(f.words,f.va,c.direction_bit,c.direction_pool[0],c.direction_pool[1]))continue;
        out.inner=c;++parents;
    }
    if(texts!=1||parents!=1)return {};
    return out;
}
}
