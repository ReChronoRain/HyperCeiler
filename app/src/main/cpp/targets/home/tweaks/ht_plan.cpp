// SPDX-License-Identifier: Apache-2.0
#include "ht_plan.h"
#include "a64.h"
#include "symtab.h"

#include <inttypes.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

namespace hometweaks {
namespace {

bool gForceSymbolOnly = false;

constexpr uint32_t kFolderColsOffset = 0x2638;
constexpr uint32_t kFolderColsOrigSmi = 8;
constexpr uint32_t kFolderColsOfficial = kFolderColsOrigSmi / 2u;

constexpr uint32_t kCellCountXOffset = 19;

void SetWhy(char* buf, size_t cap, const char* fmt, ...) {
    if (buf == nullptr || cap == 0) return;
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, cap, fmt, ap);
    va_end(ap);
}

template <typename Fn>
void ForEachExecWord(const CodeView& code, Fn fn) {
    const Image& img = code.image();
    for (size_t s = 0; s < img.segmentCount; ++s) {
        const Segment& seg = img.segments[s];
        if ((seg.flags & 0x1u) == 0) continue;
        if (seg.end <= seg.begin) continue;
        const uint32_t baseVa = static_cast<uint32_t>(seg.begin - img.base);
        const size_t words = (seg.end - seg.begin) / 4;
        const uint32_t* p = reinterpret_cast<const uint32_t*>(seg.begin);
        for (size_t i = 0; i < words; ++i) {
            if (!fn(baseVa + static_cast<uint32_t>(i * 4), p[i])) return;
        }
    }
}

bool LocateFunction(const CodeView& code, const char* needle, uint32_t* va, uint32_t* size,
                    char* why, size_t whyCap) {
    uint32_t symVa = 0, symSize = 0;
    if (SymbolIndex::Instance().Find(needle, &symVa, &symSize) && symSize >= 8) {
        *va = symVa;
        *size = symSize;
        return true;
    }
    if (gForceSymbolOnly) {
        SetWhy(why, whyCap, "符号表里没有 %s（当前是宿主验证的「只走符号表」模式）", needle);
        return false;
    }

    const FunctionSignature* sig = FindSignatureByName(needle);
    if (sig == nullptr) {
        SetWhy(why, whyCap, "签名表里没有 %s（符号表也没给）", needle);
        return false;
    }
    uint32_t found = 0;
    int matches = 0;
    if (!code.FindSignature(*sig, &found, &matches)) {
        SetWhy(why, whyCap, "按签名找不到 %s（命中 %d 处）", needle, matches);
        return false;
    }
    *va = found;
    *size = sig->wordCount * 4;
    return true;
}

bool FirstBl(const CodeView& code, uint32_t lo, uint32_t hi, uint32_t* at, uint32_t* to) {
    for (uint32_t a = lo; a + 4 <= hi; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) return false;
        if (!a64::IsBl(w)) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (at != nullptr) *at = a;
        if (to != nullptr) *to = t;
        return true;
    }
    return false;
}

int CountBlTo(const CodeView& code, uint32_t lo, uint32_t hi, uint32_t target,
              uint32_t* firstAt) {
    int n = 0;
    for (uint32_t a = lo; a + 4 <= hi; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsBl(w)) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (t != target) continue;
        if (n == 0 && firstAt != nullptr) *firstAt = a;
        ++n;
    }
    return n;
}

void FindCallers(const CodeView& code, uint32_t target, std::vector<uint32_t>* out) {
    ForEachExecWord(code, [&](uint32_t va, uint32_t w) -> bool {
        if (!a64::IsBl(w)) return true;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, va, &t)) return true;
        if (t == target) out->push_back(va);
        return true;
    });
}

bool HasInbound(const CodeView& code, uint32_t lo, uint32_t hi) {
    bool found = false;
    ForEachExecWord(code, [&](uint32_t va, uint32_t w) -> bool {
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, va, &t)) return true;
        if (t >= lo && t < hi) {
            found = true;
            return false;
        }
        return true;
    });
    return found;
}

int DefReg(uint32_t w) {
    if ((w & 0x7C000000u) == 0x14000000u) return -1;
    if ((w & 0x7E000000u) == 0x34000000u) return -1;
    if ((w & 0x7E000000u) == 0x36000000u) return -1;
    if ((w & 0xFF000010u) == 0x54000000u) return -1;
    if ((w & 0xFFFFFC1Fu) == 0xD65F0000u) return -1;
    if ((w & 0xFFFFFC1Fu) == 0xD61F0000u) return -1;
    if ((w & 0xFFFFFC1Fu) == 0xD63F0000u) return -1;
    if ((w & 0x3B000000u) == 0x38000000u || (w & 0x3B000000u) == 0x39000000u) {
        return ((w >> 22) & 1u) ? static_cast<int>(w & 31u) : -1;
    }
    if ((w & 0x3A000000u) == 0x28000000u) {
        return ((w >> 22) & 1u) ? static_cast<int>(w & 31u) : -1;
    }
    if ((w & 0x1F000000u) == 0x11000000u || (w & 0x1F000000u) == 0x12000000u ||
        (w & 0x1F000000u) == 0x13000000u || (w & 0x0F000000u) == 0x0A000000u ||
        (w & 0x0F000000u) == 0x0B000000u) {
        const uint32_t rd = w & 31u;
        return rd == 31u ? -1 : static_cast<int>(rd);
    }
    return -1;
}

bool X0IsDead(const CodeView& code, uint32_t site, int limit = 8) {
    for (int i = 1; i <= limit; ++i) {
        const uint32_t a = site + static_cast<uint32_t>(i) * 4;
        uint32_t w = 0;
        if (!code.Word(a, &w)) return false;
        const uint32_t rn = (w >> 5) & 31u;
        const uint32_t rm = (w >> 16) & 31u;
        const uint32_t op0f = w & 0x0F000000u;
        const bool readsX0 =
                rn == 0u ||
                (rm == 0u && (op0f == 0x0A000000u || op0f == 0x0B000000u)) ||
                (a64::IsTb(w) && a64::TbReg(w) == 0u) ||
                (((w & 0x7E000000u) == 0x34000000u || (w & 0x7E000000u) == 0x36000000u) &&
                 (w & 31u) == 0u);
        if (DefReg(w) == 0) return true;
        if (readsX0) return false;
        if (a64::IsB(w) || a64::IsBl(w) || w == a64::kRet || a64::IsBCond(w) || a64::IsTb(w)) {
            return true;
        }
    }
    return true;
}

bool LocateFeature9(const CodeView& code, LocatedSites* out) {
    uint32_t ovVa = 0, ovSize = 0;
    if (!LocateFunction(code, "_insertClearButtonOverlay", &ovVa, &ovSize, out->why9,
                        sizeof(out->why9))) {
        return false;
    }
    std::vector<uint32_t> callers;
    FindCallers(code, ovVa, &callers);
    if (callers.empty()) {
        SetWhy(out->why9, sizeof(out->why9), "_insertClearButtonOverlay 没有任何调用方");
        return false;
    }
    uint32_t kept = 0;
    for (uint32_t site : callers) {
        if (kept >= kMaxNoClearSites) break;
        if (!X0IsDead(code, site)) continue;
        uint32_t cur = 0;
        if (!code.Word(site, &cur)) continue;
        if (!a64::IsBl(cur)) continue;
        out->noClearSites[kept++] = site;
    }
    if (kept == 0) {
        SetWhy(out->why9, sizeof(out->why9),
               "_insertClearButtonOverlay 的 %zu 个调用点都不能安全 nop（返回值有人用）",
               callers.size());
        return false;
    }

    uint32_t fdVa = 0, fdSize = 0;
    if (!LocateFunction(code, "_buildFoldDockClearContainer", &fdVa, &fdSize, out->why9,
                        sizeof(out->why9))) {
        return false;
    }
    if (fdSize < 24) {
        SetWhy(out->why9, sizeof(out->why9), "折叠屏清理容器只有 %u 字节，塞不下早返回", fdSize);
        return false;
    }

    uint32_t constAt = 0, constWord = 0;
    int constCount = 0;
    for (uint32_t a = fdVa + 24; a + 16 <= fdVa + fdSize; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsLdrXUimm(w)) continue;
        uint32_t rt = 0;
        a64::LdrXUimmFields(w, &rt, nullptr, nullptr);
        if (rt != 0) continue;
        uint32_t t1 = 0, t2 = 0, t3 = 0;
        if (!code.Word(a + 4, &t1) || !code.Word(a + 8, &t2) || !code.Word(a + 12, &t3)) break;
        if (t1 != a64::MakeMovX15X29() || t2 != a64::MakeLdpFpLr() || t3 != a64::kRet) continue;
        ++constCount;
        if (constCount == 1) {
            constAt = a;
            constWord = w;
        }
    }
    if (constCount != 1) {
        SetWhy(out->why9, sizeof(out->why9),
               "折叠屏清理容器里「返回常量 widget」的形状有 %d 处（预期 1）", constCount);
        return false;
    }
    (void) constAt;

    uint32_t p0 = 0, p1 = 0;
    if (!code.Word(fdVa, &p0) || !code.Word(fdVa + 4, &p1) ||
        !a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) {
        SetWhy(out->why9, sizeof(out->why9), "折叠屏清理容器函数头不是标准 Dart 帧");
        return false;
    }
    const uint32_t at = fdVa + 8;
    if (HasInbound(code, at, at + 16)) {
        SetWhy(out->why9, sizeof(out->why9), "折叠屏清理容器函数头后有分支跳入，不敢覆盖");
        return false;
    }

    out->overlayVa = ovVa;
    out->foldVa = fdVa;
    out->constWord = constWord;
    out->noClearCount = kept;
    out->why9[0] = '\0';
    out->ok9 = true;
    return true;
}

bool LocateFeature16Square(const CodeView& code, LocatedSites* out) {
    constexpr const char* kFn = "FolderIconGetxController.folderIconSize";
    constexpr const char* kCfgFn = "GridController.currentConfig";

    uint32_t fn = 0, fnSize = 0, cfgFn = 0, cfgSize = 0;
    char why[192] = {};
    if (!LocateFunction(code, kFn, &fn, &fnSize, why, sizeof(why)) ||
        !LocateFunction(code, kCfgFn, &cfgFn, &cfgSize, why, sizeof(why))) {
        SetWhy(out->why16sq, sizeof(out->why16sq), "%s", why);
        return false;
    }

    uint32_t wideVa = 0, wideImm = 0, tallVa = 0, tallImm = 0;
    int found = 0;
    const uint32_t end = fn + fnSize;
    for (uint32_t a = fn + 4; a + 4 <= end; a += 4) {
        uint32_t w = 0, prev = 0;
        if (!code.Word(a, &w) || !code.Word(a - 4, &prev)) break;
        uint32_t rt = 0, rn = 0, imm = 0;
        if (!a64::IsLdurD(w, &rt, &rn, &imm) || rt != 0u || rn != 0u) continue;
        if (!a64::IsBl(prev)) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(prev, a - 4, &t) || t != cfgFn) continue;
        ++found;
        if (found == 1) {
            wideVa = a;
            wideImm = imm;
        } else {
            tallVa = a;
            tallImm = imm;
            break;
        }
    }
    if (found < 2 || wideVa == 0 || tallVa == 0) {
        SetWhy(out->why16sq, sizeof(out->why16sq),
               "%s 里找不到「bl currentConfig + ldur d0,[x0,#槽]」的宽度/高度基准对", kFn);
        return false;
    }
    if (tallImm == wideImm) {
        SetWhy(out->why16sq, sizeof(out->why16sq),
               "%s 的宽高基准已经是同一个槽位（#%u），无需修正", kFn, wideImm);
        return false;
    }
    uint32_t want = 0;
    if (!a64::MakeLdurD(0, 0, wideImm, &want)) {
        SetWhy(out->why16sq, sizeof(out->why16sq), "高度基准槽位 #%u 无法编码", wideImm);
        return false;
    }
    out->squareFuncVa = fn;
    out->squareSiteVa = tallVa;
    out->squareCalleeVa = wideVa;
    out->ok16sq = true;
    out->why16sq[0] = '\0';
    return true;
}

bool LocateFeature16(const CodeView& code, LocatedSites* out) {
    constexpr int kGateBack = 28;

    int hits = 0;
    uint32_t movzVa = 0, movzRd = 0, storeVa = 0, colsOffset = 0;

    uint32_t winVa = 0, winSize = 0;
    if (SymbolIndex::Instance().Find("GridController.init", &winVa, &winSize) && winSize >= 16) {
        const uint32_t windowEnd = winVa + winSize;
        for (uint32_t a = winVa; a + 8 <= windowEnd; a += 4) {
            uint32_t w = 0;
            if (!code.Word(a, &w)) break;
            if (!a64::IsMovz(w)) continue;
            const uint32_t imm = a64::MovzImm(w);
            if ((imm & 1u) != 0u || imm < 2u || imm > 32u) continue;
            for (int j = 1; j <= 5; ++j) {
                const uint32_t aa = a + static_cast<uint32_t>(j) * 4;
                if (aa + 4 > windowEnd) break;
                uint32_t w2 = 0;
                if (!code.Word(aa, &w2)) break;
                if ((w2 & 0xFFC00000u) != 0xF9000000u) continue;
                ++hits;
                if (hits == 1) {
                    movzVa = a;
                    movzRd = a64::MovzRd(w);
                    storeVa = aa;
                    colsOffset = ((w2 >> 10) & 0xFFFu) * 8u;
                }
                break;
            }
            if (hits >= 2) break;
        }
        if (hits != 1) {
            SetWhy(out->why16, sizeof(out->why16),
                   "GridController.init 里「movz 列数 → str 字段」的形状有 %d 处（预期 1）",
                   hits);
            return false;
        }
    } else {
        ForEachExecWord(code, [&](uint32_t va, uint32_t w) -> bool {
            if (!a64::IsMovz(w) || a64::MovzImm(w) != kFolderColsOrigSmi) return true;
            for (int j = 1; j <= 5; ++j) {
                const uint32_t aa = va + static_cast<uint32_t>(j) * 4;
                uint32_t w2 = 0;
                if (!code.Word(aa, &w2)) break;
                if (!a64::IsStrXUimm(w2, kFolderColsOffset)) continue;
                ++hits;
                if (hits == 1) {
                    movzVa = va;
                    movzRd = a64::MovzRd(w);
                    storeVa = aa;
                    colsOffset = kFolderColsOffset;
                }
                break;
            }
            return hits < 2;
        });

        if (hits != 1) {
            SetWhy(out->why16, sizeof(out->why16),
                   "列数写入点 `movz #8 → str [x?,#0x%x]` 命中 %d 处（预期 1）",
                   kFolderColsOffset, hits);
            return false;
        }
    }

    uint32_t gateVa = 0;
    for (int j = 1; j <= kGateBack; ++j) {
        const uint32_t a = storeVa - static_cast<uint32_t>(j) * 4;
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsBCond(w)) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (t == storeVa + 4 || t == movzVa) {
            gateVa = a;
            break;
        }
    }

    out->movzVa = movzVa;
    out->movzRd = movzRd;
    out->storeVa = storeVa;
    out->gateVa = gateVa;
    out->colsOffset = colsOffset;
    out->why16[0] = '\0';
    out->ok16 = true;

    LocateFeature16Square(code, out);
    return true;
}

bool LocateFeature18(const CodeView& code, LocatedSites* out) {
    uint32_t sv = 0, sSize = 0;
    if (!LocateFunction(code, "_syncVisibility", &sv, &sSize, out->why18, sizeof(out->why18))) {
        return false;
    }
    const uint32_t svEnd = sv + sSize;

    uint32_t tbAt = 0, tbTgt = 0;
    bool foundTb = false;
    for (uint32_t a = sv; a + 4 <= svEnd; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsTb(w) || !a64::TbIsNz(w) || a64::TbBit(w) != 4) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (t > sv && t < svEnd) {
            tbAt = a;
            tbTgt = t;
            foundTb = true;
            break;
        }
    }
    if (!foundTb) {
        SetWhy(out->why18, sizeof(out->why18), "_syncVisibility 里找不到 tbnz w?,#4 的在内分叉");
        return false;
    }

    uint32_t fwdAt = 0, fwdTo = 0;
    if (!FirstBl(code, tbAt + 4, tbTgt, &fwdAt, &fwdTo)) {
        SetWhy(out->why18, sizeof(out->why18), "_syncVisibility 顺序路径里没有 bl");
        return false;
    }
    uint32_t revAt = 0, revTo = 0;
    if (!FirstBl(code, tbTgt, svEnd, &revAt, &revTo)) {
        SetWhy(out->why18, sizeof(out->why18), "_syncVisibility 跳转路径里没有 bl");
        return false;
    }
    if (fwdTo == revTo) {
        SetWhy(out->why18, sizeof(out->why18), "forward/reverse 目标相同，形状可疑");
        return false;
    }

    uint32_t bneAt = 0, bneTgt = 0;
    bool foundBne = false;
    const uint32_t bneLo = (tbAt > sv + 64) ? (tbAt - 64) : sv;
    for (uint32_t a = bneLo; a + 4 <= tbAt; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsBCond(w) || a64::BCondCond(w) != 1) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (t > sv && t < svEnd) {
            bneAt = a;
            bneTgt = t;
            foundBne = true;
            break;
        }
    }
    if (!foundBne) {
        SetWhy(out->why18, sizeof(out->why18), "_syncVisibility 里找不到 early-return 的 b.ne");
        return false;
    }

    uint32_t initVa = 0, initSize = 0;
    if (!LocateFunction(code, "initState", &initVa, &initSize, out->why18, sizeof(out->why18))) {
        return false;
    }
    uint32_t initBlAt = 0;
    const int initBlCount = CountBlTo(code, initVa, initVa + initSize, fwdTo, &initBlAt);
    if (initBlCount != 1) {
        SetWhy(out->why18, sizeof(out->why18),
               "initState 里到 forward 目标的调用点有 %d 处（预期 1）", initBlCount);
        return false;
    }

    out->svVa = sv;
    out->initVa = initVa;
    out->bneAt = bneAt;
    out->bneTgt = bneTgt;
    out->tbAt = tbAt;
    out->tbTgt = tbTgt;
    out->initBlAt = initBlAt;
    out->revTo = revTo;
    out->why18[0] = '\0';
    out->ok18 = true;
    return true;
}

bool IsConditionalBranch(uint32_t w) {
    return a64::IsBCond(w) || (w & 0x7E000000u) == 0x34000000u ||
           (w & 0x7E000000u) == 0x36000000u;
}

bool LocateFeature4(const CodeView& code, LocatedSites* out) {
    uint32_t va = 0, size = 0;
    if (!LocateFunction(code, "PadRowColHandler.handle", &va, &size, out->why4,
                        sizeof(out->why4))) {
        return false;
    }
    if (size < 32) {
        SetWhy(out->why4, sizeof(out->why4), "平板 handler 只有 %u 字节，形状不像预期", size);
        return false;
    }

    uint32_t pairAt[2] = {0, 0};
    uint32_t pairRd[2][2] = {{0, 0}, {0, 0}};
    uint32_t pairImm[2][2] = {{0, 0}, {0, 0}};
    int pairs = 0;
    for (uint32_t a = va; a + 4 <= va + size; a += 4) {
        uint32_t w1 = 0, w2 = 0;
        if (!code.Word(a, &w1) || !code.Word(a + 4, &w2)) break;
        if (!a64::IsMovz(w1) || !a64::IsMovz(w2)) continue;
        const uint32_t i1 = a64::MovzImm(w1);
        const uint32_t i2 = a64::MovzImm(w2);
        if (i1 == i2) continue;
        if (i1 < kPadGridMin || i1 > kPadGridMax) continue;
        if (i2 < kPadGridMin || i2 > kPadGridMax) continue;
        if (pairs < 2) {
            pairAt[pairs] = a;
            pairRd[pairs][0] = a64::MovzRd(w1);
            pairRd[pairs][1] = a64::MovzRd(w2);
            pairImm[pairs][0] = i1;
            pairImm[pairs][1] = i2;
        }
        ++pairs;
    }
    if (pairs != 2) {
        SetWhy(out->why4, sizeof(out->why4),
               "平板 handler 里相邻 movz 组数 = %d（预期 2：竖屏/横屏各一组）", pairs);
        return false;
    }
    if (!(pairImm[0][0] == pairImm[1][1] && pairImm[0][1] == pairImm[1][0])) {
        SetWhy(out->why4, sizeof(out->why4),
               "两组 movz 不是镜像关系（(%u,%u) vs (%u,%u)），不敢动", pairImm[0][0],
               pairImm[0][1], pairImm[1][0], pairImm[1][1]);
        return false;
    }
    if (!(pairRd[0][0] == pairRd[1][0] && pairRd[0][1] == pairRd[1][1])) {
        SetWhy(out->why4, sizeof(out->why4),
               "两组 movz 的目的寄存器不一致（x%u/x%u vs x%u/x%u）", pairRd[0][0],
               pairRd[0][1], pairRd[1][0], pairRd[1][1]);
        return false;
    }

    const uint32_t a2 = pairAt[1];
    const uint32_t tailLo = a2 + 8;
    uint32_t tailHi = a2 + 8 + 64;
    if (tailHi > va + size) tailHi = va + size;
    bool hasStp = false, hasBl = false;
    for (uint32_t a = tailLo; a + 4 <= tailHi; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (a64::IsStpX15(w)) hasStp = true;
        if (a64::IsBl(w)) hasBl = true;
    }
    if (!hasStp || !hasBl) {
        SetWhy(out->why4, sizeof(out->why4),
               "两组 movz 之后（%#x..%#x）找不到 stp,[x15]+bl，不像行列数传出点", tailLo,
               tailHi);
        return false;
    }

    out->padVa = va;
    for (int i = 0; i < 2; ++i) {
        out->padSiteVa[i * 2] = pairAt[i];
        out->padSiteVa[i * 2 + 1] = pairAt[i] + 4;
        out->padSiteRd[i * 2] = pairRd[i][0];
        out->padSiteRd[i * 2 + 1] = pairRd[i][1];
        out->padSiteImm[i * 2] = pairImm[i][0];
        out->padSiteImm[i * 2 + 1] = pairImm[i][1];
    }
    out->why4[0] = '\0';
    out->ok4 = true;
    return true;
}

bool LocateFeature19(const CodeView& code, LocatedSites* out) {
    constexpr uint32_t kSlot = 19;
    uint32_t va = 0, size = 0;
    if (!LocateFunction(code, "PhoneRowColHandler.handle", &va, &size, out->why19,
                        sizeof(out->why19))) {
        return false;
    }

    uint32_t siteVa[kMaxPhoneSites] = {0, 0};
    uint32_t siteRd[kMaxPhoneSites] = {0, 0};
    uint32_t siteImm[kMaxPhoneSites] = {0, 0};
    int hits = 0;
    for (uint32_t a = va; a + 4 <= va + size; a += 4) {
        uint32_t w = 0, nxt = 0;
        if (!code.Word(a, &w) || !code.Word(a + 4, &nxt)) break;
        if (!a64::IsMovz(w)) continue;
        const uint32_t rd = a64::MovzRd(w);
        uint32_t rt = 0, rn = 0, slot = 0;
        if (!a64::IsSturW(nxt, &rt, &rn, &slot)) continue;
        if (rt != rd || slot != kSlot) continue;
        const uint32_t n = a64::MovzImm(w);
        if ((n & 1u) != 0u || n < 2u || n > 32u) continue;
        if (hits < static_cast<int>(kMaxPhoneSites)) {
            siteVa[hits] = a;
            siteRd[hits] = rd;
            siteImm[hits] = n;
        }
        ++hits;
    }
    if (hits != 2) {
        SetWhy(out->why19, sizeof(out->why19),
               "手机 handler 里列数写入点（movz+stur #%u）= %d 处（预期 2）", kSlot, hits);
        return false;
    }
    if (siteImm[0] == siteImm[1]) {
        SetWhy(out->why19, sizeof(out->why19),
               "两处列数写入都是 %u（= %u 列），不是「官方两档」的形状，不敢动", siteImm[0],
               siteImm[0] / 2);
        return false;
    }

    out->phoneVa = va;
    for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
        out->phoneSiteVa[i] = siteVa[i];
        out->phoneSiteRd[i] = siteRd[i];
        out->phoneSiteImm[i] = siteImm[i];
    }
    out->why19[0] = '\0';
    out->ok19 = true;

    uint32_t found = 0;
    static const RowTarget rowTargets[kMaxPhoneRowSites] = {
            {"PhoneCellSizeHandler.calGridSizeByVariable", 16},
            {"PhoneDeviceRules._calCellCountYWithGridHeightAndLayoutType", 8},
    };
    for (uint32_t t = 0; t < kMaxPhoneRowSites; ++t) {
        uint32_t fva = 0, fsize = 0;
        if (!LocateFunction(code, rowTargets[t].sig, &fva, &fsize, out->why19r,
                            sizeof(out->why19r))) {
            continue;
        }
        uint32_t sdivAt = 0, sdivRd = 0;
        int sdivCount = 0;
        for (uint32_t a = fva; a + 4 <= fva + fsize; a += 4) {
            uint32_t w = 0;
            if (!code.Word(a, &w)) break;
            if (!a64::IsSdivX(w)) continue;
            ++sdivCount;
            if (sdivCount == 1) {
                sdivAt = a;
                sdivRd = a64::SdivRd(w);
            }
        }
        if (sdivCount != 1) {
            SetWhy(out->why19r, sizeof(out->why19r), "%s：sdiv 有 %d 条（预期 1）",
                   rowTargets[t].sig, sdivCount);
            continue;
        }
        uint32_t subAt = 0;
        bool have = false;
        const uint32_t winEnd = sdivAt + rowTargets[t].window * 4;
        uint32_t holders = 1u << sdivRd;
        for (uint32_t a = sdivAt + 4; a + 4 <= winEnd && a + 4 <= fva + fsize; a += 4) {
            uint32_t w = 0;
            if (!code.Word(a, &w)) break;
            uint32_t dst = 0, src = 0;
            if (a64::IsMovRegX(w, &dst, &src)) {
                if (((holders >> src) & 1u) != 0u) holders |= (1u << dst);
                continue;
            }
            if (!a64::IsSubRegX(w)) continue;
            if (((holders >> a64::SubRegXRn(w)) & 1u) == 0u) continue;
            subAt = a;
            have = true;
            break;
        }
        if (!have) {
            SetWhy(out->why19r, sizeof(out->why19r),
                   "%s：sdiv 后 %u 条内没有商减修正值的 sub", rowTargets[t].sig,
                   rowTargets[t].window);
            continue;
        }
        uint32_t subWord = 0;
        if (!code.Word(subAt, &subWord)) continue;
        out->phoneRowFuncVa[found] = fva;
        out->phoneRowSdivVa[found] = sdivAt;
        out->phoneRowSiteVa[found] = subAt;
        out->phoneRowRd[found] = a64::SubRegXRd(subWord);
        ++found;
    }
    if (found == kMaxPhoneRowSites) {
        out->why19r[0] = '\0';
        out->ok19r = true;
    } else if (found == 0) {
        SetWhy(out->why19r, sizeof(out->why19r), "两份行数拷贝都没找到落点");
    } else {
        SetWhy(out->why19r, sizeof(out->why19r), "只有一份行数拷贝找到落点（%u/2）", found);
    }
    return true;
}

bool LocateFeature20(const CodeView& code, LocatedSites* out) {
    constexpr uint32_t kSlot = 7;
    constexpr uint32_t kWin = 0x60;
    uint32_t va = 0, size = 0;
    if (!LocateFunction(code, "FoldCalculateHandler.handle", &va, &size, out->why20,
                        sizeof(out->why20))) {
        return false;
    }

    struct MovzRec {
        uint32_t at;
        uint32_t rd;
        uint32_t imm;
    };
    MovzRec movz[32]{};
    size_t movzCount = 0;
    for (uint32_t a = va; a + 4 <= va + size && movzCount < 32; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!a64::IsMovz(w)) continue;
        const uint32_t n = a64::MovzImm(w);
        if (n < 2u || n > 16u) continue;
        movz[movzCount++] = {a, a64::MovzRd(w), n};
    }

    uint32_t siteVa[kMaxFoldSites] = {0, 0};
    uint32_t siteRd[kMaxFoldSites] = {0, 0};
    uint32_t siteImm[kMaxFoldSites] = {0, 0};
    int hits = 0;
    for (uint32_t a = va; a + 4 <= va + size; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        uint32_t rt = 0, rn = 0, slot = 0;
        if (!a64::IsSturX(w, &rt, &rn, &slot)) continue;
        if (rn != 0u || slot != kSlot) continue;
        const MovzRec* pick = nullptr;
        for (size_t i = 0; i < movzCount; ++i) {
            if (movz[i].rd != rt) continue;
            if (movz[i].at >= a) continue;
            if (a - movz[i].at > kWin) continue;
            pick = &movz[i];
        }
        if (pick == nullptr) continue;
        if (hits < static_cast<int>(kMaxFoldSites)) {
            siteVa[hits] = pick->at;
            siteRd[hits] = pick->rd;
            siteImm[hits] = pick->imm;
        }
        ++hits;
    }
    if (hits != 2) {
        SetWhy(out->why20, sizeof(out->why20),
               "折叠屏 handler 里「装箱的行列常量」= %d 处（预期 2）", hits);
        return false;
    }
    if (siteImm[0] == siteImm[1]) {
        SetWhy(out->why20, sizeof(out->why20),
               "两个兜底常量相同（都是 %u），分不出长边/短边", siteImm[0]);
        return false;
    }

    const uint32_t targets[2] = {siteVa[0], siteVa[1]};
    uint32_t guardVa[kMaxFoldGuards]{};
    uint32_t guardTgt[kMaxFoldGuards]{};
    uint32_t guardCount = 0;
    for (uint32_t a = va; a + 4 <= va + size; a += 4) {
        uint32_t w = 0;
        if (!code.Word(a, &w)) break;
        if (!IsConditionalBranch(w)) continue;
        uint32_t t = 0;
        if (!a64::DecodeBranch(w, a, &t)) continue;
        if (t != targets[0] && t != targets[1]) continue;
        if (guardCount >= kMaxFoldGuards) {
            SetWhy(out->why20, sizeof(out->why20),
                   "落到兜底常量的条件分支超过 %u 条，形状超出预期", kMaxFoldGuards);
            return false;
        }
        guardVa[guardCount] = a;
        guardTgt[guardCount] = t;
        ++guardCount;
    }

    out->foldHva = va;
    for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
        out->foldSiteVa[i] = siteVa[i];
        out->foldSiteRd[i] = siteRd[i];
        out->foldSiteImm[i] = siteImm[i];
    }
    out->foldGuardCount = guardCount;
    for (uint32_t i = 0; i < guardCount; ++i) {
        out->foldGuardVa[i] = guardVa[i];
        out->foldGuardTgt[i] = guardTgt[i];
    }
    out->why20[0] = '\0';
    out->ok20 = true;
    return true;
}

struct IconTarget {
    const char* sym;
    uint32_t expect;
    const char* what;
};

constexpr IconTarget kIconTargets[kMaxIconFuncs] = {
        {"IconConfigHandler.calIconSize", 1, "手机/通用设备规则"},
        {"Q18CalculateHandler._calInnerPortrait", 1, "折叠屏·内屏竖屏"},
        {"Q18CalculateHandler._calInnerLandscape", 1, "折叠屏·内屏横屏"},
        {"PadCellSizeHandler.calculateIconSize", 3, "平板"},
};

const char* const kIconSiteWhat[kMaxIconFuncs] = {
        "桌面图标大小：手机/通用设备规则的倍率",
        "桌面图标大小：折叠屏内屏竖屏的倍率",
        "桌面图标大小：折叠屏内屏横屏的倍率",
        "桌面图标大小：平板的倍率",
};

bool IsIconScaleLoad(uint32_t w, uint32_t* dreg) {
    uint32_t rt = 0, rn = 0, off = 0;
    if (!a64::IsLdurD(w, &rt, &rn, &off)) return false;
    if (off != kIconScaleSlot) return false;
    if (dreg != nullptr) *dreg = rt;
    return true;
}

bool IconSiteShapeOk(const CodeView& code, uint32_t va, uint32_t dreg) {
    uint32_t w = 0;
    if (!code.Word(va, &w)) return false;
    uint32_t d = 0;
    if (IsIconScaleLoad(w, &d)) return d == dreg;
    if (a64::IsFmovDImm(w)) return a64::FmovDImmRd(w) == dreg;
    return false;
}

bool ValidateFeature21Shape(const CodeView& code, const LocatedSites& s) {
    if (!s.ok21) return true;
    if (s.iconSiteCount != kMaxIconSites) return false;
    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.iconFuncVa[f], &p0) || !code.Word(s.iconFuncVa[f] + 4, &p1)) {
            return false;
        }
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
        if (s.iconFuncSiteCount[f] == 0) return false;
        for (uint32_t i = 0; i < s.iconFuncSiteCount[f]; ++i) {
            const uint32_t idx = s.iconFuncSiteBeg[f] + i;
            if (idx >= s.iconSiteCount) return false;
            if (s.iconSiteVa[idx] < s.iconFuncVa[f] ||
                s.iconSiteVa[idx] + 4 > s.iconFuncVa[f] + kIconFuncMaxBytes) {
                return false;
            }
            if (!IconSiteShapeOk(code, s.iconSiteVa[idx], s.iconSiteRd[idx])) return false;
        }
    }
    return true;
}

bool LocateFeature21(const CodeView& code, LocatedSites* out) {
    uint32_t funcVa[kMaxIconFuncs]{};
    uint32_t siteBeg[kMaxIconFuncs]{};
    uint32_t siteCount[kMaxIconFuncs]{};
    uint32_t siteVa[kMaxIconSites]{};
    uint32_t siteRd[kMaxIconSites]{};
    uint32_t total = 0;

    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        uint32_t va = 0, size = 0;
        if (!LocateFunction(code, kIconTargets[f].sym, &va, &size, out->why21,
                            sizeof(out->why21))) {
            return false;
        }
        uint32_t found = 0;
        for (uint32_t a = va; a + 20u <= va + size; a += 4) {
            uint32_t w = 0;
            if (!code.Word(a, &w)) break;
            uint32_t d = 0;
            if (!IsIconScaleLoad(w, &d)) continue;
            bool used = false;
            for (uint32_t k = 1; k <= 8; ++k) {
                uint32_t w2 = 0;
                if (!code.Word(a + k * 4u, &w2)) break;
                uint32_t rd = 0, rn = 0, rm = 0;
                if (!a64::IsFmulD(w2, &rd, &rn, &rm)) continue;
                if (rn == d || rm == d) {
                    used = true;
                    break;
                }
            }
            if (!used) continue;
            if (total < kMaxIconSites) {
                siteVa[total] = a;
                siteRd[total] = d;
            }
            ++found;
            ++total;
        }
        if (found != kIconTargets[f].expect) {
            SetWhy(out->why21, sizeof(out->why21), "%s：图标倍率落点 %u 处（预期 %u）",
                   kIconTargets[f].what, found, kIconTargets[f].expect);
            return false;
        }
        funcVa[f] = va;
        siteBeg[f] = total - found;
        siteCount[f] = found;
    }
    if (total != kMaxIconSites) {
        SetWhy(out->why21, sizeof(out->why21), "图标倍率落点共 %u 处（预期 %u）", total,
               kMaxIconSites);
        return false;
    }

    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        out->iconFuncVa[f] = funcVa[f];
        out->iconFuncSiteBeg[f] = siteBeg[f];
        out->iconFuncSiteCount[f] = siteCount[f];
    }
    for (uint32_t i = 0; i < kMaxIconSites; ++i) {
        out->iconSiteVa[i] = siteVa[i];
        out->iconSiteRd[i] = siteRd[i];
    }
    out->iconSiteCount = kMaxIconSites;
    out->why21[0] = '\0';
    out->ok21 = true;
    return true;
}

void Rollback(PlanResult* out, size_t mark) {
    if (out->patches.size() > mark) {
        out->patches.erase(out->patches.begin() + static_cast<long>(mark),
                           out->patches.end());
    }
}

void DeriveFeature9(const CodeView& code, const LocatedSites& s, PlanResult* out) {
    if (!s.ok9) {
        memcpy(out->why9, s.why9, sizeof(out->why9));
        return;
    }
    const size_t mark = out->patches.size();

    for (uint32_t i = 0; i < s.noClearCount; ++i) {
        const uint32_t site = s.noClearSites[i];
        uint32_t cur = 0;
        if (!code.Word(site, &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == a64::kNop) continue;
        out->patches.push_back({site, cur, a64::kNop, "最近任务：不再插入清理按钮悬浮层"});
    }

    const uint32_t at = s.foldVa + 8;
    const uint32_t early[4] = {s.constWord, a64::MakeMovX15X29(), a64::MakeLdpFpLr(),
                               a64::kRet};
    for (uint32_t i = 0; i < 4; ++i) {
        uint32_t cur = 0;
        if (!code.Word(at + i * 4, &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == early[i]) continue;
        out->patches.push_back(
                {at + i * 4, cur, early[i], "折叠屏清理容器：函数头早返回常量空 widget"});
    }

    out->ok9 = true;
}

void DeriveFeature16(const CodeView& code, const Config& cfg, const LocatedSites& s,
                     PlanResult* out) {
    if (!s.ok16) {
        memcpy(out->why16, s.why16, sizeof(out->why16));
        return;
    }
    const size_t mark = out->patches.size();

    const uint32_t cols = cfg.folderCols;
    const uint32_t wantSmi = cols * 2;
    uint32_t wantMovz = 0;
    if (!a64::MakeMovz(s.movzRd, wantSmi, &wantMovz)) {
        SetWhy(out->why16, sizeof(out->why16), "列数 %u 超出 movz 立即数范围", cols);
        return;
    }

    uint32_t cur = 0;
    if (!code.Word(s.movzVa, &cur)) {
        Rollback(out, mark);
        return;
    }
    if (cur != wantMovz) {
        out->patches.push_back(
                {s.movzVa, cur, wantMovz, "文件夹列数字段：写入用户设定的每行应用数"});
    }

    if (s.gateVa != 0) {
        uint32_t gw = 0;
        if (!code.Word(s.gateVa, &gw)) {
            Rollback(out, mark);
            return;
        }        if (gw != a64::kNop && gw != 0) {
            uint32_t wantGate = 0;
            if (a64::IsBCond(gw) && s.movzVa > s.gateVa &&
                a64::MakeBCond(s.gateVa, s.movzVa, a64::BCondCond(gw), &wantGate)) {
                if (gw != wantGate) {
                    out->patches.push_back(
                            {s.gateVa, gw, wantGate,
                             "文件夹列数：设备闸门重定向到列数写入点（三端统一生效，不带平板初始化）"});
                }
            } else {
                out->patches.push_back(
                        {s.gateVa, gw, a64::kNop, "文件夹列数：移除设备闸门（三端统一生效）"});
            }
        }
    }

    if (cols != kFolderColsOfficial) {
        if (!s.ok16sq || s.squareSiteVa == 0 || s.squareCalleeVa == 0) {
            if (s.why16sq[0] != '\0') {
                memcpy(out->why16sq, s.why16sq, sizeof(out->why16sq));
            } else {
                SetWhy(out->why16sq, sizeof(out->why16sq),
                       "找不到「文件夹图标高度基准」（可选修正，不影响功能 16 本身）");
            }
        } else {
            uint32_t tall = 0, wide = 0;
            uint32_t rt = 0, rn = 0, tallImm = 0, wideImm = 0;
            const char* kWhat =
                    "文件夹图标：高度改用宽度基准（图标框保持正方形，不再被纵向拉伸）";
            if (!code.Word(s.squareSiteVa, &tall) || !code.Word(s.squareCalleeVa, &wide) ||
                !a64::IsLdurD(tall, &rt, &rn, &tallImm) ||
                !a64::IsLdurD(wide, nullptr, nullptr, &wideImm)) {
                SetWhy(out->why16sq, sizeof(out->why16sq),
                       "图标正方形修正：落点 %#x / %#x 形状对不上", s.squareSiteVa,
                       s.squareCalleeVa);
            } else {
                uint32_t want = 0;
                if (!a64::MakeLdurD(rt, rn, wideImm, &want)) {
                    SetWhy(out->why16sq, sizeof(out->why16sq),
                           "图标正方形修正：槽位 #%u 编码失败", wideImm);
                } else {
                    if (tall != want) {
                        out->patches.push_back({s.squareSiteVa, tall, want, kWhat});
                    }
                    out->ok16sq = true;
                    out->why16sq[0] = '\0';
                }
            }
        }
    }

    out->ok16 = true;
}

void DeriveFeature18(const CodeView& code, const LocatedSites& s, PlanResult* out) {
    if (!s.ok18) {
        memcpy(out->why18, s.why18, sizeof(out->why18));
        return;
    }
    const size_t mark = out->patches.size();
    uint32_t cur = 0, newWord = 0;

    if (!code.Word(s.bneAt, &cur)) {
        Rollback(out, mark);
        return;
    }
    if (!a64::MakeB(s.bneAt, s.bneTgt, &newWord)) {
        SetWhy(out->why18, sizeof(out->why18), "b.ne 改写目标超范围");
        return;
    }
    if (cur != newWord) {
        out->patches.push_back(
                {s.bneAt, cur, newWord, "syncVisibility：同步可见性改为无条件执行"});
    }

    if (!code.Word(s.tbAt, &cur)) {
        Rollback(out, mark);
        return;
    }
    if (!a64::MakeB(s.tbAt, s.tbTgt, &newWord)) {
        SetWhy(out->why18, sizeof(out->why18), "tbnz 改写目标超范围");
        return;
    }
    if (cur != newWord) {
        out->patches.push_back(
                {s.tbAt, cur, newWord, "syncVisibility：可见性切换恒走隐藏分支"});
    }

    if (!code.Word(s.initBlAt, &cur)) {
        Rollback(out, mark);
        return;
    }
    if (!a64::MakeBl(s.initBlAt, s.revTo, &newWord)) {
        SetWhy(out->why18, sizeof(out->why18), "initState bl 改写目标超范围");
        return;
    }
    if (cur != newWord) {
        out->patches.push_back(
                {s.initBlAt, cur, newWord, "initState：初始动画 forward → reverse"});
    }

    out->ok18 = true;
}

void NormalizePair(uint32_t lo, uint32_t hi, uint32_t* major, uint32_t* minor) {
    uint32_t a = *major;
    uint32_t b = *minor;
    if (a < lo) a = lo;
    if (a > hi) a = hi;
    if (b < lo) b = lo;
    if (b > hi) b = hi;
    if (a < b) {
        const uint32_t t = a;
        a = b;
        b = t;
    }
    *major = a;
    *minor = b;
}

void DeriveFeature4(const CodeView& code, const Config& cfg, const LocatedSites& s,
                    PlanResult* out) {
    if (!s.ok4) {
        memcpy(out->why4, s.why4, sizeof(out->why4));
        return;
    }
    const size_t mark = out->patches.size();

    uint32_t major = cfg.padMajor;
    uint32_t minor = cfg.padMinor;
    NormalizePair(kPadGridMin, kPadGridMax, &major, &minor);

    uint32_t oldMajor = s.padSiteImm[0];
    uint32_t oldMinor = s.padSiteImm[0];
    for (uint32_t i = 0; i < kMaxPadSites; ++i) {
        if (s.padSiteImm[i] > oldMajor) oldMajor = s.padSiteImm[i];
        if (s.padSiteImm[i] < oldMinor) oldMinor = s.padSiteImm[i];
    }
    if (oldMajor == oldMinor) {
        SetWhy(out->why4, sizeof(out->why4), "官方两组 movz 的立即数相同（%u），分不出长短边",
               oldMajor);
        return;
    }

    for (uint32_t i = 0; i < kMaxPadSites; ++i) {
        const uint32_t want = s.padSiteImm[i] == oldMajor ? major : minor;
        uint32_t word = 0;
        if (!a64::MakeMovz(s.padSiteRd[i], want, &word)) {
            SetWhy(out->why4, sizeof(out->why4), "格数 %u 超出 movz 立即数范围", want);
            Rollback(out, mark);
            return;
        }
        uint32_t cur = 0;
        if (!code.Word(s.padSiteVa[i], &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == word) continue;
        out->patches.push_back(
                {s.padSiteVa[i], cur, word,
                 s.padSiteImm[i] == oldMajor
                         ? "平板网格：长边（竖屏行数 / 横屏列数）"
                         : "平板网格：短边（竖屏列数 / 横屏行数）"});
    }

    out->ok4 = true;
}

void DeriveFeature19(const CodeView& code, const Config& cfg, const LocatedSites& s,
                     PlanResult* out) {
    if (!s.ok19) {
        memcpy(out->why19, s.why19, sizeof(out->why19));
        return;
    }
    const size_t mark = out->patches.size();

    uint32_t cols = cfg.phoneCols;
    if (cols < kPhoneColsMin) cols = kPhoneColsMin;
    if (cols > kPhoneColsMax) cols = kPhoneColsMax;
    const uint32_t wantSmi = cols * 2;

    for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
        uint32_t word = 0;
        if (!a64::MakeMovz(s.phoneSiteRd[i], wantSmi, &word)) {
            SetWhy(out->why19, sizeof(out->why19), "列数 %u 对应的 Smi %u 超出范围", cols,
                   wantSmi);
            Rollback(out, mark);
            return;
        }
        uint32_t cur = 0;
        if (!code.Word(s.phoneSiteVa[i], &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == word) continue;
        out->patches.push_back(
                {s.phoneSiteVa[i], cur, word, "手机桌面列数：统一成用户设定的列数"});
    }

    if (cfg.phoneRows >= kPhoneRowsMin) {
        if (!s.ok19r) {
            memcpy(out->why19r, s.why19r, sizeof(out->why19r));
        } else {
            uint32_t rows = cfg.phoneRows;
            if (rows > kPhoneRowsMax) rows = kPhoneRowsMax;
            for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
                uint32_t cur = 0;
                if (!code.Word(s.phoneRowSiteVa[i], &cur)) {
                    Rollback(out, mark);
                    return;
                }
                if (cur == a64::MakeMovzRaw(s.phoneRowRd[i], rows)) continue;
                if (a64::IsSubRegX(cur)) {
                    if (a64::SubRegXRd(cur) != s.phoneRowRd[i]) {
                        SetWhy(out->why19r, sizeof(out->why19r),
                               "行数落点 %u：sub 目的寄存器不匹配", i);
                        Rollback(out, mark);
                        return;
                    }
                } else if (a64::IsMovz(cur) && a64::MovzRd(cur) == s.phoneRowRd[i]) {
                    if (a64::MovzImm(cur) != rows) {
                        SetWhy(out->why19r, sizeof(out->why19r),
                               "行数落点 %u 已钉成 %u 行，与目标 %u 不符", i,
                               a64::MovzImm(cur), rows);
                        Rollback(out, mark);
                        return;
                    }
                    continue;
                } else {
                    SetWhy(out->why19r, sizeof(out->why19r),
                           "行数落点 %u：指令形状不符（既非 sub 也非已钉 movz）", i);
                    Rollback(out, mark);
                    return;
                }
                uint32_t word2 = a64::MakeMovzRaw(s.phoneRowRd[i], rows);
                out->patches.push_back(
                        {s.phoneRowSiteVa[i], cur, word2,
                         i == 0 ? "手机桌面行数：钉成用户设定的行数（活跃管线拷贝）"
                                : "手机桌面行数：钉成用户设定的行数（DeviceRules 链拷贝）"});
            }
            out->ok19r = true;
        }
    }

    out->ok19 = true;
}

void DeriveFeature20(const CodeView& code, const Config& cfg, const LocatedSites& s,
                     PlanResult* out) {
    if (!s.ok20) {
        memcpy(out->why20, s.why20, sizeof(out->why20));
        return;
    }
    const size_t mark = out->patches.size();

    uint32_t major = cfg.foldMajor;
    uint32_t minor = cfg.foldMinor;
    NormalizePair(kPadGridMin, kPadGridMax, &major, &minor);

    const uint32_t oldMajor =
            s.foldSiteImm[0] > s.foldSiteImm[1] ? s.foldSiteImm[0] : s.foldSiteImm[1];
    const uint32_t oldMinor =
            s.foldSiteImm[0] > s.foldSiteImm[1] ? s.foldSiteImm[1] : s.foldSiteImm[0];
    if (oldMajor == oldMinor) {
        SetWhy(out->why20, sizeof(out->why20), "官方两个兜底常量相同（%u），分不出长短边",
               oldMajor);
        return;
    }

    for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
        const uint32_t want = s.foldSiteImm[i] == oldMajor ? major : minor;
        uint32_t word = 0;
        if (!a64::MakeMovz(s.foldSiteRd[i], want, &word)) {
            SetWhy(out->why20, sizeof(out->why20), "格数 %u 超出 movz 立即数范围", want);
            Rollback(out, mark);
            return;
        }
        uint32_t cur = 0;
        if (!code.Word(s.foldSiteVa[i], &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == word) continue;
        out->patches.push_back(
                {s.foldSiteVa[i], cur, word,
                 s.foldSiteImm[i] == oldMajor ? "折叠屏网格：长边格数" : "折叠屏网格：短边格数"});
    }

    for (uint32_t i = 0; i < s.foldGuardCount; ++i) {
        uint32_t word = 0;
        if (!a64::MakeB(s.foldGuardVa[i], s.foldGuardTgt[i], &word)) {
            SetWhy(out->why20, sizeof(out->why20), "条件分支改写目标超范围（%#x → %#x）",
                   s.foldGuardVa[i], s.foldGuardTgt[i]);
            Rollback(out, mark);
            return;
        }
        uint32_t cur = 0;
        if (!code.Word(s.foldGuardVa[i], &cur)) {
            Rollback(out, mark);
            return;
        }
        if (cur == word) continue;
        out->patches.push_back(
                {s.foldGuardVa[i], cur, word, "折叠屏网格：不再让设备自带配置盖过写死的行列"});
    }

    out->ok20 = true;
}

const char* IconSiteWhatOf(const LocatedSites& s, uint32_t siteIndex) {
    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        const uint32_t beg = s.iconFuncSiteBeg[f];
        if (siteIndex >= beg && siteIndex < beg + s.iconFuncSiteCount[f]) {
            return kIconSiteWhat[f];
        }
    }
    return "桌面图标大小：倍率";
}

void DeriveFeature21(const CodeView& code, const Config& cfg, const LocatedSites& s,
                     PlanResult* out) {
    if (!s.ok21) {
        memcpy(out->why21, s.why21, sizeof(out->why21));
        return;
    }
    const size_t mark = out->patches.size();

    uint32_t codeOf = cfg.iconScaleCode;
    if (!IconScaleCodeValid(codeOf)) codeOf = kIconScaleCodeDefault;

    for (uint32_t i = 0; i < s.iconSiteCount; ++i) {
        uint32_t cur = 0;
        if (!code.Word(s.iconSiteVa[i], &cur)) {
            Rollback(out, mark);
            return;
        }
        const uint32_t rd = s.iconSiteRd[i];
        if (!IconSiteShapeOk(code, s.iconSiteVa[i], rd)) {
            SetWhy(out->why21, sizeof(out->why21),
                   "图标倍率落点 %u（%#x）的指令形状不符（既非 ldur d?,[x?,#%u] 也非已钉的 fmov）",
                   i, s.iconSiteVa[i], kIconScaleSlot);
            Rollback(out, mark);
            return;
        }
        uint32_t word = 0;
        if (!a64::MakeFmovDImm(rd, codeOf, &word)) {
            SetWhy(out->why21, sizeof(out->why21), "倍率码 %u 无法编码成 fmov 立即数", codeOf);
            Rollback(out, mark);
            return;
        }
        if (cur == word) continue;
        out->patches.push_back({s.iconSiteVa[i], cur, word, IconSiteWhatOf(s, i)});
    }

    out->ok21 = true;
}

void Dedup(std::vector<PlanPatch>* patches) {
    std::vector<PlanPatch> out;
    for (const PlanPatch& p : *patches) {
        bool replaced = false;
        for (PlanPatch& q : out) {
            if (q.va == p.va) {
                q = p;
                replaced = true;
                break;
            }
        }
        if (!replaced) out.push_back(p);
    }
    *patches = std::move(out);
}

struct ByteWriter {
    std::vector<uint8_t>* out;
    void U32(uint32_t v) {
        out->push_back(static_cast<uint8_t>(v & 0xFFu));
        out->push_back(static_cast<uint8_t>((v >> 8) & 0xFFu));
        out->push_back(static_cast<uint8_t>((v >> 16) & 0xFFu));
        out->push_back(static_cast<uint8_t>((v >> 24) & 0xFFu));
    }
};

struct ByteReader {
    const uint8_t* p = nullptr;
    size_t n = 0;
    size_t at = 0;
    bool U32(uint32_t* v) {
        if (at + 4 > n) return false;
        *v = static_cast<uint32_t>(p[at]) | (static_cast<uint32_t>(p[at + 1]) << 8) |
             (static_cast<uint32_t>(p[at + 2]) << 16) |
             (static_cast<uint32_t>(p[at + 3]) << 24);
        at += 4;
        return true;
    }
};

constexpr uint32_t kSitesPayloadVersion = 9;

}

uint32_t WantedFeatureMask(const Config& cfg) {
    if (!cfg.masterEnabled()) return 0;
    const bool want9 = cfg.Has(kFeatureNoClear);
    uint32_t mask = 0;
    if (want9) mask |= kWantNoClear;
    if (cfg.Has(kFeatureFolderCols)) mask |= kWantFolderCols;
    if (cfg.Has(kFeatureHideClear) && !want9) mask |= kWantHideClear;
    if (cfg.Has(kFeaturePadGrid)) mask |= kWantPadGrid;
    if (cfg.Has(kFeaturePhoneGrid)) mask |= kWantPhoneGrid;
    if (cfg.Has(kFeatureFoldGrid)) mask |= kWantFoldGrid;
    if (cfg.Has(kFeatureIconSize)) mask |= kWantIconSize;
    return mask;
}

void LocateSites(const CodeView& code, const Config& want, LocatedSites* out) {
    if (out == nullptr) return;
    const uint32_t mask = WantedFeatureMask(want);
    if ((mask & kWantNoClear) && !out->ok9) {
        out->noClearCount = 0;
        LocateFeature9(code, out);
    }
    if ((mask & kWantFolderCols) && !out->ok16) {
        LocateFeature16(code, out);
    } else if ((mask & kWantFolderCols) && !out->ok16sq) {
        LocateFeature16Square(code, out);
    }
    if ((mask & kWantHideClear) && !out->ok18) {
        LocateFeature18(code, out);
    }
    if ((mask & kWantPadGrid) && !out->ok4) {
        LocateFeature4(code, out);
    }
    if ((mask & kWantPhoneGrid) && !out->ok19) {
        LocateFeature19(code, out);
    }
    if ((mask & kWantFoldGrid) && !out->ok20) {
        LocateFeature20(code, out);
    }
    if ((mask & kWantIconSize) && !out->ok21) {
        LocateFeature21(code, out);
    }
}

namespace {

bool ValidateSitesImpl(const CodeView& code, LocatedSites& s) {
    if (s.ok9) {
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.foldVa, &p0) || !code.Word(s.foldVa + 4, &p1)) return false;
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
        if (s.noClearCount == 0 || s.noClearCount > kMaxNoClearSites) return false;
        for (uint32_t i = 0; i < s.noClearCount; ++i) {
            uint32_t w = 0;
            if (!code.Word(s.noClearSites[i], &w)) return false;
            if (w == a64::kNop) continue;
            uint32_t t = 0;
            if (!a64::IsBl(w) || !a64::DecodeBranch(w, s.noClearSites[i], &t) ||
                t != s.overlayVa) {
                return false;
            }
        }
    }
    if (s.ok16) {
        uint32_t mw = 0, sw = 0;
        if (!code.Word(s.movzVa, &mw) || !code.Word(s.storeVa, &sw)) return false;
        if (!a64::IsMovz(mw) || a64::MovzRd(mw) != s.movzRd) return false;
        const uint32_t imm = a64::MovzImm(mw);
        if (imm != kFolderColsOrigSmi) {
            if (imm < 2u || imm > 32u || (imm & 1u) != 0u) return false;
        }
        if (!a64::IsStrXUimm(sw, s.colsOffset != 0 ? s.colsOffset : kFolderColsOffset))
            return false;
        if (s.storeVa <= s.movzVa || s.storeVa - s.movzVa > 20u) return false;

        if (s.ok16sq) {
            uint32_t tallW = 0, wideW = 0;
            uint32_t rt = 0, rn = 0, tallImm = 0, wideImm = 0;
            const bool shaped =
                    s.squareFuncVa != 0 && s.squareSiteVa != 0 && s.squareCalleeVa != 0 &&
                    s.squareCalleeVa != s.squareSiteVa &&
                    s.squareSiteVa > s.squareFuncVa &&
                    s.squareCalleeVa > s.squareFuncVa &&
                    s.squareSiteVa - s.squareFuncVa < 0x800u &&
                    s.squareCalleeVa - s.squareFuncVa < 0x800u &&
                    code.Word(s.squareSiteVa, &tallW) && code.Word(s.squareCalleeVa, &wideW) &&
                    a64::IsLdurD(tallW, &rt, &rn, &tallImm) &&
                    a64::IsLdurD(wideW, nullptr, nullptr, &wideImm) && tallImm != wideImm;
            if (!shaped) {
                s.ok16sq = false;
                SetWhy(s.why16sq, sizeof(s.why16sq),
                       "站点里的「图标正方形」落点校验不通过（已跳过该修正）");
            }
        }
    }
    if (s.ok18) {
        const uint32_t svEnd = s.svVa + 0x800;
        if (s.bneAt < s.svVa || s.bneAt >= svEnd) return false;
        if (s.tbAt < s.svVa || s.tbAt >= svEnd) return false;
        if (s.bneTgt <= s.svVa || s.bneTgt > svEnd) return false;
        if (s.tbTgt <= s.svVa || s.tbTgt > svEnd) return false;
        uint32_t w = 0;
        if (!code.Word(s.initBlAt, &w) || !a64::IsBl(w)) return false;
    }
    if (s.ok4) {
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.padVa, &p0) || !code.Word(s.padVa + 4, &p1)) return false;
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
        for (uint32_t i = 0; i < kMaxPadSites; ++i) {
            if (s.padSiteVa[i] < s.padVa || s.padSiteVa[i] + 4 > s.padVa + 0x1000) return false;
            uint32_t w = 0;
            if (!code.Word(s.padSiteVa[i], &w)) return false;
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.padSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if (imm < kPadGridMin || imm > kPadGridMax) return false;
        }
    }
    if (s.ok19) {
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.phoneVa, &p0) || !code.Word(s.phoneVa + 4, &p1)) return false;
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
        for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
            if (s.phoneSiteVa[i] < s.phoneVa || s.phoneSiteVa[i] + 8 > s.phoneVa + 0x1000) return false;
            uint32_t w = 0, nxt = 0;
            if (!code.Word(s.phoneSiteVa[i], &w) ||
                !code.Word(s.phoneSiteVa[i] + 4, &nxt)) return false;
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.phoneSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if ((imm & 1u) != 0u || imm < 2u || imm > 32u) return false;
            uint32_t rt = 0, rn = 0, slot = 0;
            if (!a64::IsSturW(nxt, &rt, &rn, &slot)) return false;
            if (rt != s.phoneSiteRd[i] || slot != kCellCountXOffset) return false;
        }
        if (s.ok19r) {
            for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
                uint32_t w = 0;
                if (!code.Word(s.phoneRowSdivVa[i], &w) || !a64::IsSdivX(w)) { s.ok19r = false; break; }
                if (!code.Word(s.phoneRowSiteVa[i], &w)) { s.ok19r = false; break; }
                if (a64::IsSubRegX(w)) {
                    if (a64::SubRegXRd(w) != s.phoneRowRd[i]) { s.ok19r = false; break; }
                } else if (a64::IsMovz(w)) {
                    if (a64::MovzRd(w) != s.phoneRowRd[i] || a64::MovzImm(w) < 2u ||
                        a64::MovzImm(w) > kPhoneRowsMax) { s.ok19r = false; break; }
                } else { s.ok19r = false; break; }
            }
        }
    }
    if (s.ok20) {
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.foldHva, &p0) || !code.Word(s.foldHva + 4, &p1)) return false;
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
        for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
            if (s.foldSiteVa[i] < s.foldHva || s.foldSiteVa[i] + 4 > s.foldHva + 0x1000) return false;
            uint32_t w = 0;
            if (!code.Word(s.foldSiteVa[i], &w)) return false;
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.foldSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if (imm < kPadGridMin || imm > kPadGridMax) return false;
        }
        for (uint32_t i = 0; i < s.foldGuardCount; ++i) {
            if (s.foldGuardVa[i] < s.foldHva || s.foldGuardVa[i] + 4 > s.foldHva + 0x1000) return false;
            uint32_t w = 0;
            if (!code.Word(s.foldGuardVa[i], &w)) return false;
            if (!IsConditionalBranch(w)) return false;
            uint32_t t = 0;
            if (!a64::DecodeBranch(w, s.foldGuardVa[i], &t)) return false;
            if (t != s.foldGuardTgt[i]) return false;
            if (t != s.foldSiteVa[0] && t != s.foldSiteVa[1]) return false;
        }
    }
    if (!ValidateFeature21Shape(code, s)) return false;
    return true;
}

}

bool ValidateSitesShape(const CodeView& code, LocatedSites& s) {
    return ValidateSitesImpl(code, s);
}

bool LoadSitePack(const char* path, LocatedSites* out) {
    if (path == nullptr || out == nullptr) return false;
    std::vector<uint8_t> data;
    if (!ReadWholeFile(path, &data, nullptr)) return false;
    if (data.size() < 48) return false;
    if (memcmp(data.data(), "HTSP", 4) != 0) return false;
    const uint32_t ver = data[4] | (uint32_t(data[5]) << 8) |
                         (uint32_t(data[6]) << 16) | (uint32_t(data[7]) << 24);
    if (ver != 1) return false;
    const uint32_t payloadLen = data[40] | (uint32_t(data[41]) << 8) |
                                (uint32_t(data[42]) << 16) | (uint32_t(data[43]) << 24);
    if (payloadLen < 8 || payloadLen > 4096 || 44 + payloadLen > data.size()) return false;
    return ParseSites(data.data() + 44, payloadLen, out);
}

bool ValidateSites(const CodeView& code, LocatedSites& s) {    if (!s.AnyOk()) return false;

    if (s.ok9) {
        const FunctionSignature* ov = FindSignatureByName("_insertClearButtonOverlay");
        const FunctionSignature* fd = FindSignatureByName("_buildFoldDockClearContainer");
        if (ov == nullptr || fd == nullptr) return false;
        if (!code.MatchSignatureAt(*ov, s.overlayVa)) return false;
        if (!code.MatchSignatureAt(*fd, s.foldVa)) return false;
        if (s.noClearCount == 0 || s.noClearCount > kMaxNoClearSites) return false;
        for (uint32_t i = 0; i < s.noClearCount; ++i) {
            const uint32_t site = s.noClearSites[i];
            uint32_t w = 0;
            if (!code.Word(site, &w)) return false;
            if (w == a64::kNop) continue;
            uint32_t t = 0;
            if (!a64::IsBl(w) || !a64::DecodeBranch(w, site, &t) || t != s.overlayVa) {
                return false;
            }
        }
        uint32_t p0 = 0, p1 = 0;
        if (!code.Word(s.foldVa, &p0) || !code.Word(s.foldVa + 4, &p1)) return false;
        if (!a64::IsStpFpLrPre(p0) || !a64::IsMovX29X15(p1)) return false;
    }

    if (s.ok16) {
        uint32_t mw = 0, sw = 0;
        if (!code.Word(s.movzVa, &mw) || !code.Word(s.storeVa, &sw)) return false;
        if (!a64::IsMovz(mw) || a64::MovzRd(mw) != s.movzRd) return false;
        const uint32_t imm = a64::MovzImm(mw);
        if (imm != kFolderColsOrigSmi) {
            if (imm < 2u || imm > 32u || (imm & 1u) != 0u) return false;
        }
        if (!a64::IsStrXUimm(sw, s.colsOffset != 0 ? s.colsOffset : kFolderColsOffset))
            return false;
        if (s.storeVa <= s.movzVa || s.storeVa - s.movzVa > 20u) return false;
        if (s.gateVa != 0) {
            uint32_t gw = 0;
            if (!code.Word(s.gateVa, &gw)) return false;
            if (gw != a64::kNop) {
                uint32_t t = 0;
                if (!a64::IsBCond(gw) || !a64::DecodeBranch(gw, s.gateVa, &t) ||
                    t != s.storeVa + 4) {
                    return false;
                }
            }
        }
    }

    if (s.ok18) {
        const FunctionSignature* sv = FindSignatureByName("_syncVisibility");
        const FunctionSignature* ini = FindSignatureByName("initState");
        if (sv == nullptr || ini == nullptr) return false;
        if (!code.MatchSignatureAt(*sv, s.svVa)) return false;
        if (!code.MatchSignatureAt(*ini, s.initVa)) return false;
        const uint32_t svEnd = s.svVa + sv->wordCount * 4;
        if (s.bneAt < s.svVa || s.bneAt >= svEnd) return false;
        if (s.tbAt < s.svVa || s.tbAt >= svEnd) return false;
        if (s.bneTgt <= s.svVa || s.bneTgt > svEnd) return false;
        if (s.tbTgt <= s.svVa || s.tbTgt > svEnd) return false;
        uint32_t w = 0;
        if (!code.Word(s.initBlAt, &w) || !a64::IsBl(w)) return false;
    }

    if (s.ok4) {
        const FunctionSignature* sig = FindSignatureByName("PadRowColHandler.handle");
        if (sig == nullptr || !code.MatchSignatureAt(*sig, s.padVa)) return false;
        const uint32_t end = s.padVa + sig->wordCount * 4;
        for (uint32_t i = 0; i < kMaxPadSites; ++i) {
            if (s.padSiteVa[i] < s.padVa || s.padSiteVa[i] + 4 > end) return false;
            uint32_t w = 0;
            if (!code.Word(s.padSiteVa[i], &w)) return false;
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.padSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if (imm < kPadGridMin || imm > kPadGridMax) return false;
        }
    }

    if (s.ok19) {
        const FunctionSignature* sig = FindSignatureByName("PhoneRowColHandler.handle");
        if (sig == nullptr || !code.MatchSignatureAt(*sig, s.phoneVa)) return false;
        const uint32_t end = s.phoneVa + sig->wordCount * 4;
        for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
            if (s.phoneSiteVa[i] < s.phoneVa || s.phoneSiteVa[i] + 8 > end) return false;
            uint32_t w = 0, nxt = 0;
            if (!code.Word(s.phoneSiteVa[i], &w) ||
                !code.Word(s.phoneSiteVa[i] + 4, &nxt)) {
                return false;
            }
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.phoneSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if ((imm & 1u) != 0u || imm < 2u || imm > 32u) return false;
            uint32_t rt = 0, rn = 0, slot = 0;
            if (!a64::IsSturW(nxt, &rt, &rn, &slot)) return false;
            if (rt != s.phoneSiteRd[i]) return false;
            if (slot != kCellCountXOffset) return false;
        }
        if (s.ok19r) {
            for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
                uint32_t w = 0;
                if (!code.Word(s.phoneRowSdivVa[i], &w) || !a64::IsSdivX(w)) {
                    s.ok19r = false;
                    break;
                }
                if (!code.Word(s.phoneRowSiteVa[i], &w)) {
                    s.ok19r = false;
                    break;
                }
                if (a64::IsSubRegX(w)) {
                    if (a64::SubRegXRd(w) != s.phoneRowRd[i]) { s.ok19r = false; break; }
                } else if (a64::IsMovz(w)) {
                    if (a64::MovzRd(w) != s.phoneRowRd[i] || a64::MovzImm(w) < 2u ||
                        a64::MovzImm(w) > kPhoneRowsMax) {
                        s.ok19r = false;
                        break;
                    }
                } else {
                    s.ok19r = false;
                    break;
                }
            }
        }
    }

    if (s.ok20) {
        const FunctionSignature* sig = FindSignatureByName("FoldCalculateHandler.handle");
        if (sig == nullptr || !code.MatchSignatureAt(*sig, s.foldHva)) return false;
        const uint32_t end = s.foldHva + sig->wordCount * 4;
        if (s.foldGuardCount > kMaxFoldGuards) return false;
        for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
            if (s.foldSiteVa[i] < s.foldHva || s.foldSiteVa[i] + 4 > end) return false;
            uint32_t w = 0;
            if (!code.Word(s.foldSiteVa[i], &w)) return false;
            if (!a64::IsMovz(w) || a64::MovzRd(w) != s.foldSiteRd[i]) return false;
            const uint32_t imm = a64::MovzImm(w);
            if (imm < kPadGridMin || imm > kPadGridMax) return false;
        }
        for (uint32_t i = 0; i < s.foldGuardCount; ++i) {
            if (s.foldGuardVa[i] < s.foldHva || s.foldGuardVa[i] + 4 > end) return false;
            uint32_t w = 0;
            if (!code.Word(s.foldGuardVa[i], &w)) return false;
            if (!IsConditionalBranch(w)) return false;
            uint32_t t = 0;
            if (!a64::DecodeBranch(w, s.foldGuardVa[i], &t)) return false;
            if (t != s.foldGuardTgt[i]) return false;
            if (t != s.foldSiteVa[0] && t != s.foldSiteVa[1]) return false;
        }
    }

    if (!ValidateFeature21Shape(code, s)) return false;

    return true;
}

void DerivePatches(const CodeView& code, const Config& cfg, const LocatedSites& sites,
                   PlanResult* out) {
    if (out == nullptr) return;
    *out = PlanResult{};
    if (!cfg.masterEnabled()) return;

    const uint32_t mask = WantedFeatureMask(cfg);
    if (mask & kWantNoClear) DeriveFeature9(code, sites, out);
    if (mask & kWantFolderCols) DeriveFeature16(code, cfg, sites, out);
    if (mask & kWantHideClear) DeriveFeature18(code, sites, out);
    if (mask & kWantPadGrid) DeriveFeature4(code, cfg, sites, out);
    if (mask & kWantPhoneGrid) DeriveFeature19(code, cfg, sites, out);
    if (mask & kWantFoldGrid) DeriveFeature20(code, cfg, sites, out);
    if (mask & kWantIconSize) DeriveFeature21(code, cfg, sites, out);
    Dedup(&out->patches);
}

bool FeatureOk(const LocatedSites& s, uint32_t num) {
    switch (num) {
        case kFeatureNoClear: return s.ok9;
        case kFeatureFolderCols: return s.ok16;
        case kFeatureHideClear: return s.ok18;
        case kFeaturePadGrid: return s.ok4;
        case kFeaturePhoneGrid: return s.ok19;
        case kFeatureFoldGrid: return s.ok20;
        case kFeatureIconSize: return s.ok21;
        default: return false;
    }
}

void KeepOnlyFeature(LocatedSites* s, uint32_t num) {
    if (s == nullptr) return;
    s->ok9 = num == kFeatureNoClear;
    s->ok16 = num == kFeatureFolderCols;
    s->ok18 = num == kFeatureHideClear;
    s->ok4 = num == kFeaturePadGrid;
    s->ok19 = num == kFeaturePhoneGrid;
    s->ok19r = num == kFeaturePhoneGrid;
    s->ok20 = num == kFeatureFoldGrid;
    s->ok21 = num == kFeatureIconSize;
}

void CopyFeature(LocatedSites* dst, const LocatedSites& src, uint32_t num) {
    if (dst == nullptr || !FeatureOk(src, num)) return;
    switch (num) {
        case kFeatureNoClear:
            dst->ok9 = true;
            dst->overlayVa = src.overlayVa;
            dst->foldVa = src.foldVa;
            dst->constWord = src.constWord;
            dst->noClearCount = src.noClearCount;
            for (uint32_t i = 0; i < src.noClearCount && i < kMaxNoClearSites; ++i) {
                dst->noClearSites[i] = src.noClearSites[i];
            }
            break;
        case kFeatureFolderCols:
            dst->ok16 = true;
            dst->movzVa = src.movzVa;
            dst->movzRd = src.movzRd;
            dst->storeVa = src.storeVa;
            dst->gateVa = src.gateVa;
            dst->colsOffset = src.colsOffset;
            dst->ok16sq = src.ok16sq;
            dst->squareFuncVa = src.squareFuncVa;
            dst->squareSiteVa = src.squareSiteVa;
            dst->squareCalleeVa = src.squareCalleeVa;
            break;
        case kFeatureHideClear:
            dst->ok18 = true;
            dst->svVa = src.svVa;
            dst->initVa = src.initVa;
            dst->bneAt = src.bneAt;
            dst->bneTgt = src.bneTgt;
            dst->tbAt = src.tbAt;
            dst->tbTgt = src.tbTgt;
            dst->initBlAt = src.initBlAt;
            dst->revTo = src.revTo;
            break;
        case kFeaturePadGrid:
            dst->ok4 = true;
            dst->padVa = src.padVa;
            for (uint32_t i = 0; i < kMaxPadSites; ++i) {
                dst->padSiteVa[i] = src.padSiteVa[i];
                dst->padSiteRd[i] = src.padSiteRd[i];
                dst->padSiteImm[i] = src.padSiteImm[i];
            }
            break;
        case kFeaturePhoneGrid:
            dst->ok19 = true;
            dst->phoneVa = src.phoneVa;
            for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
                dst->phoneSiteVa[i] = src.phoneSiteVa[i];
                dst->phoneSiteRd[i] = src.phoneSiteRd[i];
                dst->phoneSiteImm[i] = src.phoneSiteImm[i];
            }
            dst->ok19r = src.ok19r;
            for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
                dst->phoneRowFuncVa[i] = src.phoneRowFuncVa[i];
                dst->phoneRowSdivVa[i] = src.phoneRowSdivVa[i];
                dst->phoneRowSiteVa[i] = src.phoneRowSiteVa[i];
                dst->phoneRowRd[i] = src.phoneRowRd[i];
            }
            break;
        case kFeatureFoldGrid:
            dst->ok20 = true;
            dst->foldHva = src.foldHva;
            for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
                dst->foldSiteVa[i] = src.foldSiteVa[i];
                dst->foldSiteRd[i] = src.foldSiteRd[i];
                dst->foldSiteImm[i] = src.foldSiteImm[i];
            }
            dst->foldGuardCount = src.foldGuardCount;
            for (uint32_t i = 0; i < src.foldGuardCount && i < kMaxFoldGuards; ++i) {
                dst->foldGuardVa[i] = src.foldGuardVa[i];
                dst->foldGuardTgt[i] = src.foldGuardTgt[i];
            }
            break;
        case kFeatureIconSize:
            dst->ok21 = true;
            for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
                dst->iconFuncVa[f] = src.iconFuncVa[f];
                dst->iconFuncSiteBeg[f] = src.iconFuncSiteBeg[f];
                dst->iconFuncSiteCount[f] = src.iconFuncSiteCount[f];
            }
            dst->iconSiteCount = src.iconSiteCount;
            for (uint32_t i = 0; i < kMaxIconSites; ++i) {
                dst->iconSiteVa[i] = src.iconSiteVa[i];
                dst->iconSiteRd[i] = src.iconSiteRd[i];
            }
            break;
        default: break;
    }
}

bool SerializeSites(const LocatedSites& s, std::vector<uint8_t>* out) {
    if (out == nullptr) return false;
    ByteWriter w{out};
    out->clear();
    w.U32(kSitesPayloadVersion);
    uint32_t flags = 0;
    if (s.ok9) flags |= kWantNoClear;
    if (s.ok16) flags |= kWantFolderCols;
    if (s.ok18) flags |= kWantHideClear;
    if (s.ok4) flags |= kWantPadGrid;
    if (s.ok19) flags |= kWantPhoneGrid;
    if (s.ok20) flags |= kWantFoldGrid;
    if (s.ok21) flags |= kWantIconSize;
    w.U32(flags);
    w.U32(s.overlayVa);
    w.U32(s.foldVa);
    w.U32(s.constWord);
    const uint32_t count = s.noClearCount > kMaxNoClearSites ? kMaxNoClearSites : s.noClearCount;
    w.U32(count);
    for (uint32_t i = 0; i < count; ++i) w.U32(s.noClearSites[i]);
    w.U32(s.movzVa);
    w.U32(s.movzRd);
    w.U32(s.storeVa);
    w.U32(s.gateVa);
    w.U32(s.colsOffset);
    w.U32(s.svVa);
    w.U32(s.initVa);
    w.U32(s.bneAt);
    w.U32(s.bneTgt);
    w.U32(s.tbAt);
    w.U32(s.tbTgt);
    w.U32(s.initBlAt);
    w.U32(s.revTo);

    w.U32(s.padVa);
    for (uint32_t i = 0; i < kMaxPadSites; ++i) {
        w.U32(s.padSiteVa[i]);
        w.U32(s.padSiteRd[i]);
        w.U32(s.padSiteImm[i]);
    }
    w.U32(s.phoneVa);
    for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
        w.U32(s.phoneSiteVa[i]);
        w.U32(s.phoneSiteRd[i]);
        w.U32(s.phoneSiteImm[i]);
    }
    w.U32(s.foldHva);
    for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
        w.U32(s.foldSiteVa[i]);
        w.U32(s.foldSiteRd[i]);
        w.U32(s.foldSiteImm[i]);
    }
    const uint32_t guards =
            s.foldGuardCount > kMaxFoldGuards ? kMaxFoldGuards : s.foldGuardCount;
    w.U32(guards);
    for (uint32_t i = 0; i < guards; ++i) {
        w.U32(s.foldGuardVa[i]);
        w.U32(s.foldGuardTgt[i]);
    }
    w.U32(s.ok19r ? 1u : 0u);
    for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
        w.U32(s.phoneRowFuncVa[i]);
        w.U32(s.phoneRowSdivVa[i]);
        w.U32(s.phoneRowSiteVa[i]);
        w.U32(s.phoneRowRd[i]);
    }

    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        w.U32(s.iconFuncVa[f]);
        w.U32(s.iconFuncSiteBeg[f]);
        w.U32(s.iconFuncSiteCount[f]);
    }
    w.U32(s.iconSiteCount);
    for (uint32_t i = 0; i < kMaxIconSites; ++i) {
        w.U32(s.iconSiteVa[i]);
        w.U32(s.iconSiteRd[i]);
    }

    w.U32(s.ok16sq ? 1u : 0u);
    w.U32(s.squareFuncVa);
    w.U32(s.squareSiteVa);
    w.U32(s.squareCalleeVa);

    return true;
}

bool ParseSites(const uint8_t* data, size_t size, LocatedSites* out) {
    if (data == nullptr || out == nullptr) return false;
    ByteReader r{data, size, 0};
    uint32_t version = 0;
    uint32_t flags = 0;
    uint32_t count = 0;
    if (!r.U32(&version) || version != kSitesPayloadVersion) return false;
    if (!r.U32(&flags)) return false;
    const uint32_t kAllBits = kWantNoClear | kWantFolderCols | kWantHideClear | kWantPadGrid |
                              kWantPhoneGrid | kWantFoldGrid | kWantIconSize;
    if (flags == 0 || (flags & ~kAllBits) != 0) return false;

    LocatedSites s;
    s.ok9 = (flags & kWantNoClear) != 0;
    s.ok16 = (flags & kWantFolderCols) != 0;
    s.ok18 = (flags & kWantHideClear) != 0;
    s.ok4 = (flags & kWantPadGrid) != 0;
    s.ok19 = (flags & kWantPhoneGrid) != 0;
    s.ok20 = (flags & kWantFoldGrid) != 0;
    s.ok21 = (flags & kWantIconSize) != 0;
    if (!r.U32(&s.overlayVa) || !r.U32(&s.foldVa) || !r.U32(&s.constWord)) return false;
    if (!r.U32(&count)) return false;
    if (count > kMaxNoClearSites) return false;
    for (uint32_t i = 0; i < count; ++i) {
        if (!r.U32(&s.noClearSites[i])) return false;
    }
    s.noClearCount = count;
    if (!r.U32(&s.movzVa) || !r.U32(&s.movzRd) || !r.U32(&s.storeVa) || !r.U32(&s.gateVa)) {
        return false;
    }
    if (!r.U32(&s.colsOffset)) return false;
    if (!r.U32(&s.svVa) || !r.U32(&s.initVa)) return false;
    if (!r.U32(&s.bneAt) || !r.U32(&s.bneTgt) || !r.U32(&s.tbAt) || !r.U32(&s.tbTgt)) {
        return false;
    }
    if (!r.U32(&s.initBlAt) || !r.U32(&s.revTo)) return false;

    if (!r.U32(&s.padVa)) return false;
    for (uint32_t i = 0; i < kMaxPadSites; ++i) {
        if (!r.U32(&s.padSiteVa[i]) || !r.U32(&s.padSiteRd[i]) || !r.U32(&s.padSiteImm[i])) {
            return false;
        }
    }
    if (!r.U32(&s.phoneVa)) return false;
    for (uint32_t i = 0; i < kMaxPhoneSites; ++i) {
        if (!r.U32(&s.phoneSiteVa[i]) || !r.U32(&s.phoneSiteRd[i]) ||
            !r.U32(&s.phoneSiteImm[i])) {
            return false;
        }
    }
    if (!r.U32(&s.foldHva)) return false;
    for (uint32_t i = 0; i < kMaxFoldSites; ++i) {
        if (!r.U32(&s.foldSiteVa[i]) || !r.U32(&s.foldSiteRd[i]) ||
            !r.U32(&s.foldSiteImm[i])) {
            return false;
        }
    }
    uint32_t guards = 0;
    if (!r.U32(&guards) || guards > kMaxFoldGuards) return false;
    for (uint32_t i = 0; i < guards; ++i) {
        if (!r.U32(&s.foldGuardVa[i]) || !r.U32(&s.foldGuardTgt[i])) return false;
    }
    s.foldGuardCount = guards;

    uint32_t rowsFlag = 0;
    if (!r.U32(&rowsFlag)) return false;
    s.ok19r = rowsFlag != 0;
    for (uint32_t i = 0; i < kMaxPhoneRowSites; ++i) {
        if (!r.U32(&s.phoneRowFuncVa[i]) || !r.U32(&s.phoneRowSdivVa[i]) ||
            !r.U32(&s.phoneRowSiteVa[i]) || !r.U32(&s.phoneRowRd[i])) {
            return false;
        }
    }

    for (uint32_t f = 0; f < kMaxIconFuncs; ++f) {
        if (!r.U32(&s.iconFuncVa[f]) || !r.U32(&s.iconFuncSiteBeg[f]) ||
            !r.U32(&s.iconFuncSiteCount[f])) {
            return false;
        }
        if (s.iconFuncSiteCount[f] > kMaxIconSites) return false;
        if (s.iconFuncSiteBeg[f] > kMaxIconSites) return false;
    }
    if (!r.U32(&s.iconSiteCount) || s.iconSiteCount > kMaxIconSites) return false;
    for (uint32_t i = 0; i < kMaxIconSites; ++i) {
        if (!r.U32(&s.iconSiteVa[i]) || !r.U32(&s.iconSiteRd[i])) return false;
    }

    uint32_t squareFlag = 0;
    if (!r.U32(&squareFlag) || !r.U32(&s.squareFuncVa) || !r.U32(&s.squareSiteVa) ||
        !r.U32(&s.squareCalleeVa)) {
        return false;
    }
    s.ok16sq = squareFlag != 0;

    *out = s;
    return true;
}

bool BuildPatchPlan(const CodeView& code, const Config& cfg, PlanResult* out) {    if (out == nullptr) return false;
    LocatedSites sites;
    LocateSites(code, cfg, &sites);
    DerivePatches(code, cfg, sites, out);
    return !out->patches.empty();
}

void SetForceSymbolOnlyForTest(bool on) { gForceSymbolOnly = on; }

}
