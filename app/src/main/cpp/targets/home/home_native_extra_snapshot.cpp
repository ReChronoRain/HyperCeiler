/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_extra_snapshot.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_provider_endpoint.h"
#include "home_native_provider.h"
#include "home_native_input_receive.h"
#include "home_native_window.h"
#include <sys/system_properties.h>
#include "home_native_binder_watch.h"
#include <android/binder_parcel.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <time.h>
#include <mutex>
#include <deque>
namespace {
namespace wire=home_extra_gestures;
using Watch=home_native_glass::BinderWatch;
using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
using Parcel=std::unique_ptr<AParcel,decltype(&AParcel_delete)>;
struct Session {std::shared_ptr<wire::LeaseSignal> owner_signal;Binder endpoint{nullptr,AIBinder_decStrong},owner{nullptr,AIBinder_decStrong};std::optional<wire::Grant> grant;std::shared_ptr<Watch::Signal> signal;std::unique_ptr<Watch> watch;};
struct Published {wire::Snapshot snapshot;std::shared_ptr<Watch::Signal> signal;};
std::unique_ptr<Session> session;
std::shared_ptr<const Published> published;
std::shared_ptr<const wire::Lease> execution;
struct OwnerCookie {int64_t producer;std::shared_ptr<wire::LeaseSignal> signal;};
struct Action {std::shared_ptr<const wire::Lease> owner;int event;int64_t time;};
std::mutex action_mutex;
std::deque<Action> actions;
std::atomic<uint64_t> action_sent{0},action_rejected{0};
std::atomic<int64_t> due{0};
enum class Stop {none, terminal, retry, transition};
Stop stopping=Stop::none;
bool attempted=false,running=false,resident=false;
unsigned grants=0,renewals=0;
int64_t until=0;unsigned failures=0,samples=0,geometries=0,changes=0,declines=0;
int64_t previous_producer=0;uint64_t previous_owner=0;
std::string previous_reason;
int64_t now(){timespec time{};clock_gettime(CLOCK_MONOTONIC,&time);return int64_t(time.tv_sec)*1000+time.tv_nsec/1000000;}
const AIBinder_Class* owner_type() {
    static const auto* value=AIBinder_Class_define(wire::owner_descriptor.data(),
        [](void* data){return data;},[](void* data){delete static_cast<OwnerCookie*>(data);},
        [](AIBinder* binder,transaction_code_t code,const AParcel* data,AParcel*)->binder_status_t {
            if(code!=wire::owner_revoke)return STATUS_UNKNOWN_TRANSACTION;
            if(AIBinder_getCallingUid()!=1000)return STATUS_PERMISSION_DENIED;
            auto* cookie=static_cast<OwnerCookie*>(AIBinder_getUserData(binder));
            int32_t version=0;int64_t producer=0,serial=0;
            if(AParcel_readInt32(data,&version)!=STATUS_OK||version!=wire::control_version
                ||AParcel_readInt64(data,&producer)!=STATUS_OK||producer!=cookie->producer
                ||AParcel_readInt64(data,&serial)!=STATUS_OK||serial<=0
                ||AParcel_getDataPosition(data)!=AParcel_getDataSize(data))return STATUS_BAD_VALUE;
            // Do not require the grant to have arrived: a preference update can
            // revoke between server confirmation and the client reading its ACK.
            if(!cookie->signal->revoked.exchange(true,std::memory_order_acq_rel))
                cookie->signal->notices.fetch_add(1,std::memory_order_relaxed);
            hc_native_provider_notify();return STATUS_OK;
        });
    return value;
}
std::optional<wire::ControlReply> control(wire::ControlOp operation,int64_t producer,int64_t revision,int64_t serial) {
    AParcel* input=nullptr;AParcel* output=nullptr;
    const bool prepared=AIBinder_prepareTransaction(session->endpoint.get(),&input)==STATUS_OK&&input
        &&AParcel_writeInt32(input,wire::control_version)==STATUS_OK
        &&AParcel_writeInt32(input,int32_t(operation))==STATUS_OK
        &&AParcel_writeStrongBinder(input,session->owner.get())==STATUS_OK
        &&AParcel_writeInt64(input,producer)==STATUS_OK&&AParcel_writeInt64(input,revision)==STATUS_OK
        &&AParcel_writeInt64(input,serial)==STATUS_OK;
    binder_status_t status=STATUS_FAILED_TRANSACTION;
    if(prepared){status=AIBinder_transact(session->endpoint.get(),wire::control_query,&input,&output,0);input=nullptr;}
    Parcel request(input,AParcel_delete),reply(output,AParcel_delete);
    if(status!=STATUS_OK||!reply)return {};
    return wire::read_control([&](auto& value){
        using T=std::remove_cvref_t<decltype(value)>;
        if constexpr(std::is_same_v<T,int32_t>)return AParcel_readInt32(reply.get(),&value)==STATUS_OK;
        if constexpr(std::is_same_v<T,int64_t>)return AParcel_readInt64(reply.get(),&value)==STATUS_OK;
    },[&]{return AParcel_getDataPosition(reply.get())==AParcel_getDataSize(reply.get());},now());
}
bool clear(bool receiver=false) {
    std::atomic_store_explicit(&execution,std::shared_ptr<const wire::Lease>{},std::memory_order_release);
    std::atomic_store_explicit(&published,std::shared_ptr<const Published>{},std::memory_order_release);
    if((receiver||(session&&session->owner))&&!hc_native_input_receive_retire()){
        due.store(now()+50,std::memory_order_release);return false;
    }
    if(session&&session->owner) {
        bool released=false;
        if(session->grant&&!session->signal->dead.load(std::memory_order_acquire)) {
            const auto& grant=*session->grant;
            auto reply=control(wire::ControlOp::release,grant.producer,grant.revision,grant.serial);
            released=reply&&reply->status==wire::ControlStatus::released;
        }
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
            "native extra handoff retired, receiverReleased=1 releaseAck=%d grants=%u renewals=%u revoked=%llu actionsSent=%llu actionsRejected=%llu",released,grants,renewals,
            (unsigned long long)session->owner_signal->notices.load(),(unsigned long long)action_sent.load(),(unsigned long long)action_rejected.load());
    }
    session.reset();return true;
}
// A temporary observer is not allowed to outlive the owner it reserved.
// On uncertain delivery the client retires first, then releases its lease.
bool reserve(const wire::Snapshot& snapshot) {
    if(session->owner&&!hc_native_input_receive_active())return false;
    if(!session->owner) {
        if(!hc_native_input_receive_active())return true;
        std::string reason;
        if(!home_native_platform::callbacks(reason))return false;
        const auto* type=owner_type();if(!type)return false;
        session->owner_signal=std::make_shared<wire::LeaseSignal>();
        auto* cookie=new OwnerCookie{snapshot.producer,session->owner_signal};
        session->owner.reset(AIBinder_new(type,cookie));if(!session->owner){delete cookie;return false;}
    }
    if(session->owner_signal->revoked.load(std::memory_order_acquire))return false;
    const bool renewal=session->grant.has_value();
    if(renewal&&(session->grant->producer!=snapshot.producer||session->grant->revision!=snapshot.revision))return false;
    auto reply=control(renewal?wire::ControlOp::renew:wire::ControlOp::take,
        snapshot.producer,snapshot.revision,renewal?session->grant->serial:0);
    if(!reply)return false;
    if(reply->status==wire::ControlStatus::denied)return !renewal&&!resident;
    if(reply->status!=wire::ControlStatus::owned||reply->grant.producer!=snapshot.producer
        ||reply->grant.revision!=snapshot.revision||(renewal&&reply->grant.serial!=session->grant->serial))return false;
    if(session->owner_signal->revoked.load(std::memory_order_acquire))return false;
    session->grant=reply->grant;
    auto peer=std::shared_ptr<const std::atomic_bool>(session->signal,&session->signal->dead);
    std::shared_ptr<const wire::Lease> live=std::make_shared<wire::Lease>(wire::Lease{reply->grant,snapshot,session->owner_signal,std::move(peer)});
    std::atomic_store_explicit(&execution,std::move(live),std::memory_order_release);
    if(renewal)++renewals;
    else {
        ++grants;
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
            "native extra handoff acquired, source=%lld revision=%lld lease=%lld oldQueueDrained=1 pilfer=0 actions=0",
            (long long)reply->grant.producer,(long long)reply->grant.revision,(long long)reply->grant.serial);
    }
    return true;
}

void later(int64_t delay){
    const auto next=now()+delay;
    due.store(resident?next:std::min(next,until),std::memory_order_release);
}
void temporary(const std::string& reason) {
    if(previous_reason!=reason) {
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native extra snapshot pending: %s",reason.c_str());previous_reason=reason;
    }
    constexpr int delays[]={500,2000,10000,30000};later(delays[std::min(failures++,3u)]);
}
const AIBinder_Class* type() {
    static const auto* value=AIBinder_Class_define(wire::descriptor.data(),[](void* data){return data;},[](void*){},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
    return value;
}
bool complete(Stop reason) {
    stopping=reason;
    if(!clear(resident||reason==Stop::transition))return false;
    stopping=Stop::none;
    if(running)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native extra snapshot completed, resident=%d samples=%u geometry=%u ownerChanges=%u declines=%u",
        resident,samples,geometries,changes,declines);
    running=false;due.store(0,std::memory_order_release);
    if(reason==Stop::retry) {
        attempted=false;temporary("resident receiver or lease unavailable");
    }
    return true;
}
void end_session() {complete(resident?Stop::retry:Stop::terminal);}
}
std::shared_ptr<const home_extra_gestures::Snapshot> hc_native_extra_snapshot_get() {
    auto value=std::atomic_load_explicit(&published,std::memory_order_acquire);
    if(!value||value->signal->dead.load(std::memory_order_acquire))return {};
    const auto time=now();if(time<value->snapshot.published||time-value->snapshot.published>1500)return {};
    const auto* snapshot=&value->snapshot;
    return std::shared_ptr<const wire::Snapshot>(std::move(value),snapshot);
}
bool hc_native_extra_resident_selected() {
    char property[PROP_VALUE_MAX]{};
    return __system_property_get("debug.hyperceiler.home_extra_resident",property)>0&&std::strcmp(property,"1")==0;
}
void hc_native_extra_snapshot_refresh(void* context,bool probe,bool receive,bool selected) {
    // Retire the current input path before switching modes, even if it has
    // not obtained a lease yet. A probe receiver cannot become a resident.
    if(resident!=selected||stopping==Stop::transition) {
        if(!complete(Stop::transition))return;
        hc_native_input_receive_refresh(false);
        resident=selected;attempted=false;failures=0;previous_reason.clear();
    }
    if(stopping!=Stop::none){complete(stopping);return;}
    if(!resident) {
        hc_native_input_receive_refresh(receive);
        if(!probe) {
            attempted=false;
            if(running)complete(Stop::terminal);
            return;
        }
    }
    if(!running) {
        if(attempted||due.load(std::memory_order_acquire)>now())return;
        attempted=running=true;until=now()+10000;samples=geometries=changes=declines=0;
        previous_producer=0;previous_owner=0;grants=renewals=0;action_sent=action_rejected=0;
        if(!resident){failures=0;previous_reason.clear();}
    }
    if((!resident&&now()>=until)||(session&&session->owner_signal
        &&session->owner_signal->revoked.load(std::memory_order_acquire))){end_session();return;}
    if(session&&session->signal->dead.load(std::memory_order_acquire)){
        if(resident||session->owner){end_session();return;}
        clear();failures=0;due.store(0,std::memory_order_release);
    } else if(due.load(std::memory_order_acquire)>now())return;
    if(!session) {
        std::string reason;bool unsupported=false;
        Binder endpoint(hc_native_extra_endpoint(context,reason,unsupported),AIBinder_decStrong);
        if(!endpoint) {
            if(unsupported){++declines;__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native extra snapshot declined: %s",reason.c_str());complete(Stop::terminal);return;}
            temporary(reason);return;
        }
        const auto* existing=AIBinder_getClass(endpoint.get());
        const auto* contract=type();
        const auto* name=existing?AIBinder_Class_getDescriptor(existing):nullptr;
        const bool compatible=existing?name&&std::strcmp(name,wire::descriptor.data())==0:
            contract&&AIBinder_associateClass(endpoint.get(),contract);
        if(!compatible) {
            ++declines;__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native extra snapshot declined: Binder descriptor");complete(Stop::terminal);return;
        }
        auto value=std::make_unique<Session>();value->endpoint=std::move(endpoint);value->signal=std::make_shared<Watch::Signal>();
        value->watch=Watch::bind(value->endpoint.get(),value->signal,hc_native_provider_notify);
        if(!value->watch){temporary("Snapshot Binder death registration unavailable");return;}
        session=std::move(value);if(!resident)failures=0;
    }
    AParcel* input=nullptr;AParcel* output=nullptr;
    const bool prepared=AIBinder_prepareTransaction(session->endpoint.get(),&input)==STATUS_OK&&input
        &&AParcel_writeInt32(input,wire::snapshot_version)==STATUS_OK;
    binder_status_t status=STATUS_FAILED_TRANSACTION;
    if(prepared){status=AIBinder_transact(session->endpoint.get(),wire::snapshot_query,&input,&output,0);input=nullptr;}
    Parcel request(input,AParcel_delete),reply(output,AParcel_delete);
    if(status!=STATUS_OK||!reply){if(session->owner){end_session();return;}clear();temporary("Snapshot Binder request failed");return;}
    const auto read=[&](auto& value) {
        using T=std::remove_cvref_t<decltype(value)>;
        if constexpr(std::is_same_v<T,int32_t>)return AParcel_readInt32(reply.get(),&value)==STATUS_OK;
        if constexpr(std::is_same_v<T,int64_t>)return AParcel_readInt64(reply.get(),&value)==STATUS_OK;
        if constexpr(std::is_same_v<T,float>)return AParcel_readFloat(reply.get(),&value)==STATUS_OK;
    };
    const auto decoded=wire::read_snapshot(read,[&]{return AParcel_getDataPosition(reply.get())==AParcel_getDataSize(reply.get());},now());
    if(!decoded){++declines;if(session->owner){end_session();return;}clear();temporary("Snapshot schema or freshness declined");return;}
    if(decoded->status==wire::SnapshotStatus::closed){if(session->owner){end_session();return;}clear();temporary("Snapshot publisher closed");return;}
    if(!decoded->value) {
        if(session->owner){end_session();return;}
        std::atomic_store_explicit(&published,std::shared_ptr<const Published>{},std::memory_order_release);
        if(resident)temporary("resident configuration unavailable");else later(500);
        return;
    }
    const auto& snapshot=*decoded->value;
    if(resident) {
        const auto& geometry=snapshot.geometry;
        if(!geometry||(!(snapshot.mask&6)&&(!(snapshot.mask&24)||!geometry->handle))) {
            if(session->owner)end_session();
            else temporary("resident touch gestures or navigation geometry unavailable");
            return;
        }
        if(!session->owner) {
            const auto input=hc_native_input_receive_resident();
            if(input!=HomeInputStart::ready) {
                complete(input==HomeInputStart::unsupported?Stop::terminal:Stop::retry);
                return;
            }
        }
    }
    ++samples;
    const auto owner=snapshot.geometry?snapshot.geometry->owner:0;
    if(owner)++geometries;
    if(samples>1&&(snapshot.producer!=previous_producer||owner!=previous_owner))++changes;
    if(samples==1)__android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native extra snapshot accepted, source=%lld revision=%lld mask=%d geometry=%d owner=%llu",
        (long long)snapshot.producer,(long long)snapshot.revision,snapshot.mask,bool(snapshot.geometry),(unsigned long long)owner);
    char property[PROP_VALUE_MAX]{};
    const bool handoff=resident||(__system_property_get("debug.hyperceiler.home_extra_handoff",property)>0&&std::strcmp(property,"1")==0);
    if((handoff||session->owner)&&(!handoff||!reserve(snapshot))) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native extra handoff stopped, owner request or generation unavailable");
        end_session();return;
    }
    previous_producer=snapshot.producer;previous_owner=owner;
    std::shared_ptr<const Published> value=std::make_shared<Published>(Published{snapshot,session->signal});
    std::atomic_store_explicit(&published,std::move(value),std::memory_order_release);
    if(!resident||renewals>=2){failures=0;previous_reason.clear();}
    later(500);
}
std::shared_ptr<const wire::Lease> hc_native_extra_owner_get() {
    auto owner=std::atomic_load_explicit(&execution,std::memory_order_acquire);
    return owner&&owner->valid(now())?owner:nullptr;
}
std::shared_ptr<const wire::Snapshot> hc_native_extra_execution_snapshot_get() {
    auto owner=hc_native_extra_owner_get();if(!owner)return {};
    const auto* snapshot=&owner->snapshot;return std::shared_ptr<const wire::Snapshot>(std::move(owner),snapshot);
}
bool hc_native_extra_action(std::shared_ptr<const wire::Lease> owner,int event,int64_t time) {
    if(!owner||!owner->valid(now())||!wire::allows(owner->grant,owner->snapshot.mask,
        owner->grant.producer,owner->grant.revision,owner->grant.serial,event,time,now()))return false;
    {std::lock_guard guard(action_mutex);if(actions.size()>=8){++action_rejected;return false;}actions.push_back({std::move(owner),event,time});}
    hc_native_provider_notify();return true;
}
void hc_native_extra_actions_refresh() {
    std::deque<Action> queued;{std::lock_guard guard(action_mutex);queued.swap(actions);}
    for(const auto& action:queued) {
        auto current=hc_native_extra_owner_get();
        if(!session||!session->owner||!current||current->signal!=action.owner->signal||!action.owner->valid(now())
            ||!wire::allows(current->grant,current->snapshot.mask,action.owner->grant.producer,action.owner->grant.revision,
                action.owner->grant.serial,action.event,action.time,now())){++action_rejected;continue;}
        AParcel* input=nullptr;AParcel* output=nullptr;const auto& grant=action.owner->grant;
        const bool prepared=AIBinder_prepareTransaction(session->endpoint.get(),&input)==STATUS_OK&&input
            &&AParcel_writeInt32(input,wire::control_version)==STATUS_OK&&AParcel_writeStrongBinder(input,session->owner.get())==STATUS_OK
            &&AParcel_writeInt64(input,grant.producer)==STATUS_OK&&AParcel_writeInt64(input,grant.revision)==STATUS_OK
            &&AParcel_writeInt64(input,grant.serial)==STATUS_OK&&AParcel_writeInt32(input,action.event)==STATUS_OK
            &&AParcel_writeInt64(input,action.time)==STATUS_OK;
        binder_status_t status=STATUS_FAILED_TRANSACTION;
        if(prepared){status=AIBinder_transact(session->endpoint.get(),wire::action_query,&input,&output,0);input=nullptr;}
        Parcel request(input,AParcel_delete),reply(output,AParcel_delete);int32_t ack=0,accepted=0;
        const bool ok=status==STATUS_OK&&reply&&AParcel_readInt32(reply.get(),&ack)==STATUS_OK&&ack==wire::action_ack
            &&AParcel_readInt32(reply.get(),&accepted)==STATUS_OK&&accepted==1
            &&AParcel_getDataPosition(reply.get())==AParcel_getDataSize(reply.get());
        if(ok)++action_sent;
        else {++action_rejected;current->signal->revoked.store(true,std::memory_order_release);hc_native_provider_notify();}
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native extra action event=%d accepted=%d",action.event,ok);
    }
}
bool hc_native_extra_snapshot_due(){const auto at=due.load(std::memory_order_acquire);return at&&at<=now();}
int hc_native_extra_snapshot_wait_ms(){const auto at=due.load(std::memory_order_acquire);return at?int(std::clamp<int64_t>(at-now(),0,INT32_MAX)):-1;}
#else
bool hc_native_extra_resident_selected(){return false;}
void hc_native_extra_snapshot_refresh(void*,bool,bool,bool){}
std::shared_ptr<const home_extra_gestures::Lease> hc_native_extra_owner_get(){return {};}
std::shared_ptr<const home_extra_gestures::Snapshot> hc_native_extra_execution_snapshot_get(){return {};}
bool hc_native_extra_action(std::shared_ptr<const home_extra_gestures::Lease>,int,int64_t){return false;}
void hc_native_extra_actions_refresh(){}
bool hc_native_extra_snapshot_due(){return false;}
int hc_native_extra_snapshot_wait_ms(){return -1;}
std::shared_ptr<const home_extra_gestures::Snapshot> hc_native_extra_snapshot_get(){return {};}
#endif
