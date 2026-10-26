/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_grid_feature_resolver.h"
#include "fixture_image.h"
#include "grid_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
static auto identify(const Code& c) {
    std::map<std::string,uint32_t> found;
    if(auto g=grid_geometry_feature(c)) {
        found={{"current",g->current},{"rx",g->rx},{"cell",g->cell},{"occupied",g->occupied},
               {"drop",g->drop},{"counts",g->counts},{"hotseat",g->hotseat}};
        assert(g->fields.usable());
        for(size_t i=0;i<3;++i)if(i<2||g->hotseat) {
            assert(g->frames[i].usable(i==2));assert(g->patches[i]+16<=g->sizes[i]);
        }
    }
    if(auto a=grid_autofit_feature(c)) {
        found["legacy"]=a->legacy;found["cap"]=a->cap;found["handler"]=a->handler;
    }
    return found;
}
static auto identify(const Functions& f) {
    const auto bytes=image(f);const auto c=Code::parse(bytes);assert(c);return identify(*c);
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
int main(int argc,char** argv) {
    const auto fixture=grid_fixture();auto expected=grid_roots;
    for(const auto* evidence:{"local","transform","offset_allocator"})expected.erase(evidence);
    assert(identify(fixture)==expected);
    std::map<uint32_t,uint32_t> moved_addresses;auto moved=relocated(fixture,moved_addresses);
    auto wanted=expected;for(auto& [key,va]:wanted)va=moved_addresses.at(va);
    assert(identify(moved)==wanted);
    // Same numerical centre arithmetic in cellToCenterOffset must never become
    // a substitute for the global-coordinate return after that owner disappears.
    for(const auto* missing:{"current","rx","cell","occupied","drop","counts","transform","offset_allocator"}) {
        auto broken=fixture;const auto va=grid_roots.at(missing);
        std::erase_if(broken,[&](const auto& f){return f.va==va;});
        assert(!identify(broken).count("drop"));assert(identify(broken).count("cap"));
    }
    for(const auto* missing:{"legacy","cap","handler"}) {
        auto broken=fixture;const auto va=grid_roots.at(missing);
        std::erase_if(broken,[&](const auto& f){return f.va==va;});
        assert(!identify(broken).count("cap"));assert(identify(broken).count("drop"));
    }
    for(const auto* ambiguous:{"cell","occupied","drop","legacy","handler"}) {
        const auto broken=identify(duplicate(fixture,grid_roots.at(ambiguous)));
        assert(!broken.count(std::string(ambiguous)=="legacy"||std::string(ambiguous)=="handler"?"cap":"drop"));
    }
    // An ambiguous optional Dock delegate disables Dock's bank alone.
    const auto ambiguous_dock=identify(duplicate(fixture,grid_roots.at("hotseat")));
    assert(ambiguous_dock.count("drop")&&ambiguous_dock.at("hotseat")==0);
    // Config dimensions, origin, item indices and bounds all move consistently.
    for(auto& f:moved)for(auto& w:f.words) {
        using namespace home_dynamic;
        const auto role=[&](const char* key){return f.va==moved_addresses.at(grid_roots.at(key));};
        if((role("cell")&&rn(w)==3)||(role("occupied")&&rn(w)==0)||(role("drop")&&rn(w)==0))
            if((ldd(w,rn(w),0)||ldd(w,rn(w),1))&&(imm9(w)==43||imm9(w)==51))w+=8u<<12;
        if(role("drop")&&ldw(w,0,1)&&imm9(w)==59)w+=8u<<12;
        if(role("counts")&&(ldx(w,0,1))&&(imm9(w)==27||imm9(w)==35))w+=8u<<12;
        if(role("occupied")&&ldx(w,0,2)&&(imm9(w)==55||imm9(w)==63))w+=8u<<12;
        if(role("hotseat")&&ldx(w,1,4)&&imm9(w)==55)w+=8u<<12;
    }
    assert(identify(moved)==wanted);
    // Corrupt each half of the global transform chain, the Offset producer and
    // matrix return type. No local-arithmetic fallback may then take over.
    unsigned corruptions=0;
    for(const auto* owner:{"drop","transform"})for(size_t n=0;n<fixture.size();++n) {
        const auto& f=fixture[n];if(f.va!=grid_roots.at(owner))continue;
        for(size_t i=0;i<f.words.size();++i) {
            uint32_t to=0;
            if(!home_dynamic::target(f.words[i],f.va+uint32_t(i*4),to)
                ||(to!=grid_roots.at("transform")&&to!=grid_roots.at("offset_allocator")))continue;
            auto broken=fixture;broken[n].words[i]=0xd503201fu;
            // The last optional Offset allocation is downstream of the proved
            // global chain and isn't the patch's identity.
            if(to==grid_roots.at("offset_allocator")&&std::string(owner)=="drop"&&i>0x550/4)continue;
            assert(!identify(broken).count("drop"));++corruptions;
        }
    }
    if(argc>1) {
        std::ifstream in(argv[1],std::ios::binary);assert(in);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        const auto c=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(c);
        assert(identify(*c)==expected);
        const auto stripped=image(c->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        assert(identify(*anonymous)==expected);
        auto rebased=c->functions();for(auto& f:rebased)f.va+=0x02000000;
        auto translated=expected;for(auto& [key,va]:translated)va+=0x02000000;
        assert(identify(rebased)==translated);
        std::cout<<"GRID_FULL_IMAGE PASS: "<<c->functions().size()<<" candidates, executable-only repack and independent text rebase\n";
    }
    std::cout<<"GRID_FEATURE_ADDRESSING PASS: independent function/field moves, local-coordinate decoy, missing/ambiguous links and "<<corruptions<<" global graph corruptions\n";
}
