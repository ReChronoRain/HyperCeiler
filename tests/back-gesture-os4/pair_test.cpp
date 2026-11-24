/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_back_region_hooks.h"

#include <cassert>

int main() {
    using Slot = nhk::InlineSlot<1>;
    using Words = nhk::SlotWords<1>;
    static constexpr Words stock{0x100}, patch{0x200}, foreign{0x300};
    static std::array<Words, 2> live{stock, stock};
    std::array<void *, 2> originals{};
    std::array<Slot, 2> slots{};
    for (size_t i = 0; i < slots.size(); ++i) {
        slots[i] = {4 * (i + 1), reinterpret_cast<void *>(0x1000), &originals[i], {}, stock};
    }
    const std::vector<size_t> order{0, 1};
    nhk::InlineHookHost<1> host;
    host.read_slot = [](const Slot &slot, Words &words) {
        words = live.at(slot.address / 4 - 1);
        return true;
    };
    host.write_words = [](uintptr_t address, const Words &words) {
        live.at(address / 4 - 1) = words;
        return true;
    };
    static unsigned installs = 0;
    static bool fail_window = true;
    host.hook_install = [](void *address, void *, void **original) {
        ++installs;
        const auto index = reinterpret_cast<uintptr_t>(address) / 4 - 1;
        if (index == 1 && fail_window) return -1;
        live.at(index) = patch;
        *original = reinterpret_cast<void *>(0x2000 + index * 4);
        return 0;
    };
    assert(!home_layout::install_back_region_pair(slots, host, order));
    assert(installs == 2 && originals[0] && !originals[1]);
    fail_window = false;
    assert(home_layout::install_back_region_pair(slots, host, order));
    assert(installs == 3 && originals[1]);
    live[0] = stock;
    assert(home_layout::install_back_region_pair(slots, host, order));
    assert(installs == 3 && live[0] == patch);
    live[1] = foreign;
    assert(!home_layout::install_back_region_pair(slots, host, order));
    assert(installs == 3 && live[1] == foreign && originals[1]);
    slots[1].continuation_pending = true;
    live[1] = stock;
    assert(!home_layout::install_back_region_pair(slots, host, order));
    assert(installs == 3 && originals[1] && live[1] == stock);
}
