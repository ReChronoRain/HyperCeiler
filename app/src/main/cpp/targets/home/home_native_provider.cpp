/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_provider.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_provider_contract.h"
#include "home_native_context.h"
#include "home_native_glass_probe.h"
#include "home_native_glass_runtime.h"
#include "home_recent_statusbar.h"
#include "home_native_glass_motion.h"
#include "home_native_keyguard.h"
#include "home_native_input_probe.h"
#include "home_native_input_receive.h"
#include "home_native_extra_snapshot.h"
#include "home_native_unlock_transition.h"
#include "home_recent_cards_live_image.h"
#include "home_local_config_channel.h"
#include "home_layout_config.h"
#include <android/log.h>
#include <atomic>
#include <sys/eventfd.h>
#include <poll.h>
#include <pthread.h>
#include <unistd.h>
#include <time.h>
#include <cerrno>
#include <sys/system_properties.h>

namespace {
namespace sdk=home_layout::cards::sdk;
using home_native_provider::AcquireContract;
struct Reply {std::array<uint64_t,6> words{};};
struct StringRef {const char* data;size_t length;};
static_assert(sizeof(Reply)==AcquireContract::bytes&&sizeof(StringRef)==16);
struct Binding {
    AcquireContract contract;
    void*(*create)(void* const*)=nullptr;
    void(*destroy)(void*)=nullptr;
    void(*delete_client)(void*)=nullptr;
    Reply(*acquire)(void*,StringRef)=nullptr;
    void(*drop_error)(void*)=nullptr;
    uintptr_t table=0;
    std::vector<sdk::LiveImage> images;
};
std::optional<Binding> api;
std::atomic_bool enabled{false},connected{false},queued{false},working{false};
std::atomic_bool keyguard_probe_requested{false};
std::atomic_bool input_probe_requested{false};
std::atomic_bool input_receive_requested{false};
std::atomic_bool extra_probe_requested{false},extra_resident_requested{false};
std::atomic_bool capsule_probe_requested{false},capsule_probe_started{false};
std::atomic_bool glass_probe_requested{false},glass_probe_started{false},glass_hold_requested{false},glass_visible_requested{false},glass_native_requested{false};
std::atomic<int64_t> due{0};
int wake=-1;
bool last_enabled=false,last_connected=false;
bool last_capsule_probe=false,last_probe=false,last_hold=false,last_visible=false,last_keyguard=false,last_native=false;
bool last_input=false;
bool last_receive=false;
bool last_extra=false,last_resident=false;
home_dock::Config last_appearance;
// The event worker exclusively owns these SDK objects and initialization state.
void* client=nullptr;
void* resolver_owner=nullptr;
std::atomic_bool unsupported{false};
unsigned failures=0;
std::string last_error;
int64_t last_report=0;
int64_t now(){timespec t{};clock_gettime(CLOCK_BOOTTIME,&t);return int64_t(t.tv_sec)*1000+t.tv_nsec/1000000;}
void report(const std::string& detail) {
    if(detail==last_error&&now()-last_report<30000)return;
    last_error=detail;last_report=now();
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Provider bootstrap: %s",detail.c_str());
}
void retry() {
    constexpr unsigned delays[]={500,2000,10000,30000};
    due.store(now()+delays[std::min(failures++,3u)],std::memory_order_release);
}
bool load() {
    if(unsupported)return false;
    if(api) {
        if(sdk::images_current(api->images))return true;
        unsupported=true;report("SDK generation changed, waiting for launcher reload");return false;
    }
    constexpr std::array<std::string_view,6> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live) {
        unsupported=!reason.ends_with(": not loaded");report("SDK unavailable, "+reason);return false;
    }
    std::vector<sdk::Image> parsed;
    for(const auto& image:*live) {
        auto item=sdk::Image::parse(image.file,image.base);
        if(!item){unsupported=true;report("SDK metadata declined");return false;}
        parsed.push_back(std::move(*item));
    }
    const sdk::Graph graph(std::move(parsed));std::string_view stage;
    const auto contract=home_native_provider::resolve(graph,&stage);
    if(!contract){unsupported=true;report("SDK contract declined, "+std::string(stage));return false;}
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    uintptr_t drop=0;
    if(!sdk::read_live(address(contract->error.table_slot),std::as_writable_bytes(std::span(&drop,1)))
        ||drop!=address(contract->error.drop)){unsupported=true;report("SDK destructor binding declined");return false;}
    Binding ready{*contract,reinterpret_cast<void*(*)(void* const*)>(address(contract->create_resolver)),
        reinterpret_cast<void(*)(void*)>(address(contract->delete_resolver)),
        reinterpret_cast<void(*)(void*)>(address(contract->delete_client)),
        reinterpret_cast<Reply(*)(void*,StringRef)>(address(contract->acquire)),
        reinterpret_cast<void(*)(void*)>(drop),address(contract->error.table),{}};
    for(auto& image:*live){std::vector<std::byte> empty;image.file.swap(empty);}
    ready.images=std::move(*live);api=std::move(ready);return true;
}
void release_client() {
    if(!client&&!resolver_owner)return;
    // A normal SDK Client_delete closes the unstable Provider reference and
    // destroys its Box. Never manually free SDK allocations with our allocator.
    if(load()) {
        if(client)api->delete_client(client);
        if(resolver_owner)api->destroy(resolver_owner);
    }
    client=nullptr;resolver_owner=nullptr;
}
void acquire() {
    auto* context=hc_native_context_get();
    if(!context){report("waiting for a retained launcher SDK Context");retry();return;}
    if(!load()){retry();return;}
    auto* resolver=api->create(&context);
    if(!resolver){report("SDK resolver unavailable");retry();return;}
    // Keep the producer alive through the returned Client's full lifetime;
    // no assumption that the opaque SDK Client is independent of its resolver.
    resolver_owner=resolver;
    constexpr std::string_view authority="com.sevtinge.hyperceiler.provider.sharedprefs";
    auto reply=api->acquire(resolver,StringRef{authority.data(),authority.size()});
    const auto bytes=std::as_writable_bytes(std::span(reply.words));const auto& layout=api->contract;
    const auto tag=std::to_integer<uint8_t>(bytes[layout.tag]);
    if(tag==layout.rejected) {
        uintptr_t table=0;int32_t code=0;
        std::memcpy(&table,bytes.data()+layout.error_at+layout.error.vector+layout.error.table_field,8);
        std::memcpy(&code,bytes.data()+layout.error_at+layout.error.code,4);
        if(table!=api->table){unsupported=true;report("SDK returned an unsupported owned error, no further calls");return;}
        api->drop_error(bytes.data()+layout.error_at+layout.error.vector);
        release_client();
        report("platform Provider request declined, code="+std::to_string(code));retry();return;
    }
    uintptr_t owned=0;std::memcpy(&owned,bytes.data()+layout.pointer,8);
    const auto option=std::to_integer<uint8_t>(bytes[layout.option]);
    if(tag==layout.accepted&&option==layout.absent&&!owned) {
        release_client();
        report("module Provider unavailable");retry();return;
    }
    if(tag!=layout.accepted||option!=layout.present||!owned) {
        unsupported=true;report("SDK returned an unsupported owned result, no further calls");return;
    }
    client=reinterpret_cast<void*>(owned);
    // Holding an unstable reference allows normal module initialization to
    // finish without making the launcher depend on that process surviving.
    due.store(now()+15000,std::memory_order_release);
    report("module Provider acquired, waiting for authenticated configuration publication");
    if(!enabled.load(std::memory_order_acquire)||connected.load(std::memory_order_acquire))release_client();
}
void unlock_wake(){hc_native_glass_motion_notify();hc_native_provider_notify();}
void* work(void*) {
    pthread_setname_np(pthread_self(),"hc-home-wakeup");
    for(;;) {
        // Snapshot/owner renewal deadlines cannot depend on the layout worker:
        // its one-time AOT scan can take longer than the owner's entire lease.
        // With no scheduled snapshot this is still an indefinite event wait.
        pollfd pending{wake,POLLIN,0};
        const auto ready=poll(&pending,1,hc_native_extra_snapshot_wait_ms());
        if(ready<0){if(errno==EINTR)continue;return nullptr;}
        if(ready>0) {
            if(!(pending.revents&POLLIN))return nullptr;
            uint64_t signals=0;
            if(read(wake,&signals,sizeof(signals))!=sizeof(signals)){if(errno==EINTR)continue;return nullptr;}
        }
        working.store(true,std::memory_order_release);
        struct Complete {~Complete(){working.store(false,std::memory_order_release);}} complete;
        queued.store(false,std::memory_order_release);
        hc_native_input_probe_refresh(input_probe_requested.load(std::memory_order_acquire));
        hc_native_extra_snapshot_refresh(hc_native_context_get(),extra_probe_requested.load(std::memory_order_acquire),
            input_receive_requested.load(std::memory_order_acquire),extra_resident_requested.load(std::memory_order_acquire));
        hc_native_extra_actions_refresh();
        if(enabled.load(std::memory_order_acquire)&&glass_probe_requested.load(std::memory_order_acquire)) {
            home_dock::Config appearance;
            if(auto* context=hc_native_context_get();context&&home_layout::local_dock_config(appearance)
                &&!glass_probe_started.exchange(true,std::memory_order_acq_rel))hc_native_glass_probe(context,appearance);
        }
        if(capsule_probe_requested.load(std::memory_order_acquire)) {
            home_dock::Config appearance;
            if(auto* context=hc_native_context_get();context&&home_layout::local_dock_config(appearance)
                &&!capsule_probe_started.exchange(true,std::memory_order_acq_rel))hc_native_capsule_probe(context,appearance);
        }
        const bool hold=glass_hold_requested.load(std::memory_order_acquire);
        if(!hold)hc_native_glass_hold_refresh(nullptr,nullptr,hc_native_provider_notify);
        hc_native_unlock_refresh(hold||hc_native_glass_release_pending(),unlock_wake);
        hc_native_keyguard_refresh(hc_native_context_get(),keyguard_probe_requested.load(std::memory_order_acquire),hc_native_provider_notify);
        home_dock::Config appearance;
        if(hold&&home_layout::local_dock_config(appearance))
            hc_native_glass_hold_refresh(hc_native_context_get(),&appearance,hc_native_provider_notify,glass_visible_requested.load(std::memory_order_acquire),glass_native_requested.load(std::memory_order_acquire));
        else if(hold)hc_native_glass_hold_refresh(nullptr,nullptr,hc_native_provider_notify);
        if(!enabled.load(std::memory_order_acquire)||connected.load(std::memory_order_acquire)) {
            release_client();failures=0;due.store(0,std::memory_order_release);continue;
        }
        if(unsupported||now()<due.load(std::memory_order_acquire))continue;
        // An extra-gesture renewal wake must not bypass configuration startup's
        // hold/backoff or prematurely release its unstable Provider reference.
        if(client){release_client();report("configuration startup still pending");retry();continue;}
        acquire();
    }
}
}
void hc_native_provider_notify() {
    if(wake<0||queued.exchange(true,std::memory_order_acq_rel))return;
    const uint64_t signal=1;
    if(write(wake,&signal,sizeof(signal))!=sizeof(signal))queued.store(false,std::memory_order_release);
}
void hc_native_provider_refresh() {
    const bool wanted=home_local_config::requested();
    // A dozing/gated maintenance worker can expire the execution lease while
    // the authenticated service is still connected. Acquiring its Provider
    // again cannot repair that lease and needlessly wakes the module process.
    const bool live=wanted&&home_local_config::peer_connected();
    char renderer[PROP_VALUE_MAX]={};
    const bool keyguard=__system_property_get("debug.hyperceiler.home_keyguard_probe",renderer)>0&&std::strcmp(renderer,"1")==0;
    const bool input=__system_property_get("debug.hyperceiler.home_input_probe",renderer)>0&&std::strcmp(renderer,"1")==0;
    const bool receive=__system_property_get("debug.hyperceiler.home_input_receive",renderer)>0&&std::strcmp(renderer,"1")==0;
    const bool extra=__system_property_get("debug.hyperceiler.home_extra_probe",renderer)>0&&std::strcmp(renderer,"1")==0;
    const bool resident=hc_native_extra_resident_selected();
    __system_property_get("debug.hyperceiler.home_glass_backend",renderer);
    const bool probe=wanted&&!glass_probe_started.load(std::memory_order_acquire)&&std::strcmp(renderer,"app-probe")==0;
    const bool capsule_probe=wanted&&!capsule_probe_started.load(std::memory_order_acquire)
        &&std::strcmp(renderer,"app-capsule-probe")==0;
    const bool visible=wanted&&std::strcmp(renderer,"app-visible-probe")==0;
    const bool native=wanted&&std::strcmp(renderer,"app-native")==0;
    const bool hold=wanted&&(visible||native||std::strcmp(renderer,"app-hold-probe")==0);
    home_dock::Config appearance;
    if(hold) {
        home_layout::local_dock_config(appearance);
        // Experimental observer evidence, sampled by the existing maintenance
        // worker. No logger/SDK/IPC enters a Dart hook or animation callback.
        static unsigned last_scene=~0u,reports=0;static uint64_t last_frames=0;
        const unsigned scene=home_recent_bar::dock_scene()&15;
        const auto frames=hc_native_glass_motion_frames();
        if((scene!=last_scene||(frames>last_frames&&(last_frames==0||frames-last_frames>=32)))&&reports<12) {
            last_scene=scene;last_frames=frames;++reports;
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
                "native glass local scene mask=%u, statusbarRequested=%d, remoteMask=%u, localFrames=%llu",scene,
                home_recent_bar::requested(),unsigned(home_recent_bar::packed()&15),
                static_cast<unsigned long long>(frames));
        }
    }
    if(wake<0) {
        if((!wanted&&!keyguard&&!input&&!receive&&!extra&&!resident)||!hc_native_context_get())return;
        // Notifications are coalesced. The worker waits indefinitely when no
        // snapshot deadline is armed; no idle periodic wake is introduced.
        wake=eventfd(0,EFD_CLOEXEC);if(wake<0)return;
        pthread_t thread{};if(pthread_create(&thread,nullptr,work,nullptr)!=0){close(wake);wake=-1;return;}
        pthread_detach(thread);
    }
    enabled.store(wanted,std::memory_order_release);connected.store(live,std::memory_order_release);
    keyguard_probe_requested.store(keyguard,std::memory_order_release);
    input_probe_requested.store(input,std::memory_order_release);
    input_receive_requested.store(receive,std::memory_order_release);
    extra_probe_requested.store(extra,std::memory_order_release);
    extra_resident_requested.store(resident,std::memory_order_release);
    glass_probe_requested.store(probe,std::memory_order_release);
    capsule_probe_requested.store(capsule_probe,std::memory_order_release);
    glass_hold_requested.store(hold,std::memory_order_release);
    glass_visible_requested.store(visible,std::memory_order_release);
    glass_native_requested.store(native,std::memory_order_release);
    const bool changed=capsule_probe!=last_capsule_probe||resident!=last_resident||extra!=last_extra||receive!=last_receive||input!=last_input||keyguard!=last_keyguard||wanted!=last_enabled||live!=last_connected||probe!=last_probe
        ||hold!=last_hold||visible!=last_visible||native!=last_native||(hold&&appearance!=last_appearance);
    last_capsule_probe=capsule_probe;
    last_enabled=wanted;last_connected=live;
    last_input=input;
    last_receive=receive;
    last_extra=extra;last_resident=resident;
    last_keyguard=keyguard;last_probe=probe;last_hold=hold;last_visible=visible;last_native=native;last_appearance=appearance;
    if(!changed&&(working.load(std::memory_order_acquire)
        ||(!capsule_probe&&!probe&&!hc_native_extra_snapshot_due()&&!hc_native_input_receive_due()&&!(hc_native_glass_release_pending()&&hc_native_glass_hold_due())&&!(keyguard&&hc_native_keyguard_due())&&!(hold&&(hc_native_glass_hold_due()||hc_native_unlock_due()))
            &&(!wanted||live||unsupported.load(std::memory_order_acquire)||now()<due.load(std::memory_order_acquire)))))return;
    hc_native_provider_notify();
}
#else
void hc_native_provider_notify(){}
void hc_native_provider_refresh(){}
#endif
