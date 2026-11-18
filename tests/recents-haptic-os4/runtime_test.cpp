/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Only Android's process/Binder transport is substituted. The shipping refresh,
// observation/crossing, nonblocking pipe, worker, serialization and counters run.
#include "targets/home/home_recents_haptic_runtime.h"
#include "targets/home/home_recents_haptic_protocol.h"
#include "targets/home/home_layout_config.h"
#include "targets/home/home_local_config_channel.h"
#include <android/transport.h>
#include <cassert>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <iostream>
#include <mutex>
#include <thread>
#include <unistd.h>
using namespace home_layout::haptic;
namespace {
AIBinder binder;AIBinder_Class binder_class;
std::mutex capture_mutex;std::condition_variable capture_changed;
struct Packet {int operation;int64_t time;int count;bool extended;};
std::vector<Packet> packets;
bool scroll=true,closing=true,v1=false,local=false,available=true,performed=true;
uint64_t observer_revision=0;
template<class T> void put(AParcel* p,T value){auto offset=p->bytes.size();p->bytes.resize(offset+sizeof(value));std::memcpy(p->bytes.data()+offset,&value,sizeof(value));}
template<class T> T get(const AParcel* p){auto* writable=const_cast<AParcel*>(p);assert(writable->position+sizeof(T)<=p->bytes.size());T value;std::memcpy(&value,p->bytes.data()+writable->position,sizeof(T));writable->position+=sizeof(T);return value;}
void wait_worker(){for(unsigned i=0;i<2000;++i){if(scroll?__atomic_load_n(&hc_recents_haptic_gate,__ATOMIC_ACQUIRE):hc_recents_close_haptic_active())return;std::this_thread::sleep_for(std::chrono::milliseconds(1));}assert(false);}
}
namespace home_layout {
uint64_t config_observer_revision(){return observer_revision;}
bool local_haptic_profile(haptic::Profile& value){
    if(!local)return false;
    value={available,scroll,closing,3,70};return true;
}
}
namespace home_local_config {
bool requested(){return false;}
bool perform_haptic(uint32_t profile,int operation,int64_t time,int count){
    assert(local&&profile==((scroll?1u:0u)|(closing?8u:0u)|(3u<<1)|(70u<<4)));
    {std::lock_guard lock(capture_mutex);packets.push_back({operation,time,count,true});}capture_changed.notify_all();
    return performed;
}
}
extern "C" {
int __android_log_print(int,const char*,const char*,...){return 0;}
AIBinder* AServiceManager_getService(const char* name){assert(!local&&!std::strcmp(name,"window"));return &binder;}
const AIBinder_Class* AIBinder_getClass(AIBinder*){return &binder_class;}
const char* AIBinder_Class_getDescriptor(const AIBinder_Class*){return "android.view.IWindowManager";}
AIBinder_Class* AIBinder_Class_define(const char*,void*(*)(void*),void(*)(void*),binder_status_t(*)(AIBinder*,transaction_code_t,const AParcel*,AParcel*)){return &binder_class;}
bool AIBinder_associateClass(AIBinder*,const AIBinder_Class*){return true;}
void AIBinder_decStrong(AIBinder*){}
binder_status_t AIBinder_prepareTransaction(AIBinder*,AParcel** p){*p=new AParcel;return STATUS_OK;}
binder_status_t AParcel_writeInt32(AParcel* p,int32_t v){put(p,v);return STATUS_OK;}
binder_status_t AParcel_writeInt64(AParcel* p,int64_t v){put(p,v);return STATUS_OK;}
binder_status_t AParcel_readInt32(const AParcel* p,int32_t* out){if(p->position+4>p->bytes.size())return STATUS_BAD_VALUE;*out=get<int32_t>(p);return STATUS_OK;}
int32_t AParcel_getDataPosition(const AParcel* p){return int32_t(p->position);}
int32_t AParcel_getDataSize(const AParcel* p){return int32_t(p->bytes.size());}
void AParcel_delete(AParcel* p){delete p;}
binder_status_t AIBinder_transact(AIBinder*,transaction_code_t code,AParcel** in,AParcel** out,uint32_t flags){
    assert(!flags&&(code==kTransaction||code==kTransactionV2));
    Packet packet{get<int32_t>(*in),get<int64_t>(*in),get<int32_t>(*in),code==kTransactionV2};
    assert((*in)->position==(*in)->bytes.size());delete *in;*in=nullptr;
    if(v1&&packet.extended)return STATUS_UNKNOWN_TRANSACTION;
    {std::lock_guard lock(capture_mutex);packets.push_back(packet);}capture_changed.notify_all();
    const bool valid=packet.operation==0?packet.count==0:(packet.operation==1||packet.operation==2)&&packet.count>=1&&packet.count<=8;
    *out=new AParcel;put(*out,packet.extended?kAckV2:kAck);put(*out,int(valid));put(*out,int(valid&&scroll));
    if(packet.extended)put(*out,int(valid&&closing));
    put(*out,valid?3:0);put(*out,valid?70:50);put(*out,int(valid));return STATUS_OK;
}
}
int main(int argc,char** argv){
    assert(argc==2);std::string scenario=argv[1];local=scenario.starts_with("local");available=scenario!="local-unavailable";performed=scenario!="local-failure";v1=scenario=="v1";scroll=scenario!="close"&&scenario!="off";closing=!v1&&scenario!="scroll"&&scenario!="off"&&scenario!="local-scroll";
    if(scenario=="observer") {
        observer_revision=1;
        hc_recents_haptic_refresh();
        assert(packets.size()==1&&hc_recents_haptic_scroll_enabled()&&hc_recents_close_haptic_enabled());
        std::this_thread::sleep_for(std::chrono::milliseconds(1100));
        hc_recents_haptic_refresh();
        assert(packets.size()==1);
        observer_revision=2;scroll=closing=false;
        hc_recents_haptic_refresh();
        assert(packets.size()==2&&!hc_recents_haptic_scroll_enabled()&&!hc_recents_close_haptic_enabled());
        scroll=true;
        hc_recents_haptic_refresh(true);
        assert(packets.size()==3&&hc_recents_haptic_scroll_enabled());
        observer_revision=0;
        hc_recents_haptic_refresh();
        assert(packets.size()==4);
        std::cout<<"native haptic observer PASS initial sync, idle, changed, resume and fallback\n";
        return 0;
    }
    if(!available)scroll=closing=false;
    if(scenario=="local-close")scroll=false;
    hc_recents_haptic_refresh();hc_recents_haptic_ready(true);hc_recents_close_haptic_ready(true);
    assert(hc_recents_haptic_scroll_enabled()==scroll&&hc_recents_close_haptic_enabled()==closing);
    if(scroll||closing)wait_worker();
    hc_recents_haptic_observe({true,true,0,Event::Update,100});
    hc_recents_haptic_observe({true,false,0,Event::Start,0});
    hc_recents_haptic_observe({true,false,0,Event::Update,70});
    for(int i=0;i<120;++i)hc_recents_haptic_observe({true,false,0,Event::Update,70});
    hc_recents_close_haptic_commit();
    const size_t expected=size_t(!local)+size_t(scroll)+size_t(closing);
    {std::unique_lock lock(capture_mutex);assert(capture_changed.wait_for(lock,std::chrono::seconds(2),[&]{return packets.size()>=expected;}));
        assert(packets.size()==expected);
        if(!local)assert(packets[0].operation==0&&packets[0].count==0);
        if(scroll)assert(packets[size_t(!local)].operation==1&&packets[size_t(!local)].count==1);
        if(closing)assert(packets.back().operation==2&&packets.back().count==1);
        for(const auto& p:packets)assert(p.extended==!v1);
    }
    for(unsigned i=0;i<2000;++i){const auto c=hc_recents_haptic_counters();if(c.delivered+c.dropped==unsigned(scroll)&&c.close_delivered+c.close_dropped==unsigned(closing))break;std::this_thread::sleep_for(std::chrono::milliseconds(1));}
    const auto c=hc_recents_haptic_counters();assert(c.queued==unsigned(scroll)&&c.delivered==unsigned(scroll&&performed)&&c.dropped==unsigned(scroll&&!performed));
    assert(c.close_queued==unsigned(closing)&&c.close_delivered==unsigned(closing&&performed)&&c.close_dropped==unsigned(closing&&!performed));
    std::cout<<"native haptic worker PASS "<<scenario<<": real queue sends op1 scroll/op2 close, exact counts and delivery, 120 stationary frames, V1/V2 channel isolation\n";
}
