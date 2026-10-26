/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_feature_resolver.h"
#include "folder_fixture.h"
#include <cassert>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <map>

#include "fixture_image.h"

static bool resolve(const Functions& input,FolderFeatures& out) {
    const auto bytes=image(input);auto code=Code::parse(bytes);assert(code);
    return resolve_folders(*code,kCurrent,out);
}
int main(int argc,char** argv) {
    FolderFeatures result;auto fixture=folder_fixture();
    assert(resolve(fixture,result));
    assert(result.small==kSmall&&result.base==kBase&&result.big==kBig&&result.preview_builder==kBigPreview);
    assert((result.small_site.dimensions==std::array<int32_t,4>{15,23,39,31}));
    assert(result.big_site.dimensions.size()==13);
    std::map<uint32_t,uint32_t> moved_addresses;auto moved=relocated(fixture,moved_addresses);
    assert(resolve(moved,result));
    assert(result.small==moved_addresses.at(kSmall)&&result.base==moved_addresses.at(kBase)
        &&result.big==moved_addresses.at(kBig)&&result.preview_builder==moved_addresses.at(kBigPreview));
    for(auto& f:moved)for(auto& w:f.words){
        const auto kind=w&0xffe00c00u;const auto rn=home_dynamic::rn(w);
        if(f.va==moved_addresses.at(kSmall)){
            if(home_dynamic::std_(w,1,0)||home_dynamic::std_(w,1,1))w+=8u<<12;
        }else if(f.va==moved_addresses.at(kBig)||f.va==moved_addresses.at(kWidth)||f.va==moved_addresses.at(kHeight)){
            if(kind==0xfc000000u&&(rn==1||rn==2))w+=8u<<12;
        }else if(f.va==moved_addresses.at(kBigPreview)&&kind==0xfc400000u&&rn!=29)w+=8u<<12;
    }
    assert(resolve(moved,result));
    assert((result.small_site.dimensions==std::array<int32_t,4>{23,31,47,39}));
    // Removing a producer, helper, or consumer rejects the entire coherent bank.
    for(uint32_t missing:{kSmall,kBase,kBig,kWidth,kHeight,kBigPreview}){
        auto broken=fixture;std::erase_if(broken,[&](auto& f){return f.va==missing;});
        assert(!resolve(broken,result));
    }
    // Same-shape alternatives are ambiguous even when their addresses differ.
    for(uint32_t duplicate:{kSmall,kBig,kBigPreview}){
        auto broken=fixture;auto f=*std::find_if(broken.begin(),broken.end(),[&](auto& x){return x.va==duplicate;});
        const uint32_t original=f.va;f.va=0x02000000u;
        for(size_t i=0;i<f.words.size();++i){uint32_t to=0;
            if(!home_dynamic::target(f.words[i],original+uint32_t(i*4),to))continue;
            const int64_t delta=int64_t(to)-(f.va+i*4);
            f.words[i]=0x94000000u|(uint32_t(delta/4)&0x03ffffffu);
        }
        broken.push_back(std::move(f));assert(!resolve(broken,result));
    }
    // Every observed geometry store and every extra preview consumption must
    // remain part of the original producer/consumer relation.
    unsigned mutations=0;
    for(size_t n=0;n<fixture.size();++n){const auto& function=fixture[n];
        for(size_t i=0;i<function.words.size();++i){const auto w=function.words[i];
            bool mutate=false;
            if(function.va==kSmall)mutate=home_dynamic::std_(w,1,0)||home_dynamic::std_(w,1,1);
            if(function.va==kBig)mutate=(w&0xffe00c00u)==0xfc000000u&&(home_dynamic::rn(w)==1||home_dynamic::rn(w)==2);
            if(!mutate)continue;
            auto broken=fixture;broken[n].words[i]=0xd503201fu;
            assert(!resolve(broken,result));++mutations;
        }
    }
    if(argc>1){
        std::ifstream in(argv[1],std::ios::binary);assert(in);
        std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        Image bytes(raw.size());std::memcpy(bytes.data(),raw.data(),raw.size());
        auto code=Code::parse(bytes);assert(code);
        assert(resolve_folders(*code,kCurrent,result));
        assert(result.small==kSmall&&result.big==kBig&&result.base==kBase&&result.preview_builder==kBigPreview);
        std::cout<<"FOLDER_FEATURE_FULL_IMAGE passed ("<<code->functions().size()<<" unnamed executable candidates)\n";
    }
    std::cout<<"FOLDER_FEATURE_ADDRESSING passed (no symbols/sections, independently moved functions/fields, missing links, ambiguity, "
        <<mutations<<" corrupt geometry stores)\n";
}
