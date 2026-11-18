/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_extra_gesture_wire.h"
#include <atomic>
#include <memory>
namespace home_extra_gestures {
inline constexpr std::string_view owner_descriptor="com.sevtinge.hyperceiler.home.IExtraGestureOwner";
inline constexpr int32_t owner_revoke=1,action_query=3,action_ack=0x48474541;
struct LeaseSignal {std::atomic_bool revoked{false};std::atomic<uint64_t> notices{0};};
struct Lease {
    Grant grant;
    Snapshot snapshot;
    std::shared_ptr<LeaseSignal> signal;
    std::shared_ptr<const std::atomic_bool> peer_dead;
    bool valid(int64_t now)const {
        // Our lease margin, not an Android/vendor ABI constant. Stop before the
        // old backend may resume. Revocation is terminal for this owner token.
        return !signal->revoked.load(std::memory_order_acquire)&&!peer_dead->load(std::memory_order_acquire)
            &&now>=snapshot.published&&now-snapshot.published<=1500&&grant.until>now&&grant.until-now>100;
    }
};
inline bool allows(const Grant& grant,int mask,int64_t producer,int64_t revision,int64_t serial,int event,int64_t time,int64_t now) {
    return grant.producer==producer&&grant.revision==revision&&grant.serial==serial&&now<grant.until
        &&event>=9&&event<=12&&(mask&(1<<(event-8)))!=0&&time>=0&&time<=now&&now-time<=750;
}
}
