/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_scalar_feature_resolver.h"
#include "fixture_image.h"
#include "scalar_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
static auto identify(const Code& code,uint32_t current) {
    std::map<std::string,uint32_t> out;
    for(bool margin:{false,true})if(auto f=layout_scalar_feature(code,current,margin)) {
        const std::string role=margin?"margin":"row";
        out[role]=f->source.target;out[role+"_field"]=f->source.field;out[role+"_payload"]=f->source.payload;
        assert(f->size>=16);
    }
    return out;
}
static auto identify(const Functions& f,uint32_t current) {
    const auto bytes=image(f);const auto c=Code::parse(bytes);assert(c);return identify(*c,current);
}
int main(int argc,char** argv) {
    const auto fixture=scalar_fixture();const uint32_t current=scalar_roots.at("current");
    const std::map<std::string,uint32_t> expected{{"margin",scalar_roots.at("margin")},{"row",scalar_roots.at("row")},
        {"margin_field",59},{"margin_payload",31},{"row_field",115},{"row_payload",7}};
    assert(identify(fixture,current)==expected);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fixture,addresses);auto wanted=expected;
    wanted["margin"]=addresses.at(wanted["margin"]);wanted["row"]=addresses.at(wanted["row"]);
    assert(identify(moved,addresses.at(current))==wanted);
    // Independent fields/frame locals are outputs of the current executable.
    for(auto& f:moved)for(auto& w:f.words) {
        if(f.va==addresses.at(scalar_roots.at("margin"))||f.va==addresses.at(scalar_roots.at("row"))) {
            if(home_dynamic::ldw(w,0,1)||home_dynamic::ldd(w,1,0))w+=8u<<12;
        }
    }
    for(const auto* key:{"margin_field","margin_payload","row_field","row_payload"})wanted[key]+=8;
    assert(identify(moved,addresses.at(current))==wanted);
    for(const auto* key:{"current","margin","margin_consumer","screen","screen_helper","row","row_consumer","delegate_allocator"}) {
        auto broken=fixture;const auto va=scalar_roots.at(key);
        std::erase_if(broken,[&](const auto& f){return f.va==va;});
        const auto found=identify(broken,current);
        if(std::string(key)=="current")assert(found.empty());
        else assert(!found.count(std::string(key).find("row")!=std::string::npos||std::string(key)=="delegate_allocator"?"row":"margin"));
    }
    for(const std::string role:{"margin","row"}) {
        auto broken=fixture;std::map<uint32_t,uint32_t> clones;
        for(const auto& key:{role,role+"_consumer"})clones[scalar_roots.at(key)]=scalar_roots.at(key)+0x02000000;
        for(const auto& f:fixture)if(clones.count(f.va)) {
            auto clone=f;clone.va=clones.at(f.va);
            for(size_t i=0;i<f.words.size();++i) {
                uint32_t to=0;if(!home_dynamic::target(f.words[i],f.va+uint32_t(i*4),to))continue;
                clone.words[i]=0x94000000u|(uint32_t((int64_t(relocate(to,clones))-clone.va-i*4)/4)&0x03ffffffu);
            }
            broken.push_back(clone);
        }
        assert(!identify(broken,current).count(role));
    }
    // An object-returning screen helper cannot be used as an unboxed margin.
    auto object=fixture;
    for(auto& f:object)if(f.va==scalar_roots.at("screen_helper"))
        for(auto& w:f.words)if(w==0x4ea11c20u)w=0xaa1603e0u;
    assert(!identify(object,current).count("margin"));
    // A division in a caller is insufficient: corrupt every fresh delegate
    // geometry store one at a time, including the actual aspect result.
    unsigned mutations=0;
    for(size_t n=0;n<fixture.size();++n)if(fixture[n].va==scalar_roots.at("row_consumer")) {
        for(size_t i=0;i<fixture[n].words.size();++i) {
            const auto w=fixture[n].words[i];
            if(!home_dynamic::std_(w,1,0))continue;
            auto broken=fixture;broken[n].words[i]=0xd503201fu;
            assert(!identify(broken,current).count("row"));++mutations;
        }
    }
    assert(mutations==3);
    if(argc>1) {
        std::ifstream in(argv[1],std::ios::binary);assert(in);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        const auto c=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(c);
        assert(identify(*c,current)==expected);
        const auto stripped=image(c->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        assert(identify(*anonymous,current)==expected);
        auto rebased=c->functions();for(auto& f:rebased)f.va+=0x02000000;
        auto translated=expected;translated["margin"]+=0x02000000;translated["row"]+=0x02000000;
        assert(identify(rebased,current+0x02000000)==translated);
        std::cout<<"SCALAR_FULL_IMAGE PASS: executable-only repack and independently rebased text\n";
    }
    std::cout<<"SCALAR_FEATURE_ADDRESSING PASS: relocation, changed fields, missing/ambiguous graphs, wrong return type and corrupt fresh delegate stores\n";
}
