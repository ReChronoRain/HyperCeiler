/* SPDX-License-Identifier: AGPL-3.0-or-later */
#if __has_include("targets/home/home_drop_geometry.h")
#include "targets/home/home_drop_geometry.h"
#define DROP_GEOMETRY 1
#else
#define DROP_GEOMETRY 0
#endif
#include <cstdio>
#include <cmath>

int main() {
#if !DROP_GEOMETRY
    std::puts("drop-back rendered-frame regression: FAIL (raw grid center)");
    return 1;
#else
    using namespace home_layout;
    alignas(16) static unsigned char memory[4096]{};
    const uintptr_t fp = reinterpret_cast<uintptr_t>(memory + 256);
    const uintptr_t grid = reinterpret_cast<uintptr_t>(memory + 512);
    const uintptr_t origin = reinterpret_cast<uintptr_t>(memory + 768);
    const uintptr_t saved = reinterpret_cast<uintptr_t>(memory + 1024);
    WorkspaceRenderSnapshot rendered;
    int failed = 0, checks = 0;
    // drop_geometry_body() reads the grid through a resolved GridFieldOffsets rather than
    // baked-in slots, because the real ones are decoded per launcher image. This fixture
    // owns its own synthetic heap and states the same numbers explicitly; the decoding
    // itself is covered by read_grid_field_offsets()'s own suite. Every entry below is a
    // slot this file actually writes, and each also satisfies dart_plausible_field()
    // ((imm + 1) & 3 == 0), so the fixture is a legal decode target rather than a set of
    // numbers the helpers happen to accept. drop_geometry_fields() is a pass-through, so
    // stating the value once covers both spellings of the same resolved field set.
    const DropGeometryFields kDrop = drop_geometry_fields(
        GridFieldOffsets{0x1b, 0x23, 0x3b, 0x37, 0x3f, 0x2b, 0x33, 0x17, 0x13, 0x0f, 0x07});
    ++checks; if (!kDrop.usable()) ++failed;
    workspace_write(grid, kDrop.columns, int64_t{4});
    workspace_write(grid, kDrop.rows, int64_t{7});
    workspace_write(grid, kDrop.cell_width, 90.0);
    workspace_write(grid, kDrop.cell_height, 100.0);
    workspace_write(grid, kDrop.origin, static_cast<uint32_t>(origin));
    workspace_write(origin, 7, 20.0);
    workspace_write(fp, -0x10, int64_t{2});
    workspace_write(fp, -0x18, int64_t{6});
    workspace_write(fp, -8, uintptr_t{0x1234});
    const auto eq = [&](double actual, double expected) {
        ++checks;
        if (std::abs(actual - expected) > 1e-9) {
            std::printf("mismatch %d: actual=%.6f expected=%.6f\n", checks, actual, expected);
            ++failed;
        }
    };
    const auto render = [&](double top, double bottom, double side) {
        workspace_write(fp, -8, grid);
        workspace_write(fp, -0x58, 20.0);
        workspace_write(fp, -0x40, 30.0);
        workspace_write(fp, -0x50, 90.0);
        workspace_write(fp, -0x48, 100.0);
        ++checks;
        if (!inset_workspace_frame(fp, grid >> 32, false, &kDrop, top, bottom, side,
                nullptr, &rendered)) { std::puts("render failed"); ++failed; }
        workspace_write(fp, -8, uintptr_t{0x1234});
    };
    const auto drop = [&](double settings_top, double settings_bottom, double settings_side,
        double expected_top, double expected_bottom, double expected_side, uintptr_t source = 0) {
        workspace_write(fp, -0x38, 90.0);
        workspace_write(fp, -0x40, 100.0);
        workspace_write(saved, 0, source ? source : grid);
        ++checks;
        if (!drop_geometry_body(fp, grid >> 32, saved, 0, &kDrop,
                settings_top, settings_bottom, settings_side, &rendered)) { std::puts("drop x failed"); ++failed; }
        const double width = 90 - 2 * expected_side / 4;
        const double height = 100 - (expected_top + expected_bottom) / 7;
        eq(workspace_read<double>(saved, 160) + 2 * workspace_read<double>(fp, -0x38),
            20 + expected_side + 2 * width);
        eq(workspace_read<double>(fp, -0x40), height);
        workspace_write(origin, 7, 30.0);
        workspace_write(saved, 8, origin);
        workspace_write(saved, 160, 6.0);
        workspace_write(saved, 176, height);
        ++checks;
        if (!drop_geometry_body(fp, grid >> 32, saved, 1, &kDrop,
                settings_top, settings_bottom, settings_side, &rendered)
                && expected_top != 0) { std::puts("drop y failed"); ++failed; }
        eq(workspace_read<double>(saved, 176), 30 + 6 * height + expected_top);
        eq(static_cast<double>(workspace_read<uintptr_t>(saved, 8)), 0x1234);
        workspace_write(origin, 7, 20.0);
    };
    // The settings generation is newer than the RenderBox frame: drop-back
    // must agree with what was actually painted, not with the fresh slider.
    render(0, 44, 15);
    drop(80, 120, 0, 0, 44, 15);
    render(20, 44, 15);
    drop(0, 0, 0, 20, 44, 15);
    render(0, 0, 0);
    drop(80, 120, 15, 0, 0, 0);
    // A moved/clone GridInfo must still use the rendered, disabled geometry.
    const auto clone = reinterpret_cast<uintptr_t>(memory + 1792);
    std::memcpy(reinterpret_cast<void *>(clone), reinterpret_cast<void *>(grid), 128);
    drop(80, 120, 15, 0, 0, 0, clone);
    // A changed grid shape must refuse stale snapshots and use current
    // settings; the untouched GridInfo and native pointer base never change.
    workspace_write(grid, kDrop.cell_height, 110.0);
    workspace_write(fp, -0x38, 90.0);
    workspace_write(fp, -0x40, 110.0);
    workspace_write(saved, 0, grid);
    ++checks;
    if (!drop_geometry_body(fp, grid >> 32, saved, 0, &kDrop, 0, 44, 0, &rendered)) ++failed;
    eq(workspace_read<double>(fp, -0x40), 110 - 44.0 / 7);
    if (checks < 24) ++failed;
    std::puts(failed
        ? "drop-back rendered-frame regression: FAIL (raw grid center)"
        : "drop-back rendered-frame regression: PASS (rendered x/y; settings race; reset; grid change)");
    return failed ? 1 : 0;
#endif
}
