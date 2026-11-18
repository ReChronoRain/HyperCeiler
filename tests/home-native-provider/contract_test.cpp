/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_provider_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==7);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_provider::acquire_error(original);
    if(!contract){
        std::cerr<<"Provider error ownership contract declined\n";
        const auto at=original.exported("content_resolver_acquire_unstable_content_provider_client");assert(at);
        const auto public_body=original.body(*at);assert(public_body);
        for(const auto& call:trace(original,*public_body,false,true).calls)if(call.target&&call.output.kind==Value::Self) {
            std::string_view reason;home_native_provider::error_contract(original,*call.target,&reason);
            std::cerr<<"reason "<<reason<<'\n';
        }
        return 1;
    }
    assert(contract->size==40&&contract->vector+32==contract->size);
    std::string_view reply_reason;const auto reply=home_native_provider::resolve(original,&reply_reason);
    if(!reply){std::cerr<<"Provider reply layout declined: "<<reply_reason<<'\n';return 2;}
    assert(reply->accepted!=reply->rejected&&reply->present!=reply->absent);
    auto reference_mutant=files;const auto factory=original.body(reply->create_resolver);assert(factory);
    const auto factory_flow=trace(original,*factory);unsigned references=0;
    for(const auto& load:factory_flow.loads)if(load.address.kind==Value::Self) {
        write(reference_mutant,{factory->address.image,factory->address.va+load.index*4},
            shift_memory(factory->words[load.index],8));++references;
    }
    assert(references==1&&!home_native_provider::resolve(graph(reference_mutant)));
    auto reply_mutant=files;const auto acquire_body=original.body(reply->acquire);assert(acquire_body);
    const auto reply_flow=trace(original,*acquire_body,true,true);unsigned reply_fields=0;
    for(const auto& store:reply_flow.stores)if(store.address.kind==Value::Self
        &&(store.address.offset==reply->pointer||store.address.offset==reply->option)) {
        write(reply_mutant,{acquire_body->address.image,acquire_body->address.va+store.index*4},
            shift_memory(acquire_body->words[store.index],8));++reply_fields;
    }
    assert(reply_fields==2);const auto moved_reply=home_native_provider::resolve(graph(reply_mutant));
    assert(moved_reply&&moved_reply->pointer==reply->pointer+8&&moved_reply->option==reply->option+8
        &&moved_reply->error_at==reply->error_at);
    auto changed=files;const auto ctor=original.body(contract->constructor);assert(ctor);
    const auto flow=trace(original,*ctor,false,true);unsigned moved=0;
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.address.offset==contract->code&&store.width==4) {
        write(changed,{ctor->address.image,ctor->address.va+store.index*4},shift_memory(ctor->words[store.index],4));++moved;
    }
    assert(moved==1);const auto shifted=home_native_provider::acquire_error(graph(changed));
    assert(shifted&&shifted->code==contract->code+4&&shifted->vector==contract->vector);
    changed=files;moved=0;
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.value.kind==Value::Literal) {
        const auto word=ctor->words[store.index];
        const auto narrow=(word&0x3a000000u)==0x28000000u
            ?word&~(1u<<31):(word&~(3u<<30))|(2u<<30);
        write(changed,{ctor->address.image,ctor->address.va+store.index*4},narrow);++moved;
    }
    assert(moved==1&&!home_native_provider::acquire_error(graph(changed)));
    changed=files;const auto drop=original.body(contract->drop);assert(drop);
    const auto drops=trace(original,*drop);moved=0;
    for(const auto& load:drops.loads)if(load.address.kind==Value::Self&&load.address.offset==0) {
        write(changed,{drop->address.image,drop->address.va+load.index*4},shift_memory(drop->words[load.index],8));++moved;
    }
    assert(moved==1&&!home_native_provider::acquire_error(graph(changed)));
    std::cout<<"PASS real Provider error producer, inverse byte-vector destructor move, native Status consumption and relocation ownership\n";
}
