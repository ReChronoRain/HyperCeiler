/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_context.h"
#include "home_native_window_observer.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_context_contract.h"
#include "home_native_extra_snapshot.h"
#include "home_native_provider_contract.h"
#include "home_recent_cards_live_image.h"
#include "home_local_config_channel.h"
#include "nativehook/got_hook_backend.h"
#include <android/log.h>
#include <atomic>
#include <time.h>
#include <sys/system_properties.h>
#include <cstring>

namespace {
namespace sdk=home_layout::cards::sdk;
using Getter=void*(*)(void*);
struct Binding {
    Getter original=nullptr;
    void(*retain)(void*)=nullptr;
    void(*release)(void*)=nullptr;
    uint32_t(*uid)(void*)=nullptr;
    int32_t(*pid)(void*)=nullptr;
    void*(*create_resolver)(void* const*)=nullptr;
};
std::optional<Binding> storage;
std::atomic<const Binding*> binding{nullptr};
std::atomic<void*> retained{nullptr},validated{nullptr};
std::atomic_bool constructor_matches{false};
std::atomic_flag capturing=ATOMIC_FLAG_INIT,setting_up=ATOMIC_FLAG_INIT;
std::vector<nhk::GotHook<>> hooks;
std::vector<sdk::LiveImage> images;
bool failed=false;
bool pending_reported=false;
unsigned attempts=0;
int64_t retry=0,identity_at=0,installed_at=0;
int64_t now(){timespec t{};clock_gettime(CLOCK_BOOTTIME,&t);return int64_t(t.tv_sec)*1000+t.tv_nsec/1000000;}
bool needed() {
    if(home_local_config::requested())return true;
    char value[PROP_VALUE_MAX]={};
    if(__system_property_get("debug.hyperceiler.home_keyguard_probe",value)>0&&std::strcmp(value,"1")==0)return true;
    if(__system_property_get("debug.hyperceiler.home_input_probe",value)>0&&std::strcmp(value,"1")==0)return true;
    if(__system_property_get("debug.hyperceiler.home_input_receive",value)>0&&std::strcmp(value,"1")==0)return true;
    return hc_native_extra_resident_selected()
        ||(__system_property_get("debug.hyperceiler.home_extra_probe",value)>0&&std::strcmp(value,"1")==0);
}
bool read_pointer(uintptr_t address,const void*& out) {
    return sdk::read_live(address,std::as_writable_bytes(std::span(&out,1)));
}
void capture(void* runtime) {
    const auto* api=binding.load(std::memory_order_acquire);
    if(runtime&&!retained.load(std::memory_order_acquire)
        &&!capturing.test_and_set(std::memory_order_acquire)) {
        // Exactly one public SDK retain, with the caller's reference still live.
        // No Binder, allocation, property read, logging or worker wait here.
        api->retain(runtime);
        retained.store(runtime,std::memory_order_release);
    }
}
void* get_context(void* runtime) {
    const auto* api=binding.load(std::memory_order_acquire);
    auto* context=api->original(runtime);
    if(context==runtime)capture(runtime);
    return context;
}
void retain_runtime(void* runtime) {
    binding.load(std::memory_order_acquire)->retain(runtime);
    capture(runtime);
}
void release_runtime(void* runtime) {
    // Preserve our reference before the caller releases its possibly last one.
    capture(runtime);
    binding.load(std::memory_order_acquire)->release(runtime);
}
void* create_resolver(void* const* handle) {
    auto* result=binding.load(std::memory_order_acquire)->create_resolver(handle);
    // Observe a real stock SDK constructor call. The bootstrap's direct call
    // does not pass through this caller-specific GOT, so it cannot self-confirm.
    if(result&&!constructor_matches.load(std::memory_order_relaxed)&&handle&&*handle==retained.load(std::memory_order_acquire))
        constructor_matches.store(true,std::memory_order_release);
    return result;
}
void install() {
    if(failed||binding.load(std::memory_order_acquire)||now()<retry)return;
    constexpr std::array<std::string_view,4> names={"libhyper_os_app_public.so",
        "libhyper_os_app_private.dylib.so","libhyper_os_core_data_public.so","libapp_launcher.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live) {
        if(!reason.ends_with(": not loaded"))failed=true;
        constexpr unsigned delay[]={500,2000,10000,30000};
        retry=now()+delay[std::min(attempts++,3u)];
        if(failed||attempts<=3)__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
            "native Context capture pending: %s, permanent=%d",reason.c_str(),failed);
        return;
    }
    std::vector<sdk::Image> parsed;
    for(size_t i=0;i<3;++i) {
        auto image=sdk::Image::parse((*live)[i].file,(*live)[i].base);
        if(!image){failed=true;return;}parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto contract=home_native_context::resolve(graph);
    const auto factory=graph.exported("content_resolver_new");
    auto caller=sdk::Image::parse(live->back().file,live->back().base);
    if(!contract||!caller||!factory||!home_native_provider::resolver_handle_reference(graph,*factory)) {
        failed=true;__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
            "native Context ownership/getter contract declined, no repeated attempts");return;
    }
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    Binding api{reinterpret_cast<Getter>(address(contract->context)),
        reinterpret_cast<void(*)(void*)>(address(contract->retain)),
        reinterpret_cast<void(*)(void*)>(address(contract->release)),
        reinterpret_cast<uint32_t(*)(void*)>(address(contract->uid)),
        reinterpret_cast<int32_t(*)(void*)>(address(contract->pid)),
        reinterpret_cast<void*(*)(void* const*)>(address(*factory))};
    const std::array<std::pair<std::string_view,const void*>,4> originals={{{"Runtime_get_context",reinterpret_cast<const void*>(api.original)},
        {"Runtime_inc_strong",reinterpret_cast<const void*>(api.retain)},
        {"Runtime_dec_strong",reinterpret_cast<const void*>(api.release)},
        {"content_resolver_new",reinterpret_cast<const void*>(api.create_resolver)}}};
    for(const auto& expected:originals) {
        const auto slots=caller->metadata().collect_slots(expected.first);
        if(slots.empty()||slots.size()>nhk::GotHook<>::kSlotCapacity){failed=true;return;}
        for(const auto& slot:slots) {
            const void* value=nullptr;
            if(!read_pointer(caller->metadata().runtime(slot.rva),value)||value!=expected.second){failed=true;return;}
        }
    }
    storage=api;binding.store(&*storage,std::memory_order_release);
    const std::array<nhk::GotHookRequest,4> request={{{"Runtime_get_context",reinterpret_cast<void*>(get_context)},
        {"Runtime_inc_strong",reinterpret_cast<void*>(retain_runtime)},
        {"Runtime_dec_strong",reinterpret_cast<void*>(release_runtime)},
        {"content_resolver_new",reinterpret_cast<void*>(create_resolver)}}};
    bool clean=true;std::vector<nhk::GotHook<>> residual;
    const bool installed=nhk::install_got_hooks(caller->metadata(),request,hooks,read_pointer,true,&clean,{},&residual);
    if(!installed){hooks.insert(hooks.end(),residual.begin(),residual.end());failed=true;}
    for(auto& image:*live){std::vector<std::byte> empty;image.file.swap(empty);}
    images=std::move(*live);
    installed_at=now();
    __android_log_print(installed?ANDROID_LOG_INFO:ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
        "native Context capture GOT ready=%d rollback_clean=%d, public SDK reference ownership",installed,clean);
}
}
void hc_native_context_prime() {
    if(!needed()||setting_up.test_and_set(std::memory_order_acquire))return;
    // Loader completion is a new availability signal. Do not delay past a
    // just-loaded launcher's first Runtime borrow because of an earlier miss.
    retry=0;
    install();setting_up.clear(std::memory_order_release);
}
void hc_native_context_refresh() {
    hc_native_window_observer_refresh();
    if(!needed()||setting_up.test_and_set(std::memory_order_acquire))return;
    struct Unlock {~Unlock(){setting_up.clear(std::memory_order_release);}} unlock;
    install();if(failed||!binding.load(std::memory_order_acquire))return;
    if(now()-identity_at>=30000) {
        identity_at=now();if(!sdk::images_current(images)){failed=true;validated.store(nullptr,std::memory_order_release);return;}
    }
    for(const auto& hook:hooks)if(hook.partial||hook.health(read_pointer)!=nhk::GotHook<>::State::healthy) {
        failed=true;validated.store(nullptr,std::memory_order_release);
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native Context GOT changed, capture paused until launcher reload");return;
    }
    auto* runtime=retained.load(std::memory_order_acquire);
    if(validated.load(std::memory_order_acquire))return;
    if(!runtime||!constructor_matches.load(std::memory_order_acquire)) {
        if(!pending_reported&&now()-installed_at>=5000) {
            pending_reported=true;__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
                "native Context pending, retained=%d stock_constructor_confirmed=%d, bootstrap not called",
                runtime!=nullptr,constructor_matches.load(std::memory_order_acquire));
        }
        return;
    }
    const auto* api=binding.load(std::memory_order_acquire);
    if(api->uid(runtime)!=getuid()||api->pid(runtime)!=getpid()) {
        failed=true;api->release(runtime);retained.store(nullptr,std::memory_order_release);
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native Context process identity declined");return;
    }
    validated.store(runtime,std::memory_order_release);
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native Context captured and retained, SDK uid/pid and stock constructor handle match, no Java VM required");
}
void* hc_native_context_get(){return needed()?validated.load(std::memory_order_acquire):nullptr;}
#else
void hc_native_context_prime(){}
void hc_native_context_refresh(){}
void* hc_native_context_get(){return nullptr;}
#endif
