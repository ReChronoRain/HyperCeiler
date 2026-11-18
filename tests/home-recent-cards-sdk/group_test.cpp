/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "fixture.h"
#include "targets/home/home_recent_cards_query_contract.h"
#include "targets/home/home_recent_cards_reply.h"
#include <iostream>

static void test_reply(const GroupContract& group,const TaskContract& task,const QueryContract& query) {
    using namespace home_layout::cards;
    auto* hidden=new std::byte[group.size]();auto* shown=new std::byte[group.size]();Bytes member(task.task_size);
    constexpr char name[]="hidden.app";
    const auto put=[](std::byte* bytes,uint32_t offset,uint64_t value){std::memcpy(bytes+offset,&value,8);};
    const auto address=[](const auto& value){return reinterpret_cast<uintptr_t>(value.data());};
    for(auto* root:{hidden,shown}) {
        int32_t type=0;std::memcpy(root+group.type,&type,4);
        put(root,group.tasks.capacity,group.tasks.none);put(root,group.children.capacity,group.children.none);
    }
    for(auto field:task.activities)put(member.data(),field.offset,field.none);
    put(member.data(),task.base_intent.offset,task.base_intent.none);
    put(member.data(),task.activities[3].offset,0);
    put(member.data(),task.activities[3].offset+task.component_string.pointer,reinterpret_cast<uintptr_t>(name));
    put(member.data(),task.activities[3].offset+task.component_string.length,sizeof(name)-1);
    put(hidden,group.tasks.capacity,1);put(hidden,group.tasks.pointer,address(member));put(hidden,group.tasks.length,1);
    std::array<void*,8> roots{hidden,nullptr,shown};const auto before=roots;
    std::array<uint64_t,4> table{};const auto table_address=address(table);
    Reply reply{};reply.words[query.pointer/8]=address(roots);reply.words[query.length/8]=3;
    reply.words[query.capacity/8]=roots.size();reply.words[query.vtable/8]=table_address;const auto original=reply;
    auto selected=std::make_shared<Selection>();selected->packages={name};
    const auto read=[&](uintptr_t at,std::span<std::byte> output) {
        const std::array<std::span<const std::byte>,5> ranges={std::span(hidden,group.size),std::span(shown,group.size),member,
            std::as_bytes(std::span(name)),std::as_bytes(std::span(roots))};
        for(const auto range:ranges){const auto start=address(range);
            if(at>=start&&at-start<=range.size()&&output.size()<=range.size()-(at-start)) {
                std::memcpy(output.data(),range.data()+at-start,output.size());return true;
            }
        }return false;
    };
    const auto drop=+[](void* value){delete[] static_cast<std::byte*>(value);};
    auto reject=[](auto,const auto&,auto,size_t&){return false;};
    assert(!filter_reply(reply,query,group,task,table_address,selected,read,reject,drop));
    assert(reply.words==original.words&&roots==before);
    auto stale=std::make_shared<Selection>(*selected);
    auto stale_commit=[&](auto owned,const auto& plan,auto destroy,size_t& kept){
        const auto result=commit(owned,plan,stale,destroy);if(!result)return false;kept=*result;return true;
    };
    assert(!filter_reply(reply,query,group,task,table_address,selected,read,stale_commit,drop));
    assert(!filter_reply(reply,query,group,task,table_address,selected,[](auto,auto){return false;},reject,drop));
    assert(reply.words==original.words&&roots==before);
    for(const auto field:{query.capacity,query.vtable}){
        reply=original;reply.words[field/8]=0;
        assert(!filter_reply(reply,query,group,task,table_address,selected,read,reject,drop));assert(roots==before);
    }
    reply=original;
    assert(filter_reply(reply,query,group,task,table_address,std::make_shared<Selection>(),[](auto,auto){assert(false);return false;},reject,drop)==0);
    put(shown,group.tasks.capacity,1);put(shown,group.tasks.pointer,address(member));put(shown,group.tasks.length,2);
    assert(!filter_reply(reply,query,group,task,table_address,selected,read,reject,drop));
    assert(reply.words==original.words&&roots==before);put(shown,group.tasks.capacity,group.tasks.none);
    size_t commits=0;
    auto apply=[&](auto owned,const auto& plan,auto destroy,size_t& kept){
        ++commits;const auto result=commit(owned,plan,selected,destroy);if(!result)return false;kept=*result;return true;
    };
    assert(filter_reply(reply,query,group,task,table_address,selected,read,apply,drop)==1);
    assert(commits==1&&roots[0]==nullptr&&roots[1]==shown);
    auto expected=original;expected.words[query.length/8]=2;assert(reply.words==expected.words);
    delete[] shown;
}

static void test_access(const GroupContract& group,const TaskContract& task) {
    using home_layout::cards::TaskPackage;
    Bytes root(group.size),children(group.size*2),tasks(task.task_size*3);
    const std::array<std::string,3> names={"first.app","second.app","third.app"};
    const auto address=[](const auto& bytes){return reinterpret_cast<uintptr_t>(bytes.data());};
    const auto put=[&](Bytes& bytes,size_t at,uint64_t value){assert(at+8<=bytes.size());std::memcpy(bytes.data()+at,&value,8);};
    const auto type=[&](Bytes& bytes,size_t at,int32_t value){assert(at+4<=bytes.size());std::memcpy(bytes.data()+at,&value,4);};
    const auto absent=[&](Bytes& bytes,size_t at) {
        put(bytes,at+group.tasks.capacity,group.tasks.none);put(bytes,at+group.children.capacity,group.children.none);
        type(bytes,at+group.type,0);
    };
    const auto vector=[&](Bytes& bytes,size_t at,const VectorFields& fields,uintptr_t pointer,size_t count) {
        put(bytes,at+fields.capacity,count);put(bytes,at+fields.pointer,pointer);put(bytes,at+fields.length,count);
    };
    for(size_t i=0;i<names.size();++i) {
        const size_t base=i*task.task_size;
        for(const auto field:task.activities)put(tasks,base+field.offset,field.none);
        put(tasks,base+task.base_intent.offset,task.base_intent.none);
        put(tasks,base+task.activities[3].offset,0);
        put(tasks,base+task.activities[3].offset+task.component_string.pointer,address(names[i]));
        put(tasks,base+task.activities[3].offset+task.component_string.length,names[i].size());
    }
    const auto read=[&](uintptr_t at,std::span<std::byte> output) {
        const std::array<std::span<const std::byte>,6> ranges={root,children,tasks,
            std::as_bytes(std::span(names[0])),std::as_bytes(std::span(names[1])),std::as_bytes(std::span(names[2]))};
        for(const auto range:ranges) {
            const auto start=address(range);
            if(at>=start&&at-start<=range.size()&&output.size()<=range.size()-(at-start)) {
                std::memcpy(output.data(),range.data()+at-start,output.size());return true;
            }
        }
        return false;
    };
    const auto collect=[&](std::vector<std::string>& result) {
        return visit_group(group,task,address(root),read,[&](const TaskPackage& package) {
            assert(package.real_activity);result.emplace_back(*package.real_activity);return true;
        });
    };
    absent(root,0);absent(children,0);absent(children,group.size);
    std::vector<std::string> result;
    assert(collect(result)&&result.empty());
    vector(root,0,group.tasks,address(tasks),2);
    assert(collect(result)&&result==std::vector<std::string>({names[0],names[1]}));result.clear();
    type(root,group.type,group.mixed_type);
    assert(!collect(result)&&result.empty());
    vector(root,0,group.children,address(children),2);
    vector(children,0,group.tasks,address(tasks)+task.task_size,1);
    vector(children,group.size,group.tasks,address(tasks)+task.task_size*2,1);
    assert(collect(result)&&result==std::vector<std::string>({names[1],names[2]}));result.clear();
    type(children,group.type,group.mixed_type); // MIXED children still contribute only their own mTasks.
    assert(collect(result)&&result.size()==2);result.clear();
    put(children,group.null_child.offset,group.null_child.none);
    assert(!collect(result)&&result.empty());
    absent(children,0);assert(collect(result)&&result==std::vector<std::string>({names[2]}));result.clear();
    vector(children,0,group.tasks,address(tasks),1);
    put(children,group.tasks.length,2);assert(!collect(result));
    vector(children,0,group.tasks,UINTPTR_MAX-4,1);assert(!collect(result));
    vector(children,0,group.tasks,address(tasks),1);
    put(root,group.children.length,home_layout::cards::kMaxMembers+1);assert(!collect(result));
    vector(root,0,group.children,address(children),2);
    unsigned reads=0;
    assert(visit_group(group,task,address(root),[&](uintptr_t at,std::span<std::byte> output){++reads;return read(at,output);},[](const auto&){return true;}));
    for(unsigned fault=1;fault<=reads;++fault) {
        unsigned count=0;
        assert(!visit_group(group,task,address(root),[&](uintptr_t at,std::span<std::byte> output){return ++count!=fault&&read(at,output);},[](const auto&){return true;}));
    }
    assert(!visit_group(group,task,address(root),read,[](const auto&){return false;}));
}

int main(int argc,char** argv) {
    assert(argc>=7);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream input(argv[i],std::ios::binary);assert(input);
        std::vector<char> chars{std::istreambuf_iterator<char>(input),{}};Bytes bytes(chars.size());
        std::memcpy(bytes.data(),chars.data(),chars.size());files.push_back(std::move(bytes));
    }
    const auto original=graph(files);std::string_view reason;
    const auto task=resolve_task(original,&reason);assert(task);
    const auto group=resolve_group(original,*task,&reason);
    if(!group){std::cerr<<"group contract declined at "<<reason<<'\n';return 1;}
    const auto query=resolve_query(original,*group,&reason);
    if(!query){std::cerr<<"query contract declined at "<<reason<<'\n';return 3;}
    test_reply(*group,*task,*query);
    const auto query_body=success_prefix(original,query->query);assert(query_body);
    const auto query_flow=trace(original,*query_body,true,true);
    const auto reject_query=[&](const Corpus& changed){auto current=graph(changed);assert(!resolve_query(current,*group));};
    {
        auto changed=files;const Store* length=nullptr;const Store* capacity=nullptr;
        for(const auto& store:query_flow.stores)if(store.address.kind==Value::Self) {
            if(store.address.offset==query->length)length=&store;
            if(store.address.offset==query->capacity)capacity=&store;
        }
        assert(length&&capacity);
        const auto shift=[&](const Store* store){return store!=query_flow.stores.data()&&(store-1)->index==store->index?10u:0u;};
        const auto length_shift=shift(length),capacity_shift=shift(capacity);
        const auto length_word=query_body->words[length->index],capacity_word=query_body->words[capacity->index];
        const auto length_reg=(length_word>>length_shift)&31,capacity_reg=(capacity_word>>capacity_shift)&31;
        write(changed,{query->query.image,query->query.va+length->index*4},(length_word&~(31u<<length_shift))|(capacity_reg<<length_shift));
        write(changed,{query->query.image,query->query.va+capacity->index*4},(capacity_word&~(31u<<capacity_shift))|(length_reg<<capacity_shift));
        auto current=graph(changed);const auto swapped=resolve_query(current,*group);
        assert(swapped&&swapped->length==query->capacity&&swapped->capacity==query->length);
        test_reply(*group,*task,*swapped);
    }
    for(unsigned fault=0;fault<5;++fault) {
        auto changed=files;bool patched=false;
        for(size_t i=0;i<query_body->words.size();++i) {
            const auto w=query_body->words[i];uint32_t replacement=w;
            if(fault==0&&(w&0xff800000u)==0x52800000u&&((w>>5)&65535)==group->size/8)replacement+=1u<<5;
            if(fault==1&&(w&0xffc0fc00u)==0xd340fc00u&&((w>>16)&63)==3)replacement^=1u<<16;
            if(fault==2&&(w&0xff80001fu)==0x52800000u&&((w>>5)&65535)==group->size)replacement+=8u<<5;
            if(fault==3&&(w&0x9f000000u)==0x10000000u)replacement=0xaa1f03e0u|(w&31);
            if(fault==4&&(w&0xff00001fu)==0xb4000000u)replacement^=1u<<24;
            if(replacement==w)continue;
            write(changed,{query->query.image,query->query.va+i*4},replacement);patched=true;break;
        }
        assert(patched);reject_query(changed);
    }
    {
        auto changed=files;bool patched=false;
        for(const auto& step:query_flow.steps)if(step.input.kind==Value::Result&&step.delta==8
            &&step.index+1<query_body->words.size()&&(query_body->words[step.index]&0x3b200c00u)==0x38000400u) {
            write(changed,{query->query.image,query->query.va+step.index*4},query_body->words[step.index]+(8u<<12));patched=true;break;
        }
        assert(patched);reject_query(changed);
    }
    test_access(*group,*task);
    const auto reject=[&](const Corpus& changed){auto current=graph(changed);const auto t=resolve_task(current);assert(t&&!resolve_group(current,*t));};
    const auto body=original.body(group->in_place_drop);assert(body);const auto flow=trace(original,*body);
    std::optional<Address> child_loop,child_free;
    for(size_t i=1;i<flow.calls.size();++i) {
        const auto& call=flow.calls[i-1];
        if(call.target&&flow.calls[i].target&&call.receiver==flow.calls[i].receiver&&element_loop(original,*call.target,group->size,true)) {
            assert(!child_loop);child_loop=call.target;child_free=flow.calls[i].target;
        }
    }
    assert(child_loop&&child_free);
    {
        auto changed=files;const auto at=original.exported("GroupedTaskInfo_get_type");assert(at);
        const auto getter=original.body(*at);assert(getter);const auto getter_flow=trace(original,*getter);
        unsigned moved=0;
        for(const auto& load:getter_flow.loads)if(load.address.kind==Value::Self&&load.address.offset==group->type) {
            write(changed,{at->image,at->va+load.index*4},shift_memory(getter->words[load.index],4));++moved;
        }
        assert(moved==1);reject(changed);
        for(auto name:{"GroupedTaskInfo_default","GroupedTaskInfo_for_multiple_split_tasks"}) {
            const auto constructor=original.exported(name);assert(constructor);
            const auto b=success_prefix(original,*constructor);assert(b);const auto f=trace(original,*b);unsigned stores=0;
            for(const auto& store:f.stores)if(store.address.kind==Value::Returned&&store.address.number==f.regs[0].number
                &&store.address.offset==group->type&&store.width==4) {
                write(changed,{b->address.image,b->address.va+store.index*4},shift_memory(b->words[store.index],4));++stores;
            }
            assert(stores==1);
        }
        auto current=graph(changed);auto moved_group=resolve_group(current,*task,&reason);
        if(!moved_group){std::cerr<<"consistent type move declined at "<<reason<<'\n';return 2;}
        assert(moved_group->type==group->type+4);test_access(*moved_group,*task);
    }
    {
        auto changed=files;const auto loop=original.body(*child_loop);assert(loop);const auto f=trace(original,*loop);unsigned moved=0;
        for(const auto& step:f.steps)if(!step.flags&&step.delta==group->size&&step.input.kind==Value::Field) {
            write(changed,{loop->address.image,loop->address.va+step.index*4},loop->words[step.index]+(8u<<10));++moved;
        }
        assert(moved==1);reject(changed);
    }
    {
        auto changed=files;const auto loop=original.body(*child_loop);assert(loop);const auto f=trace(original,*loop);unsigned moved=0;
        for(const auto& compare:f.branch_comparisons)if(compare.field.kind==Value::IndirectField) {
            write(changed,{loop->address.image,loop->address.va+compare.index*4},loop->words[compare.index]^1u);++moved;
        }
        assert(moved==1);reject(changed);
    }
    {
        auto changed=files;const auto free=original.leaf(*child_free);assert(free);unsigned moved=0;
        for(size_t i=0;i<free->words.size();++i) {
            const auto w=free->words[i];
            if((w&0xff80001fu)==0x52800002u&&((w>>5)&65535)==group->size) {
                write(changed,{free->address.image,free->address.va+i*4},w+(8u<<5));++moved;
            }
        }
        assert(moved==1);reject(changed);
    }
    {
        auto changed=files;const auto at=original.exported("GroupedTaskInfo_default");assert(at);
        const auto constructor=success_prefix(original,*at);assert(constructor);const auto f=trace(original,*constructor);unsigned removed=0;
        for(const auto& store:f.stores)if(store.address.kind==Value::Returned&&store.address.offset==group->children.capacity) {
            write(changed,{at->image,at->va+store.index*4},0xd503201fu);++removed;
        }
        assert(removed==1);reject(changed);
    }
    {
        auto missing=files;missing.erase(missing.begin()+5);reject(missing);
        auto duplicate=files;duplicate.push_back(files[0]);auto current=graph(duplicate);assert(!resolve_group(current,*task));
    }
    std::cout<<"PASS group/query SDK contracts, type/returned-word relocation, child and ownership-loop refusal, ordinary/split/one-level mixed access and fault injection\n";
}
