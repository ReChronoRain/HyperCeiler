// SPDX-License-Identifier: Apache-2.0
#pragma once

#include <stdint.h>

namespace hometweaks {
namespace a64 {

constexpr uint32_t kNop = 0xD503201Fu;
constexpr uint32_t kRet = 0xD65F03C0u;

inline bool IsB(uint32_t w) { return (w & 0xFC000000u) == 0x14000000u; }
inline bool IsBl(uint32_t w) { return (w & 0xFC000000u) == 0x94000000u; }
inline bool IsBCond(uint32_t w) { return (w & 0xFF000010u) == 0x54000000u; }
inline uint32_t BCondCond(uint32_t w) { return w & 0x0Fu; }
inline bool IsTb(uint32_t w) { return (w & 0x7E000000u) == 0x36000000u; }
inline bool TbIsNz(uint32_t w) { return (w & 0x01000000u) != 0; }
inline uint32_t TbBit(uint32_t w) {
    return ((w >> 19) & 0x1Fu) | (((w >> 31) & 0x1u) << 5);
}
inline uint32_t TbReg(uint32_t w) { return w & 0x1Fu; }

inline bool DecodeBranch(uint32_t w, uint32_t addr, uint32_t* target) {
    if ((w & 0xFC000000u) == 0x14000000u || (w & 0xFC000000u) == 0x94000000u) {
        int32_t imm = static_cast<int32_t>(w & 0x03FFFFFFu);
        if (imm & 0x02000000) imm -= 0x04000000;
        *target = static_cast<uint32_t>(
                static_cast<int64_t>(addr) + static_cast<int64_t>(imm) * 4);
        return true;
    }
    if ((w & 0xFF000010u) == 0x54000000u) {
        int32_t imm = static_cast<int32_t>((w >> 5) & 0x7FFFFu);
        if (imm & 0x40000) imm -= 0x80000;
        *target = static_cast<uint32_t>(
                static_cast<int64_t>(addr) + static_cast<int64_t>(imm) * 4);
        return true;
    }
    if ((w & 0x7E000000u) == 0x36000000u) {
        int32_t imm = static_cast<int32_t>((w >> 5) & 0x3FFFu);
        if (imm & 0x2000) imm -= 0x4000;
        *target = static_cast<uint32_t>(
                static_cast<int64_t>(addr) + static_cast<int64_t>(imm) * 4);
        return true;
    }
    return false;
}

inline bool MakeB(uint32_t from, uint32_t to, uint32_t* out) {
    const int64_t delta = static_cast<int64_t>(to) - static_cast<int64_t>(from);
    if ((delta & 3) != 0) return false;
    const int64_t words = delta >> 2;
    if (words < -(1 << 25) || words >= (1 << 25)) return false;
    *out = 0x14000000u | (static_cast<uint32_t>(words) & 0x03FFFFFFu);
    return true;
}

inline bool MakeBl(uint32_t from, uint32_t to, uint32_t* out) {
    uint32_t b = 0;
    if (!MakeB(from, to, &b)) return false;
    *out = (b & 0x03FFFFFFu) | 0x94000000u;
    return true;
}

inline bool MakeBCond(uint32_t from, uint32_t to, uint32_t cond, uint32_t* out) {
    if (cond > 15u) return false;
    const int64_t delta = static_cast<int64_t>(to) - static_cast<int64_t>(from);
    if ((delta & 3) != 0) return false;
    const int64_t words = delta >> 2;
    if (words < -(1 << 18) || words >= (1 << 18)) return false;
    *out = 0x54000000u | ((static_cast<uint32_t>(words) & 0x7FFFFu) << 5) | cond;
    return true;
}

inline bool IsMovz(uint32_t w) { return (w & 0xFFE00000u) == 0xD2800000u; }
inline uint32_t MovzImm(uint32_t w) { return (w >> 5) & 0xFFFFu; }
inline uint32_t MovzRd(uint32_t w) { return w & 0x1Fu; }

inline bool MakeMovz(uint32_t rd, uint32_t imm, uint32_t* out) {
    if (imm > 0xFFFFu) return false;
    *out = 0xD2800000u | ((imm & 0xFFFFu) << 5) | (rd & 0x1Fu);
    return true;
}
inline uint32_t MakeMovzRaw(uint32_t rd, uint32_t imm) {
    return 0xD2800000u | ((imm & 0xFFFFu) << 5) | (rd & 0x1Fu);
}

inline bool IsCmpImm(uint32_t w) { return (w & 0xFFC0001Fu) == 0xF100001Fu; }
inline uint32_t CmpImmRn(uint32_t w) { return (w >> 5) & 0x1Fu; }
inline uint32_t CmpImmValue(uint32_t w) { return (w >> 10) & 0xFFFu; }
inline uint32_t MakeCmpImm(uint32_t rn, uint32_t imm) {
    return 0xF100001Fu | ((imm & 0xFFFu) << 10) | ((rn & 0x1Fu) << 5);
}

inline bool IsSbfmAsr1(uint32_t w, uint32_t* rd) {
    if ((w & 0xFFFFFC00u) != 0x93417C00u) return false;
    if (rd != nullptr) *rd = w & 0x1Fu;
    return true;
}

inline bool IsSdivX(uint32_t w) { return (w & 0xFFE0FC00u) == 0x9AC00C00u; }
inline uint32_t SdivRd(uint32_t w) { return w & 0x1Fu; }
inline uint32_t SdivRn(uint32_t w) { return (w >> 5) & 0x1Fu; }
inline bool IsSubRegX(uint32_t w) {
    return (w & 0xFFE0FC00u) == 0xCB000000u && (w & 0xFC00u) == 0u;
}
inline uint32_t SubRegXRd(uint32_t w) { return w & 0x1Fu; }
inline uint32_t SubRegXRn(uint32_t w) { return (w >> 5) & 0x1Fu; }

inline bool IsMovRegX(uint32_t w, uint32_t* rd, uint32_t* rm) {
    if ((w & 0xFFE0FFE0u) != 0xAA0003E0u) return false;
    const uint32_t src = (w >> 16) & 0x1Fu;
    if (src == 31u) return false;
    if (rd != nullptr) *rd = w & 0x1Fu;
    if (rm != nullptr) *rm = src;
    return true;
}

inline bool IsLdrXUimm(uint32_t w) { return (w & 0xFFC00000u) == 0xF9400000u; }

inline void LdrXUimmFields(uint32_t w, uint32_t* rt, uint32_t* rn, uint32_t* imm) {
    if (rt != nullptr) *rt = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (imm != nullptr) *imm = ((w >> 10) & 0xFFFu) * 8u;
}

inline bool IsStrXUimm(uint32_t w, uint32_t offsetBytes) {
    if ((w & 0xFFC00000u) != 0xF9000000u) return false;
    return ((w >> 10) & 0xFFFu) * 8u == offsetBytes;
}

inline bool IsStpX15(uint32_t w) {
    return (w & 0xFFC00000u) == 0xA9000000u && ((w >> 5) & 0x1Fu) == 15u &&
           ((w >> 15) & 0x7Fu) == 0u;
}

inline bool IsSturW(uint32_t w, uint32_t* rt, uint32_t* rn, uint32_t* imm) {
    if ((w & 0xFFE00C00u) != 0xB8000000u) return false;
    const uint32_t slot = (w >> 12) & 0x1FFu;
    if (slot >= 256u) return false;
    if (rt != nullptr) *rt = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (imm != nullptr) *imm = slot;
    return true;
}

inline bool IsSturX(uint32_t w, uint32_t* rt, uint32_t* rn, uint32_t* imm) {
    if ((w & 0xFFE00C00u) != 0xF8000000u) return false;
    const uint32_t slot = (w >> 12) & 0x1FFu;
    if (slot >= 256u) return false;
    if (rt != nullptr) *rt = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (imm != nullptr) *imm = slot;
    return true;
}

inline bool IsLdurD(uint32_t w, uint32_t* rt, uint32_t* rn, uint32_t* imm) {
    if ((w & 0xFFE00C00u) != 0xFC400000u) return false;
    const uint32_t off = (w >> 12) & 0x1FFu;
    if (off >= 256u) return false;
    if (rt != nullptr) *rt = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (imm != nullptr) *imm = off;
    return true;
}

inline bool MakeLdurD(uint32_t rt, uint32_t rn, uint32_t imm, uint32_t* out) {
    if (rt > 31u || rn > 31u || imm > 255u) return false;
    *out = 0xFC400000u | (imm << 12) | (rn << 5) | rt;
    return true;
}

inline bool IsFmulD(uint32_t w, uint32_t* rd, uint32_t* rn, uint32_t* rm) {
    if ((w & 0xFF20FC00u) != 0x1E200800u) return false;
    if (rd != nullptr) *rd = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (rm != nullptr) *rm = (w >> 16) & 0x1Fu;
    return true;
}

inline bool IsFmovDImm(uint32_t w) { return (w & 0xFFE01C00u) == 0x1E601000u; }
inline uint32_t FmovDImmCode(uint32_t w) { return (w >> 13) & 0xFFu; }
inline uint32_t FmovDImmRd(uint32_t w) { return w & 0x1Fu; }
inline bool MakeFmovDImm(uint32_t rd, uint32_t code, uint32_t* out) {
    if (code > 0xFFu || rd > 31u) return false;
    *out = 0x1E601000u | ((code & 0xFFu) << 13) | (rd & 0x1Fu);
    return true;
}

inline bool IsLdurUnscaled(uint32_t w, uint32_t* size, uint32_t* rt, uint32_t* rn,
                           int32_t* simm) {
    if ((w & 0x3B000000u) != 0x38000000u) return false;
    if ((w & 0x00000C00u) != 0u) return false;
    int32_t imm = static_cast<int32_t>((w >> 12) & 0x1FFu);
    if (imm & 0x100) imm -= 0x200;
    if (size != nullptr) *size = (w >> 30) & 3u;
    if (rt != nullptr) *rt = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    if (simm != nullptr) *simm = imm;
    return true;
}

inline bool IsAddXHeapBase(uint32_t w, uint32_t* rd, uint32_t* rn) {
    if ((w & 0xFFE0FC00u) != 0x8B008000u) return false;
    if (((w >> 16) & 0x1Fu) != 28u) return false;
    if (rd != nullptr) *rd = w & 0x1Fu;
    if (rn != nullptr) *rn = (w >> 5) & 0x1Fu;
    return true;
}

inline bool IsStpFpLrPre(uint32_t w) { return w == 0xA9BF79FDu; }
inline bool IsMovX29X15(uint32_t w) { return w == 0xAA0F03FDu; }
inline uint32_t MakeMovX15X29() { return 0xAA1D03EFu; }
inline uint32_t MakeLdpFpLr() { return 0xA8C179FDu; }

inline bool DefinesReg(uint32_t w, uint32_t reg) {
    if (IsMovz(w)) return MovzRd(w) == reg;
    if (IsLdrXUimm(w)) {
        uint32_t rt = 0;
        LdrXUimmFields(w, &rt, nullptr, nullptr);
        return rt == reg;
    }
    if ((w & 0xFFE0FFE0u) == 0xAA0003E0u) {
        return (w & 0x1Fu) == reg;
    }
    if ((w & 0xFFC00000u) == 0xF8400000u || (w & 0xFFC00000u) == 0xB8400000u) {
        return (w & 0x1Fu) == reg;
    }
    if ((w & 0xFFC00000u) == 0xB9400000u) return (w & 0x1Fu) == reg;
    if ((w & 0xFF800000u) == 0x11000000u) return (w & 0x1Fu) == reg;
    if ((w & 0xFF800000u) == 0x91000000u) return (w & 0x1Fu) == reg;
    if ((w & 0xFF800000u) == 0x0B000000u) return (w & 0x1Fu) == reg;
    if ((w & 0xFF800000u) == 0x8B000000u) return (w & 0x1Fu) == reg;
    return false;
}

}
}
