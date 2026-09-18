// SPDX-License-Identifier: Apache-2.0
#include "scanner.h"

#include <string.h>

namespace hometweaks {
namespace {

inline bool MatchAt(const uint32_t* words, const uint32_t* masks,
                    const uint32_t* cand, size_t count) {
    for (size_t i = 0; i < count; ++i) {
        const uint32_t mask = masks[i];
        if ((cand[i] & ~mask) != (words[i] & ~mask)) return false;
    }
    return true;
}

}

bool CodeView::RangeOk(uint32_t va, size_t length) const {
    const uintptr_t address = image_.base + static_cast<uintptr_t>(va);
    return RangeInImage(image_, address, length);
}

bool CodeView::Word(uint32_t va, uint32_t* out) const {
    if (!RangeOk(va, 4)) return false;
    const uintptr_t address = image_.base + static_cast<uintptr_t>(va);
    *out = *reinterpret_cast<const volatile uint32_t*>(address);
    return true;
}

bool CodeView::InText(uint32_t va) const { return RangeOk(va, 4); }

bool CodeView::FindSignature(const FunctionSignature& sig, uint32_t* outVa,
                             int* outMatches) const {
    const size_t count = sig.wordCount;
    if (count == 0) return false;

    int matches = 0;
    uint32_t first = 0;

    for (size_t s = 0; s < image_.segmentCount; ++s) {
        const Segment& seg = image_.segments[s];
        if ((seg.flags & 0x1u) == 0) continue;
        if (seg.end <= seg.begin) continue;

        const uint32_t segVaBegin = static_cast<uint32_t>(seg.begin - image_.base);
        const uintptr_t bytes = seg.end - seg.begin;
        if (bytes < count * 4) continue;

        const size_t slots = (bytes / 4) - count + 1;
        const uint32_t* base = reinterpret_cast<const uint32_t*>(seg.begin);

        for (size_t i = 0; i < slots; ++i) {
            if (!MatchAt(sig.words, sig.masks, base + i, count)) continue;
            ++matches;
            if (matches == 1) {
                first = segVaBegin + static_cast<uint32_t>(i * 4);
            }
            if (matches > 1) break;
        }
        if (matches > 1) break;
    }

    if (outMatches != nullptr) *outMatches = matches;
    if (matches != 1) return false;
    if (outVa != nullptr) *outVa = first;
    return true;
}

bool CodeView::MatchSignatureAt(const FunctionSignature& sig, uint32_t va) const {
    const size_t count = sig.wordCount;
    if (count == 0) return false;
    const uintptr_t address = image_.base + static_cast<uintptr_t>(va);
    if (!RangeInImage(image_, address, count * 4)) return false;
    const uint32_t* cand = reinterpret_cast<const uint32_t*>(address);
    return MatchAt(sig.words, sig.masks, cand, count);
}

const FunctionSignature* FindSignatureByName(const char* needle) {
    if (needle == nullptr) return nullptr;
    for (size_t i = 0; i < kSignatureCount; ++i) {
        const FunctionSignature& sig = kSignatures[i];
        if (sig.name != nullptr && strstr(sig.name, needle) != nullptr) return &sig;
    }
    return nullptr;
}

}
