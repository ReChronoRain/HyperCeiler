/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_wire.h"
#include <bit>
#include <cassert>
#include <cstring>
#include <iostream>
#include <span>
#include <vector>
using namespace home_extra_gestures;
static std::optional<SnapshotReply> decode(std::span<const char> bytes,int64_t now=1001) {
    size_t cursor=0;
    return read_snapshot([&](auto& value){
        if(bytes.size()-cursor<sizeof(value))return false;
        std::memcpy(&value,bytes.data()+cursor,sizeof(value));cursor+=sizeof(value);return true;
    },[&]{return cursor==bytes.size();},now);
}
int main() {
    static_assert(std::endian::native==std::endian::little);
    uint32_t count=0;std::cin.read(reinterpret_cast<char*>(&count),4);assert(count==131);
    unsigned truncated=0;
    for(unsigned i=0;i<count;++i) {
        uint32_t size=0;std::cin.read(reinterpret_cast<char*>(&size),4);assert(size<=108);
        std::vector<char> bytes(size);std::cin.read(bytes.data(),size);assert(std::cin);
        const auto result=decode(bytes);assert(result);
        if(i<2)assert(!result->value&&int(result->status)==(i==0?0:2));
        else {
            assert(result->status==SnapshotStatus::ready&&result->value);
            const auto& value=*result->value;
            assert(value.published==1000&&value.slop==12&&value.double_timeout==300&&value.long_timeout==320);
            if(i==2)assert(!value.geometry&&value.mask==0);
            else {
                const auto rotation=int(i-3)/32,mask=int(i-3)%32;
                assert(value.geometry&&value.mask==mask&&value.revision==2+mask&&value.producer==90+rotation);
                const auto& geometry=*value.geometry;
                assert(geometry.owner==uint64_t(7+rotation)&&geometry.rotation==rotation&&geometry.width==1080
                    &&geometry.height==2400&&geometry.density==3&&geometry.nav_top==2320&&geometry.corner==72);
                assert(bool(geometry.handle)==bool(mask%2));
                if(geometry.handle)assert(geometry.handle->left==350&&geometry.handle->top==2360
                    &&geometry.handle->right==730&&geometry.handle->bottom==2400);
            }
            assert(!decode(bytes,999)&&!decode(bytes,2501));
        }
        for(size_t cut=0;cut<bytes.size();++cut){assert(!decode(std::span(bytes).first(cut)));++truncated;}
        auto extra=bytes;extra.resize(extra.size()+4);assert(!decode(extra));
        auto corrupt=bytes;corrupt[0]^=1;assert(!decode(corrupt));
        corrupt=bytes;corrupt[4]=2;assert(!decode(corrupt));
        corrupt=bytes;corrupt[8]=3;assert(!decode(corrupt));
    }
    std::cout<<"PASS Java writer/native snapshot reader, 131 frames, rotations/masks/owners/timing and "<<truncated
        <<" truncations, stale/future/trailing/schema damage refused\n";
}
