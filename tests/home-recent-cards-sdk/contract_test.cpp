/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "fixture.h"
#include <cassert>
#include <fstream>
#include <iostream>
#include <random>
using namespace home_layout::cards::sdk;
static void move_writer(Corpus& files,const Graph& original,const TaskContract& contract,int delta) {
    auto body=original.body(contract.writer);assert(body);
    auto flow=trace(original,*body);unsigned changed=0;
    for(const auto& call:flow.calls)if(call.receiver.kind==Value::Nullable&&call.receiver.offset==contract.activities[3].offset) {
        assert(call.index>=6);
        for(size_t i=call.index-6;i<call.index;++i) {
            auto w=body->words[i];
            if((w&0xffc00000u)==0xf9400000u&&((w>>10)&4095)*8==contract.activities[3].offset)
                {w+=uint32_t(delta/8)<<10;++changed;}
            else if((w&0xffc00000u)==0x91000000u&&((w>>10)&4095)==contract.activities[3].offset)
                {w+=uint32_t(delta)<<10;++changed;}
            else continue;
            write(files,{body->address.image,body->address.va+i*4},w);
        }
    }
    assert(changed==2);
}
static void move_reader(Corpus& files,const Graph& original,const TaskContract& contract,int delta,bool header_only) {
    auto body=original.body(contract.reader);assert(body);const auto flow=trace(original,*body);
    const auto klass=original.exported("ComponentName_get_class_name");assert(klass);
    const auto getter=original.body(*klass);assert(getter);
    const auto fields=string_fields(original,*getter);assert(fields);
    const auto size=std::max(fields->pointer,fields->length)+8;
    unsigned changed=0;
    for(const auto& store:flow.stores)if(store.address.kind==Value::Self&&store.address.offset>=contract.activities[3].offset
        &&store.address.offset<contract.activities[3].offset+size&&(!header_only||store.address.offset==contract.activities[3].offset)) {
        write(files,{body->address.image,body->address.va+store.index*4},shift_memory(body->words[store.index],delta));++changed;
    }
    for(const auto& call:flow.calls)if(call.receiver.kind==Value::Self&&call.receiver.offset==contract.activities[3].offset) {
        unsigned addresses=0;
        for(size_t i=call.index>4?call.index-4:0;i<call.index;++i) {
            const auto word=body->words[i];
            if((word&0xffc0001fu)==0x91000000u&&((word>>10)&4095)==contract.activities[3].offset) {
                write(files,{body->address.image,body->address.va+i*4},word+(uint32_t(delta)<<10));++addresses;
            }
        }
        assert(addresses==1);
    }
    assert(header_only?changed==1:changed>=3);
}
static void test_access(const TaskContract& contract) {
    using home_layout::cards::TaskPackage;
    Bytes task(contract.task_size);
    const uintptr_t base=reinterpret_cast<uintptr_t>(task.data());
    const std::string real_name="real.app",component_name="intent.app",package_name="package.app";
    const auto put=[&](uint32_t at,uint64_t word){assert(at+8<=task.size());std::memcpy(task.data()+at,&word,8);};
    const auto name=[&](uint32_t at,StringFields fields,const std::string& value) {
        put(at,0);put(at+fields.pointer,reinterpret_cast<uintptr_t>(value.data()));put(at+fields.length,value.size());
    };
    const auto reset=[&]{
        std::fill(task.begin(),task.end(),std::byte{});
        for(auto field:contract.activities)put(field.offset,field.none);
        put(contract.base_intent.offset,contract.base_intent.none);
        put(contract.base_intent.offset+contract.intent_component.offset,contract.intent_component.none);
        put(contract.base_intent.offset+contract.intent_package.offset,contract.intent_package.none);
    };
    const auto read=[&](uintptr_t address,std::span<std::byte> output) {
        const std::array<std::span<const std::byte>,4> ranges={task,std::as_bytes(std::span(real_name)),
            std::as_bytes(std::span(component_name)),std::as_bytes(std::span(package_name))};
        for(auto range:ranges) {
            const uintptr_t start=reinterpret_cast<uintptr_t>(range.data());
            if(address>=start&&address-start<=range.size()&&output.size()<=range.size()-(address-start)) {
                std::memcpy(output.data(),range.data()+(address-start),output.size());return true;
            }
        }
        return false;
    };
    const auto expect=[&](unsigned origin,std::string_view expected) {
        bool called=false;
        assert(visit_task(contract,base,read,[&](const TaskPackage& package) {
            called=true;
            const std::array names={package.real_activity,package.intent_component,package.intent_package};
            for(size_t i=0;i<names.size();++i)assert(i==origin?names[i]&&*names[i]==expected:!names[i]);
            return true;
        }));assert(called);
    };
    reset();expect(3,{});
    name(contract.activities[3].offset,contract.component_string,real_name);expect(0,real_name);
    put(contract.base_intent.offset,0);
    name(contract.base_intent.offset+contract.intent_component.offset,contract.component_string,component_name);
    name(contract.base_intent.offset+contract.intent_package.offset,contract.package_string,package_name);
    expect(0,real_name);
    put(contract.activities[3].offset,contract.activities[3].none);expect(1,component_name);
    put(contract.base_intent.offset+contract.intent_component.offset,contract.intent_component.none);expect(2,package_name);
    put(contract.base_intent.offset+contract.intent_package.offset,contract.intent_package.none);expect(3,{});
    name(contract.activities[3].offset,contract.component_string,real_name);
    put(contract.activities[3].offset+contract.component_string.length,0);expect(0,{});
    put(contract.activities[3].offset+contract.component_string.length,256);
    put(contract.activities[3].offset+contract.component_string.pointer,UINTPTR_MAX);expect(0,{});
    put(contract.activities[3].offset+contract.component_string.length,20);
    assert(!visit_task(contract,base,read,[](const auto&){assert(false);return true;}));
    assert(!visit_task(contract,UINTPTR_MAX-4,read,[](const auto&){assert(false);return true;}));
    reset();name(contract.activities[3].offset,contract.component_string,real_name);
    for(unsigned fault=1;fault<=4;++fault) {
        unsigned calls=0;
        assert(!visit_task(contract,base,[&](uintptr_t at,std::span<std::byte> output){return ++calls!=fault&&read(at,output);},
            [](const auto&){assert(false);return true;}));
    }
    assert(!visit_task(contract,base,read,[](const auto&){return false;}));
}
int main(int argc,char** argv) {
    assert(argc>=6);Corpus files;
    for(int i=1;i<argc;++i) {
        std::ifstream input(argv[i],std::ios::binary);assert(input);
        std::vector<char> data{std::istreambuf_iterator<char>(input),{}};Bytes bytes(data.size());
        std::memcpy(bytes.data(),data.data(),data.size());files.push_back(std::move(bytes));
    }
    {
        Graph empty({});
        // Same ABI semantics with different scratch registers and a new field.
        Body getter{{0,0},{0xf941400cu,0xd2f0000eu,0x910a000du,0xeb0e019fu,0x9a8d03e0u,0xd65f03c0u}};
        auto field=optional_field(empty,getter);assert(field&&field->offset==640&&field->none==0x8000000000000000ULL);
        auto narrow=getter;narrow.words[3]&=0x7fffffffu;assert(!optional_field(empty,narrow));
        auto reserved=getter;reserved.words[1]&=0x7fffffffu;assert(!optional_field(empty,reserved));
        Body shifted{{0,0},{0xf941400cu,0xd2f0000eu,0x5280280fu,0x8b0f040du,0xeb0e019fu,0x9a8d03e0u,0xd65f03c0u}};
        assert(optional_field(empty,shifted)->offset==640);
        shifted.words[3]|=1u<<22;assert(!optional_field(empty,shifted));
        getter.words[2]+=8u<<10;assert(!optional_field(empty,getter));
        std::minstd_rand random(1731);
        for(unsigned n=0;n<5000;++n) {
            Body code{{0,0},{}};
            for(unsigned i=0;i<16;++i)code.words.push_back((uint32_t(random())<<1)^uint32_t(random()));
            code.words.push_back(0xd65f03c0u);(void)trace(empty,code);(void)forward_paths(code);
        }
    }
    auto original=graph(files);std::string_view reason;
    const auto baseline=resolve_task(original,&reason);
    if(!baseline){std::cerr<<"SDK contract declined at "<<reason<<'\n';return 1;}
    assert(baseline->task_size>baseline->activities[3].offset);
    test_access(*baseline);
    const auto reject=[&](const Corpus& changed){auto current=graph(changed);assert(!resolve_task(current));};
    {
        auto temporary=files;auto owned=graph(temporary);temporary.clear();temporary.shrink_to_fit();
        assert(resolve_task(owned)->activities==baseline->activities); // No caller-owned file spans survive.
    }
    {
        auto changed=files;for(auto& bytes:changed) { // No ELF section metadata is needed.
            for(size_t i=40;i<48;++i)bytes[i]=std::byte{};
            for(size_t i=58;i<64;++i)bytes[i]=std::byte{};
        }
        auto stripped=graph(changed);assert(resolve_task(stripped)->activities==baseline->activities);
    }
    {auto changed=files;move_writer(changed,original,*baseline,16);reject(changed);}
    {auto changed=files;move_writer(changed,original,*baseline,16);move_reader(changed,original,*baseline,16,true);reject(changed);}
    {
        auto changed=files;move_writer(changed,original,*baseline,16);move_reader(changed,original,*baseline,16,false);
        auto moved=graph(changed);auto decoded=resolve_task(moved,&reason);
        if(!decoded){std::cerr<<"consistent field move declined at "<<reason<<'\n';return 2;}
        assert(decoded->activities[3].offset==baseline->activities[3].offset+16);
        assert(decoded->activities[0]==baseline->activities[0]);
        test_access(*decoded);
    }
    {
        auto changed=files;const auto addr=original.exported("ComponentName_get_package_name");assert(addr);
        auto body=original.body(*addr);assert(body);auto flow=trace(original,*body);bool patched=false;
        for(auto load:flow.loads)if(load.address.kind==Value::Self) {
            write(changed,{addr->image,addr->va+load.index*4},body->words[load.index]&0x3fffffffu);patched=true;break;
        }
        assert(patched);reject(changed); // A W-register pair is not an RStr of two machine words.
    }
    {
        auto changed=files;auto addr=original.exported("Intent_get_package");assert(addr);
        auto body=original.body(*addr);assert(body);bool patched=false;
        for(size_t i=0;i<body->words.size();++i)if((body->words[i]&0xff00001fu)==0x54000001u) {
            write(changed,{addr->image,addr->va+i*4},body->words[i]^1);patched=true;break;
        }
        assert(patched);reject(changed);
    }
    {
        auto changed=files;auto addr=original.exported("RunningTaskInfo_write_to_parcel");assert(addr);
        auto body=original.body(*addr);assert(body);bool patched=false;
        for(size_t i=0;i<body->words.size();++i)if((body->words[i]&0xfc000000u)==0x94000000u) {
            write(changed,{addr->image,addr->va+i*4},0xd503201fu);patched=true;break;
        }
        assert(patched);reject(changed);
    }
    {
        auto changed=files;auto running=single_call(original,"RunningTaskInfo_write_to_parcel");assert(running);
        auto body=original.body(*running);assert(body);bool patched=false;
        for(size_t i=0;i<body->words.size();++i) {
            const auto word=body->words[i];
            if((word&0xff800000u)==0x52800000u&&((word>>5)&65535)==baseline->task_size) {
                write(changed,{running->image,running->va+i*4},word+(16u<<5));patched=true;break;
            }
        }
        assert(patched);reject(changed);
    }
    {
        auto duplicate=files;duplicate.push_back(files[0]);reject(duplicate); // An API owner must be unique.
    }
    {
        auto changed=files;auto running=single_call(original,"RunningTaskInfo_read_from_parcel");assert(running);
        auto body=original.body(*running);assert(body);const auto flow=trace(original,*body);bool patched=false;
        for(const auto& call:flow.calls)if(call.linkage=="memcpy"&&call.third.number==baseline->task_size) {
            assert(running->image==baseline->writer.image);
            const int64_t delta=int64_t(baseline->writer.va)-int64_t(running->va+call.index*4);
            assert(delta%4==0&&delta/4>=-(1<<25)&&delta/4<(1<<25));
            write(changed,{running->image,running->va+call.index*4},0x94000000u|(uint32_t(delta/4)&0x3ffffff));
            patched=true;break;
        }
        assert(patched);reject(changed); // Three arguments alone do not prove a copy operation.
    }
    {
        auto missing=files;missing.erase(missing.begin()+4);reject(missing); // Inlined String-family proof needs its actual linkage.
    }
    {
        Body cycle{{0,0},{0x14000000u}};assert(!forward_paths(cycle));
        Body outside{{0,0},{0x17ffffffu,0xd65f03c0u}};assert(!forward_paths(outside));
    }
    std::cout<<"PASS SDK task contract, ownership/section independence, producer-consumer field relocation, header-only refusal, widths, null branch, missing/ambiguous linkage, size mismatch and CFG refusal\n";
}
