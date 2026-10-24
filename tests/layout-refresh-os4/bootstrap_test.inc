#include "targets/home/home_layout_config.h"
#include <atomic>
#include <optional>
#include <vector>
#include <cassert>
#include <iostream>
#define __android_log_print(...) 0
#define ANDROID_LOG_INFO 4
constexpr int kTag=0;
constexpr unsigned kIconRenderSlotBase=0,kProgressIconSlotBase=6,kKnobHookSlotBase=10,kDrawerSearchSlot=11;
constexpr const char* kKnobHookSymbols[]={"test"};
struct Slot{uintptr_t address=0;void* replacement=nullptr;void** original=nullptr;int source=0,original_words=0;bool ready=false;};
Slot g_slots[13];
std::atomic_bool g_drawer_search_requested{false},g_drawer_search_ready{false};
struct Knob{bool hook_armed=false,hook_mode=false;uintptr_t hook_address=0;void* hook_entry=nullptr;void** hook_original=nullptr;int hook_source=0,hook_words=0;uint32_t getter=0,getter_size=0;const char* symbol=nullptr;std::atomic_bool requested{false};std::atomic_int delta_dp{0};};
Knob g_knobs[1];
struct Margin{struct{uint32_t target=42;}source;uint32_t size=128;std::vector<uint32_t> margin_callers{46,50};};
struct{std::optional<Margin> dock_margin;}g_runtime_features;
struct{uintptr_t load_base=0x1000;}dart;auto* g_dart=&dart;
uintptr_t hc_hotseat_margin_callers[32]{};uint32_t hc_hotseat_margin_caller_count=0;
bool g_first_render_discovery=false,have_image=true,have_config=true,allow_install=true,graph=true;
uint32_t hc_icon_render_enabled=0;unsigned discoveries=0,config_reads=0;double scale=1,margin=-1;
home_layout::Config cached;
namespace home_layout{bool query_bootstrap_config(Config& out){++config_reads;if(!have_config)return false;out=cached;return true;}}
void init_knob_hooks(){} bool ensure_dart_library(){return have_image;}
void sync_title_config(const home_layout::Config& config){scale=config.tweaks.icon_scale_enabled?1.5:1;g_drawer_search_requested=config.drawer_hide_search;}
int requested_knob_delta(const home_layout::KnobConfig& knob){return knob.delta_dp;}
bool ensure_runtime_features(){assert(g_first_render_discovery);++discoveries;if(graph)g_runtime_features.dock_margin=Margin{};return graph;}
void bind_icon_render(){if(graph)for(unsigned i=0;i<2;++i)g_slots[i].address=i+1;}
void bind_desktop_icon_render(){if(graph)for(unsigned i=2;i<6;++i)g_slots[i].address=i+1;}
void bind_progress_icon_render(){if(graph)for(unsigned i=6;i<10;++i)g_slots[i].address=i+1;}
void bind_drawer_search(){if(g_drawer_search_requested)for(unsigned i=11;i<13;++i)g_slots[i].address=i+1;}
bool bind_dart_target(uint32_t target,uintptr_t& address,int&,int&){address=target;return true;}
int slot_host(){return 0;}
namespace nhk{bool ensure_slots_live(Slot* slots,int,const std::vector<size_t>& order){for(auto i:order)slots[i].ready=allow_install;return allow_install;}bool slot_ready(const Slot& slot){return slot.ready;}}
void publish_hooks(){g_drawer_search_ready=g_slots[11].ready&&g_slots[12].ready;hc_icon_render_enabled=1;for(unsigned i=0;i<6;++i)hc_icon_render_enabled&=g_slots[i].ready;auto& k=g_knobs[0];margin=k.hook_armed&&k.requested?k.delta_dp.load()+70:-1;}
