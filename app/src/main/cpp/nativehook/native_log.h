/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Bounded native logging facade.
 *
 * Writers never call Java, allocate, or block. Messages enter a fixed-capacity
 * lock-free queue and are drained only from an explicit safe context. When the
 * Java bridge is unavailable or throws, the drain falls back to logcat.
 */
#pragma once

#include <cstdarg>
#include <cstddef>
#include <cstdint>

#if defined(__ANDROID__)
#include <jni.h>
#endif

namespace nhk::native_log {

enum class Level : int { debug = 3, info = 4, warn = 5, error = 6 };

inline constexpr size_t kCapacity = 64;
inline constexpr size_t kMaxTag = 64;
inline constexpr size_t kMaxMessage = 768;

using Sink = bool (*)(Level, const char *, const char *, void *);

struct Stats {
    uint64_t accepted;
    uint64_t dropped;
    uint64_t rate_limited;
    uint64_t delivered_java;
    uint64_t delivered_fallback;
    uint64_t java_failures;
};

void write(Level level, const char *tag, const char *message) noexcept;
void writef(Level level, const char *tag, const char *format, ...) noexcept
    __attribute__((format(printf, 3, 4)));

/* Safe-context only. Never call from an assembly observer, hot hook, page-guard
 * callback, or while holding hook_mutex/madvise_guard_mutex/protected_page_lock. */
void flush() noexcept;

Stats stats() noexcept;

#if defined(__ANDROID__)
/* Caches the module class loader and bridge method; no bridge call is made here. */
void attach_java_bridge(JavaVM *vm, JNIEnv *env) noexcept;
#endif

/* Host-test hooks. The test runner is single-threaded while configuring these. */
void set_sinks_for_test(Sink java_sink, Sink fallback_sink, void *context) noexcept;
void set_rate_limit_for_test(uint32_t messages_per_second) noexcept;
void reset_for_test() noexcept;

} // namespace nhk::native_log
