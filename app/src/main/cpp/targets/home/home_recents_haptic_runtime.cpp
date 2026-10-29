/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_recents_haptic_runtime.h"
#include "home_recents_haptic_protocol.h"
#include <android/log.h>
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <atomic>
#include <bit>
#include <cerrno>
#include <dlfcn.h>
#include <fcntl.h>
#include <mutex>
#include <pthread.h>
#include <time.h>
#include <unistd.h>

extern "C" uint32_t hc_recents_haptic_gate=0;
namespace {
std::atomic<uint64_t> config{0};
std::atomic_bool close_ready{false};
std::atomic_bool ready{false},changed{false},worker_alive{false};
std::array<std::atomic<uint64_t>,4> extents{};
std::atomic<uint64_t> samples{0},queued{0},delivered{0},dropped{0};
struct LaneCounters {
    std::atomic<uint64_t> starts{0},updates{0},ends{0},programmatic{0},extents{0},pixels{0};
};
std::array<LaneCounters,4> lanes{};
std::atomic<uint64_t> close_queued{0},close_delivered{0},close_dropped{0};
int input=-1,output=-1;
std::mutex transport;
AIBinder* window=nullptr;
struct Request {
    // Only feedback can enter this queue; operation 0 is reserved for profile queries.
    enum class Operation:uint32_t {Scroll=1,Close=2};
    uint64_t config=0;int64_t time=0;uint32_t count=0;
    Operation operation=Operation::Scroll;
};
static_assert(sizeof(Request)<=512);
int64_t now(){timespec t{};return clock_gettime(CLOCK_MONOTONIC,&t)==0?int64_t(t.tv_sec)*1000+t.tv_nsec/1000000:0;}
void publish(){__atomic_store_n(&hc_recents_haptic_gate,uint32_t(worker_alive.load(std::memory_order_acquire)
    &&ready.load(std::memory_order_acquire)&&(config.load(std::memory_order_acquire)&1)),__ATOMIC_RELEASE);}
void* on_create(void*){return nullptr;}
void on_destroy(void*){}
binder_status_t on_transact(AIBinder*,transaction_code_t,const AParcel*,AParcel*){return STATUS_UNKNOWN_TRANSACTION;}
binder_status_t exchange(int operation,int64_t timestamp,int count,home_layout::haptic::Reply& reply,bool extended=true){
    std::lock_guard guard(transport); // Only control/action workers, never a scroll callback.
    if(!window){
        using Get=AIBinder*(*)(const char*);
        auto get=reinterpret_cast<Get>(dlsym(RTLD_DEFAULT,"AServiceManager_getService"));
        if(!get||(window=get("window"))==nullptr)return STATUS_FAILED_TRANSACTION;
        const auto* cls=AIBinder_getClass(window);
        if(cls){const auto* name=AIBinder_Class_getDescriptor(cls);
            if(!name||std::strcmp(name,"android.view.IWindowManager")){AIBinder_decStrong(window);window=nullptr;return STATUS_FAILED_TRANSACTION;}
        }else{
            static auto* contract=AIBinder_Class_define("android.view.IWindowManager",on_create,on_destroy,on_transact);
            if(!contract||!AIBinder_associateClass(window,contract)){AIBinder_decStrong(window);window=nullptr;return STATUS_FAILED_TRANSACTION;}
        }
    }
    AParcel* in=nullptr;AParcel* out=nullptr;
    bool valid=AIBinder_prepareTransaction(window,&in)==STATUS_OK&&in
        &&AParcel_writeInt32(in,operation)==STATUS_OK&&AParcel_writeInt64(in,timestamp)==STATUS_OK
        &&AParcel_writeInt32(in,count)==STATUS_OK;
    binder_status_t status=STATUS_FAILED_TRANSACTION;
    if(valid){status=AIBinder_transact(window,(extended?home_layout::haptic::kTransactionV2:home_layout::haptic::kTransaction),&in,&out,0);in=nullptr;}
    if(status==STATUS_OK&&out){valid=home_layout::haptic::read_reply([&](int32_t& word){return AParcel_readInt32(out,&word)==STATUS_OK;},reply,extended)
        &&AParcel_getDataPosition(out)==AParcel_getDataSize(out);if(!valid)status=STATUS_BAD_VALUE;}
    if(in)AParcel_delete(in);if(out)AParcel_delete(out);
    if(status!=STATUS_OK){AIBinder_decStrong(window);window=nullptr;}
    return status;
}
void* worker(void*){
    worker_alive.store(true,std::memory_order_release);publish();unsigned reports=0;
    for(;;){Request r;const auto size=read(input,&r,sizeof(r));
        if(size<0&&errno==EINTR)continue;
        if(size!=sizeof(r))break;
        const auto age=now()-r.time;
        const bool closing=r.operation==Request::Operation::Close;
        auto& done=closing?close_delivered:delivered;auto& skipped=closing?close_dropped:dropped;
        if(r.config!=config.load(std::memory_order_acquire)||!(r.config&(closing?8:1))||age<0||age>160
            ||!(closing?close_ready:ready).load(std::memory_order_acquire)){skipped.fetch_add(r.count,std::memory_order_relaxed);continue;}
        home_layout::haptic::Reply result;
        const auto status=exchange(static_cast<int>(r.operation),r.time,r.count,result,(r.config&0x1000)!=0);
        const bool ok=status==STATUS_OK&&result.available&&(closing?result.close:result.enabled)&&result.performed;
        if(ok)done.fetch_add(r.count,std::memory_order_relaxed);else skipped.fetch_add(r.count,std::memory_order_relaxed);
        if(reports++<8)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeLayout",
            "recents card haptic event=%s delivered=%d cards=%u type=%d strength=%d status=%d; native-only WMS path, no launcher JVM required",
            closing?"close":"scroll",ok,r.count,result.type,result.strength,status);
    }
    worker_alive.store(false,std::memory_order_release);publish();return nullptr;
}
void configure(bool enabled,int type,int strength,bool close_enabled=false,bool extended=false){
    const uint64_t values=(enabled?1u:0u)|(unsigned(type)<<1)|(unsigned(strength)<<4)|(close_enabled?8u:0u)|(extended?0x1000u:0u);
    const auto old=config.load(std::memory_order_acquire);if((old&0xffff)==values)return;
    if((enabled||close_enabled)&&output<0){
        int fds[2];if(pipe2(fds,O_CLOEXEC)!=0)return;
        const int flags=fcntl(fds[1],F_GETFL);
        if(flags<0||fcntl(fds[1],F_SETFL,flags|O_NONBLOCK)<0){close(fds[0]);close(fds[1]);return;}
        input=fds[0];output=fds[1];pthread_t thread{};
        if(pthread_create(&thread,nullptr,worker,nullptr)!=0){close(input);close(output);input=output=-1;return;}
        pthread_detach(thread);
    }
    config.store((((old>>16)+1)<<16)|values,std::memory_order_release);
    changed.store(true,std::memory_order_release);publish();
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeLayout","recents card haptic config enabled=%d close=%d type=%d strength=%d source=physical-provider-via-WMS",enabled,close_enabled,type,strength);
}
}
void hc_recents_haptic_refresh(){
    // Owned by the existing layout worker/lease. No new polling thread and no
    // framework-layout protocol migration. Old servers disable only this feature.
    static int64_t next=0,v2_retry=0;static bool reported=false;
    const auto time=now();if(!time||time<next)return;
    home_layout::haptic::Reply profile;bool extended=time>=v2_retry;
    auto status=exchange(0,time,0,profile,extended);
    if(extended&&status==STATUS_UNKNOWN_TRANSACTION){
        v2_retry=time+30000;extended=false;status=exchange(0,time,0,profile,false);
        if(!reported)__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeLayout","close haptic endpoint not loaded; retaining V1 scroll feedback until system_server reload");
    }
    next=time+(status==STATUS_UNKNOWN_TRANSACTION?30000:status==STATUS_OK&&profile.available?1000:5000);
    if(status==STATUS_OK&&profile.available){configure(profile.enabled,profile.type,profile.strength,profile.close,extended);reported=!extended;}
    else {configure(false,0,50);if(!reported){reported=true;
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeLayout","recents card haptic config unavailable status=%d; new WMS endpoint requires system_server reload after installation",status);}}
}
bool hc_recents_haptic_enabled(){return config.load(std::memory_order_acquire)&9;}
bool hc_recents_haptic_scroll_enabled(){return config.load(std::memory_order_acquire)&1;}
bool hc_recents_close_haptic_enabled(){return config.load(std::memory_order_acquire)&8;}
void hc_recents_close_haptic_ready(bool value){close_ready.store(value,std::memory_order_release);}
bool hc_recents_close_haptic_active(){return hc_recents_close_haptic_enabled()&&close_ready.load(std::memory_order_acquire)&&worker_alive.load(std::memory_order_acquire);}
void hc_recents_close_haptic_commit(){
    const auto current=config.load(std::memory_order_acquire);
    if(!(current&8)||!close_ready.load(std::memory_order_acquire)||!worker_alive.load(std::memory_order_acquire))return;
    const Request request{current,now(),1,Request::Operation::Close};
    if(output>=0&&write(output,&request,sizeof(request))==sizeof(request))close_queued.fetch_add(1,std::memory_order_relaxed);
    else close_dropped.fetch_add(1,std::memory_order_relaxed);
}
bool hc_recents_haptic_changed(){return changed.exchange(false,std::memory_order_acq_rel);}
void hc_recents_haptic_ready(bool value){ready.store(value,std::memory_order_release);publish();}
void hc_recents_haptic_observe(const home_layout::haptic::Observation& observation){
    if(!observation.valid)return;
    const auto current=config.load(std::memory_order_acquire);
    if(!(current&1)||!ready.load(std::memory_order_acquire))return;
    const auto lane=observation.lane;if(lane>=4)return;
    if(observation.extent){
        if(!std::isfinite(observation.value)||observation.value<8||observation.value>32768)return;
        const auto bits=std::bit_cast<uint64_t>(observation.value);extents[lane].store(bits,std::memory_order_relaxed);
        lanes[lane].extents.fetch_add(1,std::memory_order_relaxed);
        if(lane==0)extents[1].store(bits,std::memory_order_relaxed);return;
    }
    samples.fetch_add(1,std::memory_order_relaxed);
    auto& trace=lanes[lane];
    (observation.programmatic?trace.programmatic:observation.event==home_layout::haptic::Event::Start?trace.starts:
        observation.event==home_layout::haptic::Event::End?trace.ends:trace.updates).fetch_add(1,std::memory_order_relaxed);
    trace.pixels.store(std::bit_cast<uint64_t>(observation.value),std::memory_order_relaxed);
    thread_local std::array<home_layout::haptic::Crossing,4> crossings;
    const auto count=crossings[lane].accept(observation.event,observation.value,
        std::bit_cast<double>(extents[lane].load(std::memory_order_relaxed)),current>>16);
    if(!count)return;const Request request{current,now(),count};
    // One bounded nonblocking write, no Binder, allocation, waiting or logging
    // on the Flutter isolate. The worker and server discard stale/disabled work.
    if(output>=0&&write(output,&request,sizeof(request))==sizeof(request))queued.fetch_add(count,std::memory_order_relaxed);
    else dropped.fetch_add(count,std::memory_order_relaxed);
}
HomeRecentsHapticCounters hc_recents_haptic_counters(){
    HomeRecentsHapticCounters out{samples.load(std::memory_order_relaxed),queued.load(std::memory_order_relaxed),delivered.load(std::memory_order_relaxed),dropped.load(std::memory_order_relaxed)};
    out.close_queued=close_queued.load(std::memory_order_relaxed);out.close_delivered=close_delivered.load(std::memory_order_relaxed);out.close_dropped=close_dropped.load(std::memory_order_relaxed);
    for(size_t i=0;i<lanes.size();++i){const auto& lane=lanes[i];
        out.lanes[i]={lane.starts.load(std::memory_order_relaxed),lane.updates.load(std::memory_order_relaxed),lane.ends.load(std::memory_order_relaxed),
            lane.programmatic.load(std::memory_order_relaxed),lane.extents.load(std::memory_order_relaxed),
            std::bit_cast<double>(lane.pixels.load(std::memory_order_relaxed)),std::bit_cast<double>(extents[i].load(std::memory_order_relaxed))};
    }
    return out;
}
