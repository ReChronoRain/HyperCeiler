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

constexpr size_t kMaxTargetSlots = 189;

class SymbolIndex {
public:
    static SymbolIndex& Instance();

    // A nonzero startup budget resets incomplete roots on timeout; workers use zero.
    bool EnsureLoaded(const Image& image, uint64_t budgetMs = 0);
    // Loader path: cached original image only, never XZ/signature scanning.
    bool TryLoadCached(const Image& image, uint64_t imageId = 0);

    bool loaded() const { return loaded_; }

    /*
     * True once EnsureLoaded has taken the work on, whether or not it succeeded. Callers that run on
     * the launcher main thread use this to decide "somebody is already doing this" without touching
     * the loader lock: calling EnsureLoaded from a dlopen callback parses the whole .gnu_debugdata
     * (tens of MB plus an xz pass) while holding the linker lock, which is what ANR'd the desktop.
     */
    bool attempted() const { return attempted_; }

    const char* status() const { return status_; }

    int foundCount() const { return foundCount_; }

    bool Find(const char* needle, uint32_t* va, uint32_t* size) const;

    /* Distance to the next function in the complete ELF symbol table, not the target whitelist. */
    bool SpanFrom(uint32_t va, uint32_t* span) const;

    bool Has(const char* needle) const;

    void ResetForTest();

private:
    SymbolIndex() = default;
    bool SaveCached(const Image& image);

    // Persistence is attempted only once per process; failed I/O never repeats per lookup.
    bool cachePersistTried_ = false;
    bool loaded_ = false;
    bool attempted_ = false;
    int foundCount_ = 0;
    char status_[192]{};
    uint32_t va_[kMaxTargetSlots]{};
    uint32_t size_[kMaxTargetSlots]{};
    uint32_t span_[kMaxTargetSlots]{};
    bool has_[kMaxTargetSlots]{};
};

}
