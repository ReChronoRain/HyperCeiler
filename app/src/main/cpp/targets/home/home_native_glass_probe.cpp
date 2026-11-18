/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_glass_probe.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_glass_client.h"
#include "home_dock_config.h"
#include <android/log.h>
#include <android/surface_control.h>
#include <future>
#include <pthread.h>
#include <thread>
#include <unistd.h>

namespace {
void probe(void* context,const home_dock::Config& appearance,home_native_glass::HostKind kind) {
    const bool capsule=kind==home_native_glass::HostKind::capsule;
    const auto report=[capsule](const char* stage) {
        __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig","native app %s probe: %s",
            capsule?"capsule":"glass",stage);
    };
    std::string reason;
    auto client=home_native_glass::Client::open(context,reason,nullptr,
        capsule?home_native_glass::Destination::preferences:home_native_glass::Destination::glass);
    if(!client){report(reason.c_str());return;}
    const auto viewport=client->viewport(reason);if(!viewport){report(reason.c_str());return;}
    const bool dark=appearance.night==2||(appearance.night==0&&(viewport->ui_mode&0x30u)==0x20u);
    auto bounds=home_dock::layout(viewport->width,viewport->height,viewport->density,
        appearance.height,appearance.margin,appearance.bottom,appearance.radius);
    if(!bounds){report("configured Dock geometry unavailable");return;}
    if(capsule) {
        // These are the capsule's existing design dimensions in dp.
        const auto width=int32_t(82*viewport->density),height=int32_t(20*viewport->density);
        const auto gap=int32_t(22*viewport->density);
        const auto inset=int32_t(std::round(height*0.8f)); // XiaoheiziCapsuleView vertical padding
        bounds=home_dock::Bounds{(viewport->width-width)/2,bounds->y-gap-height-inset,width,height,0.f};
    }
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native app glass probe: Context metrics width=%d height=%d density=%.6f, SDK-owned snapshot",
        viewport->width,viewport->height,viewport->density);
    __android_log_print(ANDROID_LOG_INFO,"HyperCeiler.HomeConfig",
        "native app glass probe: appearance enabled=%d mode=%d, configured bounds=%d,%d %dx%d radius=%.3f, hidden material test only",
        appearance.enabled,appearance.mode,bounds->x,bounds->y,bounds->width,bounds->height,bounds->radius);
    auto host=client->create(*bounds,dark,reason,kind);
    if(!host||!host->valid()){report(reason.empty()?"host not valid":reason.c_str());return;}
    report(capsule?"surfaceReady=true, surfaceControlTransferred=true, rendererLifetimeValid=true":
        "frameCommitted=true, surfaceControlTransferred=true, rendererLifetimeValid=true, owned native client lease");
    auto* parent=host->parent();auto* glass=host->surface();
    if(!parent||!glass){report("borrowed native surface unavailable");return;}
    struct Layer {
        ASurfaceControl* object;ASurfaceControl* glass;
        ~Layer() {
            if(!object)return;
            if(auto* detach=ASurfaceTransaction_create()) {
                ASurfaceTransaction_setVisibility(detach,glass,ASURFACE_TRANSACTION_VISIBILITY_HIDE);
                ASurfaceTransaction_reparent(detach,glass,nullptr);
                ASurfaceTransaction_reparent(detach,object,nullptr);
                ASurfaceTransaction_apply(detach);ASurfaceTransaction_delete(detach);
            }
            ASurfaceControl_release(object);
        }
    } layer{ASurfaceControl_create(parent,((capsule?"HC-CapsuleProbe-":"HC-GlassProbe-")+std::to_string(getpid())).c_str()),glass};
    if(!layer.object){report("native child-layer creation declined");return;}
    std::unique_ptr<ASurfaceTransaction,decltype(&ASurfaceTransaction_delete)> transaction(
        ASurfaceTransaction_create(),ASurfaceTransaction_delete);
    if(!transaction){report("native transaction unavailable");return;}
    ASurfaceTransaction_setVisibility(transaction.get(),layer.object,ASURFACE_TRANSACTION_VISIBILITY_HIDE);
    ASurfaceTransaction_setPosition(transaction.get(),layer.object,static_cast<float>(bounds->x),static_cast<float>(bounds->y));
    ASurfaceTransaction_setVisibility(transaction.get(),glass,ASURFACE_TRANSACTION_VISIBILITY_HIDE);
    ASurfaceTransaction_reparent(transaction.get(),glass,layer.object);
    auto* pending=new std::promise<void>();auto committed=pending->get_future();
    ASurfaceTransaction_setOnCommit(transaction.get(),pending,[](void* value,ASurfaceTransactionStats*) {
        // The platform owns this context, including callbacks after timeout.
        std::unique_ptr<std::promise<void>> signal(static_cast<std::promise<void>*>(value));signal->set_value();
    });
    ASurfaceTransaction_apply(transaction.get());
    if(committed.wait_for(std::chrono::seconds(3))!=std::future_status::ready) {
        report("mainLayerValid=true, nativeChildCreated=true, hidden transaction not confirmed before timeout");return;
    }
    report("mainLayerValid=true, nativeChildCreated=true, hiddenTransactionCommitted=true");
    if(capsule) {
        if(!host->play_capsule(reason)){report(reason.empty()?"play acknowledgement unavailable":reason.c_str());return;}
        report("playAck=true, child remains hidden");
        // Current bundled clip is 3400 ms. This one-shot probe stays offscreen.
        std::this_thread::sleep_for(std::chrono::milliseconds(3800));
        report("retiring hidden capsule");
    }
}
struct CapsuleProbe {void* context;home_dock::Config appearance;};
}
void hc_native_glass_probe(void* context,const home_dock::Config& appearance) {
    probe(context,appearance,home_native_glass::HostKind::glass);
}
void hc_native_capsule_probe(void* context,const home_dock::Config& appearance) {
    // Keep host IPC and the animation wait off the SDK lease renewal worker.
    auto* input=new CapsuleProbe{context,appearance};
    pthread_t worker{};
    if(pthread_create(&worker,nullptr,[](void* data)->void* {
        const std::unique_ptr<CapsuleProbe> run(static_cast<CapsuleProbe*>(data));
        probe(run->context,run->appearance,home_native_glass::HostKind::capsule);
        return nullptr;
    },input)==0)pthread_detach(worker);
    else {
        delete input;
        __android_log_print(ANDROID_LOG_WARN,"HyperCeiler.HomeConfig","native app capsule probe worker unavailable");
    }
}
#else
void hc_native_glass_probe(void*,const home_dock::Config&){}
void hc_native_capsule_probe(void*,const home_dock::Config&){}
#endif
