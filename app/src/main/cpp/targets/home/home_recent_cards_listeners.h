/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <array>
#include <cstring>
#include <mutex>

namespace home_layout::cards {
// Standard Android Shell AIDL protocol, not a vendor field/address signature.
// onRecentTasksChanged is the first, no-payload, one-way transaction. The class
// descriptor is checked before retaining a weak reference. No SDK user-data,
// Rust Arc, Dart object or callback pointer is borrowed beyond its lifetime.
class Listeners {
    static constexpr size_t kLimit=128;
    struct Batch {
        std::array<AIBinder*,kLimit> live{};
        std::array<AIBinder_Weak*,kLimit> dead{};
        size_t live_count=0,dead_count=0;
        Batch()=default;
        Batch(const Batch&)=delete;
        Batch& operator=(const Batch&)=delete;
        ~Batch(){
            for(size_t i=0;i<live_count;++i)AIBinder_decStrong(live[i]);
            for(size_t i=0;i<dead_count;++i)AIBinder_Weak_delete(dead[i]);
        }
    };
    std::mutex mutex_;
    std::array<AIBinder_Weak*,kLimit> weak_{};
    void collect_locked(Batch& batch) {
        for(auto& weak:weak_)if(weak) {
            auto* strong=AIBinder_Weak_promote(weak);
            if(strong)batch.live[batch.live_count++]=strong;
            else {batch.dead[batch.dead_count++]=weak;weak=nullptr;}
        }
    }
public:
    struct Report {size_t live=0,delivered=0,failed=0;};
    ~Listeners(){for(auto* weak:weak_)if(weak)AIBinder_Weak_delete(weak);}
    bool capture(AIBinder* binder) {
        if(!binder)return false;
        const auto* klass=AIBinder_getClass(binder);
        const auto* descriptor=klass?AIBinder_Class_getDescriptor(klass):nullptr;
        if(!descriptor||std::strcmp(descriptor,"com.android.wm.shell.recents.IRecentTasksListener"))return false;
        auto* candidate=AIBinder_Weak_new(binder);if(!candidate)return false;
        bool accepted=false;Batch batch;
        {
            std::lock_guard lock(mutex_);collect_locked(batch);
            for(auto* weak:weak_)if(weak&&!AIBinder_Weak_lt(weak,candidate)&&!AIBinder_Weak_lt(candidate,weak))accepted=true;
            if(!accepted)for(auto& weak:weak_)if(!weak){weak=candidate;candidate=nullptr;accepted=true;break;}
        }
        if(candidate)AIBinder_Weak_delete(candidate);
        return accepted;
    }
    Report notify() {
        Batch batch;
        {std::lock_guard lock(mutex_);collect_locked(batch);}
        Report report{batch.live_count,0,0};
        for(size_t i=0;i<batch.live_count;++i) {
            AParcel* input=nullptr;AParcel* output=nullptr;
            auto status=AIBinder_prepareTransaction(batch.live[i],&input);
            if(status==STATUS_OK&&input) {
                status=AIBinder_transact(batch.live[i],FIRST_CALL_TRANSACTION,&input,&output,FLAG_ONEWAY);
                input=nullptr;
            }else if(status==STATUS_OK)status=STATUS_BAD_VALUE;
            if(input)AParcel_delete(input);if(output)AParcel_delete(output);
            if(status==STATUS_OK)++report.delivered;else ++report.failed;
        }
        return report;
    }
};
}
