/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_contract.h"

namespace home_native_input {
struct CallbackDropContract {
    sdk::Address adapter;
    uint32_t carrier_bytes=0,prefix_offset=0,base_offset=0,drop_offset=0,argument_offset=0;
};

// Resume an adapter trace at a proven pointer load. Its opaque pointee gets a
// fresh identity; unrelated registers retain their provenance. No stack value
// is transferred, so a later restored unknown cannot authorize a foreign call.
struct CallbackTrace {
    sdk::Body body;
    std::vector<std::pair<unsigned,sdk::Value>> inputs;
};
inline CallbackTrace callback_resume(const sdk::Graph& graph,const CallbackTrace& current,
    size_t load,uint64_t identity) {
    auto prefix=current.body;prefix.words.resize(load+1);
    const auto flow=sdk::trace(graph,prefix,false,false,{},current.inputs);
    CallbackTrace next{{{current.body.address.image,current.body.address.va+(load+1)*4},
        {current.body.words.begin()+load+1,current.body.words.end()}},{}};
    for(unsigned reg=0;reg<31;++reg)next.inputs.push_back({reg,flow.regs[reg]});
    next.inputs[current.body.words[load]&31].second={sdk::Value::Argument,0,identity,8};
    return next;
}

// This contract permits calling only the SDK's carrier destructor adapter.
// It does not permit constructing a receiver, dropping an SDK event, or
// treating the receiver-manager handle as the borrowed callback receiver.
inline std::optional<CallbackDropContract> callback_drop(const sdk::Graph& graph) {
    const auto table=receiver_table(graph);if(!table)return {};
    const auto factory=graph.exported("input_InputEventReceiver_new");
    if(!factory)return {};
    const auto body=graph.body(*factory);if(!body)return {};
    const std::array<std::pair<unsigned,sdk::Value>,1> carrier={{{2,{sdk::Value::Argument,0,2,8}}}};
    const auto flow=sdk::trace(graph,*body,false,false,{},carrier);
    std::optional<uint32_t> bytes;
    for(const auto& call:flow.calls) {
        if(call.fourth!=sdk::Value{sdk::Value::Literal,int64_t(table->table.image),table->table.va,8})continue;
        if(bytes||call.third.kind!=sdk::Value::Returned||call.third.offset
            ||call.third.number>=flow.calls.size())return {};
        const auto& allocation=flow.calls[call.third.number];
        if(allocation.receiver.kind!=sdk::Value::Constant||!allocation.receiver.number
            ||allocation.receiver.number>4096
            ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
        std::vector<sdk::Store> copies;
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Returned
            &&store.address.number==call.third.number&&store.index<call.index
            &&store.value.kind==sdk::Value::IndirectField&&store.value.number==2)copies.push_back(store);
        if(copies.empty())return {};
        std::sort(copies.begin(),copies.end(),[](const auto& a,const auto& b){return a.value.offset<b.value.offset;});
        const auto payload=copies.front().address.offset;
        if(payload<0||uint64_t(payload)>=allocation.receiver.number)return {};
        uint64_t copied=0;
        for(const auto& copy:copies) {
            if(copy.value.offset!=int64_t(copied)||copy.address.offset!=payload+int64_t(copied)
                ||copy.value.width!=copy.width||!copy.width)return {};
            copied+=copy.width;
        }
        if(copied!=allocation.receiver.number-uint64_t(payload))return {};
        bytes=uint32_t(copied);
    }
    if(!bytes)return {};
    const auto destructor=graph.body(table->destructor);
    if(!destructor||destructor->words.empty()||destructor->words.size()>64)return {};
    CallbackTrace stage{*destructor,{}};
    CallbackDropContract result{table->destructor,*bytes};
    // Three pointer loads: carrier -> prefix -> base vtable -> drop thunk.
    // Field positions come from these actual consumers, not Rust type names.
    for(unsigned level=0;level<3;++level) {
        const auto step=sdk::trace(graph,stage.body,false,false,{},stage.inputs);
        std::optional<sdk::Load> field;
        for(const auto& load:step.loads) {
            if(load.address.kind==sdk::Value::Stack)continue;
            if(load.width!=8||load.address.offset<0||load.address.offset>4096
                ||load.address.offset%alignof(void*))return {};
            if(level==0&&load.address.kind!=sdk::Value::Self)return {};
            if(level&&load.address!=sdk::Value{sdk::Value::Argument,load.address.offset,level,8})return {};
            field=load;
            // Stop at this load; deeper pointers have not been established yet.
            break;
        }
        if(!field)return {};
        if(level==0)result.prefix_offset=uint32_t(field->address.offset);
        if(level==1)result.base_offset=uint32_t(field->address.offset);
        if(level==2)result.drop_offset=uint32_t(field->address.offset);
        // This is an integer pointer load, not a SIMD register or load pair.
        const auto word=stage.body.words[field->index];
        if((word&31)==31||((word&0xffc00000u)!=0xf9400000u&&(word&0xffe00c00u)!=0xf8400400u))return {};
        auto proven=stage.body;proven.words.resize(field->index+1);
        const auto before=sdk::trace(graph,proven,false,false,{},stage.inputs);
        if(!before.unknown.empty()||!before.calls.empty()||!before.branches.empty())return {};
        for(const auto& store:before.stores)if(store.address.kind!=sdk::Value::Stack)return {};
        stage=callback_resume(graph,stage,field->index,level+1);
    }
    if(result.prefix_offset+8>result.carrier_bytes||stage.body.words.empty())return {};
    const auto tail=stage.body.words.back();
    if((tail&0xfffffc1fu)!=0xd61f0000u)return {};
    auto prefix=stage.body;prefix.words.pop_back();
    const auto final=sdk::trace(graph,prefix,false,false,{},stage.inputs);
    if(!final.unknown.empty()||!final.calls.empty()||!final.branches.empty()
        ||final.regs[(tail>>5)&31]!=sdk::Value{sdk::Value::Argument,0,3,8}
        ||final.regs[0].kind!=sdk::Value::Self||final.regs[0].offset<0
        ||uint64_t(final.regs[0].offset)>=result.carrier_bytes)return {};
    for(const auto& load:final.loads)if(load.address.kind!=sdk::Value::Stack)return {};
    for(const auto& store:final.stores)if(store.address.kind!=sdk::Value::Stack)return {};
    result.argument_offset=uint32_t(final.regs[0].offset);
    if(result.argument_offset+sizeof(void*)>result.carrier_bytes
        ||(result.argument_offset<result.prefix_offset+sizeof(void*)
            &&result.prefix_offset<result.argument_offset+sizeof(void*)))return {};
    return result;
}

struct CallbackEventContract {
    sdk::Address adapter;
    uint32_t slot=0,flags_offset=0,flag_bit=0,function_offset=0;
    bool owns_event=false;
};
struct CallbackEventProbe {
    CallbackEventContract contract;
    void(*invoke)(void*,void*,void*,void*);
};
// Exercise verified callback bridges with module-owned storage and tokens.
// Nothing is registered with InputDispatcher and no SDK Box is fabricated.
// The foreign drop thunk owns these bytes and therefore never asks the SDK
// allocator to release memory allocated by our C++ runtime.
inline bool probe_callback_drop(const CallbackDropContract& contract,void(*adapter)(void*),
    std::span<const CallbackEventProbe> events={}) {
    struct Witness {void* argument;unsigned calls=0,event_calls=0;bool matched=false,forwarded=true;};
    size_t prefix_bytes=contract.base_offset+sizeof(void*);
    for(const auto& event:events)prefix_bytes=std::max(prefix_bytes,
        std::max(size_t(event.contract.function_offset)+sizeof(void*),size_t(event.contract.flags_offset)+1));
    std::vector<std::byte> carrier(contract.carrier_bytes),prefix(prefix_bytes),
        base(contract.drop_offset+sizeof(void*));
    Witness witness{carrier.data()+contract.argument_offset};
    const auto drop=+[](void* argument) {
        Witness* current=nullptr;std::memcpy(&current,argument,sizeof(current));
        ++current->calls;current->matched=argument==current->argument;
    };
    const auto base_pointer=base.data(),prefix_pointer=prefix.data();
    const auto witness_pointer=&witness;
    std::memcpy(base.data()+contract.drop_offset,&drop,sizeof(drop));
    std::memcpy(prefix.data()+contract.base_offset,&base_pointer,sizeof(base_pointer));
    std::memcpy(carrier.data()+contract.prefix_offset,&prefix_pointer,sizeof(prefix_pointer));
    std::memcpy(carrier.data()+contract.argument_offset,&witness_pointer,sizeof(witness_pointer));
    const auto receive=+[](void* data,void* runtime,void* receiver,void* event) {
        auto& current=*static_cast<Witness*>(data);
        ++current.event_calls;
        current.forwarded&=runtime==&current.argument&&receiver==&current.calls&&event==&current.event_calls;
    };
    for(const auto& event:events) {
        prefix[event.contract.flags_offset]|=std::byte(1u<<event.contract.flag_bit);
        std::memcpy(prefix.data()+event.contract.function_offset,&receive,sizeof(receive));
        event.invoke(carrier.data(),&witness.argument,&witness.calls,&witness.event_calls);
    }
    adapter(carrier.data());
    return witness.calls==1&&witness.matched&&witness.forwarded&&witness.event_calls==events.size();
}
// Locate all five adapters through the trait's actual relocated entries.
// Owned-event conversion must be an identity: the bridge probe passes module
// tokens, never a fabricated native KeyEvent or MotionEvent.
inline std::optional<std::vector<CallbackEventContract>> callback_methods(const sdk::Graph& graph,
    const CallbackDropContract& drop) {
    const auto table=receiver_table(graph);if(!table)return {};
    std::vector<CallbackEventContract> result;
    unsigned methods=0;bool ended=false;
    const std::array<std::pair<unsigned,sdk::Value>,3> arguments={{{1,{sdk::Value::Argument,0,11,8}},
        {2,{sdk::Value::Argument,0,12,8}},{3,{sdk::Value::Argument,0,13,8}}}};
    // Bounded discovery, not a fixed vtable layout. The adjacent carrier
    // vtable starts with the same proven destructor and ends this inventory.
    for(unsigned slot=1;slot<32;++slot) {
        const auto address=graph.data_reference({table->table.image,table->table.va+slot*sizeof(void*)});
        if(!address)continue;
        if(*address==table->destructor){ended=true;break;}
        const auto body=graph.body(*address);
        if(!body||body->words.empty()||body->words.size()>80)return {};
        ++methods;
        const auto flow=sdk::trace(graph,*body,false,false,{},arguments);
        const auto pointer=std::find_if(flow.loads.begin(),flow.loads.end(),[](const auto& load){
            return load.address.kind==sdk::Value::Self;
        });
        if(pointer==flow.loads.end()||pointer->address.offset!=drop.prefix_offset||pointer->width!=8)return {};
        auto prefix=*body;prefix.words.resize(pointer->index+1);
        const auto before=sdk::trace(graph,prefix,false,false,{},arguments);
        if(!before.unknown.empty()||!before.branches.empty())return {};
        for(const auto& store:before.stores)if(store.address.kind!=sdk::Value::Stack)return {};
        // All callbacks share the four native integer argument registers.
        // Scalar callbacks may additionally supply floating arguments, which
        // an observation-only listener deliberately does not read.
        const bool owns_event=!before.calls.empty();
        const auto pointer_word=body->words[pointer->index];
        if((pointer_word&31)==31||((pointer_word&0xffc00000u)!=0xf9400000u
            &&(pointer_word&0xffe00c00u)!=0xf8400400u))return {};
        if(owns_event) {
            if(before.calls.size()!=1||!before.calls[0].target||before.calls[0].receiver!=arguments[2].second)return {};
            const auto conversion=graph.body(*before.calls[0].target);if(!conversion)return {};
            const auto identity=sdk::trace(graph,*conversion);
            if(!identity.returned||identity.regs[0]!=sdk::Value{sdk::Value::Self}
                ||!identity.unknown.empty()||!identity.calls.empty()||!identity.branches.empty())return {};
            for(const auto& load:identity.loads)if(load.address.kind!=sdk::Value::Stack)return {};
            for(const auto& store:identity.stores)if(store.address.kind!=sdk::Value::Stack)return {};
        }
        CallbackTrace current{*body,{arguments.begin(),arguments.end()}};
        auto stage=callback_resume(graph,current,pointer->index,1);
        if(owns_event)for(auto& input:stage.inputs)if(input.second==sdk::Value{sdk::Value::Returned,0,0,8})input.second=arguments[2].second;
        size_t tail=0;
        while(tail<stage.body.words.size()&&(stage.body.words[tail]&0xfffffc1fu)!=0xd61f0000u)++tail;
        if(tail==stage.body.words.size())return {};
        auto forwarding=stage.body;forwarding.words.resize(tail);
        const auto call=sdk::trace(graph,forwarding,false,false,{},stage.inputs);
        const auto target=call.regs[(stage.body.words[tail]>>5)&31];
        if(!call.unknown.empty()||!call.calls.empty()||call.branches.size()!=1
            ||target.kind!=sdk::Value::IndirectField||target.number!=1||target.width!=8
            ||target.offset<0||target.offset>4096||target.offset%sizeof(void*)
            ||call.regs[0]!=sdk::Value{sdk::Value::Field,drop.argument_offset,0,8}
            ||call.regs[1]!=arguments[0].second||call.regs[2]!=arguments[1].second
            ||call.regs[3]!=arguments[2].second)return {};
        for(const auto& store:call.stores)if(store.address.kind!=sdk::Value::Stack)return {};
        const auto branch=call.branches[0];const auto word=stage.body.words[branch];
        if((word&0xff000000u)!=0x36000000u)return {}; // TBZ Wn, present bit.
        const auto destination=int64_t(branch)+(int32_t(word<<13)>>18);
        const unsigned bit=(word>>19)&31;
        if(bit>=8||destination<=int64_t(tail)||destination>=int64_t(stage.body.words.size()))return {};
        auto flag_path=stage.body;flag_path.words.resize(branch);
        const auto flag=sdk::trace(graph,flag_path,false,false,{},stage.inputs).regs[word&31];
        if(flag.kind!=sdk::Value::IndirectField||flag.number!=1||flag.width!=1||flag.offset<0||flag.offset>4096)return {};
        for(const auto& load:call.loads) {
            if(load.address.kind==sdk::Value::Stack)continue;
            if(load.address==sdk::Value{sdk::Value::Self,drop.argument_offset}&&load.width==8)continue;
            if(load.address==sdk::Value{sdk::Value::Argument,flag.offset,1,8}&&load.width==1)continue;
            if(load.address==sdk::Value{sdk::Value::Argument,target.offset,1,8}&&load.width==8)continue;
            return {};
        }
        if((flag.offset>=target.offset&&flag.offset<target.offset+int64_t(sizeof(void*)))
            ||(flag.offset>=drop.base_offset&&flag.offset<drop.base_offset+int64_t(sizeof(void*)))
            ||(target.offset<drop.base_offset+int64_t(sizeof(void*))&&drop.base_offset<target.offset+int64_t(sizeof(void*))))return {};
        for(const auto& method:result)if(method.function_offset==target.offset
            ||(method.flags_offset==flag.offset&&method.flag_bit==bit))return {};
        result.push_back({*address,slot,uint32_t(flag.offset),bit,uint32_t(target.offset),owns_event});
    }
    // The supported receiver trait has five callbacks and two owned-event
    // identity adapters. A changed trait must be re-proven before use.
    if(!ended||methods!=5||result.size()!=5||std::count_if(result.begin(),result.end(),[](const auto& method){return method.owns_event;})!=2)return {};
    return result;
}
inline std::optional<std::vector<CallbackEventContract>> callback_events(const sdk::Graph& graph,
    const CallbackDropContract& drop) {
    auto methods=callback_methods(graph,drop);if(!methods)return {};
    std::erase_if(*methods,[](const auto& method){return !method.owns_event;});return methods;
}
}
