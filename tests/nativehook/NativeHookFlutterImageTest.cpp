/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * End-to-end regression for a real runtime library image.
 *
 * Reproduces the two blockers a review found in the page-lifetime guard path:
 *
 *  1. A load segment's `p_vaddr - p_offset` differs per segment, so an image
 *     view built from `begin - file_offset` cannot see the real library at all.
 *  2. Its dynamic table sits far above the first megabytes (a measured runtime
 *     has PT_DYNAMIC at vaddr 0x1101638), so a fixed "first 4 MiB" snapshot can
 *     never contain it.
 *
 * This test builds the view from the program headers, snapshots the image at
 * its virtual addresses (reading the bytes from the file, which is what a
 * fault-reporting read of the mapping returns), and asserts that parsing
 * succeeds - and that the 4 MiB prefix does NOT, so the old behaviour cannot
 * silently return.
 *
 * Usage: NativeHookFlutterImageTest <library.so> [more...]
 *        No argument -> SKIP.
 */
#include "../../app/src/main/cpp/nativehook/native_image.h"

#include <cstdio>
#include <cstring>
#include <fstream>
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

std::vector<uint8_t> read_file(const std::string &path, uint64_t offset, size_t bytes) {
    std::vector<uint8_t> out;
    std::ifstream file(path, std::ios::binary);
    if (!file) return out;
    file.seekg(static_cast<std::streamoff>(offset));
    out.resize(bytes);
    file.read(reinterpret_cast<char *>(out.data()), static_cast<std::streamsize>(bytes));
    out.resize(static_cast<size_t>(file.gcount()));
    return out;
}

/** Page-aligned maps for the load segments, as the kernel would report them. */
std::string synthesize_maps(const std::vector<nhk::elf::ProgramSegment> &segments,
    std::string_view path, uintptr_t load_base) {
    const uint64_t page = nhk::host_page_size();
    std::ostringstream maps;
    maps << std::hex;
    for (const auto &segment : segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad || segment.filesz == 0) continue;
        const uint64_t first = (segment.offset / page) * page;
        const uint64_t last = ((segment.offset + segment.filesz + page - 1) / page) * page;
        for (uint64_t file_offset = first; file_offset < last; file_offset += page) {
            const uint64_t vaddr = segment.vaddr + (file_offset - segment.offset);
            maps << (load_base + vaddr) << '-' << (load_base + vaddr + page) << ' '
                 << (segment.flags == 5 || segment.flags == 4 ? "r--p" : "rw-p") << ' '
                 << file_offset << " fd:01 4242 " << path << '\n';
        }
    }
    return maps.str();
}

} // namespace

int main(int argc, char **argv) {
    if (argc < 2) {
        std::printf("SKIP NativeHookFlutterImageTest (no library given)\n");
        return 0;
    }
    for (int arg = 1; arg < argc; ++arg) {
        const std::string path = argv[arg];
        // Stage 1: the header, exactly as the guard reads it.
        const auto head = read_file(path, 0, 4096);
        if (head.empty()) {
            std::printf("FAIL: cannot read %s\n", path.c_str());
            ++failures;
            continue;
        }
        const auto segments = nhk::elf::parse_program_segments(
            std::span<const std::byte>(reinterpret_cast<const std::byte *>(head.data()),
                head.size()));
        if (!segments) {
            std::printf("FAIL: program headers rejected in %s\n", path.c_str());
            ++failures;
            continue;
        }
        constexpr uintptr_t kLoadBase = 0x7100000000ULL;
        std::istringstream maps(synthesize_maps(*segments, "/system_ext/lib64/runtime.so",
            kLoadBase));
        const auto inventory = nhk::parse_file_mappings(maps);

        // 1. The view must accept an image whose segment gaps differ.
        const auto view = nhk::image_view_from_segments(
            inventory, "/system_ext/lib64/runtime.so", *segments);
        check(view.has_value(), "image view accepts the real segment layout");
        if (!view) continue;
        check(view->load_base == kLoadBase, "load base recovered");
        check(view->ranges.size() >= 4, "every segment range present");
        check(view->needed_vaddr > 4ULL * 1024ULL * 1024ULL,
            "the dynamic table lies beyond the first 4 MiB (a prefix snapshot is not enough)");

        // 2. Snapshot by virtual address, reading the file as the mapping would.
        std::vector<std::byte> snapshot(static_cast<size_t>(view->needed_vaddr),
            std::byte{0});
        bool read_ok = true;
        for (const auto &range : view->ranges) {
            if (range.vaddr >= view->needed_vaddr) continue;
            const uint64_t usable = std::min<uint64_t>(range.bytes,
                view->needed_vaddr - range.vaddr);
            // The file offset backing this range comes from the inventory (the
            // synthesized maps carry it), which is what a real read needs.
            uint64_t backing_offset = 0;
            bool found = false;
            for (const auto &mapping : inventory) {
                if (mapping.begin != range.begin) continue;
                backing_offset = mapping.file_offset;
                found = true;
                break;
            }
            if (!found) {
                read_ok = false;
                break;
            }
            const auto bytes = read_file(path, backing_offset,
                static_cast<size_t>(usable));
            // The final page of a segment can extend past the end of the file
            // (the tail is zero-filled in memory); a short read there is not an
            // error, a read that yields nothing is.
            if (bytes.empty() || bytes.size() > usable) {
                read_ok = false;
                break;
            }
            std::memcpy(snapshot.data() + range.vaddr, bytes.data(), bytes.size());
        }
        check(read_ok, "every range could be read");

        const auto image = nhk::elf::ElfImage::make_with_segments(kLoadBase,
            std::span<const std::byte>(snapshot.data(), snapshot.size()), *segments);
        check(image.has_value(), "full virtual-address snapshot parses");
        if (image) {
            // A resolvable import proves the symbol/relocation tables were
            // reached: the dynamic table lives above the 4 MiB prefix.
            const auto slots = image->collect_slots("madvise");
            check(!slots.empty(), "madvise import slot found through the GOT tables");
        }

        // 3. The old behaviour must stay impossible. For this fixture the
        // dynamic table is known to sit above 4 MiB, so a prefix must NOT parse
        // at all - asserting "or it found no slot" would let a wrongly-accepted
        // truncation pass whenever it happened to miss the slot.
        const size_t prefix = std::min<size_t>(snapshot.size(), 4U * 1024U * 1024U);
        const auto prefix_image = nhk::elf::ElfImage::make_with_segments(kLoadBase,
            std::span<const std::byte>(snapshot.data(), prefix), *segments);
        check(!prefix_image.has_value(),
            "a 4 MiB prefix is rejected: the dynamic table lies beyond it");
    }
    if (failures == 0) std::printf("NativeHookFlutterImageTest passed\n");
    return failures == 0 ? 0 : 1;
}
