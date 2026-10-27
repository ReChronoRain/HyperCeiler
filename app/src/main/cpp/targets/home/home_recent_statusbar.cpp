/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_recent_statusbar.h"
#include "home_recent_statusbar_resolver.h"
#include "nativehook/android_diagnostics.h"
#include "nativehook/hook_bank.h"
#include "nativehook/code_patch_io.h"
#include "nativehook/memory_io.h"
#include <atomic>
#include <condition_variable>
#include <chrono>
#include <mutex>
#include <fstream>
#include <set>
#include <sys/eventfd.h>
#include <time.h>

extern "C" int dock_motion_event;
namespace home_recent_bar {
namespace {
using Words=std::array<uint32_t,4>;using Slot=nhk::InlineSlot<4>;
constexpr const char* kFeature="最近任务隐藏状态栏";
std::atomic<uint64_t> published{0},hits{0};
std::atomic_bool bank_ready{false}, enabled{false};
std::mutex wake_lock;
std::condition_variable wake;
std::atomic<uint64_t> wake_revision{0};
std::array<std::atomic<uintptr_t>,3> flags{};
std::array<void*,3> originals{};
std::array<Slot,3> slots{};
std::string path;
uint64_t view_begin=0,view_end=0,last_generation=0,next_discovery=0;
unsigned discoveries=0;
bool attempted=false,retired=false;
Hook install_hook=nullptr;Unhook remove_hook=nullptr;Protect protect_hook=nullptr;

uint64_t now_ns(){timespec t{};if(clock_gettime(CLOCK_MONOTONIC,&t))return 0;return uint64_t(t.tv_sec)*1000000000+t.tv_nsec;}
void publish(unsigned value) noexcept {
    auto old=published.load(std::memory_order_relaxed);
    for(;;){if((old&15)==value&&old)return;
        const uint64_t next=(((old>>4)+1)<<4)|value;
        if(published.compare_exchange_weak(old,next,std::memory_order_release,std::memory_order_relaxed))break;
    }
    if(dock_motion_event>=0)(void)eventfd_write(dock_motion_event,1);
}
void snapshot() noexcept {
    unsigned mask=0;
    if(enabled.load(std::memory_order_acquire) && bank_ready.load(std::memory_order_acquire)){
        for(unsigned i=0;i<3;++i){
            uint8_t value=0;auto p=flags[i].load(std::memory_order_acquire);
            if(!p||!nhk::safe_read(p,std::as_writable_bytes(std::span(&value,1)))||value>1){publish(0);return;}
            mask|=unsigned(value)<<i;
        }
        if (!enabled.load(std::memory_order_acquire) || !bank_ready.load(std::memory_order_acquire)) { publish(0); return; }
        mask|=8;
    }
    publish(mask);
}
template<unsigned I> void observe(uint8_t value){
    auto original=reinterpret_cast<void(*)(uint8_t)>(__atomic_load_n(&originals[I],__ATOMIC_ACQUIRE));
    if(!original)return;
    original(value); // Preserve original state writes/logging and do not retain its receiver.
    hits.fetch_add(1,std::memory_order_relaxed);
    // Snapshot the real native globals after the original, not the callback
    // completion order. Concurrent portrait/window setters cannot publish a
    // stale argument over a newer store. No file scan, Binder, Dart or logger.
    if (enabled.load(std::memory_order_acquire)) snapshot();
}
std::optional<std::vector<nhk::ExecutableMapping>> inventory(){
    std::ifstream maps("/proc/self/maps");if(!maps)return {};
    auto all=nhk::parse_file_mappings(maps);
    auto owned=nhk::owned_image_mappings(all,std::vector<nhk::ImageContainer>{{path,view_begin,view_end}});
    if(owned.empty())return {};return owned;
}
bool stable(const Slot& slot,Words& words){
    std::array<uintptr_t,1> a{slot.address};std::array<nhk::CodeSource,1> s{slot.source};
    auto before=inventory();if(!before||nhk::mapping_state(*before,a,s,sizeof(words))!=nhk::MappingState::same)return false;
    if(!nhk::safe_read(slot.address,std::as_writable_bytes(std::span(&words,1))))return false;
    auto after=inventory();return after&&nhk::mapping_state(*after,a,s,sizeof(words))==nhk::MappingState::same;
}
nhk::InlineHookHost<4>& host(){
    static nhk::InlineHookHost<4> h=[] {
        nhk::InlineHookHost<4> v;
        v.read_slot=stable;
        v.write_slot=[](Slot& s,const Words& expected,const Words& wanted){
            nhk::CodePatchOps<4> ops{s.address,s.source};return nhk::WriteCodePatch(ops,s.address,s.source,expected,wanted,s.write_journal);};
        v.settle_write=[](Slot& s){auto& j=s.write_journal;nhk::CodePatchOps<4> ops{j.address,j.source};return nhk::RecoverCodePatch(ops,j);};
        v.hook_install=[](void* a,void* b,void** c){return install_hook?install_hook(a,b,c):-1;};
        v.hook_uninstall=[](void* a){return remove_hook?remove_hook(a):-1;};
        v.protect_range=[](uintptr_t a,size_t n){return protect_hook&&protect_hook(a,n);};
        v.on_diagnostic=[](const nhk::DiagnosticEvent& event){
            size_t i=0;for(;i<slots.size();++i)if(slots[i].address==event.address)break;
            nhk::android_diagnostics::bank("home.recents.statusbar",i,event,kFeature);};
        return v;
    }();return h;
}
bool discover(){
    std::ifstream maps("/proc/self/maps");if(!maps)return false;
    const auto all=nhk::parse_file_mappings(maps);
    std::set<std::string> candidates;
    for(const auto& m:all)if(m.path.ends_with(".apk")||m.path.ends_with("/libapp_launcher.so"))candidates.insert(m.path);
    if(candidates.size()>64)return false;
    struct Selected {std::string path;uint64_t begin,end,base;Targets target;std::vector<std::byte> bytes;std::vector<nhk::elf::ProgramSegment> segments;std::vector<nhk::ExecutableMapping> owned;};
    std::optional<Selected> found;
    for(const auto& p:candidates){
        uint64_t begin=0,size=0;
        if(p.ends_with(".apk")){auto entry=nhk::zip_stored_entry(p,"libapp_launcher.so");if(!entry)continue;begin=entry->first;size=entry->second;}
        std::ifstream file(p,std::ios::binary|std::ios::ate);if(!file)continue;
        if(!size)size=uint64_t(file.tellg());
        if(!size||size>32*1024*1024||begin>UINT64_MAX-size)continue;
        std::vector<std::byte> bytes(size);file.seekg(begin);file.read(reinterpret_cast<char*>(bytes.data()),bytes.size());
        if(size_t(file.gcount())!=bytes.size())continue;
        auto segments=nhk::elf::parse_program_segments(bytes);if(!segments)continue;
        auto view=nhk::image_view_from_segments_in_window(all,p,*segments,begin,begin+size);if(!view)continue;
        attempted=true; // A readable but unsupported image is not rescanned per pass.
        auto target=resolve(bytes);if(!target){
            nhk::android_diagnostics::emit("home.recents.statusbar",kFeature,"NATIVE_INLINE","FAILED","RESOLVE_VALIDATE","RESOLVE_FAILED","native-scene","Scene labels/bool writers/CFG not uniquely verified; stock status bar retained");return false;}
        if(found)return false;
        auto owned=nhk::owned_image_mappings(all,std::vector<nhk::ImageContainer>{{p,begin,begin+size}});
        found=Selected{p,begin,begin+size,view->load_base,*target,std::move(bytes),std::move(*segments),std::move(owned)};
    }
    if(!found)return false;
    std::array<Slot,3> prepared{};
    std::array<uintptr_t,3> prepared_flags{};
    void* replacements[]={reinterpret_cast<void*>(observe<0>),reinterpret_cast<void*>(observe<1>),reinterpret_cast<void*>(observe<2>)};
    for(unsigned i=0;i<3;++i){
        const auto& t=found->target.values[i];if(found->base>UINTPTR_MAX-t.entry||found->base>UINTPTR_MAX-t.flag)return false;
        auto address=uintptr_t(found->base+t.entry);auto source=nhk::source_at(found->owned,address,sizeof(Words));if(!source)return false;
        Words words{};const auto image=home_layout::elf_targets::parse(found->bytes);if(!image)return false;
        for(unsigned j=0;j<4;++j){auto w=image->word(t.entry+j*4);if(!w)return false;words[j]=*w;}
        prepared[i]={address,replacements[i],&originals[i],*source,words};prepared_flags[i]=found->base+t.flag;
    }
    path=found->path;view_begin=found->begin;view_end=found->end;
    slots=prepared;
    for(unsigned i=0;i<3;++i) flags[i].store(prepared_flags[i],std::memory_order_release);
    return true;
}
}
bool requested() noexcept { return enabled.load(std::memory_order_acquire); }
void configure(bool value) noexcept {
    const bool old=enabled.exchange(value,std::memory_order_acq_rel);
    if (value != old) {
        std::lock_guard guard(wake_lock);
        wake_revision.fetch_add(1,std::memory_order_release);
        wake.notify_one();
    }
    if (!value && old) publish(0);
    if (value && !old && dock_motion_event>=0) (void)eventfd_write(dock_motion_event,1);
}
uint64_t maintenance_revision() noexcept {return wake_revision.load(std::memory_order_acquire);}
void wait_for_maintenance(long ns,uint64_t revision) noexcept {
    std::unique_lock guard(wake_lock);
    wake.wait_for(guard,std::chrono::nanoseconds(ns),[&]{return maintenance_revision()!=revision;});
}
uint64_t packed() noexcept{return published.load(std::memory_order_acquire);}
void maintain(Hook hook,Unhook unhook,Protect protect) noexcept {
    try {
        if(!requested()||retired||!hook)return;
        install_hook=hook;remove_hook=unhook;protect_hook=protect;
        const auto now=now_ns();if(!now)return;
        if(!slots[0].address){
            if(attempted||discoveries>=16||now<next_discovery)return;
            ++discoveries;next_discovery=now+500000000;
            if(!discover())return;
        }
        const std::vector<size_t> order{0,1,2};
        if(!last_generation||now-last_generation>=30000000000ULL){
            last_generation=now;auto inv=inventory();bool valid=bool(inv);
            if(inv)for(const auto& s:slots){std::array<uintptr_t,1>a{s.address};std::array<nhk::CodeSource,1>e{s.source};valid&=nhk::mapping_state(*inv,a,e,16)==nhk::MappingState::same;}
            if(!valid){bank_ready=false;publish(0);retired=true;return;}
        }
        const bool healthy=nhk::ordered_slots_healthy(slots,order,[](auto addresses,auto,auto observed){
            for(size_t i=0;i<addresses.size();++i)if(!nhk::safe_read(addresses[i],std::as_writable_bytes(observed.subspan(i,1))))return false;return true;});
        const bool live=healthy||(nhk::ensure_slots_live(slots,host(),order)
            &&nhk::ordered_slots_healthy(slots,order,[](auto a,auto,auto w){for(size_t i=0;i<a.size();++i)if(!nhk::safe_read(a[i],std::as_writable_bytes(w.subspan(i,1))))return false;return true;}));
        bank_ready.store(live,std::memory_order_release);snapshot();
        nhk::android_diagnostics::counter("home.recents.statusbar",kFeature,hits.load(std::memory_order_relaxed));
    }catch(...){bank_ready=false;publish(0);}
}
}
