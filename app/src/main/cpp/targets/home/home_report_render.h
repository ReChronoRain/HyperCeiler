/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_render.h"
namespace home_layout::report_render {
inline bool apply(recent_render::Registers& saved,bool ready,bool hidden,uintptr_t original) {
    saved.branch=original;
    if(!ready||!hidden||!original)return false;
    saved.x[0]=saved.x[22]+0x30; // Verified Dart false; no managed frame has been entered.
    saved.branch=0;
    return true;
}
}
