/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the NativeHookRuntime ELF layer (nativehook/elf_image.h):
 * symbol lookup through both hash tables and the linear fallback, relocation
 * slot collection, the unique-slot gate, and fail-closed behavior on
 * corrupted or truncated images.
 */
#include "../../app/src/main/cpp/nativehook/elf_image.h"

#include "../nativehook/elf_fixture.h"

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

int main() {
    using nhk::elf::ElfImage;

    // --- The relocation numbering itself, asserted directly. ---
    //
    // A fixture that encodes a wrong constant is perfectly self-consistent and
    // passes; only the real numbers catch a mix-up (JUMP_SLOT is 1026, not the
    // 1027 that RELATIVE uses). These assertions exist so that cannot recur.
    check(nhk::elf::kRelocJumpSlot == 1026, "R_AARCH64_JUMP_SLOT is 0x402");
    check(nhk::elf::kRelocGlobDat == 1025, "R_AARCH64_GLOB_DAT is 0x401");
    check(nhk::elf::kRelocRelative == 1027, "R_AARCH64_RELATIVE is 0x403");
    check(nhk::elf::kRelocJumpSlot != nhk::elf::kRelocRelative,
        "JUMP_SLOT and RELATIVE are distinct types");

    // --- Fixture: two defined symbols; madvise has one JUMP_SLOT; memcpy has two. ---
    const auto fixture = elf_fixture::build(
        /*defined=*/{"dock_target", "helper_fn"},
        /*relocs=*/{
            {"madvise", nhk::elf::kRelocJumpSlot, 0xc00},
            {"memcpy", nhk::elf::kRelocJumpSlot, 0xc08},
            {"memcpy", nhk::elf::kRelocJumpSlot, 0xc10},
        });
    check(fixture.has_value(), "fixture builds");

    const auto image = ElfImage::make(0x7100000000ULL,
        std::span<const std::byte>(reinterpret_cast<const std::byte *>(fixture->data()),
            fixture->size()));
    check(image.has_value(), "image parses");

    if (image) {
        // --- Symbol lookup: GNU hash, SYSV hash and linear paths must agree. ---
        const auto direct = image->find_symbol("dock_target");
        check(direct.has_value(), "defined symbol found");
        check(direct && direct->rva == 0x500, "symbol rva matches fixture");
        check(direct && direct->size == 8, "symbol size matches fixture");
        check(!image->find_symbol("absent_symbol").has_value(), "missing symbol fails closed");
        check(!image->find_symbol("madvise").has_value(),
            "undefined import is not a definition");

        // --- Slot collection and the uniqueness gate. ---
        const auto madvise_slots = image->collect_slots("madvise");
        check(madvise_slots.size() == 1, "madvise has exactly one slot");
        const auto unique = image->unique_slot("madvise");
        check(unique.has_value() && unique->rva == 0xc00, "unique slot resolves");
        check(unique && unique->type == nhk::elf::kRelocJumpSlot, "slot type preserved");

        const auto memcpy_slots = image->collect_slots("memcpy");
        check(memcpy_slots.size() == 2, "memcpy exposes both slots");
        check(!image->unique_slot("memcpy").has_value(),
            "two candidate slots fail closed");

        // --- RVA/runtime translation round trip. ---
        if (unique) {
            const uintptr_t runtime = image->runtime(unique->rva);
            check(image->rva(runtime) == unique->rva, "rva/runtime round trip");
        }
    }

    // --- Mixed JUMP_SLOT + GLOB_DAT for one symbol: still one logical hook point,
    // but unique_slot() must refuse because both live in the table. ---
    {
        const auto mixed = elf_fixture::build_with_glob_dat(
            {"dock_target"},
            {{"madvise", nhk::elf::kRelocJumpSlot, 0xc00}},
            {{"madvise", nhk::elf::kRelocGlobDat, 0xc18}});
        check(mixed.has_value(), "mixed fixture builds");
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(mixed->data()),
                mixed->size()));
        check(image.has_value(), "mixed image parses");
        if (image) {
            check(image->collect_slots("madvise").size() == 2, "both reloc kinds found");
            check(!image->unique_slot("madvise").has_value(),
                "mixed kinds cannot be disambiguated from metadata alone");
        }
    }

    // --- A RELATIVE relocation is not an import slot. ---
    {
        const auto relative = elf_fixture::build({"dock_target"},
            {{"madvise", nhk::elf::kRelocRelative, 0xc00}});
        check(relative.has_value(), "RELATIVE fixture builds");
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(relative->data()),
                relative->size()));
        check(image.has_value(), "RELATIVE image parses");
        if (image) {
            check(image->collect_slots("madvise").empty(),
                "RELATIVE entries are not collected as import slots");
        }
    }

    // --- Hashless image: symbol count falls back to the symtab/strtab gap. ---
    {
        const auto hashless = elf_fixture::build({"dock_target"}, {{"madvise", nhk::elf::kRelocJumpSlot, 0xc00}},
            /*with_sysv_hash=*/false, /*with_gnu_hash=*/false);
        check(hashless.has_value(), "hashless fixture builds");
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(hashless->data()),
                hashless->size()));
        check(image.has_value(), "hashless image parses");
        if (image) {
            check(image->find_symbol("dock_target").has_value(),
                "linear fallback finds symbols");
        }
    }

    // --- Corrupted inputs fail closed. ---
    {
        auto truncated = *fixture;
        truncated.bytes.resize(0x30);
        check(!ElfImage::make(0x7100000000ULL,
                  std::span<const std::byte>(reinterpret_cast<const std::byte *>(truncated.data()),
                      truncated.size())).has_value(),
            "truncated header rejected");

        auto bad_machine = *fixture;
        bad_machine.bytes[18] = 0x3e; // EM_X86_64
        check(!ElfImage::make(0x7100000000ULL,
                  std::span<const std::byte>(reinterpret_cast<const std::byte *>(bad_machine.data()),
                      bad_machine.size())).has_value(),
            "non-ARM64 machine rejected");

        auto bad_phoff = *fixture;
        elf_fixture::put64(bad_phoff.bytes, 32, 0x2000ULL); // e_phoff outside every segment.
        check(!ElfImage::make(0x7100000000ULL,
                  std::span<const std::byte>(reinterpret_cast<const std::byte *>(bad_phoff.data()),
                      bad_phoff.size())).has_value(),
            "program header table outside segments rejected");

        check(!ElfImage::make(0x7100000000ULL, std::span<const std::byte>{}).has_value(),
            "empty image rejected");
    }

    // --- A broken GNU table must be disabled, not armed. ---
    //
    // The invalid pointer is never dereferenced: the table fails validation and
    // is dropped during parsing, so a lookup falls back to the (valid) SYSV
    // table. Before the fix this read out of bounds on the first lookup.
    {
        auto broken_gnu = *fixture;
        // DT_GNU_HASH value lives at kDynOff + index*16 + 8 (index 5 with SYSV).
        constexpr size_t kDynOff = 0x200;
        elf_fixture::put64(broken_gnu.bytes, kDynOff + 5 * 16 + 8,
            broken_gnu.size() - 1); // Last byte: header partially outside.
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(broken_gnu.data()),
                broken_gnu.size()));
        check(image.has_value(), "image still parses with a corrupt GNU table");
        if (image) {
            check(image->find_symbol("dock_target").has_value(),
                "lookup falls back to the valid SYSV table");
            check(!image->find_symbol("absent_symbol").has_value(),
                "missing symbol still fails closed with a corrupt table");
            check(image->unique_slot("madvise").has_value(),
                "slot collection keeps working");
        }
    }

    // --- A cyclic SYSV chain must terminate. ---
    {
        auto cyclic = *fixture;
        // chains[] start after the 2-word header and the single bucket; point
        // symbol 1's chain entry at itself.
        constexpr size_t kSysvOff = 0x800;
        const size_t chains = kSysvOff + 8 + 4;
        elf_fixture::put32(cyclic.bytes, chains + 1 * 4, 1);
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(cyclic.data()),
                cyclic.size()));
        check(image.has_value(), "image with a cyclic chain parses");
        if (image) {
            // A symbol that is not in the table must return, not spin.
            check(!image->find_symbol("not_present_at_all").has_value(),
                "cyclic chain terminates without a match");
            check(image->find_symbol("dock_target").has_value(),
                "cyclic chain still finds symbols reachable from the bucket");
        }
    }

    // --- Memory view: a data segment whose p_vaddr differs from p_offset. ---
    {
        constexpr uint64_t kShift = 0x1000;
        const auto mapped = elf_fixture::build(
            {"dock_target"}, {{"madvise", nhk::elf::kRelocJumpSlot, 0xc00}}, true, true, kShift);
        check(mapped.has_value(), "memory-view fixture builds");
        const auto image = ElfImage::make(0x7100000000ULL,
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(mapped->data()),
                mapped->size()));
        check(image.has_value(), "memory view parses");
        if (image) {
            // The slot is addressed in the virtual-address domain, i.e. shifted.
            const auto slot = image->unique_slot("madvise");
            check(slot.has_value(), "slot found in the memory view");
            check(slot && slot->rva == 0xc00 + kShift,
                "slot rva follows p_vaddr, not p_offset");
            check(image->contains(0xc00 + kShift, 8, 0),
                "shifted slot is inside the RW segment");
            check(!image->contains(0xc00, 8, 0) || kShift == 0,
                "unshifted offset is not what the image reports");
            check(image->find_symbol("dock_target").has_value(),
                "symbols resolve in the memory view");
        }
    }

    if (failures == 0) std::printf("NativeHookElfTest passed\n");
    return failures == 0 ? 0 : 1;
}
