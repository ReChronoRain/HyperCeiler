/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace hyos_snapshot { struct Snapshot; }

namespace home_recents {
using ReadFresh = bool (*)(hyos_snapshot::Snapshot &);
// Retains an additional original-loader reference only if its app_entry symbol
// is this exact expected entry in the Home namespace. Success is never released
// during the process lifetime. This excludes loader unload, not arbitrary mmap
// destruction or page discard by unrelated code.
using PinImage = bool (*)(uintptr_t expected_base, uintptr_t expected_entry);
enum class Phase : unsigned { Cold, Installing, Installed, Disabled, Missed, Rejected, Residue };
enum class Error : unsigned {
    None, Busy, NotAuthorized, UnsupportedAtomicIsa, ImageMismatch, ImagePinFailed, NotCold,
    NoArena, TemplateMismatch, ForeignCode, PageLockFailed, ProtectionFailed, WriteFailed,
    ReadbackFailed, EarlyCall, Cancelled, RollbackFailed,
};
struct Status { Phase phase; Error error; bool owned; uintptr_t arena; };

// The caller must already have authenticated the exact Home child and runtime
// app_entry call, and must withhold that original entry until this call returns.
// It is outside loader and specialization callbacks. It is not a single-thread
// boundary. A cold Once observation alone never supplies this lifecycle contract.
// Installation is one-shot; a denied/late/failed attempt cannot activate later.
bool prepare_before_entry(uintptr_t rust_base, ReadFresh read_fresh, PinImage pin_image) noexcept;

// Existing HCS worker only, after its authoritative cache publication; no new
// Binder lane. Only the same initially selected percentage and epoch can renew.
// Disable, percentage change, authorization loss and expiry require cold Home
// restart; existing Rust/Dart cached objects are not retroactively rewritten.
void service() noexcept;
// Concurrent fail-closed lease cancellation; does not claim cache restoration.
void cancel() noexcept;
Status status() noexcept;
} // namespace home_recents
