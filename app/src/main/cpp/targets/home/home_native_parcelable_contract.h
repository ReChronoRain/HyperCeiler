/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_parcel_contract.h"

namespace home_native_parcelable {
namespace sdk=home_layout::cards::sdk;
struct Contract {sdk::Address get,make_parcel;};
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto get=graph.exported("Bundle_get_parcelable");if(!get)return {};
    const auto body=graph.body(*get);if(!body||body->words.empty())return {};
    const auto tail=graph.callee(*body,body->words.size()-1,true);if(!tail)return {};
    const auto maker=graph.body(*tail);if(!maker)return {};
    const auto creation=sdk::trace(graph,*maker);
    if(creation.calls.empty()||creation.calls[0].linkage!="AParcel_create")return {};
    const auto paths=home_native_provider::return_paths(graph,*maker);if(!paths||paths->size()!=1)return {};
    const auto returns=sdk::trace(graph,*maker,false,false,paths->front());
    if(!returns.returned||!returns.unknown.empty()||returns.regs[0].kind!=sdk::Value::Returned
        ||returns.regs[0].number!=0||!home_native_provider::stack_only(returns))return {};
    const auto flow=sdk::trace(graph,*body);
    if(!flow.returned||!flow.unknown.empty()||flow.regs[0].kind!=sdk::Value::Returned
        ||flow.regs[0].number>=flow.calls.size()||!home_native_provider::stack_only(flow))return {};
    const auto& parcel=flow.calls[flow.regs[0].number];
    if(!parcel.target||parcel.receiver.kind!=sdk::Value::Stack)return {};
    const auto helper=graph.body(*parcel.target);if(!helper)return {};
    const auto content=sdk::trace(graph,*helper);unsigned producers=0,destructors=0;
    for(const auto& call:content.calls)if(call.target) {
        if(call.target==tail)++producers;
        const auto destroy=graph.body(*call.target);if(!destroy||destroy->words.empty())continue;
        const auto end=graph.raw_callee(*destroy,destroy->words.size()-1,true);
        if(!end||graph.import_name(*end)!=std::optional<std::string_view>("AParcel_delete"))continue;
        auto prefix=*destroy;prefix.words.pop_back();const auto release=sdk::trace(graph,prefix);
        if(!release.calls.empty()||!release.branches.empty()||!release.unknown.empty()
            ||release.regs[0]!=sdk::Value{sdk::Value::Field,0,0,8})return {};
        ++destructors;
    }
    // Both fallback and copied-payload cases use the same owned binder-rs
    // Parcel carrier. Its platform allocation/destruction is not an SDK Box.
    if(producers!=2||destructors!=1)return {};
    return Contract{*get,*tail};
}
}
