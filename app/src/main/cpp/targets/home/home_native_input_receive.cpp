/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_input_receive.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_input_runtime_contract.h"
#include "home_native_motion_view.h"
#include "home_native_input_pilfer_runtime.h"
#include <android/binder_status.h>
#include <mutex>
#include "home_extra_gesture_observation.h"
#include "home_native_extra_snapshot.h"
#include <android/input.h>
#include <sys/system_properties.h>
#include "home_native_context.h"
#include "home_native_provider.h"
#include "home_recent_cards_live_image.h"
#include <android/log.h>
#include <atomic>
#include <memory>
#include <time.h>
#include <unistd.h>
namespace {
namespace sdk=home_layout::cards::sdk;
struct Stats : home_extra_gestures::ObservationCounters {
    std::atomic<uint64_t> pilfer_calls{0},pilfer_ok{0},pilfer_errors{0},pilfer_ms{0},enqueued{0};
    std::atomic<uint64_t> key{0},motion{0},finished{0},dropped{0},errors{0},batch{0},automatic{0},owner_drops{0};
};
using Drop=void(*)(void*);
using GetId=int32_t(*)(void*);
using Finish=uint64_t(*)(void*,void*,int32_t,bool);
using Consume=uint64_t(*)(void*,void*,int64_t);
// These buffers are native repr(C) return storage, never SDK/Dart objects.
// The indirect result's supported C ABI size must match the parsed producer
// before the compiler-generated AAPCS64 X8 call may be made.
struct Pair {uint64_t status;uintptr_t pointer;};
struct Reply {std::array<uint64_t,5> words;};
static_assert(sizeof(Pair)==16&&sizeof(Reply)==40);
struct Actuator {
    std::mutex mutex;
    std::optional<home_native_input::CheckedPilfer> binding;
    std::shared_ptr<Stats> stats;
    void close(){std::lock_guard guard(mutex);binding.reset();}
    bool apply(home_extra_gestures::Result decision) {
        std::lock_guard guard(mutex);
        auto owner=hc_native_extra_owner_get();if(!owner||!binding)return false;
        if(decision.claim) {
            stats->pilfer_calls.fetch_add(1,std::memory_order_relaxed);
            const auto start=home_extra_gestures::Observation::uptime_ms();
            const auto result=binding->claim();
            const auto elapsed=uint64_t(home_extra_gestures::Observation::uptime_ms()-start);
            stats->pilfer_ms.store(std::max(stats->pilfer_ms.load(std::memory_order_relaxed),elapsed),std::memory_order_relaxed);
            // The verified SDK consumer treats null as success and owns every
            // non-null status through AStatus_delete. Never accept void return.
            if(!result.accepted||!owner->valid(home_extra_gestures::Observation::uptime_ms())) {
                if(stats->pilfer_errors.fetch_add(1,std::memory_order_relaxed)<3)
                    __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native extra pilfer declined, exception=%d status=%d",result.exception,result.status);
                owner->signal->revoked.store(true,std::memory_order_release);hc_native_provider_notify();return false;
            }
            stats->pilfer_ok.fetch_add(1,std::memory_order_relaxed);
        }
        if(!owner->valid(home_extra_gestures::Observation::uptime_ms()))return false;
        if(decision.event) {
            if(!hc_native_extra_action(owner,decision.event,home_extra_gestures::Observation::uptime_ms()))return false;
            stats->enqueued.fetch_add(1,std::memory_order_relaxed);
        }
        return true;
    }
};
struct Session {
    std::shared_ptr<Stats> stats;
    std::vector<std::byte> prefix,base;
    GetId key_id=nullptr,motion_id=nullptr;
    Drop key_drop=nullptr,motion_drop=nullptr;
    Finish finish=nullptr;
    Consume consume=nullptr;
    uint32_t shift=0;int32_t success=0;
    std::unique_ptr<home_extra_gestures::Observation> observation;
    std::shared_ptr<Actuator> actuator;
};
struct Active {
    void* monitor=nullptr;void* manager=nullptr;
    Drop monitor_drop=nullptr,manager_drop=nullptr;
    std::shared_ptr<Stats> stats;
    std::shared_ptr<Actuator> actuator;
};
std::optional<Active> active;
std::shared_ptr<Stats> retiring;
std::atomic<int64_t> deadline{0};
bool attempted=false;
int64_t now(){timespec t{};clock_gettime(CLOCK_BOOTTIME,&t);return int64_t(t.tv_sec)*1000+t.tv_nsec/1000000;}
void foreign_drop(void* box) {
    Session* session=nullptr;std::memcpy(&session,box,sizeof(session));
    auto stats=session->stats;
    delete session; // Join the timer before publishing final counters/owner release.
    stats->owner_drops.fetch_add(1,std::memory_order_release);
    hc_native_provider_notify();
}
void event(Session* session,void* runtime,void* receiver,void* value,bool motion) {
    bool handled=false;
    if(motion&&session->observation) {
        // The resolved SDK adapter proves this exact borrowed pointer is the
        // platform MotionEvent. NDK getters borrow it; only SDK drop owns it.
        const auto* input=static_cast<const AInputEvent*>(value);
        const auto count=AMotionEvent_getPointerCount(input);
        const auto time=AMotionEvent_getEventTime(input),down=AMotionEvent_getDownTime(input);
        if(count==1&&time>=0&&down>=0) {
            const home_extra_gestures::MotionSample sample{AMotionEvent_getAction(input)&AMOTION_EVENT_ACTION_MASK,
                1,AInputEvent_getDeviceId(input),AInputEvent_getSource(input),AMotionEvent_getPointerId(input,0),
                AMotionEvent_getRawX(input,0),AMotionEvent_getRawY(input,0),time/1000000,down/1000000};
            // NDK event times are nanoseconds in the monotonic time base;
            // Java MotionEvent/State and the snapshot use uptime milliseconds.
            auto decision=session->observation->sample(sample);handled=session->actuator&&decision.handled;
        } else session->observation->reject();
    }
    const auto id=(motion?session->motion_id:session->key_id)(value);
    const auto result=session->finish(receiver,runtime,id,handled);
    const auto flag=result&((uint64_t(1)<<session->shift)-1);
    const auto status=int32_t(result>>session->shift);
    if(flag==0&&status==session->success)session->stats->finished.fetch_add(1,std::memory_order_relaxed);
    else session->stats->errors.fetch_add(1,std::memory_order_relaxed);
    (motion?session->motion_drop:session->key_drop)(value);
    session->stats->dropped.fetch_add(1,std::memory_order_relaxed);
    (motion?session->stats->motion:session->stats->key).fetch_add(1,std::memory_order_relaxed);
}
void key(Session* session,void* runtime,void* receiver,void* value){event(session,runtime,receiver,value,false);}
void motion(Session* session,void* runtime,void* receiver,void* value){event(session,runtime,receiver,value,true);}
void focus(Session* session,void*,void*,bool) {
    // Focus has no module-owned event. SDK completion follows return.
    session->stats->automatic.fetch_add(1,std::memory_order_relaxed);
}
void drag(Session* session,void*,void*,bool,float,float) {
    // Its two native float arguments are proven at the actual dispatch site.
    // This input observer records neither coordinates nor application data.
    session->stats->automatic.fetch_add(1,std::memory_order_relaxed);
}
void batch(Session* session,void* runtime,void* receiver,int32_t) {
    session->stats->batch.fetch_add(1,std::memory_order_relaxed);
    // Android InputConsumer: negative frame time drains the complete batch.
    // SDK argument forwarding and consume-batches=1 are separately proven.
    // This observer has no frame resampling or gesture/action ownership.
    session->consume(receiver,runtime,-1);
}
void stop() {
    if(!active)return;
    deadline.store(0,std::memory_order_release);
    retiring=active->stats;
    if(active->actuator)active->actuator->close(); // No borrowed host use after monitor drop.
    active->manager_drop(active->manager);
    active->monitor_drop(active->monitor);
    active.reset();
}
void report_retired() {
    if(!retiring)return;
    const auto owners=retiring->owner_drops.load(std::memory_order_acquire);
    if(!owners)return;
    const auto k=retiring->key.load(),m=retiring->motion.load(),finished=retiring->finished.load(),dropped=retiring->dropped.load();
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native input receive released, key=%llu motion=%llu finished=%llu dropped=%llu errors=%llu batches=%llu automatic=%llu ownerDrops=%llu pilferCalls=%llu pilferOk=%llu pilferErrors=%llu maxPilferMs=%llu queued=%llu",
        (unsigned long long)k,(unsigned long long)m,(unsigned long long)finished,(unsigned long long)dropped,
        (unsigned long long)retiring->errors.load(),(unsigned long long)retiring->batch.load(),
        (unsigned long long)retiring->automatic.load(),(unsigned long long)owners,
        (unsigned long long)retiring->pilfer_calls.load(),(unsigned long long)retiring->pilfer_ok.load(),
        (unsigned long long)retiring->pilfer_errors.load(),(unsigned long long)retiring->pilfer_ms.load(),(unsigned long long)retiring->enqueued.load());
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native extra motion observed, samples=%llu geometry=%llu decisions=%llu invalid=%llu timers=%llu longPress=%llu maxLateMs=%llu",
        (unsigned long long)retiring->observed.load(),(unsigned long long)retiring->geometry.load(),
        (unsigned long long)retiring->decisions.load(),(unsigned long long)retiring->invalid.load(),
        (unsigned long long)retiring->timers.load(),(unsigned long long)retiring->long_presses.load(),
        (unsigned long long)retiring->late_ms.load());
    retiring.reset();deadline.store(0,std::memory_order_release);
}
HomeInputStart start(bool resident) {
    constexpr std::array<std::string_view,8> names={"libhyper_os_ui_public.so","libhyper_os_ui_private.dylib.so",
        "libhyper_os_aidl.dylib.so","libstd.dylib.so","libhyper_os_sys.dylib.so",
        "libhyper_os_bundle_private.dylib.so","libhyper_os_graphics_private.dylib.so","libbinder_rs.dylib.so"};
    std::string reason;auto images=sdk::capture_live(names,&reason);
    if(!images) {
        if(resident&&reason.ends_with(": not loaded"))return HomeInputStart::pending;
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: %s",reason.c_str());
        return HomeInputStart::unsupported;
    }
    std::vector<sdk::Image> parsed;
    for(const auto& image:*images) {
        auto current=sdk::Image::parse(image.file,image.base);
        if(!current){__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: SDK metadata");return HomeInputStart::unsupported;}
        parsed.push_back(std::move(*current));
    }
    const sdk::Graph graph(std::move(parsed));const auto contract=home_native_input::runtime_contract(graph);
    auto* runtime=hc_native_context_get();
    if(!runtime||!sdk::images_current(*images))return HomeInputStart::pending;
    if(!contract||contract->receiver.bytes!=sizeof(Reply)) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: runtime/channel/callback/completion ownership contract");return HomeInputStart::unsupported;
    }
    char property[PROP_VALUE_MAX]{};
    const bool observe=resident||(__system_property_get("debug.hyperceiler.home_extra_probe",property)>0&&std::strcmp(property,"1")==0);
    if(observe&&!home_native_input::motion_ndk_view(graph)) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: SDK to NDK motion pointer contract");return HomeInputStart::unsupported;
    }
    const bool execute=resident||(observe&&__system_property_get("debug.hyperceiler.home_extra_execute",property)>0&&std::strcmp(property,"1")==0);
    const auto pilfer=execute?home_native_input::pilfer_contract(graph):std::nullopt;
    if(execute&&!pilfer){__android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: checked pilfer ownership contract");return HomeInputStart::unsupported;}
    const auto address=[&](sdk::Address at){return (*images)[at.image].base+at.va;};
    const auto create_monitor=reinterpret_cast<Pair(*)(const char*,size_t,int32_t)>(address(contract->monitor.create));
    const auto drop_monitor=reinterpret_cast<Drop>(address(contract->monitor.drop));
    const auto get_channel=reinterpret_cast<Pair(*)(void*)>(address(contract->channel.get));
    const auto create=reinterpret_cast<Reply(*)(void* const*,void*,void*)>(address(contract->receiver.create));
    const auto drop_manager=reinterpret_cast<Drop>(address(contract->receiver.drop));
    constexpr std::string_view name="HyperCeiler-Native-Input-Receive";
    const auto monitor=create_monitor(name.data(),name.size(),0);
    if(monitor.status||!monitor.pointer) {
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: monitor result");return HomeInputStart::pending;
    }
    auto* owned_monitor=reinterpret_cast<void*>(monitor.pointer);
    const auto channel=get_channel(owned_monitor);
    if(channel.status||!channel.pointer) {
        drop_monitor(owned_monitor);
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: borrowed channel unavailable");return HomeInputStart::pending;
    }
    auto stats=std::make_shared<Stats>();auto* session=new Session;session->stats=stats;
    std::shared_ptr<Actuator> actuator;
    if(execute) {
        auto binding=home_native_input::CheckedPilfer::bind(graph,*images,*pilfer,owned_monitor);
        if(!binding) {
            delete session;drop_monitor(owned_monitor);
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: live pilfer host/table/target");return HomeInputStart::unsupported;
        }
        actuator=std::make_shared<Actuator>();actuator->binding=std::move(binding);
        actuator->stats=stats;session->actuator=actuator;
    }
    if(observe) {
        auto source=execute?hc_native_extra_execution_snapshot_get:hc_native_extra_snapshot_get;
        session->observation=actuator?std::make_unique<home_extra_gestures::Observation>(source,stats,
            [actuator](home_extra_gestures::Result decision){return actuator->apply(decision);}):
            std::make_unique<home_extra_gestures::Observation>(source,stats);
        if(!session->observation->start()) {
            delete session;drop_monitor(owned_monitor);
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: timer startup");return HomeInputStart::pending;
        }
    }
    session->key_id=reinterpret_cast<GetId>(address(contract->dispatch.key.get_id));
    session->motion_id=reinterpret_cast<GetId>(address(contract->dispatch.motion.get_id));
    session->key_drop=reinterpret_cast<Drop>(address(contract->dispatch.key.drop));
    session->motion_drop=reinterpret_cast<Drop>(address(contract->dispatch.motion.drop));
    session->finish=reinterpret_cast<Finish>(address(contract->finish.entry));
    session->consume=reinterpret_cast<Consume>(address(contract->dispatch.consume));
    session->shift=contract->finish.shift;session->success=contract->finish.success;
    const auto& carrier=contract->carrier;size_t prefix_bytes=carrier.base_offset+sizeof(void*);
    for(const auto& method:contract->methods)prefix_bytes=std::max(prefix_bytes,
        std::max(size_t(method.function_offset)+sizeof(void*),size_t(method.flags_offset)+1));
    session->prefix.resize(prefix_bytes);session->base.resize(carrier.drop_offset+sizeof(void*));
    const auto drop=foreign_drop;const auto base=session->base.data(),prefix=session->prefix.data();
    std::memcpy(session->base.data()+carrier.drop_offset,&drop,sizeof(drop));
    std::memcpy(session->prefix.data()+carrier.base_offset,&base,sizeof(base));
    for(const auto& method:contract->methods) {
        session->prefix[method.flags_offset]|=std::byte(1u<<method.flag_bit);
        const auto target=method.slot==contract->dispatch.key_slot?reinterpret_cast<uintptr_t>(key):
            method.slot==contract->dispatch.motion_slot?reinterpret_cast<uintptr_t>(motion):
            method.slot==contract->dispatch.batch_slot?reinterpret_cast<uintptr_t>(batch):
            method.slot==contract->dispatch.drag_slot?reinterpret_cast<uintptr_t>(drag):reinterpret_cast<uintptr_t>(focus);
        std::memcpy(session->prefix.data()+method.function_offset,&target,sizeof(target));
    }
    std::vector<std::byte> foreign(carrier.carrier_bytes);
    std::memcpy(foreign.data()+carrier.prefix_offset,&prefix,sizeof(prefix));
    std::memcpy(foreign.data()+carrier.argument_offset,&session,sizeof(session));
    // Every returned factory path moves this carrier. Only foreign_drop may
    // delete Session after this point, including constructor failure paths.
    auto reply=create(&runtime,reinterpret_cast<void*>(channel.pointer),foreign.data());
    const auto bytes=std::as_writable_bytes(std::span(reply.words));const auto& result=contract->receiver;
    const auto tag=std::to_integer<uint8_t>(bytes[result.tag]);uintptr_t manager=0;
    if(tag==result.accepted)std::memcpy(&manager,bytes.data()+result.pointer,sizeof(manager));
    if(tag!=result.accepted||!manager) {
        if(tag==result.rejected) {
            uintptr_t error_table=0;
            std::memcpy(&error_table,bytes.data()+result.error_at+result.error_drop.table_field,sizeof(error_table));
            if(error_table==address(result.error_table))
                reinterpret_cast<Drop>(address(result.error_drop.drop))(bytes.data()+result.error_at);
        }
        drop_monitor(owned_monitor);retiring=stats;
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native input receive declined: SDK receiver construction");return HomeInputStart::pending;
    }
    active=Active{owned_monitor,reinterpret_cast<void*>(manager),drop_monitor,drop_manager,std::move(stats),std::move(actuator)};
    const int timeout=resident?0:12000;
    deadline.store(timeout?now()+timeout:0,std::memory_order_release);
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native input receive started, uid=%u pid=%d timeoutMs=%d execute=%d keySlot=%u motionSlot=%u batchSlot=%u",
        getuid(),getpid(),timeout,execute,contract->dispatch.key_slot,contract->dispatch.motion_slot,contract->dispatch.batch_slot);
    return HomeInputStart::ready;
}
}
void hc_native_input_receive_refresh(bool requested) {
    if(!requested){attempted=false;stop();report_retired();return;}
    if(active&&deadline.load(std::memory_order_acquire)<=now())stop();
    report_retired();
    if(attempted||active||retiring)return;
    attempted=true;start(false);report_retired();
}
HomeInputStart hc_native_input_receive_resident() {
    if(hc_native_input_receive_active())return HomeInputStart::ready;
    if(!hc_native_input_receive_retire())return HomeInputStart::pending;
    return start(true);
}
bool hc_native_input_receive_active(){return active.has_value()&&!retiring&&active->stats->owner_drops.load(std::memory_order_acquire)==0;}
bool hc_native_input_receive_retire(){stop();report_retired();return !active&&!retiring;}
bool hc_native_input_receive_due() {
    const auto at=deadline.load(std::memory_order_acquire);return at&&at<=now();
}
#else
void hc_native_input_receive_refresh(bool){}
HomeInputStart hc_native_input_receive_resident(){return HomeInputStart::unsupported;}
bool hc_native_input_receive_due(){return false;}
bool hc_native_input_receive_active(){return false;}
bool hc_native_input_receive_retire(){return true;}
#endif
