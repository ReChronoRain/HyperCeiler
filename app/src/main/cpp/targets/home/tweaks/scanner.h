// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "common.h"
#include "image.h"
#include "signatures.h"

#include <vector>

namespace hometweaks {

class CodeView {
public:
    explicit CodeView(const Image& image) : image_(image) {}

    // 64 KiB per live read, bounded temporary storage (never a stale code cache).
    static constexpr size_t kReadChunkWords = 16384;
    bool ExecutableRangesOk() const;
    bool ReadWords(uint32_t va, uint32_t* out, size_t count) const;
    bool Word(uint32_t va, uint32_t* out) const;

    bool InText(uint32_t va) const;

    bool FindSignature(const FunctionSignature& sig, uint32_t* outVa, int* outMatches = nullptr) const;

    bool MatchSignatureAt(const FunctionSignature& sig, uint32_t va) const;

    const Image& image() const { return image_; }

private:
    bool RangeOk(uint32_t va, size_t length) const;
    const Image& image_;
};

const FunctionSignature* FindSignatureByName(const char* needle);

}
