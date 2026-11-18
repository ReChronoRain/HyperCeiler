/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_bundle_contract.h"

namespace home_native_parcel {
namespace sdk=home_layout::cards::sdk;
struct Contract {sdk::Address bundle_from;uint32_t raw_field=0;bool has_intent_trailer=false;};
inline std::optional<sdk::Call> initial_read(const sdk::Graph& graph,sdk::Address at,std::string_view name) {
    const auto body=graph.body(at);if(!body)return {};
    const auto flow=sdk::trace(graph,*body);
    if(flow.calls.empty()||flow.calls[0].linkage!=name)return {};
    auto prefix=*body;prefix.words.resize(flow.calls[0].index);
    const auto before=sdk::trace(graph,prefix);
    if(before.returned||!before.branches.empty()||!before.unknown.empty()||!before.calls.empty())return {};
    return flow.calls[0];
}
inline std::optional<Contract> resolve(const sdk::Graph& graph,const home_native_bundle::Contract& bundle) {
    const auto at=graph.exported("Bundle_create_from_parcel");if(!at)return {};
    const auto body=graph.body(*at);if(!body)return {};
    const auto paths=home_native_provider::return_paths(graph,*body);if(!paths||paths->size()!=2)return {};
    unsigned present=0,absent=0;std::optional<sdk::Address> deserialize;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,false,false,path);
        if(!flow.returned||!flow.unknown.empty()||!home_native_provider::stack_only(flow)||flow.calls.empty()
            ||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}||!flow.calls[0].target
            ||flow.calls[0].output.kind!=sdk::Value::Stack)return {};
        if(deserialize&&*deserialize!=*flow.calls[0].target)return {};
        deserialize=flow.calls[0].target;
        if(flow.regs[0].kind==sdk::Value::Constant&&flow.regs[0].number==0){++absent;continue;}
        if(flow.regs[0].kind!=sdk::Value::Returned||flow.regs[0].number>=flow.calls.size())return {};
        const auto& allocation=flow.calls[flow.regs[0].number];
        if(allocation.target!=bundle.allocator||allocation.receiver.kind!=sdk::Value::Constant
            ||allocation.receiver.number!=bundle.bytes||allocation.parcel.kind!=sdk::Value::Constant
            ||allocation.parcel.number!=bundle.alignment)return {};
        unsigned copied=0;
        for(const auto& call:flow.calls)if(call.linkage=="memcpy"&&call.receiver==flow.regs[0]
            &&call.parcel==flow.calls[0].output&&call.third==allocation.receiver)++copied;
        if(copied!=1)return {};++present;
    }
    if(present!=1||absent!=1||!deserialize)return {};
    const auto decoder=graph.body(*deserialize);if(!decoder)return {};
    const auto entry=sdk::trace(graph,*decoder);
    if(entry.calls.empty()||entry.calls[0].receiver!=sdk::Value{sdk::Value::Self}||!entry.calls[0].target)return {};
    const auto read=initial_read(graph,*entry.calls[0].target,"AParcel_readInt32");if(!read)return {};
    // The actual first integer consumer loads the borrowed AParcel pointer
    // from its SDK reference before calling the platform NDK C interface.
    if(read->receiver.kind!=sdk::Value::Field||read->receiver.width!=8
        // Supported binder-rs ABI is a reference to one borrowed AParcel*.
        // This is a pointer indirection, not a private Parcel object layout.
        ||read->receiver.offset!=0||read->parcel.kind!=sdk::Value::Stack)return {};
    unsigned trailers=0;
    for(const auto& parse:entry.calls)if(parse.target&&parse.receiver.kind==sdk::Value::Stack
        &&parse.parcel==sdk::Value{sdk::Value::Self}) {
        const auto data=graph.body(*parse.target);if(!data)continue;
        const auto contents=sdk::trace(graph,*data);
        for(const auto& value:contents.calls)if(value.target) {
            const auto boolean=initial_read(graph,*value.target,"AParcel_readBool");if(!boolean)continue;
            if(boolean->receiver.kind!=sdk::Value::Field||boolean->receiver.width!=8
                ||boolean->receiver.offset!=read->receiver.offset)return {};
            ++trailers;
        }
    }
    // HyperOS BaseBundle writes mHasIntent after the payload, except the empty
    // special case. Require its actual SDK consumer before emitting that ABI.
    if(trailers!=1)return {};
    return Contract{*at,static_cast<uint32_t>(read->receiver.offset),true};
}
}
