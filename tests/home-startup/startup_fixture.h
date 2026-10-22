/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "targets/home/home_startup.h"
namespace startup_fixture {
// Test adapter only. Link actual home_startup.cpp with HOME_STARTUP_TEST.
// Load before entering the verified observer; fixture file I/O is never done
// from a production callback. One fresh OS process per scenario is recommended.
bool load(const char *elf_path);
uintptr_t base() noexcept;
bool contains(uintptr_t address) noexcept;
int hook(void *target, void *replacement, void **original);
int unhook(void *target);
bool all_words_original() noexcept;
bool gate_enabled() noexcept;
unsigned hook_count() noexcept;
uint64_t published_mask() noexcept;
// Events occur in the actual executor's backend/consumer calls. They may invoke
// real control revocation; callbacks must not outlive the test process.
void set_hook_event(void (*event)(unsigned)) noexcept;
void set_publish_event(void (*event)()) noexcept;
}
