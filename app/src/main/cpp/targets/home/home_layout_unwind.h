/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* AArch64 launcher function bounds from the ELF's own .eh_frame_hdr index. */
#pragma once

#include "nativehook/nhk_base.h"

#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>

namespace home_layout::unwind {

struct FunctionBounds {
    uintptr_t begin = 0;
    uintptr_t end = 0;
};

/*
 * LLVM's Rust image uses the standard version-1 EH-frame header with a
 * PC-relative sdata4 frame pointer and a data-relative sdata4 search table.
 * Refuse every other encoding instead of guessing how an OTA encoded it.
 */
inline std::optional<FunctionBounds> lookup(std::span<const std::byte> bytes,
    uintptr_t header_address, uintptr_t site) {
    constexpr size_t kHeaderBytes = 12;
    constexpr size_t kRowBytes = 8;
    constexpr uint32_t kMaxRows = 1U << 20;
    if (bytes.size() < kHeaderBytes || bytes[0] != std::byte{1}
        || bytes[1] != std::byte{0x1b} || bytes[2] != std::byte{0x03}
        || bytes[3] != std::byte{0x3b}) return {};
    const uint32_t count = nhk::load_le32(bytes.data() + 8);
    if (count == 0 || count > kMaxRows || count > (bytes.size() - kHeaderBytes) / kRowBytes) {
        return {};
    }
    const auto start_at = [&](size_t row) -> std::optional<uintptr_t> {
        const auto offset = static_cast<int32_t>(nhk::load_le32(
            bytes.data() + kHeaderBytes + row * kRowBytes));
        if (offset < 0 && header_address < static_cast<uintptr_t>(-int64_t(offset))) return {};
        if (offset > 0 && header_address > UINTPTR_MAX - static_cast<uintptr_t>(offset)) return {};
        return offset < 0 ? header_address - static_cast<uintptr_t>(-int64_t(offset))
                          : header_address + static_cast<uintptr_t>(offset);
    };
    size_t low = 0;
    size_t high = count;
    while (low < high) {
        const size_t mid = low + (high - low) / 2;
        const auto start = start_at(mid);
        if (!start) return {};
        if (*start <= site) low = mid + 1;
        else high = mid;
    }
    if (low == 0 || low == count) return {};
    const auto begin = start_at(low - 1);
    const auto end = start_at(low);
    if (!begin || !end || *begin > site || site >= *end || *begin >= *end) return {};
    if (low > 1) {
        const auto previous = start_at(low - 2);
        if (!previous || *previous >= *begin) return {};
    }
    if (low + 1 < count) {
        const auto following = start_at(low + 1);
        if (!following || *end >= *following) return {};
    }
    return FunctionBounds{*begin, *end};
}

} // namespace home_layout::unwind
