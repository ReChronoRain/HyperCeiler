/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstddef>
namespace home_recent_bar {
using Hook=int(*)(void*,void*,void**);using Unhook=int(*)(void*);
using Protect=bool(*)(uintptr_t,size_t);
// Existing health worker only, no loader/property/render-thread discovery.
void maintain(Hook,Unhook,Protect) noexcept;
uint64_t packed() noexcept;
void configure(bool enabled) noexcept;
bool requested() noexcept;
uint64_t maintenance_revision() noexcept;
void wait_for_maintenance(long nanoseconds, uint64_t revision) noexcept;
}
