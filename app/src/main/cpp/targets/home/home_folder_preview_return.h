/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_folder_feature_resolver.h"
#include "home_icon_feature_resolver.h"

namespace home_layout::features {
struct FolderPreviewReturnFeature {
    uint32_t origin=0,landing=0,caller_return=0,width_site=0,height_site=0;
    int32_t width_field=0,height_field=0,width_frame=0,height_frame=0;
    int32_t boxed_value_field=0,x_frame=0,y_frame=0,right_frame=0,bottom_frame=0;
};

namespace folder_preview_return_detail {
using home_dynamic::Body;
using namespace home_dynamic;
struct ItemLayout {int32_t list=0,width=0,height=0,x=0,y=0;};
struct RectLayout {uint32_t allocator=0;std::array<int32_t,4> fields{};};
inline bool double_field(int32_t f) {return field(f)&&!((f+1)&7);}
inline bool same_four(const ItemLayout& f) {
    const int32_t fields[]={f.width,f.height,f.x,f.y};
    for(unsigned i=0;i<4;++i){if(!double_field(fields[i]))return false;
        for(unsigned j=0;j<i;++j)if(std::abs(fields[i]-fields[j])<8)return false;}
    return true;
}
// The validated large strategy allocates each four-double preview item and
// appends that SAME fresh object to its item list. These are tagged-pointer
// byte offsets; the allocator's current tag bounds all four eight-byte fields.
inline std::optional<ItemLayout> item_layout(const Code& code,const FolderFeatures& folders) {
    Body b;if(!code.body(folders.preview_builder,b)||!frame_size(b))return {};
    std::optional<ItemLayout> out;
    for(size_t i=3;i+10<b.size();++i){uint32_t allocator=0;Body tag_body;uint32_t header=0;
        if(!target(b[i],folders.preview_builder+uint32_t(i*4),allocator)
            ||!ldw(b[i-3],0,1)||b[i-2]!=0x8b1c8021u||!stx(b[i-1],29,1)
            ||!frame_slot(b,imm9(b[i-1]))||!code.prefix(allocator,3,tag_body)
            ||!tag(tag_body,header)||(tag_body[2]&0xfc000000u)!=0x14000000u)continue;
        const size_t loads[]={i+1,i+4,i+6,i+8},stores[]={i+3,i+5,i+7,i+9};
        ItemLayout next;next.list=imm9(b[i-3]);int32_t* fields[]={&next.width,&next.height,&next.x,&next.y};
        bool valid=stx(b[i+2],29,0)&&frame_slot(b,imm9(b[i+2]));
        for(unsigned n=0;n<4;++n){valid&=ldd(b[loads[n]],29,0)&&frame_slot(b,imm9(b[loads[n]]))
            &&std_(b[stores[n]],0,0);*fields[n]=imm9(b[stores[n]]);}
        valid&=ldx(b[i+10],29,2)&&imm9(b[i+10])==imm9(b[i-1]);
        const uint32_t bytes=((header>>8)&15)*16;
        if(!valid||!field(next.list)||!same_four(next))continue;
        for(int32_t field:{next.width,next.height,next.x,next.y})valid&=uint32_t(field+9)<=bytes;
        // The list append reloads this allocation's rooted result after its
        // original grow branch, then stores the compressed reference in data.
        unsigned append=0;
        for(size_t j=i+11;j+3<b.size()&&j<i+48;++j)
            if(ldx(b[j],29,0)&&imm9(b[j])==imm9(b[i+2])&&b[j+1]==0x8b030839u
                &&b[j+2]==0x91003f39u&&b[j+3]==0xb9000320u)++append;
        if(!valid||append!=1)continue;
        if(out)return {};out=next;
    }
    return out;
}
// A typed controller accessor forwards the selected index into the strategy's
// actual list getter. A same-shaped unrelated four-double class is insufficient.
inline bool item_accessor(const Code& code,uint32_t va,int32_t strategy,const ItemLayout& item) {
    Body a;if(!code.body(va,a)||!folder_detail::frame(a))return false;
    unsigned access=0;
    for(size_t i=2;i+8<a.size();++i){uint32_t getter=0;Body b;
        if(!movz(a[i],17)||int32_t(zimm(a[i]))!=strategy||a[i+1]!=0xb8716820u
            ||a[i+2]!=0x8b1c8000u||a[i+6]!=0xaa0003e1u
            ||!target(a[i+7],va+uint32_t((i+7)*4),getter)
            ||a[i+8]!=0xaa1d03efu||!code.body(getter,b)||!frame_size(b))continue;
        unsigned list=0,index=0;
        for(size_t j=0;j+2<b.size();++j){
            if(ldw(b[j],0,2)&&imm9(b[j])==item.list&&b[j+1]==0x8b1c8042u
                &&ldw(b[j+2],2,3))++list;
            if(b[j]==0x8b050830u&&b[j+1]==0xb840f200u&&b[j+2]==0x8b1c8000u)++index;
        }
        if(list==1&&index==1)++access;
    }
    return access==1;
}
inline bool origin_body(const Body& b,uint32_t va,uint32_t current,const ItemLayout& item) {
    int32_t info=0;if(!receiver_slot(b,2,info)||!stx(b[3],29,1)
        ||!frame_slot(b,imm9(b[3])))return false;
    unsigned config=0,x=0,y=0;bool rooted=false;
    for(size_t i=0;i<b.size();++i){uint32_t to=0;
        if(target(b[i],va+uint32_t(i*4),to)&&to==current)++config;
        if(ldx(b[i],29,0))rooted=imm9(b[i])==info;
        if((b[i]&0xfc000000u)==0x94000000u)rooted=false;
        if(rooted&&(b[i]&0xffe00c00u)==0xfc400000u&&rn(b[i])==0){
            x+=imm9(b[i])==item.x;y+=imm9(b[i])==item.y;}
    }
    return config>=3&&x==2&&y==1;
}
inline bool no_interior_branch(const Code& code,const FolderPreviewReturnFeature& plan) {
    const uint32_t starts[]={plan.origin,plan.width_site,plan.height_site};
    for(const auto& function:code.functions())for(size_t i=0;i<function.words.size();++i){
        const auto w=function.words[i];int64_t displacement=0;bool branch=false;
        if((w&0x7c000000u)==0x14000000u){displacement=(int32_t(w<<6)>>6)*int64_t{4};branch=true;}
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)
            {displacement=(int32_t(w<<8)>>13)*int64_t{4};branch=true;}
        else if((w&0x7e000000u)==0x36000000u)
            {displacement=(int32_t(w<<13)>>18)*int64_t{4};branch=true;}
        if(!branch)continue;
        const int64_t destination=int64_t(function.va)+int64_t(i*4)+displacement;
        for(uint32_t start:starts)if(destination>start&&destination<int64_t(start)+16)return false;
    }
    return true;
}
inline bool wrapper_origin(const Code& code,uint32_t va,uint32_t origin) {
    Body b;int32_t info=0;if(!code.body(va,b)||!receiver_slot(b,2,info))return false;
    unsigned calls=0;
    for(size_t i=1;i<b.size();++i){uint32_t to=0;
        if(target(b[i],va+uint32_t(i*4),to)&&to==origin){
            if(!ldx(b[i-1],29,2)||imm9(b[i-1])!=info)return false;++calls;
        }
    }
    return calls==3;
}
// The desktop's real Rect consumer follows the same strategy list, passes that
// item to the shared coordinate producer, and adds the item's two lengths to
// the returned origin. This proves width/height roles independently of layout.
inline bool static_consumer(const Code& code,uint32_t origin,int32_t strategy,const ItemLayout& item,RectLayout& rect) {
    unsigned consumers=0;
    for(const auto& function:code.functions()){
        const auto& b=function.words;if(!frame_size(b))continue;
        unsigned list=0,geometry=0;
        for(size_t i=0;i+7<b.size();++i)
            if(movz(b[i],17)&&int32_t(zimm(b[i]))==strategy&&b[i+1]==0xb8716840u
                &&b[i+2]==0x8b1c8000u&&ldw(b[i+6],0,4)&&imm9(b[i+6])==item.list
                &&b[i+7]==0x8b1c8084u)++list;
        if(list!=1)continue;
        for(size_t i=4;i+8<b.size();++i){uint32_t wrapper=0;
            if(!target(b[i],function.va+uint32_t(i*4),wrapper)||b[i+1]!=0xaa0003e2u
                ||!ldx(b[i+2],29,0)||!stx(b[i+3],29,2)
                ||!ldd(b[i+4],0,0)||imm9(b[i+4])!=item.width
                ||!std_(b[i+5],29,0)||!ldd(b[i+6],0,1)||imm9(b[i+6])!=item.height
                ||!ldx(b[i+7],29,0)||!std_(b[i+8],29,1)
                ||!wrapper_origin(code,wrapper,origin))continue;
            const unsigned source=(b[i-3]>>16)&31;
            if((b[i-3]&0xffe0ffffu)!=0xaa0003e2u||!stx(b[i-2],29,source)
                ||imm9(b[i-2])!=imm9(b[i+2]))continue;
            const int32_t width=imm9(b[i+5]),height=imm9(b[i+8]);
            unsigned additions=0;
            for(size_t j=i+9;j+21<b.size();++j)
                if(ldd(b[j],29,1)&&imm9(b[j])==width&&ldd(b[j+1],29,0)
                    &&imm9(b[j+1])==height&&ldx(b[j+2],29,1)&&ldx(b[j+3],29,0)
                    &&ldd(b[j+4],0,2)&&std_(b[j+5],29,2)&&ldd(b[j+6],0,3)&&std_(b[j+7],29,3)
                    &&b[j+8]==0x1e612844u&&std_(b[j+9],29,4)
                    &&b[j+10]==0x1e602861u&&std_(b[j+11],29,1)){
                    uint32_t allocator=0,header=0;Body tag_body;
                    if(!target(b[j+12],function.va+uint32_t((j+12)*4),allocator)
                        ||!code.prefix(allocator,3,tag_body)||!tag(tag_body,header)
                        ||(tag_body[2]&0xfc000000u)!=0x14000000u||!stx(b[j+14],29,0))continue;
                    const size_t loads[]={j+13,j+16,j+18,j+20},stores[]={j+15,j+17,j+19,j+21};
                    const int32_t slots[]={imm9(b[j+5]),imm9(b[j+7]),imm9(b[j+9]),imm9(b[j+11])};
                    bool valid=true;std::set<int32_t> fields;
                    for(unsigned n=0;n<4;++n){
                        valid&=ldd(b[loads[n]],29,0)&&imm9(b[loads[n]])==slots[n]&&frame_slot(b,slots[n])
                            &&std_(b[stores[n]],0,0)&&double_field(imm9(b[stores[n]]))
                            &&uint32_t(imm9(b[stores[n]])+9)<=((header>>8)&15)*16;
                        fields.insert(imm9(b[stores[n]]));
                    }
                    if(valid&&fields.size()==4){
                        rect.allocator=allocator;
                        for(unsigned n=0;n<4;++n)rect.fields[n]=imm9(b[stores[n]]);
                        ++additions;
                    }
                }
            if(additions==1)++geometry;
        }
        if(geometry==1)++consumers;
    }
    return consumers==1;
}
}

inline std::optional<FolderPreviewReturnFeature> folder_preview_return_feature(
    const Code& code,const FolderFeatures& folders,const IconFeature& icon) {
    using namespace home_dynamic;using namespace folder_preview_return_detail;
    if(!icon.current||folders.current!=icon.current)return {};
    const auto item=item_layout(code,folders);if(!item)return {};
    std::optional<FolderPreviewReturnFeature> out;
    for(const auto& function:code.functions()){
        const auto& b=function.words;if(!frame_size(b))continue;
        // Adjacent noncalling four-word banks create the right/bottom endpoint.
        for(size_t w=0;w+8<b.size();++w){
            if(!ldd(b[w],0,0)||!ldd(b[w+1],29,1)||b[w+2]!=0x1e602822u||!std_(b[w+3],29,2)
                ||!ldd(b[w+4],1,0)||imm9(b[w+4])!=imm9(b[w])||!ldd(b[w+5],29,3)
                ||b[w+6]!=0x1e602864u||!std_(b[w+7],29,4))continue;
            FolderPreviewReturnFeature next;next.landing=function.va;next.width_site=function.va+uint32_t(w*4);
            next.height_site=next.width_site+16;next.width_field=item->width;next.height_field=item->height;
            next.boxed_value_field=imm9(b[w]);next.x_frame=imm9(b[w+1]);next.y_frame=imm9(b[w+5]);
            next.right_frame=next.width_frame=imm9(b[w+3]);next.bottom_frame=next.height_frame=imm9(b[w+7]);
            bool valid=double_field(next.boxed_value_field);
            const int32_t slots[]={next.x_frame,next.y_frame,next.right_frame,next.bottom_frame};
            for(unsigned n=0;n<4;++n){valid&=frame_slot(b,slots[n]);
                for(unsigned m=0;m<n;++m)valid&=slots[n]!=slots[m];}
            if(!valid)continue;
            unsigned endings=0;RectLayout landing_rect;
            for(size_t j=w+8;j+8<b.size();++j){uint32_t alloc=0;
                if(target(b[j],function.va+uint32_t(j*4),alloc)&&ldd(b[j+1],29,0)&&imm9(b[j+1])==next.x_frame
                    &&std_(b[j+2],0,0)&&ldd(b[j+3],29,0)&&imm9(b[j+3])==next.y_frame
                    &&std_(b[j+4],0,0)&&ldd(b[j+5],29,0)&&imm9(b[j+5])==next.right_frame
                    &&std_(b[j+6],0,0)&&ldd(b[j+7],29,0)&&imm9(b[j+7])==next.bottom_frame
                    &&std_(b[j+8],0,0)) {
                    landing_rect.allocator=alloc;
                    for(unsigned n=0;n<4;++n)landing_rect.fields[n]=imm9(b[j+2+n*2]);
                    ++endings;
                }
            }
            if(endings!=1)continue;
            unsigned sources=0;
            for(size_t i=0;i+9<w;++i){uint32_t accessor=0,origin=0;Body origin_code;
                if(!target(b[i],function.va+uint32_t(i*4),accessor)||!ldx(b[i+1],29,1)
                    ||b[i+2]!=0xaa0003e2u||!target(b[i+3],function.va+uint32_t((i+3)*4),origin)
                    ||!ldd(b[i+4],0,0)||imm9(b[i+4])!=next.boxed_value_field
                    ||!std_(b[i+5],29,0)||imm9(b[i+5])!=next.x_frame
                    ||!ldd(b[i+6],0,1)||imm9(b[i+6])!=next.boxed_value_field+8
                    ||!ldx(b[i+7],29,0)||imm9(b[i+7])!=imm9(b[i+1])
                    ||!std_(b[i+8],29,1)||imm9(b[i+8])!=next.y_frame||!movz(b[i+9],17))continue;
                // Read the strategy operand from the accessor, then require
                // its list getter and the independent static Rect consumer.
                Body a;if(!code.body(accessor,a))continue;int32_t strategy=0;
                for(size_t k=0;k+2<a.size();++k)if(movz(a[k],17)&&a[k+1]==0xb8716820u
                    &&a[k+2]==0x8b1c8000u){if(strategy){strategy=0;break;}strategy=int32_t(zimm(a[k]));}
                RectLayout painted_rect;
                if(!field(strategy)||!item_accessor(code,accessor,strategy,*item)
                    ||!code.body(origin,origin_code)||!origin_body(origin_code,origin,icon.current,*item)
                    ||!static_consumer(code,origin,strategy,*item,painted_rect)
                    ||painted_rect.allocator!=landing_rect.allocator||painted_rect.fields!=landing_rect.fields)continue;
                next.origin=origin;next.caller_return=function.va+uint32_t((i+4)*4);++sources;
            }
            const std::array<size_t,1> point{w};
            if(sources!=1||!passes_return_points(b,point)||!no_interior_branch(code,next))continue;
            if(out)return {};out=next;
        }
    }
    return out;
}
}
