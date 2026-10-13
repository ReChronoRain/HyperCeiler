/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_geometry.h"
#include <cstdio>
#include <cmath>

int main() {
    using namespace home_layout;
    WorkspaceRenderSnapshot cache;
    alignas(16) unsigned char mem[2048]{};
    const auto grid = reinterpret_cast<uintptr_t>(mem + 256);
    const auto clone = reinterpret_cast<uintptr_t>(mem + 512);
    const uintptr_t grids[] = {grid, clone};
    // folder_grid_geometry() now takes the resolved GridFieldOffsets instead of
    // trusting baked-in slot numbers, because the real ones are decoded per launcher
    // image. This fixture owns its own synthetic heap, so it states the same numbers
    // as a GridFieldOffsets value - what is under test is the snapshot lookup, not the
    // decoding, and read_grid_field_offsets() has its own dedicated suite.
    const GridFieldOffsets kFields{0x1b, 0x23, 0x0b, 0x37, 0x3f, 0x2b, 0x33, 0x43, 0x0b, 0x0b, 0x0b};
    if (!kFields.usable()) { std::puts("fixture offsets rejected"); return 1; }
    for (auto p : grids) {
        workspace_write(p, kFields.columns, int64_t{4});
        workspace_write(p, kFields.rows, int64_t{6});
        workspace_write(p, kFields.cell_width, 92.0);
        workspace_write(p, kFields.cell_height, 100.0);
    }
    int failures = 0, checks = 0;
    const auto equal = [&](double a, double b) { ++checks; if (std::abs(a-b)>1e-9) ++failures; };
    // A live settings publication does not imply RenderBox.performLayout.
    // currentConfig returns a clone, or GC moves its identity, before close.
    cache.rendered(grid,4,6,92,100,0,-30,92,85);
    double g[4];
    if (!folder_grid_geometry(clone,80,44,0,g,&cache,&kFields)) ++failures;
    equal(g[1],-30); equal(g[3],85);
    for (int row=0; row<6; ++row) equal(g[1]+row*g[3],-30+row*85.0);
    // A later real layout supersedes the old clone. No identity-based stale hit.
    cache.rendered(clone,4,6,92,100,0,80,92,100-124.0/6);
    if (!folder_grid_geometry(grid,-30,120,0,g,&cache,&kFields)) ++failures;
    equal(g[1],80); equal(g[3],100-124.0/6);
    // Disabled geometry is published too, so another identity cannot resurrect
    // the previous enabled margin. Different row/stride configurations miss.
    cache.rendered(clone,4,6,92,100,0,0,92,100);
    if (!folder_grid_geometry(grid,80,44,0,g,&cache,&kFields)) ++failures;
    equal(g[1],0); equal(g[3],100);
    ++checks; if (cache.geometry(grid,4,7,92,100,g)) ++failures;
    ++checks; if (cache.geometry(grid,4,6,92,101,g)) ++failures;
    // An unresolved field set must refuse rather than read through -1 offsets.
    const GridFieldOffsets kUnresolved{};
    ++checks; if (folder_grid_geometry(grid,80,44,0,g,&cache,&kUnresolved)) ++failures;
    std::puts(failures ? "folder clone/live-margin regression: FAIL (close target uses unrendered settings)"
        : "folder clone/live-margin regression: PASS (rendered target; GC/clone; latest layout; off; shape isolation)");
    return failures || checks != 15 ? 1 : 0;
}
