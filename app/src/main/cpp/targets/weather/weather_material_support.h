/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_snapshot.h"
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <limits>

namespace weather_material_support {
// Exact OS4 Weather image only. Installation must verify the complete two
// Lazy initialization bodies, their property strings and the resolved public
// SystemProperties_get_bool import before publishing this binding. This is a
// borrowed byte slice, not a NUL-terminated C string or an owned Rust object.
using GetBool = bool (*)(const char *, size_t, bool);
inline constexpr char kMaterialProperty[] = "persist.sys.background_blur_supported";
inline constexpr uintptr_t kCallerReturns[] = {0x4402e4, 0x4403dc};

struct Binding {
    GetBool original = nullptr;
    uintptr_t weather_load_bias = 0;
    bool verified = false;
};
enum class Outcome { Passthrough, Applied, MissingOriginal };
struct Result { bool value; Outcome outcome; };

inline bool exact_caller(const Binding &binding, uintptr_t caller) noexcept {
    if (!binding.verified || binding.weather_load_bias == 0) return false;
    for (const auto offset : kCallerReturns) {
        if (binding.weather_load_bias <= std::numeric_limits<uintptr_t>::max() - offset
            && caller == binding.weather_load_bias + offset) return true;
    }
    return false;
}

// read_fresh must consume one complete HWS1 publication against current
// BOOTTIME (weather_snapshot::Cache::read). The original is called exactly once
// with unchanged slice/default, before any override decision. No allocator,
// JNI, Binder, static-cache writes or Dart register access occurs here.
// The raw bionic property and background_blur_enable remain unchanged. The
// original derived isSupportGlass = material && bionic can become true when
// material is unlocked and the original bionic property is already true.
// Weather and Dart cache this capability: disable/revocation stops future
// overrides but cannot invalidate values already copied by the application.
// A changed option therefore requires a fresh Weather process/isolate.
template <class ReadFresh>
Result invoke(const Binding &binding, uintptr_t caller, const char *key,
              size_t length, bool default_value, ReadFresh &&read_fresh) {
    if (!binding.original) return {false, Outcome::MissingOriginal};
    const bool original = binding.original(key, length, default_value);
    if (!exact_caller(binding, caller) || key == nullptr
        || length != sizeof(kMaterialProperty) - 1
        || std::memcmp(key, kMaterialProperty, sizeof(kMaterialProperty) - 1) != 0) {
        return {original, Outcome::Passthrough};
    }
    weather_snapshot::Snapshot current;
    if (!read_fresh(current) || !current.available || !current.allowed
        || current.epoch <= 0 || current.revision <= 0 || !current.unlock_blur) {
        return {original, Outcome::Passthrough};
    }
    return {true, Outcome::Applied};
}
} // namespace weather_material_support
