/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstddef>
#include <vector>

namespace home_layout {
inline constexpr const char *kWidgetMoveSymbol = "AssistantDragToPAHandler.canDragToPA";

// The patched branch: `tbnz w0,#4, reject` that fires for an MIUI widget. Found by scan, not
// stored -- see widget_move_gate_offset(). The two words below are the only ones this file
// names, and they are matched against, never written into an address.
inline constexpr uint32_t kWidgetMoveOriginal = 0x37200500; // tbnz w0,#4, reject
inline constexpr uint32_t kWidgetMoveReplacement = 0xd503201f; // nop; continue to span check
inline constexpr int32_t kWidgetMoveMagic = 0x48435731; // HCW1; appended after custom titles

// Find only the editing-state rejection in the named canDragToPA body. Its
// captured closure field must come from the named Rx value getter, its reject
// arm must equal the preceding first-page rejection, and the following pointer
// null check must remain intact. Do not skip the whole precondition block.
template <typename ReadWords>
size_t widget_move_editing_gate_offset(size_t bytes, ReadWords read,
    uint32_t base, uint32_t value_callee) {
    std::vector<uint32_t> code;
    if (!value_callee || !bytes || bytes > (1u << 20) || (bytes & 3)
        || !read(bytes, code) || code.size() != bytes / 4) return SIZE_MAX;
    const auto destination = [](size_t i, uint32_t word) -> int64_t {
        const uint32_t raw = (word >> 5) & 0x3fff;
        const int64_t displacement = raw & 0x2000 ? int64_t(raw) - 0x4000 : raw;
        return int64_t(i) * 4 + displacement * 4;
    };
    size_t found = SIZE_MAX;
    for (size_t i = 2; i + 9 < code.size(); ++i) {
        if ((code[i] & 0xfff8001fU) != 0x37200000U
            || (code[i-2] & 0xffe00fffU) != 0xb8400040U
            || code[i-1] != 0x8b1c8000U
            || (code[i+1] & 0xffe00fffU) != 0xb8400040U
            || code[i+2] != 0x8b1c8000U
            || (code[i+3] & 0xfff8001fU) != 0x36200000U
            || (code[i+4] & 0xffe00fffU) != 0xb8400040U
            || code[i+5] != 0x8b1c8000U
            || (code[i+6] & 0xffe00fffU) != 0xb8400001U
            || code[i+7] != 0x8b1c8021U || code[i+8] != 0x6b16003fU
            || (code[i+9] & 0xff00001fU) != 0x54000001U) continue;
        const auto reject = destination(i, code[i]);
        if (reject <= int64_t(i+9)*4 || reject+4 > int64_t(bytes)
            || reject != destination(i+3,code[i+3])) continue;
        const uint32_t pass_raw = (code[i+9] >> 5) & 0x7ffffU;
        const int64_t pass_disp = pass_raw & 0x40000U ? int64_t(pass_raw)-0x80000 : pass_raw;
        const int64_t pass = int64_t(i+9)*4 + pass_disp*4;
        if (pass <= reject || pass+16 > int64_t(bytes)) continue;
        const size_t p = size_t(pass)/4;
        if (code[p] != 0xf85f03a0U || (code[p+1] & 0xffe00fffU) != 0xb8400001U
            || code[p+2] != 0x8b1c8021U
            || (code[p+3] & 0xfff8001fU) != 0x37200001U) continue;
        const uint32_t field = (code[i+1] >> 12) & 0x1ffU;
        if (!field || field >= 0x100 || field == ((code[i-2] >> 12) & 0x1ffU)
            || field == ((code[i+4] >> 12) & 0x1ffU)) continue;
        size_t captures = 0;
        for (size_t c = 0; c+2 < i; ++c) {
            if ((code[c] & 0xfc000000U) != 0x94000000U
                || code[c+1] != 0xf85e83a2U
                || code[c+2] != (0xb8000040U | (field << 12))) continue;
            const uint32_t raw = code[c] & 0x3ffffffU;
            const int64_t displacement = raw & 0x2000000U ? int64_t(raw)-0x4000000 : raw;
            if (int64_t(base)+int64_t(c)*4+displacement*4 == value_callee) ++captures;
        }
        if (captures != 1) continue;
        if (found != SIZE_MAX) return SIZE_MAX;
        found = (i+3)*4;
    }
    return found;
}

// Locate the model-flag rejection sharing the named span check's reject arm. Field magnitude
// is not identity: another flag can move above 0x80, and the MIUI flag can move below it.
template <typename ReadWords>
size_t widget_move_gate_offset(size_t bytes, ReadWords read, int32_t *flag_field = nullptr,
    uint32_t base = 0, uint32_t span_callee = 0) {
    if (flag_field) *flag_field = 0;
    std::vector<uint32_t> code;
    if (!bytes || bytes > (1u << 20) || (bytes & 3) || !read(bytes, code)
        || code.size() != bytes / 4) return SIZE_MAX;
    const auto test = [](uint32_t word) { return (word & 0xfff8001fu) == 0x37200000u; };
    const auto destination = [](size_t i, uint32_t word) -> int64_t {
        const uint32_t raw = (word >> 5) & 0x3fff;
        const int64_t disp = raw & 0x2000 ? static_cast<int64_t>(raw) - 0x4000 : raw;
        return static_cast<int64_t>(i) * 4 + disp * 4;
    };
    int64_t reject = -1;
    for (size_t i = 1; i < code.size(); ++i) {
        if (!test(code[i]) || (code[i - 1] & 0xfc000000u) != 0x94000000u) continue;
        const uint32_t raw = code[i - 1] & 0x3ffffff;
        const int64_t disp = raw & 0x2000000 ? static_cast<int64_t>(raw) - 0x4000000 : raw;
        if (span_callee && static_cast<int64_t>(base) + static_cast<int64_t>(i - 1) * 4
            + disp * 4 != span_callee) continue;
        const auto dest = destination(i, code[i]);
        if (dest < 0 || dest + 4 > static_cast<int64_t>(bytes) || reject >= 0) return SIZE_MAX;
        reject = dest;
    }
    if (reject < 0) return SIZE_MAX;
    size_t found = SIZE_MAX;
    int32_t learned = 0;
    for (size_t i = 2; i < code.size(); ++i) {
        if (!test(code[i]) || destination(i, code[i]) != reject) continue;
        const uint32_t load = code[i - 2];
        if ((load & 0xffe00fffu) != 0xb8400040u || code[i - 1] != 0x8b1c8000u) continue;
        const uint32_t raw = (load >> 12) & 0x1ff;
        const int32_t field = raw & 0x100 ? static_cast<int32_t>(raw) - 0x200 : raw;
        if (field <= 0 || found != SIZE_MAX) return SIZE_MAX;
        found = i * 4; learned = field;
    }
    if (found != SIZE_MAX && flag_field) *flag_field = learned;
    return found;
}

template <typename Read>
bool read_widget_move_extension(Read read, bool &enabled) {
    int32_t magic = 0, flag = 0;
    enabled = false;
    if (!read(magic)) return true; // Old endpoints have no trailing extension.
    if (magic != kWidgetMoveMagic || !read(flag) || (flag != 0 && flag != 1)) return false;
    enabled = flag == 1;
    return true;
}


// One aligned instruction: no trampoline, Dart stack/heap edit, allocation or timer.
// Reject foreign bytes; undo even a writer that changed memory before returning failure.
template <typename Read, typename Write>
bool apply_widget_move_word(uintptr_t address, bool enabled, Read read, Write write,
    uint32_t original = kWidgetMoveOriginal) {
    uint32_t before = 0, after = 0;
    if (!address || (address & 3) || (original & 0xFFF8001Fu) != 0x37200000u
        || !read(address, before)
        || (before != original && before != kWidgetMoveReplacement)) return false;
    const uint32_t wanted = enabled ? kWidgetMoveReplacement : original;
    if (before == wanted) return true;
    if (write(address, wanted) && read(address, after) && after == wanted) return true;
    (void) write(address, before);
    return false;
}
} // namespace home_layout
