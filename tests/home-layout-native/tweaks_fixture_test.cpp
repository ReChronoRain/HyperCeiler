/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Read-only host audit: map bytes to ELF virtual addresses without executable permissions,
// then exercise the production symbol index and patch planner against this actual OTA.
#include "nativehook/elf_image.h"
#include "targets/home/tweaks/ht_plan.h"
#include "targets/home/tweaks/symtab.h"
#include "targets/home/home_hotseat_capacity.h"
#include "targets/home/home_folder_geometry.h"
#include <cassert>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <iterator>
#include <vector>

namespace hometweaks {
bool HomeTweaksFindSymbol(const char *name, uint32_t *va, uint32_t *size) {
    return SymbolIndex::Instance().Find(name, va, size);
}
}
static const hometweaks::Image *fixture_image = nullptr;
bool dart_words(uint32_t va, size_t count, std::vector<uint32_t> &out) {
    assert(fixture_image);
    hometweaks::CodeView code(*fixture_image);
    out.resize(count);
    for (size_t i = 0; i < count; ++i) {
        if (!code.Word(va + static_cast<uint32_t>(i * 4), &out[i])) return false;
    }
    return true;
}
#include "production_compatibility_guards.inc"

unsigned check_layout_guards() {
    auto &index = hometweaks::SymbolIndex::Instance();
    unsigned failures = 0;
    const auto check = [&](const char *name, auto guard) {
        uint32_t va = 0, size = 0;
        const bool found = index.Find(name, &va, &size);
        const bool ok = found && guard(va, size);
        std::printf("layout guard %s symbol=%s va=%#x size=%#x\n",
            ok ? "PASS" : "FAIL", name, va, size);
        if (!ok) ++failures;
    };
    check("GridCellDelegate.performLayout", [](uint32_t va, uint32_t size) {
        return workspace_geometry_code_compatible(va, size, false);
    });
    check("GridOccupiedCellDelegate.performLayout", [](uint32_t va, uint32_t size) {
        return workspace_geometry_code_compatible(va, size, true);
    });
    check("HotSeatLayoutDelegate.cellLayout", hotseat_geometry_code_compatible);
    check("LauncherIndicatorState._buildScreenIndicator", indicator_policy_compatible);
    check("LauncherIndicatorState._wrapWithAnimation", [](uint32_t va, uint32_t size) {
        uint32_t caller = 0;
        return capsule_wrapper_layout_compatible(va, size, &caller);
    });
    for (const auto &site : home_layout::kCapacitySites) {
        check(site.symbol, [&](uint32_t va, uint32_t size) {
            std::vector<uint32_t> words;
            return size == site.size && site.offset >= 12 && site.offset + 4 <= size
                && dart_words(va + site.offset - 12, 4, words)
                && std::equal(std::begin(site.guard), std::end(site.guard), words.begin());
        });
    }
    for (size_t i = 0; i < std::size(home_layout::kFolderGeometrySites); ++i) {
        const auto &site = home_layout::kFolderGeometrySites[i];
        check(site.symbol, [&](uint32_t va, uint32_t size) {
            std::vector<uint32_t> words;
            const auto full = i < 2 || i == 4
                ? std::span<const uint32_t>(home_layout::kFolderPositionOriginal)
                : std::span<const uint32_t>(home_layout::kFolderSizeOriginal);
            return size == site.size && dart_words(va + site.offset, 4, words)
                && std::equal(std::begin(site.words), std::end(site.words), words.begin())
                && dart_words(va, full.size(), words)
                && home_layout::folder_original_body_matches(words, i < 2 || i == 4);
        });
    }
    return failures;
}

void check_known_guard_regressions() {
    auto &index = hometweaks::SymbolIndex::Instance();
    const auto address = [&](const char *name, uint32_t offset) {
        uint32_t va = 0, size = 0;
        assert(index.Find(name, &va, &size));
        // Some Dart public symbols are short entry wrappers; production validates
        // a longer owning body separately, so use the same mapped code boundary.
        assert(hometweaks::RangeInImage(*fixture_image, fixture_image->base + va + offset, 4));
        return reinterpret_cast<uint32_t *>(fixture_image->base + va + offset);
    };
    uint32_t dock_va = 0, dock_size = 0, wrapper_va = 0, wrapper_size = 0;
    assert(index.Find("HotSeatLayoutDelegate.cellLayout", &dock_va, &dock_size));
    assert(index.Find("LauncherIndicatorState._wrapWithAnimation", &wrapper_va, &wrapper_size));
    const auto capsule_ok = [&] {
        uint32_t caller = 0;
        return capsule_wrapper_layout_compatible(wrapper_va, wrapper_size, &caller);
    };
    // Both mutations affect non-call instructions consumed by a trampoline.
    auto *dock_frame = address("HotSeatLayoutDelegate.cellLayout", 0x08);
    uint32_t saved = *dock_frame;
    *dock_frame ^= 0x400;
    assert(!hotseat_geometry_code_compatible(dock_va, dock_size));
    *dock_frame = saved;
    auto *capsule_move = address("LauncherIndicatorState.build", 0x220);
    saved = *capsule_move;
    *capsule_move ^= 1;
    assert(!capsule_ok());
    *capsule_move = saved;
    // Restore the original 7722 call expectations in this heap-only synthetic
    // copy and prove the prior guard path is retained. No ARM64 is executed.
    constexpr home_layout::CallWordVariant dock_original[] = {
        {0x18, 0x9411837c}, {0x230, 0x941187bc}, {0x314, 0x97c6e9ec},
        {0x340, 0x94118778}, {0x54c, 0x97c6c09e}};
    std::vector<uint32_t> saved_dock;
    for (const auto &call : dock_original) {
        auto *word = address("HotSeatLayoutDelegate.cellLayout", call.byte_offset);
        saved_dock.push_back(*word);
        *word = call.instruction;
    }
    assert(hotseat_geometry_code_compatible(dock_va, dock_size));
    auto *array_call = address("LauncherIndicatorState.build", 0x22c);
    saved = *array_call;
    *array_call = 0x94278e38;
    assert(capsule_ok());
    *array_call ^= 1;
    assert(!capsule_ok()); // Unknown BL displacement cannot pass.
    *array_call = saved;
    for (size_t i = 0; i < std::size(dock_original); ++i) {
        *address("HotSeatLayoutDelegate.cellLayout", dock_original[i].byte_offset) = saved_dock[i];
    }
    auto *dock_call = address("HotSeatLayoutDelegate.cellLayout", 0x18);
    saved = *dock_call;
    *dock_call ^= 1;
    assert(!hotseat_geometry_code_compatible(dock_va, dock_size));
    *dock_call = saved;
    assert(hotseat_geometry_code_compatible(dock_va, dock_size) && capsule_ok());
    std::puts("dock/capsule guards: 7719 fixture and original 7722 call expectations accepted; "
              "frame/non-call/unknown-call mutations rejected PASS");
}

int main(int argc, char **argv) {
    if (argc != 2) {
        std::fprintf(stderr, "Usage: tweaks_fixture_test <libapp.so>\n");
        return 2;
    }
    std::ifstream file(argv[1], std::ios::binary);
    assert(file);
    std::string raw(std::istreambuf_iterator<char>{file}, {});
    std::span<const std::byte> bytes(reinterpret_cast<const std::byte *>(raw.data()), raw.size());
    const auto segments = nhk::elf::parse_program_segments(bytes);
    assert(segments);
    uint64_t extent = 0;
    for (const auto &s : *segments) {
        if (s.type != nhk::elf::kProgramTypeLoad) continue;
        assert(s.memsz >= s.filesz && s.vaddr + s.memsz < (1u << 30));
        extent = std::max(extent, s.vaddr + s.memsz);
    }
    assert(extent > 0);
    std::vector<std::byte> mapped(extent);
    hometweaks::Image image{};
    image.base = reinterpret_cast<uintptr_t>(mapped.data());
    const size_t path_size = strnlen(argv[1], sizeof(image.path));
    if (path_size == sizeof(image.path)) {
        std::fprintf(stderr, "Fixture path exceeds image path capacity\n");
        return 2;
    }
    std::copy_n(argv[1], path_size + 1, image.path);
    for (const auto &s : *segments) {
        if (s.type != nhk::elf::kProgramTypeLoad) continue;
        assert(nhk::in_image(bytes.size(), s.offset, s.filesz));
        assert(image.segmentCount < std::size(image.segments));
        std::memcpy(mapped.data() + s.vaddr, bytes.data() + s.offset, s.filesz);
        image.segments[image.segmentCount++] = {image.base + s.vaddr,
            image.base + s.vaddr + s.memsz, s.flags};
    }
    fixture_image = &image;
    auto &index = hometweaks::SymbolIndex::Instance();
    const bool loaded = index.EnsureLoaded(image);
    std::printf("symbols: %s\n", index.status());
    assert(loaded);
    for (size_t i = 0; i < hometweaks::TargetFunctionCount(); ++i) {
        const auto &target = hometweaks::TargetFunctionAt(i);
        uint32_t va = 0, size = 0;
        const bool found = index.Find(target.needle, &va, &size);
        std::printf("symbol %s va=%#x size=%#x %s\n", found ? "PASS" : "MISS", va, size,
            target.fullName);
    }
    hometweaks::SetForceSymbolOnlyForTest(true);
    hometweaks::CodeView code(image);
    unsigned failures = 0;
    for (const uint32_t feature : {4u, 9u, 16u, 18u, 19u, 20u, 21u}) {
        hometweaks::Config config{};
        config.flags = 1;
        config.enabled = {feature};
        config.folderCols = 5;
        config.phoneCols = 6;
        config.phoneRows = 7;
        hometweaks::LocatedSites sites{};
        hometweaks::LocateSites(code, config, &sites);
        const bool located = hometweaks::FeatureOk(sites, feature);
        const bool shape = located && hometweaks::ValidateSitesShape(code, sites);
        hometweaks::PlanResult plan{};
        if (shape) hometweaks::DerivePatches(code, config, sites, &plan);
        const char *why = "";
        bool planned = false;
        switch (feature) {
        case 4: planned = plan.ok4; why = located ? plan.why4 : sites.why4; break;
        case 9: planned = plan.ok9; why = located ? plan.why9 : sites.why9; break;
        case 16: planned = plan.ok16; why = located ? plan.why16 : sites.why16; break;
        case 18: planned = plan.ok18; why = located ? plan.why18 : sites.why18; break;
        case 19: planned = plan.ok19 && plan.ok19r; why = located ? plan.why19r : sites.why19; break;
        case 20: planned = plan.ok20; why = located ? plan.why20 : sites.why20; break;
        case 21: planned = plan.ok21; why = located ? plan.why21 : sites.why21; break;
        }
        bool bytes_match = true;
        for (const auto &patch : plan.patches) {
            uint32_t original = 0;
            bytes_match &= code.Word(patch.va, &original) && original == patch.expect;
        }
        const bool ok = located && shape && planned && bytes_match && !plan.patches.empty();
        std::printf("feature %u %s located=%d shape=%d planned=%d patches=%zu bytes=%d %s\n",
            feature, ok ? "PASS" : "FAIL", located, shape, planned, plan.patches.size(),
            bytes_match, why);
        if (!ok) ++failures;
    }
    failures += check_layout_guards();
    if (failures == 0) check_known_guard_regressions();
    const int found_before_reset = index.foundCount();
    index.ResetForTest();
    assert(index.EnsureLoaded(image));
    assert(index.foundCount() == found_before_reset);
    std::puts("symbol child reset: real-image reload PASS");
    // Do not report unsupported features as a successful compatibility test.
    return failures == 0 ? 0 : 1;
}
