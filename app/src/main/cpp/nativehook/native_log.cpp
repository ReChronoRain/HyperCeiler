/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "nativehook/native_log.h"

#include <array>
#include <atomic>
#include <ctime>
#include <cstdio>
#include <cstring>
#include <type_traits>

#if defined(__ANDROID__)
#include <android/log.h>
#include <jni.h>
#endif

namespace nhk::native_log {
namespace {

struct Entry {
    std::atomic<size_t> sequence{0};
    Level level = Level::info;
    char tag[kMaxTag]{};
    char message[kMaxMessage]{};
};

struct Ring {
    std::array<Entry, kCapacity> entries;

    Ring() noexcept {
        for (size_t i = 0; i < entries.size(); ++i)
            entries[i].sequence.store(i, std::memory_order_relaxed);
    }
};

Ring g_ring;
std::atomic<size_t> g_enqueue_position{0};
std::atomic<size_t> g_dequeue_position{0};
std::atomic<uint64_t> g_accepted{0};
std::atomic<uint64_t> g_dropped{0};
std::atomic<uint64_t> g_rate_limited{0};
std::atomic<uint64_t> g_delivered_java{0};
std::atomic<uint64_t> g_delivered_fallback{0};
std::atomic<uint64_t> g_java_failures{0};
std::atomic<Sink> g_java_sink{nullptr};
std::atomic<Sink> g_fallback_sink{nullptr};
std::atomic<void *> g_sink_context{nullptr};
std::atomic<bool> g_java_healthy{true};
std::atomic<uint32_t> g_rate_limit{32};

struct Window {
    std::atomic<int64_t> start_ms{0};
    std::atomic<uint32_t> count{0};
};
std::array<Window, 4> g_windows;

int64_t monotonic_ms() noexcept {
    struct timespec now{};
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return 0;
    return static_cast<int64_t>(now.tv_sec) * 1000 + now.tv_nsec / 1'000'000;
}

bool rate_allows(Level level) noexcept {
    const uint32_t limit = g_rate_limit.load(std::memory_order_relaxed);
    if (limit == 0) return true;
    const size_t index = static_cast<size_t>(static_cast<int>(level) - 3);
    if (index >= g_windows.size()) return true;
    Window &window = g_windows[index];
    const int64_t now = monotonic_ms();
    for (int attempt = 0; attempt < 4; ++attempt) {
        const int64_t start = window.start_ms.load(std::memory_order_relaxed);
        if (start == 0 || now - start >= 1000) {
            int64_t expected = start;
            if (window.start_ms.compare_exchange_strong(expected, now,
                    std::memory_order_acq_rel, std::memory_order_relaxed)) {
                window.count.store(1, std::memory_order_relaxed);
                return true;
            }
            continue;
        }
        uint32_t current = window.count.load(std::memory_order_relaxed);
        if (current >= limit) return false;
        if (window.count.compare_exchange_weak(current, current + 1,
                std::memory_order_acq_rel, std::memory_order_relaxed)) return true;
    }
    return false;
}

bool enqueue(Level level, const char *tag, const char *message) noexcept {
    size_t position = g_enqueue_position.load(std::memory_order_relaxed);
    for (;;) {
        Entry &entry = g_ring.entries[position % kCapacity];
        const size_t sequence = entry.sequence.load(std::memory_order_acquire);
        if (sequence == position) {
            if (!g_enqueue_position.compare_exchange_weak(position, position + 1,
                    std::memory_order_acq_rel, std::memory_order_relaxed)) continue;
            entry.level = level;
            const char *safe_tag = tag == nullptr ? "HyperCeiler.Native" : tag;
            const char *safe_message = message == nullptr ? "" : message;
            std::snprintf(entry.tag, sizeof(entry.tag), "%s", safe_tag);
            std::snprintf(entry.message, sizeof(entry.message), "%s", safe_message);
            entry.sequence.store(position + 1, std::memory_order_release);
            g_accepted.fetch_add(1, std::memory_order_relaxed);
            return true;
        }
        if (static_cast<intptr_t>(sequence) < static_cast<intptr_t>(position)) return false;
        position = g_enqueue_position.load(std::memory_order_relaxed);
    }
}

bool dequeue(Entry &out) noexcept {
    size_t position = g_dequeue_position.load(std::memory_order_relaxed);
    for (;;) {
        Entry &entry = g_ring.entries[position % kCapacity];
        const size_t sequence = entry.sequence.load(std::memory_order_acquire);
        if (sequence == position + 1) {
            if (!g_dequeue_position.compare_exchange_weak(position, position + 1,
                    std::memory_order_acq_rel, std::memory_order_relaxed)) continue;
            out.level = entry.level;
            std::memcpy(out.tag, entry.tag, sizeof(out.tag));
            std::memcpy(out.message, entry.message, sizeof(out.message));
            entry.sequence.store(position + kCapacity, std::memory_order_release);
            return true;
        }
        if (static_cast<intptr_t>(sequence) < static_cast<intptr_t>(position + 1)) return false;
        position = g_dequeue_position.load(std::memory_order_relaxed);
    }
}

bool deliver(Level level, const char *tag, const char *message) noexcept {
    const auto context = g_sink_context.load(std::memory_order_acquire);
    const Sink java_sink = g_java_sink.load(std::memory_order_acquire);
    if (java_sink != nullptr && g_java_healthy.load(std::memory_order_acquire)) {
        if (java_sink(level, tag, message, context)) {
            g_delivered_java.fetch_add(1, std::memory_order_relaxed);
            return true;
        }
        g_java_healthy.store(false, std::memory_order_release);
        g_java_failures.fetch_add(1, std::memory_order_relaxed);
    }
    const Sink fallback = g_fallback_sink.load(std::memory_order_acquire);
    if (fallback != nullptr && fallback(level, tag, message, context)) {
        g_delivered_fallback.fetch_add(1, std::memory_order_relaxed);
        return true;
    }
    return false;
}

#if defined(__ANDROID__)
std::atomic<JavaVM *> g_vm{nullptr};
std::atomic<jclass> g_bridge_class{nullptr};
std::atomic<jmethodID> g_bridge_method{nullptr};

int java_priority(Level level) noexcept {
    switch (level) {
        case Level::debug: return ANDROID_LOG_DEBUG;
        case Level::info: return ANDROID_LOG_INFO;
        case Level::warn: return ANDROID_LOG_WARN;
        case Level::error: return ANDROID_LOG_ERROR;
    }
    return ANDROID_LOG_INFO;
}

bool java_sink(Level level, const char *tag, const char *message, void *) noexcept {
    JavaVM *vm = g_vm.load(std::memory_order_acquire);
    jclass bridge = g_bridge_class.load(std::memory_order_acquire);
    jmethodID method = g_bridge_method.load(std::memory_order_acquire);
    if (vm == nullptr || bridge == nullptr || method == nullptr) return false;
    JNIEnv *env = nullptr;
    const jint env_status = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    bool attached = false;
    if (env_status == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK || env == nullptr) return false;
        attached = true;
    } else if (env_status != JNI_OK || env == nullptr) {
        return false;
    }
    if (env->ExceptionCheck()) env->ExceptionClear();
    jstring java_tag = env->NewStringUTF(tag == nullptr ? "" : tag);
    if (java_tag == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (attached) vm->DetachCurrentThread();
        return false;
    }
    jstring java_message = env->NewStringUTF(message == nullptr ? "" : message);
    if (java_message == nullptr) {
        env->DeleteLocalRef(java_tag);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (attached) vm->DetachCurrentThread();
        return false;
    }
    env->CallStaticVoidMethod(bridge, method, java_priority(level), java_tag, java_message);
    const bool threw = env->ExceptionCheck();
    if (threw) env->ExceptionClear();
    env->DeleteLocalRef(java_message);
    env->DeleteLocalRef(java_tag);
    if (attached) vm->DetachCurrentThread();
    return !threw;
}
bool logcat_sink(Level level, const char *tag, const char *message, void *) noexcept {
    __android_log_print(java_priority(level), tag == nullptr ? "HyperCeiler.Native" : tag,
        "%s", message == nullptr ? "" : message);
    return true;
}

void install_android_sinks() noexcept {
    g_java_sink.store(&java_sink, std::memory_order_release);
    g_fallback_sink.store(&logcat_sink, std::memory_order_release);
}
#else
void install_android_sinks() noexcept {}
#endif

} // namespace

void write(Level level, const char *tag, const char *message) noexcept {
    if (!rate_allows(level)) {
        g_rate_limited.fetch_add(1, std::memory_order_relaxed);
        return;
    }
    if (!enqueue(level, tag, message))
        g_dropped.fetch_add(1, std::memory_order_relaxed);
}

void writef(Level level, const char *tag, const char *format, ...) noexcept {
    if (format == nullptr) {
        write(level, tag, "");
        return;
    }
    char buffer[kMaxMessage]{};
    va_list args;
    va_start(args, format);
    std::vsnprintf(buffer, sizeof(buffer), format, args);
    va_end(args);
    write(level, tag, buffer);
}

void flush() noexcept {
    install_android_sinks();
    Entry entry;
    while (dequeue(entry)) (void)deliver(entry.level, entry.tag, entry.message);
}

Stats stats() noexcept {
    return {g_accepted.load(std::memory_order_relaxed),
        g_dropped.load(std::memory_order_relaxed),
        g_rate_limited.load(std::memory_order_relaxed),
        g_delivered_java.load(std::memory_order_relaxed),
        g_delivered_fallback.load(std::memory_order_relaxed),
        g_java_failures.load(std::memory_order_relaxed)};
}

#if defined(__ANDROID__)
void attach_java_bridge(JavaVM *vm, JNIEnv *env) noexcept {
    if (vm == nullptr || env == nullptr) return;
    install_android_sinks();
    if (env->ExceptionCheck()) env->ExceptionClear();
    jclass local = env->FindClass("com/sevtinge/hyperceiler/common/log/NativeLogBridge");
    if (local == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }
    const auto method = env->GetStaticMethodID(local, "log",
        "(ILjava/lang/String;Ljava/lang/String;)V");
    if (method == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        env->DeleteLocalRef(local);
        return;
    }
    const auto global = static_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);
    if (global == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }
    g_bridge_method.store(method, std::memory_order_release);
    g_bridge_class.store(global, std::memory_order_release);
    g_vm.store(vm, std::memory_order_release);
    g_java_healthy.store(true, std::memory_order_release);
}
#endif

void set_sinks_for_test(Sink java_sink_value, Sink fallback_sink, void *context) noexcept {
    g_java_sink.store(java_sink_value, std::memory_order_release);
    g_fallback_sink.store(fallback_sink, std::memory_order_release);
    g_sink_context.store(context, std::memory_order_release);
    g_java_healthy.store(true, std::memory_order_release);
}

void set_rate_limit_for_test(uint32_t messages_per_second) noexcept {
    g_rate_limit.store(messages_per_second, std::memory_order_release);
}

void reset_for_test() noexcept {
    Entry discarded;
    while (dequeue(discarded)) {}
    for (size_t i = 0; i < g_ring.entries.size(); ++i)
        g_ring.entries[i].sequence.store(i, std::memory_order_relaxed);
    g_enqueue_position.store(0, std::memory_order_relaxed);
    g_dequeue_position.store(0, std::memory_order_relaxed);
    g_accepted.store(0, std::memory_order_relaxed);
    g_dropped.store(0, std::memory_order_relaxed);
    g_rate_limited.store(0, std::memory_order_relaxed);
    g_delivered_java.store(0, std::memory_order_relaxed);
    g_delivered_fallback.store(0, std::memory_order_relaxed);
    g_java_failures.store(0, std::memory_order_relaxed);
    g_java_sink.store(nullptr, std::memory_order_release);
    g_fallback_sink.store(nullptr, std::memory_order_release);
    g_sink_context.store(nullptr, std::memory_order_release);
    g_java_healthy.store(true, std::memory_order_release);
    g_rate_limit.store(32, std::memory_order_release);
    for (auto &window : g_windows) {
        window.start_ms.store(0, std::memory_order_relaxed);
        window.count.store(0, std::memory_order_relaxed);
    }
}

} // namespace nhk::native_log