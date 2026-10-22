/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
#include <cstring>
#include <cmath>
namespace home_layout {
enum class WidgetScaleResult : uint8_t {
    Neutral, Applied, InvalidLayout, InvalidScale, InvalidInput, NurseryUnavailable, ConstantsUnavailable
};
inline const char* widget_scale_result_name(WidgetScaleResult value) {
    constexpr const char* names[]={"neutral","wrapped","invalid-layout","invalid-scale","invalid-input","nursery-unavailable","constants-unavailable"};
    return names[static_cast<unsigned>(value)];
}
struct WidgetScaleLayout {
    uint32_t allocator=0,tag=0,size=0,top=0,alignment_pool=0,translate_pool=0,bank=0;
    int32_t opacity=0,scale=0,rotation=0,translate=0,alignment=0,force=0,child=0,frame=0;
    bool usable()const {
        if(!tag||size<48||size>240||!top||!alignment_pool||!translate_pool||!home_dynamic::slot(frame))return false;
        const int32_t offsets[]={opacity,scale,rotation,translate,alignment,force,child};
        for(unsigned i=0;i<7;++i){
            const int32_t width=i<3?8:4;
            if(!home_dynamic::field(offsets[i])||uint32_t(offsets[i]+1+width)>size)return false;
            for(unsigned j=0;j<i;++j){const int32_t other=j<3?8:4;
                if(offsets[i]<offsets[j]+other&&offsets[j]<offsets[i]+width)return false;}
        }
        return true;
    }
};
inline bool stw(uint32_t w,unsigned base,unsigned reg){return home_dynamic::mem(w,0xb8000000u,base,reg);}
// Read the launcher's own default HyperTransform construction. Its scalar scale is
// applied by the existing renderer (including inverse hit testing), without resizing cells.
inline bool widget_scale_layout(const home_dynamic::Body& build,const home_dynamic::Body& transform,
    uint32_t va,WidgetScaleLayout& out){
    using namespace home_dynamic;WidgetScaleLayout next;unsigned ends=0,defaults=0,zeros=0;
    for(size_t i=0;i+4<build.size();++i)
        if(ldx(build[i],29,0)&&stw(build[i+1],0,1)&&build[i+2]==0xaa1d03efu
            &&build[i+3]==0xa8c179fdu&&build[i+4]==0xd65f03c0u){
            ++ends;next.bank=i*4;next.frame=imm9(build[i]);next.child=imm9(build[i+1]);}
    for(size_t i=0;i+10<transform.size();++i){
        if(transform[i]!=0x1e6e1000u||!std_(transform[i+1],0,0)
            ||!ux(transform[i+2],27,1)||!stw(transform[i+3],0,1))continue;
        ++zeros;next.opacity=imm9(transform[i+1]);next.translate_pool=uoff(transform[i+2]);
        next.translate=imm9(transform[i+3]);
    }
    for(size_t i=0;i+15<transform.size();++i){
        if(transform[i]!=0x1e6e1000u||!std_(transform[i+1],0,0)
            ||!stx(transform[i+2],0,31)||(transform[i+3]&0xff8003ffu)!=0x91000361u
            ||!ux(transform[i+4],1,1)||!stw(transform[i+5],0,1)
            ||transform[i+6]!=0x9100c2c1u||!stw(transform[i+7],0,1)
            ||!ldx(transform[i+8],29,1)||!stw(transform[i+9],0,1))continue;
        if(imm9(transform[i+9])!=next.child)return false;
        ++defaults;next.scale=imm9(transform[i+1]);next.rotation=imm9(transform[i+2]);
        next.alignment_pool=((transform[i+3]>>10)&4095)<<(((transform[i+3]>>22)&1)?12:0);
        next.alignment_pool+=uoff(transform[i+4]);next.alignment=imm9(transform[i+5]);
        next.force=imm9(transform[i+7]);
        // Same allocator constructs this default transform and the preceding opacity/scale one.
        unsigned allocs=0;
        for(size_t j=i;j>0&&i-j<12;--j){uint32_t to=0;
            if(target(transform[j-1],va+(j-1)*4,to)){next.allocator=to;++allocs;break;}}
        if(allocs!=1)return false;
    }
    if(ends!=1||defaults!=1||zeros!=1||!slot(next.frame)||!next.allocator)return false;
    out=next;return true;
}
// No safepoint, Dart call or movable pointer retention. On nursery exhaustion leave
// the original widget intact; never enter a Dart allocator from a native C frame.
inline uintptr_t wrap_scaled_widget(uintptr_t child,uintptr_t thread,uintptr_t pool,
    uintptr_t null_value,double factor,const WidgetScaleLayout& layout,WidgetScaleResult* status=nullptr){
    auto finish=[&](uintptr_t result,WidgetScaleResult why){if(status)*status=why;return result;};
    if(!layout.usable())return finish(child,WidgetScaleResult::InvalidLayout);
    if(!std::isfinite(factor)||factor<0.6||factor>1.4)return finish(child,WidgetScaleResult::InvalidScale);
    if(factor==1.0)return finish(child,WidgetScaleResult::Neutral);
    if(!(child&1)||!thread||!pool||!(null_value&1))return finish(child,WidgetScaleResult::InvalidInput);
    auto read=[](uintptr_t at,auto& value){std::memcpy(&value,reinterpret_cast<const void*>(at),sizeof(value));};
    auto write=[](uintptr_t at,const auto& value){std::memcpy(reinterpret_cast<void*>(at),&value,sizeof(value));};
    uintptr_t top=0,end=0,alignment=0,translate=0;
    read(thread+layout.top,top);read(thread+layout.top+8,end);
    // Dart's nursery can start at 8 mod 16 (new-space object alignment), unlike
    // the native stack. Reject only unaligned words, as the VM allocator does.
    if(!top||(top&7)||end<=top||end-top<=layout.size)return finish(child,WidgetScaleResult::NurseryUnavailable);
    read(pool+layout.alignment_pool,alignment);read(pool+layout.translate_pool,translate);
    if(!(alignment&1)||!(translate&1))return finish(child,WidgetScaleResult::ConstantsUnavailable);
    // PP constants are the original Alignment.center and Offset.zero, never clones.
    const uintptr_t object=top+1;
    write(top,uint64_t(layout.tag));
    for(uint32_t i=8;i<layout.size;i+=4)write(top+i,uint32_t(null_value));
    write(object+layout.child,uint32_t(child));
    write(object+layout.opacity,1.0);write(object+layout.scale,factor);write(object+layout.rotation,0.0);
    write(object+layout.translate,uint32_t(translate));write(object+layout.alignment,uint32_t(alignment));
    write(object+layout.force,uint32_t(null_value+0x30)); // Dart canonical false, as in the original builder.
    write(thread+layout.top,top+layout.size);
    return finish(object,WidgetScaleResult::Applied);
}
}
