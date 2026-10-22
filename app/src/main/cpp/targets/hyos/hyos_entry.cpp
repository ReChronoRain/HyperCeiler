/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "hyos_entry.h"
#include "hyos_control.h"
#include "hyos_runtime_guard.h"
#include "hyos_snapshot_guard.h"
#include "hyos_startup_reason.h"
#include "targets/weather/weather_binding.h"
#include "targets/weather/weather_runtime.h"
#include "targets/weather/weather_card_lifetime.h"
#include "targets/home/recents_runtime.h"
#include "nativehook/memory_io.h"
#include <android/dlext.h>
#include <android/log.h>
#include <dlfcn.h>
#include <atomic>
#include <array>
#include <cstring>
#include <span>
#include <cerrno>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <unistd.h>

extern "C" void hc_hyos_app_entry();

namespace {
constexpr char kTag[] = "HyperCeiler.HyosEntry";
// The loader ABIs carry the original caller explicitly. Public dlopen/dlsym
// trampolines substitute our caller PC and change linker namespaces.
using Dlopen = void *(*)(const char *, int, const void *);
using AndroidDlopen = void *(*)(const char *, int, const android_dlextinfo *, const void *);
using Dlsym = void *(*)(void *, const char *, const void *);
const ZygiskNextAPI *g_api = nullptr;
Dlopen g_dlopen = nullptr;
AndroidDlopen g_android_dlopen = nullptr;
Dlsym g_dlsym = nullptr;
std::atomic_bool g_registered{false};
std::atomic_bool g_home_child{false};
std::atomic_bool g_weather_child{false};
std::atomic_uintptr_t g_runtime_base{0};
std::atomic<void *> g_app_entry{nullptr};
// An extra linker reference to the exact Rust image owns the continuation's
// lifetime after installation. It is intentionally retained until process exit.
std::atomic<void *> g_home_recents_handle{nullptr};
// All fields are inherited inert across specialization. The one immutable
// SearchMapping binding is published before the first successful snapshot.
std::atomic_uintptr_t g_linker_base{0};
std::atomic_bool g_startup_armed{false};
std::atomic_uint g_snapshot_attempt{0}; // 0 unseen, 1 checking, 2 terminal, 3 cancelled, 4 dispatching
std::atomic_uint g_search_binding{0};   // 0 absent, 1 writing, 2 ready
uintptr_t g_search_flutter_base = 0;
void *g_search_handle = nullptr;
pid_t g_search_tid = 0;
static_assert(std::atomic_uint::is_always_lock_free);
static_assert(std::atomic_bool::is_always_lock_free);
static_assert(std::atomic_uintptr_t::is_always_lock_free);
static_assert(std::atomic<void *>::is_always_lock_free);

bool named(const char *path, const char *name) {
    if (path == nullptr) return false;
    const char *slash = std::strrchr(path, '/');
    return std::strcmp(slash == nullptr ? path : slash + 1, name) == 0;
}

bool home_child() {
    return g_registered.load(std::memory_order_acquire)
        && g_home_child.load(std::memory_order_acquire);
}

bool pin_home_recents_image(uintptr_t base, uintptr_t entry) noexcept {
    if (!home_child() || g_dlopen == nullptr || g_dlsym == nullptr
        || base > UINTPTR_MAX - hyos_guard::kHomeEntryVa
        || base + hyos_guard::kHomeEntryVa != entry
        || reinterpret_cast<uintptr_t>(g_app_entry.load(std::memory_order_acquire)) != entry)
        return false;
    if (g_home_recents_handle.load(std::memory_order_acquire) != nullptr) return true;
    const auto *caller = reinterpret_cast<const void *>(entry);
    void *handle = g_dlopen("libapp_launcher.so", RTLD_NOLOAD | RTLD_NOW, caller);
    if (handle == nullptr) return false;
    if (g_dlsym(handle, hyos_guard::kEntryName, caller) != reinterpret_cast<void *>(entry)) {
        dlclose(handle);
        return false;
    }
    void *empty = nullptr;
    if (!g_home_recents_handle.compare_exchange_strong(empty, handle, std::memory_order_acq_rel))
        dlclose(handle); // A competing successful caller already retained one reference.
    return true;
}

bool weather_child() {
    return g_registered.load(std::memory_order_acquire)
        && g_weather_child.load(std::memory_order_acquire);
}

enum class StartupRoute { None, Home, WeatherCard };
StartupRoute startup_route() {
    const bool home = home_child(), weather = weather_child();
    if (home == weather) return StartupRoute::None;
    return home ? StartupRoute::Home : StartupRoute::WeatherCard;
}

void snapshot_missed(StartupRoute route, uint32_t reason) {
    if (route == StartupRoute::Home) hyos_control::on_home_snapshot_window_missed(reason);
    else if (route == StartupRoute::WeatherCard) weather_runtime::on_card_snapshot_window_missed(reason);
}

bool boundary_read(uintptr_t address, void *destination, size_t length);

template <class T, size_t N>
bool matches(uintptr_t base, uintptr_t offset, const std::array<T, N> &expected) {
    if (base == 0 || offset > UINTPTR_MAX - base) return false;
    std::array<T, N> actual{};
    return nhk::safe_read(base + offset, std::as_writable_bytes(std::span(actual)))
        && actual == expected;
}

bool verified_runtime(uintptr_t base) {
    return matches(base, hyos_guard::kRuntimeNoteVa, hyos_guard::kRuntimeBuildIdNote)
        && matches(base, hyos_guard::kDlsymWindowVa, hyos_guard::kDlsymWords)
        && matches(base, hyos_guard::kEntryWindowVa, hyos_guard::kEntryCallWords);
}

void *consider_entry(void *target, const void *caller) {
    const uintptr_t source = reinterpret_cast<uintptr_t>(caller);
    const uintptr_t entry = reinterpret_cast<uintptr_t>(target);
    const uintptr_t entry_va = home_child() ? hyos_guard::kHomeEntryVa : weather_guard::kWeatherEntryVa;
    if (source < hyos_guard::kDlsymReturnVa || entry < entry_va) return target;
    const uintptr_t base = source - hyos_guard::kDlsymReturnVa;
    if (!verified_runtime(base)) return target;
    const bool application_matches = home_child()
        ? matches(entry - entry_va, entry_va, hyos_guard::kHomeEntryWords)
        : weather_child() && weather_binding::verify_weather(entry - entry_va,
            [](uintptr_t at, std::span<std::byte> bytes) { return boundary_read(at, bytes.data(), bytes.size()); });
    if (!application_matches) return target;
    // Publish one immutable pair without a post-fork lock. Different images
    // are declined rather than changing the target of a live tail wrapper.
    uintptr_t old_base = 0;
    if (!g_runtime_base.compare_exchange_strong(old_base, base, std::memory_order_acq_rel)
        && old_base != base) return target;
    void *old_entry = nullptr;
    if (!g_app_entry.compare_exchange_strong(old_entry, target, std::memory_order_acq_rel)
        && old_entry != target) return target;
    return reinterpret_cast<void *>(hc_hyos_app_entry);
}

// Unlike the legacy general-purpose safe_read, this boundary has no /proc
// fallback, allocation, retry, mapping scan, mutex or loader operation. Denial,
// interruption and partial reads all refuse the window. Kernel page faults can
// still occur: this is a finite non-waiting policy, not a realtime guarantee.
bool boundary_read(uintptr_t address, void *destination, size_t length) {
    if (address == 0 || length == 0 || address > UINTPTR_MAX - length) return false;
    iovec local{destination, length};
    iovec remote{reinterpret_cast<void *>(address), length};
    const long count = syscall(SYS_process_vm_readv, getpid(), &local, 1, &remote, 1, 0);
    return count >= 0 && static_cast<size_t>(count) == length;
}

template <class T, size_t N>
bool boundary_matches(uintptr_t base, uintptr_t offset, const std::array<T, N> &expected) {
    if (base == 0 || offset > UINTPTR_MAX - base) return false;
    std::array<T, N> actual{};
    return boundary_read(base + offset, actual.data(), sizeof(actual)) && actual == expected;
}

bool verified_linker(uintptr_t base) {
    using namespace hyos_snapshot_guard;
    // The exported __loader_dlsym prologue is subsequently inline-hooked. These
    // private implementations are not changed by this module and remain exact.
    return boundary_matches(base, kLinkerElfVa, kLinkerElf)
        && boundary_matches(base, kLinkerNoteVa, kLinkerNote)
        && boundary_matches(base, kMutexLockVa, kMutexLock)
        && boundary_matches(base, kMutexUnlockVa, kMutexUnlock)
        && boundary_matches(base, kMutexSlowLockVa, kMutexSlowLock)
        && boundary_matches(base, kDlsymImplVa, kDlsymImpl)
        && boundary_matches(base, kDlopenExtVa, kDlopenExt);
}

bool verified_flutter(uintptr_t base) {
    using namespace hyos_snapshot_guard;
    return boundary_matches(base, kFlutterElfVa, kFlutterElf)
        && boundary_matches(base, kFlutterNoteVa, kFlutterNote)
        && boundary_matches(base, kSearchMappingVa, kSearchMapping)
        && boundary_matches(base, kSymbolMappingVa, kSymbolMapping)
        && boundary_matches(base, kVmFromSettingsVa, kVmFromSettings)
        && boundary_matches(base, kIsolateFromSettingsVa, kIsolateFromSettings);
}

bool loader_observed_unlocked(uintptr_t base) {
    using namespace hyos_snapshot_guard;
    if (base == 0 || base > UINTPTR_MAX - kMutexVa) return false;
    std::array<uint32_t, 2> first{}, second{};
    // This exact linker's recursive state is uint16_t at +0, reserved at +2,
    // owner TID at +4. Inner unlock retains owner under an outer recursive hold.
    // Observing free twice is deliberately conservative; it does not reserve
    // the loader against another thread. The executor must not acquire it.
    return boundary_read(base + kMutexVa, first.data(), sizeof(first))
        && boundary_read(base + kMutexVa, second.data(), sizeof(second))
        && first == second && first[0] == 0x4000u && first[1] == 0;
}

void bind_search_mapping(const char *name, void *handle, const void *caller) {
    using namespace hyos_snapshot_guard;
    if (handle == nullptr || startup_route() == StartupRoute::None || !g_startup_armed.load(std::memory_order_acquire)
        || !named(name, "libapp.so")) return;
    const uintptr_t source = reinterpret_cast<uintptr_t>(caller);
    if (source < kSearchDlopenReturnVa) return;
    const uintptr_t base = source - kSearchDlopenReturnVa;
    // An unrelated libapp load must not consume the one SearchMapping binding.
    if (!verified_flutter(base)) return;
    unsigned absent = 0;
    if (!g_search_binding.compare_exchange_strong(absent, 1, std::memory_order_acq_rel)) return;
    g_search_flutter_base = base;
    g_search_handle = handle; // Opaque token, never an ELF address.
    g_search_tid = static_cast<pid_t>(syscall(SYS_gettid));
    g_search_binding.store(2, std::memory_order_release);
}

int snapshot_index(const char *name) {
    using namespace hyos_snapshot_guard;
    if (name == nullptr) return -1;
    for (size_t i = 0; i < kSnapshotNames.size(); ++i) {
        // Non-underscored success is incompatible with this inspected Dart ELF.
        if (std::strcmp(name, kSnapshotNames[i]) == 0) return static_cast<int>(i);
        if (std::strcmp(name, kSnapshotNames[i] + 1) == 0) return 4 + static_cast<int>(i);
    }
    return -1;
}

void observe_snapshot(void *handle, const char *symbol, void *target, const void *caller) {
    using namespace hyos_snapshot_guard;
    using namespace hyos_startup_reason;
    const StartupRoute route = startup_route();
    if (target == nullptr || route == StartupRoute::None || !g_startup_armed.load(std::memory_order_acquire)) return;
    const int index = snapshot_index(symbol);
    if (index < 0) return;
    unsigned unseen = 0;
    if (!g_snapshot_attempt.compare_exchange_strong(unseen, 1, std::memory_order_acq_rel)) {
        // A second successful snapshot can otherwise outrun the pre-read
        // window. Cancel validation, or notify the executor if already entered.
        // Its own phase must reject/abort a late commit after this notification.
        for (unsigned retry = 0; retry < 2 && (unseen == 1 || unseen == 4); ++retry) {
            if (g_snapshot_attempt.compare_exchange_strong(unseen, 3, std::memory_order_acq_rel)) {
                snapshot_missed(route, kStartupReentered);
                break;
            }
        }
        return;
    }
    uint32_t failure = 0;
    const uintptr_t linker = g_linker_base.load(std::memory_order_acquire);
    const uintptr_t source = reinterpret_cast<uintptr_t>(caller);
    const uintptr_t result = reinterpret_cast<uintptr_t>(target);
    const uintptr_t vm_data_va = route == StartupRoute::Home ? kVmDataVa : weather_guard::kDartVmDataVa;
    uintptr_t dart = 0;
    if (index != 0) failure = kUnexpectedFirstSnapshot;
    else if (linker == 0 || !verified_linker(linker)) failure = kUnknownLinker;
    else if (source < kSnapshotDlsymReturnVa || !verified_flutter(source - kSnapshotDlsymReturnVa))
        failure = kFlutterGuardRejected;
    else if (g_search_binding.load(std::memory_order_acquire) != 2
        || g_search_flutter_base != source - kSnapshotDlsymReturnVa
        || g_search_handle != handle || g_search_tid != static_cast<pid_t>(syscall(SYS_gettid)))
        failure = kUnboundHandleOrThread;
    else if (result < vm_data_va
        || !(route == StartupRoute::Home
            ? boundary_matches(result - vm_data_va, kDartIdentityVa, kDartIdentity)
            : boundary_matches(result - vm_data_va, 0, weather_guard::kDartBytes0)))
        failure = kDartGuardRejected;
    else if (!loader_observed_unlocked(linker)) failure = kLoaderBusyOrUnreadable;
    else dart = result - vm_data_va;
    // No logging, worker start or preference query is permitted here. The
    // controller consumes only an already-published coherent cache and executes
    // a bounded static plan; if preparation is late, it records a missed window.
    unsigned checking = 1;
    if (g_snapshot_attempt.compare_exchange_strong(checking, 4, std::memory_order_acq_rel)) {
        if (failure != 0) snapshot_missed(route, failure);
        else if (route == StartupRoute::Home) hyos_control::on_first_home_snapshot(dart);
        else weather_runtime::on_first_card_snapshot(dart, source - kSnapshotDlsymReturnVa);
    }
    g_snapshot_attempt.store(2, std::memory_order_release);
}

void *hooked_dlsym(void *handle, const char *symbol, const void *caller) {
    if (g_dlsym == nullptr) return nullptr;
    void *target = g_dlsym(handle, symbol, caller);
    const int saved_errno = errno;
    void *result = target;
    if (target != nullptr && (home_child() || weather_child()) && symbol != nullptr
        && std::strcmp(symbol, "app_entry_point") == 0) result = consider_entry(target, caller);
    else observe_snapshot(handle, symbol, target, caller);
    if (weather_child()) weather_runtime::observe_snapshot(symbol, target);
    // Keep the original result, namespace caller and loader error state. No
    // dlerror()/extra dlsym is issued by the snapshot observer.
    errno = saved_errno;
    return result;
}

void record_library(const char *name, void *handle) {
    if (handle != nullptr && home_child()
        && (named(name, "libapp_launcher.so") || named(name, "libapp.so"))) {
        hyperceiler_hyos_record_home_library(name);
    }
}

void *hooked_dlopen(const char *name, int flags, const void *caller) {
    if (g_dlopen == nullptr) return nullptr;
    void *handle = g_dlopen(name, flags, caller);
    const int saved_errno = errno;
    if (handle != nullptr) weather_card_lifetime::observe_parent_load(name);
    bind_search_mapping(name, handle, caller);
    record_library(name, handle);
    errno = saved_errno;
    return handle;
}

void *hooked_android_dlopen(const char *name, int flags, const android_dlextinfo *ext,
                          const void *caller) {
    if (g_android_dlopen == nullptr) return nullptr;
    void *handle = g_android_dlopen(name, flags, ext, caller);
    const int saved_errno = errno;
    if (handle != nullptr) weather_card_lifetime::observe_parent_load(name);
    bind_search_mapping(name, handle, caller);
    record_library(name, handle);
    errno = saved_errno;
    return handle;
}

void app_specialized(const ZnHyosAppSpecializeArgs *args) {
    // UID/GID/SELinux already applied. Do not retain strings, take locks,
    // allocate, read preferences, or start a worker in this post-fork callback.
    // The Weather page observer uses only bounded reads, atomics and syscalls
    // here, before the vendor app runtime starts its worker/Binder threads.
    const bool home = args != nullptr && args->package_name != nullptr
        && args->process_name != nullptr && args->se_info != nullptr
        && args->se_info[0] != '\0'
        && std::strcmp(args->package_name, "com.miui.home") == 0
        && std::strcmp(args->process_name, "com.miui.home") == 0;
    g_home_child.store(home, std::memory_order_release);
    const bool weather = args != nullptr && args->package_name != nullptr
        && args->process_name != nullptr && args->se_info != nullptr && args->se_info[0] != '\0'
        && std::strcmp(args->package_name, "com.miui.weather2") == 0
        && std::strcmp(args->process_name, "com.miui.weather2") == 0;
    g_weather_child.store(weather, std::memory_order_release);
    weather_card_lifetime::after_specialization(weather);
}
const ZygiskNextHyosModule kHyosModule{ZYGISK_NEXT_HYOS_API_VERSION, app_specialized};

bool install_loader(void *target, void *replacement, void **original) {
    if (target == nullptr) return false;
    const int result = g_api->inlineHook(target, replacement, original);
    if (result == ZN_SUCCESS && *original != nullptr) return true;
    if (result == ZN_SUCCESS && g_api->inlineUnhook(target) != ZN_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
            "FATAL backend contract violation: missing continuation and rollback failed");
    }
    return false;
}

void remember_linker_target(void *loader_symbol) {
    const uintptr_t address = reinterpret_cast<uintptr_t>(loader_symbol);
    if (address < hyos_snapshot_guard::kLoaderDlsymVa) return;
    const uintptr_t base = address - hyos_snapshot_guard::kLoaderDlsymVa;
    if (!verified_linker(base)) return;
    uintptr_t absent = 0;
    g_linker_base.compare_exchange_strong(absent, base, std::memory_order_acq_rel);
}

void module_loaded(void *, const ZygiskNextAPI *api) {
    if (api == nullptr || api->getRuntime == nullptr || api->inlineHook == nullptr
        || api->inlineUnhook == nullptr) return;
    const auto *runtime = api->getRuntime();
    if (runtime == nullptr || runtime->type != ZN_RUNTIME_HYOS
        || runtime->api_version < ZYGISK_NEXT_HYOS_API_VERSION
        || runtime->registerModule == nullptr) return;
    g_api = api;
    // Installed before the spawner's main/fork/thread activity. They remain
    // pass-through until this process receives an exact Home specialization.
    void *loader_symbol = dlsym(RTLD_DEFAULT, "__loader_dlsym");
    remember_linker_target(loader_symbol);
    const bool plain = install_loader(dlsym(RTLD_DEFAULT, "__loader_dlopen"),
        reinterpret_cast<void *>(hooked_dlopen), reinterpret_cast<void **>(&g_dlopen));
    const bool android = install_loader(dlsym(RTLD_DEFAULT, "__loader_android_dlopen_ext"),
        reinterpret_cast<void *>(hooked_android_dlopen), reinterpret_cast<void **>(&g_android_dlopen));
    const bool symbol = install_loader(loader_symbol,
        reinterpret_cast<void *>(hooked_dlsym), reinterpret_cast<void **>(&g_dlsym));
    if (!plain || !android || !symbol) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
            "Hyos registration refused: dlopen=%d android=%d dlsym=%d", plain, android, symbol);
        return;
    }
    const bool registered = runtime->registerModule(&kHyosModule) == ZN_SUCCESS;
    g_registered.store(registered, std::memory_order_release);
    __android_log_print(registered ? ANDROID_LOG_INFO : ANDROID_LOG_ERROR, kTag,
        "Hyos API 1 registration=%d; waiting for verified Home or Weather app_entry_point", registered);
}
} // namespace

extern "C" void *hc_hyos_prepare_app_entry(uintptr_t caller) {
    void *target = g_app_entry.load(std::memory_order_acquire);
    const uintptr_t base = g_runtime_base.load(std::memory_order_acquire);
    if (target != nullptr && home_child() && base <= UINTPTR_MAX - hyos_guard::kEntryReturnVa
        && caller == base + hyos_guard::kEntryReturnVa && verified_runtime(base)) {
        g_startup_armed.store(true, std::memory_order_release);
        hyperceiler_hyos_record_home_library("libapp_launcher.so");
        const bool ready = hyperceiler_hyos_start_home_control(g_api);
        const auto address = reinterpret_cast<uintptr_t>(target);
        if (address >= hyos_guard::kHomeEntryVa) {
            const bool recents = home_recents::prepare_before_entry(address - hyos_guard::kHomeEntryVa,
                hyos_control::read_snapshot, pin_home_recents_image);
            const auto status = home_recents::status();
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "Recents pre-entry installed=%d phase=%u error=%u owned=%d",
                recents, static_cast<unsigned>(status.phase), static_cast<unsigned>(status.error), status.owned);
        }
        __android_log_print(ready ? ANDROID_LOG_INFO : ANDROID_LOG_WARN, kTag,
            "verified Home app entry; initial preferences ready=%d (bounded wait)", ready);
    }
    if (target != nullptr && weather_child() && base <= UINTPTR_MAX - hyos_guard::kEntryReturnVa
        && caller == base + hyos_guard::kEntryReturnVa && verified_runtime(base)) {
        const auto address = reinterpret_cast<uintptr_t>(target);
        if (address >= weather_guard::kWeatherEntryVa) {
            const bool ready = weather_runtime::start_before_entry(address - weather_guard::kWeatherEntryVa);
            // Only an already-prepared card operation arms the first-snapshot
            // path. Existing level/material observations remain independent.
            g_startup_armed.store(weather_runtime::card_startup_pending(), std::memory_order_release);
            __android_log_print(ready ? ANDROID_LOG_INFO : ANDROID_LOG_WARN, kTag,
                "verified Weather app entry; startup result=%d (bounded wait)", ready);
        }
    }
    return target;
}

extern "C" {
[[gnu::visibility("default"), gnu::used]]
ZygiskNextModule zn_module{ZYGISK_NEXT_API_VERSION, module_loaded};
}
