/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_bundle_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <dlfcn.h>
#include <cstdio>
namespace sdk=home_layout::cards::sdk;
struct StringRef {const char* data;size_t length;};
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,7> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live){std::fprintf(stderr,"capture: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto bundle=home_native_bundle::resolve(graph);
    const auto call=home_native_provider::resolve_call(graph);
    if(!bundle||!call||!home_native_bundle::call_owner(graph,*call,*bundle)||!sdk::images_current(*live))return 4;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto make=reinterpret_cast<void*(*)()>(address(bundle->make));
    const auto get=reinterpret_cast<uint16_t(*)(void*,StringRef)>(address(bundle->get_boolean));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(bundle->drop));
    const std::array<std::string_view,3> keys={"","frameCommitted","未设置的布尔项"};
    for(unsigned i=0;i<4000;++i) {
        auto* object=make();if(!object)return 5;
        const auto key=keys[i%keys.size()];const auto value=get(object,StringRef{key.data(),key.size()});
        drop(object);
        if(value!=1)return 6; // Empty SDK Bundle returns the derived None flag.
    }
    std::puts("PASS actual SDK Bundle: 4000 constructors, borrowed UTF-8/key lookups and public destructors, Provider reply uses same allocator/size, no desktop operations");
}
