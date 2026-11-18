/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_integer_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==15);Corpus files;
    for(int i=1;i<argc;++i){std::ifstream in(argv[i],std::ios::binary);std::vector<char> data((std::istreambuf_iterator<char>(in)),{});assert(!data.empty());Bytes bytes(data.size());std::memcpy(bytes.data(),data.data(),data.size());files.push_back(std::move(bytes));}
    const auto original=graph(files);const auto contract=home_native_bundle::resolve_integer(original);assert(contract&&contract->shift==32);
    const auto body=original.body(contract->get);assert(body);unsigned changed_loads=0,changed_packs=0,changed_none=0;
    for(size_t i=0;i<body->words.size();++i) {
        const auto word=body->words[i];auto changed=files;
        if((word&0xffffffe0u)==0xb9400000u){write(changed,{body->address.image,body->address.va+i*4},word|(1u<<30));++changed_loads;}
        else if((word&0xffc0001fu)==0xd3400000u){write(changed,{body->address.image,body->address.va+i*4},word^(1u<<10));++changed_packs;}
        else if(word==0x52800020u){write(changed,{body->address.image,body->address.va+i*4},0x52800040u);++changed_none;}
        else continue;
        assert(!home_native_bundle::resolve_integer(graph(changed)));
    }
    assert(changed_loads==1&&changed_packs==1&&changed_none==1);
    std::cout<<"PASS optional SDK integer getter full CFG, borrowed scalar width, derived packing and None ABI; mutated layouts refused\n";
}
