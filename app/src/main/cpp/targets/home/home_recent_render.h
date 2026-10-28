/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_feature_resolver.h"
#include <cmath>
namespace home_layout::recent_render {
using namespace features::recent;
struct alignas(16) Registers {
    uint64_t x[32]; // x0..x30; x31 stores NZCV, not SP.
    uint64_t q[32][2];
    uint64_t fpcr,fpsr,branch,reserved;
    double d(unsigned i)const {double v;std::memcpy(&v,&q[i][0],8);return v;}
    void d(unsigned i,double v,bool clear=false){std::memcpy(&q[i][0],&v,8);if(clear)q[i][1]=0;}
};
static_assert(sizeof(Registers)==800&&offsetof(Registers,q)==256&&offsetof(Registers,branch)==784);
template<class T>T load(uintptr_t address){T value;std::memcpy(&value,reinterpret_cast<const void*>(address),sizeof(T));return value;}
template<class T>void store(uintptr_t address,T value){std::memcpy(reinterpret_cast<void*>(address),&value,sizeof(T));}
enum class Result {Neutral,Applied,UnknownCaller,WrongClass,InvalidHeap,NoSpace};
/*
 * Last visibility write, for device triage only. A text site can report Applied while the
 * header still shows, so the caller needs the object it actually touched and the value the
 * data field held before the store. Filled on this thread, read by the hook wrapper.
 */
struct VisibilityTrace {
    uintptr_t object=0;
    uint32_t before=0,after=0,empty=0;
    bool text=false;
};
inline thread_local VisibilityTrace g_visibility_trace{};
inline const char* reason(Result value){switch(value){
    case Result::WrongClass:return "style class differs from the verified allocator";
    case Result::InvalidHeap:return "nursery metadata or word alignment invalid";
    case Result::NoSpace:return "insufficient nursery space; no Dart GC slow path";
    case Result::UnknownCaller:return "caller outside the verified card-scale family";
    default:return "neutral";
}}
inline uintptr_t reserve(uintptr_t thread,uint32_t top_field,uint32_t end_field,size_t size,Result& why) {
    if(!thread){why=Result::InvalidHeap;return 0;}
    const auto top=load<uintptr_t>(thread+top_field),end=load<uintptr_t>(thread+end_field);
    // Verified Dart nursery ABI: top may be either 0 or 8 modulo 16.
    if(!top||(top&7)||end<=top){why=Result::InvalidHeap;return 0;}
    if(size>=end-top){why=Result::NoSpace;return 0;}
    return top;
}
inline uintptr_t color_at(uintptr_t top,uintptr_t pool,int32_t argb,const Plan& p) {
    std::memset(reinterpret_cast<void*>(top),0,p.color.size);
    store<uint64_t>(top,p.color.tag);
    const auto value=uint32_t(argb);
    for(unsigned i=0;i<4;++i)store<double>(top+1+p.color.channels[i],double((value>>(24-i*8))&255)/255.0);
    store<uint32_t>(top+1+p.color_space,uint32_t(load<uint64_t>(pool+p.color_space_pool)));
    return top+1;
}
inline uintptr_t make_color(uintptr_t thread,uintptr_t pool,int32_t argb,const Plan& p,Result& why) {
    auto top=reserve(thread,p.color.top,p.color.end,p.color.size,why);if(!top)return 0;
    const auto result=color_at(top,pool,argb,p);
    store<uintptr_t>(thread+p.color.top,top+p.color.size);why=Result::Applied;return result;
}
inline uintptr_t style(uintptr_t source,uintptr_t thread,uintptr_t pool,const RecentConfig& config,const Plan& p,Result& why) {
    if(!(source&1)||((load<uint64_t>(source-1)>>12)&0xfffff)!=(p.style_tag>>12)){
        why=Result::WrongClass;return source;}
    const bool font=config.values[RecentConfig::TextSize]!=-1,color=config.values[RecentConfig::TextColor]!=-1;
    const size_t size=p.style_size+(font?16:0)+(color?p.color.size:0);
    const auto top=reserve(thread,p.style_top,p.style_end,size,why);if(!top)return source;
    auto next=top+p.style_size;
    // No persistent movable references, Dart calls, or slow allocation. Publish
    // top only after the fresh style and all younger children are initialized.
    std::memcpy(reinterpret_cast<void*>(top),reinterpret_cast<const void*>(source-1),p.style_size);
    store<uint64_t>(top,p.style_tag);
    if(font){store<uint64_t>(next,p.double_tag);store<double>(next+1+p.double_field,config.values[RecentConfig::TextSize]);
        store<uint32_t>(top+1+p.style_font,uint32_t(next+1));next+=16;}
    if(color){auto child=color_at(next,pool,config.values[RecentConfig::TextColor],p);
        store<uint32_t>(top+1+p.style_color,uint32_t(child));next+=p.color.size;}
    store<uintptr_t>(thread+p.style_top,next);why=Result::Applied;return top+1;
}
inline void return_frame(Registers& r){const auto frame=r.x[29];r.x[15]=frame+16;r.x[29]=load<uint64_t>(frame);r.x[30]=load<uint64_t>(frame+8);r.branch=0;}
inline Result apply(const Plan& p,const Site& site,Registers& r,const RecentConfig& c,uintptr_t base) {
    Result result=Result::Neutral;
    const auto setting=site.setting;
    const auto selected=c.values[setting];
    const bool terminal=site.action==Action::ScaleReturn||site.action==Action::PoolReturn;
    if(site.action==Action::IconChild){
        if(!selected||!p.hide_icon_ready)return result;
        const auto empty=load<uintptr_t>(r.x[27]+p.empty_widget_pool);
        if(!(empty&1)||empty==r.x[22])return Result::WrongClass;
        g_visibility_trace={empty,uint32_t(site.reg),0,uint32_t(empty),false};
        if(site.compressed_icon){
            if(uintptr_t(uint32_t(empty))+(r.x[28]<<32)!=empty)return Result::WrongClass;
            r.x[site.reg]=uint32_t(empty);
        }else r.x[site.reg]=empty;
        return Result::Applied;
    }
    if(site.action==Action::HideIconReturn){
        if(!selected||!p.hide_icon_ready)return result;
        const auto empty=load<uintptr_t>(r.x[27]+p.empty_widget_pool);
        if(!(empty&1)||empty==r.x[22])return Result::WrongClass;
        r.x[0]=empty;if(site.frame)return_frame(r);else r.branch=0;return Result::Applied;
    }
    if(site.action==Action::Style||site.action==Action::FrameStyle){
        if(c.values[RecentConfig::HideText]&&p.hide_text_ready&&site.text_object<31){
            const auto empty=load<uintptr_t>(r.x[27]+p.empty_string_pool);
            const auto cid=(empty&1)?(load<uint64_t>(empty-1)>>12)&0xfffff:0;
            if((cid==p.string_one_cid||cid==p.string_two_cid)
                &&load<uint32_t>(empty+p.string_length)==0){
                // Original constructor is still building this fresh Text. The
                // canonical old-space String needs no generational barrier.
                const auto text=r.x[site.text_object];
                if(!(text&1)||((load<uint64_t>(text-1)>>12)&0xfffff)!=(p.text_tag>>12))return Result::WrongClass;
                const uint32_t before=load<uint32_t>(text+p.text_data);
                store<uint32_t>(text+p.text_data,uint32_t(empty));
                g_visibility_trace={text,before,uint32_t(empty),uint32_t(empty),true};
                result=Result::Applied;
            }else return Result::WrongClass;
        }
        if(c.values[RecentConfig::TextSize]==-1&&c.values[RecentConfig::TextColor]==-1)return result;
        const auto old=site.action==Action::Style?r.x[site.reg]:load<uint64_t>(r.x[29]+site.frame);
        const auto value=style(old,r.x[26],r.x[27],c,p,result);
        if(value!=old){if(site.action==Action::Style)r.x[site.reg]=value;else store<uint64_t>(r.x[29]+site.frame,value);}
        return result;
    }
    if(site.action==Action::FreeformColor){
        if(selected==-1||r.x[0]!=r.x[22]+0x20)return result;
        const auto value=make_color(r.x[26],r.x[27],selected,p,result);
        if(value)r.x[site.reg]=value;return result;
    }
    if(site.action==Action::Freeform){
        // Preserve the getter's original explicit/non-null transparent arm.
        // Only replace its nullable theme/background branch.
        if(selected==-1||r.x[1]!=r.x[22])return result;
        const auto value=make_color(r.x[26],r.x[27],selected,p,result);
        if(value){r.x[0]=value;r.branch=0;}return result;
    }
    if(site.action==Action::ScaleReturn){
        // The shared dynamic scale calculator serves several callers. Only
        // return PCs proven in this exact image select one of our three modes.
        const auto caller=load<uint64_t>(r.x[29]+8);int index=-1;
        for(unsigned i=0;i<3;++i)if(base<=UINTPTR_MAX-p.fraction_returns[i]&&base+p.fraction_returns[i]==caller)index=int(i);
        if(index<0)return Result::UnknownCaller;
        const auto kind=std::array<int,3>{RecentConfig::Vertical,RecentConfig::HorizontalPortrait,RecentConfig::HorizontalLandscape}[index];
        if(c.values[kind]==100)return result;
        const double numerator=r.d(3),denominator=r.d(4);
        if(!std::isfinite(numerator)||!std::isfinite(denominator)||denominator<=0)return result;
        r.d(0,(numerator/denominator)*(double(c.values[kind])/100),true);return_frame(r);return Result::Applied;
    }
    if(selected==RecentConfig{}.values[setting])return result;
    if(site.action==Action::CornerFrame){
        // Stack mode already applies the configured scalar before its original
        // card-layout scale. Only replace the plain mode's constant target.
        if(load<uintptr_t>(r.x[29]+site.selector_frame)==load<uintptr_t>(r.x[27]+site.pool))return result;
        store<double>(r.x[29]+site.frame,selected);return Result::Applied;
    }
    if(site.action==Action::ScalarReturn){r.d(0,selected,true);r.branch=0;return Result::Applied;}
    double value=selected;
    if(setting==RecentConfig::TaskHeight)value/=100;
    if(site.action==Action::PoolReturn){
        value=load<double>(r.x[27]+site.pool)*selected/100.0;
        r.x[17]=r.x[27]+site.frame;r.d(0,value,true);return_frame(r);return Result::Applied;
    }
    if(terminal)return result;
    if(site.action==Action::FrameScalar||site.action==Action::CornerSurface)store<double>(r.x[29]+site.frame,value);
    else r.d(site.reg,value);
    return Result::Applied;
}
}
