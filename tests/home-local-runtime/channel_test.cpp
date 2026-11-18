/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_local_config_protocol.h"
#include "targets/home/home_local_config_lease.h"
#include <cassert>
#include <cstdio>
using namespace home_local_config;
static std::vector<uint8_t> frame(uint64_t sequence,std::vector<uint8_t> payload) {
    std::vector<uint8_t> bytes(kHeaderSize+payload.size());
    put_word(bytes.data(),kMagic);put_word(bytes.data()+4,kVersion);
    put_word(bytes.data()+8,static_cast<uint32_t>(payload.size()));
    put_word(bytes.data()+12,static_cast<uint32_t>(sequence>>32));
    put_word(bytes.data()+16,static_cast<uint32_t>(sequence));
    std::copy(payload.begin(),payload.end(),bytes.begin()+kHeaderSize);return bytes;
}
int main() {
    Lease lease;lease.connect();const auto generation=lease.generation();
    assert(!lease.current(generation,100));
    lease.publication(true,100);assert(lease.current(generation,100));
    assert(lease.current(generation,100+kLeaseMs-1)&&!lease.current(generation,100+kLeaseMs));
    lease.observed(100+kLeaseMs);assert(lease.current(generation,100+kLeaseMs));
    lease.publication(false,200+kLeaseMs);lease.observed(300+kLeaseMs);
    assert(!lease.current(generation,300+kLeaseMs)); // Idle traffic cannot revive rejected publication.
    lease.disconnect();lease.connect();const auto replacement=lease.generation();
    lease.observed(500+kLeaseMs);assert(!lease.current(replacement,500+kLeaseMs));
    lease.publication(true,500+kLeaseMs);
    assert(lease.current(replacement,500+kLeaseMs)&&!lease.current(generation,500+kLeaseMs));
    lease.observed(-1);assert(!lease.current(replacement,500+kLeaseMs));
    lease.publication(true,INT64_MAX);assert(!lease.current(replacement,INT64_MAX));
    const std::vector<uint8_t> payload{1,3,5,7,9};
    auto bytes=frame(1,payload);
    for(size_t split=0;split<=bytes.size();++split) {
        Decoder decoder;unsigned calls=0;
        auto publish=[&](const uint8_t* p,size_t size,uint64_t seq,uint32_t version){
            assert(version==kVersion);
            assert(seq==1&&std::vector<uint8_t>(p,p+size)==payload);++calls;return true;};
        assert(decoder.feed(bytes.data(),split,publish));
        assert(calls==(split==bytes.size()?1:0));
        assert(decoder.feed(bytes.data()+split,bytes.size()-split,publish));assert(calls==1);
        assert(!decoder.feed(bytes.data(),bytes.size(),publish));assert(calls==1);
        decoder.reset();assert(decoder.feed(bytes.data(),bytes.size(),publish));assert(calls==2);
    }
    for(int part:{0,4,8,12,16}) {
        auto bad=bytes;put_word(bad.data()+part,part==8?kMaxPayload+1:0);
        Decoder decoder;bool called=false;
        const bool valid=decoder.feed(bad.data(),bad.size(),[&](auto,auto,auto,auto){called=true;return true;});
        // Clearing the high sequence word keeps sequence 1 valid.
        assert(part==12?valid:!valid);assert(called==(part==12));
    }
    Decoder decoder;unsigned calls=0;auto second=frame(2,payload);
    auto empty=frame(1,{});
    assert(!decoder.feed(empty.data(),empty.size(),[](auto,auto,auto,auto){assert(false);return true;}));
    decoder.reset();
    auto both=bytes;both.insert(both.end(),second.begin(),second.end());
    assert(decoder.feed(both.data(),both.size(),[&](auto,auto,uint64_t seq,auto){assert(seq==++calls);return true;}));
    assert(calls==2);
    auto large=frame(3,std::vector<uint8_t>(kMaxPayload,9));
    for(size_t start=0;start<large.size();start+=4096)
        assert(decoder.feed(large.data()+start,std::min<size_t>(4096,large.size()-start),[&](auto,size_t size,auto,auto){assert(size==kMaxPayload);++calls;return true;}));
    assert(calls==3);
    auto legacy=frame(4,payload);put_word(legacy.data()+4,2);
    assert(decoder.feed(legacy.data(),legacy.size(),[&](auto,size_t size,uint64_t seq,uint32_t version){
        assert(size==payload.size()&&seq==4&&version==2);return true;}));
    auto future=frame(5,payload);put_word(future.data()+4,kVersion+1);
    assert(!decoder.feed(future.data(),future.size(),[](auto,auto,auto,auto){assert(false);return true;}));
    decoder.reset();
    assert(!decoder.feed(bytes.data(),bytes.size(),[](auto,auto,auto,auto){return false;}));
    puts("Root channel framing PASS splits, duplicate/reset, bounds, coalescing, maximum packet");
}
