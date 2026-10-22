/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "targets/hyos/hyos_snapshot.h"
#include <cstdint>

namespace home_recents_policy {
// A single atomic word is shared with the exact native gateway. Zero means
// installation pending. One is permanently denied. Other valid words encode
// [absolute BOOTTIME milliseconds:56 | selected percentage:8]. Flooring the
// server deadline expires at most 999999ns early, never renews a stale lease.
constexpr uint64_t kPending = 0;
constexpr uint64_t kDenied = 1;
constexpr uint64_t kNanosPerMilli = 1'000'000;
static_assert(__atomic_always_lock_free(sizeof(uint64_t), nullptr));

inline uint64_t project(const hyos_snapshot::Snapshot &snapshot, int64_t now) noexcept {
    const auto &t = snapshot.config.tweaks;
    if (now <= 0 || !snapshot.available || (snapshot.flags & hyos_snapshot::kAllowed) == 0
        || !hyos_snapshot::valid_flags(static_cast<int32_t>(snapshot.flags))
        || snapshot.epoch <= 0 || snapshot.revision <= 0 || !t.animation_recents_enabled
        || t.animation_recents_rate_percent < 30 || t.animation_recents_rate_percent > 200
        || snapshot.valid_until <= now
        || snapshot.valid_until - now > hyos_snapshot::kMaxLeaseNanos) return kDenied;
    const auto deadline = static_cast<uint64_t>(snapshot.valid_until) / kNanosPerMilli;
    if (deadline <= static_cast<uint64_t>(now) / kNanosPerMilli
        || deadline > (UINT64_MAX >> 8)) return kDenied;
    return (deadline << 8) | static_cast<uint64_t>(t.animation_recents_rate_percent);
}

// Only the serialized installer/service publisher owns the immutable selection.
// The assembly gateway can change Pending -> Denied if an update arrives during
// installation. The installer competes on that same word with Pending -> lease;
// it cannot silently activate after an early original-only update won the CAS.
class Lease {
public:
    explicit Lease(uint64_t &word) noexcept : word_(word) {}
    uint64_t load() const noexcept { return __atomic_load_n(&word_, __ATOMIC_ACQUIRE); }
    void cancel() noexcept { __atomic_exchange_n(&word_, kDenied, __ATOMIC_ACQ_REL); }
    bool arm(const hyos_snapshot::Snapshot &snapshot, int64_t now) noexcept {
        const auto value = project(snapshot, now);
        if (value == kDenied) { cancel(); return false; }
        uint64_t expected = kPending;
        if (!__atomic_compare_exchange_n(&word_, &expected, value, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return false;
        epoch_ = snapshot.epoch;
        percentage_ = snapshot.config.tweaks.animation_recents_rate_percent;
        return true;
    }
    bool refresh(const hyos_snapshot::Snapshot &snapshot, int64_t now) noexcept {
        auto expected = load();
        if (expected <= kDenied) return false;
        const auto value = project(snapshot, now);
        // Never revive an expired lease even if the worker finally replies.
        if (now <= 0 || (static_cast<uint64_t>(now) / kNanosPerMilli) >= (expected >> 8)
            || value == kDenied || snapshot.epoch != epoch_
            || snapshot.config.tweaks.animation_recents_rate_percent != percentage_) {
            cancel(); return false;
        }
        return __atomic_compare_exchange_n(&word_, &expected, value, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    }
private:
    uint64_t &word_;
    int64_t epoch_ = 0;
    int percentage_ = 0;
};
} // namespace home_recents_policy
