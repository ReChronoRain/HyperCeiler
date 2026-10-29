#include "targets/home/home_layout_config.h"
#include <atomic>
#include <array>
#include <optional>
#include <vector>
#include <cassert>
#include <iostream>
#define __android_log_print(...) 0
#define ANDROID_LOG_INFO 4
constexpr int kTag=0;
constexpr unsigned kIconRenderSlotBase=0,kProgressIconSlotBase=6,kKnobHookSlotBase=53,kDrawerSearchSlot=11;
constexpr unsigned kFolderIconScaleSlot=13,kFolderAnimScaleSlotBase=14,kFolderAnimScaleSlotCount=5,kFolderReturnScaleSlotBase=19,kWidgetScaleSlot=21,kWidgetScaleSlotCount=23;
constexpr unsigned kDockPositionSlotBase=44;
constexpr unsigned kTitleCustomSlot=46,kCustomTitleDrawerSlot=47;
constexpr const char* kKnobHookSymbols[]={"dock","folder","top","bottom","side","capsule","policy","slide"};
struct Slot{uintptr_t address=0;void* replacement=nullptr;void** original=nullptr;int source=0,original_words=0;bool ready=false;};
constexpr unsigned kLayoutSaveSlot=48,kGridPersistSlot=49,kDartColumnsSlot=50,kGridAutofitSlotBase=51;
Slot g_slots[61];
uint32_t hc_dart_columns_ready=0,hc_grid_autofit_ready=0;
unsigned grid_bind_calls=0;bool columns_graph=true,autofit_graph=true;
std::atomic_bool g_drawer_search_requested{false},g_drawer_search_ready{false};
struct Knob{bool hook_armed=false,hook_mode=false;uintptr_t hook_address=0;void* hook_entry=nullptr;void** hook_original=nullptr;int hook_source=0,hook_words=0;uint32_t getter=0,getter_size=0;const char* symbol=nullptr;std::atomic_bool requested{false};std::atomic_int delta_dp{0};};
std::array<Knob,8> g_knobs;
struct Margin{struct{uint32_t target=42;}source;uint32_t size=128;std::vector<uint32_t> margin_callers{46,50};};
struct GridFields{bool valid=false;bool usable()const{return valid;}};
struct Geometry{GridFields fields{true};uint32_t cell=100,occupied=200;std::array<uint32_t,3> patches{4,8,12},sizes{64,80,128};std::array<int,3> frames{10,20,30};};
struct{std::optional<Margin> dock_margin;std::optional<Geometry> geometry;}g_runtime_features;
GridFields g_grid_field;std::array<int,3> g_geometry_frame{};
bool geometry_graph=true,geometry_bind=true;
void hc_layout_workspace_entry(){}void hc_layout_workspace_occupied_entry(){}
namespace home_layout{constexpr const char* kDropGeometrySymbol="fixture";}
void resolve_grid_fields(const char*){if(g_runtime_features.geometry)g_grid_field=g_runtime_features.geometry->fields;}

struct{uintptr_t load_base=0x1000;}dart;auto* g_dart=&dart;
uintptr_t hc_hotseat_margin_callers[32]{};uint32_t hc_hotseat_margin_caller_count=0;
bool g_first_render_discovery=false,have_image=true,have_config=true,allow_install=true,graph=true;
bool folder_graph=true,widget_graph=true,drawer_graph=true,g_first_render_features_scanned=false;
std::atomic_bool g_first_render_finished{false};
uint32_t hc_icon_render_enabled=0,hc_folder_icon_scale_enabled=0,hc_widget_scale_enabled=0;
unsigned discoveries=0,config_reads=0,install_calls=0,image_reads=0;double scale=1,margin=-1;
home_layout::Config cached;
namespace home_layout{bool query_bootstrap_config(Config& out){++config_reads;if(!have_config)return false;out=cached;return true;}}
void init_knob_hooks(){} bool ensure_dart_library(){++image_reads;return have_image;}
void sync_title_config(const home_layout::Config& config){scale=config.tweaks.icon_scale_enabled?1.5:1;g_drawer_search_requested=config.drawer_hide_search;}
int requested_knob_delta(const home_layout::KnobConfig& knob){return knob.enabled?knob.delta_dp:0;}
bool ensure_runtime_features(){assert(g_first_render_discovery);if(g_first_render_features_scanned)return true;++discoveries;if(!graph)return false;g_runtime_features.dock_margin=Margin{};if(geometry_graph&&(g_knobs[2].requested||g_knobs[3].requested||g_knobs[4].requested))g_runtime_features.geometry=Geometry{};g_first_render_features_scanned=true;return true;}
void bind_dart_columns(){++grid_bind_calls;if(columns_graph)for(auto i:{kLayoutSaveSlot,kGridPersistSlot,kDartColumnsSlot})g_slots[i].address=i+1;}
void bind_grid_autofit(){++grid_bind_calls;if(autofit_graph)for(unsigned i=51;i<53;++i)g_slots[i].address=i+1;}
void bind_icon_render(){if(graph)for(unsigned i=0;i<2;++i)g_slots[i].address=i+1;}
void bind_desktop_icon_render(){if(graph)for(unsigned i=2;i<6;++i)g_slots[i].address=i+1;}
void bind_progress_icon_render(){if(graph)for(unsigned i=6;i<10;++i)g_slots[i].address=i+1;}
void bind_drawer_search(){if(g_drawer_search_requested&&drawer_graph)for(unsigned i=11;i<13;++i)g_slots[i].address=i+1;}
void bind_folder_icon_scale(){if(folder_graph)g_slots[kFolderIconScaleSlot].address=100;}
void bind_folder_animation_scale(){if(folder_graph)for(unsigned i=14;i<19;++i)g_slots[i].address=i+100;}
void bind_folder_app_return_scale(){if(folder_graph)for(unsigned i=19;i<21;++i)g_slots[i].address=i+100;}
void bind_widget_scale(){if(widget_graph)for(unsigned i=21;i<44;++i)g_slots[i].address=i+100;}
void bind_dock_position(){if(graph)for(unsigned i=kDockPositionSlotBase;i<kDockPositionSlotBase+2;++i)g_slots[i].address=i+100;}
void bind_title_custom(){if(graph&&!cached.title_custom_labels.empty()){g_slots[kTitleCustomSlot].address=1000;g_slots[kCustomTitleDrawerSlot].address=1001;}}
bool bind_dart_target(uint32_t target,uintptr_t& address,int&,int&){if(!geometry_bind&&target==208)return false;address=target;return true;}
int slot_host(){return 0;}
namespace nhk{bool ensure_slots_live(Slot* slots,int,const std::vector<size_t>& order){++install_calls;for(auto i:order)slots[i].ready=allow_install;return allow_install;}bool slot_ready(const Slot& slot){return slot.ready;}}
void publish_hooks(){hc_dart_columns_ready=g_slots[48].ready&&g_slots[49].ready&&g_slots[50].ready;hc_grid_autofit_ready=g_slots[51].ready&&g_slots[52].ready;g_drawer_search_ready=g_slots[11].ready&&g_slots[12].ready;hc_icon_render_enabled=1;for(unsigned i=0;i<6;++i)hc_icon_render_enabled&=g_slots[i].ready;hc_folder_icon_scale_enabled=g_slots[13].ready;hc_widget_scale_enabled=g_slots[21].ready;auto& k=g_knobs[0];margin=k.hook_armed&&k.requested?k.delta_dp.load()+70:-1;}
void reset(){geometry_graph=geometry_bind=true;g_grid_field={};g_geometry_frame={};g_runtime_features.geometry.reset();columns_graph=autofit_graph=true;hc_dart_columns_ready=hc_grid_autofit_ready=grid_bind_calls=0;for(auto& s:g_slots)s={};for(auto& k:g_knobs){k.hook_armed=k.hook_mode=false;k.hook_address=0;k.requested=false;k.delta_dp=0;}g_drawer_search_ready=false;g_drawer_search_requested=false;hc_icon_render_enabled=hc_folder_icon_scale_enabled=hc_widget_scale_enabled=0;g_first_render_finished=false;g_first_render_features_scanned=false;g_first_render_discovery=false;g_runtime_features.dock_margin.reset();have_image=have_config=allow_install=graph=folder_graph=widget_graph=drawer_graph=true;discoveries=config_reads=install_calls=image_reads=0;cached={};margin=-1;scale=1;}
