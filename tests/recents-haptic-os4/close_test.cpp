/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
static auto identify(const Functions& fs){auto raw=image(fs);auto code=Code::parse(raw);assert(code);return close_haptic::resolve(*code);}
int main(){
    auto fs=fixture();auto p=identify(fs);assert(p);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fs,addresses);auto q=identify(moved);assert(q);
    const auto owner=std::find_if(fs.begin(),fs.end(),[&](const auto& f){return p->commit>=f.va&&p->commit<f.va+f.words.size()*4;});assert(owner!=fs.end());
    assert(q->commit==addresses.at(owner->va)+(p->commit-owner->va));
    assert(q->stock==addresses.at(owner->va)+(p->stock-owner->va));
    assert(q->after_stock==addresses.at(owner->va)+(p->after_stock-owner->va));
    assert(p->stock<p->after_stock&&p->after_stock<p->commit);
    for(size_t i=0;i<fs.size();++i){auto missing=fs;missing.erase(missing.begin()+i);assert(!identify(missing));}
    auto doubled=fs;doubled.insert(doubled.end(),moved.begin(),moved.end());assert(!identify(doubled));
    unsigned corruptions=0;
    for(size_t f=0;f<fs.size();++f)for(size_t i=0;i<fs[f].words.size();++i){
        auto broken=fs;broken[f].words[i]=0xd503201fu;assert(!identify(broken));++corruptions;
    }
    auto fields=fs;
    for(auto& f:fields)for(auto& word:f.words){
        if((word&0xffc003ffu)==0xd10001efu)word+=32u<<10;
        if((word&0x3b200c00u)!=0x38000000u)continue;
        const auto value=home_dynamic::imm9(word)+(home_dynamic::imm9(word)<0?-16:8);
        word=(word&~(511u<<12))|((uint32_t(value)&511)<<12);
    }
    auto changed_fields=identify(fields);assert(changed_fields&&changed_fields->commit==p->commit&&changed_fields->stock==p->stock);
    auto mismatch=fields;auto arm=std::find_if(mismatch.begin(),mismatch.end(),[](const auto& f){return f.words.size()==34;});
    assert(arm!=mismatch.end());arm->words[18]+=4u<<12;assert(!identify(mismatch));
    auto raw=image(fs);auto code=Code::parse(raw);assert(code);
    recent::Plan bank;recent::close_haptic_graph(*code,bank);assert(bank.close_haptic&&bank.sites.size()==2);
    assert(bank.sites[0].va==p->commit&&bank.sites[0].action==recent::Action::CloseHaptic);
    assert(bank.sites[1].va==p->stock&&bank.sites[1].action==recent::Action::CloseStockHaptic);
    for(const auto& site:bank.sites){assert(site.setting==RecentConfig::CloseHaptic);
        const auto i=(site.va-owner->va)/4;assert(recent::straight(owner->words,i));
        for(size_t j=i;j<i+4;++j)assert((owner->words[j]&0xfc000000u)!=0x94000000u);
    }
    recent::Plan collision;collision.sites.push_back(bank.sites[1]);recent::close_haptic_graph(*code,collision);
    assert(collision.sites.size()==1&&!collision.close_haptic);
    /*
     * 8607-style wrapper ambiguity. The impact body is instantiated once per specialization,
     * and every copy's shared-stub tail branch is a long B whose displacement follows the image
     * layout. Masking that displacement makes the shape structurally ambiguous, so resolution
     * must keep working through the owner's call linkage alone: any number of unreferenced
     * copies must not change the plan, while removing the invoked one must still refuse.
     */
    auto impact_fn=std::find_if(fs.begin(),fs.end(),[](const auto& f){return f.words.size()==20;});
    assert(impact_fn!=fs.end());
    const uint32_t impact_va=impact_fn->va;
    auto retarget=[&](Function copy,uint32_t new_va){
        for(size_t i=0;i<copy.words.size();++i){
            const uint32_t at=impact_va+uint32_t(i*4);
            if((copy.words[i]&0xfc000000u)==0x94000000u){
                uint32_t to=0;assert(home_dynamic::target(copy.words[i],at,to));
                const int64_t delta=int64_t(to)-(new_va+int64_t(i*4));
                assert(delta%4==0&&delta>=-(1ll<<27)&&delta<(1ll<<27));
                copy.words[i]=0x94000000u|(uint32_t(delta/4)&0x03ffffffu);
            }
            if((copy.words[i]&0xfc000000u)==0x14000000u){
                const int64_t delta=int64_t(impact_va)-(new_va+int64_t(i*4));
                assert(delta%4==0&&delta>=-(1ll<<27)&&delta<(1ll<<27));
                copy.words[i]=0x14000000u|(uint32_t(delta/4)&0x03ffffffu);
            }
        }
        copy.va=new_va;return copy;
    };
    Functions wrappers=fs;
    wrappers.push_back(retarget(*impact_fn,impact_va+0x400));
    auto ambiguous=identify(wrappers);
    assert(ambiguous&&ambiguous->commit==p->commit&&ambiguous->stock==p->stock&&ambiguous->after_stock==p->after_stock);
    wrappers.push_back(retarget(*impact_fn,impact_va+0x800));
    auto more=identify(wrappers);
    assert(more&&more->commit==p->commit&&more->stock==p->stock&&more->after_stock==p->after_stock);
    auto no_wrapper=fs;no_wrapper.erase(no_wrapper.begin()+(impact_fn-fs.begin()));assert(!identify(no_wrapper));
    std::cout<<"close haptic PASS: "<<corruptions<<" full-CFG corruptions, independent relocation and tagged field/frame changes, shared-field mismatch and missing/ambiguous graph refusal, accepted-only owner, two safe no-BL replay windows, atomic bank admission and "<<(fs.size()+2)<<"-wrapper call-linkage disambiguation\n";
}
