/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include <cstddef>
#include <cstdint>

namespace home_report {

using SymbolVisitor = bool (*)(const char *name, void *address, size_t size, void *data);
using EnumerateSymbols = void (*)(void *context, SymbolVisitor visitor, void *data);
using InlineHook = int (*)(void *target, void *replacement, void **original);
using InlineUnhook = int (*)(void *target);

/* The caller scopes context to the verified Home libapp.so mapping. API success is 0. */
struct Runtime {
    void *context = nullptr;
    EnumerateSymbols enumerate_symbols = nullptr;
    InlineHook inline_hook = nullptr;
    InlineUnhook inline_unhook = nullptr;
};

enum class InstallStatus {
    Installed,
    AlreadyInstalled,
    TargetNotFound,        // Loader has not published the image/symbol yet; retry is permitted.
    InvalidRuntime,
    AmbiguousSymbol,
    UnsupportedBody,
    UnreadableBody,
    ConflictingTarget,
    HookFailed,
    MissingOriginalRolledBack,
    RollbackFailed,        // Terminal: do not attempt to install over a broken detour.
};

struct InstallResult {
    InstallStatus status;
    uintptr_t target = 0;
};

/* No thread, JNI, discovery loop or authentication bypass is created here. */
InstallResult install(const Runtime &runtime);
const char *status_name(InstallStatus status);

/* Called only after the existing authenticated snapshot is decoded; defaults to false. */
void set_enabled(bool enabled);
bool enabled();

} // namespace home_report
