// SPDX-License-Identifier: AGPL-3.0-or-later
#pragma once
#include <cstdint>
#include <vector>
namespace home_layout {
struct DockEditFields { int controller = 0, value = 0; };
inline bool dock_edit_fields(const std::vector<uint32_t>& getter, uint32_t getter_va,
        const std::vector<uint32_t>& rx, uint32_t rx_va,
        const std::vector<uint32_t>& update, DockEditFields& out) {
    // A getter only supplies field evidence; its entry is never hooked.
    if (getter.size() < 9 || getter[0] != 0xa9bf79fd || getter[1] != 0xaa0f03fd
        || (getter[2] & 0xffe00fff) != 0xb8400020 || getter[3] != 0x8b1c8000
        || getter[4] != 0xaa0003e1 || (getter[5] & 0xfc000000) != 0x94000000
        || getter[6] != 0xaa1d03ef || getter[7] != 0xa8c179fd || getter[8] != 0xd65f03c0
        || getter_va + 20 + static_cast<int32_t>(getter[5] << 6) / 16 != rx_va
        || update.size() < 5 || update[0] != 0xa9bf79fd || update[1] != 0xaa0f03fd
        || update[2] != 0xd10061ef || update[3] != 0xaa0103e0 || update[4] != 0xf81f83a1)
        return false;
    int field = static_cast<int>((getter[2] >> 12) & 0x1ff);
    if (field <= 0 || field >= 256 || (field & 3) != 3) return false;
    int value = 0; unsigned matches = 0;
    for (size_t i = 0; i + 7 < rx.size(); ++i) {
        if ((rx[i] & 0xffe00fff) != 0xb8400020 || rx[i+1] != 0x8b1c8000
            || rx[i+2] != 0xf9402370 || rx[i+3] != 0x6b10001f
            || rx[i+4] != 0x54000080 || rx[i+5] != 0xaa1d03ef
            || rx[i+6] != 0xa8c179fd || rx[i+7] != 0xd65f03c0) continue;
        value = (rx[i] >> 12) & 0x1ff; ++matches;
    }
    if (matches != 1 || value <= 0 || value >= 256 || (value & 3) != 3) return false;
    out = {field, value}; return true;
}
} // namespace home_layout
