/* SPDX-License-Identifier: AGPL-3.0-or-later */
// A launcher child must be able to retry symbol loading after discarding inherited state.
#include "targets/home/tweaks/symtab.h"
#include <cassert>
#include <cstdio>

namespace hometweaks {
static unsigned reads = 0;
bool ReadImageFile(const Image &, uint64_t, void *, size_t) {
    ++reads;
    return false;
}
}

int main() {
    hometweaks::Image image{};
    auto &index = hometweaks::SymbolIndex::Instance();
    assert(!index.EnsureLoaded(image));
    assert(hometweaks::reads == 1);
    assert(!index.EnsureLoaded(image));
    assert(hometweaks::reads == 1); // Same process failure remains cached.
    index.ResetForTest();           // Production launcher-child preparation calls this too.
    assert(!index.EnsureLoaded(image));
    assert(hometweaks::reads == 2); // A new child must get a new read attempt.
    std::puts("symbol child reset: cached-failure/retry PASS");
}
