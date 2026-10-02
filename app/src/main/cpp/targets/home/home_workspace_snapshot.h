/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout {
// Owned by the Dart thread, not the settings worker. These are unboxed values:
// no cached Dart pointer is dereferenced or kept alive across a GC. A bounded
// cache distinguishes layout configurations; misses use original inset math.
struct WorkspaceRenderSnapshot {
    struct Layout {
        uintptr_t grid = 0;
        int64_t columns = 0, rows = 0;
        double raw_width = 0, raw_height = 0;
        double side = 0, top = 0, width = 0, height = 0;
    } layouts[8]{};
    struct Preview {
        uintptr_t frame = 0;
        double top = 0;
    } previews[8]{};
    unsigned next_layout = 0, next_preview = 0;
    mutable uint64_t hits = 0, misses = 0;
    uint32_t logged_hits = 0;

    void rendered(uintptr_t grid, int64_t columns, int64_t rows,
        double raw_width, double raw_height, double side, double top,
        double width, double height) {
        Layout *slot = nullptr;
        for (auto &l : layouts) if (l.grid == grid) { slot = &l; break; }
        if (slot == nullptr) slot = &layouts[next_layout++ % 8];
        *slot = {grid, columns, rows, raw_width, raw_height, side, top, width, height};
    }
    bool geometry(uintptr_t grid, int64_t columns, int64_t rows,
        double raw_width, double raw_height, double *g) const {
        for (const auto &l : layouts) {
            if (l.grid == grid && l.columns == columns && l.rows == rows
                && l.raw_width == raw_width && l.raw_height == raw_height) {
                g[0] = l.side; g[1] = l.top; g[2] = l.width; g[3] = l.height;
                ++hits;
                return true;
            }
        }
        ++misses;
        return false;
    }
    // calOriginPreviewIconLoc has an outgoing argument at FP-0x38 and live
    // pointer locals: carry its top inset natively, never in a GC stack slot.
    void begin_preview(uintptr_t frame, double top) {
        Preview *slot = nullptr;
        for (auto &p : previews) if (p.frame == frame) { slot = &p; break; }
        if (slot == nullptr) slot = &previews[next_preview++ % 8];
        *slot = {frame, top};
    }
    double finish_preview(uintptr_t frame, double fallback) {
        for (auto &p : previews) if (p.frame == frame) {
            const double top = p.top; p = {}; return top;
        }
        return fallback;
    }
};
} // namespace home_layout
