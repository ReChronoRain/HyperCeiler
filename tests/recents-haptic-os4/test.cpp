/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recents_haptic_render.h"
#include "targets/home/home_recents_haptic_protocol.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
#include <limits>
using namespace home_layout::haptic;
static auto identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return haptic::resolve(*c);}
static Function& fn(Functions& f,uint32_t va){auto it=std::find_if(f.begin(),f.end(),[&](const auto& x){return x.va==va;});assert(it!=f.end());return *it;}
int main(){
    const auto protocol=[](std::vector<int32_t> words){Reply reply;size_t at=0;
        return read_reply([&](int32_t& v){if(at==words.size())return false;v=words[at++];return true;},reply);};
    const std::vector<int32_t> valid={kAck,1,1,3,100,1};assert(protocol(valid));
    assert(protocol({kAck,0,0,0,50,0}));assert(protocol({kAck,1,0,0,50,1}));
    for(size_t n=0;n<6;++n)assert(!protocol(std::vector<int32_t>(valid.begin(),valid.begin()+n)));
    for(size_t i=0;i<6;++i){auto invalid=valid;invalid[i]=-1;assert(!protocol(invalid));}
    assert(!protocol({kAck,0,1,0,50,0}));assert(!protocol({kAck,1,1,4,50,1}));
    const auto v2=[](std::vector<int32_t> words){Reply reply;size_t at=0;
        return read_reply([&](int32_t& v){if(at==words.size())return false;v=words[at++];return true;},reply,true)&&at==words.size();};
    for(int scroll:{0,1})for(int close:{0,1})for(int type=0;type<4;++type)for(int strength:{10,50,100}){
        const std::vector<int32_t> wire={kAckV2,1,scroll,close,type,strength,1};
        assert(v2(wire)&&!protocol(wire));Reply decoded;size_t at=0;
        assert(read_reply([&](int32_t& word){if(at==wire.size())return false;word=wire[at++];return true;},decoded,true));
        assert(decoded.available&&decoded.enabled==bool(scroll)&&decoded.close==bool(close)&&decoded.type==type&&decoded.strength==strength);
        for(size_t n=0;n<wire.size();++n)assert(!v2(std::vector<int32_t>(wire.begin(),wire.begin()+n)));
        for(size_t i=0;i<wire.size();++i){auto wrong=wire;wrong[i]=-1;assert(!v2(wrong));}
        auto trailing=wire;trailing.push_back(0);assert(!v2(trailing));
    }
    assert(!v2(valid));assert(v2({kAckV2,0,0,0,0,50,0}));
    assert(!v2({kAckV2,0,0,1,0,50,0}));assert(!v2({kAckV2,0,1,0,0,50,0}));
    assert(!v2({kAckV2,1,0,2,0,50,0}));assert(!v2({kAckV2,1,1,1,4,50,0}));
    auto fs=fixture();auto p=identify(fs);assert(p);
    std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fs,addresses);auto r=identify(moved);assert(r);
    for(unsigned i=0;i<7;++i)assert(r->sites[i]==(i==5?addresses.at(p->sites[5]-31*4)+31*4:addresses.at(p->sites[i])));
    for(const auto& f:fs){auto broken=fs;std::erase_if(broken,[&](const auto& v){return v.va==f.va;});assert(!identify(broken));}
    auto ambiguous=fs;ambiguous.insert(ambiguous.end(),moved.begin(),moved.end());assert(!identify(ambiguous));
    unsigned mutations=0;
    for(const auto& f:fs){
        // The large viewport owner proves its two call edges, not an unrelated
        // full-body template. Every instruction of each admitted small body is essential.
        if(f.words.size()>512)continue;
        for(size_t i=0;i<f.words.size();++i){auto broken=fs;fn(broken,f.va).words[i]=0xd503201fu;assert(!identify(broken));++mutations;}
    }
    auto classes=fs;
    for(auto& f:classes)for(auto& w:f.words)if(home_dynamic::movz(w,17)){
        auto cid=home_dynamic::zimm(w);
        if(cid==p->start_cid||cid==p->update_cid||cid==p->end_cid)w+=7u<<5;
    }
    auto& factory=fn(classes,p->start_factory).words;uint32_t tag=0;assert(home_dynamic::tag(factory,tag));tag+=7u<<12;
    factory[0]=(factory[0]&~(65535u<<5))|((tag&65535u)<<5);
    factory[1]=(factory[1]&~(65535u<<5))|((tag>>16)<<5);
    auto c=identify(classes);assert(c&&c->start_cid==p->start_cid+7&&c->update_cid==p->update_cid+7);
    auto different_cid=fs;fn(different_cid,p->sites[3]).words[6]+=1u<<5;assert(!identify(different_cid));
    auto wrong_extent=fs;fn(wrong_extent,p->inverse).words[1]+=4u<<12;assert(!identify(wrong_extent));
    auto shifted=fs;
    for(size_t at:{size_t(32),size_t(43),size_t(91)})fn(shifted,p->sites[4]).words[at]+=4u<<12;
    auto shifted_plan=identify(shifted);assert(shifted_plan&&shifted_plan->spacing==p->spacing+4);
    fn(shifted,p->sites[4]).words[32]+=4u<<12;assert(!identify(shifted));
    auto wrong_frame=fs;fn(wrong_frame,p->sites[0]).words[3]=0xf81003a1u;assert(!identify(wrong_frame));

    // All compressed references and relative tagged byte offsets below come
    // from the production plan. Inputs are borrowed, and every register stays intact.
    std::array<std::byte,65536> heap{};const uintptr_t base=uint64_t(1)<<32;
    const uintptr_t notification=base+0x101,metrics=base+0x201,pixels=base+0x301,layout=base+0x401,viewport=base+0x801,pool=base+0x1000;
    const auto put=[&](uintptr_t at,auto value){assert(at>=base&&at-base+sizeof(value)<=heap.size());std::memcpy(heap.data()+at-base,&value,sizeof(value));};
    const auto read=[&](uintptr_t at,void* out,size_t count){if(at<base||at-base>heap.size()||count>heap.size()-(at-base))return false;std::memcpy(out,heap.data()+at-base,count);return true;};
    recent_render::Registers s{};s.x[2]=notification;s.x[1]=viewport;s.x[3]=layout;s.x[22]=base+0x901;s.x[27]=pool;s.x[28]=1;
    put(notification-1,uint64_t(p->start_cid)<<12);put(notification+p->depth,int64_t(0));
    put(notification+p->metrics,uint32_t(metrics));put(metrics+p->pixels,uint32_t(pixels));
    put(notification+p->drag_details,uint32_t(layout));
    put(pixels-1,uint64_t(p->double_cid)<<12);put(pixels+p->payload,123.5);
    auto before=s;
    for(unsigned lane=0;lane<4;++lane){auto o=observe(s,*p,lane,read);assert(o.valid&&!o.extent&&o.lane==lane&&o.event==Event::Start&&o.value==123.5);}
    put(notification+p->drag_details,uint32_t(s.x[22]));
    const auto programmatic=observe(s,*p,0,read);assert(programmatic.event==Event::End&&programmatic.programmatic);
    put(notification+p->drag_details,uint32_t(layout));
    put(notification-1,uint64_t(p->update_cid)<<12);assert(observe(s,*p,0,read).event==Event::Update);
    put(notification+p->depth,int64_t(1));assert(!observe(s,*p,0,read).valid);put(notification+p->depth,int64_t(0));
    put(pixels-1,uint64_t(p->double_cid+1)<<12);assert(!observe(s,*p,0,read).valid);
    put(notification-1,uint64_t(p->end_cid)<<12);
    const auto ended=observe(s,*p,0,read);assert(ended.event==Event::End&&!ended.programmatic);
    assert(std::memcmp(&s,&before,sizeof(s))==0);
    put(pool+p->horizontal_pool,uint64_t(0x1231));put(layout+p->axis,uint32_t(0x1231));put(layout+p->spacing,20.0);
    s.d(0,180);s.d(1,90);s.d(2,15);put(viewport+p->stack_extent,75.0);
    assert(observe(s,*p,4,read).value==110);put(layout+p->axis,uint32_t(0x2231));assert(observe(s,*p,4,read).value==200);
    assert(observe(s,*p,5,read).value==105);assert(observe(s,*p,6,read).value==75);assert(!observe(s,*p,7,read).valid);
    put(notification-1,uint64_t(p->update_cid)<<12);put(notification+p->metrics,uint32_t(s.x[22]));assert(!observe(s,*p,0,read).valid);

    Crossing state;
    assert(!state.accept(Event::Update,500,100,1));
    assert(!state.accept(Event::Start,0,100,1));
    assert(!state.accept(Event::Update,59,100,1));assert(state.accept(Event::Update,61,100,1)==1);
    for(double x:{59.,55.,49.,45.,41.})assert(!state.accept(Event::Update,x,100,1));
    assert(state.accept(Event::Update,39,100,1)==1);assert(state.accept(Event::Update,240,100,1)==2);
    assert(!state.accept(Event::Update,100000,100,1));assert(!state.accept(Event::Update,0,100,1));
    assert(!state.accept(Event::End,0,100,1));assert(!state.accept(Event::Update,200,100,1));
    assert(!state.accept(Event::Start,0,0,1));assert(!state.accept(Event::Update,0,100,1));assert(state.accept(Event::Update,70,100,1)==1);
    assert(!state.accept(Event::Update,170,100,2));assert(!state.accept(Event::Update,180,80,2));
    assert(!state.accept(Event::Update,std::numeric_limits<double>::quiet_NaN(),80,2));assert(!state.accept(Event::Update,500,80,2));
    for(int i=0;i<1000;++i){state.accept(Event::Start,0,100,3);assert(state.accept(Event::Update,100,100,3)==1);
        for(int frame=0;frame<120;++frame)assert(!state.accept(Event::Update,100,100,3));state.accept(Event::End,100,100,3);}
    std::cout<<"recents haptic PASS: "<<mutations<<" corruptions, independent relocation/classes/spacing, missing/ambiguous graphs, borrowed metrics/extent/register checks, crossing/reverse/fling/jitter/config/cancel and 120000 stationary frames\n";
}
