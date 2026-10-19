/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_report_hooks.h"
#include "home_report_guard.h"
#include "nativehook/memory_io.h"

#include <array>
#include <cstring>
#include <mutex>

extern "C" {
alignas(8) void *hc_home_report_original = nullptr;
alignas(4) uint32_t hc_home_report_enabled = 0;
void hc_home_report_entry();
}

namespace home_report {
namespace {
std::mutex g_mutex;
uintptr_t g_target = 0;
bool g_broken = false;

struct Candidate {
    uintptr_t address = 0;
    size_t size = 0;
    bool ambiguous = false;
};

bool collect(const char *name, void *address, size_t size, void *data) {
    if (name == nullptr || kSymbol != name) return true;
    auto &found = *static_cast<Candidate *>(data);
    const auto target = reinterpret_cast<uintptr_t>(address);
    if (found.address != 0 && (found.address != target || found.size != size)) {
        found.ambiguous = true;
        return false;
    }
    // A duplicate symbol-table row describing the same address/size is one target.
    found.address = target;
    found.size = size;
    return true;
}
} // namespace

void set_enabled(bool value) {
    __atomic_store_n(&hc_home_report_enabled, value ? 1u : 0u, __ATOMIC_RELEASE);
}

bool enabled() {
    return __atomic_load_n(&hc_home_report_enabled, __ATOMIC_ACQUIRE) != 0;
}

InstallResult install(const Runtime &runtime) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_broken) return {InstallStatus::RollbackFailed, g_target};
    if (runtime.enumerate_symbols == nullptr || runtime.inline_hook == nullptr
        || runtime.inline_unhook == nullptr) return {InstallStatus::InvalidRuntime};

    Candidate candidate;
    runtime.enumerate_symbols(runtime.context, collect, &candidate);
    if (candidate.ambiguous) return {InstallStatus::AmbiguousSymbol};
    if (candidate.address == 0) return {InstallStatus::TargetNotFound};
    if (g_target != 0) {
        return {g_target == candidate.address ? InstallStatus::AlreadyInstalled
                                              : InstallStatus::ConflictingTarget, g_target};
    }
    if ((candidate.address & 3u) != 0 || candidate.size != kBodyBytes) {
        return {InstallStatus::UnsupportedBody, candidate.address};
    }
    std::array<std::byte, kBodyBytes> body{};
    if (!nhk::safe_read(candidate.address, body)) {
        return {InstallStatus::UnreadableBody, candidate.address};
    }
    if (!accepts(kSymbol, body)) return {InstallStatus::UnsupportedBody, candidate.address};

    // The hook implementation must publish the original before enabling the detour.
    // Give it the actual trampoline slot, not a local copied after installation.
    const int result = runtime.inline_hook(reinterpret_cast<void *>(candidate.address),
        reinterpret_cast<void *>(&hc_home_report_entry), &hc_home_report_original);
    if (result != 0) {
        hc_home_report_original = nullptr;
        return {InstallStatus::HookFailed, candidate.address};
    }
    if (hc_home_report_original == nullptr) {
        const int rolled_back = runtime.inline_unhook(reinterpret_cast<void *>(candidate.address));
        if (rolled_back != 0) {
            g_target = candidate.address;
            g_broken = true;
            set_enabled(false);
            return {InstallStatus::RollbackFailed, candidate.address};
        }
        return {InstallStatus::MissingOriginalRolledBack, candidate.address};
    }
    g_target = candidate.address;
    return {InstallStatus::Installed, g_target};
}

const char *status_name(InstallStatus status) {
    switch (status) {
        case InstallStatus::Installed: return "installed";
        case InstallStatus::AlreadyInstalled: return "already-installed";
        case InstallStatus::TargetNotFound: return "target-not-found";
        case InstallStatus::InvalidRuntime: return "invalid-runtime";
        case InstallStatus::AmbiguousSymbol: return "ambiguous-symbol";
        case InstallStatus::UnsupportedBody: return "unsupported-body";
        case InstallStatus::UnreadableBody: return "unreadable-body";
        case InstallStatus::ConflictingTarget: return "conflicting-target";
        case InstallStatus::HookFailed: return "hook-failed";
        case InstallStatus::MissingOriginalRolledBack: return "missing-original-rolled-back";
        case InstallStatus::RollbackFailed: return "rollback-failed";
    }
    return "invalid-status";
}
} // namespace home_report
