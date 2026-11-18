/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_provider_contract.h"

namespace home_native_binder {
namespace sdk=home_layout::cards::sdk;
struct Contract {sdk::Address get;};
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto get=graph.exported("Bundle_get_spibinder");if(!get)return {};
    const auto body=graph.body(*get);if(!body)return {};
    const std::array<std::pair<unsigned,sdk::Value>,2> input={{{1,{sdk::Value::Constant,0,11}},
        {2,{sdk::Value::Constant,0,13}}}};
    const auto flow=sdk::trace(graph,*body,false,false,{},input);
    if(!flow.returned||!flow.unknown.empty()||!home_native_provider::stack_only(flow)
        ||flow.calls.size()!=4||flow.branches.size()!=3||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||flow.calls[1].parcel!=input[0].second||flow.calls[1].third!=input[1].second
        ||!flow.calls[3].target||flow.calls[3].receiver!=sdk::Value{sdk::Value::Returned,0,2,8})return {};
    const auto tail=flow.branches.back();const auto identity=graph.callee(*body,tail,true);if(!identity)return {};
    const auto adapter=home_native_provider::body_or_leaf(graph,*identity);if(!adapter)return {};
    const auto transparent=sdk::trace(graph,*adapter);
    if(!transparent.returned||!transparent.calls.empty()||!transparent.branches.empty()||!transparent.unknown.empty()
        ||!home_native_provider::stack_only(transparent)||transparent.regs[0]!=sdk::Value{sdk::Value::Self})return {};
    auto prefix=*body;prefix.words.resize(tail);const auto owned=sdk::trace(graph,prefix,false,false,{},input);
    if(owned.regs[0]!=sdk::Value{sdk::Value::Returned,0,3,8})return {};
    int64_t empty=-1;
    for(size_t index=0;index<2;++index) {
        const auto at=flow.branches[index];const auto word=body->words[at];
        if((word&0xff00001fu)!=0xb4000000u)return {};
        const int64_t destination=int64_t(at)+(int32_t(word<<8)>>13);
        if(destination<=int64_t(tail)||destination>=int64_t(body->words.size())||(empty>=0&&empty!=destination))return {};
        empty=destination;
    }
    std::vector<size_t> absent;
    for(size_t i=static_cast<size_t>(empty);i<body->words.size();++i)absent.push_back(i);
    const auto missing=sdk::trace(graph,*body,false,false,absent);
    if(!missing.returned||!missing.unknown.empty()||!missing.calls.empty()
        ||missing.regs[0].kind!=sdk::Value::Constant||missing.regs[0].number)return {};
    const auto optional=graph.body(*flow.calls[3].target);if(!optional)return {};
    const auto cloned=sdk::trace(graph,*optional);
    if(!cloned.returned||!cloned.calls.empty()||!cloned.unknown.empty()||cloned.branches.size()!=2
        ||!home_native_provider::stack_only(cloned)||cloned.regs[0]!=sdk::Value{sdk::Value::Self})return {};
    const auto check=optional->words[cloned.branches[0]];
    if((check&0xff00001fu)!=0xb4000000u)return {};
    const auto target=graph.callee(*optional,cloned.branches[1],true);if(!target)return {};
    const auto strong=graph.body(*target);if(!strong)return {};
    const auto retained=sdk::trace(graph,*strong);
    // binder-rs clones its transparent native AIBinder pointer with the NDK
    // retain, then returns that same pointer. The public SharedAIBinder adapter
    // is identity, so callers own one NDK strong reference, not an SDK Box.
    if(!retained.returned||!retained.branches.empty()||!retained.unknown.empty()||retained.calls.size()!=1
        ||retained.calls[0].linkage!="AIBinder_incStrong"||retained.calls[0].receiver!=sdk::Value{sdk::Value::Field,0,0,8}
        ||retained.regs[0]!=retained.calls[0].receiver||!home_native_provider::stack_only(retained))return {};
    return Contract{*get};
}
}
