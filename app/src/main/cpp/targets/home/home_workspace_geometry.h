/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cmath>
#include <cstdint>
#include <cstring>
#include "home_dart_fields.h"
#include "home_dynamic_layout.h"
#include "home_workspace_snapshot.h"

namespace home_layout {
// Original RenderBox layout's local geometry, not a shared Dart heap object.
// The original children use origin + cell index * stride, including
// multi-cell widgets. Change both terms, rather than translating the whole tree.
inline bool inset_workspace(double *g, int64_t columns, int64_t rows,
    double top, double bottom, double side) {
    if (g == nullptr || columns < 1 || columns > 32 || rows < 1 || rows > 32
        || !std::isfinite(top) || !std::isfinite(bottom) || !std::isfinite(side)
        || top < -30 || top > 120 || bottom < -120 || bottom > 120
        || side < -20 || side > 80) return false;
    for (int i = 0; i < 4; ++i) if (!std::isfinite(g[i])) return false;
    const double width = g[2] - 2 * side / static_cast<double>(columns);
    const double height = g[3] - (top + bottom) / static_cast<double>(rows);
    if (width < 1 || height < 1 || width > 4096 || height > 4096) return false;
    g[0] += side;
    g[1] += top;
    g[2] = width;
    g[3] = height;
    return true;
}

template <typename T> inline T workspace_read(uintptr_t p, intptr_t off) {
    T v;
    std::memcpy(&v, reinterpret_cast<const void *>(p + off), sizeof(v));
    return v;
}
template <typename T> inline void workspace_write(uintptr_t p, intptr_t off, T v) {
    std::memcpy(reinterpret_cast<void *>(p + off), &v, sizeof(v));
}

// Offsets are admitted only after the original instruction windows match.
// Occupied cells are recalculated from pristine GridInfo on EVERY iteration:
// the preceding iteration's changed constraint locals must never compound.
//
// Heap fields and Dart frame slots come from verified owning consumers.
inline bool inset_workspace_frame(uintptr_t fp, uint64_t heap, bool occupied,
    const GridFieldOffsets *field, double top, double bottom, double side, double *out,
    WorkspaceRenderSnapshot *rendered = nullptr, const home_dynamic::GeometryFrame *layout = nullptr,
    double base_height = 0) {
    if (!layout || !layout->usable() || field == nullptr || field->columns <= 0 || field->rows <= 0) return false;
    uintptr_t grid = workspace_read<uintptr_t>(fp, layout->grid);
    if (occupied) {
        if (field->occupied_grid <= 0) return false;
        grid = workspace_read<uint32_t>(grid, field->occupied_grid) + (heap << 32);
    }
    const int64_t columns = workspace_read<int64_t>(grid, field->columns);
    const int64_t rows = workspace_read<int64_t>(grid, field->rows);
    // A one-row hotseat is a separate tree and retains its original geometry.
    if (rows < 2) return false;
    if (occupied && (field->cell_width <= 0 || field->cell_height <= 0)) return false;
    const double cell_width = occupied ? workspace_read<double>(grid, field->cell_width)
                                       : workspace_read<double>(fp, layout->width);
    double cell_height = occupied ? workspace_read<double>(grid, field->cell_height)
                                  : workspace_read<double>(fp, layout->height);
    /*
     * The launcher hands the workspace a shorter box while the long-press sheet owns the bottom
     * of the screen. Subtracting the configured bottom inset from that temporary height shrinks
     * every cell and lifts the whole grid, then the sheet closes and it drops back. The inset was
     * calibrated against the resting height, so keep using it while the box is temporarily
     * shorter; a real box change (rotation) also changes the width and refreshes the baseline.
     */
    if (!occupied && base_height > 1 && cell_height > 1 && cell_height < base_height * 0.9)
        cell_height = base_height;
    double g[] = {
        workspace_read<double>(fp, layout->x),
        occupied ? 0 : workspace_read<double>(fp, layout->y),
        cell_width,
        cell_height,
    };
    if (!inset_workspace(g, columns, rows, top, bottom, side)) return false;
    if (occupied) {
        if (field->item_col <= 0 || field->item_row <= 0) return false;
        const uintptr_t info = workspace_read<uintptr_t>(fp, layout->info);
        const int64_t col = workspace_read<int64_t>(info, field->item_col);
        const int64_t row = workspace_read<int64_t>(info, field->item_row);
        if (col < 0 || col >= columns || row < 0 || row >= rows) return false;
        workspace_write(fp, layout->out_x, g[0] + col * g[2]);
        workspace_write(fp, layout->out_y, g[1] + row * g[3]);
        workspace_write(fp, layout->out_width, g[2]);
        workspace_write(fp, layout->out_height, g[3]);
    } else {
        workspace_write(fp, layout->out_x, g[0]);
        workspace_write(fp, layout->out_y, g[1]);
        workspace_write(fp, layout->out_width, g[2]);
        workspace_write(fp, layout->out_height, g[3]);
    }
    // Publish only the geometry this original layout actually used, including
    // zero insets when disabled. Latest settings alone are not a rendered frame.
    if (rendered) rendered->rendered(grid, columns, rows,
        cell_width, cell_height, side, top, g[2], g[3]);
    if (out) std::memcpy(out, g, sizeof(g));
    return true;
}

// HotSeatLayoutDelegate.cellLayout uses a different RenderBox layout from the
// workspace delegates. Move its final ParentData.offset.x, not its shared
// config or icon constraints. Match the workspace's symmetric cell-center
// displacement: side * (1 - (2 * column + 1) / columns).
inline bool inset_hotseat_frame(uintptr_t fp, uint64_t heap, double side,
    double *out, const GridFieldOffsets *field = nullptr, const home_dynamic::GeometryFrame *layout = nullptr) {
    if (!layout || !layout->usable(true) || !field || field->dock_columns <= 0 || field->dock_item <= 0
        || field->dock_info <= 0 || field->item_col <= 0) return false;
    if (!std::isfinite(side) || side < -20 || side > 80 || side == 0) return false;
    const uintptr_t delegate = workspace_read<uintptr_t>(fp, layout->grid);
    const int64_t columns = workspace_read<int64_t>(delegate, field->dock_columns);
    const uintptr_t closure = workspace_read<uintptr_t>(fp, layout->closure);
    const uintptr_t item = workspace_read<uint32_t>(closure, field->dock_item) + (heap << 32);
    const uintptr_t info = workspace_read<uint32_t>(item, field->dock_info) + (heap << 32);
    const int64_t column = workspace_read<int64_t>(info, field->item_col);
    const double original_x = workspace_read<double>(fp, layout->x);
    if (columns < 1 || columns > 32 || column < 0 || column >= columns
        || !std::isfinite(original_x)) return false;
    const double offset = side * (1 - (2.0 * column + 1) / columns);
    const double x = original_x + offset;
    if (!std::isfinite(x)) return false;
    workspace_write(fp, layout->x, x);
    if (out) { out[0] = original_x; out[1] = x; out[2] = offset; out[3] = columns; }
    return true;
}
} // namespace home_layout
