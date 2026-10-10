// Complete production material runtime/policy included below. Only image identity,
// permission-fault injection and current authorization are external adapters.
#include "targets/weather/weather_material_binding.h"
#include "nativehook/got_hook_backend.h"
#include <android/log.h>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <functional>
#include <iostream>
#include <mutex>
#include <span>
#include <stdexcept>
#include <string>
#include <sys/wait.h>
#include <thread>
#include <unistd.h>
#include <vector>
#define CHECK(x) do{if(!(x))throw std::runtime_error(#x);}while(false)
extern "C" bool material_site(const char*,size_t,bool,void*);
extern "C" char material_return;
asm(".text\n.global material_site\n.type material_site,@function\nmaterial_site:\nsub $8,%rsp\ncall *%rcx\n.global material_return\nmaterial_return:\nadd $8,%rsp\nret\n.size material_site,.-material_site\n");
namespace fx {
namespace ws=weather_snapshot;
std::atomic<int64_t> now{1000000000};ws::Cache cache;
std::atomic<int> original_calls{0},fresh_calls{0},dart_calls{0},resolve_calls{0},cold_calls{0};
uintptr_t *slot=nullptr,base=0,dart=0x800000;
bool resolve_ok=true,dart_ok=true,cold=true,original_value=false;
int mprotect_calls=0,fail_mprotect_at=0;bool fail_restore=false;
std::function<void(int,int)> permission_event;
std::function<void(int)> cold_event;
std::function<void()> original_event,fresh_event;
const char* seen_key=nullptr;size_t seen_length=0;bool seen_default=false;
bool original(const char*k,size_t n,bool d){++original_calls;seen_key=k;seen_length=n;seen_default=d;if(original_event)original_event();return original_value;}
bool foreign(const char*,size_t,bool){return true;}
void put(ws::Words&w,size_t at,int64_t x){w[at]=static_cast<int32_t>(uint64_t(x));w[at+1]=static_cast<int32_t>(uint64_t(x)>>32);}
void publish(bool blur=true,bool allowed=true,int64_t deadline=0){ws::Words w{};w[0]=ws::kAck;w[1]=1;w[2]=allowed;put(w,3,17);put(w,5,1);if(allowed){put(w,7,deadline?deadline:now+4000000000LL);w[11]=blur;}w[9]=3;CHECK(cache.publish(w,now));}
bool fresh(ws::Snapshot&s){++fresh_calls;if(fresh_event)fresh_event();return cache.read(now,s);}
uintptr_t dart_base(){++dart_calls;return dart;}
void setup(){slot=(uintptr_t*)mmap(nullptr,4096,PROT_READ|PROT_WRITE,MAP_ANONYMOUS|MAP_PRIVATE,-1,0);CHECK(slot!=MAP_FAILED);*slot=(uintptr_t)original;CHECK(mprotect(slot,4096,PROT_READ)==0);base=(uintptr_t)&material_return-0x4402e4;publish();}
void set_slot(uintptr_t value){CHECK(::mprotect(slot,4096,PROT_READ|PROT_WRITE)==0);__atomic_store_n(slot,value,__ATOMIC_RELEASE);CHECK(::mprotect(slot,4096,PROT_READ)==0);}
}
namespace test_material_binding {
using Binding=weather_material_binding::Binding;
template<class Read>bool resolve(uintptr_t base,Read&& read,Binding&out){
 ++fx::resolve_calls;if(!fx::resolve_ok||base!=fx::base)return false;uintptr_t p=0;
 if(!read((uintptr_t)fx::slot,std::as_writable_bytes(std::span(&p,1)))||p!=(uintptr_t)fx::original)return false;
 Binding b;b.weather_base=base;b.public_base=0x100000;b.private_base=0x200000;b.slot=(uintptr_t)fx::slot;b.original=fx::original;out=b;return true;
}
template<class Read>bool verify_dart(uintptr_t base,Read&&){return fx::dart_ok&&base==0x800000;}
template<class Read>bool cold_state(const Binding&,Read&&){const bool observed=fx::cold;int n=++fx::cold_calls;if(fx::cold_event)fx::cold_event(n);return observed;}
}
int fixture_mprotect(void*,size_t,int);
#define weather_material_binding test_material_binding
#define mprotect fixture_mprotect
#ifndef HC_MATERIAL_SOURCE
#define HC_MATERIAL_SOURCE "targets/weather/weather_material_runtime.cpp"
#endif
#include HC_MATERIAL_SOURCE
#undef mprotect
#undef weather_material_binding
int fixture_mprotect(void *p,size_t n,int prot){
 int c=++fx::mprotect_calls;
 if(c==fx::fail_mprotect_at||(fx::fail_restore&&prot==PROT_READ)){errno=EPERM;return -1;}
 int rc=::mprotect(p,n,prot);if(rc==0&&fx::permission_event)fx::permission_event(c,prot);return rc;
}
namespace {
using namespace weather_material_runtime;
using namespace std::chrono_literals;
void installed(){CHECK(start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(g_phase==Phase::Installed&&g_binding_ready);CHECK(*fx::slot==(uintptr_t)hooked_get_bool);CHECK(!has_residue());CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);}
bool call(const char *key=weather_material_support::kMaterialProperty,size_t length=37,bool def=false){return material_site(key,length,def,(void*)__atomic_load_n(fx::slot,__ATOMIC_ACQUIRE));}
__attribute__((noinline)) bool foreign_call(){auto f=reinterpret_cast<weather_material_support::GetBool>(*fx::slot);bool v=f(weather_material_support::kMaterialProperty,37,true);CHECK(fx::original_calls>0);return v;}
void check_original(const char*k,size_t n,bool d){CHECK(fx::original_calls==1&&fx::seen_key==k&&fx::seen_length==n&&fx::seen_default==d);}
void rejected(){CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(g_phase==Phase::Rejected);CHECK(*fx::slot==(uintptr_t)fx::original&&!has_residue());}
void until(std::function<bool()>f){for(int i=0;i<200&&!f();++i)std::this_thread::sleep_for(1ms);CHECK(f());}
struct Case{std::string name;std::function<void()>run;};
}
int main(){std::vector<Case>cases;auto add=[&](std::string n,std::function<void()>f){cases.push_back({n,f});};
 add("cold residue false",[]{CHECK(!has_residue());});
 add("installed exact caller override original once",[]{installed();CHECK(call());check_original(weather_material_support::kMaterialProperty,37,false);CHECK(g_applied==1&&fx::fresh_calls==1);});
 add("unmodified original true retained",[]{installed();fx::original_value=true;CHECK(call());CHECK(fx::original_calls==1);});
 add("foreign caller passthrough once",[]{installed();CHECK(!foreign_call());check_original(weather_material_support::kMaterialProperty,37,true);CHECK(fx::fresh_calls==0&&g_phase==Phase::Installed);});
 add("foreign property passthrough once",[]{installed();const char*k="persist.sys.bionic_material_supported";CHECK(!call(k,37,true));check_original(k,37,true);CHECK(fx::fresh_calls==0&&g_phase==Phase::Installed);});
 for(size_t n:{0U,36U,38U})add("wrong property length "+std::to_string(n),[=]{installed();const char*k=weather_material_support::kMaterialProperty;CHECK(!call(k,n,true));check_original(k,n,true);CHECK(fx::fresh_calls==0&&g_phase==Phase::Installed);});
 add("null property passes original once",[]{installed();CHECK(!call(nullptr,37,true));check_original(nullptr,37,true);CHECK(g_phase==Phase::Installed);});
 add("missing Dart is terminal miss",[]{installed();fx::dart=0;CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);fx::dart=0x800000;CHECK(!call());CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(g_applied==0&&fx::original_calls==2);});
 add("unverified Dart is terminal miss",[]{installed();fx::dart_ok=false;CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::fresh_calls==0);});
 add("expired authorization is terminal miss",[]{installed();fx::now=5000000000LL;CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);});
 add("disabled setting is terminal miss",[]{fx::cache.revoke();fx::publish(false);installed();CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);});
 add("denied authorization is terminal miss",[]{fx::cache.revoke();fx::publish(false,false);installed();CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);});
 add("unavailable authorization is terminal miss",[]{installed();fx::cache.revoke();CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);});
 add("slow original expires before override decision",[]{installed();fx::original_event=[]{fx::now=5000000000LL;};CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);});
 add("changed image fails without late repair",[]{installed();fx::resolve_ok=false;CHECK(!call());CHECK(g_phase==Phase::Missed&&fx::original_calls==1);fx::resolve_ok=true;CHECK(!call());CHECK(g_applied==0);});
 add("foreign runtime GOT refuses stale wrapper",[]{installed();fx::set_slot((uintptr_t)fx::foreign);CHECK(!material_site(weather_material_support::kMaterialProperty,37,false,(void*)hooked_get_bool));CHECK(g_phase==Phase::Missed&&*fx::slot==(uintptr_t)fx::foreign&&fx::original_calls==1);});
 add("running1 is not rejected inside callback",[]{installed();fx::cold=false;CHECK(call());CHECK(fx::cold_calls==2);});
 add("duplicate start creates no additional write",[]{installed();const int calls=fx::mprotect_calls;CHECK(start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(fx::mprotect_calls==calls);});
 add("null authorization callback rejected",[]{CHECK(!start_before_entry(fx::base,nullptr,fx::dart_base));CHECK(g_phase==Phase::Rejected&&!g_binding_ready);});
 add("null Dart callback rejected",[]{CHECK(!start_before_entry(fx::base,fx::fresh,nullptr));CHECK(g_phase==Phase::Rejected&&!g_binding_ready);});
 add("identity mismatch rejected without writes",[]{fx::resolve_ok=false;rejected();CHECK(fx::mprotect_calls==0&&!g_binding_ready);});
 add("warm capability rejected without writes",[]{fx::cold=false;rejected();CHECK(fx::mprotect_calls==0&&!g_binding_ready);});
 add("nonreadonly page rejected",[]{CHECK(::mprotect(fx::slot,4096,PROT_READ|PROT_WRITE)==0);rejected();CHECK(fx::mprotect_calls==0);});
 add("write permission failure keeps original",[]{fx::fail_mprotect_at=1;rejected();CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);});
 add("first restore failure rolls back original",[]{fx::fail_mprotect_at=2;rejected();CHECK(fx::mprotect_calls>=4);CHECK(nhk::read_protection((uintptr_t)fx::slot)==PROT_READ);});
 add("foreign owner before CAS preserved",[]{fx::permission_event=[](int c,int){if(c==1)__atomic_store_n(fx::slot,(uintptr_t)fx::foreign,__ATOMIC_RELEASE);};CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(*fx::slot==(uintptr_t)fx::foreign&&g_phase==Phase::Residue&&has_residue());});
 add("foreign owner after CAS rollback preserved",[]{fx::permission_event=[](int c,int){if(c==2)fx::set_slot((uintptr_t)fx::foreign);};CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(*fx::slot==(uintptr_t)fx::foreign&&g_phase==Phase::Residue&&has_residue());});
 add("permanent restore failure residue",[]{fx::fail_restore=true;CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(has_residue()&&g_phase==Phase::Residue);});
 add("rollback cannot write keeps passthrough residue",[]{fx::permission_event=[](int c,int){if(c==2)fx::resolve_ok=false;};fx::fail_mprotect_at=3;CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(has_residue()&&*fx::slot==(uintptr_t)hooked_get_bool);CHECK(!call());CHECK(fx::original_calls==1);});
 add("binding published before observable GOT",[]{fx::permission_event=[](int c,int){if(c==2){CHECK(g_binding_ready&&g_policy.original==fx::original);CHECK(!foreign_call());CHECK(fx::original_calls==1);}};installed();CHECK(g_phase==Phase::Installed);});
 add("target query during Preparing closes window",[]{fx::permission_event=[](int c,int){if(c==2){CHECK(!call());CHECK(g_phase==Phase::Missed);}};CHECK(!start_before_entry(fx::base,fx::fresh,fx::dart_base));CHECK(g_phase==Phase::Missed&&*fx::slot==(uintptr_t)fx::original&&!has_residue());CHECK(fx::original_calls==1);});
 add("non-target during Preparing permits commit",[]{fx::permission_event=[](int c,int){if(c==2){const char*k="persist.sys.bionic_material_supported";CHECK(!call(k,37));CHECK(g_phase==Phase::Preparing);}};installed();CHECK(fx::original_calls==1);});
 add("late installer cannot reopen blocked first original",[]{std::atomic_bool entered=false,release=false;std::thread query;bool value=true,missedBefore=false;fx::original_event=[&]{entered=true;until([&]{return release.load();});};fx::cold_event=[&](int n){if(n==2){query=std::thread([&]{value=call();});until([&]{return entered.load();});missedBefore=g_phase==Phase::Missed;}};bool started=start_before_entry(fx::base,fx::fresh,fx::dart_base);release=true;query.join();CHECK(missedBefore&&!started&&!value&&g_phase==Phase::Missed&&*fx::slot==(uintptr_t)fx::original&&!has_residue());CHECK(fx::original_calls==1&&g_applied==0);});
 add("concurrent starters one writer",[]{std::atomic_bool at_write=false,release=false;fx::permission_event=[&](int c,int){if(c==1){at_write=true;until([&]{return release.load();});}};bool first=false;std::thread t([&]{first=start_before_entry(fx::base,fx::fresh,fx::dart_base);});until([&]{return at_write.load();});bool second=start_before_entry(fx::base,fx::fresh,fx::dart_base);release=true;t.join();CHECK(first&&!second&&g_phase==Phase::Installed&&fx::mprotect_calls==2);});
 add("real process vm reader rejects inaccessible range",[]{std::byte x;CHECK(!read_memory(1,std::span(&x,1)));CHECK(!read_memory(UINTPTR_MAX,std::span(&x,1)));});
 unsigned failures=0;
 for(const auto&c:cases){pid_t pid=fork();CHECK(pid>=0);if(pid==0){try{fx::setup();c.run();std::cout<<"PASS "<<c.name<<std::endl;_exit(0);}catch(const std::exception&e){std::cerr<<"FAIL "<<c.name<<": "<<e.what()<<std::endl;_exit(1);}}int status=0;CHECK(waitpid(pid,&status,0)==pid);if(!WIFEXITED(status)||WEXITSTATUS(status)!=0)failures++;}
 std::cout<<"RESULT cases="<<cases.size()<<" failed="<<failures<<std::endl;return failures?1:0;
}
