/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_geometry.h"
#include <cassert>
#include <cstdio>
#include <vector>

int main() {
    for (bool position : {false, true}) {
        const auto original = position
            ? std::span<const uint32_t>(home_layout::kFolderPositionOriginal)
            : std::span<const uint32_t>(home_layout::kFolderSizeOriginal);
        const auto changes = position
            ? std::span<const home_layout::CallWordVariant>(home_layout::kFolderPositionCalls7719)
            : std::span<const home_layout::CallWordVariant>(home_layout::kFolderSizeCalls7719);
        std::vector<uint32_t> body(original.begin(), original.end());
        assert(home_layout::folder_original_body_matches(body, position)); // Retain 7722.
        for (const auto &change : changes) body[change.byte_offset / 4] = change.instruction;
        assert(home_layout::folder_original_body_matches(body, position)); // Admit exact 7719.
        const auto known = body;
        body[2] ^= 0x400; // Change frame allocation while preserving a valid opcode.
        assert(!home_layout::folder_original_body_matches(body, position));
        body = known;
        body[changes[0].byte_offset / 4] ^= 1; // An arbitrary different BL is refused.
        assert(!home_layout::folder_original_body_matches(body, position));
        body = known;
        body[changes[0].byte_offset / 4] = original[changes[0].byte_offset / 4];
        assert(!home_layout::folder_original_body_matches(body, position)); // No mixed versions.
        body = known;
        body.pop_back();
        assert(!home_layout::folder_original_body_matches(body, position));
    }
    constexpr uint32_t original[] = {0xa9bf79fd, 0x94000001, 0xd65f03c0};
    constexpr uint32_t alternate[] = {0xa9bf79fd, 0x94000002, 0xd65f03c0};
    constexpr home_layout::CallWordVariant valid[] = {{4, 0x94000002}};
    constexpr home_layout::CallWordVariant unaligned[] = {{5, 0x94000002}};
    constexpr home_layout::CallWordVariant outside[] = {{12, 0x94000002}};
    constexpr home_layout::CallWordVariant non_call[] = {{0, 0x94000002}};
    constexpr home_layout::CallWordVariant duplicate[] = {{4, 0x94000002}, {4, 0x94000002}};
    assert(home_layout::exact_call_variant(alternate, original, valid));
    assert(!home_layout::exact_call_variant(alternate, original, unaligned));
    assert(!home_layout::exact_call_variant(alternate, original, outside));
    assert(!home_layout::exact_call_variant(alternate, original, non_call));
    assert(!home_layout::exact_call_variant(alternate, original, duplicate));
    std::puts("known call variants: 7719/7722 accepted; changed-frame/arbitrary-call/mixed/truncated/malformed refused PASS");
}
