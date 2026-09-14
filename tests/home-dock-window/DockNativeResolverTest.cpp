/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../../app/src/main/cpp/targets/home/dock_native_resolver.h"
#include <cassert>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>

template<class T> T read(const std::vector<char> &bytes, size_t offset) {
    if (offset > bytes.size() || sizeof(T) > bytes.size() - offset) {
        throw std::out_of_range("read: offset out of range");
    }
    T result;
    std::memcpy(&result, bytes.data() + offset, sizeof(result));
    return result;
}

constexpr uint32_t movz_x(uint32_t reg, uint16_t immediate, uint32_t halfword) {
    return 0xd2800000u | ((halfword & 3) << 21)
        | (static_cast<uint32_t>(immediate) << 5) | reg;
}
constexpr uint32_t movk_x(uint32_t reg, uint16_t immediate, uint32_t halfword) {
    return 0xf2800000u | ((halfword & 3) << 21)
        | (static_cast<uint32_t>(immediate) << 5) | reg;
}
constexpr uint32_t compressed_pointer_add(uint32_t reg, uint32_t shift_kind = 0) {
    return 0x8b000000u | ((shift_kind & 3) << 22) | (28u << 16)
        | (32u << 10) | (reg << 5) | reg;
}

void test_decoder_rejects_unsafe_abi() {
    using namespace dock_motion;

    const std::array materialization{movz_x(2, 0xabcd, 0), movk_x(2, 0x1234, 1)};
    assert(materialized_u32(materialization, 2) == 0x1234abcd);
    auto shifted_low_half = materialization;
    shifted_low_half[0] = movz_x(2, 0xabcd, 1);
    assert(!materialized_u32(shifted_low_half, 2));
    auto shifted_high_half = materialization;
    shifted_high_half[1] = movk_x(2, 0x1234, 2);
    assert(!materialized_u32(shifted_high_half, 2));

    assert(is_compressed_pointer_add(compressed_pointer_add(4)));
    assert(!is_compressed_pointer_add(compressed_pointer_add(4, 1)));
    assert(!is_compressed_pointer_add(compressed_pointer_add(4, 2)));

    const TagAbi valid{-1, 12, 20, 8, 4, 4};
    assert(valid_tag_abi(valid));
    const uint32_t tag = (0xabcdeu << 12) | (3u << 8);
    assert(class_id(tag, valid) == 0xabcde);
    assert(object_size(tag, valid) == 48);
    assert(!valid_tag_abi({0, 12, 20, 8, 4, 4}));
    assert(!valid_tag_abi({-1, 13, 20, 8, 4, 4})); // class UBFX crosses bit 31.
    assert(!valid_tag_abi({-1, 12, 20, 31, 2, 4})); // size UBFX crosses bit 31.
    assert(!valid_tag_abi({-1, 12, 20, 10, 4, 4})); // class and size overlap.
    assert(class_id(tag, {-1, 32, 1, 8, 4, 4}) == 0);
    assert(object_size(tag, {-1, 12, 20, 32, 1, 4}) == 0);

    const auto alpha = scalar_field(7, -1, 64, 8, 8);
    const auto scale = scalar_field(15, -1, 64, 16, 8);
    const auto surface = scalar_field(31, -1, 64, 4, 4);
    const auto recents = scalar_field(35, -1, 64, 4, 4);
    assert(alpha && alpha->begin == 8 && alpha->end == 16);
    assert(scale && scale->begin == 16 && scale->end == 32);
    assert(surface && recents);
    assert(scalar_fields_disjoint(std::array{*alpha, *scale, *surface, *recents}));
    assert(!scalar_field(8, -1, 16, 8, 1)); // Tagged bias makes this byte 9: it overruns.
    assert(!scalar_field(6, -1, 64, 8, 8)); // Allocation-relative byte 7 is unaligned.
    assert(!scalar_field(-1, -1, 64, 8, 8)); // Header itself is not a scalar field.
    assert(!scalar_field(-1, -9, 64, 8, 8)); // Physical byte 8, but Layout cannot encode -1.
    assert(!scalar_field(7, 0, 64, 8, 8));
    assert(!scalar_field(7, -1, 64, 8, 3));
    const auto overlapping = scalar_field(11, -1, 64, 4, 4);
    assert(overlapping && scalar_fields_overlap(*alpha, *overlapping));
    assert(!scalar_fields_disjoint(std::array{*alpha, *overlapping}));
}

int main(int argc, char **argv) {
    using namespace dock_motion;
    const bool require_unlock = std::getenv("DOCK_REQUIRE_UNLOCK") != nullptr;
    test_decoder_rejects_unsafe_abi();
    assert(!resolve({}));
    assert(!call_target({0, {}}, 0));
    for (int arg = 1; arg < argc; ++arg) {
        std::ifstream file(argv[arg], std::ios::binary);
        assert(file.is_open());
        const std::vector<char> bytes{std::istreambuf_iterator<char>(file), {}};
        assert(read<uint32_t>(bytes, 0) == 0x464c457f && bytes[4] == 2 && bytes[5] == 1);
        // ELF64 header offsets are the file format ABI, not launcher addresses.
        const auto phoff = read<uint64_t>(bytes, 32);
        const auto phsize = read<uint16_t>(bytes, 54);
        const auto phcount = read<uint16_t>(bytes, 56);
        std::vector<std::vector<uint32_t>> storage;
        std::vector<CodeRange> ranges;
        for (size_t p = 0; p < phcount; ++p) {
            const auto pos = phoff + p * phsize;
            if (read<uint32_t>(bytes, pos) != 1 || (read<uint32_t>(bytes, pos + 4) & 5) != 5) continue;
            const auto offset = read<uint64_t>(bytes, pos + 8);
            const auto address = read<uint64_t>(bytes, pos + 16);
            const auto size = read<uint64_t>(bytes, pos + 32);
            assert(size % 4 == 0 && offset <= bytes.size() && size <= bytes.size() - offset);
            storage.emplace_back(size / 4);
            std::memcpy(storage.back().data(), bytes.data() + offset, size);
            ranges.push_back({address, storage.back()});
        }
        const auto original = resolve(ranges);
        assert(original);
        std::cout << argv[arg] << " scale=" << std::hex << original->scale
            << " animate=" << original->animate << " set=" << original->set
            << std::dec << " CID=" << original->layout.params_class_id;
        if (original->unlock) {
            std::cout << " unlockScale=" << std::hex << original->unlock->scale << std::dec;
        }
        std::cout << '\n';
        assert(original->layout.tagged_header_offset < 0);
        assert(original->layout.class_id_mask != 0);
        if (require_unlock) assert(original->unlock);
        if (original->unlock) {
            const auto &unlock = *original->unlock;
            assert(unlock.scale != original->scale && unlock.scale != original->animate
                && unlock.scale != original->set);
            assert(plausible_pointer_field(unlock.layout.state_widget_offset,
                original->layout.tagged_header_offset, 4));
            assert(plausible_pointer_field(unlock.layout.widget_cell_offset,
                original->layout.tagged_header_offset, 4));
            assert(plausible_pointer_field(unlock.layout.cell_container_offset,
                original->layout.tagged_header_offset, 8));
            // The setter is generic; only the exact call site proves the projection. Its LR must
            // be a real code address distinct from the setter entry.
            assert(unlock.call_return != 0 && unlock.call_return != unlock.scale);
            auto containers = unlock.layout.hotseat_containers;
            assert(std::ranges::all_of(containers, [](int64_t value) { return value < 0; }));
            std::ranges::sort(containers);
            assert(std::adjacent_find(containers.begin(), containers.end()) == containers.end());
            const auto contract = resolve_hook_targets(ranges, 0);
            assert(contract && contract->unlock_target);
            assert(contract->unlock_target->rva == unlock.scale);
            assert(contract->unlock_target->original_words.size() == kPatchTargetWords);
        }
        for (auto &range : ranges) range.address += 0x7123450000ULL;
        const auto relocated = resolve(ranges);
        assert(relocated && relocated->scale == original->scale + 0x7123450000ULL);
        assert(relocated->animate == original->animate + 0x7123450000ULL);
        assert(relocated->set == original->set + 0x7123450000ULL);
        assert(relocated->unlock.has_value() == original->unlock.has_value());
        if (original->unlock) {
            assert(relocated->unlock->scale
                == original->unlock->scale + 0x7123450000ULL);
            assert(relocated->unlock->call_return
                == original->unlock->call_return + 0x7123450000ULL);
            assert(same_unlock_layout(
                relocated->unlock->layout, original->unlock->layout));
        }
        // Duplicate identities must fail closed, never select the first match.
        auto duplicate = ranges;
        duplicate.push_back(ranges.front());
        assert(!resolve(duplicate));
        // Simulate HYOS retaining a patched old AOT mapping while switching to a
        // fresh runtime mapping of the same code. Patched entries no longer have
        // Dart prologues, so semantic resolution must select only the new copy.
        auto old_storage = storage;
        auto new_storage = storage;
        std::vector<CodeRange> old_ranges;
        std::vector<CodeRange> new_ranges;
        constexpr uintptr_t runtime_relocation = 0x2300000000ULL;
        for (size_t i = 0; i < ranges.size(); ++i) {
            old_ranges.push_back({ranges[i].address, old_storage[i]});
            new_ranges.push_back({ranges[i].address + runtime_relocation, new_storage[i]});
        }
        for (const auto target : {relocated->scale, relocated->animate, relocated->set}) {
            auto entry = at(old_ranges, target, 1);
            assert(!entry.empty());
            *const_cast<uint32_t *>(entry.data()) = 0;
        }
        auto switched_ranges = old_ranges;
        switched_ranges.insert(switched_ranges.end(), new_ranges.begin(), new_ranges.end());
        const auto switched = resolve(switched_ranges);
        assert(switched && switched->scale == relocated->scale + runtime_relocation);
        assert(switched->animate == relocated->animate + runtime_relocation);
        assert(switched->set == relocated->set + runtime_relocation);
        // This synthetic input deliberately keeps the unpatched optional unlock setter in the
        // retired copy while patching only its required recents triple. The optional resolver may
        // therefore fail closed on the two indistinguishable setters. Production resolves each
        // mapping generation independently; if this combined-image test does select one, it must
        // be the setter in the same (new) copy as the selected recents graph, never the retired one.
        if (switched->unlock) {
            assert(relocated->unlock);
            assert(switched->unlock->scale
                == relocated->unlock->scale + runtime_relocation);
            assert(same_unlock_layout(
                switched->unlock->layout, relocated->unlock->layout));
        }
        const auto scale = at(ranges, relocated->scale, 1);
        auto *instruction = const_cast<uint32_t *>(scale.data());
        const auto saved = *instruction;
        *instruction = 0;
        assert(!resolve(ranges));
        *instruction = saved;
    }
    std::cout << "Dynamic resolver tests passed\n";
}
