/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dock_geometry.h"
#include <memory>
#include <optional>
#include <string>

struct AIBinder;
struct ASurfaceControl;
struct ASurfaceTransaction;
namespace home_native_glass {
struct Viewport {int32_t width,height;float density;uint32_t ui_mode;};
enum class Command {pause,resume,fresh,refresh,sync_geometry};
enum class Failure {temporary,unsupported};
enum class Destination {glass,preferences};
enum class HostKind {glass,capsule};
struct KeyguardProtocol {uint32_t add,remove,changed;};
struct FrameEffects {
    void*(*corner)(void*,void*,float,float)=nullptr;
    void*(*matrix)(void*,void*,float,float,float,float)=nullptr;
};
// Control-worker ownership only: creation, configuration reads and destruction
// may call Binder. Frame callbacks only use the borrowed native surfaces.
class Client {
    struct State;
    std::shared_ptr<State> state_;
    explicit Client(std::shared_ptr<State> state);
public:
    class Window {
        struct State;
        std::unique_ptr<State> state_;
        explicit Window(std::unique_ptr<State> state);
        friend class Client;
    public:
        ~Window();
        ASurfaceControl* surface()const;
    };
    class Host {
        struct State;
        std::unique_ptr<State> state_;
        explicit Host(std::unique_ptr<State> state);
        friend class Client;
    public:
        ~Host();
        ASurfaceControl* surface()const;
        ASurfaceControl* parent()const;
        AIBinder* lifetime()const; // Borrowed until Host destruction.
        bool valid()const;
        bool notify(Command command);
        bool play_capsule(std::string& reason);
        bool unlock(int64_t epoch_ms,int32_t style,std::string& reason);
        std::optional<bool> ready(std::string& reason);
    };
    static std::unique_ptr<Client> open(void* retained_context,std::string& reason,Failure* failure=nullptr,Destination destination=Destination::glass);
    static std::unique_ptr<Client> open_native(void* retained_context,std::string& reason,Failure* failure=nullptr);
    std::unique_ptr<Window> window(std::string& reason);
    bool compatible()const;
    std::optional<KeyguardProtocol> keyguard_protocol(std::string& reason,Failure* failure=nullptr);
    bool set_effect(ASurfaceTransaction* transaction,ASurfaceControl* layer,float radius,int32_t blur);
    FrameEffects frame_effects()const;
    std::optional<Viewport> viewport(std::string& reason);
    std::unique_ptr<Host> create(const home_dock::Bounds& bounds,bool dark,std::string& reason,HostKind kind=HostKind::glass);
};
}
