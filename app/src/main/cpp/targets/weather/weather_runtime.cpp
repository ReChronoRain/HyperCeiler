/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "weather_runtime.h"
#include "weather_binding.h"
#include "weather_material_binding.h"
#include "weather_material_runtime.h"
#include "weather_card_runtime.h"
#include "weather_card_lifetime.h"
#include "weather_snapshot.h"
#include "nativehook/got_hook_backend.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <atomic>
#include <cerrno>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <dlfcn.h>
#include <limits>
#include <mutex>
#include <pthread.h>
#include <span>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <time.h>
#include <unistd.h>

namespace weather_runtime {
namespace {
constexpr char kDescriptor[] = "android.view.IWindowManager";
constexpr char kTag[] = "HyperCeiler.Weather";
enum class Phase : unsigned { Cold, Preparing, Installed, Inactive, Missed, Rejected, Residue };
std::atomic<Phase> g_phase{Phase::Cold};
std::atomic_bool g_stop{false};
std::atomic_bool g_binding_ready{false};
std::atomic_bool g_level_installed{false};
std::atomic_bool g_dart_rejected{false};
std::atomic_uintptr_t g_dart_base{0};
weather_binding::Binding g_binding{}; // immutable before any replacement is published
weather_snapshot::Cache g_cache;
std::mutex g_initial_mutex;
std::condition_variable g_initial_condition;
bool g_initial_completed = false;
std::atomic<unsigned> g_queries{0}, g_applied{0}, g_allocation_failed{0}, g_refused{0};

int64_t now() {
    timespec value{};
    if (clock_gettime(CLOCK_BOOTTIME, &value) != 0 || value.tv_sec < 0
        || value.tv_nsec < 0 || value.tv_nsec >= 1'000'000'000L
        || value.tv_sec > (std::numeric_limits<int64_t>::max() - 999'999'999) / 1'000'000'000)
        return 0;
    return static_cast<int64_t>(value.tv_sec) * 1'000'000'000LL + value.tv_nsec;
}

// No allocation, retries, /proc fallback, loader operations or preferences.
// Failure and short reads reject, including in the loader return observer.
bool read_memory(uintptr_t address, std::span<std::byte> output) {
    if (address == 0 || output.empty() || address > UINTPTR_MAX - output.size()) return false;
    iovec local{output.data(), output.size()};
    iovec remote{reinterpret_cast<void *>(address), output.size()};
    const long copied = syscall(SYS_process_vm_readv, getpid(), &local, 1, &remote, 1, 0);
    return copied >= 0 && static_cast<size_t>(copied) == output.size();
}

void *on_create(void *) { return nullptr; }
void on_destroy(void *) {}
binder_status_t on_transact(AIBinder *, transaction_code_t, const AParcel *, AParcel *) {
    return STATUS_UNKNOWN_TRANSACTION;
}

bool query(weather_snapshot::Words &words) {
    using CheckService = AIBinder *(*)(const char *);
    const auto service = reinterpret_cast<CheckService>(dlsym(RTLD_DEFAULT, "AServiceManager_checkService"));
    if (service == nullptr) return false;
    AIBinder *window = service("window");
    if (window == nullptr) return false;
    const AIBinder_Class *clazz = AIBinder_getClass(window);
    bool associated = false;
    if (clazz != nullptr) {
        const char *name = AIBinder_Class_getDescriptor(clazz);
        associated = name != nullptr && std::strcmp(name, kDescriptor) == 0;
    } else {
        static const auto *type = AIBinder_Class_define(kDescriptor, on_create, on_destroy, on_transact);
        associated = type != nullptr && AIBinder_associateClass(window, type);
    }
    AParcel *input = nullptr, *output = nullptr;
    binder_status_t status = STATUS_BAD_VALUE;
    if (associated && AIBinder_prepareTransaction(window, &input) == STATUS_OK && input != nullptr)
        status = AIBinder_transact(window, weather_snapshot::kTransaction, &input, &output, 0);
    if (input != nullptr) AParcel_delete(input);
    AIBinder_decStrong(window);
    bool valid = status == STATUS_OK && output != nullptr
        && AParcel_getDataSize(output) == weather_snapshot::kByteCount;
    // Java Parcel primitives align to four bytes, including these three longs.
    // The actual Java writer corpus is also consumed by the C++ decoder tests.
    for (auto &word : words) {
        if (!valid || AParcel_readInt32(output, &word) != STATUS_OK) { valid = false; break; }
    }
    if (output != nullptr) AParcel_delete(output);
    return valid;
}

void poll() {
    weather_snapshot::Words words{};
    const auto began = now();
    const bool received = query(words);
    const auto ended = now();
    const bool timely = received && began > 0 && ended >= began
        && ended - began < weather_snapshot::kMaxLeaseNanos;
    if (!timely || g_stop.load(std::memory_order_acquire)) g_cache.revoke();
    else g_cache.publish(words, ended);
    // Reuse this one HWS worker. Card maintenance only checks ownership and
    // publishes an absolute lease; it never installs or repairs live Dart code.
    weather_card_runtime::service();
    {
        std::lock_guard lock(g_initial_mutex);
        g_initial_completed = true;
    }
    g_initial_condition.notify_all();
}

void *worker(void *) {
    // A stuck Binder call keeps this one worker; there is no replacement worker
    // and no renewal from receipt time. Hook reads independently enforce expiry.
    while (!g_stop.load(std::memory_order_acquire)) {
        poll();
        if (g_stop.load(std::memory_order_acquire)) break;
        timespec remaining{1, 500'000'000};
        while (nanosleep(&remaining, &remaining) != 0 && errno == EINTR) {}
    }
    return nullptr;
}

bool fresh(weather_snapshot::Snapshot &out) {
    return g_phase.load(std::memory_order_acquire) == Phase::Installed
        && !g_dart_rejected.load(std::memory_order_acquire) && g_cache.load_snapshot(now(), out);
}

uintptr_t observed_dart() {
    return g_dart_rejected.load(std::memory_order_acquire) ? 0
        : g_dart_base.load(std::memory_order_acquire);
}

void reject_dart() {
    g_dart_rejected.store(true, std::memory_order_release);
    weather_card_runtime::cancel();
}

void *hooked_device_level(const void *runtime, int32_t selector);

bool same_binding() {
    // Account only for our one known owned relocation. Every other guard and
    // import is read as-is; foreign code or ownership is never silently masked.
    auto read_owned = [](uintptr_t at, std::span<std::byte> bytes) {
        if (!read_memory(at, bytes)) return false;
        if (at == g_binding.slot && bytes.size() == sizeof(uintptr_t)) {
            uintptr_t value;
            std::memcpy(&value, bytes.data(), sizeof(value));
            if (value != reinterpret_cast<uintptr_t>(hooked_device_level)) return false;
            value = reinterpret_cast<uintptr_t>(g_binding.api.get);
            std::memcpy(bytes.data(), &value, sizeof(value));
        }
        return true;
    };
    weather_binding::Binding current;
    return weather_binding::resolve(g_binding.weather_base, read_owned, current)
        && current.weather_base == g_binding.weather_base
        && current.public_base == g_binding.public_base
        && current.standard_base == g_binding.standard_base && current.slot == g_binding.slot
        && current.api.get == g_binding.api.get && current.api.drop == g_binding.api.drop
        && current.api.allocate == g_binding.api.allocate
        && current.api.allocation_shim == g_binding.api.allocation_shim;
}

void *hooked_device_level(const void *runtime, int32_t selector) {
    // Binding publication precedes the atomic GOT replacement. It is retained
    // even after a failed permission restore so an in-flight call can continue.
    if (!g_binding_ready.load(std::memory_order_acquire)) return nullptr;
    const auto caller = reinterpret_cast<uintptr_t>(__builtin_return_address(0));
    const auto dart = g_dart_base.load(std::memory_order_acquire);
    const auto query_index = g_queries.fetch_add(1);
    if (caller != g_binding.weather_base + 0x5fc76c || dart == 0
        || !g_level_installed.load(std::memory_order_acquire)
        || g_dart_rejected.load(std::memory_order_acquire)
        || !weather_binding::verify_dart(dart, read_memory) || !same_binding()) {
        ++g_refused;
        if (query_index == 0) __android_log_print(ANDROID_LOG_WARN, kTag,
            "first DeviceLevel query refused: caller or live image binding unverified; cold restart required");
        return g_binding.api.get(runtime, selector);
    }
    const auto result = weather_device_level::invoke(g_binding.api, runtime, selector, fresh);
    if (result.outcome == weather_device_level::Outcome::Applied) ++g_applied;
    else if (result.outcome == weather_device_level::Outcome::AllocationFailed) ++g_allocation_failed;
    else ++g_refused;
    if (query_index == 0) __android_log_print(ANDROID_LOG_INFO, kTag,
        "first DeviceLevel result outcome=%u applied=%u allocation_failed=%u; cached Dart results require cold restart",
        static_cast<unsigned>(result.outcome), g_applied.load(), g_allocation_failed.load());
    return result.object;
}

struct WriteResult { bool changed, protected_again; };
WriteResult exchange_slot(uintptr_t expected, uintptr_t desired, int protection) {
    const auto page = static_cast<uintptr_t>(nhk::page_down(g_binding.slot));
    if (mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection | PROT_WRITE) != 0)
        return {false, false};
    const bool changed = __atomic_compare_exchange_n(reinterpret_cast<uintptr_t *>(g_binding.slot),
        &expected, desired, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    const bool restored = mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection) == 0;
    return {changed, restored};
}

bool install() {
    weather_binding::Binding resolved;
    if (!weather_binding::resolve(g_binding.weather_base, read_memory, resolved)) return false;
    const auto page = static_cast<uintptr_t>(nhk::page_down(resolved.slot));
    const int protection = nhk::read_protection(page);
    if (protection != PROT_READ || (resolved.slot & (alignof(uintptr_t) - 1)) != 0) return false;
    weather_snapshot::Snapshot authorization;
    if (g_dart_rejected.load(std::memory_order_acquire)
        || !g_cache.load_snapshot(now(), authorization) || !weather_device_level::selected(authorization)) return false;
    g_binding = resolved;
    g_binding_ready.store(true, std::memory_order_release);
    const auto original = reinterpret_cast<uintptr_t>(resolved.api.get);
    const auto replacement = reinterpret_cast<uintptr_t>(hooked_device_level);
    const auto written = exchange_slot(original, replacement, protection);
    if (written.changed && written.protected_again && !g_dart_rejected.load(std::memory_order_acquire)
        && nhk::read_protection(page) == protection
        && same_binding()) return true;
    // CAS avoids overwriting a foreign replacement. Retain the binding even
    // when rollback fails; permanent pass-through is safer than clearing it.
    if (written.changed) exchange_slot(replacement, original, protection);
    const bool restored = mprotect(reinterpret_cast<void *>(page), nhk::host_page_size(), protection) == 0;
    uintptr_t value = 0;
    const bool home = read_memory(resolved.slot, std::as_writable_bytes(std::span(&value, 1))) && value == original;
    if (!restored || !home || nhk::read_protection(page) != protection)
        g_phase.store(Phase::Residue, std::memory_order_release);
    return false;
}
} // namespace

bool start_before_entry(uintptr_t weather_base) {
    auto cold = Phase::Cold;
    if (!g_phase.compare_exchange_strong(cold, Phase::Preparing, std::memory_order_acq_rel))
        return cold == Phase::Installed || cold == Phase::Inactive;
    if (weather_card_lifetime::has_residue()) {
        g_phase.store(Phase::Residue, std::memory_order_release);
        return false;
    }
    if (g_dart_rejected.load(std::memory_order_acquire)
        || (!weather_binding::verify_weather(weather_base, read_memory)
            && !weather_material_binding::verify_weather(weather_base, read_memory))) {
        g_phase.store(Phase::Rejected, std::memory_order_release);
        return false;
    }
    g_binding.weather_base = weather_base;
    pthread_attr_t attributes;
    if (pthread_attr_init(&attributes) != 0) {
        g_phase.store(Phase::Rejected, std::memory_order_release);
        return false;
    }
    pthread_t thread;
    const bool detached = pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_DETACHED) == 0;
    const bool started = detached && pthread_create(&thread, &attributes, worker, nullptr) == 0;
    pthread_attr_destroy(&attributes);
    if (!started) {
        g_phase.store(Phase::Rejected, std::memory_order_release);
        return false;
    }
    {
        std::unique_lock lock(g_initial_mutex);
        g_initial_condition.wait_for(lock, std::chrono::milliseconds(1000), [] { return g_initial_completed; });
    }
    weather_snapshot::Snapshot snapshot;
    if (!g_cache.load_snapshot(now(), snapshot) || !snapshot.available) {
        g_phase.store(Phase::Missed, std::memory_order_release);
        g_stop.store(true, std::memory_order_release);
        __android_log_print(ANDROID_LOG_WARN, kTag, "startup configuration unavailable; cold Weather restart required");
        return false;
    }
    const bool level_selected = weather_device_level::selected(snapshot);
    const bool material_selected = snapshot.allowed && snapshot.unlock_blur;
    const bool card_selected = snapshot.allowed && (snapshot.card_type == 2 || snapshot.card_type == 3);
    if (!level_selected && !material_selected && !card_selected) {
        g_phase.store(Phase::Inactive, std::memory_order_release);
        g_stop.store(true, std::memory_order_release);
        return true;
    }
    const bool level_installed = level_selected && install();
    // Each import resolves its own complete chain. A rejected DeviceLevel
    // binding must not make blur-only configuration force a device class.
    // Unresolved permission/ownership residue closes the entire startup batch.
    const bool material_installed = material_selected
        && g_phase.load(std::memory_order_acquire) != Phase::Residue
        && !g_dart_rejected.load(std::memory_order_acquire)
        && weather_material_runtime::start_before_entry(weather_base, fresh, observed_dart);
    if (weather_material_runtime::has_residue())
        g_phase.store(Phase::Residue, std::memory_order_release);
    const bool card_prepared = card_selected
        && g_phase.load(std::memory_order_acquire) != Phase::Residue
        && !g_dart_rejected.load(std::memory_order_acquire)
        && weather_card_runtime::prepare(fresh);
    if ((!level_installed && !material_installed && !card_prepared)
        || g_phase.load(std::memory_order_acquire) == Phase::Residue
        || g_dart_rejected.load(std::memory_order_acquire)) {
        auto preparing = Phase::Preparing;
        g_phase.compare_exchange_strong(preparing, Phase::Rejected, std::memory_order_acq_rel);
        g_stop.store(true, std::memory_order_release);
        weather_card_runtime::cancel();
        __android_log_print(ANDROID_LOG_WARN, kTag, "pinned image/import or GOT transaction rejected; no Weather override");
        return false;
    }
    g_level_installed.store(level_installed, std::memory_order_release);
    g_phase.store(Phase::Installed, std::memory_order_release);
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "Weather startup: level=%u material=%u card_prepared=%u; awaiting verified Dart image and card installation window",
        static_cast<unsigned>(level_installed), static_cast<unsigned>(material_installed),
        static_cast<unsigned>(card_prepared));
    return true;
}

bool card_startup_pending() noexcept {
    return g_phase.load(std::memory_order_acquire) == Phase::Installed
        && !g_dart_rejected.load(std::memory_order_acquire)
        && weather_card_runtime::status().phase == weather_card_runtime::Phase::Prepared;
}

void on_first_card_snapshot(uintptr_t dart_base, uintptr_t flutter_base) noexcept {
    if (g_phase.load(std::memory_order_acquire) != Phase::Installed
        || g_dart_rejected.load(std::memory_order_acquire)) {
        weather_card_runtime::cancel();
        return;
    }
    weather_card_runtime::install_before_snapshot(dart_base, flutter_base);
    if (weather_card_runtime::status().phase == weather_card_runtime::Phase::Residue) {
        // Permission/ownership residue is a process-wide Weather refusal, just
        // like a failed native GOT rollback. Existing wrappers keep originals.
        g_phase.store(Phase::Residue, std::memory_order_release);
        g_stop.store(true, std::memory_order_release);
        // Do not take Cache's writer mutex at the loader return boundary.
        // The phase immediately denies every consumer; the worker clears its
        // cache if an already in-flight transport later returns.
    }
}

void on_card_snapshot_window_missed(uint32_t) noexcept {
    weather_card_runtime::cancel();
}

void observe_snapshot(const char *symbol, void *result) {
    if (result == nullptr || symbol == nullptr) return;
    if (std::strcmp(symbol, "kDartVmSnapshotData") == 0
        || std::strcmp(symbol, "kDartVmSnapshotInstructions") == 0
        || std::strcmp(symbol, "kDartIsolateSnapshotData") == 0
        || std::strcmp(symbol, "kDartIsolateSnapshotInstructions") == 0) {
        // The pinned Weather ELF exports only the underscored names. A
        // successful fallback denotes an unverified or already missed window.
        reject_dart();
        return;
    }
    const bool vm_data = std::strcmp(symbol, "_kDartVmSnapshotData") == 0;
    const bool other_snapshot = std::strcmp(symbol, "_kDartVmSnapshotInstructions") == 0
        || std::strcmp(symbol, "_kDartIsolateSnapshotData") == 0
        || std::strcmp(symbol, "_kDartIsolateSnapshotInstructions") == 0;
    if (!vm_data && !other_snapshot) return;
    if (g_phase.load(std::memory_order_acquire) != Phase::Installed
        || (!vm_data && g_dart_base.load(std::memory_order_acquire) == 0)) {
        // A snapshot observed before preparation finished is a missed window;
        // observing a later engine must not silently reopen that process.
        reject_dart();
        return;
    }
    if (g_dart_rejected.load(std::memory_order_acquire)) return;
    const uintptr_t address = reinterpret_cast<uintptr_t>(result);
    if (!vm_data) {
        // Successful exports must all identify the same pinned image. A valid
        // VMData guard cannot authorize isolate data or instructions from a
        // different mapping. Never reopen after a mismatched export.
        const auto base = g_dart_base.load(std::memory_order_acquire);
        const uintptr_t offset = std::strcmp(symbol, "_kDartVmSnapshotInstructions") == 0
            ? weather_guard::kDartVmInstructionsVa
            : (std::strcmp(symbol, "_kDartIsolateSnapshotData") == 0
                ? weather_guard::kDartIsolateDataVa : weather_guard::kDartIsolateInstructionsVa);
        if (base == 0 || base > UINTPTR_MAX - offset || address != base + offset)
            reject_dart();
        return;
    }
    if (address < weather_guard::kDartVmDataVa) {
        reject_dart();
        return;
    }
    const auto base = address - weather_guard::kDartVmDataVa;
    if (!weather_binding::verify_dart(base, read_memory)
        && !weather_material_binding::verify_dart(base, read_memory)) {
        reject_dart();
        return;
    }
    uintptr_t absent = 0;
    if (!g_dart_base.compare_exchange_strong(absent, base, std::memory_order_acq_rel) && absent != base)
        reject_dart();
}
} // namespace weather_runtime
