/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_surface_contract.h"

namespace home_native_surface {
// Native SDK FFI bridges, derived from the public SDK setters. Both accept a
// borrowed platform Transaction* and SurfaceControl*, not SDK Box addresses.
struct Effects {sdk::Address blur,corner,matrix;};
inline std::optional<sdk::Address> effect_bridge(const sdk::Graph& graph,const Contract& surface,
    std::string_view name,std::string_view platform,unsigned float_count) {
    const auto entry=graph.exported(name);if(!entry)return {};
    const sdk::Value integer{sdk::Value::Argument,0,2,4};
    const std::array<std::pair<unsigned,sdk::Value>,1> inputs={{{2,integer}}};
    const std::array<std::pair<unsigned,sdk::Value>,4> floats={{{0,{sdk::Value::Argument,0,0,4}},
        {1,{sdk::Value::Argument,0,1,4}},{2,{sdk::Value::Argument,0,2,4}},{3,{sdk::Value::Argument,0,3,4}}}};
    const auto forwards=[&](const auto& values) {
        for(unsigned i=0;i<float_count;++i)if(values[i]!=floats[i].second)return false;
        return true;
    };
    const auto arguments=[&](const sdk::Call& call) {
        return forwards(std::array{call.float0,call.float1,call.float2,call.float3});
    };
    auto at=*entry;std::optional<sdk::Body> body;
    for(unsigned hop=0;hop<5;++hop) {
        body=home_native_provider::body_or_leaf(graph,at);if(!body)return {};
        const auto forwarding=sdk::trace(graph,*body,false,false,{},inputs,floats);
        if(!forwarding.calls.empty())break;
        if(!forwarding.unknown.empty()||forwarding.branches.size()!=1
            ||forwarding.branches.front()!=body->words.size()-1
            ||forwarding.regs[0]!=sdk::Value{sdk::Value::Self}||forwarding.regs[1]!=sdk::Value{sdk::Value::Parcel}
            ||forwarding.regs[2]!=integer||!forwards(forwarding.floats))return {};
        const auto next=graph.callee(*body,body->words.size()-1,true);if(!next)return {};at=*next;
    }
    if(!body)return {};
    const auto complete=sdk::trace(graph,*body,false,false,{},inputs,floats);
    if(complete.calls.size()<3)return {};
    auto prefix=*body;prefix.words.resize(complete.calls[2].index+1);
    const auto calls=sdk::trace(graph,prefix,false,false,{},inputs,floats);
    if(!calls.unknown.empty()||calls.calls.size()!=3||!home_native_provider::stack_only(calls))return {};
    const auto& transaction=calls.calls[0];const auto& control=calls.calls[1];const auto& setter=calls.calls[2];
    if(!transaction.target||transaction.receiver!=sdk::Value{sdk::Value::Self}
        ||control.target!=surface.native_ref||control.receiver!=sdk::Value{sdk::Value::Parcel}
        ||setter.receiver!=sdk::Value{sdk::Value::Returned,0,0,8}
        ||setter.parcel!=sdk::Value{sdk::Value::Returned,0,1,8}||!setter.target)return {};
    if(float_count?!arguments(setter):setter.third!=integer)return {};
    if(calls.branches.size()!=2)return {};
    for(const auto index:calls.branches) {
        const auto word=prefix.words[index];if((word&0xff000000u)!=0xb4000000u)return {};
        auto before=prefix;before.words.resize(index);
        const auto branch=sdk::trace(graph,before,false,false,{},inputs,floats);
        const auto value=branch.regs[word&31];
        if(value!=sdk::Value{sdk::Value::Returned,0,0,8}&&value!=sdk::Value{sdk::Value::Returned,0,1,8})return {};
    }
    // The native Transaction accessor is a transparent borrow. No field is
    // manually read/written by the module, and no SDK allocation is invented.
    const auto borrow=home_native_provider::body_or_leaf(graph,*transaction.target);if(!borrow)return {};
    const auto borrowed=sdk::trace(graph,*borrow);
    if(!borrowed.returned||!borrowed.unknown.empty()||!borrowed.calls.empty()||!borrowed.branches.empty()
        ||!home_native_provider::stack_only(borrowed)||borrowed.regs[0]!=sdk::Value{sdk::Value::Field,0,0,8})return {};
    const auto wrapper=home_native_provider::body_or_leaf(graph,*setter.target);if(!wrapper||wrapper->words.empty())return {};
    auto front=*wrapper;front.words.pop_back();
    const std::array<std::pair<unsigned,sdk::Value>,2> wrapper_inputs={{{1,{sdk::Value::Argument,0,1,8}},{2,integer}}};
    const auto unwrapped=sdk::trace(graph,front,false,false,{},wrapper_inputs,floats);
    if(!unwrapped.unknown.empty()||!unwrapped.calls.empty()||!unwrapped.branches.empty()
        ||!home_native_provider::stack_only(unwrapped)||unwrapped.regs[0]!=sdk::Value{sdk::Value::Self}
        ||unwrapped.regs[1]!=sdk::Value{sdk::Value::IndirectField,0,1,8}
        ||unwrapped.regs[2]!=integer||!forwards(unwrapped.floats))return {};
    const auto bridge=graph.callee(*wrapper,wrapper->words.size()-1,true);if(!bridge)return {};
    const auto native=graph.body(*bridge);if(!native)return {};
    const auto paths=home_native_provider::return_paths(graph,*native);if(!paths)return {};
    bool retained=false;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*native,false,false,path,inputs,floats);
        if(!flow.returned||!flow.unknown.empty()||!home_native_provider::stack_only(flow))return {};
        const auto target=std::find_if(flow.calls.begin(),flow.calls.end(),[&](const auto& call){return call.linkage==platform;});
        if(target==flow.calls.end()||target->receiver!=sdk::Value{sdk::Value::Self}||target->parcel.kind!=sdk::Value::Stack)return {};
        if(float_count?!arguments(*target):target->third!=integer)return {};
        const auto storage=target->parcel;
        const bool owner=std::any_of(flow.stores.begin(),flow.stores.end(),[&](const auto& store){
            return store.address==storage&&store.value==sdk::Value{sdk::Value::Parcel,0,0,8}&&store.width==8;
        });
        if(!owner)return {};
        if(flow.calls.size()==3) {
            const auto& retain=flow.calls[0];const auto& release=flow.calls[2];
            if(target!=flow.calls.begin()+1||retain.linkage!="_ZNK7android7RefBase9incStrongEPKv"
                ||release.linkage!="_ZNK7android7RefBase9decStrongEPKv"
                ||retain.receiver!=sdk::Value{sdk::Value::Parcel}||retain.parcel!=storage
                ||release.receiver!=sdk::Value{sdk::Value::Parcel,0,0,8}||release.parcel!=storage)return {};
            retained=true;
        } else if(flow.calls.size()!=1&&flow.calls.size()!=2)return {};
    }
    if(!retained)return {};return bridge;
}
inline std::optional<Effects> resolve_effects(const sdk::Graph& graph,const Contract& surface) {
    const auto blur=effect_bridge(graph,surface,"SurfaceControlTransaction_set_background_blur_radius",
        "_ZN7android21SurfaceComposerClient11Transaction23setBackgroundBlurRadiusERKNS_2spINS_14SurfaceControlEEEi",0);
    const auto corner=effect_bridge(graph,surface,"SurfaceControlTransaction_set_corner_radius",
        "_ZN7android21SurfaceComposerClient11Transaction15setCornerRadiusERKNS_2spINS_14SurfaceControlEEEff",2);
    const auto matrix=effect_bridge(graph,surface,"SurfaceControlTransaction_set_matrix",
        "_ZN7android21SurfaceComposerClient11Transaction9setMatrixERKNS_2spINS_14SurfaceControlEEEffff",4);
    if(!blur||!corner||!matrix)return {};return Effects{*blur,*corner,*matrix};
}
}
