/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_display_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <dlfcn.h>
#include <cmath>
#include <cstdio>
namespace sdk=home_layout::cards::sdk;
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_ui_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_app_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,13> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so","libhyper_os_ui_public.so",
        "libhyper_os_ui_private.dylib.so","libhyper_os_schema_private.dylib.so",
        "libhyper_os_app_public.so","libhyper_os_app_private.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live){std::fprintf(stderr,"capture: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto contract=home_native_display::resolve(graph);
    if(!contract||!sdk::images_current(*live))return 4;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto make_at=graph.exported("DisplayMetrics_default");if(!make_at)return 5;
    const auto maker=graph.body(*make_at);if(!maker)return 6;
    const auto paths=home_native_provider::return_paths(graph,*maker);if(!paths||paths->size()!=1)return 7;
    const auto created=sdk::trace(graph,*maker,false,false,paths->front());
    if(!created.returned||!created.unknown.empty()||created.regs[0].kind!=sdk::Value::Returned
        ||created.regs[0].number>=created.calls.size())return 8;
    const auto& allocation=created.calls[created.regs[0].number];
    if(allocation.receiver!=sdk::Value{sdk::Value::Constant,0,contract->bytes}
        ||allocation.parcel!=sdk::Value{sdk::Value::Constant,0,contract->alignment})return 9;
    const auto make=reinterpret_cast<void*(*)()>(address(*make_at));
    const auto width=reinterpret_cast<int32_t(*)(void*)>(address(contract->width));
    const auto height=reinterpret_cast<int32_t(*)(void*)>(address(contract->height));
    const auto density=reinterpret_cast<float(*)(void*)>(address(contract->density));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(contract->drop_metrics));
    for(unsigned i=0;i<4000;++i) {
        void* values=make();if(!values)return 10;
        const auto w=width(values),h=height(values);const auto d=density(values);
        drop(values);if(w<=0||h<=0||!std::isfinite(d)||d<=0)return 11;
    }
    std::puts("PASS 4000 actual SDK default DisplayMetrics constructor/typed getters/drop; no global Display or live configuration calls, not device geometry acceptance");
}
