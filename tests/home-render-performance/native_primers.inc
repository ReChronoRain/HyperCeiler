#include "targets/home/home_layout_config.h"
#include <sstream>
#include <atomic>
#include <cassert>
#include <iostream>
#include <optional>
#include <vector>
#define __android_log_print(...) 0
#define ANDROID_LOG_INFO 4
#define ANDROID_LOG_WARN 5
constexpr int kTag=0;
using HookFunction=int(*)(void*,void*,void**);using UnhookFunction=int(*)(void*);
using Words=std::array<uint32_t,4>;
struct Slot{uintptr_t address=0;void* entry=nullptr;void** original=nullptr;int source=0;Words words{};bool backend_owned=false;};
constexpr unsigned kNativeColumnsSlot=0,kBackGestureSlot=1,kBackWindowSlot=2;Slot g_slots[3],g_back_window_update;
std::atomic_bool g_back_setup_complete{false},g_dart_ready{false},g_native_columns_prime_attempted{false},g_back_prime_attempted{false};
std::atomic_flag g_loader_priming=ATOMIC_FLAG_INIT;
std::atomic_uint g_back_prime_stage{0},g_back_gesture_config{0};
uint64_t g_native_columns_prime_next=0,clock_ms=1000,g_view_begin=0,g_view_end=0;
std::string g_container_path;HookFunction g_hook_function=nullptr;UnhookFunction g_unhook_function=nullptr;
void* hc_native_columns_original=nullptr;void* hc_back_gesture_original=nullptr;void* hc_back_window_original=nullptr;
void hc_native_columns_entry(){}void hc_back_gesture_entry(){}void hc_back_window_entry(){}
uint32_t hc_native_columns_request=0,hc_grid_autofit_requested=0;
unsigned map_reads=0,image_reads=0,column_scans=0,back_scans=0,installs=0;int thread_id=1;
bool config_ok=true,image_available=true,columns_ok=true,back_ok=true,allow_install=true;
int fixture_pid(){return 1;}int fixture_tid(){return thread_id;}uint64_t monotonic_ms(){return clock_ms;}
namespace home_layout {
Config cfg;
bool query_bootstrap_config(Config& c){if(!config_ok)return false;c=cfg;return true;}
namespace elf_targets {std::optional<int> resolve(const std::vector<std::byte>&){++column_scans;return columns_ok?std::optional<int>{1}:std::nullopt;}}
struct ColumnShape{uint64_t splice=256;unsigned slot=5;};
std::optional<ColumnShape> resolve_native_columns(const std::vector<std::byte>&,int){return ColumnShape{};}
std::optional<BackGestureShape> resolve_back_gesture(const std::vector<std::byte>&){++back_scans;if(!back_ok)return {};BackGestureShape r;r.splice=64;return r;}
std::optional<BackWindowShape> resolve_back_window(const std::vector<std::byte>&){if(!back_ok)return {};BackWindowShape r;r.splice=128;r.update=192;return r;}
}
home_layout::ColumnShape g_native_columns;home_layout::BackGestureShape g_back_gesture_shape;home_layout::BackWindowShape g_back_window_shape;
namespace nhk {
using CodeSource=int;
std::vector<int> parse_file_mappings(std::istream&){++map_reads;return {};}
bool ensure_slots_live(Slot* slots,int,const std::vector<size_t>& order){++installs;for(auto n:order)slots[n].backend_owned=allow_install;return allow_install;}
}
std::optional<std::string> launcher_apk(const std::vector<int>&){return "fixture.apk";}
struct Lib{std::string path="fixture.apk";uint64_t view_begin=4096,view_end=8192;std::vector<std::byte> bytes;};
std::optional<Lib> open_library(const std::string&,const char*,const std::vector<int>&){++image_reads;return image_available?std::optional<Lib>{Lib{}}:std::nullopt;}
bool bind_target(const Lib&,uint64_t target,uintptr_t& address,int&,Words&){address=target;return true;}
int slot_host(){return 0;}int hook(void*,void*,void**){return 0;}
void reset(){for(auto& s:g_slots)s={};g_back_window_update={};g_back_setup_complete=false;g_dart_ready=false;g_native_columns_prime_attempted=false;g_back_prime_attempted=false;g_loader_priming.clear();g_native_columns_prime_next=0;clock_ms+=1000;map_reads=image_reads=column_scans=back_scans=installs=0;config_ok=image_available=columns_ok=back_ok=allow_install=true;thread_id=1;home_layout::cfg={};home_layout::cfg.grid_enabled=true;home_layout::cfg.back_gesture={80,100};}
