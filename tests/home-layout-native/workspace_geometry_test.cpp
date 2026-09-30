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
    workspace_write(grid, 0x1b, int64_t{4});
    workspace_write(grid, 0x23, int64_t{6});
    workspace_write(grid, 0x2b, 90.0);
    workspace_write(grid, 0x33, 100.0);
    workspace_write(fp, -8, grid);
    workspace_write(fp, -0x58, 20.0);
    workspace_write(fp, -0x40, 0.0);
    workspace_write(fp, -0x50, 90.0);
    workspace_write(fp, -0x48, 100.0);
    assert(home_layout::inset_workspace_frame(fp, grid >> 32, false, 20, 30, 15, g));
    eq(workspace_read<double>(fp, -0x58), 35);
    eq(workspace_read<double>(fp, -0x40), 20);
    eq(workspace_read<double>(fp, -0x50), 82.5);
    eq(workspace_read<double>(fp, -0x48), 100 - 50.0 / 6);
    workspace_write(fp, -8, delegate);
    workspace_write(delegate, 0x17, static_cast<uint32_t>(grid));
    workspace_write(fp, -0x10, info);
    workspace_write(info, 0x37, int64_t{3});
    workspace_write(info, 0x3f, int64_t{5});
    workspace_write(fp, -0x50, 20.0);
    for (int i = 0; i < 1000; ++i) {
        assert(home_layout::inset_workspace_frame(fp, grid >> 32, true, 20, 30, 15, g));
        eq(workspace_read<double>(fp, -0x70), 282.5);
        eq(workspace_read<double>(fp, -0x68), 20 + 5 * (100 - 50.0 / 6));
        eq(workspace_read<double>(fp, -0x60), 82.5);
    }
    workspace_write(grid, 0x23, int64_t{1});
    assert(!home_layout::inset_workspace_frame(fp, grid >> 32, true, 20, 30, 15, g));
    std::puts("workspace geometry: neutral/top/bottom/symmetric-side/combined/extremes/guards/rebuild ok");
    std::puts("workspace frame: grid/occupied/pristine-per-child/hotseat-exclusion ok");
}
