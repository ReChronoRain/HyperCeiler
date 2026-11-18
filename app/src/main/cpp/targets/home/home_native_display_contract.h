/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_provider_contract.h"
#include "home_native_context_contract.h"

namespace home_native_display {
namespace sdk=home_layout::cards::sdk;
struct Contract {
    sdk::Address configuration,drop_configuration,metrics,drop_metrics,width,height,density,ui_mode;
    uint32_t bytes=0,alignment=0,configuration_bytes=0;
};
inline std::optional<Contract> resolve(const sdk::Graph& graph) {
    const auto configuration=graph.exported("context_get_configuration"),drop_configuration=graph.exported("Configuration_drop"),
        metrics=graph.exported("DisplayMetrics_get_metrics"),drop_metrics=graph.exported("DisplayMetrics_drop"),
        width=graph.exported("DisplayMetrics_get_width_pixels"),height=graph.exported("DisplayMetrics_get_height_pixels"),
        density=graph.exported("DisplayMetrics_get_density"),ui_mode=graph.exported("Configuration_get_ui_mode");
    if(!configuration||!drop_configuration||!metrics||!drop_metrics||!width||!height||!density||!ui_mode
        ||!home_native_context::resolve(graph))return {};
    const auto source=graph.body(*configuration);if(!source)return {};
    const auto snapshot=sdk::trace(graph,*source);
    if(!snapshot.returned||!snapshot.unknown.empty()||!snapshot.branches.empty()||snapshot.calls.size()!=2
        ||snapshot.calls[0].receiver!=sdk::Value{sdk::Value::Self}||!snapshot.calls[0].target
        ||snapshot.calls[0].output.kind!=sdk::Value::Stack||snapshot.calls[1].receiver!=snapshot.calls[0].output
        ||!snapshot.calls[1].target||snapshot.regs[0]!=sdk::Value{sdk::Value::Returned,0,1,8}
        ||!home_native_provider::stack_only(snapshot))return {};
    const auto constructor=graph.body(*snapshot.calls[1].target);if(!constructor)return {};
    std::optional<size_t> copy;
    for(size_t i=0;i<constructor->words.size();++i) {
        const auto target=graph.raw_callee(*constructor,i,true);
        if(target&&graph.import_name(*target)==std::optional<std::string_view>("memcpy")) {
            if(copy)return {};copy=i;
        }
    }
    if(!copy)return {};
    auto prefix=*constructor;prefix.words.resize(*copy);const auto owned=sdk::trace(graph,prefix);
    if(!owned.unknown.empty()||owned.calls.size()!=2||owned.regs[0].kind!=sdk::Value::Returned
        ||owned.regs[0].number>=owned.calls.size()||owned.regs[1]!=sdk::Value{sdk::Value::Self}
        ||owned.regs[2].kind!=sdk::Value::Constant||owned.regs[2].number==0||owned.regs[2].number>4096
        ||!home_native_provider::stack_only(owned)||owned.branches.size()!=1)return {};
    const auto& allocation=owned.calls[owned.regs[0].number];
    if(!allocation.target||allocation.receiver!=owned.regs[2]
        ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
    const auto condition=prefix.words[owned.branches.front()];
    const int64_t failed=int64_t(owned.branches.front())+(int32_t(condition<<8)>>13);
    if((condition&0xff00001fu)!=0xb4000000u||failed<=int64_t(*copy)||failed>=int64_t(constructor->words.size()))return {};
    const auto configuration_bytes=static_cast<uint32_t>(allocation.receiver.number);
    const auto drop=graph.body(*drop_configuration);if(!drop||drop->words.empty())return {};
    const auto free_configuration=graph.callee(*drop,drop->words.size()-1,true);if(!free_configuration)return {};
    auto dropping=*drop;dropping.words.pop_back();const auto release=sdk::trace(graph,dropping);
    if(release.regs[0]!=sdk::Value{sdk::Value::Self}||release.regs[1]!=allocation.receiver||release.regs[2]!=allocation.parcel)return {};
    // Configuration is the real Context snapshot, not an invented/default
    // object. DisplayMetrics_get_metrics explicitly supplies displayId zero.
    // Display_get_real_metrics is a different two-input API and is not called.
    const auto make=graph.body(*metrics);if(!make)return {};
    const auto alternatives=home_native_provider::return_paths(graph,*make);if(!alternatives||alternatives->size()!=1)return {};
    const auto measured=sdk::trace(graph,*make,false,false,alternatives->front());
    if(!measured.returned||!measured.unknown.empty()||measured.calls.empty()||!measured.calls[0].target
        ||measured.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||measured.calls[0].parcel!=sdk::Value{sdk::Value::Constant}||measured.calls[0].output.kind!=sdk::Value::Stack
        ||measured.regs[0].kind!=sdk::Value::Returned||measured.regs[0].number>=measured.calls.size())return {};
    const auto implementation=graph.body(*measured.calls[0].target);if(!implementation)return {};
    const auto consumed=sdk::trace(graph,*implementation);unsigned config_reads=0;
    for(const auto& load:consumed.loads)if(load.address.kind==sdk::Value::Self) {
        if(load.address.offset<0||uint64_t(load.address.offset)+load.width>configuration_bytes)return {};
        ++config_reads;
    }
    if(config_reads<2)return {};
    const auto& memory=measured.calls[measured.regs[0].number];
    if(!memory.target||memory.receiver.kind!=sdk::Value::Constant||memory.receiver.number==0||memory.receiver.number>4096
        ||memory.parcel.kind!=sdk::Value::Constant||memory.parcel.number!=alignof(float))return {};
    const auto size=static_cast<uint32_t>(memory.receiver.number);std::vector<bool> copied(size);
    for(const auto& store:measured.stores) {
        if(store.address.kind==sdk::Value::Stack)continue;
        if(store.address.kind!=sdk::Value::Returned||store.address.number!=measured.regs[0].number||store.address.offset<0
            ||uint64_t(store.address.offset)+store.width>size||store.value.kind!=sdk::Value::Result||store.value.number!=0
            ||store.value.offset!=store.address.offset||store.value.width!=store.width)return {};
        for(unsigned i=0;i<store.width;++i)copied[static_cast<size_t>(store.address.offset)+i]=true;
    }
    if(!std::all_of(copied.begin(),copied.end(),[](bool byte){return byte;}))return {};
    const auto metrics_drop=graph.body(*drop_metrics);if(!metrics_drop||metrics_drop->words.empty())return {};
    const auto free_metrics=graph.callee(*metrics_drop,metrics_drop->words.size()-1,true);
    if(free_metrics!=free_configuration)return {};
    auto freeing=*metrics_drop;freeing.words.pop_back();const auto freed=sdk::trace(graph,freeing);
    if(!freed.calls.empty()||!freed.unknown.empty()||!freed.branches.empty()||freed.regs[0]!=sdk::Value{sdk::Value::Self}
        ||freed.regs[1]!=memory.receiver||freed.regs[2]!=memory.parcel)return {};
    std::vector<int64_t> fields;
    for(const auto& entry:{*width,*height,*density}) {
        const auto getter=graph.body(entry);if(!getter)return {};
        const auto scalar=sdk::trace(graph,*getter);
        if(!scalar.returned||!scalar.calls.empty()||!scalar.branches.empty()||!scalar.unknown.empty()
            ||!home_native_provider::stack_only(scalar))return {};
        unsigned loads=0;int64_t offset=0;
        for(const auto& load:scalar.loads)if(load.address.kind==sdk::Value::Self) {
            if(load.width!=4||load.address.offset<0||uint64_t(load.address.offset)+4>size)return {};
            offset=load.address.offset;++loads;
            // Float C return uses S0, integer C return uses W0 (AAPCS64).
            if(entry==*density) {
                if((getter->words[load.index]&0xffc0001fu)!=0xbd400000u)return {};
            } else if(scalar.regs[0]!=sdk::Value{sdk::Value::Field,offset,0,4})return {};
        }
        if(loads!=1||std::find(fields.begin(),fields.end(),offset)!=fields.end())return {};fields.push_back(offset);
    }
    const auto mode_body=graph.body(*ui_mode);if(!mode_body)return {};
    const auto mode=sdk::trace(graph,*mode_body);
    if(!mode.returned||!mode.calls.empty()||!mode.branches.empty()||!mode.unknown.empty()
        ||!home_native_provider::stack_only(mode)||mode.regs[0].kind!=sdk::Value::Field||mode.regs[0].width!=4
        ||mode.regs[0].offset<0||uint64_t(mode.regs[0].offset)+4>configuration_bytes)return {};
    return Contract{*configuration,*drop_configuration,*metrics,*drop_metrics,*width,*height,*density,*ui_mode,size,alignof(float),configuration_bytes};
}
}
