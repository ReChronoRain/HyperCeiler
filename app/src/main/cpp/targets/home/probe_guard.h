/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * A fault boundary for reading memory that might not be readable.
 *
 * A mapping table says a range is readable; it does not promise that every page of that range can
 * be faulted in. A loader that maps an image and then discards parts of it, or a mapping whose
 * file shrank underneath it, leaves pages whose page-table entry exists but whose contents cannot
 * be produced. Reading one raises SIGBUS/BUS_ADRERR, and in a process as important as the launcher
 * that is not a recoverable error - it is the end of the process.
 *
 * A scan of unknown memory therefore needs a boundary: while a probe is running, a fault inside the
 * probed range unwinds back to the probe and the caller learns that the range could not be read.
 * Everything else is passed through untouched - a fault outside the probed range, or on another
 * thread, restores the original disposition and re-raises, so a genuine crash is still reported as
 * one rather than being swallowed by this file.
 *
 * The state is thread-local on purpose: the handler must decide whether the fault belongs to the
 * probe *on the faulting thread*, and a longjump onto another thread's stack would be fatal.
 */
#pragma once

#include <csetjmp>
#include <csignal>
#include <cstddef>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>

namespace probe {

/** The probe the faulting thread is inside, or null when that thread is not probing. */
inline thread_local sigjmp_buf *t_target = nullptr;
inline thread_local const char *t_low = nullptr;
inline thread_local const char *t_high = nullptr;

/** Dispositions to hand back on the way out; a process-wide handler needs one copy, not per-thread. */
inline struct sigaction g_previous_segv {};
inline struct sigaction g_previous_bus {};
inline bool g_had_segv = false;
inline bool g_had_bus = false;

inline void fault_handler(int signal, siginfo_t *info, void *) {
    sigjmp_buf *const target = t_target;
    const char *const address = info == nullptr ? nullptr : static_cast<const char *>(info->si_addr);
    if (target != nullptr && address != nullptr && address >= t_low && address < t_high) {
        t_target = nullptr;
        siglongjmp(*target, signal);
    }

    // Somebody else's fault. Put the original disposition back before re-raising, so the handler
    // that was there before this file ran gets to describe the crash it was installed for.
    const struct sigaction *previous =
        signal == SIGBUS ? &g_previous_bus : &g_previous_segv;
    const bool had = signal == SIGBUS ? g_had_bus : g_had_segv;
    if (had) {
        ::sigaction(signal, previous, nullptr);
    } else {
        ::signal(signal, SIG_DFL);
    }
    ::raise(signal);
}

/**
 * Installs the boundary for a scan and restores the previous state when it goes away.
 *
 * A generator is sometimes the only thing that cannot be undone later, and this one must not be
 * left installed: the previous handlers belong to the runtime and to the crash reporter.
 */
class Guard {
public:
    Guard() {
        // The handler can fire with no usable stack left, so it gets its own. Allocated rather
        // than a thread-local array: a scan is rare and a launcher thread should not carry this.
        const size_t bytes = 64 * 1024;
        void *const stack = ::mmap(nullptr, bytes, PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (stack != MAP_FAILED) {
            stack_t alternate{};
            alternate.ss_sp = stack;
            alternate.ss_size = bytes;
            alternate.ss_flags = 0;
            if (::sigaltstack(&alternate, &previous_stack_) == 0) {
                altstack_ = stack;
                altstack_bytes_ = bytes;
                have_altstack_ = true;
            } else {
                ::munmap(stack, bytes);
            }
        }

        struct sigaction action {};
        action.sa_sigaction = fault_handler;
        action.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESTART;
        ::sigemptyset(&action.sa_mask);
        g_had_segv = ::sigaction(SIGSEGV, &action, &g_previous_segv) == 0;
        g_had_bus = ::sigaction(SIGBUS, &action, &g_previous_bus) == 0;
    }

    Guard(const Guard &) = delete;
    Guard &operator=(const Guard &) = delete;

    ~Guard() {
        if (g_had_segv) ::sigaction(SIGSEGV, &g_previous_segv, nullptr);
        if (g_had_bus) ::sigaction(SIGBUS, &g_previous_bus, nullptr);
        if (have_altstack_) {
            ::sigaltstack(&previous_stack_, nullptr);
            ::munmap(altstack_, altstack_bytes_);
        }
    }

    /**
     * Run `body` with the boundary armed for [from, from + bytes).
     *
     * Returns false when a fault cut the body short. `body` must not own anything: a fault leaves
     * the stack frame without running destructors.
     */
    template <typename Body>
    bool visit(const void *from, size_t bytes, Body &&body) const {
        sigjmp_buf target;
        t_low = static_cast<const char *>(from);
        t_high = t_low + bytes;
        if (sigsetjmp(target, 1) != 0) {
            t_target = nullptr;
            t_low = nullptr;
            t_high = nullptr;
            return false;
        }
        t_target = &target;
        body();
        t_target = nullptr;
        t_low = nullptr;
        t_high = nullptr;
        return true;
    }

    /** Copy `bytes` out of `from`; false when the range could not be read. */
    bool read(const void *from, void *to, size_t bytes) const {
        return visit(from, bytes, [from, to, bytes] { std::memcpy(to, from, bytes); });
    }

private:
    void *altstack_ = nullptr;
    size_t altstack_bytes_ = 0;
    bool have_altstack_ = false;
    stack_t previous_stack_ {};
};

} // namespace probe
