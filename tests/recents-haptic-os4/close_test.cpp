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
    std::cout<<"close haptic PASS: "<<corruptions<<" full-CFG corruptions, independent relocation and tagged field/frame changes, shared-field mismatch and missing/ambiguous graph refusal, accepted-only owner, two safe no-BL replay windows and atomic bank admission\n";
}
