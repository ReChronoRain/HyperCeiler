/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_indicator_feature_resolver.h"
#include "fixture_image.h"
#include "indicator_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
static auto identify(const Code& c) {
    std::map<std::string,uint32_t> out;
    if(auto m=indicator_margin_feature(c)) {
        out["margin"]=m->owner;out["margin_bank"]=m->owner+m->patch;out["margin_cid"]=m->layout.cid;
        out["deflate"]=m->deflate;out["container_constructor"]=m->constructor;
        out["insets_allocator"]=m->insets_allocator;out["container_allocator"]=m->container_allocator;
    }
    if(auto p=indicator_policy_feature(c)) {
        out["policy"]=p->owner;out["gate"]=p->owner+p->gate;out["result"]=p->owner+p->result;
        out["editing"]=p->editing;out["empty_pool"]=p->empty_pool;
    }
    if(auto s=indicator_slide_feature(c)) {
        out["show"]=s->owner;out["patch"]=s->owner+s->patch;out["idle"]=s->idle_caller;
        out["animate"]=s->animate;out["refresh"]=s->refresh;out["type"]=s->type;
    }
    return out;
}
static auto identify(const Functions& f) {
    const auto bytes=image(f);const auto c=Code::parse(bytes);assert(c);return identify(*c);
}
static Functions duplicate(const Functions& input,uint32_t va) {
    auto f=input;auto clone=*std::find_if(f.begin(),f.end(),[&](const auto& x){return x.va==va;});
    clone.va=0x04000000;
    for(size_t i=0;i<clone.words.size();++i) {
        uint32_t to=0;if(!home_dynamic::target(clone.words[i],va+uint32_t(i*4),to))continue;
        clone.words[i]=0x94000000u|(uint32_t((int64_t(to)-clone.va-i*4)/4)&0x03ffffffu);
    }
    f.push_back(clone);return f;
}
int main(int argc,char**argv) {
    const auto fixture=indicator_fixture();const auto found=identify(fixture);
    for(const auto* key:{"margin","deflate","container_constructor","insets_allocator","container_allocator","policy","editing","show","animate","refresh","type"})assert(found.at(key)==indicator_roots.at(key));
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fixture,addresses);auto wanted=found;
    for(auto& [key,va]:wanted)if(key!="empty_pool"&&key!="margin_cid") {
        const auto owner=std::find_if(fixture.begin(),fixture.end(),[&](const auto& f){return va>=f.va&&va<f.va+f.words.size()*4;});
        assert(owner!=fixture.end());va=addresses.at(owner->va)+va-owner->va;
    }
    assert(identify(moved)==wanted);
    unsigned locals=0,fields=0;
    for(auto& f:moved) {
        using namespace home_dynamic;
        if(f.va==addresses.at(indicator_roots.at("insets_allocator"))) {
            f.words[0]+=0x1000u<<5;++wanted["margin_cid"];
        }
        if(!frame_size(f.words))continue;
        f.words[2]+=8u<<10;
        for(auto& w:f.words) {
            if((w&0x3b200c00u)==0x38000000u&&rn(w)==29&&imm9(w)<0) {w-=8u<<12;++locals;}
            if(f.va==addresses.at(indicator_roots.at("show"))&&ldx(w,0,2)&&imm9(w)==27){w+=8u<<12;++fields;}
            if(f.va==addresses.at(indicator_roots.at("animate"))&&stx(w,3,2)&&imm9(w)==27){w+=8u<<12;++fields;}
        }
        if(f.va==addresses.at(indicator_roots.at("deflate")))
            for(auto& w:f.words)if(home_dynamic::ldd(w,1,1))w+=8u<<12;
        if(f.va==addresses.at(indicator_roots.at("margin"))) {
            const size_t root=(found.at("margin_bank")-found.at("margin"))/4-6;
            for(size_t j:{root+1,root+2,root+3,root+5})f.words[j]+=8u<<12;
        }
        if(f.va==addresses.at(indicator_roots.at("policy"))) {
            const size_t empty=(found.at("result")-found.at("policy"))/4+3;
            f.words[empty]+=1u<<10;wanted["empty_pool"]+=8;
        }
    }
    assert(locals>30&&fields==2&&identify(moved)==wanted);
    // Execute the shipping fresh-Inset mutation using both independently
    // resolved layouts. The scalar frame stays intact; zero/invalid cases do
    // not write, and the class guard comes from the current allocation tag.
    for(const Functions* functions:std::array<const Functions*,2>{&fixture,&moved}) {
        const auto bytes=image(*functions);const auto c=Code::parse(bytes);assert(c);
        const auto margin=indicator_margin_feature(*c);assert(margin);
        alignas(8) std::array<std::byte,256> object{};
        alignas(8) std::array<std::byte,1024> frame{};
        const auto edge=reinterpret_cast<uintptr_t>(object.data())+1;
        const auto fp=reinterpret_cast<uintptr_t>(frame.data())+512;
        const uint64_t header=uint64_t(margin->layout.cid)<<12;
        std::memcpy(object.data(),&header,8);
        const double bottom=36;
        std::memcpy(reinterpret_cast<void*>(edge+margin->layout.field[3]),&bottom,8);
        std::memcpy(reinterpret_cast<void*>(fp+margin->layout.source),&bottom,8);
        const auto before=object;const auto frame_before=frame;
        assert(!hc::indicator::inset_original_frame(fp,edge>>32,edge,0,0,margin->layout)&&object==before);
        assert(hc::indicator::inset_original_frame(fp,edge>>32,edge,0,-24,margin->layout));
        double top=0,after_bottom=0;
        std::memcpy(&top,reinterpret_cast<const void*>(edge+margin->layout.field[1]),8);
        std::memcpy(&after_bottom,reinterpret_cast<const void*>(edge+margin->layout.field[3]),8);
        assert(top==-24&&after_bottom==60&&frame==frame_before);
        object=before;const uint64_t wrong=uint64_t(margin->layout.cid+1)<<12;std::memcpy(object.data(),&wrong,8);
        const auto bad=object;
        assert(!hc::indicator::inset_original_frame(fp,edge>>32,edge,0,-24,margin->layout)&&object==bad);
    }
    for(const auto* key:{"margin","deflate","container_constructor","insets_allocator","container_allocator"}) {
        auto broken=fixture;const auto va=indicator_roots.at(key);
        std::erase_if(broken,[&](const auto& f){return f.va==va;});
        const auto result=identify(broken);
        assert(!result.count("margin")&&result.count("policy")&&result.count("show"));
    }
    for(const auto* key:{"margin","deflate"})
        assert(!identify(duplicate(fixture,indicator_roots.at(key))).count("margin"));
    // Missing one relation disables its own family; the same four-instruction
    // show shape in the real decoy may never stand in for the missing owner.
    for(const auto* key:{"policy","editing","comparison","show","animate","refresh","type"}) {
        auto broken=fixture;const auto va=indicator_roots.at(key);
        std::erase_if(broken,[&](const auto& f){return f.va==va;});
        const auto result=identify(broken);
        const bool policy=std::string(key)=="policy"||std::string(key)=="editing"||std::string(key)=="comparison";
        assert(!result.count(policy?"policy":"show"));assert(result.count(policy?"show":"policy"));
    }
    assert(!identify(duplicate(fixture,indicator_roots.at("policy"))).count("policy"));
    assert(!identify(duplicate(fixture,indicator_roots.at("refresh"))).count("show"));
    unsigned corruptions=0;
    for(size_t n=0;n<fixture.size();++n) {
        const auto& f=fixture[n];
        for(size_t i=0;i<f.words.size();++i) {
            const auto w=f.words[i];uint32_t to=0;
            const bool bl=home_dynamic::target(w,f.va+uint32_t(i*4),to);
            bool mutate=bl&&(to==indicator_roots.at("editing")||to==indicator_roots.at("animate")||to==indicator_roots.at("show")||to==indicator_roots.at("type"));
            mutate|=f.va==indicator_roots.at("comparison")&&w==0x9a911202u;
            mutate|=f.va==indicator_roots.at("animate")&&home_dynamic::stx(w,3,2)&&home_dynamic::imm9(w)==27;
            if(!mutate)continue;
            auto broken=fixture;broken[n].words[i]=0xd503201fu;
            const auto result=identify(broken);
            assert(!result.count(f.va==indicator_roots.at("policy")||f.va==indicator_roots.at("comparison")?"policy":"show"));++corruptions;
        }
    }
    for(const auto* key:{"margin","policy","show"}) {
        auto broken=fixture;
        for(auto& f:broken)if(f.va==indicator_roots.at(key)) {
            const auto destination=(found.at(std::string(key)=="margin"?"margin_bank":std::string(key)=="policy"?"gate":"patch")-f.va)/4+1;
            f.words[3]=0x14000000u|(uint32_t(int64_t(destination)-3)&0x03ffffffu);
        }
        assert(!identify(broken).count(key));
    }
    unsigned margins=0;
    for(size_t n=0;n<fixture.size();++n)if(fixture[n].va==indicator_roots.at("margin")) {
        const size_t root=(found.at("margin_bank")-found.at("margin"))/4-6;
        std::set<size_t> required{root-1,root+1,root+2,root+3,root+5};
        for(size_t j=root+7;j+7<fixture[n].words.size();++j) {
            const auto& b=fixture[n].words;uint32_t to=0;
            if(home_dynamic::ldx(b[j],29,30)&&home_dynamic::imm9(b[j])==home_dynamic::imm9(b[root])
                &&b[j+1]==0xa900c1feu&&home_dynamic::target(b[j+7],fixture[n].va+uint32_t((j+7)*4),to)
                &&to==indicator_roots.at("container_constructor")) {required.insert(j-4);required.insert(j+7);}
        }
        assert(required.size()==7);
        for(auto i:required) {
            auto broken=fixture;broken[n].words[i]=0xd503201fu;
            assert(!identify(broken).count("margin"));++margins;
        }
    }
    assert(margins>=7);
    auto wrong_type=fixture;
    for(auto& f:wrong_type)if(f.va==indicator_roots.at("type"))
        for(auto& w:f.words)if(home_dynamic::movz(w,0)&&home_dynamic::zimm(w)==2)w+=7u<<5;
    assert(!identify(wrong_type).count("show"));
    if(argc>1) {
        std::ifstream in(argv[1],std::ios::binary);assert(in);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        const auto c=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(c);
        assert(identify(*c)==found);
        const auto stripped=image(c->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        assert(identify(*anonymous)==found);
        auto rebased=c->functions();for(auto& f:rebased)f.va+=0x02000000;
        auto translated=found;for(auto& [key,va]:translated)if(key!="empty_pool"&&key!="margin_cid")va+=0x02000000;
        assert(identify(rebased)==translated);
        std::cout<<"INDICATOR_FULL_IMAGE PASS: anonymous policy owner, executable-only repack, independent text rebase and real same-shape show decoy\n";
    }
    std::cout<<"INDICATOR_FEATURE_ADDRESSING PASS: function/frame/field/pool movement, missing/ambiguous calls, "<<corruptions+margins<<" graph corruptions, return types, fresh Insets replay and entry rejection\n";
}
