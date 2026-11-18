#include "targets/home/home_medium_folder_scale.h"
#include "targets/home/home_folder_preview_return.h"
#include "targets/home/home_object_scale.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
int main() {
    const auto original=fixture();
    auto bytes=image(original);auto code=Code::parse(bytes);assert(code);
    const auto found=medium_folder_feature(*code,config);assert(found&&found->producer==producer);
    assert(found->preview==preview&&found->span_site&&found->base);
    FolderFeatures folders;folders.current=config;folders.preview_builder=preview;
    IconFeature icon;icon.current=config;
    auto animation=folder_preview_return_feature(*code,folders,icon);
    assert(animation&&animation->origin==origin&&animation->landing==landing);
    std::map<uint32_t,uint32_t> mapping;auto moved=relocated(original,mapping);
    bytes=image(moved);code=Code::parse(bytes);assert(code);
    auto translated=medium_folder_feature(*code,mapping.at(config));assert(translated);
    assert(translated->producer==mapping.at(producer)&&translated->base==mapping.at(found->base));
    assert(translated->span_site==found->span_site&&translated->geometry.dimensions==found->geometry.dimensions);
    folders.current=mapping.at(config);folders.preview_builder=mapping.at(preview);icon.current=folders.current;
    auto relocated_animation=folder_preview_return_feature(*code,folders,icon);
    assert(relocated_animation&&relocated_animation->origin==mapping.at(origin)&&relocated_animation->landing==mapping.at(landing));
    auto wrong_config=original;
    for(auto& f:wrong_config)if(f.va==origin)for(auto& w:f.words)if(home_dynamic::ldw(w,0,1)) {w+=8u<<12;break;}
    bytes=image(wrong_config);code=Code::parse(bytes);assert(code);
    folders.current=config;folders.preview_builder=preview;icon.current=config;
    assert(!folder_preview_return_feature(*code,folders,icon));
    // Missing links and a corrupt span replay must never pick another renderer.
    for(auto missing:{producer,preview,found->base}) {
        auto broken=original;std::erase_if(broken,[&](const auto& f){return f.va==missing;});
        bytes=image(broken);code=Code::parse(bytes);assert(code);
        assert(!medium_folder_feature(*code,config));
    }
    auto broken=original;
    for(auto& f:broken)if(f.va==producer)f.words[found->span_site/4]=0xaa0003e1;
    bytes=image(broken);code=Code::parse(bytes);assert(code);assert(!medium_folder_feature(*code,config));
    auto duplicate=moved;for(auto& f:duplicate)f.va-=0x02000000;
    auto ambiguous=original;ambiguous.insert(ambiguous.end(),duplicate.begin(),duplicate.end());
    bytes=image(ambiguous);code=Code::parse(bytes);assert(code);assert(!medium_folder_feature(*code,config));
    assert(medium_folder_kind(4,2)==1&&medium_folder_kind(2,4)==2);
    for(auto w:{0u,2u,4u,6u,8u,7u})for(auto h:{0u,2u,4u,6u,8u,3u})
        if(!((w==4&&h==2)||(w==2&&h==4)))assert(medium_folder_kind(w,h)==0);
    // Independent persisted switches and percentages survive the optional tail.
    for(int mask=0;mask<4;++mask)for(int a:{60,100,140})for(int b:{60,100,140}) {
        home_layout::ObjectScales scales{};std::vector<int> words={home_layout::kMediumScaleMagic,mask&1,a,(mask>>1)&1,b};size_t at=0;
        assert(home_layout::read_medium_scales([&](int32_t& out){if(at==words.size())return false;out=words[at++];return true;},scales,false));
        assert(scales[3].factor()==((mask&1)?a/100.:1.)&&scales[4].factor()==((mask&2)?b/100.:1.));
        for(unsigned i=0;i<3;++i)assert(scales[i].factor()==1.);
    }
    std::cout<<"MEDIUM_FOLDER PASS: executable graph, independent relocation, missing/ambiguous links, span isolation and independent wire settings\n";
}
