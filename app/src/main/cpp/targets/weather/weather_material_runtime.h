/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "weather_snapshot.h"
#include <cstdint>

namespace weather_material_runtime {
// The shared Weather controller owns its one HWS1 worker and lifecycle gate.
// The material runtime verifies DartBase before consuming ReadFresh's snapshot.
using ReadFresh = bool (*)(weather_snapshot::Snapshot &);
using DartBase = uintptr_t (*)();
// Only called before the pinned Rust app_entry; never from a loader callback.
bool start_before_entry(uintptr_t weather_base, ReadFresh read_fresh, DartBase dart_base);
// Unresolved GOT ownership or RELRO restoration closes the shared startup batch.
bool has_residue();
} // namespace weather_material_runtime
