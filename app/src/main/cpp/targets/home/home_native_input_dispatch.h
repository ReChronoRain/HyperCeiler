/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_status.h"
#include "home_native_input_callback.h"
namespace home_native_input {
// Enumerate bounded acyclic witnesses to a consumer call. Back edges belong
// to input draining and lock retry, not a guessed new callback target. An
// indirect call ends a witness, so one callback cannot borrow another's state.
inline std::optional<std::vector<std::vector<size_t>>> input_paths_to(const sdk::Body& body,size_t goal) {
    if(body.words.size()>1024||goal>=body.words.size())return {};
    const auto edges=[&](size_t at) {
        std::vector<size_t> next;const auto w=body.words[at];bool conditional=false;int64_t target=-1;
        if(w==0xd65f03c0u||(w&0xfffffc1fu)==0xd61f0000u||(w&0xfffffc1fu)==0xd63f0000u)return next;
        if((w&0xfc000000u)==0x14000000u)target=int64_t(at)+(int32_t(w<<6)>>6);
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)
            {target=int64_t(at)+(int32_t(w<<8)>>13);conditional=true;}
        else if((w&0x7e000000u)==0x36000000u)
            {target=int64_t(at)+(int32_t(w<<13)>>18);conditional=true;}
        else next.push_back(at+1);
        if(target>int64_t(at)&&target<=int64_t(goal))next.push_back(size_t(target));
        if(conditional)next.push_back(at+1);return next;
    };
    std::vector<bool> reachable(goal+1);reachable[goal]=true;
    for(size_t at=goal;at-->0;)for(auto next:edges(at))if(next<=goal&&reachable[next])reachable[at]=true;
    if(!reachable[0])return std::vector<std::vector<size_t>>{};
    std::vector<std::vector<size_t>> active{{0}},result;
    while(!active.empty()) {
        auto path=std::move(active.back());active.pop_back();const auto at=path.back();
        if(at==goal){result.push_back(std::move(path));continue;}
        for(auto next:edges(at))if(next<=goal&&reachable[next]) {
            auto following=path;following.push_back(next);active.push_back(std::move(following));
        }
        if(active.size()+result.size()>256)return {};
    }
    return result;
}
inline bool input_constants_match(const sdk::Graph& graph,const sdk::Body& body,const sdk::Trace& flow,std::span<const size_t> path) {
    for(const auto& branch:flow.branch_comparisons) {
        if(branch.field.kind!=sdk::Value::Constant||branch.value.kind!=sdk::Value::Constant)continue;
        const auto lhs=branch.field.width==4?uint64_t(uint32_t(branch.field.number)):branch.field.number;
        const auto rhs=branch.field.width==4?uint64_t(uint32_t(branch.value.number)):branch.value.number;
        const auto signed_lhs=branch.field.width==4?int64_t(int32_t(lhs)):int64_t(lhs);
        const auto signed_rhs=branch.field.width==4?int64_t(int32_t(rhs)):int64_t(rhs);
        const auto instruction=body.words[branch.index];std::optional<bool> taken;
        switch(instruction&15) {
            case 0:taken=lhs==rhs;break;case 1:taken=lhs!=rhs;break;
            case 2:taken=lhs>=rhs;break;case 3:taken=lhs<rhs;break;
            case 8:taken=lhs>rhs;break;case 9:taken=lhs<=rhs;break;
            case 10:taken=signed_lhs>=signed_rhs;break;case 11:taken=signed_lhs<signed_rhs;break;
            case 12:taken=signed_lhs>signed_rhs;break;case 13:taken=signed_lhs<=signed_rhs;break;
            default:return false;
        }
        const auto at=std::find(path.begin(),path.end(),branch.index);
        if(at==path.end()||at+1==path.end())return false;
        const auto target=int64_t(branch.index)+(int32_t(instruction<<8)>>13);
        if(*taken!=(int64_t(*(at+1))==target))return false;
    }
    for(const auto branch:flow.branches) {
        const auto instruction=body.words[branch];
        if((instruction&0x7e000000u)!=0x34000000u&&(instruction&0x7e000000u)!=0x36000000u)continue;
        const auto at=std::find(path.begin(),path.end(),branch);
        if(at==path.end()||at+1==path.end())return false;
        const std::vector<size_t> prefix(path.begin(),at);
        auto value=sdk::trace(graph,body,false,false,prefix).regs[instruction&31];
        if(value.kind!=sdk::Value::Constant)continue;
        const bool bit_test=(instruction&0x7e000000u)==0x36000000u;
        bool zero=false;int64_t target;
        if(bit_test) {
            const unsigned bit=((instruction>>19)&31)|((instruction>>26)&32);
            zero=!(value.number&(uint64_t(1)<<bit));
            target=int64_t(branch)+(int32_t(instruction<<13)>>18);
        } else {
            if(!(instruction&(1u<<31)))value.number&=UINT32_MAX;
            zero=value.number==0;target=int64_t(branch)+(int32_t(instruction<<8)>>13);
        }
        const bool taken=zero==!(instruction&(1u<<24));
        if(taken!=(int64_t(*(at+1))==target))return false;
    }
    return true;
}
struct OwnedEventContract {
    sdk::Address get_id,drop,destructor;
    uint32_t bytes=0,alignment=0,id_field=0;
};
// Public SDK wrappers sometimes forward through several crates. Only
// transparent stack-only tail wrappers may be crossed; unknown code stops.
inline std::optional<sdk::Body> input_forwarded(const sdk::Graph& graph,sdk::Address entry) {
    for(unsigned depth=0;depth<6;++depth) {
        const auto body=graph.body(entry);if(!body||body->words.empty())return {};
        const auto tail=graph.callee(*body,body->words.size()-1,true);
        if(!tail)return body;
        auto prefix=*body;prefix.words.pop_back();const auto flow=sdk::trace(graph,prefix);
        if(!flow.calls.empty()) {
            if(flow.calls.size()!=1||!flow.calls[0].target||flow.calls[0].receiver!=sdk::Value{sdk::Value::Self}
                ||flow.regs[0]!=sdk::Value{sdk::Value::Returned,0,0,8})return body;
            const auto helper=home_native_provider::body_or_leaf(graph,*flow.calls[0].target);if(!helper)return {};
            const auto identity=sdk::trace(graph,*helper);
            if(!identity.returned||identity.regs[0]!=sdk::Value{sdk::Value::Self}||!identity.unknown.empty()
                ||!identity.calls.empty()||!identity.branches.empty()||!home_native_provider::stack_only(identity))return body;
            for(const auto& load:identity.loads)if(load.address.kind!=sdk::Value::Stack)return body;
            if(!flow.unknown.empty()||!flow.branches.empty()||!home_native_provider::stack_only(flow))return {};
            entry=*tail;continue;
        }
        if(!flow.unknown.empty()||!flow.branches.empty()||!home_native_provider::stack_only(flow)
            ||flow.regs[0]!=sdk::Value{sdk::Value::Self})return {};
        entry=*tail;
    }
    return {};
}
inline std::optional<OwnedEventContract> owned_event(const sdk::Graph& graph,std::string_view id_name,std::string_view drop_name) {
    const auto get_id=graph.exported(id_name),drop=graph.exported(drop_name);
    if(!get_id||!drop)return {};
    const auto getter=input_forwarded(graph,*get_id),destructor=input_forwarded(graph,*drop);
    if(!getter||!destructor||destructor->words.empty())return {};
    const auto id=sdk::trace(graph,*getter);
    if(!id.returned||!id.unknown.empty()||!id.calls.empty()||!id.branches.empty()
        ||!home_native_provider::stack_only(id)||id.regs[0].kind!=sdk::Value::Field
        ||id.regs[0].width!=4||id.regs[0].offset<0||id.regs[0].offset>4096)return {};
    const auto tail=graph.callee(*destructor,destructor->words.size()-1,true);if(!tail)return {};
    auto prefix=*destructor;prefix.words.pop_back();const auto release=sdk::trace(graph,prefix);
    if(!release.unknown.empty()||!release.branches.empty()||release.calls.size()!=1||!release.calls[0].target
        ||release.calls[0].receiver!=sdk::Value{sdk::Value::Self}||!home_native_provider::stack_only(release)
        ||release.regs[0]!=sdk::Value{sdk::Value::Self}||release.regs[1].kind!=sdk::Value::Constant
        ||!release.regs[1].number||release.regs[1].number>4096
        ||release.regs[2]!=sdk::Value{sdk::Value::Constant,0,alignof(void*)}
        ||uint64_t(id.regs[0].offset)+4>release.regs[1].number)return {};
    return OwnedEventContract{*get_id,*drop,destructor->address,uint32_t(release.regs[1].number),
        uint32_t(release.regs[2].number),uint32_t(id.regs[0].offset)};
}
struct DispatchContract {
    sdk::Address consume;
    uint32_t key_slot=0,motion_slot=0,batch_slot=0;
    uint32_t focus_slot=0,drag_slot=0;
    std::vector<uint32_t> automatic_slots;
    OwnedEventContract key,motion;
};
inline bool input_pointer(sdk::Value value,sdk::Value::Kind kind) {
    // A native pointer reloaded from a stack word has width 8; its original
    // abstract argument has unspecified width. A narrowed scalar is neither.
    return (value.width==0||value.width==8)&&value==sdk::Value{kind,0,0,value.width};
}
inline std::optional<DispatchContract> input_dispatch(const sdk::Graph& graph) {
    const auto entry=graph.exported("input_InputEventReceiver_consume_batched_input_events");
    const auto key=owned_event(graph,"input_KeyEvent_getId","input_KeyEvent_drop"),
        motion=owned_event(graph,"input_MotionEvent_getId","input_MotionEvent_drop");
    if(!entry||!key||!motion||key->bytes==motion->bytes)return {};
    const auto bridge=graph.body(*entry);if(!bridge)return {};
    const auto call=sdk::trace(graph,*bridge);
    if(call.calls.size()!=1||!call.calls[0].target)return {};
    const auto body=graph.body(*call.calls[0].target);if(!body)return {};
    DispatchContract result;result.consume=*entry;result.key=*key;result.motion=*motion;
    std::optional<uint32_t> table_field;unsigned sites=0;
    for(size_t goal=0;goal<body->words.size();++goal)if((body->words[goal]&0xfffffc1fu)==0xd63f0000u) {
        ++sites;const auto paths=input_paths_to(*body,goal);if(!paths||paths->empty())return {};
        std::optional<uint32_t> scalar_slot;bool owned=false;
        for(auto path:*paths) {
            path.pop_back();const auto flow=sdk::trace(graph,*body,false,false,path);
            if(!input_constants_match(graph,*body,flow,path))continue;
            const auto target=flow.regs[(body->words[goal]>>5)&31];
            if(target.kind!=sdk::Value::IndirectField||target.width!=8||target.offset<0
                ||target.offset>4096||!target.number||target.number>4096||target.number%sizeof(void*)
                ||!input_pointer(flow.regs[1],sdk::Value::Parcel)||!input_pointer(flow.regs[2],sdk::Value::Self))return {};
            if(table_field&&*table_field!=target.offset)return {};table_field=uint32_t(target.offset);
            const auto slot=uint32_t(target.number/sizeof(void*));const auto event=flow.regs[3];
            if(event.kind!=sdk::Value::Returned) {
                if(scalar_slot&&*scalar_slot!=slot)return {};scalar_slot=slot;continue;
            }
            if(event.offset||event.number>=flow.calls.size())return {};
            const auto& allocation=flow.calls[event.number];
            if(!allocation.target||allocation.receiver.kind!=sdk::Value::Constant
                ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,alignof(void*)})return {};
            bool copied=false;
            for(const auto& copy:flow.calls)if(copy.index>allocation.index&&copy.linkage=="memcpy"
                &&copy.receiver==event&&copy.parcel.kind==sdk::Value::Stack&&copy.third==allocation.receiver)copied=true;
            if(!copied)return {};
            auto* destination=allocation.receiver.number==key->bytes?&result.key_slot:
                allocation.receiver.number==motion->bytes?&result.motion_slot:nullptr;
            if(!destination||(*destination&&*destination!=slot))return {};
            *destination=slot;owned=true;
        }
        if(owned){if(scalar_slot)return {};continue;}
        if(!scalar_slot)return {};
        // Non-event callbacks followed by the SDK's sequence completion own
        // no event allocation. The pending-batch callback instead sets a
        // receiver byte before dispatch and leaves draining to its listener.
        bool automatic=false;
        for(const auto& path:*paths) {
            auto after=path;
            for(size_t at=goal+1;at<body->words.size()&&at<=goal+12;++at) {
                const auto instruction=body->words[at];after.push_back(at);
                if((instruction&0xfc000000u)==0x94000000u) {
                    const auto flow=sdk::trace(graph,*body,false,false,after);
                    const auto& completion=flow.calls.back();
                    automatic=input_pointer(completion.receiver,sdk::Value::Self)
                        &&input_pointer(completion.parcel,sdk::Value::Parcel)
                        &&completion.fourth==sdk::Value{sdk::Value::Constant,0,1};
                    break;
                }
                if((instruction&0x7c000000u)==0x14000000u||instruction==0xd65f03c0u)break;
            }
            if(automatic)break;
        }
        if(automatic) {
            bool x=false,y=false;
            for(size_t at=goal>12?goal-12:0;at<goal;++at) {
                const auto instruction=body->words[at];
                if((instruction&0xfffffc1fu)==0x1e204000u)x=true; // FMOV S0, Sn.
                if((instruction&0xfffffc1fu)==0x1e270001u)y=true; // FMOV S1, Wn.
            }
            if(x!=y||goal==0)return {};
            const auto boolean=body->words[goal-1];
            if((boolean&0xff80001fu)!=0x12000003u||sdk::logical_immediate(boolean)!=1)return {};
            auto& slot=x?result.drag_slot:result.focus_slot;if(slot)return {};slot=*scalar_slot;
            result.automatic_slots.push_back(*scalar_slot);
        }
        else {
            bool pending=false;
            for(auto path:*paths) {
                path.pop_back();const auto flow=sdk::trace(graph,*body,false,false,path);
                for(const auto& store:flow.stores)if(store.index+16>=goal&&store.address.kind==sdk::Value::Self
                    &&store.address.offset>=0&&store.address.offset<=4096&&store.width==1
                    &&store.value==sdk::Value{sdk::Value::Constant,0,1,1})pending=true;
            }
            if(!pending||result.batch_slot)return {};result.batch_slot=*scalar_slot;
        }
    }
    if(sites!=4||!result.key_slot||!result.motion_slot||result.key_slot==result.motion_slot
        ||!result.batch_slot||!result.focus_slot||!result.drag_slot||result.automatic_slots.size()!=2)return {};
    return result;
}
}
