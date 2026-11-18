/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_surface_contract.h"

namespace home_native_surface {
struct Access {sdk::Address native,main;uint32_t native_field=0;};
inline std::optional<Access> resolve_access(const sdk::Graph& graph,const Contract& surface) {
    const auto native=graph.exported("SurfaceControl_as_native_surface_control"),
        main=graph.exported("SurfaceControl_get_main_surface_control");
    if(!native||!main)return {};
    const auto reference=graph.body(surface.native_ref);if(!reference)return {};
    const auto borrowed=sdk::trace(graph,*reference);
    if(!borrowed.returned||!borrowed.unknown.empty()||!borrowed.branches.empty()||!borrowed.calls.empty()
        ||!home_native_provider::stack_only(borrowed)||borrowed.regs[0].kind!=sdk::Value::Nullable
        ||borrowed.selects.size()!=1)return {};
    const auto& nullable=borrowed.selects.front();
    const auto field=borrowed.regs[0].offset;
    if(field<0||uint64_t(field)+8>surface.bytes||nullable.condition!=0
        ||nullable.yes.kind!=sdk::Value::Constant||nullable.yes.number
        ||nullable.no!=sdk::Value{sdk::Value::Self,field}
        ||nullable.field!=sdk::Value{sdk::Value::Field,field,0,8}
        ||nullable.value.kind!=sdk::Value::Constant||nullable.value.number)return {};
    const auto reader=graph.body(surface.reader);if(!reader)return {};
    const auto decoded=sdk::trace(graph,*reader);unsigned native_writes=0;
    for(const auto& store:decoded.stores)if(store.address==sdk::Value{sdk::Value::Self,field}&&store.width==8
        &&store.value.kind==sdk::Value::Result&&store.value.width==8)++native_writes;
    if(native_writes!=1)return {};
    const auto accessor=graph.body(*native);if(!accessor)return {};
    const auto unwrap=sdk::trace(graph,*accessor);
    if(!unwrap.returned||!unwrap.unknown.empty()||unwrap.calls.size()!=1||unwrap.branches.size()!=2
        ||unwrap.calls[0].target!=surface.native_ref||unwrap.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||!home_native_provider::stack_only(unwrap))return {};
    const auto null_branch=unwrap.branches[0],tail=unwrap.branches[1];
    // Supported AAPCS64 adapter: nullable borrowed sp reference in X0,
    // CBZ X0 to the null return; otherwise tail-call sp::deref. No raw SDK
    // field is read by the caller and no new strong reference is manufactured.
    const auto condition=accessor->words[null_branch];
    if((condition&0xff00001fu)!=0xb4000000u||null_branch<=unwrap.calls[0].index||tail<=null_branch)return {};
    const int64_t zero=int64_t(null_branch)+(int32_t(condition<<8)>>13);
    if(zero<=int64_t(tail)||zero>=int64_t(accessor->words.size()))return {};
    const auto deref=graph.callee(*accessor,tail,true);if(!deref)return {};
    auto forward=*accessor;forward.words.resize(tail);const auto args=sdk::trace(graph,forward);
    if(args.regs[0]!=sdk::Value{sdk::Value::Returned,0,0,8})return {};
    std::vector<size_t> absent;
    for(size_t i=0;i<=null_branch;++i)absent.push_back(i);
    for(size_t i=static_cast<size_t>(zero);i<accessor->words.size();++i)absent.push_back(i);
    const auto no_value=sdk::trace(graph,*accessor,false,false,absent);
    if(!no_value.returned||!no_value.unknown.empty()||no_value.calls.size()!=1
        ||no_value.regs[0]!=sdk::Value{sdk::Value::Returned,0,0,8})return {};
    const auto pointer=home_native_provider::body_or_leaf(graph,*deref);if(!pointer)return {};
    const auto raw=sdk::trace(graph,*pointer);
    if(!raw.returned||!raw.calls.empty()||!raw.branches.empty()||!raw.unknown.empty()
        ||!home_native_provider::stack_only(raw)||raw.regs[0]!=sdk::Value{sdk::Value::Field,0,0,8})return {};

    const auto main_body=graph.body(*main);if(!main_body)return {};
    const auto output=sdk::trace(graph,*main_body);
    if(!output.returned||!output.branches.empty()||output.calls.size()!=1||!output.calls[0].target
        ||output.calls[0].output.kind!=sdk::Value::Stack||output.unknown.size()!=1||output.selects.size()!=1
        ||!home_native_provider::stack_only(output))return {};
    const auto index=output.unknown.front();if(index<3)return {};
    const auto join=main_body->words[index],shift_word=main_body->words[index-3];
    const auto& selected=output.selects.front();const auto low=output.regs[(join>>16)&31];
    auto before=*main_body;before.words.resize(index-3);const auto inputs=sdk::trace(graph,before);
    const auto high=inputs.regs[(shift_word>>5)&31];
    // Public two-register result: packed (u32 tag, i32 error) plus an owned
    // SDK pointer. Derive private sret offsets from the adapter's data flow.
    if((join&0xffe0fc1fu)!=0xaa000000u||((join>>5)&31)!=selected.destination
        ||(shift_word&0xffc00000u)!=0xd3400000u||((shift_word>>16)&63)!=32||((shift_word>>10)&63)!=31
        ||(shift_word&31)!=selected.destination||selected.condition!=1
        ||selected.no.kind!=sdk::Value::Constant||selected.no.number
        ||selected.field!=low||selected.value.kind!=sdk::Value::Constant||selected.value.number
        ||low.kind!=sdk::Value::Result||low.width!=4||low.number!=0
        ||high.kind!=sdk::Value::Result||high.width!=4||high.number!=0||high.offset!=low.offset+4
        ||output.regs[1].kind!=sdk::Value::Result||output.regs[1].width!=8||output.regs[1].number!=0
        ||output.regs[1].offset<high.offset+4)return {};
    const auto producer=graph.body(*output.calls[0].target);if(!producer)return {};
    const auto ownership=sdk::trace(graph,*producer,false,true);unsigned owned=0,success=0,unavailable=0;
    for(const auto& store:ownership.stores)if(store.address.kind==sdk::Value::Self) {
        if(store.address.offset==output.regs[1].offset&&store.width==8&&store.value.kind==sdk::Value::Returned
            &&store.value.number<ownership.calls.size()&&ownership.calls[store.value.number].target==surface.constructor)++owned;
        else if(store.address.offset==low.offset&&store.width==4&&store.value.kind==sdk::Value::Constant&&!store.value.number)++success;
        else if(store.address.offset==low.offset&&store.width==8&&store.value.kind==sdk::Value::Constant
            &&uint32_t(store.value.number)!=0)++unavailable;
        else return {};
    }
    if(owned!=1||success!=1||!unavailable)return {};
    return Access{*native,*main,static_cast<uint32_t>(field)};
}
}
