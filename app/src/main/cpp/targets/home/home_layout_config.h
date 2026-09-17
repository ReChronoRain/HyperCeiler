/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "home_layout_knobs.h"

#include <array>

namespace home_layout {

/* One knob's requested state, in pixels. */
struct KnobConfig {
    bool enabled = false;
    int delta_px = 0;

    bool operator==(const KnobConfig &other) const = default;
};

/*
 * The features that are planned as code patches rather than redirected accessors. Every value here
 * is a setting; the addresses are located at run time from the launcher image's symbol table, so
 * nothing about this struct can go stale with an OTA.
 */
struct TweaksConfig {
    bool folder_enabled = false;
    int folder_cols = 5;
    bool pad_enabled = false;
    int pad_major = 8;
    int pad_minor = 5;
    bool fold_enabled = false;
    int fold_major = 8;
    int fold_minor = 5;
    bool icon_scale_enabled = false;
    /* Settings-page default level 70; nearest acceptable launcher code is 0x66 (0.6875) - see
     * kIconScaleCodeDefault in tweaks/blob.h for why 0.70 itself is not representable. */
    int icon_scale_code = 0x66;
    bool recents_hide_clear = false;
    bool recents_no_clear = false;
};

inline bool operator==(const TweaksConfig &a, const TweaksConfig &b) {
    return a.folder_enabled == b.folder_enabled && a.folder_cols == b.folder_cols
        && a.pad_enabled == b.pad_enabled && a.pad_major == b.pad_major
        && a.pad_minor == b.pad_minor && a.fold_enabled == b.fold_enabled
        && a.fold_major == b.fold_major && a.fold_minor == b.fold_minor
        && a.icon_scale_enabled == b.icon_scale_enabled
        && a.icon_scale_code == b.icon_scale_code
        && a.recents_hide_clear == b.recents_hide_clear
        && a.recents_no_clear == b.recents_no_clear;
}

struct Config {
    bool grid_enabled = false;
    int cell_x = 0;
    int cell_y = 0;
    /*
     * One row per HC_LAYOUT_KNOBS entry, in that order. Deltas are in PIXELS:
     * the settings pages store dp and system_server owns the default display's
     * density, so the launcher-side trampolines never need a density of their own.
     * A zero delta is a knob's neutral point (its settings-page default), which is
     * what keeps an untouched slider from changing anything.
     */
    std::array<KnobConfig, HC_LAYOUT_KNOB_COUNT> knobs{};
    TweaksConfig tweaks{};
};

/** Read the module preference snapshot through the authenticated WMS Binder endpoint. */
bool query_config(Config &result);

} // namespace home_layout
