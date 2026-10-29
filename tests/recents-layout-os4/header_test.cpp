/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "header_fixture.h"
#include <iostream>
using namespace home_layout::features::recent;
using namespace home_layout::recent_render;
static Plan plan(const Functions& fs){auto bytes=image(fs);auto code=Code::parse(bytes);assert(code);Plan p;
    assert(header_graph(*code,p)&&offset_graph(*code,p)&&inline_headers(*code,p));
    assert((p.hide_text_ready=header_text_contract(*code,p)));
    assert((p.hide_icon_ready=header_icon_contract(*code,p)));return p;
}
int main(){(void)&relocated;auto fixture=header_fixture();auto p=plan(fixture);unsigned cases=0;
    assert(p.sites.size()==12); // Five title stores, one offset publication, six icon paths.
    alignas(16) std::byte pool[0x200000]{},text[256]{},empty[64]{},frame[512]{},thread[1024]{};
    auto pp=reinterpret_cast<uintptr_t>(pool),obj=reinterpret_cast<uintptr_t>(text)+1;
    auto str=reinterpret_cast<uintptr_t>(empty)+1,fp=reinterpret_cast<uintptr_t>(frame)+256;
    store<uint64_t>(pp+p.empty_string_pool,str);store<uint64_t>(str-1,uint64_t(p.string_one_cid)<<12);
    store<uint32_t>(str+p.string_length,0);store<uint64_t>(obj-1,p.text_tag);
    store<uint64_t>(pp+p.empty_widget_pool,0x123456789);
    for(const auto& site:p.sites){
        if(site.action!=Action::Style&&site.action!=Action::FrameStyle&&site.action!=Action::HideIconReturn&&site.action!=Action::IconChild)continue;
        for(int icon:{0,1})for(int title:{0,1}){
            RecentConfig c;c.values[RecentConfig::HideIcon]=icon;c.values[RecentConfig::HideText]=title;
            Registers r{};r.branch=0xbeef;r.x[27]=pp;r.x[29]=fp;r.x[30]=0x987654321;r.x[15]=fp-64;
            r.x[26]=reinterpret_cast<uintptr_t>(thread);r.x[28]=1;
            store<uint64_t>(fp,0x112233445);store<uint64_t>(fp+8,0x667788999);
            store<uint32_t>(obj+p.text_data,0xabcdef01);
            if(site.text_object<31)r.x[site.text_object]=obj;
            auto result=apply(p,site,r,c,0);
            bool applied=(site.action==Action::HideIconReturn||site.action==Action::IconChild)?icon:title;
            assert(result==(applied?Result::Applied:Result::Neutral));
            if(site.action==Action::IconChild){
                assert(r.branch==0xbeef&&r.x[29]==fp&&r.x[30]==0x987654321);
                if(icon)assert(r.x[site.reg]==(site.compressed_icon?0x23456789ull:0x123456789ull));
                assert(load<uint32_t>(obj+p.text_data)==0xabcdef01);
            }else if(site.action==Action::HideIconReturn){
                if(icon){assert(r.x[0]==0x123456789&&r.branch==0);
                    assert(r.x[29]==(site.frame?0x112233445:fp));assert(r.x[30]==(site.frame?0x667788999:0x987654321));
                    assert(r.x[15]==(site.frame?fp+16:fp-64));}
                else assert(r.branch==0xbeef&&r.x[29]==fp&&r.x[30]==0x987654321);
                assert(load<uint32_t>(obj+p.text_data)==0xabcdef01);
            }else{
                assert(load<uint32_t>(obj+p.text_data)==(title?uint32_t(str):0xabcdef01));
                assert(r.branch==0xbeef&&r.x[29]==fp&&r.x[30]==0x987654321);
            }
            // Both operations allocate no heap and invoke no Dart call.
            assert(load<uint64_t>(r.x[26]+p.style_top)==0);++cases;
        }
    }
    auto site=*std::find_if(p.sites.begin(),p.sites.end(),[](auto&s){return s.action==Action::Style;});
    for(int bad:{0,1,2}){
        Registers r{};r.x[27]=pp;r.x[site.text_object]=obj;r.branch=0xbeef;
        RecentConfig c;c.values[RecentConfig::HideText]=1;store<uint32_t>(obj+p.text_data,0xdeadbeef);
        store<uint64_t>(str-1,uint64_t(p.string_one_cid)<<12);store<uint32_t>(str+p.string_length,0);store<uint64_t>(obj-1,p.text_tag);
        if(bad==0)store<uint32_t>(str+p.string_length,2);
        if(bad==1)store<uint64_t>(str-1,0);
        if(bad==2)store<uint64_t>(obj-1,0);
        assert(apply(p,site,r,c,0)==Result::WrongClass&&load<uint32_t>(obj+p.text_data)==0xdeadbeef);++cases;
    }
    {
        auto compressed=*std::find_if(p.sites.begin(),p.sites.end(),[](auto&s){return s.compressed_icon;});
        RecentConfig c;c.values[RecentConfig::HideIcon]=1;Registers r{};r.x[27]=pp;r.x[28]=2;r.x[compressed.reg]=7;r.branch=0xbeef;
        assert(apply(p,compressed,r,c,0)==Result::WrongClass&&r.x[compressed.reg]==7&&r.branch==0xbeef);++cases;
    }
    // Relocate complete executable owners and every direct call. A 64 KiB
    // spacing preserves the larger icon-layout function without overlap.
    Functions moved=fixture;std::map<uint32_t,uint32_t> addresses;
    for(size_t i=0;i<fixture.size();++i)addresses[fixture[i].va]=0x04000000+uint32_t((fixture.size()-i)*0x10000);
    for(auto& f:moved){auto old=f.va;f.va=addresses.at(old);for(size_t i=0;i<f.words.size();++i){auto&w=f.words[i];
        if((w&0x7c000000u)!=0x14000000u)continue;
        auto to=old+uint32_t(i*4)+(int32_t(w<<6)>>6)*4;auto found=addresses.find(to);if(found==addresses.end())continue;
        w=(w&0xfc000000u)|(uint32_t((int64_t(found->second)-f.va-i*4)/4)&0x3ffffffu);
    }}
    auto relocated_plan=plan(moved);assert(relocated_plan.hide_icon_ready&&relocated_plan.hide_text_ready);++cases;
    // Pool migration is decoded from instructions on both sides of the
    // nullable icon branch and the nullable title String branch.
    auto pools=fixture;
    for(auto& f:pools)for(auto& w:f.words)
        if((w&0xffc003e0u)==0xf9400360u&&((w>>10)&4095)<4095)w+=1u<<10;
    auto shifted=plan(pools);assert(shifted.empty_string_pool==p.empty_string_pool+8
        &&shifted.empty_widget_pool==p.empty_widget_pool+8);++cases;
    // Hiding names composes with font customization on a NEW style. The
    // canonical source TextStyle and empty String remain immutable.
    alignas(16) std::byte heap[512]{},style_source[256]{};
    auto top=reinterpret_cast<uintptr_t>(heap),source=reinterpret_cast<uintptr_t>(style_source)+1;
    store<uint64_t>(source-1,p.style_tag);store<uint64_t>(str-1,uint64_t(p.string_one_cid)<<12);
    store<uint32_t>(str+p.string_length,0);store<uint64_t>(obj-1,p.text_tag);
    for(unsigned alignment:{0u,8u}){
        Registers r{};r.x[27]=pp;r.x[26]=reinterpret_cast<uintptr_t>(thread);r.x[29]=fp;r.branch=0xbeef;
        r.x[site.reg]=source;r.x[site.text_object]=obj;
        store<uint64_t>(r.x[26]+p.style_top,top+alignment);store<uint64_t>(r.x[26]+p.style_end,top+512);
        RecentConfig c;c.values[RecentConfig::HideText]=1;c.values[RecentConfig::TextSize]=22;
        assert(apply(p,site,r,c,0)==Result::Applied&&r.x[site.reg]==top+alignment+1);
        assert(load<uint32_t>(obj+p.text_data)==uint32_t(str)&&load<uint64_t>(source-1)==p.style_tag);
        assert(load<uint32_t>(source+p.style_font)==0&&load<uint32_t>(str+p.string_length)==0);++cases;
    }
    // Duplicate the complete cache witness: reject the optional icon family,
    // never select the first same-shaped function or publish half the paths.
    auto dup=fixture;auto cache=*std::find_if(p.sites.begin(),p.sites.end(),[](auto&s){return s.action==Action::HideIconReturn&&s.frame;});
    auto original=*std::find_if(fixture.begin(),fixture.end(),[&](auto&f){return f.va<=cache.va&&cache.va<f.va+f.words.size()*4;});
    auto copy=original;copy.va=0x03000000;for(size_t i=0;i<copy.words.size();++i){uint32_t to=0;if(home_dynamic::target(copy.words[i],original.va+i*4,to))copy.words[i]=0x94000000u|(uint32_t((int64_t(to)-copy.va-i*4)/4)&0x3ffffffu);}
    dup.push_back(copy);auto bytes=image(dup);auto code=Code::parse(bytes);assert(code);Plan refused=p;refused.sites.erase(std::remove_if(refused.sites.begin(),refused.sites.end(),[](auto&s){return s.action==Action::HideIconReturn||s.action==Action::IconChild;}),refused.sites.end());auto count=refused.sites.size();
    assert(!header_icon_contract(*code,refused)&&refused.sites.size()==count);++cases;
    auto no_icon=fixture;
    no_icon.erase(std::remove_if(no_icon.begin(),no_icon.end(),[&](auto&f){return f.va==original.va;}),no_icon.end());
    bytes=image(no_icon);code=Code::parse(bytes);assert(code);
    assert(!header_icon_contract(*code,refused)&&refused.sites.size()==count);++cases;
    // Missing/invalid string ABI leaves title hiding unavailable, other header
    // functions still resolve and original app names remain untouched.
    auto missing=fixture;missing.erase(std::remove_if(missing.begin(),missing.end(),[](auto&f){return std::find(f.words.begin(),f.words.end(),0xf94001e2u)!=f.words.end();}),missing.end());
    bytes=image(missing);code=Code::parse(bytes);assert(code);Plan old=p;assert(!header_text_contract(*code,old));++cases;
    std::cout<<"recents header PASS "<<cases<<" cases, independent toggles, managed-frame return, fresh Text validation, zero allocation, relocation, ambiguity and missing ABI\n";
}
