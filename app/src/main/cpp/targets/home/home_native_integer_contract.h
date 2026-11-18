/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_bundle_contract.h"
namespace home_native_bundle {
struct IntegerContract {sdk::Address get;unsigned shift=0;};
// Public C API anchor plus every return path: nullable cast, 32-bit load,
// packed value in X0 and exact None sentinel. No Rust enum/Box layout is read.
inline std::optional<IntegerContract> resolve_integer(const sdk::Graph& graph) {
    const auto entry=graph.exported("Bundle_get_i32");if(!entry)return {};
    const auto body=graph.body(*entry);if(!body)return {};
    const auto paths=home_native_provider::return_paths(graph,*body);if(!paths||paths->size()!=3)return {};
    const std::array<std::pair<unsigned,sdk::Value>,2> inputs={{{1,{sdk::Value::Constant,0,11}},{2,{sdk::Value::Constant,0,13}}}};
    unsigned success=0,absent=0;std::optional<unsigned> shift;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,false,false,path,inputs);
        if(!flow.returned||!flow.unknown.empty()||!home_native_provider::stack_only(flow)
            ||flow.calls.size()<2||flow.calls.size()>3||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}
            ||flow.calls[1].receiver!=sdk::Value{sdk::Value::Returned,0,0,8}
            ||flow.calls[1].parcel!=inputs[0].second||flow.calls[1].third!=inputs[1].second)return {};
        if(flow.calls.size()==3&&flow.calls[2].receiver!=sdk::Value{sdk::Value::Returned,0,1,8})return {};
        unsigned checks=0;
        for(const auto at:flow.branches) {
            const auto instruction=body->words[at];
            if((instruction&0xff00001fu)==0xb4000000u) {
                auto prefix=*body;prefix.words.resize(at);
                const auto checked=sdk::trace(graph,prefix,false,false,{},inputs);
                if(checked.regs[0].kind!=sdk::Value::Returned||checked.regs[0].number<1
                    ||checked.regs[0].number>2)return {};
                ++checks;
            }else if((instruction&0xfc000000u)!=0x14000000u)return {};
        }
        if(checks!=flow.calls.size()-1)return {};
        if(flow.regs[0]==sdk::Value{sdk::Value::Constant,0,1}){++absent;continue;}
        if(flow.calls.size()!=3)return {};
        std::optional<size_t> packing;
        for(const auto& load:flow.loads)if(load.address==sdk::Value{sdk::Value::Returned,0,2,8}) {
            if(packing||load.width!=4||load.index+1>=body->words.size())return {};
            const auto value=body->words[load.index],pack=body->words[load.index+1];
            if((value&0xffffffe0u)!=0xb9400000u||(pack&0xffc0001fu)!=0xd3400000u
                ||((pack>>5)&31)!=(value&31))return {};
            const unsigned r=(pack>>16)&63,s=(pack>>10)&63;
            if(!r||s+1!=r||64-r>32||shift)return {};
            shift=64-r;packing=load.index+1;
        }
        if(!packing)return {};
        auto suffix=path;const auto begin=std::find(suffix.begin(),suffix.end(),*packing);if(begin==suffix.end())return {};
        suffix.erase(suffix.begin(),begin+1);
        const std::array<std::pair<unsigned,sdk::Value>,1> result={{{0,{sdk::Value::Constant,0,0x1234}}}};
        const auto returned=sdk::trace(graph,*body,false,false,suffix,result);
        if(!returned.returned||!returned.calls.empty()||!returned.unknown.empty()||returned.regs[0]!=result[0].second)return {};
        ++success;
    }
    if(success!=1||absent!=2||!shift)return {};
    return IntegerContract{*entry,*shift};
}
}
