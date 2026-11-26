/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_shortcut_management.h"
#include "targets/home/home_shortcut_actions.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
using namespace home_layout::features::shortcuts;
static auto identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return shortcuts::resolve(*c);}
static Function& function(Functions& f,uint32_t va){return *std::find_if(f.begin(),f.end(),[&](const auto& x){return x.va==va;});}

static void check_share_apk_click() {
    using namespace home_layout::features::shortcut_actions;
    constexpr uint64_t heap = 8, dart_null = (heap << 32) + 0x101;
    constexpr uint64_t frame = 0x1000, closure = (heap << 32) + 0x201;
    constexpr uint32_t item = 0x301, component = 0x401, package = 0x501;
    const std::map<uintptr_t, uint64_t> fields{
        {frame - 32, closure}, {closure + 0x17, item},
        {(heap << 32) + item + 0x17, component}, {(heap << 32) + component + 0x7, package},
    };
    for (const auto result : {dart_null, dart_null + 0x20, dart_null + 0x30}) {
        for (const auto missing : {uint64_t{0}, frame - 32, closure + 0x17,
                                   (heap << 32) + item + 0x17, (heap << 32) + component + 0x7}) {
            recent_render::Registers saved{};
            saved.x[0] = result;
            saved.x[22] = dart_null;
            saved.x[28] = heap;
            saved.x[29] = frame;
            saved.branch = 0x6000;
            auto expected = saved;
            const bool applied = result == dart_null && missing == 0;
            if (applied) expected.x[0] += 0x20;
            assert(apply_share_apk_click(saved, [&](uintptr_t address, auto& value) {
                const auto entry = fields.find(address);
                if (entry == fields.end()) return false;
                value = static_cast<std::remove_reference_t<decltype(value)>>(
                    address == missing ? uint32_t(dart_null) : entry->second);
                return true;
            }) == applied);
            assert(std::memcmp(&saved, &expected, sizeof(saved)) == 0);
        }
    }
}

static void check_freeform_reject_entry() {
    using namespace home_layout::features::shortcut_actions;
    home_dynamic::Body words(74, 0xd503201f);
    words[56] = 0x372001c1; // Existing device rejection branch to word 70.
    words[69] = 0x37200260; // Getter result branches past the rejection block.
    assert(has_interior_entry(words, 69, 4));
    assert(!has_interior_entry(words, 70, 4));
    words[45] = 0x54000340; // A new conditional edge to word 71.
    assert(has_interior_entry(words, 70, 4));

    constexpr uintptr_t original = 0x1000, continuation = 0x2000;
    for (const bool enabled : {false, true}) {
        for (const uint64_t value : {0u, 0x30u, 0x20u, 0x4000u}) {
            for (const uint64_t device : {0x20u, 0x30u}) {
                recent_render::Registers saved{};
                saved.x[22] = 0x8001;
                saved.x[0] = saved.x[22] + value;
                saved.x[1] = saved.x[22] + device;
                auto expected = saved;
                const bool override = enabled && value == 0x4000 && device == 0x30;
                expected.branch = override ? continuation : original;
                assert(apply_freeform(saved, enabled, original, continuation) == override);
                assert(std::memcmp(&saved, &expected, sizeof(saved)) == 0);
            }
        }
    }
}

int main(){
    check_share_apk_click();
    check_freeform_reject_entry();
    const auto f=fixture();auto p=identify(f);assert(p&&p->entry==expected_entry);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(f,addresses);
    // Keep the combined ambiguity fixture within Code's 64 MiB image budget.
    for(auto& x:moved){const auto old=x.va;x.va-=0x01000000u;
        for(size_t i=0;i<x.words.size();++i){uint32_t to=0;
            if(!home_dynamic::target(x.words[i],old+uint32_t(i*4),to))continue;
            const bool internal=std::any_of(addresses.begin(),addresses.end(),[&](const auto& a){return a.second==to;});
            if(internal)to-=0x01000000u;
            x.words[i]=0x94000000u|(uint32_t((int64_t(to)-x.va-i*4)/4)&0x03ffffffu);
        }
    }
    for(auto& a:addresses)a.second-=0x01000000u;
    auto r=identify(moved);assert(r&&r->entry==addresses.at(p->entry)&&r->loader==addresses.at(p->loader));
    for(const auto& x:f){auto missing=f;std::erase_if(missing,[&](const auto& v){return x.va==v.va;});assert(!identify(missing));}
    auto ambiguous=f;ambiguous.insert(ambiguous.end(),moved.begin(),moved.end());assert(!identify(ambiguous));
    unsigned corruptions=0;
    for(auto va:{p->entry,p->eligibility,p->local,p->global,p->prohibited}){
        for(size_t i=0;i<function(ambiguous,va).words.size();++i){
            auto broken=f;function(broken,va).words[i]=0xd503201fu;assert(!identify(broken));++corruptions;
        }
    }
    // Metadata belongs to the fresh object, not a saved hardcoded field.
    auto fields=f;
    for(auto va:{p->local,p->global,p->prohibited})function(fields,va).words[2]+=4u<<12;
    function(fields,p->entry).words[83]+=4u<<12;assert(identify(fields));
    function(fields,p->entry).words[83]+=4u<<12;assert(!identify(fields));
    auto frames=f;
    // Large async frames also use indexed addressing. Do not manufacture an
    // unencodable LDUR below -256 when moving their boundary spill slots.
    for(auto& x:frames){if(!home_dynamic::frame_size(x.words)||home_dynamic::frame_size(x.words)>=256)continue;
        x.words[2]+=8u<<10;
        for(auto& w:x.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<0)
            w=(w&~(0x1ffu<<12))|(uint32_t(home_dynamic::imm9(w)-8)&0x1ffu)<<12;
    }
    assert(identify(frames));
    auto pools=f;
    for(auto& x:pools)for(auto& w:x.words){
        if((w&0x3b000000u)==0x39000000u)w+=1u<<10;
        if((w&0xffc003e0u)==0x91400360u)w+=1u<<10;
    }
    assert(identify(pools));
    auto wrong_key=f;function(wrong_key,p->global).words[7]=function(wrong_key,p->local).words[7];
    function(wrong_key,p->global).words[8]=function(wrong_key,p->local).words[8];assert(!identify(wrong_key));
    auto wrong_bridge=f;function(wrong_bridge,p->global).words[9]+=1;assert(!identify(wrong_bridge));
    auto wrong_frame=f;function(wrong_frame,p->eligibility).words[19]-=8u<<12;assert(!identify(wrong_frame));
    const auto& loader=function(ambiguous,p->loader);
    for(size_t i=0;i<loader.words.size();++i){uint32_t to=0;
        if(!home_dynamic::target(loader.words[i],loader.va+uint32_t(i*4),to)||to!=p->entry)continue;
        auto bad_branch=f;function(bad_branch,p->loader).words[i+1]+=1u<<5;assert(!identify(bad_branch));
        auto bad_argument=f;function(bad_argument,p->loader).words[i+12]-=8u<<12;assert(!identify(bad_argument));
    }
    auto unknown=f;const uint32_t va=0x04000000;
    unknown.push_back({va,{0xa9bf79fd,0xaa0f03fd,0x94000000u|(uint32_t((int64_t(p->entry)-va-8)/4)&0x3ffffffu),0xd65f03c0}});
    assert(!identify(unknown));
    unsigned register_cases=0;
    for(bool ready:{false,true})for(uintptr_t original:{uintptr_t(0),uintptr_t(0x12345678)}){
        recent_render::Registers s{};for(unsigned i=0;i<32;++i)s.x[i]=0x900000+i*8;
        for(unsigned i=0;i<32;++i){s.q[i][0]=i;s.q[i][1]=~uint64_t(i);}s.fpcr=0x400000;s.fpsr=0x123;
        auto expected=s;const bool applied=ready&&original;expected.branch=applied?0:original;
        if(applied)expected.x[0]=s.x[22]+0x20;
        assert(shortcuts::apply(s,ready,original)==applied);assert(std::memcmp(&s,&expected,sizeof(s))==0);++register_cases;
    }
    std::cout<<"shortcut management PASS: "<<corruptions<<" corruptions, independent relocation, missing/ambiguous/unknown consumers, metadata/key/bridge/frame relationships, "<<register_cases<<" register cases\n";
}
