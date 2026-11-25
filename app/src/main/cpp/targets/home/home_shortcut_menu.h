/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "home_feature_code.h"
#include "home_recent_render.h"

namespace home_layout::features::shortcut_menu {
namespace detail {
inline constexpr uint32_t kCall = 0x94000000;
inline constexpr uint32_t kReturn = 0xd65f03c0;
inline constexpr size_t kLimitCall = 5;

inline bool matches(std::span<const uint32_t> words, std::span<const uint32_t> pattern) {
    if (words.size() < pattern.size()) return false;
    for (size_t i = 0; i < pattern.size(); ++i) {
        const auto expected = pattern[i];
        uint32_t mask = 0xffffffff;
        if (expected == kCall) mask = 0xfc000000;
        // Pool/static offsets, compressed fields and dispatch indices can move.
        else if ((expected & 0xffc00000) == 0xf9400000
            || (expected & 0xffc00000) == 0x91400000) mask = 0xffc003ff;
        else if ((expected & 0xffe00c00) == 0xb8400000
            || (expected & 0xffe00c00) == 0xb8000000) mask = 0xffe00fff;
        else if (expected == 0xd2800011 || (expected & 0xff800000) == 0xf2800000)
            mask = 0xffe0001f;
        if ((words[i] & mask) != (expected & mask)) return false;
    }
    return true;
}

inline constexpr uint32_t sort_prefix[] = {
    0xa9bf79fd, 0xaa0f03fd, 0xd10241ef, 0xaa0103e0, 0xf81f83a1, kCall,
    0xaa0003e2, 0xf85f83a1, 0xf81f03a2, 0xf85ff020, 0xd34c7c00, 0xf90001e1,
    0xd2800011, 0xf2a00031, 0x8b11001e, 0xf87e7abe, 0xd63f03c0, 0x93417c01,
    0xf85f03a3, 0xeb03003f, 0x540000ac, 0xf85f83a0, 0xaa1d03ef, 0xa8c179fd,
    kReturn, 0xf85f83a4, 0xf85ff080, 0xd34c7c00, 0xaa0403e1, 0xaa0303e2,
    0xd2800011, 0xf2a00031, 0x8b11001e, 0xf87e7abe, 0xd63f03c0, 0xaa0003e1,
    0xf9400364, kCall,
};
inline constexpr uint32_t lookup[] = {
    0xf9403f40, 0xf9400000, 0x6b16001f, 0x54000181, 0xf9403f40, 0xf9400000,
    0xf9402370, 0x6b10001f, 0x54000061, 0xf9400362, kCall, 0xf9400370,
    0xf90001f0, 0xf9400364, kCall,
};
inline constexpr uint32_t early_return[] = {
    0xb8400001, 0x8b1c8021, 0x372000a1, 0xd2800080, 0xaa1d03ef, 0xa8c179fd, kReturn,
};
inline constexpr uint32_t orientation_return[] = {
    0xaa0003e1, 0x14000002, 0xaa0003e1, kCall, 0xf27c001f, 0x9a9f07e1,
    0xd1000421, 0x927ff821, 0x91002021, 0x93417c20, 0xaa1d03ef, 0xa8c179fd, kReturn,
};
inline constexpr uint32_t query_to_sort[] = {
    0xd2800121, 0x91400364, 0xf9400084, kCall, 0xaa0003e1, 0xf81e83a1, kCall,
    0x91400361, 0xf9400021, 0xaa1603e2, 0xf81e83a0, kCall, 0xf85e83a2, 0xf85ff041,
    0xd34c7c21, 0xf90001e0, 0xaa0103e0, 0xaa0203e1, 0xf9400364, 0xd2800011,
    0xf2a00051, 0x8b11001e, 0xf87e7abe, 0xd63f03c0, 0xf85e83a0, 0xf85d83a2,
    0xb8000040, 0x360000e0, 0x385ff050, 0x385ff011, 0x8a500a30, 0xea5c821f,
    0x54000040, kCall, 0xf85e83a1, kCall, 0xf85d83a3, 0xb8000060,
    0x385ff070, 0x385ff011, 0x8a500a30, 0xea5c821f, 0x54000040, kCall,
};

inline bool returns_raw_limit(std::span<const uint32_t> words) {
    constexpr uint32_t prologue[] = {0xa9bf79fd, 0xaa0f03fd, 0xd10021ef};
    if (!matches(words, prologue)) return false;
    words = words.subspan(std::size(prologue));
    const auto first = words;
    unsigned early_paths = 0;
    while (words.size() >= std::size(lookup) && words[3] == 0x54000181) {
        if (++early_paths > 2 || !matches(words, lookup)
            || words.size() < std::size(lookup) + std::size(early_return)
            || words[1] != first[1] || words[5] != first[5]
            || !matches(words.subspan(std::size(lookup)), early_return)) return false;
        words = words.subspan(std::size(lookup) + std::size(early_return));
    }
    if (early_paths == 0 || words.size() != std::size(lookup) + std::size(orientation_return)
        || words[3] != 0x540001c1 || !matches(words.subspan(4), std::span(lookup).subspan(4))) return false;
    return words[0] == first[0] && words[1] == first[1] && words[2] == first[2] && words[5] == first[5]
        && matches(words.subspan(std::size(lookup)), orientation_return);
}
} // namespace detail

inline std::optional<uint32_t> resolve(const Code& code) {
    using namespace detail;
    std::optional<uint32_t> sort;
    for (const auto& function : code.functions()) {
        if (!matches(function.words, sort_prefix)) continue;
        uint32_t limit;
        home_dynamic::Body body;
        if (!home_dynamic::target(function.words[kLimitCall], function.va + kLimitCall * 4, limit)
            || !code.body(limit, body) || !returns_raw_limit(body)) continue;
        if (sort) return {};
        sort = function.va;
    }
    if (!sort) return {};

    // Require the async app list consumer and reject an unexpected second caller.
    bool found = false;
    for (const auto& function : code.functions()) {
        const std::span words(function.words);
        for (size_t i = 0; i < words.size(); ++i) {
            uint32_t target;
            if (!home_dynamic::target(words[i], function.va + i * 4, target) || target != *sort) continue;
            constexpr size_t call = 35;
            if (found || i < call || !matches(words.subspan(i - call), query_to_sort)
                || home_dynamic::imm9(words[i - 9]) != home_dynamic::imm9(words[i + 2])) return {};
            found = true;
        }
    }
    if (!found) return {};
    return *sort + (kLimitCall + 1) * 4;
}

inline bool apply(recent_render::Registers& saved, bool enabled, uintptr_t original) {
    saved.branch = original;
    // The sort routine consumes an unboxed count. Only expand the larger stock
    // layout, and replay its original instructions after updating the result.
    if (!enabled || !original || saved.x[0] != 4) return false;
    saved.x[0] = 6;
    return true;
}
} // namespace home_layout::features::shortcut_menu
