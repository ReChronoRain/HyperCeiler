/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <span>

namespace home_layout {
// Exact call-word alternatives from a separately inspected launcher image.
// These are guard expectations only: no instruction is written or relocated here.
struct CallWordVariant {
    uint32_t byte_offset;
    uint32_t instruction;
};

inline bool exact_call_variant(std::span<const uint32_t> actual,
    std::span<const uint32_t> original, std::span<const CallWordVariant> calls) {
    if (actual.size() != original.size()) return false;
    size_t previous = 0;
    bool first = true;
    for (const auto &call : calls) {
        const size_t index = call.byte_offset / sizeof(uint32_t);
        if ((call.byte_offset & 3) || index >= original.size()
            || (!first && index <= previous)
            || (original[index] & 0xfc000000u) != 0x94000000u
            || (call.instruction & 0xfc000000u) != 0x94000000u) return false;
        previous = index;
        first = false;
    }
    size_t replacement = 0;
    for (size_t i = 0; i < original.size(); ++i) {
        uint32_t expected = original[i];
        if (replacement < calls.size() && calls[replacement].byte_offset / 4 == i) {
            expected = calls[replacement++].instruction;
        }
        if (actual[i] != expected) return false;
    }
    return replacement == calls.size();
}
} // namespace home_layout
