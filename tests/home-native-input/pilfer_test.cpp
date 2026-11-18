/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_input_pilfer.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==9);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> data((std::istreambuf_iterator<char>(stream)),{});
        assert(!data.empty());Bytes bytes(data.size());std::memcpy(bytes.data(),data.data(),data.size());files.push_back(std::move(bytes));
    }
    const auto original=graph(files);const auto contract=home_native_input::pilfer_contract(original);assert(contract);
    const auto body=original.body(contract->implementation);assert(body);
    unsigned field_changes=0,slot_changes=0,call_damage=0,branch_damage=0;
    for(size_t at=0;at<body->words.size();++at) {
        const auto word=body->words[at];
        if((word&0xffc00000u)==0xf9400000u) {
            const auto offset=((word>>10)&4095)*8;
            if(offset==contract->data) {
                auto changed=files;write(changed,{body->address.image,body->address.va+at*4},shift_memory(word,-8));
                auto next=home_native_input::pilfer_contract(graph(changed));assert(next&&next->data==contract->data-8);++field_changes;
            }
            if(offset==contract->method) {
                auto changed=files;write(changed,{body->address.image,body->address.va+at*4},shift_memory(word,8));
                auto next=home_native_input::pilfer_contract(graph(changed));assert(next&&next->method==contract->method+8);++slot_changes;
            }
        }
        if((word&0xfffffc1fu)==0xd63f0000u) {
            auto changed=files;write(changed,{body->address.image,body->address.va+at*4},0xd503201fu);
            assert(!home_native_input::pilfer_contract(graph(changed)));++call_damage;
            changed=files;write(changed,{body->address.image,body->address.va+(at+1)*4},body->words[at+1]^(1u<<24));
            assert(!home_native_input::pilfer_contract(graph(changed)));++branch_damage;
        }
    }
    assert(field_changes==1&&slot_changes==1&&call_damage==1&&branch_damage==1);
    const auto drop=original.body(contract->status_drop);assert(drop);unsigned drop_damage=0;
    for(size_t at=0;at<drop->words.size();++at)if((drop->words[at]&0xffc003ffu)==0xf9400000u) {
        auto changed=files;write(changed,{drop->address.image,drop->address.va+at*4},shift_memory(drop->words[at],8));
        assert(!home_native_input::pilfer_contract(graph(changed)));++drop_damage;
    }
    assert(drop_damage==1);
    auto changed=files;write(changed,{drop->address.image,drop->address.va+(drop->words.size()-1)*4},0xd65f03c0u);
    assert(!home_native_input::pilfer_contract(graph(changed)));
    std::cout<<"PASS real SDK nullable pilfer result, monitor forwarding, virtual slot and NDK status ownership; no pilfer invocation\n";
}
