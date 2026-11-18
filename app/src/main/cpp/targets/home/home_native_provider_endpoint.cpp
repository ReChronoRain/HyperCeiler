/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_provider_endpoint.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_bundle_contract.h"
#include "home_native_binder_contract.h"
#include "home_recent_cards_live_image.h"
#include <android/binder_ibinder.h>
#include <memory>
namespace {
namespace sdk=home_layout::cards::sdk;
using Drop=void(*)(void*);
struct Reply {std::array<uint64_t,6> words{};};
struct StringRef {const char* data;size_t length;};
static_assert(sizeof(Reply)==48&&sizeof(StringRef)==16);
struct Owned {
    void* value;Drop drop;
    ~Owned(){if(value)drop(value);}
};
struct Api {
    home_native_provider::AcquireContract acquire;
    home_native_provider::CallContract call;
    home_native_bundle::Contract bundle;
    home_native_binder::Contract binder;
    std::vector<sdk::LiveImage> images;
    uintptr_t address(sdk::Address at)const{return images[at.image].base+at.va;}
    Drop drop(sdk::Address at)const{return reinterpret_cast<Drop>(address(at));}
    bool error(Reply& reply,uint32_t at,const home_native_provider::ErrorContract& error,std::string& reason)const {
        auto bytes=std::as_writable_bytes(std::span(reply.words));uintptr_t table=0;
        std::memcpy(&table,bytes.data()+at+error.vector+error.table_field,sizeof(table));
        if(table!=address(error.table)){reason="Unsupported SDK error owner";return false;}
        int32_t code=0;std::memcpy(&code,bytes.data()+at+error.code,sizeof(code));
        drop(error.drop)(bytes.data()+at+error.vector);reason="Provider request declined, code="+std::to_string(code);return true;
    }
};
std::unique_ptr<Api> api;
bool load(std::string& reason,bool& unsupported) {
    if(api&&sdk::images_current(api->images))return true;
    api.reset();
    constexpr std::array<std::string_view,9> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so","libhyper_os_binder_private.dylib.so"};
    auto live=sdk::capture_live(names,&reason);if(!live)return false;
    unsupported=true;std::vector<sdk::Image> parsed;
    for(const auto& image:*live) {
        auto value=sdk::Image::parse(image.file,image.base);if(!value){reason="SDK metadata unavailable";return false;}
        parsed.push_back(std::move(*value));
    }
    const sdk::Graph graph(std::move(parsed));
    const auto acquire=home_native_provider::resolve(graph);const auto call=home_native_provider::resolve_call(graph);
    const auto bundle=home_native_bundle::resolve(graph);const auto binder=home_native_binder::resolve(graph);
    if(!acquire||!call||!bundle||!binder||!home_native_bundle::call_owner(graph,*call,*bundle)||!sdk::images_current(*live)) {
        reason="SDK Provider/Bundle/Binder ownership declined";return false;
    }
    auto ready=std::make_unique<Api>();ready->acquire=*acquire;ready->call=*call;ready->bundle=*bundle;ready->binder=*binder;
    ready->images=std::move(*live);
    for(const auto* error:{&ready->acquire.error,&ready->call.error}) {
        uintptr_t target=0;
        if(!sdk::read_live(ready->address(error->table_slot),std::as_writable_bytes(std::span(&target,1)))
            ||target!=ready->address(error->drop)) {reason="SDK error destructor linkage changed";return false;}
    }
    // Callbacks retain only parsed value contracts and mapping identities.
    for(auto& image:ready->images){std::vector<std::byte>().swap(image.file);}
    api=std::move(ready);unsupported=false;return true;
}
}
AIBinder* hc_native_extra_endpoint(void* context,std::string& reason,bool& unsupported) {
    unsupported=false;if(!context){reason="Retained Context unavailable";return nullptr;}
    if(!load(reason,unsupported))return nullptr;
    const auto& acquire=api->acquire;
    Owned resolver{reinterpret_cast<void*(*)(void* const*)>(api->address(acquire.create_resolver))(&context),api->drop(acquire.delete_resolver)};
    if(!resolver.value){reason="SDK Resolver unavailable";return nullptr;}
    constexpr std::string_view authority="com.sevtinge.hyperceiler.provider.sharedprefs";
    auto acquired=reinterpret_cast<Reply(*)(void*,StringRef)>(api->address(acquire.acquire))(
        resolver.value,{authority.data(),authority.size()});
    auto bytes=std::as_bytes(std::span(acquired.words));const auto tag=std::to_integer<uint8_t>(bytes[acquire.tag]);
    if(tag==acquire.rejected){unsupported=!api->error(acquired,acquire.error_at,acquire.error,reason);return nullptr;}
    uintptr_t pointer=0;std::memcpy(&pointer,bytes.data()+acquire.pointer,sizeof(pointer));
    if(tag!=acquire.accepted||std::to_integer<uint8_t>(bytes[acquire.option])!=acquire.present||!pointer) {
        unsupported=tag!=acquire.accepted;reason="Provider unavailable or incompatible result";return nullptr;
    }
    Owned client{reinterpret_cast<void*>(pointer),api->drop(acquire.delete_client)};
    const auto& call=api->call;alignas(8) std::array<uint8_t,32> argument{};
    // Actual adapter selects a null Java arg for a nonzero optional-RStr tag.
    // All argument fields/roles were established by resolve_call.
    argument[call.arg_tag]=1;
    constexpr std::string_view method="hc_home_extra_endpoint";
    auto called=reinterpret_cast<Reply(*)(void*,StringRef,const void*,void*)>(api->address(call.call))(
        client.value,{method.data(),method.size()},argument.data(),nullptr);
    bytes=std::as_bytes(std::span(called.words));const auto state=std::to_integer<uint8_t>(bytes[call.tag]);
    if(state==call.rejected){unsupported=!api->error(called,call.error_at,call.error,reason);return nullptr;}
    pointer=0;std::memcpy(&pointer,bytes.data()+call.pointer,sizeof(pointer));
    if(state!=call.accepted||!pointer){unsupported=state!=call.accepted;reason="Endpoint Bundle unavailable";return nullptr;}
    Owned result{reinterpret_cast<void*>(pointer),api->drop(api->bundle.drop)};
    constexpr std::string_view key="endpoint";
    auto* endpoint=reinterpret_cast<AIBinder*(*)(void*,StringRef)>(api->address(api->binder.get))(
        result.value,{key.data(),key.size()});
    if(!endpoint)reason="System UI snapshot endpoint not registered";
    return endpoint;
}
#else
AIBinder* hc_native_extra_endpoint(void*,std::string&,bool& unsupported){unsupported=true;return nullptr;}
#endif
