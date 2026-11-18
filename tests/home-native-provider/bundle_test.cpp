/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_bundle_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==8);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto bundle=home_native_bundle::resolve(original);
    const auto call=home_native_provider::resolve_call(original);
    assert(bundle&&call&&home_native_bundle::call_owner(original,*call,*bundle));
    auto moved=files;const auto getter=original.body(bundle->get_boolean);assert(getter);
    unsigned packing=0;
    for(size_t i=0;i<getter->words.size();++i)if((getter->words[i]&0xff20001fu)==0xaa000000u) {
        const auto word=getter->words[i];const auto shift=(word>>10)&63;
        write(moved,{getter->address.image,getter->address.va+i*4},(word&~(63u<<10))|((shift+1)<<10));++packing;
    }
    assert(packing==1);const auto shifted=home_native_bundle::resolve(graph(moved));
    assert(shifted&&shifted->boolean_shift==bundle->boolean_shift+1);
    moved=files;const auto drop=original.body(bundle->drop);assert(drop);unsigned changed=0;
    for(size_t i=0;i<drop->words.size();++i) {
        const auto w=drop->words[i];
        if((w&0xffe0001fu)==0x52800001u&&((w>>5)&65535)==bundle->bytes) {
            write(moved,{drop->address.image,drop->address.va+i*4},(w&~(65535u<<5))|((bundle->bytes+8)<<5));++changed;
        }
    }
    assert(changed==1&&!home_native_bundle::resolve(graph(moved)));
    auto incompatible=*bundle;incompatible.bytes+=8;
    assert(!home_native_bundle::call_owner(original,*call,incompatible));
    moved=files;changed=0;
    for(size_t i=0;i<getter->words.size();++i)if((getter->words[i]&0xffffffe0u)==0x1a9f17e0u) {
        write(moved,{getter->address.image,getter->address.va+i*4},getter->words[i]^(1u<<12));++changed;
    }
    assert(changed==1&&!home_native_bundle::resolve(graph(moved)));
    std::cout<<"PASS SDK Bundle constructor/drop and Provider result ownership, derived boolean packing, size/condition mismatch refusal\n";
}
