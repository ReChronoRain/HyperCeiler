/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cmath>
#include <cstdint>

namespace home_layout::haptic {
enum class Event {Start,Update,End};
// Scalar-only UI-isolate state. Never retain a movable Dart receiver/metrics.
class Crossing {
    bool active_=false,known_=false;
    double extent_=0;
    int64_t index_=0;
    uint64_t revision_=0;
public:
    void reset(){*this={};}
    unsigned accept(Event event,double pixels,double extent,uint64_t revision) {
        if(event==Event::End){reset();return 0;}
        if(event==Event::Start){reset();active_=true;}
        if(!revision||!std::isfinite(pixels)){reset();return 0;}
        if(!std::isfinite(extent)||extent<8||extent>32768){known_=false;return 0;}
        if(std::abs(pixels/extent)>100000){reset();return 0;}
        const double position=pixels/extent;
        if(!active_)return 0;
        if(!known_||revision_!=revision||std::abs(extent_-extent)>0.001){
            known_=true;extent_=extent;revision_=revision;index_=int64_t(std::floor(position+0.5));return 0;
        }
        // A tenth-card dead band prevents a stationary boundary from buzzing
        // repeatedly due to fractional layout rounding or tiny finger jitter.
        if(position<index_+0.6&&position>index_-0.6)return 0;
        const auto next=int64_t(std::floor(position+0.5));
        const auto distance=next>index_?next-index_:index_-next;index_=next;
        // Programmatic jumps/large discontinuities are not a chain of swipes.
        return distance<=8?static_cast<unsigned>(distance):0;
    }
};
} // namespace home_layout::haptic
