/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_vibrator.h"

#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_local_config_channel.h"
#include "home_native_vibrator_contract.h"
#include "home_recent_cards_live_image.h"
#include "home_native_window.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <sys/system_properties.h>
#include <atomic>
#include <chrono>
#include <dlfcn.h>

namespace {
namespace sdk=home_native_vibrator::sdk;
using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
using Parcel=std::unique_ptr<AParcel,decltype(&AParcel_delete)>;
std::atomic<uint32_t> transaction{0};
std::atomic<uint64_t> policy_transactions{0};
Binder service(nullptr,AIBinder_decStrong),token(nullptr,AIBinder_decStrong);

enum class Backend {off,direct,cached,published};
Backend backend() {
    char value[PROP_VALUE_MAX]{};
    if(__system_property_get("debug.hyperceiler.home_haptic_backend",value)==0)
        return home_local_config::requested()?Backend::published:Backend::off;
    if(std::strcmp(value,"launcher")==0)return Backend::direct;
    if(std::strcmp(value,"launcher-cached")==0)return Backend::cached;
    if(std::strcmp(value,"launcher-published")==0)return Backend::published;
    return Backend::off;
}
const AIBinder_Class* binder_class(const char* descriptor) {
    return AIBinder_Class_define(descriptor,[](void* data){return data;},[](void*){},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
}
bool policy_probe_selected() {
    char value[PROP_VALUE_MAX]{};
    return __system_property_get("debug.hyperceiler.home_haptic_policy_probe",value)>0
        &&std::strcmp(value,"1")==0;
}
std::pair<int,int> read_policy(uint64_t codes) {
    static thread_local Binder power(nullptr,AIBinder_decStrong),window(nullptr,AIBinder_decStrong);
    static const auto* type=binder_class("android.os.IPowerManager");
    if(!power||!AIBinder_isAlive(power.get())) {
        using Check=AIBinder*(*)(const char*);
        auto check=reinterpret_cast<Check>(dlsym(RTLD_DEFAULT,"AServiceManager_checkService"));
        power.reset(check?check("power"):nullptr);
        if(power) {
            const auto* existing=AIBinder_getClass(power.get());
            const auto* descriptor=existing?AIBinder_Class_getDescriptor(existing):nullptr;
            if(existing?(!descriptor||std::strcmp(descriptor,"android.os.IPowerManager")!=0):
                (!type||!AIBinder_associateClass(power.get(),type)))power.reset();
        }
    }
    std::string reason;
    if(!window||!AIBinder_isAlive(window.get()))window=home_native_platform::window(reason);
    const auto read=[&](AIBinder* target,uint32_t code) {
        if(!target)return -1;
        auto reply=home_native_platform::call(target,code,[](AParcel*){return STATUS_OK;},reason);
        int32_t value=-1;
        if(!reply||AParcel_readInt32(reply.get(),&value)!=STATUS_OK||value<0||value>1
            ||AParcel_getDataPosition(reply.get())!=AParcel_getDataSize(reply.get()))return -1;
        return value;
    };
    return {read(power.get(),static_cast<uint32_t>(codes>>32)),
        read(window.get(),static_cast<uint32_t>(codes))};
}
void sample_policy(uint64_t codes) {
    const auto state=read_policy(codes);
    const auto [interactive,locked]=state;
    static std::pair<int,int> previous{-2,-2};
    static unsigned reports=0;
    if(state!=previous&&reports++<12)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeLayout",
        "launcher haptic policy sample interactive=%d locked=%d uid=%u",interactive,locked,getuid());
    previous=state;
}
}

void hc_native_vibrator_refresh() {
    const auto current=backend();
    const bool playback=current!=Backend::off&&!transaction.load(std::memory_order_relaxed);
    const bool probe=policy_probe_selected();
    const bool want_policy=probe||current==Backend::cached||current==Backend::published;
    if(!playback&&!want_policy)return;
    static bool playback_unsupported=false,policy_unsupported=false;
    static std::chrono::steady_clock::time_point next_resolution,next_sample;
    const auto now=std::chrono::steady_clock::now();
    const bool resolve_playback=playback&&!playback_unsupported;
    auto codes=policy_transactions.load(std::memory_order_acquire);
    const bool resolve_policy=want_policy&&!codes&&!policy_unsupported;
    if((resolve_playback||resolve_policy)&&now>=next_resolution) {
        next_resolution=now+std::chrono::seconds(2);
        constexpr std::array<std::string_view,1> names={"libhyper_os_aidl.dylib.so"};
        auto live=sdk::capture_live(names);
        if(!live)return;
        auto image=sdk::Image::parse(live->front().file,live->front().base);
        if(!sdk::images_current(*live))return;
        std::vector<sdk::Image> images;
        if(image)images.push_back(std::move(*image));
        const sdk::Graph graph(std::move(images));
        if(resolve_playback) {
            auto contract=home_native_vibrator::resolve(graph);
            playback_unsupported=!contract;
            if(contract)transaction.store(contract->vibrate.transaction,std::memory_order_release);
            __android_log_print(contract?ANDROID_LOG_INFO:ANDROID_LOG_WARN,"HyperCeiler.HomeLayout",
                "launcher vibration SDK contract %s",contract?"ready":"unavailable");
        }
        if(resolve_policy) {
            const auto policy=home_native_vibrator::resolve_policy(graph);
            policy_unsupported=!policy;
            if(policy) {
                codes=(uint64_t(policy->interactive.transaction)<<32)|policy->locked.transaction;
                policy_transactions.store(codes,std::memory_order_release);
            }
            __android_log_print(policy?ANDROID_LOG_INFO:ANDROID_LOG_WARN,"HyperCeiler.HomeLayout",
                "launcher haptic policy SDK contract %s",policy?"ready":"unavailable");
        }
    }
    if(probe&&codes&&now>=next_sample) {
        next_sample=now+std::chrono::milliseconds(500);
        sample_policy(codes);
    }
}

bool hc_native_vibrator_cache_selected(){const auto mode=backend();return mode==Backend::cached||mode==Backend::published;}
bool hc_native_vibrator_published_selected(){return backend()==Backend::published;}

std::optional<bool> hc_native_vibrator_policy_allows() {
    const auto codes=policy_transactions.load(std::memory_order_acquire);
    if(!codes)return {};
    const auto [interactive,locked]=read_policy(codes);
    if(interactive==0||locked==1)return false;
    if(interactive<0||locked<0)return {};
    return true;
}

bool hc_native_vibrator_ready() {
    if(!transaction.load(std::memory_order_acquire)||backend()==Backend::off)return false;
    if(!service||!AIBinder_isAlive(service.get())) {
        using Check=AIBinder*(*)(const char*);
        auto check=reinterpret_cast<Check>(dlsym(RTLD_DEFAULT,"AServiceManager_checkService"));
        service.reset(check?check("vibrator_manager"):nullptr);
        if(!service)return false;
        constexpr char descriptor[]="android.os.IVibratorManagerService";
        const auto* existing=AIBinder_getClass(service.get());
        static const auto* binding=binder_class(descriptor);
        const auto* name=existing?AIBinder_Class_getDescriptor(existing):nullptr;
        if(existing?(!name||std::strcmp(name,descriptor)!=0):
            (!binding||!AIBinder_associateClass(service.get(),binding)))service.reset();
    }
    if(!token) {
        static const auto* binding=binder_class("com.sevtinge.hyperceiler.home.IRecentsVibrationToken");
        if(binding)token.reset(AIBinder_new(binding,nullptr));
    }
    return service&&token;
}

bool hc_native_vibrator_play(std::span<const uint8_t> arguments) {
    if(backend()==Backend::off||!service||!token)return false;
    Parcel body(AParcel_create(),AParcel_delete);
    if(!body||AParcel_unmarshal(body.get(),arguments.data(),arguments.size())!=STATUS_OK)return false;
    // NDK unmarshal may leave its cursor at the end of the imported bytes.
    if(AParcel_setDataPosition(body.get(),0)!=STATUS_OK)return false;
    int32_t uid=-1;
    if(AParcel_readInt32(body.get(),&uid)!=STATUS_OK||uid!=static_cast<int32_t>(getuid()))return false;
    AParcel* request=nullptr;AParcel* reply=nullptr;
    auto status=AIBinder_prepareTransaction(service.get(),&request);
    if(status==STATUS_OK&&request) {
        status=AParcel_appendFrom(body.get(),request,0,static_cast<int32_t>(arguments.size()));
        if(status==STATUS_OK)status=AParcel_writeStrongBinder(request,token.get());
        if(status==STATUS_OK)status=AIBinder_transact(service.get(),transaction.load(std::memory_order_acquire),&request,&reply,0);
    }
    Parcel sent(request,AParcel_delete),response(reply,AParcel_delete);
    AStatus* result=nullptr;
    const bool decoded=status==STATUS_OK&&reply&&AParcel_readStatusHeader(reply,&result)==STATUS_OK&&result;
    const std::unique_ptr<AStatus,decltype(&AStatus_delete)> header(result,AStatus_delete);
    const bool accepted=decoded&&AStatus_isOk(result)&&AParcel_getDataPosition(reply)==AParcel_getDataSize(reply);
    static unsigned reports=0;
    if(reports++<12)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeLayout",
        "launcher vibration submit accepted=%d status=%d exception=%d uid=%u",accepted,status,
        decoded?AStatus_getExceptionCode(result):0,getuid());
    if(status==STATUS_DEAD_OBJECT)service.reset();
    return accepted;
}
#else
void hc_native_vibrator_refresh(){}
bool hc_native_vibrator_ready(){return false;}
bool hc_native_vibrator_cache_selected(){return false;}
bool hc_native_vibrator_published_selected(){return false;}
std::optional<bool> hc_native_vibrator_policy_allows(){return {};}
bool hc_native_vibrator_play(std::span<const uint8_t>){return false;}
#endif
