/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout {
// Owned by the Dart thread, not the settings worker. These are unboxed values:
// no cached Dart pointer is dereferenced or kept alive across a GC. A bounded
// cache distinguishes layout configurations; misses use original inset math.
// currentConfig may return a new GridInfo with identical scalar geometry.
// Match that geometry, not a GC-movable Dart identity: otherwise a setting
// update can send the close animation to an unrendered grid and snap back.
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
    Preview drops[8]{};
    unsigned next_layout = 0, next_preview = 0, next_drop = 0;
    mutable uint64_t hits = 0, misses = 0;
    uint32_t logged_hits = 0;

    static bool same_grid(const Layout &l, int64_t columns, int64_t rows,
        double raw_width, double raw_height) {
        return l.grid != 0 && l.columns == columns && l.rows == rows
            && l.raw_width == raw_width && l.raw_height == raw_height;
    }

    void rendered(uintptr_t grid, int64_t columns, int64_t rows,
        double raw_width, double raw_height, double side, double top,
        double width, double height) {
        Layout *slot = nullptr;
        // One latest rendered entry per scalar configuration, so a clone or
        // moving GC cannot select an older duplicate of the same layout.
        for (auto &l : layouts) if (same_grid(l, columns, rows, raw_width, raw_height)) {
            slot = &l; break;
        }
        if (slot == nullptr) slot = &layouts[next_layout++ % 8];
        *slot = {grid, columns, rows, raw_width, raw_height, side, top, width, height};
    }
    bool geometry(uintptr_t grid, int64_t columns, int64_t rows,
        double raw_width, double raw_height, double *g) const {
        (void)grid; // Identity is diagnostic only; never dereference it here.
        for (const auto &l : layouts) {
            if (same_grid(l, columns, rows, raw_width, raw_height)) {
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
    // A drop-back computes X and Y across separate original Dart calls. Keep
    // only the unboxed top inset between those sites, keyed by its native frame.
    void begin_drop(uintptr_t frame, double top) {
        Preview *slot = nullptr;
        for (auto &p : drops) if (p.frame == frame) { slot = &p; break; }
        if (slot == nullptr) slot = &drops[next_drop++ % 8];
        *slot = {frame, top};
    }
    double finish_drop(uintptr_t frame, double fallback) {
        for (auto &p : drops) if (p.frame == frame) {
            const double top = p.top; p = {}; return top;
        }
        return fallback;
    }
};
} // namespace home_layout
