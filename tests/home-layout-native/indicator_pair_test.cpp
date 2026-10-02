/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_indicator_pair.h"
#include <cassert>
#include <cstdio>
#include <limits>

int main() {
    alignas(16) unsigned char space[512]{};
    const auto base = reinterpret_cast<uint64_t>(space);
    uint64_t capsule = base + 1, dots = base + 0x51, top = base + 0xa0;
    const uint64_t heap = (base >> 32) | (uint64_t{1} << 32); // x28 barrier mask must be ignored
    const uint64_t dart_null = ((base >> 32) << 32) | 0x71;
    const uint64_t head = 0x218341cu;
    const uint32_t child = static_cast<uint32_t>(base + 0x1c1);
    std::memcpy(space, &head, 8); std::memcpy(space + 0x50, &head, 8);
    std::memcpy(space + 0x38, &child, 4); std::memcpy(space + 0x88, &child, 4);
    unsigned char before[0xa0]; std::memcpy(before, space, sizeof(before));
    const auto original_capsule = capsule, original_dots = dots, original_top = top;
    const double deltas[] = {-630.0, -80.0, 80.0, 370.0};
    for (double delta : deltas) {
        capsule = original_capsule; dots = original_dots; top = original_top;
        assert(hc::indicator::wrap_pair(capsule, dots, top, base + sizeof(space), heap, dart_null, delta));
        assert(top == original_top + 0xa0 && capsule == original_top + 0x31 && dots == capsule + 0x50);
        double a = 0, b = 0, bottom = 0;
        std::memcpy(&a, reinterpret_cast<const void *>(original_top + 0x10), 8);
        std::memcpy(&b, reinterpret_cast<const void *>(original_top + 0x60), 8);
        std::memcpy(&bottom, reinterpret_cast<const void *>(original_top + 0x20), 8);
        assert(a == delta && b == delta && bottom == -delta);
        uint32_t wrapped = 0;
        std::memcpy(&wrapped, reinterpret_cast<const void *>(capsule + 0xb), 4);
        assert(wrapped == static_cast<uint32_t>(original_capsule));
        std::memcpy(&wrapped, reinterpret_cast<const void *>(dots + 0xb), 4);
        assert(wrapped == static_cast<uint32_t>(original_dots));
        assert(std::memcmp(space, before, sizeof(before)) == 0);
    }
    const double bad_values[] = {0.0, -631.0, 371.0, std::numeric_limits<double>::infinity(),
        std::numeric_limits<double>::quiet_NaN()};
    for (double bad : bad_values) {
        capsule = original_capsule; dots = original_dots; top = original_top;
        assert(!hc::indicator::wrap_pair(capsule, dots, top, base + sizeof(space), heap, dart_null, bad));
        assert(capsule == original_capsule && dots == original_dots && top == original_top);
    }
    capsule = original_capsule; dots = original_dots; top = original_top;
    assert(!hc::indicator::wrap_pair(capsule, dots, top, top + 0xa0, heap, dart_null, 10));
    assert(!hc::indicator::wrap_pair(capsule, dots, top, top - 1, heap, dart_null, 10));
    dots = dart_null;
    assert(!hc::indicator::wrap_pair(capsule, dots, top, base + sizeof(space), heap, dart_null, 10));
    assert(capsule == original_capsule && top == original_top);
    // Real launcher 7722 frame: both wrappers are valid, but bump top ends in 0x28.
    // Exercise both legal word-alignment classes, including the previous regression.
    const uint64_t offsets[] = {0xa0, 0xa8};
    for (uint64_t offset : offsets) {
        capsule = original_capsule; dots = original_dots; top = base + offset;
        assert(hc::indicator::wrap_pair(capsule, dots, top, base + sizeof(space), heap, dart_null, -114));
        assert(top == base + offset + 0xa0);
        assert(capsule == base + offset + 0x31 && dots == capsule + 0x50);
    }
    capsule = original_capsule; dots = original_dots; top = original_top + 4;
    assert(!hc::indicator::wrap_pair(capsule, dots, top, base + sizeof(space), heap, dart_null, -114));
    assert(capsule == original_capsule && dots == original_dots && top == original_top + 4);
    puts("indicator pair: equal displacement / zero-sum / originals intact / bounds / atomic skip PASS");
}
