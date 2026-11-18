/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_native_glass_client.h"
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_unlock_bundle.h"
#endif
#if defined(__ANDROID__) && !defined(HYPERCEILER_HYOS_BACKEND)
#include "home_native_binder_contract.h"
#include "home_native_integer_contract.h"
#include "home_native_parcel_contract.h"
#include "home_native_parcelable_contract.h"
#include "home_native_parcelable_reader.h"
#include "home_native_surface_access.h"
#include "home_native_effect_contract.h"
#include "home_native_display_contract.h"
#include "home_recent_cards_live_image.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/surface_control.h>
#include <bit>
#include <cmath>
#include <utility>
#include <unistd.h>

namespace home_native_glass {
namespace {
namespace sdk=home_layout::cards::sdk;
struct Reply {std::array<uint64_t,6> words{};};
struct StringRef {const char* data;size_t length;};
struct MainResult {uint64_t status;void* object;};
static_assert(sizeof(Reply)==48&&sizeof(StringRef)==16&&sizeof(MainResult)==16);
using Drop=void(*)(void*);
struct Api {
    home_native_provider::AcquireContract acquire;
    home_native_provider::CallContract call;
    home_native_bundle::Contract bundle;
    std::optional<home_native_bundle::IntegerContract> integer;
    home_native_parcel::Contract parcel;
    home_native_parcelable::Contract parcelable;
    home_native_surface::Contract surface;
    home_native_surface::Access access;
    std::optional<home_native_surface::Effects> effects;
    home_native_display::Contract display;
    home_native_binder::Contract binder;
    std::vector<sdk::LiveImage> images;
    Drop acquire_error=nullptr,call_error=nullptr;
    uintptr_t address(sdk::Address at)const{return images[at.image].base+at.va;}
    Drop drop(sdk::Address at)const{return reinterpret_cast<Drop>(address(at));}
    static std::shared_ptr<Api> load(std::string& reason,Failure& failure) {
        // One control worker owns setup. Retain only the small resolved table
        // across renderer restarts; a material-process death is not an SDK ABI change.
        static std::shared_ptr<Api> bound;
        if(bound&&sdk::images_current(bound->images))return bound;
        bound.reset();
        constexpr std::array<std::string_view,14> names={"libhyper_os_core_data_public.so","libhyper_os_core_data_private.dylib.so",
            "libhyper_os_foundation_private.dylib.so","libhyper_os_sys.dylib.so","libstd.dylib.so","libbinder_rs.dylib.so",
            "libhyper_os_schema_public.so","libhyper_os_aidl.dylib.so","libhyper_os_ui_public.so",
            "libhyper_os_ui_private.dylib.so","libhyper_os_schema_private.dylib.so",
            "libhyper_os_app_public.so","libhyper_os_app_private.dylib.so","libhyper_os_binder_private.dylib.so"};
        auto live=sdk::capture_live(names,&reason);if(!live)return {};
        failure=Failure::unsupported;
        std::vector<sdk::Image> parsed;
        for(const auto& file:*live) {
            auto image=sdk::Image::parse(file.file,file.base);
            if(!image){reason="SDK metadata declined";return {};}parsed.push_back(std::move(*image));
        }
        const sdk::Graph graph(std::move(parsed));
        const auto acquire=home_native_provider::resolve(graph);const auto call=home_native_provider::resolve_call(graph);
        const auto bundle=home_native_bundle::resolve(graph);
        if(!acquire||!call||!bundle||!home_native_bundle::call_owner(graph,*call,*bundle)) {
            reason="Provider/Bundle ownership contract declined";return {};
        }
        const auto parcel=home_native_parcel::resolve(graph,*bundle);const auto parcelable=home_native_parcelable::resolve(graph);
        const auto surface=home_native_surface::resolve(graph);
        const auto display=home_native_display::resolve(graph);const auto binder=home_native_binder::resolve(graph);
        if(!parcel||!parcelable||!surface||!display||!binder){reason="SDK object input/ownership contract declined";return {};}
        const auto access=home_native_surface::resolve_access(graph,*surface);
        if(!access||!sdk::images_current(*live)){reason="SDK surface linkage changed";return {};}
        auto api=std::make_shared<Api>();
        api->acquire=*acquire;api->call=*call;api->bundle=*bundle;
        api->integer=home_native_bundle::resolve_integer(graph);api->parcel=*parcel;
        api->parcelable=*parcelable;api->surface=*surface;api->access=*access;api->display=*display;api->binder=*binder;
        api->effects=home_native_surface::resolve_effects(graph,*surface);
        api->images=std::move(*live);
        const auto destructor=[&](const home_native_provider::ErrorContract& error)->Drop {
            uintptr_t target=0;
            if(!sdk::read_live(api->address(error.table_slot),std::as_writable_bytes(std::span(&target,1)))
                ||target!=api->address(error.drop))return nullptr;
            return reinterpret_cast<Drop>(target);
        };
        api->acquire_error=destructor(api->acquire.error);api->call_error=destructor(api->call.error);
        if(!api->acquire_error||!api->call_error){reason="SDK error destructor binding declined";return {};}
        // Runtime callbacks retain resolved functions and mapping identities,
        // never ELF snapshots or a parser per frame/host.
        for(auto& image:api->images){std::vector<std::byte>().swap(image.file);}
        bound=api;return api;
    }
    bool error(Reply& reply,uint32_t at,const home_native_provider::ErrorContract& error,Drop destroy,std::string& reason)const {
        auto bytes=std::as_writable_bytes(std::span(reply.words));uintptr_t table=0;
        std::memcpy(&table,bytes.data()+at+error.vector+error.table_field,sizeof(table));
        if(table!=address(error.table)){reason="Unsupported owned SDK error";return false;}
        int32_t code=0;std::memcpy(&code,bytes.data()+at+error.code,sizeof(code));destroy(bytes.data()+at+error.vector);
        reason="Platform request declined, code="+std::to_string(code);return true;
    }
    std::optional<bool> boolean(void* object,std::string_view key)const {
        const auto value=reinterpret_cast<uint16_t(*)(void*,StringRef)>(address(bundle.get_boolean))(object,{key.data(),key.size()});
        const auto mask=uint16_t(1u|(1u<<bundle.boolean_shift));
        if((value&~mask)||(value&1u))return {};return ((value>>bundle.boolean_shift)&1u)!=0;
    }
};
struct Owned {
    void* value=nullptr;Drop drop=nullptr;
    Owned()=default;
    Owned(void* value,Drop drop):value(value),drop(drop){}
    Owned(const Owned&)=delete;Owned& operator=(const Owned&)=delete;
    Owned(Owned&& other)noexcept:value(std::exchange(other.value,nullptr)),drop(other.drop){}
    Owned& operator=(Owned&& other)noexcept {
        if(this!=&other){if(value)drop(value);value=std::exchange(other.value,nullptr);drop=other.drop;}return *this;
    }
    ~Owned(){if(value)drop(value);}
};
using Parcel=std::unique_ptr<AParcel,decltype(&AParcel_delete)>;
using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
const AIBinder_Class* owner_class() {
    static const auto* type=AIBinder_Class_define("com.sevtinge.hyperceiler.home.GlassOwner",[](void* data){return data;},[](void*){},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
    return type;
}
}
struct Client::State {
    std::shared_ptr<Api> api;
    void* context=nullptr;
    Owned resolver,client;
    Binder owner{nullptr,AIBinder_decStrong};
    uint64_t sequence=0;bool unsupported=false;
    // These members destruct in reverse: Client before its Resolver. SDK
    // Context is independently retained by the caller for the launcher process.
    Owned invoke(std::string_view method,std::string_view id,void* input,std::string& reason) {
        if(unsupported){reason="SDK session unsupported";return {};}
        if(!client.value){reason="Glass Provider session not opened";return {};}
        const auto& contract=api->call;const char* data=id.data();const size_t length=id.size();
        alignas(8) std::array<uint8_t,32> argument{};
        std::memcpy(argument.data()+contract.arg_pointer,&data,sizeof(data));
        std::memcpy(argument.data()+contract.arg_length,&length,sizeof(length));
        auto reply=reinterpret_cast<Reply(*)(void*,StringRef,const void*,void*)>(api->address(contract.call))(
            client.value,{method.data(),method.size()},argument.data(),input);
        const auto bytes=std::as_bytes(std::span(reply.words));const auto tag=std::to_integer<uint8_t>(bytes[contract.tag]);
        if(tag==contract.rejected) {
            if(!api->error(reply,contract.error_at,contract.error,api->call_error,reason))unsupported=true;
            return {};
        }
        uintptr_t result=0;std::memcpy(&result,bytes.data()+contract.pointer,sizeof(result));
        if(tag!=contract.accepted){unsupported=true;reason="Unsupported Provider reply";return {};}
        if(!result){reason="Provider Bundle unavailable";return {};}
        return {reinterpret_cast<void*>(result),api->drop(api->bundle.drop)};
    }
};
struct Client::Window::State {
    std::shared_ptr<Api> api;
    Owned control;
};
struct Client::Host::State {
    std::shared_ptr<Client::State> session;
    std::string id;
    HostKind kind=HostKind::glass;
    Owned control,parent;
    Binder lifetime{nullptr,AIBinder_decStrong};
    ~State(){
        // A dead lifetime Binder cannot revive. The renderer already lost its
        // resources, so do not issue a synchronous release to that dead process.
        if(session&&!id.empty()&&(!lifetime||AIBinder_isAlive(lifetime.get()))) {
            std::string ignored;session->invoke(kind==HostKind::capsule?"xiaoheizi_native_release":"dock_glass_release",id,nullptr,ignored);
        }
    }
};
Client::Client(std::shared_ptr<State> state):state_(std::move(state)){}
Client::Window::Window(std::unique_ptr<State> state):state_(std::move(state)){}
Client::Window::~Window()=default;
ASurfaceControl* Client::Window::surface()const {
    return reinterpret_cast<ASurfaceControl*(*)(void*)>(state_->api->address(state_->api->access.native))(state_->control.value);
}
std::unique_ptr<Client> Client::open_native(void* context,std::string& reason,Failure* failure) {
    Failure kind=Failure::temporary;
    struct Result {Failure* target;Failure& kind;~Result(){if(target)*target=kind;}} result{failure,kind};
    if(!context){reason="Retained Context unavailable";return {};}
    auto api=Api::load(reason,kind);if(!api)return {};
    kind=Failure::temporary;
    auto state=std::make_shared<State>();state->api=std::move(api);state->context=context;
    return std::unique_ptr<Client>(new Client(std::move(state)));
}
std::unique_ptr<Client::Window> Client::window(std::string& reason) {
    const auto& api=*state_->api;
    const auto parent=reinterpret_cast<MainResult(*)()>(api.address(api.access.main))();
    if(uint32_t(parent.status)!=0||!parent.object){reason="Main SurfaceControl unavailable";return {};}
    auto window=std::make_unique<Window::State>();window->api=state_->api;
    window->control={parent.object,api.drop(api.surface.drop)};
    if(!reinterpret_cast<bool(*)(void*)>(api.address(api.surface.valid))(parent.object)) {
        reason="Main SurfaceControl is not valid";return {};
    }
    return std::unique_ptr<Window>(new Window(std::move(window)));
}
Client::Host::Host(std::unique_ptr<State> state):state_(std::move(state)){}
Client::Host::~Host()=default;
std::unique_ptr<Client> Client::open(void* context,std::string& reason,Failure* failure,Destination destination) {
    Failure kind=Failure::temporary;
    struct Result {Failure* target;Failure& kind;~Result(){if(target)*target=kind;}} result{failure,kind};
    if(!context){reason="Retained Context unavailable";return {};}
    auto api=Api::load(reason,kind);if(!api)return {};
    kind=Failure::temporary;
    auto state=std::make_shared<State>();state->api=std::move(api);state->context=context;
    const auto& contract=state->api->acquire;
    state->resolver={reinterpret_cast<void*(*)(void* const*)>(state->api->address(contract.create_resolver))(&context),
        state->api->drop(contract.delete_resolver)};
    if(!state->resolver.value){reason="Resolver unavailable";return {};}
    const std::string_view authority=destination==Destination::glass
        ?"com.sevtinge.hyperceiler.provider.homeglass":"com.sevtinge.hyperceiler.provider.sharedprefs";
    auto reply=reinterpret_cast<Reply(*)(void*,StringRef)>(state->api->address(contract.acquire))(
        state->resolver.value,{authority.data(),authority.size()});
    const auto bytes=std::as_bytes(std::span(reply.words));const auto tag=std::to_integer<uint8_t>(bytes[contract.tag]);
    if(tag==contract.rejected) {
        if(!state->api->error(reply,contract.error_at,contract.error,state->api->acquire_error,reason))kind=Failure::unsupported;
        return {};
    }
    uintptr_t client=0;std::memcpy(&client,bytes.data()+contract.pointer,sizeof(client));
    if(tag!=contract.accepted||std::to_integer<uint8_t>(bytes[contract.option])!=contract.present||!client) {
        if(tag!=contract.accepted)kind=Failure::unsupported;
        reason="Provider unavailable or unsupported acquire reply";return {};
    }
    state->client={reinterpret_cast<void*>(client),state->api->drop(contract.delete_client)};
    const auto* type=owner_class();if(!type){reason="Native owner class unavailable";return {};}
    state->owner.reset(AIBinder_new(type,nullptr));if(!state->owner){reason="Native owner unavailable";return {};}
    return std::unique_ptr<Client>(new Client(std::move(state)));
}
bool Client::compatible()const{return !state_->unsupported;}
std::optional<KeyguardProtocol> Client::keyguard_protocol(std::string& reason,Failure* failure) {
    Failure kind=Failure::unsupported;
    struct Result {Failure* target;Failure& kind;~Result(){if(target)*target=kind;}} report{failure,kind};
    const auto& api=*state_->api;
    if(!api.integer){reason="SDK integer result ABI unavailable";return {};}
    auto result=state_->invoke("hc_home_platform_contract","",nullptr,reason);
    if(!result.value){kind=state_->unsupported?Failure::unsupported:Failure::temporary;return {};}
    if(api.boolean(result.value,"available")!=std::optional<bool>{true}) {
        reason="Platform keyguard subscription shape unavailable";return {};
    }
    std::array<uint32_t,3> values{};
    constexpr std::array<std::string_view,3> keys={"keyguardAdd","keyguardRemove","keyguardChanged"};
    for(size_t i=0;i<keys.size();++i) {
        const auto value=reinterpret_cast<uint64_t(*)(void*,StringRef)>(api.address(api.integer->get))(
            result.value,{keys[i].data(),keys[i].size()});
        const auto mask=(uint64_t(1)<<api.integer->shift)-1;
        if((value&mask)!=0||(value>>api.integer->shift)<FIRST_CALL_TRANSACTION
            ||(value>>api.integer->shift)>LAST_CALL_TRANSACTION) {
            reason="Platform keyguard operation outside Binder range";return {};
        }
        values[i]=static_cast<uint32_t>(value>>api.integer->shift);
    }
    if(values[0]==values[1]){reason="Platform keyguard operations overlap";return {};}
    kind=Failure::temporary;
    return KeyguardProtocol{values[0],values[1],values[2]};
}
bool Client::set_effect(ASurfaceTransaction* transaction,ASurfaceControl* layer,float radius,int32_t blur) {
    if(!transaction||!layer)return false;
    const auto& api=*state_->api;
    if(!api.effects){state_->unsupported=true;return false;}
    // NDK Transaction/SurfaceControl are native platform pointers. The SDK FFI
    // bridges are proven to construct their own borrowed sp<> and balance it;
    // callers neither fabricate an SDK Box nor reinterpret an SDK field.
    const auto native_blur=reinterpret_cast<void*(*)(void*,void*,int32_t)>(api.address(api.effects->blur));
    const auto native_corner=reinterpret_cast<void*(*)(void*,void*,float,float)>(api.address(api.effects->corner));
    if(native_blur(transaction,layer,blur)!=transaction||native_corner(transaction,layer,radius,radius)!=transaction) {
        state_->unsupported=true;return false;
    }
    return true;
}
FrameEffects Client::frame_effects()const {
    const auto& api=*state_->api;if(!api.effects)return {};
    // Api is cached for this process. These are proven native C++ bridges,
    // not SDK objects: frames perform no SDK lookup or control-plane RPC.
    return {reinterpret_cast<decltype(FrameEffects::corner)>(api.address(api.effects->corner)),
        reinterpret_cast<decltype(FrameEffects::matrix)>(api.address(api.effects->matrix))};
}
std::optional<Viewport> Client::viewport(std::string& reason) {
    const auto& api=*state_->api;const auto& display=api.display;
    Owned configuration{reinterpret_cast<void*(*)(void*)>(api.address(display.configuration))(state_->context),api.drop(display.drop_configuration)};
    if(!configuration.value){reason="Context configuration unavailable";return {};}
    Owned values{reinterpret_cast<void*(*)(void*)>(api.address(display.metrics))(configuration.value),api.drop(display.drop_metrics)};
    if(!values.value){reason="Context metrics unavailable";return {};}
    Viewport result{reinterpret_cast<int32_t(*)(void*)>(api.address(display.width))(values.value),
        reinterpret_cast<int32_t(*)(void*)>(api.address(display.height))(values.value),
        reinterpret_cast<float(*)(void*)>(api.address(display.density))(values.value),
        reinterpret_cast<uint32_t(*)(void*)>(api.address(display.ui_mode))(configuration.value)};
    if(result.width<=0||result.height<=0||!std::isfinite(result.density)||result.density<=0){reason="Context metrics invalid";return {};}
    return result;
}
std::unique_ptr<Client::Host> Client::create(const home_dock::Bounds& bounds,bool dark,std::string& reason,HostKind kind) {
    const auto& api=*state_->api;
    const bool capsule=kind==HostKind::capsule;
    if(bounds.width<=0||bounds.height<=0||!std::isfinite(bounds.radius)||bounds.radius<0){reason="Invalid glass bounds";return {};}
    Parcel parcel(AParcel_create(),AParcel_delete);if(!parcel){reason="Native Parcel unavailable";return {};}
    enum Kind:int32_t {Integer=1,Float=7,Boolean=9,BinderValue=15};
    struct Field {std::string_view key;Kind kind;uint32_t value;};
    std::array<Field,5> fields={{{"width",Integer,static_cast<uint32_t>(bounds.width)},
        {capsule?"capsuleHeight":"height",Integer,static_cast<uint32_t>(bounds.height)},{"owner",BinderValue,0},
        {"radius",Float,std::bit_cast<uint32_t>(bounds.radius)},{"dark",Boolean,dark?1u:0u}}};
    auto configuration=std::span(fields).first(capsule?3:fields.size());
    const auto hash=[](std::string_view key){uint32_t value=0;for(unsigned char c:key)value=value*31+c;return std::bit_cast<int32_t>(value);};
    std::sort(configuration.begin(),configuration.end(),[&](const auto& a,const auto& b){return hash(a.key)<hash(b.key);});
    bool written=AParcel_writeInt32(parcel.get(),-1)==STATUS_OK&&AParcel_writeInt32(parcel.get(),0x4c444e42)==STATUS_OK;
    const auto start=AParcel_getDataPosition(parcel.get());written=written&&AParcel_writeInt32(parcel.get(),configuration.size())==STATUS_OK;
    for(const auto& field:configuration) {
        written=written&&AParcel_writeString(parcel.get(),field.key.data(),field.key.size())==STATUS_OK
            &&AParcel_writeInt32(parcel.get(),field.kind)==STATUS_OK;
        if(field.kind==BinderValue)written=written&&AParcel_writeStrongBinder(parcel.get(),state_->owner.get())==STATUS_OK;
        else if(field.kind==Float)written=written&&AParcel_writeFloat(parcel.get(),std::bit_cast<float>(field.value))==STATUS_OK;
        else written=written&&AParcel_writeInt32(parcel.get(),static_cast<int32_t>(field.value))==STATUS_OK;
    }
    const auto end=AParcel_getDataPosition(parcel.get());
    written=written&&AParcel_setDataPosition(parcel.get(),0)==STATUS_OK&&AParcel_writeInt32(parcel.get(),end-start)==STATUS_OK
        &&AParcel_setDataPosition(parcel.get(),end)==STATUS_OK&&AParcel_writeBool(parcel.get(),false)==STATUS_OK
        &&AParcel_setDataPosition(parcel.get(),0)==STATUS_OK;
    if(!written){reason="Typed glass configuration write failed";return {};}
    AParcel* borrowed=parcel.get();
    Owned input{reinterpret_cast<void*(*)(AParcel* const*)>(api.address(api.parcel.bundle_from))(&borrowed),api.drop(api.bundle.drop)};
    if(!input.value){reason="SDK rejected typed glass configuration";return {};}
    auto host=std::make_unique<Host::State>();host->session=state_;
    host->kind=kind;
    host->id=(capsule?"xc-native-":"native-glass-")+std::to_string(getpid())+"-"+std::to_string(++state_->sequence);
    auto response=state_->invoke(capsule?"xiaoheizi_native_create":"dock_glass_native_surface_create",host->id,input.value,reason);
    if(!response.value)return {};
    if(api.boolean(response.value,capsule?"surfaceReady":"frameCommitted")!=std::optional<bool>{true}){
        reason=capsule?"Capsule surface unavailable":"Glass frame unavailable";return {};
    }
    constexpr std::string_view key="surfaceControl",life="lifetime";
    Parcel payload(reinterpret_cast<AParcel*(*)(void*,StringRef)>(api.address(api.parcelable.get))(
        response.value,{key.data(),key.size()}),AParcel_delete);
    if(!payload||!home_native_parcelable::read_payload(payload.get(),"android.view.SurfaceControl")) {
        reason="Glass Parcelable unavailable";return {};
    }
    host->control={reinterpret_cast<void*(*)()>(api.address(api.surface.make))(),api.drop(api.surface.drop)};
    if(!host->control.value){reason="SDK SurfaceControl unavailable";return {};}
    borrowed=payload.get();const auto status=reinterpret_cast<uint64_t(*)(void* const*,AParcel* const*)>(api.address(api.surface.read))(
        &host->control.value,&borrowed);
    if((status&api.surface.error_tag)||!reinterpret_cast<bool(*)(void*)>(api.address(api.surface.valid))(host->control.value)) {
        reason="Glass SurfaceControl decode declined";return {};
    }
    host->lifetime.reset(reinterpret_cast<AIBinder*(*)(void*,StringRef)>(api.address(api.binder.get))(
        response.value,{life.data(),life.size()}));
    if(!host->lifetime||!AIBinder_isAlive(host->lifetime.get())){reason="Renderer lifetime unavailable";return {};}
    const auto parent=reinterpret_cast<MainResult(*)()>(api.address(api.access.main))();
    if(uint32_t(parent.status)!=0||!parent.object){reason="Main SurfaceControl unavailable";return {};}
    host->parent={parent.object,api.drop(api.surface.drop)};
    if(!reinterpret_cast<bool(*)(void*)>(api.address(api.surface.valid))(host->parent.value)) {
        reason="Main SurfaceControl is not valid";return {};
    }
    return std::unique_ptr<Host>(new Host(std::move(host)));
}
ASurfaceControl* Client::Host::surface()const {
    return reinterpret_cast<ASurfaceControl*(*)(void*)>(state_->session->api->address(state_->session->api->access.native))(state_->control.value);
}
ASurfaceControl* Client::Host::parent()const {
    return reinterpret_cast<ASurfaceControl*(*)(void*)>(state_->session->api->address(state_->session->api->access.native))(state_->parent.value);
}
AIBinder* Client::Host::lifetime()const{return state_->lifetime.get();}
bool Client::Host::valid()const {
    const auto test=reinterpret_cast<bool(*)(void*)>(state_->session->api->address(state_->session->api->surface.valid));
    return AIBinder_isAlive(state_->lifetime.get())&&test(state_->control.value)&&test(state_->parent.value);
}
std::optional<bool> Client::Host::ready(std::string& reason) {
    auto response=state_->session->invoke("dock_glass_status",state_->id,nullptr,reason);
    if(!response.value)return {};
    const auto result=state_->session->api->boolean(response.value,"backgroundReady");
    if(!result)reason="Material readiness flag unavailable";
    return result;
}
bool Client::Host::unlock(int64_t epoch,int32_t style,std::string& reason) {
    Parcel parcel(AParcel_create(),AParcel_delete);
    if(!parcel||!write_unlock(parcel.get(),epoch,style)){reason="Unlock epoch/style Parcel unavailable";return false;}
    const auto& api=*state_->session->api;AParcel* borrowed=parcel.get();
    Owned input{reinterpret_cast<void*(*)(AParcel* const*)>(api.address(api.parcel.bundle_from))(&borrowed),api.drop(api.bundle.drop)};
    if(!input.value){reason="Unlock epoch/style Bundle unavailable";return false;}
    return state_->session->invoke("dock_glass_unlock",state_->id,input.value,reason).value!=nullptr;
}
bool Client::Host::play_capsule(std::string& reason) {
    if(state_->kind!=HostKind::capsule)return false;
    auto response=state_->session->invoke("xiaoheizi_native_play",state_->id,nullptr,reason);
    return response.value&&state_->session->api->boolean(response.value,"playing")==std::optional<bool>{true};
}
bool Client::Host::notify(Command command) {
    constexpr std::array<std::string_view,5> names={"dock_glass_pause_capture","dock_glass_resume_capture",
        "dock_glass_fresh_capture","dock_glass_refresh","dock_glass_sync_geometry"};
    std::string ignored;return state_->session->invoke(names[static_cast<size_t>(command)],state_->id,nullptr,ignored).value!=nullptr;
}
}
#endif
