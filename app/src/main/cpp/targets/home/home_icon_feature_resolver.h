/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_icon_render.h"
#include "home_folder_app_return.h"
#include <map>
#include <optional>
#include <set>

namespace home_layout::features {
struct IconFeature { uint32_t app=0,current=0; IconRenderSites render{}; };
inline std::optional<IconFeature> icon_feature(const Code& code) {
    using namespace home_dynamic;
    std::optional<IconFeature> out;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(!frame_size(b))continue;
        for(size_t i=0;i+8<b.size();++i) {
            uint32_t current=0;
            if(!target(b[i],f.va+uint32_t(i*4),current)||!ldw(b[i+1],0,1)
                ||b[i+2]!=0x8b1c8021u||!ldd(b[i+3],1,1)||!std_(b[i+4],29,1))continue;
            IconRenderSites shape;Body source;
            if(!code.body(current,source)||!frame_size(source)
                ||!icon_render_sites(b,f.va,current,shape)
                ||!frame_slot(b,shape.width_frame)||!frame_slot(b,shape.height_frame))continue;
            if(out)return {};
            out=IconFeature{f.va,current,shape};
        }
    }
    return out;
}
struct MixinFeature {uint32_t va=0,rx=0,config=0;IconMixinSites shape{};};
inline std::vector<MixinFeature> icon_mixins(const Code& code,const IconFeature& icon) {
    using namespace home_dynamic;
    std::vector<MixinFeature> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;uint32_t rx=0;unsigned candidates=0;
        for(size_t i=0;i+5<b.size();++i) {
            uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&ldd(b[i+1],0,1)
                &&b[i+2]==0xa9460740u&&b[i+3]==0x91004000u&&b[i+4]==0xeb00003fu) {
                rx=to;++candidates;
            }
        }
        if(candidates!=1)continue;
        IconMixinSites shape;
        if(!icon_mixin_sites(b,f.va,rx,icon.render.grid,icon.render.value,shape))continue;
        // Dock owns another config producer. Decode it from the alternate path
        // and require width/height to share it; it is not the workspace accessor.
        unsigned dock=0;uint32_t config=0;
        for(size_t i=0;i+4<b.size();++i) {
            uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&to!=rx
                &&ldw(b[i+1],0,1)&&imm9(b[i+1])==icon.render.grid
                &&b[i+2]==0x8b1c8021u&&ldd(b[i+3],1,0)
                &&imm9(b[i+3])==icon.render.value){++dock;config=to;}
        }
        Body source;
        if(dock==1&&code.body(config,source)&&frame_size(source))result.push_back({f.va,rx,config,shape});
    }
    return result;
}
struct DesktopIconFeature {uint32_t draw=0,width=0,height=0,rx=0;IconMixinSites ws{},hs{};};
inline std::optional<DesktopIconFeature> desktop_icon_feature(const Code& code,const IconFeature& icon) {
    using namespace home_dynamic;
    const auto mixins=icon_mixins(code,icon);
    std::map<uint32_t,MixinFeature> by_va;
    for(const auto& m:mixins)by_va.emplace(m.va,m);
    std::optional<DesktopIconFeature> out;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;uint32_t width=0,height=0;unsigned wh=0,hh=0;
        if(!frame_size(b))continue;
        for(size_t i=0;i+5<b.size();++i) {
            uint32_t to=0;if(!target(b[i],f.va+uint32_t(i*4),to)||!by_va.count(to))continue;
            if(ldx(b[i+1],29,0)&&std_(b[i+2],29,0)&&ldw(b[i+3],0,1)&&b[i+4]==0x8b1c8021u){width=to;++wh;}
            if(ldx(b[i+1],29,2)&&std_(b[i+2],29,0)&&ldw(b[i+3],2,3)&&b[i+4]==0x8b1c8063u){height=to;++hh;}
        }
        if(wh!=1||hh!=1||width==height)continue;
        const auto& w=by_va.at(width);const auto& h=by_va.at(height);IconRenderSites drawing;
        if(w.rx!=h.rx||w.config!=h.config||w.shape.value+8!=h.shape.value
            ||!desktop_icon_sites(b,f.va,width,height,drawing)
            ||!frame_slot(b,drawing.width_frame)||!frame_slot(b,drawing.height_frame))continue;
        if(out)return {};
        out=DesktopIconFeature{f.va,width,height,w.rx,w.shape,h.shape};
    }
    return out;
}
struct ProgressIconFeature {uint32_t init=0;IconInitDimensions dimensions{};};
inline std::optional<ProgressIconFeature> progress_icon_feature(const Code& code,
    const IconFeature& icon,const DesktopIconFeature& desktop) {
    using namespace home_dynamic;
    std::optional<ProgressIconFeature> out;
    for(const auto& f:code.functions()) {
        // The lifecycle owner boxes both canonical dimensions. Forwarding
        // ancestors are not additional targets; publish the shared endpoints.
        unsigned boxes=0;
        for(size_t i=0;i+4<f.words.size();++i)
            if((f.words[i]&0xfc000000u)==0x94000000u&&f.words[i+1]==0xa9460740u
                &&f.words[i+2]==0x91004000u&&f.words[i+3]==0xeb00003fu
                &&(f.words[i+4]&0xff00001fu)==0x54000009u)++boxes;
        if(boxes!=2)continue;
        IconInitDimensions shape;
        if(!icon_init_dimensions(f.va,[&](uint32_t va,Body& b){return code.body(va,b);},
            desktop.rx,icon.render.grid,icon.render.value,shape))continue;
        if(shape.width==desktop.width&&shape.height==desktop.height)continue;
        if(out) {
            if(out->dimensions.width!=shape.width||out->dimensions.height!=shape.height)return {};
            continue; // Several real lifecycle callers of the same two banks.
        }
        out=ProgressIconFeature{f.va,shape};
    }
    return out;
}

struct FolderCloseFeature {uint32_t init=0,cache=0;std::array<uint32_t,3> sites{};std::array<uint32_t,2> cache_sites{};};
inline std::optional<FolderCloseFeature> folder_close_feature(const Code& code,const IconFeature& icon) {
    using namespace home_dynamic;
    std::optional<FolderCloseFeature> out;
    for(const auto& f:code.functions()) {
        std::array<uint32_t,3> sites{};
        if(!frame_size(f.words)||!folder_animation_scale_sites(f.words,f.va,icon.current,sites))continue;
        const auto& b=f.words;
        if(sites[0]<12||imm9(b[sites[0]/4-3])!=icon.render.grid
            ||imm9(b[sites[0]/4-1])!=icon.render.value)continue;
        for(const auto& c:code.functions()) {
            std::array<uint32_t,2> cached{};
            if(!frame_size(c.words)||!folder_animation_cache_sites(c.words,c.va,icon.current,icon.render.grid,icon.render.value,cached))continue;
            if(out)return {};
            out=FolderCloseFeature{f.va,c.va,sites,cached};
        }
    }
    return out;
}
struct FolderReturnFeature {uint32_t stable=0,caller=0;std::array<uint32_t,2> sites{};};
inline std::optional<FolderReturnFeature> folder_return_feature(const Code& code,const IconFeature& icon) {
    using namespace home_dynamic;
    std::optional<FolderReturnFeature> out;
    // Enumerate the actual tail caller first, then prove both lengths in its
    // exact destination before the original centering/edit-scale calculation.
    for(const auto& caller:code.functions()) {
        const auto& c=caller.words;
        for(size_t i=1;i+3<c.size();++i) {
            uint32_t to=0;
            if(!ldx(c[i-1],29,2)||c[i+1]!=0xaa1d03efu||c[i+2]!=0xa8c179fdu
                ||c[i+3]!=0xd65f03c0u||!target(c[i],caller.va+uint32_t(i*4),to))continue;
            Body b;std::array<uint32_t,2> sites{};
            if(!code.body(to,b)||!folder_app_return_sites(b,to,icon.current,
                icon.render.grid,icon.render.value,c,caller.va,sites))continue;
            if(out)return {};
            out=FolderReturnFeature{to,caller.va,sites};
        }
    }
    return out;
}
} // namespace home_layout::features
