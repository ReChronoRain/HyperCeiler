/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <span>
namespace home_layout {
// Only scalars survive a Dart frame/GC. The delegate expectation scopes Flutter's
// shared getLayout consumer to the folder builder; an unrelated grid is ignored.
struct FolderPreviewSnapshot {
    uint64_t stamp = 0; int cols = 0; double main_gap=0, cross_gap=0, aspect=0, grid_width=0;
    double main_stride=0, cross_stride=0, child_height=0, child_width=0;
    bool valid=false, padding_valid=false; double padding=0; uint64_t padding_stamp=0;
    static bool near(double a,double b) { return std::isfinite(a)&&std::isfinite(b)&&std::abs(a-b)<0.0001; }
    void expect(int n,double mg,double cg,double ar,double gw,uint64_t st) {
        const bool same=stamp==st&&cols==n&&near(main_gap,mg)&&near(cross_gap,cg)&&near(aspect,ar)&&near(grid_width,gw);
        if(!same) valid=false;
        stamp=st;cols=n;main_gap=mg;cross_gap=cg;aspect=ar;grid_width=gw;
    }
    bool capture(int n,double ms,double cs,double ch,double cw,uint64_t st) {
        if(st!=stamp||n!=cols||n<3||n>6||cw<24||cw>2000||ch<24||ch>2000
            ||!near(ms-ch,main_gap)||!near(cs-cw,cross_gap)||!near(cw/ch,aspect)
            ||!std::isfinite(grid_width)||grid_width<24*n) return false;
        main_stride=ms;cross_stride=cs;child_height=ch;child_width=cw;valid=true;return true;
    }
    bool delta(int index,double rw,double rh,double raw_inset_y,uint64_t st,double &dx,double &dy) const {
        if(!valid||st!=stamp||index<0||index>10000||!std::isfinite(rw)||!std::isfinite(rh)
            ||rw<24||rh<24||rw>2000||rh>2000||!std::isfinite(raw_inset_y)||std::abs(raw_inset_y)>2000) return false;
        const double viewport=cols*child_width+(cols-1)*cross_gap;
        // The layout engine clamps the requested grid to its actual constraints.
        // Animation still used the unclamped cells and icon-centering insets.
        dx=(grid_width-viewport)/2+(child_width-rw)/2+(index%cols)*(cross_stride-rw-cross_gap);
        // RenderFlex centers only positive remaining main-axis space; overflowing
        // icon+title columns start at zero, not a negative centered inset.
        dy=std::max(0.0,raw_inset_y+(child_height-rh)/2)-raw_inset_y
            +(index/cols)*(main_stride-rh-main_gap);
        return std::isfinite(dx)&&std::isfinite(dy)&&std::abs(dx)<4000&&std::abs(dy)<5000;
    }
    // Match the painted icon, not the requested cell. Narrow cells constrain the
    // square icon inside the original two-sided folderItemPadding. RenderFlex's
    // vertical overflow starts at zero. The scale endpoints must use that same
    // painted size, otherwise the static thumbnail enlarges on the last frame.
    bool painted(double rw,double rh,double iw,double ih,double ix,double iy,
                 uint64_t st,double &ax,double &ay,double &extra_x,double &ratio) const {
        if(!valid||stamp!=st||!padding_valid||padding_stamp!=st
            ||!std::isfinite(padding)||padding<0||padding>64
            ||!std::isfinite(iw)||!std::isfinite(ih)||iw<8||iw>2000||!near(iw,ih)
            ||!std::isfinite(ix)||!std::isfinite(iy)||std::abs(ix)>2000||std::abs(iy)>2000
            ||!std::isfinite(rw)||!std::isfinite(rh)||rw<24||rh<24) return false;
        const double pw=std::min(iw,child_width-2*padding);
        if(pw<8||pw>2000) return false;
        ax=(child_width-pw)/2; ay=std::max(0.0,iy+(child_height-rh)/2);
        extra_x=ax-ix-(child_width-rw)/2; ratio=iw/pw;
        return std::isfinite(ax)&&std::isfinite(ay)&&std::isfinite(extra_x)
            &&std::isfinite(ratio)&&ratio>=1&&ratio<8;
    }
    // _calcFolderPreviewLoc uses the same configured cells/insets as the normal
    // close source. Apply the SAME constrained geometry kernel to its fresh Offset,
    // including non-square cells and RenderFlex's zero-clamped remaining space.
    bool destination(int index,double rw,double rh,double inset_y,double anim_x,double anim_y,
                     uint64_t st,double &x,double &y) const {
        if (!std::isfinite(anim_x) || !std::isfinite(anim_y)) return false;
        double dx=0,dy=0;
        if (!delta(index,rw,rh,inset_y,st,dx,dy)) return false;
        x=anim_x+dx;y=anim_y+dy;
        return std::isfinite(x)&&std::isfinite(y)&&std::abs(x)<4000&&std::abs(y)<5000;
    }

};
// Carry only scalar index/generation and a live stack-frame identity, never Dart
// objects. Nested original calculations keep independent bounded entries.
struct FolderPreviewIndexCarry {
    struct Entry { uintptr_t frame=0; uint64_t stamp=0; int64_t index=-1; };
    std::array<Entry,8> entries{};
    void discard(uintptr_t frame) { for(auto& e:entries)if(e.frame==frame)e={}; }
    void put(uintptr_t frame,uint64_t stamp,int64_t index) {
        discard(frame);
        if(!frame||(frame&7)||index<0||index>10000)return;
        for(auto& e:entries)if(!e.frame){e={frame,stamp,index};return;}
        // Overflow invalidates the pending carries, rather than using a different cell.
        entries={};entries[0]={frame,stamp,index};
    }
    int64_t take(uintptr_t frame,uint64_t stamp) {
        for(auto& e:entries)if(e.frame==frame){const auto result=e.stamp==stamp?e.index:-1;e={};return result;}
        return -1;
    }
};
struct FolderPreviewPlan {
    std::array<uint32_t,3> site{};
    uint32_t probe_site=UINT32_MAX, anchor_site=UINT32_MAX;
    uint32_t source_alloc_site=UINT32_MAX, anchor_alloc_site=UINT32_MAX, real_alloc_site=UINT32_MAX;
    int anchor_frame=0, scale_x_frame=0, scale_y_frame=0;
    uint32_t padding_site=UINT32_MAX; int config_cell=0, cell_padding=0, cache_icon_width=0, cache_icon_height=0;
    // FolderAnimController._calcFolderPreviewLoc: the destination preview is built there, so the
    // index that selects the cell is still a live register. Two windows are spliced - the one that
    // has just materialised `index` in x2, and the one that stores the finished Offset into the
    // controller - because they are in the same function and the correction must stay scoped to it.
    uint32_t destination_site=UINT32_MAX, destination_store=UINT32_MAX;
    int preview_index=0, destination_controller=0, destination_frame=0, destination_field=0;
    int width_owner_gap=0;
    int context_local=0, context_controller=0, grid_width=0, cache_width=0, cache_height=0, item_local=0;
    int delegate_count=0, delegate_main=0, delegate_cross=0;
    int source_offset=0, edit_flag=0, cache_inset_y=0, cache_inset_x=0;
    int layout_count=0, layout_main_stride=0, layout_cross_stride=0, layout_height=0, layout_width=0;
};
inline int preview_imm9(uint32_t w){int n=int((w>>12)&511);return n&256?n-512:n;}
inline bool preview_window(std::span<const uint32_t>b,uint32_t at){
    if(at%4||at/4+4>b.size())return false;
    for(size_t i=0;i<b.size();++i){uint32_t w=b[i];int32_t displacement=0;bool branch=false;
        if((w&0x7c000000)==0x14000000){displacement=int32_t(w<<6)>>6;branch=true;}
        else if((w&0xff000010)==0x54000000||(w&0x7e000000)==0x34000000){displacement=int32_t(w<<8)>>13;branch=true;}
        else if((w&0x7e000000)==0x36000000){displacement=int32_t(w<<13)>>18;branch=true;}
        if(i>=at/4&&i<at/4+4 && (branch||(w&0xfffffc1f)==0xd63f0000))return false;
        if(branch){const int64_t target=int64_t(i*4)+int64_t(displacement)*4;if(target>at&&target<at+16)return false;}
    }return true;
}
inline bool preview_call_to(uint32_t w,uint32_t pc,uint32_t target){
    return (w&0xfc000000u)==0x94000000u
        &&int64_t(pc)+int64_t(int32_t(w<<6)>>6)*4==target;
}
inline bool folder_preview_plan(std::span<const uint32_t>build,std::span<const uint32_t>layout,
        std::span<const uint32_t>close,std::span<const uint32_t>cache,std::span<const uint32_t>width_owner,
        uint32_t build_va,uint32_t height_va,std::span<const uint32_t> end,std::span<const uint32_t> real,
        std::span<const uint32_t> preview,std::span<const uint32_t> init,std::span<const uint32_t> padding,FolderPreviewPlan &out){
    FolderPreviewPlan p;unsigned builders=0,contexts=0,layouts=0,closes=0,indices=0;
    for(size_t i=0;i+15<build.size();++i){
        // Fresh SliverGridDelegate: count, both spacings, then its aspect ratio.
        if((build[i]&0xffe00fff)!=0xf8000020||build[i+1]!=0xfc5b03a0
            ||(build[i+2]&0xffe00fff)!=0xfc000020||build[i+3]!=0xfc5a83a0
            ||(build[i+4]&0xffe00fff)!=0xfc000020||build[i+5]!=0xfc5983a0
            ||(build[i+6]&0xffe00fff)!=0xfc000020)continue;
        p.delegate_count=preview_imm9(build[i]);p.delegate_main=preview_imm9(build[i+2]);p.delegate_cross=preview_imm9(build[i+4]);p.site[0]=uint32_t((i+6)*4);++builders;
    }
    for(size_t i=0;i+4<build.size();++i){
        if((build[i]&0xffe00fff)!=0xf84003a2 || (build[i+1]&0xffe00fff)!=0xfc0003a0
            ||(build[i+2]&0xffe00fff)!=0xb8400041||build[i+3]!=0x8b1c8021
            ||(build[i+4]&0xfc000000)!=0x94000000)continue;
        int32_t d=int32_t(build[i+4]<<6)>>6;
        if(int64_t(build_va+(i+4)*4)+int64_t(d)*4!=height_va)continue;
        p.context_local=preview_imm9(build[i]);p.context_controller=preview_imm9(build[i+2]);++contexts;
    }
    for(size_t i=0;i+13<layout.size();++i){
        if((layout[i]&0xffe00fff)!=0xf8000001)continue;
        if((layout[i+2]&0xffe00fff)!=0xfc000000||(layout[i+4]&0xffe00fff)!=0xfc000000
            ||(layout[i+6]&0xffe00fff)!=0xfc000000||(layout[i+8]&0xffe00fff)!=0xfc000000
            ||layout[i+11]!=0xaa1d03ef||layout[i+12]!=0xa8c179fd||layout[i+13]!=0xd65f03c0)continue;
        p.layout_count=preview_imm9(layout[i]);p.layout_main_stride=preview_imm9(layout[i+2]);p.layout_cross_stride=preview_imm9(layout[i+4]);p.layout_height=preview_imm9(layout[i+6]);p.layout_width=preview_imm9(layout[i+8]);p.site[1]=uint32_t((i+10)*4);++layouts;
    }
    for(size_t i=0;i+3<close.size();++i){
        if(close[i]==0xfc407083&&close[i+1]==0x1e633844&&(close[i+2]&0xffe0001f)==0xd2800011&&close[i+3]==0xb8716825){if(i<2||i+9>=close.size()||close[i+9]!=0xfc40f085||close[i-1]!=0x8b1c8084||(close[i-2]&0xffe00fff)!=0xb8400024)return false;
            if(i<5||(close[i-5]&0xffe00fff)!=0xfc0003a1||(close[i-4]&0xffe00fff)!=0xfc0003a0) return false;
            p.scale_x_frame=preview_imm9(close[i-5]);p.scale_y_frame=preview_imm9(close[i-4]);
            if(p.scale_x_frame>=0||p.scale_y_frame>=0||p.scale_x_frame==p.scale_y_frame)return false;
            p.source_offset=preview_imm9(close[i-2]);p.site[2]=uint32_t(i*4);++closes;}
        if(i<12&&(close[i]&0xffe00fff)==0xf80003a3&&preview_imm9(close[i])<0){p.item_local=preview_imm9(close[i]);++indices;}
    }
    // Width/height are boxed in the cache from the owning currentConfig metrics.
    for(size_t i=0;i+30<cache.size();++i){
        if(cache[i]!=0xfc40f020&&cache[i]!=0xfc407020)continue;
        for(size_t j=i+1;j<i+30&&j+1<cache.size();++j){
            if((cache[j]&0xffe0001f)==0xd2800011&&(cache[j+1]==0xb8316840||cache[j+1]==0xb8316820)){
                int f=int((cache[j]>>5)&65535);
                if(cache[i]==0xfc40f020){if(p.cache_width)return false;p.cache_width=f;}
                else {if(p.cache_height)return false;p.cache_height=f;}break;
            }
        }
    }
    unsigned pads=0,cache_pads=0,icons=0;
    for(size_t i=0;i+2<padding.size();++i)
        if((padding[i]&0xffe00fff)==0xb8400001&&padding[i+1]==0x8b1c8021
            &&(padding[i+2]&0xffe00fff)==0xfc400020){
            p.config_cell=preview_imm9(padding[i]);p.cell_padding=preview_imm9(padding[i+2]);++pads;
        }
    for(size_t i=0;i+3<cache.size();++i){
        if((cache[i]&0xffe00fff)==0xb8400001&&preview_imm9(cache[i])==p.config_cell
            &&cache[i+1]==0x8b1c8021&&cache[i+2]==0xfc40f020&&cache[i+3]==0xa9460740){
            p.padding_site=uint32_t(i*4);++cache_pads;
        }
        if(cache[i]!=0xfc417020)continue; // original config iconSize
        for(size_t j=i+1;j<i+30&&j+1<cache.size();++j)
            if((cache[j]&0xffe0001f)==0xd2800011&&(cache[j+1]==0xb8316840||cache[j+1]==0xb8316820)){
                int f=int((cache[j]>>5)&65535);
                if(icons==0)p.cache_icon_width=f;else if(icons==1)p.cache_icon_height=f;
                ++icons;break;
            }
    }
    if(pads!=1||cache_pads!=1||icons!=2||p.config_cell<=0||p.cell_padding<=0
        ||!preview_window(cache,p.padding_site))return false;
    // calGridWidth writes this exact FolderGridViewGetxController receiver.
    // gridContainerWidth's same-looking field belongs to GridConfig and is NOT
    // evidence for this object, even when both happen to be +0x53 in one build.
    unsigned width_owners=0;
    for(size_t i=7;i+4<width_owner.size();++i){
        if((width_owner[i]&0xffe00fff)!=0xfc000040||width_owner[i-1]!=0x1e632840
            ||width_owner[i-2]!=0x1e610803||width_owner[i-3]!=0x9e620021
            ||width_owner[i-4]!=0xd1000461||width_owner[i-5]!=0x93417c23
            ||(width_owner[i-6]&0xffe00fff)!=0xfc400040||width_owner[i-7]!=0xf85f83a2
            ||width_owner[i+1]!=0xaa1603e0||width_owner[i+2]!=0xaa1d03ef
            ||width_owner[i+3]!=0xa8c179fd||width_owner[i+4]!=0xd65f03c0)continue;
        p.grid_width=preview_imm9(width_owner[i]);p.width_owner_gap=preview_imm9(width_owner[i-6]);++width_owners;
    }
    if(width_owners!=1)return false;
    // The normal close source is a freshly allocated Offset, not a pooled
    // constant. The editing alternative replaces it; leave that path untouched.
    unsigned sources=0,flags=0;
    for(size_t i=0;i+7<end.size();++i){
        if(i>0&&(end[i-1]&0xfc000000)==0x94000000&&end[i]==0xaa0003e1&&end[i+2]==0xfc007020&&end[i+4]==0xfc00f020
            &&end[i+5]==0xaa0103e0&&(end[i+7]&0xffe00fff)==0xb8000040
            &&preview_imm9(end[i+7])==p.source_offset){p.source_alloc_site=uint32_t((i-1)*4);++sources;}
        if((end[i]&0xffe0001f)==0xd2800011&&end[i+1]==0xb8716840
            &&end[i+2]==0x8b1c8000&&(end[i+3]&0xfff8001f)==0x37200000){
            p.edit_flag=int((end[i]>>5)&65535);++flags;
        }
    }
    unsigned insets=0;
    for(size_t i=0;i+4<real.size();++i){
        if((real[i]&0xffe0001f)==0xd2800011&&real[i+1]==0xb8716820
            &&real[i+2]==0x8b1c8000&&real[i+3]==0xfc407001&&real[i+4]==0x1e612803){
            p.cache_inset_y=int((real[i]>>5)&65535);++insets;
        }
    }
    // The x half of the same sum: the y pattern differs only in which fadd destination register
    // receives `field + cached` (d3 above, d2 here), so the two are told apart by that word.
    unsigned insets_x=0;
    for(size_t i=0;i+4<real.size();++i){
        if((real[i]&0xffe0001f)==0xd2800011&&real[i+1]==0xb8716820
            &&real[i+2]==0x8b1c8000&&real[i+3]==0xfc407001&&real[i+4]==0x1e612802){
            p.cache_inset_x=int((real[i]>>5)&65535);++insets_x;
        }
    }
    unsigned real_allocs=0;
    for(size_t i=0;i+7<real.size();++i)if((real[i]&0xfc000000u)==0x94000000u
        &&real[i+1]==0xfc5f03a0&&real[i+2]==0xfc007000
        &&real[i+3]==0xfc5f83a0&&real[i+4]==0xfc00f000
        &&real[i+5]==0xaa1d03ef&&real[i+6]==0xa8c179fd&&real[i+7]==0xd65f03c0){
        p.real_alloc_site=uint32_t(i*4);++real_allocs;}
    if(real_allocs!=1)return false;
    if(sources!=1||flags!=1||insets!=1||insets_x!=1)return false;
    // The read-only destination observer splices _calcRealIconPos' own prologue, where the
    // incoming receiver (x1) and item (x2) are still live. Derive it from the prologue shape
    // rather than trusting offset zero, and refuse anything but a single match.
    unsigned prologues=0;
    for(size_t i=0;i+2<real.size();++i){
        if(real[i]==0xa9bf79fd&&real[i+1]==0xaa0f03fd&&real[i+2]==0xd10041ef){p.probe_site=uint32_t(i*4);++prologues;}
    }
    if(prologues!=1||!preview_window(real,p.probe_site))return false;
    // _calcFolderPreviewLoc's destination: `index` is materialised in x2 by `add x2, x1, x0` right
    // before the calcPositionForCellX call, and the finished Offset is written back to the
    // controller with `stur w0, [x2, #0xd3]`. Both windows are four arithmetic/load words with no
    // branch, so the two splices stay inside this one function and cannot leak to its siblings.
    unsigned dests=0,stores=0;
    for(size_t i=0;i+3<preview.size();++i){
        // mov x1, x0 ; ldur x0, [x29, #local] ; add x2, x1, x0 ; ldur x1, [x29, #local2]
        if(preview[i]==0xaa0003e1&&(preview[i+1]&0xffe00fff)==0xf84003a0
            &&preview[i+2]==0x8b000022&&(preview[i+3]&0xffe00fff)==0xf84003a1){
            if(dests)return false;
            p.preview_index=preview_imm9(preview[i+1]);p.destination_controller=preview_imm9(preview[i+3]);
            p.destination_site=uint32_t(i*4);++dests;
        }
        // mov x1, x0 ; ldur x2, [x29, #local] ; stur w0, [x2, #0xd3] ; ldurb w16, [x2, #-1]
        if(preview[i]==0xaa0003e1&&(preview[i+1]&0xffe00fff)==0xf84003a2
            &&(preview[i+2]&0xffe00fff)==0xb8000040&&preview[i+3]==0x385ff050){
            if(stores)return false;
            p.destination_frame=preview_imm9(preview[i+1]);p.destination_field=preview_imm9(preview[i+2]);
            p.destination_store=uint32_t(i*4);++stores;
        }
    }
    // The normal and editing arms write the SAME decoded destination field.
    // Verify the original write barriers and bool dispatch, not just a lone store.
    if(stores==1){const size_t q=p.destination_store/4;
        if(q+24>=preview.size()||preview[q+3]!=0x385ff050||preview[q+4]!=0x385ff011
            ||preview[q+5]!=0x8a500a30||preview[q+6]!=0xea5c821f
            ||preview[q+7]!=0x54000040||(preview[q+8]&0xfc000000u)!=0x94000000u
            ||(preview[q+9]&0xffe0001fu)!=0xd2800011||int((preview[q+9]>>5)&65535)!=p.edit_flag
            ||preview[q+10]!=0xb8716840||preview[q+11]!=0x8b1c8000||preview[q+12]!=0x37200160
            ||(preview[q+13]&0xfc000000u)!=0x94000000u
            ||(preview[q+14]&0xffe00fffu)!=0xf84003a1||preview_imm9(preview[q+14])!=p.destination_frame
            ||(preview[q+15]&0xffe00fffu)!=0xb8000020||preview_imm9(preview[q+15])!=p.destination_field
            ||preview[q+16]!=0x385ff030||preview[q+17]!=0x385ff011
            ||preview[q+18]!=0x8a500a30||preview[q+19]!=0xea5c821f
            ||preview[q+20]!=0x54000040||(preview[q+21]&0xfc000000u)!=0x94000000u
            ||preview[q+22]!=0x14000002||preview[q+23]!=0xaa0203e1)return false;
    }
    // Fresh transform anchor Offset in _initGridViewItemAnimParams. The y store
    // follows the original allocation and x store. Replay retains the map insertion
    // and every Dart allocation/GC return PC; the fresh X and pending Y use the paint origin.
    unsigned anchors=0,anchor_frames=0;
    for(size_t i=0;i<12&&i<init.size();++i)
        if((init[i]&0xffe00fff)==0xf80003a1&&preview_imm9(init[i])<0){p.anchor_frame=preview_imm9(init[i]);++anchor_frames;}
    for(size_t i=6;i+3<init.size();++i){
        if((init[i-6]&0xfc000000)==0x94000000&&init[i-5]==0xaa0003e2
            &&init[i-3]==0xfc007040&&(init[i-2]&0xffe00fff)==0xf84003a0
            &&init[i-1]==0xfc407000&&init[i]==0xfc00f040
            &&(init[i+1]&0xffe00fff)==0xf84003a3&&init[i+2]==0x937f7860&&init[i+3]==0xeb80047f){
            p.anchor_site=uint32_t(i*4);p.anchor_alloc_site=uint32_t((i-6)*4);++anchors;
        }
    }
    if(anchors!=1||anchor_frames!=1||!preview_window(init,p.anchor_site))return false;
    if(dests!=1||stores!=1||!preview_window(preview,p.destination_site)
        ||!preview_window(preview,p.destination_store))return false;
    // All fields subsequently dereferenced by native callbacks are tagged-layout
    // offsets. Frame locals must remain live stack slots, never an object header
    // or positive caller argument. Roles on each object must not alias.
    const auto fields=[](std::initializer_list<int> values,unsigned alignment,unsigned bytes=4){
        for(auto it=values.begin();it!=values.end();++it){
            if(*it<=0||*it>65535||((*it+1)&(alignment-1)))return false;
            for(auto j=it+1;j!=values.end();++j)if(std::abs(*it-*j)<int(bytes))return false;
        }return true;
    };
    const auto locals=[](std::initializer_list<int> values){
        for(int v:values)if(v>=0||v< -512||(v&7))return false;
        return true;
    };
    if(!fields({p.delegate_count,p.delegate_main,p.delegate_cross},4,8)
        ||!fields({p.layout_count,p.layout_main_stride,p.layout_cross_stride,p.layout_height,p.layout_width},4,8)
        ||!fields({p.source_offset,p.edit_flag,p.cache_width,p.cache_height,p.cache_icon_width,
            p.cache_icon_height,p.cache_inset_x,p.cache_inset_y},4)
        ||!fields({p.grid_width,p.width_owner_gap},4,8)||!fields({p.config_cell},4)
        ||!fields({p.context_controller},4)||!fields({p.cell_padding},8)
        ||!fields({p.destination_field},4)
        ||!locals({p.context_local,p.item_local,p.preview_index,p.destination_controller,
            p.destination_frame,p.anchor_frame,p.scale_x_frame,p.scale_y_frame}))return false;
    if(builders!=1||contexts!=1||layouts!=1||closes!=1||indices!=1||!p.cache_width||!p.cache_height||p.grid_width<=0)return false;
    if(!preview_window(build,p.site[0])||!preview_window(layout,p.site[1])||!preview_window(close,p.site[2]))return false;
    out=p;return true;
}
} // namespace home_layout

