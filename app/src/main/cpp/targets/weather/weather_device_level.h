/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "weather_snapshot.h"
#include <cstddef>
#include <cstdint>

namespace weather_device_level {
// The pinned OS4 public API allocates an exclusively owned three-byte object.
// RAM/CPU/GPU getters read offsets 0/1/2; DeviceLevel_drop uses Rust (3,1).
// Installation must verify the complete public functions AND the allocation
// imports resolved by that same image. Never substitute malloc/free from our
// own C runtime or supply a static/stack object to the original Rust drop.
struct Api {
    void *(*get)(const void *runtime, int32_t selector) = nullptr;
    void *(*allocate)(size_t size, size_t alignment) = nullptr;
    void (*allocation_shim)() = nullptr;
    void (*drop)(void *object) = nullptr;

    bool complete() const { return get && allocate && allocation_shim && drop; }
};

enum class Outcome { Passthrough, Applied, AllocationFailed, AuthorizationChanged, MissingApi };
struct Result { void *object; Outcome outcome; };

inline bool selected(const weather_snapshot::Snapshot &snapshot) {
    return snapshot.available && snapshot.allowed && snapshot.epoch > 0
        && snapshot.revision > 0 && snapshot.device_level >= 0 && snapshot.device_level < 3;
}

// This is called only by the verified Weather getDeviceLevel import wrapper.
// read_fresh must read the complete HWS1 publication using current BOOTTIME.
// The original call always happens once with unchanged arguments, including
// its runtime reference accounting. No object escapes until all three fields
// agree. This does not claim a three-byte atomic store to a shared object.
template <class ReadFresh>
Result invoke(const Api &api, const void *runtime, int32_t selector, ReadFresh &&read_fresh) {
    if (!api.complete()) return {api.get ? api.get(runtime, selector) : nullptr, Outcome::MissingApi};
    void *object = api.get(runtime, selector);
    weather_snapshot::Snapshot chosen;
    if (!read_fresh(chosen) || !selected(chosen)) return {object, Outcome::Passthrough};

    const bool created = object == nullptr;
    if (created) {
        api.allocation_shim();
        object = api.allocate(3, 1);
        // An allocation failure is explicitly not an applied override. Keeping
        // the original null result lets the caller retain its normal -1 map.
        if (object == nullptr) return {nullptr, Outcome::AllocationFailed};
    }

    weather_snapshot::Snapshot current;
    if (!read_fresh(current) || !selected(current) || current.epoch != chosen.epoch
        || current.revision != chosen.revision || current.device_level != chosen.device_level) {
        if (created) api.drop(object);
        return {created ? nullptr : object, Outcome::AuthorizationChanged};
    }

    // Native 1/2/3 -> Dart min() -> Dart 0/1/2. Setting all three fields also
    // covers the original null result, without changing the selector or map.
    const auto native_level = static_cast<uint8_t>(chosen.device_level + 1);
    auto *bytes = static_cast<uint8_t *>(object);
    bytes[0] = native_level;
    bytes[1] = native_level;
    bytes[2] = native_level;
    return {object, Outcome::Applied};
}
} // namespace weather_device_level
