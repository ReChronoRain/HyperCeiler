/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_prestart_feature_resolver.h"
#include "targets/home/home_prestart_policy.h"
#include "targets/home/home_recent_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "prestart_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
using namespace home_layout::features::prestart;
static std::optional<Plan> resolve(const Functions& fs){auto bytes=image(fs);auto code=Code::parse(bytes);assert(code);return prestart::resolve(*code);}
int main(int argc,char**argv) {
    const auto fs=prestart_fixture();const auto plan=resolve(fs);assert(plan);
    assert(plan->entry==expected_entry&&plan->initializer==expected_initializer);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fs,addresses);auto p=resolve(moved);assert(p);
    assert(p->entry==addresses.at(plan->entry)&&p->initializer==addresses.at(plan->initializer));
    // Static table/flag fields are decoded, and producer/consumer identity is
    // required. A consistent field relocation passes; a stale producer fails.
    auto fields=fs;
    for(auto& f:fields)for(auto& w:f.words) {
        if(home_dynamic::ux(w,26,0)||home_dynamic::ux(w,26,1))w+=1u<<10;
        if(home_dynamic::uoff(w)==plan->flag&&((w&0xffc003ffu)==0xf9400000u||(w&0xffc003ffu)==0xf9000020u))w+=32u/8<<10;
    }
    p=resolve(fields);assert(p&&p->table==plan->table+8&&p->flag==plan->flag+32);
    for(auto& f:fields)if(f.va==plan->initializer)for(auto& w:f.words)
        if((w&0xffc003ffu)==0xf9000020u&&home_dynamic::uoff(w)==plan->flag+32)w+=8u<<10;
    assert(!resolve(fields));
    // Every member of the graph is necessary; names/sections are absent even
    // in positive fixtures. Same-shape decoys do not permit first-match wins.
    for(size_t i=0;i<fs.size();++i){auto missing=fs;missing.erase(missing.begin()+i);assert(!resolve(missing));}
    auto duplicate=fs;auto root=*std::find_if(fs.begin(),fs.end(),[&](auto& f){return f.va==plan->entry;});
    const auto old=root.va;root.va=0x04000000;
    for(size_t i=0;i<root.words.size();++i){uint32_t to=0;if(home_dynamic::target(root.words[i],old+uint32_t(i*4),to))root.words[i]=0x94000000u|(uint32_t((int64_t(to)-root.va-i*4)/4)&0x03ffffffu);}
    duplicate.push_back(root);assert(!resolve(duplicate));
    auto returning_value=fs;for(auto& f:returning_value)if(f.va==plan->entry)
        for(auto& w:f.words)if(w==0xaa1603e0u){w=0x910082c0u;break;}
    assert(!resolve(returning_value));
    auto frames=fs;for(auto& f:frames){if(!home_dynamic::frame_size(f.words))continue;
        f.words[2]+=8u<<10;
        for(auto& w:f.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<0)
            w=(w&~(511u<<12))|((uint32_t(home_dynamic::imm9(w)-8)&511u)<<12);
    }
    assert(resolve(frames));
    // Exact shipping entry policy: no object or managed-stack reads. Each
    // request observes ready + current authoritative authorization together.
    unsigned cases=0;using home_layout::recent_render::Registers;
    for(bool ready:{false,true})for(bool authorized:{false,true}) {
        Registers r;std::memset(&r,0x5a,sizeof(r));r.x[22]=0x123456781;r.x[30]=0x987654321;
        auto wanted=r;wanted.branch=0xabcdef;const bool blocked=ready&&authorized;
        if(blocked){wanted.x[0]=r.x[22];wanted.branch=0;}
        assert(home_prestart::apply(r,ready,authorized,0xabcdef)==blocked);
        assert(!std::memcmp(&r,&wanted,sizeof(r)));++cases;
    }
    Registers toggled{};toggled.x[22]=0x12341;
    for(bool authorized:{true,false,true,false}) {
        toggled.x[0]=0xbad;assert(home_prestart::apply(toggled,true,authorized,0xabcdef)==authorized);
        assert(toggled.x[0]==(authorized?toggled.x[22]:0xbad)&&toggled.branch==(authorized?0:0xabcdef));++cases;
    }
    if(argc>1){std::ifstream input(argv[1],std::ios::binary);std::vector<char> raw{std::istreambuf_iterator<char>(input),{}};
        auto code=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(code);
        auto full=prestart::resolve(*code);assert(full&&full->entry==plan->entry&&full->initializer==plan->initializer);
    }
    std::cout<<"prestart PASS: linked process/property/FRB graph, relocation, changed fields/frames, missing/ambiguous graph, synchronous-void contract; "<<cases<<" policy cases\n";
}
