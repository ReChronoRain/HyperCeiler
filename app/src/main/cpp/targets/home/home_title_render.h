/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstring>

namespace home_title {
inline bool new_install_asset(uint64_t text) {
    if (!(text & 1U)) return false;
    uint64_t header = 0;
    std::memcpy(&header, reinterpret_cast<const void *>(text - 1), 8);
    const unsigned cid = (header >> 12) & 0xfffffU;
    if (cid != 94 && cid != 95) return false;
    uint32_t smi = 0;
    std::memcpy(&smi, reinterpret_cast<const void *>(text + 7), 4);
    constexpr char asset[] = "assets/images/new_install_notification.webp";
    constexpr unsigned length = sizeof(asset) - 1;
    if ((smi & 1U) || (smi >> 1) != length) return false;
    for (unsigned index = 0; index < length; ++index) {
        uint16_t value = 0;
        std::memcpy(&value, reinterpret_cast<const void *>(text + 15 + index * (cid == 94 ? 1 : 2)),
            cid == 94 ? 1 : 2);
        if (value != static_cast<unsigned char>(asset[index])) return false;
    }
    return true;
}
// These paths are enabled only after validating the original allocator instructions.
// Execute on the Dart mutator thread, never from settings/Binder/background workers.
inline uint64_t clone_color(uint64_t color, uint64_t thread, int32_t argb) {
    if (argb == -1 || !(color & 1U) || !thread) return color;
    uint64_t header = 0, top = 0, end = 0;
    std::memcpy(&header, reinterpret_cast<const void *>(color - 1), 8);
    if (((header >> 12) & 0xfffffU) != 0x23d7U) return color;
    std::memcpy(&top, reinterpret_cast<const void *>(thread + 0x60), 8);
    std::memcpy(&end, reinterpret_cast<const void *>(thread + 0x68), 8);
    // Dart heap objects require eight-byte alignment; native AAPCS SP requires sixteen.
    // Do not reject a valid nursery top ending in 0x8 merely because it is not native-SP aligned.
    if (!top || (top & 7) || end <= top || end - top <= 48) return color;
    // No GC, lock, runtime call or persistent handle. The caller immediately roots the result.
    const uint64_t next = top + 48;
    std::memcpy(reinterpret_cast<void *>(top), reinterpret_cast<const void *>(color - 1), 48);
    constexpr uint64_t nursery_tag = 0x023d731c;
    std::memcpy(reinterpret_cast<void *>(top), &nursery_tag, 8);
    const uint32_t value = static_cast<uint32_t>(argb);
    for (unsigned index = 0; index < 4; ++index) {
        const unsigned shift = 24 - index * 8;
        const double channel = static_cast<double>((value >> shift) & 255U) / 255.0;
        std::memcpy(reinterpret_cast<void *>(top + 8 + index * 8), &channel, 8);
    }
    std::memcpy(reinterpret_cast<void *>(thread + 0x60), &next, 8);
    return top + 1;
}
}
