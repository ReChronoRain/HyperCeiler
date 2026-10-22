/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_material_support.h"
#include "weather_material_guard.generated.h"
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>
#include <span>

namespace weather_material_binding {
static_assert(sizeof(uintptr_t) == 8);
struct Binding {
    uintptr_t weather_base = 0;
    uintptr_t public_base = 0;
    uintptr_t private_base = 0;
    uintptr_t slot = 0;
    weather_material_support::GetBool original = nullptr;
};

namespace detail {
inline bool address(uintptr_t base, uintptr_t offset, size_t size, uintptr_t &out) {
    constexpr auto limit = std::numeric_limits<uintptr_t>::max();
    if (base == 0 || offset > limit - base) return false;
    const auto candidate = base + offset;
    if (size > limit - candidate) return false;
    out = candidate;
    return true;
}
inline bool image_base(uintptr_t base, uintptr_t extent) {
    uintptr_t ignored;
    return (base & 4095) == 0 && address(base, extent, 0, ignored);
}
template <class Read>
bool match(uintptr_t base, const weather_material_guard::Guard &guard, Read &read_bytes) {
    uintptr_t start;
    if (!address(base, guard.va, guard.bytes.size(), start)) return false;
    std::array<std::byte, 256> bytes{};
    for (size_t offset = 0; offset < guard.bytes.size();) {
        const size_t left = guard.bytes.size() - offset;
        const size_t count = left < bytes.size() ? left : bytes.size();
        if (!read_bytes(start + offset, std::span(bytes).first(count))
            || std::memcmp(bytes.data(), guard.bytes.data() + offset, count) != 0) return false;
        offset += count;
    }
    return true;
}
template <size_t Count, class Read>
bool image(uintptr_t base, uintptr_t extent,
           const std::array<weather_material_guard::Guard, Count> &guards, Read &read_bytes) {
    if (!image_base(base, extent)) return false;
    for (const auto &guard : guards) {
        if (guard.va > extent || guard.bytes.size() > extent - guard.va
            || !match(base, guard, read_bytes)) return false;
    }
    return true;
}
template <class Read>
bool pointer(uintptr_t base, uintptr_t slot, Read &read_bytes, uintptr_t &out) {
    uintptr_t start;
    if ((slot & 7) != 0 || !address(base, slot, sizeof(uintptr_t), start)) return false;
    std::array<std::byte, sizeof(uintptr_t)> bytes{};
    if (!read_bytes(start, std::span<std::byte>(bytes))) return false;
    std::memcpy(&out, bytes.data(), sizeof(out));
    return true;
}
template <class Read>
bool vtables(uintptr_t base, Read &read_bytes) {
    for (const auto &row : weather_material_guard::kWeatherVtables) {
        uintptr_t expected, observed;
        if (!address(base, row.target, 0, expected)
            || !pointer(base, row.slot, read_bytes, observed) || observed != expected) return false;
    }
    return true;
}
} // namespace detail

template <class Read>
bool verify_weather(uintptr_t base, Read &&read_bytes) {
    return detail::image(base, weather_material_guard::kWeatherImageSpan,
                         weather_material_guard::kWeatherGuards, read_bytes)
        && detail::vtables(base, read_bytes);
}
template <class Read>
bool verify_dart(uintptr_t base, Read &&read_bytes) {
    return detail::image(base, weather_material_guard::kDartImageSpan,
                         weather_material_guard::kDartGuards, read_bytes);
}

// Read-only observation, not a reservation/lock. The installer must coordinate
// its first-consumption window. A legitimate original property invocation runs
// with state1 (or state5 with waiters); state3 is required only before first
// initialization, not in policy.
template <class Read>
bool cold_state(const Binding &binding, Read &&read_bytes) {
    if (!detail::image_base(binding.weather_base, weather_material_guard::kWeatherImageSpan))
        return false;
    uintptr_t start;
    if (!detail::address(binding.weather_base, weather_material_guard::kLazyStateVa,
                         sizeof(uint32_t), start)) return false;
    std::array<std::byte, sizeof(uint32_t)> bytes{};
    if (!read_bytes(start, std::span<std::byte>(bytes))) return false;
    uint32_t state;
    std::memcpy(&state, bytes.data(), sizeof(state));
    return state == 3;
}

// All guards, imports and relocated dispatch pointers are independently checked
// without DeviceLevel APIs, symbol lookup, heap allocation or memory mutation.
// read_bytes must fail for unreadable spans. out is assigned only after final rechecks.
// The installer still owns CAS, RELRO restoration and publication to other threads.
template <class Read>
bool resolve(uintptr_t weather_base, Read &&read_bytes, Binding &out) {
    if (!verify_weather(weather_base, read_bytes)) return false;
    uintptr_t public_get;
    if (!detail::pointer(weather_base, weather_material_guard::kWeatherPropertySlot, read_bytes, public_get)
        || public_get < weather_material_guard::kPublicGetVa) return false;
    const uintptr_t public_base = public_get - weather_material_guard::kPublicGetVa;
    if (!detail::image(public_base, weather_material_guard::kPublicImageSpan,
                       weather_material_guard::kPublicGuards, read_bytes)) return false;
    uintptr_t private_get;
    if (!detail::pointer(public_base, weather_material_guard::kPublicPrivateSlot, read_bytes, private_get)
        || private_get < weather_material_guard::kPrivateGetVa) return false;
    const uintptr_t private_base = private_get - weather_material_guard::kPrivateGetVa;
    if (!detail::image(private_base, weather_material_guard::kPrivateImageSpan,
                       weather_material_guard::kPrivateGuards, read_bytes)) return false;

    uintptr_t public_again, private_again;
    if (!detail::pointer(weather_base, weather_material_guard::kWeatherPropertySlot, read_bytes, public_again)
        || public_again != public_get
        || !detail::pointer(public_base, weather_material_guard::kPublicPrivateSlot, read_bytes, private_again)
        || private_again != private_get || !detail::vtables(weather_base, read_bytes)) return false;
    Binding candidate;
    candidate.weather_base = weather_base;
    candidate.public_base = public_base;
    candidate.private_base = private_base;
    if (!detail::address(weather_base, weather_material_guard::kWeatherPropertySlot,
                         sizeof(uintptr_t), candidate.slot)) return false;
    candidate.original = reinterpret_cast<weather_material_support::GetBool>(public_get);
    out = candidate;
    return true;
}
} // namespace weather_material_binding
