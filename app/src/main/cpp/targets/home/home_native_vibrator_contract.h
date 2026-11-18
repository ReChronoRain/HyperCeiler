/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_trace.h"

namespace home_native_vibrator {
namespace sdk=home_layout::cards::sdk;

struct Operation {
    sdk::Address proxy,prepare,submit;
    uint32_t transaction;
    size_t prepare_at,submit_at;
};
struct Contract {Operation ids,vibrate;};
struct Policy {Operation interactive,locked;};

inline std::optional<Operation> resolve_operation(const sdk::Graph& graph,size_t image,
    std::string_view interface,std::string_view proxy_name,std::string_view method) {
    const auto* metadata=graph.metadata(image);
    if(!metadata)return {};
    const auto symbol=metadata->unique_executable_symbol([&](std::string_view name) {
        return name.starts_with("_R")&&name.find(interface)!=name.npos
            &&name.find(proxy_name)!=name.npos&&name.ends_with(method);
    });
    if(!symbol)return {};
    const auto proxy=graph.body({image,symbol->rva});
    if(!proxy||proxy->words.size()>256)return {};
    std::optional<sdk::Address> prepare;
    std::optional<Operation> result;
    size_t begin=0,prepare_at=0;
    for(size_t i=0;i<proxy->words.size();++i) {
        const auto word=proxy->words[i];
        if((word&0xfc000000u)!=0x94000000u) {
            if((word&0xff000010u)==0x54000000u||(word&0x7e000000u)==0x34000000u
                ||(word&0x7e000000u)==0x36000000u||(word&0xfc000000u)==0x14000000u
                ||word==0xd65f03c0u)begin=i+1;
            continue;
        }
        const auto target=graph.callee(*proxy,i);
        const auto body=target?graph.body(*target):std::nullopt;
        if(body&&body->words.size()<=64) {
            const std::array<std::pair<unsigned,sdk::Value>,1> input{{{3,{sdk::Value::Constant,0,37}}}};
            const auto flow=sdk::trace(graph,*body,false,false,{},input);
            for(const auto& call:flow.calls) {
                if(call.linkage=="AIBinder_prepareTransaction") {
                    if(prepare||call.receiver.kind!=sdk::Value::Field||call.parcel.kind!=sdk::Value::Stack)return {};
                    prepare=target;prepare_at=i;
                }
                if(call.linkage!="AIBinder_transact")continue;
                if(result||!prepare||call.receiver.kind!=sdk::Value::Field
                    ||call.parcel!=sdk::Value{sdk::Value::Parcel}||call.third.kind!=sdk::Value::Stack
                    ||call.fourth.kind!=sdk::Value::Stack||call.third==call.fourth
                    ||call.fifth!=input.front().second)return {};
                // Decode the proxy's actual argument, not the upstream AIDL ordinal.
                std::vector<size_t> block;
                for(size_t at=begin;at<=i;++at)block.push_back(at);
                const auto dispatch=sdk::trace(graph,*proxy,false,false,block);
                if(dispatch.calls.size()!=1||!dispatch.unknown.empty())return {};
                const auto code=dispatch.calls.front().parcel,flags=dispatch.calls.front().fourth;
                if(code.kind!=sdk::Value::Constant||!code.number||code.number>0x00ffffff
                    ||flags.kind!=sdk::Value::Constant||flags.number)return {};
                result=Operation{proxy->address,*prepare,*target,static_cast<uint32_t>(code.number),prepare_at,i};
            }
        }
        begin=i+1;
    }
    return result;
}
inline std::optional<Contract> resolve(const sdk::Graph& graph,size_t image=0) {
    constexpr std::string_view interface="23IVibratorManagerService",proxy="24BpVibratorManagerService";
    auto ids=resolve_operation(graph,image,interface,proxy,"14getVibratorIds");
    auto vibrate=resolve_operation(graph,image,interface,proxy,"7vibrate");
    if(!ids||!vibrate||ids->prepare!=vibrate->prepare||ids->submit!=vibrate->submit
        ||ids->transaction==vibrate->transaction)return {};
    return Contract{*ids,*vibrate};
}
inline std::optional<Policy> resolve_policy(const sdk::Graph& graph,size_t image=0) {
    auto interactive=resolve_operation(graph,image,"13IPowerManager","14BpPowerManager","13isInteractive");
    auto locked=resolve_operation(graph,image,"14IWindowManager","15BpWindowManager","16isKeyguardLocked");
    if(!interactive||!locked||interactive->prepare!=locked->prepare||interactive->submit!=locked->submit)return {};
    for(const auto& operation:{*interactive,*locked}) {
        const auto body=graph.body(operation.proxy);
        if(!body)return {};
        // These are no-argument synchronous getters. Reject intervening
        // writers, and require the actual Rust bool reader after submission.
        bool status_read=false;
        unsigned readers=0;
        for(size_t at=operation.prepare_at+1;at<body->words.size();++at) {
            const auto word=body->words[at];
            const auto call=graph.raw_callee(*body,at);
            if(at<operation.submit_at&&(call||(word&0xfffffc1fu)==0xd63f0000u))return {};
            if(at<=operation.submit_at||!call)continue;
            const auto name=graph.import_name(*call);
            if(!name)continue;
            if(name->find("6StatusNt")!=name->npos&&name->ends_with("11Deserialize11deserialize"))status_read=true;
            if(name->find("10parcelablebNt")!=name->npos&&name->ends_with("11Deserialize11deserialize")) {
                if(!status_read)return {};
                ++readers;
            }
        }
        if(readers!=1)return {};
    }
    return Policy{*interactive,*locked};
}
}
