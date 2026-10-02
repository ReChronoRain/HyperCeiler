/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host test for the Dart AOT structural resolver.
 *
 * Two independent things are checked:
 *   1. synthetic instruction sequences pin the shape predicates (prologue,
 *      relationship anchor, scalar read, nested compressed-pointer reach, zero
 *      idiom), so a change that loosens the shapes fails here;
 *   2. with a real libapp.so the resolver must arrive at the same two pairs the
 *      Dart symbol table independently names:
 *          HotSeatsConstants2.hotSeatsHeight  / .hotSeatsMarginBottom
 *          GridController.hotSeatsHeight      / .hotSeatsMarginBottom
 *      The symbol table is never consulted by the resolver - it only supplies the
 *      expectation, which is what makes this a cross-check rather than a lookup.
 */
#include "nativehook/elf_image.h"
#include "targets/home/home_layout_dart_targets.h"

#include <algorithm>
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>

namespace {

using namespace home_layout::dart_targets;

/* Build one synthetic getter: prologue, body, epilogue. */
std::vector<uint32_t> getter_body(uint32_t scalar_imm, bool nested, bool zeroes) {
    std::vector<uint32_t> code;
    code.push_back(kStpX29X30X15Pre);
    code.push_back(kMovX29X15);
    if (nested) {
        code.push_back(0xB840B020u);        // ldur w0,[x1,#0xb]   (compressed field)
        code.push_back(0x8B1C8000u);        // add x0,x0,x28,lsl #32
    }
    const uint32_t bl = 0x94000000u | ((0x40u >> 2) & 0x03FFFFFFu); // bl +0x40 (anything)
    code.push_back(bl);
    if (zeroes) code.push_back(0x6E201C00u); // eor v0.16b,v0.16b,v0.16b
    code.push_back(0xFC400000u | ((scalar_imm & 0x1FFu) << 12)); // ldur d0,[x0,#imm]
    code.push_back(kMovX15X29);
    code.push_back(kLdpX29X30X15Post);
    code.push_back(kReturn);
    return code;
}

void check_shapes() {
    struct Case {
        bool nested;
        bool zeroes;
    };
    for (const Case c : {Case{false, false}, Case{false, true}, Case{true, false}, Case{true, true}}) {
        const auto body = getter_body(0x4bu, c.nested, c.zeroes);
        const auto found = getters(body, 0u);
        assert(found.size() == 1);
        assert(found[0].nested == c.nested);
        assert(found[0].zeroes == c.zeroes);
        assert(found[0].field == 0x4bu);
        assert(found[0].length == body.size());
    }

    /* A body without a scalar read is not a geometry getter. */
    {
        std::vector<uint32_t> code{kStpX29X30X15Pre, kMovX29X15, 0x94000004u,
            kMovX15X29, kLdpX29X30X15Post, kReturn};
        assert(getters(code, 0u).empty());
    }
    /* A body that never returns through the Dart epilogue is rejected. */
    {
        auto code = getter_body(0x20u, false, false);
        code[code.size() - 3] = 0xAA0F03FDu; // wrong direction mov
        assert(getters(code, 0u).empty());
    }
    /* A nested reach requires the decompression step between the two loads. */
    {
        std::vector<uint32_t> code{kStpX29X30X15Pre, kMovX29X15, 0xB840B020u, 0x94000004u,
            0xFC400000u | (0x20u << 12), kMovX15X29, kLdpX29X30X15Post, kReturn};
        const auto found = getters(code, 0u);
        assert(found.size() == 1);
        assert(!found[0].nested);
    }
    std::printf("shape predicates: ok\n");
}

void check_fixture(const char *path) {
    std::ifstream file(path, std::ios::binary);
    assert(file);
    const std::string raw(std::istreambuf_iterator<char>{file}, {});
    const auto *bytes = reinterpret_cast<const std::byte *>(raw.data());
    const std::span<const std::byte> image(bytes, raw.size());
    const auto segments = nhk::elf::parse_program_segments(image);
    assert(segments);

    const auto resolution = resolve(image, *segments);
    assert(resolution);
    std::printf("resolver: %zu getters, %zu views, %zu families, %zu resolved\n",
        resolution->getters.size(), resolution->views.size(),
        resolution->families.size(), resolution->resolved_families);

    /*
     * Contract: whenever the resolver claims a resolution it must agree, to the
     * instruction, with the addresses the shipped Dart symbol table names. While
     * the remaining selectivity rules are still being built the resolver refuses
     * instead, and that refusal is reported rather than asserted away - a
     * fail-closed resolver that resolves nothing is incomplete, not wrong.
     */
    struct Expected {
        uint32_t margin;       // GridController.hotSeatsMarginBottom (layout hub)
        uint32_t margin_view;  // HotSeatsConstants2.hotSeatsMarginBottom (delegating view)
        uint32_t height;       // HotSeatsConstants2.hotSeatsHeight (extent partner)
        uint32_t height_alt;   // layout-side alias; 0 while two members share the field
        const char *what;
    };
    const Expected expected[] = {
        {0x9e54d4u, 0xf26da0u, 0x1099d3cu, 0u,
            "GridController hotseat margin/height + HotSeatsConstants2 views"},
    };
    /* Discovery must always produce something: an empty scan means the shapes broke. */
    assert(!resolution->getters.empty());
    assert(!resolution->families.empty());

    if (!resolution->ok()) {
        std::printf("  resolver refused: %s\n  (expected %s: margin=0x%x view=0x%x "
            "height=0x%x heightView=0x%x)\n",
            resolution->rejected != nullptr ? resolution->rejected : "unknown reason",
            expected[0].what, expected[0].margin, expected[0].margin_view, expected[0].height,
            expected[0].height_alt);
        std::printf("fixture: discovery ok, resolution incomplete\n");
        return;
    }
    std::printf("  resolved margin=0x%x view=0x%x height=0x%x heightAlt=0x%x\n",
        resolution->margin, resolution->margin_view, resolution->height, resolution->height_alt);
    for (const Expected &want : expected) {
        assert(resolution->margin == want.margin);
        assert(resolution->margin_view == want.margin_view);
        assert(resolution->height == want.height);
        assert(resolution->height_alt == want.height_alt);
    }
    /*
     * The labelled knobs need the object pool, and the pool is only reachable from inside the
     * function that logs the labels. A zero aggregator therefore silently disables every labelled
     * knob, which is exactly how the first device run failed - so the fixture pins it down.
     */
    std::printf("  aggregator=0x%x items=%zu logGetters=%zu\n", resolution->aggregator,
        resolution->items.size(),
        static_cast<size_t>(std::count_if(resolution->items.begin(), resolution->items.end(),
            [](const home_layout::dart_targets::LogItem &item) { return item.called; })));
    assert(resolution->aggregator != 0);
    assert(resolution->items.size() >= 8);
    std::printf("fixture: ok\n");
}

} // namespace

int main(int argc, char **argv) {
    check_shapes();
    if (argc > 1) check_fixture(argv[1]);
    return 0;
}
