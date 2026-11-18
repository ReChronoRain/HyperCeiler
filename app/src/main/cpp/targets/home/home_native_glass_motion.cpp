/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_glass_motion.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_dock_motion.h"
#include "home_dock_reveal.h"
#include "home_native_unlock_transition.h"
#include "home_native_provider.h"
#include "home_recent_statusbar.h"
#include "home_local_config_channel.h"
#include <android/log.h>
#include <android/choreographer.h>
#include <android/looper.h>
#include <android/surface_control.h>
#include <atomic>
#include <memory>
#include <pthread.h>
#include <sys/eventfd.h>
#include <sys/system_properties.h>
#include <poll.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <time.h>

extern "C" {
extern std::atomic<uint64_t> dock_motion_value,dock_motion_entry_hits,dock_motion_publish_hits,dock_edit_packed;
extern std::atomic<uint64_t> dock_auto_aim_value,dock_auto_aim_entry_hits,dock_auto_aim_publish_hits;
}
namespace {
struct Target {
    ASurfaceControl* layer;home_dock::Bounds bounds;int32_t width,height;float density;int32_t style;
    home_native_glass::FrameEffects effects;
    Target(ASurfaceControl* layer,const home_dock::Bounds& bounds,int32_t width,int32_t height,float density,
        int32_t style,home_native_glass::FrameEffects effects)
        :layer(layer),bounds(bounds),width(width),height(height),density(density),style(style),effects(effects){ASurfaceControl_acquire(layer);}
    ~Target(){ASurfaceControl_release(layer);}
};
std::shared_ptr<Target> target;
std::atomic_bool ipc_pending{false};
std::atomic<uint64_t> frames{0};
std::atomic<int> source_event{-1},ipc_event{-1};
int64_t now_ns(){timespec t{};return clock_gettime(CLOCK_MONOTONIC,&t)==0?int64_t(t.tv_sec)*1000000000+t.tv_nsec:0;}
void forward() {
    if(ipc_pending.exchange(true,std::memory_order_acq_rel))return;
    if(eventfd_write(ipc_event,1))ipc_pending.store(false,std::memory_order_release);
}
void* run(void*) {
    pthread_setname_np(pthread_self(),"hc-dock-local");
    auto* looper=ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    auto* choreographer=looper?AChoreographer_getInstance():nullptr;
    if(choreographer&&ALooper_addFd(looper,source_event,1,ALOOPER_EVENT_INPUT,nullptr,nullptr)<0)choreographer=nullptr;
    struct Clock {bool pending=false,ready=false;int64_t presentation=0;AVsyncId vsync=0;} clock;
    const auto request_frame=[&] {
        if(clock.pending)return;
        clock.pending=true;
        AChoreographer_postVsyncCallback(choreographer,[](const AChoreographerFrameCallbackData* data,void* context) {
            auto& frame=*static_cast<Clock*>(context);
            const auto index=AChoreographerFrameCallbackData_getPreferredFrameTimelineIndex(data);
            frame.presentation=AChoreographerFrameCallbackData_getFrameTimelineExpectedPresentationTimeNanos(data,index);
            frame.vsync=AChoreographerFrameCallbackData_getFrameTimelineVsyncId(data,index);
            frame.pending=false;frame.ready=true;
        },&clock);
    };
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock frame clock=%s",
        choreographer?"platform vsync":"timer fallback, native vsync unavailable");
    std::shared_ptr<Target> last_target;
    home_dock::NativeMotion native;home_dock::RecentsMotion spring;
    std::optional<home_dock::MotionSample> last,auto_sample;
    home_dock::Reveal reveal;
    bool native_applied=false,last_overview=false,last_editing=false,pose_valid=false;
    int64_t sequence=0,unlock_epoch=-1,auto_sequence=0;
    uint64_t auto_published=0;
    float old_scale=-1,old_scale_y=-1,old_y=0,old_x=0,old_alpha=-1,old_radius=-1;
    ARect old_crop{};
    for(int timeout=-1;;) {
        bool input=false;
        if(choreographer) {
            const int result=ALooper_pollOnce(timeout,nullptr,nullptr,nullptr);
            if(result==ALOOPER_POLL_ERROR)return nullptr;
            input=result==1;
        } else {
            pollfd descriptor{source_event,POLLIN,0};int result;
            do{result=poll(&descriptor,1,timeout);}while(result<0&&errno==EINTR);
            if(result<0)return nullptr;
            input=(descriptor.revents&POLLIN)!=0;
        }
        if(input) {
            eventfd_t count=0;
            if(eventfd_read(source_event,&count)&&errno!=EAGAIN)return nullptr;
            forward();
        }
        const auto current=std::atomic_load_explicit(&target,std::memory_order_acquire);const auto now=now_ns();
        // Unsupported/unbound targets still forward legacy events, but never
        // request display callbacks or retain the previous frame target.
        if(!current){last_target.reset();clock.ready=false;timeout=-1;continue;}
        // Sample freshness uses real monotonic time. Animation phase uses the
        // platform's presentation target, not callback dispatch/start time.
        const auto millis=(choreographer&&clock.ready?clock.presentation:now)/1000000;
        if(current!=last_target) {
            native.reset();spring=home_dock::RecentsMotion{};last.reset();sequence=0;
            native_applied=false;last_overview=false;last_editing=false;pose_valid=false;old_scale=-1;last_target=current;
            reveal=home_dock::Reveal{};unlock_epoch=-1;auto_sequence=0;auto_sample.reset();auto_published=dock_auto_aim_publish_hits.load(std::memory_order_acquire);
            if(current)reveal.style(current->style);
        }
        timeout=-1;if(!current||now<=0)continue;
        const bool editing=hc_native_glass_editing();
        if(editing!=last_editing) {
            last_editing=editing;
            // Wake the capture controller only on an edit-state edge. The
            // renderer frame still performs no SDK query or control RPC.
            hc_native_provider_notify();
        }
        uint64_t before,published,packed;
        do {
            before=dock_motion_publish_hits.load(std::memory_order_acquire);
            packed=dock_motion_value.load(std::memory_order_acquire);
            published=dock_motion_publish_hits.load(std::memory_order_acquire);
        }while(before!=published);
        const auto entries=dock_motion_entry_hits.load(std::memory_order_acquire);
        const unsigned scene=home_recent_bar::dock_scene()&15;
        const bool overview=(scene&8)&&(scene&5);
        const auto incoming=home_dock::validate_motion(sequence+1,now,packed,int64_t(entries),int64_t(published),sequence,now);
        if(home_dock::published_progress(last,incoming)) {
            last=incoming;sequence=last->sequence;
            native.accept(last,overview);native_applied=last->scene!=3;
        } else if(last&&overview&&!last_overview)native.accept(last,true);
        if(overview!=last_overview) {
            spring.set_overview(overview,millis);
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock overview=%d frames=%llu",
                overview,(unsigned long long)frames.load(std::memory_order_relaxed));
        }
        last_overview=overview;
        const bool fresh=last&&now-last->timestamp<=500000000;
        if(native_applied&&!fresh&&!overview) {
            spring.resume(native.progress(),false,millis);native.reset();native_applied=false;
        }
        const auto early=hc_native_unlock_epoch();
        if(early>unlock_epoch&&home_dock::Reveal::accepts_pending(early,now/1000000)) {
            unlock_epoch=early;if(reveal.arm(early))reveal.start(millis);
        }
        const bool revealing=reveal.needs_frame(millis);
        // A keepalive/read does not renew a projection sample. Only an actual
        // producer publication can give it a new timestamp.
        uint64_t auto_before,auto_after,auto_packed;
        do {
            auto_before=dock_auto_aim_publish_hits.load(std::memory_order_acquire);
            auto_packed=dock_auto_aim_value.load(std::memory_order_acquire);
            auto_after=dock_auto_aim_publish_hits.load(std::memory_order_acquire);
        }while(auto_before!=auto_after);
        if(auto_after>auto_published) {
            auto_published=auto_after;
            auto_sample=home_dock::validate_motion(auto_sequence+1,now,auto_packed,
                int64_t(dock_auto_aim_entry_hits.load(std::memory_order_acquire)),int64_t(auto_after),auto_sequence,now);
            if(auto_sample)auto_sequence=auto_sample->sequence;
        }
        auto pose=reveal.container(millis);
        if(reveal.style()==7&&revealing) {
            const auto live=home_dock::auto_aim_scale(auto_sample,reveal.epoch(),now);
            if(live)pose=home_dock::Reveal::Container::projected(*live);
            else {
                const double dx=current->bounds.x+current->bounds.width*.5-current->width*.5;
                const double dy=current->bounds.y+current->bounds.height*.5-current->height*.32572;
                pose=reveal.auto_pose(millis,home_dock::Reveal::projected_start(std::hypot(dx,dy)));
            }
        }
        const auto progress=native_applied?native.progress():spring.progress(millis);
        const float follow=native_applied?native.scale():1.f-.05f*progress;
        const float scale=follow*pose.scale_x,scale_y=follow*pose.scale_y;
        const float x=float(current->bounds.x)+(current->width*.5f-current->bounds.x)*(1.f-scale)
            +reveal.slide(float(current->bounds.width),millis);
        // These pivot fractions are our existing visual policy, not decoded
        // vendor ABI offsets. Both follow and reveal share that resting pivot.
        const float y=float(current->bounds.y)+(current->height*.32572f-current->bounds.y)*(1.f-scale_y)
            +home_dock::NativeMotion::relative_y(current->density,progress)+reveal.rise(current->density,millis);
        const float alpha=editing?0.f:(reveal.pending()?reveal.alpha(millis):1.f);
        const float radius=pose.radius(current->bounds.radius,float(current->bounds.height));
        const int width=std::clamp(int(current->bounds.width*pose.crop_width),1,current->bounds.width);
        const int height=std::clamp(int(current->bounds.height*pose.crop_height),1,current->bounds.height);
        const int left=(current->bounds.width-width)/2,top=(current->bounds.height-height)/2;
        const ARect crop{left,top,left+width,top+height};
        const bool force=!pose_valid||(reveal.pending()&&!revealing);
        const bool move=force||x!=old_x||y!=old_y;
        const bool matrix=force||scale!=old_scale||scale_y!=old_scale_y;
        const bool round=force||radius!=old_radius;
        const bool clip=force||crop.left!=old_crop.left||crop.top!=old_crop.top||crop.right!=old_crop.right||crop.bottom!=old_crop.bottom;
        const bool fade=force||alpha!=old_alpha;
        if(move||matrix||round||clip||fade) {
            // Observe publications immediately; schedule a display callback
            // only after finding a changed pose. Neutral samples stay idle.
            if(choreographer&&!clock.ready){request_frame();timeout=-1;continue;}
            if(auto* transaction=ASurfaceTransaction_create()) {
                if(choreographer)ASurfaceTransaction_setFrameTimeline(transaction,clock.vsync);
                // Never reparent or show here. A retired target can remain
                // alive for this frame but teardown already detached it.
                if(move)ASurfaceTransaction_setPosition(transaction,current->layer,x,y);
                // NDK setScale forbids zero/negative values; AUTO_AIM's signed
                // footprint uses the runtime-proven four-float platform bridge.
                if(matrix)current->effects.matrix(transaction,current->layer,scale,0.f,0.f,scale_y);
                if(round)current->effects.corner(transaction,current->layer,radius,radius);
                if(clip)ASurfaceTransaction_setCrop(transaction,current->layer,crop);
                if(fade)ASurfaceTransaction_setBufferAlpha(transaction,current->layer,alpha);
                ASurfaceTransaction_apply(transaction);ASurfaceTransaction_delete(transaction);
                pose_valid=true;
                old_scale=scale;old_scale_y=scale_y;old_x=x;old_y=y;old_alpha=alpha;old_radius=radius;old_crop=crop;
                reveal.committed(millis);frames.fetch_add(1,std::memory_order_relaxed);
            }
        }
        clock.ready=false;
        if(revealing||(!native_applied&&spring.running(millis))) {
            if(choreographer){request_frame();timeout=-1;}else timeout=8;
        }
        else if(native_applied&&fresh&&!overview)timeout=int(std::max(int64_t(1),(last->timestamp+500000000-now+999999)/1000000));
    }
}
}
int hc_native_glass_motion_begin(int event) {
    if(event<0)return event;
    if(ipc_event>=0)return ipc_event;
    char mode[PROP_VALUE_MAX]={};__system_property_get("debug.hyperceiler.home_glass_backend",mode);
    if(!home_local_config::requested()||(std::strcmp(mode,"app-hold-probe")!=0&&std::strcmp(mode,"app-visible-probe")!=0&&std::strcmp(mode,"app-native")!=0))return event;
    ipc_event=eventfd(0,EFD_CLOEXEC|EFD_NONBLOCK);if(ipc_event<0)return event;
    source_event=event;pthread_t thread{};
    if(pthread_create(&thread,nullptr,run,nullptr)!=0){close(ipc_event);ipc_event=-1;return event;}
    pthread_detach(thread);forward();(void)eventfd_write(source_event,1);
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","local experimental Dock motion worker ready, legacy IPC isolated");
    return ipc_event;
}
void hc_native_glass_motion_consumed(){ipc_pending.store(false,std::memory_order_release);}
void hc_native_glass_motion_target(ASurfaceControl* layer,const home_dock::Bounds& bounds,int32_t width,int32_t height,float density,
    int32_t style,home_native_glass::FrameEffects effects) {
    if(!effects.corner||!effects.matrix)return;
    std::atomic_store_explicit(&target,std::make_shared<Target>(layer,bounds,width,height,density,style,effects),std::memory_order_release);
    if(source_event>=0)(void)eventfd_write(source_event,1);
}
void hc_native_glass_motion_notify(){if(source_event>=0)(void)eventfd_write(source_event,1);}
void hc_native_glass_motion_clear(){std::atomic_store_explicit(&target,std::shared_ptr<Target>{},std::memory_order_release);if(source_event>=0)(void)eventfd_write(source_event,1);}
uint64_t hc_native_glass_motion_frames(){return frames.load(std::memory_order_relaxed);}
bool hc_native_glass_editing(){return (dock_edit_packed.load(std::memory_order_acquire)&1u)!=0;}
#else
int hc_native_glass_motion_begin(int event){return event;}
void hc_native_glass_motion_consumed(){}
void hc_native_glass_motion_target(ASurfaceControl*,const home_dock::Bounds&,int32_t,int32_t,float,int32_t,home_native_glass::FrameEffects){}
void hc_native_glass_motion_notify(){}
void hc_native_glass_motion_clear(){}
uint64_t hc_native_glass_motion_frames(){return 0;}
bool hc_native_glass_editing(){return false;}
#endif
