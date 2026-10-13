/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <span>

namespace home_layout {
// Appended protocol extension: old endpoints/caches retain stock folder geometry.
constexpr int32_t kFolderLayoutMagic = 0x48434631; // HCF1
struct FolderLayoutConfig {
    int title_center = 0, full_width = 0, padding_enabled = 0;
    int phone_padding = 0, landscape_padding = 0, portrait_padding = 0;
    int tablet = 0;
    bool operator==(const FolderLayoutConfig &) const = default;
};
inline bool valid_folder_layout(const FolderLayoutConfig &c) {
    const auto flag = [](int n) { return n == 0 || n == 1; };
    return flag(c.title_center) && flag(c.full_width) && flag(c.padding_enabled)
        && flag(c.tablet) && c.phone_padding >= 0 && c.phone_padding <= 50
        && c.landscape_padding >= 0 && c.landscape_padding <= 450
        && c.portrait_padding >= 0 && c.portrait_padding <= 200;
}
template<class Read> bool read_folder_layout(Read read, FolderLayoutConfig &out, bool optional = true) {
    int32_t magic = 0;
    if (!read(magic)) { if (optional) { out = {}; return true; } return false; }
    if (magic != kFolderLayoutMagic) return false;
    FolderLayoutConfig c;
    for (int *p : {&c.title_center, &c.full_width, &c.padding_enabled, &c.phone_padding,
            &c.landscape_padding, &c.portrait_padding, &c.tablet}) {
        int32_t n = 0; if (!read(n)) return false; *p = n;
    }
    if (!valid_folder_layout(c)) return false;
    out = c; return true;
}
// Single release/acquire word: a Dart layout never sees a torn gate/padding/column snapshot.
inline uint64_t pack_folder_layout(const FolderLayoutConfig &c, int cols) {
    if (!valid_folder_layout(c) || cols < 3 || cols > 6) return 0;
    return uint64_t(c.title_center) | (uint64_t(c.full_width) << 1)
        | (uint64_t(c.padding_enabled) << 2) | (uint64_t(c.tablet) << 3)
        | (uint64_t(c.phone_padding) << 4) | (uint64_t(c.landscape_padding) << 10)
        | (uint64_t(c.portrait_padding) << 19) | (uint64_t(cols) << 27);
}
inline FolderLayoutConfig unpack_folder_layout(uint64_t p) {
    return {int(p & 1), int((p >> 1) & 1), int((p >> 2) & 1), int((p >> 4) & 63),
        int((p >> 10) & 511), int((p >> 19) & 255), int((p >> 3) & 1)};
}
inline double folder_cell_width(double original, double width, double height,
        double gap, uint64_t packed) {
    const auto c = unpack_folder_layout(packed);
    const int cols = int((packed >> 27) & 7);
    if (!c.full_width || cols < 3 || cols > 6 || !std::isfinite(width)
        || !std::isfinite(height) || !std::isfinite(gap)
        || width < 100 || width > 4000 || height < 100 || height > 5000
        || gap < 0 || gap > 200) return original;
    double padding = c.padding_enabled ? c.phone_padding : 0;
    if (c.tablet && c.padding_enabled)
        padding = width > height ? c.landscape_padding : c.portrait_padding;
    // Keep positive cells even with an excessive tablet inset, without changing stored settings.
    padding = std::clamp(padding, 0.0, std::max(0.0, (width - cols * 24.0 - (cols - 1) * gap) / 2));
    const double value = (width - 2 * padding - (cols - 1) * gap) / cols;
    return std::isfinite(value) && value >= 24 ? value : original;
}

struct FolderReturnSite { uint32_t offset = 0; int field = -1; };
// One owning-function return, not a guessed symbol+offset. The native stub executes the
// original epilogue; the allocator slow path still rejoins its ORIGINAL Dart store PC.
inline bool folder_return_site(std::span<const uint32_t> body, unsigned rn,
        bool store, FolderReturnSite &out) {
    unsigned hits = 0; FolderReturnSite found;
    for (size_t i = 0; i + 3 < body.size(); ++i) {
        const auto w = body[i];
        const uint32_t op = store ? 0xfc000000 : 0xfc400000;
        if ((w & 0xffe00c1f) != op || ((w >> 5) & 31) != rn
            || body[i + 1] != 0xaa1d03ef || body[i + 2] != 0xa8c179fd
            || body[i + 3] != 0xd65f03c0) continue;
        int field = int((w >> 12) & 511); if (field & 256) field -= 512;
        if (field < 0 || field > 255 || (field & 7) != 7) return false;
        found = {uint32_t(i * 4), field}; ++hits;
    }
    if (hits != 1) return false;
    out = found; return true;
}
// Select CENTER_HORIZONTAL (gravity masked to 1), not RIGHT (5).
// Check the branch's common join and independently resolve View.TEXT_ALIGNMENT_CENTER (4).
// The two named functions must agree on the root; no persistent Dart object pointer.
inline bool folder_center_pool(std::span<const uint32_t> body, uint32_t &pool) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 3 < body.size(); ++i) {
        if (body[i] != 0x7100043f || body[i + 1] != 0x54000061
            || (body[i + 2] & 0xffc003ff) != 0xf9400360
            || (body[i + 3] & 0xfc000000) != 0x14000000) continue;
        int32_t jump = int32_t(body[i + 3] & 0x03ffffff);
        if (jump & 0x02000000) jump -= 0x04000000;
        const int64_t join = int64_t(i + 3) + jump;
        if (join <= int64_t(i + 3) || join + 3 >= int64_t(body.size())
            || body[size_t(join)] != 0x6b16001f
            || body[size_t(join + 1)] != 0x54000041
            || (body[size_t(join + 2)] & 0xffc003ff) != 0xf9400360
            || body[size_t(join + 3)] != 0xaa1d03ef) continue;
        found = ((body[i + 2] >> 10) & 4095) * 8; ++hits;
    }
    if (hits != 1 || !found) return false;
    pool = found; return true;
}
inline bool folder_text_alignment_center_pool(std::span<const uint32_t> body, uint32_t &pool) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 3 < body.size(); ++i) {
        if (body[i] != 0xf100107f || body[i + 1] != 0x5400006c
            || (body[i + 2] & 0xffc003ff) != 0xf9400360
            || body[i + 3] != 0xd65f03c0) continue;
        found = ((body[i + 2] >> 10) & 4095) * 8; ++hits;
    }
    if (hits != 1 || !found) return false;
    pool = found; return true;
}
} // namespace home_layout

// ---------------------------------------------------------------------------
// Inner-Container relocation for the folder title.
//
// Everything below is transcribed from tests/folder-title-center-os4/
// probe_inner_container.py, which proves each step offline over the real
// launcher-7722 image before any of it is allowed near the device. Two rules from
// that run are load-bearing and are repeated here because they are easy to undo:
//
//  1. `Container.alignment` is NOT the only null-tested 32-bit field.
//     Container.build reads nine of them (+0xb,+0xf,+0x17,+0x1b,+0x1f,+0x23,+0x27,
//     +0x2b,+0x33), one per Container field in declaration order. An earlier version
//     of this scan believed +0xf was unique; that was an artefact of a cmp mask that
//     normalised nothing (0x7F801FFF leaves bit 7 intact, so every `cmp wN, w22` with
//     Rn != 0 fell through). The discriminator that actually holds is structural: +0xf
//     is the ONLY slot read through a base register other than x0, and the producer
//     side confirms it by storing a pool root straight into that slot.
//
//  2. The two AlignmentDirectional roots are NOT two independent constants. They are
//     the two arms of a single TBNZ diamond converging on one frame store, so they
//     can only be derived together. Matching "pp load followed by a store" finds one
//     of them and silently misses the other.
//
// Every offset in here is resolved at runtime from the instruction stream. Nothing is
// a compiled-in address.
namespace home_layout {

// Instruction-field accessors. These are architecture encodings, not addresses.
inline uint32_t insn_imm9(uint32_t w) {
    const uint32_t v = (w >> 12) & 0x1FFu;
    return (v & 0x100u) ? v - 0x200u : v;
}
inline uint32_t insn_rt(uint32_t w) { return w & 31u; }
inline uint32_t insn_rn(uint32_t w) { return (w >> 5) & 31u; }
inline uint32_t insn_imm12(uint32_t w) { return (w >> 10) & 0xFFFu; }
inline bool insn_is_bl(uint32_t w) { return (w >> 26) == 0x25u; }
inline bool insn_is_b(uint32_t w) { return (w & 0x7C000000u) == 0x14000000u; }
// LDUR/STUR unscaled (simm9) - the family Dart uses for every object field access.
inline bool insn_is_ldur_x(uint32_t w) { return (w & 0xFFC00C00u) == 0xF8400000u; }
inline bool insn_is_stur_x(uint32_t w) { return (w & 0xFFC00C00u) == 0xF8000000u; }
inline bool insn_is_ldur_w(uint32_t w) { return (w & 0xFFC00C00u) == 0xB8400000u; }
inline bool insn_is_stur_w(uint32_t w) { return (w & 0xFFC00C00u) == 0xB8000000u; }
// LDR (unsigned imm12) - used only to reach the object pool.
inline bool insn_is_ldr_x(uint32_t w) { return (w & 0xFFC00000u) == 0xF9400000u; }
// MOV Xd, Xm (register form). The shift field must be zero or a shifted variant
// would be mistaken for a plain move.
inline bool insn_is_mov_xrr(uint32_t w) {
    return (w & 0x7F80FFE0u) == 0x2A0003E0u && ((w >> 10) & 0x3Fu) == 0;
}
// ADD Xd, Xd, X28, LSL #32 - decompresses a Dart compressed pointer.
inline bool insn_is_heap_add(uint32_t w) {
    return (w & 0xFFE0FC00u) == 0x8B008000u && ((w >> 16) & 31u) == 28u;
}
// ADD Xd, X27, #hi, LSL #12 - the object-pool base add.
inline bool insn_is_pp_add(uint32_t w) {
    return (w & 0xFFC00000u) == 0x91400000u && ((w >> 5) & 31u) == 27u;
}
// SUB Xd, Xn, #imm12 - the Dart frame allocation.
inline bool insn_is_sub_imm(uint32_t w) { return (w & 0xFF800000u) == 0xD1000000u; }
// STP <X29,X30>,[Xn,#-imm]! - the Dart prologue frame push.
inline bool insn_is_stp_pre(uint32_t w) { return (w & 0xFFC00000u) == 0xA9800000u; }
// STP's second register is bits 15:10, NOT the 20:16 slot a MOV uses. Reading it
// from 20:16 yields x31 and rejects every real prologue.
inline uint32_t insn_stp_rt2(uint32_t w) { return (w >> 10) & 31u; }
// TBNZ (32-bit): bit number in 23:19, imm14 in 18:5, Rt in 4:0.
inline bool insn_is_tbnz(uint32_t w) { return (w & 0x7F800000u) == 0x37000000u; }
inline uint32_t insn_tbnz_bit(uint32_t w) { return (w >> 19) & 31u; }
inline bool insn_tbnz_taken(uint32_t w) { return (w & 0x400000u) != 0; }
// SIMD&FP STUR Vt, [Xn, #simm9]. Bit 31 is what separates it from the GPR family,
// whose sf bit is 0. Pinning (w & 0xFFC00000) == 0xFC000000 selects all six SIMD
// unscaled stores in the corpus and rejects all five GPR ones.
inline bool insn_is_stur_d(uint32_t w) { return (w & 0xFFC00000u) == 0xFC000000u; }
// SUBS WZR, Wn, W22 - the Dart null test against the x22 sentinel. This mask must
// clear bit 7; 0x7F801FFF does not, and every Rn != 0 test then fails to match.
inline bool insn_is_cmp_null(uint32_t w) { return (w & 0xFFFFFC1Fu) == 0x6B16001Fu; }

// Absolute target of a B/BL. The 26-bit field is a WORD displacement relative to the
// branch itself, so the instruction address must be added. Omitting it yields an
// offset that looks like a plausible VA and resolves no symbol at all.
inline bool insn_branch_target(uint32_t base, uint32_t w, uint32_t &out) {
    if (!insn_is_bl(w) && !insn_is_b(w)) return false;
    uint32_t d = (w & 0x03FFFFFFu) << 2;
    if (d & (1u << 27)) d -= (1u << 28);
    out = base + d; return true;
}
// Absolute target of a TBNZ. A 14-bit field - a different decoder from B/BL, and
// using one for the other silently yields an in-range but wrong index.
inline bool insn_tbnz_target(uint32_t base, uint32_t w, uint32_t &out) {
    if (!insn_is_tbnz(w)) return false;
    uint32_t v = (w >> 5) & 0x3FFFu;
    if (v & 0x2000u) v -= 0x4000u;
    out = base + (v << 2); return true;
}

// ADD Xd,X27,#hi,lsl#12 ; LDR Xd,[Xd,#imm12] -> pool byte offset.
// imm12 counts WORDS, so the byte displacement is imm12*8. Using imm12 directly gives
// an offset eight times too small, which still lands inside the pool and reads a
// plausible-looking value.
inline bool insn_pool_offset(uint32_t add, uint32_t ldr, uint32_t &out) {
    if (!insn_is_pp_add(add) || !insn_is_ldr_x(ldr)) return false;
    const uint32_t rd = add & 31u;
    if (insn_rn(ldr) != rd || insn_rt(ldr) != rd) return false;
    out = insn_imm12(add) * 4096u + insn_imm12(ldr) * 8u; return true;
}

// Everything the runtime helper needs, all of it derived from the instruction stream.
struct FolderInnerContainer {
    uint32_t window = 0;          // VA of the 4-instruction replay window
    uint32_t frame = 0;           // owning closure frame size, from its own prologue
    uint32_t owner_slot = 0;      // FP offset of the freshly built Container
    uint32_t alignment = 0;       // Container.alignment field offset
    uint32_t padding = 0;         // Container.padding field offset
    uint32_t inset = 0;           // EdgeInsets.left, the three others follow at +8/16/24
    uint32_t center_pool = 0;     // Alignment.center object-pool offset
    uint32_t direction_pool[2] = {0, 0}; // the two AlignmentDirectional roots
    uint32_t direction_bit = 0;   // which bit of the cross-axis value selects between them
};

// Reject direct control-flow edges into the middle of a 16-byte patch. The editor
// joins the text path after its owner reload: overwriting that reload would turn the
// editor's branch into a jump to the middle of the native absolute-jump sequence.
inline bool folder_window_single_entry(std::span<const uint32_t> body,
        uint32_t va, uint32_t window) {
    for (size_t j = 0; j < body.size(); ++j) {
        const uint32_t op = body[j]; int64_t delta = 0;
        if ((op & 0x7c000000) == 0x14000000) {
            int32_t n = int32_t(op & 0x3ffffff);
            if (n & 0x2000000) n -= 0x4000000;
            delta = int64_t(n) * 4;
        } else if ((op & 0xff000010) == 0x54000000
                || (op & 0x7e000000) == 0x34000000) {
            int32_t n = int32_t((op >> 5) & 0x7ffff);
            if (n & 0x40000) n -= 0x80000;
            delta = int64_t(n) * 4;
        } else if ((op & 0x7e000000) == 0x36000000) {
            int32_t n = int32_t((op >> 5) & 0x3fff);
            if (n & 0x2000) n -= 0x4000;
            delta = int64_t(n) * 4;
        } else continue;
        const int64_t target = int64_t(va) + int64_t(j) * 4 + delta;
        if (target > window && target < int64_t(window) + 16) return false;
    }
    return true;
}

// Resolve the default TEXT_ALIGNMENT_* start root from the named converter's
// case 1 fallthrough (cmp #1; b.gt; cmp #0; b.le; pool root; ret).
// Pool high/low immediates are data, not a launcher version signature.
inline bool folder_pool_load(std::span<const uint32_t> b, size_t at,
        unsigned reg, uint32_t &pool, size_t &count) {
    if (at >= b.size() || reg > 30) return false;
    const auto w = b[at];
    if (insn_is_ldr_x(w) && insn_rt(w) == reg && insn_rn(w) == 27) {
        pool = insn_imm12(w) * 8; count = 1; return pool != 0;
    }
    if ((w & 0xff800000u) != 0x91000000u || insn_rt(w) != reg
        || insn_rn(w) != 27 || at + 1 >= b.size()) return false;
    const auto load = b[at + 1];
    if (!insn_is_ldr_x(load) || insn_rt(load) != reg || insn_rn(load) != reg) return false;
    pool = (insn_imm12(w) << ((w & 0x400000u) ? 12 : 0)) + insn_imm12(load) * 8;
    count = 2; return pool != 0;
}
inline bool folder_start_pool(std::span<const uint32_t> b, uint32_t &pool) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 5 < b.size(); ++i) {
        // CMP Xn,#1 / CMP Xn,#0, same input register and signed GT/LE.
        if ((b[i] & 0xfffffc1fu) != 0xf100041fu
            || (b[i + 2] & 0xfffffc1fu) != 0xf100001fu
            || insn_rn(b[i]) != insn_rn(b[i + 2])
            || (b[i + 1] & 0xff00001fu) != 0x5400000cu
            || (b[i + 3] & 0xff00001fu) != 0x5400000du) continue;
        uint32_t root = 0; size_t count = 0;
        if (!folder_pool_load(b, i + 4, 0, root, count)
            || i + 4 + count >= b.size() || b[i + 4 + count] != 0xd65f03c0u) continue;
        bool targets_ok = true;
        for (size_t j : {i + 1, i + 3}) {
            int32_t d = int32_t((b[j] >> 5) & 0x7ffffu);
            if (d & 0x40000) d -= 0x80000;
            const int64_t target = int64_t(j) + d;
            if (target <= int64_t(i + 4 + count) || target >= int64_t(b.size())) targets_ok = false;
        }
        if (targets_ok) { found = root; ++hits; }
    }
    if (hits != 1) return false;
    pool = found; return true;
}
// Keep the existing splice ABI: X0 is the freshly built Text/TextField, X1 is
// TextAlign.start. Derive its store field and following replay, independently
// for each owner. Never place a trampoline over a call/GC PC or an interior join.
inline bool folder_text_align_store(std::span<const uint32_t> b, uint32_t va,
        uint32_t start_pool, uint32_t &offset) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 1; i + 3 < b.size(); ++i) {
        const auto w = b[i]; const int field = int32_t(insn_imm9(w));
        if (!insn_is_stur_w(w) || insn_rn(w) != 0 || insn_rt(w) != 1
            || field <= 0 || field > 255 || (field & 3) != 3) continue;
        bool producer = false;
        for (size_t back = 1; back <= 2 && back <= i; ++back) {
            uint32_t root = 0; size_t count = 0;
            if (folder_pool_load(b, i - back, 1, root, count)
                && count == back && root == start_pool) producer = true;
        }
        if (!producer) continue;
        // X1 must be replaced immediately after the first store, so overriding
        // it cannot leak into another unrelated constructor field.
        const auto next = b[i + 1];
        const bool reload = (insn_is_ldr_x(next) && insn_rt(next) == 1
                && (insn_rn(next) == 27 || insn_rn(next) == 1))
            || ((next & 0xff800000u) == 0x91000000u && insn_rt(next) == 1
                && (insn_rn(next) == 27 || insn_rn(next) == 22))
            || (insn_is_ldur_x(next) && insn_rt(next) == 1 && insn_rn(next) == 29);
        if (!reload) continue;
        bool replay = true;
        for (size_t j = i + 1; j <= i + 3; ++j) {
            const auto op = b[j];
            const bool allowed = (insn_is_stur_w(op) && insn_rn(op) == 0)
                || (insn_is_ldr_x(op) && insn_rt(op) == 1
                    && (insn_rn(op) == 27 || insn_rn(op) == 1))
                || ((op & 0xff800000u) == 0x91000000u && insn_rt(op) == 1
                    && (insn_rn(op) == 27 || insn_rn(op) == 22))
                || (insn_is_ldur_x(op) && insn_rt(op) == 1 && insn_rn(op) == 29);
            if (!allowed) replay = false;
        }
        if (i > (UINT32_MAX - va) / 4 || va + i * 4 > UINT32_MAX - 16) return false;
        if (!replay || !folder_window_single_entry(b, va, va + uint32_t(i * 4))) continue;
        found = uint32_t(i * 4); ++hits;
    }
    if (hits != 1) return false;
    offset = found; return true;
}

// 1. Find the Container handoff, then splice at the COMMON join one instruction
// after the text-only owner reload. Saved x3 identifies the selected child on both
// paths. Replay has no BL/GC return-PC relocation and all incoming edges hit its start.
inline bool folder_inner_window(std::span<const uint32_t> body, uint32_t va,
        uint32_t &window, uint32_t &owner_slot) {
    unsigned hits = 0; uint32_t w = 0, os = 0;
    for (size_t i = 0; i + 5 < body.size(); ++i) {
        if (!insn_is_bl(body[i])) continue;
        const uint32_t la = body[i + 1], lb = body[i + 2], mv = body[i + 3], st = body[i + 4];
        if (!insn_is_ldur_x(la) || !insn_is_ldur_x(lb) || !insn_is_mov_xrr(mv)
            || !insn_is_stur_x(st)) continue;
        if (insn_rn(la) != 29 || insn_rt(la) != 3) continue;
        if (insn_rn(lb) != 29) continue;
        if (insn_rt(mv) != 2 || ((mv >> 16) & 31u) != insn_rt(lb)) continue;
        if (insn_rt(st) != 3 || insn_rn(st) != 29 || insn_imm9(st) != uint32_t(-8)) continue;
        const uint32_t next = body[i + 5];
        if ((next & 0xffc003ff) != 0x91400361) continue; // ADD x1, PP, #imm, LSL #12
        w = va + uint32_t(i + 2) * 4;
        if (!folder_window_single_entry(body, va, w)) continue;
        os = uint32_t(-int32_t(insn_imm9(la))); ++hits;
    }
    if (hits != 1) return false;
    window = w; owner_slot = os; return true;
}

// 2. The closure frame size. The function holds several Dart closures, each with its
//    own pre-index STP; the frame that owns the window is the NEAREST prologue walking
//    backwards from it, and it must be a complete stp/mov/sub triple. A partial match
//    is refused rather than guessed, because a wrong frame size shifts every local.
inline bool folder_inner_frame(std::span<const uint32_t> body, uint32_t va,
        uint32_t window, uint32_t &frame) {
    const size_t w_at = (window - va) / 4;
    if (w_at == 0 || w_at >= body.size()) return false;
    for (size_t j = w_at - 1; j-- > 0;) {
        if (!insn_is_stp_pre(body[j])) continue;
        if (insn_rt(body[j]) != 29 || insn_stp_rt2(body[j]) != 30) continue;
        if (j + 2 >= body.size()) continue;
        if (!insn_is_mov_xrr(body[j + 1]) || insn_rt(body[j + 1]) != 29
            || ((body[j + 1] >> 16) & 31u) != 15) continue;
        if (!insn_is_sub_imm(body[j + 2]) || insn_rn(body[j + 2]) != 15
            || insn_rt(body[j + 2]) != 15) continue;
        frame = insn_imm12(body[j + 2]); return frame != 0;
    }
    return false;
}

// 3. The freshly built Container must be stored into exactly the slot the window
//    returns, otherwise the two derivations describe different objects.
inline bool folder_inner_owner_store(std::span<const uint32_t> body, uint32_t va,
        uint32_t window, uint32_t owner_slot) {
    const size_t w_at = (window - va) / 4;
    if (w_at == 0) return false;
    const size_t lo = w_at > 64 ? w_at - 64 : 1;
    for (size_t j = w_at - 1; j-- > lo;) {
        if (!insn_is_stur_x(body[j]) || insn_rt(body[j]) != 0 || insn_rn(body[j]) != 29
            || insn_imm9(body[j]) != uint32_t(-int32_t(owner_slot))) continue;
        // it must be the result of a call, otherwise it is some other frame local
        for (size_t k = (j > 8 ? j - 8 : 0); k < j; ++k)
            if (insn_is_bl(body[k])) return true;
    }
    return false;
}

// 4. Container.alignment. The discriminator is NOT uniqueness - Container.build reads
//    nine null-tested 32-bit slots. It is that +0xf is the only slot read through a
//    base register other than x0: every other field is `ldur wN,[x0,#..]` with x0 the
//    Container itself, while alignment goes through a reloaded x1. The base register
//    comes from the instruction, so the offset is derived, not assumed.
inline bool folder_inner_alignment(std::span<const uint32_t> build, uint32_t &alignment) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 2 < build.size(); ++i) {
        if (!insn_is_ldur_w(build[i]) || !insn_is_heap_add(build[i + 1])) continue;
        if (insn_rt(build[i + 1]) != insn_rt(build[i]) || insn_rn(build[i + 1]) != insn_rt(build[i]))
            continue;
        if (insn_rn(build[i]) == 0) continue;   // x0 is the Container itself
        uint32_t off = insn_imm9(build[i]); found = off; ++hits;
    }
    if (hits != 1) return false;
    alignment = found; return true;
}

// 5. Container.padding, derived from its own named function. The null test for +0x13
//    arrives eight instructions later than the load, because the function ORs the
//    padding in at the end; requiring a null test nearby finds only the decoration and
//    reports half the answer while looking rigorous. The invariant that does hold for
//    both fields is that each is decompressed with x28 and spilled to its OWN frame
//    slot, and read order is the declaration order.
inline bool folder_inner_padding(std::span<const uint32_t> body,
        uint32_t &padding, uint32_t &decoration) {
    unsigned hits = 0; uint32_t first = 0, second = 0, slot0 = 0, slot1 = 0;
    bool have_first = false;
    for (size_t i = 0; i + 2 < body.size(); ++i) {
        if (!insn_is_ldur_w(body[i]) || !insn_is_heap_add(body[i + 1])) continue;
        if (insn_rt(body[i + 1]) != insn_rt(body[i])) continue;
        if (!insn_is_stur_x(body[i + 2]) || insn_rt(body[i + 2]) != insn_rt(body[i])) continue;
        const int32_t sp = int32_t(insn_imm9(body[i + 2]));
        const uint32_t off = insn_imm9(body[i]);
        if (!have_first) {
            first = off; slot0 = uint32_t(sp); have_first = true;
        } else if (sp != int32_t(slot0)) {
            second = off; slot1 = uint32_t(sp); ++hits;
        }
    }
    // The two spill slots must be genuinely distinct. Sharing one would mean the scan
    // matched a value twice rather than finding padding AND decoration, and the single
    // `first == second` guard below cannot see that difference.
    if (hits != 1 || first == second || slot1 == slot0) return false;
    padding = first; decoration = second; return true;
}

// 6. EdgeInsets. The left value is stored as `stur d0,[x0,#7]` - a SIMD&FP store, one
//    encoding family above the GPR stores the three zero fields use. A scan restricted
//    to GPR stores sees only three of the four and cannot derive the base, which is
//    exactly the failure mode where every offset comes back zero.
inline bool folder_inner_inset(std::span<const uint32_t> body, uint32_t &inset) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 4 < body.size(); ++i) {
        if (!insn_is_stur_d(body[i]) || insn_rt(body[i]) != 0 || insn_rn(body[i]) != 0) continue;
        bool run = true;
        int32_t want[3];
        for (int k = 0; k < 3; ++k) {
            want[k] = int32_t(insn_imm9(body[i])) + 8 * (k + 1);
            const uint32_t s = body[i + 1 + size_t(k)];
            if (!insn_is_stur_x(s) || insn_rt(s) != 31 || insn_rn(s) != 0
                || int32_t(insn_imm9(s)) != want[k]) { run = false; break; }
        }
        if (!run) continue;
        found = insn_imm9(body[i]); ++hits;
    }
    if (hits != 1) return false;
    inset = found; return true;
}

// 7. The two AlignmentDirectional roots. They are the two arms of ONE TBNZ diamond
//    converging on a single frame store, selected by a single bit of the cross-axis
//    value, so they can only be derived together. Requiring the taken arm, the
//    fall-through arm, the hop over the target arm and the shared join is what makes
//    this structural rather than two unrelated constants.
inline bool folder_inner_directions(std::span<const uint32_t> body, uint32_t va,
        uint32_t &bit, uint32_t &root_taken, uint32_t &root_fallthrough) {
    unsigned hits = 0;
    for (size_t i = 0; i + 6 < body.size(); ++i) {
        if (!insn_is_tbnz(body[i])) continue;
        uint32_t tgt = 0;
        if (!insn_tbnz_target(va + uint32_t(i) * 4, body[i], tgt)) continue;
        if (tgt <= va || tgt >= va + uint32_t(body.size() - 4) * 4) continue;
        const size_t t = (tgt - va) / 4;
        if (t + 2 > body.size()) continue;
        uint32_t lo = 0, hi = 0;
        if (!insn_pool_offset(body[i + 1], body[i + 2], lo)) continue;
        if (!insn_is_b(body[i + 3])) continue;
        uint32_t join_va = 0;
        if (!insn_branch_target(va + uint32_t(i + 3) * 4, body[i + 3], join_va)) continue;
        if (join_va <= va || join_va >= va + uint32_t(body.size()) * 4) continue;
        const size_t j = (join_va - va) / 4;
        // the fall-through arm occupies i+1,i+2 and the hop i+3; the target arm i+4,i+5
        if (j != i + 6 || t + 2 != j) continue;
        if (!insn_pool_offset(body[t], body[t + 1], hi)) continue;
        if (!insn_is_stur_x(body[j]) || insn_rn(body[j]) != 29 || insn_imm9(body[j]) != uint32_t(-8)) continue;
        bit = insn_tbnz_bit(body[i]);
        root_taken = hi; root_fallthrough = lo; ++hits;
    }
    if (hits != 1) return false;
    return root_taken != root_fallthrough;
}

// 8. Alignment.center, from the PRODUCER side. Resolving it by the symbol a consumer
//    calls does not work here: the launcher's Dart symbols are obfuscated to unrelated
//    strings, so Container.build's coercion helper for the alignment slot resolves to
//    "p_category_shopping". The closure instead fills in the Container it just built,
//    storing a 32-bit compressed pool root straight into the alignment slot derived in
//    step 4 - so consumer-derived field and producer-derived root meet at one
//    instruction.
inline bool folder_inner_center(std::span<const uint32_t> body, uint32_t alignment,
        uint32_t &center) {
    unsigned hits = 0; uint32_t found = 0;
    for (size_t i = 0; i + 2 < body.size(); ++i) {
        uint32_t p = 0;
        if (!insn_pool_offset(body[i], body[i + 1], p)) continue;
        if (!insn_is_stur_w(body[i + 2]) || insn_rn(body[i + 2]) != 0
            || insn_imm9(body[i + 2]) != alignment) continue;
        found = p; ++hits;
    }
    if (hits != 1) return false;
    center = found; return true;
}

// Assemble the whole plan. Any step that cannot be proven refuses the entire bank
// rather than half-applying it: a partially relocated Container is worse than an
// untouched one, because the symptom (mis-centred title) is indistinguishable from
// several unrelated causes.
//
// No base VA is taken for Container.build or Container._paddingIncludingDecoration -
// both steps are purely structural (which base register, which spill slot, which read
// order), so a launcher that moves either function cannot silently shift a field.
inline bool folder_inner_container(std::span<const uint32_t> buildText,
        std::span<const uint32_t> build, std::span<const uint32_t> paddingBody,
        uint32_t bt_va, FolderInnerContainer &out) {
    FolderInnerContainer c;
    uint32_t dec = 0;
    if (!folder_inner_window(buildText, bt_va, c.window, c.owner_slot)) return false;
    if (!folder_inner_frame(buildText, bt_va, c.window, c.frame)) return false;
    if (c.owner_slot > c.frame) return false;
    if (!folder_inner_owner_store(buildText, bt_va, c.window, c.owner_slot)) return false;
    if (!folder_inner_alignment(build, c.alignment)) return false;
    if (!folder_inner_padding(paddingBody, c.padding, dec)) return false;
    if (!folder_inner_inset(buildText, c.inset)) return false;
    if (!folder_inner_directions(buildText, bt_va, c.direction_bit,
            c.direction_pool[0], c.direction_pool[1])) return false;
    if (!folder_inner_center(buildText, c.alignment, c.center_pool)) return false;
    if (c.center_pool == c.direction_pool[0] || c.center_pool == c.direction_pool[1]) return false;
    if (c.alignment == c.padding || c.alignment == c.inset || c.padding == c.inset) return false;
    out = c; return true;
}
} // namespace home_layout
