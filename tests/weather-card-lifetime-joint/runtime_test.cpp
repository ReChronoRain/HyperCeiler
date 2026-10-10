/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Four complete production runtimes execute. Only external OS/transport and
// legacy level/material identity boundaries are adapted; lifetime GOT is real.
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
#include <fstream>
#include <sstream>
#include <iomanip>
#include <sys/mman.h>
#include <linux/memfd.h>
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
bool card_restore_failure=false,deny_second_write=false;
uintptr_t libc=0;
std::atomic<int> lifetime_forward_calls{0};
std::atomic_bool block_forward=false,forward_entered=false,release_forward=false;
bool lifetime_restore_failure=false,life_fail_writable=false;
std::function<void()> before_life_cas;
std::function<void(void*,int)> card_permission_event;
struct Forward {uintptr_t address;size_t length;int advice;};
std::mutex forward_mutex;
std::vector<Forward> forwards;
int forward_fail_at=0;
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
// Use the real production implementation and actual GOT CAS. Host images are
// anonymous PT_LOAD copies; only the first Flutter mapping pathname is rewritten.
#define WEATHER_CARD_LIFETIME_TEST
#ifndef HC_LIFETIME_SOURCE
#define HC_LIFETIME_SOURCE "targets/weather/weather_card_lifetime.cpp"
#endif
#include HC_LIFETIME_SOURCE
#undef WEATHER_CARD_LIFETIME_TEST
namespace weather_card_lifetime_test {
long page_size() noexcept {return ::sysconf(_SC_PAGESIZE);}
long call(long n,uintptr_t a,uintptr_t b,uintptr_t c,uintptr_t d,uintptr_t e,uintptr_t f) noexcept {
 if(n==SYS_openat && std::strcmp(reinterpret_cast<const char*>(b),"/proc/self/maps")==0){
  std::ifstream maps("/proc/self/maps");std::string line,text;
  while(std::getline(maps,line)){
   std::istringstream fields(line);std::string addresses,perms,offset,device,inode;
   fields>>addresses>>perms>>offset>>device>>inode;
   auto dash=addresses.find('-');
   if(dash!=std::string::npos && std::stoull(addresses.substr(0,dash),nullptr,16)==fx::flutter)
    line=addresses+" "+perms+" "+offset+" "+device+" "+inode+" /system_ext/lib64/libhyper_os_flutter.so";
   text+=line+"\n";
  }
  int fd=static_cast<int>(::syscall(SYS_memfd_create,"hc-real-maps-path-adapter",MFD_CLOEXEC));
  if(fd<0)return fd;
  if(::write(fd,text.data(),text.size())!=ssize_t(text.size())||::lseek(fd,0,SEEK_SET)<0){::close(fd);errno=EIO;return -1;}
  return fd;
 }
 if(n==SYS_mprotect && a==((fx::flutter+weather_card_lifetime_guard::kMadviseSlotVa)&~uintptr_t(4095))
   && ((fx::lifetime_restore_failure && c==PROT_READ) || (fx::life_fail_writable && c==(PROT_READ|PROT_WRITE)))){errno=EPERM;return -1;}
 return ::syscall(n,a,b,c,d,e,f);
}
int forward(uintptr_t original,void*address,size_t length,int advice) noexcept {
 if(original!=fx::libc+weather_card_lifetime_guard::kMadviseVa){errno=ENOSYS;return -1;}
 const int count=++fx::lifetime_forward_calls;
 {std::lock_guard lock(fx::forward_mutex);fx::forwards.push_back({reinterpret_cast<uintptr_t>(address),length,advice});}
 if(fx::block_forward){fx::forward_entered=true;while(!fx::release_forward)std::this_thread::yield();}
 if(count==fx::forward_fail_at){errno=EIO;return -1;}
 return ::madvise(address,length,advice);
}
void before_slot_cas() noexcept {if(fx::before_life_cas)fx::before_life_cas();}
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
 if(fx::deny_second_write && reinterpret_cast<uintptr_t>(p)==((fx::dart+0x3aece0)&~uintptr_t(4095)) && (prot&PROT_WRITE)){errno=EPERM;return -1;}
 if(fx::card_restore_failure&&reinterpret_cast<uintptr_t>(p)==first&&prot==(PROT_READ|PROT_EXEC)){errno=EPERM;return -1;}
 int rc=::mprotect(p,size,prot);if(rc==0&&fx::card_permission_event)fx::card_permission_event(p,prot);return rc;
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
namespace wl=weather_card_lifetime;
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
namespace {
uintptr_t life_slot(){return fx::flutter+weather_card_lifetime_guard::kMadviseSlotVa;}
uintptr_t first_page(){return (fx::dart+0x3a8408)&~uintptr_t(4095);}
uintptr_t last_page(){return (fx::dart+0x3aece0)&~uintptr_t(4095);}
int discard(uintptr_t address,size_t length,int advice=MADV_DONTNEED){
 auto fn=reinterpret_cast<int(*)(void*,size_t,int)>(__atomic_load_n(reinterpret_cast<uintptr_t*>(life_slot()),__ATOMIC_ACQUIRE));
 CHECK(fn==wl::observed_madvise);return fn(reinterpret_cast<void*>(address),length,advice);
}
void specialize(){errno=E2BIG;wl::after_specialization(true);CHECK(errno==E2BIG&&wl::ready()&&!wl::has_residue());CHECK(*reinterpret_cast<uintptr_t*>(life_slot())==reinterpret_cast<uintptr_t>(wl::observed_madvise));}
void change_slot(uintptr_t value){auto p=life_slot()&~uintptr_t(4095);CHECK(::mprotect(reinterpret_cast<void*>(p),4096,PROT_READ|PROT_WRITE)==0);__atomic_store_n(reinterpret_cast<uintptr_t*>(life_slot()),value,__ATOMIC_RELEASE);CHECK(::mprotect(reinterpret_cast<void*>(p),4096,PROT_READ)==0);}
void start_all(){specialize();card_config(2,true);card_installed();level_applies();material_applies();CHECK(wl::intact());}
void branch_words(){CHECK(*reinterpret_cast<uint32_t*>(fx::dart+0x3a8408)==cr::g_branches[0]&&*reinterpret_cast<uint32_t*>(fx::dart+0x3aece0)==cr::g_branches[1]);}
}
int main(int argc,char**argv){
 CHECK(argc==4);fx::dart=map_elf(argv[1]);fx::flutter=map_elf(argv[2]);fx::libc=map_elf(argv[3]);
 change_slot(fx::libc+weather_card_lifetime_guard::kMadviseVa);
 errno=ERANGE;wl::observe_parent_load("/system_ext/lib64/libhyper_os_flutter.so");
 CHECK(errno==ERANGE&&wl::g_capture==wl::Capture::Published&&wl::g_image.parent_pid==getpid());
 CHECK(!wl::ready()&&*reinterpret_cast<uintptr_t*>(life_slot())==fx::libc+weather_card_lifetime_guard::kMadviseVa);
 std::vector<Case> cases;auto add=[&](std::string name,std::function<void()>run){cases.push_back({name,run});};
 add("real parent capture fork admission and three feature installation",[]{start_all();CHECK(fx::workers==1&&fx::transacts==1&&cr::status().owned_mask==3);});
 add("card-only consumes actual lifetime observer and one worker",[]{specialize();card_config();card_installed();CHECK(fx::workers==1&&fx::transacts==1&&fx::level_resolves==0&&fx::material_resolves==0&&wl::intact());original_slots();});
 add("parent remains original and specialization is child-local",[]{specialize();CHECK(wl::g_child_pid==getpid()&&wl::g_image.parent_pid==getppid());wl::after_specialization(false);CHECK(wl::ready());});
 add("nonweather child does not seize original import",[]{wl::after_specialization(false);CHECK(!wl::ready()&&!wl::has_residue());CHECK(*reinterpret_cast<uintptr_t*>(life_slot())==fx::libc+weather_card_lifetime_guard::kMadviseVa);card_config(2,true);CHECK(start()&&!wr::card_startup_pending());verify();level_applies();material_applies();});
 add("grandchild cannot reuse a different parent capture",[]{auto child=fork();CHECK(child>=0);if(child==0){wl::after_specialization(true);_exit(!wl::ready()&&!wl::has_residue()?0:1);}int status;CHECK(waitpid(child,&status,0)==child&&WIFEXITED(status)&&WEXITSTATUS(status)==0);});
 add("specialization with second real thread declines card",[]{std::atomic_bool running=false,done=false;std::thread other([&]{running=true;while(!done)std::this_thread::yield();});fx::until([&]{return running.load();});wl::after_specialization(true);done=true;other.join();CHECK(!wl::ready()&&!wl::has_residue());card_config(2,true);CHECK(start()&&!wr::card_startup_pending());verify();level_applies();material_applies();});
 add("foreign GOT before specialization remains untouched",[]{change_slot(0x12345000);wl::after_specialization(true);CHECK(!wl::ready()&&!wl::has_residue()&&*reinterpret_cast<uintptr_t*>(life_slot())==0x12345000);card_config(2,true);CHECK(start()&&!wr::card_startup_pending());verify();level_applies();material_applies();});
 add("clean early mprotect rejection leaves other two features usable",[]{fx::life_fail_writable=true;wl::after_specialization(true);CHECK(!wl::ready()&&!wl::has_residue());card_config(2,true);CHECK(start()&&!wr::card_startup_pending());verify();level_applies();material_applies();});
 add("early permission residue stops all consumers and worker",[]{fx::lifetime_restore_failure=true;wl::after_specialization(true);CHECK(wl::has_residue()&&!wl::ready());card_config(2,true);CHECK(!start()&&wr::g_phase==wr::Phase::Residue&&fx::workers==0&&fx::transacts==0);original_slots();});
 add("foreign CAS ownership creates explicit residue and global deny",[]{fx::before_life_cas=[] {__atomic_store_n(reinterpret_cast<uintptr_t*>(life_slot()),uintptr_t(0x12345000),__ATOMIC_RELEASE);};wl::after_specialization(true);CHECK(wl::has_residue()&&*reinterpret_cast<uintptr_t*>(life_slot())==0x12345000);card_config(2,true);CHECK(!start()&&fx::workers==0);});
 add("actual hooked import skips each protected epilogue page",[]{start_all();errno=EDOM;CHECK(discard(first_page(),4096)==0&&errno==EDOM);CHECK(discard(last_page(),4096)==0&&fx::lifetime_forward_calls==0);branch_words();CHECK(cr::image_state(3)&&wl::intact());});
 add("actual import splits covering discard into three original calls",[]{start_all();const auto begin=first_page()-4096,end=last_page()+8192;CHECK(discard(begin,end-begin)==0);CHECK(fx::lifetime_forward_calls==3&&fx::forwards.size()==3);CHECK(fx::forwards[0].address==begin&&fx::forwards[0].length==4096);CHECK(fx::forwards[1].address==first_page()+4096&&fx::forwards[1].length==last_page()-first_page()-4096);CHECK(fx::forwards[2].address==last_page()+4096&&fx::forwards[2].length==4096);branch_words();CHECK(*reinterpret_cast<uint64_t*>(begin)==0&&*reinterpret_cast<uint64_t*>(last_page()+4096)==0);});
 add("unrelated madvise advice executes original once without splitting",[]{start_all();CHECK(discard(first_page(),last_page()-first_page()+4096,MADV_NORMAL)==0);CHECK(fx::lifetime_forward_calls==1&&fx::forwards[0].advice==MADV_NORMAL);branch_words();});
 add("unaligned original argument and errno are preserved",[]{start_all();CHECK(discard(first_page()+1,4096)==-1&&errno==EINVAL);CHECK(fx::lifetime_forward_calls==1&&fx::forwards[0].address==first_page()+1);branch_words();});
 add("original segment failure exits with its errno",[]{start_all();fx::forward_fail_at=1;CHECK(discard(first_page()-4096,4096*2)==-1&&errno==EIO&&fx::lifetime_forward_calls==1);branch_words();});
 add("busy original reader prevents both code patches",[]{specialize();card_config(2,true);CHECK(start());fx::block_forward=true;std::thread reader([]{CHECK(discard(first_page(),4096,MADV_NORMAL)==0);});fx::until([]{return fx::forward_entered.load();});wr::on_first_card_snapshot(fx::dart,fx::flutter);bool refused=cr::status().error==cr::Error::LifetimeRejected&&cr::status().owned_mask==0&&cr::image_state(0);fx::release_forward=true;reader.join();CHECK(refused&&!wl::intact());verify();level_applies();material_applies();});
 add("post-protect in-flight reader sees immutable protected pages",[]{start_all();fx::block_forward=true;std::thread reader([]{CHECK(discard(first_page()-4096,8192)==0);});fx::until([]{return fx::forward_entered.load();});CHECK(wl::g_readers.load()&1);CHECK(!wl::release_pages());fx::release_forward=true;reader.join();CHECK(fx::lifetime_forward_calls==1);branch_words();});
 add("second point write denial rolls first back and releases pages",[]{specialize();card_config(2,true);CHECK(start());fx::card_permission_event=[](void*,int){ }; /* separate failure switch below */ fx::deny_second_write=true;wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().phase==cr::Phase::Rejected&&cr::status().owned_mask==0&&cr::image_state(0)&&wl::g_readers==0);verify();level_applies();material_applies();CHECK(discard(first_page(),4096)==0&&fx::lifetime_forward_calls==1&&*reinterpret_cast<uint32_t*>(fx::dart+0x3a8408)==0);});
 add("cancel during first write restores code and never publishes lease",[]{specialize();card_config(2,true);CHECK(start());bool cancelled=false;fx::card_permission_event=[&](void*p,int prot){if(!cancelled&&reinterpret_cast<uintptr_t>(p)==first_page()&&prot==(PROT_READ|PROT_WRITE|PROT_EXEC)){cancelled=true;cr::cancel();}};wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cancelled&&cr::g_lease.terminal()&&cr::status().owned_mask==0&&cr::image_state(0)&&wl::g_readers==0);});
 add("rollback with active protected reader retains immutable page descriptor",[]{specialize();card_config(2,true);CHECK(start());std::thread reader;bool launched=false;fx::block_forward=true;fx::deny_second_write=true;fx::card_permission_event=[&](void*p,int prot){if(!launched&&reinterpret_cast<uintptr_t>(p)==first_page()&&prot==(PROT_READ|PROT_WRITE|PROT_EXEC)){launched=true;reader=std::thread([]{CHECK(discard(first_page()-4096,8192,MADV_DONTNEED)==0);});fx::until([]{return fx::forward_entered.load();});}};wr::on_first_card_snapshot(fx::dart,fx::flutter);const bool retained=cr::status().owned_mask==0&&cr::image_state(0)&&(wl::g_readers.load()&1);fx::release_forward=true;reader.join();CHECK(launched&&retained&&wl::g_readers==1&&fx::lifetime_forward_calls==1);CHECK(*reinterpret_cast<uint32_t*>(fx::dart+0x3a8408)==weather_card_guard::kOriginalEpilogueWord);CHECK(discard(first_page(),4096)==0&&fx::lifetime_forward_calls==1);});
 add("foreign word prevents rollback ownership claim and keeps page protection",[]{specialize();card_config(2,true);CHECK(start());bool replaced=false;fx::card_permission_event=[&](void*p,int prot){if(!replaced&&reinterpret_cast<uintptr_t>(p)==first_page()&&prot==(PROT_READ|PROT_EXEC)){replaced=true;CHECK(::mprotect(p,4096,PROT_READ|PROT_WRITE|PROT_EXEC)==0);__atomic_store_n(reinterpret_cast<uint32_t*>(fx::dart+0x3a8408),uint32_t(0xd503201f),__ATOMIC_RELEASE);CHECK(::mprotect(p,4096,PROT_READ|PROT_EXEC)==0);}};wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(replaced&&cr::status().phase==cr::Phase::Residue&&wr::g_phase==wr::Phase::Residue&&(wl::g_readers.load()&1)&&cr::g_lease.terminal());CHECK(discard(first_page(),4096)==0&&fx::lifetime_forward_calls==0);CHECK(*reinterpret_cast<uint32_t*>(fx::dart+0x3a8408)==0xd503201f);CHECK(!material()&&level()==fx::original_bytes);});
 add("parallel discard readers preserve both owned code words",[]{start_all();std::vector<std::thread> readers;for(unsigned i=0;i<8;++i)readers.emplace_back([i]{for(unsigned j=0;j<32;++j)CHECK(discard(i%2?first_page():last_page(),4096)==0);});for(auto&t:readers)t.join();CHECK(wl::g_readers==1&&fx::lifetime_forward_calls==0);branch_words();CHECK(wl::intact());});
 add("card rollback RX residue closes shared level and material",[]{specialize();card_config(2,true);CHECK(start());fx::card_restore_failure=true;wr::on_first_card_snapshot(fx::dart,fx::flutter);CHECK(cr::status().phase==cr::Phase::Residue&&wr::g_phase==wr::Phase::Residue&&cr::g_lease.terminal()&&(wl::g_readers.load()&1));CHECK(!material()&&level()==fx::original_bytes&&wr::g_applied==0&&mr::g_applied==0);});
 add("lease expiry retains code and prevents reclamation",[]{start_all();fx::time_ns+=5'000'000'000LL;wr::poll();CHECK(cr::g_lease.read()==0);CHECK(discard(first_page(),4096)==0&&fx::lifetime_forward_calls==0);branch_words();CHECK(!material()&&level()==fx::original_bytes);});
 add("explicit inactive frame revokes lease without losing page protection",[]{start_all();fx::reply=weather_snapshot::Words{};fx::reply[0]=weather_snapshot::kAck;fx::reply[1]=1;fx::reply[9]=3;wr::poll();CHECK(cr::g_lease.read()==0&&wl::intact());CHECK(discard(last_page(),4096)==0&&fx::lifetime_forward_calls==0);branch_words();});
 add("foreign observer removal terminally closes card on poll",[]{start_all();change_slot(fx::libc+weather_card_lifetime_guard::kMadviseVa);wr::poll();CHECK(cr::g_lease.terminal()&&!wl::ready()&&cr::image_state(3));});
 add("wrong later Dart export denies all policies but protects remaining code",[]{start_all();wr::observe_snapshot("_kDartIsolateSnapshotInstructions",reinterpret_cast<void*>(fx::dart+weather_guard::kDartIsolateInstructionsVa+4));CHECK(wr::g_dart_rejected&&cr::g_lease.terminal());CHECK(discard(first_page(),4096)==0&&fx::lifetime_forward_calls==0);branch_words();CHECK(!material()&&level()==fx::original_bytes);});
 add("repeated startup cannot add a second worker or renew stale lease",[]{start_all();fx::time_ns+=5'000'000'000LL;CHECK(start());wr::poll();CHECK(fx::workers==1&&cr::g_lease.read()==0);CHECK(!wl::release_pages());branch_words();});
 add("default card does not publish page descriptor",[]{specialize();both();level_applies();material_applies();CHECK(cr::status().phase==cr::Phase::Cold&&wl::g_readers==0&&fx::workers==1);});
 unsigned failures=0;
 for(const auto&test:cases){pid_t child=fork();CHECK(child>=0);if(child==0){alarm(15);try{fx::setup();test.run();std::cout<<"PASS "<<test.name<<std::endl;_exit(0);}catch(const std::exception&e){std::cerr<<"FAIL "<<test.name<<": "<<e.what()<<std::endl;_exit(1);}}int status;CHECK(waitpid(child,&status,0)==child);if(!WIFEXITED(status)||WEXITSTATUS(status)!=0){failures++;if(!WIFEXITED(status))std::cerr<<"SIGNAL "<<test.name<<" status="<<status<<std::endl;}}
 CHECK(wl::g_capture==wl::Capture::Published&&!wl::g_closed&&wl::g_phase==wl::Phase::Cold&&wl::g_readers==0&&*reinterpret_cast<uintptr_t*>(life_slot())==fx::libc+weather_card_lifetime_guard::kMadviseVa);
 std::cout<<"RESULT cases="<<cases.size()<<" failed="<<failures<<std::endl;return failures?1:0;
}
