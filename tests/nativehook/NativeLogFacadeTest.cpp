/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the bounded native logging facade. The Java sink is injected
 * here; Android-only JNI attachment remains covered by the arm64 NDK build.
 */
#include "nativehook/native_log.h"

#include <cstdio>
#include <cstring>

namespace {

int failures = 0;

#define CHECK(expression) do { \
    if (!(expression)) { \
        std::fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #expression); \
        ++failures; \
    } \
} while (false)

struct SinkState {
    int java_calls = 0;
    int fallback_calls = 0;
    bool java_result = true;
    bool fallback_result = true;
    nhk::native_log::Level level = nhk::native_log::Level::info;
    char tag[nhk::native_log::kMaxTag]{};
    char message[nhk::native_log::kMaxMessage]{};
};

bool java_sink(nhk::native_log::Level level, const char *tag, const char *message, void *context) {
    auto &state = *static_cast<SinkState *>(context);
    ++state.java_calls;
    state.level = level;
    std::snprintf(state.tag, sizeof(state.tag), "%s", tag == nullptr ? "" : tag);
    std::snprintf(state.message, sizeof(state.message), "%s", message == nullptr ? "" : message);
    return state.java_result;
}

bool fallback_sink(nhk::native_log::Level, const char *tag, const char *message, void *context) {
    auto &state = *static_cast<SinkState *>(context);
    ++state.fallback_calls;
    std::snprintf(state.tag, sizeof(state.tag), "%s", tag == nullptr ? "" : tag);
    std::snprintf(state.message, sizeof(state.message), "%s", message == nullptr ? "" : message);
    return state.fallback_result;
}

void reset(SinkState &state) {
    nhk::native_log::reset_for_test();
    state = {};
}

} // namespace

int main() {
    using namespace nhk::native_log;
    SinkState state;

    // The queue is bounded and drops rather than growing without limit.
    reset(state);
    set_rate_limit_for_test(0);
    for (size_t i = 0; i < kCapacity + 17; ++i) write(Level::info, "queue", "entry");
    CHECK(stats().accepted == kCapacity);
    CHECK(stats().dropped == 17);

    // A silent bridge is harmless: messages stay queued until a safe flush with a backend.
    set_sinks_for_test(nullptr, nullptr, &state);
    flush();
    CHECK(stats().delivered_java == 0 && stats().delivered_fallback == 0);

    // Per-level rate limiting is enforced before enqueue.
    reset(state);
    set_rate_limit_for_test(1);
    write(Level::info, "rate", "first");
    write(Level::info, "rate", "second");
    CHECK(stats().accepted == 1);
    CHECK(stats().rate_limited == 1);

    // Normal Java delivery uses the existing bridge and preserves level/text.
    reset(state);
    set_rate_limit_for_test(0);
    set_sinks_for_test(java_sink, fallback_sink, &state);
    writef(Level::warn, "bridge", "value=%d", 7);
    flush();
    CHECK(stats().delivered_java == 1);
    CHECK(stats().delivered_fallback == 0);
    CHECK(state.java_calls == 1 && state.fallback_calls == 0);
    CHECK(state.level == Level::warn);
    CHECK(std::strcmp(state.tag, "bridge") == 0);
    CHECK(std::strcmp(state.message, "value=7") == 0);

    // A failed or unavailable Java bridge falls back and is not called repeatedly.
    reset(state);
    set_rate_limit_for_test(0);
    set_sinks_for_test(java_sink, fallback_sink, &state);
    state.java_result = false;
    write(Level::error, "failure", "first");
    write(Level::error, "failure", "second");
    flush();
    CHECK(stats().java_failures == 1);
    CHECK(stats().delivered_fallback == 2);
    CHECK(state.java_calls == 1 && state.fallback_calls == 2);
    CHECK(std::strcmp(state.message, "second") == 0);

    // Early messages are retained until the Java layer becomes available.
    reset(state);
    set_rate_limit_for_test(0);
    write(Level::debug, "early", "buffered");
    set_sinks_for_test(java_sink, fallback_sink, &state);
    flush();
    CHECK(stats().delivered_java == 1);
    CHECK(std::strcmp(state.message, "buffered") == 0);

    // Overlong strings remain terminated and never exceed their fixed slots.
    reset(state);
    set_rate_limit_for_test(0);
    set_sinks_for_test(java_sink, fallback_sink, &state);
    char long_tag[kMaxTag + 32];
    char long_message[kMaxMessage + 32];
    std::memset(long_tag, 't', sizeof(long_tag) - 1);
    std::memset(long_message, 'm', sizeof(long_message) - 1);
    long_tag[sizeof(long_tag) - 1] = '\0';
    long_message[sizeof(long_message) - 1] = '\0';
    write(Level::info, long_tag, long_message);
    flush();
    CHECK(std::strlen(state.tag) == kMaxTag - 1);
    CHECK(std::strlen(state.message) == kMaxMessage - 1);

    if (failures == 0) std::puts("NativeLogFacadeTest passed");
    return failures == 0 ? 0 : 1;
}
