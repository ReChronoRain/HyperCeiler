/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <android/binder_ibinder.h>
#include <atomic>
#include <memory>

namespace home_native_glass {
class BinderWatch {
public:
    struct Signal {std::atomic_bool dead{false};};
    using Wake=void(*)();
private:
    struct Cookie {std::shared_ptr<Signal> signal;Wake wake;};
    std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)> binder_{nullptr,AIBinder_decStrong};
    std::unique_ptr<AIBinder_DeathRecipient,decltype(&AIBinder_DeathRecipient_delete)> recipient_{
        nullptr,AIBinder_DeathRecipient_delete};
    BinderWatch()=default;
public:
    // Caller retains its signal, not a raw Runtime/Host pointer. Binder death
    // and explicit unlink routinely complete on different Binder threads.
    static std::unique_ptr<BinderWatch> bind(AIBinder* binder,std::shared_ptr<Signal> signal,Wake wake) {
        auto watch=std::unique_ptr<BinderWatch>(new BinderWatch);
        AIBinder_incStrong(binder);watch->binder_.reset(binder);
        watch->recipient_.reset(AIBinder_DeathRecipient_new([](void* value) {
            const auto& cookie=*static_cast<Cookie*>(value);
            cookie.signal->dead.store(true,std::memory_order_release);
            if(cookie.wake)cookie.wake();
        }));
        if(!watch->recipient_)return {};
        // API33 guarantees exactly one onUnlinked per link attempt, including
        // an error return, after pending death callbacks. delete/unlink need not
        // wait, so only this callback may release the heap cookie.
        AIBinder_DeathRecipient_setOnUnlinked(watch->recipient_.get(),[](void* value){delete static_cast<Cookie*>(value);});
        auto* cookie=new Cookie{std::move(signal),wake};
        if(AIBinder_linkToDeath(binder,watch->recipient_.get(),cookie)!=STATUS_OK)return {};
        return watch;
    }
};
}
