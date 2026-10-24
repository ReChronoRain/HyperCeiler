/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_prestart {
// Entry intercept, before the managed frame exists. The proved consumer is
// synchronous void: all original normal returns use the canonical null in x22.
// No Dart call, allocation, object write or retained managed pointer is needed.
template<class Registers>
inline bool apply(Registers& r,bool ready,bool authorized,uintptr_t original) {
    r.branch=original;
    if(!ready||!authorized)return false;
    r.x[0]=r.x[22];
    r.branch=0;
    return true;
}
}
