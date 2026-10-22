/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_snapshot.h"
#include <atomic>
#include <cstdint>
#include <limits>

namespace weather_card_control {
// The ARM gateway consumes one acquire-loaded word. Low bits 2/3 are the
// original raw light/dark values; the upper 62 bits are an absolute BOOTTIME
// deadline. No receipt-time extension is permitted.
constexpr uint64_t kTerminal = UINT64_MAX - 2; // low bits 1: never an enabled mode
static_assert((kTerminal & 3) == 1);
static_assert(__atomic_always_lock_free(sizeof(uint64_t), nullptr));

inline uint64_t project(const weather_snapshot::Snapshot &snapshot, int64_t now) noexcept {
    if (now <= 0 || !snapshot.available || !snapshot.allowed
        || snapshot.epoch <= 0 || snapshot.revision <= 0
        || (snapshot.card_type != 2 && snapshot.card_type != 3)
        || snapshot.valid_until <= now
        || snapshot.valid_until - now > weather_snapshot::kMaxLeaseNanos
        || static_cast<uint64_t>(snapshot.valid_until) > (UINT64_MAX >> 2)) return 0;
    return (static_cast<uint64_t>(snapshot.valid_until) << 2)
        | static_cast<uint64_t>(snapshot.card_type);
}

// Exactly one serialized installer/control publisher owns normal updates.
// Concurrent terminal cancellation may come from the snapshot observer. A
// terminal marker differs from every valid or temporarily disabled lease, so
// a publisher holding an old value cannot reopen it after cancellation.
// This does not rewind a gateway that already sampled its authorization, or
// undo a raw field value already consumed/cached by Dart.
class Lease {
public:
    explicit Lease(uint64_t &word) noexcept : word_(word) {}
    uint64_t load() const noexcept { return __atomic_load_n(&word_, __ATOMIC_ACQUIRE); }
    bool terminal() const noexcept { return load() == kTerminal; }
    void cancel() noexcept { __atomic_exchange_n(&word_, kTerminal, __ATOMIC_ACQ_REL); }
    bool publish(uint64_t expected, const weather_snapshot::Snapshot &snapshot,
                 int64_t now) noexcept {
        if (expected == kTerminal) return false;
        return __atomic_compare_exchange_n(&word_, &expected, project(snapshot, now), false,
            __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    }
    bool clear(uint64_t expected) noexcept {
        if (expected == kTerminal) return false;
        return __atomic_compare_exchange_n(&word_, &expected, uint64_t{0}, false,
            __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    }
private:
    uint64_t &word_;
};

// A64 B, never BL. Compute the signed displacement without signed overflow or
// truncating a far target. Both endpoints must be 4-byte aligned.
inline bool branch(uintptr_t source, uintptr_t target, uint32_t &word) noexcept {
    if (source == 0 || target == 0 || ((source | target) & 3) != 0 || source == target)
        return false;
    uint32_t immediate;
    if (target > source) {
        const uintptr_t distance = target - source;
        if (distance > 0x07fffffc) return false;
        immediate = static_cast<uint32_t>(distance >> 2);
    } else {
        const uintptr_t distance = source - target;
        if (distance > 0x08000000) return false;
        immediate = (uint32_t{0} - static_cast<uint32_t>(distance >> 2)) & 0x03ffffff;
    }
    word = 0x14000000 | immediate;
    return true;
}
} // namespace weather_card_control
