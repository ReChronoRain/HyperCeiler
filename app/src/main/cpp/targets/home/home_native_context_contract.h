/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_trace.h"

namespace home_native_context {
namespace sdk=home_layout::cards::sdk;
struct Contract {
    sdk::Address context,retain,release,uid,pid;
    uint32_t uid_field=0,pid_field=0;
};
inline bool local_writes(const sdk::Trace& flow) {
    return std::all_of(flow.stores.begin(),flow.stores.end(),[](const auto& store){
        return store.address.kind==sdk::Value::Stack;
    });
}
// Public C ABI anchors identify the interface; instructions establish that the
// returned Context is borrowed from this exact Runtime, not a new owned Box.
inline std::optional<uint32_t> scalar(const sdk::Graph& graph,sdk::Address at) {
    for(unsigned depth=0;depth<4;++depth) {
        auto body=graph.body(at);if(!body||body->words.empty()||body->words.size()>64)return {};
        const auto last=body->words.back();
        const bool tail=(last&0xfc000000u)==0x14000000u;
        const auto next=tail?graph.callee(*body,body->words.size()-1,true):std::nullopt;
        if(tail)body->words.pop_back();
        const auto flow=sdk::trace(graph,*body);
        if(!flow.calls.empty()||!flow.branches.empty()||!flow.unknown.empty()||!local_writes(flow))return {};
        const auto value=flow.regs[0];
        if(tail) {
            if(!next||value.kind!=sdk::Value::Self||value.offset)return {};
            at=*next;continue;
        }
        if(!flow.returned||value.kind!=sdk::Value::Field||value.width!=4
            ||value.offset<0||value.offset>65536||value.offset%4)return {};
        return static_cast<uint32_t>(value.offset);
    }
    return {};
}
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto context=graph.exported("Runtime_get_context"),retain=graph.exported("Runtime_inc_strong"),
        release=graph.exported("Runtime_dec_strong"),uid=graph.exported("context_get_uid"),
        pid=graph.exported("context_get_pid");
    if(!context||!retain||!release||!uid||!pid||*retain==*release)return {};
    const auto body=graph.body(*context);if(!body||body->words.size()>32)return {};
    const auto flow=sdk::trace(graph,*body);
    if(!flow.returned||!flow.calls.empty()||!flow.branches.empty()||!flow.unknown.empty()
        ||!local_writes(flow)||flow.regs[0].kind!=sdk::Value::Self||flow.regs[0].offset)return {};
    const auto uid_field=scalar(graph,*uid),pid_field=scalar(graph,*pid);
    if(!uid_field||!pid_field||*uid_field==*pid_field)return {};
    // The public retain/release operations own reference counting. We neither
    // infer an Arc header nor retain a Dart pointer or an unowned Context.
    if(!graph.body(*retain)||!graph.body(*release))return {};
    return Contract{*context,*retain,*release,*uid,*pid,*uid_field,*pid_field};
}
}
