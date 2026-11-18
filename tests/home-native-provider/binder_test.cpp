/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_binder_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==15);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_binder::resolve(original);assert(contract);
    const auto api=original.body(contract->get);assert(api);const auto getter=trace(original,*api);
    assert(getter.calls.size()==4&&getter.calls[3].target);
    const auto optional=original.body(*getter.calls[3].target);assert(optional);const auto cloning=trace(original,*optional);
    const auto copier=original.callee(*optional,cloning.branches.back(),true);assert(copier);
    const auto strong=original.body(*copier);assert(strong);const auto retain=trace(original,*strong);
    auto changed=files;write(changed,{strong->address.image,strong->address.va+retain.calls[0].index*4},0xd503201fu);
    assert(!home_native_binder::resolve(graph(changed)));
    changed=files;unsigned moved=0;
    for(const auto& load:retain.loads)if(load.address.kind==Value::Self) {
        write(changed,{strong->address.image,strong->address.va+load.index*4},shift_memory(strong->words[load.index],8));++moved;
    }
    assert(moved==1&&!home_native_binder::resolve(graph(changed)));
    std::cout<<"PASS SDK Bundle native Binder getter, explicit NDK retain/transparent owned pointer and incompatible ownership refused\n";
}
