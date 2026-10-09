/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace weather_runtime {
// Only from the verified Weather app_entry wrapper, before the original entry.
// Never from specialization or while inside the original Android loader call.
bool start_before_entry(uintptr_t weather_base);
// Called after original loader dlsym returns; finite reads and atomics only.
void observe_snapshot(const char *symbol, void *result);
// Card code installation has a stricter first-use boundary than observing
// read-only image identity. Hyos supplies its verified handle/TID/loader window.
bool card_startup_pending() noexcept;
void on_first_card_snapshot(uintptr_t dart_base, uintptr_t flutter_base) noexcept;
void on_card_snapshot_window_missed(uint32_t reason) noexcept;
} // namespace weather_runtime
