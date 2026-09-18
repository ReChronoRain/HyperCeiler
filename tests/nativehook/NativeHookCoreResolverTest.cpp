/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the NativeHookRuntime core resolver primitives
 * (nativehook/arm64_decode.h) and a generic, non-Dart semantic resolver
 * fixture proving the core does not depend on Dart/MiuiHome: the fixture
 * resolves "the function that calls import X then import Y in that order"
 * over synthetic code through a PLT/GOT indirection, with the full
 * 0/1/many-candidate fail-closed gate.
 */
#include "../../app/src/main/cpp/nativehook/arm64_decode.h"
#include "../../app/src/main/cpp/nativehook/resolver.h"

#include <cstdio>

#include <optional>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

namespace {

constexpr uint32_t kRet = 0xd65f03c0;

uint32_t bl(int32_t instruction_offset_words, int32_t target_offset_words) {
    const int32_t delta = target_offset_words - instruction_offset_words;
    return 0x94000000U | static_cast<uint32_t>(delta & 0x03ffffff);
}

uint32_t adrp(uint32_t reg, int32_t page_delta) {
    // immhi:immlo encodes page_delta (21-bit signed).
    uint32_t word = 0x90000000U | reg;
    const uint32_t imm = static_cast<uint32_t>(page_delta & 0x1fffff);
    word |= ((imm >> 2) & 0x7ffff) << 5; // immhi
    word |= (imm & 0x3) << 29;           // immlo
    return word;
}

uint32_t add_imm(uint32_t reg, uint32_t value) {
    return 0x91000000U | ((value & 0xfff) << 10) | (reg << 5) | reg;
}

uint32_t ldr64(uint32_t reg, uint32_t base, uint32_t offset8) {
    return 0xf9400000U | ((offset8 / 8) << 10) | (base << 5) | reg;
}

struct ImportCallFixture {
    // One executable range; the PLT stubs live at the end of the image.
    std::vector<uint32_t> words;
    uintptr_t base = 0x100000;
    uint64_t plt_stub_rva = 0;  // RVA of the first PLT stub.
    uint64_t got_madvise_rva = 0x2000;
    uint64_t got_memcpy_rva = 0x2008;

    // Assemble: caller prologue, bl plt_madvise, bl plt_memcpy, ret; PLT stubs.
    void assemble() {
        words.clear();
        // Function A (offset 0): calls madvise then memcpy in order.
        words.push_back(0xa9bf7bfd);            // stp x29, x30, [sp, #-32]!
        words.push_back(bl(1, 17));             // bl plt_madvise (word 18)
        words.push_back(0xd503201f);            // nop
        words.push_back(bl(3, 21));             // bl plt_memcpy (word 22)
        words.push_back(kRet);
        // Function B (offset 5): calls memcpy first, then madvise (wrong order).
        words.push_back(0xa9bf7bfd);
        words.push_back(bl(6, 21));
        words.push_back(bl(7, 17));
        words.push_back(kRet);
        // Function C (offset 9): calls madvise twice (no memcpy).
        words.push_back(0xa9bf7bfd);
        words.push_back(bl(10, 17));
        words.push_back(bl(11, 17));
        words.push_back(kRet);
        // Function D (offset 13): calls madvise then memcpy in order - ambiguous
        // twin of A, but without the NOP between the calls.
        words.push_back(0xa9bf7bfd);
        words.push_back(bl(14, 17));
        words.push_back(bl(15, 21));
        words.push_back(kRet);
        // PLT stub for madvise: ADRP x16; LDR x17,[x16,imm]; ADD x16,x16,imm; BR x17.
        plt_stub_rva = static_cast<uint64_t>(words.size()) * 4; // Where it actually lands.
        words.push_back(adrp(16, static_cast<int32_t>((got_madvise_rva >> 12)
            - static_cast<int32_t>((base + plt_stub_rva) >> 12))));
        words.push_back(ldr64(17, 16, got_madvise_rva & 0xff8));
        words.push_back(add_imm(16, 0));
        words.push_back(0xd61f0220);            // BR x17.
        // PLT stub for memcpy, again anchored to its real position.
        const uint64_t memcpy_stub_rva = static_cast<uint64_t>(words.size()) * 4;
        words.push_back(adrp(16, static_cast<int32_t>((got_memcpy_rva >> 12)
            - static_cast<int32_t>((base + memcpy_stub_rva) >> 12))));
        words.push_back(ldr64(17, 16, got_memcpy_rva & 0xff8));
        words.push_back(add_imm(16, 0));
        words.push_back(0xd61f0220);
    }

    nhk::arm64::CodeRange range() const {
        return {base, std::span<const uint32_t>(words.data(), words.size())};
    }
};

/**
 * Generic semantic resolver: unique function that calls `first_import` then
 * `second_import` in program order, each through a PLT stub resolving to the
 * named GOT slot. Returns the function's Match or nothing; multiple
 * candidates fail closed.
 */
std::optional<nhk::arm64::Match> find_ordered_import_caller(
    const nhk::arm64::CodeRange &code, uint64_t got_first, uint64_t got_second,
    bool require_nop_between, nhk::ResolverEvidence *evidence_out) {
    std::optional<nhk::arm64::Match> found;
    size_t candidates = 0;
    for (size_t start = 0; start < code.words.size();) {
        // Walk one function (ret-terminated).
        size_t end = start;
        while (end < code.words.size() && !nhk::arm64::is_ret(code.words[end])) ++end;
        if (end >= code.words.size()) break;
        const nhk::arm64::Match function{code.address + start * 4,
            code.words.subspan(start, end - start + 1)};
        bool saw_first = false;
        bool ordered = false;
        size_t first_call = 0;
        size_t second_call = 0;
        for (size_t i = 0; i < function.words.size(); ++i) {
            const auto target = nhk::arm64::call_target(function, i);
            if (!target || *target < code.address) continue;
            const size_t stub_index = (*target - code.address) / 4;
            if ((*target - code.address) % 4 != 0 || stub_index + 4 > code.words.size()) {
                continue;
            }
            // Resolve the call through its PLT stub to a GOT slot address.
            const auto stub_words = code.words.subspan(stub_index, 4);
            const auto got = nhk::arm64::decode_plt_got(stub_words, 0, *target);
            if (got == got_first && !saw_first) {
                saw_first = true;
                first_call = i;
            } else if (got == got_second && saw_first) {
                ordered = true;
                second_call = i;
            }
        }
        // Optional disambiguation evidence: the accepted shape carries a NOP
        // between the two import calls (function A has one, its twin does not).
        if (ordered && require_nop_between) {
            bool nop = false;
            for (size_t i = first_call + 1; i < second_call; ++i) {
                nop |= function.words[i] == 0xd503201f;
            }
            ordered = nop;
        }
        if (ordered) {
            ++candidates;
            evidence_out->call_sites.push_back(function.address - code.address);
            if (!found) found = function;
        }
        start = end + 1;
    }
    evidence_out->candidate_count = candidates;
    evidence_out->stage = candidates == 1 ? "ordered-import-unique" : "ordered-import-ambiguous";
    if (candidates != 1) return {};
    return found;
}

} // namespace

int main() {
    // --- Decode primitives. ---
    {
        const uintptr_t pc = 0x100000;
        const auto page = nhk::arm64::decode_adrp(pc, adrp(16, 1));
        check(page.has_value() && *page == (pc & ~0xfffULL) + 0x1000, "adrp forward page");
        const auto back = nhk::arm64::decode_adrp(pc, adrp(16, -1));
        check(back.has_value() && *back == (pc & ~0xfffULL) - 0x1000, "adrp backward page");
        check(!nhk::arm64::decode_adrp(pc, 0x91000000U | 16).has_value(),
            "ADD is not ADRP");
        check(nhk::arm64::decode_add_immediate(add_imm(16, 0x123)).has_value()
            && *nhk::arm64::decode_add_immediate(add_imm(16, 0x123)) == 0x123,
            "add immediate decoded");
        check(nhk::arm64::is_ldr64_immediate(ldr64(17, 16, 8)), "ldr64 shape");
        const auto branch = nhk::arm64::branch_target(pc, bl(0, 4), true);
        check(branch.has_value() && *branch == pc + 16, "bl target");
        check(nhk::arm64::decode_conditional_branch_target(pc, 0x54000100U).has_value(),
            "b.cond shape");
    }

    // --- Generic ordered-import resolver over the fixture. ---
    {
        ImportCallFixture fixture;
        fixture.assemble();
        const auto code = fixture.range();
        nhk::ResolverEvidence evidence;
        const auto match = find_ordered_import_caller(code,
            fixture.got_madvise_rva, fixture.got_memcpy_rva,
            /*require_nop_between=*/false, &evidence);
        check(evidence.candidate_count == 2,
            "two ordered callers produce two candidates");
        check(!match.has_value(), "multiple candidates fail closed");

        const auto unique = find_ordered_import_caller(code,
            fixture.got_madvise_rva, fixture.got_memcpy_rva,
            /*require_nop_between=*/true, &evidence);
        check(evidence.candidate_count == 1, "single candidate after disambiguation");
        check(nhk::evidence_acceptable(evidence), "evidence accepted");
        check(unique.has_value() && unique->address == code.address,
            "function A is the unique ordered caller");
    }

    // --- Zero candidates also fail closed. ---
    {
        ImportCallFixture fixture;
        fixture.assemble();
        // Range covering only functions B and C (no ordered pair).
        const auto narrowed = nhk::arm64::CodeRange{fixture.base + 5 * 4,
            std::span<const uint32_t>(fixture.words).subspan(5, 8)};
        nhk::ResolverEvidence evidence;
        const auto none = find_ordered_import_caller(narrowed,
            fixture.got_madvise_rva, fixture.got_memcpy_rva,
            /*require_nop_between=*/false, &evidence);
        check(!none.has_value() && evidence.candidate_count == 0,
            "zero candidates fail closed");
    }

    if (failures == 0) std::printf("NativeHookCoreResolverTest passed\n");
    return failures == 0 ? 0 : 1;
}
