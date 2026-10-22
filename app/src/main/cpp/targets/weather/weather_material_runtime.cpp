/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "weather_material_runtime.h"
#include "weather_material_binding.h"
#include "nativehook/got_hook_backend.h"
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <span>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <unistd.h>

namespace weather_material_runtime {
namespace {
enum class Phase : unsigned { Cold, Preparing, Installed, Missed, Rejected, Residue };
std::atomic<Phase> g_phase{Phase::Cold};
std::atomic_bool g_binding_ready{false};
weather_material_binding::Binding g_binding{};
weather_material_support::Binding g_policy{};
ReadFresh g_read_fresh = nullptr;
DartBase g_dart_base = nullptr;
std::atomic<unsigned> g_applied{0}, g_refused{0};
constexpr char kTag[] = "HyperCeiler.WeatherMaterial";

bool read_memory(uintptr_t address, std::span<std::byte> output) {
    if (address == 0 || output.empty() || address > UINTPTR_MAX - output.size()) return false;
    iovec local{output.data(), output.size()};
    iovec remote{reinterpret_cast<void *>(address), output.size()};
    const long copied = syscall(SYS_process_vm_readv, getpid(), &local, 1, &remote, 1, 0);
    return copied >= 0 && static_cast<size_t>(copied) == output.size();
}

bool hooked_get_bool(const char *key, size_t length, bool default_value);

bool same_binding() {
    auto owned_read = [](uintptr_t address, std::span<std::byte> output) {
        if (!read_memory(address, output)) return false;
        if (address == g_binding.slot && output.size() == sizeof(uintptr_t)) {
            uintptr_t value;
            std::memcpy(&value, output.data(), sizeof(value));
            if (value != reinterpret_cast<uintptr_t>(hooked_get_bool)) return false;
            value = reinterpret_cast<uintptr_t>(g_binding.original);
            std::memcpy(output.data(), &value, sizeof(value));
        }
        return true;
    };
    weather_material_binding::Binding current;
    return weather_material_binding::resolve(g_binding.weather_base, owned_read, current)
        && current.weather_base == g_binding.weather_base
        && current.public_base == g_binding.public_base
        && current.private_base == g_binding.private_base
        && current.slot == g_binding.slot && current.original == g_binding.original;
}

bool fresh(weather_snapshot::Snapshot &snapshot) {
    if (g_phase.load(std::memory_order_acquire) != Phase::Installed
        || g_read_fresh == nullptr || g_dart_base == nullptr) return false;
    const auto dart = g_dart_base();
    return dart != 0 && weather_material_binding::verify_dart(dart, read_memory)
        && same_binding() && g_read_fresh(snapshot);
}

bool hooked_get_bool(const char *key, size_t length, bool default_value) {
    // The immutable original and callbacks are published before replacing GOT.
    // Retain them for process life even if protection restoration later fails.
    if (!g_binding_ready.load(std::memory_order_acquire)) return default_value;
    const auto caller = reinterpret_cast<uintptr_t>(__builtin_return_address(0));
    const bool material_query = weather_material_support::exact_caller(g_policy, caller)
        && key != nullptr && length == sizeof(weather_material_support::kMaterialProperty) - 1
        && std::memcmp(key, weather_material_support::kMaterialProperty, length) == 0;
    if (material_query) {
        // A target query that crosses installation consumes the Lazy even when
        // its original property call blocks. Close the window before that call;
        // a late installer commit must not reopen this process.
        auto preparing = Phase::Preparing;
        g_phase.compare_exchange_strong(preparing, Phase::Missed, std::memory_order_acq_rel);
    }
    const auto result = weather_material_support::invoke(
        g_policy, caller, key, length, default_value, fresh);
    if (material_query) {
        if (result.outcome == weather_material_support::Outcome::Applied) {
            if (g_applied.fetch_add(1) == 0)
                __android_log_print(ANDROID_LOG_INFO, kTag,
                    "material capability overridden; original system blur switch and bionic bit retained; cold restart required for changes");
        } else {
            ++g_refused;
            // A real Lazy initialization caches this result. It is too late to
            // reopen this process after a missing snapshot, expired permission,
            // unknown image or missed Dart observation. Do not edit the cache.
            auto installed = Phase::Installed;
            if (g_phase.compare_exchange_strong(installed, Phase::Missed, std::memory_order_acq_rel))
                __android_log_print(ANDROID_LOG_WARN, kTag,
                    "material capability initialization passed through; no late activation; cold Weather restart required");
        }
    }
    return result.value;
}

struct WriteResult { bool changed, restored; };
WriteResult exchange_slot(uintptr_t expected, uintptr_t desired, int protection) {
    const auto page = static_cast<uintptr_t>(nhk::page_down(g_binding.slot));
    if (mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection | PROT_WRITE) != 0)
        return {false, false};
    const bool changed = __atomic_compare_exchange_n(reinterpret_cast<uintptr_t *>(g_binding.slot),
        &expected, desired, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    // Restoration includes a fresh kernel-mapping observation, not only the
    // mprotect return code. Never reuse the pre-write permission observation.
    const bool restored = mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection) == 0
        && nhk::read_protection(page) == protection;
    return {changed, restored};
}
} // namespace

bool has_residue() {
    return g_phase.load(std::memory_order_acquire) == Phase::Residue;
}

bool start_before_entry(uintptr_t weather_base, ReadFresh read_fresh, DartBase dart_base) {
    auto cold = Phase::Cold;
    if (!g_phase.compare_exchange_strong(cold, Phase::Preparing, std::memory_order_acq_rel))
        return cold == Phase::Installed;
    weather_material_binding::Binding binding;
    if (read_fresh == nullptr || dart_base == nullptr
        || !weather_material_binding::resolve(weather_base, read_memory, binding)
        || !weather_material_binding::cold_state(binding, read_memory)) {
        g_phase.store(Phase::Rejected, std::memory_order_release);
        return false;
    }
    const auto page = static_cast<uintptr_t>(nhk::page_down(binding.slot));
    const int protection = nhk::read_protection(page);
    if (protection != PROT_READ || (binding.slot & 7) != 0) {
        g_phase.store(Phase::Rejected, std::memory_order_release);
        return false;
    }
    g_binding = binding;
    g_policy = {binding.original, weather_base, true};
    g_read_fresh = read_fresh;
    g_dart_base = dart_base;
    g_binding_ready.store(true, std::memory_order_release);
    const auto original = reinterpret_cast<uintptr_t>(binding.original);
    const auto replacement = reinterpret_cast<uintptr_t>(hooked_get_bool);
    const auto written = exchange_slot(original, replacement, protection);
    auto preparing = Phase::Preparing;
    if (written.changed && written.restored
        && same_binding() && weather_material_binding::cold_state(binding, read_memory)
        && g_phase.compare_exchange_strong(preparing, Phase::Installed, std::memory_order_acq_rel)) {
        return true;
    }
    if (written.changed) exchange_slot(replacement, original, protection);
    const bool restored = mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection) == 0;
    uintptr_t actual = 0;
    const bool original_owned = read_memory(binding.slot, std::as_writable_bytes(std::span(&actual, 1)))
        && actual == original;
    const auto failed = g_phase.load(std::memory_order_acquire) == Phase::Missed
        ? Phase::Missed : Phase::Rejected;
    g_phase.store(restored && original_owned && nhk::read_protection(page) == protection
        ? failed : Phase::Residue, std::memory_order_release);
    return false;
}
} // namespace weather_material_runtime
