/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "hyos_entry.h"
#include "hyos_control.h"
#include "hyos_snapshot.h"
#include "hyos_report_runtime.h"
#include "targets/home/native_home_entry.h"
#include "targets/home/home_startup.h"
#include "targets/home/recents_runtime.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <pthread.h>
#include <time.h>
#include <cerrno>
#include <dlfcn.h>
#include <chrono>
#include <condition_variable>
#include <mutex>
#include <limits>

namespace {
constexpr char kDescriptor[] = "android.view.IWindowManager";
constexpr char kTag[] = "HyperCeiler.HyosControl";
constexpr int64_t kLeaseNanos = 5'000'000'000LL;
std::atomic_bool g_active{false};
std::atomic_bool g_worker_started{false};
std::atomic_bool g_watchdog_started{false};
std::atomic_bool g_watchdog_ready{false};
std::atomic_flag g_polling = ATOMIC_FLAG_INIT;
std::atomic<const ZygiskNextAPI *> g_api{nullptr};
std::atomic_uint g_pending_libraries{0};
std::atomic_uint g_flags{0};
std::atomic<int64_t> g_lease_deadline{0};
hyos_snapshot::Cache g_snapshot;
std::atomic<int64_t> g_received_at{0};
std::atomic_bool g_received_available{false};
std::mutex g_publish_mutex;
std::mutex g_completion_mutex;
std::condition_variable g_first_poll;
bool g_poll_completed = false;
home_native::LibraryLoaded g_library_loaded = nullptr;
bool g_initialized = false;
int32_t g_last_flags = -2;
enum class StartupPhase : uint32_t { Pending, Installing, Cancelling, Applied, Declined, Missed, TerminalFailure };
std::atomic<StartupPhase> g_startup_phase{StartupPhase::Pending};
std::atomic_bool g_startup_prepared{false};
std::atomic_bool g_startup_cancelled{false}, g_startup_fault{false};
std::atomic_uint g_startup_reason{0};
std::atomic<uint64_t> g_startup_requested{0}, g_startup_applied{0}, g_startup_declined{0};
std::atomic<home_startup::Error> g_startup_error{home_startup::Error::None};
StartupPhase g_last_startup_phase = StartupPhase::Pending; // control worker only
home_recents::Status g_last_recents_status{home_recents::Phase::Cold,
    home_recents::Error::None, false, 0}; // control worker only

bool startup_finished() {
    const auto phase = g_startup_phase.load(std::memory_order_acquire);
    return phase != StartupPhase::Pending && phase != StartupPhase::Installing
        && phase != StartupPhase::Cancelling;
}

void finish_startup(StartupPhase result) {
    auto expected = StartupPhase::Installing;
    if (!g_startup_phase.compare_exchange_strong(expected, result, std::memory_order_acq_rel)
        && expected == StartupPhase::Cancelling) {
        g_startup_phase.compare_exchange_strong(expected, StartupPhase::TerminalFailure,
            std::memory_order_acq_rel);
    }
}

struct PublicationIdentity { int64_t epoch, revision; };

int64_t boottime_now() {
    timespec time{};
    if (clock_gettime(CLOCK_BOOTTIME, &time) != 0 || time.tv_sec < 0
        || time.tv_nsec < 0 || time.tv_nsec >= 1'000'000'000L
        || time.tv_sec > (std::numeric_limits<int64_t>::max() - 999'999'999) / 1'000'000'000)
        return 0;
    return static_cast<int64_t>(time.tv_sec) * 1'000'000'000LL + time.tv_nsec;
}

// The complete HCS1 frame and its original server deadline are one atomic-word
// publication. Never combine flags from HCY1 with an independent HCL4 read.
bool fresh_flags(unsigned &flags) {
    return g_snapshot.authorization(boottime_now(), flags);
}

bool same_authorization(void *context) {
    const auto *identity = static_cast<const PublicationIdentity *>(context);
    hyos_snapshot::Snapshot current;
    return identity != nullptr && !g_startup_cancelled.load(std::memory_order_acquire)
        && !g_startup_fault.load(std::memory_order_acquire)
        && g_snapshot.load_snapshot(boottime_now(), current)
        && current.available && (current.flags & hyos_control::kAllowed) != 0
        && current.epoch == identity->epoch && current.revision == identity->revision;
}

void remember_result(const home_startup::Result &result) {
    g_startup_requested.store(result.requested, std::memory_order_relaxed);
    g_startup_applied.store(result.applied, std::memory_order_relaxed);
    g_startup_declined.store(result.declined, std::memory_order_relaxed);
    g_startup_error.store(result.error, std::memory_order_relaxed);
}

void service_startup_revoke() {
    const auto result = home_startup::service_revoke();
    if (result.outcome == home_startup::Outcome::TerminalFailure) {
        g_startup_fault.store(true, std::memory_order_release);
        g_startup_error.store(result.error, std::memory_order_release);
        auto phase = g_startup_phase.load(std::memory_order_acquire);
        for (;;) {
            const auto desired = phase == StartupPhase::Installing || phase == StartupPhase::Cancelling
                ? StartupPhase::Cancelling : StartupPhase::TerminalFailure;
            if (g_startup_phase.compare_exchange_weak(phase, desired, std::memory_order_acq_rel)) break;
        }
    }
}

// Caller holds the short publication lock. This function only changes atomics
// in Home/Dock/Report; it must never call Binder, install hooks or wait for I/O.
void apply_options(unsigned flags) {
    const bool enabled = (flags & hyos_control::kAllowed) != 0;
    if (!enabled) {
        home_startup::request_revoke();
        home_recents::cancel();
    }
    hyperceiler_home_runtime_options(enabled && (flags & hyos_control::kHighModels) != 0,
        enabled && (flags & hyos_control::kDisablePrestart) != 0,
        enabled && (flags & hyos_control::kSoftGlass) != 0,
        enabled && (flags & hyos_control::kDock) != 0,
        enabled && (flags & hyos_control::kHideReport) != 0);
}

bool publish_reply(bool received, const hyos_snapshot::Words &words) {
    std::lock_guard lock(g_publish_mutex);
    const auto now = boottime_now();
    hyos_snapshot::Snapshot snapshot;
    const bool ready = received && g_watchdog_ready.load(std::memory_order_acquire)
        && hyos_snapshot::decode(words, now, snapshot) && g_snapshot.publish(words, now);
    if (!ready) g_snapshot.revoke();
    const auto flags = ready ? snapshot.flags : 0;
    g_lease_deadline.store(0, std::memory_order_release);
    g_flags.store(flags, std::memory_order_release);
    if (ready) g_lease_deadline.store(snapshot.valid_until, std::memory_order_release);
    g_received_at.store(now, std::memory_order_release);
    g_received_available.store(ready && snapshot.available, std::memory_order_release);
    apply_options(flags);
    return ready && snapshot.available;
}

void expire_lease() {
    std::lock_guard lock(g_publish_mutex);
    const auto deadline = g_lease_deadline.load(std::memory_order_acquire);
    if (deadline == 0) return;
    const auto now = boottime_now();
    if (now > 0 && now < deadline && deadline - now <= kLeaseNanos) return;
    g_lease_deadline.store(0, std::memory_order_release);
    g_flags.store(0, std::memory_order_release);
    g_snapshot.revoke();
    g_received_available.store(false, std::memory_order_release);
    apply_options(0);
}

void *on_create(void *) { return nullptr; }
void on_destroy(void *) {}
binder_status_t on_transact(AIBinder *, transaction_code_t, const AParcel *, AParcel *) {
    return STATUS_UNKNOWN_TRANSACTION;
}

bool query_snapshot(hyos_snapshot::Words &words) {
    using CheckService = AIBinder *(*)(const char *);
    const auto check_service = reinterpret_cast<CheckService>(
        dlsym(RTLD_DEFAULT, "AServiceManager_checkService"));
    if (check_service == nullptr) return false;
    AIBinder *window = check_service("window");
    if (window == nullptr) return false;
    const AIBinder_Class *clazz = AIBinder_getClass(window);
    bool associated = false;
    if (clazz != nullptr) {
        const char *descriptor = AIBinder_Class_getDescriptor(clazz);
        associated = descriptor != nullptr && std::strcmp(descriptor, kDescriptor) == 0;
    } else {
        static const AIBinder_Class *type = AIBinder_Class_define(kDescriptor,
            on_create, on_destroy, on_transact);
        associated = type != nullptr && AIBinder_associateClass(window, type);
    }
    AParcel *input = nullptr;
    AParcel *output = nullptr;
    binder_status_t status = STATUS_BAD_VALUE;
    if (associated && AIBinder_prepareTransaction(window, &input) == STATUS_OK && input != nullptr) {
        status = AIBinder_transact(window, hyos_snapshot::kTransaction, &input, &output, 0);
    }
    if (input != nullptr) AParcel_delete(input);
    AIBinder_decStrong(window);
    bool valid = status == STATUS_OK && output != nullptr
        && AParcel_getDataSize(output) == hyos_snapshot::kByteCount;
    // Android Parcel aligns both primitives to four bytes. This arm64-only
    // reader consumes the three little-endian int64 fields as paired words.
    // Actual Java writer bytes are covered by the cross-language corpus test.
    for (auto &word : words) {
        if (!valid || AParcel_readInt32(output, &word) != STATUS_OK) { valid = false; break; }
    }
    if (output != nullptr) AParcel_delete(output);
    return valid;
}

void poll_flags() {
    if (g_polling.test_and_set(std::memory_order_acquire)) return;
    const auto *api = g_api.load(std::memory_order_acquire);
    if (api == nullptr) {
        g_polling.clear(std::memory_order_release);
        return;
    }
    hyos_snapshot::Words words{};
    const auto query_started = boottime_now();
    const bool received = query_snapshot(words);
    const auto query_finished = boottime_now();
    // A delayed response must not turn an arbitrarily old server decision into
    // a new lease. A stalled call is retained, not duplicated by new workers.
    const bool timely = received && query_started != 0 && query_finished >= query_started
        && query_finished - query_started < kLeaseNanos;
    const bool ready = publish_reply(timely, words);
    // Same existing worker and full HCS publication. Never renew under the
    // publication mutex or create an independent settings/Binder query lane.
    home_recents::service();
    const auto recents = home_recents::status();
    if (recents.phase != g_last_recents_status.phase || recents.error != g_last_recents_status.error
        || recents.owned != g_last_recents_status.owned) {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Recents phase=%u error=%u owned=%d",
            static_cast<unsigned>(recents.phase), static_cast<unsigned>(recents.error), recents.owned);
        g_last_recents_status = recents;
    }
    const int32_t flags = ready ? static_cast<int32_t>(g_flags.load(std::memory_order_acquire)) : 0;
    if (hyos_control::allowed() && !g_initialized) {
        const home_native::ApiEntries entries{1, api->inlineHook, api->inlineUnhook};
        g_library_loaded = native_init(&entries);
        g_initialized = g_library_loaded != nullptr;
    }
    // Preparation is entirely outside the loader/snapshot observer. Retry a
    // busy try-CAS owner on this worker, without reopening a missed phase.
    if (g_initialized && hyos_control::allowed()
        && !g_startup_prepared.load(std::memory_order_acquire)
        && home_startup::prepare_for_child())
        g_startup_prepared.store(true, std::memory_order_release);
    if (hyos_control::allowed() && g_library_loaded != nullptr) {
        unsigned pending = g_pending_libraries.exchange(0, std::memory_order_acq_rel);
        if ((pending & 1U) != 0 && hyos_control::allowed()) {
            g_library_loaded("libapp_launcher.so", nullptr);
            pending &= ~1U;
        }
        if ((pending & 2U) != 0 && hyos_control::allowed()) {
            g_library_loaded("libapp.so", nullptr);
            pending &= ~2U;
        }
        // If authorization expired during initialization/callbacks, keep the
        // remaining image notifications for a later authoritative recovery.
        if (pending != 0) g_pending_libraries.fetch_or(pending, std::memory_order_acq_rel);
    }
    // The runtime can map Dart without a linker callback. Retry discovery on
    // this existing worker only, after a fresh feature-specific authorization.
    if (startup_finished()) {
        // This is the only maintenance lane. Legacy layout/tweaks workers are
        // excluded from the Hyos build's startup and loader callbacks.
        if (g_startup_phase.load(std::memory_order_acquire) == StartupPhase::Applied) {
            hyos_snapshot::Snapshot snapshot;
            const bool authorized = g_snapshot.load_snapshot(boottime_now(), snapshot)
                && snapshot.available && (snapshot.flags & hyos_control::kAllowed) != 0;
            PublicationIdentity identity{snapshot.epoch, snapshot.revision};
            const auto result = home_startup::maintenance(authorized ? &snapshot.config : nullptr,
                authorized, same_authorization, &identity);
            remember_result(result);
            if (result.outcome == home_startup::Outcome::TerminalFailure) {
                g_startup_fault.store(true, std::memory_order_release);
                g_startup_phase.store(StartupPhase::TerminalFailure, std::memory_order_release);
            }
        }
        service_startup_revoke();
        if (g_initialized && hyos_control::allowed()) hyperceiler_home_after_startup();
    }
    if (startup_finished() && hyos_control::feature_enabled(hyos_control::kHideReport)) {
        hyperceiler_hyos_retry_home_report(api);
    }
    const auto phase = g_startup_phase.load(std::memory_order_acquire);
    if (phase != g_last_startup_phase && phase != StartupPhase::Installing
        && phase != StartupPhase::Cancelling) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "startup phase=%u reason=%u error=%u requested=%llu applied=%llu declined=%llu",
            static_cast<unsigned>(phase), g_startup_reason.load(),
            static_cast<unsigned>(g_startup_error.load()),
            static_cast<unsigned long long>(g_startup_requested.load()),
            static_cast<unsigned long long>(g_startup_applied.load()),
            static_cast<unsigned long long>(g_startup_declined.load()));
        g_last_startup_phase = phase;
    }
    const int32_t state = ready ? flags : -1;
    if (state != g_last_flags) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "authoritative prefs=%d flags=%d native_api=%d", ready, state, g_initialized);
        g_last_flags = state;
    }
    {
        std::lock_guard lock(g_completion_mutex);
        g_poll_completed = true;
    }
    g_first_poll.notify_all();
    g_polling.clear(std::memory_order_release);
}

void sleep_for(long seconds, long nanos) {
    timespec delay{seconds, nanos};
    while (nanosleep(&delay, &delay) != 0 && errno == EINTR) {}
}

void *control_worker(void *) {
    pthread_setname_np(pthread_self(), "hc-hyos-control");
    for (;;) {
        poll_flags();
        sleep_for(1, 500000000);
    }
}

void *lease_watchdog(void *) {
    pthread_setname_np(pthread_self(), "hc-hyos-lease");
    for (;;) {
        sleep_for(0, 250000000);
        expire_lease();
        // A blocked Binder worker must not prevent restoration. The owner uses
        // a try-CAS writer gate and returns immediately while another writer runs.
        service_startup_revoke();
    }
}

bool ensure_thread(std::atomic_bool &started, void *(*body)(void *)) {
    if (started.exchange(true, std::memory_order_acq_rel)) return true;
    pthread_t thread{};
    if (pthread_create(&thread, nullptr, body, nullptr) == 0) {
        if (body == lease_watchdog) g_watchdog_ready.store(true, std::memory_order_release);
        pthread_detach(thread);
        return true;
    }
    started.store(false, std::memory_order_release);
    return false;
}
} // namespace

namespace hyos_control {
bool active() { return g_active.load(std::memory_order_acquire); }
bool feature_enabled(uint32_t flag) {
    if (flag == 0 || (flag & ~kKnownFlags) != 0) return false;
    unsigned flags = 0;
    return fresh_flags(flags) && (flags & kAllowed) != 0 && (flags & flag) == flag;
}
bool allowed() { return feature_enabled(kAllowed); }
bool read_snapshot(hyos_snapshot::Snapshot &result) noexcept {
    return g_snapshot.load_snapshot(boottime_now(), result);
}
bool layout_config(home_layout::Config &result) noexcept {
    hyos_snapshot::Snapshot snapshot;
    const bool ready = g_snapshot.load_snapshot(boottime_now(), snapshot)
        && snapshot.available && (snapshot.flags & kAllowed) != 0;
    result = ready ? snapshot.config : hyos_snapshot::neutral_config();
    return ready;
}

void on_home_snapshot_window_missed(uint32_t reason) noexcept {
    auto expected = StartupPhase::Pending;
    if (g_startup_phase.compare_exchange_strong(expected, StartupPhase::Missed,
            std::memory_order_acq_rel)) {
        g_startup_reason.store(reason, std::memory_order_release);
        home_startup::request_revoke();
    } else if (expected == StartupPhase::Installing || expected == StartupPhase::Cancelling) {
        // Reentry cannot change the first writer's phase or take its lock.
        g_startup_cancelled.store(true, std::memory_order_release);
        home_startup::request_revoke();
        auto installing = StartupPhase::Installing;
        if (!g_startup_phase.compare_exchange_strong(installing, StartupPhase::Cancelling,
                std::memory_order_acq_rel) && installing != StartupPhase::Cancelling) {
            // The owner completed between observing Installing and cancelling.
            // It no longer writes code, so the terminal state is safe to expose.
            g_startup_phase.store(StartupPhase::TerminalFailure, std::memory_order_release);
        }
    }
}

void on_first_home_snapshot(uintptr_t dart_load_bias) noexcept {
    auto expected = StartupPhase::Pending;
    if (!g_startup_phase.compare_exchange_strong(expected, StartupPhase::Installing,
            std::memory_order_acq_rel)) return;
    const auto *api = g_api.load(std::memory_order_acquire);
    hyos_snapshot::Snapshot snapshot;
    const bool prepared = g_startup_prepared.load(std::memory_order_acquire);
    const bool authorized = prepared && api != nullptr
        && g_snapshot.load_snapshot(boottime_now(), snapshot) && snapshot.available
        && (snapshot.flags & kAllowed) != 0;
    if (!authorized) {
        g_startup_error.store(prepared ? home_startup::Error::NotAuthorized
            : home_startup::Error::NotPrepared, std::memory_order_relaxed);
        home_startup::request_revoke();
        finish_startup(StartupPhase::Missed);
        return;
    }
    PublicationIdentity identity{snapshot.epoch, snapshot.revision};
    auto result = home_startup::execute({dart_load_bias, api->inlineHook, api->inlineUnhook,
        &snapshot.config, same_authorization, &identity});
    if (!same_authorization(&identity)) {
        home_startup::request_revoke();
        // The maintenance/watchdog lane will finish any journal restoration;
        // no second installation is allowed at a later snapshot notification.
        if (result.outcome == home_startup::Outcome::Applied)
            result.error = home_startup::Error::Revoked;
    }
    remember_result(result);
    const auto phase = g_startup_fault.load(std::memory_order_acquire)
        || g_startup_cancelled.load(std::memory_order_acquire)
        ? StartupPhase::TerminalFailure
        : result.outcome == home_startup::Outcome::Applied ? StartupPhase::Applied
        : result.outcome == home_startup::Outcome::Missed ? StartupPhase::Missed
        : result.outcome == home_startup::Outcome::TerminalFailure ? StartupPhase::TerminalFailure
        : StartupPhase::Declined;
    // A concurrent cancellation/rollback fault owns its terminal transition.
    // Completion must never overwrite it with a late Applied result.
    finish_startup(phase);
}
}

bool hyperceiler_hyos_start_home_control(const ZygiskNextAPI *api) {
    g_active.store(true, std::memory_order_release);
    if (api == nullptr || api->inlineHook == nullptr || api->inlineUnhook == nullptr) return false;
    const ZygiskNextAPI *expected = nullptr;
    if (!g_api.compare_exchange_strong(expected, api, std::memory_order_acq_rel)
        && expected != api) return false;
    // No replacement thread is created for a stuck Binder call. Both workers
    // live until this Home process exits; never dlclose an active companion.
    if (!ensure_thread(g_watchdog_started, lease_watchdog)
        || !ensure_thread(g_worker_started, control_worker)) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "control workers unavailable; original defaults retained");
        return false;
    }
    std::unique_lock lock(g_completion_mutex);
    const bool completed = g_first_poll.wait_for(lock, std::chrono::milliseconds(150),
        [] { return g_poll_completed; });
    const auto now = boottime_now();
    const auto received_at = g_received_at.load(std::memory_order_acquire);
    hyos_snapshot::Snapshot snapshot;
    const bool ready = completed && g_received_available.load(std::memory_order_acquire)
        && now > 0 && received_at > 0 && now >= received_at
        && now - received_at < kLeaseNanos && g_snapshot.load_snapshot(now, snapshot) && snapshot.available;
    if (!ready) __android_log_print(ANDROID_LOG_INFO, kTag,
        "control not ready at application entry; original defaults retained");
    return ready;
}

void hyperceiler_hyos_record_home_library(const char *name) {
    if (name == nullptr) return;
    const char *slash = std::strrchr(name, '/');
    const char *base = slash == nullptr ? name : slash + 1;
    if (std::strcmp(base, "libapp_launcher.so") == 0) g_pending_libraries.fetch_or(1U);
    if (std::strcmp(base, "libapp.so") == 0) g_pending_libraries.fetch_or(2U);
}

// Compatibility wrappers. New Hyos entry code starts control at app_entry_point,
// and only records images from loader notifications.
void hyperceiler_hyos_before_home_load(const ZygiskNextAPI *api) {
    (void) hyperceiler_hyos_start_home_control(api);
}
void hyperceiler_hyos_after_home_load(const ZygiskNextAPI *, const char *name, void *) {
    hyperceiler_hyos_record_home_library(name);
}
