/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_provider_contract.h"

namespace home_native_input {
namespace sdk=home_layout::cards::sdk;
struct MonitorContract {
    sdk::Address create,drop;
    uint32_t bytes=0,alignment=0,error_shift=0;
    uint64_t error_flag=0;
};
struct ReceiverTable {sdk::Address table,destructor;};
// Identify the SDK's own callback adapter from its constructor argument.
// This inventory does not authorize a foreign callback or its event ABI.
inline std::optional<ReceiverTable> receiver_table(const sdk::Graph& graph) {
    const auto factory=graph.exported("input_InputEventReceiver_new");if(!factory)return {};
    const auto body=graph.body(*factory);if(!body||body->words.size()>256)return {};
    const auto flow=sdk::trace(graph,*body);std::optional<ReceiverTable> result;
    for(const auto& call:flow.calls)if(call.third.kind==sdk::Value::Returned&&call.fourth.kind==sdk::Value::Literal) {
        if(result||!call.target||call.third.number>=flow.calls.size())return {};
        const auto& allocation=flow.calls[call.third.number];
        if(!allocation.target||allocation.receiver.kind!=sdk::Value::Constant
            ||!allocation.receiver.number||allocation.receiver.number>4096
            ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
        const sdk::Address table{size_t(call.fourth.offset),call.fourth.number};
        const auto destructor=graph.data_reference(table);
        if(!destructor||!graph.body(*destructor))return {};
        result=ReceiverTable{table,*destructor};
    }
    return result;
}

// The SDK C bridge returns an AAPCS64 pair, not a Dart reference. The bridge
// owns the Box allocation; only its public destructor may release a success.
// Locate the allocator/copy/destructor relationship and both return paths
// before calling it. No monitor field or object size is supplied by us.
inline std::optional<MonitorContract> resolve_monitor(const sdk::Graph& graph) {
    const auto create=graph.exported("input_InputManager_monitorGestureInput"),
        drop=graph.exported("input_InputMonitor_drop");
    if(!create||!drop)return {};
    const auto body=graph.body(*create),destructor=graph.body(*drop);
    if(!body||!destructor||destructor->words.empty())return {};
    const auto paths=home_native_provider::return_paths(graph,*body);
    if(!paths||paths->size()!=2)return {};
    const std::array<std::pair<unsigned,sdk::Value>,2> inputs={{{1,{sdk::Value::Constant,0,11}},
        {2,{sdk::Value::Constant,0,17}}}};
    std::optional<MonitorContract> result;
    unsigned errors=0;uint32_t error_shift=0;uint64_t error_flag=0;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,false,false,path,inputs);
        if(!flow.returned||flow.calls.size()<3||!home_native_provider::stack_only(flow))return {};
        const auto& singleton=flow.calls[0];const auto& display=flow.calls[1];const auto& producer=flow.calls[2];
        if(!singleton.target||!producer.target||display.receiver!=inputs[1].second
            ||producer.receiver!=sdk::Value{sdk::Value::Returned,0,0,8}
            ||producer.parcel!=sdk::Value{sdk::Value::Self}||producer.third!=inputs[0].second
            ||producer.fourth!=sdk::Value{sdk::Value::ScalarResult,0,1,4}
            ||producer.output.kind!=sdk::Value::Stack)return {};
        if(flow.unknown.empty()&&flow.regs[0]==sdk::Value{sdk::Value::Constant}) {
            if(result||flow.calls.size()!=6||flow.regs[1].kind!=sdk::Value::Returned
                ||flow.regs[1].number>=flow.calls.size())return {};
            const auto& allocation=flow.calls[flow.regs[1].number];
            if(!allocation.target||allocation.receiver.kind!=sdk::Value::Constant
                ||!allocation.receiver.number||allocation.receiver.number>4096
                ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
            const auto& copy=flow.calls.back();
            if(copy.linkage!="memcpy"||copy.receiver!=flow.regs[1]
                ||copy.parcel!=producer.output||copy.third!=allocation.receiver)return {};
            result=MonitorContract{*create,*drop,uint32_t(allocation.receiver.number),uint32_t(allocation.parcel.number)};
            continue;
        }
        if(flow.unknown.size()!=1||flow.calls.size()!=5||++errors!=1)return {};
        const auto index=flow.unknown.front();const auto word=body->words[index];
        // ORR X0, flag, status LSL shift. Inspect actual incoming values,
        // never infer the error layout from a private Rust symbol name.
        if((word&0xffe0001fu)!=0xaa000000u)return {};
        error_shift=(word>>10)&63;
        if(!error_shift||error_shift>32)return {};
        auto prefix=path;const auto end=std::find(prefix.begin(),prefix.end(),index);
        if(end==prefix.end())return {};prefix.erase(end,prefix.end());
        const auto packing=sdk::trace(graph,*body,false,false,prefix,inputs);
        const auto low=packing.regs[(word>>5)&31],high=packing.regs[(word>>16)&31];
        if(low.kind!=sdk::Value::Constant||!low.number||low.number>=(uint64_t(1)<<error_shift)
            ||high.kind!=sdk::Value::ScalarResult||high.number!=3||high.width!=4
            ||flow.calls[3].receiver!=producer.output||flow.calls[4].receiver!=producer.output)return {};
        error_flag=low.number;
    }
    if(!result||errors!=1)return {};
    const auto tail=graph.raw_callee(*destructor,destructor->words.size()-1,true);
    if(!tail||!graph.linked(*tail))return {};
    auto prefix=*destructor;prefix.words.pop_back();const auto release=sdk::trace(graph,prefix);
    if(release.calls.empty()||!release.calls[0].target||release.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||release.regs[0]!=sdk::Value{sdk::Value::Self}
        ||release.regs[1]!=sdk::Value{sdk::Value::Constant,0,result->bytes}
        ||release.regs[2]!=sdk::Value{sdk::Value::Constant,0,result->alignment}
        ||!home_native_provider::stack_only(release))return {};
    for(const auto& load:release.loads)if(load.address.kind==sdk::Value::Self
        &&(load.address.offset<0||uint64_t(load.address.offset)+load.width>result->bytes))return {};
    result->error_shift=error_shift;result->error_flag=error_flag;return result;
}
}
