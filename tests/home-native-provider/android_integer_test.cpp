/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_integer_contract.h"
#include "targets/home/home_native_parcel_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <android/binder_parcel.h>
#include <bit>
#include <cstdio>
#include <dlfcn.h>
namespace sdk=home_layout::cards::sdk;
struct StringRef {const char* data;size_t length;};
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,8> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live){std::fprintf(stderr,"capture: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live){auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));}
    const sdk::Graph graph(std::move(parsed));const auto bundle=home_native_bundle::resolve(graph);
    const auto integer=home_native_bundle::resolve_integer(graph);if(!bundle||!integer)return 4;
    const auto parcel_contract=home_native_parcel::resolve(graph,*bundle);if(!parcel_contract||!sdk::images_current(*live))return 5;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto from=reinterpret_cast<void*(*)(AParcel* const*)>(address(parcel_contract->bundle_from));
    const auto get=reinterpret_cast<uint64_t(*)(void*,StringRef)>(address(integer->get));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(bundle->drop));
    constexpr std::string_view key="operation",missing="absent";
    for(unsigned i=0;i<4000;++i) {
        AParcel* parcel=AParcel_create();if(!parcel)return 6;
        const int32_t value=std::bit_cast<int32_t>(i*0x9e3779b9u),kind=i%4==0?9:i%4==1?7:1;
        if(AParcel_writeInt32(parcel,-1)!=STATUS_OK||AParcel_writeInt32(parcel,0x4c444e42)!=STATUS_OK)return 7;
        const auto start=AParcel_getDataPosition(parcel);
        if(AParcel_writeInt32(parcel,1)!=STATUS_OK||AParcel_writeString(parcel,key.data(),key.size())!=STATUS_OK
            ||AParcel_writeInt32(parcel,kind)!=STATUS_OK
            ||(kind==7?AParcel_writeFloat(parcel,1.25f):AParcel_writeInt32(parcel,kind==9?(value&1):value))!=STATUS_OK)return 8;
        const auto end=AParcel_getDataPosition(parcel);
        if(AParcel_setDataPosition(parcel,0)!=STATUS_OK||AParcel_writeInt32(parcel,end-start)!=STATUS_OK
            ||AParcel_setDataPosition(parcel,end)!=STATUS_OK||AParcel_writeBool(parcel,false)!=STATUS_OK
            ||AParcel_setDataPosition(parcel,0)!=STATUS_OK)return 9;
        void* object=from(&parcel);if(!object)return 10;AParcel_delete(parcel);
        const auto actual=get(object,{key.data(),key.size()}),absent=get(object,{missing.data(),missing.size()});
        const auto expected=kind==1?uint64_t(std::bit_cast<uint32_t>(value))<<integer->shift:1;
        if(actual!=expected||absent!=1)return 11;drop(object);
    }
    std::puts("PASS 4000 real Android typed Parcels -> owned SDK Bundle -> optional i32, signed values, wrong types, absent keys and producer release; no desktop operations");
}
