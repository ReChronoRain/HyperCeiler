/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_task_contract.h"

namespace home_layout::cards::sdk {
struct VectorFields {
    uint32_t capacity=0,pointer=0,length=0,stride=0;
    uint64_t none=0;
    bool operator==(const VectorFields&)const=default;
};
struct GroupContract {
    uint32_t size=0,alignment=0,type=0;
    int mixed_type=0;
    VectorFields tasks,children,vector_layout;
    OptionalField null_child;
    Address drop,in_place_drop;
};
inline bool zero(Value value){return value.kind==Value::Constant&&value.number==0;}
inline bool stack_writes_only(const Trace& flow) {
    return std::all_of(flow.stores.begin(),flow.stores.end(),[](const auto& store){return store.address.kind==Value::Stack;});
}
inline std::optional<VectorFields> task_vector(const Graph& graph) {
    const auto at=graph.exported("GroupedTaskInfo_task_info1");if(!at)return {};
    const auto body=graph.body(*at);if(!body||body->words.size()>64||body->words.back()!=0xd65f03c0u)return {};
    const auto flow=trace(graph,*body);
    if(!flow.calls.empty()||!flow.branches.empty()||flow.unknown.size()!=1||flow.selects.size()!=1||!stack_writes_only(flow))return {};
    const auto index=flow.unknown[0];const auto w=body->words[index];
    // CCMP Xn,Xm,NZCV,NE follows length == 0. Its fallback Z flag is set,
    // hence the following EQ select returns null for empty OR absent vectors.
    if((w&0xffe00c10u)!=0xfa400000u||((w>>12)&15)!=1||!(w&4)||!index)return {};
    Body prefix=*body;prefix.words.resize(index);const auto before=trace(graph,prefix);
    const auto cap=before.regs[(w>>5)&31],none=before.regs[(w>>16)&31];
    if(cap.kind!=Value::Field||cap.width!=8||none.kind!=Value::Constant
        ||before.comparisons.empty()||before.comparisons.back().index+1!=index)return {};
    const auto compared=before.comparisons.back();
    const auto& select=flow.selects[0];
    if(compared.field.kind!=Value::Field||compared.field.width!=8||!zero(compared.value)
        ||select.index!=index+1||select.destination!=0||select.condition!=0||!zero(select.yes)
        ||select.no.kind!=Value::Field||select.no.width!=8||select.no.number!=0)return {};
    if(flow.regs[0].kind!=Value::Selected||flow.regs[0].number!=0)return {};
    for(auto field:{cap.offset,select.no.offset,compared.field.offset})if(field<0||field>65536)return {};
    if(cap.offset==select.no.offset||cap.offset==compared.field.offset||select.no.offset==compared.field.offset)return {};
    return VectorFields{static_cast<uint32_t>(cap.offset),static_cast<uint32_t>(select.no.offset),
        static_cast<uint32_t>(compared.field.offset),0,none.number};
}
inline bool field_branch(const Trace& flow,std::span<const size_t> path,const Body& body,uint32_t field,uint64_t none,bool equal) {
    auto copy=flow;
    for(auto& compare:copy.branch_comparisons) {
        if(compare.field.kind==Value::Field&&compare.field.offset==field&&compare.field.width==8) {
            compare.field.kind=Value::Result;compare.field.offset=0;compare.field.number=0;
        } else compare.field={};
    }
    return compared_equal(copy,path,body,0,none,equal);
}
inline bool second_task(const Graph& graph,VectorFields& vector,uint32_t size) {
    const auto at=graph.exported("GroupedTaskInfo_task_info2");if(!at)return false;
    const auto body=graph.body(*at);if(!body)return false;
    const auto paths=forward_paths(*body);if(!paths||paths->size()!=2)return false;
    unsigned absent=0,present=0;
    for(const auto& path:*paths) {
        const auto flow=trace(graph,*body,false,false,path);
        if(!flow.calls.empty()||!flow.unknown.empty()||!stack_writes_only(flow))return false;
        if(flow.selects.empty()) {
            if(!zero(flow.regs[0])||!field_branch(flow,path,*body,vector.capacity,vector.none,true))return false;
            ++absent;continue;
        }
        if(flow.selects.size()!=1||flow.regs[0].kind!=Value::Selected||flow.regs[0].number!=0)return false;
        const auto& select=flow.selects[0];
        if(select.destination!=0||select.condition!=8||!zero(select.no)
            ||select.yes.kind!=Value::RelativeField||select.yes.offset!=vector.pointer||select.yes.number!=size
            ||select.field.kind!=Value::Field||select.field.offset!=vector.length||select.field.width!=8
            ||select.value.kind!=Value::Constant||select.value.number!=1
            ||!field_branch(flow,path,*body,vector.capacity,vector.none,false))return false;
        ++present;
    }
    vector.stride=size;return absent==1&&present==1;
}
inline std::optional<Body> success_prefix(const Graph& graph,Address address) {
    auto body=graph.body(address);if(!body)return {};
    const auto end=std::find(body->words.begin(),body->words.end(),0xd65f03c0u);
    if(end==body->words.end())return {};body->words.resize(static_cast<size_t>(end-body->words.begin())+1);return body;
}
inline std::optional<GroupContract> resolve_group_prefix(const Graph& graph,const TaskContract& task,std::string_view* reason=nullptr) {
    GroupContract out;if(reason)*reason="first task vector";
    const auto vector=task_vector(graph);if(!vector)return {};out.tasks=*vector;
    if(reason)*reason="second task stride/guards";
    if(!second_task(graph,out.tasks,task.task_size))return {};
    if(reason)*reason="group type getter";
    const auto type=graph.exported("GroupedTaskInfo_get_type"),drop=graph.exported("GroupedTaskInfo_drop"),factory=graph.exported("GroupedTaskInfo_default");
    if(!type||!drop||!factory)return {};
    const auto type_body=graph.body(*type),drop_body=graph.body(*drop),factory_body=success_prefix(graph,*factory);
    if(!type_body||!drop_body||!factory_body)return {};
    const auto type_flow=trace(graph,*type_body),drop_flow=trace(graph,*drop_body),factory_flow=trace(graph,*factory_body);
    const auto field=type_flow.regs[0];
    if(field.kind!=Value::Field||field.width!=4||field.offset<0||field.offset>65536||!type_flow.calls.empty()
        ||!type_flow.unknown.empty()||!type_flow.branches.empty()||!stack_writes_only(type_flow))return {};
    out.type=static_cast<uint32_t>(field.offset);
    if(reason)*reason="box allocation/drop";
    if(drop_flow.calls.size()!=1||!drop_flow.calls[0].target||drop_flow.calls[0].receiver.kind!=Value::Self
        ||drop_flow.calls[0].receiver.offset!=0||drop_flow.regs[0].kind!=Value::Self||drop_flow.regs[0].offset!=0
        ||drop_flow.regs[1].kind!=Value::Constant||drop_flow.regs[2].kind!=Value::Constant)return {};
    const auto size=drop_flow.regs[1].number,alignment=drop_flow.regs[2].number;
    if(size<24||size>65536||alignment!=8||size%alignment||out.type+4>size)return {};
    const auto result=factory_flow.regs[0];
    if(result.kind!=Value::Returned||result.offset!=0||result.number>=factory_flow.calls.size())return {};
    const auto& allocation=factory_flow.calls[result.number];
    if(allocation.receiver.kind!=Value::Constant||allocation.receiver.number!=size
        ||allocation.parcel.kind!=Value::Constant||allocation.parcel.number!=alignment||allocation.linkage.empty()||!allocation.target
        ||!graph.callee(*drop_body,drop_body->words.size()-1,true))return {};
    bool null_guard=false;
    for(auto branch:factory_flow.branches)if(branch==allocation.index+1) {
        const auto w=factory_body->words[branch];
        const int64_t destination=int64_t(branch)+(int32_t(w<<8)>>13);
        null_guard=(w&0xff00001fu)==0xb4000000u&&destination>=int64_t(factory_body->words.size());
    }
    if(!null_guard)return {};
    bool initial_tasks=false;std::optional<int> mixed;
    for(const auto& store:factory_flow.stores)if(store.address.kind==Value::Returned&&store.address.number==result.number) {
        if(store.address.offset<0||store.address.offset+store.width>int64_t(size))return {};
        if(store.address.offset==out.tasks.capacity&&store.width==8&&store.value.kind==Value::Constant&&store.value.number==out.tasks.none)initial_tasks=true;
        if(store.address.offset==out.type&&store.width==4&&store.value.kind==Value::Constant)mixed=static_cast<int>(store.value.number);
    }
    // Explicit current Android Shell Parcelable enum contract. This is not a
    // vendor field offset; a changed default/type family declines the adapter.
    if(!initial_tasks||!mixed||*mixed!=4)return {};
    out.size=static_cast<uint32_t>(size);out.alignment=static_cast<uint32_t>(alignment);out.mixed_type=*mixed;
    for(auto offset:{out.tasks.capacity,out.tasks.pointer,out.tasks.length})if(offset+8>size)return {};
    out.drop=*drop;out.in_place_drop=*drop_flow.calls[0].target;
    if(reason)*reason="group prefix ready";return out;
}
struct ElementLoop {
    uint32_t pointer=0,length=0;
    Address drop;
    std::optional<OptionalField> null_element;
};
inline std::optional<ElementLoop> element_loop(const Graph& graph,Address at,uint32_t stride,bool nullable) {
    const auto body=graph.body(at);if(!body)return {};
    const auto flow=trace(graph,*body);
    if(!flow.unknown.empty()||!stack_writes_only(flow))return {};
    std::optional<ElementLoop> result;
    for(const auto& step:flow.steps) {
        if(step.flags||step.delta!=stride||step.input.kind!=Value::Field||step.input.width!=8
            ||step.index+1>=body->words.size())continue;
        const auto advance=body->words[step.index],back=body->words[step.index+1];
        if(!(advance>>31)||((advance>>5)&31)!=step.destination||(back&0xff00001fu)!=0x54000001u)continue;
        const int64_t start=int64_t(step.index+1)+(int32_t(back<<8)>>13);
        if(start<0||start>=int64_t(step.index))continue;
        const Step* count=nullptr;
        for(const auto& other:flow.steps)if(other.index+1==step.index&&other.flags&&other.delta==-1
            &&other.input.kind==Value::Field&&other.input.width==8
            &&((body->words[other.index]>>5)&31)==other.destination&&(body->words[other.index]>>31))count=&other;
        if(!count||count->destination==step.destination||count->input.offset==step.input.offset)continue;
        bool empty_guard=false;
        for(const auto branch:flow.branches)if(branch<static_cast<size_t>(start)) {
            const auto w=body->words[branch];
            const int64_t destination=int64_t(branch)+(int32_t(w<<8)>>13);
            if((w&0xff00001fu)==(0xb4000000u|count->destination)&&destination>int64_t(step.index+1)) {
                Body prefix=*body;prefix.words.resize(branch);
                const auto input=trace(graph,prefix).regs[count->destination];
                empty_guard=input==count->input;
            }
        }
        if(!empty_guard)continue;
        const Call* drop=nullptr;bool ambiguous=false;
        for(const auto& call:flow.calls)if(call.index>=static_cast<size_t>(start)&&call.index<count->index) {
            if(drop){ambiguous=true;break;}drop=&call;
        }
        if(ambiguous||!drop||!drop->target||drop->receiver!=step.input)continue;
        std::optional<OptionalField> null_element;
        for(const auto& compare:flow.branch_comparisons) {
            if(compare.index<static_cast<size_t>(start)||compare.index>=drop->index
                ||compare.field.kind!=Value::IndirectField||compare.field.offset!=step.input.offset
                ||compare.field.width!=8||compare.field.number>stride||stride-compare.field.number<8||compare.value.kind!=Value::Constant)continue;
            const auto w=body->words[compare.index];
            if((w&0xff00001fu)!=0x54000000u||int64_t(compare.index)+(int32_t(w<<8)>>13)!=int64_t(count->index))continue;
            if(null_element){ambiguous=true;break;}
            null_element=OptionalField{static_cast<uint32_t>(compare.field.number),compare.value.number};
        }
        if(ambiguous||nullable!=null_element.has_value()||step.input.offset<0||count->input.offset<0
            ||step.input.offset>65536||count->input.offset>65536)continue;
        if(result)return {};
        result=ElementLoop{static_cast<uint32_t>(step.input.offset),static_cast<uint32_t>(count->input.offset),*drop->target,null_element};
    }
    return result;
}
inline bool none_guard(const Trace& flow,const Body& body,uint32_t field,uint64_t none,size_t first,size_t last) {
    for(const auto& compare:flow.branch_comparisons) {
        if(compare.field.kind!=Value::Field||compare.field.offset!=field||compare.field.width!=8
            ||compare.value.kind!=Value::Constant||compare.value.number!=none||compare.index>=first)continue;
        const auto w=body.words[compare.index];const auto condition=w&15;
        const int64_t destination=int64_t(compare.index)+(int32_t(w<<8)>>13);
        if(condition==0&&destination>int64_t(last))return true;
        if(condition==1&&destination>int64_t(compare.index)&&destination<=int64_t(first)
            &&std::find(body.words.begin()+compare.index+1,body.words.begin()+destination,0xd65f03c0u)!=body.words.begin()+destination)return true;
    }
    return false;
}
inline std::optional<Address> vector_free_tail(const Graph& graph,const Body& body,uint32_t stride,uint32_t alignment) {
    if(body.words.empty())return {};
    const auto flow=trace(graph,body);
    if(flow.regs[0].kind!=Value::Self||flow.regs[0].offset!=0
        ||flow.regs[1].kind!=Value::Constant||flow.regs[1].number!=alignment
        ||flow.regs[2].kind!=Value::Constant||flow.regs[2].number!=stride)return {};
    // Compare the actual common RawVec linkage of the task and child vector
    // adapters, without naming the compiler's private Rust monomorphization.
    return graph.raw_callee(body,body.words.size()-1,true);
}
inline std::optional<GroupContract> resolve_group(const Graph& graph,const TaskContract& task,std::string_view* reason=nullptr) {
    auto out=resolve_group_prefix(graph,task,reason);if(!out)return {};
    const auto body=graph.body(out->in_place_drop);if(!body)return {};
    const auto flow=trace(graph,*body);
    if(reason)*reason="task vector destructor and common allocator";
    std::optional<ElementLoop> tasks;std::optional<Address> allocator;uint32_t tasks_base=0,local_capacity=0;
    for(const auto& call:flow.calls) {
        if(!call.target||call.receiver.kind!=Value::Self||call.receiver.offset<0)continue;
        const auto option_body=graph.body(*call.target);if(!option_body)continue;
        const auto option_flow=trace(graph,*option_body);
        if(option_flow.calls.size()!=1||!option_flow.calls[0].target||option_flow.calls[0].receiver.kind!=Value::Self
            ||option_flow.calls[0].receiver.offset!=0)continue;
        const auto loop=element_loop(graph,*option_flow.calls[0].target,task.task_size,false);if(!loop)continue;
        const uint32_t base=static_cast<uint32_t>(call.receiver.offset);
        if(out->tasks.capacity<base||loop->pointer+base!=out->tasks.pointer||loop->length+base!=out->tasks.length
            ||!none_guard(option_flow,*option_body,out->tasks.capacity-base,out->tasks.none,
                option_flow.calls[0].index,option_flow.calls[0].index))continue;
        const auto adapter=graph.callee(*option_body,option_body->words.size()-1,true);if(!adapter)continue;
        const auto free_body=graph.leaf(*adapter);if(!free_body)continue;
        const auto shared=vector_free_tail(graph,*free_body,task.task_size,out->alignment);if(!shared)continue;
        if(tasks)return {};tasks=*loop;allocator=*shared;tasks_base=base;local_capacity=out->tasks.capacity-base;
    }
    if(!tasks||!allocator)return {};
    out->vector_layout={local_capacity,tasks->pointer,tasks->length,0,0};
    if(reason)*reason="child vector loop and nullable element";
    std::optional<ElementLoop> children;uint32_t children_base=0;
    for(size_t i=1;i<flow.calls.size();++i) {
        const auto& first=flow.calls[i-1];const auto& second=flow.calls[i];
        if(!first.target||!second.target||first.receiver.kind!=Value::Self||first.receiver!=second.receiver
            ||first.receiver.offset<0||first.receiver.offset==tasks_base)continue;
        const auto loop=element_loop(graph,*first.target,out->size,true);
        const auto free_body=graph.leaf(*second.target);
        if(!loop||!free_body||loop->pointer!=tasks->pointer||loop->length!=tasks->length)continue;
        const auto shared=vector_free_tail(graph,*free_body,out->size,out->alignment);
        const uint32_t base=static_cast<uint32_t>(first.receiver.offset);
        if(shared!=allocator||base>out->size||local_capacity>out->size-base
            ||!none_guard(flow,*body,base+local_capacity,out->tasks.none,first.index,second.index))continue;
        if(children)return {};children=*loop;children_base=base;
    }
    if(!children)return {};
    out->children={children_base+local_capacity,children_base+children->pointer,children_base+children->length,out->size,out->tasks.none};
    out->null_child=*children->null_element;
    for(auto field:{out->children.capacity,out->children.pointer,out->children.length})if(field+8>out->size)return {};
    if(reason)*reason="recursive child schema";
    const auto child_body=graph.body(children->drop);if(!child_body)return {};
    const auto child_flow=trace(graph,*child_body);unsigned task_match=0,child_match=0;
    for(const auto& call:child_flow.calls) {
        if(!call.target||call.receiver.kind!=Value::Self)continue;
        const bool is_task=call.receiver.offset==tasks_base,is_child=call.receiver.offset==children_base;
        if(!is_task&&!is_child)continue;
        const auto loop=element_loop(graph,*call.target,is_task?task.task_size:out->size,is_child);
        const auto loop_body=graph.body(*call.target);if(!loop||!loop_body)continue;
        if(loop->pointer!=tasks->pointer||loop->length!=tasks->length
            ||loop->drop!=(is_task?tasks->drop:children->drop)||loop->null_element!=(is_task?std::optional<OptionalField>{}:children->null_element)
            ||vector_free_tail(graph,*loop_body,is_task?task.task_size:out->size,out->alignment)!=allocator
            ||!none_guard(child_flow,*child_body,is_task?out->tasks.capacity:out->children.capacity,out->tasks.none,call.index,call.index))continue;
        if(is_task)++task_match;else ++child_match;
    }
    if(task_match!=1||child_match!=1)return {};
    const auto factory=graph.exported("GroupedTaskInfo_default");if(!factory)return {};
    const auto factory_body=success_prefix(graph,*factory);if(!factory_body)return {};
    const auto factory_flow=trace(graph,*factory_body);bool absent_children=false;
    for(const auto& store:factory_flow.stores)if(store.address.kind==Value::Returned&&store.address.number==factory_flow.regs[0].number
        &&store.address.offset==out->children.capacity&&store.width==8&&store.value.kind==Value::Constant&&store.value.number==out->children.none)absent_children=true;
    if(!absent_children)return {};
    if(reason)*reason="group schema ready";return out;
}
}
