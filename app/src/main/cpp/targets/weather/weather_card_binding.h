/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_card_guard.generated.h"
#include <array>
#include <bit>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>
#include <span>

namespace weather_card {
static_assert(sizeof(uintptr_t) == 8 && std::endian::native == std::endian::little);

// The caller must establish that these are the observed Dart snapshot image and
// its actual Flutter owner. Bytes alone do not prove that lifecycle association.
struct Binding {
    uintptr_t dart_base = 0;
    uintptr_t flutter_base = 0;
    std::array<uintptr_t, 2> epilogue{}; // recompute, constructor
};
using BranchWords = std::array<uint32_t, 2>;

namespace detail {
inline bool address(uintptr_t base, uintptr_t offset, size_t size, uintptr_t &out) noexcept {
    constexpr auto max = std::numeric_limits<uintptr_t>::max();
    if (base == 0 || offset > max - base) return false;
    const auto value = base + offset;
    if (size > max - value) return false;
    out = value;
    return true;
}
inline bool bases(uintptr_t dart, uintptr_t flutter) noexcept {
    uintptr_t dart_end, flutter_end;
    return (dart & 4095) == 0 && (flutter & 4095) == 0
        && address(dart, weather_card_guard::kDartImageSpan, 0, dart_end)
        && address(flutter, weather_card_guard::kFlutterImageSpan, 0, flutter_end)
        && (dart_end <= flutter || flutter_end <= dart);
}
inline bool shape(const Binding &binding) noexcept {
    if (!bases(binding.dart_base, binding.flutter_base)) return false;
    for (size_t i = 0; i < 2; ++i) {
        uintptr_t site;
        if (!address(binding.dart_base, weather_card_guard::kEpilogueVa[i], 16, site)
            || binding.epilogue[i] != site) return false;
    }
    return true;
}
inline bool branch_target(uintptr_t pc, uint32_t word, uintptr_t &target) noexcept {
    if ((pc & 3) != 0 || (word & 0xfc000000u) != 0x14000000u) return false;
    int64_t displacement = word & 0x03ffffffu;
    if (displacement & 0x02000000) displacement -= 0x04000000;
    displacement *= 4;
    if (displacement == 0) return false;
    if (displacement < 0) {
        const auto magnitude = static_cast<uintptr_t>(-displacement);
        if (pc <= magnitude) return false;
        target = pc - magnitude;
    } else {
        if (static_cast<uintptr_t>(displacement) > std::numeric_limits<uintptr_t>::max() - pc)
            return false;
        target = pc + static_cast<uintptr_t>(displacement);
    }
    return true;
}
inline bool plan(const Binding &binding, const BranchWords &words) noexcept {
    for (size_t i = 0; i < 2; ++i) {
        uintptr_t target;
        if (!branch_target(binding.epilogue[i], words[i], target)) return false;
        // Approved gateway arenas must be outside both guarded source images.
        if ((target >= binding.dart_base && target - binding.dart_base < weather_card_guard::kDartImageSpan)
            || (target >= binding.flutter_base && target - binding.flutter_base < weather_card_guard::kFlutterImageSpan))
            return false;
    }
    return true;
}

template <class Read>
bool match(uintptr_t base, uintptr_t extent, const weather_card_guard::GuardBytes &guard,
           const BranchWords &words, unsigned owned_mask, bool dart, Read &read_bytes) {
    uintptr_t start;
    if (guard.va > extent || guard.bytes.size() > extent - guard.va
        || !address(base, guard.va, guard.bytes.size(), start)) return false;
    std::array<std::byte, 256> actual{};
    for (size_t offset = 0; offset < guard.bytes.size();) {
        const size_t left = guard.bytes.size() - offset;
        const size_t count = left < actual.size() ? left : actual.size();
        if (!read_bytes(start + offset, std::span(actual).first(count))) return false;
        if (dart) for (size_t i = 0; i < 2; ++i) {
            if ((owned_mask & (1u << i)) == 0) continue;
            const uintptr_t site = weather_card_guard::kEpilogueVa[i];
            const uintptr_t chunk = guard.va + offset;
            if (site >= chunk + count || site + 4 <= chunk) continue;
            // Never normalize a partial word or accept a broad branch opcode mask.
            if (site < chunk || site + 4 > chunk + count) return false;
            const size_t at = site - chunk;
            uint32_t observed;
            std::memcpy(&observed, actual.data() + at, 4);
            if (observed != words[i]) return false;
            constexpr uint32_t original = weather_card_guard::kOriginalEpilogueWord;
            std::memcpy(actual.data() + at, &original, 4);
        }
        if (std::memcmp(actual.data(), guard.bytes.data() + offset, count) != 0) return false;
        offset += count;
    }
    return true;
}

template <class Read>
bool verify(const Binding &binding, const BranchWords &words, unsigned owned_mask, Read &read_bytes) {
    if (!shape(binding) || (owned_mask & ~3u) != 0) return false;
    for (const auto &guard : weather_card_guard::kDartGuards)
        if (!match(binding.dart_base, weather_card_guard::kDartImageSpan, guard, words, owned_mask, true, read_bytes)) return false;
    for (const auto &guard : weather_card_guard::kFlutterGuards)
        if (!match(binding.flutter_base, weather_card_guard::kFlutterImageSpan, guard, words, 0, false, read_bytes)) return false;
    // Detect word ownership changes during the bounded full-image checks.
    // This is a final reread, not an atomic transaction or a lifetime reservation.
    for (size_t i = 0; i < 2; ++i) {
        std::array<std::byte, 4> bytes{};
        if (!read_bytes(binding.epilogue[i], std::span<std::byte>(bytes))) return false;
        uint32_t observed;
        std::memcpy(&observed, bytes.data(), 4);
        const uint32_t expected = (owned_mask & (1u << i)) != 0 ? words[i] : weather_card_guard::kOriginalEpilogueWord;
        if (observed != expected) return false;
    }
    return true;
}
} // namespace detail

// read_bytes(address, span<byte>) must fail for inaccessible spans. No heap, loader,
// Binder, lock, object call or mutation is performed. Each read is at most 256B.
// The only output publication occurs after complete original guards pass.
template <class Read>
bool resolve(uintptr_t dart_base, uintptr_t flutter_base, Read &&read_bytes, Binding &out) {
    if (!detail::bases(dart_base, flutter_base)) return false;
    Binding candidate{dart_base, flutter_base, {}};
    for (size_t i = 0; i < 2; ++i)
        if (!detail::address(dart_base, weather_card_guard::kEpilogueVa[i], 16, candidate.epilogue[i])) return false;
    if (!detail::verify(candidate, BranchWords{}, 0, read_bytes)) return false;
    out = candidate;
    return true;
}

// The installer supplies the exact two approved B encodings derived from its
// owned, verified executable gateways. This function does not approve gateways.
// Bit0 = recompute, bit1 = constructor; unset bits require the original word.
// Even mask0 validates the supplied plan; use resolve before a plan exists.
template <class Read>
bool verify_state(const Binding &binding, Read &&read_bytes, const BranchWords &expected_branch, unsigned owned_mask) {
    return detail::shape(binding) && detail::plan(binding, expected_branch)
        && detail::verify(binding, expected_branch, owned_mask, read_bytes);
}
template <class Read>
bool verify_owned(const Binding &binding, Read &&read_bytes, const BranchWords &expected_branch) {
    return verify_state(binding, read_bytes, expected_branch, 3);
}
} // namespace weather_card
