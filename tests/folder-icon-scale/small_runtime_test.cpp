#include "targets/home/home_folder_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
static std::optional<FolderFeatures> resolve(const Functions& functions,uint32_t current){
    auto bytes=image(functions);auto code=Code::parse(bytes);assert(code);
    FolderFeatures result;if(!resolve_small_folder(*code,current,result))return {};
    return result;
}
int main(){
    auto original=fixture();auto result=resolve(original,config);assert(result&&result->small==small);
    assert(result->small_site.caller_return&&result->preview_height&&result->base);
    uint32_t width=0;
    for(auto& f:original)if(f.va==small)for(size_t i=0;i+2<f.words.size();++i){uint32_t to=0;
        if(home_dynamic::target(f.words[i],f.va+uint32_t(i*4),to)
            &&home_dynamic::std_(f.words[i+2],1,0)
            &&home_dynamic::imm9(f.words[i+2])==result->small_site.dimensions[2])width=to;
    }
    assert(width&&width!=result->preview_height);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(original,addresses);
    auto translated=resolve(moved,addresses.at(config));assert(translated);
    assert(translated->small==addresses.at(small)&&translated->base==addresses.at(result->base)
        &&translated->preview_height==addresses.at(result->preview_height)
        &&translated->small_site.dimensions==result->small_site.dimensions);
    auto changedFields=original;
    for(auto& f:changedFields){
        if(f.va==small)for(auto& word:f.words)
            if(home_dynamic::std_(word,1,0)||home_dynamic::std_(word,1,1))word+=8u<<12;
        if(f.va==width||f.va==result->preview_height)for(size_t i=0;i+2<f.words.size();++i)
            if(home_dynamic::ldw(f.words[i],0,1)&&f.words[i+1]==0x8b1c8021u
                &&home_dynamic::ldd(f.words[i+2],1,0))f.words[i]+=8u<<12;
    }
    auto changed=resolve(changedFields,config);assert(changed);
    for(size_t i=0;i<4;++i)assert(changed->small_site.dimensions[i]==result->small_site.dimensions[i]+8);
    for(auto missing:{small,config,width,result->base,result->preview_height}){
        auto broken=original;std::erase_if(broken,[&](auto& f){return f.va==missing;});
        assert(!resolve(broken,config));
    }
    // An unrelated helper with matching field numbers cannot substitute for
    // the same current-config dataflow. Corrupt each consumed helper word.
    unsigned mutations=0;
    for(auto& f:original)if(f.va==width||f.va==result->preview_height)
        for(size_t i=0;i<f.words.size();++i){
            auto broken=original;
            for(auto& candidate:broken)if(candidate.va==f.va)candidate.words[i]=0xd503201fu;
            assert(!resolve(broken,config));++mutations;
        }
    auto ambiguous=original;
    auto copy=*std::find_if(original.begin(),original.end(),[](auto& f){return f.va==small;});
    copy.va=0x02000000u;
    for(size_t i=0;i<copy.words.size();++i){uint32_t to=0;
        if(!home_dynamic::target(copy.words[i],small+uint32_t(i*4),to))continue;
        int64_t delta=int64_t(to)-(copy.va+i*4);
        copy.words[i]=0x94000000u|(uint32_t(delta/4)&0x03ffffffu);
    }
    ambiguous.push_back(copy);
    assert(!resolve(ambiguous,config));
    std::cout<<"SMALL_FOLDER_RUNTIME PASS: moved functions/fields, missing/ambiguous producers, "<<mutations<<" helper corruptions\n";
}
