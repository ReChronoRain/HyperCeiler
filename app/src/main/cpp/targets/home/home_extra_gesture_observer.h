/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_extra_gesture_wire.h"
namespace home_extra_gestures {
struct MotionSample {
    int action=0,pointers=0,device=0,source=0,pointer=0;
    float x=0,y=0;
    int64_t time=0,down=0;
};
// Serialized by Observation when shared with the long-press timer. Results
// are observations, never permission to pilfer or emit an action. Production
// activation still requires acknowledged legacy/native backend handoff.
class Observer {
    std::optional<State> state_;
    int64_t producer_=0,revision_=0;
    float slop_=0;
    int double_timeout_=0,long_timeout_=0,device_=0,source_=0,pointer_=0;
    bool stream_=false;
    bool same_config(const Snapshot& snapshot)const {
        return state_&&producer_==snapshot.producer&&revision_==snapshot.revision
            &&java_float(slop_)==java_float(snapshot.slop)&&double_timeout_==snapshot.double_timeout
            &&long_timeout_==snapshot.long_timeout;
    }
public:
    void reset(){state_.reset();producer_=revision_=0;stream_=false;}
    Result sample(const MotionSample& event,const Snapshot* snapshot) {
        // Android SOURCE_TOUCHSCREEN is the stable public source bitfield.
        if(!snapshot||!snapshot->geometry||(event.source&0x1002)!=0x1002) {reset();return {};}
        if(!same_config(*snapshot)) {
            state_.emplace(snapshot->slop,snapshot->double_timeout,snapshot->long_timeout);
            producer_=snapshot->producer;revision_=snapshot->revision;slop_=snapshot->slop;
            double_timeout_=snapshot->double_timeout;long_timeout_=snapshot->long_timeout;stream_=false;
        }
        if(event.action==0) {
            if(!stream_||device_!=event.device||source_!=event.source)state_->reset();
            device_=event.device;source_=event.source;pointer_=event.pointer;stream_=true;
        }
        if(!stream_||device_!=event.device||source_!=event.source||pointer_!=event.pointer) {
            state_->reset();stream_=false;return {};
        }
        return state_->sample(event.action,event.pointers,event.x,event.y,event.time,event.down,
            snapshot->geometry,snapshot->revision,snapshot->mask);
    }
    std::optional<int64_t> deadline()const {
        return state_&&state_->pending_long()?std::optional<int64_t>{state_->long_deadline()}:std::nullopt;
    }
    Result expire(int64_t time,const Snapshot* snapshot) {
        if(!snapshot||!snapshot->geometry||!same_config(*snapshot)){reset();return {};}
        return state_->long_press(time,snapshot->geometry,snapshot->revision,snapshot->mask);
    }
};
}
