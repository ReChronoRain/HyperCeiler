/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_shortcut_menu.h"
#include <cassert>
#include <fstream>
#include <iostream>

namespace menu = home_layout::features::shortcut_menu;
using home_layout::features::Code;
using home_layout::recent_render::Registers;

int main(int argc, char** argv) {
    assert(argc >= 3 && argc % 2 == 1);
    for (int i = 1; i < argc; i += 2) {
        const auto expected = static_cast<uint32_t>(std::stoul(argv[i + 1], nullptr, 16));
        std::ifstream file(argv[i], std::ios::binary);
        assert(file);
        std::vector<char> image((std::istreambuf_iterator<char>(file)), {});
        auto code = Code::parse(std::as_bytes(std::span(image)));
        assert(code && menu::resolve(*code) == expected);
        // The post-call replay window must decline changed instructions.
        const uint32_t replacement = 0xd503201f;
        const auto loads = nhk::elf::parse_program_segments(std::as_bytes(std::span(image)));
        assert(loads);
        const auto load = std::find_if(loads->begin(), loads->end(), [&](const auto& segment) {
            return segment.type == nhk::elf::kProgramTypeLoad && (segment.flags & 1)
                && segment.vaddr <= expected && expected + 16 <= segment.vaddr + segment.filesz;
        });
        assert(load != loads->end());
        const auto offset = load->offset + expected - load->vaddr;
        for (size_t at = 0; at < 16; at += 4) {
            uint32_t original;
            std::memcpy(&original, image.data() + offset + at, 4);
            std::memcpy(image.data() + offset + at, &replacement, 4);
            code = Code::parse(std::as_bytes(std::span(image)));
            assert(code && !menu::resolve(*code));
            std::memcpy(image.data() + offset + at, &original, 4);
        }
        std::cout << argv[i] << " menu count resolver PASS\n";
    }

    for (const bool enabled : {false, true}) {
        for (const uintptr_t original : {0u, 0x1000u}) {
            for (const uint64_t count : {0u, 3u, 4u, 5u, 20u}) {
                Registers saved{};
                saved.x[0] = count;
                saved.x[15] = 0x2000;
                saved.x[30] = 0x3000;
                Registers expected = saved;
                expected.branch = original;
                const bool applied = enabled && original && count == 4;
                if (applied) expected.x[0] = 6;
                assert(menu::apply(saved, enabled, original) == applied);
                assert(std::memcmp(&saved, &expected, sizeof(saved)) == 0);
            }
        }
    }
    std::cout << "menu count register replay PASS\n";
}
