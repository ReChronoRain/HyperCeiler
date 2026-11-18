/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
struct HomeUnlockObservation {int64_t epoch_ms=-1;int32_t flags=0;uint64_t received=0,accepted=0;bool connected=false;};
void hc_native_unlock_refresh(bool wanted,void(*wake)());
bool hc_native_unlock_due();
HomeUnlockObservation hc_native_unlock_observation();
// Frame thread reads only this one immutable epoch, never takes the control lock.
int64_t hc_native_unlock_epoch();
// 0 unknown, 1 known hidden, 3 known visible; optional HCU1 callback extension.
unsigned hc_native_home_visibility();
// Control worker uses the authenticated retirement edge to replace its window.
uint64_t hc_native_home_window_generation();
// Optional HCU1 admission operations, called only by the SDK control worker.
bool hc_native_dock_admission(bool wanted);
bool hc_native_dock_owned();
bool hc_native_dock_requested();
bool hc_native_dock_rejected();
