/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_input_pilfer_runtime.h"
#include "targets/home/home_native_window.h"
#include <cstdio>
namespace sdk=home_layout::cards::sdk;
struct Pair {uint64_t status;uintptr_t pointer;};
static_assert(sizeof(Pair)==16);
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_ui_public.so",RTLD_NOW|RTLD_GLOBAL)) {
        std::fprintf(stderr,"SDK load declined: %s\n",dlerror());return 1;
    }
    constexpr std::array<std::string_view,8> names={"libhyper_os_ui_public.so","libhyper_os_ui_private.dylib.so",
        "libhyper_os_aidl.dylib.so","libstd.dylib.so","libhyper_os_sys.dylib.so",
        "libhyper_os_bundle_private.dylib.so","libhyper_os_graphics_private.dylib.so","libbinder_rs.dylib.so"};
    std::string reason;auto images=sdk::capture_live(names,&reason);
    if(!images){std::fprintf(stderr,"SDK capture declined: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& image:*images){auto value=sdk::Image::parse(image.file,image.base);if(!value)return 3;parsed.push_back(std::move(*value));}
    const sdk::Graph graph(std::move(parsed));auto monitor=home_native_input::resolve_monitor(graph);
    auto contract=home_native_input::pilfer_contract(graph);if(!monitor||!contract||!sdk::images_current(*images))return 4;
    if(!home_native_platform::callbacks(reason)){std::fprintf(stderr,"Binder pool declined: %s\n",reason.c_str());return 5;}
    const auto address=[&](sdk::Address at){return (*images)[at.image].base+at.va;};
    const auto create=reinterpret_cast<Pair(*)(const char*,size_t,int32_t)>(address(monitor->create));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(monitor->drop));
    constexpr std::string_view name="HC-Pilfer-ABI-Probe";
    const auto value=create(name.data(),name.size(),0);
    if(value.status||!value.pointer){std::fprintf(stderr,"Monitor factory declined, status=%llu\n",(unsigned long long)value.status);return 6;}
    auto* owned=reinterpret_cast<void*>(value.pointer);int exit=0;
    {
        auto binding=home_native_input::CheckedPilfer::bind(graph,*images,*contract,owned);
        if(!binding){std::fprintf(stderr,"Live host/table/target declined\n");exit=7;}
        else {
            auto result=binding->claim();
            std::printf("Actual checked pilfer accepted=%d exception=%d status=%d, no receiver or active test gesture\n",result.accepted,result.exception,result.status);
            if(!result.accepted)exit=8;
        }
    }
    drop(owned);return exit;
}
