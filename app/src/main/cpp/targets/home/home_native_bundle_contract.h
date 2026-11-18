/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_provider_contract.h"

namespace home_native_bundle {
namespace sdk=home_layout::cards::sdk;
struct Contract {
    sdk::Address make,drop,get_boolean,allocator;
    uint32_t bytes=0,alignment=0,boolean_shift=0;
};
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto make=graph.exported("Bundle_default"),drop=graph.exported("Bundle_drop"),
        get=graph.exported("Bundle_get_boolean");
    if(!make||!drop||!get)return {};
    const auto make_body=graph.body(*make),drop_body=graph.body(*drop),get_body=graph.body(*get);
    if(!make_body||!drop_body||!get_body||drop_body->words.empty())return {};
    const auto paths=home_native_provider::return_paths(graph,*make_body);
    if(!paths||paths->size()!=1)return {};
    const auto creation=sdk::trace(graph,*make_body,false,false,paths->front());
    if(!creation.returned||!creation.unknown.empty()||creation.regs[0].kind!=sdk::Value::Returned)return {};
    const auto allocation=creation.regs[0].number;
    if(allocation>=creation.calls.size())return {};
    const auto& alloc=creation.calls[allocation];
    if(!alloc.target||alloc.receiver.kind!=sdk::Value::Constant||alloc.parcel.kind!=sdk::Value::Constant
        ||alloc.receiver.number==0||alloc.receiver.number>4096||alloc.parcel.number!=8)return {};
    const auto size=static_cast<uint32_t>(alloc.receiver.number);
    unsigned copies=0;
    for(const auto& call:creation.calls)if(call.linkage=="memcpy"&&call.receiver==creation.regs[0]
        &&call.parcel.kind==sdk::Value::Stack&&call.third.kind==sdk::Value::Constant&&call.third.number==size)++copies;
    if(copies!=1||!home_native_provider::stack_only(creation))return {};
    const auto tail=graph.raw_callee(*drop_body,drop_body->words.size()-1,true);if(!tail||!graph.linked(*tail))return {};
    auto prefix=*drop_body;prefix.words.pop_back();const auto destruction=sdk::trace(graph,prefix);
    if(!destruction.unknown.empty()||!destruction.branches.empty()||destruction.calls.size()!=1
        ||!home_native_provider::stack_only(destruction)||destruction.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||destruction.regs[0]!=sdk::Value{sdk::Value::Self}||destruction.regs[1].kind!=sdk::Value::Constant
        ||destruction.regs[1].number!=size||destruction.regs[2]!=alloc.parcel)return {};
    // Supported AAPCS64 getter returns two packed bytes in X0: a CSET None
    // flag and a masked bool. Derive the packing, do not assume byte positions.
    const std::array<std::pair<unsigned,sdk::Value>,2> inputs={{{1,{sdk::Value::Constant,0,11}},
        {2,{sdk::Value::Constant,0,13}}}};
    const auto getter=sdk::trace(graph,*get_body,false,false,{},inputs);
    if(getter.calls.size()!=3||getter.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||getter.calls[1].parcel!=inputs[0].second||getter.calls[1].third!=inputs[1].second
        ||!home_native_provider::stack_only(getter))return {};
    std::optional<uint32_t> shift;
    for(size_t i=3;i<get_body->words.size();++i) {
        const auto word=get_body->words[i];
        if((word&0xff20001fu)!=0xaa000000u||(word>>22&3)!=0)continue;
        const auto comparison=get_body->words[i-3],mask=get_body->words[i-2],flag=get_body->words[i-1];
        const unsigned low=(word>>5)&31,value=(word>>16)&31,source=(comparison>>5)&31;
        const auto bits=sdk::logical_immediate(mask);
        if((comparison&0xffc0001fu)!=0x7100001fu||((comparison>>10)&4095)<=1
            ||((comparison>>10)&4095)>255||(mask&0xff800000u)!=0x12000000u||!bits||*bits!=1
            ||(mask&31)!=value||((mask>>5)&31)!=source||(flag&0xffffffe0u)!=0x1a9f17e0u
            ||(flag&31)!=low||((word>>10)&63)==0||((word>>10)&63)>=16||shift)return {};
        if(getter.unknown.size()!=1||getter.unknown.front()!=i)return {};
        shift=(word>>10)&63;
    }
    if(!shift)return {};
    return Contract{*make,*drop,*get,*alloc.target,size,8,*shift};
}
// Match the owned Bundle returned by ContentProviderClient_call to the public
// constructor's allocator/size and drop API. No raw object is freed by us.
inline bool call_owner(const sdk::Graph& graph,const home_native_provider::CallContract& call,const Contract& bundle) {
    const auto body=graph.body(call.call);if(!body)return false;
    const auto paths=home_native_provider::return_paths(graph,*body);if(!paths)return false;
    unsigned matched=0;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,true,true,path);
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self
            &&store.address.offset==call.pointer&&store.width==8&&store.value.kind==sdk::Value::Returned) {
            if(store.value.number>=flow.calls.size())return false;
            const auto target=flow.calls[store.value.number].target;if(!target)return false;
            const auto maker=graph.body(*target);if(!maker)return false;
            std::optional<size_t> copy;
            for(size_t i=0;i<maker->words.size();++i)if(const auto tail=graph.raw_callee(*maker,i,true)) {
                if(graph.import_name(*tail)==std::optional<std::string_view>("memcpy")) {
                    if(copy)return false;copy=i;
                }
            }
            if(!copy)return false;
            auto prefix=*maker;prefix.words.resize(*copy);const auto owns=sdk::trace(graph,prefix);
            if(!owns.unknown.empty()||!home_native_provider::stack_only(owns)
                ||owns.branches.size()!=1
                ||owns.regs[0].kind!=sdk::Value::Returned||owns.regs[1]!=sdk::Value{sdk::Value::Self}
                ||owns.regs[2].kind!=sdk::Value::Constant||owns.regs[2].number!=bundle.bytes
                ||owns.regs[0].number>=owns.calls.size())return false;
            const auto check=owns.branches.front();const auto instruction=prefix.words[check];
            if((instruction&0xff000000u)!=0xb4000000u)return false; // CBZ on an allocation pointer.
            auto before=prefix;before.words.resize(check);const auto checked=sdk::trace(graph,before);
            const int64_t failure=int64_t(check)+(int32_t(instruction<<8)>>13);
            if(checked.regs[instruction&31]!=owns.regs[0]||failure<=int64_t(*copy)
                ||failure>=int64_t(maker->words.size()))return false;
            const auto& allocation=owns.calls[owns.regs[0].number];
            if(allocation.target!=bundle.allocator||allocation.receiver!=owns.regs[2]
                ||allocation.parcel.kind!=sdk::Value::Constant||allocation.parcel.number!=bundle.alignment)return false;
            ++matched;
        }
    }
    return matched==1;
}
}
