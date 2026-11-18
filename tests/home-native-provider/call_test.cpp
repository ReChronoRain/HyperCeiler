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
    const auto original=graph(files);const auto contract=home_native_provider::resolve_call(original);assert(contract);
    const auto acquire=home_native_provider::resolve(original);assert(acquire);
    assert(contract->pointer!=acquire->pointer); // Equal result sizes, distinct layouts.
    const auto body=original.body(contract->call);assert(body);
    const auto flow=trace(original,*body,true,true);
    auto moved=files;unsigned pointers=0;
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.width==8&&store.address.offset==contract->pointer) {
        write(moved,{body->address.image,body->address.va+store.index*4},shift_memory(body->words[store.index],8));++pointers;
    }
    assert(pointers==1); // Both normal paths merge at the same store.
    const auto shifted=home_native_provider::resolve_call(graph(moved));
    assert(shifted&&shifted->pointer==contract->pointer+8&&shifted->error_at==contract->error_at);
    const std::array<std::pair<unsigned,Value>,1> inputs={{{3,{Value::Self}}}};
    const auto arguments=trace(original,*body,true,false,{},inputs);
    moved=files;unsigned lengths=0;
    for(const auto& load:arguments.loads)if(load.address.kind==Value::Self&&load.address.offset==contract->arg_pointer&&load.width==8) {
        write(moved,{body->address.image,body->address.va+load.index*4},shift_memory(body->words[load.index],8));++lengths;
    }
    assert(lengths==1);const auto moved_args=home_native_provider::resolve_call(graph(moved));
    assert(moved_args&&moved_args->arg_pointer==contract->arg_pointer+8&&moved_args->arg_length==contract->arg_length+8);
    moved=files;unsigned forwarded=0;
    // AAPCS64 argument registers and MOV encoding, never a vendor address.
    for(size_t i=0;i<arguments.calls.front().index;++i)if(body->words[i]==0xaa0403e5u) {
        write(moved,{body->address.image,body->address.va+i*4},0xaa0003e5u);++forwarded;
    }
    assert(forwarded==1&&!home_native_provider::resolve_call(graph(moved)));
    moved=files;unsigned tags=0;
    for(const auto& load:arguments.loads)if(load.address.kind==Value::Self&&load.width==1) {
        write(moved,{body->address.image,body->address.va+load.index*4},shift_memory(body->words[load.index],24));++tags;
    }
    assert(tags==1);const auto moved_tag=home_native_provider::resolve_call(graph(moved));
    assert(moved_tag&&moved_tag->arg_tag==contract->arg_tag+24);
    std::cout<<"PASS actual Provider call result/input forwarding, coherent optional-string/result moves and invalid Bundle forwarding rejection\n";
}
