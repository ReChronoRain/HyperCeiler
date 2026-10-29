/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_custom_title_feature.h"
#include "targets/home/home_recent_render.h"
#include "fixture_image.h"
#include "custom_fixture.h"
#include <atomic>
#include <memory>
#include <iostream>
#include <fstream>
#include <iterator>
struct CustomTitleRouting {uintptr_t base;home_layout::features::CustomTitleFeature feature;};
CustomTitleRouting g_custom_title_routing;
std::atomic_bool g_title_custom_bound{true};
using CustomTitles=std::vector<home_title::CustomTitle>;
std::shared_ptr<const CustomTitles> g_title_names;
std::atomic_uint64_t g_title_custom_hits{0},g_title_custom_applied{0};
std::atomic_uint32_t g_title_custom_skip_reasons{0},g_title_custom_model_cid{0},g_title_custom_package_cid{0};
std::atomic_uintptr_t g_title_custom_method{0};
#include "actual_callback.h"
static auto resolve(const Functions& f,uint32_t app=custom_app,std::array<uint32_t,2> builders=custom_builders){
    auto bytes=image(f);auto c=Code::parse(bytes);assert(c);home_layout::features::IconFeature icon;icon.app=app;
    home_layout::features::DockTitleFeature dock;dock.builders[0].va=builders[0];dock.builders[1].va=builders[1];
    return home_layout::features::custom_title_feature(*c,icon,dock);
}
template<class T>void put(uintptr_t at,T value){std::memcpy(reinterpret_cast<void*>(at),&value,sizeof(T));}
template<class T>T get(uintptr_t at){T value;std::memcpy(&value,reinterpret_cast<void*>(at),sizeof(T));return value;}
int main(int argc,char** argv){
    const auto fixture=custom_fixture();const auto found=resolve(fixture);assert(found);
    assert(found->sites[0].aux_frame&&found->sites[0].resume-found->sites[0].patch==20);
    std::map<uint32_t,uint32_t> moved;auto shifted=relocated(fixture,moved);auto other=resolve(shifted,moved.at(custom_app),{moved.at(custom_builders[0]),moved.at(custom_builders[1])});
    assert(other&&other->component==moved.at(found->component)&&other->two_allocator==moved.at(found->two_allocator));
    for(uint32_t required:{found->package,found->component,found->pin,found->two_allocator,found->one_allocator,found->sites[0].owner,found->sites[1].owner}){
        auto bad=fixture;std::erase_if(bad,[&](const auto& f){return f.va==required;});assert(!resolve(bad));
    }
    auto changed=fixture;
    for(auto& fn:changed)if(fn.va==found->one_allocator||fn.va==found->two_allocator)for(auto& w:fn.words)
        if(home_dynamic::rn(w)==26&&((w&0xffc00000u)==0xf9400000u||(w&0xffc00000u)==0xf9000000u))w+=4u<<10;
    auto fields=resolve(changed);assert(fields&&fields->strings.top==found->strings.top+32&&fields->strings.end==found->strings.end+32);
    auto frames=fixture;
    for(auto& fn:frames)if(fn.va==found->sites[0].owner){fn.words[2]+=8u<<10;
        for(auto& w:fn.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<0)
            w=(w&~0x1ff000u)|(uint32_t((home_dynamic::imm9(w)-8)&511)<<12);}
    auto new_frames=resolve(frames);assert(new_frames&&new_frames->sites[0].source_frame==found->sites[0].source_frame-8&&new_frames->sites[0].label_frame==found->sites[0].label_frame-8);
    auto tags=fixture;
    for(auto& fn:tags)if(fn.va==found->one_allocator||fn.va==found->two_allocator)
        for(size_t i=0;i+1<fn.words.size();++i)if(home_dynamic::movz(fn.words[i],16)&&(fn.words[i+1]&0xffe0001fu)==0xf2a00010u){
            auto tag=home_dynamic::zimm(fn.words[i])|(home_dynamic::zimm(fn.words[i+1])<<16);tag+=32u<<12;
            fn.words[i]=(fn.words[i]&~0x1fffe0u)|((tag&65535)<<5);fn.words[i+1]=(fn.words[i+1]&~0x1fffe0u)|((tag>>16)<<5);}
    auto new_tags=resolve(tags);assert(new_tags&&new_tags->strings.two_cid==found->strings.two_cid+32);
    auto models=fixture;
    for(auto& fn:models)if(fn.va==found->sites[0].owner)for(auto& w:fn.words)
        if(home_dynamic::movz(w,17)&&home_dynamic::zimm(w)==unsigned(found->sites[0].model_field))w+=4u<<5;
    auto new_models=resolve(models);assert(new_models&&new_models->sites[0].model_field==found->sites[0].model_field+4);
    unsigned checks=0;
    for(const auto& variant:std::vector<home_layout::features::CustomTitleFeature>{*found,*fields,*new_frames,*new_tags,*new_models})
    for(unsigned alignment:{0,8})for(auto label:{std::u16string(u"重命名🙂"),std::u16string(u"N"),std::u16string(512,u'字')}){
        alignas(16) unsigned char arena[65536]{},stack[1024]{};alignas(16) uint64_t dispatch[32]{};
        auto raw=reinterpret_cast<uintptr_t>(arena),fp=reinterpret_cast<uintptr_t>(stack+512);
        auto model=raw+0x1001,intent=raw+0x2001,component=raw+0x3001,package=raw+0x4001,original=raw+0x5001,thread=raw+0x6000,widget=raw+0x7001,controller=raw+0xb001,top=raw+0x8000+alignment;
        const auto& f=variant;const auto heap=raw>>32;assert((raw+sizeof(arena))>>32==heap);
        put(model-1,uint64_t(f.model.dispatch+5)<<12);dispatch[5]=0x10000000+f.component;
        put(model+f.model.flag,uint32_t(0));put(model+f.model.intent,uint32_t(intent));put(intent+f.model.component,uint32_t(component));
        put(component+f.model.component_package,uint32_t(package));put(model+f.model.package,uint32_t(package));put(model+f.model.pin_name,uint32_t(package));
        constexpr char pkg[]="com.miui.notes";
        put(package-1,uint64_t(f.strings.one_cid)<<12);put(package+f.strings.length,uint32_t((sizeof(pkg)-1)*2));std::memcpy(reinterpret_cast<void*>(package+f.strings.data),pkg,sizeof(pkg)-1);
        put(original-1,uint64_t(f.strings.two_cid)<<12);put(original+f.strings.length,uint32_t(4));std::memcpy(reinterpret_cast<void*>(original+f.strings.data),u"笔记",4);
        g_custom_title_routing={0x10000000,f};g_title_names=std::make_shared<const CustomTitles>(CustomTitles{{u"com.miui.notes",label}});
        for(unsigned site=0;site<2;++site){
            const auto& s=f.sites[site];put(thread+f.strings.top,uint64_t(top));put(thread+f.strings.end,uint64_t(top+4096));
            home_layout::recent_render::Registers r{};for(unsigned i=0;i<32;++i){r.x[i]=0x1000+i;r.q[i][0]=100+i;r.q[i][1]=200+i;}
            r.x[29]=fp;r.x[28]=heap;r.x[26]=thread;r.x[21]=reinterpret_cast<uintptr_t>(dispatch);r.x[22]=(heap<<32)+0x81;r.x[0]=widget;r.x[1]=0x1234;
            if(site==0){put(fp+s.source_frame,uint64_t(controller));put(controller+s.model_field,uint32_t(model));put(controller+s.label_field,uint32_t(original));if(s.aux_frame)put(fp+s.aux_frame,uint64_t(3));}
            else {put(fp+s.model_frame,uint64_t(model));put(widget+s.label_field,uint32_t(original));}
            auto before=r;hc_customtitle_body(&r,site);const auto result=r.x[site==0?3:2];
            assert(result==top+1&&home_title::string_equals(result,label,f.strings));assert(get<uint64_t>(fp+s.label_frame)==result);
            assert(r.branch==0x10000000+s.owner+s.resume);
            for(unsigned i=0;i<32;++i)if(i!=2&&i!=3&&!(site==0&&i==4))assert(r.x[i]==before.x[i]);
            assert(std::memcmp(r.q,before.q,sizeof(r.q))==0);if(site==0)assert(r.x[2]==controller&&r.x[4]==3);else assert(r.x[3]==before.x[3]&&get<uint64_t>(fp+s.aux_frame)==0x1234);++checks;
        }
        for(unsigned fault=0;fault<5;++fault){
            put(thread+f.strings.top,uint64_t(top));put(thread+f.strings.end,uint64_t(top+4096));dispatch[5]=0x10000000+f.component;
            auto names=*g_title_names;home_title::CustomResult why{};
            if(fault==0)names.clear();if(fault==1)put(thread+f.strings.top,uint64_t(top+1));
            if(fault==2)put(thread+f.strings.end,uint64_t(top+1));if(fault==3)dispatch[5]=0x12345678;if(fault==4)names[0].package=u"com.other";
            auto before=get<uint64_t>(thread+f.strings.top);auto result=home_title::custom_label(original,model,heap,thread,reinterpret_cast<uintptr_t>(dispatch),(heap<<32)+0x81,0x10000000+f.component,0x10000000+f.pin,names,f.model,f.strings,&why);
            assert(result==original&&get<uint64_t>(thread+f.strings.top)==before);++checks;
        }
        dispatch[5]=0x10000000+f.pin;put(thread+f.strings.top,uint64_t(top));put(thread+f.strings.end,uint64_t(top+4096));
        assert(home_title::model_package(model,heap,reinterpret_cast<uintptr_t>(dispatch),(heap<<32)+0x81,0x10000000+f.component,0x10000000+f.pin,f.model)==package);++checks;
        home_title::CustomResult why{};auto neutral=home_title::clone_label(original,thread,u"",f.strings,&why);assert(neutral==original&&get<uint64_t>(thread+f.strings.top)==top);++checks;
    }
    if(argc>1){std::ifstream in(argv[1],std::ios::binary);std::vector<char> bytes{std::istreambuf_iterator<char>(in),{}};auto c=Code::parse({reinterpret_cast<std::byte*>(bytes.data()),bytes.size()});assert(c);auto icon=home_layout::features::icon_feature(*c);auto dock=home_layout::features::dock_title_feature(*c);assert(icon&&dock);auto all=home_layout::features::custom_title_feature(*c,*icon,*dock);assert(all&&all->component==found->component&&all->sites[0].patch==found->sites[0].patch);std::cout<<"full image feature graph PASS\n";}
    std::cout<<"Custom title production callback PASS "<<checks<<" cases, root replay, Unicode/512 units, nursery 0/8 mod 16, guards, disabled, pin dispatch, relocated and missing graphs\n";
}
