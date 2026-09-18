/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime image layer: runtime mapping inventory, container
 * attribution and mapping generations.
 *
 * Abstracted from the desktop Dart hook's dock_native_runtime.h (this
 * project's own mature implementation). The desktop-specific naming
 * (`libapp.so`, "AOT image") moved out; everything here speaks of images and
 * containers so any native library - a plain system `.so`, an
 * application-installed `.so`, or a library mapped straight out of an APK
 * ZIP - is described by the same vocabulary.
 *
 * Design invariants carried over unchanged:
 *  - A whole-file inventory is kept, never a name-filtered one: Android maps
 *    libraries straight out of APKs (extractNativeLibs=false), the kernel
 *    reports the container path, and sibling libraries share it. Ownership is
 *    decided by the image's own program headers, never by a name.
 *  - Two candidate images claiming one mapping range is ambiguous: fail closed.
 *  - Writable executable mappings never contribute hook targets.
 *  - Bounded budgets everywhere: a pathological inventory must not be able to
 *    grow memory or runtime without bound.
 */
#pragma once

#include "elf_image.h"
#include "nhk_base.h"

#include <algorithm>
#include <array>
#include <concepts>
#include <cstdint>
#include <fstream>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace nhk {

inline constexpr size_t kMaxExecutableMappings = 64;
inline constexpr size_t kMaxExecutableCodeBytes = 256U * 1024U * 1024U;

/** Immutable file location backing one executable byte in this process. */
struct CodeSource {
    uint32_t device_major;
    uint32_t device_minor;
    uint64_t inode;
    uint64_t file_offset;

    bool operator==(const CodeSource &) const = default;
};

struct ExecutableMapping {
    uintptr_t begin;
    uintptr_t end;
    uint64_t file_offset;
    uint32_t device_major;
    uint32_t device_minor;
    uint64_t inode;

    std::optional<CodeSource> source_at(uintptr_t address, size_t bytes) const {
        if (bytes == 0 || address < begin || address >= end || bytes > end - address) return {};
        const uint64_t displacement = address - begin;
        if (add_overflows(file_offset, displacement)) return {};
        return CodeSource{device_major, device_minor, inode, file_offset + displacement};
    }

    bool operator==(const ExecutableMapping &) const = default;
};

/** One independent mapping of a file, including same-inode aliases. */
struct RuntimeGenerationKey {
    uint32_t device_major;
    uint32_t device_minor;
    uint64_t inode;
    uint64_t load_bias;

    bool operator==(const RuntimeGenerationKey &) const = default;
};

inline std::optional<RuntimeGenerationKey> generation_key(
    const ExecutableMapping &mapping) {
    if (mapping.file_offset > mapping.begin) return {};
    return RuntimeGenerationKey{mapping.device_major, mapping.device_minor,
        mapping.inode, mapping.begin - static_cast<uintptr_t>(mapping.file_offset)};
}

inline std::vector<std::vector<ExecutableMapping>> runtime_generations(
    const std::vector<ExecutableMapping> &mappings) {
    std::vector<RuntimeGenerationKey> keys;
    std::vector<std::vector<ExecutableMapping>> result;
    for (const auto &mapping : mappings) {
        const auto key = generation_key(mapping);
        if (!key) continue;
        const auto found = std::ranges::find(keys, *key);
        if (found == keys.end()) {
            keys.push_back(*key);
            result.push_back({mapping});
        } else {
            result[static_cast<size_t>(found - keys.begin())].push_back(mapping);
        }
    }
    return result;
}

/** Drop the kernel's retained-name marker so paths compare across unlink races. */
inline std::string_view strip_deleted(std::string_view path) {
    constexpr std::string_view deleted = " (deleted)";
    if (path.ends_with(deleted)) path.remove_suffix(deleted.size());
    return path;
}

/**
 * One parsed `/proc/self/maps` line naming a file-backed range.
 *
 * The whole file inventory is kept, not just `.so` names: an image may be
 * mapped straight out of its application container, where the kernel reports
 * the container path and the same file also backs unrelated libraries.
 */
struct FileMapping {
    uintptr_t begin;
    uintptr_t end;
    uint64_t file_offset;
    uint32_t device_major;
    uint32_t device_minor;
    uint64_t inode;
    bool executable;
    bool writable;
    std::string path; // Verbatim; may still carry " (deleted)".
};

/** Parse metadata only. Callers must use a fault-reporting read API for bytes. */
inline std::vector<FileMapping> parse_file_mappings(std::istream &maps) {
    // A pathological inventory must not be able to grow without bound.
    constexpr size_t kMaxInventoryLines = 16384;
    std::vector<FileMapping> result;
    result.reserve(256);
    std::string line;
    while (std::getline(maps, line)) {
        if (result.size() >= kMaxInventoryLines) break;
        unsigned long long begin = 0;
        unsigned long long end = 0;
        unsigned long long offset = 0;
        unsigned long long inode = 0;
        unsigned device_major = 0;
        unsigned device_minor = 0;
        char permissions[5]{};
        int path_start = 0;
        if (std::sscanf(line.c_str(), "%llx-%llx %4s %llx %x:%x %llu %n",
                &begin, &end, permissions, &offset, &device_major, &device_minor,
                &inode, &path_start) != 7 || path_start <= 0) {
            continue;
        }
        if (end <= begin || begin > UINTPTR_MAX || end > UINTPTR_MAX) continue;
        const std::string_view path(line.c_str() + path_start,
            line.size() - static_cast<size_t>(path_start));
        // Anonymous, heap and pseudo mappings have no path and are never used.
        if (path.empty() || path.front() != '/') continue;
        result.push_back({static_cast<uintptr_t>(begin), static_cast<uintptr_t>(end),
            offset, device_major, device_minor, inode, permissions[2] == 'x',
            permissions[1] == 'w', std::string(path)});
    }
    return result;
}

/** A file-backed ELF image: either a bare library or one embedded in a container. */
struct ImageContainer {
    std::string path;    // Inventory path with " (deleted)" removed.
    uint64_t view_begin; // Container file offset of the ELF header.
    uint64_t view_end;   // One past the last container offset mapped from that ELF.
};

/** Program-header summary of one embedded ELF64 image. */
struct EmbeddedImage {
    uint64_t view_begin;
    uint64_t view_end;
    bool has_executable = false;
};

/**
 * Decode an ELF64 program header table that starts at the ELF header.
 *
 * `bytes` must begin at the image's ELF header and be long enough to hold the
 * header plus its whole program header table; the caller supplies whatever it
 * could read from the mapped image. Only the loadable segment table is used:
 * it bounds the container view and reports whether an executable segment
 * exists. No address, file offset, Build ID or segment count is pinned.
 */
inline std::optional<EmbeddedImage> parse_embedded_image(
    std::span<const std::byte> bytes, uint64_t view_begin) {
    // The program header table is decoded by the shared decoder (loose mode: a
    // container probe needs the segment list, not ARM64/ET_DYN semantics), so
    // there is exactly one place that knows the layout.
    const auto segments = elf::parse_program_segments(bytes, /*require_arm64_dyn=*/false);
    if (!segments) return {};

    EmbeddedImage image{view_begin, view_begin, false};
    for (const auto &segment : *segments) {
        if (segment.type != elf::kProgramTypeLoad || segment.filesz == 0) continue;
        if (add_overflows(segment.offset, segment.filesz)) return {};
        const uint64_t relative_end = segment.offset + segment.filesz;
        if (relative_end > image.view_end - view_begin) {
            image.view_end = view_begin + relative_end;
        }
        if ((segment.flags & elf::kFlagExecute) != 0) image.has_executable = true;
    }
    if (!image.has_executable || image.view_end <= image.view_begin) return {};
    return image;
}

/**
 * Locate a stored, uncompressed and page-aligned entry inside a ZIP container
 * such as an APK.
 *
 * Android keeps native libraries uncompressed and page aligned inside their
 * APK, so the extracted file may not exist on disk at all and the kernel
 * reports the APK path for every library it backs. Recovering the entry's
 * data offset is what makes the executable ranges attributable to one image.
 *
 * `entry_suffix` matches an entry whose name equals it or ends with "/" + it,
 * so callers can address `libapp.so` under any `lib/<abi>/` directory.
 *
 * Returns `{data_offset, size}` or nothing. Central directories with more than
 * one candidate, compressed entries, entries using a data descriptor, split
 * archives and ZIP64 are all refused: an ambiguous or unverifiable container
 * must never be guessed at.
 */
inline std::optional<std::pair<uint64_t, uint64_t>> zip_stored_entry(
    const std::string &path, std::string_view entry_suffix) {
    constexpr uint32_t kEndOfCentralDirectory = 0x06054b50U;
    constexpr uint32_t kCentralHeader = 0x02014b50U;
    constexpr uint32_t kLocalHeader = 0x04034b50U;
    constexpr uint32_t kUnknown32 = 0xffffffffU;
    constexpr uint16_t kUnknown16 = 0xffffU;
    constexpr size_t kEndBytes = 22;
    constexpr size_t kCommentBytes = 0xffff;
    constexpr size_t kCentralHeaderBytes = 46;
    constexpr size_t kLocalHeaderBytes = 30;
    constexpr uint64_t kMaxCentralDirectoryBytes = 32U * 1024U * 1024U;
    std::ifstream file(path, std::ios::binary);
    if (!file) return {};
    file.seekg(0, std::ios::end);
    const std::streamoff length = file.tellg();
    if (length < static_cast<std::streamoff>(kEndBytes)) return {};
    const uint64_t total = static_cast<uint64_t>(length);
    const uint64_t tail_bytes = std::min<uint64_t>(total, kEndBytes + kCommentBytes);
    std::vector<std::byte> tail(static_cast<size_t>(tail_bytes));
    file.seekg(static_cast<std::streamoff>(total - tail_bytes));
    file.read(reinterpret_cast<char *>(tail.data()),
        static_cast<std::streamsize>(tail.size()));
    if (static_cast<uint64_t>(file.gcount()) != tail_bytes) return {};

    // Scan backwards: a comment may itself contain the end signature, so only a
    // record whose declared comment length reaches the end of the file counts.
    std::optional<size_t> end_record;
    for (size_t at = tail.size() - kEndBytes + 1; at-- > 0;) {
        if (load_le32(tail.data() + at) != kEndOfCentralDirectory) continue;
        if (at + kEndBytes + load_le16(tail.data() + at + 20) != tail.size()) continue;
        end_record = at;
        break;
    }
    if (!end_record) return {};
    const uint16_t disk = load_le16(tail.data() + *end_record + 4);
    const uint16_t disk_entries = load_le16(tail.data() + *end_record + 8);
    const uint16_t entries = load_le16(tail.data() + *end_record + 10);
    const uint32_t directory_bytes = load_le32(tail.data() + *end_record + 12);
    const uint32_t directory_offset = load_le32(tail.data() + *end_record + 16);
    if (disk != 0 || disk_entries != entries || entries == 0
        || entries == kUnknown16 || directory_bytes == kUnknown32
        || directory_offset == kUnknown32) return {};
    if (directory_bytes > kMaxCentralDirectoryBytes || directory_offset > total
        || directory_bytes > total - directory_offset) return {};

    std::vector<std::byte> directory(directory_bytes);
    file.clear();
    file.seekg(static_cast<std::streamoff>(directory_offset));
    file.read(reinterpret_cast<char *>(directory.data()),
        static_cast<std::streamsize>(directory.size()));
    if (static_cast<uint64_t>(file.gcount()) != directory_bytes) return {};

    const std::string nested = std::string("/").append(entry_suffix);
    const auto entry_matches = [&](std::string_view name) {
        return name == entry_suffix || name.ends_with(nested);
    };

    std::optional<std::pair<uint64_t, uint64_t>> found;
    size_t at = 0;
    for (uint16_t index = 0; index < entries; ++index) {
        if (at + kCentralHeaderBytes > directory.size()
            || load_le32(directory.data() + at) != kCentralHeader) return {};
        const uint16_t flags = load_le16(directory.data() + at + 8);
        const uint16_t method = load_le16(directory.data() + at + 10);
        const uint32_t compressed = load_le32(directory.data() + at + 20);
        const uint32_t uncompressed = load_le32(directory.data() + at + 24);
        const uint16_t name_bytes = load_le16(directory.data() + at + 28);
        const uint16_t extra_bytes = load_le16(directory.data() + at + 30);
        const uint16_t comment_bytes = load_le16(directory.data() + at + 32);
        const uint32_t local_offset = load_le32(directory.data() + at + 42);
        if (name_bytes + extra_bytes + comment_bytes
            > directory.size() - at - kCentralHeaderBytes) return {};
        const std::string_view name(
            reinterpret_cast<const char *>(directory.data() + at + kCentralHeaderBytes),
            name_bytes);
        at += kCentralHeaderBytes + name_bytes + extra_bytes + comment_bytes;
        if (!entry_matches(name)) continue;
        // Only an uncompressed, descriptor-free entry is mapped in place.
        if (method != 0 || (flags & 0x08U) != 0 || compressed != uncompressed
            || compressed == 0 || compressed == kUnknown32
            || local_offset == kUnknown32) return {};
        std::array<std::byte, kLocalHeaderBytes> local{};
        file.clear();
        file.seekg(static_cast<std::streamoff>(local_offset));
        file.read(reinterpret_cast<char *>(local.data()),
            static_cast<std::streamsize>(local.size()));
        if (static_cast<uint64_t>(file.gcount()) != local.size()
            || load_le32(local.data()) != kLocalHeader) return {};
        // The local header carries its own extra-field length, which does not
        // have to match the central record's.
        const uint16_t local_name = load_le16(local.data() + 26);
        const uint16_t local_extra = load_le16(local.data() + 28);
        if (local_name != name_bytes) return {};
        const uint64_t data_offset = static_cast<uint64_t>(local_offset)
            + kLocalHeaderBytes + local_name + local_extra;
        if (data_offset > total || compressed > total - data_offset) return {};
        if (found) return {};
        found = std::pair<uint64_t, uint64_t>{data_offset, compressed};
    }
    return found;
}

/**
 * Select the executable container ranges owned by one discovered image.
 *
 * A range is accepted only when its recorded file identity resolves inside the
 * image's own view, so sibling libraries sharing the container file can never
 * contribute code. Offsets are rebased on the view start, keeping
 * [CodeSource::file_offset] stable across reloads of the same image.
 */
inline std::vector<ExecutableMapping> owned_image_mappings(
    const std::vector<FileMapping> &mappings,
    const std::vector<ImageContainer> &containers) {
    std::vector<ExecutableMapping> result;
    size_t total = 0;
    for (const auto &mapping : mappings) {
        if (!mapping.executable) continue;
        const std::string_view path = strip_deleted(mapping.path);
        const ImageContainer *owner = nullptr;
        for (const auto &container : containers) {
            if (container.path != path) continue;
            if (mapping.file_offset < container.view_begin
                || mapping.file_offset >= container.view_end) continue;
            // Two candidate images claiming one range is ambiguous: fail closed.
            if (owner != nullptr) return {};
            owner = &container;
        }
        if (owner == nullptr) continue;
        const uint64_t length = mapping.end - mapping.begin;
        // Hook targets must never be discovered from writable executable memory.
        // Treat a malformed owned mapping as a bad snapshot instead of silently
        // resolving against a partial generation.
        if (mapping.writable || mapping.inode == 0
            || mapping.begin % alignof(uint32_t) != 0
            || length % sizeof(uint32_t) != 0
            || length > kMaxExecutableCodeBytes
            || length > kMaxExecutableCodeBytes - total
            || result.size() >= kMaxExecutableMappings) return {};
        total += static_cast<size_t>(length);
        result.push_back({mapping.begin, mapping.end,
            mapping.file_offset - owner->view_begin, mapping.device_major,
            mapping.device_minor, mapping.inode});
    }
    std::ranges::sort(result, {}, &ExecutableMapping::begin);
    for (size_t i = 1; i < result.size(); ++i) {
        if (result[i - 1].end > result[i].begin) return {};
    }
    return result;
}

/**
 * Inventory of bare-file image generations only, for a caller-supplied path
 * predicate. Kept for artifact-independent reasoning and host tests; the
 * device path also discovers images embedded in an application container.
 */
template<typename PathPredicate>
requires std::invocable<PathPredicate, std::string_view>
inline std::vector<ExecutableMapping> bare_image_mappings(
    std::istream &maps, PathPredicate is_image_path) {
    const auto mappings = parse_file_mappings(maps);
    std::vector<ImageContainer> containers;
    for (const auto &mapping : mappings) {
        const std::string_view path = strip_deleted(mapping.path);
        if (!is_image_path(path)) continue;
        const bool known = std::ranges::any_of(containers,
            [&](const ImageContainer &container) { return container.path == path; });
        if (!known) containers.push_back({std::string(path), 0, UINT64_MAX});
    }
    return owned_image_mappings(mappings, containers);
}

/**
 * One mapped range of an image, expressed in the image's virtual address space.
 */
struct MappedRange {
    uint64_t vaddr = 0;   // Virtual address of the first byte of the range.
    uintptr_t begin = 0;  // Runtime address of that same byte.
    uint64_t bytes = 0;   // Mapped length.
};

/**
 * A whole image as it is mapped, described by virtual address.
 *
 * `load_base` is the runtime address of virtual address 0, `needed_vaddr` is
 * how far a snapshot must reach to cover the image (the highest segment end,
 * including a dynamic segment that sits far above the first pages), and
 * `ranges` are the mapped ranges sorted by virtual address.
 */
struct ImageView {
    uint64_t load_base = 0;
    uint64_t needed_vaddr = 0;
    std::vector<MappedRange> ranges;
};

/**
 * Build an image view from `mappings` and an already-decoded program header
 * table.
 *
 * This is the only correct way to place a runtime mapping in the image's
 * virtual address space: a load segment's `p_offset` and `p_vaddr` routinely
 * differ by a segment-specific amount (a system library measured 0x0 / 0x10000
 * / 0x20000 / 0x30000 across four PT_LOADs), so the file→virtual relation has
 * to come from the program headers, never from assuming `begin - file_offset`
 * is constant.
 *
 * Ownership is still verified, just correctly: every mapping of `path` must
 * fall inside some load segment's *file* range, and every mapping must agree on
 * one load bias (`begin - vaddr`). A mapping that no segment claims, two
 * segments claiming one offset, disagreeing biases, or overlapping virtual
 * ranges all fail closed.
 */
inline std::optional<ImageView> image_view_from_segments_in_window(
    const std::vector<FileMapping> &mappings, std::string_view path,
    std::span<const elf::ProgramSegment> segments, uint64_t view_begin,
    uint64_t view_end) {
    ImageView view;
    uint64_t needed = 0;
    for (const auto &segment : segments) {
        if (segment.type != elf::kProgramTypeLoad && segment.type != elf::kProgramTypeDynamic) {
            continue;
        }
        needed = std::max<uint64_t>(needed, segment.vaddr + segment.memsz);
    }
    if (needed == 0) return {};
    view.needed_vaddr = needed;

    const uint64_t page = host_page_size();
    // The kernel maps a load segment page-aligned on both sides, so a mapping
    // can be claimed by two segments at once (the last page of one and the
    // first page of the next often share a file offset). Ownership is therefore
    // decided by the *load bias* the mapping implies, not by the file offset
    // alone: every mapping of one image must imply the same bias.
    struct Candidate {
        uint64_t vaddr;
        uint64_t bias;
        uint64_t bytes;
        uint64_t file_offset;
    };
    std::vector<std::vector<Candidate>> rows;
    for (const auto &mapping : mappings) {
        if (strip_deleted(mapping.path) != path) continue;
        // A container-backed image shares its path with every sibling library
        // stored in the same APK, so the path alone cannot select its mappings -
        // the *view window* (the stored entry's page range inside the container)
        // is what does. A bare file has the trivial window [0, UINT64_MAX) and
        // its reported offsets already are image offsets, so the two cases agree
        // once the relative offset is taken first.
        if (mapping.file_offset < view_begin || mapping.file_offset >= view_end) continue;
        const uint64_t relative = mapping.file_offset - view_begin;
        const uint64_t length = mapping.end - mapping.begin;
        if (length == 0 || length > kMaxExecutableCodeBytes) return {};
        std::vector<Candidate> row;
        for (const auto &segment : segments) {
            if (segment.filesz == 0) continue;
            const uint64_t segment_page = page_down(segment.offset);
            const uint64_t segment_page_end = page_up(segment.offset + segment.filesz);
            if (relative < segment_page || relative >= segment_page_end) {
                continue;
            }
            const uint64_t delta = relative - segment_page;
            if (delta % page != 0) continue; // A mapping starts on a page boundary.
            const uint64_t vaddr = page_down(segment.vaddr) + delta;
            if (add_overflows(vaddr, length) || mapping.begin < vaddr) continue;
            row.push_back({vaddr, mapping.begin - vaddr, length, relative});
        }
        if (row.empty()) return {};
        rows.push_back(std::move(row));
    }
    if (rows.empty()) return {};

    // Exactly one load bias must be consistent across every mapping.
    std::optional<uint64_t> agreed;
    for (const auto &candidate : rows.front()) {
        const uint64_t bias = candidate.bias;
        bool everywhere = true;
        for (size_t i = 1; i < rows.size() && everywhere; ++i) {
            const bool present = std::ranges::any_of(rows[i],
                [&](const Candidate &other) { return other.bias == bias; });
            everywhere = present;
        }
        if (!everywhere) continue;
        if (agreed && *agreed != bias) return {};
        agreed = bias;
    }
    if (!agreed) return {};
    view.load_base = *agreed;

    // Pick the segment each mapping belongs to and merge neighbouring pages:
    // a segment is reported as a handful of maps, so keeping one range per page
    // would blow past every range budget for no benefit.
    //
    // Once the bias is known, `vaddr = begin - bias` is determined - the
    // segment only *confirms* the mapping belongs to this image. Two segments
    // can legitimately confirm the same mapping (page-aligned segment ends
    // overlap), which is not an ambiguity.
    std::vector<MappedRange> chosen;
    for (const auto &row : rows) {
        // Any candidate carrying the agreed bias yields the same virtual
        // address (`begin - bias`), so the first match is authoritative.
        const Candidate *match = nullptr;
        for (const auto &candidate : row) {
            if (candidate.bias != view.load_base) continue;
            match = &candidate;
            break;
        }
        if (match == nullptr) return {};
        // RELRO re-protection surfaces as a second map of the same address
        // range; it is the same bytes, so keep the first occurrence.
        const bool duplicate = std::ranges::any_of(chosen,
            [&](const MappedRange &range) { return range.vaddr == match->vaddr; });
        if (duplicate) continue;
        chosen.push_back({match->vaddr,
            static_cast<uintptr_t>(view.load_base + match->vaddr), match->bytes});
    }
    if (chosen.empty()) return {};

    std::ranges::sort(chosen, {}, &MappedRange::vaddr);
    for (const auto &range : chosen) {
        if (!view.ranges.empty()) {
            auto &previous = view.ranges.back();
            if (previous.vaddr + previous.bytes == range.vaddr
                && static_cast<uintptr_t>(previous.begin + previous.bytes) == range.begin) {
                previous.bytes += range.bytes; // Contiguous: merge into one range.
                continue;
            }
            if (previous.vaddr + previous.bytes > range.vaddr) {
                return {};
            }
        }
        if (view.ranges.size() >= kMaxExecutableMappings) return {};
        view.ranges.push_back(range);
    }
    return view;
}

/**
 * Convenience overload for a bare file image, where every mapping of `path`
 * belongs to the image and its reported file offsets already are image offsets.
 */
inline std::optional<ImageView> image_view_from_segments(
    const std::vector<FileMapping> &mappings, std::string_view path,
    std::span<const elf::ProgramSegment> segments) {
    return image_view_from_segments_in_window(
        mappings, path, segments, 0, UINT64_MAX);
}

inline std::optional<CodeSource> source_at(
    const std::vector<ExecutableMapping> &mappings, uintptr_t address, size_t bytes) {
    std::optional<CodeSource> result;
    for (const auto &mapping : mappings) {
        const auto candidate = mapping.source_at(address, bytes);
        if (!candidate) continue;
        if (result) return {}; // Overlapping executable mappings are ambiguous.
        result = candidate;
    }
    return result;
}

enum class MappingState { same, absent, replaced, mixed };

template<size_t N>
inline MappingState mapping_state(const std::vector<ExecutableMapping> &mappings,
    const std::array<uintptr_t, N> &addresses,
    const std::array<CodeSource, N> &sources, size_t bytes) {
    size_t same = 0;
    size_t absent = 0;
    size_t replaced = 0;
    for (size_t i = 0; i < N; ++i) {
        const auto current = source_at(mappings, addresses[i], bytes);
        if (!current) ++absent;
        else if (*current == sources[i]) ++same;
        else ++replaced;
    }
    if (same == N) return MappingState::same;
    if (absent == N) return MappingState::absent;
    if (replaced == N) return MappingState::replaced;
    return MappingState::mixed;
}

template<size_t N>
inline bool sources_for(const std::vector<ExecutableMapping> &mappings,
    const std::array<uintptr_t, N> &addresses, size_t bytes,
    std::array<CodeSource, N> &sources) {
    for (size_t i = 0; i < N; ++i) {
        const auto source = source_at(mappings, addresses[i], bytes);
        if (!source) return false;
        sources[i] = *source;
    }
    return true;
}

} // namespace nhk
