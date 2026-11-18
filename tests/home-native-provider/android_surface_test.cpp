/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_parcelable_contract.h"
#include "targets/home/home_native_surface_contract.h"
#include "targets/home/home_native_surface_access.h"
#include "targets/home/home_native_parcelable_reader.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <android/binder_parcel.h>
#include <dlfcn.h>
#include <cstdio>
namespace sdk=home_layout::cards::sdk;
struct StringRef {const char* data;size_t length;};
int main(int argc,char** argv) {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_ui_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,11> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so","libhyper_os_ui_public.so",
        "libhyper_os_ui_private.dylib.so","libhyper_os_schema_private.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live){std::fprintf(stderr,"capture: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto surface=home_native_surface::resolve(graph);
    const auto parcelable=home_native_parcelable::resolve(graph);const auto bundle=home_native_bundle::resolve(graph);
    if(!surface||!parcelable||!bundle||!sdk::images_current(*live))return 4;
    const auto access=home_native_surface::resolve_access(graph,*surface);if(!access)return 18;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto make=reinterpret_cast<void*(*)()>(address(surface->make));
    const auto valid=reinterpret_cast<bool(*)(void*)>(address(surface->valid));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(surface->drop));
    const auto read=reinterpret_cast<uint64_t(*)(void* const*,AParcel* const*)>(address(surface->read));
    const auto make_bundle=reinterpret_cast<void*(*)()>(address(bundle->make));
    const auto get=reinterpret_cast<AParcel*(*)(void*,StringRef)>(address(parcelable->get));
    const auto drop_bundle=reinterpret_cast<void(*)(void*)>(address(bundle->drop));
    const auto as_native=reinterpret_cast<void*(*)(void*)>(address(access->native));
    struct MainResult {uint64_t status;void* object;};static_assert(sizeof(MainResult)==16);
    const auto main_control=reinterpret_cast<MainResult(*)()>(address(access->main));
    const auto main=main_control();
    if(uint32_t(main.status)==0) {
        if(!main.object)return 19;drop(main.object);
    }
    std::printf("Independent main-layer status=%#llx, no window created\n",static_cast<unsigned long long>(main.status));
    if(argc==2) {
        const std::string_view hex=argv[1];std::vector<uint8_t> bytes;
        for(size_t i=0;i+1<hex.size();i+=2)bytes.push_back(static_cast<uint8_t>(std::stoul(std::string(hex.substr(i,2)),nullptr,16)));
        AParcel* parcel=AParcel_create();if(!parcel)return 10;
        if(AParcel_unmarshal(parcel,bytes.data(),bytes.size())!=STATUS_OK||AParcel_setDataPosition(parcel,0)!=STATUS_OK)return 11;
        const auto input=home_native_parcel::resolve(graph,*bundle);if(!input)return 12;
        const auto from=reinterpret_cast<void*(*)(AParcel* const*)>(address(input->bundle_from));
        void* object=from(&parcel);if(!object)return 13;
        constexpr std::string_view field="parcelable";
        for(unsigned iteration=0;iteration<4000;++iteration) {
            AParcel* payload=get(object,StringRef{field.data(),field.size()});if(!payload)return 14;
            if(AParcel_getDataPosition(payload)!=AParcel_getDataSize(payload)
                ||home_native_parcelable::read_payload(payload,"android.graphics.Roct")
                ||home_native_parcelable::read_payload(payload,"android.view.SurfaceControl")
                ||!home_native_parcelable::read_payload(payload,"android.graphics.Rect"))return 15;
            for(unsigned i=0;i<4;++i){int32_t n=0;if(AParcel_readInt32(payload,&n)!=STATUS_OK||n!=int32_t(11*(i+1)))return 16;}
            if(AParcel_getDataPosition(payload)!=AParcel_getDataSize(payload))return 17;
            AParcel_delete(payload);
        }
        drop_bundle(object);AParcel_delete(parcel);
        std::puts("PASS 4000 real Java Rect -> SDK AnyParcelable -> owned Parcel, rewind/descriptor/body and wrong-type refusal");return 0;
    }
    constexpr std::string_view key="missingSurfaceControl";
    for(unsigned i=0;i<4000;++i) {
        void* object=make();if(!object)return 5;
        if(valid(object)||as_native(object)!=nullptr){drop(object);return 6;}
        void* container=make_bundle();if(!container){drop(object);return 7;}
        AParcel* empty=get(container,StringRef{key.data(),key.size()});
        if(!empty){drop_bundle(container);drop(object);return 8;}
        const bool empty_payload=AParcel_getDataSize(empty)==0;
        const auto status=read(&object,&empty);
        const bool invalid=!valid(object);
        AParcel_delete(empty);drop_bundle(container);drop(object);
        if(!empty_payload||!invalid||(status&surface->error_tag)!=surface->error_tag)return 9;
    }
    std::puts("PASS 4000 real SDK SurfaceControl default/is_valid/destruct, missing Parcelable owned AParcel, empty read error and separate reference ABI; desktop untouched");
}
