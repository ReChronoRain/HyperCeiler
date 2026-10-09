/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_config.h"
#include <cstddef>
#include <cstdint>

namespace home_startup {
using HookFunction = int (*)(void *, void *, void **);
using UnhookFunction = int (*)(void *);
using AuthorizationCheck = bool (*)(void *);
enum Feature : uint64_t {
    PhoneGridDart = 1ull << 0, FolderColumns = 1ull << 1,
    PadGrid = 1ull << 2, FoldGrid = 1ull << 3, IconScale = 1ull << 4,
    RemoveClear = 1ull << 5, HideClear = 1ull << 6,
    HotseatMargin = 1ull << 7, FolderSpacing = 1ull << 8,
    Workspace = 1ull << 9, CapsuleMargin = 1ull << 10,
    IndicatorPolicy = 1ull << 11, FolderGeometry = 1ull << 12,
    HotseatCapacity = 1ull << 13, OpenAnimation = 1ull << 14,
    // These earlier Rust first-use deadlines have NOT been established by
    // the Dart snapshot window. Keep them separate from successful Dart work.
    PhoneGridRust = 1ull << 15, RecentsAnimationRust = 1ull << 16,
};
enum class Outcome : uint32_t { Applied, Declined, Missed, TerminalFailure };
enum class Error : uint32_t {
    None, NotPrepared, Busy, NotAuthorized, InvalidRequest, ImageMismatch,
    GuardMismatch, UnsupportedConfiguration, HookFailed, MissingContinuation,
    WriteFailed, ReadbackFailed, RollbackFailed, ForeignCode, EarlierRustDeadline,
    LateConfiguration, Revoked,
};
struct Result {
    Outcome outcome = Outcome::Declined;
    uint64_t requested = 0, applied = 0, declined = 0;
    Error error = Error::None;
};
struct Request {
    uintptr_t dart_load_bias = 0;
    HookFunction hook = nullptr;
    UnhookFunction unhook = nullptr;
    const home_layout::Config *config = nullptr;
    AuthorizationCheck still_authorized = nullptr;
    void *context = nullptr;
};

// Called once in the specialized child before publishing it to startup/control.
// No file/Binder lookup, allocation, worker or inherited mutex is used.
bool prepare_for_child() noexcept;
Result execute(const Request &request) noexcept;
// Normal control-worker calls. Borrowed config/context are never retained.
Result maintenance(const home_layout::Config *config, bool authorized,
    AuthorizationCheck still_authorized = nullptr, void *context = nullptr) noexcept;
// Safe inside the short control publish lock: atomics only, no code writes.
void request_revoke() noexcept;
// Watchdog calls outside its lock. A busy writer is never waited for.
Result service_revoke() noexcept;

namespace detail {
// Bridge to the unchanged production Dart ABI. All addresses belong to a
// generated, fully guarded image; no dynamic lookup occurs in these functions.
struct HookBinding { void *replacement; void **original; };
HookBinding binding(unsigned index) noexcept;
void initialize_consumers(uintptr_t base, uintptr_t indicator_edit,
    uintptr_t indicator_empty, uintptr_t indicator_dots, uintptr_t idle_caller,
    const uint32_t *folder_resume) noexcept;
void publish_consumers(const home_layout::Config &, uint64_t applied) noexcept;
void clear_consumers() noexcept;
}
} // namespace home_startup
