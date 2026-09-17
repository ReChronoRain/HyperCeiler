// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "common.h"
#include "image.h"

namespace hometweaks {

struct TargetFunction {
    const char* needle;
    const char* fullName;
    const char* label;
};

size_t TargetFunctionCount();
const TargetFunction& TargetFunctionAt(size_t index);

constexpr size_t kMaxTargetSlots = 96;

class SymbolIndex {
public:
    static SymbolIndex& Instance();

    bool EnsureLoaded(const Image& image);

    bool loaded() const { return loaded_; }

    const char* status() const { return status_; }

    int foundCount() const { return foundCount_; }

    bool Find(const char* needle, uint32_t* va, uint32_t* size) const;

    bool Has(const char* needle) const;

    void ResetForTest();

private:
    SymbolIndex() = default;

    bool loaded_ = false;
    bool attempted_ = false;
    int foundCount_ = 0;
    char status_[192]{};
    uint32_t va_[kMaxTargetSlots]{};
    uint32_t size_[kMaxTargetSlots]{};
    bool has_[kMaxTargetSlots]{};
};

}
