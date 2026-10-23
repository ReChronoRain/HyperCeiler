/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_columns_feature_resolver.h"
#include "fixture_image.h"
#include "columns_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
static auto identify(const Code& c,uint32_t current) {return columns_feature(c,current);}
static auto identify(const Functions& f,uint32_t current) {
    const auto bytes=image(f);const auto c=Code::parse(bytes);assert(c);return identify(*c,current);
}
static Functions duplicate(const Functions& f,uint32_t va) {
    auto out=f;auto clone=*std::find_if(f.begin(),f.end(),[&](const auto& x){return x.va==va;});
    clone.va=0x04000000;
    for(size_t i=0;i<clone.words.size();++i) {
        uint32_t to=0;if(!home_dynamic::target(clone.words[i],va+uint32_t(i*4),to))continue;
        clone.words[i]=0x94000000u|(uint32_t((int64_t(to)-clone.va-i*4)/4)&0x03ffffffu);
    }
    out.push_back(clone);return out;
}
static void expected(const ColumnsFeature& f,const std::map<std::string,uint32_t>& roots) {
    assert(f.owner==roots.at("owner")&&f.copy==roots.at("copy")&&f.persist==roots.at("persist")
        &&f.save==roots.at("save")&&f.put==roots.at("put"));
    assert(f.argument.splice&&f.argument.frame<0&&f.argument.field>0&&f.save_field>0);
}
int main(int argc,char** argv) {
    const auto fixture=columns_fixture();const auto current=columns_roots.at("current");
    auto found=identify(fixture,current);assert(found);expected(*found,columns_roots);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fixture,addresses);
    auto wanted=columns_roots;for(auto& [key,va]:wanted)va=addresses.at(va);
    found=identify(moved,addresses.at(current));assert(found);expected(*found,wanted);
    // Move the clone's complete layout, including its allocated extent, along
    // with the bounds and persisted counts. No fields or class size are inputs.
    unsigned counts=0,clones=0,arguments=0;
    for(auto& f:moved)for(auto& w:f.words) {
        using namespace home_dynamic;const auto role=[&](const char* k){return f.va==wanted.at(k);};
        if(role("copy")) {
            const auto kind=w&0xffe00000u;
            if((rn(w)==1&&(kind==0xf8400000u||kind==0xb8400000u||kind==0xfc400000u))
                ||(rn(w)==0&&(kind==0xf8000000u||kind==0xb8000000u||kind==0xfc000000u))) {
                if(imm9(w)>0){w+=8u<<12;++clones;}
            }
        }
        if(role("context_allocator")&&movz(w,2))w+=0x100u<<5; // decoded size tag: +16 bytes
        if(role("counts")&&ldx(w,0,1)&&(imm9(w)==27||imm9(w)==35)){w+=8u<<12;++counts;}
        if(role("persist")&&ldx(w,0,2)&&(imm9(w)==27||imm9(w)==35)){w+=8u<<12;++counts;}
        if(role("save")&&stx(w,0,2)&&imm9(w)>0)w+=8u<<12;
        if(role("owner")&&((ldw(w,0,2)&&imm9(w)==19)||((w&0xffe00fe0u)==0xb8000000u&&imm9(w)==19))) {
            w+=4u<<12;++arguments;
        }
    }
    assert(counts==4&&clones>=40&&arguments==3);
    found=identify(moved,addresses.at(current));assert(found);expected(*found,wanted);
    assert(found->save_field==15&&found->argument.field==23);
    for(const auto& [role,va]:columns_roots) {
        auto broken=fixture;std::erase_if(broken,[&](const auto& f){return f.va==va;});
        assert(!identify(broken,current));
    }
    for(const auto* role:{"owner","persist","save"})
        assert(!identify(duplicate(fixture,columns_roots.at(role)),current));
    // Each source argument instruction, type/receiver load and named argument
    // descriptor must survive. Matching a function prologue is never identity.
    unsigned mutations=0;
    const auto baseline=identify(fixture,current);assert(baseline);
    for(size_t n=0;n<fixture.size();++n) {
        const auto& f=fixture[n];
        if(f.va==baseline->owner)for(int delta=-4;delta<=6;++delta) {
            auto broken=fixture;broken[n].words[baseline->argument.splice/4+delta]=0xd503201fu;
            assert(!identify(broken,current));++mutations;
        }
        for(size_t i=0;i<f.words.size();++i) {
            uint32_t to=0;const bool bl=home_dynamic::target(f.words[i],f.va+uint32_t(i*4),to);
            if(bl&&(to==baseline->put||to==baseline->copy||to==columns_roots.at("context_allocator"))) {
                auto broken=fixture;broken[n].words[i]=0xd503201fu;
                assert(!identify(broken,current));++mutations;
            }
            if(f.va==baseline->copy&&home_dynamic::stx(f.words[i],0,1)
                &&(home_dynamic::imm9(f.words[i])==27||home_dynamic::imm9(f.words[i])==35)) {
                auto broken=fixture;broken[n].words[i]=0xd503201fu;
                assert(!identify(broken,current));++mutations;
            }
        }
    }
    // Replay interior entry and a forged void return cannot be admitted.
    auto unsafe=fixture;
    for(auto& f:unsafe)if(f.va==baseline->owner) {
        const int64_t to=baseline->argument.splice/4+1;
        f.words[3]=0x14000000u|(uint32_t(to-3)&0x03ffffffu);
    }
    assert(!identify(unsafe,current));
    unsafe=fixture;
    for(auto& f:unsafe)if(f.va==baseline->persist)f.words[f.words.size()-4]=0xaa0103e0u;
    assert(!identify(unsafe,current));
    if(argc>1) {
        std::ifstream in(argv[1],std::ios::binary);assert(in);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        const auto c=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(c);
        found=identify(*c,current);assert(found);expected(*found,columns_roots);
        const auto stripped=image(c->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        found=identify(*anonymous,current);assert(found);expected(*found,columns_roots);
        auto rebased=c->functions();for(auto& f:rebased)f.va+=0x02000000;
        auto translated=columns_roots;for(auto& [key,va]:translated)va+=0x02000000;
        found=identify(rebased,current+0x02000000);assert(found);expected(*found,translated);
        std::cout<<"COLUMNS_FULL_IMAGE PASS: "<<c->functions().size()<<" unnamed candidates, executable-only repack and independent text rebase\n";
    }
    std::cout<<"COLUMNS_FEATURE_ADDRESSING PASS: independent functions/fields/class-size, missing/ambiguous graph, "<<mutations<<" argument/callee/type corruptions and replay branch rejection\n";
}
