/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_input_probe.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_input_callback.h"
#include "home_recent_cards_live_image.h"
#include <android/log.h>
#include <unistd.h>
void hc_native_input_probe_refresh(bool requested) {
    static bool attempted=false;
    if(!requested){attempted=false;return;}
    if(attempted)return;
    attempted=true;
    namespace sdk=home_layout::cards::sdk;
    constexpr std::array<std::string_view,4> names={"libhyper_os_ui_public.so","libhyper_os_ui_private.dylib.so",
        "libhyper_os_aidl.dylib.so","libstd.dylib.so"};
    std::string reason;auto images=sdk::capture_live(names,&reason);
    if(!images){__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: %s",reason.c_str());return;}
    std::vector<sdk::Image> parsed;
    for(const auto& image:*images) {
        auto current=sdk::Image::parse(image.file,image.base);
        if(!current){__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: SDK metadata");return;}
        parsed.push_back(std::move(*current));
    }
    const sdk::Graph graph(std::move(parsed));const auto contract=home_native_input::resolve_monitor(graph);
    if(!contract||!sdk::images_current(*images)) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: producer/result/drop contract");return;
    }
    const auto callback=home_native_input::callback_drop(graph);
    if(!callback) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: carrier copy/destructor contract");return;
    }
    const auto adapter=reinterpret_cast<void(*)(void*)>((*images)[callback->adapter.image].base+callback->adapter.va);
    const auto events=home_native_input::callback_events(graph,*callback);
    if(!events) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: event forwarding contract");return;
    }
    std::vector<home_native_input::CallbackEventProbe> event_probes;
    for(const auto& event:*events)event_probes.push_back({event,
        reinterpret_cast<void(*)(void*,void*,void*,void*)>((*images)[event.adapter.image].base+event.adapter.va)});
    if(!sdk::images_current(*images)||!home_native_input::probe_callback_drop(*callback,adapter,event_probes)) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe declined: carrier/event bridge callback");return;
    }
    // These two words are the validated public C AAPCS64 return registers.
    // No SDK object or Dart heap field is read, retained or allocated by us.
    struct Reply {uint64_t status;uintptr_t owned;};
    static_assert(sizeof(Reply)==16);
    const auto create=reinterpret_cast<Reply(*)(const char*,size_t,int32_t)>((*images)[contract->create.image].base+contract->create.va);
    const auto drop=reinterpret_cast<void(*)(void*)>((*images)[contract->drop.image].base+contract->drop.va);
    constexpr std::string_view name="HyperCeiler-Native-Input-Probe";
    const auto reply=create(name.data(),name.size(),0);
    if(reply.status==0&&reply.owned) {
        // Immediate SDK destruction, no receiver, no event cloning, no
        // pilferPointers and no retained monitor or display surface.
        drop(reinterpret_cast<void*>(reply.owned));
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
            "native input probe created and released, uid=%u pid=%d receiver=0 pilfer=0 liveCallbackTable=verified carrierDrop=verified eventBridge=verified",getuid(),getpid());return;
    }
    const auto flag=reply.status&((uint64_t(1)<<contract->error_shift)-1);
    if(flag==contract->error_flag) {
        const auto status=static_cast<int32_t>(reply.status>>contract->error_shift);
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe API declined: status=%d",status);return;
    }
    __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input probe returned unsupported result; no object access");
}
#else
void hc_native_input_probe_refresh(bool){}
#endif
