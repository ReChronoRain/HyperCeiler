/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../../app/src/main/cpp/targets/home/dock_native_runtime.h"

#include <array>
#include <cassert>
#include <iomanip>
#include <sstream>
#include <string>

namespace {
using dock_motion::ExecutableMapping;

std::string mapping_line(uintptr_t begin, uintptr_t end, std::string_view permissions,
    uint64_t file_offset, uint32_t device_major, uint32_t device_minor,
    uint64_t inode, std::string_view path = "/data/app/com.miui.home/lib/arm64/libapp.so") {
    std::ostringstream line;
    line << std::hex << begin << '-' << end << ' ' << permissions << ' '
         << file_offset << ' ' << device_major << ':' << device_minor << ' '
         << std::dec << inode << "  " << path << '\n';
    return line.str();
}

std::vector<ExecutableMapping> parse(std::string_view text) {
    std::istringstream maps{std::string{text}};
    return dock_motion::executable_libapp_mappings(maps);
}

void test_valid_multi_generation_inventory() {
    const auto newest = mapping_line(0x50000000, 0x50004000, "r-xp", 0x2000,
        0xfd, 1, 9002, "/data/app/new/libapp.so");
    const auto oldest = mapping_line(0x10000000, 0x10003000, "r-xp", 0x1000,
        0xfd, 1, 9001, "/data/app/old/libapp.so (deleted)");
    const std::string ignored =
        "20000000-20001000 r--p 00000000 fd:01 9001  /data/app/old/libapp.so\n"
        "30000000-30001000 r-xp 00000000 fd:01 33  /data/app/old/libother.so\n";

    const auto mappings = parse(newest + ignored + oldest);
    assert(mappings.size() == 2);
    assert(mappings[0].begin == 0x10000000);
    assert(mappings[0].end == 0x10003000);
    assert(mappings[0].file_offset == 0x1000);
    assert(mappings[0].inode == 9001);
    assert(mappings[1].begin == 0x50000000);
    assert(mappings[1].inode == 9002);
}

void test_malformed_snapshots_fail_closed() {
    const auto valid = mapping_line(0x08000000, 0x08001000, "r-xp",
        0, 0xfd, 1, 99);
    const auto writable = parse(valid + mapping_line(0x10000000, 0x10001000, "rwxp",
        0, 0xfd, 1, 1));
    assert(writable.empty());

    const auto no_inode = parse(valid + mapping_line(0x10000000, 0x10001000, "r-xp",
        0, 0xfd, 1, 0));
    assert(no_inode.empty());

    const auto overlapping = parse(
        mapping_line(0x10000000, 0x10003000, "r-xp", 0, 0xfd, 1, 1)
        + mapping_line(0x10002000, 0x10004000, "r-xp", 0, 0xfd, 1, 2));
    assert(overlapping.empty());

    std::string too_many;
    for (size_t i = 0; i <= dock_motion::kMaxExecutableMappings; ++i) {
        const uintptr_t begin = 0x20000000 + i * 0x2000;
        too_many += mapping_line(begin, begin + 0x1000, "r-xp", i * 0x1000,
            0xfd, 1, 10 + i);
    }
    assert(parse(too_many).empty());

    const uintptr_t oversized_begin = 0x40000000;
    const uintptr_t oversized_end = oversized_begin
        + dock_motion::kMaxExecutableCodeBytes + sizeof(uint32_t);
    assert(parse(valid + mapping_line(oversized_begin, oversized_end, "r-xp",
        0, 0xfd, 1, 1)).empty());
}

void test_code_source_is_file_identity() {
    const ExecutableMapping first{0x10000000, 0x10002000, 0x4000, 0xfd, 1, 77};
    const ExecutableMapping alias{0x30000000, 0x30002000, 0x4000, 0xfd, 1, 77};
    const ExecutableMapping replacement{0x50000000, 0x50002000, 0x4000, 0xfd, 1, 78};

    const auto first_source = first.source_at(first.begin + 0x180, 16);
    const auto alias_source = alias.source_at(alias.begin + 0x180, 16);
    const auto replacement_source = replacement.source_at(replacement.begin + 0x180, 16);
    assert(first_source && alias_source && replacement_source);
    assert(first_source->file_offset == 0x4180);
    assert(*first_source == *alias_source); // Virtual address is intentionally absent.
    assert(*first_source != *replacement_source);
    assert(!first.source_at(first.end - 8, 16));
    assert(!first.source_at(first.begin, 0));
}

void test_inventory_change_discovers_without_replacing_old_generation() {
    const auto old_line = mapping_line(0x10000000, 0x10004000, "r-xp", 0x1000,
        0xfd, 1, 101, "/data/app/old/libapp.so (deleted)");
    const auto clean_line = mapping_line(0x50000000, 0x50004000, "r-xp", 0x1000,
        0xfd, 1, 202, "/data/app/new/libapp.so");
    const auto old_inventory = parse(old_line);
    const auto changed_inventory = parse(old_line + clean_line);
    assert(old_inventory.size() == 1);
    assert(changed_inventory.size() == 2);
    assert(old_inventory != changed_inventory);

    const std::array<uintptr_t, 3> old_targets{
        0x10000100, 0x10000800, 0x10001200};
    std::array<dock_motion::CodeSource, old_targets.size()> old_sources{};
    assert(dock_motion::sources_for(old_inventory, old_targets, 16, old_sources));
    // Adding a clean runtime generation does not invalidate the still-mapped
    // old targets. It must receive an independent bank rather than replacing
    // the old generation's layout and trampoline state.
    assert(dock_motion::mapping_state(changed_inventory, old_targets, old_sources, 16)
        == dock_motion::MappingState::same);
    const auto clean_source = dock_motion::source_at(changed_inventory, 0x50000100, 16);
    assert(clean_source && clean_source->inode == 202);
}

void test_runtime_generations_include_load_bias() {
    const auto mappings = parse(
        mapping_line(0x10000000, 0x10002000, "r-xp", 0x1000, 0xfd, 1, 77)
        + mapping_line(0x10004000, 0x10006000, "r-xp", 0x5000, 0xfd, 1, 77)
        + mapping_line(0x30000000, 0x30002000, "r-xp", 0x1000, 0xfd, 1, 77)
        + mapping_line(0x50000000, 0x50002000, "r-xp", 0x1000, 0xfd, 1, 88));
    const auto grouped = dock_motion::runtime_generations(mappings);
    assert(grouped.size() == 3);
    assert(grouped[0].size() == 2); // Same file identity and load bias.
    assert(grouped[1].size() == 1); // Same inode, independently mapped alias.
    assert(grouped[2].size() == 1); // Different file generation.

    const ExecutableMapping invalid{0x1000, 0x2000, 0x3000, 1, 1, 1};
    assert(!dock_motion::generation_key(invalid));
}
} // namespace

int main() {
    test_valid_multi_generation_inventory();
    test_malformed_snapshots_fail_closed();
    test_code_source_is_file_identity();
    test_inventory_change_discovers_without_replacing_old_generation();
    test_runtime_generations_include_load_bias();
}
