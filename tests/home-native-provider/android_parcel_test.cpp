/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_parcel_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <android/binder_parcel.h>
#include <dlfcn.h>
#include <cstdio>
#include <cassert>
namespace sdk=home_layout::cards::sdk;
struct StringRef {const char* data;size_t length;};
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_GLOBAL)
        ||!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL))return 1;
    constexpr std::array<std::string_view,8> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
        "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);if(!live)return 2;
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto bundle=home_native_bundle::resolve(graph);
    if(!bundle)return 4;const auto contract=home_native_parcel::resolve(graph,*bundle);if(!contract)return 5;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    const auto from=reinterpret_cast<void*(*)(const void*)>(address(contract->bundle_from));
    const auto get=reinterpret_cast<uint16_t(*)(void*,StringRef)>(address(bundle->get_boolean));
    const auto drop=reinterpret_cast<void(*)(void*)>(address(bundle->drop));
    {
        AParcel* empty=AParcel_create();assert(empty);
        assert(AParcel_writeInt32(empty,0)==STATUS_OK&&AParcel_setDataPosition(empty,0)==STATUS_OK);
        alignas(8) std::array<std::byte,32> reference{};std::memcpy(reference.data()+contract->raw_field,&empty,sizeof(empty));
        auto* object=from(reference.data());
        if(!object){std::fprintf(stderr,"SDK declined even empty native Parcel\n");AParcel_delete(empty);return 8;}
        drop(object);AParcel_delete(empty);std::puts("Empty native Parcel ABI PASS");
    }
    // Android Bundle/Parcel wire constants, defined by BaseBundle and
    // frameworks/native/libs/binder/ParcelValTypes.h, not vendor object offsets.
    // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/BaseBundle.java
    constexpr int32_t bundle_magic=0x4c444e42,value_boolean=9;
    const std::array<std::string_view,3> keys={"frameCommitted","布尔测试",""};
    for(unsigned i=0;i<4000;++i) {
        AParcel* parcel=AParcel_create();assert(parcel);
        assert(AParcel_writeInt32(parcel,-1)==STATUS_OK&&AParcel_writeInt32(parcel,bundle_magic)==STATUS_OK);
        const auto start=AParcel_getDataPosition(parcel);const auto key=keys[i%keys.size()];const bool expected=i%2;
        assert(AParcel_writeInt32(parcel,1)==STATUS_OK);
        assert(AParcel_writeString(parcel,key.data(),static_cast<int32_t>(key.size()))==STATUS_OK);
        assert(AParcel_writeInt32(parcel,value_boolean)==STATUS_OK&&AParcel_writeInt32(parcel,expected?1:0)==STATUS_OK);
        const auto end=AParcel_getDataPosition(parcel);
        assert(AParcel_setDataPosition(parcel,0)==STATUS_OK&&AParcel_writeInt32(parcel,end-start)==STATUS_OK
            &&AParcel_setDataPosition(parcel,end)==STATUS_OK);
        // Current framework BaseBundle and the SDK both consume this after the
        // length-delimited payload. Our typed primitive packet contains no Intent.
        assert(contract->has_intent_trailer&&AParcel_writeBool(parcel,false)==STATUS_OK
            &&AParcel_setDataPosition(parcel,0)==STATUS_OK);
        alignas(8) std::array<std::byte,32> borrowed{};
        std::memcpy(borrowed.data()+contract->raw_field,&parcel,sizeof(parcel));
        void* object=from(borrowed.data());
        if(!object) {
            const auto failed_at=AParcel_getDataPosition(parcel);
            AParcel_setDataPosition(parcel,0);int32_t length=0,magic=0,count=0;
            AParcel_readInt32(parcel,&length);AParcel_readInt32(parcel,&magic);AParcel_readInt32(parcel,&count);
            std::vector<uint8_t> raw(AParcel_getDataSize(parcel));
            if(AParcel_marshal(parcel,raw.data(),0,raw.size())==STATUS_OK){std::fprintf(stderr,"NDK_BUNDLE_HEX=");for(auto byte:raw)std::fprintf(stderr,"%02x",byte);std::fprintf(stderr,"\n");}
            std::fprintf(stderr,"SDK declined populated Bundle i=%u bytes=%d magic=%#x entries=%d native_size=%d failed_at=%d\n",i,length,magic,count,AParcel_getDataSize(parcel),failed_at);
            AParcel_delete(parcel);return 7;
        }
        const auto value=get(object,StringRef{key.data(),key.size()});
        drop(object);AParcel_delete(parcel); // Keep both borrow and native producer alive through the SDK object.
        if(value!=uint16_t(expected?1u<<bundle->boolean_shift:0u))return 6;
    }
    std::puts("PASS 4000 NDK Parcel -> SDK Bundle -> true/false UTF-8 lookups and SDK/native destruction, no desktop operations");
}
