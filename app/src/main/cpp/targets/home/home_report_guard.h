/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "nativehook/nhk_base.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <string_view>

namespace home_report {

inline constexpr std::string_view kSymbol = "UninstallController._shouldShowReport";
inline constexpr std::string_view kPreference = "home_title_hide_report_text";

/*
 * Supplied popsicle 7719 libapp.so, SHA256
 * a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992.
 * Body SHA256 68ad096db7c40a7e4f17f8656aa364bced1a9fd95ed11e50c628129a0ee114f8.
 * The address is resolved from the loaded ELF's exact symbol, never this fixture's VA.
 * No instruction is masked, including the final range-error BL displacement.
 *
 * This complete body establishes the Dart x15 frame, list argument in x2, bool
 * object return in x0, canonical bool offsets from x22, and no stack arguments.
 * Its three observed direct callers consume the result as a dialog report flag.
 * A changed body must undergo a new ABI/scope audit; its name is insufficient.
 */
inline constexpr std::array<uint32_t, 35> kBody7719{
    0xa9bf79fdu, 0xaa0f03fdu, 0xd10041efu, 0xb840b040u, 0x93417c01u,
    0xb4000061u, 0xf100043fu, 0x5400006du, 0x910082c1u, 0x14000015u,
    0xaa0103e0u, 0xd2800001u, 0xeb00003fu, 0x540002a2u, 0xb840f040u,
    0x8b1c8000u, 0xb840f001u, 0x8b1c8021u, 0xb84c7020u, 0x8b1c8000u,
    0xf85ff001u, 0xd34c7c21u, 0x91405f70u, 0xf941ba10u, 0xa90001f0u,
    0xaa0103e0u, 0xaa0003feu, 0xf87e7abeu, 0xd63f03c0u, 0xaa0003e1u,
    0xd27c0020u, 0xaa1d03efu, 0xa8c179fdu, 0xd65f03c0u, 0x941a925eu,
};
inline constexpr size_t kBodyBytes = kBody7719.size() * sizeof(uint32_t);

inline bool accepts(std::string_view symbol, std::span<const std::byte> bytes) {
    if (symbol != kSymbol || bytes.size() != kBodyBytes) return false;
    for (size_t i = 0; i < kBody7719.size(); ++i) {
        if (nhk::load_le32(bytes.data() + i * sizeof(uint32_t)) != kBody7719[i]) return false;
    }
    return true;
}

} // namespace home_report
