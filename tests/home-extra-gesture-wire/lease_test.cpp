/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_lease.h"
#include <cassert>
#include <iostream>
using namespace home_extra_gestures;
int main() {
    int mask,event,expected;int64_t producer,revision,serial,time,now;size_t count=0;
    while(std::cin>>mask>>producer>>revision>>serial>>event>>time>>now>>expected) {
        assert(allows(Grant{90,7,3,5000},mask,producer,revision,serial,event,time,now)==bool(expected));++count;
    }
    assert(count==19200);
    auto signal=std::make_shared<LeaseSignal>();auto dead=std::make_shared<std::atomic_bool>(false);
    Snapshot snapshot{90,7,1000,2,12,300,400,{}};
    Lease lease{Grant{90,7,3,2000},snapshot,signal,dead};
    assert(lease.valid(1000)&&lease.valid(1899)&&!lease.valid(1900)&&!lease.valid(2000));
    assert(!lease.valid(999));dead->store(true);assert(!lease.valid(1000));dead->store(false);
    // This may occur before a late grant ACK arrives. Publishing that ACK must
    // never turn a revoked owner token back into a valid input permission.
    signal->revoked.store(true);Lease late_ack{Grant{90,7,3,2500},snapshot,signal,dead};assert(!late_ack.valid(1000));
    std::cout<<"PASS "<<count<<" Java/native typed action authorizations, live lease margin, peer death and revoke-before-ACK\n";
}
