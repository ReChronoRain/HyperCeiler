/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_parcel_contract.h"

namespace home_native_surface {
namespace sdk=home_layout::cards::sdk;
struct Contract {
    sdk::Address make,constructor,drop,allocator,valid,read,native_ref,reader;
    uint32_t bytes=0,alignment=0;
    uint32_t status_shift=0,error_tag=0;
};
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto make=graph.exported("SurfaceControl_default"),drop=graph.exported("SurfaceControl_destruct"),
        valid=graph.exported("SurfaceControl_is_valid"),
        read=graph.exported("SurfaceControl_read_from_parcel");
    if(!make||!drop||!valid||!read)return {};
    const auto make_body=graph.body(*make),drop_body=graph.body(*drop);
    if(!make_body||!drop_body||drop_body->words.empty())return {};
    const auto factory=sdk::trace(graph,*make_body);
    if(!factory.returned||!factory.unknown.empty()||!factory.branches.empty()||factory.calls.size()!=1
        ||factory.regs[0].kind!=sdk::Value::Returned||factory.regs[0].number!=0||!factory.calls[0].target
        ||factory.calls[0].receiver.kind!=sdk::Value::Stack||!home_native_provider::stack_only(factory))return {};
    const auto constructor=graph.body(*factory.calls[0].target);if(!constructor)return {};
    const auto paths=home_native_provider::return_paths(graph,*constructor);if(!paths||paths->size()!=1)return {};
    const auto owns=sdk::trace(graph,*constructor,false,false,paths->front());
    if(!owns.returned||!owns.unknown.empty()||owns.regs[0].kind!=sdk::Value::Returned
        ||owns.regs[0].number>=owns.calls.size())return {};
    const auto& allocation=owns.calls[owns.regs[0].number];
    if(!allocation.target||allocation.receiver.kind!=sdk::Value::Constant||allocation.receiver.number==0
        ||allocation.receiver.number>4096||allocation.parcel.kind!=sdk::Value::Constant||allocation.parcel.number!=8)return {};
    const auto size=static_cast<uint32_t>(allocation.receiver.number);std::vector<bool> copied(size);
    for(const auto& store:owns.stores) {
        if(store.address.kind==sdk::Value::Stack)continue;
        if(store.address.kind!=sdk::Value::Returned||store.address.number!=owns.regs[0].number
            ||store.address.offset<0||uint64_t(store.address.offset)+store.width>size
            ||store.value.kind!=sdk::Value::Field||store.value.offset!=store.address.offset||store.value.width!=store.width)return {};
        for(unsigned byte=0;byte<store.width;++byte) {
            const auto at=static_cast<size_t>(store.address.offset)+byte;if(copied[at])return {};copied[at]=true;
        }
    }
    if(!std::all_of(copied.begin(),copied.end(),[](bool byte){return byte;}))return {};
    const auto tail=graph.raw_callee(*drop_body,drop_body->words.size()-1,true);if(!tail||!graph.linked(*tail))return {};
    auto prefix=*drop_body;prefix.words.pop_back();const auto destruction=sdk::trace(graph,prefix);
    if(!destruction.unknown.empty()||!destruction.branches.empty()||destruction.calls.size()!=1
        ||!home_native_provider::stack_only(destruction)||destruction.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||destruction.regs[0]!=sdk::Value{sdk::Value::Self}||destruction.regs[1]!=allocation.receiver
        ||destruction.regs[2]!=allocation.parcel)return {};
    const auto valid_body=graph.body(*valid);if(!valid_body||valid_body->words.empty())return {};
    const auto valid_tail=graph.callee(*valid_body,valid_body->words.size()-1,true);if(!valid_tail)return {};
    auto valid_prefix=*valid_body;valid_prefix.words.pop_back();const auto forwarded=sdk::trace(graph,valid_prefix);
    if(!forwarded.calls.empty()||!forwarded.branches.empty()||!forwarded.unknown.empty()
        ||forwarded.regs[0]!=sdk::Value{sdk::Value::Self})return {};
    const auto predicate=graph.body(*valid_tail);if(!predicate)return {};
    const auto boolean=sdk::trace(graph,*predicate);
    if(!boolean.returned||!boolean.unknown.empty()||!boolean.branches.empty()||boolean.calls.size()!=1
        ||!boolean.calls[0].target||boolean.calls[0].receiver!=sdk::Value{sdk::Value::Self}||boolean.regs[0].kind!=sdk::Value::Selected
        ||boolean.regs[0].number>=boolean.selects.size())return {};
    const auto& selected=boolean.selects[boolean.regs[0].number];
    if(selected.yes.kind!=sdk::Value::Constant||selected.yes.number||selected.no.kind!=sdk::Value::Constant
        ||selected.no.number!=1||selected.condition!=0||selected.field.kind!=sdk::Value::Returned
        ||selected.field.number!=0||selected.value.kind!=sdk::Value::Constant||selected.value.number)return {};
    const auto read_body=graph.body(*read);if(!read_body)return {};
    const auto reading=sdk::trace(graph,*read_body);
    if(!reading.returned||reading.calls.size()!=1||!reading.calls[0].target
        ||reading.calls[0].receiver!=sdk::Value{sdk::Value::Field,0,0,8}
        ||reading.calls[0].parcel!=sdk::Value{sdk::Value::Parcel}
        ||!home_native_provider::stack_only(reading)||reading.unknown.size()!=1||reading.selects.size()!=1)return {};
    const auto packed=read_body->words[reading.unknown.front()];const auto& status=reading.selects.front();
    const auto shift=(packed>>10)&63;
    if((packed&0xff20001fu)!=0xaa000000u||shift!=32||(packed>>22&3)!=0
        ||status.destination!=((packed>>5)&31)||((packed>>16)&31)!=0||status.condition!=0
        ||status.yes.kind!=sdk::Value::Constant||status.yes.number||status.no.kind!=sdk::Value::Constant
        ||status.no.number!=1||status.field!=sdk::Value{sdk::Value::Returned,0,0,4}
        ||status.value.kind!=sdk::Value::Constant||status.value.number!=1)return {};
    const auto reader=graph.body(*reading.calls[0].target);if(!reader)return {};
    const auto fields=sdk::trace(graph,*reader);
    if(fields.calls.empty()||!fields.calls[0].target||fields.calls[0].receiver!=sdk::Value{sdk::Value::Parcel})return {};
    const auto primitive=home_native_parcel::initial_read(graph,*fields.calls[0].target,"AParcel_readInt32");
    // Both public arguments are references to one pointer: &Unique<SC> and
    // &BorrowedParcel. Neither is the raw object expected by is_valid/destruct.
    if(!primitive||primitive->receiver!=sdk::Value{sdk::Value::Field,0,0,8}
        ||primitive->parcel.kind!=sdk::Value::Stack)return {};
    return Contract{*make,*factory.calls[0].target,*drop,*allocation.target,*valid,*read,*boolean.calls[0].target,
        *reading.calls[0].target,size,8,shift,1};
}
}
