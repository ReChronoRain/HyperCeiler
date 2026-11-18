/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_observer.h"
#include <cassert>
#include <iostream>
using namespace home_extra_gestures;
int main() {
    Snapshot snapshot{1,1,1000,30,20,300,400,Geometry{1000,2000,0,1,1950,100,Region{400,1950,600,2000},1}};
    const MotionSample down{0,1,7,0x1002,0,10,1980,1000,1000};
    const MotionSample move{2,1,7,0x1002,0,90,1900,1100,1000};
    const MotionSample up{1,1,7,0x1002,0,90,1900,1150,1000};
    Observer observer;assert(observer.sample(down,&snapshot)==Result{});
    assert((observer.sample(move,&snapshot)==Result{true,true,0}));
    assert((observer.sample(up,&snapshot)==Result{false,true,9}));
    auto right=down;right.x=990;observer.sample(right,&snapshot);
    auto right_move=move;right_move.x=910;assert(observer.sample(right_move,&snapshot).claim);
    auto right_up=up;right_up.x=910;assert(observer.sample(right_up,&snapshot).event==10);
    auto tap=down;tap.x=500;
    auto release=tap;release.action=1;release.time=1050;
    observer.reset();observer.sample(tap,&snapshot);assert(observer.sample(release,&snapshot).event==0);
    tap.time=tap.down=1100;assert(observer.sample(tap,&snapshot).claim);
    release.time=1150;release.down=1100;assert(observer.sample(release,&snapshot).event==12);
    // A restarted publisher may reuse both revision and window generation.
    // Its producer identity still invalidates a pending double tap.
    for(int boundary=0;boundary<8;++boundary) {
        observer.reset();auto first=down;first.x=500;auto first_up=first;first_up.action=1;first_up.time=1050;
        observer.sample(first,&snapshot);observer.sample(first_up,&snapshot);
        auto next=first;next.time=next.down=1100;auto updated=snapshot;
        if(boundary==0)++updated.producer;
        if(boundary==1)++updated.revision;
        if(boundary==2)++updated.geometry->owner;
        if(boundary==3)updated.geometry->rotation=1;
        if(boundary==4)++next.device;
        if(boundary==5)++updated.double_timeout;
        if(boundary==6){observer.sample(next,nullptr);}
        if(boundary==7){auto stylus=next;stylus.source=0x4002;observer.sample(stylus,&snapshot);}
        assert(!observer.sample(next,&updated).claim);
    }
    for(int boundary=0;boundary<5;++boundary) {
        observer.reset();observer.sample(down,&snapshot);auto changed=move;auto updated=snapshot;
        if(boundary==0)++changed.pointer;
        if(boundary==1)++changed.device;
        if(boundary==2)changed.pointers=2;
        if(boundary==3)++updated.revision;
        if(boundary==4)updated.mask=0;
        assert(!observer.sample(changed,&updated).claim);
        assert(observer.sample(up,&updated).event==0);
    }
    std::cout<<"PASS native observer actual-snapshot generation, timing, identity, corner/double decisions and cancellation boundaries\n";
}
