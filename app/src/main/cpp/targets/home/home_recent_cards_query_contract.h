/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_group_contract.h"

namespace home_layout::cards::sdk {
// The exported C SDK returns abi_stable::RVec<Box<Group>> as four native words
// through AAPCS64 x8. Discover each word's role from its actual producer, never
// mistake capacity or the destructor table for an error/success flag.
struct QueryContract {
    static constexpr size_t kBytes=4*sizeof(uint64_t);
    uint32_t pointer=0,length=0,capacity=0,vtable=0;
    Address query,table;
};
inline std::optional<QueryContract> resolve_query(const Graph& graph,const GroupContract& group,std::string_view* reason=nullptr) {
    if(reason)*reason="recent query C ABI";
    const auto at=graph.exported("IRecentTasks_get_recent_tasks_api36");if(!at)return {};
    const auto body=success_prefix(graph,*at);if(!body)return {};
    const auto flow=trace(graph,*body,true,true);
    std::optional<size_t> source;
    for(size_t i=0;i<flow.calls.size();++i) {
        const auto& call=flow.calls[i];
        if(call.receiver.kind==Value::Parcel&&call.output.kind==Value::Stack&&call.target) {
            if(source)return {};source=i;
        }
    }
    if(!source)return {};
    QueryContract out;out.query=*at;
    if(reason)*reason="four returned words and raw Vec provenance";
    std::array<bool,4> words{};unsigned pointers=0,lengths=0,capacities=0,tables=0;
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self) {
        if(store.width!=8||store.address.offset<0||store.address.offset%8
            ||static_cast<uint64_t>(store.address.offset)+8>QueryContract::kBytes)return {};
        const auto index=static_cast<size_t>(store.address.offset)/8;if(words[index])return {};words[index]=true;
        const auto value=store.value;const auto offset=static_cast<uint32_t>(store.address.offset);
        if(value.kind==Value::Result&&value.number==*source&&value.offset==group.vector_layout.pointer) {
            out.pointer=offset;++pointers;continue;
        }
        if(value.kind==Value::Count&&value.number==*source&&value.offset==group.vector_layout.pointer
            &&value.extra==int64_t(sizeof(uint64_t))&&value.factor==sizeof(uint64_t)) {
            out.length=offset;++lengths;continue;
        }
        if(value.kind==Value::ScaledResult&&value.number==*source&&value.offset==group.vector_layout.capacity
            &&value.factor==group.size/sizeof(uint64_t)) {
            out.capacity=offset;++capacities;continue;
        }
        if(value.kind==Value::Literal&&value.offset>=0) {
            const Address table{static_cast<size_t>(value.offset),value.number};if(!graph.readable(table,32))return {};
            out.vtable=offset;out.table=table;++tables;continue;
        }
        return {};
    }
    if(pointers!=1||lengths!=1||capacities!=1||tables!=1||!std::all_of(words.begin(),words.end(),[](bool set){return set;}))return {};
    if(reason)*reason="owned group box allocation";
    const auto factory=graph.exported("GroupedTaskInfo_default");if(!factory)return {};
    const auto factory_body=success_prefix(graph,*factory);if(!factory_body)return {};
    const auto factory_flow=trace(graph,*factory_body);const auto factory_result=factory_flow.regs[0];
    if(factory_result.kind!=Value::Returned||factory_result.number>=factory_flow.calls.size())return {};
    const auto allocator=factory_flow.calls[factory_result.number].target;if(!allocator)return {};
    std::optional<size_t> box;
    for(size_t i=0;i<flow.calls.size();++i) {
        const auto& call=flow.calls[i];
        if(call.target!=allocator||call.receiver.kind!=Value::Constant||call.receiver.number!=group.size
            ||call.parcel.kind!=Value::Constant||call.parcel.number!=group.alignment)continue;
        if(box)return {};box=i;
    }
    if(!box)return {};
    const auto& allocation=flow.calls[*box];bool null_guard=false;
    if(allocation.index+1<body->words.size()) {
        const auto w=body->words[allocation.index+1];
        null_guard=(w&0xff00001fu)==0xb4000000u
            &&int64_t(allocation.index+1)+(int32_t(w<<8)>>13)>=int64_t(body->words.size());
    }
    if(!null_guard)return {};
    if(reason)*reason="element move and pointer compaction loop";
    const Store* pointer_store=nullptr;
    for(const auto& store:flow.stores)if((store.address.kind==Value::RelativeResult||store.address.kind==Value::Result)
        &&store.address.offset==group.vector_layout.pointer&&store.address.number==*source
        &&(store.address.kind==Value::Result||store.address.extra==0)
        &&store.value.kind==Value::Returned&&store.value.number==*box&&store.value.offset==0&&store.width==8) {
        if(pointer_store)return {};pointer_store=&store;
    }
    if(!pointer_store||pointer_store->index+1>=body->words.size())return {};
    const auto back=body->words[pointer_store->index+1];
    const int64_t start=int64_t(pointer_store->index+1)+(int32_t(back<<8)>>13);
    if((back&0xff00001fu)!=0x54000001u||start<0||start>=int64_t(allocation.index))return {};
    unsigned pointer_advance=0,source_advance=0,counter=0;
    for(const auto& step:flow.steps) {
        if(step.index==pointer_store->index&&step.input.kind==Value::Result&&step.input.number==*source
            &&step.input.offset==group.vector_layout.pointer&&step.delta==int64_t(sizeof(uint64_t))&&!step.flags)++pointer_advance;
        if(step.index>=static_cast<size_t>(start)&&step.index<pointer_store->index&&step.input.kind==Value::Result
            &&step.input.number==*source&&step.input.offset==group.vector_layout.pointer&&step.delta==group.size&&!step.flags)++source_advance;
        if(step.index+1==pointer_store->index&&step.input.kind==Value::ScaledResult&&step.input.number==*source
            &&step.input.offset==group.vector_layout.length&&step.input.factor==group.size&&step.delta==-int64_t(group.size)&&step.flags)++counter;
    }
    if(pointer_advance!=1||source_advance!=1||counter!=1)return {};
    std::optional<Value> temporary;unsigned before=0,after=0;
    for(const auto& call:flow.calls) {
        if(call.index<static_cast<size_t>(start)||call.index>=pointer_store->index
            ||(call.linkage!="memcpy"&&call.linkage!="memmove")||call.third.kind!=Value::Constant||call.third.number!=group.size)continue;
        if(call.index<allocation.index&&call.receiver.kind==Value::Stack&&call.parcel.kind==Value::Result
            &&call.parcel.number==*source&&call.parcel.offset==group.vector_layout.pointer) {
            temporary=call.receiver;++before;
        }
        if(call.index>allocation.index&&temporary&&call.parcel==*temporary&&call.receiver.kind==Value::Returned
            &&call.receiver.number==*box&&call.receiver.offset==0)++after;
    }
    if(before!=1||after!=1)return {};
    bool empty_guard=false;
    for(const auto branch:flow.branches)if(branch<static_cast<size_t>(start)) {
        const auto w=body->words[branch];const int64_t destination=int64_t(branch)+(int32_t(w<<8)>>13);
        if((w&0xff000000u)!=0xb4000000u||destination<=int64_t(pointer_store->index+1))continue;
        Body prefix=*body;prefix.words.resize(branch);const auto count=trace(graph,prefix,true,true).regs[w&31];
        if(count.kind==Value::Result&&count.number==*source&&count.offset==group.vector_layout.length)empty_guard=true;
    }
    if(!empty_guard)return {};
    if(reason)*reason="recent query ready";return out;
}
}
