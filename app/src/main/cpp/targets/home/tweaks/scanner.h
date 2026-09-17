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
