#include "targets/home/home_workspace_swipe.h"
#include <mutex>
#include <atomic>
#include <vector>
#include <cassert>
#include <iostream>
#define ANDROID_LOG_INFO 4
#define __android_log_print(...) 0
struct AIBinder;
std::atomic<uint64_t> dock_edit_packed{0};
void* hc_workspace_scene_original=nullptr;
uint32_t hc_workspace_double_enabled=0,hc_workspace_double_selected=0,hc_workspace_swipe_enabled=0;
std::vector<int> emitted;
void emit(int action,uint64_t){if(action)emitted.push_back(action);}
bool start_worker_locked(){return true;}
struct Event {int masked=0,full=0,count=1;int64_t time=1000;float x=500,y=1000;int id=7;};
bool stock_active=false;std::vector<int> forwarded;
void stock(void*,void* input){auto& e=*static_cast<Event*>(input);forwarded.push_back(e.masked);if(e.masked==0)stock_active=true;if(e.masked==1||e.masked==3)stock_active=false;}
void install_api(){
 api.masked=[](void* p){return static_cast<Event*>(p)->masked;};
 api.action=[](void* p){return static_cast<Event*>(p)->full;};
 api.set_action=[](void* p,int value){auto& e=*static_cast<Event*>(p);e.full=value;e.masked=value&255;};
 api.pointers=[](void* p)->size_t{return static_cast<Event*>(p)->count;};
 api.time=[](void* p){return static_cast<Event*>(p)->time;};
 api.x=[](void* p,size_t){return static_cast<Event*>(p)->x;};
 api.y=[](void* p,size_t){return static_cast<Event*>(p)->y;};
 api.pointer_id=[](void* p,size_t i){return static_cast<Event*>(p)->id+int(i);};
 hc_workspace_scene_original=reinterpret_cast<void*>(stock);settings={30,false,1,1200,1};
 wake=1;workspace_ready=scene_ready=true;scope_contract.down_return=1;
}
void reset_fixture(){workspace_state.reset();workspace_start=stock_block_start=stock_block_end=0;stock_active=false;forwarded.clear();emitted.clear();dock_edit_packed=0;install_api();publish_workspace_locked();}
