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
    // Model the actual close equation: scaled source / scaled preview, reversed for close.
    for(double icon:{.6875,1.,1.375})for(double folder:{.6,1.,1.4}){
        const double source=64*icon,preview=22*folder;
        const double open_ratio=source/preview;
        assert(std::abs(source/open_ratio-preview)<1e-10);
        // The selected-icon Rect correction uses this same scaled source dimension.
        const double width=53.25;assert(std::abs(width*(source/width)/open_ratio-preview)<1e-10);
    }
    auto build=WIDGET,transform=TRANSFORM;
    WidgetScaleLayout layout;
    assert(widget_scale_layout(build,transform,TRANSFORM_VA,layout));
    assert(layout.bank==0x50&&layout.frame==-8&&layout.child==11);
    assert(layout.opacity==15&&layout.scale==27&&layout.rotation==43&&layout.translate==23
        &&layout.alignment==51&&layout.force==63&&layout.alignment_pool==0x8180&&layout.translate_pool==0x1de0);
    layout.tag=0x1dda51c;layout.size=80;layout.top=96;
    assert(layout.usable());
    auto old=transform;
    for(auto& w:transform)if(home_dynamic::std_(w,0,0)&&home_dynamic::imm9(w)==layout.scale)w=0xd503201f;
    WidgetScaleLayout ignored;assert(!widget_scale_layout(build,transform,TRANSFORM_VA,ignored));transform=old;
    // Runtime allocation never changes child geometry, only wraps the completed widget.
    alignas(16) unsigned char threadmem[256]{},heap[512]{},poolmem[0x8200]{};
    const uintptr_t thread=uintptr_t(threadmem),pool=uintptr_t(poolmem),start=uintptr_t(heap),child=0x12340001,nullptr_dart=0xdead0001;
    put(pool+layout.alignment_pool,uintptr_t(0x12341001));put(pool+layout.translate_pool,uintptr_t(0x12342001));
    for(double factor:{.6,1.,1.4}){
        std::memset(heap,0x5a,sizeof(heap));put(thread+96,start);put(thread+104,start+sizeof(heap));
        auto object=wrap_scaled_widget(child,thread,pool,nullptr_dart,factor,layout);
        if(factor==1){assert(object==child&&get<uintptr_t>(thread+96)==start);continue;}
        assert(object==start+1&&get<uintptr_t>(thread+96)==start+80);
        assert(get<uint64_t>(start)==layout.tag&&get<uint32_t>(object+11)==child);
        assert(get<double>(object+15)==1&&get<double>(object+27)==factor&&get<double>(object+43)==0);
        assert(get<uint32_t>(object+23)==0x12342001&&get<uint32_t>(object+51)==0x12341001);
        assert(get<uint32_t>(object+7)==nullptr_dart&&get<uint32_t>(object+55)==nullptr_dart
            &&get<uint32_t>(object+59)==nullptr_dart&&get<uint32_t>(object+67)==nullptr_dart);
        assert(get<uint32_t>(object+63)==uint32_t(nullptr_dart+0x30));
        for(unsigned i=80;i<sizeof(heap);++i)assert(heap[i]==0x5a);
    }
    put(thread+96,start);put(thread+104,start+80);
    assert(wrap_scaled_widget(child,thread,pool,nullptr_dart,.8,layout)==child&&get<uintptr_t>(thread+96)==start);
    std::cout<<"independent scales, old endpoint/reset/range guards, close endpoints, widget resolver/allocation: PASS\n";
}
