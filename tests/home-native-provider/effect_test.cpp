/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_effect_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==15);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto surface=home_native_surface::resolve(original);assert(surface);
    const auto contract=home_native_surface::resolve_effects(original,*surface);assert(contract);
    const auto declined=[&](const Corpus& changed) {
        const auto g=graph(changed);const auto sc=home_native_surface::resolve(g);assert(sc);
        assert(!home_native_surface::resolve_effects(g,*sc));
    };
    for(const auto at:{contract->blur,contract->corner,contract->matrix}) {
        const auto bridge=original.body(at);assert(bridge);const auto flow=trace(original,*bridge);
        assert(flow.calls.size()==3);
        for(const auto& call:{flow.calls[0],flow.calls[2]}) {
            auto changed=files;write(changed,{at.image,at.va+call.index*4},0xd503201fu);declined(changed);
        }
        auto changed=files;unsigned moved=0;
        for(const auto& store:flow.stores)if(store.value==Value{Value::Parcel,0,0,8}) {
            write(changed,{at.image,at.va+store.index*4},shift_memory(bridge->words[store.index],8));++moved;
        }
        assert(moved==1);declined(changed);
    }
    const auto corner=original.body(contract->corner);assert(corner);unsigned altered=0;
    for(size_t i=0;i<corner->words.size();++i) {
        const auto word=corner->words[i];
        if((word&0xfffffc00u)==0x1e204000u) {
            auto changed=files;write(changed,{corner->address.image,corner->address.va+i*4},word^(1u<<5));
            declined(changed);++altered;
        }
    }
    assert(altered==4);
    const auto matrix=original.body(contract->matrix);assert(matrix);unsigned matrix_arguments=0;
    for(size_t i=0;i<matrix->words.size();++i) {
        const auto word=matrix->words[i];
        if((word&0xfffffc00u)==0x1e204000u) {
            auto changed=files;write(changed,{matrix->address.image,matrix->address.va+i*4},word^(1u<<5));
            declined(changed);++matrix_arguments;
        }
    }
    assert(matrix_arguments==8);
    const auto blur=original.body(contract->blur);assert(blur);altered=0;
    for(size_t i=0;i<blur->words.size();++i) {
        const auto word=blur->words[i];
        if((word&0x7fe0ffe0u)==0x2a0003e0u&&(word&31)==2) {
            auto changed=files;write(changed,{blur->address.image,blur->address.va+i*4},(word&~(31u<<16))|(31u<<16));
            declined(changed);++altered;
        }
    }
    assert(altered==1);
    std::cout<<"PASS SDK-derived native blur/corner/matrix bridges, integer, two-float and four-float AAPCS forwarding, transparent borrowing and paired native ownership; mutated arguments/retains refused\n";
}
