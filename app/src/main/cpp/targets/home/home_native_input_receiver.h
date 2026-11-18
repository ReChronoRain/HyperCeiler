/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_callback.h"

namespace home_native_input {
struct ReceiverResultContract {
    sdk::Address create,drop,runtime_deref,error_table;
    home_native_provider::ByteVectorDrop error_drop;
    uint32_t bytes=0,tag=0,pointer=0,error_at=0,manager_bytes=0,manager_alignment=0;
    uint8_t accepted=0,rejected=0;
};

// Resolve ownership of the factory's indirect C result before any receiver
// may be registered. This does not authorize event completion on its manager
// pointer; actual callbacks supply a different, borrowed receiver object.
inline std::optional<ReceiverResultContract> receiver_result(const sdk::Graph& graph) {
    const auto table=receiver_table(graph);const auto carrier=callback_drop(graph);
    const auto create=graph.exported("input_InputEventReceiver_new"),drop=graph.exported("input_InputEventReceiver_drop");
    if(!table||!carrier||!create||!drop)return {};
    const auto body=graph.body(*create),destructor=graph.body(*drop);
    if(!body||!destructor||destructor->words.empty())return {};
    const auto paths=home_native_provider::return_paths(graph,*body);
    if(!paths||paths->size()!=4)return {};
    const std::array<std::pair<unsigned,sdk::Value>,3> inputs={{{0,{sdk::Value::Argument,0,100,8}},
        {1,{sdk::Value::Argument,0,101,8}},{2,{sdk::Value::Argument,0,102,8}}}};
    ReceiverResultContract result;result.create=*create;result.drop=*drop;
    unsigned successes=0,errors=0;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,false,true,path,inputs);
        if(!flow.returned||!flow.unknown.empty()||flow.calls.empty()||!flow.calls[0].target
            ||flow.calls[0].receiver!=inputs[0].second)return {};
        const auto deref=graph.body(*flow.calls[0].target);if(!deref)return {};
        const auto handle=sdk::trace(graph,*deref);
        if(!handle.returned||handle.regs[0]!=sdk::Value{sdk::Value::Field,0,0,8}
            ||!handle.unknown.empty()||!handle.calls.empty()||!handle.branches.empty()
            ||!home_native_provider::stack_only(handle))return {};
        for(const auto& load:handle.loads)if(load.address.kind!=sdk::Value::Stack
            &&(load.address!=sdk::Value{sdk::Value::Self}||load.width!=8))return {};
        if(successes+errors&&result.runtime_deref!=*flow.calls[0].target)return {};
        result.runtime_deref=*flow.calls[0].target;
        std::optional<size_t> producer;
        for(size_t i=0;i<flow.calls.size();++i)if(flow.calls[i].fourth==
            sdk::Value{sdk::Value::Literal,int64_t(table->table.image),table->table.va,8}) {
            if(producer)return {};producer=i;
        }
        if(!producer)return {};
        const auto& construct=flow.calls[*producer];
        if(!construct.target||construct.output.kind!=sdk::Value::Stack||construct.receiver.kind!=sdk::Value::Stack
            ||construct.parcel.kind!=sdk::Value::Returned||construct.parcel.number||construct.parcel.offset<=0
            ||construct.third.kind!=sdk::Value::Returned)return {};
        std::optional<sdk::Store> tag,owned,literal;
        uint32_t extent=0;
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
            if(store.address.offset<0||store.address.offset>256||store.width>256-uint64_t(store.address.offset))return {};
            extent=std::max(extent,uint32_t(store.address.offset+store.width));
            if(store.width==1&&store.value.kind==sdk::Value::Constant&&store.value.number<=UINT8_MAX) {
                if(tag)return {};tag=store;
            } else if(store.width==8&&store.value.kind==sdk::Value::Returned) {
                if(owned)return {};owned=store;
            } else if(store.width==8&&store.value.kind==sdk::Value::Literal) {
                if(literal)return {};literal=store;
            }
        }
        if(!tag||bool(owned)==bool(literal))return {};
        if(successes+errors&&result.tag!=tag->address.offset)return {};
        result.tag=uint32_t(tag->address.offset);
        if(owned) {
            if(owned->value.number>=flow.calls.size()||owned->value.offset)return {};
            const auto& allocation=flow.calls[owned->value.number];
            if(!allocation.target||allocation.receiver.kind!=sdk::Value::Constant||!allocation.receiver.number
                ||allocation.receiver.number>4096
                ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
            const auto bytes=uint32_t(allocation.receiver.number);std::vector<bool> copied(bytes);
            for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Returned
                &&store.address.number==owned->value.number) {
                if(store.address.offset<0||uint64_t(store.address.offset)+store.width>bytes
                    ||store.value.kind!=sdk::Value::Result||store.value.number!=*producer
                    ||store.value.offset!=store.address.offset||store.value.width!=store.width)return {};
                for(unsigned byte=0;byte<store.width;++byte) {
                    const auto index=size_t(store.address.offset)+byte;if(copied[index])return {};copied[index]=true;
                }
            }
            if(!std::all_of(copied.begin(),copied.end(),[](bool value){return value;}))return {};
            if(successes&&(result.pointer!=owned->address.offset||result.manager_bytes!=bytes
                ||result.manager_alignment!=allocation.parcel.number||result.accepted!=tag->value.number))return {};
            for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self
                &&store.index!=owned->index&&store.index!=tag->index)return {};
            result.pointer=uint32_t(owned->address.offset);result.manager_bytes=bytes;
            result.manager_alignment=uint32_t(allocation.parcel.number);result.accepted=uint8_t(tag->value.number);
            ++successes;continue;
        }
        const sdk::Address error_table{size_t(literal->value.offset),literal->value.number};
        const auto release=home_native_provider::table_drop(graph,error_table);
        if(!release||literal->address.offset<int64_t(release->table_field))return {};
        const auto at=uint32_t(literal->address.offset-release->table_field);
        // The supported repr(C) RVec is four native words; the selected drop
        // proves each payload word and the remaining destructor-table field.
        if(extent!=at+4*sizeof(void*)||result.tag>=at)return {};
        std::optional<size_t> formatter;
        for(size_t i=*producer+1;i<flow.calls.size();++i) {
            const auto& call=flow.calls[i];
            if(call.output.kind!=sdk::Value::Stack||call.parcel.kind!=sdk::Value::Stack
                ||call.receiver.kind!=sdk::Value::Stack)continue;
            bool vector_ref=false,writer_table=false;
            std::array<std::optional<uint64_t>,3> empty;
            for(const auto& store:flow.stores)if(store.index<call.index&&store.address.kind==sdk::Value::Stack) {
                const auto delta=store.address.offset-call.output.offset;
                if(delta>=0&&uint64_t(delta)<release->input.size()*sizeof(void*)&&delta%8==0&&store.width==8
                    &&store.value.kind==sdk::Value::Constant)empty[size_t(delta/8)]=store.value.number;
                if(store.address==call.parcel&&store.width==8&&store.value.kind==sdk::Value::Stack
                    &&store.value.offset==call.output.offset)vector_ref=true;
                if(store.address==sdk::plus(call.parcel,8)&&store.width==8&&store.value.kind==sdk::Value::Literal)writer_table=true;
            }
            if(!vector_ref||!writer_table||empty[0]!=0||!empty[1]||!*empty[1]||empty[2]!=0)continue;
            if(formatter)return {};formatter=i;
        }
        if(!formatter)return {};
        const auto& format=flow.calls[*formatter];
        const auto end=std::find(path.begin(),path.end(),format.index);
        if(end==path.end())return {};
        const std::vector<size_t> before_format(path.begin(),end+1),after_format(end+1,path.end());
        const auto state=sdk::trace(graph,*body,false,true,before_format,inputs);
        std::vector<std::pair<unsigned,sdk::Value>> moved;
        // Rebase the observed frame aliases onto the formatted native Vec.
        // SP scratch spills start afresh; no pre-call spill is trusted. This
        // tracks the actual field moves even if formatting is an opaque call.
        for(unsigned reg=0;reg<31;++reg) {
            auto value=state.regs[reg];
            if(value.kind==sdk::Value::Stack)value={sdk::Value::Self,value.offset-format.output.offset};
            else if(value.kind==sdk::Value::Self)value={sdk::Value::Argument,value.offset,200,8};
            moved.push_back({reg,value});
        }
        const auto payload=sdk::trace(graph,*body,false,false,after_format,moved);
        if(!payload.unknown.empty())return {};
        for(size_t word=0;word<release->input.size();++word) {
            const auto field=at+release->input[word];bool found=false;
            for(const auto& store:payload.stores)if(store.address.kind==sdk::Value::Argument&&store.address.number==200
                &&store.address.offset<=field&&uint64_t(store.address.offset)+store.width>=field+8) {
                if(found||store.value.kind!=sdk::Value::Field
                    ||store.value.offset+int64_t(field)-store.address.offset!=int64_t(word*8))return {};
                found=true;
            }
            if(!found)return {};
        }
        std::vector<bool> copied(extent-at);
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self&&store.index!=tag->index) {
            if(store.address.offset<at||uint64_t(store.address.offset)+store.width>extent)return {};
            for(unsigned byte=0;byte<store.width;++byte) {
                const auto index=size_t(store.address.offset-at)+byte;if(copied[index])return {};copied[index]=true;
            }
        }
        if(!std::all_of(copied.begin(),copied.end(),[](bool value){return value;}))return {};
        if(errors&&(result.error_table!=error_table||result.error_at!=at||result.bytes!=extent
            ||result.error_drop.drop!=release->drop||result.rejected!=tag->value.number))return {};
        result.error_table=error_table;result.error_drop=*release;result.error_at=at;
        result.bytes=extent;result.rejected=uint8_t(tag->value.number);++errors;
    }
    if(successes!=2||errors!=2||result.accepted==result.rejected
        ||result.pointer+sizeof(void*)>result.bytes
        ||(result.tag>=result.pointer&&result.tag<result.pointer+sizeof(void*)))return {};
    const auto tail=graph.raw_callee(*destructor,destructor->words.size()-1,true);
    if(!tail||!graph.linked(*tail))return {};
    auto prefix=*destructor;prefix.words.pop_back();const auto release=sdk::trace(graph,prefix);
    if(release.calls.empty()||release.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||release.regs[0]!=sdk::Value{sdk::Value::Self}
        ||release.regs[1]!=sdk::Value{sdk::Value::Constant,0,result.manager_bytes}
        ||release.regs[2]!=sdk::Value{sdk::Value::Constant,0,result.manager_alignment})return {};
    for(const auto& load:release.loads)if(load.address.kind==sdk::Value::Self
        &&(load.address.offset<0||uint64_t(load.address.offset)+load.width>result.manager_bytes))return {};
    return result;
}
}
