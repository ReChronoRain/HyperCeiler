/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>
namespace home_layout {
/* Resolve only bounded original consumers. Counts come from the named currentConfig
 * receiver followed by X/Y bounds checks with a shared reject arm. Item indices come from
 * occupied-cell X/Y stride arithmetic. No neighbouring function or adjacent-field guess.
 * Frame/register contracts are still verified: dynamic offsets do not relax stub ABI.
 */
struct GridFieldOffsets {
    int32_t columns = -1;
    int32_t rows = -1;
    int32_t origin = -1;
    int32_t item_col = -1;
    int32_t item_row = -1;
    int32_t cell_width = -1;
    int32_t cell_height = -1;
    int32_t occupied_grid = -1;
    int32_t dock_columns = -1, dock_item = -1, dock_info = -1;
    bool usable() const {
        return columns > 0 && rows > 0 && origin > 0 && item_col > 0 && item_row > 0
            && cell_width > 0 && cell_height > 0 && occupied_grid > 0;
    }
};

// Drop uses original frame copies and validates against the same resolved stride fields.

inline bool dart_ldur_w(uint32_t w, uint32_t rn, uint32_t rt, int32_t *out_imm) {
    if ((w & 0xFFE00C00u) != 0xB8400000u) return false;
    if (((w >> 30) & 3u) != 2u) return false;
    if (((w >> 5) & 0x1Fu) != rn || (w & 0x1Fu) != rt) return false;
    const uint32_t raw = (w >> 12) & 0x1FFu;
    if (out_imm != nullptr) {
        *out_imm = (raw & 0x100u) ? static_cast<int32_t>(raw) - 0x200
                                  : static_cast<int32_t>(raw);
    }
    return true;
}

inline bool dart_ldur_x(uint32_t w, uint32_t rn, uint32_t rt, int32_t *out_imm) {
    if ((w & 0xFFE00C00u) != 0xF8400000u) return false;
    if (((w >> 30) & 3u) != 3u) return false;
    if (((w >> 5) & 0x1Fu) != rn || (w & 0x1Fu) != rt) return false;
    const uint32_t raw = (w >> 12) & 0x1FFu;
    if (out_imm != nullptr) {
        *out_imm = (raw & 0x100u) ? static_cast<int32_t>(raw) - 0x200
                                  : static_cast<int32_t>(raw);
    }
    return true;
}

inline bool dart_ldur_d(uint32_t w, uint32_t rn, uint32_t rt, int32_t *out_imm) {
    if ((w & 0xFFE00C00u) != 0xFC400000u) return false;
    if (((w >> 30) & 3u) != 3u) return false;
    if (((w >> 5) & 0x1Fu) != rn || (w & 0x1Fu) != rt) return false;
    const uint32_t raw = (w >> 12) & 0x1FFu;
    if (out_imm != nullptr) {
        *out_imm = (raw & 0x100u) ? static_cast<int32_t>(raw) - 0x200
                                  : static_cast<int32_t>(raw);
    }
    return true;
}

inline bool dart_sign_extend_pair(uint32_t w, uint32_t rt) {
    return w == (0x8B1C8000u | (rt << 5) | rt);
}

inline bool dart_is_bl(uint32_t w) { return (w & 0xFC000000u) == 0x94000000u; }

inline bool dart_stur_w(uint32_t w) {
    return (w & 0xFFE00C00u) == 0xB8000000u && (w & 0x1fu) == 1u;
}

// GridCellDelegate's locals are the two strides the geometry helper adjusts. Both loads share
// the same receiver and feed the verified width/height frame slots. The immediates are outputs.
inline void read_grid_cell_size(const std::vector<uint32_t> &body, GridFieldOffsets &out) {
    out.cell_width = out.cell_height = -1;
    size_t hits = 0;
    int32_t width = -1, height = -1;
    for (size_t i = 0; i + 3 < body.size(); ++i) {
        int32_t w = 0, h = 0;
        if (!dart_ldur_d(body[i], 3, 0, &w) || body[i + 1] != 0xfc1b03a0
            || !dart_ldur_d(body[i + 2], 3, 1, &h) || body[i + 3] != 0xfc1b83a1
            || w <= 0 || h <= 0 || w == h || ((w + 1) & 3) || ((h + 1) & 3)) continue;
        ++hits; width = w; height = h;
    }
    if (hits == 1) { out.cell_width = width; out.cell_height = height; }
}

inline bool dart_plausible_field(int32_t imm) { return imm > 0 && imm < 256 && ((imm + 1) & 3) == 0; }

inline bool dart_call_to(uint32_t word, uint32_t pc, uint32_t target) {
    if (!dart_is_bl(word) || !target) return false;
    int64_t imm = word & 0x03ffffffu;
    if (imm & 0x02000000) imm -= 0x04000000;
    return static_cast<int64_t>(pc) + imm * 4 == target;
}
inline int64_t dart_cond_target(size_t index, uint32_t word) {
    int32_t imm = (word >> 5) & 0x7ffff;
    if (imm & 0x40000) imm -= 0x80000;
    return static_cast<int64_t>(index) + imm;
}

// calculateCenterGlobalPosition consumes the origin returned by currentConfig.
inline GridFieldOffsets read_grid_field_offsets(const std::vector<uint32_t> &body,
    uint32_t = 29) {
    GridFieldOffsets out;
    size_t hits = 0;
    for (size_t i = 0; i + 2 < body.size(); ++i) {
        int32_t imm = 0;
        if (dart_ldur_w(body[i], 0, 1, &imm) && dart_plausible_field(imm)
            && dart_sign_extend_pair(body[i + 1], 1) && body[i + 2] == 0xfc407020) {
            ++hits; out.origin = imm;
        }
    }
    if (hits != 1) out.origin = -1;
    return out;
}

// isItemPosEmpty checks column (unboxed -0x18) and row (boxed -0x10), each
// against the same named currentConfig return. Both GE branches must reject together.
inline bool read_grid_counts(const std::vector<uint32_t> &body, uint32_t va,
    uint32_t current_config, GridFieldOffsets &out) {
    out.columns = out.rows = -1;
    size_t xhits = 0, yhits = 0; int32_t x = -1, y = -1;
    int64_t xr = -1, yr = -1;
    for (size_t i = 0; i + 8 < body.size(); ++i) {
        int32_t imm = 0;
        if (!dart_call_to(body[i], va + static_cast<uint32_t>(i * 4), current_config)
            || !dart_ldur_x(body[i + 1], 0, 1, &imm) || !dart_plausible_field(imm)) continue;
        if (body[i + 2] == 0xf85e83a0 && body[i + 3] == 0xeb01001f
            && (body[i + 4] & 0xff00001f) == 0x5400000a) {
            ++xhits; x = imm; xr = dart_cond_target(i + 4, body[i + 4]);
        }
        if (body[i + 2] == 0xf85f03a2 && body[i + 3] == 0x93417c43
            && body[i + 4] == 0x36000042 && body[i + 5] == 0xf8407043
            && body[i + 6] == 0xeb01007f
            && (body[i + 7] & 0xff00001f) == 0x5400000a) {
            ++yhits; y = imm; yr = dart_cond_target(i + 7, body[i + 7]);
        }
    }
    if (xhits != 1 || yhits != 1 || x == y || xr != yr || xr < 0
        || xr >= static_cast<int64_t>(body.size()) || body[xr] != 0x9100c2c0) return false;
    out.columns = x; out.rows = y; return true;
}

inline bool read_occupied_fields(const std::vector<uint32_t> &body, GridFieldOffsets &out) {
    out.item_col = out.item_row = out.occupied_grid = -1;
    size_t hits = 0, grid_hits = 0; int32_t col = -1, row = -1, grid = -1;
    for (size_t i = 0; i + 12 < body.size(); ++i) {
        int32_t x = 0, y = 0;
        if (body[i] == 0xf85f03a0 && body[i + 1] == 0xf85e83a1
            && dart_ldur_x(body[i + 2], 0, 2, &x) && dart_plausible_field(x)
            && body[i + 3] == 0x9e620043 && body[i + 4] == 0x1e600864
            && body[i + 5] == 0x1e642843 && body[i + 6] == 0xfc1903a3
            && dart_ldur_x(body[i + 7], 0, 2, &y) && dart_plausible_field(y)
            && body[i + 8] == 0x9e620044 && body[i + 9] == 0x1e610885
            && body[i + 10] == 0xfc1983a5 && x != y) { ++hits; col = x; row = y; }
        int32_t ptr = 0, w = 0, h = 0;
        if (body[i] == 0xf85f83a2 && dart_ldur_w(body[i + 1], 2, 0, &ptr)
            && dart_plausible_field(ptr) && dart_sign_extend_pair(body[i + 2], 0)
            && dart_ldur_d(body[i + 3], 0, 0, &w) && body[i + 4] == 0xfc1a03a0
            && dart_ldur_d(body[i + 5], 0, 1, &h) && body[i + 6] == 0xfc1a83a1
            && w == out.cell_width && h == out.cell_height) { ++grid_hits; grid = ptr; }
    }
    if (hits != 1 || grid_hits != 1) return false;
    out.item_col = col; out.item_row = row; out.occupied_grid = grid; return true;
}
// The delegate count is boxed into the original -0x38 argument; the per-item
// column chain originates in the -0x50 closure and agrees with occupied-cell indices.
inline bool read_hotseat_fields(const std::vector<uint32_t> &body, GridFieldOffsets &out) {
    out.dock_columns = out.dock_item = out.dock_info = -1;
    size_t counts = 0, chains = 0; int32_t count = -1, item = -1, info = -1;
    for (size_t i = 0; i + 8 < body.size(); ++i) {
        int32_t imm = 0, a = 0, b = 0, col = 0;
        if (body[i] == 0xf85f83a5 && body[i + 1] == 0xf81c03a4
            && dart_ldur_x(body[i + 2], 5, 6, &imm) && dart_plausible_field(imm)
            && body[i + 3] == 0x937f78c0 && body[i + 4] == 0xeb8004df
            && body[i + 5] == 0x54000060 && dart_is_bl(body[i + 6])
            && body[i + 7] == 0xf8007006 && body[i + 8] == 0xf81c83a0) {
            ++counts; count = imm;
        }
        if (body[i] == 0xf85b03a2 && dart_ldur_w(body[i + 1], 2, 0, &a)
            && dart_plausible_field(a) && dart_sign_extend_pair(body[i + 2], 0)
            && dart_ldur_w(body[i + 3], 0, 1, &b) && dart_plausible_field(b)
            && dart_sign_extend_pair(body[i + 4], 1)
            && dart_ldur_x(body[i + 5], 1, 4, &col) && col == out.item_col
            && body[i + 6] == 0x937f7880 && body[i + 7] == 0xeb80049f) {
            ++chains; item = a; info = b;
        }
    }
    if (counts != 1 || chains != 1) return false;
    out.dock_columns = count; out.dock_item = item; out.dock_info = info; return true;
}
} // namespace home_layout
