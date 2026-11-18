/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_local_config_protocol.h"
#include <array>
#include <cassert>
#include <cstdio>

using namespace home_layout::cards;
using home_local_config::put_word;
using home_local_config::read_snapshot;
struct Group {
    std::vector<TaskPackage> members; bool valid=true; int* destroyed=nullptr;
    ~Group(){if(destroyed)++*destroyed;}
};
static size_t visits=0;
static auto access=[](void* raw,auto visit) {
    auto& group=*static_cast<Group*>(raw);
    for(const auto& task:group.members){++visits;if(!visit(task))return false;}
    return group.valid;
};
static void append(std::vector<uint8_t>& bytes,uint32_t word) {
    const auto at=bytes.size();bytes.resize(at+4);put_word(bytes.data()+at,word);
}
static std::vector<uint8_t> packet(std::initializer_list<std::string_view> names) {
    std::vector<uint8_t> bytes;
    for(auto word:{3u,2u,60u,static_cast<uint32_t>(names.size())})append(bytes,word);
    for(auto name:names){append(bytes,static_cast<uint32_t>(name.size()));bytes.insert(bytes.end(),name.begin(),name.end());}
    bytes.push_back(42);return bytes;
}
int main() {
    for(auto name:{"a","a.b","_a.B2","com.example.app"})assert(valid_package(name));
    for(auto name:{"",".a","a.","a..b","1abc","a.2b","a-b","a b","应用","a\nb"})assert(!valid_package(name));
    assert(valid_package(std::string(255,'a'))&&!valid_package(std::string(256,'a')));
    auto selected=std::make_shared<const Selection>(Selection{{"hide.app"}});
    const TaskPackage hidden{"hide.app",{},{}},shown{"keep.app",{},{}},unknown{};
    assert(hidden.hidden(*selected)&&!shown.hidden(*selected)&&!unknown.hidden(*selected));
    assert((TaskPackage{{},"hide.app",{}}).hidden(*selected));
    assert((TaskPackage{{},{},"hide.app"}).hidden(*selected));
    assert(!(TaskPackage{"keep.app","hide.app","hide.app"}).hidden(*selected));
    assert(!(TaskPackage{{},"keep.app","hide.app"}).hidden(*selected));
    assert(!(TaskPackage{"","hide.app","hide.app"}).hidden(*selected));
    Group one{{shown}},split{{shown,hidden}},mixed{{shown,unknown,shown,hidden}},empty{{}},bad{{hidden},false};
    std::array<void*,5> groups{&one,&split,nullptr,&mixed,&empty};
    const auto original=groups;
    Plan plan;assert(prepare(groups,selected,access,plan));
    assert(groups==original&&plan.count==5&&plan.removed==2&&plan.remove[1]&&plan.remove[3]);
    std::vector<void*> dropped;
    assert(!commit(groups,plan,std::make_shared<const Selection>(*selected),[&](void* value){dropped.push_back(value);}));
    assert(groups==original&&dropped.empty());
    assert(!commit(groups,plan,Snapshot{},[&](void* value){dropped.push_back(value);}));
    assert(groups==original&&dropped.empty());
    assert(commit(groups,plan,selected,[&](void* value){dropped.push_back(value);})==3);
    assert(groups[0]==&one&&groups[1]==nullptr&&groups[2]==&empty);
    assert(dropped==std::vector<void*>({&split,&mixed}));
    std::array<void*,2> invalid{&split,&bad};
    const auto before=invalid;const auto previous=plan;
    visits=0;assert(!prepare(invalid,selected,access,plan));
    assert(visits==3&&invalid==before&&plan.remove==previous.remove&&plan.count==previous.count);
    invalid={&split,&split};assert(!prepare(invalid,selected,access,plan));
    Group large{std::vector<TaskPackage>(129,hidden)};invalid={&split,&large};
    assert(!prepare(invalid,selected,access,plan));
    large.members.resize(128);assert(prepare(invalid,selected,access,plan)&&plan.removed==2);
    auto many=std::vector<void*>(4097,nullptr);assert(!prepare(many,selected,access,plan));
    many.resize(4096);assert(prepare(many,selected,access,plan)&&plan.removed==0);
    visits=0;assert(prepare(invalid,std::make_shared<const Selection>(),access,plan)&&plan.removed==0&&visits==0);
    for(int i=0;i<10000;++i){auto current=original;assert(prepare(current,selected,access,plan)&&plan.removed==2);}
    int destroyed=0;
    std::array<void*,2> allocated{new Group{{hidden},true,&destroyed},new Group{{shown},true,&destroyed}};
    assert(prepare(allocated,selected,access,plan));
    assert(commit(allocated,plan,selected,[](void* group){delete static_cast<Group*>(group);})==1);
    assert(destroyed==1);delete static_cast<Group*>(allocated[0]);assert(destroyed==2);

    // Exercise the actual boundary parser, including deterministic ordering,
    // unavailable/reset distinctions and transactional output on rejection.
    auto bytes=packet({"a.app","z.app"});home_local_config::Snapshot decoded;
    assert(read_snapshot(3,bytes,decoded)&&decoded.cards&&decoded.cards->packages.size()==2);
    assert(decoded.layout.size()==1&&decoded.layout[0]==42&&decoded.profile.strength==60);
    auto token=decoded.cards;
    for(size_t size=0;size<bytes.size();++size) {
        assert(!read_snapshot(3,{bytes.data(),size},decoded));assert(decoded.cards==token);
    }
    for(auto invalidNames:{packet({"z.app","a.app"}),packet({"a.app","a.app"}),packet({"a..app"})}) {
        assert(!read_snapshot(3,invalidNames,decoded)&&decoded.cards==token);
    }
    auto badCount=bytes;put_word(badCount.data()+12,1025);
    assert(!read_snapshot(3,badCount,decoded)&&decoded.cards==token);
    auto badLength=bytes;put_word(badLength.data()+16,UINT32_MAX);
    assert(!read_snapshot(3,badLength,decoded)&&decoded.cards==token);
    auto neutral=packet({});assert(read_snapshot(3,neutral,decoded)&&decoded.cards&&decoded.cards->packages.empty());
    put_word(neutral.data()+12,UINT32_MAX);
    assert(read_snapshot(3,neutral,decoded)&&!decoded.cards&&decoded.layout[0]==42);
    neutral.erase(neutral.begin()+12,neutral.begin()+16);
    assert(read_snapshot(2,neutral,decoded)&&!decoded.cards&&decoded.layout[0]==42);
    assert(!read_snapshot(1,neutral,decoded)&&!read_snapshot(home_local_config::kVersion+1,neutral,decoded));
    std::puts("PASS native card classification/transactional compaction, priorities, budgets, 10000 queries, v2/v3 selection parsing");
}
