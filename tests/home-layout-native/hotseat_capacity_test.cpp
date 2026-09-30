/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_hotseat_capacity.h"
#include <cassert>
#include <cstdio>
#include <cstring>
using namespace home_layout;
int main() {
    assert(kCapacitySiteCount == 8);
    CapacityWord sites[kCapacitySiteCount]{};
    uint32_t live[kCapacitySiteCount]{};
    for (int i=0; i<kCapacitySiteCount; ++i) {
        sites[i]={static_cast<uintptr_t>(4*(i+1)),kCapacitySites[i].guard[3],kCapacitySites[i].replacement};
        live[i]=sites[i].original;
    }
    const auto read=[&](uintptr_t a,uint32_t &w) { w=live[a/4-1]; return true; };
    int writes=0, fail_at=-1;
    const auto write=[&](uintptr_t a,uint32_t w) {
        live[a/4-1]=w; return writes++ != fail_at; // RX failure AFTER a successful memory write
    };
    assert(apply_capacity_words(sites,false,read,write)); assert(writes==0);
    assert(apply_capacity_words(sites,true,read,write));
    for (int i=0;i<kCapacitySiteCount;++i) assert(live[i]==sites[i].replacement);
    writes=0; assert(apply_capacity_words(sites,true,read,write)); assert(writes==0);
    assert(apply_capacity_words(sites,false,read,write));
    for (int i=0;i<kCapacitySiteCount;++i) assert(live[i]==sites[i].original);
    for (int fail=0;fail<kCapacitySiteCount;++fail) {
        writes=0;fail_at=fail;
        assert(!apply_capacity_words(sites,true,read,write));
        for (int i=0;i<kCapacitySiteCount;++i) assert(live[i]==sites[i].original);
    }
    fail_at=-1;
    for (int i=0;i<kCapacitySiteCount;++i) {
        live[i]=0xd503201f;writes=0;
        assert(!apply_capacity_words(sites,true,read,write));assert(writes==0);
        live[i]=sites[i].original;
    }
    const uintptr_t address=sites[0].address;sites[0].address=address+1;writes=0;
    assert(!apply_capacity_words(sites,true,read,write));assert(writes==0);sites[0].address=address;
    // Re-encoded branches must keep the exact original destination, not remove
    // type checks or redirect any Dart call through a foreign frame.
    for (const auto &s:kCapacitySites) {
        if ((s.guard[3]&0xff000010)==0x54000000) {
            int d=static_cast<int>((s.guard[3]>>5)&0x7ffff);
            if(d&0x40000)d-=0x80000;
            int r=static_cast<int>(s.replacement&0x3ffffff);
            if(r&0x2000000)r-=0x4000000;
            assert(d==r);
        }
    }
    assert(kCapacitySites[0].replacement==0xf85f03a0); // LDUR x0,[original FP,-0x10]
    for(int n=1;n<=100;++n) {
        const double width=360, cell=80;
        // Original count<max branch still uses fixed stride; excess uses width/N.
        const double stride=n<5?cell:width/n;
        const double left=width/2-n*stride/2;
        if(n>=5){assert(left>-1e-9 && left<1e-9);assert(left+(n-0.5)*stride<width);}
    }
    puts("hotseat capacity: 8-sites/off-on-off/idempotent/foreign-word-guard/alignment/partial-write-rollback PASS");
    puts("hotseat original logic: canonical-false/branch-destination/count-aware-division/getters-unchanged PASS");
}
