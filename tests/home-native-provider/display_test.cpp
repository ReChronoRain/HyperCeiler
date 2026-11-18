/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_display_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==14);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_display::resolve(original);assert(contract);
    const auto density=original.body(contract->density);assert(density);const auto getter=trace(original,*density);
    auto moved=files;unsigned changed=0;
    for(const auto& load:getter.loads)if(load.address.kind==Value::Self) {
        write(moved,{density->address.image,density->address.va+load.index*4},density->words[load.index]^(1u<<30));++changed;
    }
    assert(changed==1&&!home_native_display::resolve(graph(moved)));
    moved=files;changed=0;const auto width=original.body(contract->width);assert(width);
    for(const auto& load:trace(original,*width).loads)if(load.address.kind==Value::Self) {
        write(moved,{width->address.image,width->address.va+load.index*4},shift_memory(width->words[load.index],contract->bytes));++changed;
    }
    assert(changed==1&&!home_native_display::resolve(graph(moved)));
    moved=files;const auto drop=original.body(contract->drop_metrics);assert(drop);changed=0;
    for(size_t i=0;i<drop->words.size();++i)if((drop->words[i]&0xffe0001fu)==0x52800001u) {
        write(moved,{drop->address.image,drop->address.va+i*4},drop->words[i]^(1u<<5));++changed;
    }
    assert(changed==1&&!home_native_display::resolve(graph(moved)));
    moved=files;changed=0;const auto metrics=original.body(contract->metrics);assert(metrics);
    for(size_t i=0;i<metrics->words.size();++i)if(metrics->words[i]==0x2a1f03e1u) {
        write(moved,{metrics->address.image,metrics->address.va+i*4},0xd503201fu);++changed;
    }
    assert(changed==1&&!home_native_display::resolve(graph(moved)));
    std::cout<<"PASS actual Context configuration clone/owned constructor and Drop, complete configuration/default-display metrics input, scalar consumers and invalid contracts refused\n";
}
