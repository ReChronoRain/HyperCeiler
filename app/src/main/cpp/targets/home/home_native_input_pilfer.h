/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_dispatch.h"
namespace home_native_input {
struct PilferContract {sdk::Address entry,implementation,status_drop;uint32_t data=0,table=0,method=0;};
// Public C pilfer returns void, consuming failures internally. Resolve its
// nullable-status call before checked invocation. X0 is the native AAPCS64
// receiver/return register. No vendor field or virtual slot is supplied here.
// This value contract alone never authorizes or invokes pointer pilfer.
inline std::optional<PilferContract> pilfer_contract(const sdk::Graph& graph) {
    const auto monitor=resolve_monitor(graph);const auto entry=graph.exported("input_InputMonitor_pilferPointers");
    if(!monitor||!entry)return {};
    const auto body=input_forwarded(graph,*entry);if(!body||body->words.size()>256)return {};
    std::optional<size_t> goal;
    for(size_t at=0;at<body->words.size();++at)if((body->words[at]&0xfffffc1fu)==0xd63f0000u){if(goal)return {};goal=at;}
    if(!goal||*goal+1>=body->words.size())return {};
    const size_t check=*goal+1;const auto condition=body->words[check];
    if((condition&0xff00001fu)!=0xb4000000u)return {}; // CBZ X0: nullable native status.
    const auto success_at=int64_t(check)+(int32_t(condition<<8)>>13);
    const auto paths=home_native_provider::return_paths(graph,*body);if(!paths)return {};
    std::optional<PilferContract> result;unsigned absent=0,success=0,errors=0;
    for(auto path:*paths) {
        const auto call_at=std::find(path.begin(),path.end(),*goal);
        if(call_at==path.end()) {
            const auto flow=sdk::trace(graph,*body,false,false,path);
            if(!flow.returned||!flow.calls.empty()||!flow.unknown.empty()||!home_native_provider::stack_only(flow))return {};
            ++absent;continue;
        }
        const std::vector<size_t> prefix(path.begin(),call_at);
        const auto before=sdk::trace(graph,*body,false,false,prefix);
        const auto self=before.regs[0],target=before.regs[(body->words[*goal]>>5)&31];
        if(!before.calls.empty()||!before.unknown.empty()||!home_native_provider::stack_only(before)
            ||self.kind!=sdk::Value::Field||self.width!=8||self.offset<0||uint64_t(self.offset)+8>monitor->bytes
            ||target.kind!=sdk::Value::IndirectField||target.width!=8||target.offset<0
            ||uint64_t(target.offset)+8>monitor->bytes||self.offset==target.offset
            ||self.offset%8||target.offset%8||target.number<8||target.number>4096||target.number%8)return {};
        bool nullable_host=false;
        for(const auto branch:before.branches) {
            const auto instruction=body->words[branch];if((instruction&0xff000000u)!=0xb4000000u)return {};
            std::vector<size_t> inspected;for(const auto at:prefix){if(at==branch)break;inspected.push_back(at);}
            const auto guarded=sdk::trace(graph,*body,false,false,inspected);
            if(guarded.regs[instruction&31]!=self)return {};nullable_host=true;
        }
        if(!nullable_host)return {};
        if(!result)result=PilferContract{*entry,body->address,{},uint32_t(self.offset),uint32_t(target.offset),uint32_t(target.number)};
        if(result->data!=self.offset||result->table!=target.offset||result->method!=target.number)return {};
        const std::vector<size_t> suffix(call_at+1,path.end());if(suffix.size()<2||suffix.front()!=check)return {};
        const std::array<std::pair<unsigned,sdk::Value>,1> returned={{{0,{sdk::Value::Argument,0,77,8}}}};
        const auto after=sdk::trace(graph,*body,false,false,suffix,returned);
        if(!after.returned||!after.unknown.empty()||!home_native_provider::stack_only(after))return {};
        if(int64_t(suffix[1])==success_at){if(!after.calls.empty()||!after.stores.empty())return {};++success;continue;}
        if(suffix[1]!=check+1||after.calls.empty())return {};
        const auto& drop=after.calls.back();if(!drop.target||drop.receiver.kind!=sdk::Value::Stack)return {};
        const auto destructor=home_native_provider::body_or_leaf(graph,*drop.target);if(!destructor||destructor->words.empty())return {};
        const auto release=graph.raw_callee(*destructor,destructor->words.size()-1,true);
        if(!release||graph.import_name(*release)!=std::optional<std::string_view>("AStatus_delete"))return {};
        auto unwrap=*destructor;unwrap.words.pop_back();const auto raw=sdk::trace(graph,unwrap);
        if(!raw.calls.empty()||!raw.branches.empty()||!raw.unknown.empty()||!home_native_provider::stack_only(raw)
            ||raw.regs[0]!=sdk::Value{sdk::Value::Field,0,0,8})return {};
        unsigned retained=0;
        for(const auto& store:after.stores) {
            if(store.address.kind!=sdk::Value::Stack||store.address.offset+store.width<=drop.receiver.offset
                ||store.address.offset>=drop.receiver.offset+8)continue;
            if(store.address!=drop.receiver||store.width!=8||store.value!=returned[0].second)return {};++retained;
        }
        if(retained!=1)return {};
        for(size_t at=0;at+1<after.calls.size();++at)if(after.calls[at].receiver.kind!=sdk::Value::Literal)return {};
        if(errors&&result->status_drop!=*drop.target)return {};result->status_drop=*drop.target;++errors;
    }
    return result&&absent&&success&&errors?result:std::nullopt;
}
}
