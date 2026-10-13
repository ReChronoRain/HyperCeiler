/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout {
// Original control flow, never a hotSeatMaxCount/getter override. Each aligned
// instruction changes only the capacity decision. All original Dart calls,
// frames, type checks, database writes and insertion loops stay in the image.
//
// `offset` is not stored. The four-word guard is what identifies the site, and the instruction
// that gets replaced is the one the guard ends on, so the position falls out of the search: a
// launcher that inserts a check above one of these sites moves the guard, and the hook follows
// it instead of landing on whatever shifted into the old slot. Two of the guards are the same
// shape in different functions, which is why the symbol name stays part of the identity.
struct CapacitySite {
    const char *symbol;
    uint32_t guard[4]; // cmp and canonical bool operands, or count+branch setup
    uint32_t replacement;
};
inline constexpr CapacitySite kCapacitySites[] = {
    // Only this division uses actual count (FP-0x10); the max-count getter stays pristine.
    {"HotseatLayerGetxController.calculatePositionX", {0xaa0003e1, 0xfc5c83a0, 0xfc5d83a1, 0x94000053}, 0xf85f03a0},
    {"HotseatLayerGetxController._isSeatsFull", {0xeb01001f, 0x910082d0, 0x9100c2d1, 0x9a91a202}, 0xaa1103e2},
    {"HotseatLayerGetxController._isSeatsFull", {0xeb00003f, 0x910082d0, 0x9100c2d1, 0x9a91a202}, 0xaa1103e2},
    {"HotseatDragHandler._isSeatsFull", {0xeb00005f, 0x910082d0, 0x9100c2d1, 0x9a91a201}, 0xaa1103e1},
    {"HotseatLayerGetxController.setHotSeatItems", {0xf85d83a0, 0x93417c02, 0xeb01005f, 0x5400084d}, 0x14000042},
    {"HotseatLayerGetxController.resetAnimationAndUpdateDatabase", {0x93417c43, 0xf85e03a4, 0xeb04007f, 0x5400076d}, 0x1400003b},
    {"HotseatLayerGetxController._updateDragItemCount", {0xaa0003e1, 0xf85f03a0, 0xeb01001f, 0x5400066b}, 0x14000033},
    {"HotseatLayerGetxController._pushDragItem", {0xaa0003e1, 0xf85e83a0, 0xeb01001f, 0x540015e1}, 0x140000af},
};
/* The replaced instruction is the word the guard run ends on. */
inline constexpr size_t kCapacityGuardWords = 4;
inline constexpr int32_t kCapacityGuardLead = 12;
inline constexpr int kCapacitySiteCount = sizeof(kCapacitySites) / sizeof(kCapacitySites[0]);
struct CapacityWord { uintptr_t address = 0; uint32_t original = 0, replacement = 0; };

// Preflight the whole bank. A failed write/readback rolls every word back to
// its previously observed state, including a writer that changed memory before
// reporting an RX/cache-flush failure. No setting is published on partial success.
template <typename Read, typename Write>
bool apply_capacity_words(CapacityWord *sites, bool enabled, Read read, Write write) {
    uint32_t before[kCapacitySiteCount]{};
    for (int i = 0; i < kCapacitySiteCount; ++i) {
        if (!sites[i].address || (sites[i].address & 3) || !read(sites[i].address, before[i])
            || (before[i] != sites[i].original && before[i] != sites[i].replacement)) return false;
    }
    for (int i = 0; i < kCapacitySiteCount; ++i) {
        const uint32_t wanted = enabled ? sites[i].replacement : sites[i].original;
        uint32_t now = 0;
        if (before[i] == wanted) continue;
        if (!write(sites[i].address, wanted) || !read(sites[i].address, now) || now != wanted) {
            for (int j = 0; j <= i; ++j) (void) write(sites[j].address, before[j]);
            return false;
        }
    }
    return true;
}
} // namespace home_layout
