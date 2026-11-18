/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_unlock_transition.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_window.h"
#include "home_dock_reveal.h"
#include <android/log.h>
#include <atomic>
#include <time.h>
#include <mutex>
namespace {
using home_native_platform::Binder;
// HCU1 is our module protocol. It does not replace or reinterpret HCL4/HCS1.
constexpr uint32_t transaction=0x00484355,ack=0x48435531,version=1;
std::atomic<int64_t> epoch{-1},next_attempt{0};
std::atomic<int32_t> event_flags{0};
std::atomic<uint64_t> received{0},accepted{0};
std::atomic_bool connected{false};
std::atomic<unsigned> visibility{0};
std::atomic_bool dock_owned{false},dock_requested{false},dock_rejected{false},dock_releasing{false};
std::atomic<int64_t> dock_epoch{0};
std::atomic<uint64_t> window_generation{0};
std::mutex signal_lock;
int64_t now(){timespec t{};return clock_gettime(CLOCK_MONOTONIC,&t)==0?int64_t(t.tv_sec)*1000+t.tv_nsec/1000000:0;}
struct Cookie {void(*wake)();std::atomic_bool active{true};explicit Cookie(void(*callback)()):wake(callback){}};
const AIBinder_Class* listener_class() {
    static auto* type=AIBinder_Class_define("com.sevtinge.hyperceiler.home.UnlockTransition",[](void* data){return data;},
        [](void* data){delete static_cast<Cookie*>(data);},
        [](AIBinder* binder,transaction_code_t code,const AParcel* input,AParcel*)->binder_status_t {
            auto* cookie=static_cast<Cookie*>(AIBinder_getUserData(binder));
            if(AIBinder_getCallingUid()!=1000)return STATUS_PERMISSION_DENIED;
            if(code==FIRST_CALL_TRANSACTION+3) {
                int64_t claim=0;bool owned=false;
                auto status=AParcel_readInt64(input,&claim);if(status!=STATUS_OK)return status;
                status=AParcel_readBool(input,&owned);if(status!=STATUS_OK)return status;
                if(claim<=0||AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
                if(cookie->active.load(std::memory_order_acquire)&&claim==dock_epoch.load(std::memory_order_acquire)) {
                    if(!owned&&dock_requested.load(std::memory_order_acquire)&&!dock_releasing.load(std::memory_order_acquire))dock_rejected.store(true,std::memory_order_release);
                    dock_owned.store(owned,std::memory_order_release);if(cookie->wake)cookie->wake();
                }
                return STATUS_OK;
            }
            if(code==FIRST_CALL_TRANSACTION+2) {
                bool known=false,visible=false;
                auto status=AParcel_readBool(input,&known);if(status!=STATUS_OK)return status;
                status=AParcel_readBool(input,&visible);if(status!=STATUS_OK)return status;
                if((!known&&visible)||AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
                if(cookie->active.load(std::memory_order_acquire)) {
                    const unsigned next=known?(visible?3u:1u):0u;
                    if(visibility.exchange(next,std::memory_order_acq_rel)!=next&&cookie->wake)cookie->wake();
                }
                return STATUS_OK;
            }
            if(code==FIRST_CALL_TRANSACTION+1) {
                if(AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
                if(cookie->active.load(std::memory_order_acquire)) {
                    window_generation.fetch_add(1,std::memory_order_release);
                    dock_owned.store(false,std::memory_order_release);
                    connected.store(false,std::memory_order_release);visibility.store(0,std::memory_order_release);if(cookie->wake)cookie->wake();
                }
                return STATUS_OK;
            }
            if(code!=FIRST_CALL_TRANSACTION)return STATUS_UNKNOWN_TRANSACTION;
            int64_t at=0;int32_t flags=0;
            auto status=AParcel_readInt64(input,&at);if(status!=STATUS_OK)return status;
            status=AParcel_readInt32(input,&flags);if(status!=STATUS_OK)return status;
            if(AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
            received.fetch_add(1,std::memory_order_relaxed);
            if(!cookie->active.load(std::memory_order_acquire)||!home_dock::Reveal::accepts_pending(at,now()))return STATUS_OK;
            {
                std::lock_guard lock(signal_lock);
                if(at<=epoch.load(std::memory_order_relaxed))return STATUS_OK;
                event_flags.store(flags,std::memory_order_relaxed);epoch.store(at,std::memory_order_release);
                accepted.fetch_add(1,std::memory_order_relaxed);
            }
            if(cookie->wake)cookie->wake();return STATUS_OK;
        });
    return type;
}
struct Session {
    Binder window{nullptr,AIBinder_decStrong},listener{nullptr,AIBinder_decStrong};
    Cookie* cookie=nullptr;bool requested=false,unsupported=false;
    bool send(int operation,std::string& reason) {
        auto reply=home_native_platform::call(window.get(),transaction,[&](AParcel* input) {
            auto status=AParcel_writeInt32(input,operation);
            if(status==STATUS_OK&&operation==1)status=AParcel_writeStrongBinder(input,listener.get());
            return status;
        },reason,&unsupported);
        if(!reply)return false;
        int32_t marker=0,revision=0,available=0;
        if(AParcel_readInt32(reply.get(),&marker)!=STATUS_OK||AParcel_readInt32(reply.get(),&revision)!=STATUS_OK
            ||AParcel_readInt32(reply.get(),&available)!=STATUS_OK||uint32_t(marker)!=ack||uint32_t(revision)!=version
            ||available< -1||available>1||AParcel_getDataPosition(reply.get())!=AParcel_getDataSize(reply.get())) {
            unsupported=true;reason="early unlock protocol mismatch";return false;
        }
        if(available<0){unsupported=true;reason="early unlock source unsupported";return false;}
        if(!available){reason="early unlock window identity pending";return false;}return true;
    }
    bool admission(bool wanted) {
        if(!wanted&&!dock_requested.load(std::memory_order_acquire))return true;
        if(wanted&&dock_owned.load(std::memory_order_acquire))return true;
        if(wanted&&dock_rejected.load(std::memory_order_acquire))return false;
        // A delayed release callback must not revoke a later configuration's
        // admission on the same registered Binder listener.
        if(wanted&&!dock_requested.exchange(true,std::memory_order_acq_rel))dock_epoch.fetch_add(1,std::memory_order_acq_rel);
        const auto claim=dock_epoch.load(std::memory_order_acquire);
        dock_releasing.store(!wanted,std::memory_order_release);
        std::string reason;bool unsupported_operation=false;
        // Admission has its own optional capability. An older server may
        // reject op 3 without disabling its existing window/time listener.
        auto reply=home_native_platform::call(window.get(),transaction,[&](AParcel* input) {
            auto status=AParcel_writeInt32(input,wanted?3:4);
            return status==STATUS_OK?AParcel_writeInt64(input,claim):status;
        },reason,&unsupported_operation);
        if(!reply){
            if(wanted&&unsupported_operation) {
                dock_requested.store(false,std::memory_order_release);dock_rejected.store(true,std::memory_order_release);
            }
            // The pre-extension unregister operation also releases a lease.
            if(!wanted&&unsupported_operation&&send(2,reason)) {
                connected.store(false,std::memory_order_release);dock_requested.store(false,std::memory_order_release);dock_owned.store(false,std::memory_order_release);return true;
            }
            return false;
        }
        int32_t marker=0,revision=0,available=0;int64_t echoed=0;
        if(AParcel_readInt32(reply.get(),&marker)!=STATUS_OK||AParcel_readInt32(reply.get(),&revision)!=STATUS_OK
            ||AParcel_readInt32(reply.get(),&available)!=STATUS_OK||uint32_t(marker)!=ack||uint32_t(revision)!=version
            ||available<0||available>1||AParcel_readInt64(reply.get(),&echoed)!=STATUS_OK||echoed!=claim
            ||AParcel_getDataPosition(reply.get())!=AParcel_getDataSize(reply.get())) {
            if(wanted)dock_rejected.store(true,std::memory_order_release);return false;
        }
        if(wanted&&available)dock_owned.store(true,std::memory_order_release);
        if(!wanted&&available){dock_requested.store(false,std::memory_order_release);dock_owned.store(false,std::memory_order_release);dock_rejected.store(false,std::memory_order_release);dock_releasing.store(false,std::memory_order_release);}
        return available==1;
    }
    bool start(void(*wake)(),std::string& reason) {
        window=home_native_platform::window(reason);if(!window||!send(0,reason))return false;
        const auto* type=listener_class();if(!type){reason="early unlock listener class unavailable";return false;}
        cookie=new Cookie(wake);listener.reset(AIBinder_new(type,cookie));
        if(!listener){delete cookie;cookie=nullptr;reason="early unlock listener unavailable";return false;}
        if(!home_native_platform::callbacks(reason)){unsupported=true;return false;}
        requested=true;return send(1,reason);
    }
    ~Session() {
        if(cookie)cookie->active.store(false,std::memory_order_release);
        dock_requested.store(false,std::memory_order_release);dock_owned.store(false,std::memory_order_release);dock_rejected.store(false,std::memory_order_release);dock_releasing.store(false,std::memory_order_release);
        if(requested){std::string reason;if(!send(2,reason))__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","early unlock listener cleanup: %s",reason.c_str());}
    }
};
std::unique_ptr<Session> session;
}
bool hc_native_dock_admission(bool wanted){return session?session->admission(wanted):!wanted;}
bool hc_native_dock_owned(){return connected.load(std::memory_order_acquire)&&dock_owned.load(std::memory_order_acquire);}
bool hc_native_dock_requested(){return dock_requested.load(std::memory_order_acquire);}
bool hc_native_dock_rejected(){return dock_rejected.load(std::memory_order_acquire);}
int64_t hc_native_unlock_epoch(){return epoch.load(std::memory_order_acquire);}
unsigned hc_native_home_visibility(){return connected.load(std::memory_order_acquire)?visibility.load(std::memory_order_acquire):0;}
uint64_t hc_native_home_window_generation(){return window_generation.load(std::memory_order_acquire);}
HomeUnlockObservation hc_native_unlock_observation(){std::lock_guard lock(signal_lock);return {epoch.load(std::memory_order_acquire),event_flags.load(std::memory_order_relaxed),received.load(std::memory_order_relaxed),accepted.load(std::memory_order_relaxed),connected.load(std::memory_order_acquire)};}
bool hc_native_unlock_due(){const auto at=next_attempt.load(std::memory_order_acquire);return at&&now()>=at;}
void hc_native_unlock_refresh(bool wanted,void(*wake)()) {
    static bool blocked=false;static unsigned failures=0;
    static std::string previous;static uint64_t reported=0;
    if(!wanted){session.reset();visibility.store(0,std::memory_order_release);connected.store(false,std::memory_order_release);epoch.store(-1,std::memory_order_release);next_attempt.store(0,std::memory_order_release);return;}
    if(session&&!connected.load(std::memory_order_acquire))session.reset();
    if(session) {
        const auto count=accepted.load(std::memory_order_acquire);
        if(count!=reported){reported=count;__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","early unlock observed epochMs=%lld flags=%d accepted=%llu",(long long)epoch.load(std::memory_order_acquire),event_flags.load(std::memory_order_relaxed),(unsigned long long)count);}
        return;
    }
    if(blocked||now()<next_attempt.load(std::memory_order_acquire))return;
    auto pending=std::make_unique<Session>();std::string reason;
    if(pending->start(wake,reason)) {
        session=std::move(pending);connected.store(true,std::memory_order_release);failures=0;next_attempt.store(0,std::memory_order_release);
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","early unlock listener ready, timestamp only, native rendering independent");return;
    }
    blocked=pending->unsupported;
    if(reason!=previous||failures<3){previous=reason;__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","early unlock source unavailable: %s, unsupported=%d",reason.c_str(),blocked);}
    constexpr int64_t delay[]={500,2000,10000,30000};next_attempt.store(blocked?0:now()+delay[std::min(failures++,3u)],std::memory_order_release);
}
#else
void hc_native_unlock_refresh(bool,void(*)()){}
bool hc_native_unlock_due(){return false;}
int64_t hc_native_unlock_epoch(){return -1;}
unsigned hc_native_home_visibility(){return 0;}
bool hc_native_dock_admission(bool wanted){return !wanted;}
bool hc_native_dock_owned(){return false;}
bool hc_native_dock_requested(){return false;}
bool hc_native_dock_rejected(){return false;}
uint64_t hc_native_home_window_generation(){return 0;}
HomeUnlockObservation hc_native_unlock_observation(){return {};}
#endif
