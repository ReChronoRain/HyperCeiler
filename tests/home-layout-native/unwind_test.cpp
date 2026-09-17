/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_layout_unwind.h"
#include "targets/home/home_layout_elf_targets.h"
#include "nativehook/elf_image.h"

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <fstream>
#include <iterator>
#include <span>
#include <string>
#include <vector>

namespace {
void put32(std::vector<std::byte> &bytes, size_t at, uint32_t value) {
    for (size_t index = 0; index < 4; ++index) {
        bytes[at + index] = static_cast<std::byte>((value >> (index * 8)) & 0xff);
    }
}
}

int main(int argc, char **argv) {
    constexpr uintptr_t base = 0x100000;
    std::vector<std::byte> header(12 + 3 * 8);
    header[0] = std::byte{1};
    header[1] = std::byte{0x1b};
    header[2] = std::byte{3};
    header[3] = std::byte{0x3b};
    put32(header, 8, 3);
    put32(header, 12, 0x100);
    put32(header, 20, 0x200);
    put32(header, 28, 0x300);
    auto bounds = home_layout::unwind::lookup(header, base, base + 0x240);
    assert(bounds && bounds->begin == base + 0x200 && bounds->end == base + 0x300);
    assert(!home_layout::unwind::lookup(header, base, base + 0x90));
    assert(!home_layout::unwind::lookup(header, base, base + 0x310));
    header[3] = std::byte{0xff};
    assert(!home_layout::unwind::lookup(header, base, base + 0x240));

    // Optional OTA calibration fixture. The addresses below are assertions about this extracted
    // build, not target offsets used by the module. The index location itself comes from ELF.
    if (argc > 1) {
        std::ifstream file(argv[1], std::ios::binary);
        assert(file);
        const std::string raw(std::istreambuf_iterator<char>{file}, {});
        const auto *bytes = reinterpret_cast<const std::byte *>(raw.data());
        const std::span<const std::byte> image(bytes, raw.size());
        const auto programs = nhk::elf::parse_program_segments(image);
        assert(programs);
        const auto targets = home_layout::elf_targets::resolve(image);
        assert(targets);
        // Symbol-table ground truth for this extracted build (launcher 8.01.02.6305), demangled from
        // .gnu_debugdata: `<GridConfig>::instance`, `PhoneDeviceRules::get_grid_height_with_search_
        // and_indicator`, `DeviceConfigs::get_cell_count_x/_y`, `device_utils::resolve_dimen_px`.
        // They are the independent second path the resolver is checked against; the resolver itself
        // carries no address.
        assert(targets->grid_config_initializer == 0x58f588);
        assert(targets->grid_height == 0x63bed4);
        assert(targets->cell_count_x == 0x620f34);
        assert(targets->cell_count_y == 0x620fe0);
        assert(targets->resolve_dimen_px == 0x8217c0);

        /*
         * Every resolved entry must be a function begin according to the image's own
         * `.eh_frame_hdr`, which is discovered from the program headers rather than hardcoded. This is
         * what keeps the fixture meaningful across an OTA: the expected values above change, but the
         * invariant does not.
         */
        bool checked = false;
        for (const auto &segment : *programs) {
            if (segment.type != 0x6474e550u) continue;
            assert(nhk::in_image(image.size(), segment.offset, segment.filesz));
            const auto table = image.subspan(segment.offset, segment.filesz);
            for (const uint64_t va : {targets->grid_config_initializer, targets->grid_height,
                     targets->cell_count_x, targets->cell_count_y, targets->resolve_dimen_px}) {
                const auto bounds = home_layout::unwind::lookup(table, segment.vaddr, va);
                assert(bounds && bounds->begin == va);
            }
            checked = true;
        }
        assert(checked);
    }
}
