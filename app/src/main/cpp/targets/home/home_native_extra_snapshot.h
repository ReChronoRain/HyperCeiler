/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_extra_gesture_lease.h"
#include <memory>
bool hc_native_extra_resident_selected();
void hc_native_extra_snapshot_refresh(void* retained_context,bool probe,bool receive,bool resident);
bool hc_native_extra_snapshot_due();
// Event worker timed wait, monotonic clock; -1 means no scheduled work.
int hc_native_extra_snapshot_wait_ms();
// Memory-only read. A stale/dead publication is unavailable, never last-good
// geometry. Callers reset recognition when producer/revision/owner changes.
std::shared_ptr<const home_extra_gestures::Snapshot> hc_native_extra_snapshot_get();
// Live lease and its attested snapshot; no binder/parser/SDK work here.
std::shared_ptr<const home_extra_gestures::Lease> hc_native_extra_owner_get();
std::shared_ptr<const home_extra_gestures::Snapshot> hc_native_extra_execution_snapshot_get();
// Typed action queue: input/timer callbacks never wait on the privilege broker.
bool hc_native_extra_action(std::shared_ptr<const home_extra_gestures::Lease> owner,int event,int64_t time);
void hc_native_extra_actions_refresh();
