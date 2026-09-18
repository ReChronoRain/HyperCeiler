/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the image view layer (nativehook/native_image.h):
 * image_view_from_segments() must place every runtime mapping in the image's
 * virtual address space using the *program headers*, because PT_LOADs legally
 * have segment-specific `p_vaddr - p_offset` gaps. The fixture below mirrors a
 * real system library whose four PT_LOADs differ by 0x0 / 0x10000 / 0x20000 /
 * 0x30000 - an implementation that assumes `begin - file_offset` is constant
 * cannot see such an image at all.
 */
#include "../../app/src/main/cpp/nativehook/native_image.h"

#include <array>
#include <cstdio>
#include <sstream>
#include <string>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

namespace {

void put16(std::vector<uint8_t> &out, size_t at, uint16_t value) {
    out[at] = static_cast<uint8_t>(value);
    out[at + 1] = static_cast<uint8_t>(value >> 8);
}

void put32(std::vector<uint8_t> &out, size_t at, uint32_t value) {
    for (int i = 0; i < 4; ++i) out[at + i] = static_cast<uint8_t>(value >> (i * 8));
}

void put64(std::vector<uint8_t> &out, size_t at, uint64_t value) {
    for (int i = 0; i < 8; ++i) out[at + i] = static_cast<uint8_t>(value >> (i * 8));
}

struct Segment {
    uint32_t type;
    uint32_t flags;
    uint64_t offset;
    uint64_t vaddr;
    uint64_t filesz;
    uint64_t memsz;
};

/** An ELF header plus program header table describing `segments`. */
std::vector<uint8_t> make_head(const std::vector<Segment> &segments) {
    std::vector<uint8_t> head(64 + segments.size() * 56, 0);
    const uint8_t ident[16] = {0x7f, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    for (int i = 0; i < 16; ++i) head[static_cast<size_t>(i)] = ident[i];
    put16(head, 16, 3);   // ET_DYN
    put16(head, 18, 183); // EM_AARCH64
    put64(head, 32, 64);  // e_phoff
    put16(head, 52, 64);
    put16(head, 54, 56);
    put16(head, 56, static_cast<uint16_t>(segments.size()));
    for (size_t i = 0; i < segments.size(); ++i) {
        const size_t at = 64 + i * 56;
        put32(head, at, segments[i].type);
        put32(head, at + 4, segments[i].flags);
        put64(head, at + 8, segments[i].offset);
        put64(head, at + 16, segments[i].vaddr);
        put64(head, at + 32, segments[i].filesz);
        put64(head, at + 40, segments[i].memsz);
    }
    return head;
}

// The layout of a real system runtime: four PT_LOADs whose vaddr - offset gaps
// differ, plus a dynamic segment far above the first pages.
const std::vector<Segment> kRealSegments{
    {1, 5, 0x0, 0x0, 0x612578, 0x612578},
    {1, 6, 0x612580, 0x622580, 0xa7cc50, 0xa7cc50},
    {1, 6, 0x108f1d0, 0x10af1d0, 0x535d0, 0x53e30},
    {1, 6, 0x10e27a0, 0x11127a0, 0x42a0, 0x95ce8},
    {2, 6, 0x10e1638, 0x1101638, 0x220, 0x220},
};

constexpr uintptr_t kLoadBase = 0x7100000000ULL;
// Page alignment must use the same page size the runtime code uses: a mapping's
// file offset is aligned to the page size of the process being inspected.
const uint64_t kPage = nhk::host_page_size();

/** Page-aligned maps for the PT_LOADs above, as the kernel would report them. */
std::string make_maps() {
    std::ostringstream maps;
    maps << std::hex;
    for (const auto &segment : kRealSegments) {
        if (segment.type != 1) continue; // The kernel maps PT_LOADs.
        const uint64_t first_page = (segment.offset / kPage) * kPage;
        const uint64_t last_page = ((segment.offset + segment.filesz + kPage - 1) / kPage) * kPage;
        for (uint64_t file_offset = first_page; file_offset < last_page; file_offset += kPage) {
            const uint64_t vaddr = segment.vaddr + (file_offset - segment.offset);
            maps << (kLoadBase + vaddr) << '-' << (kLoadBase + vaddr + kPage) << ' '
                 << (segment.flags == 5 ? "r-xp" : "rw-p") << ' '
                 << file_offset << " fd:01 4242 /system_ext/lib64/libhyper_os_flutter.so\n";
        }
    }
    return maps.str();
}

} // namespace

int main() {
    // --- Program header decoding. ---
    const auto head = make_head(kRealSegments);
    const auto segments = nhk::elf::parse_program_segments(
        std::span<const std::byte>(reinterpret_cast<const std::byte *>(head.data()),
            head.size()));
    check(segments.has_value(), "program headers decode");
    if (!segments) return 1;
    check(segments->size() == kRealSegments.size(), "every entry decoded");

    // --- A table larger than an old fixed ceiling still decodes. ---
    //
    // One decoder and one ceiling: a well-formed image with more program
    // headers than a stale limit allowed must not be refused by either the
    // decoder or the image's own header validation.
    {
        std::vector<Segment> many{{1, 5, 0, 0, 0x1000, 0x1000}};
        for (int i = 0; i < 120; ++i) many.push_back({0, 0, 0, 0, 0, 0}); // PT_NULL.
        const auto head = make_head(many);
        const auto decoded = nhk::elf::parse_program_segments(
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(head.data()),
                head.size()));
        check(decoded.has_value() && decoded->size() == many.size(),
            "a table with 121 headers decodes");
        check(nhk::elf::kMaxProgramHeaders >= 121,
            "the shared ceiling admits that table");
    }

    // --- Image view over the real layout. ---
    const std::string maps_text = make_maps();
    std::istringstream maps(maps_text);
    const auto inventory = nhk::parse_file_mappings(maps);
    check(!inventory.empty(), "inventory parsed");

    const auto view = nhk::image_view_from_segments(
        inventory, "/system_ext/lib64/libhyper_os_flutter.so", *segments);
    check(view.has_value(),
        "a multi-gap image (vaddr-offset = 0/0x10000/0x20000/0x30000) is accepted");
    if (view) {
        check(view->load_base == kLoadBase, "load base recovered from the segments");
        // The dynamic segment sits at vaddr 0x1101638; a "first few megabytes"
        // snapshot could never reach it, so the view must ask for the range.
        check(view->needed_vaddr >= 0x1101638ULL,
            "snapshot requirement covers the dynamic segment");
        check(view->ranges.size() >= 4, "all mapped ranges present");

        // Spot-check the second segment's first page. Its file offset is
        // page-aligned down, and its virtual address must be the segment's
        // p_vaddr page-aligned the same way - not the file offset.
        const uint64_t expected_vaddr = (0x622580ULL / kPage) * kPage;
        const uint64_t expected_offset = (0x612580ULL / kPage) * kPage;
        bool found = false;
        for (const auto &range : view->ranges) {
            if (range.begin != kLoadBase + expected_vaddr) continue;
            found = true;
            check(range.vaddr == expected_vaddr,
                "range vaddr follows the segment's p_vaddr, not p_offset");
            check(expected_vaddr != expected_offset,
                "the fixture really has p_vaddr != p_offset for this segment");
        }
        check(found, "second segment's page is present at its virtual address");
    }

    // --- A mapping the program headers do not claim is refused. ---
    {
        std::istringstream extra(maps_text
            + "7100500000-7100501000 r-xp 00000000 fd:01 4242 /system_ext/lib64/libhyper_os_flutter.so\n");
        const auto inventory2 = nhk::parse_file_mappings(extra);
        check(!nhk::image_view_from_segments(
                  inventory2, "/system_ext/lib64/libhyper_os_flutter.so", *segments)
                  .has_value(),
            "an unclaimed mapping is refused");
    }

    // --- A path with no mappings has no view. ---
    {
        std::istringstream foreign(maps_text);
        const auto inventory2 = nhk::parse_file_mappings(foreign);
        check(!nhk::image_view_from_segments(inventory2, "/lib/absent.so", *segments)
                  .has_value(),
            "a path with no mappings has no view");
    }

    if (failures == 0) std::printf("NativeHookImageViewTest passed\n");
    return failures == 0 ? 0 : 1;
}
