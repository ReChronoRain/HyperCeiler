/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_geometry.h"
#include <cstdio>
#include <cstring>
#if __has_include("targets/home/home_workspace_snapshot.h")
#define SNAPSHOT 1
#else
#define SNAPSHOT 0
#endif

int main() {
    using namespace home_layout;
    alignas(16) static unsigned char memory[4096]{};
    const uintptr_t fp = reinterpret_cast<uintptr_t>(memory + 256);
    const uintptr_t grid = reinterpret_cast<uintptr_t>(memory + 512);
    const uintptr_t origin = reinterpret_cast<uintptr_t>(memory + 768);
    const uintptr_t saved = reinterpret_cast<uintptr_t>(memory + 1024);
    const uintptr_t delegate = reinterpret_cast<uintptr_t>(memory + 1792);
    const uintptr_t info = reinterpret_cast<uintptr_t>(memory + 2048);
    // The render/snapshot helpers read the grid through a resolved GridFieldOffsets rather
    // than baked-in slots, because the real ones are decoded per launcher image. This
    // fixture owns its own synthetic heap and states the same numbers explicitly; the
    // decoding itself is covered by read_grid_field_offsets()'s own suite. Every entry
    // below is a slot this file actually writes, and each also satisfies
    // dart_plausible_field() ((imm + 1) & 3 == 0), so the fixture is a legal decode
    // target rather than a set of numbers the helpers happen to accept.
    int failed = 0, checks = 0;
    const GridFieldOffsets kFields{0x1b, 0x23, 0x3b, 0x37, 0x3f, 0x2b, 0x33, 0x17, 0x13, 0x0f, 0x07};
    ++checks; if (!kFields.usable()) ++failed;
    workspace_write(grid, kFields.columns, int64_t{4});
    workspace_write(grid, kFields.rows, int64_t{7});
    workspace_write(grid, kFields.cell_width, 90.0);
    workspace_write(grid, kFields.cell_height, 100.0);
    workspace_write(grid, kFields.origin, static_cast<uint32_t>(origin));
    unsigned char pristine[256]; std::memcpy(pristine, memory + 512, sizeof(pristine));
#if SNAPSHOT
    WorkspaceRenderSnapshot cache;
#endif
    const auto eq = [&](double a, double b) {
        ++checks; if (std::abs(a-b) > 1e-9) ++failed;
    };
    const auto render = [&](double top, double bottom, double side, bool occupied) {
        workspace_write(fp, -8, occupied ? delegate : grid);
        workspace_write(delegate, kFields.occupied_grid, static_cast<uint32_t>(grid));
        workspace_write(fp, -0x10, info);
        workspace_write(info, kFields.item_col, int64_t{2});
        workspace_write(info, kFields.item_row, int64_t{6});
        workspace_write(fp, -0x58, 20.0); workspace_write(fp, -0x40, 30.0);
        workspace_write(fp, -0x50, occupied ? 20.0 : 90.0);
        workspace_write(fp, -0x48, workspace_read<double>(grid, kFields.cell_height));
        double g[4];
        const bool ok = inset_workspace_frame(fp, grid >> 32, occupied, &kFields, top, bottom, side, g
#if SNAPSHOT
            , &cache
#endif
        );
        ++checks; if (!ok) ++failed;
    };
    const auto geometry = [&](double top, double bottom, double side,
        double expected_top, double expected_side, double width, double height) {
        double g[4];
        const bool ok = folder_grid_geometry(grid, top, bottom, side, g
#if SNAPSHOT
            , &cache
#endif
            , &kFields
        );
        ++checks; if (!ok) ++failed;
        eq(g[0], expected_side); eq(g[1], expected_top); eq(g[2], width); eq(g[3], height);
        // Every row, not just the first-row small-folder recording.
        for (int row=0; row<7; ++row) eq(g[1] + row*g[3], expected_top + row*height);
    };
    // Only bottom enabled: settings can change before another RenderBox layout.
    render(0, 44, 0, false);
    geometry(0, 120, 0, 0, 0, 90, 100-44.0/7);
    workspace_write(fp, -8, int64_t{2}); workspace_write(fp, -0x18, int64_t{7});
    workspace_write(fp, -0x20, 90.0); workspace_write(fp, -0x28, 100.0);
    workspace_write(origin, 7, 20.0); workspace_write(saved, 0, grid);
    folder_geometry_body(fp, grid>>32, saved, 0, 0, 120, 0
#if SNAPSHOT
        , &cache
#endif
            , &kFields
    );
    workspace_write(saved, 176, 2.0);
    workspace_write(saved, 192, workspace_read<double>(fp, -0x20));
    folder_geometry_body(fp, grid>>32, saved, 4, 0, 0, 0
#if SNAPSHOT
        , &cache
#endif
            , &kFields
    );
    eq(workspace_read<double>(saved, 160), 200);
    workspace_write(fp, -0x30, uintptr_t{0x12345678});
    workspace_write(origin, 7, 30.0); workspace_write(saved, 8, origin);
    workspace_write(saved, 0, int64_t{6});
    folder_geometry_body(fp, grid>>32, saved, 1, 0, 0, 0
#if SNAPSHOT
        , &cache
#endif
            , &kFields
    );
    eq(workspace_read<double>(saved, 160)+workspace_read<double>(saved,192), 30+6*(100-44.0/7));
    workspace_write(origin, 7, 0.0);
    // The next real layout, including occupied multi-cell delegates, replaces it.
    render(0, 120, 0, true);
    geometry(0, -120, 0, 0, 0, 90, 100-120.0/7);
    // Disabling must record a zero-inset layout, not keep the old margin forever.
    render(0, 0, 0, false);
    geometry(0, 44, 0, 0, 0, 90, 100);
    // The preview x/y parts must consume one rendered top/stride snapshot.
    render(20, 44, 15, false);
    workspace_write(origin, 7, 20.0); workspace_write(saved, 0, grid);
    workspace_write(fp, -8, uintptr_t{123});
    workspace_write(fp, -0x18, 90.0); workspace_write(fp, -0x20, 100.0);
    folder_geometry_body(fp, grid >> 32, saved, 2, 80, 120, 0
#if SNAPSHOT
        , &cache
#endif
            , &kFields
    );
    eq(workspace_read<double>(saved, 160), 35);
    eq(workspace_read<double>(fp, -0x20), 100-64.0/7);
    // Reproduce a settings publication and overwritten Dart outgoing argument.
    workspace_write(fp, -0x38, uintptr_t{0x12345678});
    workspace_write(origin, 7, 30.0); workspace_write(saved, 8, origin);
    workspace_write(saved, 24, int64_t{6});
    folder_geometry_body(fp, grid >> 32, saved, 3, 0, 0, 0
#if SNAPSHOT
        , &cache
#endif
            , &kFields
    );
    eq(workspace_read<double>(saved, 160) + workspace_read<double>(saved, 192),
        50 + 6*(100-64.0/7));
    // Grid shape changes and unseen configurations must not use stale geometry.
    workspace_write(grid, kFields.cell_height, 110.0);
    geometry(0, 70, 0, 0, 0, 90, 100);
    workspace_write(grid, kFields.cell_height, 100.0);
    ++checks; if (std::memcmp(pristine, memory+512, sizeof(pristine)) != 0) ++failed;
#if SNAPSHOT
    // Bounded entries: no eviction or nested invocation leaks another grid/frame.
    WorkspaceRenderSnapshot bounded;
    for (uintptr_t i=1; i<=9; ++i) bounded.rendered(i,4,7,90,100+i,0,0,90,100-i);
    double g[4]; ++checks; if (bounded.geometry(1,4,7,90,101,g)) ++failed;
    ++checks; if (!bounded.geometry(9,4,7,90,109,g)) ++failed;
    eq(g[3],91);
    bounded.begin_preview(100,20); bounded.begin_preview(200,30);
    eq(bounded.finish_preview(100,0),20); eq(bounded.finish_preview(200,0),30);
    eq(bounded.finish_preview(100,0),0);
#endif
    if (checks < 55) ++failed;
    std::puts(failed ? "folder rendered-frame regression: FAIL (animation does not match last rendered layout)"
        : "folder rendered-frame regression: PASS (bottom-only; occupied; off/on; preview snapshot; grid change; bounded cache; heap unchanged)");
    return failed ? 1 : 0;
}
