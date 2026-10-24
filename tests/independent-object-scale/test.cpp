/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_object_scale.h"
#include "targets/home/home_icon_render.h"
#include "targets/home/home_widget_scale.h"
#include "fixture.h"
#include <cassert>
#include <cstring>
#include <cmath>
#include <iostream>
using namespace home_layout;
template<class T>void put(uintptr_t p,T v){std::memcpy(reinterpret_cast<void*>(p),&v,sizeof(v));}
template<class T>T get(uintptr_t p){T v;std::memcpy(&v,reinterpret_cast<void*>(p),sizeof(v));return v;}
int main(){
    ObjectScales config{};
    auto parse=[&](std::vector<int32_t> words,bool optional=true){size_t i=0;
        return read_object_scales([&](int32_t& w){if(i==words.size())return false;w=words[i++];return true;},config,optional);};
    assert(parse({}));for(auto s:config)assert(!s.enabled&&s.percent==100&&s.factor()==1);
    assert(parse({kObjectScaleMagic,1,60,0,140,1,125}));
    assert(config[0].factor()==.6&&config[1].factor()==1&&config[2].factor()==1.25);
    assert(!parse({},false));assert(!parse({0}));assert(!parse({kObjectScaleMagic,1,100}));
    assert(!parse({kObjectScaleMagic,2,100,0,100,0,100}));
    assert(!parse({kObjectScaleMagic,0,59,0,100,0,100}));
    assert(!parse({kObjectScaleMagic,0,100,0,100,0,141}));
    auto animation=ANIMATION;std::array<uint32_t,3> sites{};
    assert(folder_animation_scale_sites(animation,ANIMATION_VA,CURRENT,sites));
    assert((sites==std::array<uint32_t,3>{0xc0,0x160,0x2b0}));
    assert(!folder_animation_scale_sites(animation,ANIMATION_VA,CURRENT+4,sites));
    for(size_t i=0;i<animation.size();++i)if(home_dynamic::ldd(animation[i],1,0)){
        auto saved=animation[i];animation[i]=0xd503201f;
        assert(!folder_animation_scale_sites(animation,ANIMATION_VA,CURRENT,sites));animation[i]=saved;
    }
    auto cache=ANIMATION_CACHE;std::array<uint32_t,2> cache_sites{};
    const int32_t grid=home_dynamic::imm9(animation[sites[0]/4-3]);
    const int32_t size=home_dynamic::imm9(animation[sites[0]/4-1]);
    assert(folder_animation_cache_sites(cache,ANIMATION_CACHE_VA,CURRENT,grid,size,cache_sites));
    assert(!folder_animation_cache_sites(cache,ANIMATION_CACHE_VA,CURRENT+4,grid,size,cache_sites));
    assert(!folder_animation_cache_sites(cache,ANIMATION_CACHE_VA,CURRENT,grid+4,size,cache_sites));
    assert(!folder_animation_cache_sites(cache,ANIMATION_CACHE_VA,CURRENT,grid,size+8,cache_sites));
    for(size_t i=0;i<cache.size();++i)
        if((home_dynamic::ldd(cache[i],1,0)&&home_dynamic::imm9(cache[i])==size)
            ||cache[i]==0x1e623823u){
            auto saved=cache[i];cache[i]=0xd503201f;
            assert(!folder_animation_cache_sites(cache,ANIMATION_CACHE_VA,CURRENT,grid,size,cache_sites));cache[i]=saved;
        }
    // Model the actual close equation: scaled source / scaled preview, reversed for close.
    for(double icon:{.6875,1.,1.375})for(double folder:{.6,1.,1.4}){
        const double source=64*icon,preview=22*folder;
        const double open_ratio=source/preview;
        assert(std::abs(source/open_ratio-preview)<1e-10);
        // The selected-icon Rect correction uses this same scaled source dimension.
        const double width=53.25;assert(std::abs(width*(source/width)/open_ratio-preview)<1e-10);
        // The center stays fixed across the animated/static handoff, in both axes.
        // The previous unscaled inset produced a diagonal snap of 64*(1-icon)/2.
        for(double cell:{53.25,92.5})for(double title:{0.,18.}){
            const double old_inset=(cell-64-title)/2;
            const double inset=(cell-source-title)/2;
            assert(std::abs(inset+source/2-(cell-title)/2)<1e-10);
            if(icon!=1)assert(std::abs(old_inset+source/2-(cell-title)/2)>1);
        }
    }
    auto build=WIDGET,transform=TRANSFORM;
    WidgetScaleLayout layout;
    assert(widget_scale_layout(build,transform,TRANSFORM_VA,layout));
    assert(layout.bank==0x50&&layout.frame==-8&&layout.child==11);
    assert(layout.opacity==15&&layout.scale==27&&layout.rotation==43&&layout.translate==23
        &&layout.alignment==51&&layout.force==63&&layout.alignment_pool==0x8180&&layout.translate_pool==0x1de0);
    layout.tag=0x1dda51c;layout.size=80;layout.top=96;
    assert(layout.usable());
    // Relocation and changed field/frame/pool operands must be decoded, never
    // replaced with this fixture's addresses or offsets.
    auto shifted_build=build,shifted_transform=transform;
    auto shift_field=[](uint32_t w,int delta){
        return (w&~0x001ff000u)|(uint32_t(home_dynamic::imm9(w)+delta)&511u)<<12;
    };
    for(auto& w:shifted_build){
        if(home_dynamic::ldx(w,29,0))w=shift_field(w,-16);
        else if(stw(w,0,1))w=shift_field(w,16);
    }
    for(auto& w:shifted_transform){
        if(home_dynamic::std_(w,0,0)||home_dynamic::stx(w,0,31)||stw(w,0,1))w=shift_field(w,16);
        if(home_dynamic::ux(w,27,1)||home_dynamic::ux(w,1,1))w+=1u<<10;
    }
    WidgetScaleLayout shifted;
    assert(widget_scale_layout(shifted_build,shifted_transform,TRANSFORM_VA+0x200000,shifted));
    assert(shifted.allocator==layout.allocator+0x200000&&shifted.frame==layout.frame-16);
    assert(shifted.child==layout.child+16&&shifted.scale==layout.scale+16
        &&shifted.opacity==layout.opacity+16&&shifted.rotation==layout.rotation+16
        &&shifted.translate==layout.translate+16&&shifted.alignment==layout.alignment+16
        &&shifted.force==layout.force+16&&shifted.alignment_pool==layout.alignment_pool+8
        &&shifted.translate_pool==layout.translate_pool+8);
    auto old=transform;
    for(auto& w:transform)if(home_dynamic::std_(w,0,0)&&home_dynamic::imm9(w)==layout.scale)w=0xd503201f;
    WidgetScaleLayout ignored;assert(!widget_scale_layout(build,transform,TRANSFORM_VA,ignored));transform=old;
    // Runtime allocation never changes child geometry, only wraps the completed widget.
    alignas(16) unsigned char threadmem[256]{},heap[512]{},poolmem[0x8200]{};
    const uintptr_t thread=uintptr_t(threadmem),pool=uintptr_t(poolmem),start=uintptr_t(heap),child=0x12340001,nullptr_dart=0xdead0001;
    put(pool+layout.alignment_pool,uintptr_t(0x12341001));put(pool+layout.translate_pool,uintptr_t(0x12342001));
    // The real Dart nursery is 8 mod 16. Testing only a native alignas(16)
    // buffer previously hid a guard that silently skipped every real widget.
    for(unsigned offset:{0u,8u})for(double factor:{.6,.7,1.,1.25,1.4}){
        const uintptr_t top=start+offset;
        std::memset(heap,0x5a,sizeof(heap));put(thread+96,top);put(thread+104,start+sizeof(heap));
        WidgetScaleResult why;
        auto object=wrap_scaled_widget(child,thread,pool,nullptr_dart,factor,layout,&why);
        if(factor==1){assert(why==WidgetScaleResult::Neutral&&object==child&&get<uintptr_t>(thread+96)==top);continue;}
        assert(why==WidgetScaleResult::Applied&&object==top+1&&get<uintptr_t>(thread+96)==top+80);
        assert(get<uint64_t>(top)==layout.tag&&get<uint32_t>(object+11)==child);
        assert(get<double>(object+15)==1&&get<double>(object+27)==factor&&get<double>(object+43)==0);
        assert(get<uint32_t>(object+23)==0x12342001&&get<uint32_t>(object+51)==0x12341001);
        assert(get<uint32_t>(object+7)==nullptr_dart&&get<uint32_t>(object+55)==nullptr_dart
            &&get<uint32_t>(object+59)==nullptr_dart&&get<uint32_t>(object+67)==nullptr_dart);
        assert(get<uint32_t>(object+63)==uint32_t(nullptr_dart+0x30));
        for(unsigned i=0;i<offset;++i)assert(heap[i]==0x5a);
        for(unsigned i=offset+80;i<sizeof(heap);++i)assert(heap[i]==0x5a);
    }
    for(unsigned offset:{0u,8u,4u}){
        const auto top=start+offset;
        for(unsigned room:{0u,79u,80u}){
            put(thread+96,top);put(thread+104,top+room);
            WidgetScaleResult why;
            assert(wrap_scaled_widget(child,thread,pool,nullptr_dart,.8,layout,&why)==child
                &&why==WidgetScaleResult::NurseryUnavailable&&get<uintptr_t>(thread+96)==top);
        }
    }
    put(thread+96,start+4);put(thread+104,start+512);
    assert(wrap_scaled_widget(child,thread,pool,nullptr_dart,.8,layout)==child);
    std::cout<<"independent scales, old endpoint/reset/range guards, close endpoints, widget resolver/allocation: PASS\n";
}
