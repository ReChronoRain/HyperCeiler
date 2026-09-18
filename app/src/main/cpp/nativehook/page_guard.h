/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime page lifetime policy: MADV_DONTNEED protection.
 *
 * Ported from MiuiBackGestureHook's lsposed_hook_backend.cpp
 * (AddProtectedPage / GuardedMadvise / guard state machine; Apache-2.0, see
 * THIRD_PARTY_NOTICES.md in this directory). In the source project the
 * library whose `madvise` is hooked was hard-wired to the HyperOS Flutter
 * runtime; here that decision belongs to the caller - the policy is optional
 * and parameterized, because an ordinary native library never issues
 * MADV_DONTNEED against its own code and must not pay for this machinery.
 *
 * Model: hook targets registered with [add_protected_page] survive an
 * MADV_DONTNEED issued by anyone whose `madvise` import was hooked through
 * [install_madvise_guard] - the hook splits the requested range around the
 * protected pages and passes the remaining pieces to the real madvise.
 *
 * Publication protocol: the table is written under a spin lock and published
 * with a release store of the count; readers acquire the count before touching
 * the slots, so a reader racing an installer sees either the old table or the
 * new entry, never a torn one. Registration is all-or-nothing: a full table
 * reports failure instead of silently dropping the page.
 */
#pragma once

#include "nhk_base.h"

#include <atomic>
#include <cstdint>
#include <span>

#include <sys/mman.h>
#include <unistd.h>

namespace nhk {

inline constexpr size_t kMaxProtectedPages = 128;

inline std::atomic<uint32_t> *protected_page_lock() {
    static std::atomic<uint32_t> lock{0};
    return &lock;
}

inline uintptr_t *protected_page_slots() {
    static uintptr_t slots[kMaxProtectedPages]{};
    return slots;
}

/** Published count: stored with release, loaded with acquire. */
inline std::atomic<size_t> *protected_page_count() {
    static std::atomic<size_t> count{0};
    return &count;
}

/**
 * Register a page that must survive MADV_DONTNEED. Idempotent per page.
 * Returns false when the table is full, so the caller can report that the
 * target is not actually protected instead of assuming it is.
 */
inline bool add_protected_page(uintptr_t address) {
    const uintptr_t page = static_cast<uintptr_t>(page_down(address));
    auto *lock = protected_page_lock();
    uint32_t expected = 0;
    while (!lock->compare_exchange_weak(expected, 1U, std::memory_order_acquire)) {
        expected = 0;
    }
    auto *slots = protected_page_slots();
    auto *count = protected_page_count();
    const size_t current = count->load(std::memory_order_relaxed);
    bool registered = false;
    for (size_t i = 0; i < current; ++i) {
        if (slots[i] == page) {
            registered = true;
            break;
        }
    }
    if (!registered && current < kMaxProtectedPages) {
        slots[current] = page;
        // The value must be visible before the count that publishes it.
        count->store(current + 1, std::memory_order_release);
        registered = true;
    }
    lock->store(0U, std::memory_order_release);
    return registered;
}

/** True when [begin, begin+length) overlaps any protected page. */
inline bool range_touches_protected_page(uintptr_t begin, size_t length) {
    auto *slots = protected_page_slots();
    // Acquire pairs with the release store of the count above: every slot
    // below the published count is fully initialized.
    const size_t count = protected_page_count()->load(std::memory_order_acquire);
    const uint64_t page = host_page_size();
    for (size_t i = 0; i < count; ++i) {
        const uintptr_t start = slots[i];
        const uintptr_t end = start + static_cast<uintptr_t>(page);
        if (begin < end && begin + length > start) return true;
    }
    return false;
}

/**
 * A drop-in `madvise` replacement. Non-DONTNEED advice, empty ranges and
 * unaligned ranges pass straight through, so the kernel reports its own
 * EINVAL instead of receiving a silently re-shaped request. DONTNEED ranges
 * that touch a protected page are split around it.
 *
 * `real_madvise` must be the original function (the GOT slot's previous value)
 * and must be published before this replacement can be reached.
 */
inline int guarded_madvise(void *address, size_t length, int advice,
    int (*real_madvise)(void *, size_t, int)) {
    constexpr int kMadvDontneed = 4;
    if (real_madvise == nullptr) return -1; // Never forward into a null target.
    const uintptr_t begin = reinterpret_cast<uintptr_t>(address);
    const uint64_t page = host_page_size();
    // Only an invalid range is handed back to the kernel unchanged. `length`
    // deliberately is NOT required to be page-aligned: Linux accepts any byte
    // length for MADV_DONTNEED and discards every page it covers, so a short
    // request against a protected page must still be reshaped.
    if (advice != kMadvDontneed || address == nullptr || length == 0
        || begin % page != 0 || add_overflows(begin, length)) {
        return real_madvise(address, length, advice);
    }
    // The pages the kernel would discard: from `begin` through the page that
    // holds the last requested byte.
    const uintptr_t last_page = static_cast<uintptr_t>(page_down(begin + length - 1));
    if (!range_touches_protected_page(begin, static_cast<size_t>(last_page - begin + page))) {
        return real_madvise(address, length, advice);
    }
    uintptr_t chunk = begin;
    while (chunk <= last_page) {
        if (range_touches_protected_page(chunk, page)) {
            // Carve the protected page out of the discard request entirely.
            chunk += page;
            continue;
        }
        // Forward the tail's real byte length, not a page-rounded one, so the
        // kernel sees exactly what the caller asked for.
        const size_t chunk_bytes = (chunk == last_page)
            ? static_cast<size_t>(begin + length - chunk)
            : static_cast<size_t>(page);
        const int result = real_madvise(reinterpret_cast<void *>(chunk),
            chunk_bytes, advice);
        if (result != 0) return result;
        chunk += page;
    }
    return 0;
}

} // namespace nhk
