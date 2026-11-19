/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_back_arrow.h"
#include <fstream>
#include <iostream>

int main(int argc, char **argv) {
    if (argc != 2)
        return 2;
    std::ifstream file(argv[1], std::ios::binary);
    std::vector<char> raw((std::istreambuf_iterator<char>(file)), {});
    std::vector<std::byte> bytes(raw.size());
    std::memcpy(bytes.data(), raw.data(), raw.size());
    auto target = home_back_arrow::resolve(bytes);
    if (!target) {
        std::cerr << "real arrow contracts not resolved\n";
        return 1;
    }
    auto image = home_layout::elf_targets::parse(bytes);
    auto edited = bytes;
    unsigned changed = 0;
    auto bounds = image->bounds(target->bitmap);
    for (uint64_t pc = bounds->begin; pc < bounds->end; pc += 4) {
        uint32_t word = *image->word(pc);
        if ((word & 0xffc003ff) != 0xbd400020)
            continue;
        word += 1u << 10;
        std::memcpy(edited.data() + *image->file_offset(pc, 4), &word, 4);
        changed++;
    }
    if (changed != 1 || home_back_arrow::resolve(edited)) {
        std::cerr << "mismatched view layout accepted\n";
        return 1;
    }
    edited = bytes;
    changed = 0;
    for (uint64_t pc = bounds->begin; pc < bounds->end; pc += 4) {
        auto to = nhk::arm64::branch_target(pc, *image->word(pc), true);
        if (!to || *to != target->imports[3])
            continue;
        uint32_t nop = 0xd503201f;
        std::memcpy(edited.data() + *image->file_offset(pc, 4), &nop, 4);
        changed++;
    }
    if (changed != 1 || home_back_arrow::resolve(edited)) {
        std::cerr << "missing bitmap drawing call accepted\n";
        return 1;
    }
    edited = bytes;
    edited[0] = std::byte{0};
    if (home_back_arrow::resolve(edited)) {
        std::cerr << "invalid ELF accepted\n";
        return 1;
    }
    edited = bytes;
    uint32_t nop = 0xd503201f;
    std::memcpy(edited.data() + *image->file_offset(target->destroy + 4, 4), &nop, 4);
    if (home_back_arrow::resolve(edited)) {
        std::cerr << "unverified view destructor accepted\n";
        return 1;
    }
    edited = bytes;
    bounds = image->bounds(target->arc_drop);
    changed = 0;
    for (uint64_t pc = bounds->begin; pc < bounds->end; pc += 4) {
        auto to = nhk::arm64::branch_target(pc, *image->word(pc), true);
        if (to && *to == target->destroy) {
            std::memcpy(edited.data() + *image->file_offset(pc, 4), &nop, 4);
            changed++;
        }
    }
    if (changed != 1 || home_back_arrow::resolve(edited)) {
        std::cerr << "unverified Arc lifetime accepted\n";
        return 1;
    }
    for (unsigned i = 11; i <= 13; i++) {
        edited = bytes;
        std::memcpy(edited.data() + *image->file_offset(target->imports[i], 4), &nop, 4);
        if (home_back_arrow::resolve(edited)) {
            std::cerr << "missing configuration getter accepted\n";
            return 1;
        }
    }
    std::cout << "Arrow resolver: real image and eight rejection cases passed\n";
}
