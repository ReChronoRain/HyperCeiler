/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout { struct Config; }
namespace hyos_snapshot { struct Snapshot; }
namespace hyos_control {
// Legacy HCY1 remains a Java-side compatibility endpoint. New native control
// uses the combined HCS1 wire contract in hyos_snapshot.h.
constexpr int32_t kTransaction = 0x00484359;
constexpr int32_t kAck = 0x48435931;
constexpr uint32_t kAllowed = 1;
constexpr uint32_t kHighModels = 2;
constexpr uint32_t kDisablePrestart = 4;
constexpr uint32_t kSoftGlass = 8;
constexpr uint32_t kDock = 16;
constexpr uint32_t kHideReport = 32;
constexpr uint32_t kKnownFlags = 63;

inline bool valid_reply(int32_t ack, int32_t available, int32_t flags) {
    return ack == kAck && available == 1 && flags >= 0
        && (static_cast<uint32_t>(flags) & ~kKnownFlags) == 0
        && ((flags & kAllowed) != 0 || flags == 0)
        && ((flags & kSoftGlass) == 0 || (flags & kDock) != 0);
}
bool active();
bool allowed();
bool feature_enabled(uint32_t flag);
// Bounded atomic read; always clears result to neutral on unavailable/denied.
bool layout_config(home_layout::Config &result) noexcept;
// Complete authoritative frame for the independent, pre-entry Recents owner.
// Bounded cache read only; preserves the server epoch, revision and deadline.
bool read_snapshot(hyos_snapshot::Snapshot &result) noexcept;
// Called only by the guarded first-snapshot observer. No Binder or waiting.
void on_first_home_snapshot(uintptr_t dart_load_bias) noexcept;
void on_home_snapshot_window_missed(uint32_t reason) noexcept;
}

struct ZygiskNextAPI;
// May wait at most 150ms for first poll; call outside loader/fork callbacks.
// true means a fresh authoritative snapshot (which may explicitly disable hooks).
bool hyperceiler_hyos_start_home_control(const ZygiskNextAPI *api);
// Loader-safe bookkeeping only; no Binder, hooks, allocation or thread creation.
void hyperceiler_hyos_record_home_library(const char *name);
