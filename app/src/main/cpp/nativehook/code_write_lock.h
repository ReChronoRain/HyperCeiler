/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <atomic>
#include <memory>
#include <mutex>
#if defined(_WIN32)
#include <process.h>
#else
#include <unistd.h>
#endif

namespace nhk {
struct CodeWriteEpoch {
    long pid;
    std::recursive_mutex mutex;
    explicit CodeWriteEpoch(long owner) : pid(owner) {}
};
inline std::atomic<CodeWriteEpoch*> g_code_write_epoch{nullptr};
inline CodeWriteEpoch* code_write_epoch() {
#if defined(_WIN32)
    const long self = _getpid();
#else
    const long self = getpid();
#endif
    if (self <= 0) return nullptr;
    auto* observed = g_code_write_epoch.load(std::memory_order_acquire);
    std::unique_ptr<CodeWriteEpoch> candidate;
    for (;;) {
        if (observed && observed->pid == self) return observed;
        if (!candidate) candidate = std::make_unique<CodeWriteEpoch>(self);
        if (g_code_write_epoch.compare_exchange_weak(observed,candidate.get(),
            std::memory_order_release,std::memory_order_acquire)) return candidate.release();
        // Losing candidates were never published and have no waiter. A foreign-PID
        // inherited epoch is deliberately not destroyed: its mutex may have been held
        // by a vanished parent thread. Never wait on that mutex in the child.
    }
}
class CodeWriteGuard {
    CodeWriteEpoch* epoch = nullptr;
    bool locked = false;
public:
    CodeWriteGuard() noexcept {
        try { epoch = code_write_epoch(); if (epoch) { epoch->mutex.lock(); locked = true; } }
        catch (...) { /* Allocation/locking failure is an uncompleted write, never an unguarded one. */ }
    }
    ~CodeWriteGuard() { if (locked) epoch->mutex.unlock(); }
    CodeWriteGuard(const CodeWriteGuard&) = delete;
    CodeWriteGuard& operator=(const CodeWriteGuard&) = delete;
    explicit operator bool() const { return locked; }
};
// Serializes this runtime's page permission mutations only. It is not an SDK-wide
// lock, mapping lease, execution-quiescence barrier, or atomic 16-byte publication.
} // namespace nhk
