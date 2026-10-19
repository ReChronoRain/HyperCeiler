/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include "home_dynamic_layout.h"

namespace home_layout {
// Original control flow, never a hotSeatMaxCount/getter override. Each aligned
// instruction changes only the capacity decision. All original Dart calls,
// frames, type checks, database writes and insertion loops stay in the image.
//
// `offset` is not stored. The four-word guard is what identifies the site, and the instruction
// that gets replaced is the one the guard ends on, so the position falls out of the search: a
// launcher that inserts a check above one of these sites moves the guard, and the hook follows
// it instead of landing on whatever shifted into the old slot. Two of the guards are the same
// shape in different functions, which is why the symbol name stays part of the identity.
struct CapacitySite {
    const char *symbol;
    uint32_t guard[4]; // cmp and canonical bool operands, or count+branch setup
    uint32_t replacement;
};
inline constexpr CapacitySite kCapacitySites[] = {
    // Only this division uses actual count (FP-0x10); the max-count getter stays pristine.
    {"HotseatLayerGetxController.calculatePositionX", {0xaa0003e1, 0xfc5c83a0, 0xfc5d83a1, 0x94000053}, 0xf85f03a0},
    {"HotseatLayerGetxController._isSeatsFull", {0xeb01001f, 0x910082d0, 0x9100c2d1, 0x9a91a202}, 0xaa1103e2},
    {"HotseatLayerGetxController._isSeatsFull", {0xeb00003f, 0x910082d0, 0x9100c2d1, 0x9a91a202}, 0xaa1103e2},
    {"HotseatDragHandler._isSeatsFull", {0xeb00005f, 0x910082d0, 0x9100c2d1, 0x9a91a201}, 0xaa1103e1},
    {"HotseatLayerGetxController.setHotSeatItems", {0xf85d83a0, 0x93417c02, 0xeb01005f, 0x5400084d}, 0x14000042},
    {"HotseatLayerGetxController.resetAnimationAndUpdateDatabase", {0x93417c43, 0xf85e03a4, 0xeb04007f, 0x5400076d}, 0x1400003b},
    {"HotseatLayerGetxController._updateDragItemCount", {0xaa0003e1, 0xf85f03a0, 0xeb01001f, 0x5400066b}, 0x14000033},
    {"HotseatLayerGetxController._pushDragItem", {0xaa0003e1, 0xf85e83a0, 0xeb01001f, 0x540015e1}, 0x140000af},
};
/* The replaced instruction is the word the guard run ends on. */
inline constexpr size_t kCapacityGuardWords = 4;
inline constexpr int32_t kCapacityGuardLead = 12;
inline constexpr int kCapacitySiteCount = sizeof(kCapacitySites) / sizeof(kCapacitySites[0]);
// Resolve operands from the owned function, not a frozen BL/branch displacement.
// The final word alone changes; named max-count calls and scalar producers anchor it.
struct CapacityResolved {uint32_t offset=0, original=0, replacement=0;};
inline bool capacity_position_shape(const home_dynamic::Body& b) {
    unsigned n=0;
    for(size_t i=0;i+5<b.size();++i)
        if((b[i]&0xfc000000u)==0x94000000u && b[i+1]==0xd37ff801u
          && b[i+2]==0x93417c30u && b[i+3]==0x1e620200u
          && home_dynamic::ldd(b[i+4],29,1) && b[i+5]==0x1e601822u)++n;
    return n==1;
}
inline bool capacity_site(const home_dynamic::Body& b,uint32_t va,unsigned kind,
    uint32_t max_count,uint32_t fixed_count,CapacityResolved& out) {
    using namespace home_dynamic;
    if(kind>=kCapacitySiteCount || b.size()<8 || b[0]!=0xa9bf79fdu || b[1]!=0xaa0f03fdu)return false;
    CapacityResolved next;unsigned hits=0;
    if(kind==0){
        // x2 is the original raw item-count argument. Its owned frame slot,
        // not a guessed FP-0x10, becomes the spacing division numerator.
        int32_t count=0;unsigned stores=0;
        for(size_t j=2;j<8&&j<b.size();++j)if(stx(b[j],29,2)&&slot(imm9(b[j]))){count=imm9(b[j]);++stores;}
        if(stores!=1||!capacity_position_shape(b))return false;
        for(size_t i=2;i+5<b.size();++i){uint32_t to=0;
            if(!target(b[i],va+uint32_t(i*4),to)||to!=max_count
              ||!ldd(b[i-2],29,0)||!ldd(b[i-1],29,1)
              ||!slot(imm9(b[i-2]))||!slot(imm9(b[i-1]))
              ||b[i+1]!=0xd37ff801u||b[i+2]!=0x93417c30u||b[i+3]!=0x1e620200u
              ||!ldd(b[i+4],29,1)||imm9(b[i+4])!=imm9(b[i-2])||b[i+5]!=0x1e601822u)continue;
            ++hits;next={uint32_t(i*4),b[i],0xf84003a0u|((uint32_t(count)&511)<<12)};
        }
    }else{
        const auto& spec=kCapacitySites[kind];
        for(size_t i=1;i+4<=b.size();++i){
            bool same=true;
            for(size_t j=0;j<4;++j){const uint32_t mask=operand_mask(spec.guard[j]);
                if((b[i+j]&mask)!=(spec.guard[j]&mask)){same=false;break;}}
            if(!same)continue;
            bool anchored=false;size_t call=0;
            const size_t reach=kind==5?12:kind==3?7:4;
            for(size_t j=i>reach?i-reach:0;j<i;++j){uint32_t to=0;
                if(target(b[j],va+uint32_t(j*4),to)&&to==(kind==3?fixed_count:max_count)){
                    if(anchored)return false;anchored=true;call=j;}}
            if(!anchored)continue;
            // The capacity compare must consume the getter's return and the
            // same live count (including its original unbox where applicable).
            if(kind==1){if(call+3!=i||!ldx(b[i-1],29,0)||!slot(imm9(b[i-1])))continue;}
            if(kind==2){if(call+2!=i||!ldx(b[i-1],29,1)||!slot(imm9(b[i-1])))continue;}
            if(kind==6||kind==7){if(call+1!=i||!ldx(b[i+1],29,0)||!slot(imm9(b[i+1])))continue;}
            if(kind==4 && (call+2!=i||!ldx(b[i],29,0)||!slot(imm9(b[i]))))continue;
            if(kind==5 && (!ldx(b[i+1],29,4)||!slot(imm9(b[i+1]))))continue;
            next={uint32_t((i+3)*4),b[i+3],spec.replacement};
            if((next.original&0xff000010u)==0x54000000u){
                int32_t delta=static_cast<int32_t>(next.original<<8)>>13;
                const int64_t dest=int64_t(next.offset)+int64_t(delta)*4;
                if(delta<=0||dest>=int64_t(b.size()*4)||dest<=next.offset+4)return false;
                // Re-encode the original target; preserve all native work below it.
                next.replacement=0x14000000u|(uint32_t(delta)&0x03ffffffu);
            }
            ++hits;
        }
    }
    if(hits!=1)return false;out=next;return true;
}
struct CapacityWord { uintptr_t address = 0; uint32_t original = 0, replacement = 0; };

// Preflight the whole bank. A failed write/readback rolls every word back to
// its previously observed state, including a writer that changed memory before
// reporting an RX/cache-flush failure. No setting is published on partial success.
template <typename Read, typename Write>
bool apply_capacity_words(CapacityWord *sites, bool enabled, Read read, Write write) {
    uint32_t before[kCapacitySiteCount]{};
    for (int i = 0; i < kCapacitySiteCount; ++i) {
        if (!sites[i].address || (sites[i].address & 3) || !read(sites[i].address, before[i])
            || (before[i] != sites[i].original && before[i] != sites[i].replacement)) return false;
    }
    for (int i = 0; i < kCapacitySiteCount; ++i) {
        const uint32_t wanted = enabled ? sites[i].replacement : sites[i].original;
        uint32_t now = 0;
        if (before[i] == wanted) continue;
        if (!write(sites[i].address, wanted) || !read(sites[i].address, now) || now != wanted) {
            for (int j = 0; j <= i; ++j) (void) write(sites[j].address, before[j]);
            return false;
        }
    }
    return true;
}
} // namespace home_layout
