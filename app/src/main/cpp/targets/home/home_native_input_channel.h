/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_receiver.h"
namespace home_native_input {
struct ChannelContract {
    sdk::Address get,clone;
    uint32_t tag=0,payload=0;
    uint64_t present=0,unavailable=0;
};
// getChannel returns a borrowed Option inside the monitor. The receiver's
// own factory clones it. Never destroy that borrowed pointer as a channel.
inline std::optional<ChannelContract> channel_contract(const sdk::Graph& graph) {
    const auto get=graph.exported("input_InputMonitor_getChannel"),factory=graph.exported("input_InputEventReceiver_new");
    if(!get||!factory)return {};
    const auto body=graph.body(*get),create=graph.body(*factory);if(!body||!create)return {};
    const auto flow=sdk::trace(graph,*body);
    if(!flow.returned||!flow.unknown.empty()||!flow.branches.empty()||flow.calls.size()!=1
        ||!flow.calls[0].target||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}
        ||flow.regs[1]!=sdk::Value{sdk::Value::Returned,0,0,8}
        ||home_native_provider::boolean(flow,flow.regs[0],flow.regs[1])!=0
        ||!home_native_provider::stack_only(flow))return {};
    const auto borrow=graph.body(*flow.calls[0].target);if(!borrow)return {};
    const auto optional=sdk::optional_field(graph,*borrow);if(!optional||optional->offset)return {};
    const auto paths=home_native_provider::return_paths(graph,*create);if(!paths||paths->size()!=4)return {};
    const std::array<std::pair<unsigned,sdk::Value>,1> input={{{1,{sdk::Value::Argument,0,101,8}}}};
    ChannelContract result;result.get=*get;result.unavailable=optional->none;
    unsigned cloned=0,absent=0;
    for(const auto& path:*paths) {
        const auto constructor=sdk::trace(graph,*create,false,false,path,input);
        std::optional<sdk::Call> copy;
        for(const auto& call:constructor.calls)if(call.receiver.kind==sdk::Value::Argument&&call.receiver.number==101) {
            if(copy||!call.target||call.receiver.offset<=0||call.receiver.offset>4096
                ||call.output.kind!=sdk::Value::Stack)return {};
            copy=call;
        }
        if(!copy){++absent;continue;}
        std::optional<sdk::Compare> present;
        for(const auto& branch:constructor.branch_comparisons) {
            if(branch.index>=copy->index||branch.field.kind!=sdk::Value::IndirectField||branch.field.number!=101)continue;
            const auto instruction=create->words[branch.index];
            if(present||branch.field.width!=4||branch.field.offset<0||branch.field.offset>4096
                ||branch.value.kind!=sdk::Value::Constant||(instruction&0xff00001fu)!=0x54000001u)return {};
            const auto next=std::find(path.begin(),path.end(),branch.index);
            if(next==path.end()||next+1==path.end()||*(next+1)!=branch.index+1)return {};
            present=branch;
        }
        if(!present||uint64_t(present->field.offset)+4>uint64_t(copy->receiver.offset))return {};
        if(cloned&&(result.clone!=*copy->target||result.tag!=present->field.offset
            ||result.payload!=copy->receiver.offset||result.present!=present->value.number))return {};
        result.clone=*copy->target;result.tag=uint32_t(present->field.offset);
        result.payload=uint32_t(copy->receiver.offset);result.present=present->value.number;++cloned;
    }
    if(cloned!=2||absent!=2||result.present==result.unavailable)return {};
    return result;
}
}
