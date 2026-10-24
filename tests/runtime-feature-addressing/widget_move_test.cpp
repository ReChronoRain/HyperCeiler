/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_widget_move_feature_resolver.h"
#include "fixture_image.h"
#include "widget_move_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
static auto identify(const Functions& f) {
    const auto bytes=image(f);const auto c=Code::parse(bytes);assert(c);return widget_move_feature(*c);
}
static void expected(const WidgetMoveFeature& f,const std::map<std::string,uint32_t>& roots) {
    assert(f.owner==roots.at("owner")&&f.span==roots.at("span")&&f.rx==roots.at("rx"));
    assert(f.gate&&f.editing&&f.gate!=f.editing&&f.gate+4<=f.size&&f.editing+4<=f.size);
}
int main(int argc,char** argv) {
    const auto fixture=widget_move_fixture();auto found=identify(fixture);assert(found);expected(*found,widget_move_roots);
    assert(found->flag==251);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fixture,addresses);auto wanted=widget_move_roots;
    for(auto& [key,va]:wanted)va=addresses.at(va);
    found=identify(moved);assert(found);expected(*found,wanted);
    unsigned frames=0,fields=0;
    for(auto& f:moved) {
        using namespace home_dynamic;
        assert(frame_size(f.words));f.words[2]+=8u<<10;
        if(f.va==wanted.at("owner")) {
            // Extra context slot accompanies the +4 move of its fields.
            assert(movz(f.words[5],1));f.words[5]+=1u<<5;
        }
        for(auto& w:f.words) {
            if((w&0x3b200c00u)==0x38000000u&&rn(w)==29&&imm9(w)<0){w-=8u<<12;++frames;}
            if(f.va==wanted.at("owner")&&(ldw(w,2,rd(w))||mem(w,0xb8000000u,2,rd(w)))&&imm9(w)>0) {
                w+=4u<<12;++fields;
            }
            if(f.va==wanted.at("rx")&&ldw(w,1,0)&&imm9(w)>0)w+=4u<<12;
        }
    }
    assert(frames>15&&fields>5);
    found=identify(moved);assert(found);expected(*found,wanted);assert(found->flag==255);
    // No magnitude heuristic may identify the MIUI model flag. Its only role is
    // the branch's relationship to the actual span decision and reject arm.
    auto low_flag=fixture;const auto base=identify(fixture);assert(base);
    for(auto& f:low_flag)if(f.va==base->owner) {
        auto& w=f.words[base->gate/4-2];assert(home_dynamic::imm9(w)==base->flag);
        w=(w&~(511u<<12))|(123u<<12);
    }
    found=identify(low_flag);assert(found&&found->flag==123&&found->gate==base->gate);
    for(const auto& [key,va]:widget_move_roots) {
        auto broken=fixture;std::erase_if(broken,[&](const auto& f){return f.va==va;});assert(!identify(broken));
    }
    auto duplicate=fixture;
    auto clone=*std::find_if(fixture.begin(),fixture.end(),[&](const auto& f){return f.va==base->owner;});
    clone.va=0x04000000;
    for(size_t i=0;i<clone.words.size();++i) {
        uint32_t to=0;if(!home_dynamic::target(clone.words[i],base->owner+uint32_t(i*4),to))continue;
        clone.words[i]=0x94000000u|(uint32_t((int64_t(to)-clone.va-i*4)/4)&0x03ffffffu);
    }
    duplicate.push_back(clone);assert(!identify(duplicate));
    const auto owner=std::find_if(fixture.begin(),fixture.end(),[&](const auto& f){return f.va==base->owner;});
    const auto editing_field=home_dynamic::imm9(owner->words[base->editing/4-2]);
    unsigned mutations=0;
    for(size_t n=0;n<fixture.size();++n) {
        const auto& f=fixture[n];
        for(size_t i=0;i<f.words.size();++i) {
            const auto w=f.words[i];uint32_t to=0;const bool bl=home_dynamic::target(w,f.va+uint32_t(i*4),to);
            bool corrupt=f.va==base->owner&&(i==base->gate/4||i==base->editing/4);
            corrupt|=f.va==base->owner&&bl&&to==base->span&&i+1<f.words.size()
                &&(f.words[i+1]&0xfff8001fu)==0x37200000u;
            corrupt|=f.va==base->owner&&i+2<f.words.size()&&bl&&to==base->rx
                &&home_dynamic::ldx(f.words[i+1],29,2)&&home_dynamic::mem(f.words[i+2],0xb8000000u,2,0)
                &&home_dynamic::imm9(f.words[i+2])==editing_field;
            corrupt|=f.va==base->owner&&i<16&&home_dynamic::stx(w,29,0);
            corrupt|=f.va==base->owner&&home_dynamic::stx(w,29,3)&&i>2
                &&f.words[i-1]==0x8b1c8063u&&home_dynamic::ldw(f.words[i-2],0,3)
                &&i+2<f.words.size()&&home_dynamic::ldw(f.words[i+1],3,1)&&f.words[i+2]==0x8b1c8021u;
            corrupt|=f.va==base->span&&w==0xd27c0020u;
            if(!corrupt)continue;
            auto broken=fixture;broken[n].words[i]=0xd503201fu;assert(!identify(broken));++mutations;
        }
    }
    assert(mutations>=7);
    // A valid but wrong frame slot must not satisfy closure or grid provenance.
    for(bool capture:{false,true}) {
        auto broken=fixture;
        for(auto& f:broken)if(f.va==base->owner) {
            if(capture)for(size_t i=0;i+2<f.words.size();++i) {
                uint32_t to=0;
                if(home_dynamic::target(f.words[i],f.va+uint32_t(i*4),to)&&to==base->rx
                    &&home_dynamic::ldx(f.words[i+1],29,2)&&home_dynamic::mem(f.words[i+2],0xb8000000u,2,0)
                    &&home_dynamic::imm9(f.words[i+2])==editing_field)
                    f.words[i+1]+=8u<<12;
            }
            else {
                const size_t at=base->editing/4+6;
                const int64_t destination=int64_t(at)+(int32_t(f.words[at]<<8)>>13);
                assert(destination>=0&&destination<int64_t(f.words.size()));f.words[destination]+=8u<<12;
            }
        }
        assert(!identify(broken));
    }
    if(argc>1) {
        std::ifstream in(argv[1],std::ios::binary);assert(in);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};
        const auto c=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(c);
        found=widget_move_feature(*c);assert(found);expected(*found,widget_move_roots);
        const auto stripped=image(c->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        found=widget_move_feature(*anonymous);assert(found);expected(*found,widget_move_roots);
        auto rebased=c->functions();for(auto& f:rebased)f.va+=0x02000000;
        auto translated=widget_move_roots;for(auto& [key,va]:translated)va+=0x02000000;
        found=identify(rebased);assert(found);expected(*found,translated);
        std::cout<<"WIDGET_MOVE_FULL_IMAGE PASS: executable-only repack, independently rebased text and actual span/reactive producers\n";
    }
    std::cout<<"WIDGET_MOVE_FEATURE_ADDRESSING PASS: function/frame/field movement, low model flag, missing/ambiguous graph, "<<mutations<<" corrupt producers and wrong frame aliases rejected\n";
}
