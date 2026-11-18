/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_keyguard.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_glass_client.h"
#include "home_native_window.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <dlfcn.h>
#include <time.h>
namespace {
using home_native_glass::Client;
using home_native_glass::KeyguardProtocol;
using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
std::atomic<uint64_t> observed{0},unlocked{0},callbacks{0},received{0};
std::atomic<int32_t> last_uid{-1},last_status{0};
std::atomic<uint32_t> last_code{0};
std::atomic<int64_t> next_attempt{0};
uint64_t now_ns(){timespec t{};if(clock_gettime(CLOCK_MONOTONIC,&t))return 0;return uint64_t(t.tv_sec)*1000000000+uint64_t(t.tv_nsec);}
struct Cookie {
    uint32_t changed;void(*wake)();std::atomic_bool active{true};
    Cookie(uint32_t code,void(*callback)()):changed(code),wake(callback){}
};
const AIBinder_Class* listener_class() {
    // Standard platform AIDL descriptor. Transaction numbers come from actual
    // framework Stub fields, with reflected method/payload validation.
    static auto* type=AIBinder_Class_define("com.android.internal.policy.IKeyguardLockedStateListener",
        [](void* data){return data;},[](void* data){delete static_cast<Cookie*>(data);},
        [](AIBinder* binder,transaction_code_t code,const AParcel* input,AParcel* output)->binder_status_t {
            auto* cookie=static_cast<Cookie*>(AIBinder_getUserData(binder));
            received.fetch_add(1,std::memory_order_relaxed);
            last_uid.store(AIBinder_getCallingUid(),std::memory_order_relaxed);last_code.store(code,std::memory_order_relaxed);
            const auto reject=[&](binder_status_t value){last_status.store(value,std::memory_order_relaxed);if(cookie->active.load(std::memory_order_acquire)&&cookie->wake)cookie->wake();return value;};
            if(code!=cookie->changed)return reject(STATUS_UNKNOWN_TRANSACTION);
            // This is an observation from WMS, never a caller-supplied unlock.
            if(AIBinder_getCallingUid()!=1000)return reject(STATUS_PERMISSION_DENIED);
            bool locked=false;const auto read=AParcel_readBool(input,&locked);if(read!=STATUS_OK)return reject(read);
            if(AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return reject(STATUS_BAD_VALUE);
            last_status.store(STATUS_OK,std::memory_order_relaxed);
            if(cookie->active.load(std::memory_order_acquire)) {
                const auto time=now_ns();
                if(time) {
                    const auto old=observed.exchange((time&~uint64_t(3))|1u|(locked?2u:0u),std::memory_order_acq_rel);
                    if((old&3u)==3u&&!locked)unlocked.store(time,std::memory_order_release);
                    callbacks.fetch_add(1,std::memory_order_relaxed);
                    if(cookie->wake)cookie->wake();
                }
            }
            // Also works when a platform revision uses a synchronous callback;
            // one-way callers ignore the reply. No platform state is changed.
            if(output){auto* status=AStatus_newOk();const auto written=AParcel_writeStatusHeader(output,status);AStatus_delete(status);return written;}
            return STATUS_OK;
        });
    return type;
}
struct Session {
    KeyguardProtocol protocol;
    Binder window{nullptr,AIBinder_decStrong},listener{nullptr,AIBinder_decStrong};
    Cookie* cookie=nullptr;
    bool requested=false,unsupported=false;
    explicit Session(KeyguardProtocol value):protocol(value){}
    bool send(uint32_t code,std::string& reason) {
        return bool(home_native_platform::call(window.get(),code,[&](AParcel* input) {
            return AParcel_writeStrongBinder(input,listener.get());
        },reason,&unsupported));
    }
    bool start(void(*wake)(),std::string& reason) {
        window=home_native_platform::window(reason);if(!window)return false;
        const auto* listener_type=listener_class();if(!listener_type){reason="keyguard listener class unavailable";return false;}
        cookie=new Cookie(protocol.changed,wake);listener.reset(AIBinder_new(listener_type,cookie));
        if(!listener){delete cookie;cookie=nullptr;reason="keyguard listener allocation failed";return false;}
        observed.store(0,std::memory_order_release);unlocked.store(0,std::memory_order_release);callbacks.store(0,std::memory_order_release);received.store(0,std::memory_order_release);
        last_uid.store(-1,std::memory_order_relaxed);last_status.store(0,std::memory_order_relaxed);last_code.store(0,std::memory_order_relaxed);
        if(!home_native_platform::callbacks(reason)){unsupported=true;return false;}
        requested=true;
        return send(protocol.add,reason);
    }
    ~Session(){
        if(cookie)cookie->active.store(false,std::memory_order_release);
        if(requested){std::string reason;if(!send(protocol.remove,reason))
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native keyguard listener cleanup: %s",reason.c_str());}
        // User data dies with the Binder, including any outstanding remote ref.
    }
};
}
HomeKeyguardObservation hc_native_keyguard_observation() {
    const auto value=observed.load(std::memory_order_acquire);
    return {(value&1u)!=0,(value&2u)!=0,value&~uint64_t(3),unlocked.load(std::memory_order_acquire),callbacks.load(std::memory_order_relaxed),received.load(std::memory_order_relaxed),last_uid.load(std::memory_order_relaxed),last_status.load(std::memory_order_relaxed),last_code.load(std::memory_order_relaxed)};
}
bool hc_native_keyguard_due(){const auto at=next_attempt.load(std::memory_order_acquire);return at&&int64_t(now_ns()/1000000)>=at;}
void hc_native_keyguard_refresh(void* context,bool wanted,void(*wake)()) {
    static std::unique_ptr<Session> session;static bool blocked=false;static unsigned failures=0;
    static std::string previous;static uint64_t last_received=0;
    if(!wanted){
        if(session) {
            const auto value=hc_native_keyguard_observation();
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
                "native keyguard observer retiring: known=%d locked=%d callbacks=%llu received=%llu unlockUptimeNs=%llu",
                value.known,value.locked,(unsigned long long)value.callbacks,(unsigned long long)value.received,(unsigned long long)value.unlock_ns);
        }
        session.reset();observed.store(0,std::memory_order_release);unlocked.store(0,std::memory_order_release);
        callbacks.store(0,std::memory_order_release);next_attempt.store(0,std::memory_order_release);return;
    }
    if(session) {
        const auto value=hc_native_keyguard_observation();
        if(value.received!=last_received) {
            last_received=value.received;
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
                "native keyguard observer known=%d locked=%d observedUptimeNs=%llu unlockUptimeNs=%llu callbacks=%llu received=%llu code=%u uid=%d status=%d",
                value.known,value.locked,(unsigned long long)value.observed_ns,(unsigned long long)value.unlock_ns,(unsigned long long)value.callbacks,(unsigned long long)value.received,value.last_code,value.last_uid,value.last_status);
        }
        return;
    }
    if(blocked||!context||int64_t(now_ns()/1000000)<next_attempt.load(std::memory_order_acquire))return;
    std::string reason;home_native_glass::Failure failure;
    auto client=Client::open(context,reason,&failure,home_native_glass::Destination::preferences);
    auto protocol=client?client->keyguard_protocol(reason,&failure):std::nullopt;
    if(failure==home_native_glass::Failure::unsupported)blocked=true;
    if(protocol) {
        auto pending=std::make_unique<Session>(*protocol);
        if(pending->start(wake,reason)) {
            session=std::move(pending);failures=0;previous.clear();next_attempt.store(0,std::memory_order_release);last_received=0;
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native keyguard observer subscribed: add=%u remove=%u changed=%u, no module system Hook",protocol->add,protocol->remove,protocol->changed);
            return;
        }
        if(pending->unsupported)blocked=true;
    }
    if(reason!=previous||failures<3){previous=reason;__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native keyguard observer unavailable: %s",reason.c_str());}
    constexpr int64_t delay[]={500,2000,10000,30000};
    next_attempt.store(blocked?0:int64_t(now_ns()/1000000)+delay[std::min(failures++,3u)],std::memory_order_release);
}
#else
void hc_native_keyguard_refresh(void*,bool,void(*)()){}
bool hc_native_keyguard_due(){return false;}
HomeKeyguardObservation hc_native_keyguard_observation(){return {};}
#endif
