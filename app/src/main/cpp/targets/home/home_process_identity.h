/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <atomic>
#include <cstdint>
#include <string_view>

namespace home_layout {
// Native scalars only. PID scopes inherited state after fork, and setprogname
// invalidates the same atomic publication before/after a rename. A stale name
// read cannot republish across that invalidation. Without a working rename
// hook, callers deliberately keep the original uncached /proc behavior.
class LauncherProcessIdentity {
    static constexpr uint64_t kPayloadMask=(uint64_t{1}<<34)-1;
    static constexpr uint64_t kGenerationStep=uint64_t{1}<<34;
    std::atomic<uint64_t> state_{0};
public:
    static_assert(std::atomic<uint64_t>::is_always_lock_free);
    void invalidate() {
        auto previous=state_.load(std::memory_order_relaxed);
        while(!state_.compare_exchange_weak(previous,
            (previous&~kPayloadMask)+kGenerationStep,
            std::memory_order_acq_rel,std::memory_order_relaxed)) {}
    }
    template<class Read> bool matches(uint32_t pid,bool cache_safe,Read read) {
        if(!pid)return false;
        if(!cache_safe)invalidate();
        auto previous=state_.load(std::memory_order_acquire);
        if(cache_safe&&uint32_t(previous>>2)==pid&&(previous&3))return (previous&3)==1;
        char name[128]{};const auto size=read(name,sizeof(name));
        if(!size||size>=sizeof(name))return false;
        const auto raw=std::string_view(name,size);
        const auto value=raw.substr(0,raw.find('\0'));
        if(value.empty())return false;
        const bool launcher=value=="com.miui.home";
        const bool transient=value=="usap64"||value=="usap32"||value=="hyos_spawner"
            ||value=="zygote64"||value=="zygote";
        if(cache_safe&&!transient) {
            const auto next=(previous&~kPayloadMask)|(uint64_t(pid)<<2)|(launcher?1u:2u);
            (void)state_.compare_exchange_strong(previous,next,std::memory_order_release,std::memory_order_relaxed);
        }
        return launcher;
    }
};
} // namespace home_layout
