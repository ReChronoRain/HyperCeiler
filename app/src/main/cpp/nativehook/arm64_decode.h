/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime ARM64 decode layer: generic instruction decoding and
 * PC-relative address reconstruction over snapshotted code words.
 *
 * Two sources merged here:
 *  - the desktop Dart hook's generic decoders (this project, previously
 *    inlined in dock_native_resolver.h);
 *  - the ADRP/ADD pair, PLT-stub and import-call identification from
 *    MiuiBackGestureHook's runtime_profile_resolver.cpp (Apache-2.0, see
 *    THIRD_PARTY_NOTICES.md in this directory).
 *
 * Everything decodes from a snapshotted word span; nothing here reads live
 * memory and nothing here hooks. Target-specific shapes (Dart prologues,
 * compressed pointers, launcher fingerprints) do not belong here.
 */
#pragma once

#include <array>
#include <cstdint>
#include <optional>
#include <span>
#include <utility>
#include <vector>

namespace nhk::arm64 {

/** One executable snapshot range: runtime address plus its code words. */
struct CodeRange {
    uintptr_t address;
    std::span<const uint32_t> words;
    bool contains(uintptr_t location, size_t count = 1) const {
        if (location < address || (location - address) % sizeof(uint32_t) != 0) return false;
        const auto index = (location - address) / sizeof(uint32_t);
        return index <= words.size() && count <= words.size() - index;
    }
};

struct Match {
    uintptr_t address;
    std::span<const uint32_t> words;
};

inline constexpr size_t kMaxFunctionWords = 768;
inline uint32_t rd(uint32_t word) { return word & 0x1f; }
inline uint32_t rn(uint32_t word) { return (word >> 5) & 0x1f; }
inline uint32_t rm(uint32_t word) { return (word >> 16) & 0x1f; }
inline bool is_bl(uint32_t word) { return (word & 0xfc000000) == 0x94000000; }
inline bool is_b(uint32_t word) { return (word & 0xfc000000) == 0x14000000; }
inline bool is_ret(uint32_t word) { return word == 0xd65f03c0; }

inline bool is_ldur_w(uint32_t word) { return (word & 0xffe00c00) == 0xb8400000; }
inline bool is_ldur_x(uint32_t word) { return (word & 0xffe00c00) == 0xf8400000; }
inline bool is_ldur_d(uint32_t word) { return (word & 0xffe00c00) == 0xfc400000; }
inline bool is_stur_w(uint32_t word) { return (word & 0xffe00c00) == 0xb8000000; }
inline bool is_stur_x(uint32_t word) { return (word & 0xffe00c00) == 0xf8000000; }
inline bool is_stur_d(uint32_t word) { return (word & 0xffe00c00) == 0xfc000000; }
inline int memory_offset(uint32_t word) {
    int value = static_cast<int>((word >> 12) & 0x1ff);
    return value >= 0x100 ? value - 0x200 : value;
}
inline bool is_add_imm_x(uint32_t word) { return (word & 0xff000000) == 0x91000000; }
inline uint32_t add_imm(uint32_t word) {
    const uint32_t value = (word >> 10) & 0xfff;
    return ((word >> 22) & 1) != 0 ? value << 12 : value;
}

/** BL/B target with overflow-checked sign extension. */
inline std::optional<uintptr_t> branch_target(uintptr_t pc, uint32_t word, bool link) {
    if ((word & 0xfc000000) != (link ? 0x94000000u : 0x14000000u)) return {};
    int64_t displacement = word & 0x03ffffff;
    if ((displacement & 0x02000000) != 0) displacement -= 0x04000000;
    displacement *= 4;
    if (displacement < 0 && pc < static_cast<uintptr_t>(-displacement)) return {};
    if (displacement > 0 && pc > UINTPTR_MAX - static_cast<uintptr_t>(displacement)) return {};
    return displacement < 0 ? pc - static_cast<uintptr_t>(-displacement)
                            : pc + static_cast<uintptr_t>(displacement);
}

inline std::optional<uintptr_t> call_target(const Match &match, size_t index) {
    if (index >= match.words.size()) return {};
    return branch_target(match.address + index * sizeof(uint32_t), match.words[index], true);
}

inline std::span<const uint32_t> at(std::span<const CodeRange> ranges,
    uintptr_t address, size_t count) {
    for (const auto &range : ranges) {
        if (range.contains(address, count)) {
            return range.words.subspan((address - range.address) / sizeof(uint32_t), count);
        }
    }
    return {};
}

/** MOVZ/MOVK pair materializing a 32-bit constant into `reg`. */
inline std::optional<uint32_t> materialized_u32(std::span<const uint32_t> words,
    uint32_t reg) {
    if (words.size() < 2 || (words[0] & 0xff80001f) != (0xd2800000 | reg)
        || (words[1] & 0xff80001f) != (0xf2800000 | reg)
        || ((words[0] >> 21) & 3) != 0
        || ((words[1] >> 21) & 3) != 1) return {};
    return ((words[0] >> 5) & 0xffff) | (((words[1] >> 5) & 0xffff) << 16);
}

/** UBFX extraction of `source` into `destination`: returns {shift, width}. */
inline std::optional<std::pair<uint32_t, uint32_t>> ubfx(uint32_t word,
    uint32_t source, uint32_t destination) {
    if ((word & 0xffc00000) != 0xd3400000 || rn(word) != source || rd(word) != destination) {
        return {};
    }
    const uint32_t shift = (word >> 16) & 0x3f;
    const uint32_t last = (word >> 10) & 0x3f;
    if (last < shift) return {};
    return std::pair{shift, last - shift + 1};
}

/** SBFX-shaped LSL amount trick used by Dart allocation stubs. */
inline std::optional<uint32_t> lsl_amount(uint32_t word, uint32_t reg) {
    if ((word & 0xffc00000) != 0xd3400000 || rn(word) != reg || rd(word) != reg) return {};
    const uint32_t rotate = (word >> 16) & 0x3f;
    const uint32_t last = (word >> 10) & 0x3f;
    if (rotate == 0 || last + 1 != rotate) return {};
    return 64 - rotate;
}

// --- Ported from MiuiBackGestureHook (runtime_profile_resolver.cpp) ---

/** ADRP: 4KB-page PC-relative immediate. Returns the absolute page address. */
inline std::optional<uint64_t> decode_adrp(uintptr_t pc, uint32_t word) {
    if ((word & 0x9f000000U) != 0x90000000U) return {};
    if (rd(word) == 31) return {}; // SP is not a valid ADRP destination.
    // imm = immhi (bits 23:5) : immlo (bits 30:29), a 21-bit signed page count.
    int64_t encoded = static_cast<int64_t>((word >> 5) & 0x7ffffU) << 2
        | static_cast<int64_t>((word >> 29) & 0x3U);
    if ((encoded & 0x100000) != 0) encoded -= 0x200000;
    const uint64_t page = pc & ~static_cast<uint64_t>(0xfff);
    const int64_t displacement = encoded * 4096;
    if (displacement < 0 && page < static_cast<uint64_t>(-displacement)) return {};
    return displacement < 0 ? page - static_cast<uint64_t>(-displacement)
                            : page + static_cast<uint64_t>(displacement);
}

/** ADD immediate with shift bit: the low half of an ADRP+ADD address pair. */
inline std::optional<uint32_t> decode_add_immediate(uint32_t word) {
    if ((word & 0xffc00000U) != 0x91000000U) return {};
    return (word >> 10) & 0xfff;
}

/** LDR X-register, unsigned immediate (used by PLT stubs and pool loads). */
inline bool is_ldr64_immediate(uint32_t word) {
    return (word & 0xffc00000U) == 0xf9400000U;
}
inline std::optional<uint64_t> decode_ldr64_immediate(uint32_t word) {
    if (!is_ldr64_immediate(word)) return {};
    return static_cast<uint64_t>(((word >> 10) & 0xfffU) << 3);
}

/** ADRP + ADD pair: reconstruct a PC-relative data/strings address. */
inline std::optional<uint64_t> decode_address_pair(std::span<const uint32_t> words,
    size_t index, uintptr_t pc) {
    if (index + 1 >= words.size()) return {};
    const auto page = decode_adrp(pc, words[index]);
    if (!page) return {};
    const auto add = decode_add_immediate(words[index + 1]);
    if (!add) return {};
    if (rd(words[index]) != rd(words[index + 1]) || rn(words[index + 1]) != rd(words[index])) {
        return {};
    }
    return *page + *add;
}

/** B.cond target. */
inline std::optional<uintptr_t> decode_conditional_branch_target(uintptr_t pc, uint32_t word) {
    if ((word & 0xff000010U) != 0x54000000U) return {};
    int64_t displacement = static_cast<int64_t>((word >> 5) & 0x7ffffU);
    if ((displacement & 0x40000) != 0) displacement -= 0x80000;
    displacement *= 4;
    if (displacement < 0 && pc < static_cast<uintptr_t>(-displacement)) return {};
    if (displacement > 0 && pc > UINTPTR_MAX - static_cast<uintptr_t>(displacement)) return {};
    return displacement < 0 ? pc - static_cast<uintptr_t>(-displacement)
                            : pc + static_cast<uintptr_t>(displacement);
}

/**
 * A PLT stub: `ADRP x16, page; LDR x17, [x16, imm]; ADD x16, x16, imm; BR x17`
 * (register numbering varies; the shape and `BR` are what identify it).
 * Returns the GOT slot address the stub loads through.
 */
inline std::optional<uint64_t> decode_plt_got(std::span<const uint32_t> words,
    size_t index, uintptr_t pc) {
    if (index + 3 >= words.size()) return {};
    const auto page = decode_adrp(pc, words[index]);
    if (!page || rd(words[index]) != 16) return {};
    if (!is_ldr64_immediate(words[index + 1]) || rd(words[index + 1]) != 17
        || rn(words[index + 1]) != 16) return {};
    const auto offset = decode_ldr64_immediate(words[index + 1]);
    if (!offset) return {};
    if ((words[index + 3] & 0xffffffffU) != 0xd61f0220U) return {}; // BR x17.
    return *page + *offset;
}

/**
 * True when a call target is a PLT stub resolving through `got_rva` - i.e.
 * the caller provably calls the named import instead of any lookalike.
 */
inline bool call_targets_import(const std::optional<uint64_t> &got_rva,
    std::optional<uint64_t> slot_rva) {
    return got_rva.has_value() && slot_rva.has_value() && *got_rva == *slot_rva;
}

} // namespace nhk::arm64
