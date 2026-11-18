/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_native_provider_contract.h"
#include "targets/home/home_recent_cards_live_image.h"
#include <android/binder_status.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
namespace sdk=home_layout::cards::sdk;
struct ErrorWords {std::array<uint64_t,5> words{};};
int main() {
    if(!dlopen("/system_ext/lib64/libhyper_os_core_data_public.so",RTLD_NOW|RTLD_GLOBAL)) {
        std::fprintf(stderr,"SDK load declined: %s\n",dlerror());return 1;
    }
    constexpr std::array<std::string_view,6> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
        "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live){std::fprintf(stderr,"SDK capture declined: %s\n",reason.c_str());return 2;}
    std::vector<sdk::Image> parsed;
    for(const auto& file:*live) {
        auto image=sdk::Image::parse(file.file,file.base);if(!image)return 3;
        parsed.push_back(std::move(*image));
    }
    const sdk::Graph graph(std::move(parsed));const auto contract=home_native_provider::acquire_error(graph);
    if(!contract||contract->size!=sizeof(ErrorWords)||!sdk::images_current(*live))return 4;
    const auto address=[&](sdk::Address at){return (*live)[at.image].base+at.va;};
    uintptr_t target=0;
    if(!sdk::read_live(address(contract->table_slot),std::as_writable_bytes(std::span(&target,1)))
        ||target!=address(contract->drop))return 5;
    const auto make=reinterpret_cast<ErrorWords(*)(AStatus*)>(address(contract->constructor));
    const auto drop=reinterpret_cast<void(*)(void*)>(target);
    // The resolver proved the one-word native Status input is consumed through
    // AStatus_delete. These are new private test objects, never desktop state.
    const std::array<std::string,4> messages={"","Provider recovery probe","原生接口所有权检查",std::string(4096,'x')};
    for(unsigned iteration=0;iteration<4000;++iteration) {
        AStatus* status=AStatus_fromExceptionCodeWithMessage(EX_ILLEGAL_ARGUMENT,messages[iteration%messages.size()].c_str());
        if(!status)return 6;
        auto error=make(status); // SDK consumes status; no second AStatus_delete.
        int32_t code=0;const auto data=std::as_writable_bytes(std::span(error.words));
        std::memcpy(&code,data.data()+contract->code,4);
        uintptr_t table=0;std::memcpy(&table,data.data()+contract->vector+contract->table_field,8);
        if(code!=EX_ILLEGAL_ARGUMENT||table!=address(contract->table))return 7;
        drop(data.data()+contract->vector);
    }
    std::puts("PASS actual relocated SDK error ABI: 4000 native Status consumptions and SDK-owned byte-vector destructors, no desktop/settings changes");
}
