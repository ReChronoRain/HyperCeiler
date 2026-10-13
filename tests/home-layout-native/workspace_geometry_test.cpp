/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_workspace_geometry.h"
#include <cassert>
#include <cstdio>
#include <cstring>
#include <limits>

int main() {
    const double baseline[] = {20, 30, 90, 100};
    double g[4];
    const auto reset = [&] { std::memcpy(g, baseline, sizeof(g)); };
    const auto eq = [](double a, double b) { assert(std::abs(a - b) < 1e-9); };
    reset(); assert(home_layout::inset_workspace(g, 4, 6, 0, 0, 0));
    assert(std::memcmp(g, baseline, sizeof(g)) == 0);
    reset(); assert(home_layout::inset_workspace(g, 4, 6, 20, 0, 0));
    eq(g[1], 50); eq(g[1] + 6 * g[3], 630);
    reset(); assert(home_layout::inset_workspace(g, 4, 6, 0, 20, 0));
    eq(g[1], 30); eq(g[1] + 6 * g[3], 610);
    reset(); assert(home_layout::inset_workspace(g, 4, 6, 0, 0, 15));
    eq(g[0], 35); eq(g[0] + 4 * g[2], 365);
    // Rebuild from pristine input, never compound the previous frame's deltas.
    for (int i = 0; i < 1000; ++i) {
        reset(); assert(home_layout::inset_workspace(g, 4, 6, 20, 30, 15));
        eq(g[0], 35); eq(g[1], 50);
        eq(g[0] + 4 * g[2], 365); eq(g[1] + 6 * g[3], 600);
    }
    reset(); assert(home_layout::inset_workspace(g, 4, 6, -30, -120, -20));
    eq(g[0], 0); eq(g[1], 0); eq(g[0] + 4 * g[2], 400);
    eq(g[1] + 6 * g[3], 750);
    reset(); assert(home_layout::inset_workspace(g, 4, 6, 120, 120, 80));
    eq(g[0] + 4 * g[2], 300); eq(g[1] + 6 * g[3], 510);
    reset(); assert(!home_layout::inset_workspace(g, 0, 6, 20, 20, 15));
    assert(std::memcmp(g, baseline, sizeof(g)) == 0);
    assert(!home_layout::inset_workspace(g, 4, 6, 121, 0, 0));
    assert(!home_layout::inset_workspace(g, 4, 6, 0, 0, 81));
    assert(!home_layout::inset_workspace(g, 4, 6,
        std::numeric_limits<double>::quiet_NaN(), 0, 0));
    g[2] = 1; assert(!home_layout::inset_workspace(g, 4, 6, 0, 0, 80));
    using home_layout::workspace_write;
    using home_layout::workspace_read;
    alignas(16) unsigned char storage[1024] = {};
    const uintptr_t fp = reinterpret_cast<uintptr_t>(storage + 256);
    const uintptr_t grid = reinterpret_cast<uintptr_t>(storage + 384);
    const uintptr_t delegate = reinterpret_cast<uintptr_t>(storage + 512);
    const uintptr_t info = reinterpret_cast<uintptr_t>(storage + 640);
    // inset_workspace_frame() reads the grid through a resolved GridFieldOffsets rather
    // than baked-in slots, because the real ones are decoded per launcher image. This
    // fixture owns its own synthetic heap and states the same numbers explicitly; the
    // decoding itself is covered by read_grid_field_offsets()'s own suite. Every entry
    // below is a slot this file actually writes through kFields, and each one also
    // satisfies dart_plausible_field() ((imm + 1) & 3 == 0), so the fixture is a legal
    // decode target and not just a set of numbers the helpers happen to accept.
    // origin is never dereferenced by either helper; it is stated only because
    // usable() requires a resolved value for all seven grid slots.
    const home_layout::GridFieldOffsets kFields{0x1b, 0x23, 0x0b, 0x37, 0x3f, 0x2b, 0x33, 0x17, 0x13, 0x0f, 0x07};
    assert(kFields.usable());
    workspace_write(grid, kFields.columns, int64_t{4});
    workspace_write(grid, kFields.rows, int64_t{6});
    workspace_write(grid, kFields.cell_width, 90.0);
    workspace_write(grid, kFields.cell_height, 100.0);
    workspace_write(fp, -8, grid);
    workspace_write(fp, -0x58, 20.0);
    workspace_write(fp, -0x40, 0.0);
    workspace_write(fp, -0x50, 90.0);
    workspace_write(fp, -0x48, 100.0);
    assert(home_layout::inset_workspace_frame(fp, grid >> 32, false, &kFields, 20, 30, 15, g));
    eq(workspace_read<double>(fp, -0x58), 35);
    eq(workspace_read<double>(fp, -0x40), 20);
    eq(workspace_read<double>(fp, -0x50), 82.5);
    eq(workspace_read<double>(fp, -0x48), 100 - 50.0 / 6);
    workspace_write(fp, -8, delegate);
    workspace_write(delegate, kFields.occupied_grid, static_cast<uint32_t>(grid));
    workspace_write(fp, -0x10, info);
    workspace_write(info, kFields.item_col, int64_t{3});
    workspace_write(info, kFields.item_row, int64_t{5});
    workspace_write(fp, -0x50, 20.0);
    for (int i = 0; i < 1000; ++i) {
        assert(home_layout::inset_workspace_frame(fp, grid >> 32, true, &kFields, 20, 30, 15, g));
        eq(workspace_read<double>(fp, -0x70), 282.5);
        eq(workspace_read<double>(fp, -0x68), 20 + 5 * (100 - 50.0 / 6));
        eq(workspace_read<double>(fp, -0x60), 82.5);
    }
    workspace_write(grid, kFields.rows, int64_t{1});
    assert(!home_layout::inset_workspace_frame(fp, grid >> 32, true, &kFields, 20, 30, 15, g));

    // The real Dock uses HotSeatLayoutDelegate, not either workspace delegate.
    // Reproduce its compressed per-item chain and final Offset.x stack local.
    const uintptr_t closure = grid;
    const uintptr_t item = reinterpret_cast<uintptr_t>(storage + 768);
    workspace_write(fp, -8, delegate);
    workspace_write(fp, -0x50, closure);
    workspace_write(closure, kFields.dock_item, static_cast<uint32_t>(item));
    workspace_write(item, kFields.dock_info, static_cast<uint32_t>(info));
    const double sides[] = {-20, 0, 15, 80};
    unsigned char snapshot[sizeof(storage)];
    for (int64_t columns = 1; columns <= 8; ++columns) {
        workspace_write(delegate, kFields.dock_columns, columns);
        for (const double side : sides) {
            double total_shift = 0;
            for (int64_t column = 0; column < columns; ++column) {
                workspace_write(info, kFields.item_col, column);
                const double x = 20 + column * 90;
                workspace_write(fp, -0x80, x);
                workspace_write(fp, -0x88, 37.0);
                workspace_write(fp, -0x70, 100.0);
                std::memcpy(snapshot, storage, sizeof(storage));
                const bool changed = home_layout::inset_hotseat_frame(fp, info >> 32, side, g, &kFields);
                assert(changed == (side != 0));
                const double shift = side * (1 - (2.0 * column + 1) / columns);
                eq(workspace_read<double>(fp, -0x80), x + shift);
                total_shift += shift;
                // No other stack, heap, vertical or size bytes may change.
                std::memcpy(snapshot + 256 - 0x80, storage + 256 - 0x80, sizeof(double));
                assert(std::memcmp(snapshot, storage, sizeof(storage)) == 0);
                if (columns == 1) eq(shift, 0);
            }
            eq(total_shift, 0);
        }
    }
    // Each original layout recomputes x before the splice; 1000 frames cannot drift.
    workspace_write(delegate, kFields.dock_columns, int64_t{4});
    workspace_write(info, kFields.item_col, int64_t{3});
    for (int i = 0; i < 1000; ++i) {
        workspace_write(fp, -0x80, 290.0);
        assert(home_layout::inset_hotseat_frame(fp, info >> 32, 15, g, &kFields));
        eq(workspace_read<double>(fp, -0x80), 278.75);
    }
    // Disabled/extreme-invalid/non-finite inputs and column/count guards are no-ops.
    const double invalid_sides[] = {0, -21, 81, std::numeric_limits<double>::quiet_NaN()};
    for (const double side : invalid_sides) {
        std::memcpy(snapshot, storage, sizeof(storage));
        assert(!home_layout::inset_hotseat_frame(fp, info >> 32, side, g, &kFields));
        assert(std::memcmp(snapshot, storage, sizeof(storage)) == 0);
    }
    const int64_t invalid_columns[] = {-1, 4};
    for (const int64_t column : invalid_columns) {
        workspace_write(info, kFields.item_col, column);
        std::memcpy(snapshot, storage, sizeof(storage));
        assert(!home_layout::inset_hotseat_frame(fp, info >> 32, 15, g, &kFields));
        assert(std::memcmp(snapshot, storage, sizeof(storage)) == 0);
    }
    workspace_write(info, kFields.item_col, int64_t{0});
    const int64_t invalid_counts[] = {0, 33};
    for (const int64_t columns : invalid_counts) {
        workspace_write(delegate, kFields.dock_columns, columns);
        std::memcpy(snapshot, storage, sizeof(storage));
        assert(!home_layout::inset_hotseat_frame(fp, info >> 32, 15, g, &kFields));
        assert(std::memcmp(snapshot, storage, sizeof(storage)) == 0);
    }
    workspace_write(delegate, kFields.dock_columns, int64_t{4});
    workspace_write(fp, -0x80, std::numeric_limits<double>::quiet_NaN());
    std::memcpy(snapshot, storage, sizeof(storage));
    assert(!home_layout::inset_hotseat_frame(fp, info >> 32, 15, g, &kFields));
    assert(std::memcmp(snapshot, storage, sizeof(storage)) == 0);
    std::puts("workspace geometry: neutral/top/bottom/symmetric-side/combined/extremes/guards/rebuild ok");
    std::puts("workspace frame: grid/occupied/pristine-per-child/hotseat-exclusion ok");
    std::puts("dock horizontal: 1..8-icons/symmetric/signed-extremes/1000-frames/no-heap-or-Y-or-size-writes/guards ok");
}
