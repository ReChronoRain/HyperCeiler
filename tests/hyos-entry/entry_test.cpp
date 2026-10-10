/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <dlfcn.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
static void *fixture_symbol(void *, const char *);
static int fixture_close(void *);
#define dlsym fixture_symbol
#define dlclose fixture_close
#include "../../app/src/main/cpp/targets/hyos/hyos_entry.cpp"
#undef dlsym
#undef dlclose
#include "../../app/src/main/cpp/targets/hyos/hyos_control.h"
#include "targets/hyos/hyos_snapshot.h"
extern "C" void hc_hyos_app_entry() {}

namespace test {
int starts=0,records=0,calls=0,hooks=0,unhooks=0,registrations=0;
int recents_prepares=0,closed=0;uintptr_t recents_base=0;
int lifetime_loads=0,lifetime_children=0;bool lifetime_weather=false;std::string lifetime_name;
int weather_starts=0,weather_snapshots=0;uintptr_t weather_started_base=0;
bool weather_card_pending=false;
int card_first=0,card_missed=0;uint32_t card_reason=0;uintptr_t card_dart=0,card_flutter=0;
void (*card_callback)()=nullptr;
int first_snapshots=0,missed=0; uint32_t missed_reason=0; uintptr_t snapshot_base=0;
void (*snapshot_callback)()=nullptr;
int registration_result=ZN_SUCCESS;
bool fail_plain=false,fail_ext=false,fail_sym=false,null_original=false,failed_load=false;
bool missing_plain=false,missing_ext=false,missing_sym=false,no_runtime=false,nested=false;
const ZygiskNextHyosModule *module=nullptr;
const void *last_caller=nullptr;
const android_dlextinfo *last_ext=nullptr;
const char *last_name=nullptr;
void *last_handle=nullptr;
int last_flags=0;
void *handle=reinterpret_cast<void *>(0x5678000);
std::vector<std::byte> runtime_bytes(0x65000);
auto entry_words=hyos_guard::kHomeEntryWords;
void *symbol_result=entry_words.data();
uintptr_t base(){return reinterpret_cast<uintptr_t>(runtime_bytes.data());}
const void *caller(){return reinterpret_cast<void *>(base()+hyos_guard::kDlsymReturnVa);}
void *plain(const char *name,int flags,const void *source){
 ++calls;last_name=name;last_flags=flags;last_caller=source;
 if(nested){nested=false;hooked_dlopen("libapp.so",17,source);}
 return failed_load?nullptr:handle;
}
void *extended(const char *name,int flags,const android_dlextinfo *ext,const void *source){
 last_ext=ext;return plain(name,flags,source);
}
void *symbol(void *h,const char *name,const void *source){
 ++calls;last_handle=h;last_name=name;last_caller=source;return symbol_result;
}
int hook(void *target,void *,void **original){
 ++hooks;
 if((target==reinterpret_cast<void *>(plain)&&fail_plain)
  ||(target==reinterpret_cast<void *>(extended)&&fail_ext)
  ||(target==reinterpret_cast<void *>(symbol)&&fail_sym))return ZN_FAILED;
 *original=null_original?nullptr:target;return ZN_SUCCESS;
}
int unhook(void *){++unhooks;return ZN_SUCCESS;}
int register_module(const void *value){++registrations;module=static_cast<const ZygiskNextHyosModule *>(value);return registration_result;}
ZygiskNextRuntime runtime{ZN_RUNTIME_HYOS,1,register_module};
const ZygiskNextRuntime *get_runtime(){return no_runtime?nullptr:&runtime;}
ZygiskNextAPI api{};
template<class T,size_t N>void put(size_t offset,const std::array<T,N>&bytes){std::memcpy(runtime_bytes.data()+offset,bytes.data(),sizeof(bytes));}
void fixture(){
 std::fill(runtime_bytes.begin(),runtime_bytes.end(),std::byte{0});
 put(hyos_guard::kRuntimeNoteVa,hyos_guard::kRuntimeBuildIdNote);
 put(hyos_guard::kDlsymWindowVa,hyos_guard::kDlsymWords);
 put(hyos_guard::kEntryWindowVa,hyos_guard::kEntryCallWords);
 entry_words=hyos_guard::kHomeEntryWords;symbol_result=entry_words.data();
}
void reset(){
 lifetime_loads=lifetime_children=0;lifetime_weather=false;lifetime_name.clear();
 starts=records=calls=hooks=unhooks=registrations=0;registration_result=ZN_SUCCESS;
 recents_prepares=closed=0;recents_base=0;g_home_recents_handle=nullptr;
 first_snapshots=missed=0;missed_reason=0;snapshot_base=0;snapshot_callback=nullptr;
 g_linker_base=0;g_startup_armed=false;g_snapshot_attempt=0;g_search_binding=0;
 g_search_flutter_base=0;g_search_handle=nullptr;g_search_tid=0;
 fail_plain=fail_ext=fail_sym=null_original=failed_load=false;
 missing_plain=missing_ext=missing_sym=no_runtime=nested=false;
 module=nullptr;last_caller=nullptr;last_ext=nullptr;last_name=nullptr;last_handle=nullptr;last_flags=0;
 runtime={ZN_RUNTIME_HYOS,1,register_module};api={};api.inlineHook=hook;api.inlineUnhook=unhook;api.getRuntime=get_runtime;
 g_api=nullptr;g_dlopen=nullptr;g_android_dlopen=nullptr;g_dlsym=nullptr;
 g_registered=false;g_home_child=false;g_weather_child=false;g_runtime_base=0;g_app_entry=nullptr;fixture();
 weather_starts=weather_snapshots=0;weather_started_base=0;
 weather_card_pending=false;card_first=card_missed=0;card_reason=0;card_dart=card_flutter=0;card_callback=nullptr;
}
void specialize(const char *package="com.miui.home",const char *process="com.miui.home",const char *seinfo="platform"){
 assert(module);ZnHyosAppSpecializeArgs args{process,package,seinfo};module->onAppSpecialized(&args);
}
void setup(){module_loaded(nullptr,&api);specialize();}
void *lookup(){return hooked_dlsym(handle,"app_entry_point",caller());}
void *wrapper(){return reinterpret_cast<void *>(hc_hyos_app_entry);}
int count=0;
template<class F>void run(const char *name,F fn){reset();std::fprintf(stderr,"RUN_CASE %s\n",name);fn();++count;std::printf("PASS %s\n",name);}
}
static void *fixture_symbol(void *,const char *name){
 if(std::strcmp(name,"__loader_dlopen")==0)return test::missing_plain?nullptr:reinterpret_cast<void *>(test::plain);
 if(std::strcmp(name,"__loader_android_dlopen_ext")==0)return test::missing_ext?nullptr:reinterpret_cast<void *>(test::extended);
 if(std::strcmp(name,"__loader_dlsym")==0)return test::missing_sym?nullptr:reinterpret_cast<void *>(test::symbol);
 assert(false&&"Only explicit-caller loader APIs may be hooked");return nullptr;
}
bool hyperceiler_hyos_start_home_control(const ZygiskNextAPI *api){assert(api==&test::api);++test::starts;return true;}
void hyperceiler_hyos_record_home_library(const char *name){assert(name);++test::records;}
static int fixture_close(void *handle){assert(handle==test::handle);++test::closed;return 0;}
namespace home_recents {
bool prepare_before_entry(uintptr_t base,ReadFresh read,PinImage pin) noexcept {
 assert(test::starts>0&&read==hyos_control::read_snapshot&&pin==pin_home_recents_image);
 ++test::recents_prepares;test::recents_base=base;return false;
}
Status status() noexcept {return {Phase::Rejected,Error::NotAuthorized,false,0};}
}

namespace weather_card_lifetime {
void observe_parent_load(const char *name) noexcept {++test::lifetime_loads;test::lifetime_name=name?name:"<null>";errno=EIO;}
void after_specialization(bool weather) noexcept {++test::lifetime_children;test::lifetime_weather=weather;assert(weather==g_weather_child.load());assert(test::starts==0&&test::weather_starts==0);}
}
namespace weather_runtime {
bool start_before_entry(uintptr_t base){++test::weather_starts;test::weather_started_base=base;return true;}
bool card_startup_pending() noexcept {return test::weather_card_pending;}
void on_first_card_snapshot(uintptr_t dart,uintptr_t flutter) noexcept {
 ++test::card_first;test::card_dart=dart;test::card_flutter=flutter;
 if(test::card_callback)test::card_callback();
}
void on_card_snapshot_window_missed(uint32_t reason) noexcept {
 ++test::card_missed;test::card_reason=reason;
}
void observe_snapshot(const char *symbol,void*){
 if(symbol&&std::strcmp(symbol,"_kDartVmSnapshotData")==0)++test::weather_snapshots;
}
}

struct WeatherImage {
 size_t size=(weather_guard::kWeatherImageSpan+4095)&~size_t(4095);
 void *memory=mmap(nullptr,size,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
 uintptr_t base=reinterpret_cast<uintptr_t>(memory);
 WeatherImage(){assert(memory!=MAP_FAILED);
  for(const auto &guard:weather_guard::kWeatherGuards)std::memcpy(reinterpret_cast<void*>(base+guard.va),guard.bytes.data(),guard.bytes.size());
  for(const auto &slot:weather_guard::kWeatherVtables)*reinterpret_cast<uintptr_t*>(base+slot.slot)=base+slot.target;
  test::symbol_result=reinterpret_cast<void*>(base+weather_guard::kWeatherEntryVa);
 }
 ~WeatherImage(){munmap(memory,size);}
};

namespace hyos_control {
bool read_snapshot(hyos_snapshot::Snapshot &) noexcept {return false;}
void on_first_home_snapshot(uintptr_t base) noexcept {
 ++test::first_snapshots;test::snapshot_base=base;
 if(test::snapshot_callback)test::snapshot_callback();
}
void on_home_snapshot_window_missed(uint32_t reason) noexcept {
 ++test::missed;test::missed_reason=reason;
}
}

int main(){using namespace test;
 run("Recents pin retains exact original namespace image without loading a new image",[]{
  setup();assert(lookup()==wrapper());auto entry=reinterpret_cast<uintptr_t>(symbol_result);
  assert(pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry));
  assert(g_home_recents_handle==handle&&last_flags==(RTLD_NOLOAD|RTLD_NOW));
  assert(last_caller==symbol_result&&std::strcmp(last_name,hyos_guard::kEntryName)==0&&closed==0);
  int before=calls;assert(pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry));assert(calls==before&&closed==0);
 });
 run("Recents pin refuses changed identities before a loader call",[]{
  setup();assert(lookup()==wrapper());auto entry=reinterpret_cast<uintptr_t>(symbol_result);int before=calls;
  assert(!pin_home_recents_image(entry-hyos_guard::kHomeEntryVa+4,entry));
  assert(!pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry+4));
  assert(!pin_home_recents_image(UINTPTR_MAX,entry));
  g_home_child=false;assert(!pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry));
  assert(calls==before&&!g_home_recents_handle&&closed==0);
 });
 run("Recents NOLOAD failure or wrong entry never retains the image",[]{
  setup();assert(lookup()==wrapper());auto entry=reinterpret_cast<uintptr_t>(symbol_result);
  failed_load=true;assert(!pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry));assert(closed==0);
  failed_load=false;symbol_result=nullptr;
  assert(!pin_home_recents_image(entry-hyos_guard::kHomeEntryVa,entry));assert(closed==1&&!g_home_recents_handle);
 });
 run("Recents prepare follows initial control and uses Rust target base",[]{
  setup();assert(lookup()==wrapper());auto entry=reinterpret_cast<uintptr_t>(symbol_result);
  assert(hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa)==symbol_result);
  assert(recents_prepares==1&&recents_base==entry-hyos_guard::kHomeEntryVa);
 });
 run("parent successful plain load observes exact returned path and preserves errno",[]{module_loaded(nullptr,&api);errno=EDOM;const char *path="/system_ext/lib64/libhyper_os_flutter.so";assert(hooked_dlopen(path,91,caller())==handle);assert(lifetime_loads==1&&lifetime_name==path&&errno==EDOM&&last_flags==91&&last_caller==caller()&&starts==0&&weather_starts==0);});
 run("parent successful extended load observes after return and preserves errno",[]{module_loaded(nullptr,&api);android_dlextinfo ext{0x198,29};errno=ERANGE;const char *path="/system_ext/lib64/libhyper_os_flutter.so";assert(hooked_android_dlopen(path,77,&ext,caller())==handle);assert(lifetime_loads==1&&lifetime_name==path&&errno==ERANGE&&last_ext==&ext&&last_flags==77&&last_caller==caller());});
 run("failed loader does not capture a lifetime image",[]{module_loaded(nullptr,&api);failed_load=true;errno=EACCES;assert(hooked_dlopen("/system_ext/lib64/libhyper_os_flutter.so",0,caller())==nullptr);assert(hooked_android_dlopen("/system_ext/lib64/libhyper_os_flutter.so",0,nullptr,caller())==nullptr);assert(lifetime_loads==0&&errno==EACCES);});
 run("specialization routes only exact Weather main to lifetime early window",[]{module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");assert(lifetime_children==1&&lifetime_weather&&starts==0&&weather_starts==0);});
 run("Home and other descendants close capture without Weather installation",[]{module_loaded(nullptr,&api);specialize();assert(lifetime_children==1&&!lifetime_weather);specialize("com.miui.weather2","com.miui.weather2:widget");assert(lifetime_children==2&&!lifetime_weather);module->onAppSpecialized(nullptr);assert(lifetime_children==3&&!lifetime_weather);});
 run("API4 registers Hyos1 after all three caller-preserving hooks",[]{module_loaded(nullptr,&api);assert(module&&module->target_api_version==1&&zn_module.target_api_version==4&&hooks==3);});
 run("null API inert",[]{module_loaded(nullptr,nullptr);assert(hooks==0);});
 run("missing runtime or hook API inert",[]{api.getRuntime=nullptr;module_loaded(nullptr,&api);api.getRuntime=get_runtime;api.inlineHook=nullptr;module_loaded(nullptr,&api);api.inlineHook=hook;api.inlineUnhook=nullptr;module_loaded(nullptr,&api);assert(hooks==0);});
 run("unknown null old runtime or missing registration inert",[]{runtime.type=static_cast<ZygiskNextRuntimeType>(99);module_loaded(nullptr,&api);runtime.type=ZN_RUNTIME_HYOS;no_runtime=true;module_loaded(nullptr,&api);no_runtime=false;runtime.api_version=0;module_loaded(nullptr,&api);runtime.api_version=1;runtime.registerModule=nullptr;module_loaded(nullptr,&api);assert(hooks==0);});
 run("missing any loader rejects registration",[]{missing_plain=true;module_loaded(nullptr,&api);assert(!g_registered&&registrations==0);missing_plain=false;missing_ext=true;module_loaded(nullptr,&api);assert(!g_registered);missing_ext=false;missing_sym=true;module_loaded(nullptr,&api);assert(!g_registered&&registrations==0);});
 run("failed hook leaves other paths passing through",[]{fail_sym=true;module_loaded(nullptr,&api);assert(!g_registered);assert(hooked_dlopen("libapp.so",7,caller())==handle);assert(records==0&&starts==0);});
 run("missing continuation requests rollback",[]{null_original=true;module_loaded(nullptr,&api);assert(unhooks==3&&registrations==0);});
 run("failed registration remains inert",[]{registration_result=ZN_FAILED;module_loaded(nullptr,&api);specialize();assert(lookup()==symbol_result&&starts==0);});
 run("spawner and other descendants cannot activate",[]{module_loaded(nullptr,&api);assert(lookup()==symbol_result);specialize("com.other","com.miui.home");assert(lookup()==symbol_result);specialize("com.miui.home","com.miui.home:widget");assert(lookup()==symbol_result);assert(starts==0);});
 run("null and incomplete specialization data inert",[]{module_loaded(nullptr,&api);module->onAppSpecialized(nullptr);specialize(nullptr);specialize("com.miui.home",nullptr);specialize("com.miui.home","com.miui.home","");assert(!g_home_child);});
 run("specialization only records identity",[]{setup();assert(starts==0&&records==0&&calls==0);});
 run("borrowed strings never retained",[]{module_loaded(nullptr,&api);char name[]="com.miui.home";specialize(name,name);std::memset(name,'x',sizeof(name)-1);assert(lookup()==wrapper());assert(starts==0);});
 run("successful lookup preserves original arguments and replaces only result",[]{setup();assert(lookup()==wrapper());assert(last_handle==handle&&last_caller==caller()&&std::strcmp(last_name,"app_entry_point")==0);assert(starts==0&&records==0);});
 run("null or other symbol and null result pass through",[]{setup();assert(hooked_dlsym(handle,nullptr,caller())==symbol_result);assert(hooked_dlsym(handle,"get_app_interface",caller())==symbol_result);symbol_result=nullptr;assert(lookup()==nullptr);assert(starts==0);});
 run("unknown caller and inaccessible body declined",[]{setup();assert(hooked_dlsym(handle,"app_entry_point",reinterpret_cast<void *>(4))==symbol_result);assert(hooked_dlsym(handle,"app_entry_point",reinterpret_cast<void *>(0x100000))==symbol_result);symbol_result=reinterpret_cast<void *>(0xa00000);assert(lookup()==symbol_result);});
 run("guarded call starts control only from exact post-loader return",[]{setup();assert(lookup()==wrapper());assert(hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa+4)==symbol_result);assert(starts==0);assert(hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa)==symbol_result);assert(starts==1&&records==1);});
 run("changed call-site after lookup cannot start control",[]{setup();assert(lookup()==wrapper());runtime_bytes[hyos_guard::kEntryWindowVa]^=std::byte{1};assert(hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa)==symbol_result&&starts==0);});
 run("same binding idempotent and different entry declined",[]{setup();assert(lookup()==wrapper()&&lookup()==wrapper());auto other=entry_words;symbol_result=other.data();assert(lookup()==symbol_result);assert(g_app_entry==entry_words.data());});
 run("different runtime mapping declined",[]{setup();assert(lookup()==wrapper());auto other=runtime_bytes;auto source=other.data()+hyos_guard::kDlsymReturnVa;assert(hooked_dlsym(handle,"app_entry_point",source)==symbol_result);});
 run("all home entry body bit mutations declined",[]{setup();size_t mutations=0;for(size_t i=0;i<entry_words.size();++i)for(unsigned bit=0;bit<32;++bit){entry_words[i]^=uint32_t{1}<<bit;assert(lookup()==symbol_result);entry_words[i]^=uint32_t{1}<<bit;++mutations;}assert(g_app_entry==nullptr);std::printf("  rejected %zu body bit mutations\n",mutations);});
 run("all runtime identity and call-site bit mutations declined",[]{setup();size_t mutations=0;auto mutate=[&](size_t at,size_t n){for(size_t i=at;i<at+n;++i)for(unsigned bit=0;bit<8;++bit){runtime_bytes[i]^=std::byte{static_cast<unsigned char>(1u<<bit)};assert(lookup()==symbol_result);runtime_bytes[i]^=std::byte{static_cast<unsigned char>(1u<<bit)};++mutations;}};mutate(hyos_guard::kRuntimeNoteVa,sizeof(hyos_guard::kRuntimeBuildIdNote));mutate(hyos_guard::kDlsymWindowVa,sizeof(hyos_guard::kDlsymWords));mutate(hyos_guard::kEntryWindowVa,sizeof(hyos_guard::kEntryCallWords));assert(g_app_entry==nullptr);std::printf("  rejected %zu caller bit mutations\n",mutations);});
 run("loader callbacks record only and preserve namespace ext flags",[]{setup();android_dlextinfo ext{0x1234,27};assert(hooked_android_dlopen("libapp.so",123,&ext,caller())==handle);assert(last_flags==123&&last_caller==caller()&&last_ext==&ext);assert(records==1&&starts==0);hooked_dlopen("/a.apk!/lib/arm64-v8a/libapp_launcher.so",1,caller());assert(records==2&&starts==0);});
 run("bad basename null path and failed load never recorded",[]{setup();hooked_dlopen(nullptr,0,caller());hooked_dlopen("otherlibapp.so",0,caller());hooked_dlopen("libapp.so.bak",0,caller());failed_load=true;hooked_dlopen("libapp.so",0,caller());assert(records==0&&starts==0);});
 run("nested constructors cannot start control",[]{setup();nested=true;hooked_dlopen("libapp_launcher.so",0,caller());assert(records==2&&calls==2&&starts==0);});
 run("real fork leaves spawner binding and activity unchanged",[]{module_loaded(nullptr,&api);pid_t pid=fork();assert(pid>=0);if(pid==0){specialize();assert(lookup()==wrapper());hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa);_exit(starts==1?0:17);}int status=0;assert(waitpid(pid,&status,0)==pid&&WIFEXITED(status)&&WEXITSTATUS(status)==0);assert(!g_home_child&&g_app_entry==nullptr&&starts==0);assert(lookup()==symbol_result);});
 run("control wire accepts only canonical flag combinations",[]{assert(!hyos_control::valid_reply(0,1,1));assert(!hyos_control::valid_reply(hyos_control::kAck,0,0));for(int f=-100;f<256;++f){bool e=f==0||(f>=1&&f<=63&&(f&1)&&(!(f&8)||(f&16)));assert(hyos_control::valid_reply(hyos_control::kAck,1,f)==e);}});
 run("Weather exact main process only records identity until verified entry",[]{
  WeatherImage image;module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");
  assert(g_weather_child&&!g_home_child&&weather_starts==0&&starts==0);
  assert(lookup()==wrapper());assert(weather_starts==0&&records==0);
  assert(hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa)==symbol_result);
  assert(weather_starts==1&&weather_started_base==image.base&&starts==0&&records==0&&!g_startup_armed);
 });
 run("Weather secondary mismatched null or unlabelled process cannot activate",[]{
  WeatherImage image;module_loaded(nullptr,&api);
  for(const char *process:{"com.miui.weather2:widget","com.miui.weather20","com.miui.home"}){
   specialize("com.miui.weather2",process);assert(lookup()==symbol_result&&!g_weather_child);}
  specialize("com.other","com.miui.weather2");assert(lookup()==symbol_result);
  specialize("com.miui.weather2","com.miui.weather2",nullptr);assert(lookup()==symbol_result);
  specialize("com.miui.weather2","com.miui.weather2","");assert(lookup()==symbol_result&&weather_starts==0);
 });
 run("Weather cannot accept Home image or unverified runtime caller",[]{
  module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");assert(lookup()==symbol_result);
  WeatherImage image;assert(hooked_dlsym(handle,"app_entry_point",reinterpret_cast<void*>(4))==symbol_result);
  runtime_bytes[hyos_guard::kRuntimeNoteVa]^=std::byte{1};assert(lookup()==symbol_result&&weather_starts==0);
 });
 run("Home cannot accept Weather image",[]{WeatherImage image;setup();assert(lookup()==symbol_result&&starts==0&&weather_starts==0);});
 run("Weather code metadata and dispatch mutations all refuse wrapper",[]{
  WeatherImage image;module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");
  for(const auto &guard:weather_guard::kWeatherGuards){
   auto *bytes=reinterpret_cast<unsigned char*>(image.base+guard.va);
   for(size_t at:{size_t(0),guard.bytes.size()/2,guard.bytes.size()-1}){bytes[at]^=1;assert(lookup()==symbol_result);bytes[at]^=1;}}
  for(const auto &slot:weather_guard::kWeatherVtables){auto *value=reinterpret_cast<uintptr_t*>(image.base+slot.slot);*value^=4;assert(lookup()==symbol_result);*value^=4;}
  assert(!g_app_entry&&weather_starts==0);
 });
 run("Weather loader observations never start Home controllers",[]{
  WeatherImage image;module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");
  hooked_dlopen("libapp.so",0,caller());hooked_dlopen("libapp_launcher.so",0,caller());
  hooked_dlsym(handle,"_kDartVmSnapshotData",caller());
  assert(weather_snapshots==1&&starts==0&&records==0&&first_snapshots==0&&missed==0&&weather_starts==0);
 });
 run("Weather entry requires exact original return caller",[]{
  WeatherImage image;module_loaded(nullptr,&api);specialize("com.miui.weather2","com.miui.weather2");assert(lookup()==wrapper());
  hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa+4);assert(weather_starts==0);
  runtime_bytes[hyos_guard::kEntryWindowVa]^=std::byte{1};hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa);
  assert(weather_starts==0&&starts==0);
 });
 run("real Weather fork does not alter spawner Home or Weather state",[]{
  WeatherImage image;module_loaded(nullptr,&api);pid_t pid=fork();assert(pid>=0);
  if(pid==0){specialize("com.miui.weather2","com.miui.weather2");assert(lookup()==wrapper());
   hc_hyos_prepare_app_entry(base()+hyos_guard::kEntryReturnVa);_exit(weather_starts==1&&starts==0?0:19);}
  int status;assert(waitpid(pid,&status,0)==pid&&WIFEXITED(status)&&WEXITSTATUS(status)==0);
  assert(!g_weather_child&&!g_home_child&&!g_app_entry&&weather_starts==0&&starts==0);
 });
 std::printf("%d/%d PASS; production entry with fixture loaders and real fork; no device injection proof\n",count,count);
 return 0;
}
