/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_input_callback.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==5);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);
        std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_input::resolve_monitor(original);
    if(!contract){std::cerr<<"Actual monitor producer/drop contract unavailable\n";return 1;}
    const auto factory=original.body(contract->create);assert(factory);
    const auto paths=home_native_provider::return_paths(original,*factory);assert(paths);
    unsigned damaged=0;
    for(const auto& path:*paths) {
        const auto flow=trace(original,*factory,false,false,path);
        for(const auto& call:flow.calls)if(call.linkage=="memcpy") {
            // Same exported API with a different copy length is not proof of
            // a compatible result. Exercise the real factory's data flow.
            auto changed=files;
            write(changed,{factory->address.image,factory->address.va+(call.index-1)*4},0x52800002u);
            assert(!home_native_input::resolve_monitor(graph(changed)));++damaged;
        }
        for(const auto at:flow.unknown) {
            auto changed=files;write(changed,{factory->address.image,factory->address.va+at*4},0xaa1f03e0u);
            assert(!home_native_input::resolve_monitor(graph(changed)));++damaged;
        }
    }
    assert(damaged==2);
    // Locate the receiver's actual SDK-owned callback table from the factory
    // argument, then resolve its destructor through relocation metadata. The
    // sample uses Android RELR; a raw pointer read would hide a missing format.
    assert(home_native_input::receiver_table(original));
    const auto callback=home_native_input::callback_drop(original);
    assert(callback);
    const auto events=home_native_input::callback_events(original,*callback);assert(events);
    unsigned forwarding_damage=0,flag_damage=0,entry_shifts=0;
    for(const auto& event:*events) {
        const auto bridge=original.body(event.adapter);assert(bridge);
        auto tail=std::find_if(bridge->words.begin(),bridge->words.end(),[](uint32_t word){
            return (word&0xfffffc1fu)==0xd61f0000u;
        });assert(tail!=bridge->words.end());
        for(size_t i=0;i<size_t(tail-bridge->words.begin());++i) {
            const auto word=bridge->words[i];
            if((word&0x7fe0ffe0u)==0x2a0003e0u&&(word&31)>=1&&(word&31)<=3) {
                auto altered=files;
                write(altered,{bridge->address.image,bridge->address.va+i*4},0xd503201fu);
                assert(!home_native_input::callback_events(graph(altered),*callback));++forwarding_damage;
            }
            if((word&0xff000000u)==0x36000000u) {
                auto altered=files;
                write(altered,{bridge->address.image,bridge->address.va+i*4},word|(1u<<24));
                assert(!home_native_input::callback_events(graph(altered),*callback));++flag_damage;
            }
            if((word&0xffc00000u)==0xf9400000u&&(word&31)==((*tail>>5)&31)) {
                auto altered=files;
                write(altered,{bridge->address.image,bridge->address.va+i*4},shift_memory(word,128));
                const auto shifted=home_native_input::callback_events(graph(altered),*callback);assert(shifted);
                const auto current=std::find_if(shifted->begin(),shifted->end(),[&](const auto& item){return item.adapter==event.adapter;});
                assert(current!=shifted->end()&&current->function_offset==event.function_offset+128);++entry_shifts;
            }
        }
        auto altered=files;
        write(altered,{bridge->address.image,bridge->address.va+size_t(tail-bridge->words.begin())*4},0xd503201fu);
        assert(!home_native_input::callback_events(graph(altered),*callback));
        const auto helper=trace(original,*bridge).calls.front().target;assert(helper);
        altered=files;write(altered,*helper,0xaa1f03e0u);
        assert(!home_native_input::callback_events(graph(altered),*callback));
    }
    assert(forwarding_damage==6&&flag_damage==2&&entry_shifts==2);
    const auto adapter=original.body(callback->adapter);assert(adapter);
    unsigned callback_damage=0;
    for(size_t i=0;i<adapter->words.size();++i) {
        const auto word=adapter->words[i];
        if((word&0xffc00000u)==0xf9400000u||(word&0xffe00c00u)==0xf8400400u
            ||(word&0xfffffc1fu)==0xd61f0000u) {
            auto altered=files;
            write(altered,{adapter->address.image,adapter->address.va+i*4},0xd503201fu);
            assert(!home_native_input::callback_drop(graph(altered)));++callback_damage;
        }
    }
    assert(callback_damage==4);
    for(size_t i=0;i<adapter->words.size();++i)if((adapter->words[i]&0xffc00000u)==0xf9400000u
        ||(adapter->words[i]&0xffe00c00u)==0xf8400400u) {
        auto altered=files;
        write(altered,{adapter->address.image,adapter->address.va+i*4},adapter->words[i]|31);
        assert(!home_native_input::callback_drop(graph(altered)));
    }
    unsigned shifted_fields=0;
    for(size_t i=0;i<adapter->words.size();++i)if((adapter->words[i]&0xffc00000u)==0xf9400000u) {
        auto altered=files;
        write(altered,{adapter->address.image,adapter->address.va+i*4},shift_memory(adapter->words[i],8));
        const auto shifted=home_native_input::callback_drop(graph(altered));assert(shifted);
        assert(shifted->base_offset==callback->base_offset+8||shifted->drop_offset==callback->drop_offset+8);
        ++shifted_fields;
    }
    assert(shifted_fields==2);
    const auto receiver_factory=original.exported("input_InputEventReceiver_new");assert(receiver_factory);
    const auto receiver_body=original.body(*receiver_factory);assert(receiver_body);
    const auto receiver_paths=home_native_provider::return_paths(original,*receiver_body);assert(receiver_paths);
    unsigned indexed_writers=0;
    for(const auto& path:*receiver_paths) {
        const auto output=trace(original,*receiver_body,false,true,path);
        assert(output.returned&&output.unknown.empty());
        for(size_t i:path)if((receiver_body->words[i]&0x3b200c00u)==0x38200800u) {
            const auto writer=std::find_if(output.stores.begin(),output.stores.end(),[&](const auto& store){return store.index==i;});
            assert(writer!=output.stores.end()&&writer->address.kind==Value::Self&&writer->width==8);
            // The real success/error writer chooses its result field with a
            // constant X-register offset. A signed index cannot inherit it.
            auto altered=*receiver_body;
            altered.words[i]=(altered.words[i]&~(7u<<13))|(6u<<13);
            const auto signed_index=trace(original,altered,false,true,path);
            assert(std::find(signed_index.unknown.begin(),signed_index.unknown.end(),i)!=signed_index.unknown.end());
            assert(std::none_of(signed_index.stores.begin(),signed_index.stores.end(),[&](const auto& store){return store.index==i;}));
            altered=*receiver_body;
            altered.words[i]|=1u<<12;
            const auto scaled=trace(original,altered,false,true,path);
            const auto shifted=std::find_if(scaled.stores.begin(),scaled.stores.end(),[&](const auto& store){return store.index==i;});
            assert(shifted!=scaled.stores.end()&&shifted->address.kind==Value::Self
                &&shifted->address.offset==writer->address.offset*8);
            ++indexed_writers;
        }
    }
    assert(indexed_writers==receiver_paths->size());
    const std::array<std::pair<unsigned,Value>,1> carrier={{{2,{Value::Argument,0,2,8}}}};
    const auto receiver_flow=trace(original,*receiver_body,false,false,{},carrier);
    unsigned copy_damage=0;
    for(const auto& store:receiver_flow.stores)if(store.address.kind==Value::Returned
        &&store.value.kind==Value::IndirectField&&store.value.number==2) {
        auto altered=files;
        write(altered,{receiver_body->address.image,receiver_body->address.va+store.index*4},
            shift_memory(receiver_body->words[store.index],int(store.width)));
        assert(!home_native_input::callback_drop(graph(altered)));++copy_damage;
    }
    assert(copy_damage==2);
    auto changed=files;
    write(changed,{contract->create.image,contract->create.va},0xd65f03c0u);
    assert(!home_native_input::resolve_monitor(graph(changed)));
    changed=files;const auto drop=original.body(contract->drop);assert(drop);
    write(changed,{contract->drop.image,contract->drop.va+(drop->words.size()-1)*4},0xd65f03c0u);
    assert(!home_native_input::resolve_monitor(graph(changed)));
    std::cout<<"PASS actual SDK monitor result/ownership contract, copy/error/return/destructor damage refused, bytes="
        <<contract->bytes<<" alignment="<<contract->alignment<<" errorShift="<<contract->error_shift
        <<", foreign carrier destructor chain validated, carrierBytes="<<callback->carrier_bytes<<'\n';
}
