/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_render.h"
namespace home_layout::drawer_render {
// The final size wrapper has a nullable child. Replace only its last-use stack
// root; original width, allocation, GC PCs and all four stores remain managed.
inline bool apply(recent_render::Registers& r,bool ready,bool hidden,uintptr_t original,int32_t child_frame) {
    r.branch=original;
    if(!ready||!hidden||!original||!r.x[29]||(r.x[29]&7)
        ||child_frame>=0||child_frame<-16384||(child_frame&7))return false;
    recent_render::store<uint64_t>(r.x[29]+child_frame,r.x[22]);return true;
}
// At the Finder holder's entry, its managed frame has not been entered. The
// gateway restores SP/X15/X29/X30 and then branches to the stock empty-size
// allocator. Its GC slow path executes on the original Dart stack after this
// native frame is gone. No native allocation or C-to-Dart call occurs here.
inline bool finder(recent_render::Registers& r,bool ready,bool hidden,uintptr_t original,uintptr_t factory) {
    r.branch=original;
    if(!ready||!hidden||!original||!factory||(factory&3))return false;
    r.branch=factory;return true;
}

}
