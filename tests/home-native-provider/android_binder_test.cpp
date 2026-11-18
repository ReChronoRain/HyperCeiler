/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_binder_contract.h"
#include "targets/home/home_native_parcel_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <dlfcn.h>
#include <cstdio>
namespace sdk=home_layout::cards::sdk;
struct StringRef {const char* data;size_t length;};
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,9> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so","libhyper_os_binder_private.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live){std::fprintf(stderr,"capture: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto bundle=home_native_bundle::resolve(graph);
    const auto binder=home_native_binder::resolve(graph);if(!bundle||!binder)return 4;
    const auto parcel_contract=home_native_parcel::resolve(graph,*bundle);if(!parcel_contract||!sdk::images_current(*live))return 5;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto from=reinterpret_cast<void*(*)(AParcel* const*)>(address(parcel_contract->bundle_from));
    const auto get=reinterpret_cast<AIBinder*(*)(void*,StringRef)>(address(binder->get));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(bundle->drop));
    const auto* type=AIBinder_Class_define("com.sevtinge.hyperceiler.test.LocalOwner",[](void* value){return value;},
        [](void* value){++*static_cast<unsigned*>(value);},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
    unsigned destroyed=0;
    if(!type)return 6;AIBinder* owner=AIBinder_new(type,&destroyed);if(!owner)return 7;
    constexpr std::string_view field="lifetime",missing="absent";
    for(unsigned i=0;i<4000;++i) {
        AParcel* parcel=AParcel_create();if(!parcel)return 8;
        if(AParcel_writeInt32(parcel,-1)!=STATUS_OK||AParcel_writeInt32(parcel,0x4c444e42)!=STATUS_OK)return 9;
        const auto start=AParcel_getDataPosition(parcel);
        if(AParcel_writeInt32(parcel,1)!=STATUS_OK||AParcel_writeString(parcel,field.data(),field.size())!=STATUS_OK
            ||AParcel_writeInt32(parcel,15)!=STATUS_OK||AParcel_writeStrongBinder(parcel,owner)!=STATUS_OK)return 10;
        const auto end=AParcel_getDataPosition(parcel);
        if(AParcel_setDataPosition(parcel,0)!=STATUS_OK||AParcel_writeInt32(parcel,end-start)!=STATUS_OK
            ||AParcel_setDataPosition(parcel,end)!=STATUS_OK||AParcel_writeBool(parcel,false)!=STATUS_OK
            ||AParcel_setDataPosition(parcel,0)!=STATUS_OK)return 11;
        void* object=from(&parcel);if(!object)return 12;
        AIBinder* cloned=get(object,StringRef{field.data(),field.size()});
        AIBinder* absent=get(object,StringRef{missing.data(),missing.size()});
        if(!cloned||absent||!AIBinder_isAlive(cloned)||AIBinder_getClass(cloned)!=type)return 13;
        // Keep the getter result after both producers die, proving its own
        // strong reference rather than borrowing a Bundle/Parcel field.
        drop(object);AParcel_delete(parcel);
        if(!AIBinder_isAlive(cloned))return 14;
        AIBinder_decStrong(cloned);
    }
    AIBinder_decStrong(owner);
    if(destroyed!=1)return 15;
    std::puts("PASS 4000 real NDK Binder -> SDK Bundle -> owned NDK Binder, missing key and destruction after both producer releases; no desktop operations");
}
