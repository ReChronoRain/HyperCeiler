/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime ELF layer: runtime parsing of a mapped ELF64 image.
 *
 * Ported and generalized from MiuiBackGestureHook's runtime ELF resolution
 * (miui-home-hyos-native/runtime_profile_resolver.cpp ParseElf + dynamic
 * view, lsposed_hook_backend.cpp BuildDynamicView; Apache-2.0, see
 * THIRD_PARTY_NOTICES.md in this directory). Deltas from the source:
 *  - Decoding is byte-offset based (no <elf.h> layout assumptions), so the
 *    same code runs on device and in host tests on any OS.
 *  - Symbol lookup walks the GNU and SYSV hash tables instead of only using
 *    them for a symbol count, then falls back to a bounded linear scan.
 *  - Works over a caller-supplied snapshot span instead of live memory:
 *    the runtime never dereferences mappings that could be reclaimed
 *    concurrently; callers snapshot the metadata region first.
 *
 * Failure discipline: a malformed, truncated or hostile ELF fails closed at
 * every step. Nothing here can read outside the supplied span, and nothing
 * here installs a hook - resolvers answer "where and why", backends act.
 */
#pragma once

#include "nhk_base.h"

#include <cstdint>
#include <cstdio>
#include <optional>
#include <span>
#include <string_view>
#include <utility>
#include <vector>

namespace nhk::elf {

inline constexpr uint16_t kMachineArm64 = 183;   // EM_AARCH64
inline constexpr uint32_t kDynamicNull = 0;      // DT_NULL
inline constexpr uint32_t kDynamicHash = 4;      // DT_HASH
inline constexpr uint32_t kDynamicStrtab = 5;    // DT_STRTAB
inline constexpr uint32_t kDynamicSymtab = 6;    // DT_SYMTAB
inline constexpr uint32_t kDynamicStrsz = 10;    // DT_STRSZ
inline constexpr uint32_t kDynamicSyment = 11;   // DT_SYMENT
inline constexpr uint32_t kDynamicRela = 7;      // DT_RELA
inline constexpr uint32_t kDynamicRelasz = 8;    // DT_RELASZ
inline constexpr uint32_t kDynamicRelaent = 9;   // DT_RELAENT
inline constexpr uint32_t kDynamicJmprel = 23;   // DT_JMPREL
inline constexpr uint32_t kDynamicPltrelsz = 2;  // DT_PLTRELSZ
inline constexpr uint32_t kDynamicPltrel = 20;   // DT_PLTREL
inline constexpr uint32_t kDynamicPltrelRela = 7;// DT_RELA as the PLT relocation type
inline constexpr uint32_t kDynamicGnuHash = 0x6ffffef5;

// AArch64 relocation types (ELF for the ARM 64-bit Architecture). Note the
// numbering: JUMP_SLOT is 1026 and RELATIVE is 1027 - confusing them silently
// drops every PLT import, which is exactly what a real library exposed once
// (nothing on the host caught it, because a self-consistent fixture can encode
// the wrong constant just as easily as the right one).
inline constexpr uint32_t kRelocGlobDat = 1025;   // R_AARCH64_GLOB_DAT   (0x401)
inline constexpr uint32_t kRelocJumpSlot = 1026;  // R_AARCH64_JUMP_SLOT  (0x402)
inline constexpr uint32_t kRelocRelative = 1027;  // R_AARCH64_RELATIVE   (0x403)

inline constexpr size_t kSymBytes = 24;   // Elf64_Sym
inline constexpr size_t kRelaBytes = 24;  // Elf64_Rela
inline constexpr size_t kDynBytes = 16;   // Elf64_Dyn
inline constexpr size_t kMaxLoadSegments = 16;
inline constexpr size_t kMaxPltSlots = 16;
inline constexpr uint64_t kMaxImageSpan = 0x40000000ULL; // 1 GiB: a live mapped image cap.
inline constexpr uint64_t kMaxSymbols = 1U << 24;

/**
 * A parsed program header entry. Both address domains are kept: `offset` /
 * `filesz` describe the file, `vaddr` / `memsz` the image's virtual address
 * space.
 *
 * Legal ELF files routinely have `vaddr - offset` differ per segment (a real
 * system library measured 0x0 / 0x10000 / 0x20000 / 0x30000 across its four
 * PT_LOADs), so anything that maps a *runtime* mapping back to a virtual
 * address must consult this table rather than assuming the two domains
 * coincide.
 */
struct ProgramSegment {
    uint32_t type = 0;
    uint32_t flags = 0;
    uint64_t offset = 0;
    uint64_t vaddr = 0;
    uint64_t filesz = 0;
    uint64_t memsz = 0;
};

inline constexpr uint32_t kProgramTypeLoad = 1;
inline constexpr uint32_t kProgramTypeDynamic = 2;
inline constexpr uint16_t kMaxProgramHeaders = 128;
inline constexpr size_t kProgramHeaderBytes = 56;
inline constexpr size_t kElfHeaderBytes = 64;

// Segment permission bits (PF_*). One definition, referenced by the class.
inline constexpr uint32_t kFlagExecute = 1;
inline constexpr uint32_t kFlagWrite = 2;
inline constexpr uint32_t kFlagRead = 4;

/**
 * Decode the program header table of an ELF64 image.
 *
 * `head` must start at the ELF header; it only needs to contain the header plus
 * the program header table (a caller reads those few kilobytes first and then
 * uses the table to work out what else it must read). Every bound is checked,
 * so a hostile table cannot make the caller read outside `head`.
 */
inline std::optional<std::vector<ProgramSegment>> parse_program_segments(
    std::span<const std::byte> head, bool require_arm64_dyn = true) {
    if (head.size() < kElfHeaderBytes) return {};
    if (head[0] != std::byte{0x7f} || head[1] != std::byte{'E'}
        || head[2] != std::byte{'L'} || head[3] != std::byte{'F'}) return {};
    if (head[4] != std::byte{2} || head[5] != std::byte{1}) return {};
    // ARM64/ET_DYN are required where the table drives address translation; a
    // container probe only needs the loadable segment list and may run looser.
    if (require_arm64_dyn) {
        if (load_le16(head.data() + 18) != kMachineArm64) return {};
        if (load_le16(head.data() + 16) != 3) return {};
    }
    const uint64_t program_offset = load_le64(head.data() + 32);
    const uint16_t program_entry_size = load_le16(head.data() + 54);
    const uint16_t program_count = load_le16(head.data() + 56);
    if (program_entry_size != kProgramHeaderBytes || program_count == 0
        || program_count > kMaxProgramHeaders) return {};
    if (program_offset > 0x1000) return {};
    if (!in_image(head.size(), program_offset,
        static_cast<uint64_t>(program_count) * kProgramHeaderBytes)) return {};

    std::vector<ProgramSegment> segments;
    segments.reserve(program_count);
    for (uint16_t i = 0; i < program_count; ++i) {
        const std::byte *entry = head.data() + program_offset
            + static_cast<size_t>(i) * kProgramHeaderBytes;
        ProgramSegment segment;
        segment.type = load_le32(entry);
        segment.flags = load_le32(entry + 4);
        segment.offset = load_le64(entry + 8);
        segment.vaddr = load_le64(entry + 16);
        segment.filesz = load_le64(entry + 32);
        segment.memsz = load_le64(entry + 40);
        if (add_overflows(segment.offset, segment.filesz)
            || add_overflows(segment.vaddr, segment.memsz)) return {};
        // The memory-size invariants matter only when the table drives address
        // translation; a container probe uses the file view alone, and a probe
        // fixture legitimately leaves p_memsz unset.
        if (require_arm64_dyn
            && (segment.memsz < segment.filesz || segment.memsz > kMaxImageSpan)) {
            return {};
        }
        segments.push_back(segment);
    }
    return segments;
}

/** A parsed PT_LOAD segment.
 *
 * `vaddr` / `mem_end` / `file_end` live in the image's virtual-address domain,
 * which is the domain every ELF metadata field uses: dynamic pointers,
 * st_value and r_offset are virtual addresses, never file offsets. `offset`
 * (p_offset) is kept only to translate file-relative references (e_phoff,
 * DT_* that name file positions in a non-prelinked image) into that domain.
 * `mem_end` uses p_memsz so the zero-filled tail of a segment is inside the
 * image too; bytes that exist only in memory are simply not addressable in a
 * file-content snapshot, which the span bounds check catches.
 */
struct LoadSegment {
    uint64_t vaddr;
    uint64_t offset;    // p_offset, for file-relative translation only.
    uint64_t file_end;  // One past vaddr + p_filesz.
    uint64_t mem_end;   // One past vaddr + p_memsz.
    uint32_t flags;     // PF_* bits.
};

/** One dynamic symbol table entry, resolved against strtab. */
struct Symbol {
    uint64_t rva;   // Image-relative virtual address (st_value of an ET_DYN).
    uint64_t size;  // st_size; may be 0.
};

struct GotSlot {
    uint64_t rva;            // Image-relative address of the slot itself.
    uint32_t type;           // R_AARCH64_JUMP_SLOT / R_AARCH64_GLOB_DAT.
    uint32_t symbol_index;   // Index into the dynamic symbol table.
};

/**
 * Metadata view over one mapped ELF64 image.
 *
 * `base` is the runtime address of `image[0]` (the load bias of the first
 * PT_LOAD when mapped from its start). All parsing reads only from `image`;
 * runtime addresses of slots and symbols are expressed as image-relative
 * virtual addresses (RVA), which stay stable for a given generation.
 */
class ElfImage {
public:
    // ELF header field offsets (64-bit).
    static constexpr size_t kHeaderBytes = 64;
    static constexpr size_t kProgramHeaderBytes = 56;
    static constexpr uint32_t kProgramHeaderLoad = 1;
    // Aliases of the namespace constants: one definition, no drift.
    static constexpr uint32_t kFlagRead = elf::kFlagRead;
    static constexpr uint32_t kFlagWrite = elf::kFlagWrite;
    static constexpr uint32_t kFlagExecute = elf::kFlagExecute;

    static std::optional<ElfImage> make(uintptr_t base, std::span<const std::byte> image) {
        ElfImage result;
        result.base_ = base;
        result.image_ = image;
        if (!result.parse_header_and_segments()) return {};
        if (!result.parse_dynamic()) return {};
        return result;
    }

    /**
     * Same, but with the program header table supplied by the caller.
     *
     * A mapped image is described by its program headers, which the caller
     * needs anyway to lay out a virtual-address snapshot; taking that same
     * table here keeps one decoding of it instead of two that could drift.
     */
    static std::optional<ElfImage> make_with_segments(uintptr_t base,
        std::span<const std::byte> image,
        const std::vector<ProgramSegment> &programs) {
        ElfImage result;
        result.base_ = base;
        result.image_ = image;
        if (!result.adopt_segments(programs)) return {};
        if (!result.parse_dynamic()) return {};
        return result;
    }

    [[nodiscard]] uintptr_t base() const { return base_; }
    [[nodiscard]] uint64_t rva(uintptr_t address) const {
        return address >= base_ && address - base_ < image_.size()
            ? address - base_
            : UINT64_MAX;
    }
    [[nodiscard]] uintptr_t runtime(uint64_t image_rva) const {
        return add_overflows(base_, image_rva) ? 0 : base_ + image_rva;
    }

    /** Exact dynamic symbol lookup (GNU hash, then SYSV hash, then linear). */
    std::optional<Symbol> find_symbol(std::string_view name) const {
        if (auto symbol = find_by_gnu_hash(name)) return symbol;
        if (auto symbol = find_by_sysv_hash(name)) return symbol;
        return find_by_linear_scan(name);
    }

    /** Import address of a symbol that must be present and defined. */
    std::optional<uint64_t> find_symbol_rva(std::string_view name) const {
        const auto symbol = find_symbol(name);
        if (!symbol || symbol->rva == 0) return {};
        if (!contains(symbol->rva, 1, kFlagRead)) return {};
        return symbol->rva;
    }

    /**
     * Every JUMP_SLOT/GLOB_DAT relocation naming `symbol`, regardless of how
     * many exist. Callers that cannot disambiguate multiple slots must use
     * [unique_slot] and fail closed instead.
     */
    std::vector<GotSlot> collect_slots(std::string_view symbol,
        bool include_jump_slot = true, bool include_glob_dat = true) const {
        std::vector<GotSlot> result;
        const auto scan = [&](uint64_t table_offset, uint64_t table_bytes) {
            if (table_offset == 0 || table_bytes == 0) return;
            if (!in_image(image_.size(), table_offset, table_bytes)) return;
            const size_t entries = static_cast<size_t>(table_bytes / kRelaBytes);
            for (size_t i = 0; i < entries; ++i) {
                if (result.size() >= kMaxPltSlots) return;
                const std::byte *entry = image_.data() + table_offset + i * kRelaBytes;
                const uint64_t info = load_le64(entry + 8);
                const uint32_t type = static_cast<uint32_t>(info & 0xffffffffU);
                if (type != kRelocJumpSlot && type != kRelocGlobDat) continue;
                if ((type == kRelocJumpSlot && !include_jump_slot)
                    || (type == kRelocGlobDat && !include_glob_dat)) continue;
                const uint32_t symbol_index = static_cast<uint32_t>(info >> 32);
                if (symbol_index == 0 || symbol_index >= symbol_count_) return;
                const auto symbol_name = symbol_name_at(symbol_index);
                if (!symbol_name || *symbol_name != symbol) continue;
                const uint64_t offset = load_le64(entry);
                // A slot must live in a writable-or-readonly data segment; a slot
                // outside every load segment is a corrupted image.
                if (offset == 0 || !contains(offset, sizeof(void *), 0)) continue;
                result.push_back({offset, type, symbol_index});
            }
        };
        scan(jmprel_offset_, jmprel_bytes_);
        scan(rela_offset_, rela_bytes_);
        return result;
    }

    /**
     * The single JUMP_SLOT/GLOB_DAT slot of `symbol`, or nothing. More than one
     * candidate cannot be told apart from metadata alone, so this fails closed;
     * callers must never "take the first" of several slots.
     */
    std::optional<GotSlot> unique_slot(std::string_view symbol,
        bool include_jump_slot = true, bool include_glob_dat = true) const {
        const auto slots = collect_slots(symbol, include_jump_slot, include_glob_dat);
        if (slots.size() != 1) return {};
        return slots.front();
    }

    /** Segment membership test used by every address validation (vaddr domain). */
    bool contains(uint64_t offset, uint64_t bytes, uint32_t required_flags,
        uint32_t forbidden_flags = 0) const {
        if (bytes == 0 || add_overflows(offset, bytes)) return false;
        for (const auto &segment : segments_) {
            if (offset < segment.vaddr || segment.mem_end < offset + bytes) continue;
            if ((segment.flags & required_flags) != required_flags) continue;
            if ((segment.flags & forbidden_flags) != 0) continue;
            return true;
        }
        return false;
    }

private:
    uintptr_t base_ = 0;
    std::span<const std::byte> image_;
    std::vector<LoadSegment> segments_;
    uint64_t dynamic_file_offset_ = 0; // As written in the program header.
    uint64_t dynamic_offset_ = 0;      // Translated into the vaddr domain.
    uint64_t dynamic_bytes_ = 0;
    uint64_t strtab_offset_ = 0;
    uint64_t strtab_bytes_ = 0;
    uint64_t symtab_offset_ = 0;
    uint64_t jmprel_offset_ = 0;
    uint64_t jmprel_bytes_ = 0;
    uint64_t rela_offset_ = 0;
    uint64_t rela_bytes_ = 0;
    uint64_t sysv_hash_offset_ = 0;
    uint64_t gnu_hash_offset_ = 0;
    uint32_t symbol_count_ = 0; // Upper bound derived from the hash tables.

    /** ELF header sanity, independent of which segment table is used. */
    bool validate_header() const {
        if (image_.size() < kHeaderBytes) return false;
        if (image_[0] != std::byte{0x7f} || image_[1] != std::byte{'E'}
            || image_[2] != std::byte{'L'} || image_[3] != std::byte{'F'}) return false;
        if (image_[4] != std::byte{2} || image_[5] != std::byte{1}) return false; // ELF64, LE.
        if (load_le16(image_.data() + 18) != kMachineArm64) return false;
        if (load_le16(image_.data() + 16) != 3) return false; // ET_DYN only.
        const uint16_t program_entry_size = load_le16(image_.data() + 54);
        const uint16_t program_count = load_le16(image_.data() + 56);
        // Same ceiling as the decoder: they must agree, or a well-formed image
        // with more headers than one of them allows would be rejected.
        return program_entry_size == kProgramHeaderBytes && program_count != 0
            && program_count <= kMaxProgramHeaders;
    }

    /**
     * Fill the segment/dynamic state from a program header table.
     *
     * Accepts the table from [parse_program_segments] so a caller that already
     * decoded it (to lay out a snapshot) and this parser cannot disagree.
     */
    bool adopt_segments(const std::vector<ProgramSegment> &programs) {
        if (!validate_header()) return false;
        const uint64_t program_offset = load_le64(image_.data() + 32);
        const uint16_t program_count = load_le16(image_.data() + 56);
        const uint64_t table_bytes = static_cast<uint64_t>(program_count) * kProgramHeaderBytes;
        if (program_offset > 0x1000 || add_overflows(program_offset, table_bytes)) return false;
        // The table bytes themselves need not be inside `image_` here: the caller
        // decoded the table and supplied it. Everything below only needs the
        // segments, so nothing is read from that region.

        bool headers_covered = false;
        for (const auto &segment : programs) {
            if (segment.type == kProgramTypeLoad) {
                if (segment.filesz == 0) continue;
                if (segments_.size() >= kMaxLoadSegments) return false;
                segments_.push_back({segment.vaddr, segment.offset,
                    segment.vaddr + segment.filesz, segment.vaddr + segment.memsz,
                    segment.flags});
                // The program header table is described by a file offset; it must
                // land inside the file content of some load segment.
                if (segment.offset <= program_offset
                    && program_offset + static_cast<uint64_t>(program_count) * kProgramHeaderBytes
                        <= segment.offset + segment.filesz) {
                    headers_covered = true;
                }
            } else if (segment.type == kProgramTypeDynamic) {
                if (segment.filesz == 0 || segment.filesz % kDynBytes != 0) return false;
                dynamic_file_offset_ = segment.offset;
                dynamic_bytes_ = segment.filesz;
            }
        }
        if (!headers_covered || segments_.empty()) return false;
        // Translate the dynamic table's file offset into the vaddr domain, the
        // same way the loader would.
        if (dynamic_file_offset_ != 0) {
            const auto translated = file_offset_to_vaddr(dynamic_file_offset_);
            if (!translated) return false;
            dynamic_offset_ = *translated;
        }
        return true;
    }

    bool parse_header_and_segments() {
        if (!validate_header()) return false;
        const uint64_t program_offset = load_le64(image_.data() + 32);
        const uint16_t program_count = load_le16(image_.data() + 56);
        const uint64_t table_end = program_offset
            + static_cast<uint64_t>(program_count) * kProgramHeaderBytes;
        if (table_end > image_.size()) return false;
        const auto programs = parse_program_segments(
            image_.subspan(0, static_cast<size_t>(table_end)));
        if (!programs) return false;
        return adopt_segments(*programs);
    }

    /**
     * Translate a file offset (e_phoff, a file-view DT_* value) into the image's
     * virtual-address domain through the load segment that contains it.
     */
    std::optional<uint64_t> file_offset_to_vaddr(uint64_t offset) const {
        for (const auto &segment : segments_) {
            const uint64_t file_bytes = segment.file_end - segment.vaddr;
            if (offset < segment.offset || offset - segment.offset >= file_bytes) continue;
            const uint64_t delta = offset - segment.offset;
            if (add_overflows(segment.vaddr, delta)) return {};
            return segment.vaddr + delta;
        }
        return {};
    }

    /** MBG-style normalization: a dynamic pointer may be absolute or pre-relocated. */
    std::optional<uint64_t> normalize_dynamic_pointer(uint64_t value) const {
        if (value >= base_ && value - base_ < image_.size()) return value - base_;
        if (value < image_.size()) return value;
        return {};
    }

    std::optional<std::string_view> symbol_name_at(uint32_t index) const {
        if (index >= symbol_count_ || strtab_bytes_ == 0) return {};
        const uint64_t entry = symtab_offset_ + static_cast<uint64_t>(index) * kSymBytes;
        if (!in_image(image_.size(), entry, kSymBytes)) return {};
        const uint32_t name_offset = load_le32(image_.data() + entry);
        if (name_offset >= strtab_bytes_) return {};
        // Names are NUL-terminated inside the string table: bound the view at
        // the terminator, never at the end of the table.
        const char *name = reinterpret_cast<const char *>(
            image_.data() + strtab_offset_ + name_offset);
        const uint64_t remaining = strtab_bytes_ - name_offset;
        return std::string_view(name, strnlen(name, remaining));
    }

    std::optional<Symbol> symbol_at(uint32_t index) const {
        if (index >= symbol_count_) return {};
        const uint64_t entry = symtab_offset_ + static_cast<uint64_t>(index) * kSymBytes;
        if (!in_image(image_.size(), entry, kSymBytes)) return {};
        const std::byte *sym = image_.data() + entry;
        const uint64_t value = load_le64(sym + 8);
        // SHN_UNDEF (0) with no value is an import, not a definition.
        if (load_le16(sym + 6) == 0 || value == 0) return {};
        if (!contains(value, 1, kFlagRead)) return {};
        return Symbol{value, load_le64(sym + 16)};
    }

    bool parse_dynamic() {
        if (dynamic_offset_ == 0 || dynamic_bytes_ == 0) return false;
        if (!in_image(image_.size(), dynamic_offset_, dynamic_bytes_)) return false;
        bool strtab_seen = false;
        bool symtab_seen = false;
        bool syment_ok = false;
        const size_t entries = static_cast<size_t>(dynamic_bytes_ / kDynBytes);
        for (size_t i = 0; i < entries; ++i) {
            const std::byte *entry = image_.data() + dynamic_offset_ + i * kDynBytes;
            const uint64_t tag = load_le64(entry);
            if (tag == kDynamicNull) break;
            const uint64_t value = load_le64(entry + 8);
            switch (static_cast<uint32_t>(tag)) {
                case kDynamicStrtab:
                    if (auto normalized = normalize_dynamic_pointer(value)) strtab_offset_ = *normalized;
                    break;
                case kDynamicStrsz:
                    strtab_bytes_ = value;
                    strtab_seen = true;
                    break;
                case kDynamicSymtab:
                    if (auto normalized = normalize_dynamic_pointer(value)) symtab_offset_ = *normalized;
                    symtab_seen = true;
                    break;
                case kDynamicSyment:
                    syment_ok = value == kSymBytes;
                    break;
                case kDynamicJmprel:
                    if (auto normalized = normalize_dynamic_pointer(value)) jmprel_offset_ = *normalized;
                    break;
                case kDynamicPltrelsz:
                    jmprel_bytes_ = value;
                    break;
                case kDynamicRela:
                    if (auto normalized = normalize_dynamic_pointer(value)) rela_offset_ = *normalized;
                    break;
                case kDynamicRelasz:
                    rela_bytes_ = value;
                    break;
                case kDynamicRelaent:
                    if (value != kRelaBytes) return false;
                    break;
                case kDynamicPltrel:
                    if (value != kDynamicPltrelRela) return false;
                    break;
                case kDynamicHash:
                    if (auto normalized = normalize_dynamic_pointer(value)) sysv_hash_offset_ = *normalized;
                    break;
                case kDynamicGnuHash:
                    if (auto normalized = normalize_dynamic_pointer(value)) gnu_hash_offset_ = *normalized;
                    break;
                default:
                    break;
            }
        }
        if (!strtab_seen || !symtab_seen || !syment_ok) return false;
        if (strtab_offset_ == 0 || symtab_offset_ == 0) return false;
        if (!in_image(image_.size(), strtab_offset_, strtab_bytes_)) return false;
        if (!contains(symtab_offset_, kSymBytes, kFlagRead)) return false;
        if (jmprel_offset_ != 0
            && (jmprel_bytes_ == 0 || jmprel_bytes_ % kRelaBytes != 0
                || !in_image(image_.size(), jmprel_offset_, jmprel_bytes_))) return false;
        if (rela_offset_ != 0
            && (rela_bytes_ == 0 || rela_bytes_ % kRelaBytes != 0
                || !in_image(image_.size(), rela_offset_, rela_bytes_))) return false;
        // Validate each hash table independently and DISABLE the ones that fail:
        // a later lookup must never read through a pointer that was never
        // checked, merely because a different table happened to validate.
        if (gnu_hash_offset_ != 0 && !gnu_hash_usable()) gnu_hash_offset_ = 0;
        if (sysv_hash_offset_ != 0 && !sysv_hash_usable()) sysv_hash_offset_ = 0;
        symbol_count_ = derive_symbol_count();
        return symbol_count_ > 0;
    }

    /** Header, bloom filter and bucket array bounds of a GNU hash table. */
    bool gnu_hash_usable() const {
        constexpr uint64_t kHeaderWords = 4;
        if (gnu_hash_offset_ == 0
            || !in_image(image_.size(), gnu_hash_offset_,
                kHeaderWords * sizeof(uint32_t))) return false;
        const std::byte *table = image_.data() + gnu_hash_offset_;
        const uint32_t bucket_count = load_le32(table);
        const uint32_t symbol_offset = load_le32(table + 4);
        const uint32_t bloom_count = load_le32(table + 8);
        if (bucket_count == 0 || bucket_count > kMaxSymbols || bloom_count == 0
            || bloom_count > 512 || symbol_offset > kMaxSymbols) return false;
        const uint64_t buckets_offset = kHeaderWords * sizeof(uint32_t)
            + static_cast<uint64_t>(bloom_count) * sizeof(uint64_t);
        return in_image(image_.size(), gnu_hash_offset_ + buckets_offset,
            static_cast<uint64_t>(bucket_count) * sizeof(uint32_t));
    }

    /** Header, bucket array and chain array bounds of a SYSV hash table. */
    bool sysv_hash_usable() const {
        if (sysv_hash_offset_ == 0
            || !in_image(image_.size(), sysv_hash_offset_, 3 * sizeof(uint32_t))) return false;
        const std::byte *table = image_.data() + sysv_hash_offset_;
        const uint32_t bucket_count = load_le32(table);
        const uint32_t chain_count = load_le32(table + 4);
        if (bucket_count == 0 || chain_count == 0 || bucket_count > kMaxSymbols
            || chain_count > kMaxSymbols) return false;
        const uint64_t buckets_offset = sysv_hash_offset_ + 2 * sizeof(uint32_t);
        if (!in_image(image_.size(), buckets_offset,
            static_cast<uint64_t>(bucket_count) * sizeof(uint32_t))) return false;
        const uint64_t chains_offset = buckets_offset
            + static_cast<uint64_t>(bucket_count) * sizeof(uint32_t);
        return in_image(image_.size(), chains_offset,
            static_cast<uint64_t>(chain_count) * sizeof(uint32_t));
    }

    /** Number of chain entries a GNU table can hold inside the image. */
    uint64_t gnu_chain_capacity() const {
        constexpr uint64_t kHeaderWords = 4;
        const std::byte *table = image_.data() + gnu_hash_offset_;
        const uint32_t bucket_count = load_le32(table);
        const uint32_t bloom_count = load_le32(table + 8);
        const uint64_t chains_offset = kHeaderWords * sizeof(uint32_t)
            + static_cast<uint64_t>(bloom_count) * sizeof(uint64_t)
            + static_cast<uint64_t>(bucket_count) * sizeof(uint32_t);
        if (gnu_hash_offset_ + chains_offset >= image_.size()) return 0;
        return (image_.size() - (gnu_hash_offset_ + chains_offset)) / sizeof(uint32_t);
    }

    /**
     * Upper bound on dynamic symbols.
     *
     * Two independent bounds are combined, because neither is sufficient:
     *  - the hash tables only index *exported* symbols, so their count misses
     *    the imports that sit after them in the table (a real system library has
     *    404 PLT imports, none of them hash-indexed);
     *  - the distance to the next table that follows the symbol table bounds the
     *    whole table, exports and imports alike.
     * The larger of the two is used, still capped by the image span.
     */
    uint32_t derive_symbol_count() const {
        uint32_t count = 0;
        if (gnu_hash_offset_ != 0) count = gnu_symbol_count();
        if (count == 0 && sysv_hash_offset_ != 0) count = sysv_symbol_count();
        // Nearest table above the symbol table bounds every entry it can hold.
        uint64_t next_table = UINT64_MAX;
        for (const uint64_t candidate : {strtab_offset_, gnu_hash_offset_,
                 sysv_hash_offset_, jmprel_offset_, rela_offset_, dynamic_offset_}) {
            if (candidate > symtab_offset_ && candidate < next_table) next_table = candidate;
        }
        if (next_table != UINT64_MAX
            && (next_table - symtab_offset_) % kSymBytes == 0) {
            count = std::max<uint32_t>(count,
                static_cast<uint32_t>((next_table - symtab_offset_) / kSymBytes));
        }
        if (count == 0 || count > kMaxSymbols) return 0;
        // The symbol table must be able to hold that many entries inside the image.
        const uint64_t needed = static_cast<uint64_t>(count) * kSymBytes;
        if (add_overflows(symtab_offset_, needed) || symtab_offset_ + needed > image_.size()) {
            return 0;
        }
        return count;
    }

    /** GNU hash header: nbucket, symoffset, bloom_size, bloom_shift, then bloom/buckets/chains. */
    uint32_t gnu_symbol_count() const {
        constexpr uint64_t kHeaderWords = 4;
        if (!gnu_hash_usable()) return 0;
        const std::byte *table = image_.data() + gnu_hash_offset_;
        const uint32_t bucket_count = load_le32(table);
        const uint32_t symbol_offset = load_le32(table + 4);
        const uint32_t bloom_count = load_le32(table + 8);
        const uint64_t buckets_offset = kHeaderWords * sizeof(uint32_t)
            + static_cast<uint64_t>(bloom_count) * sizeof(uint64_t);
        const uint64_t chains_offset = buckets_offset
            + static_cast<uint64_t>(bucket_count) * sizeof(uint32_t);
        const uint64_t capacity = gnu_chain_capacity();
        if (capacity == 0) return 0;
        uint32_t maximum = symbol_offset;
        // Walk budget: a hostile chain table must not be able to spin forever.
        uint64_t steps = 0;
        for (uint32_t i = 0; i < bucket_count; ++i) {
            const uint32_t bucket = load_le32(table + buckets_offset + static_cast<size_t>(i) * 4);
            if (bucket < symbol_offset) continue;
            const uint64_t first = static_cast<uint64_t>(bucket) - symbol_offset;
            if (first >= capacity) continue;
            maximum = std::max(maximum, bucket);
            for (uint64_t index = first;; ++index) {
                if (steps++ > capacity) return 0;
                if (index >= capacity) break;
                const uint64_t entry_offset = gnu_hash_offset_ + chains_offset
                    + index * sizeof(uint32_t);
                const uint32_t chain_hash = load_le32(image_.data() + entry_offset);
                maximum = std::max<uint32_t>(maximum,
                    static_cast<uint32_t>(symbol_offset + index + 1));
                if ((chain_hash & 1U) != 0) break;
            }
        }
        return maximum;
    }

    /** SYSV hash: nbucket, nchain, buckets[], chains[]. nchain is the symbol count. */
    uint32_t sysv_symbol_count() const {
        if (!sysv_hash_usable()) return 0;
        return load_le32(image_.data() + sysv_hash_offset_ + 4); // nchain
    }

    static uint32_t gnu_hash(std::string_view name) {
        uint32_t hash = 5381;
        for (const char character : name) {
            hash = hash * 33 + static_cast<uint8_t>(character);
        }
        return hash;
    }

    static uint32_t sysv_hash(std::string_view name) {
        uint32_t hash = 0;
        for (const char character : name) {
            hash = (hash << 4) + static_cast<uint8_t>(character);
            const uint32_t nibble = hash & 0xf0000000U;
            if (nibble != 0) hash ^= nibble >> 24;
            hash &= ~nibble;
        }
        return hash;
    }

    std::optional<Symbol> find_by_gnu_hash(std::string_view name) const {
        if (symbol_count_ == 0 || name.empty() || !gnu_hash_usable()) return {};
        constexpr uint64_t kHeaderWords = 4;
        const std::byte *table = image_.data() + gnu_hash_offset_;
        const uint32_t bucket_count = load_le32(table);
        const uint32_t symbol_offset = load_le32(table + 4);
        const uint32_t bloom_count = load_le32(table + 8);
        const uint64_t buckets_offset = kHeaderWords * sizeof(uint32_t)
            + static_cast<uint64_t>(bloom_count) * sizeof(uint64_t);
        const uint64_t chains_offset = buckets_offset
            + static_cast<uint64_t>(bucket_count) * sizeof(uint32_t);
        const uint64_t capacity = gnu_chain_capacity();
        if (capacity == 0) return {};
        const uint32_t hash = gnu_hash(name);
        const uint32_t bucket = load_le32(table + buckets_offset
            + static_cast<size_t>(hash % bucket_count) * 4);
        if (bucket < symbol_offset) return {};
        const uint64_t first = static_cast<uint64_t>(bucket) - symbol_offset;
        if (first >= capacity) return {};
        // The chain walk is bounded by the table's own capacity, so a corrupt
        // table can neither read out of bounds nor loop forever.
        for (uint64_t index = first, steps = 0; index < capacity; ++index) {
            if (steps++ > capacity) return {};
            const uint64_t entry_offset = gnu_hash_offset_ + chains_offset
                + index * sizeof(uint32_t);
            const uint32_t chain_hash = load_le32(image_.data() + entry_offset);
            const uint32_t symbol_index =
                static_cast<uint32_t>(symbol_offset + index);
            if ((chain_hash | 1U) == (hash | 1U) && symbol_index < symbol_count_) {
                const auto name_view = symbol_name_at(symbol_index);
                if (name_view
                    && bounded_string_equals(name_view->data(), name_view->size() + 1, name)) {
                    return symbol_at(symbol_index);
                }
            }
            if ((chain_hash & 1U) != 0) break;
        }
        return {};
    }

    std::optional<Symbol> find_by_sysv_hash(std::string_view name) const {
        if (symbol_count_ == 0 || name.empty() || !sysv_hash_usable()) return {};
        const uint32_t bucket_count = load_le32(image_.data() + sysv_hash_offset_);
        const uint32_t chain_count = load_le32(image_.data() + sysv_hash_offset_ + 4);
        const uint64_t buckets_offset = sysv_hash_offset_ + 2 * sizeof(uint32_t);
        const uint64_t chains_offset = buckets_offset
            + static_cast<uint64_t>(bucket_count) * sizeof(uint32_t);
        const uint32_t hash = sysv_hash(name);
        uint32_t index = load_le32(image_.data() + buckets_offset
            + static_cast<size_t>(hash % bucket_count) * 4);
        // A cycle in the chain array would otherwise loop forever: the walk is
        // bounded by the declared chain count.
        for (uint32_t steps = 0; index != 0; ++steps) {
            if (index >= chain_count || index >= symbol_count_) return {};
            if (steps > chain_count) return {};
            const auto name_view = symbol_name_at(index);
            if (name_view
                && bounded_string_equals(name_view->data(), name_view->size() + 1, name)) {
                return symbol_at(index);
            }
            const uint64_t chain_offset = chains_offset
                + static_cast<uint64_t>(index) * sizeof(uint32_t);
            index = load_le32(image_.data() + chain_offset);
        }
        return {};
    }

    std::optional<Symbol> find_by_linear_scan(std::string_view name) const {
        if (symbol_count_ == 0 || name.empty()) return {};
        for (uint32_t index = 1; index < symbol_count_; ++index) {
            const auto name_view = symbol_name_at(index);
            if (!name_view || *name_view != name) continue;
            if (const auto symbol = symbol_at(index)) return symbol;
        }
        return {};
    }
};

} // namespace nhk::elf
