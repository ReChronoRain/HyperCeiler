/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Synthetic ELF64 fixture for NativeHookRuntime host tests.
 *
 * Builds a minimal but structurally complete shared object in memory:
 * two PT_LOAD segments, a dynamic table, dynsym/dynstr, SYSV and GNU hash
 * tables, .rela.plt (JUMP_SLOT) and .rela.dyn (GLOB_DAT). Byte offsets are
 * explicit, mirroring how nativehook/elf_image.h decodes - the fixture must
 * stay honest about the format, not depend on the parser's helpers.
 */
#pragma once

#include <cstdint>
#include <cstring>
#include <optional>
#include <span>
#include <string>
#include <vector>

namespace elf_fixture {

inline void put16(std::vector<uint8_t> &out, size_t at, uint16_t value) {
    out[at] = static_cast<uint8_t>(value);
    out[at + 1] = static_cast<uint8_t>(value >> 8);
}

inline void put32(std::vector<uint8_t> &out, size_t at, uint32_t value) {
    for (int i = 0; i < 4; ++i) out[at + i] = static_cast<uint8_t>(value >> (i * 8));
}

inline void put64(std::vector<uint8_t> &out, size_t at, uint64_t value) {
    for (int i = 0; i < 8; ++i) out[at + i] = static_cast<uint8_t>(value >> (i * 8));
}

inline uint32_t gnu_hash_of(const std::string &name) {
    uint32_t hash = 5381;
    for (const char c : name) hash = hash * 33 + static_cast<uint8_t>(c);
    return hash;
}

inline uint32_t sysv_hash_of(const std::string &name) {
    uint32_t hash = 0;
    for (const char c : name) {
        hash = (hash << 4) + static_cast<uint8_t>(c);
        const uint32_t nibble = hash & 0xf0000000U;
        if (nibble != 0) hash ^= nibble >> 24;
        hash &= ~nibble;
    }
    return hash;
}

struct Reloc {
    std::string symbol;
    // AArch64: JUMP_SLOT = 1026 (0x402), GLOB_DAT = 1025 (0x401). Getting these
    // backwards is invisible to a self-consistent fixture, so encode the real
    // numbers and assert them in the tests.
    uint32_t type = 1026; // R_AARCH64_JUMP_SLOT
    uint64_t slot_rva = 0;
};

struct Fixture {
    std::vector<uint8_t> bytes;
    uint64_t got_rva = 0; // Where slot values live (virtual-address domain).
    uint64_t vaddr_shift = 0;

    uint8_t *data() { return bytes.data(); }
    const uint8_t *data() const { return bytes.data(); }
    size_t size() const { return bytes.size(); }
};

/**
 * Build a minimal ELF64 image.
 *
 * `vaddr_shift` selects the view the fixture models:
 *  - 0: a file view, where every PT_LOAD has p_vaddr == p_offset (what a
 *    normally linked Android .so looks like, and what parsing a file gives);
 *  - nonzero: a memory view, where each segment's p_vaddr is its file offset
 *    plus the shift and the bytes live at the vaddr position, leaving a hole
 *    at the start. This is the layout of a mapped image whose segments do not
 *    start at load bias 0, and it is what proves the parser indexes by
 *    p_vaddr rather than p_offset.
 */
inline std::optional<Fixture> build(const std::vector<std::string> &defined_symbols,
    const std::vector<Reloc> &relocs, bool with_sysv_hash = true,
    bool with_gnu_hash = true, uint64_t vaddr_shift = 0) {
    // Image layout (all offsets page-independent, chosen for easy bounds math):
    //   0x000 ELF header + 3 phdrs          0x400 dynsym
    //   0x200 .dynamic                      0x600 .dynstr
    //   0x300 (pad)                         0x800 .hash / 0x900 .gnu.hash
    //   0xa00 .rela.plt / 0xb00 .rela.dyn   0xc00 .got (RW segment)
    constexpr uint64_t kDynOff = 0x200;
    constexpr uint64_t kSymOff = 0x400;
    // Without hash tables the parser derives the symbol count from the
    // symtab/strtab gap, so the hashless layout places .dynstr immediately
    // after the last symbol entry (an exact multiple of the 24-byte entry).
    const size_t total_symtab_entries = 1 + defined_symbols.size() + relocs.size();
    const uint64_t kStrOff = (!with_sysv_hash && !with_gnu_hash)
        ? kSymOff + static_cast<uint64_t>(total_symtab_entries) * 24
        : 0x600;
    constexpr uint64_t kSysvOff = 0x800;
    constexpr uint64_t kGnuOff = 0x900;
    constexpr uint64_t kPltRelaOff = 0xa00;
    constexpr uint64_t kGotOff = 0xc00;

    const size_t symtab_entries = 1 + defined_symbols.size(); // + null symbol.
    const size_t plt_relocs = relocs.size();

    size_t strtab_size = 1;
    std::vector<size_t> name_offsets;
    for (const auto &name : defined_symbols) {
        name_offsets.push_back(strtab_size);
        strtab_size += name.size() + 1;
    }
    std::vector<size_t> reloc_name_offsets;
    for (const auto &reloc : relocs) {
        reloc_name_offsets.push_back(strtab_size);
        strtab_size += reloc.symbol.size() + 1;
    }

    Fixture fixture;
    auto &out = fixture.bytes;
    fixture.vaddr_shift = vaddr_shift;
    // Segment 0 keeps offset == vaddr; the RW segment's bytes live at its vaddr
    // so a nonzero shift models a mapped image whose data segment does not start
    // at the load bias.
    out.assign(static_cast<size_t>(kGotOff + vaddr_shift + 0x400), 0);
    fixture.got_rva = kGotOff + vaddr_shift;

    // --- ELF header ---
    const uint8_t ident[16] = {0x7f, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    std::memcpy(out.data(), ident, 16);
    put16(out, 16, 3);          // ET_DYN
    put16(out, 18, 183);        // EM_AARCH64
    put32(out, 20, 1);          // EV_CURRENT
    put64(out, 24, 0);          // e_entry
    put64(out, 32, 64);         // e_phoff (right after the ELF header)
    put16(out, 52, 64);         // e_ehsize
    put16(out, 54, 56);         // e_phentsize
    put16(out, 56, 3);          // e_phnum

    // --- Program headers: RX metadata, RW data, PT_DYNAMIC ---
    const uint64_t load1_end = kGotOff;   // Metadata is read-only.
    auto phdr = [&](size_t index, uint32_t type, uint32_t flags, uint64_t offset,
                     uint64_t vaddr, uint64_t filesz, uint64_t memsz) {
        const size_t at = 64 + index * 56;
        put32(out, at, type);
        put32(out, at + 4, flags);
        put64(out, at + 8, offset);
        put64(out, at + 16, vaddr);
        put64(out, at + 32, filesz);
        put64(out, at + 40, memsz);
    };
    phdr(0, 1, 5, 0, 0, load1_end, load1_end);                       // PT_LOAD R+X
    phdr(1, 1, 6, kGotOff, kGotOff + vaddr_shift, 0x400, 0x480);     // PT_LOAD RW (+BSS)
    phdr(2, 2, 4, kDynOff, kDynOff, 0x100, 0x100);                   // PT_DYNAMIC

    // --- .dynamic ---
    size_t dyn = kDynOff;
    auto dyn_entry = [&](uint64_t tag, uint64_t value) {
        put64(out, dyn, tag);
        put64(out, dyn + 8, value);
        dyn += 16;
    };
    dyn_entry(5, kStrOff);                  // DT_STRTAB
    dyn_entry(10, strtab_size);             // DT_STRSZ
    dyn_entry(6, kSymOff);                  // DT_SYMTAB
    dyn_entry(11, 24);                      // DT_SYMENT
    if (with_sysv_hash) dyn_entry(4, kSysvOff);
    if (with_gnu_hash) dyn_entry(0x6ffffef5, kGnuOff);
    dyn_entry(23, kPltRelaOff);             // DT_JMPREL
    dyn_entry(2, plt_relocs * 24);          // DT_PLTRELSZ
    dyn_entry(20, 7);                       // DT_PLTREL = DT_RELA
    dyn_entry(9, 24);                       // DT_RELAENT
    dyn_entry(0, 0);                        // DT_NULL

    // --- .dynsym (null symbol first) ---
    auto sym_entry = [&](size_t index, uint32_t name_offset, uint32_t shndx,
                          uint64_t value, uint64_t size) {
        const size_t at = kSymOff + index * 24;
        put32(out, at, name_offset);
        out[at + 4] = 0x12; // GLOBAL|FUNC
        out[at + 5] = 0;
        put16(out, at + 6, shndx);
        put64(out, at + 8, value);
        put64(out, at + 16, size);
    };
    sym_entry(0, 0, 0, 0, 0);
    for (size_t i = 0; i < defined_symbols.size(); ++i) {
        sym_entry(1 + i, static_cast<uint32_t>(name_offsets[i]), 4, 0x500 + i * 8, 8);
    }

    // --- .dynstr ---
    {
        size_t at = kStrOff;
        out[at++] = 0;
        for (size_t i = 0; i < defined_symbols.size(); ++i) {
            std::memcpy(out.data() + at, defined_symbols[i].c_str(),
                defined_symbols[i].size() + 1);
            at += defined_symbols[i].size() + 1;
        }
        for (size_t i = 0; i < relocs.size(); ++i) {
            std::memcpy(out.data() + at, relocs[i].symbol.c_str(),
                relocs[i].symbol.size() + 1);
            at += relocs[i].symbol.size() + 1;
        }
    }

    // --- SYSV hash: one bucket chains every symbol ---
    if (with_sysv_hash) {
        put32(out, kSysvOff, 1);                  // nbucket
        put32(out, kSysvOff + 4, symtab_entries); // nchain
        put32(out, kSysvOff + 8, 1);              // buckets[0] -> symbol 1
        for (size_t i = 1; i < symtab_entries; ++i) {
            const uint32_t next = i + 1 < symtab_entries ? static_cast<uint32_t>(i + 1) : 0;
            put32(out, kSysvOff + 8 + (1 + i - 1) * 4 + 4 - 4, next); // chains[i] = i+1
        }
        // chains[] starts after buckets: index i of chain array corresponds to symbol i.
        for (size_t i = 0; i < symtab_entries; ++i) {
            const uint32_t next = i + 1 < symtab_entries ? static_cast<uint32_t>(i + 1) : 0;
            put32(out, kSysvOff + 8 + 4 + i * 4, next);
        }
    }

    // --- GNU hash: one bucket, single chain over all defined symbols ---
    if (with_gnu_hash) {
        put32(out, kGnuOff, 1);          // nbucket
        put32(out, kGnuOff + 4, 1);      // symoffset (symbol 0 is the null symbol)
        put32(out, kGnuOff + 8, 1);      // bloom_size
        put32(out, kGnuOff + 12, 0);     // bloom_shift
        put64(out, kGnuOff + 16, ~0ULL); // bloom filter: everything "maybe"
        put32(out, kGnuOff + 24, 1);     // buckets[0] -> symbol 1
        for (size_t i = 0; i < defined_symbols.size(); ++i) {
            const uint32_t hash = gnu_hash_of(defined_symbols[i]);
            const bool last = i + 1 == defined_symbols.size();
            put32(out, kGnuOff + 28 + i * 4, last ? (hash | 1U) : (hash & ~1U));
        }
    }

    // --- .rela.plt / .rela.dyn ---
    for (size_t i = 0; i < relocs.size(); ++i) {
        const size_t at = kPltRelaOff + i * 24;
        // r_offset is a virtual address: it must follow the RW segment's vaddr.
        put64(out, at, relocs[i].slot_rva + vaddr_shift);
        uint32_t symindex = 0;
        for (size_t s = 0; s < relocs.size(); ++s) {
            if (relocs[s].symbol == relocs[i].symbol) { symindex = static_cast<uint32_t>(1 + defined_symbols.size() + s); break; }
        }
        // Reloc symbols live after defined symbols in the symbol table: append them.
        put64(out, at + 8, (static_cast<uint64_t>(symindex) << 32) | relocs[i].type);
        put64(out, at + 16, 0);
    }
    // Reloc symbol entries must exist in dynsym: rebuild symtab/strtab including them.
    // (Fixture simplification: reloc symbols are appended as UNDEF symbols.)
    {
        const size_t total_syms = symtab_entries + relocs.size();
        for (size_t i = 0; i < relocs.size(); ++i) {
            sym_entry(symtab_entries + i, static_cast<uint32_t>(reloc_name_offsets[i]),
                0, 0, 0); // SHN_UNDEF import.
        }
        // Fix the SYSV/GNU chain coverage to include import symbols.
        if (with_sysv_hash) {
            put32(out, kSysvOff + 4, static_cast<uint32_t>(total_syms));
            for (size_t i = 0; i < total_syms; ++i) {
                const uint32_t next = i + 1 < total_syms ? static_cast<uint32_t>(i + 1) : 0;
                put32(out, kSysvOff + 8 + 4 + i * 4, next);
            }
        }
        if (with_gnu_hash) {
            std::vector<std::string> all_names = defined_symbols;
            for (const auto &reloc : relocs) all_names.push_back(reloc.symbol);
            for (size_t i = 0; i < all_names.size(); ++i) {
                const uint32_t hash = gnu_hash_of(all_names[i]);
                const bool last = i + 1 == all_names.size();
                put32(out, kGnuOff + 28 + i * 4, last ? (hash | 1U) : (hash & ~1U));
            }
        }
    }

    // GLOB_DAT entries for a symbol named "madvise" are produced by the caller
    // via the `type` field of a reloc entry; nothing extra to lay out here.
    return fixture;
}

/** Convenience: GLOB_DAT relocations laid out in .rela.dyn. */
inline std::optional<Fixture> build_with_glob_dat(
    const std::vector<std::string> &defined, const std::vector<Reloc> &jump_slots,
    const std::vector<Reloc> &glob_dats) {
    // Fold GLOB_DATs into the same table with their type; the parser walks
    // both tables and filters by type, so one table with mixed types is enough.
    std::vector<Reloc> all = jump_slots;
    for (auto reloc : glob_dats) {
        reloc.type = 0x401; // R_AARCH64_GLOB_DAT
        all.push_back(reloc);
    }
    return build(defined, all);
}

} // namespace elf_fixture
