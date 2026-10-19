/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "weather_snapshot.h"
#include <cstdint>

namespace weather_card_runtime {
using ReadFresh = bool (*)(weather_snapshot::Snapshot &);
enum class Phase : unsigned { Cold, Prepared, Installing, Installed, Missed, Rejected, Residue };
enum class Error : unsigned {
    None, NotPrepared, Busy, NotAuthorized, ImageMismatch, NoArena,
    TemplateRejected, ProtectionRejected, ForeignCode, WriteFailed,
    ReadbackFailed, Cancelled, RollbackFailed, LifetimeRejected,
};
struct Status { Phase phase; Error error; unsigned owned_mask; uintptr_t arena; };

// Specialized child, before entry; prepares immutable templates and page size.
// This does not install, publish an enabled lease, or start another worker.
bool prepare(ReadFresh read_fresh) noexcept;
// Only the verified first snapshot return window may call this. Caller/handle/
// TID/loader binding is an independent prerequisite, not inferred from bases.
bool install_before_snapshot(uintptr_t dart_base, uintptr_t flutter_base) noexcept;
// Existing HWS1 worker only. Never writes code or reinstalls a lost detour.
void service() noexcept;
// Finite atomic cancellation; permanently denies future lease publications.
// Already consumed Dart state is not restored by this call.
void cancel() noexcept;
Status status() noexcept;
} // namespace weather_card_runtime
