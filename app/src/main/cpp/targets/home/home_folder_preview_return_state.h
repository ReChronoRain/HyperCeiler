/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstring>

namespace home_layout {
// AAPCS64 returns these two words in x0/x1. The assembly consumes the flag
// separately, so a missing capture never replaces an original scalar with zero.
struct FolderPreviewReturnValue { uint64_t bits, applied; };

// Bridge the original origin call and its two later length consumers with plain
// native scalars. frame identifies a native stack frame, never a Dart heap root.
// No Dart frame/stack-map lifetime, object reference, allocation or GC call is
// added. Each UI thread owns an instance; nested original calls remain separate.
class FolderPreviewReturnState {
    struct Entry {
        uintptr_t frame=0;
        uint64_t width=0, height=0;
        bool width_taken=false;
    };
    std::array<Entry,8> entries_{};
    std::size_t next_=0;

    static bool dimension(uint64_t bits) {
        double value=0;
        std::memcpy(&value,&bits,sizeof(value));
        return std::isfinite(value)&&value>0&&value<=65536;
    }
    void discard(uintptr_t frame) {
        for(auto& entry:entries_)if(entry.frame==frame)entry={};
    }
public:
    void reset() { entries_={}; next_=0; }

    void capture(uintptr_t frame,uint64_t width_bits,uint64_t height_bits) {
        // A failed retry invalidates that invocation rather than resurrecting
        // scalars captured before a preference update or a reused stack frame.
        discard(frame);
        if(!frame||(frame&7)||!dimension(width_bits)||!dimension(height_bits))return;
        for(std::size_t step=0;step<entries_.size();++step) {
            const std::size_t index=(next_+step)%entries_.size();
            if(entries_[index].frame)continue;
            entries_[index]={frame,width_bits,height_bits,false};
            next_=(index+1)%entries_.size();
            return;
        }
        // Evict only an unused pair. Once width was supplied, retain its height
        // until the matching consumer so overflow cannot mix two geometries.
        for(std::size_t step=0;step<entries_.size();++step) {
            const std::size_t index=(next_+step)%entries_.size();
            if(entries_[index].width_taken)continue;
            entries_[index]={frame,width_bits,height_bits,false};
            next_=(index+1)%entries_.size();
            return;
        }
        // All entries are incomplete consumed pairs: refuse this new invocation.
        // Memory stays bounded; a reused frame or reset can recover safely.
    }

    FolderPreviewReturnValue take(uintptr_t frame,bool height) {
        if(!frame||(frame&7))return {};
        for(auto& entry:entries_)if(entry.frame==frame) {
            if(height) {
                const auto value=entry.width_taken
                    ?FolderPreviewReturnValue{entry.height,1}:FolderPreviewReturnValue{};
                entry={};
                return value;
            }
            if(entry.width_taken) {
                // An unexpected repeated width is not another logical item.
                entry={};
                return {};
            }
            entry.width_taken=true;
            return {entry.width,1};
        }
        return {};
    }
};
} // namespace home_layout
