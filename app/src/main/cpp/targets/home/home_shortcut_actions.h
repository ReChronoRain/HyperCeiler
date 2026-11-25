/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "home_feature_code.h"
#include "home_recent_render.h"

namespace home_layout::features::shortcut_actions {
struct ShareApk {
    uint32_t eligibility;
    uint32_t async_return;
};

inline std::optional<ShareApk> resolve_share_apk(const Code& code) {
    // Both verified AOT versions compute the current-user match in x2,
    // then finish through the standard Dart async return with false.
    constexpr uint32_t user_match[] = {
        0xaa0003e1, 0xf85083a0, 0xb840f002, 0x8b1c8042, 0xf840f043,
        0xeb03003f, 0x910082d0, 0x9100c2d1, 0x9a910202, 0xf81103a2,
    };
    constexpr uint32_t async_return_prefix[] = {
        0xf85f83a2, 0xaa1d03ef, 0xa8c179fd, 0xa9bf79fd, 0xaa0f03fd, 0xa9bf09e0,
    };
    std::optional<ShareApk> result;
    for (const auto& function : code.functions()) {
        const auto& w = function.words;
        if (w.size() != 138 || w[0] != 0xa9bf79fd || w[2] != 0xd10401ef
            || w[3] != 0xf81f83b6 || w[24] != 0x9100c2c0
            || w[106] != 0x540003c1 || w[136] != 0x9100c2c0) continue;
        constexpr uint32_t site = 102;
        if (!std::equal(std::begin(user_match), std::end(user_match), w.begin() + site - 9)
            || w[site + 1] != 0xf9403f41 || (w[site + 2] & 0xffc003ff) != 0xf9400021
            || w[site + 3] != 0x6b16003f
            || (w[137] >> 26) != 5 || (w[25] >> 26) != 5) continue;
        const uint32_t tail = function.va + 137 * 4 + (int32_t(w[137] << 6) >> 4);
        if (tail != function.va + 25 * 4 + (int32_t(w[25] << 6) >> 4)) continue;
        home_dynamic::Body body;
        if (!code.prefix(tail, 3, body)
            || !std::equal(body.begin(), body.end(), async_return_prefix)
            || !code.prefix(tail + 12, 3, body)
            || !std::equal(body.begin(), body.end(), async_return_prefix + 3)) continue;
        if (result) return {};
        result = ShareApk{function.va + site * 4, tail};
    }
    return result;
}

inline bool apply_share_apk(recent_render::Registers& saved, bool enabled,
                           uintptr_t original, uintptr_t async_return) {
    saved.branch = original;
    const auto dart_true = saved.x[22] + 0x20;
    if (!enabled || !original || !async_return || saved.x[2] != dart_true) return false;
    saved.x[0] = dart_true;
    saved.branch = async_return;
    return true;
}
} // namespace home_layout::features::shortcut_actions
