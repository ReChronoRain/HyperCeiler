/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstring>
#include <cmath>
#include "home_dynamic_layout.h"

namespace hc::indicator {
struct InsetsLayout {
    uint32_t cid=0,size=0;
    std::array<int32_t,4> field{}; // left/top/right/bottom, byte offsets on tagged Insets.
    int32_t root=0,source=0;
    bool usable()const {
        if(!cid||size<40||size>240||!home_dynamic::slot(root)||!home_dynamic::slot(source))return false;
        for(unsigned i=0;i<4;++i){
            if(!home_dynamic::field(field[i])||field[i]<7||unsigned(field[i]+9)>size)return false;
            for(unsigned j=0;j<i;++j)if(std::abs(field[i]-field[j])<8)return false;
        }
        return true;
    }
};
// Rect deflation pairs each coordinate with the matching Insets: two additions
// (left/top), two subtractions (right/bottom), then returns the corresponding Rect.
// Registers/arithmetic define the roles; every field and spill is decoded.
inline bool inset_fields(const home_dynamic::Body& b,std::array<int32_t,4>& out) {
    using namespace home_dynamic;
    const uint32_t op[]={0x1e612802u,0x1e612803u,0x1e613804u,0x1e613805u};
    std::array<int32_t,4> rect{},local{},fields{};std::array<unsigned,4> hits{},uses{};
    for(size_t i=0;i+3<b.size();++i)for(unsigned role=0;role<4;++role) {
        if(ldd(b[i],2,0)&&ldd(b[i+1],1,1)&&b[i+2]==op[role]
            &&std_(b[i+3],29,role+2)&&frame_slot(b,imm9(b[i+3]))) {
            fields[role]=imm9(b[i+1]);rect[role]=imm9(b[i]);local[role]=imm9(b[i+3]);++hits[role];
        }
        if(ldd(b[i],29,0)&&std_(b[i+1],0,0)&&imm9(b[i])==local[role]
            &&imm9(b[i+1])==rect[role])++uses[role];
    }
    for(unsigned i=0;i<4;++i){
        if(hits[i]!=1||uses[i]!=1||!field(fields[i])||fields[i]<7)return false;
        for(unsigned j=0;j<i;++j)if(std::abs(fields[i]-fields[j])<8||rect[i]==rect[j]||local[i]==local[j])return false;
    }
    out=fields;return true;
}
// The owning Workspace._createIndicator branch has already allocated this EdgeInsets
// through its original Dart BL (including its normal GC slow path), and has written
// left/top/right = 0 and bottom = the original unboxed frame local. Change only
// this fresh Insets, AFTER its original bottom store. ORIGINAL_INDICATOR_FRAME_UNCHANGED:
// The decoded source local also feeds the original animation and Container height.
// Changing it couples the user position to subsequent layout/animation rebuilds.
// Never reserve TLAB memory, retain a pointer, or change that shared scalar local.
inline bool inset_original_frame(uintptr_t frame, uint64_t heap, uint64_t edge,
    uint64_t dart_null, double delta, const InsetsLayout& layout) {
    if (!layout.usable()||frame < uintptr_t(-layout.source) || (frame & 7u) || (edge & 7u) != 1u || edge == dart_null
        || (edge >> 32) != static_cast<uint32_t>(heap)
        || !std::isfinite(delta) || delta < -630 || delta > 370 || delta == 0) return false;
    uint64_t header = 0;
    std::memcpy(&header, reinterpret_cast<const void *>(edge - 1), 8);
    if (((header >> 12) & 0xfffffu) != layout.cid) return false;
    double left = 0, top = 0, right = 0, bottom = 0, original_bottom = 0;
    std::memcpy(&left, reinterpret_cast<const void *>(edge + layout.field[0]), 8);
    std::memcpy(&top, reinterpret_cast<const void *>(edge + layout.field[1]), 8);
    std::memcpy(&right, reinterpret_cast<const void *>(edge + layout.field[2]), 8);
    std::memcpy(&bottom, reinterpret_cast<const void *>(edge + layout.field[3]), 8);
    std::memcpy(&original_bottom, reinterpret_cast<const void *>(frame + layout.source), 8);
    if (left != 0 || top != 0 || right != 0 || !std::isfinite(bottom)
        || bottom < 0 || bottom > 4096 || bottom != original_bottom) return false;
    bottom -= delta;
    std::memcpy(reinterpret_cast<void *>(edge + layout.field[1]), &delta, 8);
    std::memcpy(reinterpret_cast<void *>(edge + layout.field[3]), &bottom, 8);
    return true;
}
// Historical pair-allocation fixture remains below for regression comparison only.
// The production entry no longer calls it: a low TLAB must never drop a position update.
#ifdef HC_INDICATOR_LEGACY_FIXTURE
inline bool wrapper_valid(uint64_t object, uint64_t heap, uint64_t dart_null) {
    if ((object & 1u) == 0 || object == dart_null || (object >> 32) != static_cast<uint32_t>(heap)) return false;
    uint64_t header = 0;
    std::memcpy(&header, reinterpret_cast<const void *>(object - 1), 8);
    if (((header >> 12) & 0xfffffu) != 0x2183) return false;
    uint32_t child = 0;
    std::memcpy(&child, reinterpret_cast<const void *>(object + 0x37), 4);
    return (child & 1u) != 0 && child != static_cast<uint32_t>(dart_null);
}
inline uint64_t padding(uint64_t start, uint64_t child, uint64_t dart_null, double delta) {
    std::memset(reinterpret_cast<void *>(start), 0, 0x50);
    const uint64_t edge_header = 0x15c931cu, padding_header = 0x1e2521cu;
    const uint64_t edge = start + 1, widget = start + 0x31;
    const double bottom = -delta;
    std::memcpy(reinterpret_cast<void *>(start), &edge_header, 8);
    std::memcpy(reinterpret_cast<void *>(edge + 0xf), &delta, 8);
    std::memcpy(reinterpret_cast<void *>(edge + 0x1f), &bottom, 8);
    std::memcpy(reinterpret_cast<void *>(widget - 1), &padding_header, 8);
    const uint32_t key = static_cast<uint32_t>(dart_null);
    const uint32_t compressed_child = static_cast<uint32_t>(child);
    const uint32_t compressed_edge = static_cast<uint32_t>(edge);
    std::memcpy(reinterpret_cast<void *>(widget + 7), &key, 4);
    std::memcpy(reinterpret_cast<void *>(widget + 0xb), &compressed_child, 4);
    std::memcpy(reinterpret_cast<void *>(widget + 0xf), &compressed_edge, 4);
    return widget;
}
// Validate the entire pair before consuming any bump space or replacing either branch.
inline bool wrap_pair(uint64_t &capsule, uint64_t &dots, uint64_t &top, uint64_t end,
    uint64_t heap, uint64_t dart_null, double delta) {
    if (!std::isfinite(delta) || delta < -630 || delta > 370 || delta == 0
        // Dart's bump cursor is word-aligned (8 bytes), not necessarily 16-byte aligned.
        // Both 0x50-byte allocations preserve that alignment. Launcher 7722 has
        // been observed with top=...0e28; requiring 16 bytes rejects both branches.
        || top > end || end - top <= 0xa0 || (top & 7u) != 0
        || (top >> 32) != static_cast<uint32_t>(heap) || ((top + 0x9f) >> 32) != static_cast<uint32_t>(heap)
        || !wrapper_valid(capsule, heap, dart_null) || !wrapper_valid(dots, heap, dart_null)) return false;
    const auto new_capsule = padding(top, capsule, dart_null, delta);
    const auto new_dots = padding(top + 0x50, dots, dart_null, delta);
    capsule = new_capsule;
    dots = new_dots;
    top += 0xa0;
    return true;
}
#endif
}
