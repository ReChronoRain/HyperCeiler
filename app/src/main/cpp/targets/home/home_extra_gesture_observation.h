/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_extra_gesture_observer.h"
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <pthread.h>
#include <time.h>
namespace home_extra_gestures {
struct ObservationCounters {
    std::atomic<uint64_t> observed{0},geometry{0},decisions{0},invalid{0},timers{0},long_presses{0},late_ms{0};
};
// Memory-only source. Optional actuation only performs checked pointer pilfer
// and queues typed actions; preference/Provider/action IPC stays off this path.
// The timed wait is independent of the Provider's blocking IPC
// worker. It sleeps indefinitely when idle and is joined before owner release.
class Observation {
    using Source=std::function<std::shared_ptr<const Snapshot>()>;
    Source source_;
    std::function<bool(Result)> actuate_;
    std::shared_ptr<ObservationCounters> counts_;
    Observer observer_;
    std::mutex mutex_;
    std::condition_variable changed_;
    std::optional<int64_t> due_;
    pthread_t thread_{};
    bool started_=false,closed_=false;
    Result apply(Result decision) {
        if(actuate_&&(decision.claim||decision.handled||decision.event)&&!actuate_(decision)) {
            observer_.reset();due_.reset();return {};
        }
        if(decision.claim||decision.handled||decision.event)counts_->decisions.fetch_add(1,std::memory_order_relaxed);
        return decision;
    }
    static void* run(void* value){static_cast<Observation*>(value)->work();return nullptr;}
    void work() {
#if defined(__ANDROID__)
        pthread_setname_np(pthread_self(),"hc-extra-timer");
#endif
        std::unique_lock guard(mutex_);
        while(!closed_) {
            if(!due_){changed_.wait(guard,[&]{return closed_||due_.has_value();});continue;}
            const auto deadline=*due_,time=uptime_ms();
            if(deadline>time) {
                changed_.wait_for(guard,std::chrono::milliseconds(deadline-time),[&]{return closed_||!due_||*due_!=deadline;});
                continue;
            }
            due_.reset(); // One delivery per arm, including a declined snapshot.
            auto snapshot=source_();
            const auto decision=apply(observer_.expire(time,snapshot.get()));
            counts_->timers.fetch_add(1,std::memory_order_relaxed);
            if(decision.event==11)counts_->long_presses.fetch_add(1,std::memory_order_relaxed);
            counts_->late_ms.store(std::max(counts_->late_ms.load(std::memory_order_relaxed),uint64_t(time-deadline)),std::memory_order_relaxed);
        }
    }
public:
    static int64_t uptime_ms(){timespec time{};clock_gettime(CLOCK_MONOTONIC,&time);return int64_t(time.tv_sec)*1000+time.tv_nsec/1000000;}
    Observation(Source source,std::shared_ptr<ObservationCounters> counts,std::function<bool(Result)> actuate={}):source_(std::move(source)),actuate_(std::move(actuate)),counts_(std::move(counts)){}
    Observation(const Observation&)=delete;
    Observation& operator=(const Observation&)=delete;
    bool start(){started_=pthread_create(&thread_,nullptr,run,this)==0;return started_;}
    ~Observation() {
        {std::lock_guard guard(mutex_);closed_=true;due_.reset();observer_.reset();}
        changed_.notify_one();if(started_)pthread_join(thread_,nullptr);
    }
    Result sample(const MotionSample& event) {
        std::lock_guard guard(mutex_);if(closed_)return {};
        auto snapshot=source_();
        const auto decision=apply(observer_.sample(event,snapshot.get()));
        counts_->observed.fetch_add(1,std::memory_order_relaxed);
        if(snapshot&&snapshot->geometry)counts_->geometry.fetch_add(1,std::memory_order_relaxed);
        const auto next=observer_.deadline();
        if(next!=due_){due_=next;changed_.notify_one();}
        return decision;
    }
    void reject() {
        std::lock_guard guard(mutex_);observer_.reset();due_.reset();changed_.notify_one();
        counts_->invalid.fetch_add(1,std::memory_order_relaxed);
    }
};
}
