/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cmath>
#include <cstdint>
#include <cstring>

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
inline bool inset_workspace_frame(uintptr_t fp, uint64_t heap, bool occupied,
    double top, double bottom, double side, double *out) {
    uintptr_t grid = workspace_read<uintptr_t>(fp, -8);
    if (occupied) grid = workspace_read<uint32_t>(grid, 0x17) + (heap << 32);
    const int64_t columns = workspace_read<int64_t>(grid, 0x1b);
    const int64_t rows = workspace_read<int64_t>(grid, 0x23);
    // A one-row hotseat is a separate tree and retains its original geometry.
    if (rows < 2) return false;
    double g[] = {
        workspace_read<double>(fp, occupied ? -0x50 : -0x58),
        occupied ? 0 : workspace_read<double>(fp, -0x40),
        occupied ? workspace_read<double>(grid, 0x2b) : workspace_read<double>(fp, -0x50),
        occupied ? workspace_read<double>(grid, 0x33) : workspace_read<double>(fp, -0x48),
    };
    if (!inset_workspace(g, columns, rows, top, bottom, side)) return false;
    if (occupied) {
        const uintptr_t info = workspace_read<uintptr_t>(fp, -0x10);
        const int64_t col = workspace_read<int64_t>(info, 0x37);
        const int64_t row = workspace_read<int64_t>(info, 0x3f);
        if (col < 0 || col >= columns || row < 0 || row >= rows) return false;
        workspace_write(fp, -0x70, g[0] + col * g[2]);
        workspace_write(fp, -0x68, g[1] + row * g[3]);
        workspace_write(fp, -0x60, g[2]);
        workspace_write(fp, -0x58, g[3]);
    } else {
        workspace_write(fp, -0x58, g[0]);
        workspace_write(fp, -0x40, g[1]);
        workspace_write(fp, -0x50, g[2]);
        workspace_write(fp, -0x48, g[3]);
    }
    if (out) std::memcpy(out, g, sizeof(g));
    return true;
}
} // namespace home_layout
