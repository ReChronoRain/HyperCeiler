/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_device_level.h"
#include "weather_guard.generated.h"
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>
#include <span>

namespace weather_binding {
static_assert(sizeof(uintptr_t) == 8);

struct Binding {
    uintptr_t weather_base = 0;
    uintptr_t public_base = 0;
    uintptr_t standard_base = 0;
    uintptr_t slot = 0;
    weather_device_level::Api api;
};

namespace detail {
inline bool address(uintptr_t base, uintptr_t offset, size_t size, uintptr_t &out) {
    constexpr uintptr_t limit = std::numeric_limits<uintptr_t>::max();
    if (base == 0 || offset > limit - base) return false;
    const uintptr_t candidate = base + offset;
    // Reject a one-past end that cannot be represented too; no reader sees a wrapped span.
    if (size > limit - candidate) return false;
    out = candidate;
    return true;
}

inline bool image_base(uintptr_t base, uintptr_t extent) {
    uintptr_t ignored;
    return base != 0 && (base & 4095) == 0 && address(base, extent, 0, ignored);
}

template <class Read>
bool match(uintptr_t base, const weather_guard::Guard &guard, Read &read_bytes) {
    uintptr_t start;
    if (!address(base, guard.va, guard.bytes.size(), start)) return false;
    std::array<std::byte, 256> actual{};
    for (size_t offset = 0; offset < guard.bytes.size();) {
        const size_t count = guard.bytes.size() - offset < actual.size()
            ? guard.bytes.size() - offset : actual.size();
        if (!read_bytes(start + offset, std::span(actual).first(count))
            || std::memcmp(actual.data(), guard.bytes.data() + offset, count) != 0) return false;
        offset += count;
    }
    return true;
}

template <size_t Count, class Read>
bool image(uintptr_t base, uintptr_t extent,
           const std::array<weather_guard::Guard, Count> &guards, Read &read_bytes) {
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

template <size_t Count, class Read>
bool pointers(uintptr_t base, const std::array<weather_guard::Import, Count> &imports,
              Read &read_bytes, std::array<uintptr_t, Count> &out) {
    for (size_t i = 0; i < Count; i++) if (!pointer(base, imports[i].slot, read_bytes, out[i])) return false;
    return true;
}

template <size_t Count>
bool targets(uintptr_t base, const std::array<weather_guard::Import, Count> &imports,
             const std::array<uintptr_t, Count> &observed) {
    for (size_t i = 0; i < Count; i++) {
        uintptr_t expected;
        if (!address(base, imports[i].target, 0, expected) || observed[i] != expected) return false;
    }
    return true;
}
} // namespace detail

/** Complete pinned Weather guards and its two relocated channel dispatch entries. */
template <class Read>
bool verify_weather(uintptr_t base, Read &&read_bytes) {
    if (!detail::image(base, weather_guard::kWeatherImageSpan, weather_guard::kWeatherGuards, read_bytes))
        return false;
    std::array<uintptr_t, weather_guard::kWeatherVtables.size()> observed{};
    return detail::pointers(base, weather_guard::kWeatherVtables, read_bytes, observed)
        && detail::targets(base, weather_guard::kWeatherVtables, observed);
}

/** Called with the load bias inferred from the actual observed Dart export, never a guessed map. */
template <class Read>
bool verify_dart(uintptr_t base, Read &&read_bytes) {
    return detail::image(base, weather_guard::kDartImageSpan, weather_guard::kDartGuards, read_bytes);
}

/**
 * Read-only binding. read_bytes(address, span<byte>) must return false for inaccessible memory.
 * No allocation, mutex, loader call, object-function call or memory patch occurs here.
 * The installer must still compare ownership of slot against api.get when it writes it.
 */
template <class Read>
bool resolve(uintptr_t weather_base, Read &&read_bytes, Binding &out) {
    if (!verify_weather(weather_base, read_bytes)) return false;
    std::array<uintptr_t, weather_guard::kWeatherImports.size()> weather_pointers{};
    if (!detail::pointers(weather_base, weather_guard::kWeatherImports, read_bytes, weather_pointers)) return false;
    // The generator pins imports[0] to DeviceLevel_get_device_level.
    if (weather_pointers[0] < weather_guard::kPublicGetVa) return false;
    const uintptr_t public_base = weather_pointers[0] - weather_guard::kPublicGetVa;
    if (!detail::image(public_base, weather_guard::kPublicImageSpan, weather_guard::kPublicGuards, read_bytes)
        || !detail::targets(public_base, weather_guard::kWeatherImports, weather_pointers)) return false;

    std::array<uintptr_t, weather_guard::kStandardImports.size()> standard_pointers{};
    if (!detail::pointers(public_base, weather_guard::kStandardImports, read_bytes, standard_pointers)) return false;
    // This explicit read identifies the same public image's allocator, not a global symbol lookup.
    uintptr_t allocate;
    if (!detail::pointer(public_base, weather_guard::kPublicAllocateSlot, read_bytes, allocate)
        || allocate < weather_guard::kStandardAllocateVa) return false;
    const uintptr_t standard_base = allocate - weather_guard::kStandardAllocateVa;
    if (!detail::image(standard_base, weather_guard::kStandardImageSpan, weather_guard::kStandardGuards, read_bytes)
        || !detail::targets(standard_base, weather_guard::kStandardImports, standard_pointers)) return false;

    // Detect import changes during validation. This is not an atomic multi-library transaction.
    std::array<uintptr_t, weather_guard::kWeatherImports.size()> weather_again{};
    std::array<uintptr_t, weather_guard::kStandardImports.size()> standard_again{};
    std::array<uintptr_t, weather_guard::kWeatherVtables.size()> vtables_again{};
    if (!detail::pointers(weather_base, weather_guard::kWeatherImports, read_bytes, weather_again)
        || weather_again != weather_pointers
        || !detail::pointers(public_base, weather_guard::kStandardImports, read_bytes, standard_again)
        || standard_again != standard_pointers
        || !detail::pointers(weather_base, weather_guard::kWeatherVtables, read_bytes, vtables_again)
        || !detail::targets(weather_base, weather_guard::kWeatherVtables, vtables_again)) return false;

    Binding candidate;
    candidate.weather_base = weather_base;
    candidate.public_base = public_base;
    candidate.standard_base = standard_base;
    if (!detail::address(weather_base, weather_guard::kWeatherGetSlot, sizeof(uintptr_t), candidate.slot))
        return false;
    uintptr_t get, drop, shim;
    if (!detail::address(public_base, weather_guard::kPublicGetVa, 0, get)
        || !detail::address(public_base, weather_guard::kPublicDropVa, 0, drop)
        || !detail::address(standard_base, weather_guard::kStandardShimVa, 0, shim)) return false;
    candidate.api.get = reinterpret_cast<decltype(candidate.api.get)>(get);
    candidate.api.allocate = reinterpret_cast<decltype(candidate.api.allocate)>(allocate);
    candidate.api.allocation_shim = reinterpret_cast<decltype(candidate.api.allocation_shim)>(shim);
    candidate.api.drop = reinterpret_cast<decltype(candidate.api.drop)>(drop);
    out = candidate; // The only write to caller output, after every check succeeds.
    return true;
}
} // namespace weather_binding
