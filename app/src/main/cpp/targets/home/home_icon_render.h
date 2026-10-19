/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
#include <cstdlib>

namespace home_layout {
struct IconRenderSites {uint32_t width=0,height=0;int32_t grid=0,value=0,width_frame=0,height_frame=0;};
// Resolve the actual width/height inputs of AppIcon.build. AOT bypasses both
// IconConfigHandler.calIconSize and _IconConfig.getIconSize on the Rust grid path.
// The source banks change scalar registers only, before the original frame stores.
inline bool icon_render_sites(const home_dynamic::Body& b,uint32_t va,uint32_t current,IconRenderSites& out) {
    using namespace home_dynamic;
    IconRenderSites next;unsigned widths=0,heights=0;
    for(size_t i=0;i+8<b.size();++i){
        uint32_t callee=0;
        if(!target(b[i],va+uint32_t(i*4),callee)||callee!=current
          || !ldw(b[i+1],0,1)||b[i+2]!=0x8b1c8021u||!field(imm9(b[i+1])))continue;
        if(ldd(b[i+3],1,1)&&std_(b[i+4],29,1)&&slot(imm9(b[i+4]))
          &&ux(b[i+5],26,0)&&ux(b[i+6],0,0)&&b[i+7]==0x6b16001fu){
            ++widths;next.width=uint32_t((i+4)*4);next.grid=imm9(b[i+1]);
            next.value=imm9(b[i+3]);next.width_frame=imm9(b[i+4]);
        }
    }
    if(widths!=1||!field(next.value))return false;
    for(size_t i=0;i+8<b.size();++i){
        uint32_t callee=0;
        if(!target(b[i],va+uint32_t(i*4),callee)||callee!=current
          ||!ldw(b[i+1],0,1)||imm9(b[i+1])!=next.grid||b[i+2]!=0x8b1c8021u
          ||!ldd(b[i+3],1,0)||imm9(b[i+3])!=next.value
          ||!ldx(b[i+4],29,0)||!std_(b[i+5],29,0)||!slot(imm9(b[i+5]))
          ||!ldw(b[i+6],0,2)||b[i+7]!=0x8b1c8042u)continue;
        ++heights;next.height=uint32_t((i+4)*4);next.height_frame=imm9(b[i+5]);
    }
    if(heights!=1||next.height_frame==next.width_frame||next.height<=next.width+16)return false;
    unsigned consumed=0;
    for(size_t i=next.height/4+4;i+6<b.size();++i){
        if(!ldd(b[i],29,0)||imm9(b[i])!=next.height_frame
          ||!ldd(b[i+2],29,1)||imm9(b[i+2])!=next.width_frame)continue;
        uint32_t callee=0;if(target(b[i+5],va+uint32_t((i+5)*4),callee))++consumed;
    }
    if(consumed!=1)return false;
    out=next;return true;
}

// Follow the widget's unique typed wrapper into the matching mixin copy. The AOT
// contains other mixin instantiations with identical names; never pick the first.
inline bool icon_dimension_callee(const home_dynamic::Body& b,uint32_t va,uint32_t& out) {
    unsigned hits=0;uint32_t next=0;
    for(size_t i=0;i+3<b.size();++i)if(home_dynamic::target(b[i],va+uint32_t(i*4),next)
      &&b[i+1]==0xaa1d03efu&&b[i+2]==0xa8c179fdu&&b[i+3]==0xd65f03c0u){++hits;out=next;}
    return hits==1;
}
inline bool desktop_icon_sites(const home_dynamic::Body& b,uint32_t va,uint32_t width,uint32_t height,IconRenderSites& out) {
    using namespace home_dynamic;IconRenderSites next;unsigned wh=0,hh=0;
    for(size_t i=0;i+5<b.size();++i){uint32_t to=0;
        if(!target(b[i],va+uint32_t(i*4),to))continue;
        if(to==width&&ldx(b[i+1],29,0)&&std_(b[i+2],29,0)&&ldw(b[i+3],0,1)&&b[i+4]==0x8b1c8021u){
            ++wh;next.width=uint32_t((i+1)*4);next.width_frame=imm9(b[i+2]);next.grid=imm9(b[i+3]);}
        if(to==height&&ldx(b[i+1],29,2)&&std_(b[i+2],29,0)&&ldw(b[i+3],2,3)&&b[i+4]==0x8b1c8063u){
            ++hh;next.height=uint32_t((i+1)*4);next.height_frame=imm9(b[i+2]);}
    }
    if(wh!=1||hh!=1||!slot(next.width_frame)||!slot(next.height_frame)
      ||next.width_frame==next.height_frame||next.height<=next.width+16||!field(next.grid))return false;
    out=next;return true;
}

struct IconMixinSites {uint32_t normal=0,dock=0;int32_t value=0;};
inline bool icon_mixin_sites(const home_dynamic::Body& b,uint32_t va,uint32_t rx,
    int32_t grid,int32_t size,IconMixinSites& out) {
    using namespace home_dynamic;IconMixinSites next;unsigned normal=0,dock=0;
    if(b.size()<8||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu)return false;
    for(size_t i=0;i+8<b.size();++i){uint32_t targetVa=0;
        if(!target(b[i],va+uint32_t(i*4),targetVa))continue;
        size_t start=0;
        if(targetVa==rx&&ldd(b[i+1],0,1)&&field(imm9(b[i+1]))){
            start=i+2;next.value=imm9(b[i+1]);}
        else if(ldw(b[i+1],0,1)&&imm9(b[i+1])==grid&&b[i+2]==0x8b1c8021u
          &&ldd(b[i+3],1,0)&&imm9(b[i+3])==size)start=i+4;
        if(!start||start+4>b.size()||b[start]!=0xa9460740u||b[start+1]!=0x91004000u
          ||b[start+2]!=0xeb00003fu||(b[start+3]&0xff00001fu)!=0x54000009u)continue;
        if(targetVa==rx){++normal;next.normal=uint32_t(start*4);}else{++dock;next.dock=uint32_t(start*4);}
    }
    if(normal!=1||dock!=1||next.normal==next.dock)return false;out=next;return true;
}

struct SmallFolderScaleSite {
    uint32_t caller_return=0;
    std::array<int32_t,4> dimensions{}; // background width/height, preview width/height
};
// Both the Dock constants and workspace config paths overwrite all four dimensions
// before calling the base padding calculator. Scale there, once per recomputation,
// so the original padding, preview and animation consumers see the same geometry.
inline bool small_folder_scale_site(const home_dynamic::Body& b,uint32_t va,
    const home_dynamic::Body& base,uint32_t base_va,uint32_t current,
    uint32_t preview_height,SmallFolderScaleSite& out) {
    using namespace home_dynamic;
    if(b.size()<12||base.size()<4||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu
      ||base[0]!=0xa9bf79fdu||base[1]!=0xaa0f03fdu
      ||(base[2]&0xffc003ffu)!=0xd10001efu||!stx(base[3],29,1)
      ||!slot(imm9(base[3])))return false;
    SmallFolderScaleSite next;unsigned dock=0,config=0,height=0,tail=0;
    uint32_t dock_end=0;int32_t config_field=0,value_field=0;
    for(size_t i=0;i+4<b.size();++i){uint32_t to=0;
        if(std_(b[i],1,1)&&std_(b[i+1],1,1)&&std_(b[i+2],1,0)&&std_(b[i+3],1,0)
          &&(b[i+4]&0xfc000000u)==0x14000000u){
            if(++dock!=1)return false;
            next.dimensions={imm9(b[i]),imm9(b[i+1]),imm9(b[i+2]),imm9(b[i+3])};
            const int64_t end=int64_t((i+4)*4)+(int32_t(b[i+4]<<6)>>6)*int64_t{4};
            if(end<0||end>=int64_t(b.size()*4))return false;
            dock_end=uint32_t(end);
        }
        if(!target(b[i],va+uint32_t(i*4),to))continue;
        if(to==current&&i+5<b.size()&&ldw(b[i+1],0,1)&&b[i+2]==0x8b1c8021u
          &&ldd(b[i+3],1,0)&&ldx(b[i+4],29,1)&&std_(b[i+5],1,0)){
            if(config>=3||imm9(b[i+4])!=imm9(base[3]))return false;
            if(config==0){config_field=imm9(b[i+1]);value_field=imm9(b[i+3]);}
            if((config<2&&imm9(b[i+1])!=config_field)||imm9(b[i+3])!=value_field)return false;
            if(imm9(b[i+5])!=next.dimensions[config])return false;
            ++config;
        }
        if(to==preview_height&&ldx(b[i+1],29,1)&&std_(b[i+2],1,0)){
            if(imm9(b[i+1])!=imm9(base[3])||imm9(b[i+2])!=next.dimensions[3])return false;
            ++height;
        }
        if(to==base_va&&b[i+1]==0xaa1603e0u&&b[i+2]==0xaa1d03efu
          &&b[i+3]==0xa8c179fdu&&b[i+4]==0xd65f03c0u){
            ++tail;next.caller_return=uint32_t((i+1)*4);
            if(dock_end!=i*4)return false;
        }
    }
    if(dock!=1||config!=3||height!=1||tail!=1||!field(config_field)||!field(value_field))return false;
    for(unsigned i=0;i<4;++i){
        if(!field(next.dimensions[i])||next.dimensions[i]>255)return false;
        for(unsigned j=0;j<i;++j)if(std::abs(next.dimensions[i]-next.dimensions[j])<8)return false;
    }
    out=next;return true;
}

struct BigFolderScaleSite {
    uint32_t caller_return=0;
    std::array<int32_t,13> dimensions{};
};
// Large strategies finish both preview helpers before entering the same padding
// calculator. Their dimensions, gaps and corner-preview offsets are all lengths;
// scale the finished geometry together, leaving grid cells and counts untouched.
inline bool big_folder_scale_site(const home_dynamic::Body& producer,uint32_t va,
    const std::array<home_dynamic::Body,2>& helpers,const uint32_t helper_va[2],
    const home_dynamic::Body& preview,uint32_t base,
    const SmallFolderScaleSite& small,BigFolderScaleSite& out) {
    using namespace home_dynamic;
    BigFolderScaleSite next;size_t count=0;unsigned calls[3]{};
    auto add=[&](int32_t field_offset){
        if(!field(field_offset)||field_offset>255)return false;
        for(size_t i=0;i<count;++i){
            if(next.dimensions[i]==field_offset)return true;
            if(std::abs(next.dimensions[i]-field_offset)<8)return false;
        }
        if(count==next.dimensions.size())return false;
        next.dimensions[count++]=field_offset;return true;
    };
    // The shared background fields must agree with the independent small-folder
    // producer. The aliases are reset on every large-folder size calculation.
    unsigned backgrounds=0;
    for(size_t i=0;i+3<producer.size();++i)
        if(std_(producer[i],2,1)&&std_(producer[i+1],2,0)
          &&std_(producer[i+2],2,1)&&std_(producer[i+3],2,0)){
            for(unsigned j=0;j<4;++j)if(imm9(producer[i+j])!=small.dimensions[j])return false;
            ++backgrounds;
        }
    if(backgrounds!=1)return false;
    for(size_t i=0;i<producer.size();++i){uint32_t to=0;
        if(target(producer[i],va+uint32_t(i*4),to)){
            for(unsigned j=0;j<2;++j)if(to==helper_va[j])++calls[j];
            if(to==base){
                if(i+4>=producer.size()||producer[i+1]!=0xaa1603e0u
                  ||producer[i+2]!=0xaa1d03efu||producer[i+3]!=0xa8c179fdu
                  ||producer[i+4]!=0xd65f03c0u)return false;
                ++calls[2];next.caller_return=uint32_t((i+1)*4);
            }
        }
    }
    if(calls[0]!=1||calls[1]!=1||calls[2]!=1)return false;
    for(const Body* body:{&producer,&helpers[0],&helpers[1]}){
        if(body->size()<12||(*body)[0]!=0xa9bf79fdu||(*body)[1]!=0xaa0f03fdu)return false;
        for(uint32_t w:*body)
            if((w&0xffe00c00u)==0xfc000000u&&(rn(w)==1||rn(w)==2))
                if(!add(imm9(w)))return false;
    }
    if(count!=next.dimensions.size())return false;
    // Every extra geometry field must actually feed the large preview builder.
    // Background/alias dimensions are consumed by the shared layout instead.
    for(int32_t offset:next.dimensions){
        bool used=false;
        for(int32_t common:small.dimensions)used|=offset==common;
        for(uint32_t w:preview)
            if((w&0xffe00c00u)==0xfc400000u&&rn(w)!=29&&imm9(w)==offset)used=true;
        if(!used)return false;
    }
    out=next;return true;
}
// Progress icons instantiate another ShortCutCommonMixin. Resolve that instance
// through its typed lifecycle chain, never the first duplicate mixin symbol.
struct IconInitDimensions {uint32_t width=0,height=0;IconMixinSites ws{},hs{};};
template<class Read>
inline bool icon_init_dimensions(uint32_t root,Read read,uint32_t rx,int32_t grid,
    int32_t size,IconInitDimensions& out) {
    using namespace home_dynamic;
    std::array<uint32_t,4> visited{};
    for(unsigned depth=0;depth<visited.size();++depth){
        for(unsigned j=0;j<depth;++j)if(visited[j]==root)return false;
        visited[depth]=root;Body b;
        if(!read(root,b)||b.size()<8||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu)return false;
        IconInitDimensions next;unsigned hits=0;uint32_t first=0;
        for(size_t i=2;i+4<b.size();++i){uint32_t to=0;
            if(!target(b[i],root+uint32_t(i*4),to))continue;
            const bool receiver_forward=b[i-1]==0xaa0003e1u
                ||(stx(b[i-1],29,1)&&slot(imm9(b[i-1]))&&b[i-2]==0xaa0103e0u);
            if(!first&&receiver_forward)first=to;
            // An unboxed double returned by the dimension getter is boxed by
            // the original init frame immediately after the call.
            if(b[i+1]!=0xa9460740u||b[i+2]!=0x91004000u||b[i+3]!=0xeb00003fu
              ||(b[i+4]&0xff00001fu)!=0x54000009u)continue;
            Body callee;IconMixinSites shape;
            if(!read(to,callee)||!icon_mixin_sites(callee,to,rx,grid,size,shape))return false;
            if(++hits==1){next.width=to;next.ws=shape;}
            else if(hits==2){next.height=to;next.hs=shape;}
            else return false;
        }
        if(hits){if(hits!=2||next.width==next.height||next.ws.value+8!=next.hs.value)return false;
            out=next;return true;}
        if(!first||first==root)return false;
        root=first;
    }
    return false;
}
}

namespace home_layout {
// All three raw grid dimensions feed closing-preview scale/size, including the
// selected icon's live resize path. Keep numerator and rendered AppIcon in the same units.
inline bool folder_animation_scale_sites(const home_dynamic::Body& b,uint32_t va,
    uint32_t current,std::array<uint32_t,3>& out){
    using namespace home_dynamic;unsigned hits=0;int32_t grid=0,value=0;
    std::array<uint32_t,3> next{};
    for(size_t i=0;i+7<b.size();++i){uint32_t to=0;
        if(!target(b[i],va+i*4,to)||to!=current||!ldw(b[i+1],0,1)
            ||b[i+2]!=0x8b1c8021u||!ldd(b[i+3],1,0))continue;
        if(hits==3)return false;
        if(!hits){grid=imm9(b[i+1]);value=imm9(b[i+3]);}
        if(imm9(b[i+1])!=grid||imm9(b[i+3])!=value)return false;
        if(hits<2){
            if(!ldx(b[i+4],29,0)||!std_(b[i+5],29,0)||!ldw(b[i+6],0,1)
                ||b[i+7]!=0x8b1c8021u||!slot(imm9(b[i+4]))||!slot(imm9(b[i+5])))return false;
        }else if(b[i+4]!=0xa9460740u||b[i+5]!=0x91004000u||b[i+6]!=0xeb00003fu
            ||(b[i+7]&0xff00001fu)!=0x54000009u)return false;
        next[hits++]=(i+4)*4;
    }
    if(hits!=3||!field(grid)||!field(value))return false;out=next;return true;
}
}
