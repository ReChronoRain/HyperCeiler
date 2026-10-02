/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime inline hook backend: the LSPosed native API boundary.
 *
 * LSPosed injects two entry points (hookFunc/unhookFunc) at native_init time
 * - the same `NativeAPIEntries` shape MiuiBackGestureHook formalizes in its
 * native_api.h (Apache-2.0). This wrapper adds what both projects learned
 * the hard way:
 *  - a backend that reports success without a continuation pointer has left
 *    the target branching through an out-parameter we do not own; the caller
 *    must treat the slot as refused, not installed (hook_bank.h enforces it);
 *  - trampoline pages of file-backed images can be reclaimed and refilled by
 *    the kernel; registering them with the optional page guard (page_guard.h)
 *    before installation closes the reclaim-vs-patch race.
 *
 * The engine behind the entry points is whatever LSPosed ships; this layer
 * has no dependency on any specific inline hook implementation.
 */
#pragma once

#include "hook_bank.h"
#include "page_guard.h"

#include <cstdint>

namespace nhk {

struct NativeApiEntries {
    int (*hook_func)(void *target, void *replacement, void **original);
    int (*unhook_func)(void *target);
};

/**
 * Process-wide backend handle. The entry points arrive once, during
 * native_init; every feature resolves them through this singleton, which is
 * exactly the role the desktop's global `hook_function` pointer used to play.
 */
class LsposedInlineBackend {
public:
    static LsposedInlineBackend &instance() {
        static LsposedInlineBackend backend;
        return backend;
    }

    void install_entries(const NativeApiEntries &entries) { entries_ = entries; }

    const NativeApiEntries &entries() const { return entries_; }

    /** Feature hook: fills the install function of a hook_bank host. */
    template<size_t kPatchWords>
    bool attach(InlineHookHost<kPatchWords> &host) const {
        host.hook_install = [](void *target, void *replacement, void **original) -> int {
            const NativeApiEntries *api = LsposedInlineBackend::instance().live();
            return api != nullptr ? api->hook_func(target, replacement, original) : -1;
        };
        return entries_.hook_func != nullptr && entries_.unhook_func != nullptr;
    }

private:
    LsposedInlineBackend() = default;
    const NativeApiEntries *live() const {
        return entries_.hook_func != nullptr ? &entries_ : nullptr;
    }
    NativeApiEntries entries_{};
};

} // namespace nhk
