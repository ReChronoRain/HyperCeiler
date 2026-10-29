/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recents_haptic_render.h"

extern "C" uint32_t hc_recents_haptic_gate;
void hc_recents_haptic_refresh();
bool hc_recents_haptic_enabled();
bool hc_recents_haptic_scroll_enabled();
bool hc_recents_close_haptic_enabled();
void hc_recents_close_haptic_ready(bool);
bool hc_recents_close_haptic_active();
void hc_recents_close_haptic_commit();
bool hc_recents_haptic_changed();
void hc_recents_haptic_ready(bool);
void hc_recents_haptic_observe(const home_layout::haptic::Observation&);
struct HomeRecentsHapticCounters {
    uint64_t samples=0,queued=0,delivered=0,dropped=0;
    uint64_t close_queued=0,close_delivered=0,close_dropped=0;
    struct Lane {uint64_t starts=0,updates=0,ends=0,programmatic=0,extents=0;double pixels=0,extent=0;};
    std::array<Lane,4> lanes{};
};
HomeRecentsHapticCounters hc_recents_haptic_counters();
