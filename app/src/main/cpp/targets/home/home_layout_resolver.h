/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Runtime resolver for the HyperOS 4 launcher layout targets.
 *
 * Everything here is discovered at runtime, on purpose: no code offset, no image base and no
 * version constant is written down. The launcher's Dart and Rust images are rebuilt by every OTA,
 * so an offset captured during development is only ever calibration data - what has to survive is
 * the *relationship* between a log literal and the code that references it.
 *
 * Two discoveries, because this ROM needs both:
 *
 *   - Structure. A readable mapping that the mapping table attributes to a file offset lets an
 *     image base be derived; if a valid ELF64/AArch64 image starts there, the image is known by
 *     its build ID and every range belonging to it is known from the same table. This identifies
 *     a named system library without trusting its name.
 *   - Content. The launcher's own images are mapped anonymously - no name, no ELF header at the
 *     mapping - so structure alone cannot see them. Their literals still are what they are, so
 *     the needle itself is the identifier, and the range that contains it is reported as found.
 *
 * Both then take the same last three steps, within whatever set of ranges the discovery produced:
 *
 *   3. xref: scan the executable ranges for the `adrp` + `add` pair that materialises the needle's
 *      address, which lands inside the owning function;
 *   4. use the ELF's unwind-function index to recover the exact entry;
 *   5. report a file-relative offset when the range has a file, and the raw range-relative offset
 *      when it does not.
 *
 * Every read goes through probe::Guard. This code runs inside the launcher, and a mapping the
 * table calls readable can still fail to fault in: the boundary turns one unreadable page into a
 * skipped page instead of a dead desktop. That is not hypothetical - an unguarded scan of this
 * process died on SIGBUS/BUS_ADRERR reading a page the table advertised as readable.
 */
#pragma once

#include "probe_guard.h"
#include "home_layout_unwind.h"

#include "nativehook/arm64_decode.h"
#include "nativehook/elf_image.h"

#include <android/log.h>
#include <dlfcn.h>
#include <sys/stat.h>

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace home_layout {

inline constexpr const char *kLogTag = "HyperCeiler.HomeLayout";

/** Enough of an image to hold the ELF header together with its whole program header table. */
inline constexpr size_t kImageHeadBytes = 16 * 1024;

/** The longest path field a mapping line can carry, buffer included; one byte left for the NUL. */
inline constexpr size_t kMappingPathBytes = 512;

/** Upper bound on one phase's literal scan: a pathological process must not make this unbounded. */
inline constexpr uint64_t kScanBudgetBytes = 384ull << 20;

/**
 * The largest unowned range the literal scan will walk.
 *
 * The process reserves far more address space than it fills - tens of gigabytes of never-touched
 * anonymous range - and reading one of those reservations would cost the whole budget to learn
 * nothing. A range bigger than any image is not where a literal table lives.
 */
inline constexpr uint64_t kContentRangeMaxBytes = 64ull << 20;

/** How much of the literal's neighbourhood is shown, so the log says what kind of table it is in. */
inline constexpr size_t kContextBefore = 32;
inline constexpr size_t kContextAfter = 16;

/** One readable mapping, exactly as the mapping table reports it. */
struct Mapping {
    uintptr_t begin = 0;
    uintptr_t end = 0;
    uint64_t file_offset = 0;
    uint64_t inode = 0;
    bool executable = false;
    bool writable = false;
    std::string path;

    [[nodiscard]] uint64_t bytes() const { return end - begin; }
};

/** A mapped ELF image: its load bias, its program headers, and the mappings derived from it. */
struct LayoutImage {
    uintptr_t base = 0;
    std::string path;
    uint64_t inode = 0;
    std::string build_id;
    std::vector<nhk::elf::ProgramSegment> programs;
    std::vector<size_t> ranges;  ///< Indices into the mapping list, not copies of it.
};

/** The mappings a resolution may touch, split by what they can hold. */
struct RangeSet {
    std::vector<size_t> data;  ///< Readable, non-executable: literals live here.
    std::vector<size_t> code;  ///< Executable: references and code live here.
};

/** Everything one phase of the literal scan cost, so the log can be read as a measurement. */
struct ScanStats {
    uint64_t bytes = 0;
    size_t skipped_pages = 0;
    bool budget_reached = false;
};

/**
 * Every readable mapping of this process.
 *
 * The permission field is read rather than assumed: a non-readable range would fault the scan, and
 * every later step treats this list as candidates, never as truth. The file offset comes from the
 * same line and is what lets a mapping be attributed to the image it belongs to.
 */
inline std::vector<Mapping> readable_mappings() {
    std::ifstream maps{"/proc/self/maps"};
    if (!maps) return {};

    std::vector<Mapping> mappings;
    std::string line;
    while (std::getline(maps, line)) {
        unsigned long long begin = 0;
        unsigned long long end = 0;
        unsigned long long file_offset = 0;
        unsigned long long inode = 0;
        char perms[8] = {};
        char path[kMappingPathBytes] = {};
        const int fields = std::sscanf(line.c_str(), "%llx-%llx %7s %llx %*s %llu %511s",
            &begin, &end, perms, &file_offset, &inode, path);
        if (fields < 5 || end <= begin) continue;  // 5 = the line carries no path at all
        if (perms[0] != 'r') continue;

        Mapping mapping;
        mapping.begin = static_cast<uintptr_t>(begin);
        mapping.end = static_cast<uintptr_t>(end);
        mapping.file_offset = file_offset;
        mapping.inode = inode;
        mapping.executable = perms[2] == 'x';
        mapping.writable = perms[1] == 'w';
        mapping.path = std::string(path);
        mappings.push_back(std::move(mapping));
    }
    return mappings;
}

inline RangeSet ranges_of(const std::vector<Mapping> &mappings, const std::vector<size_t> &indices) {
    RangeSet ranges;
    for (const size_t index : indices) {
        if (mappings[index].executable) {
            ranges.code.push_back(index);
        } else {
            ranges.data.push_back(index);
        }
    }
    return ranges;
}

/** Enough of a mapping's identity to read a log line without a second lookup. */
inline std::string mapping_label(const std::vector<Mapping> &mappings, size_t index) {
    const Mapping &mapping = mappings[index];
    char span[64] = {};
    std::snprintf(span, sizeof(span), " [%p+0x%llx]", reinterpret_cast<void *>(mapping.begin),
        static_cast<unsigned long long>(mapping.bytes()));
    return (mapping.path.empty() ? std::string("<anonymous>") : mapping.path) + span;
}

/** The image this resolver itself lives in. */
struct SelfImage {
    uintptr_t base = 0;
    std::string path;
    uint64_t inode = 0;

    /** Whether a mapping belongs to this image, by any of the three things that identify it. */
    [[nodiscard]] bool owns(const Mapping &mapping) const {
        if (base != 0 && mapping.begin == base) return true;
        if (inode != 0 && mapping.inode == inode) return true;
        return !path.empty() && mapping.path == path;
    }
};

/**
 * Identify the image this resolver runs inside.
 *
 * It has to be excluded, and not out of tidiness: every needle below is a string literal in this
 * file, so this image is the one place all of them are guaranteed to be found, and a match here
 * would resolve a reference in this module's own code while looking like a success. The identity
 * comes from the runtime (`dladdr` on a function of this file) and from the file system (the inode
 * of that path), so nothing here is written down as a name.
 */
inline SelfImage own_image() {
    SelfImage self;
    Dl_info info{};
    if (::dladdr(reinterpret_cast<const void *>(&own_image), &info) == 0) return self;
    self.base = reinterpret_cast<uintptr_t>(info.dli_fbase);
    if (info.dli_fname != nullptr) self.path = info.dli_fname;
    struct stat status {};
    if (!self.path.empty() && ::stat(self.path.c_str(), &status) == 0) {
        self.inode = static_cast<uint64_t>(status.st_ino);
    }
    return self;
}

/** `bytes` as lower-case hex. */
inline std::string to_hex(const std::byte *bytes, size_t count) {
    static constexpr char kDigits[] = "0123456789abcdef";
    std::string text;
    text.reserve(count * 2);
    for (size_t index = 0; index < count; ++index) {
        const auto value = static_cast<unsigned>(bytes[index]);
        text.push_back(kDigits[value >> 4]);
        text.push_back(kDigits[value & 0xf]);
    }
    return text;
}

/** Printable rendering of `bytes`; anything else becomes '.', so a table's shape is visible. */
inline std::string to_text(const std::byte *bytes, size_t count) {
    std::string text;
    text.reserve(count);
    for (size_t index = 0; index < count; ++index) {
        const auto value = static_cast<unsigned char>(bytes[index]);
        text.push_back(value >= 0x20 && value < 0x7f ? static_cast<char>(value) : '.');
    }
    return text;
}

/**
 * The image's GNU build ID.
 *
 * Worth the parse: it is the one field that still identifies an image whose name was never written
 * down, and it can be matched against the same file on disk. Absence is not an error - an image
 * may simply carry no notes.
 */
inline std::string read_build_id(const probe::Guard &guard, uintptr_t base,
    const std::vector<nhk::elf::ProgramSegment> &programs) {
    constexpr uint32_t kProgramTypeNote = 4;
    constexpr uint32_t kNoteTypeBuildId = 3;
    constexpr size_t kMaxNoteBytes = 4 * 1024;
    constexpr size_t kNoteHeadBytes = 12;

    for (const nhk::elf::ProgramSegment &segment : programs) {
        if (segment.type != kProgramTypeNote || segment.filesz == 0) continue;
        const size_t bytes = static_cast<size_t>(std::min<uint64_t>(segment.filesz, kMaxNoteBytes));
        std::vector<std::byte> notes(bytes);
        if (!guard.read(reinterpret_cast<const void *>(base + segment.vaddr), notes.data(), bytes)) {
            continue;
        }

        // Note layout: namesz, descsz, type, then the name and the descriptor, each padded to 4.
        size_t at = 0;
        while (at + kNoteHeadBytes <= bytes) {
            const uint32_t namesz = nhk::load_le32(notes.data() + at);
            const uint32_t descsz = nhk::load_le32(notes.data() + at + 4);
            const uint32_t type = nhk::load_le32(notes.data() + at + 8);
            const size_t name_at = at + kNoteHeadBytes;
            const size_t desc_at = name_at + ((namesz + 3u) & ~3u);
            if (desc_at + descsz > bytes) break;
            if (type == kNoteTypeBuildId && namesz == 4
                && std::memcmp(notes.data() + name_at, "GNU", 4) == 0) {
                return to_hex(notes.data() + desc_at, descsz);
            }
            at = desc_at + ((descsz + 3u) & ~3u);
        }
    }
    return {};
}

/**
 * Validate an image base and describe what is there, or nothing.
 *
 * The decoding is the shared ELF layer's, which already checks the magic, the class, the machine
 * and the segment table's own bounds, so a range that merely starts with `\x7fELF` is not mistaken
 * for an image. Only the head is read; everything after it is reached through the program headers.
 */
inline std::optional<LayoutImage> probe_image(const probe::Guard &guard, uintptr_t base,
    const Mapping &from) {
    std::byte head[kImageHeadBytes];
    if (!guard.read(reinterpret_cast<const void *>(base), head, sizeof(head))) return std::nullopt;

    const std::optional<std::vector<nhk::elf::ProgramSegment>> programs =
        nhk::elf::parse_program_segments(std::span<const std::byte>(head, sizeof(head)));
    if (!programs) return std::nullopt;

    LayoutImage image;
    image.base = base;
    image.path = from.path;
    image.inode = from.inode;
    image.programs = *programs;
    image.build_id = read_build_id(guard, base, image.programs);
    return image;
}

/**
 * The image's own byte extent, read from its program headers.
 *
 * Needed because a segment mapped out of an application container reports a file offset that is
 * relative to the *container*, not to the image: such a segment can never derive its own base, and
 * the only way to attribute it is to notice that its address falls inside an image already found.
 */
inline uint64_t image_span(const LayoutImage &image) {
    uint64_t span = 0;
    for (const nhk::elf::ProgramSegment &segment : image.programs) {
        if (segment.type != nhk::elf::kProgramTypeLoad) continue;
        span = std::max(span, nhk::page_up(segment.vaddr + segment.memsz));
    }
    return span;
}

/**
 * Group the readable mappings into the images they belong to.
 *
 * A mapping line offers two candidate bases and neither is assumed: the mapping may start where the
 * image starts (a container entry, whose file offset belongs to the container rather than to the
 * image), or it may be a whole file whose reported offset is exactly the delta between the address
 * and the image's first byte. The ELF header decides which - a candidate that holds no valid image
 * is simply not one.
 *
 * A segment that can name neither still gets attributed afterwards, by falling inside the extent of
 * an image already found and carrying the same path. Mappings that stay unowned are exactly the
 * ones with no image to belong to, which is what leaves them to the content phase rather than
 * dropping them: the launcher's images may be among them.
 */
inline std::vector<LayoutImage> layout_images(const probe::Guard &guard,
    const std::vector<Mapping> &mappings, std::vector<bool> &owned, const SelfImage &self) {
    std::vector<LayoutImage> images;
    owned.assign(mappings.size(), false);

    for (size_t index = 0; index < mappings.size(); ++index) {
        const Mapping &mapping = mappings[index];

        // The resolver's own image is taken out of play before anything else looks at it, and
        // marked as spoken for so the content phase cannot pick it up either.
        if (self.owns(mapping)) {
            owned[index] = true;
            continue;
        }

        const uintptr_t from_offset = mapping.file_offset <= mapping.begin
            ? mapping.begin - mapping.file_offset
            : 0;
        for (const uintptr_t base : {mapping.begin, from_offset}) {
            if (base == 0) continue;

            size_t known = images.size();
            for (size_t at = 0; at < images.size(); ++at) {
                if (images[at].base == base) known = at;
            }
            if (known != images.size()) {
                images[known].ranges.push_back(index);
                owned[index] = true;
                break;
            }

            std::optional<LayoutImage> image = probe_image(guard, base, mapping);
            if (!image) continue;
            image->ranges.push_back(index);
            owned[index] = true;
            images.push_back(std::move(*image));
            break;
        }
    }

    // Second pass: the segments that could not name their own base, placed by containment.
    for (size_t index = 0; index < mappings.size(); ++index) {
        if (owned[index]) continue;
        const Mapping &mapping = mappings[index];
        for (const LayoutImage &image : images) {
            if (image.path != mapping.path || mapping.begin < image.base) continue;
            if (mapping.begin - image.base >= image_span(image)) continue;
            owned[index] = true;
            break;
        }
    }
    for (size_t index = 0; index < mappings.size(); ++index) {
        if (!owned[index]) continue;
        const Mapping &mapping = mappings[index];
        for (LayoutImage &image : images) {
            if (image.path != mapping.path || mapping.begin < image.base) continue;
            if (mapping.begin - image.base >= image_span(image)) continue;
            if (std::find(image.ranges.begin(), image.ranges.end(), index) != image.ranges.end()) {
                continue;
            }
            image.ranges.push_back(index);
            break;
        }
    }
    return images;
}

/** Where a needle was found. */
struct NeedleHit {
    size_t needle = 0;
    size_t mapping = 0;
    uintptr_t address = 0;
};

/**
 * A few unowned ranges worth naming in the log: the read-only ones first, then the writable ones.
 *
 * The list is capped because the answer to "which ranges does no image claim" is long and its
 * interesting part is at the front - a literal can only be in read-only data of something the
 * structure phase could not see.
 */
inline std::vector<size_t> unowned_selection(const std::vector<Mapping> &mappings,
    const std::vector<bool> &owned, size_t limit = 12) {
    std::vector<size_t> chosen;
    for (const bool writable_pass : {false, true}) {
        for (size_t index = 0; index < mappings.size() && chosen.size() < limit; ++index) {
            if (!owned[index] && mappings[index].writable == writable_pass) chosen.push_back(index);
        }
    }
    return chosen;
}

/**
 * Mappings whose name mentions the launcher, whether or not they validated as an image.
 *
 * This is the diagnostic for the one question the rest of the file cannot answer by itself: if the
 * launcher's own images are mapped at all, what are they called and where are they? A mapping that
 * appears here but owns no image is a mapping whose derived base did not hold a header - worth
 * seeing rather than inferring.
 */
inline std::vector<size_t> launcher_named(const std::vector<Mapping> &mappings, size_t limit = 12) {
    std::vector<size_t> chosen;
    for (size_t index = 0; index < mappings.size() && chosen.size() < limit; ++index) {
        const std::string &path = mappings[index].path;
        if (path.find("miui.home") != std::string::npos
            || path.find("libapp") != std::string::npos
            || path.find("flutter") != std::string::npos) {
            chosen.push_back(index);
        }
    }
    return chosen;
}

/**
 * One page-by-page pass over a set of mappings, looking for any of the needles.
 *
 * Code is skipped where the caller does not ask for it: a literal is never an instruction. Each
 * page is armed one needle longer than the page itself, so a literal straddling a page boundary is
 * still readable while the scan never looks past what it was allowed to touch. A page that faults
 * costs that page and nothing else - which is the entire reason the boundary exists.
 */
inline std::vector<NeedleHit> find_needles(const probe::Guard &guard,
    const std::vector<Mapping> &mappings, const std::vector<size_t> &ranges,
    const std::vector<std::string_view> &needles, const std::vector<bool> &wanted, ScanStats &stats) {
    std::vector<NeedleHit> hits;
    std::vector<bool> found(needles.size(), false);

    size_t longest = 0;
    for (const std::string_view needle : needles) longest = std::max(longest, needle.size());
    std::vector<std::vector<size_t>> by_first_byte(256);
    for (size_t index = 0; index < needles.size(); ++index) {
        if (wanted[index] && !needles[index].empty()) {
            by_first_byte[static_cast<unsigned char>(needles[index].front())].push_back(index);
        }
    }

    const uint64_t page_size = nhk::host_page_size();
    for (const size_t index : ranges) {
        const Mapping &mapping = mappings[index];
        for (uintptr_t at = mapping.begin; at < mapping.end; at += page_size) {
            if (stats.bytes >= kScanBudgetBytes) {
                stats.budget_reached = true;
                return hits;
            }
            const size_t size =
                static_cast<size_t>(std::min<uintptr_t>(at + page_size, mapping.end) - at);
            const auto *page = reinterpret_cast<const unsigned char *>(at);
            const bool readable = guard.visit(reinterpret_cast<const void *>(at), size + longest,
                [&] {
                    for (size_t offset = 0; offset < size; ++offset) {
                        const std::vector<size_t> &candidates = by_first_byte[page[offset]];
                        for (const size_t needle_index : candidates) {
                            if (found[needle_index]) continue;
                            const std::string_view needle = needles[needle_index];
                            if (offset + needle.size() > size + longest) continue;
                            if (std::memcmp(page + offset, needle.data(), needle.size()) != 0) {
                                continue;
                            }
                            found[needle_index] = true;
                            hits.push_back(NeedleHit{needle_index, index, at + offset});
                        }
                    }
                });
            if (!readable) ++stats.skipped_pages;
            stats.bytes += size;
        }
    }
    return hits;
}

/** Decode the two instruction forms an AArch64 string reference is built from. */
struct Reference {
    uintptr_t site = 0;
    uintptr_t target = 0;
};

/**
 * Find the instruction that materialises `want` inside the given code ranges.
 *
 * Both halves of the idiom are checked: an `adrp` that page-aligns the address, then an `add` that
 * contributes the low bits. The page register is dropped at a return or a call, so a page the
 * linker materialised long before cannot be credited with a reference it never made.
 */
inline std::vector<Reference> xref(const probe::Guard &guard, const std::vector<Mapping> &mappings,
    const std::vector<size_t> &code_ranges, uintptr_t want, uintptr_t tolerance = 8) {
    std::vector<Reference> found;
    const uint64_t page_size = nhk::host_page_size();

    for (const size_t mapping_index : code_ranges) {
        const Mapping &mapping = mappings[mapping_index];
        for (uintptr_t at = mapping.begin; at < mapping.end; at += page_size) {
            const size_t size =
                static_cast<size_t>(std::min<uintptr_t>(at + page_size, mapping.end) - at);
            guard.visit(reinterpret_cast<const void *>(at), size, [&] {
                const auto *code = reinterpret_cast<const uint32_t *>(at);
                const size_t count = size / sizeof(uint32_t);
                for (size_t index = 0; index + 1 < count; ++index) {
                    const auto target = nhk::arm64::decode_address_pair(
                        std::span<const uint32_t>(code, count), index, at + index * 4);
                    if (target && *target <= UINTPTR_MAX - tolerance
                        && want <= UINTPTR_MAX - tolerance
                        && *target + tolerance >= want && want + tolerance >= *target) {
                        found.push_back({at + (index + 1) * 4, static_cast<uintptr_t>(*target)});
                    }
                }
            });
        }
    }
    return found;
}

/** Rust's unwind index supplies exact entries, including frameless and split-prologue functions. */
inline std::optional<unwind::FunctionBounds> indexed_function(
    const probe::Guard &guard, const LayoutImage &image, uintptr_t site) {
    constexpr uint32_t kProgramTypeEhFrame = 0x6474e550u;
    constexpr size_t kMaxHeaderBytes = 4U << 20;
    for (const nhk::elf::ProgramSegment &segment : image.programs) {
        if (segment.type != kProgramTypeEhFrame || segment.filesz < 20
            || segment.filesz > kMaxHeaderBytes) continue;
        std::vector<std::byte> bytes(static_cast<size_t>(segment.filesz));
        const uintptr_t address = image.base + segment.vaddr;
        if (!guard.read(reinterpret_cast<const void *>(address), bytes.data(), bytes.size())) {
            return {};
        }
        return unwind::lookup(bytes, address, site);
    }
    return {};
}

/**
 * The bytes around a hit, so the log says what kind of table the literal sits in.
 *
 * The window is deliberately not centred on the hit: it shows the text that *precedes* the literal
 * plus the beginning of the literal itself, which is what tells a string table apart from a
 * serialised document.
 */
inline std::string context_around(const probe::Guard &guard, uintptr_t address) {
    if (address <= kContextBefore) return {};

    std::byte window[kContextBefore + kContextAfter] = {};
    if (!guard.read(reinterpret_cast<const void *>(address - kContextBefore), window,
            sizeof(window))) {
        return {};
    }
    return to_text(window, sizeof(window));
}

/**
 * Resolve one needle end to end and report what was found.
 *
 * The log line is the deliverable of this stage: it carries where the literal was found (so an
 * anonymous range is as usable as a named image), and the file-relative offset of the function
 * entry when the range has a file behind it - calibration data for a signature, never an address
 * to patch without checking it against the build it was measured on.
 */
inline bool resolve(const probe::Guard &guard, const std::vector<Mapping> &mappings,
    const RangeSet &ranges, const LayoutImage *image, std::string_view needle,
    const NeedleHit &hit) {
    const Mapping &found_in = mappings[hit.mapping];
    const std::string where = mapping_label(mappings, hit.mapping);
    const std::string context = context_around(guard, hit.address);

    const std::vector<Reference> references = xref(guard, mappings, ranges.code, hit.address);
    if (references.empty()) {
        __android_log_print(ANDROID_LOG_INFO, kLogTag,
            "no xref for %.*s at %s+0x%llx context='%s'", static_cast<int>(needle.size()),
            needle.data(), where.c_str(),
            static_cast<unsigned long long>(hit.address - found_in.begin), context.c_str());
        return false;
    }
    std::vector<unwind::FunctionBounds> candidates;
    for (const Reference &reference : references) {
        const auto bounds = image ? indexed_function(guard, *image, reference.site)
            : std::optional<unwind::FunctionBounds>{};
        if (!bounds) continue;
        const bool known = std::any_of(candidates.begin(), candidates.end(),
            [&](const auto &candidate) { return candidate.begin == bounds->begin; });
        if (!known) candidates.push_back(*bounds);
    }
    if (candidates.size() != 1) {
        __android_log_print(ANDROID_LOG_WARN, kLogTag,
            "ambiguous %.*s refs=%zu functions=%zu; no target accepted",
            static_cast<int>(needle.size()), needle.data(), references.size(), candidates.size());
        return false;
    }
    const uintptr_t reference = references.front().site;
    const uintptr_t entry = candidates.front().begin;
    uint64_t file_offset = 0;
    uint64_t inode = found_in.inode;
    bool file_backed = !found_in.path.empty();
    if (entry != 0) {
        for (const size_t index : ranges.code) {
            const Mapping &mapping = mappings[index];
            if (entry < mapping.begin || entry >= mapping.end) continue;
            file_backed = !mapping.path.empty();
            inode = mapping.inode;
            if (file_backed) file_offset = mapping.file_offset + (entry - mapping.begin);
            break;
        }
    }

    __android_log_print(ANDROID_LOG_INFO, kLogTag,
        "resolved %.*s refs=%zu needle=%s+0x%llx ref=%p entry=%p entryFileOffset=0x%llx"
        " inode=%llu fileBacked=%d context='%s'",
        static_cast<int>(needle.size()), needle.data(), references.size(), where.c_str(),
        static_cast<unsigned long long>(hit.address - found_in.begin),
        reinterpret_cast<void *>(reference), reinterpret_cast<void *>(entry),
        static_cast<unsigned long long>(file_offset), static_cast<unsigned long long>(inode),
        file_backed ? 1 : 0, context.c_str());
    return true;
}

/**
 * Resolve every layout needle this stage knows about, watching for a window of time.
 *
 * An attempt is not one lookup but two: phase one searches the images the structure phase
 * identified, phase two searches every readable range no image claimed. The second phase is not a
 * fallback for a failure of the first - on this ROM the launcher's images are mapped without a
 * header, and a literal is the only stable thing about them, so searching by content is the only
 * way to find them at all.
 *
 * The attempts exist because of *when* this runs. The module's native entry is reached while the
 * process is still starting, and the launcher loads its Flutter and Rust images after that, so a
 * single scan would only ever see the system libraries. Each attempt re-reads the mapping table,
 * and the attempt count in the log is therefore a measurement of when the launcher's image became
 * visible - not a timeout that happened to be long enough.
 */
inline void resolve_layout_targets(uint64_t window_ms = 60 * 1000, uint64_t interval_ms = 2000) {
    const probe::Guard guard;
    const SelfImage self = own_image();
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
        "resolver image: base=%p inode=%llu path='%s'", reinterpret_cast<void *>(self.base),
        static_cast<unsigned long long>(self.inode), self.path.c_str());

    // Needles are the launcher's own layout names: stable text, no addresses.
    const std::vector<std::string_view> needles{
        " get_grid_height_with_search_and_indicator: square_hot_seat_cell_height:",
        " cal_grid_size: x:",
    };
    std::vector<bool> wanted(needles.size(), true);
    std::vector<std::optional<NeedleHit>> hits(needles.size());
    std::vector<bool> reported(needles.size(), false);

    const size_t attempts = interval_ms == 0 ? 1 : static_cast<size_t>(window_ms / interval_ms) + 1;
    size_t previous_images = SIZE_MAX;

    for (size_t attempt = 1; attempt <= attempts; ++attempt) {
        const std::vector<Mapping> mappings = readable_mappings();
        std::vector<bool> owned;
        const std::vector<LayoutImage> images = layout_images(guard, mappings, owned, self);
        std::vector<RangeSet> owner_ranges;
        owner_ranges.reserve(images.size());
        for (const LayoutImage &image : images) {
            owner_ranges.push_back(ranges_of(mappings, image.ranges));
        }

        size_t excluded = 0;
        for (const Mapping &mapping : mappings) {
            if (self.owns(mapping)) ++excluded;
        }

        // Phase one: the images structure identified.
        ScanStats structural;
        for (size_t index = 0; index < images.size(); ++index) {
            size_t remaining = 0;
            for (size_t needle = 0; needle < needles.size(); ++needle) {
                if (wanted[needle]) ++remaining;
            }
            if (remaining == 0) break;
            for (const NeedleHit &hit : find_needles(guard, mappings, owner_ranges[index].data,
                     needles, wanted, structural)) {
                if (hits[hit.needle]) continue;
                hits[hit.needle] = hit;
                wanted[hit.needle] = false;
            }
        }

        // Phase two: every readable range no image claimed, read-only first - an image's literals
        // are in its read-only data, a writable range can only ever hold a copy of them. A range
        // far larger than an image is skipped: the process reserves tens of gigabytes it never
        // fills, and reading one of those would spend the whole budget to learn nothing. Every
        // unowned range still counts as a place a reference may be found in, though - the
        // attribute is about the data side, and the code side is small enough to always keep.
        std::vector<size_t> unowned;
        RangeSet content_ranges;
        uint64_t oversized = 0;
        size_t oversized_ranges = 0;
        for (size_t index = 0; index < mappings.size(); ++index) {
            if (owned[index]) continue;
            const Mapping &mapping = mappings[index];
            (mapping.executable ? content_ranges.code : content_ranges.data).push_back(index);
            if (mapping.bytes() > kContentRangeMaxBytes) {
                oversized += mapping.bytes();
                ++oversized_ranges;
                continue;
            }
            unowned.push_back(index);
        }
        std::stable_partition(unowned.begin(), unowned.end(),
            [&mappings](size_t index) { return !mappings[index].writable; });

        ScanStats content;
        for (const NeedleHit &hit :
            find_needles(guard, mappings, unowned, needles, wanted, content)) {
            if (hits[hit.needle]) continue;
            hits[hit.needle] = hit;
            wanted[hit.needle] = false;
        }

        size_t found = 0;
        for (const std::optional<NeedleHit> &hit : hits) {
            if (hit) ++found;
        }
        __android_log_print(ANDROID_LOG_INFO, kLogTag,
            "attempt %zu/%zu: mappings=%zu images=%zu self=%zu launcherNamed=%zu unowned=%zu"
            " scanned=%llu/%llu bytes unreadable=%zu oversized=%zu(%llu bytes) found=%zu/%zu",
            attempt, attempts, mappings.size(), images.size(), excluded,
            launcher_named(mappings).size(), unowned.size(),
            static_cast<unsigned long long>(structural.bytes),
            static_cast<unsigned long long>(content.bytes),
            structural.skipped_pages + content.skipped_pages, oversized_ranges,
            static_cast<unsigned long long>(oversized), found, needles.size());

        // The inventory is only interesting when it changes: that is the moment an image the
        // launcher needed appeared, and the moment a needle can first be found in it.
        if (images.size() != previous_images) {
            for (const size_t index : launcher_named(mappings)) {
                __android_log_print(ANDROID_LOG_INFO, kLogTag, "  launcher-named mapping: %s%s%s",
                    mapping_label(mappings, index).c_str(),
                    mappings[index].executable ? " executable" : "",
                    owned[index] ? " (owns an image)" : " (owns no image)");
            }
            for (const size_t index : unowned_selection(mappings, owned)) {
                __android_log_print(ANDROID_LOG_INFO, kLogTag,
                    "  unowned readable range: %s%s", mapping_label(mappings, index).c_str(),
                    mappings[index].writable ? " (writable)" : "");
            }
            size_t named = 0;
            for (const LayoutImage &image : images) {
                if (image.path.empty() || image.path.starts_with("/data/")) {
                    __android_log_print(ANDROID_LOG_INFO, kLogTag,
                        "  own-install image base=%p ranges=%zu name='%s' buildId=%s",
                        reinterpret_cast<void *>(image.base), image.ranges.size(),
                        image.path.c_str(), image.build_id.c_str());
                    ++named;
                }
            }
            __android_log_print(ANDROID_LOG_INFO, kLogTag,
                "  inventory changed: %zu images, %zu of them not a system library", images.size(),
                named);
            previous_images = images.size();
        }

        for (size_t needle = 0; needle < needles.size(); ++needle) {
            if (!hits[needle] || reported[needle]) continue;
            // Whoever found the literal also owns the ranges its reference is looked for in: the
            // image that claimed the mapping, or - for an anonymous one - every range no image
            // claimed, since an unnamed image's code is exactly as unnamed as its data.
            const size_t mapping_index = hits[needle]->mapping;
            const RangeSet *ranges = &content_ranges;
            const LayoutImage *owner = nullptr;
            for (size_t index = 0; index < images.size(); ++index) {
                const std::vector<size_t> &owned_ranges = images[index].ranges;
                if (std::find(owned_ranges.begin(), owned_ranges.end(), mapping_index)
                    != owned_ranges.end()) {
                    ranges = &owner_ranges[index];
                    owner = &images[index];
                    break;
                }
            }
            reported[needle] = resolve(guard, mappings, *ranges, owner,
                needles[needle], *hits[needle]);
        }

        size_t done = 0;
        for (const bool is_reported : reported) {
            if (is_reported) ++done;
        }
        if (done == needles.size()) {
            __android_log_print(ANDROID_LOG_INFO, kLogTag, "resolver finished: %zu/%zu needles",
                done, needles.size());
            return;
        }
        if (attempt < attempts) ::usleep(static_cast<useconds_t>(interval_ms * 1000));
    }

    for (size_t needle = 0; needle < needles.size(); ++needle) {
        if (hits[needle]) continue;
        __android_log_print(ANDROID_LOG_INFO, kLogTag,
            "needle not found in %zu attempts: %.*s", attempts,
            static_cast<int>(needles[needle].size()), needles[needle].data());
    }
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "resolver finished without every needle");
}

} // namespace home_layout
