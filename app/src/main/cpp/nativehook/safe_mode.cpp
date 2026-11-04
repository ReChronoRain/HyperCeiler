/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "nativehook/safe_mode.h"

#include <atomic>
#include <cstring>
#include <unistd.h>

#if defined(__ANDROID__)
#include <sys/system_properties.h>
#endif

namespace nhk::home_safe_mode {
namespace {

constexpr char kProperty[] = "persist.service.hyperceiler.crash.report";

// Java's CrashScope currently has exactly these aliases. Keep parsing strict:
// accepting arbitrary tokens would let a malformed value look like normal.
constexpr const char *kAliases[] = {"systemui", "settings", "home", "center", "demo"};
#if defined(__ANDROID__)
constexpr std::size_t kMaxValue = PROP_VALUE_MAX;
#else
constexpr std::size_t kMaxValue = 92;
#endif

std::atomic<Reader> g_reader{nullptr};
std::atomic<pid_t> g_cached_pid{-1};
std::atomic<int> g_cached_state{static_cast<int>(State::unreadable)};
pid_t g_test_pid = -1;

bool production_reader(const char *name, char *value, std::size_t capacity) noexcept {
#if defined(__ANDROID__)
    if (name == nullptr || value == nullptr || capacity == 0) return false;
    struct ReadContext {
        char *value;
        std::size_t capacity;
        bool ok;
    } context{value, capacity, false};

    const prop_info *property = __system_property_find(name);
    if (property == nullptr) {
        // Missing and empty both mean recovered/normal in the Java implementation.
        value[0] = '\0';
        return true;
    }
    __system_property_read_callback(
        property,
        [](void *cookie, const char *, const char *read_value, uint32_t) {
            auto *read = static_cast<ReadContext *>(cookie);
            if (read_value == nullptr) return;
            const std::size_t length = strnlen(read_value, read->capacity);
            if (length >= read->capacity) return;
            std::memcpy(read->value, read_value, length + 1);
            read->ok = true;
        },
        &context);
    if (!context.ok) return false;
    return true;
#else
    (void)name;
    (void)value;
    (void)capacity;
    return false;
#endif
}

bool known_alias(const char *begin, std::size_t length) noexcept {
    for (const char *alias : kAliases) {
        if (std::strlen(alias) == length && std::strncmp(alias, begin, length) == 0) return true;
    }
    return false;
}

bool is_space(unsigned char c) noexcept {
    return c == ' ' || c == '\t' || c == '\r' || c == '\n';
}

pid_t current_pid() noexcept {
    return g_test_pid == -1 ? getpid() : g_test_pid;
}

} // namespace

Decision parse(const char *value) noexcept {
    if (value == nullptr) return {State::corrupt, true};
    if (value[0] == '\0') return {State::missing, false};

    const char *token = value;
    bool saw_home = false;
    bool saw_token = false;
    for (;;) {
        const char *comma = std::strchr(token, ',');
        const char *end = comma == nullptr ? token + std::strlen(token) : comma;
        const char *begin = token;
        while (begin < end && is_space(static_cast<unsigned char>(*begin))) ++begin;
        const char *trimmed_end = end;
        while (trimmed_end > begin && is_space(static_cast<unsigned char>(trimmed_end[-1]))) --trimmed_end;
        const std::size_t length = static_cast<std::size_t>(trimmed_end - begin);
        if (length == 0u) return {State::corrupt, true};
        if (!known_alias(begin, length)) return {State::corrupt, true};
        if (length == 4u && std::strncmp(begin, "home", 4) == 0) saw_home = true;
        saw_token = true;
        if (comma == nullptr) break;
        token = comma + 1;
    }
    if (!saw_token) return {State::corrupt, true};
    return saw_home ? Decision{State::safe_mode, true} : Decision{State::normal, false};
}

Decision decision() noexcept {
    const pid_t pid = current_pid();
    if (g_cached_pid.load(std::memory_order_acquire) == pid) {
        const auto state = static_cast<State>(g_cached_state.load(std::memory_order_relaxed));
        return {state, state != State::normal && state != State::missing};
    }

    char value[kMaxValue]{};
    const Reader configured = g_reader.load(std::memory_order_acquire);
    const Reader reader = configured == nullptr ? &production_reader : configured;
    Decision result;
    if (!reader(kProperty, value, sizeof(value))) {
        result = {State::unreadable, true};
    } else if (std::memchr(value, '\0', sizeof(value)) == nullptr) {
        result = {State::corrupt, true};
    } else {
        result = parse(value);
    }
    // Publish the state before the PID. A concurrent first reader may compute
    // again, but it cannot observe a cached PID with an unrelated old state.
    g_cached_state.store(static_cast<int>(result.state), std::memory_order_relaxed);
    g_cached_pid.store(pid, std::memory_order_release);
    return result;
}

void set_reader_for_test(Reader reader) noexcept {
    g_reader.store(reader, std::memory_order_release);
    reset_for_test();
}

void set_pid_for_test(pid_t pid) noexcept {
    g_test_pid = pid;
    reset_for_test();
}

void reset_for_test() noexcept {
    g_cached_pid.store(-1, std::memory_order_release);
    g_cached_state.store(static_cast<int>(State::unreadable), std::memory_order_relaxed);
}

const char *state_name(State state) noexcept {
    switch (state) {
        case State::normal: return "normal";
        case State::safe_mode: return "safe_mode";
        case State::missing: return "missing";
        case State::corrupt: return "corrupt";
        case State::unreadable: return "unreadable";
    }
    return "unknown";
}

} // namespace nhk::home_safe_mode
