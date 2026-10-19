/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Three complete production translation units are included. External binding
// identity, Binder, lifetime observer, clock and permission faults are adapted.
#include "targets/weather/weather_binding.h"
#include "targets/weather/weather_material_binding.h"
#include "targets/weather/weather_material_runtime.h"
#include "targets/weather/weather_card_runtime.h"
#include "targets/weather/weather_card_lifetime.h"
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
#include <stdexcept>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <sys/wait.h>
#include <thread>
#include <time.h>
#include <unistd.h>
#include <vector>
#include <elf.h>
#include <fcntl.h>
#define CHECK(x) do { if (!(x)) throw std::runtime_error(#x); } while(false)
extern "C" bool joint_material_call(const char*,size_t,bool,void*);
extern "C" void *joint_level_call(const void*,int32_t,void*);
extern "C" char joint_material_return,joint_level_return;
namespace fx {
using Words=weather_snapshot::Words;
std::atomic<int64_t> time_ns{10'000'000'000LL};
Words reply{};
std::atomic<int> workers{0},transacts{0},level_calls{0},material_calls{0},level_resolves{0},material_resolves{0},cold_calls{0};
uintptr_t *level_slot=nullptr,*material_slot=nullptr,base=0,dart=0x800000;
uintptr_t flutter=0;
bool card_restore_failure=false;
std::atomic_bool lifetime_ready=true,lifetime_residue=false,lifetime_busy=false;
std::atomic_bool lifetime_protected=false,lifetime_lost=false;
uint32_t lazy_state=3;
uint8_t original_bytes[3]={3,1,2},allocated_bytes[3]={0,0,0};
int32_t seen_selector=0;
const void *seen_runtime=nullptr;
const char *seen_key=nullptr;size_t seen_length=0;bool seen_default=false;
bool level_weather=true,material_weather=true,level_dart=true,material_dart=true;
bool level_identity=true,material_identity=true,original_bool=false;
bool level_restore_fail=false,material_restore_fail=false;
int level_protect_calls=0,material_protect_calls=0,level_fail_at=0,material_fail_at=0;
std::function<void(bool,int,int)> permission_event;
std::function<void(int)> cold_event;
std::function<void()> original_event;
std::atomic_bool block_binder=false,release_binder=false,entered_binder=false;
std::mutex mutex;std::condition_variable condition;
void *level_original(const void*r,int32_t s){++level_calls;seen_selector=s;seen_runtime=r;return original_bytes;}
bool material_original(const char*k,size_t n,bool d){++material_calls;seen_key=k;seen_length=n;seen_default=d;if(original_event)original_event();return original_bool;}
void *allocate(size_t size,size_t alignment){CHECK(size==3&&alignment==1);return allocated_bytes;}
void allocation_shim(){}
void drop(void*p){CHECK(p==allocated_bytes);}
void put(Words&w,size_t offset,int64_t n){w[offset]=int32_t(uint64_t(n));w[offset+1]=int32_t(uint64_t(n)>>32);}
Words active(int level=2,bool blur=true,int64_t revision=1){Words w{};w[0]=weather_snapshot::kAck;w[1]=1;w[2]=1;put(w,3,17);put(w,5,revision);put(w,7,time_ns+4'000'000'000LL);w[9]=level;w[11]=blur;return w;}
uintptr_t* new_slot(uintptr_t original){auto*p=(uintptr_t*)mmap(nullptr,4096,PROT_READ|PROT_WRITE,MAP_ANONYMOUS|MAP_PRIVATE,-1,0);CHECK(p!=MAP_FAILED);*p=original;CHECK(::mprotect(p,4096,PROT_READ)==0);return p;}
void setup(){reply=active();level_slot=new_slot((uintptr_t)level_original);material_slot=new_slot((uintptr_t)material_original);base=(uintptr_t)&joint_material_return-0x4402e4;CHECK((uintptr_t)&joint_level_return==base+0x5fc76c);}
void until(std::function<bool()> f){for(int i=0;i<1000&&!f();++i)std::this_thread::sleep_for(std::chrono::milliseconds(1));CHECK(f());}
}
namespace weather_card_lifetime {
bool ready() noexcept{return fx::lifetime_ready;}
bool has_residue() noexcept{return fx::lifetime_residue;}
bool protect(uintptr_t dart,uintptr_t flutter,const std::array<uintptr_t,2>&sites) noexcept{
 if(fx::lifetime_busy||!fx::lifetime_ready||dart!=fx::dart||flutter!=fx::flutter
    ||sites!=std::array<uintptr_t,2>{dart+0x3a8408,dart+0x3aece0})return false;
 fx::lifetime_protected=true;return true;
}
bool intact() noexcept{return fx::lifetime_protected&&!fx::lifetime_lost;}
bool release_pages() noexcept{fx::lifetime_protected=false;return true;}
}
namespace test_level_binding {
using Binding=weather_binding::Binding;
template<class Read>bool verify_weather(uintptr_t base,Read&&){return fx::level_weather&&base==fx::base;}
template<class Read>bool verify_dart(uintptr_t base,Read&&){return fx::level_dart&&base==fx::dart;}
template<class Read>bool resolve(uintptr_t base,Read&&read,Binding&out){
 ++fx::level_resolves;uintptr_t value=0;
 if(!fx::level_identity||!verify_weather(base,read)||!read((uintptr_t)fx::level_slot,std::as_writable_bytes(std::span(&value,1)))||value!=(uintptr_t)fx::level_original)return false;
 Binding b;b.weather_base=base;b.public_base=0x100000;b.standard_base=0x200000;b.slot=(uintptr_t)fx::level_slot;b.api={fx::level_original,fx::allocate,fx::allocation_shim,fx::drop};out=b;return true;
}
}
namespace test_material_binding {
using Binding=weather_material_binding::Binding;
template<class Read>bool verify_weather(uintptr_t base,Read&&){return fx::material_weather&&base==fx::base;}
template<class Read>bool verify_dart(uintptr_t base,Read&&){return fx::material_dart&&base==fx::dart;}
template<class Read>bool resolve(uintptr_t base,Read&&read,Binding&out){
 ++fx::material_resolves;uintptr_t value=0;
 if(!fx::material_identity||!verify_weather(base,read)||!read((uintptr_t)fx::material_slot,std::as_writable_bytes(std::span(&value,1)))||value!=(uintptr_t)fx::material_original)return false;
 Binding b;b.weather_base=base;b.public_base=0x100000;b.private_base=0x300000;b.slot=(uintptr_t)fx::material_slot;b.original=fx::material_original;out=b;return true;
}
template<class Read>bool cold_state(const Binding&,Read&&read){
 uint32_t value=0;bool good=read((uintptr_t)&fx::lazy_state,std::as_writable_bytes(std::span(&value,1)));
 int count=++fx::cold_calls;if(fx::cold_event)fx::cold_event(count);return good&&value==3;
}
}
int fixture_mprotect(void*,size_t,int);
int fixture_clock_gettime(clockid_t c,timespec*t){if(c!=CLOCK_BOOTTIME)return ::clock_gettime(c,t);auto n=fx::time_ns.load();t->tv_sec=n/1'000'000'000LL;t->tv_nsec=n%1'000'000'000LL;return 0;}
int fixture_nanosleep(const timespec*,timespec*){std::unique_lock lock(fx::mutex);fx::condition.wait(lock,[]{return false;});return 0;}
int fixture_pthread_create(pthread_t*t,const pthread_attr_t*a,void*(*f)(void*),void*x){++fx::workers;return ::pthread_create(t,a,f,x);}
AIBinder *service(const char*name){CHECK(std::strcmp(name,"window")==0);static AIBinder_Class c;static AIBinder b;c.descriptor="android.view.IWindowManager";b.clazz=&c;return &b;}
void *fixture_dlsym(void*,const char*name){CHECK(std::strcmp(name,"AServiceManager_checkService")==0);return (void*)service;}
int fixture_card_mprotect(void*p,size_t size,int prot){
 const auto page_size=size_t(sysconf(_SC_PAGESIZE));
 const auto first=(fx::dart+0x3a8408)&~(uintptr_t(page_size)-1);
 if(fx::card_restore_failure&&reinterpret_cast<uintptr_t>(p)==first&&prot==(PROT_READ|PROT_EXEC)){errno=EPERM;return -1;}
 return ::mprotect(p,size,prot);
}
// Actual card installer and guards run before the legacy binding adapters are
// selected. Its pinned Dart/Flutter PT_LOAD images are real ROM bytes below.
#define clock_gettime fixture_clock_gettime
#define mprotect fixture_card_mprotect
#ifndef HC_CARD_RUNTIME_SOURCE
#define HC_CARD_RUNTIME_SOURCE "targets/weather/weather_card_runtime.cpp"
#endif
#include HC_CARD_RUNTIME_SOURCE
#undef mprotect
#undef clock_gettime
#define weather_binding test_level_binding
#define weather_material_binding test_material_binding
#define mprotect fixture_mprotect
#ifndef HC_MATERIAL_SOURCE
#define HC_MATERIAL_SOURCE "targets/weather/weather_material_runtime.cpp"
#endif
#include HC_MATERIAL_SOURCE
#define clock_gettime fixture_clock_gettime
#define nanosleep fixture_nanosleep
#define pthread_create fixture_pthread_create
#define dlsym fixture_dlsym
#ifndef HC_RUNTIME_SOURCE
#define HC_RUNTIME_SOURCE "targets/weather/weather_runtime.cpp"
#endif
#include HC_RUNTIME_SOURCE
#undef dlsym
#undef pthread_create
#undef nanosleep
#undef clock_gettime
#undef mprotect
#undef weather_material_binding
#undef weather_binding
int fixture_mprotect(void*p,size_t size,int prot){
 const bool material=p==(void*)fx::material_slot;CHECK(material||p==(void*)fx::level_slot);
 int count=material?++fx::material_protect_calls:++fx::level_protect_calls;
 bool fail=count==(material?fx::material_fail_at:fx::level_fail_at)||((material?fx::material_restore_fail:fx::level_restore_fail)&&prot==PROT_READ);
 if(fail){errno=EPERM;return -1;}
 int rc=::mprotect(p,size,prot);if(rc==0&&fx::permission_event)fx::permission_event(material,count,prot);return rc;
}
const AIBinder_Class *AIBinder_getClass(AIBinder*b){return b->clazz;}
const char *AIBinder_Class_getDescriptor(const AIBinder_Class*c){return c->descriptor;}
const AIBinder_Class *AIBinder_Class_define(const char*d,void*(*)(void*),void(*)(void*),binder_status_t(*)(AIBinder*,transaction_code_t,const AParcel*,AParcel*)){static AIBinder_Class c;c.descriptor=d;return &c;}
bool AIBinder_associateClass(AIBinder*,const AIBinder_Class*){return true;}
binder_status_t AIBinder_prepareTransaction(AIBinder*,AParcel**p){*p=new AParcel;return 0;}
binder_status_t AIBinder_transact(AIBinder*,transaction_code_t code,AParcel**in,AParcel**out,uint32_t flags){
 CHECK(code==weather_snapshot::kTransaction&&flags==0);++fx::transacts;delete *in;*in=nullptr;
 if(fx::block_binder){fx::entered_binder=true;std::unique_lock lock(fx::mutex);fx::condition.wait(lock,[]{return fx::release_binder.load();});}
 *out=new AParcel;(*out)->bytes=48;std::copy(fx::reply.begin(),fx::reply.end(),(*out)->values);return 0;
}
void AIBinder_decStrong(AIBinder*){}
int32_t AParcel_getDataSize(const AParcel*p){return p->bytes;}
binder_status_t AParcel_readInt32(const AParcel*raw,int32_t*v){auto*p=const_cast<AParcel*>(raw);CHECK(p->pos<12);*v=p->values[p->pos++];return 0;}
void AParcel_delete(AParcel*p){delete p;}
namespace wr=weather_runtime;
namespace mr=weather_material_runtime;
namespace cr=weather_card_runtime;
namespace {
bool start(){return wr::start_before_entry(fx::base);}
void verify(){wr::observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart+weather_guard::kDartVmDataVa));CHECK(wr::g_dart_base==fx::dart&&!wr::g_dart_rejected);}
bool material(void*target=nullptr,const char*key=weather_material_support::kMaterialProperty){return joint_material_call(key,37,false,target?target:(void*)__atomic_load_n(fx::material_slot,__ATOMIC_ACQUIRE));}
void *level(void*target=nullptr){return joint_level_call((void*)0x98765,INT32_MIN,target?target:(void*)__atomic_load_n(fx::level_slot,__ATOMIC_ACQUIRE));}
void both(){CHECK(start());CHECK(wr::g_phase==wr::Phase::Installed&&wr::g_level_installed&&mr::g_phase==mr::Phase::Installed);CHECK(fx::workers==1&&fx::transacts==1);verify();}
void level_applies(){CHECK(level()==fx::original_bytes);CHECK(fx::level_calls==1&&fx::seen_selector==INT32_MIN&&fx::seen_runtime==(void*)0x98765&&wr::g_applied==1);for(uint8_t v:fx::original_bytes)CHECK(v==3);}
void material_applies(){CHECK(material());CHECK(fx::material_calls==1&&mr::g_applied==1&&fx::seen_key==weather_material_support::kMaterialProperty&&fx::seen_length==37&&!fx::seen_default);}
void original_slots(){CHECK(*fx::level_slot==(uintptr_t)fx::level_original&&*fx::material_slot==(uintptr_t)fx::material_original);}
struct Case{std::string name;std::function<void()>run;};
uintptr_t map_elf(const char*path){
 const int fd=open(path,O_RDONLY|O_CLOEXEC);CHECK(fd>=0);
 Elf64_Ehdr eh{};CHECK(pread(fd,&eh,sizeof(eh),0)==sizeof(eh)&&eh.e_machine==EM_AARCH64&&eh.e_phnum<=64);
 std::array<Elf64_Phdr,64> headers{};size_t count=eh.e_phnum*sizeof(Elf64_Phdr);
 CHECK(pread(fd,headers.data(),count,eh.e_phoff)==ssize_t(count));size_t extent=0,page=size_t(sysconf(_SC_PAGESIZE));
 for(unsigned i=0;i<eh.e_phnum;++i)if(headers[i].p_type==PT_LOAD)extent=std::max(extent,size_t(headers[i].p_vaddr+headers[i].p_memsz));
 extent=(extent+page-1)&~(page-1);void*region=mmap(nullptr,extent,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);CHECK(region!=MAP_FAILED);
 const uintptr_t base=reinterpret_cast<uintptr_t>(region);
 for(unsigned i=0;i<eh.e_phnum;++i){const auto&h=headers[i];if(h.p_type==PT_LOAD&&h.p_filesz)CHECK(pread(fd,reinterpret_cast<void*>(base+h.p_vaddr),h.p_filesz,h.p_offset)==ssize_t(h.p_filesz));}
 close(fd);CHECK(::mprotect(region,extent,PROT_READ)==0);
 for(unsigned i=0;i<eh.e_phnum;++i){const auto&h=headers[i];if(h.p_type!=PT_LOAD||!(h.p_flags&PF_X))continue;auto start=(base+h.p_vaddr)&~(uintptr_t(page)-1);auto end=(base+h.p_vaddr+h.p_memsz+page-1)&~(uintptr_t(page)-1);CHECK(::mprotect(reinterpret_cast<void*>(start),end-start,PROT_READ|PROT_EXEC)==0);}
 return base;
}
void card_config(int raw=2,bool all=false){fx::reply=fx::active(all?2:3,all);fx::reply[10]=raw;}
void card_installed(){CHECK(start());CHECK(wr::card_startup_pending()&&cr::status().phase==cr::Phase::Prepared);wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().phase==cr::Phase::Installed&&cr::status().owned_mask==3);CHECK(!wr::card_startup_pending());verify();}
}
int main(int argc,char**argv){CHECK(argc==3);fx::dart=map_elf(argv[1]);fx::flutter=map_elf(argv[2]);std::vector<Case> cases;auto add=[&](std::string name,std::function<void()>run){cases.push_back({name,run});};
 add("card-only real installer shares HWS worker",[]{card_config();card_installed();CHECK(fx::workers==1&&fx::transacts==1&&fx::level_resolves==0&&fx::material_resolves==0);original_slots();CHECK(cr::g_lease.read()==(uint64_t(weather_snapshot::long_at(fx::reply,7))<<2|2));});
 add("three real components consume one HWS snapshot",[]{card_config(3,true);card_installed();level_applies();material_applies();CHECK((cr::g_lease.read()&3)==3&&fx::workers==1&&fx::transacts==1);});
 add("card mode update flows through existing poll",[]{card_config();card_installed();fx::reply[10]=3;fx::put(fx::reply,5,2);wr::poll();CHECK((cr::g_lease.read()&3)==3&&fx::workers==1&&fx::transacts==2);CHECK(cr::image_state(3));});
 add("card default stops lease without reinstall",[]{card_config();card_installed();fx::reply[10]=0;fx::put(fx::reply,5,2);wr::poll();CHECK(cr::g_lease.read()==0&&cr::image_state(3)&&cr::status().phase==cr::Phase::Installed);});
 add("card malformed frame revokes current lease",[]{card_config();card_installed();fx::reply[10]=1;wr::poll();CHECK(cr::g_lease.read()==0&&fx::workers==1);});
 add("card lease expiry cannot gain receipt time",[]{card_config();card_installed();fx::time_ns+=5'000'000'000LL;wr::poll();CHECK(cr::g_lease.read()==0);});
 add("wrong later export cancels real card immediately",[]{card_config(2,true);card_installed();wr::observe_snapshot("_kDartIsolateSnapshotData",(void*)(fx::dart+weather_guard::kDartIsolateDataVa+4));CHECK(cr::g_lease.terminal()&&wr::g_dart_rejected);CHECK(!material()&&level()==fx::original_bytes&&wr::g_applied==0&&mr::g_applied==0);});
 add("missed card window leaves other real components active",[]{card_config(2,true);CHECK(start());wr::on_card_snapshot_window_missed(17);CHECK(cr::g_lease.terminal()&&cr::status().phase==cr::Phase::Missed);verify();level_applies();material_applies();CHECK(!wr::card_startup_pending());});
 add("card early window cannot be reopened by entry",[]{card_config();wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(!start()&&cr::g_lease.terminal()&&cr::status().phase==cr::Phase::Missed);original_slots();});
 add("card foreign Dart body leaves other components active",[]{card_config(2,true);CHECK(start());auto at=fx::dart+0x3a840c;auto page=at&~uintptr_t(4095);CHECK(::mprotect((void*)page,4096,PROT_READ|PROT_WRITE|PROT_EXEC)==0);*(uint32_t*)at=0xd503201f;CHECK(::mprotect((void*)page,4096,PROT_READ|PROT_EXEC)==0);wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().phase==cr::Phase::Rejected&&cr::status().owned_mask==0&&wr::g_phase==wr::Phase::Installed);verify();level_applies();material_applies();});
 add("card residue closes all three real components",[]{card_config(2,true);CHECK(start());fx::card_restore_failure=true;wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().phase==cr::Phase::Residue&&wr::g_phase==wr::Phase::Residue&&wr::g_stop&&cr::g_lease.terminal());CHECK(level()==fx::original_bytes&&!material()&&wr::g_applied==0&&mr::g_applied==0);});
 add("duplicate card window permanently closes card",[]{card_config();card_installed();wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::g_lease.terminal()&&cr::status().phase==cr::Phase::Missed&&cr::image_state(3));});
 add("default startup cannot late prepare card",[]{fx::reply=fx::active(3,false);CHECK(start());fx::reply[10]=2;fx::put(fx::reply,5,2);wr::poll();CHECK(start());CHECK(!wr::card_startup_pending()&&cr::status().phase==cr::Phase::Cold&&cr::status().arena==0&&wr::g_stop);});
 add("card future epoch accepted only from current cache",[]{card_config();card_installed();fx::reply[10]=3;fx::put(fx::reply,3,18);fx::put(fx::reply,5,1);wr::poll();CHECK((cr::g_lease.read()&3)==3);fx::put(fx::reply,5,0);wr::poll();CHECK(cr::g_lease.read()==0);});
 add("early lifetime residue prevents all workers and overrides",[]{card_config(2,true);fx::lifetime_residue=true;CHECK(!start());CHECK(wr::g_phase==wr::Phase::Residue&&fx::workers==0&&fx::transacts==0&&fx::level_resolves==0&&fx::material_resolves==0);original_slots();});
 add("unavailable lifetime isolates only card rejection",[]{card_config(2,true);fx::lifetime_ready=false;CHECK(start());CHECK(!wr::card_startup_pending()&&cr::status().error==cr::Error::LifetimeRejected&&cr::status().arena==0);verify();level_applies();material_applies();});
 add("busy lifetime handshake leaves other components active",[]{card_config(2,true);CHECK(start());fx::lifetime_busy=true;wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().error==cr::Error::LifetimeRejected&&cr::status().owned_mask==0&&cr::image_state(0));verify();level_applies();material_applies();});
 add("lost lifetime stops card via shared poll",[]{card_config(2,true);card_installed();fx::lifetime_lost=true;wr::poll();CHECK(cr::g_lease.terminal()&&cr::image_state(3));level_applies();material_applies();});
 add("blur only real component and same caller base",[]{fx::reply=fx::active(3,true);CHECK(start());CHECK(!wr::g_level_installed&&fx::level_resolves==0&&fx::level_protect_calls==0&&mr::g_phase==mr::Phase::Installed);CHECK(*fx::level_slot==(uintptr_t)fx::level_original);verify();material_applies();CHECK(fx::workers==1&&fx::transacts==1);});
 add("level only never starts material",[]{fx::reply=fx::active(2,false);CHECK(start());verify();level_applies();CHECK(mr::g_phase==mr::Phase::Cold&&fx::material_resolves==0&&fx::material_protect_calls==0&&*fx::material_slot==(uintptr_t)fx::material_original&&fx::workers==1);});
 add("both real components consume one shared publication",[]{both();level_applies();material_applies();CHECK(fx::workers==1&&fx::transacts==1);});
 add("level identity rejection leaves material functional",[]{fx::level_identity=false;CHECK(start());verify();CHECK(!wr::g_level_installed&&*fx::level_slot==(uintptr_t)fx::level_original);material_applies();});
 add("material identity rejection leaves level functional",[]{fx::material_identity=false;CHECK(start());verify();CHECK(mr::g_phase==mr::Phase::Rejected&&*fx::material_slot==(uintptr_t)fx::material_original);level_applies();});
 add("warm material cache rejection leaves level functional",[]{fx::lazy_state=0;CHECK(start());verify();CHECK(mr::g_phase==mr::Phase::Rejected&&fx::material_protect_calls==0);level_applies();});
 add("both identity failures stop shared worker",[]{fx::level_identity=false;fx::material_identity=false;CHECK(!start());CHECK(wr::g_phase==wr::Phase::Rejected&&wr::g_stop&&fx::workers==1);original_slots();});
 add("level residue closes material startup",[]{fx::level_restore_fail=true;CHECK(!start());CHECK(wr::g_phase==wr::Phase::Residue&&wr::g_stop&&!wr::g_level_installed&&mr::g_phase==mr::Phase::Cold);CHECK(fx::material_resolves==0&&fx::workers==1);CHECK(level((void*)wr::hooked_device_level)==fx::original_bytes&&fx::level_calls==1&&wr::g_applied==0);});
 add("material residue closes both real components",[]{fx::material_restore_fail=true;CHECK(!start());CHECK(mr::has_residue()&&wr::g_phase==wr::Phase::Residue&&wr::g_stop&&!wr::g_level_installed);CHECK(*fx::level_slot==(uintptr_t)wr::hooked_device_level);CHECK(level()==fx::original_bytes&&fx::level_calls==1&&wr::g_applied==0);CHECK(!material((void*)mr::hooked_get_bool)&&fx::material_calls==1&&mr::g_applied==0);});
 add("foreign level CAS owner retained and material not installed",[]{fx::permission_event=[](bool m,int c,int){if(!m&&c==1)__atomic_store_n(fx::level_slot,uintptr_t(0x12345678),__ATOMIC_RELEASE);};CHECK(!start());CHECK(*fx::level_slot==0x12345678&&wr::g_phase==wr::Phase::Residue&&mr::g_phase==mr::Phase::Cold&&wr::g_stop);});
 add("foreign material CAS closes batch without overwriting owner",[]{fx::permission_event=[](bool m,int c,int){if(m&&c==1)__atomic_store_n(fx::material_slot,uintptr_t(0x87654321),__ATOMIC_RELEASE);};CHECK(!start());CHECK(*fx::material_slot==0x87654321&&mr::has_residue()&&wr::g_phase==wr::Phase::Residue&&!wr::g_level_installed);CHECK(level()==fx::original_bytes&&wr::g_applied==0);});
 add("clean material permission failure leaves level functional",[]{fx::material_fail_at=1;CHECK(start());verify();CHECK(mr::g_phase==mr::Phase::Rejected&&!mr::has_residue());level_applies();});
 add("clean level rollback cannot authorize retained level wrapper",[]{fx::level_fail_at=2;CHECK(start());verify();CHECK(wr::g_binding_ready&&!wr::g_level_installed&&*fx::level_slot==(uintptr_t)fx::level_original);CHECK(level((void*)wr::hooked_device_level)==fx::original_bytes&&fx::level_calls==1&&wr::g_applied==0);material_applies();});
 add("first material query before VMData never activates later",[]{fx::reply=fx::active(3,true);CHECK(start());CHECK(!material());CHECK(mr::g_phase==mr::Phase::Missed&&fx::material_calls==1);verify();CHECK(!material()&&mr::g_applied==0&&fx::material_calls==2&&fx::workers==1);});
 add("material query during blur-only install is terminal",[]{fx::reply=fx::active(3,true);fx::permission_event=[](bool m,int c,int){if(m&&c==2){CHECK(mr::g_binding_ready&&mr::g_policy.original==fx::material_original);CHECK(!material());CHECK(mr::g_phase==mr::Phase::Missed);}};CHECK(!start());CHECK(wr::g_phase==wr::Phase::Rejected&&wr::g_stop&&mr::g_phase==mr::Phase::Missed);original_slots();CHECK(!start()&&fx::workers==1&&fx::material_calls==1);});
 add("material query during both install leaves only level active",[]{fx::permission_event=[](bool m,int c,int){if(m&&c==2)CHECK(!material());};CHECK(start());verify();CHECK(mr::g_phase==mr::Phase::Missed&&*fx::material_slot==(uintptr_t)fx::material_original);level_applies();CHECK(fx::material_calls==1&&mr::g_applied==0&&fx::workers==1);});
 add("snapshot during material install closes entire batch",[]{fx::permission_event=[](bool m,int c,int){if(m&&c==2)wr::observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart+weather_guard::kDartVmDataVa));};CHECK(!start());CHECK(wr::g_dart_rejected&&wr::g_phase==wr::Phase::Rejected&&wr::g_stop&&!wr::g_level_installed);CHECK(!material()&&level()==fx::original_bytes&&mr::g_applied==0&&wr::g_applied==0);});
 add("original bionic query while preparing does not consume material window",[]{fx::permission_event=[](bool m,int c,int){if(m&&c==2)CHECK(!material(nullptr,"persist.sys.bionic_material_supported"));};CHECK(start());verify();CHECK(mr::g_phase==mr::Phase::Installed&&fx::material_calls==1);CHECK(material()&&fx::material_calls==2&&mr::g_applied==1);});
 add("running Once state5 does not reject verified material query",[]{both();fx::lazy_state=5;material_applies();CHECK(fx::cold_calls==2);});
 add("shared lease expiry prevents both overrides",[]{both();fx::time_ns+=5'000'000'000LL;CHECK(!material()&&mr::g_phase==mr::Phase::Missed);CHECK(level()==fx::original_bytes&&wr::g_applied==0&&fx::level_calls==1&&fx::material_calls==1);});
 add("blocked original expires before fresh shared snapshot read",[]{both();fx::original_event=[]{fx::time_ns+=5'000'000'000LL;};CHECK(!material()&&mr::g_phase==mr::Phase::Missed&&fx::material_calls==1&&mr::g_applied==0);});
 add("canonical denied installs neither component",[]{fx::reply=weather_snapshot::unavailable_words();fx::reply[1]=1;fx::put(fx::reply,3,17);fx::put(fx::reply,5,1);CHECK(start());CHECK(wr::g_phase==wr::Phase::Inactive&&wr::g_stop&&mr::g_phase==mr::Phase::Cold&&fx::workers==1);original_slots();});
 add("default startup cannot late install either component",[]{fx::reply=fx::active(3,false);CHECK(start());CHECK(wr::g_cache.publish(fx::active(0,true,2),fx::time_ns));CHECK(start());CHECK(wr::g_phase==wr::Phase::Inactive&&wr::g_stop&&mr::g_phase==mr::Phase::Cold&&fx::workers==1);original_slots();});
 add("level Dart identity cannot substitute for material Dart",[]{both();fx::material_dart=false;CHECK(!material()&&mr::g_phase==mr::Phase::Missed);level_applies();});
 add("material Dart identity works without level Dart",[]{CHECK(start());fx::level_dart=false;verify();material_applies();CHECK(level()==fx::original_bytes&&wr::g_applied==0&&fx::level_calls==1);});
 add("both Dart identities failed refuse both",[]{CHECK(start());fx::level_dart=fx::material_dart=false;wr::observe_snapshot("_kDartVmSnapshotData",(void*)(fx::dart+weather_guard::kDartVmDataVa));CHECK(wr::g_dart_rejected);CHECK(!material()&&level()==fx::original_bytes&&mr::g_applied==0&&wr::g_applied==0);});
 add("mismatched later export revokes both",[]{both();wr::observe_snapshot("_kDartIsolateSnapshotInstructions",(void*)(fx::dart+weather_guard::kDartIsolateInstructionsVa+8));CHECK(wr::g_dart_rejected);CHECK(!material()&&level()==fx::original_bytes&&mr::g_applied==0&&wr::g_applied==0);});
 add("material live binding changed only material refused",[]{both();fx::material_identity=false;CHECK(!material()&&mr::g_phase==mr::Phase::Missed);level_applies();});
 add("level live binding changed only level refused",[]{both();fx::level_identity=false;CHECK(level()==fx::original_bytes&&wr::g_applied==0);material_applies();});
 add("concurrent entry attempts create exactly one real worker",[]{std::vector<std::thread> threads;for(int i=0;i<8;++i)threads.emplace_back([]{start();});for(auto&t:threads)t.join();CHECK(fx::workers==1&&fx::transacts==1&&fx::level_protect_calls==2&&fx::material_protect_calls==2);verify();level_applies();material_applies();});
 add("blocked first original cannot be authorized by late installer",[]{std::atomic_bool entered=false,release=false;std::thread query;bool result=true,closed=false;fx::original_event=[&]{entered=true;fx::until([&]{return release.load();});};fx::cold_event=[&](int count){if(count==2){query=std::thread([&]{result=material();});fx::until([&]{return entered.load();});closed=mr::g_phase==mr::Phase::Missed;}};bool started=start();if(started)verify();release=true;query.join();CHECK(closed&&started&&!result&&mr::g_phase==mr::Phase::Missed&&mr::g_applied==0&&fx::material_calls==1);CHECK(*fx::material_slot==(uintptr_t)fx::material_original);level_applies();});
 add("blocked Binder has one bounded startup and no late reopen",[]{fx::block_binder=true;auto began=std::chrono::steady_clock::now();CHECK(!start());auto duration=std::chrono::steady_clock::now()-began;CHECK(duration>=std::chrono::milliseconds(900)&&duration<std::chrono::milliseconds(1500));CHECK(fx::entered_binder&&wr::g_phase==wr::Phase::Missed&&wr::g_stop&&fx::workers==1&&mr::g_phase==mr::Phase::Cold);fx::release_binder=true;fx::condition.notify_all();fx::until([]{std::lock_guard lock(wr::g_initial_mutex);return wr::g_initial_completed;});CHECK(!start()&&fx::workers==1);original_slots();});
 add("material-only image admits blur without level import resolution",[]{fx::reply=fx::active(3,true);fx::level_weather=false;CHECK(start());verify();material_applies();CHECK(fx::level_resolves==0&&fx::workers==1);});
 add("real shared material reader refuses inaccessible spans",[]{std::array<std::byte,8> b;CHECK(!mr::read_memory(1,b)&&!wr::read_memory(1,b));CHECK(!mr::read_memory(UINTPTR_MAX-1,b)&&!wr::read_memory(UINTPTR_MAX-1,b));});
 unsigned failures=0;
 for(const auto&test:cases){pid_t child=fork();CHECK(child>=0);if(child==0){try{fx::setup();test.run();std::cout<<"PASS "<<test.name<<std::endl;_exit(0);}catch(const std::exception&e){std::cerr<<"FAIL "<<test.name<<": "<<e.what()<<std::endl;_exit(1);}}int status;CHECK(waitpid(child,&status,0)==child);if(!WIFEXITED(status)||WEXITSTATUS(status)!=0){failures++;if(!WIFEXITED(status))std::cerr<<"SIGNAL "<<test.name<<" status="<<status<<std::endl;}}
 std::cout<<"RESULT cases="<<cases.size()<<" failed="<<failures<<std::endl;return failures?1:0;
}
