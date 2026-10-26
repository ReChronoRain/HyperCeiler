/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_gestures_os4.h"
#include "home_workspace_swipe.h"
#include <android/log.h>
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <dlfcn.h>
#include <time.h>
#include <pthread.h>
#include <sys/eventfd.h>
#include <unistd.h>
#include <cerrno>
#include <sys/system_properties.h>
#include <atomic>
#include <mutex>

#if defined(HYPERCEILER_DOCK_NATIVE_MOTION)
extern "C" std::atomic<uint64_t> dock_edit_packed;
#endif

extern "C" uint32_t hc_workspace_double_enabled=0;
extern "C" uint32_t hc_workspace_double_selected=0;
extern "C" uint32_t hc_workspace_swipe_enabled=0;
void *hc_home_touch_original=nullptr;
void *hc_back_touch_original=nullptr;
void *hc_workspace_scene_original=nullptr;
namespace {
struct Api {
    int (*masked)(void*)=nullptr;
    int (*action)(void*)=nullptr;
    float (*x)(void*,size_t)=nullptr;
    float (*y)(void*,size_t)=nullptr;
    int64_t (*time)(void*)=nullptr;
    size_t (*pointers)(void*)=nullptr;
    void (*set_action)(void*,int)=nullptr;
    void *(*clone)(void*)=nullptr;
    void (*drop)(void*)=nullptr;
    int (*pointer_id)(void*,size_t)=nullptr;
    uint32_t home_mode=0;
};
Api api;
std::mutex lock;
home_gestures::Recognizer home_state,back_state;
home_gestures::Settings settings;
std::atomic_bool probe{false};
AIBinder *window=nullptr;
struct Request {int event=0;int64_t time=0;uint64_t revision=0;};
std::array<Request,8> requests{};
size_t head=0,count=0;
int wake=-1;
bool workspace_ready=false;
bool scene_ready=false;
home_workspace_gesture::ScopeContract scope_contract;
home_workspace_gesture::SwipeRecognizer workspace_state;
home_workspace_gesture::StatusPolicy status_policy;
int64_t workspace_start=0,stock_block_start=0,stock_block_end=0;
void publish_workspace_locked(){
    __atomic_store_n(&hc_workspace_double_enabled,uint32_t(workspace_ready&&wake>=0&&(settings.mask&1u)),__ATOMIC_RELEASE);
    __atomic_store_n(&hc_workspace_swipe_enabled,uint32_t(workspace_ready&&scene_ready
        &&scope_contract.down_return&&wake>=0&&(settings.mask&30u)),__ATOMIC_RELEASE);
}
void *on_create(void*) {return nullptr;}
void on_destroy(void*) {}
binder_status_t on_transact(AIBinder*,transaction_code_t,const AParcel*,AParcel*) {return STATUS_UNKNOWN_TRANSACTION;}
void send(Request request) {
    if(!window) {
        using GetService=AIBinder*(*)(const char*);
        auto get=reinterpret_cast<GetService>(dlsym(RTLD_DEFAULT,"AServiceManager_getService"));
        if(!get)return;
        window=get("window");if(!window)return;
        const auto *cls=AIBinder_getClass(window);
        if(cls) {
            const char *descriptor=AIBinder_Class_getDescriptor(cls);
            if(!descriptor||std::strcmp(descriptor,"android.view.IWindowManager")) {
                AIBinder_decStrong(window);window=nullptr;return;
            }
        } else {
            static auto *contract=AIBinder_Class_define("android.view.IWindowManager",on_create,on_destroy,on_transact);
            if(!contract||!AIBinder_associateClass(window,contract)) {AIBinder_decStrong(window);window=nullptr;return;}
        }
    }
    AParcel *in=nullptr,*out=nullptr;int32_t ack=0,result=0;
    bool ok=AIBinder_prepareTransaction(window,&in)==STATUS_OK&&in
        &&AParcel_writeInt32(in,request.event)==STATUS_OK
        &&AParcel_writeInt64(in,request.time)==STATUS_OK;
    binder_status_t status=STATUS_FAILED_TRANSACTION;
    if(ok) {status=AIBinder_transact(window,0x00484351,&in,&out,0);in=nullptr;}
    if(status==STATUS_OK&&out)ok=AParcel_readInt32(out,&ack)==STATUS_OK&&ack==0x48435131
        &&AParcel_readInt32(out,&result)==STATUS_OK&&result==1;
    else ok=false;
    if(in)AParcel_delete(in);if(out)AParcel_delete(out);
    if(status!=STATUS_OK) {AIBinder_decStrong(window);window=nullptr;}
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture","action event=%d accepted=%d status=%d",request.event,ok,status);
}
void *action_worker(void*) {
    for(;;) {
        uint64_t signals=0;
        if(read(wake,&signals,sizeof(signals))!=sizeof(signals)) {if(errno==EINTR)continue;return nullptr;}
        for(;;) {
            Request r;bool valid=false;
            {
                std::lock_guard guard(lock);
                if(!count)break;
                r=requests[head];head=(head+1)%requests.size();--count;
                valid=r.revision==settings.revision&&(r.event==6?settings.quick:(settings.mask&(1u<<(r.event-1))));
            }
            timespec now{};
            if(valid&&clock_gettime(CLOCK_MONOTONIC,&now)==0) {
                const int64_t age=int64_t(now.tv_sec)*1000+now.tv_nsec/1000000-r.time;
                if(age>=0&&age<=750)send(r);
            }
        }
    }
}
bool start_worker_locked() {
    if(wake>=0)return true;
    wake=eventfd(0,EFD_CLOEXEC);if(wake<0)return false;
    pthread_t thread{};
    if(pthread_create(&thread,nullptr,action_worker,nullptr)!=0){close(wake);wake=-1;return false;}
    pthread_detach(thread);return true;
}
void emit(int event,uint64_t revision) {
    if(!event)return;
    timespec now{};if(clock_gettime(CLOCK_MONOTONIC,&now)!=0)return;
    int fd=-1;
    {
        std::lock_guard guard(lock);
        if(wake<0||count==requests.size()||revision!=settings.revision)return;
        requests[(head+count)%requests.size()]={event,int64_t(now.tv_sec)*1000+now.tv_nsec/1000000,revision};
        ++count;fd=wake;
    }
    const uint64_t one=1;(void)write(fd,&one,sizeof(one));
}
home_gestures::Decision observe(void *owner,void *event,bool back) {
    std::lock_guard guard(lock);
    if(!api.masked||(!settings.mask&&!settings.quick))return {};
    if(!back) {
#if defined(HYPERCEILER_DOCK_NATIVE_MOTION)
        if(dock_edit_packed.load(std::memory_order_acquire)&1u) {home_state.reset();return {};}
#endif
        uint32_t mode=0;std::memcpy(&mode,static_cast<std::byte*>(owner)+api.home_mode,4);
        // Same gate as the original dispatcher, before any pointer API call.
        if(mode<1||mode>2) {home_state.reset();return {};}
    }
    auto &state=back?back_state:home_state;
    const int action=api.masked(event);
    if(action==3||action<0||action>6||action==4) {
        if(probe.load(std::memory_order_relaxed))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture","probe back=%d a=%d reset owner=%p",back,action,owner);
        state.reset();return {};
    }
    const size_t pointers=api.pointers(event);
    if(pointers<1||pointers>10) {state.reset();return {};}
    home_gestures::Sample e{action,static_cast<int>(pointers),
        api.x(event,0),api.y(event,0),api.time(event),reinterpret_cast<uintptr_t>(owner)};
    auto effective=settings;
    // A genuine workspace consumer owns double tap once its bank is ready.
    // The Rust recents/window route must not emit a second copy of that action.
    if(!back)effective.mask=0; // normal workspace is owned by its real scene, never recents
    const auto decision=state.accept(e,effective,back);
    if(probe.load(std::memory_order_relaxed))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture",
        "probe back=%d a=%d n=%zu x=%.1f y=%.1f t=%lld owner=%p decision=%d/%d/%d rv=%llu",
        back,action,pointers,e.x,e.y,static_cast<long long>(e.time),owner,
        decision.cancel,decision.consume,decision.gesture,static_cast<unsigned long long>(settings.revision));
    return decision;
}
template<class F> void forward(F original,void *event,home_gestures::Decision d) {
    if(d.cancel) {
        // The event is shared with the input receiver. Restore the full action/index even on cancel.
        const int saved=api.action(event);
        api.set_action(event,3);
        original();
        api.set_action(event,saved);
    } else if(!d.consume) original();
    // Original cancel/animation cleanup has completed before any task promotion or action.
    emit(d.gesture,d.revision);
}
}
extern "C" void hc_home_touch(void *owner,void *event,uintptr_t a,uintptr_t b) {
    using F=void(*)(void*,void*,uintptr_t,uintptr_t);
    auto original=reinterpret_cast<F>(hc_home_touch_original);if(!original)return;
    forward([&]{original(owner,event,a,b);},event,observe(owner,event,false));
}
// The window dispatcher consumes its MotionEvent handle. Never restore or read it after
// original() returns. Cancel a clone instead, leaving other consumers' shared event untouched.
template<class F> void forward_owned(F original,void *event,home_gestures::Decision d) {
    if(d.cancel) {
        void *copy=api.clone(event);
        if(!copy) {original(event);return;} // Allocation failure stays stock; no action emitted.
        api.set_action(copy,3);
        api.drop(event);
        original(copy);
    } else if(!d.consume) original(event);
    else api.drop(event);
    emit(d.gesture,d.revision);
}
extern "C" void hc_back_touch(void *owner,void *event) {
    using F=void(*)(void*,void*);
    auto original=reinterpret_cast<F>(hc_back_touch_original);if(!original)return;
    forward_owned([&](void *input){original(owner,input);},event,observe(owner,event,true));
}

bool hc_gestures_bind(uintptr_t base,const home_gestures::Targets&t) {
    std::lock_guard guard(lock);
    if(api.masked)return true; // Live bank owns this generation; never overwrite its ABI pointers.
    for(auto v:t.imports)if(!v||base>UINTPTR_MAX-v)return false;
    // bind() is called only by locate() on the existing post-specialization layout worker.
    // No Java/pre-fork Handler and no Binder transaction on the native input thread.
    if(!start_worker_locked())return false;
    api={reinterpret_cast<int(*)(void*)>(base+t.imports[0]),
        reinterpret_cast<int(*)(void*)>(base+t.imports[1]),
        reinterpret_cast<float(*)(void*,size_t)>(base+t.imports[2]),
        reinterpret_cast<float(*)(void*,size_t)>(base+t.imports[3]),
        reinterpret_cast<int64_t(*)(void*)>(base+t.imports[4]),
        reinterpret_cast<size_t(*)(void*)>(base+t.imports[5]),
        reinterpret_cast<void(*)(void*,int)>(base+t.imports[6]),
        reinterpret_cast<void*(*)(void*)>(base+t.imports[7]),
        reinterpret_cast<void(*)(void*)>(base+t.imports[8]),
        reinterpret_cast<int(*)(void*,size_t)>(base+t.imports[9]),t.home_mode};
    return true;
}
void hc_gestures_config(const home_gestures::Config &config) {
    char diagnostic[PROP_VALUE_MAX]{};
    probe.store(__system_property_get("debug.hyperceiler.gesture_probe",diagnostic)>0&&diagnostic[0]=='1',std::memory_order_relaxed);
    std::lock_guard guard(lock);
    const unsigned mask=config.enabled?config.mask:0;
    const float density=float(config.density_dpi)/160.f,width=float(config.width);
    if(settings.mask!=mask||settings.quick!=config.quick||settings.density!=density||settings.width!=width) {
        settings={mask,config.quick,density,width,settings.revision+1};
        home_state.reset();back_state.reset();
        publish_workspace_locked();
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture","config mask=%u quick=%d dpi=%d width=%d",mask,config.quick,config.density_dpi,config.width);
    }
}

void hc_workspace_gestures_ready(bool ready) {
    std::lock_guard guard(lock);
    workspace_ready=ready;
    if(ready)(void)start_worker_locked();
    publish_workspace_locked();
}
extern "C" void hc_workspace_double_emit() {
    uint64_t revision=0;
    {std::lock_guard guard(lock);if(!workspace_ready||!(settings.mask&1u))return;revision=settings.revision;}
    if(probe.load(std::memory_order_relaxed))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture","workspace original double accepted rv=%llu",static_cast<unsigned long long>(revision));
    emit(home_gestures::DoubleTap,revision);
}

void hc_workspace_scope_bind(const home_workspace_gesture::ScopeContract& c) {
    std::lock_guard guard(lock);scope_contract=c;
    if(!c.down_return){workspace_state.reset();workspace_start=stock_block_start=stock_block_end=0;}
    publish_workspace_locked();
}
void hc_workspace_status_bind(const home_workspace_gesture::StatusPolicy& c) {
    std::lock_guard guard(lock);status_policy=c;stock_block_start=stock_block_end=0;
}
extern "C" uintptr_t hc_workspace_status_result(uintptr_t frame,uint64_t heap,uintptr_t table,
    const uintptr_t* saved,unsigned kind,uintptr_t null) {
    std::lock_guard guard(lock);uintptr_t value=null+0x20;
    if(kind==1)value=saved[status_policy.value_register];
    if(kind==2){uint32_t v=0;std::memcpy(&v,reinterpret_cast<void*>(saved[status_policy.base_register]+status_policy.bool_field),4);value=uint64_t(v)+(heap<<32);}
    uintptr_t event=0;std::memcpy(&event,reinterpret_cast<void*>(frame+status_policy.event_frame),sizeof(event));
    auto read=[](uintptr_t p,auto& v){if(p<4096)return false;std::memcpy(&v,reinterpret_cast<void*>(p),sizeof(v));return true;};
    int64_t time=0;bool block=value==null+0x20&&stock_block_start>0
        &&home_workspace_gesture::event_timestamp(event,heap,table,scope_contract,read,time)
        &&time>=stock_block_start&&time<=stock_block_end;
    if(block&&probe.load(std::memory_order_relaxed))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture",
        "workspace stock status interception suppressed kind=%u time=%lld",kind,static_cast<long long>(time));
    return block?null+0x30:value;
}
void hc_workspace_scene_ready(bool ready) {
    std::lock_guard guard(lock);scene_ready=ready;publish_workspace_locked();
}
extern "C" void hc_workspace_scope_authorize(uintptr_t frame,uint64_t heap,uintptr_t table) {
    std::lock_guard guard(lock);
    if(!__atomic_load_n(&hc_workspace_swipe_enabled,__ATOMIC_ACQUIRE))return;
    int64_t stamp=0;
    auto read=[](uintptr_t p,auto& value){if(p<4096)return false;std::memcpy(&value,reinterpret_cast<const void*>(p),sizeof(value));return true;};
    bool decoded=home_workspace_gesture::timestamp(frame,heap,table,scope_contract,read,stamp);
    bool accepted=decoded&&workspace_state.authorize(stamp,settings.revision);
    if(probe.load(std::memory_order_relaxed))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture",
        "workspace scope decoded=%d accepted=%d time=%lld",decoded,accepted,static_cast<long long>(stamp));
}
extern "C" void hc_workspace_scene(void* owner,void* event) {
    using F=void(*)(void*,void*);auto original=reinterpret_cast<F>(hc_workspace_scene_original);if(!original)return;
    home_gestures::Decision decision;
    {
        std::lock_guard guard(lock);
        const bool can_claim=workspace_ready&&scene_ready&&scope_contract.down_return
            &&(settings.mask&30u);
        // Readiness controls NEW claims, not delivery of lifecycle events to
        // an existing recognizer. Otherwise a Down forwarded while unready can
        // later lose its Move/Up to the previous cancelled sequence on recovery.
        // Keep draining an owned stream and clear its timestamp interval on the
        // next Down, even while the source-bank readiness gate is closed.
        if(!api.masked) {
            workspace_state.reset();workspace_start=stock_block_start=stock_block_end=0;
        } else if(can_claim||workspace_state.tracking()||stock_block_start>0) {
            home_workspace_gesture::SwipeSample e;e.action=api.masked(event);e.count=api.pointers(event);
            e.time=api.time(event);e.owner=reinterpret_cast<uintptr_t>(owner);
            if(e.action==0){workspace_start=e.time;stock_block_start=stock_block_end=0;}
            for(int i=0;i<std::min(e.count,2);++i){e.id[i]=api.pointer_id(event,i);e.x[i]=api.x(event,i);e.y[i]=api.y(event,i);}
            bool editing=!can_claim;
#if defined(HYPERCEILER_DOCK_NATIVE_MOTION)
            editing|=(dock_edit_packed.load(std::memory_order_acquire)&1u)!=0;
#endif
            decision=workspace_state.accept(e,settings,editing);
            if(decision.consume){stock_block_start=workspace_start;stock_block_end=e.time;}
            if(probe.load(std::memory_order_relaxed)&&(settings.mask&30u))__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeGesture",
                "workspace scene a=%d n=%d x=%.1f y=%.1f t=%lld decision=%d/%d/%d",e.action,e.count,e.x[0],e.y[0],
                static_cast<long long>(e.time),decision.cancel,decision.consume,decision.gesture);
        }
    }
    // PlatformScene borrows the handle. CANCEL runs the original Flutter / Rust
    // cleanup first, restoring the complete shared action/index before dispatch.
    forward([&]{original(owner,event);},event,decision);
}
