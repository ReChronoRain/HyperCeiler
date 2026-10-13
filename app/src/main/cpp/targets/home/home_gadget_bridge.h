/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace home_layout {
// Private protocol, NOT Gadget itemType=5 or PA ServiceDelivery widget_type=5.
// Fits a compressed Dart Smi (31 signed bits); original putInt performs conversion.
inline constexpr uint32_t kClearGadgetWireType = 0x18430c01;
inline constexpr uint32_t kClearGadgetCid = 0x7f2;

inline constexpr const char *kGadgetSerializer = "AssistantDragDataHelper.putWidgetIntoBundle";
inline constexpr const char *kGadgetModel = "GadgetInfoModel.cloneModel";
inline constexpr const char *kGadgetBundlePut = "BundleImpl.putInt";
// kWidgetMoveSymbol lives in home_widget_move.h; the gate function is shared by both bridges.

/*
 * No offset, address or field displacement is written down anywhere in this file. Each anchor
 * below names an *instruction semantic* that the runtime scans for, and the scan must find
 * exactly one site: zero matches means the launcher reshaped that part and the bridge declines
 * to bind; two matches means the shape does not identify the site, and binding either one
 * would splice the wrong code. Both outcomes are failures by design -- a silently mis-bound
 * bridge is worse than an inert one, because then the original path no longer runs at all.
 *
 * The semantics were read off launcher 7722 with a disassembler, not by eyeballing hex. On
 * that build each shape below is unique inside its function, which is what makes the scan a
 * determination rather than a guess.
 */
struct GadgetAnchors {
    uint32_t put_int = 0;      // bl BundleImpl.putInt
    uint32_t ret = 0;          // the pair storing the private marker
    uint32_t select = 0;       // the branch that picks the marker path
    uint32_t common = 0;       // the Bundle field read shared by both paths
    uint32_t id_offset = 0;    // ldur w2,[x0,#imm]: the cleared id, immediate learned
    uint32_t span_entry = 0;   // tbnz after the span bl
    uint32_t span_reject = 0;  // shared reject arm the tbnz branches to
    uint32_t widget_gate = 0;  // tbnz on the isMIUIWidget bit
    bool ok = false;
};

/* ---- ARM64 encodings the scan compares against, built rather than pasted ---- */

constexpr uint32_t movz_32(uint32_t imm16, uint32_t rd) {
    return 0xD2800000u | ((imm16 & 0xFFFFu) << 5) | (rd & 0x1Fu);
}
/*
 * The discriminator is only ever compared as "this one instruction, with this immediate".
 * Matching on the opcode field and the 12-bit immediate -- and deliberately not on the
 * register fields -- keeps the scan working across the flag encodings a 32-bit immediate
 * compare can take, which differ in bits the address form would otherwise pin down. The
 * immediate stays unique inside the body, which is what the scan actually needs.
 */
constexpr bool is_imm_compare(uint32_t w, uint32_t imm12) {
    return (w & 0x7F800000u) == 0x71000000u && ((w >> 10) & 0xFFFu) == imm12;
}
constexpr bool is_cond_branch(uint32_t w) { return (w & 0xFF000010u) == 0x54000000u; }
/* ldur wRt,[xRn,#imm9]: the only 32-bit load that reaches a positive-byte-offset field. */
constexpr bool is_ldur_w(uint32_t w, uint32_t rt, int32_t &imm9) {
    if ((w & 0xFFE00C00u) != 0xB8400000u || (w & 0x1Fu) != rt) return false;
    imm9 = static_cast<int32_t>((w >> 12) & 0x1FF);
    if (imm9 & 0x100) imm9 -= 0x200;
    return true;
}
/* tbnz wRt,#bit: 0x37 in the top byte, bit in [23:19], Rt in [4:0]. */
constexpr bool is_tbnz_w(uint32_t w, uint32_t rt, uint32_t bit) {
    return (w >> 24) == 0x37u && (w & 0x1Fu) == rt && ((w >> 19) & 0x1Fu) == bit;
}
// TBZ/TBNZ uses signed imm14 in [18:5], not the B.cond imm19 or an imm16.
constexpr int32_t branch_disp_words(uint32_t w) {
    const uint32_t imm = (w >> 5) & 0x3FFFu;
    return (imm & 0x2000u) ? static_cast<int32_t>(imm) - 0x4000 : static_cast<int32_t>(imm);
}
constexpr bool is_bl(uint32_t w) { return (w & 0xFC000000u) == 0x94000000u; }
constexpr int32_t bl_imm_words(uint32_t w) {
    uint32_t imm = w & 0x03FFFFFFu;
    if (imm & 0x02000000u) imm -= 0x04000000u;
    return static_cast<int32_t>(imm);
}
/*
 * `add x0, x0, x28, lsl #32`: the sign-extension pair the compiler emits after a narrow load.
 * Matched on the full low half -- shift, second operand and register -- because the immediate
 * 32 in this position is a register number, not a value.
 */
constexpr bool is_add_shift32(uint32_t w) { return (w & 0xFFFFFC00u) == 0x8B1C8000u; }
/*
 * `ldr wRt,[xRn,xRm]`: the register-offset 32-bit load, the only form that takes the field key
 * as a register. The `option` field selects the addressing mode, so it is compared too --
 * otherwise the unscaled-immediate form would match as well and stop the scan from being a
 * determination.
 */
constexpr bool is_ldr32_reg(uint32_t w, uint32_t rt, uint32_t rm) {
    return (w & 0xFFE0FC00u) == 0xB8606800u && (w & 0x1Fu) == rt
        && ((w >> 16) & 0x1Fu) == rm;
}

/* imm9 is a signed 9-bit field at [20:12] for both the 32- and 64-bit unscaled loads. */
constexpr int32_t load_imm9(uint32_t w) {
    const uint32_t raw = (w >> 12) & 0x1FFu;
    return (raw & 0x100u) ? static_cast<int32_t>(raw) - 0x200 : static_cast<int32_t>(raw);
}
/* The unscaled 64-bit load that reloads a spilled slot from the frame pointer. */
constexpr bool is_ldur_sp(uint32_t w, int32_t want_imm, uint32_t base) {
    return (w & 0xFFE00C00u) == 0xF8400000u && ((w >> 5) & 0x1Fu) == base
        && load_imm9(w) == want_imm;
}

/*
 * These three are data the launcher itself carries, not addresses: the Bundle key the marker
 * is stored under, the discriminator the select branches on, and the model field key the clone
 * factory indexes. Each is compared against, never used to form an address -- if the launcher
 * renumbers any of them the scan finds nothing and the bridge declines, which is the correct
 * outcome, whereas a stored offset would have kept pointing somewhere plausible.
 */
inline constexpr uint32_t kMarkerKey = 0x143;            // mov x17,#0x143; ldr w3,[x0,x17]
inline constexpr uint32_t kSelectDiscriminator = 0x7ED;  // cmp #0x7ed; b.<cond>
inline constexpr uint32_t kClearField = 0x9F;            // ldur w3,[x0,#0x9f]
inline constexpr int32_t kClearSlot = -0x18;            // the spilled Bundle both reads share
inline constexpr uint32_t kModelFieldKey = 0x11B;        // mov x17,#0x11b; ldr w3,[x1,x17]
/* The Dart frame pointer every AOT body keeps its locals under. */
inline constexpr uint32_t kFramePointer = 29;

} // namespace home_layout
