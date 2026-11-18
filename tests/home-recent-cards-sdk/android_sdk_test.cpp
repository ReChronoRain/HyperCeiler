/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_cards_live_image.h"
#include "targets/home/home_recent_cards_query_contract.h"
#include "targets/home/home_recent_cards_group_access.h"
#include "targets/home/home_recent_cards_listeners.h"
#include <atomic>
#include <cassert>
#include <cstdio>
#include <dlfcn.h>
using namespace home_layout::cards::sdk;
struct Counter {std::atomic_uint calls{0},destroyed{0};home_layout::cards::Listeners* registry=nullptr;};
static void* created(void* args){return args;}
static void destroyed(void* args){++static_cast<Counter*>(args)->destroyed;}
static binder_status_t changed(AIBinder* binder,transaction_code_t code,const AParcel* input,AParcel*) {
    if(code!=FIRST_CALL_TRANSACTION||AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
    auto& counter=*static_cast<Counter*>(AIBinder_getUserData(binder));++counter.calls;
    assert(counter.registry->capture(binder)); // Callback reentry must not hold the registry lock.
    return STATUS_OK;
}
int main() {
    auto* loaded=dlopen("/system_ext/lib64/libhyper_os_ui_public.so",RTLD_NOW|RTLD_LOCAL);
    if(!loaded){std::fprintf(stderr,"SDK load declined: %s\n",dlerror());return 10;}
    auto* schema=dlopen("/system_ext/lib64/libhyper_os_schema_public.so",RTLD_NOW|RTLD_LOCAL);
    if(!schema){std::fprintf(stderr,"schema load declined: %s\n",dlerror());return 10;}
    const std::array<std::string_view,6> names={"libhyper_os_ui_public.so","libhyper_os_aidl.dylib.so",
        "libhyper_os_ui_private.dylib.so","libhyper_os_schema_public.so","libbinder_rs.dylib.so","libstd.dylib.so"};
    std::string capture_reason;auto live=capture_live(names,&capture_reason);
    if(!live){std::fprintf(stderr,"live capture declined: %s\n",capture_reason.c_str());return 11;}
    std::vector<Image> images;
    for(const auto& entry:*live) {
        auto image=Image::parse(entry.file,entry.base);
        if(!image){std::fprintf(stderr,"live parse declined: %s\n",entry.path.c_str());return 12;}
        images.push_back(std::move(*image));
    }
    Graph graph(std::move(images));std::string_view reason;
    const auto task=resolve_task(graph,&reason);
    if(!task){std::fprintf(stderr,"live TaskInfo declined: %.*s\n",int(reason.size()),reason.data());return 13;}
    const auto group=resolve_group(graph,*task,&reason);
    if(!group){std::fprintf(stderr,"live group declined: %.*s\n",int(reason.size()),reason.data());return 14;}
    const auto query=resolve_query(graph,*group,&reason);
    if(!query){std::fprintf(stderr,"live query declined: %.*s\n",int(reason.size()),reason.data());return 15;}
    const auto at=[&](Address address){return (*live)[address.image].base+address.va;};
    const auto make=graph.exported("GroupedTaskInfo_default"),get_type=graph.exported("GroupedTaskInfo_get_type");
    assert(make&&get_type);
    const auto create=reinterpret_cast<void*(*)()>(at(*make));
    const auto type=reinterpret_cast<int32_t(*)(const void*)>(at(*get_type));
    const auto drop=reinterpret_cast<void(*)(void*)>(at(group->drop));
    void* box=create();assert(box);
    assert(type(box)==group->mixed_type);
    uint64_t capacity=0;
    assert(read_live(reinterpret_cast<uintptr_t>(box)+group->tasks.capacity,std::as_writable_bytes(std::span(&capacity,1))));
    assert(capacity==group->tasks.none);
    assert(!visit_group(*group,*task,reinterpret_cast<uintptr_t>(box),read_live,[](const auto&){assert(false);return true;}));
    drop(box);
    assert(images_current(*live));
    Counter counter;home_layout::cards::Listeners listeners;counter.registry=&listeners;
    auto* klass=AIBinder_Class_define("com.android.wm.shell.recents.IRecentTasksListener",created,destroyed,changed);assert(klass);
    auto* binder=AIBinder_new(klass,&counter);assert(binder&&listeners.capture(binder)&&listeners.capture(binder));
    auto report=listeners.notify();assert(report.live==1&&report.delivered==1&&report.failed==0&&counter.calls==1);
    AIBinder_decStrong(binder);assert(counter.destroyed==1);
    report=listeners.notify();assert(!report.live&&!report.delivered&&counter.calls==1);
    std::puts("PASS live relocated SDK TaskInfo/group/query contracts, actual tagged heap read, native group allocator and destructor");
    std::puts("PASS actual Binder listener notification, interface token/no-payload protocol, weak expiry, deduplication and callback reentry");
    dlclose(schema);dlclose(loaded);
}
