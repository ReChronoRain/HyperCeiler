/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_drawer_feature_resolver.h"
#include "targets/home/home_drawer_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <fstream>
#include <iterator>
#include <iostream>
int main(int argc,char**argv){
    auto functions=drawer_fixture();auto raw=image(functions);auto code=Code::parse(raw);assert(code);
    auto p=drawer::resolve(*code);assert(p&&p->owner==owner&&p->site==site&&p->width==width&&p->caller==caller&&p->closure==closure&&p->empty_site==empty_site&&p->child_frame==child_frame&&p->holder==holder&&p->blank_factory==blank_factory);
    std::map<uint32_t,uint32_t> map;auto moved=relocated(functions,map);
    for(size_t n=0;n<moved.size();++n)for(size_t i=0;i<moved[n].words.size();++i){uint32_t to=0;
        auto w=functions[n].words[i];if((w&0xfc000000u)!=0x14000000u||!home_dynamic::target(w|0x80000000u,functions[n].va+uint32_t(i*4),to))continue;
        if(!map.count(to))continue;
        const auto delta=int64_t(map.at(to))-int64_t(moved[n].va+i*4);
        moved[n].words[i]=0x14000000u|(uint32_t(delta/4)&0x3ffffffu);}
    auto moved_raw=image(moved);
    auto moved_code=Code::parse(moved_raw);auto q=drawer::resolve(*moved_code);
    assert(q&&q->owner==map.at(owner)&&q->site==map.at(owner)+(site-owner)&&q->width==map.at(width)&&q->caller==map.at(caller)&&q->empty_site==map.at(closure)+(empty_site-closure)&&q->holder==map.at(holder)&&q->blank_factory==map.at(blank_factory));
    // An equally valid independent graph must be rejected instead of picking a
    // nearby address, first match, private name or particular allocator class ID.
    auto duplicated=functions;duplicated.insert(duplicated.end(),moved.begin(),moved.end());
    auto duplicate_raw=image(duplicated);assert(!drawer::resolve(*Code::parse(duplicate_raw)));
    for(auto remove:{owner,caller,width,closure}){auto broken=functions;
        broken.erase(std::remove_if(broken.begin(),broken.end(),[&](auto& f){return f.va==remove;}),broken.end());
        auto broken_raw=image(broken);assert(!drawer::resolve(*Code::parse(broken_raw)));}
    auto broken=functions;
    auto& body=std::find_if(broken.begin(),broken.end(),[](auto& f){return f.va==owner;})->words;
    body[(site-owner)/4+1]^=0x400;auto broken_raw=image(broken);
    assert(!drawer::resolve(*Code::parse(broken_raw)));
    unsigned checks=0;
    for(bool ready:{false,true})for(bool hidden:{false,true})for(uintptr_t original:{uintptr_t{0},uintptr_t{0xabc000}})
        for(int32_t offset:{child_frame,0,-3,-32768}) {
            alignas(16) std::array<uint64_t,4096> frame;frame.fill(0xfedcba9876543211);
            recent_render::Registers r{};for(unsigned i=0;i<32;++i){r.x[i]=0xaaa000+i;r.q[i][0]=i+123;r.q[i][1]=i+789;}
            r.x[29]=reinterpret_cast<uintptr_t>(frame.data()+2048);r.x[22]=0xabcd01;auto before=r;
            bool applied=drawer_render::apply(r,ready,hidden,original,offset);
            assert(applied==(ready&&hidden&&original&&offset==child_frame));
            assert(r.branch==original);before.branch=original;assert(!std::memcmp(&before,&r,sizeof(r)));
            for(unsigned i=0;i<frame.size();++i)assert(frame[i]==(applied&&i==2048+child_frame/8?r.x[22]:0xfedcba9876543211));
            ++checks;
        }
    for(bool ready:{false,true})for(bool hidden:{false,true})for(uintptr_t original:{uintptr_t{0},uintptr_t{0xabc000}})
        for(uintptr_t factory:{uintptr_t{0},uintptr_t{0xdef000},uintptr_t{0xdef001}}){
            recent_render::Registers r{};for(unsigned i=0;i<32;++i){r.x[i]=i+123;r.q[i][0]=i+456;r.q[i][1]=i+789;}auto before=r;
            bool applied=drawer_render::finder(r,ready,hidden,original,factory);
            assert(applied==(ready&&hidden&&original&&factory==0xdef000));
            before.branch=applied?factory:original;assert(!std::memcmp(&before,&r,sizeof(r)));++checks;}
    if(argc==2){std::ifstream in(argv[1],std::ios::binary);std::vector<char> b{std::istreambuf_iterator<char>(in),{}};
        auto real=Code::parse({reinterpret_cast<const std::byte*>(b.data()),b.size()});assert(real);
        auto found=drawer::resolve(*real);assert(found&&found->owner==owner&&found->site==site&&found->width==width&&found->caller==caller);}
    std::cout<<"drawer runtime discovery/render PASS: full image + sectionless fixture, independent relocation, ambiguity/missing links, "<<checks<<" last-use child/pass-through/register cases\n";
}
