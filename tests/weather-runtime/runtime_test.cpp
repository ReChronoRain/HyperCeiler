// Actual production runtime included below; only external bindings and scheduling are adapted.
#include "targets/weather/weather_binding.h"
#include "targets/weather/weather_material_binding.h"
#include "targets/weather/weather_material_runtime.h"
#include "nativehook/got_hook_backend.h"
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <dlfcn.h>
#include <functional>
#include <iostream>
#include <limits>
#include <mutex>
#include <pthread.h>
#include <span>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <thread>
#include <vector>
#include <stdexcept>

#include "card_adapter.h"
#include "lifetime_adapter.h"

#define CHECK(x) do { if (!(x)) throw std::runtime_error(#x); } while(false)
extern "C" void *site_call(const void *,int32_t,void *);
extern "C" char site_return;
asm(".text\n.global site_call\n.type site_call,@function\nsite_call:\nsub $8,%rsp\ncall *%rdx\n.global site_return\nsite_return:\nadd $8,%rsp\nret\n.size site_call,.-site_call\n");
namespace fx {
using Words=weather_snapshot::Words;
std::atomic<int64_t> time_ns{10'000'000'000LL};
Words reply{};
std::atomic<int> transacts{0}, prepares{0}, deletes{0}, releases{0}, reads{0}, threads{0}, resolve_calls{0}, verify_dart_calls{0};
std::atomic_bool release_binder{false}, blocked{false};
std::mutex mu;std::condition_variable cv;
int parcel_bytes=48,fail_read=-1,transact_status=0,prepare_status=0,create_status=0;
bool service_present=true,class_null=false,descriptor_null=false,association=true,define_present=true,output_null=false,readable_weather=true,readable_dart=true,resolve_ok=true,clock_error=false;
std::string descriptor="android.view.IWindowManager";
bool block_binder=false;int64_t query_duration=0;
int mprotect_calls=0,fail_mprotect_at=0;bool fail_restores=false,foreign_on_write=false,ready_on_write=false,ready_seen=false;
std::function<void()> write_event,dart_event,alloc_event;
uintptr_t *slot=nullptr;uintptr_t weather_base=0;uintptr_t dart_base=0x800000;
uint8_t original_bytes[3]={3,1,2},allocated_bytes[3]={7,7,7};
int original_calls=0,alloc_calls=0,drop_calls=0,shim_calls=0;int32_t selector_seen=0;const void *runtime_seen=nullptr;
bool original_null=false,allocate_null=false;
bool material_weather=false,material_dart=false,material_install_ok=true,material_residue=false;
int material_installs=0;
weather_material_runtime::ReadFresh material_read=nullptr;
weather_material_runtime::DartBase material_base=nullptr;
std::function<void()> material_install_event;
void *original(const void *r,int32_t s){++original_calls;runtime_seen=r;selector_seen=s;return original_null?nullptr:original_bytes;}
void *allocate(size_t n,size_t a){CHECK(n==3&&a==1);++alloc_calls;if(alloc_event)alloc_event();return allocate_null?nullptr:allocated_bytes;}
void shim(){++shim_calls;}
void drop(void *p){CHECK(p==allocated_bytes);++drop_calls;}
void put_long(Words &v,int at,int64_t n){v[at]=int32_t(uint64_t(n));v[at+1]=int32_t(uint64_t(n)>>32);}
Words active(int level=2,int64_t revision=1){Words w{};w[0]=weather_snapshot::kAck;w[1]=1;w[2]=1;put_long(w,3,7);put_long(w,5,revision);put_long(w,7,time_ns+4'000'000'000LL);w[9]=level;return w;}
void setup(){reply=active();slot=(uintptr_t*)mmap(nullptr,4096,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);CHECK(slot!=MAP_FAILED);*slot=(uintptr_t)original;CHECK(mprotect(slot,4096,PROT_READ)==0);weather_base=(uintptr_t)&site_return-0x5fc76c;}
}
namespace test_binding {
using Binding=weather_binding::Binding;
template<class Read>bool verify_weather(uintptr_t b,Read&&){return fx::readable_weather&&b==fx::weather_base;}
template<class Read>bool verify_dart(uintptr_t b,Read&&){++fx::verify_dart_calls;if(fx::dart_event)fx::dart_event();return fx::readable_dart&&b==fx::dart_base;}
template<class Read>bool resolve(uintptr_t b,Read&&read,Binding &out){
 ++fx::resolve_calls;if(!fx::resolve_ok||!verify_weather(b,read))return false;uintptr_t current=0;
 if(!read((uintptr_t)fx::slot,std::as_writable_bytes(std::span(&current,1)))||current!=(uintptr_t)fx::original)return false;
 Binding result;result.weather_base=b;result.public_base=0x100000;result.standard_base=0x200000;result.slot=(uintptr_t)fx::slot;result.api={fx::original,fx::allocate,fx::shim,fx::drop};out=result;return true;
}
}
namespace test_material_binding {
template<class Read>bool verify_weather(uintptr_t b,Read&&){return fx::material_weather&&b==fx::weather_base;}
template<class Read>bool verify_dart(uintptr_t b,Read&&){return fx::material_dart&&b==fx::dart_base;}
}
namespace test_material_runtime {
bool start_before_entry(uintptr_t b,weather_material_runtime::ReadFresh read,weather_material_runtime::DartBase base){
 CHECK(b==fx::weather_base);++fx::material_installs;fx::material_read=read;fx::material_base=base;
 weather_snapshot::Snapshot s;CHECK(!read(s)&&base()==0);
 if(fx::material_install_event)fx::material_install_event();
 return fx::material_install_ok;
}
bool has_residue(){return fx::material_residue;}
}
int fixture_clock_gettime(clockid_t id,timespec *t){if(id!=CLOCK_BOOTTIME)return ::clock_gettime(id,t);if(fx::clock_error)return -1;const auto n=fx::time_ns.load();t->tv_sec=n/1'000'000'000LL;t->tv_nsec=n%1'000'000'000LL;return 0;}
int fixture_nanosleep(const timespec*,timespec*){std::unique_lock l(fx::mu);fx::cv.wait(l,[]{return false;});return 0;}
int fixture_pthread_create(pthread_t*t,const pthread_attr_t*a,void*(*f)(void*),void*x){++fx::threads;if(fx::create_status)return fx::create_status;return ::pthread_create(t,a,f,x);}
AIBinder *service(const char *name){CHECK(std::string(name)=="window");static AIBinder_Class c;static AIBinder b; c.descriptor=fx::descriptor_null?nullptr:fx::descriptor.c_str();b.clazz=fx::class_null?nullptr:&c;return fx::service_present?&b:nullptr;}
void *fixture_dlsym(void*,const char *n){CHECK(std::string(n)=="AServiceManager_checkService");return (void*)service;}
int fixture_mprotect(void*,size_t,int);
#define weather_binding test_binding
#define weather_material_binding test_material_binding
#define weather_material_runtime test_material_runtime
#define weather_card_runtime test_card_runtime
#define weather_card_lifetime test_card_lifetime
#define clock_gettime fixture_clock_gettime
#define nanosleep fixture_nanosleep
#define pthread_create fixture_pthread_create
#define dlsym fixture_dlsym
#define mprotect fixture_mprotect
#ifndef HC_RUNTIME_SOURCE
#define HC_RUNTIME_SOURCE "targets/weather/weather_runtime.cpp"
#endif
#include HC_RUNTIME_SOURCE
#undef mprotect
#undef dlsym
#undef pthread_create
#undef nanosleep
#undef clock_gettime
#undef weather_card_runtime
#undef weather_card_lifetime
#undef weather_binding
#undef weather_material_binding
#undef weather_material_runtime

int fixture_mprotect(void *p,size_t n,int prot){
 ++fx::mprotect_calls;if(fx::fail_mprotect_at==fx::mprotect_calls||(fx::fail_restores&&prot==PROT_READ)){errno=EPERM;return -1;}
 const auto rc=::mprotect(p,n,prot);
 if(rc==0&&fx::mprotect_calls==1&&(prot&PROT_WRITE)){
  fx::ready_seen=weather_runtime::g_binding_ready.load()&&weather_runtime::g_binding.api.get==fx::original;
  if(fx::foreign_on_write)__atomic_store_n(fx::slot,(uintptr_t)0x12345678,__ATOMIC_RELEASE);
  if(fx::write_event)fx::write_event();
 }
 return rc;
}
const AIBinder_Class *AIBinder_getClass(AIBinder*b){return b->clazz;}
const char *AIBinder_Class_getDescriptor(const AIBinder_Class*c){return c->descriptor;}
const AIBinder_Class *AIBinder_Class_define(const char*d,void*(*)(void*),void(*)(void*),binder_status_t(*)(AIBinder*,transaction_code_t,const AParcel*,AParcel*)){static AIBinder_Class c;c.descriptor=d;return fx::define_present?&c:nullptr;}
bool AIBinder_associateClass(AIBinder*,const AIBinder_Class*c){CHECK(std::string(c->descriptor)=="android.view.IWindowManager");return fx::association;}
binder_status_t AIBinder_prepareTransaction(AIBinder*,AParcel**p){++fx::prepares;if(fx::prepare_status)return fx::prepare_status;*p=new AParcel;return 0;}
binder_status_t AIBinder_transact(AIBinder*,transaction_code_t c,AParcel**in,AParcel**out,uint32_t flags){
 CHECK(c==weather_snapshot::kTransaction&&flags==0);++fx::transacts;
 delete *in;*in=nullptr; // Binder consumes the prepared input parcel.
 if(fx::block_binder){fx::blocked=true;fx::cv.notify_all();std::unique_lock l(fx::mu);fx::cv.wait(l,[]{return fx::release_binder.load();});}
 fx::time_ns+=fx::query_duration;
 if(!fx::output_null){*out=new AParcel;auto*p=*out;p->bytes=fx::parcel_bytes;p->fail_index=fx::fail_read;std::copy(fx::reply.begin(),fx::reply.end(),p->values);}
 return fx::transact_status;
}
void AIBinder_decStrong(AIBinder*){++fx::releases;}
int32_t AParcel_getDataSize(const AParcel*p){return p->bytes;}
binder_status_t AParcel_readInt32(const AParcel*raw,int32_t*v){auto*p=const_cast<AParcel*>(raw);++fx::reads;if(p->pos==p->fail_index)return -1;*v=p->values[p->pos++];return 0;}
void AParcel_delete(AParcel*p){++fx::deletes;delete p;}
namespace {
using namespace weather_runtime;
void verify(){observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart_base+weather_guard::kDartVmDataVa));CHECK(g_dart_base==fx::dart_base&&!g_dart_rejected);}
void installed(){CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Installed);CHECK(*fx::slot==(uintptr_t)hooked_device_level);CHECK(fx::ready_seen);CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);}
void *call(int32_t s=1){return site_call((void*)0x34567,s,(void*)*fx::slot);}
void wait_for(std::function<bool()>p){for(int i=0;i<200&&!p();++i)std::this_thread::sleep_for(std::chrono::milliseconds(5));CHECK(p());}
void neutral(){weather_snapshot::Snapshot s;CHECK(!g_cache.read(fx::time_ns,s)||!s.available||!s.allowed);}
struct Case{std::string name;std::function<void()> run;};
}
int main(){std::vector<Case> cases;
 auto add=[&](std::string n,std::function<void()>f){cases.push_back({std::move(n),std::move(f)});};
 add("query exact48 transaction and flags0",[]{weather_snapshot::Words w;CHECK(query(w)&&w==fx::reply);CHECK(fx::reads==12&&fx::releases==1&&fx::deletes==1);});
 for(int size:{0,44,49,52})add("query rejects size "+std::to_string(size),[=]{fx::parcel_bytes=size;weather_snapshot::Words w;CHECK(!query(w));CHECK(fx::reads==0&&fx::deletes==1&&fx::releases==1);});
 for(int index:{0,5,11})add("query read failure "+std::to_string(index),[=]{fx::fail_read=index;weather_snapshot::Words w;CHECK(!query(w));CHECK(fx::deletes==1&&fx::releases==1);});
 add("wrong binder descriptor",[]{fx::descriptor="wrong";weather_snapshot::Words w;CHECK(!query(w)&&fx::transacts==0&&fx::releases==1);});
 add("null descriptor",[]{fx::descriptor_null=true;weather_snapshot::Words w;CHECK(!query(w)&&fx::transacts==0);});
 add("missing service",[]{fx::service_present=false;weather_snapshot::Words w;CHECK(!query(w)&&fx::prepares==0);});
 add("associate absent binder class",[]{fx::class_null=true;weather_snapshot::Words w;CHECK(query(w)&&fx::transacts==1);});
 add("associate failed",[]{fx::class_null=true;fx::association=false;weather_snapshot::Words w;CHECK(!query(w)&&fx::transacts==0);});
 add("class definition failed",[]{fx::class_null=true;fx::define_present=false;weather_snapshot::Words w;CHECK(!query(w)&&fx::transacts==0);});
 add("prepare failed released binder",[]{fx::prepare_status=-1;weather_snapshot::Words w;CHECK(!query(w)&&fx::transacts==0&&fx::releases==1);});
 add("transact failed output deleted",[]{fx::transact_status=-1;weather_snapshot::Words w;CHECK(!query(w)&&fx::deletes==1);});
 add("null output",[]{fx::output_null=true;weather_snapshot::Words w;CHECK(!query(w)&&fx::deletes==0);});
 add("poll late reply cannot renew",[]{fx::query_duration=6'000'000'000LL;fx::put_long(fx::reply,7,fx::time_ns+9'000'000'000LL);poll();neutral();});
 add("poll BOOTTIME rollback rejects",[]{fx::query_duration=-1;poll();neutral();});
 add("poll clock unavailable rejects",[]{fx::clock_error=true;poll();neutral();});
 add("poll stopped rejects valid reply",[]{g_stop=true;poll();neutral();});
 add("malformed denied payload rejected",[]{fx::reply[2]=0;poll();neutral();});
 add("canonical denied remains inactive",[]{fx::reply=weather_snapshot::unavailable_words();fx::reply[1]=1;fx::put_long(fx::reply,3,7);fx::put_long(fx::reply,5,1);CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Inactive&&g_stop);CHECK(*fx::slot==(uintptr_t)fx::original);});
 add("default cannot late install",[]{fx::reply=fx::active(3);CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Inactive&&g_stop);g_cache.publish(fx::active(0,2),fx::time_ns);CHECK(start_before_entry(fx::weather_base));CHECK(*fx::slot==(uintptr_t)fx::original&&fx::threads==1);});
 add("unavailable startup missed",[]{fx::reply=weather_snapshot::unavailable_words();CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Missed&&g_stop);});
 add("invalid Weather no worker",[]{fx::readable_weather=false;CHECK(!start_before_entry(fx::weather_base)&&fx::threads==0);});
 add("thread failure no write",[]{fx::create_status=EAGAIN;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Rejected&&*fx::slot==(uintptr_t)fx::original);});
 add("one worker under parallel entry",[]{std::vector<std::thread> ts;for(int i=0;i<8;++i)ts.emplace_back([]{start_before_entry(fx::weather_base);});for(auto&t:ts)t.join();CHECK(fx::threads==1&&g_phase==Phase::Installed);});
 add("blocked Binder bounded startup late result terminal",[]{fx::block_binder=true;auto start=std::chrono::steady_clock::now();CHECK(!start_before_entry(fx::weather_base));auto elapsed=std::chrono::steady_clock::now()-start;CHECK(elapsed<std::chrono::milliseconds(1500)&&elapsed>=std::chrono::milliseconds(900));CHECK(g_phase==Phase::Missed&&fx::blocked&&g_stop);fx::release_binder=true;fx::cv.notify_all();wait_for([]{std::lock_guard lock(g_initial_mutex);return g_initial_completed;});CHECK(!start_before_entry(fx::weather_base));neutral();CHECK(fx::threads==1&&*fx::slot==(uintptr_t)fx::original);});
 for(int level:{0,1,2})add("owned full policy level "+std::to_string(level),[=]{fx::reply=fx::active(level);installed();verify();CHECK(call(INT32_MIN)==fx::original_bytes);CHECK(fx::original_calls==1&&fx::selector_seen==INT32_MIN&&fx::runtime_seen==(void*)0x34567);for(auto v:fx::original_bytes)CHECK(v==level+1);CHECK(g_applied==1);});
 for(int level:{0,1,2})add("null same allocator full policy "+std::to_string(level),[=]{fx::reply=fx::active(level);fx::original_null=true;installed();verify();CHECK(call()==fx::allocated_bytes);CHECK(fx::original_calls==1&&fx::shim_calls==1&&fx::alloc_calls==1&&fx::drop_calls==0);for(auto v:fx::allocated_bytes)CHECK(v==level+1);});
 add("allocation failure originalnull remains",[]{fx::original_null=true;fx::allocate_null=true;installed();verify();CHECK(call()==nullptr&&g_allocation_failed==1&&fx::original_calls==1&&fx::drop_calls==0);});
 add("allocation authorization epoch changed cleanup",[]{fx::original_null=true;installed();verify();fx::alloc_event=[]{auto w=fx::active(1,2);g_cache.publish(w,fx::time_ns);};CHECK(call()==nullptr&&fx::drop_calls==1&&g_applied==0);});
 add("binding available before atomic write",[]{installed();CHECK(fx::ready_seen&&g_binding_ready&&g_binding.api.get==fx::original);});
 add("foreign caller pass through once",[]{installed();verify();CHECK(hooked_device_level((void*)4,37)==fx::original_bytes);CHECK(fx::original_calls==1&&fx::selector_seen==37&&g_applied==0);});
 add("missing Dart pass through once",[]{installed();CHECK(call()==fx::original_bytes&&fx::original_calls==1&&g_applied==0);});
 add("expired lease pass through once",[]{installed();verify();fx::time_ns+=5'000'000'000LL;CHECK(call()==fx::original_bytes&&fx::original_calls==1&&g_applied==0);});
 add("revoked fresh publication pass through",[]{installed();verify();g_cache.revoke();CHECK(call()==fx::original_bytes&&g_applied==0);});
 add("Dart live guard changed pass through",[]{installed();verify();fx::readable_dart=false;CHECK(call()==fx::original_bytes&&g_applied==0);});
 add("public import identity changed pass through",[]{installed();verify();fx::resolve_ok=false;CHECK(call()==fx::original_bytes&&g_applied==0&&fx::original_calls==1);});
 add("snapshot before entry permanently rejects",[]{observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart_base+weather_guard::kDartVmDataVa));CHECK(g_dart_rejected);CHECK(!start_before_entry(fx::weather_base)&&fx::threads==0);});
 for(const std::string name:{"_kDartVmSnapshotInstructions","_kDartIsolateSnapshotData","_kDartIsolateSnapshotInstructions","kDartVmSnapshotData","kDartVmSnapshotInstructions","kDartIsolateSnapshotData","kDartIsolateSnapshotInstructions"})add("wrong first successful snapshot "+name,[=]{installed();observe_snapshot(name.c_str(),(void*)0x800340);CHECK(g_dart_rejected);observe_snapshot("_kDartVmSnapshotData",(void*)0x800340);CHECK(call()==fx::original_bytes&&g_applied==0);});
 add("failed nonunderscore then expected fallback",[]{installed();observe_snapshot("kDartVmSnapshotData",nullptr);CHECK(!g_dart_rejected);verify();CHECK(call()==fx::original_bytes&&g_applied==1);});
 add("other symbol ignored",[]{installed();observe_snapshot("other",(void*)1);CHECK(!g_dart_rejected&&g_dart_base==0);});
 add("snapshot result underflow rejects",[]{installed();observe_snapshot("_kDartVmSnapshotData",(void*)1);CHECK(g_dart_rejected);});
 add("snapshot exact image invalid rejects",[]{installed();fx::readable_dart=false;observe_snapshot("_kDartVmSnapshotData",(void*)0x800340);CHECK(g_dart_rejected);});
 for(const auto &[name,offset]:std::vector<std::pair<std::string,uintptr_t>>{{"_kDartVmSnapshotInstructions",0x2d0000},{"_kDartIsolateSnapshotData",0x4ac0},{"_kDartIsolateSnapshotInstructions",0x2e6940}}){
  add("same image following snapshot "+name,[=]{installed();verify();observe_snapshot(name.c_str(),(void*)(fx::dart_base+offset));observe_snapshot(name.c_str(),(void*)(fx::dart_base+offset));CHECK(!g_dart_rejected);CHECK(call()==fx::original_bytes&&g_applied==1);});
  add("overflow following snapshot "+name,[=]{installed();verify();g_dart_base=UINTPTR_MAX-offset+2;observe_snapshot(name.c_str(),(void*)1);CHECK(g_dart_rejected);CHECK(call()==fx::original_bytes&&g_applied==0);});
  add("different image following snapshot "+name,[=]{installed();verify();observe_snapshot(name.c_str(),(void*)(fx::dart_base+offset+0x100000));CHECK(g_dart_rejected);CHECK(call()==fx::original_bytes&&g_applied==0&&fx::original_calls==1);});
 }
 add("null optional exports without VMData never authorize",[]{installed();for(const char*n:{"_kDartVmSnapshotInstructions","_kDartIsolateSnapshotData","_kDartIsolateSnapshotInstructions"})observe_snapshot(n,nullptr);CHECK(!g_dart_rejected&&g_dart_base==0);CHECK(call()==fx::original_bytes&&g_applied==0&&fx::original_calls==1);});
 add("null lookups do not contradict already verified image",[]{installed();verify();for(const char*n:{"_kDartVmSnapshotInstructions","_kDartIsolateSnapshotData","_kDartIsolateSnapshotInstructions"})observe_snapshot(n,nullptr);CHECK(!g_dart_rejected);CHECK(call()==fx::original_bytes&&g_applied==1);});
 add("null symbol ignored",[]{installed();observe_snapshot(nullptr,(void*)1);CHECK(!g_dart_rejected&&g_dart_base==0);});
 add("same Dart base repeated accepted",[]{installed();verify();verify();CHECK(!g_dart_rejected);});
 add("new Dart base denied",[]{installed();verify();fx::dart_base=0x900000;observe_snapshot("_kDartVmSnapshotData",(void*)0x900340);CHECK(g_dart_rejected);});
 add("reject while verifying cannot reopen",[]{installed();fx::dart_event=[]{g_dart_rejected=true;};observe_snapshot("_kDartVmSnapshotData",(void*)0x800340);CHECK(g_dart_rejected);CHECK(call()==fx::original_bytes&&g_applied==0);});
 add("snapshot during install rollback",[]{fx::write_event=[]{observe_snapshot("_kDartVmSnapshotData",(void*)0x800340);};CHECK(!start_before_entry(fx::weather_base));CHECK(g_dart_rejected&&*fx::slot==(uintptr_t)fx::original&&g_phase==Phase::Rejected);});
 add("CAS foreign owner retained",[]{fx::foreign_on_write=true;CHECK(!start_before_entry(fx::weather_base));CHECK(*fx::slot==0x12345678&&g_phase==Phase::Residue);CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);});
 add("first writable permission denied",[]{fx::fail_mprotect_at=1;CHECK(!start_before_entry(fx::weather_base));CHECK(*fx::slot==(uintptr_t)fx::original&&g_phase==Phase::Rejected);});
 add("first restore failure rollback clean",[]{fx::fail_mprotect_at=2;CHECK(!start_before_entry(fx::weather_base));CHECK(*fx::slot==(uintptr_t)fx::original&&g_phase==Phase::Rejected&&g_binding_ready);CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);});
 add("all restore failures reported residue",[]{fx::fail_restores=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&g_binding_ready&&g_stop);CHECK(*fx::slot==(uintptr_t)fx::original);CHECK(nhk::read_protection((uintptr_t)fx::slot)==(PROT_READ|PROT_WRITE));CHECK(hooked_device_level(nullptr,91)==fx::original_bytes&&fx::original_calls==1);});
 add("postwrite binding mismatch rollback",[]{fx::write_event=[]{fx::resolve_ok=false;};CHECK(!start_before_entry(fx::weather_base));CHECK(*fx::slot==(uintptr_t)fx::original&&g_phase==Phase::Rejected);});
 add("blur only leaves device level import untouched",[]{fx::reply=fx::active(3);fx::reply[11]=1;CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Installed&&!g_stop&&!g_level_installed);CHECK(fx::material_installs==1&&fx::resolve_calls==0&&fx::mprotect_calls==0&&fx::threads==1);CHECK(*fx::slot==(uintptr_t)fx::original);verify();weather_snapshot::Snapshot s;CHECK(fx::material_read(s)&&s.unlock_blur&&s.device_level==3&&fx::material_base()==fx::dart_base);});
 add("both features share single worker",[]{fx::reply[11]=1;installed();verify();CHECK(fx::material_installs==1&&fx::threads==1&&g_level_installed);CHECK(call()==fx::original_bytes&&g_applied==1);weather_snapshot::Snapshot s;CHECK(fx::material_read(s)&&s.unlock_blur);});
 add("material image can authorize independent startup",[]{fx::reply=fx::active(3);fx::reply[11]=1;fx::readable_weather=false;fx::material_weather=true;CHECK(start_before_entry(fx::weather_base));CHECK(fx::threads==1&&fx::material_installs==1&&fx::resolve_calls==0);});
 add("material Dart image independent from device level guards",[]{fx::reply=fx::active(3);fx::reply[11]=1;CHECK(start_before_entry(fx::weather_base));fx::readable_dart=false;fx::material_dart=true;verify();CHECK(fx::material_base()==fx::dart_base);});
 add("level binding rejection still allows independently resolved material",[]{fx::reply[11]=1;fx::resolve_ok=false;CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Installed&&!g_level_installed&&fx::material_installs==1&&!g_stop);CHECK(*fx::slot==(uintptr_t)fx::original);});
 add("material rejection still allows device level",[]{fx::reply[11]=1;fx::material_install_ok=false;installed();verify();CHECK(fx::material_installs==1&&g_level_installed);CHECK(call()==fx::original_bytes&&g_applied==1);});
 add("both bindings rejected stop worker",[]{fx::reply[11]=1;fx::resolve_ok=false;fx::material_install_ok=false;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Rejected&&g_stop&&!g_level_installed&&fx::material_installs==1);});
 add("blur only rejected stop worker",[]{fx::reply=fx::active(3);fx::reply[11]=1;fx::material_install_ok=false;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Rejected&&g_stop&&fx::threads==1);});
 add("level residue closes material startup batch",[]{fx::reply[11]=1;fx::fail_restores=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&g_stop&&fx::material_installs==0&&!g_level_installed);});
 add("material residue closes installed level batch",[]{fx::reply[11]=1;fx::material_install_ok=false;fx::material_residue=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&g_stop&&!g_level_installed);CHECK(*fx::slot==(uintptr_t)hooked_device_level);CHECK(call()==fx::original_bytes&&fx::original_calls==1&&g_applied==0);});
 add("early Dart inside material install cannot reopen",[]{fx::reply[11]=1;fx::material_install_event=[]{observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart_base+weather_guard::kDartVmDataVa));};CHECK(!start_before_entry(fx::weather_base));CHECK(g_dart_rejected&&g_phase==Phase::Rejected&&g_stop&&!g_level_installed);CHECK(call()==fx::original_bytes&&g_applied==0);weather_snapshot::Snapshot s;CHECK(!fx::material_read(s)&&fx::material_base()==0);});
 add("shared material callback expiry and rejection",[]{fx::reply=fx::active(3);fx::reply[11]=1;CHECK(start_before_entry(fx::weather_base));verify();weather_snapshot::Snapshot s;CHECK(fx::material_read(s));fx::time_ns+=5'000'000'000LL;CHECK(!fx::material_read(s));g_dart_rejected=true;CHECK(fx::material_base()==0&&!fx::material_read(s));});
 add("no late material install after default startup",[]{fx::reply=fx::active(3);CHECK(start_before_entry(fx::weather_base));auto w=fx::active(3,2);w[11]=1;g_cache.publish(w,fx::time_ns);CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Inactive&&g_stop&&fx::material_installs==0&&fx::threads==1);});
 add("material success cannot authorize rolled back level callback",[]{fx::reply[11]=1;fx::fail_mprotect_at=2;CHECK(start_before_entry(fx::weather_base));verify();CHECK(g_binding_ready&&!g_level_installed&&g_phase==Phase::Installed);CHECK(site_call(nullptr,1,(void*)hooked_device_level)==fx::original_bytes);CHECK(fx::original_calls==1&&g_applied==0);});
 add("real process_vm_readv invalid range",[]{std::array<std::byte,8>b{};CHECK(!read_memory(0,b)&&!read_memory(UINTPTR_MAX-4,b)&&!read_memory(1,b));});

 CHECK(cases.size()==92); // Preserve every previous shared-controller case.
 for(int card:{2,3})add("card only raw "+std::to_string(card),[=]{fx::reply=fx::active(3);fx::reply[10]=card;CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Installed&&card_startup_pending());CHECK(card_fixture::prepares==1&&fx::threads==1&&fx::material_installs==0&&fx::resolve_calls==0&&fx::mprotect_calls==0);CHECK(*fx::slot==(uintptr_t)fx::original&&!g_level_installed);verify();on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::installs==1&&card_fixture::phase==card_fixture::Phase::Installed&&!card_startup_pending());CHECK(card_fixture::seen_dart==fx::dart_base&&card_fixture::seen_flutter==0x400000&&card_fixture::install_read&&card_fixture::at_install.card_type==card&&card_fixture::at_install.device_level==3&&!card_fixture::at_install.unlock_blur);});
 add("card default never prepares",[]{fx::reply=fx::active(3);CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Inactive&&g_stop&&!card_startup_pending()&&card_fixture::prepares==0&&fx::material_installs==0&&fx::resolve_calls==0);});
 for(int raw:{-1,1,4,INT32_MIN,INT32_MAX})add("card invalid raw "+std::to_string(raw),[=]{fx::reply[10]=raw;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Missed&&g_stop&&card_fixture::prepares==0&&fx::material_installs==0&&fx::resolve_calls==0);});
 add("card only prepare failure closes startup",[]{fx::reply=fx::active(3);fx::reply[10]=2;card_fixture::prepare_ok=false;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Rejected&&g_stop&&card_fixture::prepares==1&&card_fixture::cancels==1&&!card_startup_pending());});
 add("card prepare failure leaves level and material independent",[]{fx::reply[10]=2;fx::reply[11]=1;card_fixture::prepare_ok=false;installed();verify();CHECK(card_fixture::prepares==1&&!card_startup_pending()&&fx::material_installs==1&&fx::threads==1);CHECK(call()==fx::original_bytes&&g_applied==1);weather_snapshot::Snapshot v;CHECK(fx::material_read(v)&&v.unlock_blur);});
 add("both import failures still admit card preparation",[]{fx::reply[10]=2;fx::reply[11]=1;fx::resolve_ok=false;fx::material_install_ok=false;CHECK(start_before_entry(fx::weather_base));verify();CHECK(card_startup_pending()&&!g_level_installed&&fx::material_installs==1&&*fx::slot==(uintptr_t)fx::original);on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::install_read&&card_fixture::phase==card_fixture::Phase::Installed);});
 add("level residue prevents card preparation",[]{fx::reply[10]=2;fx::reply[11]=1;fx::fail_restores=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&g_stop&&card_fixture::prepares==0&&fx::material_installs==0);});
 add("material residue prevents card preparation",[]{fx::reply[10]=2;fx::reply[11]=1;fx::material_install_ok=false;fx::material_residue=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&g_stop&&card_fixture::prepares==0);});
 add("card residue closes all shared callbacks",[]{fx::reply[10]=2;fx::reply[11]=1;installed();verify();card_fixture::installation_result=card_fixture::Phase::Residue;on_first_card_snapshot(fx::dart_base,0x400000);CHECK(g_phase==Phase::Residue&&g_stop&&!card_startup_pending());weather_snapshot::Snapshot v;CHECK(!fresh(v)&&!fx::material_read(v)&&!card_fixture::read(v));CHECK(call()==fx::original_bytes&&fx::original_calls==1&&g_applied==0);poll();CHECK(!card_fixture::service_read&&card_fixture::services==2);});
 add("card clean window failure preserves original components",[]{fx::reply[10]=3;fx::reply[11]=1;installed();verify();card_fixture::installation_result=card_fixture::Phase::Rejected;on_first_card_snapshot(fx::dart_base,0x400000);CHECK(g_phase==Phase::Installed&&!g_stop&&!card_startup_pending());CHECK(call()==fx::original_bytes&&g_applied==1);weather_snapshot::Snapshot v;CHECK(fx::material_read(v));});
 add("card window missed cannot late install",[]{fx::reply[10]=2;installed();on_card_snapshot_window_missed(7);CHECK(card_fixture::cancels==1&&!card_startup_pending());verify();on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::phase==card_fixture::Phase::Missed&&g_phase==Phase::Installed);CHECK(call()==fx::original_bytes&&g_applied==1);});
 add("card early first callback cannot prepare later",[]{fx::reply=fx::active(3);fx::reply[10]=2;on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::cancels==1&&card_fixture::installs==0);CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Rejected&&g_stop&&!card_startup_pending());});
 add("early snapshot during card preparation cancels batch",[]{fx::reply[10]=2;card_fixture::prepare_event=[]{observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart_base+weather_guard::kDartVmDataVa));};CHECK(!start_before_entry(fx::weather_base));CHECK(g_dart_rejected&&g_phase==Phase::Rejected&&g_stop&&card_fixture::cancels>=1&&!card_startup_pending());CHECK(call()==fx::original_bytes&&g_applied==0);});
 for(const char*name:{"kDartVmSnapshotData","_kDartIsolateSnapshotInstructions"})add(std::string("wrong first Dart cancels prepared card ")+name,[=]{fx::reply[10]=2;installed();observe_snapshot(name,(void*)(fx::dart_base+weather_guard::kDartVmDataVa));CHECK(g_dart_rejected&&card_fixture::cancels==1&&!card_startup_pending());on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::installs==0&&card_fixture::phase==card_fixture::Phase::Missed);});
 add("later mixed image cancels installed card",[]{fx::reply[10]=2;installed();verify();on_first_card_snapshot(fx::dart_base,0x400000);observe_snapshot("_kDartIsolateSnapshotData",(void*)(fx::dart_base+weather_guard::kDartIsolateDataVa+8));CHECK(g_dart_rejected&&card_fixture::cancels==1&&card_fixture::phase==card_fixture::Phase::Missed);weather_snapshot::Snapshot v;CHECK(!card_fixture::read(v));});
 add("shared poll services card after current publication",[]{fx::reply=fx::active(3);fx::reply[10]=2;CHECK(start_before_entry(fx::weather_base));verify();on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::services==1);fx::time_ns+=5'000'000'000LL;weather_snapshot::Snapshot v;CHECK(!card_fixture::read(v));fx::reply=fx::active(3,2);fx::reply[10]=3;poll();CHECK(card_fixture::services==2&&card_fixture::service_read&&card_fixture::at_service.card_type==3&&card_fixture::at_service.revision==2);CHECK(card_fixture::prepares==1&&card_fixture::installs==1&&fx::threads==1);fx::reply=fx::active(3,3);poll();CHECK(card_fixture::service_read&&card_fixture::at_service.card_type==0&&card_fixture::at_service.revision==3&&card_fixture::services==3);});
 add("failed shared query clears card supplied authorization",[]{fx::reply=fx::active(3);fx::reply[10]=2;CHECK(start_before_entry(fx::weather_base));verify();on_first_card_snapshot(fx::dart_base,0x400000);fx::parcel_bytes=44;poll();CHECK(card_fixture::services==2&&card_fixture::service_read&&!card_fixture::at_service.available&&!card_fixture::at_service.allowed&&card_fixture::at_service.card_type==0);CHECK(fx::threads==1&&card_fixture::prepares==1);});
 add("default startup cannot late prepare card",[]{fx::reply=fx::active(3);CHECK(start_before_entry(fx::weather_base));auto w=fx::active(3,2);w[10]=2;CHECK(g_cache.publish(w,fx::time_ns));CHECK(start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Inactive&&g_stop&&card_fixture::prepares==0&&!card_startup_pending()&&fx::threads==1);});
 add("concurrent card starts share one worker and preparation",[]{fx::reply[10]=2;fx::reply[11]=1;std::vector<std::thread> t;for(int i=0;i<8;++i)t.emplace_back([]{start_before_entry(fx::weather_base);});for(auto&v:t)v.join();CHECK(fx::threads==1&&fx::transacts==1&&card_fixture::prepares==1&&fx::material_installs==1&&card_startup_pending());verify();on_first_card_snapshot(fx::dart_base,0x400000);CHECK(card_fixture::installs==1);});
 CHECK(cases.size()==117); // Keep every previous card/controller scenario.
 add("early lifetime residue refuses all features before worker",[]{fx::reply[10]=2;fx::reply[11]=1;lifetime_fixture::residue=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&fx::threads==0&&fx::transacts==0&&fx::prepares==0);CHECK(lifetime_fixture::checks==1&&fx::resolve_calls==0&&fx::mprotect_calls==0&&fx::material_installs==0&&card_fixture::prepares==0);CHECK(!g_binding_ready&&!g_level_installed&&*fx::slot==(uintptr_t)fx::original);});
 add("early lifetime residue also refuses default mode",[]{fx::reply=fx::active(3);lifetime_fixture::residue=true;CHECK(!start_before_entry(fx::weather_base));CHECK(g_phase==Phase::Residue&&fx::threads==0&&fx::transacts==0&&fx::material_installs==0&&card_fixture::prepares==0&&lifetime_fixture::checks==1);});
 add("early lifetime residue remains terminal after adapter clears",[]{fx::reply[10]=3;lifetime_fixture::residue=true;CHECK(!start_before_entry(fx::weather_base));lifetime_fixture::residue=false;CHECK(!start_before_entry(fx::weather_base));observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart_base+weather_guard::kDartVmDataVa));on_first_card_snapshot(fx::dart_base,0x400000);CHECK(g_phase==Phase::Residue&&fx::threads==0&&fx::transacts==0&&card_fixture::prepares==0&&card_fixture::installs==0&&lifetime_fixture::checks==1);CHECK(*fx::slot==(uintptr_t)fx::original&&!g_level_installed);});
 add("parallel early lifetime residue attempts create zero workers",[]{lifetime_fixture::residue=true;std::atomic<int>accepted{0};std::vector<std::thread>ts;for(int i=0;i<8;++i)ts.emplace_back([&]{if(start_before_entry(fx::weather_base))++accepted;});for(auto&t:ts)t.join();CHECK(accepted==0&&g_phase==Phase::Residue&&fx::threads==0&&fx::transacts==0&&lifetime_fixture::checks==1);CHECK(card_fixture::prepares==0&&fx::material_installs==0&&fx::mprotect_calls==0);});
 CHECK(cases.size()==121);
 int failed=0;for(auto &c:cases){pid_t child=fork();if(child==0){try{fx::setup();c.run();std::cout<<"PASS "<<c.name<<std::endl;_exit(0);}catch(const std::exception&e){std::cerr<<"FAIL "<<c.name<<": "<<e.what()<<std::endl;_exit(1);}}CHECK(child>0);int status=0;CHECK(waitpid(child,&status,0)==child);if(!WIFEXITED(status)||WEXITSTATUS(status)!=0){++failed;if(!WIFEXITED(status))std::cerr<<"SIGNAL "<<c.name<<" status="<<status<<std::endl;}}
 std::cout<<"RESULT cases="<<cases.size()<<" failed="<<failed<<std::endl;return failed?1:0;
}
