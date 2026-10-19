/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_native {
using HookFunction = int (*)(void *, void *, void **);
using UnhookFunction = int (*)(void *);
using LibraryLoaded = void (*)(const char *, void *);
struct ApiEntries {
    uint32_t version;
    HookFunction hook_func;
    UnhookFunction unhook_func;
};
}
extern "C" home_native::LibraryLoaded native_init(const home_native::ApiEntries *entries);
void hyperceiler_home_runtime_options(bool high_models, bool disable_prestart,
                                     bool soft_glass, bool dock_enabled, bool hide_report);
// Hyos control-worker only, after the one-shot first-layout phase terminates.
void hyperceiler_home_after_startup();
