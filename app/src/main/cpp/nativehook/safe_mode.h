/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Launcher native safe-mode gate.
 *
 * The state is read from the existing read-only crash-report property. A
 * decision is cached per PID because a specialized HYOS child must never inherit
 * its parent's result. Any unreadable or malformed value blocks installation.
 */
#pragma once

#include <cstddef>
#include <sys/types.h>

namespace nhk::home_safe_mode {

enum class State { normal, safe_mode, missing, corrupt, unreadable };

struct Decision {
    State state;
    bool blocked;
};

using Reader = bool (*)(const char *name, char *value, std::size_t capacity);

/* Pure parser: no allocation, no property access. */
Decision parse(const char *value) noexcept;

/* Reads the existing crash-report property once per PID and caches the result. */
Decision decision() noexcept;

/* Host-test seam. Passing a null reader restores the production reader. */
void set_reader_for_test(Reader reader) noexcept;
void set_pid_for_test(pid_t pid) noexcept;
void reset_for_test() noexcept;

const char *state_name(State state) noexcept;

} // namespace nhk::home_safe_mode