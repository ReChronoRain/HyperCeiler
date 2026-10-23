/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_widget_feature_resolver.h"
#include "fixture_image.h"
#include "widget_render_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>

static WidgetScaleFeature resolve(const Functions& fs) {
    auto bytes=image(fs);auto code=Code::parse(bytes);assert(code);
    auto feature=widget_scale_feature(*code);assert(feature);return *feature;
}
int main(int argc,char**argv) {
    auto fixture=widget_render_fixture();auto feature=resolve(fixture);
    assert(feature.dispatch.owner==widget_render_roots.at("dispatcher"));
    assert(feature.tail==widget_render_roots.at("tail"));
    std::map<uint32_t,uint32_t> addresses;
    auto moved=relocated(fixture,addresses);
    // VM allocator stubs tail-B into the shared allocator, not BL. Move that
    // link as well as calls; keeping its old displacement is a broken fixture.
    for(size_t k=0;k<moved.size();++k)for(size_t i=0;i<moved[k].words.size();++i) {
        auto& word=moved[k].words[i];if((word&0xfc000000u)!=0x14000000u)continue;
        const uint32_t target=fixture[k].va+uint32_t(i*4)+(int32_t(word<<6)>>6)*4;
        if(addresses.count(target))word=0x14000000u|(uint32_t((int64_t(addresses.at(target))-moved[k].va-i*4)/4)&0x03ffffffu);
    }
    auto shifted=resolve(moved);
    assert(shifted.dispatch.owner==addresses.at(feature.dispatch.owner));
    assert(shifted.tail==addresses.at(feature.dispatch.closure_allocator)+60);
    for(uint32_t bad:{feature.dispatch.owner,feature.dispatch.context_allocator,
        feature.dispatch.closure_allocator,feature.transform.allocator,widget_render_roots.at("common")}) {
        auto fs=fixture;auto it=std::find_if(fs.begin(),fs.end(),[&](auto& f){return f.va==bad;});assert(it!=fs.end());
        it->words[0]=0xd503201fu;auto bytes=image(fs);auto code=Code::parse(bytes);assert(code);
        assert(!widget_scale_feature(*code));
    }
    // A second complete dispatcher graph is ambiguity, never first-match wins.
    auto ambiguous=fixture;auto clone=*std::find_if(fixture.begin(),fixture.end(),[&](auto& f){return f.va==feature.dispatch.owner;});
    const uint32_t old=clone.va;clone.va=0x04000000;
    for(size_t i=0;i<clone.words.size();++i){uint32_t to=0;if(home_dynamic::target(clone.words[i],old+i*4,to))
        clone.words[i]=0x94000000u|(uint32_t((int64_t(to)-clone.va-i*4)/4)&0x03ffffffu);}
    ambiguous.push_back(clone);auto bytes=image(ambiguous);auto code=Code::parse(bytes);assert(code&&!widget_scale_feature(*code));

    // Execute the shipping Observer completion + wrapper, including a valid
    // 8-mod-16 nursery. Parent/closure pointers never escape this invocation.
    alignas(16) std::byte thread[1024]{},pool[0x200000]{},heap[512]{},parent_storage[512]{},frame_storage[2048]{};
    auto put=[](uintptr_t at,auto value){std::memcpy(reinterpret_cast<void*>(at),&value,sizeof(value));};
    auto get64=[](uintptr_t at){uint64_t value;std::memcpy(&value,reinterpret_cast<void*>(at),8);return value;};
    const uintptr_t th=reinterpret_cast<uintptr_t>(thread),pp=reinterpret_cast<uintptr_t>(pool),top=reinterpret_cast<uintptr_t>(heap),
        parent=reinterpret_cast<uintptr_t>(parent_storage)+1,fp=reinterpret_cast<uintptr_t>(frame_storage)+1024;
    const auto& c=feature.builders.front();const auto& layout=feature.layout;constexpr uint64_t base=0x100000000,closure=0x12340001,null_value=0xdead0001;
    put(pp+layout.alignment_pool,uintptr_t{0x12341001});put(pp+layout.translate_pool,uintptr_t{0x12342001});
    unsigned cases=0;
    for(unsigned alignment:{0u,8u,4u})for(unsigned room:{79u,80u,512u})for(double factor:{.6,.79,1.,1.25,1.4}) {
        put(th+layout.top,top+alignment);put(th+layout.top+8,top+room);put(fp+c.parent_frame,parent);
        put(parent+c.child,uint32_t{0});WidgetScaleResult why{};
        auto result=complete_widget_observer(closure,fp,base+c.return_pc,base,th,pp,null_value,factor,feature.builders,layout,&why);
        assert(result.handled);assert(uint32_t(get64(parent+c.child))==closure);
        const bool allocated=factor!=1&&alignment!=4&&room>alignment+layout.size;
        assert(result.value==(allocated?top+alignment+1:parent));
        assert(get64(th+layout.top)==top+alignment+(allocated?layout.size:0));
        assert(why==(factor==1?WidgetScaleResult::Neutral:allocated?WidgetScaleResult::Applied:WidgetScaleResult::NurseryUnavailable));
        ++cases;
    }
    // Unknown caller must not even dereference an invalid frame.
    auto untouched=complete_widget_observer(closure,0,base+c.return_pc+4,base,0,0,0,.79,feature.builders,layout);
    assert(!untouched.handled&&untouched.value==closure);
    if(argc>1) {
        std::ifstream file(argv[1],std::ios::binary);std::vector<char>raw{std::istreambuf_iterator<char>(file),{}};
        auto full=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(full);
        auto found=widget_scale_feature(*full);assert(found&&found->dispatch.owner==feature.dispatch.owner&&found->tail==feature.tail);
        const auto anonymous=image(full->functions());auto stripped=Code::parse(anonymous);assert(stripped&&widget_scale_feature(*stripped));
    }
    std::cout<<"WIDGET_RENDER PASS: full/sparse executable discovery, independent relocation, missing links, ambiguity; "<<cases<<" real completion/allocation cases and unknown caller protection\n";
}
