/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstdint>

namespace weather_card_lifetime {
// Parent loader-return observation is read-only. Late child discovery is forbidden.
void observe_parent_load(const char *path) noexcept;
// Called at the documented post-SELinux/pre-runtime specialization boundary.
// The finite single-thread observation is conservative admission, not a lock.
void after_specialization(bool weather) noexcept;
bool ready() noexcept;
bool has_residue() noexcept;
// Reserve only the two exact Weather epilogue pages, before either code write.
// Any pass-through madvise still in flight causes immediate refusal.
bool protect(uintptr_t dart_base, uintptr_t flutter_base,
             const std::array<uintptr_t, 2> &sites) noexcept;
bool intact() noexcept;
// Call only after both original code words are restored. A busy reader keeps
// protection in place, without waiting or an automatic retry. The forwarding
// wrapper itself stays mapped; the immutable descriptor is never reused.
bool release_pages() noexcept;
}
