/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_input_receiver.h"
#include "targets/home/home_native_input_status.h"
#include "targets/home/home_native_input_channel.h"
#include "targets/home/home_native_input_dispatch.h"
#include "targets/home/home_native_input_runtime_contract.h"
#include "targets/home/home_native_motion_view.h"
#include <iostream>
int main(int argc,char** argv) {
    assert(argc==8);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream stream(argv[i],std::ios::binary);
        std::vector<char> bytes((std::istreambuf_iterator<char>(stream)),{});
        assert(!bytes.empty());Bytes file(bytes.size());std::memcpy(file.data(),bytes.data(),bytes.size());files.push_back(std::move(file));
    }
    const auto original=graph(files);const auto contract=home_native_input::receiver_result(original);
    assert(contract);
    const auto finish=home_native_input::finish_status(original);assert(finish);
    const auto channel=home_native_input::channel_contract(original);assert(channel);
    const auto dispatch=home_native_input::input_dispatch(original);assert(dispatch);
    const auto batch=home_native_input::batch_consumer(original);assert(batch);
    const auto carrier=home_native_input::callback_drop(original);assert(carrier);
    const auto callbacks=home_native_input::callback_methods(original,*carrier);assert(callbacks);
    for(const auto& method:*callbacks)assert(method.owns_event==(method.slot==dispatch->key_slot||method.slot==dispatch->motion_slot));
    assert(home_native_input::runtime_contract(original));
    assert(home_native_input::motion_ndk_view(original));
    const auto count=original.exported("input_MotionEvent_getPointerCount");assert(count);
    const auto ndk=home_native_input::input_forwarded(original,*count);assert(ndk);
    auto count_prefix=*ndk;count_prefix.words.pop_back();
    const auto identity_at=trace(original,count_prefix).calls[0].target;assert(identity_at);
    const auto identity_body=home_native_provider::body_or_leaf(original,*identity_at);assert(identity_body);
    auto shifted_pointer=files;write(shifted_pointer,*identity_at,0x91002000u); // ADD X0,X0,#8
    assert(!home_native_input::motion_ndk_view(graph(shifted_pointer)));
    auto neutral_prefix=files;write(neutral_prefix,*identity_at,0xd503201fu); // NOP instead of BTI
    assert(home_native_input::motion_ndk_view(graph(neutral_prefix)));
    auto broken_ndk=files;write(broken_ndk,{ndk->address.image,ndk->address.va+(ndk->words.size()-1)*4},0xd65f03c0u);
    assert(!home_native_input::motion_ndk_view(graph(broken_ndk)));
    const auto completion=original.body(finish->entry);assert(completion);
    for(size_t i=0;i<completion->words.size();++i)if((completion->words[i]&0xffe0001fu)==0xaa000000u) {
        auto altered=files;
        write(altered,{completion->address.image,completion->address.va+i*4},completion->words[i]|(1u<<16));
        assert(!home_native_input::finish_status(graph(altered)));
    }
    auto mismatch=files;write(mismatch,completion->address,0x2a1f03e2u);
    assert(!home_native_input::finish_status(graph(mismatch)));
    const auto borrowed=original.body(channel->get);assert(borrowed);
    const auto getter=trace(original,*borrowed).calls[0].target;assert(getter);
    const auto getter_body=original.body(*getter);assert(getter_body);
    for(size_t i=0;i<getter_body->words.size();++i)if((getter_body->words[i]&0xffc00000u)==0xf9400000u) {
        auto altered=files;
        write(altered,{getter_body->address.image,getter_body->address.va+i*4},getter_body->words[i]&~(1u<<30));
        assert(!home_native_input::channel_contract(graph(altered)));
    }
    const auto consume_bridge=original.body(dispatch->consume);assert(consume_bridge);
    const auto consumer_at=trace(original,*consume_bridge).calls[0].target;assert(consumer_at);
    const auto consumer=original.body(*consumer_at);assert(consumer);
    unsigned borrowed_damage=0,slot_swaps=0;
    for(size_t goal=0;goal<consumer->words.size();++goal)if((consumer->words[goal]&0xfffffc1fu)==0xd63f0000u) {
        for(size_t i=goal>10?goal-10:0;i<goal;++i)if((consumer->words[i]&0xffffffe0u)==0xaa1303e0u
            &&(consumer->words[i]&31)==2) {
            auto altered=files;
            write(altered,{consumer->address.image,consumer->address.va+i*4},0xaa0103e2u);
            assert(!home_native_input::input_dispatch(graph(altered)));++borrowed_damage;
        }
    }
    for(size_t i=0;i+1<consumer->words.size();++i) {
        const auto branch=consumer->words[i],load=consumer->words[i+1];
        if((branch&0xff00001fu)!=0x54000001u||(load&0xffc00000u)!=0xf9400000u
            ||((load>>10)&4095)*8!=dispatch->key_slot*8||(load&31)!=((load>>5)&31))continue;
        auto altered=files;write(altered,{consumer->address.image,consumer->address.va+i*4},branch^1);
        const auto swapped=home_native_input::runtime_contract(graph(altered));assert(swapped);
        assert(swapped->dispatch.key_slot==dispatch->motion_slot&&swapped->dispatch.motion_slot==dispatch->key_slot);
        ++slot_swaps;
    }
    assert(borrowed_damage==4&&slot_swaps==1);
    unsigned float_damage=0,boolean_damage=0;
    for(size_t i=0;i<consumer->words.size();++i) {
        const auto word=consumer->words[i];
        if((word&0xfffffc1fu)==0x1e204000u) {
            auto altered=files;write(altered,{consumer->address.image,consumer->address.va+i*4},0xd503201fu);
            assert(!home_native_input::input_dispatch(graph(altered)));++float_damage;
        }
        if((word&0xff80001fu)==0x12000003u&&logical_immediate(word)==1) {
            auto altered=files;write(altered,{consumer->address.image,consumer->address.va+i*4},word^(1u<<16));
            assert(!home_native_input::input_dispatch(graph(altered)));++boolean_damage;
        }
    }
    assert(float_damage==1&&boolean_damage==2);
    const auto body=original.body(contract->create);assert(body);
    const auto paths=home_native_provider::return_paths(original,*body);assert(paths);
    unsigned indexed=0,copies=0,inverse_damage=0;
    for(const auto& path:*paths) {
        const auto flow=trace(original,*body,false,true,path);
        for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.width==1) {
            auto changed=files;
            write(changed,{body->address.image,body->address.va+store.index*4},shift_memory(body->words[store.index],1));
            const auto shifted=home_native_input::receiver_result(graph(changed));
            assert(shifted&&shifted->tag==contract->tag+1);
        }
        for(size_t i:path)if((body->words[i]&0x3b200c00u)==0x38200800u) {
            auto changed=files;
            write(changed,{body->address.image,body->address.va+i*4},(body->words[i]&~(7u<<13))|(6u<<13));
            assert(!home_native_input::receiver_result(graph(changed)));++indexed;
        }
        for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.value.kind==Value::Returned) {
            for(const auto& copy:flow.stores)if(copy.address.kind==Value::Returned&&copy.address.number==store.value.number) {
                auto changed=files;
                write(changed,{body->address.image,body->address.va+copy.index*4},0xd503201fu);
                assert(!home_native_input::receiver_result(graph(changed)));++copies;
            }
        }
        for(const auto& call:flow.calls)if(call.output.kind==Value::Stack&&call.parcel.kind==Value::Stack
            &&call.receiver.kind==Value::Stack) {
            for(const auto& load:flow.loads)if(load.index>call.index&&load.width==8&&load.address==call.output) {
                auto changed=files;
                write(changed,{body->address.image,body->address.va+load.index*4},shift_memory(body->words[load.index],8));
                assert(!home_native_input::receiver_result(graph(changed)));++inverse_damage;
            }
        }
    }
    assert(indexed==4&&copies==6&&inverse_damage==2);
    auto changed=files;const auto error=original.body(contract->error_drop.drop);assert(error);
    write(changed,{error->address.image,error->address.va+(error->words.size()-1)*4},0xd503201fu);
    assert(!home_native_input::receiver_result(graph(changed)));
    changed=files;const auto drop=original.body(contract->drop);assert(drop);
    write(changed,{drop->address.image,drop->address.va+(drop->words.size()-1)*4},0xd65f03c0u);
    assert(!home_native_input::receiver_result(graph(changed)));
    std::cout<<"PASS actual SDK channel borrow/clone, completion packing, typed callback dispatch and receiver ownership; "
        <<"shifted tag accepted, indexed/copy/drop damage refused, resultBytes="<<contract->bytes
        <<" managerBytes="<<contract->manager_bytes<<'\n';
}
