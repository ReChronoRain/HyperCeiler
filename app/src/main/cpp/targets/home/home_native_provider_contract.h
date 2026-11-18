/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_trace.h"
#include <set>

namespace home_native_provider {
namespace sdk=home_layout::cards::sdk;
struct ByteVectorDrop {
    sdk::Address drop,table_slot;
    // For each word of the SDK's own temporary Vec, its RVec input byte field.
    std::array<uint32_t,3> input{};
    uint32_t table_field=0;
};
inline std::optional<sdk::Body> body_or_leaf(const sdk::Graph& graph,sdk::Address at) {
    auto body=graph.body(at);return body?std::move(body):graph.leaf(at);
}
inline bool stack_only(const sdk::Trace& flow) {
    return std::all_of(flow.stores.begin(),flow.stores.end(),[](const auto& s){return s.address.kind==sdk::Value::Stack;});
}
inline std::optional<ByteVectorDrop> byte_vector_drop(const sdk::Graph& graph,sdk::Address candidate) {
    const auto body=graph.body(candidate);if(!body||body->words.size()>64)return {};
    const auto flow=sdk::trace(graph,*body);
    if(!flow.returned||!flow.unknown.empty()||!flow.branches.empty()||flow.calls.size()!=2||!stack_only(flow))return {};
    const auto& elements=flow.calls[0];const auto& storage=flow.calls[1];
    if(!elements.target||!storage.target||elements.receiver.kind!=sdk::Value::Stack||elements.receiver!=storage.receiver)return {};
    const auto element_body=body_or_leaf(graph,*elements.target),storage_body=body_or_leaf(graph,*storage.target);
    if(!element_body||!storage_body)return {};
    const auto no_elements=sdk::trace(graph,*element_body),raw=sdk::trace(graph,*storage_body);
    if(!no_elements.returned||!no_elements.calls.empty()
        ||!std::all_of(no_elements.loads.begin(),no_elements.loads.end(),[](const auto& load){return load.address.kind==sdk::Value::Stack;})
        ||!no_elements.unknown.empty()||!no_elements.branches.empty()||!stack_only(no_elements))return {};
    // The actual RawVec adapter passes both element size and alignment as one.
    // This distinguishes byte storage from trivially-droppable wider elements.
    if(raw.regs[0].kind!=sdk::Value::Self||raw.regs[0].offset
        ||raw.regs[1].kind!=sdk::Value::Constant||raw.regs[1].number!=1
        ||raw.regs[2].kind!=sdk::Value::Constant||raw.regs[2].number!=1
        ||!graph.raw_callee(*storage_body,storage_body->words.size()-1,true))return {};
    ByteVectorDrop out;out.drop=candidate;std::array<bool,3> seen{};
    for(const auto& store:flow.stores) {
        const auto delta=store.address.offset-elements.receiver.offset;
        if(delta<0||delta>=24)continue;
        if(store.width!=8||delta%8||store.value.kind!=sdk::Value::Field
            ||store.value.offset<0||store.value.offset>=32||store.value.offset%8)return {};
        const auto index=static_cast<size_t>(delta/8);if(seen[index])return {};
        seen[index]=true;out.input[index]=static_cast<uint32_t>(store.value.offset);
    }
    if(!std::all_of(seen.begin(),seen.end(),[](bool value){return value;}))return {};
    std::set<uint32_t> fields(out.input.begin(),out.input.end());if(fields.size()!=3)return {};
    // Current supported repr(C) SDK RVec is four native words. The table word
    // is the remaining field, rather than an assumed fixed table index.
    for(uint32_t offset=0;offset<32;offset+=8)if(!fields.contains(offset))out.table_field=offset;
    return out;
}
inline std::optional<ByteVectorDrop> table_drop(const sdk::Graph& graph,sdk::Address table) {
    std::optional<ByteVectorDrop> out;
    // Bounded metadata inspection, not a hard-coded WithMetadata header size.
    // Only relocation-backed executable targets with the complete byte drop
    // data flow can authorize a callback.
    for(unsigned word=0;word<32;++word) {
        const sdk::Address slot{table.image,table.va+word*8};
        const auto target=graph.data_reference(slot);if(!target)continue;
        auto candidate=byte_vector_drop(graph,*target);if(!candidate)continue;
        if(out&&out->drop!=candidate->drop)return {};
        candidate->table_slot=slot;out=*candidate;
    }
    return out;
}
struct ErrorContract {
    sdk::Address constructor,table,drop,table_slot;
    uint32_t code=0,vector=0,table_field=0,size=0;
};
inline std::optional<ErrorContract> error_contract(const sdk::Graph& graph,sdk::Address at,std::string_view* reason=nullptr) {
    if(reason)*reason="error constructor straight-line data flow";
    const auto body=graph.body(at);if(!body||body->words.size()>96)return {};
    const auto flow=sdk::trace(graph,*body,true,true);
    if(!flow.returned||!flow.unknown.empty()||!flow.branches.empty()||flow.calls.size()!=3)return {};
    const auto& code=flow.calls[0];const auto& description=flow.calls[1];const auto& status_drop=flow.calls[2];
    if(reason)*reason="native Status producer/consumer";
    if(!code.target||!description.target||!status_drop.target||code.receiver.kind!=sdk::Value::Stack
        ||code.receiver!=description.receiver||code.receiver!=status_drop.receiver
        ||description.output.kind!=sdk::Value::Stack)return {};
    const auto native_status=body_or_leaf(graph,*status_drop.target);if(!native_status||native_status->words.empty())return {};
    const auto tail=graph.raw_callee(*native_status,native_status->words.size()-1,true);
    if(!tail||graph.import_name(*tail)!=std::optional<std::string_view>("AStatus_delete"))return {};
    auto status_prefix=*native_status;status_prefix.words.pop_back();
    const auto status_flow=sdk::trace(graph,status_prefix);
    if(!status_flow.calls.empty()||!status_flow.unknown.empty()||!status_flow.branches.empty()
        ||!stack_only(status_flow)||status_flow.regs[0].kind!=sdk::Value::Field
        ||status_flow.regs[0].offset||status_flow.regs[0].width!=8)return {};
    unsigned status_input=0;
    for(const auto& store:flow.stores)if(store.address==code.receiver&&store.width==8
        &&store.value.kind==sdk::Value::Parcel&&!store.value.offset)++status_input;
    if(status_input!=1)return {};
    if(reason)*reason="code field and literal vector table";
    std::optional<sdk::Address> table;uint32_t table_at=0;std::optional<uint32_t> code_at;
    for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
        if(store.address.offset<0||store.address.offset>128)return {};
        if(store.value.kind==sdk::Value::Literal&&store.width==8&&store.value.offset>=0) {
            if(table)return {};table=sdk::Address{static_cast<size_t>(store.value.offset),store.value.number};
            table_at=static_cast<uint32_t>(store.address.offset);
        }
        if(store.value.kind==sdk::Value::ScalarResult&&store.value.number==0&&store.width==4) {
            if(code_at)return {};code_at=static_cast<uint32_t>(store.address.offset);
        }
    }
    if(!table||!code_at)return {};
    if(reason)*reason="relocation-backed byte vector drop";
    const auto drop=table_drop(graph,*table);if(!drop||table_at<drop->table_field)return {};
    const uint32_t vector=table_at-drop->table_field;
    if(vector%8||*code_at+4>vector)return {};
    if(reason)*reason="complete error payload bounds";
    for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
        for(unsigned byte=0;byte<store.width;++byte) {
            const auto target=store.address.offset+byte;
            if(!(target>=*code_at&&target<*code_at+4)
                &&!(target>=vector&&target<vector+32))return {};
        }
    }
    // Prove that the producer's three description words are exactly the inverse
    // of the selected callback's temporary-Vec move, before invoking that SDK
    // destructor on an error returned by the same SDK generation.
    std::array<bool,3> words{};
    if(reason)*reason="inverse description/byte-vector moves";
    for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
        if(store.width%8&&store.address.offset!=*code_at)return {};
        for(unsigned byte=0;byte<store.width;byte+=8) {
            const auto target=store.address.offset+byte;
            for(size_t word=0;word<3;++word)if(target==vector+drop->input[word]) {
                if(store.value.kind!=sdk::Value::Result||store.value.number!=1
                    ||store.value.offset+byte!=int64_t(word*8)||words[word])return {};
                words[word]=true;
            }
        }
    }
    if(!std::all_of(words.begin(),words.end(),[](bool value){return value;}))return {};
    if(reason)*reason="ready";
    return ErrorContract{at,*table,drop->drop,drop->table_slot,*code_at,vector,drop->table_field,vector+32};
}
inline std::optional<ErrorContract> provider_error(const sdk::Graph& graph,std::string_view name) {
    const auto at=graph.exported(name);if(!at)return {};
    const auto body=graph.body(*at);if(!body)return {};
    const auto flow=sdk::trace(graph,*body,false,true);std::optional<ErrorContract> found;
    for(const auto& call:flow.calls) {
        if(!call.target||call.output.kind!=sdk::Value::Self)continue;
        const auto candidate=error_contract(graph,*call.target);if(!candidate)continue;
        if(found)return {};found=*candidate;
    }
    return found;
}
inline std::optional<ErrorContract> acquire_error(const sdk::Graph& graph) {
    return provider_error(graph,"content_resolver_acquire_unstable_content_provider_client");
}
// Enumerate only returns in the current unwind span. The final allocator-error
// call may not return; it produces no reply and cannot authorize a layout.
inline std::optional<std::vector<std::vector<size_t>>> return_paths(const sdk::Graph& graph,const sdk::Body& body) {
    if(body.words.empty()||body.words.size()>256)return {};
    struct Pending {size_t pc;std::vector<size_t> path;};
    std::vector<Pending> pending{{0,{}}};std::vector<std::vector<size_t>> returned;
    while(!pending.empty()) {
        auto current=std::move(pending.back());pending.pop_back();
        for(;;) {
            if(current.pc>=body.words.size()) {
                if(current.pc!=body.words.size()||!graph.raw_callee(body,body.words.size()-1))return {};
                break;
            }
            const auto index=current.pc;const auto word=body.words[index];current.path.push_back(index);
            if(word==0xd65f03c0u){returned.push_back(std::move(current.path));break;}
            int64_t target=-1;bool conditional=false,branch=true;
            if((word&0xfc000000u)==0x14000000u)target=int64_t(index)+(int32_t(word<<6)>>6);
            else if((word&0xff000010u)==0x54000000u||(word&0x7e000000u)==0x34000000u)
                {target=int64_t(index)+(int32_t(word<<8)>>13);conditional=true;}
            else if((word&0x7e000000u)==0x36000000u)
                {target=int64_t(index)+(int32_t(word<<13)>>18);conditional=true;}
            else if((word&0xfffffc1fu)==0xd61f0000u)return {};
            else branch=false;
            if(branch) {
                if(target<=int64_t(index)||target>=int64_t(body.words.size()))return {};
                if(conditional)pending.push_back({index+1,current.path});current.pc=static_cast<size_t>(target);
            } else ++current.pc;
            if(pending.size()+returned.size()>32)return {};
        }
    }
    return returned;
}
struct AcquireContract {
    // Supported C ABI reply is 48 bytes, returned indirectly in AAPCS64 X8.
    // Every field's role, offset and discriminant is derived below.
    static constexpr uint32_t bytes=48;
    sdk::Address acquire,create_resolver,delete_resolver,delete_client;
    ErrorContract error;
    uint32_t tag=0,option=0,pointer=0,error_at=0;
    uint8_t accepted=0,rejected=0,present=0,absent=0;
};
struct CallContract {
    static constexpr uint32_t bytes=48;
    sdk::Address call;
    ErrorContract error;
    uint32_t tag=0,pointer=0,error_at=0;
    uint32_t arg_tag=0,arg_pointer=0,arg_length=0;
    uint8_t accepted=0,rejected=0;
};
// call() returns Result<nullable Bundle pointer, Error>, not acquire()'s
// Result<Option<Client>, Error>. Equal result sizes do not imply equal layouts.
inline std::optional<CallContract> resolve_call(const sdk::Graph& graph) {
    const auto at=graph.exported("ContentProviderClient_call");
    const auto error=provider_error(graph,"ContentProviderClient_call");
    if(!at||!error)return {};
    const auto body=graph.body(*at);if(!body)return {};
    const auto paths=return_paths(graph,*body);if(!paths||paths->size()!=3)return {};
    CallContract out;out.call=*at;out.error=*error;
    // Supported C ABI: Client*, method RStr in X1/X2, pointer to optional RStr
    // in X3, nullable Bundle* in X4. Validate the complete adapter before any
    // foreign call, including the Bundle forwarded to the private sixth arg.
    const std::array<std::pair<unsigned,sdk::Value>,4> inputs={{{1,{sdk::Value::Constant,0,11}},
        {2,{sdk::Value::Constant,0,13}},{3,{sdk::Value::Self}},{4,{sdk::Value::Constant,0,17}}}};
    const auto input=sdk::trace(graph,*body,true,false,{},inputs);
    if(input.calls.empty()||input.selects.size()!=1)return {};
    const auto& invoke=input.calls.front();const auto& selection=input.selects.front();
    if(invoke.receiver.kind!=sdk::Value::Parcel||invoke.parcel!=inputs[0].second||invoke.third!=inputs[1].second
        ||invoke.fourth.kind!=sdk::Value::Selected||invoke.fourth.number!=0||invoke.sixth!=inputs[3].second
        ||selection.condition!=1||selection.yes.kind!=sdk::Value::Constant||selection.yes.number!=0
        ||selection.no.kind!=sdk::Value::Field||selection.no.width!=8
        ||selection.field.kind!=sdk::Value::Field||selection.field.width!=4
        ||selection.value.kind!=sdk::Value::Constant||selection.value.number!=0
        ||invoke.fifth.kind!=sdk::Value::Field||invoke.fifth.width!=8)return {};
    if(selection.field.offset<0||selection.no.offset<0||invoke.fifth.offset<0
        ||selection.field.offset>=32||selection.no.offset+8>32||invoke.fifth.offset+8>32
        ||selection.no.offset==invoke.fifth.offset)return {};
    out.arg_tag=static_cast<uint32_t>(selection.field.offset);
    out.arg_pointer=static_cast<uint32_t>(selection.no.offset);
    out.arg_length=static_cast<uint32_t>(invoke.fifth.offset);
    if(std::count_if(input.loads.begin(),input.loads.end(),[&](const sdk::Load& load) {
        return load.address.kind==sdk::Value::Self&&load.address.offset==selection.field.offset&&load.width==1;
    })!=1)return {};
    if((out.arg_tag>=out.arg_pointer&&out.arg_tag<out.arg_pointer+8)
        ||(out.arg_tag>=out.arg_length&&out.arg_tag<out.arg_length+8))return {};
    std::optional<uint32_t> tag_at,pointer_at;
    unsigned errors=0,present=0,absent=0;
    for(const auto& path:*paths) {
        const auto flow=sdk::trace(graph,*body,true,true,path);
        if(!flow.returned||!flow.unknown.empty())return {};
        std::optional<uint64_t> tag;const sdk::Store* pointer=nullptr;
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
            if(store.address.offset<0||uint64_t(store.address.offset)+store.width>CallContract::bytes)return {};
            const auto offset=static_cast<uint32_t>(store.address.offset);
            if(store.width==1&&store.value.kind==sdk::Value::Constant&&store.value.number<=1) {
                if(tag||(tag_at&&*tag_at!=offset))return {};
                tag_at=offset;tag=store.value.number;
            } else if(store.width==8) {
                if(pointer||(pointer_at&&*pointer_at!=offset)||offset%8)return {};
                pointer=&store;
            } else return {};
        }
        if(!tag)return {};
        bool failed=false;
        for(const auto& call:flow.calls)if(call.target==error->constructor&&call.output.kind==sdk::Value::Self) {
            if(failed||call.output.offset<0||uint64_t(call.output.offset)+error->size!=CallContract::bytes)return {};
            out.error_at=static_cast<uint32_t>(call.output.offset);failed=true;
        }
        if(failed) {
            if(pointer)return {};
            out.rejected=static_cast<uint8_t>(*tag);++errors;continue;
        }
        if(!pointer||(present+absent&&out.accepted!=*tag))return {};
        out.accepted=static_cast<uint8_t>(*tag);pointer_at=static_cast<uint32_t>(pointer->address.offset);
        if(pointer->value.kind==sdk::Value::Constant&&pointer->value.number==0)++absent;
        else if(pointer->value.kind==sdk::Value::Returned)++present;
        else return {};
    }
    if(!tag_at||!pointer_at||errors!=1||present!=1||absent!=1||out.accepted==out.rejected
        ||(*tag_at>=*pointer_at&&*tag_at<*pointer_at+8))return {};
    out.tag=*tag_at;out.pointer=*pointer_at;return out;
}
inline bool resolver_handle_reference(const sdk::Graph& graph,sdk::Address at) {
    const auto body=graph.body(at);if(!body||body->words.size()>128)return false;
    const auto flow=sdk::trace(graph,*body);
    // The C API takes &Shared<Runtime>, not the raw Runtime value returned by
    // Runtime_get_context. Confirm its first native-word load and propagation
    // to the SDK's clone/conversion operations. We never infer Arc header bytes.
    std::optional<sdk::Value> handle;
    for(const auto& load:flow.loads)if(load.address.kind==sdk::Value::Self) {
        if(handle||load.address.offset||load.width!=8)return false;
        handle=sdk::Value{sdk::Value::Field,0,0,8};
    }
    return handle&&flow.calls.size()>=3&&flow.calls[0].receiver==*handle&&flow.calls[1].receiver==*handle;
}
inline std::optional<uint64_t> boolean(const sdk::Trace& flow,sdk::Value value,sdk::Value pointer) {
    if(value.kind==sdk::Value::Constant)return value.number;
    if(value.kind!=sdk::Value::Selected||value.number>=flow.selects.size())return {};
    const auto& selection=flow.selects[value.number];
    if(selection.condition!=1||selection.yes.kind!=sdk::Value::Constant||selection.yes.number!=0
        ||selection.no.kind!=sdk::Value::Constant||selection.no.number!=1
        ||selection.value.kind!=sdk::Value::Constant||selection.value.number!=0)return {};
    if(selection.field.kind==sdk::Value::Constant)return selection.field.number==0?1:0;
    if(selection.field.kind==sdk::Value::Returned&&pointer.kind==sdk::Value::Returned
        &&selection.field.number==pointer.number&&selection.field.offset==pointer.offset)return 0;
    return {};
}
inline std::optional<AcquireContract> resolve(const sdk::Graph& graph,std::string_view* reason=nullptr) {
    if(reason)*reason="public provider C ABI";
    const auto at=graph.exported("content_resolver_acquire_unstable_content_provider_client"),
        create=graph.exported("content_resolver_new"),destroy=graph.exported("content_resolver_delete"),
        client=graph.exported("ContentProviderClient_delete");
    const auto error=acquire_error(graph);
    if(!at||!create||!destroy||!client||!error)return {};
    if(reason)*reason="resolver input is a reference to the retained handle";
    if(!resolver_handle_reference(graph,*create))return {};
    const auto body=graph.body(*at);if(!body)return {};
    const auto paths=return_paths(graph,*body);if(!paths||paths->size()!=3)return {};
    if(reason)*reason="all returned discriminants and owned payloads";
    AcquireContract out;out.acquire=*at;out.create_resolver=*create;out.delete_resolver=*destroy;
    out.delete_client=*client;out.error=*error;
    std::vector<sdk::Trace> flows;
    for(const auto& path:*paths) {
        auto flow=sdk::trace(graph,*body,true,true,path);
        if(!flow.returned||!flow.unknown.empty())return {};
        flows.push_back(std::move(flow));
    }
    std::set<uint32_t> common;bool first=true;
    for(const auto& flow:flows) {
        std::set<uint32_t> tags;
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self) {
            if(store.address.offset<0||uint64_t(store.address.offset)+store.width>AcquireContract::bytes)return {};
            if(store.width==1&&store.value.kind==sdk::Value::Constant&&store.value.number<=1)
                tags.insert(static_cast<uint32_t>(store.address.offset));
        }
        if(first){common=std::move(tags);first=false;}
        else {
            for(auto it=common.begin();it!=common.end();)if(!tags.contains(*it))it=common.erase(it);else ++it;
        }
    }
    if(common.size()!=1)return {};out.tag=*common.begin();
    unsigned errors=0,present=0,absent=0;std::optional<uint32_t> pointer_at,option_at;
    for(const auto& flow:flows) {
        std::optional<uint64_t> tag;bool failed=false;
        for(const auto& call:flow.calls)if(call.target==error->constructor&&call.output.kind==sdk::Value::Self) {
            if(failed||call.output.offset<0||uint64_t(call.output.offset)+error->size!=AcquireContract::bytes)return {};
            out.error_at=static_cast<uint32_t>(call.output.offset);failed=true;
        }
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self&&store.address.offset==out.tag) {
            if(tag||store.width!=1||store.value.kind!=sdk::Value::Constant||store.value.number>1)return {};
            tag=store.value.number;
        }
        if(!tag)return {};
        if(failed){out.rejected=static_cast<uint8_t>(*tag);++errors;continue;}
        if(present+absent&&out.accepted!=*tag)return {};out.accepted=static_cast<uint8_t>(*tag);
        const sdk::Store* pointer=nullptr;const sdk::Store* option=nullptr;
        for(const auto& store:flow.stores)if(store.address.kind==sdk::Value::Self&&store.address.offset!=out.tag) {
            if(store.width==8){if(pointer)return {};pointer=&store;}
            else if(store.width==1){if(option)return {};option=&store;}
            else return {};
        }
        if(!pointer||!option)return {};
        const auto p=static_cast<uint32_t>(pointer->address.offset),o=static_cast<uint32_t>(option->address.offset);
        if((pointer_at&&*pointer_at!=p)||(option_at&&*option_at!=o)||p%8||(o>=p&&o<p+8))return {};
        pointer_at=p;option_at=o;
        const auto flag=boolean(flow,option->value,pointer->value);if(!flag||*flag>1)return {};
        if(pointer->value.kind==sdk::Value::Constant&&pointer->value.number==0){out.absent=static_cast<uint8_t>(*flag);++absent;}
        else if(pointer->value.kind==sdk::Value::Returned){out.present=static_cast<uint8_t>(*flag);++present;}
        else return {};
    }
    if(errors!=1||present!=1||absent!=1||out.accepted==out.rejected||out.present==out.absent
        ||!pointer_at||!option_at)return {};
    out.pointer=*pointer_at;out.option=*option_at;if(reason)*reason="ready";return out;
}
}
