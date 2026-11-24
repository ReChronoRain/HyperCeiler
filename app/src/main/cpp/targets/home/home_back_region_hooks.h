/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "nativehook/hook_bank.h"

namespace home_layout {

template <size_t N, size_t Count>
bool install_back_region_pair(std::array<nhk::InlineSlot<N>, Count> &slots,
                              const nhk::InlineHookHost<N> &host,
                              const std::vector<size_t> &order) {
    for (const auto index : order) {
        const auto &slot = slots[index];
        // A failed backend call may have published an unverified continuation.
        // Hold those records for process lifetime until the backend can safely drain.
        if (slot.cleanup_pending || slot.install_pending || slot.continuation_pending) return false;
    }
    return nhk::ensure_slots_live(slots, host, order);
}

} // namespace home_layout
