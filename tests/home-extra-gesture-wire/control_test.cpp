/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_wire.h"
#include <vector>
#include <iostream>
#include <cassert>
using namespace home_extra_gestures;
static std::optional<ControlReply> decode(const std::vector<unsigned char>& bytes,int64_t now=1000) {
    size_t at=0;
    return read_control([&](auto& value){
        if(at+sizeof(value)>bytes.size())return false;
        uint64_t bits=0;for(unsigned i=0;i<sizeof(value);++i)bits|=uint64_t(bytes[at++])<<(8*i);
        value=static_cast<std::remove_reference_t<decltype(value)>>(bits);return true;
    },[&]{return at==bytes.size();},now);
}
static uint32_t word(){uint32_t value=0;for(unsigned i=0;i<4;++i){int byte=std::cin.get();assert(byte>=0);value|=uint32_t(byte)<<(8*i);}return value;}
int main() {
    const auto count=word();assert(count==18);unsigned truncations=0;
    for(unsigned i=0;i<count;++i) {
        std::vector<unsigned char> bytes(word());for(auto& byte:bytes){int value=std::cin.get();assert(value>=0);byte=unsigned(value);}
        auto frame=decode(bytes);assert(frame&&bytes.size()==44);
        assert(frame->status==(i==0?ControlStatus::denied:i==1?ControlStatus::released:ControlStatus::owned));
        if(i>1) {
            assert(frame->grant.producer==90&&frame->grant.revision==2&&frame->grant.serial==i&&frame->grant.until==2000+i);
            assert(!decode(bytes,3000));assert(!decode(bytes,-1));
        }
        for(size_t end=0;end<bytes.size();++end){auto shorter=bytes;shorter.resize(end);assert(!decode(shorter));++truncations;}
        auto bad=bytes;bad[0]^=1;assert(!decode(bad));bad=bytes;bad[4]^=1;assert(!decode(bad));
        bad=bytes;bad[8]=3;assert(!decode(bad));bad=bytes;bad.push_back(0);assert(!decode(bad));
        bad=bytes;bad[12]=0;assert(!decode(bad)||i<2);
        if(i<2){bad=bytes;bad[12]=1;assert(!decode(bad));}
    }
    std::cout<<"PASS 18 Java/native handoff frames, "<<truncations<<" truncations and status/freshness/length failures\n";
}
