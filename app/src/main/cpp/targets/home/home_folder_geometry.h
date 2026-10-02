/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_workspace_geometry.h"

namespace home_layout {
// Five original-body windows, after currentConfig returns and before any
// edit-mode transform or Dart allocator. Never replace a getter's return.
struct FolderGeometrySite {
    const char *symbol;
    uint32_t size, offset;
    uint32_t words[4];
};
inline constexpr FolderGeometrySite kFolderGeometrySites[] = {
    {"WidgetPositionUtil.getCellPosition", 0x204, 0x128,
        {0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a0}},
    {"WidgetPositionUtil.getCellPosition", 0x204, 0x1cc,
        {0x9e620000, 0xfc5d83a1, 0x1e610802, 0xfc407020}},
    {"FolderIconGetxController.calOriginPreviewIconLoc", 0x2a4, 0x11c,
        {0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a1}},
    {"FolderIconGetxController.calOriginPreviewIconLoc", 0x2a4, 0x1d4,
        {0x9e620060, 0xfc5e03a1, 0x1e610802, 0xfc407020}},
    {"WidgetPositionUtil.getCellPosition", 0x204, 0x140,
        {0x1e620823, 0x1e632801, 0x4ea11c20, 0xf85e83a1}},
};

// Whole bodies lock down frame-local lifetime and every original GC-call PC.
inline constexpr uint32_t kFolderPositionOriginal[] = {
    0xa9bf79fd, 0xaa0f03fd, 0xd100c1ef, 0xaa0103e0, 0xf81f83a1, 0xaa0303e1,
    0xf81f03a2, 0xf81e83a3, 0xf9403f43, 0xf9538863, 0x6b16007f, 0x540001e1,
    0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362,
    0x94447561, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, 0x94039ad2,
    0xaa0003e1, 0x14000002, 0xaa0303e1, 0x94038502, 0xfc42b000, 0xfc1e03a0,
    0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800,
    0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362, 0x9444754b, 0x91402370,
    0xf947e210, 0xf90001f0, 0xf9402b64, 0x94039abc, 0xaa0003e1, 0x14000002,
    0xaa0003e1, 0x940384ec, 0xfc433000, 0xfc1d83a0, 0xf9403f40, 0xf9538800,
    0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f,
    0x54000061, 0xf97c0362, 0x94447535, 0x91402370, 0xf947e210, 0xf90001f0,
    0xf9402b64, 0x94039aa6, 0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f83a0,
    0xfc5e03a0, 0x940384d4, 0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a0,
    0x9e620001, 0xfc5e03a2, 0x1e620823, 0x1e632801, 0x4ea11c20, 0xf85e83a1,
    0xf9403b64, 0x9400002c, 0xfc1e03a0, 0xf9403f40, 0xf9538800, 0x6b16001f,
    0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061,
    0xf97c0362, 0x94447512, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64,
    0x94039a83, 0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f03a0, 0xfc5e03a0,
    0xfc5d83a1, 0xb84f3022, 0x8b1c8042, 0xaa0203e1, 0x94366132, 0xaa0003e1,
    0xf85f03a0, 0x9e620000, 0xfc5d83a1, 0x1e610802, 0xfc407020, 0x1e622801,
    0xfc1d83a1, 0x9439f409, 0xfc5e03a0, 0xfc007000, 0xfc5d83a0, 0xfc00f000,
    0xaa1d03ef, 0xa8c179fd, 0xd65f03c0,
};
inline constexpr uint32_t kFolderSizeOriginal[] = {
    0xa9bf79fd, 0xaa0f03fd, 0xd100e1ef, 0xf81f83a1, 0xf81f03a2, 0xf9403f40,
    0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370,
    0x6b10001f, 0x54000061, 0xf97c0362, 0x94436858, 0x91402370, 0xf947e210,
    0xf90001f0, 0xf9402b64, 0x94028dc9, 0xaa0003e1, 0x14000002, 0xaa0003e1,
    0x940277f9, 0xfc42b000, 0xfc1e83a0, 0xf9403f40, 0xf9538800, 0x6b16001f,
    0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061,
    0xf97c0362, 0x94436842, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64,
    0x94028db3, 0xaa0003e1, 0x14000002, 0xaa0003e1, 0x940277e3, 0xfc433000,
    0xfc1e03a0, 0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40,
    0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362, 0x9443682c,
    0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, 0x94028d9d, 0xaa0003e1,
    0x14000002, 0xaa0003e1, 0xf85f83a0, 0xfc5e83a0, 0x940277cb, 0xb843b001,
    0x8b1c8021, 0xfc407020, 0xf85f83a1, 0xb8483020, 0x8b1c8000, 0xf9402370,
    0x6b10001f, 0x54000ae0, 0xf8437002, 0x9e620041, 0xfc5e83a2, 0x1e620823,
    0x1e632801, 0xfc1d83a1, 0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1,
    0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362,
    0x94436807, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, 0x94028d78,
    0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f83a0, 0xfc5e83a0, 0xfc5e03a1,
    0xb84f3022, 0x8b1c8042, 0xaa0203e1, 0x94355427, 0xaa0003e1, 0xf85f83a0,
    0xb8483002, 0x8b1c8042, 0xf843f043, 0x9e620060, 0xfc5e03a1, 0x1e610802,
    0xfc407020, 0x1e622801, 0xfc1e03a1, 0xf842f041, 0xfc5d83a0, 0xf9403b64,
    0x97fef2f7, 0xf85f83a1, 0xfc1d03a0, 0xb8483020, 0x8b1c8000, 0xf841f002,
    0x9e620041, 0xfc5e83a2, 0x1e610843, 0xfc1d83a3, 0x97f9a946, 0x37200160,
    0xf85f03a0, 0xfc5d03a0, 0xfc5d83a1, 0x1e612802, 0xfc417000, 0x1e603841,
    0xfc407000, 0x1e603822, 0x4ea21c41, 0x14000006, 0xf85f03a0, 0xfc5d03a0,
    0xfc417001, 0x1e612802, 0x4ea21c41, 0xfc5e03a0, 0xfc1d83a1, 0xfc41f002,
    0x1e622803, 0xfc1e83a3, 0x9438e6d8, 0xfc5d83a0, 0xfc007000, 0xfc5e83a0,
    0xfc00f000, 0xaa1d03ef, 0xa8c179fd, 0xd65f03c0, 0x91402769, 0xf9470d29,
    0x94437207,
};

inline bool folder_grid_geometry(uintptr_t grid, double top, double bottom,
    double side, double *g, const WorkspaceRenderSnapshot *rendered = nullptr) {
    const int64_t columns = workspace_read<int64_t>(grid, 0x1b);
    const int64_t rows = workspace_read<int64_t>(grid, 0x23);
    if (rows < 2) return false;
    g[0] = g[1] = 0;
    g[2] = workspace_read<double>(grid, 0x2b);
    g[3] = workspace_read<double>(grid, 0x33);
    if (rendered && rendered->geometry(grid, columns, rows, g[2], g[3], g)) return true;
    return inset_workspace(g, columns, rows, top, bottom, side);
}

// The native save block has x0..x14 at 0..112, x18 at 120,
// x15/LR at 128/136, and q0..q31 at 160..671. Change only the
// outputs of the five strictly verified displaced instructions. Upper SIMD
// lanes retain their original contents (LDUR D clears the upper 64 bits).
inline bool folder_geometry_body(uintptr_t fp, uint64_t heap,
    uintptr_t saved, unsigned kind, double top, double bottom, double side,
    WorkspaceRenderSnapshot *rendered = nullptr) {
    const uintptr_t x0 = workspace_read<uintptr_t>(saved, 0);
    const auto d = [saved](int n, double v, bool clear = true) {
        workspace_write(saved, 160 + n * 16, v);
        if (clear) workspace_write(saved, 168 + n * 16, uint64_t{0});
    };
    bool valid = false;
    if (kind == 0) {
        // +128: origin pointer, decompression, origin.x, saved column.
        const uintptr_t origin = workspace_read<uint32_t>(x0, 0x3b) + (heap << 32);
        double g[4];
        valid = folder_grid_geometry(x0, top, bottom, side, g, rendered);
        workspace_write(saved, 8, origin);
        d(0, workspace_read<double>(origin, 7) + (valid ? g[0] : 0));
        workspace_write(saved, 0, workspace_read<uintptr_t>(fp, -8));
        if (valid) {
            workspace_write(fp, -0x20, g[2]);
            workspace_write(fp, -0x28, g[3]);
        }
        // The outgoing-argument slot is temporary ONLY until +140, with
        // no intervening Dart call. Kind 4 moves this scalar into the dead
        // screen-index local AFTER its last read, before transformPointX.
        // Thus later Inst.find outgoing arguments cannot overwrite the delta.
        workspace_write(fp, -0x30, valid ? g[1] : 0.0);
    } else if (kind == 1) {
        // +1cc: row conversion, changed stride, product, original origin.y.
        const uintptr_t origin = workspace_read<uintptr_t>(saved, 8);
        const double height = workspace_read<double>(fp, -0x28);
        d(0, workspace_read<double>(origin, 7) + workspace_read<double>(fp, -0x18));
        d(1, height);
        d(2, static_cast<double>(static_cast<int64_t>(x0)) * height);
        valid = true;
    } else if (kind == 4) {
        // +140: original column*stride and x-origin sum, MOV V0,V1,
        // final screen-index read. Only then reuse the dead unboxed local.
        const double left = workspace_read<double>(saved, 160);
        const double product = workspace_read<double>(saved, 176)
            * workspace_read<double>(saved, 192);
        d(3, product); d(1, left + product);
        std::memcpy(reinterpret_cast<void *>(saved + 160),
            reinterpret_cast<const void *>(saved + 176), 16);
        workspace_write(saved, 8, workspace_read<uintptr_t>(fp, -0x18));
        workspace_write(fp, -0x18, workspace_read<double>(fp, -0x30));
        valid = true;
    } else if (kind == 2) {
        // calOriginPreviewIconLoc +11c, not the rendered folder widget size.
        const uintptr_t origin = workspace_read<uint32_t>(x0, 0x3b) + (heap << 32);
        double g[4]; valid = folder_grid_geometry(x0, top, bottom, side, g, rendered);
        if (rendered) rendered->begin_preview(fp, valid ? g[1] : 0);
        workspace_write(saved, 8, workspace_read<uintptr_t>(fp, -8));
        d(0, workspace_read<double>(origin, 7) + (valid ? g[0] : 0));
        if (valid) {
            workspace_write(fp, -0x18, g[2]);
            workspace_write(fp, -0x20, g[3]);
        }
    } else if (kind == 3) {
        // +1d4: row conversion/stride/product/original margin Rx double.
        // No pointer or scalar is carried through an outgoing-argument slot.
        const uintptr_t margin = workspace_read<uintptr_t>(saved, 8);
        const int64_t row = workspace_read<int64_t>(saved, 24);
        const double height = workspace_read<double>(fp, -0x20);
        if (rendered) top = rendered->finish_preview(fp, top);
        valid = std::isfinite(top) && top >= -30 && top <= 120
            && std::isfinite(height) && height >= 1 && row >= 0 && row < 32;
        d(0, workspace_read<double>(margin, 7) + (valid ? top : 0));
        d(1, height); d(2, static_cast<double>(row) * height);
    }

    return valid;
}
} // namespace home_layout
