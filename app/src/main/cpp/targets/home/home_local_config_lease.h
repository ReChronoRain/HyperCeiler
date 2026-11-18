/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <atomic>
#include <cstdint>

namespace home_local_config {
// Refreshed by the existing approximately 1 Hz configuration worker. No new
// heartbeat, poller or callback I/O. Expiry also covers a suspended/gated worker.
constexpr int64_t kLeaseMs=3000;
class Lease {
    std::atomic<uint64_t> generation_{0};
    std::atomic<int64_t> deadline_{0};
    bool published_=false; // Only the serialized configuration worker writes.
public:
    uint64_t generation()const{return generation_.load(std::memory_order_acquire);}
    void connect(){published_=false;deadline_.store(0,std::memory_order_release);generation_.fetch_add(1,std::memory_order_acq_rel);}
    void disconnect(){connect();}
    void observed(int64_t now) {
        deadline_.store(published_&&now>=0&&now<=INT64_MAX-kLeaseMs?now+kLeaseMs:0,std::memory_order_release);
    }
    void publication(bool accepted,int64_t now){published_=accepted;observed(now);}
    bool current(uint64_t expected,int64_t now)const {
        const auto before=generation();
        if(!expected||before!=expected||now<0||now>=deadline_.load(std::memory_order_acquire))return false;
        return generation()==before;
    }
};
}
