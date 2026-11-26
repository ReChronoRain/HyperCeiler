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

struct Freeform {
    uint32_t device_reject;
    uint32_t continue_after_device;
};

inline bool has_interior_entry(const home_dynamic::Body& words, size_t start, size_t count) {
    for (size_t i = 0; i < words.size(); ++i) {
        const uint32_t word = words[i];
        int32_t displacement;
        if ((word & 0x7c000000u) == 0x14000000u) {
            displacement = int32_t(word << 6) >> 6;
        } else if ((word & 0xff000010u) == 0x54000000u || (word & 0x7e000000u) == 0x34000000u) {
            displacement = int32_t(word << 8) >> 13;
        } else if ((word & 0x7e000000u) == 0x36000000u) {
            displacement = int32_t(word << 13) >> 18;
        } else {
            continue;
        }
        const auto target = int64_t(i) + displacement;
        if (target > int64_t(start) && target < int64_t(start + count)) return true;
    }
    return false;
}

inline std::optional<uint32_t> resolve_share_apk_click(const Code& code) {
    constexpr uint32_t file_check[] = {
        0xf85f83a0, 0xb840b001, 0x8b1c8021, 0x6b16003f, 0x54000061,
        0xaa1603e0, 0x14000002,
    };
    constexpr uint32_t check_result[] = {0x6b16001f, 0x54000220, 0x37200200};
    constexpr uint32_t file_exists_prefix[] = {
        0xa9bf79fd, 0xaa0f03fd, 0xd10081ef, 0xf81f83a1,
    };
    // The stock callback reads the package from the captured menu arguments.
    // Its actual share call consumes only the receiver and package.
    constexpr uint32_t package_load[] = {
        0xb8417020, 0x8b1c8000, 0xb8417002, 0x8b1c8042, 0x6b16005f,
        0x54000061, 0xaa1603e0, 0x14000003, 0xb8407040, 0x8b1c8000,
        0x6b16001f, 0x54000061, 0xf9407f62, 0x14000002, 0xaa0003e2,
        0xb840f020, 0x8b1c8000, 0xaa0003e1,
    };
    constexpr uint32_t share_prefix[] = {
        0xa9bf79fd, 0xaa0f03fd, 0xd10041ef, 0xaa0103e0, 0xaa0203e3,
        0xf81f83a2, 0xaa1603e1,
    };
    constexpr uint32_t confirmation_args[] = {
        0xb8413022, 0x8b1c8042, 0xb8417040, 0x8b1c8000, 0x6b16001f,
        0x540000e0, 0xb840f020, 0x8b1c8000, 0xb8417023, 0x8b1c8063, 0xaa0003e1,
    };
    constexpr size_t file = 54, exists = 61, result_site = 62;
    std::optional<uint32_t> result;
    for (const auto& function : code.functions()) {
        const auto& w = function.words;
        if (w.size() != 84 || w[2] != 0xd100e1ef || w[49] != 0x372000a0
            || w[80] != 0xaa1603e0 || w[83] != 0xd65f03c0
            || !std::equal(std::begin(file_check), std::end(file_check), w.begin() + file)
            || !std::equal(std::begin(check_result), std::end(check_result), w.begin() + result_site)
            || w[5] != 0xf81e83a3 || w[9] != 0xf81e03a0 || w[13] != 0xf85e83a2
            || w[14] != 0xb8017002 || w[69] != 0xf85e03a2
            || (w[65] & 0xffc003ffu) != 0x91400361u
            || has_interior_entry(w, result_site, 4)) continue;
        uint32_t target = 0, share = 0, confirm = 0;
        home_dynamic::Body body, callback, confirmation;
        const uint32_t callback_va = function.va + uint32_t(w.size() * 4);
        if (!home_dynamic::target(w[exists], function.va + exists * 4, target)
            || !code.prefix(target, 4, body)
            || !std::equal(body.begin(), body.end(), file_exists_prefix)
            || !code.body(callback_va, callback) || callback.size() != 43
            || callback[2] != 0xf9400fa0 || callback[3] != 0xb8417001 || callback[4] != 0x8b1c8021
            || callback[6] != 0x372002a0
            || !std::equal(std::begin(package_load), std::end(package_load), callback.begin() + 7)
            || !std::equal(std::begin(confirmation_args), std::end(confirmation_args), callback.begin() + 27)
            || !home_dynamic::target(callback[25], callback_va + 25 * 4, share)
            || !code.body(share, body) || body.size() != 47
            || !std::equal(std::begin(share_prefix), std::end(share_prefix), body.begin())
            || !home_dynamic::target(callback[38], callback_va + 38 * 4, confirm)
            || !code.body(confirm, confirmation) || confirmation.size() != 151) continue;
        if (result) return {};
        result = function.va + result_site * 4;
    }
    return result;
}

inline const char* share_apk_click_result(const recent_render::Registers& saved) {
    if (saved.x[0] == saved.x[22]) return "file_missing";
    if (saved.x[0] == saved.x[22] + 0x30) return "file_not_found";
    if (saved.x[0] == saved.x[22] + 0x20) return "file_ready";
    return "unexpected_file_check_result";
}

template<class Read>
inline bool apply_share_apk_click(recent_render::Registers& saved, Read read) {
    const auto dart_null = saved.x[22];
    if (saved.x[0] != dart_null || saved.x[29] < 32) return false;
    const auto object = [&](uint64_t value) {
        return (value & 1) && uint32_t(value) != uint32_t(dart_null);
    };
    const auto field = [&](uint64_t owner, unsigned offset, uint64_t& value) {
        uint32_t compressed = 0;
        if (!object(owner) || !read(owner + offset, compressed)) return false;
        value = (saved.x[28] << 32) + compressed;
        return object(value);
    };
    uint64_t closure = 0, item = 0, component = 0, package = 0;
    if (!read(saved.x[29] - 32, closure) || !field(closure, 0x17, item)
        || !field(item, 0x17, component) || !field(component, 0x7, package)) return false;
    // Continue through the original confirmation and share callback. A real
    // file that failed existsSync still takes the stock rejection path.
    saved.x[0] = dart_null + 0x20;
    return true;
}

inline std::optional<Freeform> resolve_freeform(const Code& code) {
    constexpr uint32_t prefix[] = {
        0xa9bf79fd, 0xaa0f03fd, 0xd10101ef, 0xf81f83b6, 0xf81f03a1,
        0xaa0203f0, 0xaa0103e2, 0xaa1003e1, 0xf81e83a1, 0xf81e03a3, 0xf81d83a5,
    };
    constexpr uint32_t result_window[] = {0x37200260, 0xf9403f40};
    constexpr uint32_t continuation[] = {0xf85e83a1, 0xf85ff020, 0xd34c7c00};
    std::optional<Freeform> result;
    for (const auto& function : code.functions()) {
        const auto& w = function.words;
        if (w.size() != 305 || !std::equal(std::begin(prefix), std::end(prefix), w.begin())) continue;
        constexpr uint32_t device_branch = 56, site = 69, reject = site + 1, accept = site + 19;
        if (!std::equal(std::begin(result_window), std::end(result_window), w.begin() + site)
            || !std::equal(std::begin(continuation), std::end(continuation), w.begin() + accept)
            || !home_dynamic::ldw(w[device_branch - 2], 0, 1)
            || !home_dynamic::field(home_dynamic::imm9(w[device_branch - 2]))
            || w[device_branch - 1] != 0x8b1c8021
            || w[device_branch] != 0x372001c1 || w[device_branch + 1] != 0xf9403f40
            || (w[reject + 1] & 0xffc003ffu) != 0xf9400000u
            || w[reject + 2] != 0xf9402370 || w[reject + 3] != 0x6b10001f
            || has_interior_entry(w, reject, 4)
            || w[site - 5] != 0xb8473002 || w[site - 4] != 0x8b1c8042
            || w[site - 3] != 0xaa0203e1 || w[site - 2] != 0xf81d83a2
            || w[accept - 2] != 0x9100c2c0 || (w[accept - 1] >> 26) != 5
            || w[accept + 4] != 0xf87e7abe || w[accept + 5] != 0xd63f03c0) continue;
        uint32_t getter = 0, second_use = 0;
        home_dynamic::Body body;
        const auto call = [&](uint32_t index, uint32_t& target) {
            return home_dynamic::target(w[index], function.va + index * 4, target);
        };
        if (!call(site - 1, getter) || !call(site + 66, second_use) || getter != second_use
            || !code.body(getter, body) || body.size() < 23
            || body[2] != 0xd10021ef || body[22] != 0xd65f03c0) continue;
        if (result) return {};
        // The device branch enters with its owner in x0 and false in x1.
        // Resume directly after that branch so Dart still initializes the
        // reactive value and frame spill used by the subsequent checks.
        result = Freeform{function.va + reject * 4, function.va + (device_branch + 1) * 4};
    }
    return result;
}

inline bool apply_freeform(recent_render::Registers& saved, bool enabled,
                           uintptr_t original, uintptr_t continue_after_device) {
    saved.branch = original;
    const auto dart_null = saved.x[22];
    const auto dart_true = dart_null + 0x20;
    const auto dart_false = dart_null + 0x30;
    if (!enabled || !original || !continue_after_device || saved.x[1] != dart_false
        || saved.x[0] == dart_null || saved.x[0] == dart_true || saved.x[0] == dart_false) return false;
    saved.branch = continue_after_device;
    return true;
}

inline std::optional<uint32_t> resolve_freeform_click(const Code& code) {
    constexpr uint32_t component_check[] = {
        0xf85803a0, 0xb8417001, 0x8b1c8021, 0x6b16003f, 0x54000061, 0xaa1603e0,
    };
    constexpr uint32_t start_prefix[] = {
        0xa9bf79fd, 0xaa0f03fd, 0xd100a1ef, 0xf81f83b6, 0xf81f03a1, 0xf81e83a2,
    };
    constexpr size_t component = 69, result_site = 72, start_call = 78;
    std::optional<uint32_t> result;
    for (const auto& function : code.functions()) {
        const auto& w = function.words;
        if (w.size() != 134 || w[2] != 0xd10241ef
            || w[46] != 0x372002e0 || w[66] != 0x36200060
            || !std::equal(std::begin(component_check), std::end(component_check), w.begin() + component)
            || (w[75] >> 26) != 5 || w[76] != 0xf85903a1 || w[77] != 0xaa0003e2
            || has_interior_entry(w, result_site, 4)) continue;
        uint32_t target = 0;
        home_dynamic::Body body;
        if (!home_dynamic::target(w[start_call], function.va + start_call * 4, target)
            || !code.prefix(target, 6, body)
            || !std::equal(body.begin(), body.end(), start_prefix)) continue;
        if (result) return {};
        result = function.va + result_site * 4;
    }
    return result;
}

inline const char* freeform_click_result(const recent_render::Registers& saved) {
    // The following Dart comparison uses the compressed halves of x1 and x22.
    return uint32_t(saved.x[1]) == uint32_t(saved.x[22]) ? "component_missing" : "calling_start";
}
} // namespace home_layout::features::shortcut_actions
