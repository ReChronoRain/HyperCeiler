/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_glass_runtime.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_glass_client.h"
#include "home_native_binder_watch.h"
#include "home_dock_config.h"
#include "home_recent_statusbar.h"
#include "home_native_glass_motion.h"
#include "home_native_unlock_transition.h"
#include "home_native_provider.h"
#include <android/log.h>
#include <android/surface_control.h>
#include <atomic>
#include <future>
#include <time.h>
#include <unistd.h>

namespace {
using home_native_glass::Client;
using home_native_glass::BinderWatch;
using Transaction=std::unique_ptr<ASurfaceTransaction,decltype(&ASurfaceTransaction_delete)>;
std::atomic<int64_t> next_work{0};
std::atomic_bool release_pending{false},detach_committed{true};
int64_t now(){timespec t{};return clock_gettime(CLOCK_BOOTTIME,&t)==0?int64_t(t.tv_sec)*1000+t.tv_nsec/1000000:0;}
struct Runtime {
    std::unique_ptr<Client> native,renderer;
    std::unique_ptr<Client::Window> window;
    std::unique_ptr<Client::Host> host;
    std::shared_ptr<BinderWatch::Signal> signal;
    std::unique_ptr<BinderWatch> watch;
    ASurfaceControl* layer=nullptr;
    home_dock::Config appearance;
    home_dock::Bounds bounds{};
    bool visible=false,dark=false,material_ready=false,native_blocked=false,material_blocked=false,parent_hidden=false;
    bool exclusive=false,native_shown=false,admission_blocked=false;
    unsigned native_failures=0,material_failures=0,visibility_seen=~0u;
    uint64_t generation_seen=0;
    int64_t warm_until=0,reveal_epoch=-1;
    std::string last_native_failure,last_material_failure;

    void fallback(ASurfaceTransaction* transaction) {
        const uint32_t color=appearance.mode==3?(dark?0x18505050u:0x18ffffffu):(dark?0x66505050u:0x66ffffffu);
        ASurfaceTransaction_setColor(transaction,layer,float(color>>16&255)/255.f,float(color>>8&255)/255.f,
            float(color&255)/255.f,float(color>>24)/255.f,ADATASPACE_SRGB);
        (void)native->set_effect(transaction,layer,bounds.radius,120);
    }
    void retire_material(bool restore) {
        watch.reset();signal.reset();material_ready=false;warm_until=0;reveal_epoch=-1;visibility_seen=~0u;
        if(layer&&restore) {
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(transaction){fallback(transaction.get());ASurfaceTransaction_apply(transaction.get());}
        }
        if(host&&host->valid()) {
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(transaction) {
                ASurfaceTransaction_setVisibility(transaction.get(),host->surface(),ASURFACE_TRANSACTION_VISIBILITY_HIDE);
                ASurfaceTransaction_reparent(transaction.get(),host->surface(),nullptr);
                ASurfaceTransaction_apply(transaction.get());
            }
        }
        host.reset();renderer.reset();
    }
    void stop() {
        hc_native_glass_motion_clear();home_recent_bar::observe_for_dock(false);
        retire_material(false);
        if(exclusive&&hc_native_dock_requested())release_pending.store(true,std::memory_order_release);
        if(layer) {
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(transaction) {
                if(exclusive&&native_shown) {
                    detach_committed.store(false,std::memory_order_release);
                    ASurfaceTransaction_setOnCommit(transaction.get(),nullptr,[](void*,ASurfaceTransactionStats*) {
                        detach_committed.store(true,std::memory_order_release);hc_native_provider_notify();
                    });
                }
                ASurfaceTransaction_setVisibility(transaction.get(),layer,ASURFACE_TRANSACTION_VISIBILITY_HIDE);
                ASurfaceTransaction_reparent(transaction.get(),layer,nullptr);
                ASurfaceTransaction_apply(transaction.get());
            }
            ASurfaceControl_release(layer);layer=nullptr;
        }
        window.reset();native.reset();parent_hidden=false;native_shown=false;next_work.store(0,std::memory_order_release);
        if(release_pending.load(std::memory_order_acquire)&&detach_committed.load(std::memory_order_acquire))hc_native_provider_notify();
    }
    void fail(std::string reason,bool unsupported,bool material) {
        if(material)retire_material(true);else stop();
        auto& failures=material?material_failures:native_failures;
        auto& blocked=material?material_blocked:native_blocked;
        auto& previous=material?last_material_failure:last_native_failure;
        blocked=unsupported;
        if(reason!=previous||failures<3) {
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
                "native Dock %s unavailable: %s, unsupported=%d, localFallbackRetained=%d",
                material?"material":"base",reason.c_str(),unsupported,material&&layer!=nullptr);
            previous=std::move(reason);
        }
        constexpr int64_t delays[]={500,2000,10000,30000};
        next_work.store(blocked?0:now()+delays[std::min(failures,3u)],std::memory_order_release);++failures;
    }
    bool create_base(void* context,const home_dock::Config& requested) {
        std::string reason;home_native_glass::Failure failure;
        native=Client::open_native(context,reason,&failure);
        if(!native){fail(std::move(reason),failure==home_native_glass::Failure::unsupported,false);return false;}
        const auto viewport=native->viewport(reason);
        if(!viewport){fail(std::move(reason),!native->compatible(),false);return false;}
        const auto geometry=home_dock::layout(viewport->width,viewport->height,viewport->density,
            requested.height,requested.margin,requested.bottom,requested.radius);
        if(!geometry){native.reset();next_work.store(0,std::memory_order_release);return false;}
        window=native->window(reason);
        if(!window){fail(std::move(reason),!native->compatible(),false);return false;}
        auto* parent=window->surface();if(!parent){fail("native parent unavailable",false,false);return false;}
        const std::string name="HC-GlassHold-"+std::to_string(getpid());
        layer=ASurfaceControl_create(parent,name.c_str());if(!layer){fail("native layer creation declined",false,false);return false;}
        appearance=requested;bounds=*geometry;parent_hidden=hc_native_home_visibility()==1;
        dark=requested.night==2||(requested.night==0&&(viewport->ui_mode&0x30u)==0x20u);
        Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
        if(!transaction){fail("base transaction unavailable",false,false);return false;}
        ASurfaceTransaction_setVisibility(transaction.get(),layer,visible&&!exclusive?ASURFACE_TRANSACTION_VISIBILITY_SHOW:ASURFACE_TRANSACTION_VISIBILITY_HIDE);
        ASurfaceTransaction_setZOrder(transaction.get(),layer,-1);
        ASurfaceTransaction_setPosition(transaction.get(),layer,float(bounds.x),float(bounds.y));
        const ARect crop{0,0,bounds.width,bounds.height};ASurfaceTransaction_setCrop(transaction.get(),layer,crop);
        fallback(transaction.get());
        if(!native->compatible()){fail("base corner/blur ABI declined",true,false);return false;}
        auto* pending=new std::promise<void>();auto committed=pending->get_future();
        ASurfaceTransaction_setOnCommit(transaction.get(),pending,[](void* value,ASurfaceTransactionStats*) {
            std::unique_ptr<std::promise<void>> signal(static_cast<std::promise<void>*>(value));signal->set_value();
        });
        ASurfaceTransaction_apply(transaction.get());
        if(committed.wait_for(std::chrono::seconds(3))!=std::future_status::ready){fail("base commit not acknowledged",false,false);return false;}
        native_failures=0;last_native_failure.clear();next_work.store(0,std::memory_order_release);
        home_recent_bar::observe_for_dock(true);
        if(!exclusive)hc_native_glass_motion_target(layer,bounds,viewport->width,viewport->height,viewport->density,requested.style,native->frame_effects());
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
            "native Dock base ready: mode=%d hidden=%d bounds=%d,%d %dx%d, material independent",
            appearance.mode,!visible,bounds.x,bounds.y,bounds.width,bounds.height);
        return true;
    }
    void create_material(void* context,void(*wake)()) {
        std::string reason;home_native_glass::Failure failure;
        renderer=Client::open(context,reason,&failure);
        if(!renderer){fail(std::move(reason),failure==home_native_glass::Failure::unsupported,true);return;}
        host=renderer->create(bounds,dark,reason);
        if(!host){fail(std::move(reason),!renderer->compatible(),true);return;}
        signal=std::make_shared<BinderWatch::Signal>();watch=BinderWatch::bind(host->lifetime(),signal,wake);
        if(!watch){fail("material death registration failed",false,true);return;}
        auto* glass=host->surface();if(!glass){fail("native material borrow unavailable",false,true);return;}
        Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
        if(!transaction){fail("material attach transaction unavailable",false,true);return;}
        ASurfaceTransaction_reparent(transaction.get(),glass,layer);
        ASurfaceTransaction_setVisibility(transaction.get(),glass,visible?ASURFACE_TRANSACTION_VISIBILITY_SHOW:ASURFACE_TRANSACTION_VISIBILITY_HIDE);
        ASurfaceTransaction_setZOrder(transaction.get(),glass,2);
        ASurfaceTransaction_setBufferAlpha(transaction.get(),glass,visible?1.f/255.f:1.f);
        ASurfaceTransaction_setPosition(transaction.get(),glass,0,0);
        const ARect crop{0,0,bounds.width,bounds.height};ASurfaceTransaction_setCrop(transaction.get(),glass,crop);
        ASurfaceTransaction_apply(transaction.get());
        warm_until=visible?now()+8000:0;
        next_work.store(warm_until?now()+500:0,std::memory_order_release);
        material_failures=0;last_material_failure.clear();
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock material attached: hidden=%d, existing base retained",!visible);
    }
    void warmup() {
        if(!visible||!host||material_ready||!warm_until)return;
        std::string reason;const auto ready=host->ready(reason);
        if(ready==std::optional<bool>{true}) {
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(!transaction){fail("material reveal transaction unavailable",false,true);return;}
            if(!native->set_effect(transaction.get(),layer,bounds.radius,0)){fail("material reveal ABI declined",true,false);return;}
            ASurfaceTransaction_setColor(transaction.get(),layer,0,0,0,0,ADATASPACE_SRGB);
            ASurfaceTransaction_setBufferAlpha(transaction.get(),host->surface(),1.f);
            ASurfaceTransaction_apply(transaction.get());material_ready=true;warm_until=0;
            next_work.store(0,std::memory_order_release);
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native visible glass probe: real backgroundReady=true, material revealed, compositor fallback cleared");return;
        }
        if(now()>=warm_until) {
            warm_until=0;next_work.store(0,std::memory_order_release);
            __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig",
                "native visible glass probe: real background capture not confirmed, compositor fallback retained, status=%s",
                ready?"not ready":reason.c_str());return;
        }
        next_work.store(now()+500,std::memory_order_release);
    }
    void refresh(void* context,const home_dock::Config* requested,void(*wake)(),bool show_probe,bool native_backend) {
        if(release_pending.load(std::memory_order_acquire)) {
            if(!detach_committed.load(std::memory_order_acquire))return;
            if(!hc_native_dock_admission(false)){next_work.store(now()+500,std::memory_order_release);return;}
            release_pending.store(false,std::memory_order_release);
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock detached, legacy admission restored");
        }
        const auto generation=hc_native_home_window_generation();
        if(generation!=generation_seen) {
            stop();generation_seen=generation;
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock retired window resources, generation=%llu",(unsigned long long)generation);
        }
        const bool wanted_visible=show_probe||native_backend;
        if(visible!=wanted_visible||exclusive!=native_backend) {
            stop();if(release_pending.load(std::memory_order_acquire))return;
            visible=wanted_visible;exclusive=native_backend;admission_blocked=false;
        }
        // Root-only visible probe requires the original feature off; the test
        // additionally verifies the legacy SF layers have retired first.
        if(!context||!requested||!requested->available||((exclusive||!visible)?!requested->enabled:requested->enabled)){stop();return;}
        if(exclusive&&(!requested->capsule_known||requested->capsule_enabled)) {
            // The capsule still runs in legacy traversal. Detach and wait for
            // the compositor commit before releasing that traversal's lease.
            stop();admission_blocked=false;return;
        }
        if(layer&&appearance!=*requested){stop();if(release_pending.load(std::memory_order_acquire))return;admission_blocked=false;}
        if(native_blocked||admission_blocked)return;
        const auto window_visibility=hc_native_home_visibility();
        if(layer&&window_visibility==1)parent_hidden=true;
        if(layer&&window_visibility==3&&parent_hidden) {
            // The launcher replaces its client VRI when returning from
            // an app even though the WMS WindowState remains the same.
            // Keep our base/material, but acquire the current SDK
            // parent instead of reviving a detached previous VRI.
            std::string reason;
            auto current=native->window(reason);
            if(!current){fail(std::move(reason),!native->compatible(),false);return;}
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(!transaction){fail("return parent transaction unavailable",false,false);return;}
            ASurfaceTransaction_reparent(transaction.get(),layer,current->surface());
            ASurfaceTransaction_setZOrder(transaction.get(),layer,-1);
            ASurfaceTransaction_apply(transaction.get());window=std::move(current);parent_hidden=false;
            if(host&&!material_ready){warm_until=now()+8000;next_work.store(0,std::memory_order_release);}
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock rebound current launcher parent");
        }
        if(signal&&signal->dead.load(std::memory_order_acquire)){fail("renderer process died",false,true);return;}
        const auto due=next_work.load(std::memory_order_acquire);
        // A compositor admission callback wakes us immediately. Its display
        // must not wait behind the half-second pending-query retry delay.
        if(due&&now()<due&&!(exclusive&&!native_shown&&hc_native_dock_owned()))return;
        if(!layer&&!create_base(context,*requested))return;
        if(exclusive&&material_blocked){admission_blocked=true;stop();return;}
        if(appearance.mode==3&&!host&&!material_blocked){create_material(context,wake);return;}
        if(exclusive&&!native_shown) {
            if(hc_native_dock_rejected()){admission_blocked=true;stop();return;}
            if(!hc_native_dock_admission(true)){next_work.store(now()+500,std::memory_order_release);return;}
            std::string reason;const auto viewport=native->viewport(reason);
            if(!viewport){fail(std::move(reason),!native->compatible(),false);return;}
            Transaction transaction(ASurfaceTransaction_create(),ASurfaceTransaction_delete);
            if(!transaction){fail("native admission visibility transaction unavailable",false,false);return;}
            ASurfaceTransaction_setVisibility(transaction.get(),layer,ASURFACE_TRANSACTION_VISIBILITY_SHOW);
            ASurfaceTransaction_apply(transaction.get());native_shown=true;
            hc_native_glass_motion_target(layer,bounds,viewport->width,viewport->height,viewport->density,appearance.style,native->frame_effects());
            if(host)warm_until=now()+8000;
            __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock admitted after committed legacy retirement");
        }
        if(appearance.mode!=3||material_blocked)return;
        warmup();
        const auto visibility=window_visibility|(hc_native_glass_editing()?4u:0u);
        if(material_ready&&visibility!=visibility_seen) {
            const bool paused=(visibility_seen&3u)==1u||(visibility_seen!=~0u&&(visibility_seen&4u)!=0);
            const bool pause=(visibility&3u)==1u||(visibility&4u)!=0;
            visibility_seen=visibility;
            // Unknown is not hidden. A server without the optional visibility
            // edge keeps the existing capture behavior instead of freezing it.
            if(pause!=paused) {
                const auto command=pause?home_native_glass::Command::pause:home_native_glass::Command::resume;
                if(!host->notify(command)||(paused&&!pause&&!host->notify(home_native_glass::Command::fresh))) {
                    fail("material visibility command unavailable",!renderer->compatible(),true);return;
                }
                __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native Dock capture %s, windowState=%u frames=%llu",
                    pause?"paused":"resumed with fresh capture",visibility,(unsigned long long)hc_native_glass_motion_frames());
            }
        }
        const auto epoch=hc_native_unlock_epoch();
        // One control-worker RPC per host/epoch. The auxiliary process keeps
        // its original 3D sampler; the launcher owns all container frames.
        if(material_ready&&epoch>reveal_epoch) {
            reveal_epoch=epoch;
            timespec mono{};clock_gettime(CLOCK_MONOTONIC,&mono);
            const int64_t elapsed=int64_t(mono.tv_sec)*1000+mono.tv_nsec/1000000-epoch;
            if(epoch>0&&elapsed>=0&&elapsed<831) {
                std::string reason;
                if(!host->unlock(epoch,appearance.style,reason)) {
                    fail(std::move(reason),!renderer->compatible(),true);return;
                }
            }
        }
    }
};
}
bool hc_native_glass_hold_due(){const auto due=next_work.load(std::memory_order_acquire);return due&&now()>=due;}
bool hc_native_glass_release_pending(){return release_pending.load(std::memory_order_acquire);}
void hc_native_glass_hold_refresh(void* context,const home_dock::Config* appearance,void(*wake)(),bool visible,bool native_backend) {
    static Runtime runtime;runtime.refresh(context,appearance,wake,visible,native_backend);
}
#else
bool hc_native_glass_hold_due(){return false;}
bool hc_native_glass_release_pending(){return false;}
void hc_native_glass_hold_refresh(void*,const home_dock::Config*,void(*)(),bool,bool){}
#endif
