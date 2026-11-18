/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_trace.h"
#include <set>

namespace home_layout::cards::sdk {
inline std::optional<Address> single_call(const Graph& graph,std::string_view public_name) {
    const auto address=graph.exported(public_name);if(!address)return {};
    const auto body=graph.body(*address);if(!body)return {};
    const auto flow=trace(graph,*body);
    if(flow.calls.size()!=1||!flow.calls[0].target)return {};
    return flow.calls[0].target;
}
inline bool reaches(const Graph& graph,Address start,Address wanted) {
    std::vector<Address> frontier{start},seen;
    for(unsigned depth=0;depth<=4&&!frontier.empty();++depth) {
        std::vector<Address> next;
        for(const auto address:frontier) {
            if(address==wanted)return true;
            if(std::find(seen.begin(),seen.end(),address)!=seen.end())continue;
            if(seen.size()>=64)return false;seen.push_back(address);
            const auto body=graph.body(address);if(!body)continue;
            for(auto call:graph.calls(*body,true))next.push_back(call);
            if(next.size()>256)return false;
        }
        frontier=std::move(next);
    }
    return false;
}
inline bool straight_region(const Body& body,const Trace& flow,size_t first,size_t last) {
    if(first>last||last>=body.words.size())return false;
    for(auto at:flow.unknown)if(at>=first&&at<=last)return false;
    // Failure branches may leave the region; an alternate producer path inside
    // it would require CFG interpretation and is deliberately not inferred.
    for(auto at:flow.branches)if(at>=first&&at<=last) {
        const auto w=body.words[at];int64_t target=-1;
        if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)
            target=int64_t(at)+(int32_t(w<<8)>>13);
        else if((w&0x7e000000u)==0x36000000u)target=int64_t(at)+(int32_t(w<<13)>>18);
        else return false;
        if(target<0||target>=int64_t(body.words.size())||(target>=int64_t(first)&&target<=int64_t(last)))return false;
    }
    return true;
}
struct TaskContract {
    uint32_t task_size=0;
    std::array<OptionalField,4> activities; // Android parcel order: base,top,orig,real.
    OptionalField base_intent,intent_component,intent_package;
    StringFields component_string,package_string;
    Address writer,reader;
};
inline bool copies_result(const Trace& flow,uint32_t destination,size_t call,unsigned width) {
    if(!width||width>256)return false;
    std::vector<bool> bytes(width);
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self) {
        for(unsigned n=0;n<store.width;++n) {
            const int64_t offset=store.address.offset+n-destination;
            if(offset<0||offset>=width)continue;
            bytes[static_cast<size_t>(offset)]=store.value.kind==Value::Result&&store.value.number==call
                &&store.value.offset+n==offset;
        }
    }
    return std::all_of(bytes.begin(),bytes.end(),[](bool valid){return valid;});
}
inline bool compared_equal(const Trace& flow,std::span<const size_t> path,const Body& body,
    size_t call,uint64_t literal,bool expected) {
    unsigned found=0;
    for(const auto& compare:flow.branch_comparisons) {
        if(compare.field.kind!=Value::Result||compare.field.offset!=0||compare.field.number!=call
            ||compare.value.kind!=Value::Constant||compare.value.number!=literal)continue;
        const auto at=std::find(path.begin(),path.end(),compare.index);
        if(at==path.end()||at+1==path.end())return false;
        const auto w=body.words[*at];
        if((w&0xff00001eu)!=0x54000000u)return false; // Only EQ or NE describes null/error.
        const int64_t target=int64_t(*at)+(int32_t(w<<8)>>13);
        const bool taken=target==int64_t(*(at+1));
        if(!taken&&*(at+1)!=*at+1)return false;
        if(((w&1)?!taken:taken)!=expected)return false;++found;
    }
    return found==1;
}
inline bool component_option_reader(const Graph& graph,Address option,Address reader,uint64_t none,uint64_t error) {
    const auto source=graph.body(reader),nullable=graph.body(option);if(!source||!nullable)return false;
    const auto ordinary=trace(graph,*source);
    std::vector<size_t> reads;
    for(size_t i=0;i<ordinary.calls.size();++i)if(ordinary.calls[i].receiver.kind==Value::Parcel
        &&ordinary.calls[i].output.kind==Value::Stack)reads.push_back(i);
    if(reads.size()!=2)return false;
    const auto string=ordinary.calls[reads[0]].target;
    if(!string||ordinary.calls[reads[1]].target!=string)return false;
    const auto string_body=graph.body(*string);if(!string_body)return false;
    const auto string_calls=graph.calls(*string_body);
    if(string_calls.size()!=1)return false;
    // The non-null and optional ComponentName readers may inline each other.
    // Prove both string producers against the two public member consumers.
    std::array<StringFields,2> members;size_t member=0;
    for(auto name:{"ComponentName_get_package_name","ComponentName_get_class_name"}) {
        const auto at=graph.exported(name);if(!at)return false;
        const auto body=graph.body(*at);if(!body)return false;
        const auto fields=string_fields(graph,*body);if(!fields)return false;members[member++]=*fields;
    }
    if(members[1].pointer<=members[0].pointer||members[1].length<=members[0].length
        ||members[1].pointer-members[0].pointer!=members[1].length-members[0].length)return false;
    const auto second=members[1].pointer-members[0].pointer;
    const auto extent=std::max(members[0].pointer,members[0].length)+8;
    if(extent>second||!copies_result(ordinary,0,reads[0],extent)||!copies_result(ordinary,second,reads[1],extent))return false;
    const auto paths=forward_paths(*nullable);if(!paths)return false;
    unsigned present=0,absent=0;
    for(const auto& path:*paths) {
        const auto flow=trace(graph,*nullable,true,true,path);
        if(!flow.unknown.empty())return false;
        std::vector<size_t> input;
        for(size_t i=0;i<flow.calls.size();++i)if(flow.calls[i].receiver.kind==Value::Parcel
            &&flow.calls[i].output.kind==Value::Stack)input.push_back(i);
        if(input.empty()||input.size()>2||flow.calls[input[0]].target!=string_calls[0])return false;
        const Store* tag=nullptr;
        for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.address.offset==0&&store.width==8)tag=&store;
        if(!tag)return false;
        if(tag->value.kind==Value::Constant&&tag->value.number==error)continue;
        if(tag->value.kind!=Value::Result||tag->value.offset!=0||tag->value.number!=input[0])return false;
        if(input.size()==2) {
            if(flow.calls[input[1]].target!=string||!copies_result(flow,0,input[0],extent)
                ||!copies_result(flow,second,input[1],extent)
                ||!compared_equal(flow,path,*nullable,input[0],none,false))return false;
            ++present;
        } else {
            if(!copies_result(flow,0,input[0],8)||!compared_equal(flow,path,*nullable,input[0],none,true))return false;++absent;
        }
    }
    return present==1&&absent==1;
}
inline std::optional<std::pair<OptionalField,StringFields>> intent_package(const Graph& graph) {
    const auto exported=graph.exported("Intent_get_package");if(!exported)return {};
    const auto body=graph.body(*exported);if(!body||body->words.size()>64)return {};
    const auto flow=trace(graph,*body);
    if(!flow.returned||flow.calls.size()!=1||!flow.calls[0].target||!flow.unknown.empty())return {};
    const auto call=flow.calls[0];
    if(call.receiver.kind!=Value::Self||call.receiver.offset<0||call.receiver.offset>65536)return {};
    const auto borrowed=graph.body(*call.target);if(!borrowed)return {};
    const auto fields=string_fields(graph,*borrowed);if(!fields)return {};
    std::optional<OptionalField> result;
    for(const auto& compare:flow.comparisons) {
        if(compare.field.kind!=Value::Field||compare.field.offset!=call.receiver.offset
            ||compare.value.kind!=Value::Constant||compare.index+1>=body->words.size())continue;
        const auto branch=body->words[compare.index+1];
        if((branch&0xff00001fu)!=0x54000001u)continue; // NE enters the present-value getter.
        const int64_t target=int64_t(compare.index+1)+(int32_t(branch<<8)>>13);
        if(target!=int64_t(call.index)||result)return {};
        result=OptionalField{static_cast<uint32_t>(compare.field.offset),compare.value.number};
    }
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self)return {};
    return result?std::optional(std::pair{*result,*fields}):std::nullopt;
}
inline std::optional<TaskContract> resolve_task(const Graph& graph,std::string_view* reason=nullptr) {
    if(reason)*reason="activity getters";
    TaskContract out;
    const std::array<std::string_view,3> getter_names={"TaskInfo_get_base_activity","TaskInfo_get_top_activity","TaskInfo_get_orig_activity"};
    for(size_t i=0;i<getter_names.size();++i) {
        const auto address=graph.exported(getter_names[i]);if(!address)return {};
        const auto body=graph.body(*address);if(!body)return {};
        const auto field=optional_field(graph,*body);if(!field)return {};out.activities[i]=*field;
    }
    if(reason)*reason="intent getters";
    const auto get_optional=[&](std::string_view name)->std::optional<OptionalField> {
        const auto address=graph.exported(name);if(!address)return {};
        const auto body=graph.body(*address);return body?optional_field(graph,*body):std::nullopt;
    };
    const auto intent=get_optional("TaskInfo_get_baseIntent"),component=get_optional("Intent_get_component");
    const auto package=intent_package(graph);if(!intent||!component||!package)return {};
    out.base_intent=*intent;out.intent_component=*component;out.intent_package=package->first;out.package_string=package->second;
    if(reason)*reason="component string";
    const auto component_getter=graph.exported("ComponentName_get_package_name");if(!component_getter)return {};
    const auto component_body=graph.body(*component_getter);if(!component_body)return {};
    const auto string=string_fields(graph,*component_body);if(!string)return {};out.component_string=*string;
    if(reason)*reason="parcel entry graph";
    const auto component_writer=single_call(graph,"ComponentName_write_to_parcel");
    const auto component_reader=single_call(graph,"ComponentName_read_from_parcel");
    const auto intent_writer=single_call(graph,"Intent_write_to_parcel"),intent_reader=single_call(graph,"Intent_read_from_parcel");
    const auto running_writer=single_call(graph,"RunningTaskInfo_write_to_parcel");
    const auto running_reader=single_call(graph,"RunningTaskInfo_read_from_parcel");
    if(!component_writer||!component_reader||!intent_writer||!intent_reader||!running_writer||!running_reader)return {};
    const auto running_write_body=graph.body(*running_writer),running_read_body=graph.body(*running_reader);
    if(!running_write_body||!running_read_body)return {};
    const auto running_write=trace(graph,*running_write_body),running_read=trace(graph,*running_read_body);
    // RunningTaskInfo writes its extra integer after the inline TaskInfo. The
    // reader copies the same number of bytes in both directions. This is an
    // executable producer/consumer proof of size, not a Rust layout assumption.
    if(running_write.calls.empty()||running_write.calls[0].receiver.kind!=Value::Self
        ||running_write.calls[0].receiver.offset<=0||running_write.calls[0].receiver.offset>65536)return {};
    out.task_size=static_cast<uint32_t>(running_write.calls[0].receiver.offset);
    if(reason)*reason="TaskInfo size copies";
    unsigned copies=0;
    for(const auto& call:running_read.calls)if(call.third.kind==Value::Constant&&call.third.number==out.task_size
        &&(call.receiver.kind==Value::Self||call.receiver.kind==Value::Stack)
        &&call.parcel.kind==Value::Stack&&(call.linkage=="memcpy"||call.linkage=="memmove"))++copies;
    if(copies!=2)return {};
    if(reason)*reason="activity serializer quartet";
    std::optional<Address> serializer;
    for(const auto candidate:graph.calls(*running_write_body,true)) {
        const auto body=graph.body(candidate);if(!body)continue;
        const auto flow=trace(graph,*body);
        for(size_t first=0;first+4<=flow.calls.size();++first) {
            bool match=true;
            for(size_t j=0;j<3;++j) {
                const auto& call=flow.calls[first+j];
                match&=call.target&&call.receiver.kind==Value::Nullable&&call.parcel.kind==Value::Parcel
                    &&call.receiver.offset==out.activities[j].offset&&call.receiver.number==out.activities[j].none;
                if(j&&call.target!=flow.calls[first].target)match=false;
            }
            const auto& last=flow.calls[first+3];
            match&=last.receiver.kind==Value::Nullable&&last.parcel.kind==Value::Parcel
                &&last.target==flow.calls[first].target&&last.receiver.number==out.activities[0].none
                &&last.receiver.offset>=0&&last.receiver.offset+8<=out.task_size;
            if(!match||!straight_region(*body,flow,flow.calls[first].index,last.index+2))continue;
            const auto target=*last.target;
            if(std::count_if(flow.calls.begin(),flow.calls.end(),[&](const auto& call){return call.target==target;})!=4
                ||!reaches(graph,target,*component_writer)||serializer)return {};
            const bool intent_produced=std::any_of(flow.calls.begin(),flow.calls.begin()+first,[&](const auto& call) {
                return call.receiver.kind==Value::Nullable&&call.receiver.offset==out.base_intent.offset
                    &&call.receiver.number==out.base_intent.none&&call.target&&reaches(graph,*call.target,*intent_writer);
            });
            if(!intent_produced)return {};
            serializer=target;out.writer=candidate;
            out.activities[3]={static_cast<uint32_t>(last.receiver.offset),last.receiver.number};
        }
    }
    if(!serializer)return {};
    if(reason)*reason="activity deserializer quartet";
    std::optional<Address> deserializer;
    for(const auto candidate:graph.calls(*running_read_body)) {
        const auto body=graph.body(candidate);if(!body)continue;
        const auto flow=trace(graph,*body);
        std::array<const Store*,4> fields{};
        for(const auto& store:flow.stores)for(size_t j=0;j<fields.size();++j)
            if(store.address.kind==Value::Self&&store.address.offset==out.activities[j].offset
                &&store.width==8&&store.value.kind==Value::Result&&store.value.offset==0) {
                if(fields[j])return {};fields[j]=&store;
            }
        if(std::any_of(fields.begin(),fields.end(),[](auto value){return !value;}))continue;
        bool valid=true;std::optional<Address> target,cleanup;
        size_t previous=0,first=0;
        std::optional<uint64_t> error;
        for(size_t j=0;j<fields.size();++j) {
            const auto& store=*fields[j];
            if(store.value.number>=flow.calls.size()){valid=false;break;}
            const auto& call=flow.calls[store.value.number];
            if(!call.target||call.receiver.kind!=Value::Parcel||call.output.kind!=Value::Stack
                ||call.index>=store.index||(j&&call.index<=previous)||(target&&target!=call.target)){valid=false;break;}
            const auto extent=std::max(out.component_string.pointer,out.component_string.length)+8;
            if(!copies_result(flow,out.activities[j].offset,store.value.number,extent)){valid=false;break;}
            std::optional<Address> destructor;
            for(const auto& prior:flow.calls)if(prior.index>call.index&&prior.index<store.index
                &&prior.receiver.kind==Value::Self&&prior.receiver.offset==out.activities[j].offset) {
                if(destructor||!prior.target){valid=false;break;}destructor=prior.target;
            }
            if(!valid||!destructor||(cleanup&&cleanup!=destructor)){valid=false;break;}cleanup=destructor;
            std::optional<uint64_t> checked_error;
            for(const auto& compare:flow.comparisons)if(compare.field.kind==Value::Result
                &&compare.field.number==store.value.number&&compare.value.kind==Value::Constant
                &&compare.index>call.index&&compare.index<store.index) {
                if(checked_error){valid=false;break;}checked_error=compare.value.number;
            }
            if(!checked_error||(error&&error!=checked_error)){valid=false;break;}error=checked_error;
            if(!j)first=call.index;previous=call.index;target=call.target;
        }
        if(!valid||!target||!straight_region(*body,flow,first,fields.back()->index)
            ||!(reaches(graph,*target,*component_reader)||component_option_reader(graph,*target,*component_reader,out.activities[0].none,*error)))continue;
        const bool intent_produced=std::any_of(flow.stores.begin(),flow.stores.end(),[&](const auto& store) {
            if(store.address.kind!=Value::Self||store.address.offset!=out.base_intent.offset||store.width!=8
                ||store.value.kind!=Value::Result||store.value.offset!=0||store.value.number>=flow.calls.size())return false;
            const auto& call=flow.calls[store.value.number];
            return call.index<first&&call.target&&reaches(graph,*call.target,*intent_reader);
        });
        if(!intent_produced)return {};
        if(deserializer)return {};deserializer=target;out.reader=candidate;
    }
    if(!deserializer)return {};
    if(reason)*reason="activity field overlap/bounds";
    std::set<uint32_t> distinct;
    const auto extent=std::max(out.component_string.pointer,out.component_string.length)+8;
    for(auto field:out.activities)if(field.offset+extent>out.task_size||!distinct.insert(field.offset).second)return {};
    const auto intent_extent=std::max(out.intent_component.offset+extent,
        out.intent_package.offset+std::max(out.package_string.pointer,out.package_string.length)+8);
    if(out.base_intent.offset+intent_extent>out.task_size)return {};
    if(reason)*reason="ready";
    return out;
}
}
