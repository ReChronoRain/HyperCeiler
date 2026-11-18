/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_observation.h"
#include <cassert>
#include <iostream>
#include <thread>
using namespace home_extra_gestures;
using namespace std::chrono_literals;
// Module-owned immutable publications, no SDK/Android substitutes or clock mocks.
struct Publications {
    std::shared_ptr<const Snapshot> value;
    Publications() {set(Snapshot{1,1,Observation::uptime_ms(),8,20,300,70,Geometry{1000,2000,0,1,1950,100,Region{400,1950,600,2000},1}});}
    void set(Snapshot next){std::atomic_store(&value,std::make_shared<const Snapshot>(std::move(next)));}
    std::shared_ptr<const Snapshot> get(){return std::atomic_load(&value);}
    MotionSample down(){const auto time=Observation::uptime_ms();return {0,1,7,0x1002,0,500,1980,time,time};}
};
static void await(const std::atomic<uint64_t>& counter) {
    const auto until=std::chrono::steady_clock::now()+2s;
    while(!counter.load(std::memory_order_acquire)&&std::chrono::steady_clock::now()<until)std::this_thread::sleep_for(1ms);
    assert(counter.load(std::memory_order_acquire));
}
int main() {
    {
        Publications publications;auto counts=std::make_shared<ObservationCounters>();
        Observation observation([&]{return publications.get();},counts);assert(observation.start());
        observation.sample(publications.down());await(counts->timers);assert(counts->long_presses==1);
        assert(counts->decisions==1);std::this_thread::sleep_for(100ms);
        assert(counts->timers==1&&counts->long_presses==1); // No repeated idle timeout.
    }
    for(int change=0;change<11;++change) {
        Publications publications;auto counts=std::make_shared<ObservationCounters>();
        Observation observation([&]{return publications.get();},counts);assert(observation.start());
        auto event=publications.down();observation.sample(event);auto snapshot=*publications.get();
        if(change<4) {
            if(change==0)event.action=1;
            if(change==1)event.action=3;
            if(change==2){event.action=2;event.x+=30;}
            if(change==3){observation.reject();}
            if(change!=3)observation.sample(event);
            std::this_thread::sleep_for(110ms);assert(counts->timers==0);
        } else {
            if(change==4)++snapshot.producer;
            if(change==5)++snapshot.revision;
            if(change==6)++snapshot.geometry->owner;
            if(change==7)snapshot.geometry->rotation=1;
            if(change==8)++snapshot.long_timeout;
            if(change==9)snapshot.mask=0;
            if(change==10)snapshot.geometry.reset();
            publications.set(snapshot);await(counts->timers);
        }
        assert(counts->long_presses==0&&counts->decisions==0);
    }
    {
        Publications publications;auto counts=std::make_shared<ObservationCounters>();
        {
            auto snapshot=*publications.get();snapshot.long_timeout=5000;publications.set(snapshot);
            Observation observation([&]{return publications.get();},counts);assert(observation.start());
            observation.sample(publications.down());
        } // Pending five-second timer must be cancelled and joined now.
        std::this_thread::sleep_for(100ms);assert(counts->timers==0);
    }
    std::cout<<"PASS real monotonic timer, one-shot delivery, gesture/config/owner invalidation and pending-wait teardown\n";
}
