/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "recents_guard.generated.h"
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <span>

namespace home_recents_binding {
struct Binding { uintptr_t base=0, entry=0, patch=0, resume=0, owner=0, once=0; };
inline bool address(uintptr_t base, uintptr_t va, size_t length, uintptr_t &out) noexcept {
    if (base==0 || va>UINTPTR_MAX-base || length>UINTPTR_MAX-base-va) return false;
    out=base+va; return true;
}
// The only masked byte region is our one aligned instruction, and callers must
// supply its exact currently owned value. No foreign detour is normalized away.
template<class Read>
bool verify(uintptr_t base, Read &&read_bytes, uint32_t patch_word=home_recents_guard::kOriginalWord) noexcept {
    using namespace home_recents_guard;
    uintptr_t end=0;
    if ((base&0xfff)!=0 || !address(base,kImageSpan,1,end)) return false;
    std::array<std::byte,256> actual{};
    for (const auto &guard:kGuards) {
        // Validate the complete source span before forming any chunk offset.
        if (guard.va > kImageSpan || guard.bytes.size() > kImageSpan - guard.va) return false;
        for (size_t offset=0;offset<guard.bytes.size();) {
            const size_t count=guard.bytes.size()-offset<actual.size()?guard.bytes.size()-offset:actual.size();
            uintptr_t at=0;
            if (!address(base,guard.va+offset,count,at) || !read_bytes(at,std::span(actual).first(count))) return false;
            for (size_t i=0;i<count;++i) {
                const auto va=guard.va+offset+i;
                const auto expected=va>=kPatch && va<kPatch+4
                    ?static_cast<uint8_t>(patch_word>>((va-kPatch)*8)):guard.bytes[offset+i];
                if (actual[i]!=static_cast<std::byte>(expected)) return false;
            }
            offset+=count;
        }
    }
    for (const auto &pointer:kPointers) {
        uintptr_t slot=0,target=0,value=0;
        if (!address(base,pointer.slot,sizeof(value),slot) || !address(base,pointer.target,1,target)
            || !read_bytes(slot,std::as_writable_bytes(std::span(&value,1))) || value!=target) return false;
    }
    return true;
}
template<class Read>
bool resolve(uintptr_t base, Read &&read_bytes, Binding &out) noexcept {
    using namespace home_recents_guard;
    if (!verify(base,read_bytes)) return false;
    Binding result{base};
    if (!address(base,kEntry,1,result.entry) || !address(base,kPatch,4,result.patch)
        || !address(base,kResume,4,result.resume) || !address(base,kOwner,16,result.owner)
        || !address(base,kOnce,4,result.once)) return false;
    out=result; return true;
}
template<class Read>
bool cold(const Binding &binding, Read &&read_bytes) noexcept {
    // An observation, never a lock or a reservation. The original lifecycle and
    // the gateway's pending->denied CAS supply separate admission constraints.
    std::array<uintptr_t,2> pointers{};
    uint32_t once=0;
    return binding.base!=0 && read_bytes(binding.once,std::as_writable_bytes(std::span(&once,1)))
        && once==3 && read_bytes(binding.owner,std::as_writable_bytes(std::span(pointers)))
        && pointers[0]==0 && pointers[1]==0;
}
inline bool branch(uintptr_t from, uintptr_t to, uint32_t &word) noexcept {
    if ((from|to)&3) return false;
    if (to>=from) {
        const auto delta=to-from; if (delta>=0x08000000) return false;
        word=0x14000000u|static_cast<uint32_t>(delta>>2);
    } else {
        const auto delta=from-to; if (delta>0x08000000) return false;
        word=0x14000000u|(static_cast<uint32_t>(-(delta>>2))&0x03ffffffu);
    }
    return true;
}
} // namespace home_recents_binding
