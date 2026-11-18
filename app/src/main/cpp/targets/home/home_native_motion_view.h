/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_dispatch.h"
namespace home_native_input {
// Prove that the SDK callback's owned MotionEvent is already the pointer
// accepted by the platform's typed NDK motion API. This is not a reinterpret
// based on matching sizes: follow the actual public adapter and every alias
// helper to its AMotionEvent_getPointerCount call. Destruction stays with SDK.
inline bool motion_ndk_view(const sdk::Graph& graph) {
    const auto entry=graph.exported("input_MotionEvent_getPointerCount");
    if(!entry)return false;
    const auto body=input_forwarded(graph,*entry);
    if(!body||body->words.empty())return false;
    const auto target=graph.raw_callee(*body,body->words.size()-1,true);
    if(!target||graph.import_name(*target)!=std::optional<std::string_view>("AMotionEvent_getPointerCount"))return false;
    auto prefix=*body;prefix.words.pop_back();const auto flow=sdk::trace(graph,prefix);
    if(!flow.unknown.empty()||!flow.branches.empty()||!home_native_provider::stack_only(flow)
        ||flow.calls.size()!=1||!flow.calls[0].target||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||flow.regs[0]!=sdk::Value{sdk::Value::Returned,0,0,8})return false;
    const auto alias=home_native_provider::body_or_leaf(graph,*flow.calls[0].target);
    if(!alias)return false;
    const auto identity=sdk::trace(graph,*alias);
    if(!identity.returned||!identity.calls.empty()||!identity.branches.empty()||!identity.unknown.empty()
        ||identity.regs[0]!=sdk::Value{sdk::Value::Self}||!home_native_provider::stack_only(identity))return false;
    for(const auto& load:identity.loads)if(load.address.kind!=sdk::Value::Stack)return false;
    return true;
}
}
