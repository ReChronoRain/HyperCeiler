/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_window_observer.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_recent_cards_live_image.h"
#include "nativehook/got_hook_backend.h"
#include <android/binder_ibinder.h>
#include <android/log.h>
#include <sys/system_properties.h>
#include <dlfcn.h>
#include <atomic>
#include <cstring>
#include <time.h>
namespace {
namespace sdk=home_layout::cards::sdk;
using Define=decltype(&AIBinder_Class_define);
std::atomic<Define> original{nullptr};
std::atomic<AIBinder_Class_onTransact> dispatch{nullptr};
std::atomic_bool watching{false},ambiguous{false};
std::atomic<uint64_t> definitions{0},callbacks{0};
std::atomic<uintptr_t> first_window{0};
std::atomic<uint32_t> last_code{0},last_uid{0};
std::atomic<int32_t> last_status{0};
std::atomic_flag installing=ATOMIC_FLAG_INIT;
std::vector<nhk::GotHook<>> hooks;
std::vector<sdk::LiveImage> images;
bool blocked=false,installed=false;
int64_t retry=0;unsigned failures=0;
int64_t now(){timespec t{};clock_gettime(CLOCK_BOOTTIME,&t);return int64_t(t.tv_sec)*1000+t.tv_nsec/1000000;}
bool requested(){char value[PROP_VALUE_MAX]={};__system_property_get("debug.hyperceiler.home_window_probe",value);return std::strcmp(value,"1")==0;}
bool read_pointer(uintptr_t address,const void*& value){return sdk::read_live(address,std::as_writable_bytes(std::span(&value,1)));}
binder_status_t observed(AIBinder* binder,transaction_code_t code,const AParcel* input,AParcel* output) {
    const auto uid=AIBinder_getCallingUid();
    const auto result=dispatch.load(std::memory_order_acquire)(binder,code,input,output);
    if(watching.load(std::memory_order_acquire)) {
        // Numeric identity for ambiguity diagnostics only, never dereferenced
        // or retained as an object after this framework-owned callback.
        const auto id=reinterpret_cast<uintptr_t>(binder);uintptr_t absent=0;
        if(!first_window.compare_exchange_strong(absent,id,std::memory_order_relaxed)&&absent!=id)
            ambiguous.store(true,std::memory_order_relaxed);
        last_code.store(code,std::memory_order_relaxed);last_uid.store(uid,std::memory_order_relaxed);last_status.store(result,std::memory_order_relaxed);
        callbacks.fetch_add(1,std::memory_order_release);
    }
    return result;
}
AIBinder_Class* define(const char* descriptor,AIBinder_Class_onCreate create,AIBinder_Class_onDestroy destroy,AIBinder_Class_onTransact transact) {
    if(descriptor&&std::strcmp(descriptor,"android.view.IWindow")==0&&transact) {
        auto expected=AIBinder_Class_onTransact{};
        if(dispatch.compare_exchange_strong(expected,transact,std::memory_order_acq_rel)||expected==transact) {
            auto* type=original.load(std::memory_order_acquire)(descriptor,create,destroy,observed);
            if(type)definitions.fetch_add(1,std::memory_order_relaxed);
            return type;
        }
        ambiguous.store(true,std::memory_order_relaxed);
    }
    return original.load(std::memory_order_acquire)(descriptor,create,destroy,transact);
}
void install() {
    if(installed||blocked||now()<retry)return;
    constexpr std::array<std::string_view,3> names={"libhyper_os_aidl.dylib.so","libbinder_rs.dylib.so","libapp_launcher.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live) {
        blocked=!reason.ends_with(": not loaded");constexpr unsigned delay[]={500,2000,10000,30000};
        retry=now()+delay[std::min(failures++,3u)];
        if(blocked||failures<=3)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","own-window probe pending: %s permanent=%d",reason.c_str(),blocked);
        return;
    }
    // Standard NDK C ABI, not a guessed private Rust callback/vtable. Match
    // every current caller relocation against the actual platform function.
    const auto real=reinterpret_cast<Define>(dlsym(RTLD_DEFAULT,"AIBinder_Class_define"));
    if(!real){blocked=true;return;}
    std::vector<sdk::Image> callers;
    for(const auto& image:*live) {
        auto parsed=sdk::Image::parse(image.file,image.base);if(!parsed){blocked=true;return;}
        const auto slots=parsed->metadata().collect_slots("AIBinder_Class_define");
        if(slots.empty()||slots.size()>nhk::GotHook<>::kSlotCapacity){blocked=true;return;}
        for(const auto& slot:slots){const void* value=nullptr;if(!read_pointer(parsed->metadata().runtime(slot.rva),value)||value!=reinterpret_cast<void*>(real)){blocked=true;return;}}
        callers.push_back(std::move(*parsed));
    }
    original.store(real,std::memory_order_release);
    const std::array<nhk::GotHookRequest,1> requests={{{"AIBinder_Class_define",reinterpret_cast<void*>(define)}}};
    for(const auto& caller:callers) {
        std::vector<nhk::GotHook<>> added,residual;bool clean=true;
        if(!nhk::install_got_hooks(caller.metadata(),requests,added,read_pointer,true,&clean,{},&residual)) {
            hooks.insert(hooks.end(),residual.begin(),residual.end());blocked=true;
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","own-window probe caller install declined clean=%d",clean);break;
        }
        hooks.insert(hooks.end(),added.begin(),added.end());
    }
    if(blocked) {
        const bool clean=nhk::restore_got_hooks(std::span<const nhk::GotHook<>>(hooks),read_pointer,nhk::write_pointer);
        if(clean)hooks.clear();watching.store(false,std::memory_order_release);
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","own-window probe multi-caller rollback clean=%d",clean);
    }
    for(auto& image:*live){std::vector<std::byte> empty;image.file.swap(empty);}images=std::move(*live);
    installed=!blocked;
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","own-window standard NDK callback factory probe ready=%d, original callbacks preserved",installed);
}
}
void hc_native_window_observer_prime() {
    if(installing.test_and_set(std::memory_order_acquire))return;
    struct Unlock {~Unlock(){installing.clear(std::memory_order_release);}} unlock;
    const bool active=requested();watching.store(active&&!blocked,std::memory_order_release);
    if(active){retry=0;install();}
}
void hc_native_window_observer_refresh() {
    if(installing.test_and_set(std::memory_order_acquire))return;
    struct Unlock {~Unlock(){installing.clear(std::memory_order_release);}} unlock;
    const bool active=requested();watching.store(active&&!blocked,std::memory_order_release);if(!active)return;
    install();
    static uint64_t reported=UINT64_MAX,reported_definitions=UINT64_MAX;
    const auto count=callbacks.load(std::memory_order_acquire),types=definitions.load(std::memory_order_relaxed);
    if(count==reported&&types==reported_definitions)return;
    reported=count;reported_definitions=types;
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","own-window callback probe definitions=%llu callbacks=%llu lastCode=%u lastUid=%u lastStatus=%d ambiguous=%d",
        (unsigned long long)types,(unsigned long long)count,last_code.load(std::memory_order_relaxed),last_uid.load(std::memory_order_relaxed),last_status.load(std::memory_order_relaxed),ambiguous.load(std::memory_order_relaxed));
}
#else
void hc_native_window_observer_prime(){}
void hc_native_window_observer_refresh(){}
#endif
