/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_contract.h"

namespace home_native_input {
struct PackedStatusContract {
    sdk::Address entry,implementation;
    uint32_t shift=0;
    int32_t success=0;
};
// The bridge returns a packed Rust status, not Android status_t. In
// particular a success can have nonzero high bits. Identify both its success
// comparison and the low error flag before interpreting a real completion.
inline std::optional<PackedStatusContract> packed_status(const sdk::Graph& graph,std::string_view name,
    std::span<const std::pair<unsigned,sdk::Value>> arguments) {
    const auto entry=graph.exported(name);if(!entry)return {};
    const auto body=graph.body(*entry);if(!body||body->words.size()>64)return {};
    const auto flow=sdk::trace(graph,*body,false,false,{},arguments);
    if(!flow.returned||flow.calls.size()!=1||!flow.calls[0].target||flow.unknown.size()!=1
        ||!flow.branches.empty()||!home_native_provider::stack_only(flow))return {};
    const auto at=flow.unknown[0];const auto word=body->words[at];
    if((word&0xffe0001fu)!=0xaa000000u)return {};
    const unsigned shift=(word>>10)&63;
    if(!shift||shift>32)return {};
    auto prefix=*body;prefix.words.resize(at);
    const auto packing=sdk::trace(graph,prefix,false,false,{},arguments);
    const auto low=packing.regs[(word>>5)&31],high=packing.regs[(word>>16)&31];
    if(low.kind!=sdk::Value::Selected||low.number>=packing.selects.size()
        ||high!=sdk::Value{sdk::Value::Returned,0,0,8})return {};
    const auto& flag=packing.selects[low.number];
    if(flag.condition!=0||flag.yes!=sdk::Value{sdk::Value::Constant,0,0}
        ||flag.no!=sdk::Value{sdk::Value::Constant,0,1}
        ||flag.field!=sdk::Value{sdk::Value::Returned,0,0,4}
        ||flag.value.kind!=sdk::Value::Constant||flag.value.number>INT32_MAX)return {};
    const auto& call=flow.calls[0];
    if(call.receiver!=sdk::Value{sdk::Value::Self}||call.parcel!=sdk::Value{sdk::Value::Parcel})return {};
    for(const auto& argument:arguments) {
        const auto value=argument.first==2?call.third:argument.first==3?call.fourth:sdk::Value{};
        if(value!=argument.second)return {};
    }
    return PackedStatusContract{*entry,*call.target,shift,int32_t(flag.value.number)};
}
inline std::optional<PackedStatusContract> finish_status(const sdk::Graph& graph) {
    const std::array<std::pair<unsigned,sdk::Value>,2> arguments={{{2,{sdk::Value::Argument,0,2,4}},
        {3,{sdk::Value::Argument,0,3,4}}}};
    return packed_status(graph,"input_InputEventReceiver_finish_input_event",arguments);
}
inline std::optional<sdk::Address> batch_consumer(const sdk::Graph& graph) {
    const auto entry=graph.exported("input_InputEventReceiver_consume_batched_input_events");if(!entry)return {};
    const auto body=graph.body(*entry);if(!body||body->words.size()>64)return {};
    const std::array<std::pair<unsigned,sdk::Value>,1> time={{{2,{sdk::Value::Argument,0,20,8}}}};
    const auto bridge=sdk::trace(graph,*body,false,false,{},time);
    if(!bridge.returned||!bridge.branches.empty()||bridge.calls.size()!=1||!bridge.calls[0].target
        ||!home_native_provider::stack_only(bridge))return {};
    const auto& call=bridge.calls[0];
    if(call.receiver!=sdk::Value{sdk::Value::Self}||call.parcel!=sdk::Value{sdk::Value::Parcel}
        ||call.third!=sdk::Value{sdk::Value::Constant,0,1}||call.fourth!=time[0].second)return {};
    const auto consumer=graph.body(*call.target);if(!consumer)return {};
    const std::array<std::pair<unsigned,sdk::Value>,2> arguments={{{2,call.third},{3,call.fourth}}};
    const auto native=sdk::trace(graph,*consumer,false,false,{},arguments);
    if(native.calls.empty()||!native.calls[0].target
        ||native.calls[0].third!=sdk::Value{sdk::Value::Constant,0,1,4}
        ||native.calls[0].fourth!=time[0].second||native.calls[0].output.kind!=sdk::Value::Stack)return {};
    // The packed return is intentionally opaque to the observer. Only the
    // receiver/runtime/frame-time call ABI and consume-batches flag authorize
    // draining; no zero/nonzero interpretation or retry is inferred here.
    return entry;
}
}
