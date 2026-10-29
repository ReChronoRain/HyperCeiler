#!/usr/bin/env python3
"""Compile actual property dispatch/start bodies with counted OS boundaries."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
native=(ROOT/'app/src/main/cpp/targets/home/hyperceiler_home.cpp').read_text()
hooks=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
engine=(ROOT/'app/src/main/cpp/targets/home/tweaks/entry.cpp').read_text()
def body(source,name):
    start=source.index(name+' {') if name+' {' in source else source.index(name+'{')
    at=source.index('{',start);end=at+1;depth=1
    while depth:depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]
prefix=r'''
#include <atomic>
#include <cassert>
#include <cstring>
#include <string_view>
#include <iostream>
#include <pthread.h>
using HookFunction=int(*)(void*,void*,void**);using UnhookFunction=int(*)(void*);
constexpr int ANDROID_LOG_ERROR=6;constexpr const char* kTag="test";
void __android_log_print(int,const char*,const char*,...){}
unsigned identity_reads=0,motion_calls=0,prepares=0,first_calls=0,columns=0,back=0,threads=0,adoptions=0;
bool launcher=true,recursive=false;int process_pid=100,process_tid=0,create_result=0;
int fixture_pid(){return process_pid;}
int fixture_tid(){return process_tid?process_tid:process_pid;}
std::atomic_bool g_started{false},g_ready{false},g_dart_ready{false},g_back_setup_complete{false};
std::atomic_int g_attempts{0};constexpr int kMaxWorkerAttempts=3;
std::atomic_flag g_loader_priming=ATOMIC_FLAG_INIT;
int hook(void*,void*,void**){return 0;}
HookFunction g_hook_function=hook;UnhookFunction g_unhook_function=nullptr;
bool adopt_layout_state(){++adoptions;return false;}
void report_start_verdict(const char*,const char*,bool,int){}
void prime_native_columns(HookFunction,UnhookFunction){++columns;}
void prime_back_regions(HookFunction,UnhookFunction){++back;}
void* worker(void*){return nullptr;}
int fixture_create(pthread_t*,const pthread_attr_t*,void*(*)(void*),void*){++threads;return create_result;}
int fixture_setname(pthread_t,const char*){return 0;}
void home_layout_prepare_for_launcher_child(){++prepares;}
void prime_home_layout_first_render(HookFunction,UnhookFunction){++first_calls;}
bool is_launcher_process(){return launcher;}
bool is_prestart_property(std::string_view n){return n=="persist.sys.usap_pool_enabled";}
int copy_property_value(char* dst,std::string_view v){if(!dst)return 0;std::memcpy(dst,v.data(),v.size());dst[v.size()]=0;return v.size();}
std::atomic_bool g_high_device_level{false},g_disable_prestart{false},g_soft_glass{false};
int original(const char*,char* v){if(v)std::strcpy(v,"OEM");return 3;}
auto g_original_system_property_get=original;
int hooked_system_property_get(const char*,char*);
void start_motion_after_specialization(){++motion_calls;if(recursive){char value[92]{};assert(hooked_system_property_get("ro.config.device_level_for_animation",value)==1);assert(std::strcmp(value,"2")==0);}}
namespace home_layout_engine {
std::atomic_bool g_layoutEngineStarted{false};std::atomic<int> g_layoutEngineOwner{0};
unsigned resets=0,guards=0,wakes=0;
bool IsLauncherProcess(){++identity_reads;return launcher;}
void HomeLayoutEnginePrepareForLauncherChild(){++resets;if(g_layoutEngineOwner!=fixture_pid())g_layoutEngineStarted=false;}
void MonitorLoop(){}void InstallMadviseGuard(){++guards;}void WakeMonitor(){++wakes;}
#define LOGE(...) ((void)0)
#define LOGI(...) ((void)0)
'''
engine_start=body(engine,'void StartHomeLayoutEngine()').replace('getpid()','fixture_pid()').replace('pthread_create(','fixture_create(').replace('pthread_setname_np(','fixture_setname(')
def thread_boundaries(source):return source.replace('gettid()','fixture_tid()').replace('getpid()','fixture_pid()')
start=thread_boundaries(body(hooks,'void start_home_layout_hooks(const char *site, HookFunction hook, UnhookFunction unhook)')).replace('pthread_create(','fixture_create(')
primer=thread_boundaries(body(hooks,'void prime_home_layout_first_render_from_property(HookFunction hook,UnhookFunction unhook)'))
dispatch=thread_boundaries(body(native,'int hooked_system_property_get(const char *name, char *value)'))
tests=r'''
int main(){
    using home_layout_engine::StartHomeLayoutEngine;
    StartHomeLayoutEngine();assert(identity_reads==1&&threads==1);
    for(unsigned i=0;i<10000;++i)StartHomeLayoutEngine();
    assert(identity_reads==1&&threads==1);
    process_pid=101;StartHomeLayoutEngine();assert(identity_reads==2&&threads==2);
    // Failed monitor creation must not publish a permanently started worker.
    process_pid=102;create_result=1;StartHomeLayoutEngine();
    assert(identity_reads==3&&threads==3&&!home_layout_engine::g_layoutEngineStarted);
    create_result=0;StartHomeLayoutEngine();assert(identity_reads==4&&threads==4);
    for(unsigned i=0;i<10000;++i)StartHomeLayoutEngine();
    assert(identity_reads==4&&threads==4);
    // A failed layout worker creation retains its retry path and budget.
    g_started=false;g_attempts=0;threads=0;columns=back=0;
    create_result=1;start_home_layout_hooks("property",hook,nullptr);
    assert(threads==1&&columns==1&&back==1&&!g_started&&g_attempts==1);
    create_result=0;threads=0;columns=back=0;
    start_home_layout_hooks("property",hook,nullptr);assert(threads==1&&columns==1&&back==1);
    for(unsigned i=0;i<10000;++i)start_home_layout_hooks("property",hook,nullptr);
    assert(threads==1&&columns==1&&back==1);
    first_calls=0;for(unsigned i=0;i<10000;++i)prime_home_layout_first_render_from_property(hook,nullptr);
    assert(first_calls==0);
    start_home_layout_hooks("library",hook,nullptr);assert(columns==2&&back==2);
    g_started=false;g_attempts=kMaxWorkerAttempts;
    start_home_layout_hooks("property",hook,nullptr);prime_home_layout_first_render_from_property(hook,nullptr);
    assert(columns==2&&back==2&&first_calls==0);
    g_attempts=0;g_back_setup_complete=false;
    prime_home_layout_first_render_from_property(hook,nullptr);assert(first_calls==1);
    g_started=true;g_high_device_level=true;recursive=true;
    const auto before_motion=motion_calls;const auto before_threads=threads;
    char value[92]{};assert(hooked_system_property_get("ordinary.property",value)==3&&std::strcmp(value,"OEM")==0);
    assert(motion_calls==before_motion+1&&threads==before_threads);
    assert(hooked_system_property_get("ro.config.device_level_for_animation",value)==1&&std::strcmp(value,"2")==0);
    g_disable_prestart=true;
    assert(hooked_system_property_get("persist.sys.usap_pool_enabled",value)==5&&std::strcmp(value,"false")==0);
    recursive=false;g_high_device_level=false;g_disable_prestart=false;
    for(unsigned i=0;i<10000;++i)assert(hooked_system_property_get("ordinary.property",value)==3);
    assert(identity_reads==4&&threads==before_threads&&first_calls==1&&columns==2&&back==2);
    // The reporter's raster thread reached module file reads during Impeller
    // initialization. Warm-only tests miss the cold/failed-worker route.
    g_started=false;g_ready=false;g_dart_ready=false;g_back_setup_complete=false;g_attempts=0;
    home_layout_engine::g_layoutEngineStarted=false;
    process_pid=200;process_tid=201;
    const auto cold_motion=motion_calls,cold_reads=identity_reads,cold_threads=threads;
    const auto cold_first=first_calls,cold_columns=columns,cold_back=back,cold_prepares=prepares;
    g_high_device_level=true;g_disable_prestart=true;g_soft_glass=true;
    for(unsigned i=0;i<10000;++i)assert(hooked_system_property_get("ordinary.property",value)==3&&std::strcmp(value,"OEM")==0);
    assert(motion_calls==cold_motion&&identity_reads==cold_reads&&threads==cold_threads);
    assert(first_calls==cold_first&&columns==cold_columns&&back==cold_back&&prepares==cold_prepares);
    // Background queries still receive every existing scalar override.
    assert(hooked_system_property_get("ro.config.device_level_for_animation",value)==1&&std::strcmp(value,"2")==0);
    assert(hooked_system_property_get("persist.sys.usap_pool_enabled",value)==5&&std::strcmp(value,"false")==0);
    assert(hooked_system_property_get("persist.sys.background_blur_supported",value)==4&&std::strcmp(value,"true")==0);
    // Defend helper entry points too: an alternate caller cannot bypass dispatch.
    prime_home_layout_first_render_from_property(hook,nullptr);
    start_home_layout_hooks("property",hook,nullptr);
    assert(first_calls==cold_first&&columns==cold_columns&&back==cold_back&&threads==cold_threads&&prepares==cold_prepares);
    // Main-thread cold startup and first-frame binding remain functional; the
    // render thread neither consumes its attempt nor publishes fake readiness.
    assert(!g_started&&g_attempts==0&&!home_layout_engine::g_layoutEngineStarted);
    process_tid=0;
    assert(hooked_system_property_get("ordinary.property",value)==3);
    assert(first_calls==cold_first+1&&columns==cold_columns+1&&back==cold_back+1);
    assert(threads==cold_threads+2&&g_started&&home_layout_engine::g_layoutEngineStarted);
    std::cout<<"property shipping path PASS: 10000 warm and 10000 cold raster queries -> no discovery/I/O/start attempts on raster; scalar overrides, main-thread first frame, recursion, fork, failed worker creation and loader fallback retained\n";
}
'''
with tempfile.TemporaryDirectory(prefix='hc-property-path-')as temp:
    out=Path(temp);src=out/'test.cpp';exe=out/'test'
    src.write_text(prefix+engine_start+'\n}\n'+primer+'\n'+start+'\n'+dispatch+tests)
    subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread',str(src),'-o',str(exe)],check=True)
    subprocess.run([str(exe)],check=True)
# Direct launchers need the rename observer as well as spawners. No observer
# means the production identity cache deliberately refuses to cache names.
init=body(native,'NativeOnModuleLoaded native_init(const NativeApiEntries *entries)')
assert 'if (spawner || is_launcher_process()) install_process_name_hook();' in init
