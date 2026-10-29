/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_dock_position_feature.h"
#include "fixture_image.h"
#include "position_fixture.h"
#include <iostream>
using namespace home_layout::features;
static auto resolve(const Functions& f,uint32_t root=position_config) {
    const auto bytes=image(f);const auto code=Code::parse(bytes);assert(code);
    return dock_position_feature(*code,root);
}
int main(){
    const auto fixture=position_fixture();const auto found=resolve(fixture);assert(found&&found->owner==position_owner);
    std::map<uint32_t,uint32_t> map;const auto moved=relocated(fixture,map);
    // Config is an external shared root in this bounded fixture.
    assert(resolve(moved)->owner==map.at(position_owner));
    auto duplicate=fixture;auto copy=fixture[0];copy.va+=0x2000000;
    for(size_t i=0;i<copy.words.size();++i){uint32_t target=0;
        if(home_dynamic::target(copy.words[i],fixture[0].va+uint32_t(i*4),target))
            copy.words[i]=0x94000000u|(uint32_t((int64_t(target)-copy.va-i*4)/4)&0x3ffffffu);}
    duplicate.push_back(copy);assert(!resolve(duplicate));
    for(size_t role=0;role<fixture.size();++role){auto broken=fixture;broken.erase(broken.begin()+role);assert(!resolve(broken));}
    for(auto site:found->sites){auto broken=fixture;broken[0].words[site/4]=0xd503201fu;assert(!resolve(broken));}
    for(auto field:{found->count,found->index}){auto broken=fixture;
        for(auto& word:broken[0].words)if(home_dynamic::stx(word,29,field==found->count?2:3)&&home_dynamic::imm9(word)==field){word=0xd503201fu;break;}
        assert(!resolve(broken));}
    alignas(16) unsigned char frame[256]{},saved[512]{};
    const auto fp=reinterpret_cast<uintptr_t>(frame+128);unsigned checks=0;
    for(int64_t count=1;count<=12;++count)for(int64_t index=0;index<count;++index)
        for(unsigned arm=0;arm<2;++arm)for(int delta:{-20,0,20,80}){
            std::memcpy(reinterpret_cast<void*>(fp+found->count),&count,8);std::memcpy(reinterpret_cast<void*>(fp+found->index),&index,8);
            std::memset(saved,0x53,sizeof(saved));const size_t at=(arm==0?3:2)*16;double value=43.5;std::memcpy(saved+at,&value,8);
            unsigned char before[512];std::memcpy(before,saved,512);
            assert(inset_dock_position(fp,saved,arm,delta,*found)==(delta!=0));double actual=0;std::memcpy(&actual,saved+at,8);
            assert(std::abs(actual-(value+delta*(1.0-(2.0*index+1)/count)))<1e-8);
            std::memcpy(saved+at,before+at,8);assert(std::memcmp(saved,before,512)==0);++checks;
        }
    for(int64_t bad:{-1,0,33}){int64_t index=0;std::memcpy(reinterpret_cast<void*>(fp+found->count),&bad,8);std::memcpy(reinterpret_cast<void*>(fp+found->index),&index,8);assert(!inset_dock_position(fp,saved,0,80,*found));}
    std::cout<<"Dock position discovery and operands PASS "<<checks<<" checks, stripped executable fixture, relocation, missing/ambiguous graph, both returns and untouched registers\n";
}
