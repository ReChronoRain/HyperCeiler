/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_bundle_contract.h"
#include "targets/home/home_native_binder_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==10);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto actual=graph(files);
    const auto acquire=home_native_provider::resolve(actual);const auto call=home_native_provider::resolve_call(actual);
    const auto bundle=home_native_bundle::resolve(actual);const auto binder=home_native_binder::resolve(actual);
    assert(acquire&&call&&bundle&&binder&&home_native_bundle::call_owner(actual,*call,*bundle));
    assert(!actual.exported("input_InputEventReceiver_new"));
    auto changed=files;const auto drop=actual.body(bundle->drop);assert(drop);
    write(changed,{drop->address.image,drop->address.va+(drop->words.size()-1)*4},0xd65f03c0u);
    assert(!home_native_bundle::resolve(graph(changed)));
    std::cout<<"PASS minimal nine-image Provider/Bundle/owned Binder endpoint contract, no UI/material/surface SDK dependency\n";
}
