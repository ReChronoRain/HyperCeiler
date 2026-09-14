/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <android/log.h>
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <dlfcn.h>
#include <poll.h>
#include <sys/eventfd.h>
#include <time.h>

extern "C" {
// Low two bits carry scene: 0=unrelated, 1=recents, 2=home return.
// Removing two mantissa bits loses < 1e-15, well below a physical pixel.
alignas(8) std::atomic<uint64_t> dock_motion_value{0x3ff0000000000000ULL};
alignas(8) std::atomic<uint64_t> dock_motion_entry_hits{0};
alignas(8) std::atomic<uint64_t> dock_motion_publish_hits{0};
// AUTO_AIM deliberately owns a second coalescing slot. The unlock setter and the established
// recents setters execute in the same Flutter frame; sharing dock_motion_value let a trailing
// scene-0/2 write erase scene 3 before either the transport thread or WMS's 8 ms tick observed it.
alignas(8) std::atomic<uint64_t> dock_auto_aim_value{0x3ff0000000000003ULL};
alignas(8) std::atomic<uint64_t> dock_auto_aim_entry_hits{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_publish_hits{0};
// Filter-stage counters. Each names one hop of the decoded state -> widget -> cell -> container
// chain, so a rejected projection is diagnosable from the periodic pipeline log.
alignas(8) std::atomic<uint64_t> dock_auto_aim_receiver_bad{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_widget_bad{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_cell_bad{0};
alignas(8) std::atomic<uint64_t> dock_auto_aim_container_miss{0};
alignas(8) std::atomic<uint64_t> dock_motion_active_callbacks{0};
alignas(4) std::atomic<uint32_t> dock_motion_subscribed{0};
int dock_motion_event = -1;
extern const uint64_t dock_motion_one = 1;
}
static_assert(std::atomic<uint64_t>::is_always_lock_free && sizeof(std::atomic<uint64_t>) == 8);
static_assert(std::atomic<uint32_t>::is_always_lock_free && sizeof(std::atomic<uint32_t>) == 4);

namespace {
constexpr char kTag[] = "HyperCeiler.DockNative";
constexpr transaction_code_t kMotionTransaction = 0x0048434A;
constexpr transaction_code_t kAutoAimTransaction = 0x0048434B;
constexpr int32_t kMotionAck = 0x48434B32;
constexpr int32_t kMotionAckRevalidate = 0x48434B33;
constexpr char kWindowDescriptor[] = "android.view.IWindowManager";
std::atomic<uint64_t> motion_sequence{0};
std::atomic<uint64_t> auto_aim_sequence{0};

void retry_delay() {
    timespec delay{0, 500000000};
    while (nanosleep(&delay, &delay) != 0 && errno == EINTR) {}
}

bool clock_ns(clockid_t clock, uint64_t &value) {
    timespec now{};
    if (clock_gettime(clock, &now) != 0) return false;
    value = static_cast<uint64_t>(now.tv_sec) * 1000000000ULL + now.tv_nsec;
    return true;
}

struct Sample {
    uint64_t sequence;
    uint64_t uptime_ns;
    uint64_t value;
    uint64_t entry_hits;
    uint64_t publish_hits;
};

bool current_sample(Sample &sample) {
    uint64_t now = 0;
    if (!clock_ns(CLOCK_MONOTONIC, now)) return false;
    sample.sequence = motion_sequence.fetch_add(1, std::memory_order_relaxed) + 1;
    sample.uptime_ns = now;
    // The replacement publishes packed motion before incrementing publishHits.
    // Retry around that release sequence so a parcel never pairs a new counter
    // with the preceding packed value.
    uint64_t before;
    do {
        before = dock_motion_publish_hits.load(std::memory_order_acquire);
        sample.value = dock_motion_value.load(std::memory_order_acquire);
        sample.publish_hits = dock_motion_publish_hits.load(std::memory_order_acquire);
    } while (before != sample.publish_hits);
    sample.entry_hits = dock_motion_entry_hits.load(std::memory_order_acquire);
    return true;
}

bool current_auto_aim_sample(Sample &sample) {
    uint64_t now = 0;
    if (!clock_ns(CLOCK_MONOTONIC, now)) return false;
    sample.sequence = auto_aim_sequence.fetch_add(1, std::memory_order_relaxed) + 1;
    sample.uptime_ns = now;
    uint64_t before;
    do {
        before = dock_auto_aim_publish_hits.load(std::memory_order_acquire);
        sample.value = dock_auto_aim_value.load(std::memory_order_acquire);
        sample.publish_hits = dock_auto_aim_publish_hits.load(std::memory_order_acquire);
    } while (before != sample.publish_hits);
    sample.entry_hits = dock_auto_aim_entry_hits.load(std::memory_order_acquire);
    return true;
}

void report_auto_aim_sample(const Sample &sample, unsigned &reported) {
    if (reported >= 12) return;
    const uint64_t scalar_bits = sample.value & ~3ULL;
    double scale = 1.0;
    std::memcpy(&scale, &scalar_bits, sizeof(scale));
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "auto aim projection scale=%.6f publish=%llu entry=%llu",
        scale, static_cast<unsigned long long>(sample.publish_hits),
        static_cast<unsigned long long>(sample.entry_hits));
    ++reported;
}

void *binder_on_create(void *) {
    return nullptr;
}

void binder_on_destroy(void *) {}

binder_status_t binder_on_transact(AIBinder *, transaction_code_t,
    const AParcel *, AParcel *) {
    return STATUS_UNKNOWN_TRANSACTION;
}

const AIBinder_Class *window_manager_class() {
    static AIBinder_Class *clazz = AIBinder_Class_define(kWindowDescriptor,
        binder_on_create, binder_on_destroy, binder_on_transact);
    return clazz;
}

class WindowBinderTransport {
public:
    ~WindowBinderTransport() {
        if (window_ != nullptr) AIBinder_decStrong(window_);
    }

    bool connect() {
        // Service-manager lookup is a platform extension omitted from the app
        // NDK headers, but exported by the same libbinder_ndk already used by
        // the OS4 launcher. Resolve the symbol, never a library/address offset.
        using GetService = AIBinder *(*)(const char *instance);
        const auto get_service = reinterpret_cast<GetService>(
            dlsym(RTLD_DEFAULT, "AServiceManager_getService"));
        if (get_service == nullptr) return false;
        window_ = get_service("window");
        if (window_ == nullptr) return false;

        const AIBinder_Class *clazz = AIBinder_getClass(window_);
        if (clazz != nullptr) {
            const char *descriptor = AIBinder_Class_getDescriptor(clazz);
            return descriptor != nullptr && std::strcmp(descriptor, kWindowDescriptor) == 0;
        }
        clazz = window_manager_class();
        return clazz != nullptr && AIBinder_associateClass(window_, clazz);
    }

    enum class SendResult { failed, acknowledged, revalidate };

    SendResult send(const Sample &sample,
                    transaction_code_t code = kMotionTransaction) {
        AParcel *input = nullptr;
        if (AIBinder_prepareTransaction(window_, &input) != STATUS_OK || input == nullptr) {
            return SendResult::failed;
        }
        if (AParcel_writeInt64(input, static_cast<int64_t>(sample.sequence)) != STATUS_OK
            || AParcel_writeInt64(input, static_cast<int64_t>(sample.uptime_ns)) != STATUS_OK
            || AParcel_writeInt64(input, static_cast<int64_t>(sample.value)) != STATUS_OK
            || AParcel_writeInt64(input, static_cast<int64_t>(sample.entry_hits)) != STATUS_OK
            || AParcel_writeInt64(input, static_cast<int64_t>(sample.publish_hits)) != STATUS_OK) {
            AParcel_delete(input);
            return SendResult::failed;
        }
        AParcel *output = nullptr;
        const binder_status_t status = AIBinder_transact(window_, code,
            &input, &output, 0);
        int32_t acknowledgment = 0;
        const bool replied = status == STATUS_OK && output != nullptr
            && AParcel_readInt32(output, &acknowledgment) == STATUS_OK;
        if (output != nullptr) AParcel_delete(output);
        if (!replied) return SendResult::failed;
        if (acknowledgment == kMotionAckRevalidate) return SendResult::revalidate;
        return acknowledgment == kMotionAck ? SendResult::acknowledged : SendResult::failed;
    }

private:
    AIBinder *window_ = nullptr;
};
} // namespace

bool prepare_dock_motion() {
    dock_motion_event = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
    if (dock_motion_event < 0) return false;
    // The established recents bank is installed transactionally. AUTO_AIM is
    // an independent optional fourth hook and cannot take that channel down.
    dock_motion_subscribed.store(0, std::memory_order_release);
    return true;
}

void activate_dock_motion() {
    if (dock_motion_subscribed.exchange(1, std::memory_order_acq_rel) == 0
        && dock_motion_event >= 0) {
        // A gesture may have updated the coalesced sample while hooks were being
        // repaired. Wake the sender as soon as subscription is restored instead
        // of making the next gesture wait for the poll timeout.
        (void)eventfd_write(dock_motion_event, 1);
    }
}

void deactivate_dock_motion() {
    dock_motion_subscribed.store(0, std::memory_order_release);
}

// The Dock preference, pushed from the launcher-side Java. Fail-open: it starts true, so a missing
// or stale push keeps the previous behaviour instead of silently disabling real-time following.
std::atomic<bool> dock_motion_feature_enabled{true};

void set_dock_motion_feature_enabled(bool enabled) {
    const bool previous = dock_motion_feature_enabled.exchange(enabled, std::memory_order_acq_rel);
    if (previous != enabled) {
        __android_log_print(ANDROID_LOG_INFO, kTag, "motion feature %s by preference",
            enabled ? "enabled" : "disabled");
        // Wake the sender so a re-enable resumes on the next frame instead of the next poll.
        if (enabled && dock_motion_event >= 0) (void)eventfd_write(dock_motion_event, 1);
    }
}

bool dock_motion_feature_active() {
    return dock_motion_feature_enabled.load(std::memory_order_acquire);
}

uint64_t active_dock_motion_callbacks() {
    return dock_motion_active_callbacks.load(std::memory_order_acquire);
}

bool revalidate_dock_motion_hooks();

void run_dock_motion() {
    const int event = dock_motion_event;
    if (event < 0) return;
    constexpr int kPollMs = 1000;
    constexpr uint64_t kKeepAliveNs = 5000000000ULL;
    constexpr uint64_t kRevalidationRetryNs = 750000000ULL;
    constexpr uint64_t kSuspendGapNs = 5000000000ULL;
    unsigned reconnects = 0;
    bool unavailable_reported = false;
    // Persist across Binder reconnects. An old projection must never be re-timestamped as a new
    // unlock sample merely because system_server replaced the endpoint.
    uint64_t last_auto_aim_value = dock_auto_aim_value.load(std::memory_order_acquire);
    uint64_t last_auto_aim_publish_hits = 0;
    unsigned auto_aim_reports = 0;
    for (;;) {
        WindowBinderTransport transport;
        Sample sample{};
        const auto initial = transport.connect() && current_sample(sample)
            ? transport.send(sample) : WindowBinderTransport::SendResult::failed;
        if (initial == WindowBinderTransport::SendResult::failed) {
            if (!unavailable_reported) {
                __android_log_print(ANDROID_LOG_WARN, kTag,
                    "motion Binder transport unavailable; retrying in background");
                unavailable_reported = true;
            }
            retry_delay();
            continue;
        }
        Sample auto_aim{};
        if (current_auto_aim_sample(auto_aim)
            && auto_aim.publish_hits != last_auto_aim_publish_hits) {
            const auto sent = transport.send(auto_aim, kAutoAimTransaction);
            if (sent == WindowBinderTransport::SendResult::failed) {
                ++reconnects;
                retry_delay();
                continue;
            }
            last_auto_aim_value = auto_aim.value;
            last_auto_aim_publish_hits = auto_aim.publish_hits;
            report_auto_aim_sample(auto_aim, auto_aim_reports);
        }
        bool revalidation_pending = initial == WindowBinderTransport::SendResult::revalidate;
        uint64_t last_revalidation_ns = 0;
        if (revalidation_pending) {
            revalidate_dock_motion_hooks();
            last_revalidation_ns = sample.uptime_ns;
        }

        unavailable_reported = false;
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "motion v33 ready: semantic native scale + independent Hotseat projection reconnect=%u",
            reconnects);

        bool disconnected = false;
        uint64_t last_value = sample.value;
        uint64_t last_publish_hits = sample.publish_hits;
        uint64_t last_sent_ns = sample.uptime_ns;
        uint64_t last_boot_ns = 0;
        uint64_t last_mono_ns = 0;
        if (!clock_ns(CLOCK_BOOTTIME, last_boot_ns)
            || !clock_ns(CLOCK_MONOTONIC, last_mono_ns)) {
            // Never terminate the transport on a clock failure. Returning here used to
            // end run_dock_motion() for the whole launcher process, and nothing re-armed
            // it, so real-time following stayed dead until the desktop was restarted.
            // Rebuild the transport instead and keep the worker alive.
            ++reconnects;
            retry_delay();
            continue;
        }
        while (!disconnected) {
            pollfd descriptor{event, POLLIN, 0};
            int result;
            do {
                result = poll(&descriptor, 1, kPollMs);
            } while (result < 0 && errno == EINTR);
            if (result < 0) {
                disconnected = true;
                continue;
            }
            uint64_t boot_ns = 0;
            uint64_t mono_ns = 0;
            if (!clock_ns(CLOCK_BOOTTIME, boot_ns)
                || !clock_ns(CLOCK_MONOTONIC, mono_ns)) {
                disconnected = true;
                continue;
            }
            const uint64_t boot_delta = boot_ns >= last_boot_ns
                ? boot_ns - last_boot_ns : UINT64_MAX;
            const uint64_t mono_delta = mono_ns >= last_mono_ns
                ? mono_ns - last_mono_ns : UINT64_MAX;
            if (boot_delta == UINT64_MAX || mono_delta == UINT64_MAX) {
                disconnected = true;
                continue;
            }
            // A short suspend/resume cycle is normal while the screen is off; the device
            // was observed resuming every ~10-20 s. Tearing the transport down here used
            // to cost 0.4-3.7 s per cycle with no sample delivered, which is exactly the
            // window in which the first post-unlock swipe lost its real-time follow.
            // The Binder proxy survives suspend, so rebase the clock baseline, force one
            // immediate publish, and keep the connection. A genuinely stale proxy is
            // still caught by the failing send below, which rebuilds it.
            const bool suspended = boot_delta > mono_delta
                && boot_delta - mono_delta > kSuspendGapNs;
            last_boot_ns = boot_ns;
            last_mono_ns = mono_ns;
            if (descriptor.revents & POLLIN) {
                eventfd_t count = 0;
                if (eventfd_read(event, &count) != 0) {
                    disconnected = true;
                    continue;
                }
            }
            if (!current_sample(sample)) {
                disconnected = true;
                continue;
            }
            if (!current_auto_aim_sample(auto_aim)) {
                disconnected = true;
                continue;
            }
            if (suspended) {
                __android_log_print(ANDROID_LOG_INFO, kTag,
                    "device resume detected; reusing motion Binder transport");
            }
            if (!dock_motion_feature_enabled.load(std::memory_order_acquire)) {
                // Preference off: publish nothing and leave the baselines untouched, so the first
                // sample after re-enabling looks changed and following resumes immediately. The
                // transport and the suspend handling above keep running either way.
                continue;
            }
            const bool changed = suspended || sample.value != last_value
                || sample.publish_hits != last_publish_hits;
            const bool auto_aim_changed = auto_aim.publish_hits != last_auto_aim_publish_hits
                || (auto_aim.publish_hits != 0 && auto_aim.value != last_auto_aim_value);
            const bool keep_alive = !changed
                && (sample.uptime_ns - last_sent_ns) >= kKeepAliveNs;
            const bool revalidation_probe = revalidation_pending
                && sample.uptime_ns - last_sent_ns >= kRevalidationRetryNs;
            if (changed || keep_alive || revalidation_probe) {
                const auto sent = transport.send(sample);
                if (sent == WindowBinderTransport::SendResult::failed) {
                    disconnected = true;
                    continue;
                }
                if (sent == WindowBinderTransport::SendResult::revalidate) {
                    revalidation_pending = true;
                    if (sample.uptime_ns - last_revalidation_ns >= kRevalidationRetryNs) {
                        revalidate_dock_motion_hooks();
                        last_revalidation_ns = sample.uptime_ns;
                    }
                } else {
                    revalidation_pending = false;
                }
                last_value = sample.value;
                last_publish_hits = sample.publish_hits;
                last_sent_ns = sample.uptime_ns;
            }
            if (!disconnected && auto_aim_changed) {
                const auto sent = transport.send(auto_aim, kAutoAimTransaction);
                if (sent == WindowBinderTransport::SendResult::failed) {
                    disconnected = true;
                    continue;
                }
                last_auto_aim_value = auto_aim.value;
                last_auto_aim_publish_hits = auto_aim.publish_hits;
                report_auto_aim_sample(auto_aim, auto_aim_reports);
            }
        }

        if (reconnects < 3) {
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "motion Binder transport disconnected; reconnecting");
        }
        ++reconnects;
        retry_delay();
    }
}
