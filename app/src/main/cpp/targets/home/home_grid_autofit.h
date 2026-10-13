/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cmath>
#include <cstdint>
#include <span>
#include "home_dart_fields.h"

namespace home_layout {
// The original calculators start with equal horizontal/vertical cells and one
// same-width hotseat row. Their final cap grows row and hotseat height together,
// then redistributes leftover height to outer padding. Keep the stock hotseat
// result, assign ALL remaining height to workspace rows before GridInfo creation.
inline bool grid_autofit_height(double base_width, double remaining, double dock_height,
        int64_t rows, unsigned requested_rows, double &height) {
    if (rows != requested_rows || rows < 4 || rows > 13 || !std::isfinite(base_width)
        || !std::isfinite(remaining) || !std::isfinite(dock_height)
        || base_width < 24 || base_width > 1024 || dock_height < 24 || dock_height > 1024
        || remaining < -2048 || remaining > 4000) return false;
    const double usable = base_width * (rows + 1) + remaining - dock_height;
    const double fit = usable / double(rows);
    if (!std::isfinite(fit) || fit < 24 || fit > 1024 || usable > 4000) return false;
    height = fit; return true;
}
struct GridAutofitSites {
    uint32_t legacy = 0, handler = 0;
    int32_t legacy_height = -1, legacy_dock = -1;
    int32_t width = -1, height = -1, dock = -1, rows = -1;
};
inline bool grid_autofit_sites(std::span<const uint32_t> old,
        std::span<const uint32_t> cap, std::span<const uint32_t> handler, GridAutofitSites &out) {
    GridAutofitSites f; unsigned legacy = 0, caps = 0, copy = 0, dock = 0, height = 0;
    // Final original cap publishes row height and hotseat height from the same result.
    for (size_t i = 0; i + 2 < cap.size(); ++i) {
        int32_t d = -1, h = -1;
        if (cap[i] == 0x4ee0f422
            && (cap[i + 1] & 0xffe00c00u) == 0xfc000000u
            && dart_ldur_d(cap[i + 1] | 0x00400000u, 1, 2, &d)
            && (cap[i + 2] & 0xffe00c00u) == 0xfc000000u
            && dart_ldur_d(cap[i + 2] | 0x00400000u, 1, 2, &h)
            && d > 0 && h > 0 && d != h && (d & 7) == 7 && (h & 7) == 7) {
            f.legacy_height = h; f.legacy_dock = d; ++caps;
        }
    }
    for (size_t i = 0; i + 3 < old.size(); ++i) {
        int32_t h = -1;
        if (dart_ldur_d(old[i], 1, 0, &h) && h == f.legacy_height
            && old[i + 1] == 0xf9420f64 && dart_is_bl(old[i + 2])
            && old[i + 3] == 0x4ea01c01) ++height;
        if (old[i] == 0xfc5c83a1 && old[i + 1] == 0x1e600822
            && old[i + 2] == 0xfc5d83a0 && old[i + 3] == 0x1e623801
            && i >= 2 && old[i - 2] == 0x91000420 && old[i - 1] == 0x9e620000) {
            f.legacy = uint32_t(i * 4); ++legacy;
        }
    }
    for (size_t i = 0; i + 9 < handler.size(); ++i) {
        int32_t w = -1, h = -1, r = -1;
        if (dart_ldur_d(handler[i], 1, 0, &h) && dart_ldur_d(handler[i + 1], 1, 1, &w)
            && handler[i + 2] == 0x1e613802 && dart_ldur_x(handler[i + 3], 1, 3, &r)
            && handler[i + 4] == 0x91000464 && handler[i + 5] == 0x9e620080
            && handler[i + 6] == 0x1e600841 && handler[i + 7] == 0xfc407000
            && handler[i + 8] == 0x1e613802 && h > 0 && w > 0 && r > 0 && h != w) {
            f.handler = uint32_t((i + 5) * 4); f.width = w; f.height = h; f.rows = r; ++copy;
        }
        int32_t d = -1;
        if (dart_ldur_d(handler[i], 0, 0, &d) && handler[i + 1] == 0xfc1b83a0
            && handler[i + 2] == 0x1e602823 && handler[i + 3] == 0xfc063003 && d > 0) {
            f.dock = d; ++dock;
        }
    }
    if (legacy != 1 || caps != 1 || height != 1 || copy != 1 || dock != 1
        || f.dock == f.height || f.dock == f.width) return false;
    out = f; return true;
}
} // namespace home_layout
