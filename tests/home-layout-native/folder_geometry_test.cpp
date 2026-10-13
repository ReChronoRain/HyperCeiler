/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_workspace_geometry.h"
#if __has_include("targets/home/home_folder_geometry.h")
#include "targets/home/home_folder_geometry.h"
#define FIXED 1
#else
#define FIXED 0
#endif
#include <cassert>
#include <cstdio>
#include <cstring>
#include <limits>
int main() {
    using namespace home_layout;
    alignas(16) unsigned char memory[2048]{};
    const uintptr_t fp = reinterpret_cast<uintptr_t>(memory + 256);
    const uintptr_t grid = reinterpret_cast<uintptr_t>(memory + 384);
    const uintptr_t origin = reinterpret_cast<uintptr_t>(memory + 512);
    const uintptr_t saved = reinterpret_cast<uintptr_t>(memory + 768);
    // folder_geometry_body() reads the grid through a resolved GridFieldOffsets rather
    // than baked-in slots, because the real ones are decoded per launcher image. This
    // fixture owns its own synthetic heap and states the same numbers explicitly; the
    // decoding itself is covered by read_grid_field_offsets()'s own suite. Every entry
    // below is a slot this file actually writes, and each also satisfies
    // dart_plausible_field() ((imm + 1) & 3 == 0), so the fixture is a legal decode
    // target rather than a set of numbers the helpers happen to accept. item_col,
    // item_row, occupied_grid and the dock_* triple are never dereferenced on the
    // folder path, but usable() requires resolved values for the first seven slots.
    const GridFieldOffsets kFields{0x1b, 0x23, 0x3b, 0x37, 0x3f, 0x2b, 0x33, 0x17, 0x13, 0x0f, 0x07};
    assert(kFields.usable());
    workspace_write(grid, kFields.columns, int64_t{4});
    workspace_write(grid, kFields.rows, int64_t{6});
    workspace_write(grid, kFields.cell_width, 90.0);
    workspace_write(grid, kFields.cell_height, 100.0);
    workspace_write(grid, kFields.origin, static_cast<uint32_t>(origin));
    workspace_write(origin, 7, 20.0);
    unsigned char pristine[256]; std::memcpy(pristine, memory + 384, sizeof(pristine));
    const double tops[] = {-30, 0, 20, 120};
    const double bottoms[] = {-120, 0, 30, 120};
    const double sides[] = {-20, 0, 15, 80};
    double max_error = 0;
    for (double top : tops) for (double bottom : bottoms) for (double side : sides)
    for (int row = 0; row < 6; ++row) for (int col = 0; col < 4; ++col) {
        double g[] = {20, 30, 90, 100}; assert(inset_workspace(g, 4, 6, top, bottom, side));
        workspace_write(fp, -8, int64_t{col}); workspace_write(fp, -0x10, int64_t{row});
        workspace_write(fp, -0x20, 90.0); workspace_write(fp, -0x28, 100.0);
        workspace_write(origin, 7, 20.0); workspace_write(saved, 0, grid);
#if FIXED
        assert(folder_geometry_body(fp, grid >> 32, saved, 0, top, bottom, side, nullptr, &kFields));
        workspace_write(fp, -0x18, int64_t{7});
        workspace_write(saved, 176, static_cast<double>(col));
        workspace_write(saved, 192, workspace_read<double>(fp, -0x20));
        assert(folder_geometry_body(fp, grid >> 32, saved, 4, 0, 0, 0, nullptr, &kFields));
        assert(workspace_read<uintptr_t>(saved, 8)==7);
        // Reproduce Inst.find writing the outgoing slot between x and y.
        workspace_write(fp, -0x30, uintptr_t{0x12345678});
        const double x = workspace_read<double>(saved, 160);
        workspace_write(origin, 7, 30.0); workspace_write(saved, 0, int64_t{row});
        workspace_write(saved, 8, origin);
        // A configuration change between original calls must not split x/y.
        assert(folder_geometry_body(fp, grid >> 32, saved, 1, 0, 0, 0, nullptr, &kFields));
        const double y = workspace_read<double>(saved, 160) + workspace_read<double>(saved, 192);
#else
        const double x = 20 + col * 90.0, y = 30 + row * 100.0;
#endif
        const double errors[] = {std::abs(x-(g[0]+col*g[2])), std::abs(y-(g[1]+row*g[3]))};
        for (double err : errors) if (err > max_error) max_error = err;
#if FIXED
        // Actual preview path (independent of getCellPosition/folderIconSize).
        workspace_write(origin, 7, 20.0); workspace_write(saved, 0, grid);
        workspace_write(fp, -8, uintptr_t{123});
        workspace_write(fp, -0x18, 90.0); workspace_write(fp, -0x20, 100.0);
        assert(folder_geometry_body(fp,grid>>32,saved,2,top,bottom,side,nullptr,&kFields));
        assert(workspace_read<uintptr_t>(saved,8)==123);
        const double preview_x=workspace_read<double>(saved,160)+col*workspace_read<double>(fp,-0x18);
        workspace_write(origin,7,30.0);workspace_write(saved,8,origin);workspace_write(saved,24,int64_t{row});
        assert(folder_geometry_body(fp,grid>>32,saved,3,top,bottom,side,nullptr,&kFields));
        const double preview_y=workspace_read<double>(saved,160)+workspace_read<double>(saved,192);
        assert(std::abs(preview_x-(g[0]+col*g[2]))<1e-9);
        assert(std::abs(preview_y-(g[1]+row*g[3]))<1e-9);

#endif
    }
    workspace_write(origin, 7, 20.0);
    assert(std::memcmp(pristine, memory+384, sizeof(pristine))==0);
#if FIXED
    for (int i=0;i<1000;++i) {
        workspace_write(saved, 0, grid); workspace_write(fp,-8,int64_t{3});
        workspace_write(fp,-0x20,90.0); workspace_write(fp,-0x28,100.0);
        assert(folder_geometry_body(fp, grid>>32,saved,0,20,30,15,nullptr,&kFields));
        assert(workspace_read<double>(fp,-0x20)==82.5);
    }
    workspace_write(grid,0x23,int64_t{1}); workspace_write(saved,0,grid);
    assert(!folder_geometry_body(fp,grid>>32,saved,0,20,30,15,nullptr,&kFields));
    assert(workspace_read<double>(fp,-0x30)==0);
    workspace_write(grid,0x23,int64_t{6}); workspace_write(saved,0,grid);
    assert(!folder_geometry_body(fp,grid>>32,saved,0,std::numeric_limits<double>::quiet_NaN(),0,0,nullptr,&kFields));
    assert(workspace_read<double>(fp,-0x30)==0);
#endif
    if (max_error > 1e-9) { std::puts("folder endpoint regression: FAIL (stock coordinates differ from inset RenderBox)"); return 1; }
    std::puts("folder endpoint regression: PASS (1536 positions; cell and actual preview paths; 1000 rebuilds; heap unchanged)");
}
