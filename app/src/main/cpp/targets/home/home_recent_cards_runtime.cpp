/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_recent_cards_runtime.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_recent_cards_live_image.h"
#include "home_recent_cards_reply.h"
#include "home_recent_cards_listeners.h"
#include "home_layout_config.h"
#include "home_local_config_channel.h"
#include "nativehook/got_hook_backend.h"
#include "nativehook/android_diagnostics.h"
#include <android/log.h>
#include <atomic>
#include <sys/system_properties.h>
#include <time.h>

namespace {
namespace sdk=home_layout::cards::sdk;
using home_layout::cards::Snapshot;
using Query=sdk::Reply(*)(void*,int32_t,int32_t,int32_t);
using NewBinder=AIBinder*(*)(const AIBinder_Class*,void*);
constexpr char kTag[]="HyperCeiler.HomeLayout";
constexpr char kOwner[]="debug.hyperceiler.home_cards_owner";
struct Owner {
    std::array<char,PROP_VALUE_MAX> text{};
    bool native()const{return std::string_view(text.data()).starts_with("native:");}
    bool operator==(const Owner&)const=default;
};
Owner owner(){Owner out;(void)__system_property_get(kOwner,out.text.data());return out;}
int64_t now(){timespec value{};clock_gettime(CLOCK_BOOTTIME,&value);return int64_t(value.tv_sec)*1000+value.tv_nsec/1000000;}
struct Binding {
    sdk::TaskContract task;
    sdk::GroupContract group;
    sdk::QueryContract query;
    uintptr_t table=0;
    Query original=nullptr;
    void(*drop)(void*)=nullptr;
};
std::optional<Binding> storage;
std::atomic<const Binding*> binding{nullptr};
std::atomic<NewBinder> original_new{nullptr};
std::atomic_bool overriding{false},listener_ready{false};
std::atomic<uint64_t> captured{0},calls{0},executed{0},removed{0},declined{0};
home_layout::cards::Listeners listeners;
std::atomic_flag setup=ATOMIC_FLAG_INIT;
std::vector<nhk::GotHook<>> listener_hooks,query_hooks;
std::vector<sdk::LiveImage> listener_images,query_images;
bool listener_failed=false,query_failed=false;
int64_t query_retry=0,last_identity=0;
unsigned pending_attempts=0;
Owner last_owner;
Snapshot last_selection;
uint64_t last_capture=0,last_reported_calls=0;
int64_t last_report=0;
unsigned reports=0;
bool observed=false;
struct Unlock {~Unlock(){setup.clear(std::memory_order_release);}};
bool read_pointer(uintptr_t address,const void*& value){
    return sdk::read_live(address,std::as_writable_bytes(std::span(&value,1)));
}
void discard_files(std::vector<sdk::LiveImage>& images){
    for(auto& image:images){std::vector<std::byte> empty;image.file.swap(empty);}
}
bool preflight(const nhk::elf::ElfImage& image,std::string_view symbol,const void* expected){
    const auto slots=image.collect_slots(symbol);
    if(slots.empty()||slots.size()>nhk::GotHook<>::kSlotCapacity)return false;
    for(const auto& slot:slots){const void* value=nullptr;if(!read_pointer(image.runtime(slot.rva),value)||value!=expected)return false;}
    return true;
}
bool maintain(std::vector<nhk::GotHook<>>& hooks){
    if(hooks.empty())return false;
    for(auto& hook:hooks){
        if(hook.partial)return false;
        const auto health=hook.health(read_pointer);
        if(health==nhk::GotHook<>::State::healthy)continue;
        if(health!=nhk::GotHook<>::State::rearmable)return false;
        bool clean=true;
        const bool restored=nhk::write_all_or_rollback(std::span(hook.slots,hook.slot_count),hook.replacement,hook.expected,
            [&](uintptr_t address,void* value){
                const auto protection=hook.captured_protection(address);
                return protection&&nhk::perform_pointer_write(address,value,*protection).ok();
            },&clean);
        if(!restored){hook.partial=!clean;return false;}
    }
    return true;
}
AIBinder* new_binder(const AIBinder_Class* klass,void* args){
    auto* result=original_new.load(std::memory_order_acquire)(klass,args);
    if(listeners.capture(result))captured.fetch_add(1,std::memory_order_relaxed);
    return result;
}
sdk::Reply query_recent(void* receiver,int32_t maximum,int32_t flags,int32_t user){
    const auto* current=binding.load(std::memory_order_acquire);
    const auto captured_owner=owner();
    const auto selected=captured_owner.native()&&overriding.load(std::memory_order_acquire)
        ?home_layout::local_recent_cards():Snapshot{};
    auto reply=current->original(receiver,maximum,flags,user);
    calls.fetch_add(1,std::memory_order_relaxed);
    if(!selected||selected->packages.empty()||owner()!=captured_owner
        ||!overriding.load(std::memory_order_acquire))return reply;
    const auto result=sdk::filter_reply(reply,current->query,current->group,current->task,current->table,selected,
        sdk::read_live,[&](auto owned,const auto& plan,auto drop,size_t& kept){
            return owner()==captured_owner&&overriding.load(std::memory_order_acquire)
                &&home_layout::commit_local_recent_cards(owned,plan,drop,kept);
        },current->drop);
    if(result){executed.fetch_add(1,std::memory_order_relaxed);removed.fetch_add(*result,std::memory_order_relaxed);}
    else declined.fetch_add(1,std::memory_order_relaxed);
    return reply;
}
void install_listener(){
    if(listener_failed||listener_ready.load(std::memory_order_acquire))return;
    constexpr std::array<std::string_view,1> names={"libhyper_os_ui_private.dylib.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live){
        // Missing dependencies are normal in the early loader callback. Do not
        // back off past the later libapp load and miss listener construction.
        if(reason.ends_with(": not loaded"))return;
        listener_failed=true;
        __android_log_print(ANDROID_LOG_WARN,kTag,"native recent listener snapshot declined: %s",reason.c_str());return;
    }
    auto image=sdk::Image::parse((*live)[0].file,(*live)[0].base);
    if(!image||!sdk::images_current(*live)||!preflight(image->metadata(),"AIBinder_new",reinterpret_cast<void*>(&AIBinder_new))){
        listener_failed=true;
        __android_log_print(ANDROID_LOG_WARN,kTag,"native recent listener binding declined, SDK metadata or original GOT ownership");return;
    }
    original_new.store(&AIBinder_new,std::memory_order_release); // Publish before exposing any replacement slot.
    const std::array<nhk::GotHookRequest,1> request={nhk::GotHookRequest{"AIBinder_new",reinterpret_cast<void*>(&new_binder)}};
    bool clean=true;std::vector<nhk::GotHook<>> residual;
    const bool installed=nhk::install_got_hooks(image->metadata(),request,listener_hooks,read_pointer,true,&clean,{},&residual);
    if(!installed){listener_hooks.insert(listener_hooks.end(),residual.begin(),residual.end());listener_failed=true;}
    discard_files(*live);listener_images=std::move(*live);
    listener_ready.store(installed,std::memory_order_release);
    __android_log_print(installed?ANDROID_LOG_INFO:ANDROID_LOG_WARN,kTag,
        "native recent listener GOT ready=%d rollback_clean=%d, weak Binder references only",installed,clean);
}
void install_query(){
    if(query_failed||binding.load(std::memory_order_acquire)||now()<query_retry)return;
    constexpr std::array<std::string_view,7> names={"libhyper_os_ui_public.so","libhyper_os_aidl.dylib.so",
        "libhyper_os_ui_private.dylib.so","libhyper_os_schema_public.so","libbinder_rs.dylib.so","libstd.dylib.so","libapp_launcher.so"};
    std::string reason;auto live=sdk::capture_live(names,&reason);
    if(!live){
        if(!reason.ends_with(": not loaded")){
            query_failed=true;__android_log_print(ANDROID_LOG_WARN,kTag,"native recent SDK snapshot refused: %s, no repeated attempts",reason.c_str());return;
        }
        const std::array<int64_t,4> backoff={500,2000,10000,30000};
        query_retry=now()+backoff[std::min<size_t>(pending_attempts++,backoff.size()-1)];
        if(pending_attempts==1)__android_log_print(ANDROID_LOG_INFO,kTag,"native recent query waiting for SDK: %s",reason.c_str());
        return;
    }
    std::vector<sdk::Image> images;
    for(size_t i=0;i<6;++i){
        auto image=sdk::Image::parse((*live)[i].file,(*live)[i].base);
        if(!image){query_failed=true;__android_log_print(ANDROID_LOG_WARN,kTag,"native recent SDK ELF declined: %s",names[i].data());return;}
        images.push_back(std::move(*image));std::vector<std::byte> empty;(*live)[i].file.swap(empty);
    }
    auto caller=sdk::Image::parse(live->back().file,live->back().base);
    sdk::Graph graph(std::move(images));std::string_view stage;
    const auto task=sdk::resolve_task(graph,&stage);
    const auto group=task?sdk::resolve_group(graph,*task,&stage):std::nullopt;
    const auto query=group?sdk::resolve_query(graph,*group,&stage):std::nullopt;
    if(!task||!group||!query||!caller||!sdk::images_current(*live)){
        query_failed=true;
        __android_log_print(ANDROID_LOG_WARN,kTag,"native recent SDK contract declined at %.*s, no repeated hook attempts",int(stage.size()),stage.data());
        nhk::android_diagnostics::emit("home.recents.hide_cards","最近任务隐藏卡片","NATIVE_GOT","FAILED","VALIDATION","SDK_CONTRACT","sdk",stage.data());
        return;
    }
    Binding next{*task,*group,*query,(*live)[query->table.image].base+query->table.va,
        reinterpret_cast<Query>((*live)[query->query.image].base+query->query.va),
        reinterpret_cast<void(*)(void*)>((*live)[group->drop.image].base+group->drop.va)};
    if(!preflight(caller->metadata(),"IRecentTasks_get_recent_tasks_api36",reinterpret_cast<void*>(next.original))){
        query_failed=true;__android_log_print(ANDROID_LOG_WARN,kTag,"native recent query GOT original ownership declined");return;
    }
    storage=std::move(next);binding.store(&*storage,std::memory_order_release);
    const std::array<nhk::GotHookRequest,1> request={nhk::GotHookRequest{"IRecentTasks_get_recent_tasks_api36",reinterpret_cast<void*>(&query_recent)}};
    bool clean=true;std::vector<nhk::GotHook<>> residual;
    const bool installed=nhk::install_got_hooks(caller->metadata(),request,query_hooks,read_pointer,true,&clean,{},&residual);
    if(!installed){query_hooks.insert(query_hooks.end(),residual.begin(),residual.end());query_failed=true;}
    discard_files(*live);query_images=std::move(*live);
    overriding.store(installed,std::memory_order_release);
    __android_log_print(installed?ANDROID_LOG_INFO:ANDROID_LOG_WARN,kTag,
        "native recent query GOT ready=%d rollback_clean=%d, capacity and destructor table retained",installed,clean);
    if(installed)nhk::android_diagnostics::emit("home.recents.hide_cards","最近任务隐藏卡片","NATIVE_GOT","INSTALLED","INSTALL_HEALTH","INSTALLED","sdk","Current in-memory SDK producer, consumer and allocator contracts validated");
}
}
void hc_recent_cards_prime(){
    if(setup.test_and_set(std::memory_order_acquire))return;Unlock unlock;
    install_listener();
}
void hc_recent_cards_refresh(){
    if(setup.test_and_set(std::memory_order_acquire))return;Unlock unlock;
    install_listener();
    if(home_local_config::requested())install_query();
    if(!binding.load(std::memory_order_acquire)){home_local_config::set_cards_ready(false);return;}
    if(!query_failed){
        const bool healthy=maintain(query_hooks)&&maintain(listener_hooks);
        overriding.store(healthy,std::memory_order_release);
        if(!healthy){query_failed=true;__android_log_print(ANDROID_LOG_WARN,kTag,"native recent GOT health declined, forwarding stock and stopping automatic writes");}
    }
    if(now()-last_identity>=30000){
        last_identity=now();
        if(!sdk::images_current(query_images)||!sdk::images_current(listener_images)){
            overriding.store(false,std::memory_order_release);query_failed=true;
            __android_log_print(ANDROID_LOG_WARN,kTag,"native recent SDK generation changed, stock retained until launcher reload");
        }
    }
    home_local_config::set_cards_ready(home_local_config::requested()&&hc_recent_cards_status()==15);
    const auto current_owner=owner();Snapshot selected;
    if(current_owner.native()&&!home_layout::inspect_local_recent_cards(selected))return;
    const uint64_t current_capture=captured.load(std::memory_order_relaxed);
    const bool selection_changed=bool(selected)!=bool(last_selection)
        ||(selected&&last_selection&&selected->packages!=last_selection->packages);
    if((observed&&last_owner!=current_owner)||(current_owner.native()&&(selection_changed||current_capture!=last_capture))){
        const auto report=listeners.notify();
        __android_log_print(ANDROID_LOG_INFO,kTag,"native recent list refresh live=%zu delivered=%zu failed=%zu owner=%s",
            report.live,report.delivered,report.failed,current_owner.native()?"launcher":"legacy");
    }
    last_owner=current_owner;last_selection=selected;last_capture=current_capture;observed=true;
    const auto total=calls.load(std::memory_order_relaxed);
    if(total!=last_reported_calls&&(reports<8||now()-last_report>=30000)){
        ++reports;last_report=now();
        last_reported_calls=total;
        __android_log_print(ANDROID_LOG_INFO,kTag,"native recent query calls=%llu executed=%llu removed=%llu declined=%llu listeners=%llu",
            static_cast<unsigned long long>(total),static_cast<unsigned long long>(executed.load()),
            static_cast<unsigned long long>(removed.load()),static_cast<unsigned long long>(declined.load()),
            static_cast<unsigned long long>(current_capture));
        nhk::android_diagnostics::counter("home.recents.hide_cards","最近任务隐藏卡片",executed.load(),removed.load(),"NATIVE_GOT","sdk");
    }
}
uint32_t hc_recent_cards_status(){
    return (listener_ready.load(std::memory_order_acquire)?1u:0u)
        |(binding.load(std::memory_order_acquire)?2u:0u)
        |(captured.load(std::memory_order_acquire)?4u:0u)
        |(overriding.load(std::memory_order_acquire)?8u:0u);
}
#else
void hc_recent_cards_prime(){}
void hc_recent_cards_refresh(){}
uint32_t hc_recent_cards_status(){return 0;}
#endif
