/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>
#include <span>

extern "C" {
extern const std::byte hc_weather_card_recompute_template[];
extern const std::byte hc_weather_card_recompute_template_code_end[];
extern const std::byte hc_weather_card_recompute_template_lease_literal[];
extern const std::byte hc_weather_card_recompute_template_end[];
extern const std::byte hc_weather_card_constructor_template[];
extern const std::byte hc_weather_card_constructor_template_code_end[];
extern const std::byte hc_weather_card_constructor_template_lease_literal[];
extern const std::byte hc_weather_card_constructor_template_end[];
}

namespace weather_card_gateway {
enum class Kind { Recompute, Constructor };
inline constexpr std::size_t kCodeSize = 284;
inline constexpr std::size_t kLeaseLiteralOffset = 288;
inline constexpr std::size_t kTemplateSize = 296;
inline constexpr uintptr_t kRecomputeEpilogueVa = 0x3a8408;
inline constexpr uintptr_t kConstructorEpilogueVa = 0x3aece0;

// Writes only the caller's unpublished, writable, eight-byte-aligned buffer.
// It does not verify the pointer's mapping, install a branch, change page
// protection or flush I-cache. The caller owns these publication operations.
// Both RX memory and the naturally aligned lease word must remain alive while
// any gateway can run. Revoke by release-storing a word with lowbits0 or1
// (zero or a producer-owned terminal sentinel); never free either to revoke. A reader already past its final lease observation may still write.
// Runtime lease accesses are acquire loads. Lowbits0/1 always deny; active
// producer values are (absolute_boottime_ns << 2) | raw, where raw is2 or3
// and deadline fits62bits. Semantic authorization/generation control belongs to the producer.
inline bool copy_template(Kind kind, std::span<std::byte> output,
                          uintptr_t lease_address) noexcept {
    static_assert(sizeof(uintptr_t) == 8);
    const auto destination = reinterpret_cast<uintptr_t>(output.data());
    if (output.size() != kTemplateSize || destination == 0 ||
        (destination & 7) != 0 || lease_address == 0 ||
        (lease_address & 7) != 0 ||
        destination > std::numeric_limits<uintptr_t>::max() - kTemplateSize) {
        return false;
    }
    const std::byte* begin;
    const std::byte* code_end;
    const std::byte* literal;
    const std::byte* end;
    switch (kind) {
        case Kind::Recompute:
            begin = hc_weather_card_recompute_template;
            code_end = hc_weather_card_recompute_template_code_end;
            literal = hc_weather_card_recompute_template_lease_literal;
            end = hc_weather_card_recompute_template_end;
            break;
        case Kind::Constructor:
            begin = hc_weather_card_constructor_template;
            code_end = hc_weather_card_constructor_template_code_end;
            literal = hc_weather_card_constructor_template_lease_literal;
            end = hc_weather_card_constructor_template_end;
            break;
        default:
            return false;
    }
    const auto start = reinterpret_cast<uintptr_t>(begin);
    if (reinterpret_cast<uintptr_t>(code_end) - start != kCodeSize ||
        reinterpret_cast<uintptr_t>(literal) - start != kLeaseLiteralOffset ||
        reinterpret_cast<uintptr_t>(end) - start != kTemplateSize) {
        return false;
    }
    std::memcpy(output.data(), begin, kTemplateSize);
    std::memcpy(output.data() + kLeaseLiteralOffset, &lease_address,
                sizeof(lease_address));
    return true;
}
} // namespace weather_card_gateway
