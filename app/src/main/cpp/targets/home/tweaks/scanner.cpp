// SPDX-License-Identifier: Apache-2.0
#include "scanner.h"
#include "nativehook/memory_io.h"
#include "nativehook/native_image.h"

#include <array>
#include <span>
#include <string.h>

namespace hometweaks {
namespace {
constexpr size_t kMaxSignatureWords = 4096;
constexpr size_t kMaxComparisons = 256U * 1024U * 1024U;

bool SignatureOk(const FunctionSignature& sig) {
    return sig.wordCount > 0 && sig.wordCount <= kMaxSignatureWords &&
           sig.words != nullptr && sig.masks != nullptr;
}

// Masks mark ignored bits, not the bits to compare. The budget bounds even
// adversarial long common prefixes; exhaustion never establishes uniqueness.
bool MatchAt(const FunctionSignature& sig, const uint32_t* cand, size_t* budget) {
    for (size_t i = 0; i < sig.wordCount; ++i) {
        if (*budget == 0) return false;
        --*budget;
        if ((cand[i] & ~sig.masks[i]) != (sig.words[i] & ~sig.masks[i])) return false;
    }
    return true;
}
}

bool CodeView::ExecutableRangesOk() const {
    if (image_.base == 0 || (image_.base & 3u) != 0 || image_.segmentCount == 0 ||
        image_.segmentCount > std::size(image_.segments)) return false;
    size_t total = 0;
    bool found = false;
    for (size_t s = 0; s < image_.segmentCount; ++s) {
        const Segment& seg = image_.segments[s];
        if ((seg.flags & 1u) == 0) continue;
        if ((seg.flags & 7u) != 5u || seg.begin < image_.base || seg.end <= seg.begin ||
            (seg.begin & 3u) != 0 || (seg.end & 3u) != 0 ||
            uint64_t(seg.end - image_.base) > uint64_t(UINT32_MAX) + 1u) return false;
        const size_t bytes = seg.end - seg.begin;
        if (bytes > nhk::kMaxExecutableCodeBytes - total) return false;
        total += bytes;
        for (size_t j = 0; j < s; ++j) {
            const Segment& prev = image_.segments[j];
            if ((prev.flags & 1u) != 0 && seg.begin < prev.end && prev.begin < seg.end)
                return false;
        }
        found = true;
    }
    return found;
}

bool CodeView::RangeOk(uint32_t va, size_t length) const {
    if (!ExecutableRangesOk() || (va & 3u) != 0 || length == 0 || (length & 3u) != 0 ||
        uint64_t(length) > uint64_t(UINT32_MAX) + 1u - va ||
        image_.base > UINTPTR_MAX - va) return false;
    const uintptr_t address = image_.base + va;
    for (size_t s = 0; s < image_.segmentCount; ++s) {
        const Segment& seg = image_.segments[s];
        if ((seg.flags & 7u) == 5u && address >= seg.begin && address < seg.end &&
            length <= seg.end - address) return true;
    }
    return false;
}

bool CodeView::ReadWords(uint32_t va, uint32_t* out, size_t count) const {
    if (out == nullptr || count == 0 || count > kReadChunkWords ||
        !RangeOk(va, count * sizeof(uint32_t))) return false;
    // Caller-owned scratch may be partially filled when the OS reports a fault.
    // No live-memory pointer is dereferenced or retained by this code view.
    return nhk::safe_read(image_.base + va,
        std::as_writable_bytes(std::span(out, count)));
}

bool CodeView::Word(uint32_t va, uint32_t* out) const {
    if (out == nullptr) return false;
    uint32_t word = 0;
    if (!ReadWords(va, &word, 1)) return false;
    *out = word;
    return true;
}

bool CodeView::InText(uint32_t va) const { return RangeOk(va, 4); }

bool CodeView::FindSignature(const FunctionSignature& sig, uint32_t* outVa,
                             int* outMatches) const {
    if (outMatches != nullptr) *outMatches = -1; // invalid/incomplete, not zero hits
    if (!SignatureOk(sig) || !ExecutableRangesOk()) return false;
    const size_t count = sig.wordCount;
    std::array<uint32_t, kReadChunkWords> scratch;
    size_t budget = kMaxComparisons;
    int matches = 0;
    uint32_t first = 0;
    for (size_t s = 0; s < image_.segmentCount; ++s) {
        const Segment& seg = image_.segments[s];
        if ((seg.flags & 1u) == 0) continue;
        const size_t words = (seg.end - seg.begin) / 4;
        const uint32_t start = static_cast<uint32_t>(seg.begin - image_.base);
        size_t offset = 0;
        while (words - offset >= count) {
            const size_t copied = std::min(kReadChunkWords, words - offset);
            const uint32_t va = start + static_cast<uint32_t>(offset * 4);
            if (!ReadWords(va, scratch.data(), copied)) return false;
            const size_t slots = copied - count + 1;
            for (size_t i = 0; i < slots; ++i) {
                const bool matched = MatchAt(sig, scratch.data() + i, &budget);
                if (budget == 0) return false;
                if (!matched) continue;
                ++matches;
                if (matches == 1) first = va + static_cast<uint32_t>(i * 4);
                if (matches > 1) {
                    if (outMatches != nullptr) *outMatches = matches;
                    return false;
                }
            }
            // Re-read the count-1 trailing words so cross-chunk matches are
            // considered exactly once. This buffer never becomes a live cache.
            offset += slots;
        }
    }
    if (outMatches != nullptr) *outMatches = matches;
    if (matches != 1) return false;
    if (outVa != nullptr) *outVa = first;
    return true;
}

bool CodeView::MatchSignatureAt(const FunctionSignature& sig, uint32_t va) const {
    if (!SignatureOk(sig)) return false;
    std::array<uint32_t, kMaxSignatureWords> scratch;
    if (!ReadWords(va, scratch.data(), sig.wordCount)) return false;
    size_t budget = sig.wordCount;
    return MatchAt(sig, scratch.data(), &budget);
}

const FunctionSignature* FindSignatureByName(const char* needle) {
    if (needle == nullptr || *needle == '\0') return nullptr;
    const FunctionSignature* exact = nullptr;
    const FunctionSignature* partial = nullptr;
    size_t exactCount = 0, partialCount = 0;
    for (size_t i = 0; i < kSignatureCount; ++i) {
        const FunctionSignature& sig = kSignatures[i];
        if (sig.name == nullptr) continue;
        if (strcmp(sig.name, needle) == 0) { exact = &sig; ++exactCount; }
        if (strstr(sig.name, needle) != nullptr) { partial = &sig; ++partialCount; }
    }
    if (exactCount != 0) return exactCount == 1 ? exact : nullptr;
    return partialCount == 1 ? partial : nullptr;
}
}
