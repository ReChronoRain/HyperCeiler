/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Entry point for the HyperOS 4 launcher layout targets.
 *
 * The resolver header owns the work; this file only decides *whether* to run it, so the release
 * build stays silent unless someone is investigating. It reads nothing else and patches nothing
 * yet: this stage exists to prove the dynamic lookup finds the same functions the development
 * cross-reference did.
 */
#include "targets/home/home_layout_resolver.h"

#include <android/log.h>
#include <pthread.h>
#include <sys/system_properties.h>

#include <cstdlib>
#include <string_view>

namespace {

constexpr const char *kEnableProperty = "debug.hyperceiler.home_layout_resolve";
constexpr const char *kWindowProperty = "debug.hyperceiler.home_layout_window_ms";

/**
 * How long to keep watching for the launcher's images.
 *
 * The module's native entry runs before the launcher has loaded its Flutter and Rust images, so the
 * first scan cannot see them; this window is what gives the resolver time to notice. A default, not
 * a constant that matters: the property above overrides it, so a session can watch longer without
 * a rebuild.
 */
constexpr uint64_t kDefaultWindowMs = 15 * 1000;
constexpr uint64_t kIntervalMs = 1000;

bool enabled() {
    char value[PROP_VALUE_MAX] = {};
    const int length = __system_property_get(kEnableProperty, value);
    if (length <= 0) return false;
    return std::string_view(value, static_cast<size_t>(length)) != "0";
}

uint64_t window_ms() {
    char value[PROP_VALUE_MAX] = {};
    if (__system_property_get(kWindowProperty, value) <= 0) return kDefaultWindowMs;
    char *end = nullptr;
    const long long parsed = std::strtoll(value, &end, 10);
    if (end == value || *end != '\0' || parsed < 0) return kDefaultWindowMs;
    return static_cast<uint64_t>(parsed);
}

void *scan(void *) {
    __android_log_print(ANDROID_LOG_INFO, home_layout::kLogTag,
        "resolver: images identified by structure and by content, no offsets assumed");
    home_layout::resolve_layout_targets(window_ms(), kIntervalMs);
    return nullptr;
}

} // namespace

void resolve_home_layout_targets() {
    if (!enabled()) return;

    // Off the launcher's critical path: the scan walks every mapped image and arms a signal
    // boundary around its reads, and neither belongs on the thread that is starting the desktop.
    pthread_attr_t attributes{};
    if (pthread_attr_init(&attributes) != 0) return;
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_DETACHED);

    pthread_t worker{};
    const int created = pthread_create(&worker, &attributes, scan, nullptr);
    pthread_attr_destroy(&attributes);
    if (created != 0) {
        __android_log_print(ANDROID_LOG_WARN, home_layout::kLogTag,
            "resolver: worker thread not started (%d)", created);
    }
}
