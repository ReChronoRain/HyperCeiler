/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_parcel_contract.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==9);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto bundle=home_native_bundle::resolve(original);assert(bundle);
    const auto contract=home_native_parcel::resolve(original,*bundle);assert(contract&&contract->has_intent_trailer);
    const auto api=original.body(contract->bundle_from);assert(api);
    const auto entry=trace(original,*api);assert(!entry.calls.empty()&&entry.calls[0].target);
    const auto decoder=original.body(*entry.calls[0].target);assert(decoder);
    const auto inner=trace(original,*decoder);assert(!inner.calls.empty()&&inner.calls[0].target);
    const auto primitive=original.body(*inner.calls[0].target);assert(primitive);
    const auto integer=trace(original,*primitive);assert(!integer.calls.empty());
    auto moved=files;unsigned changed=0;
    for(const auto& load:integer.loads)if(load.address.kind==Value::Self&&load.width==8&&load.address.offset==contract->raw_field) {
        write(moved,{primitive->address.image,primitive->address.va+load.index*4},shift_memory(primitive->words[load.index],8));++changed;
    }
    // The supported public input is &BorrowedParcel (one pointer), not an
    // arbitrary private Parcel struct with room for an invented extra field.
    assert(changed==1&&!home_native_parcel::resolve(graph(moved),*bundle));
    moved=files;changed=0;
    for(const auto& call:inner.calls)if(call.target&&call.receiver.kind==Value::Stack&&call.parcel==Value{Value::Self}) {
        const auto data=original.body(*call.target);if(!data)continue;
        const auto flow=trace(original,*data);
        for(const auto& value:flow.calls)if(value.target) {
            const auto helper=original.body(*value.target);if(!helper)continue;
            const auto boolean=trace(original,*helper);
            if(boolean.calls.empty()||boolean.calls[0].linkage!="AParcel_readBool")continue;
            write(moved,helper->address,0xd65f03c0u);++changed;
        }
    }
    assert(changed==1&&!home_native_parcel::resolve(graph(moved),*bundle));
    std::cout<<"PASS SDK native Parcel borrow/owned Bundle constructor and actual hasIntent trailer consumer, incompatible reference and missing trailer refusal\n";
}
