// Separate joint fixture; the source of both controller and installer is included unchanged.
#include <algorithm>
#include <array>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>
/* Actual production control.cpp, with a Binder transport fixture and deterministic thread gates. */
#include <dlfcn.h>
#include <pthread.h>
#include <time.h>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <thread>
#include <vector>
#include <string>
#include <cstdio>
#include <stdexcept>
#include <functional>
static int fixture_clock(clockid_t, timespec *);
static void *fixture_dlsym(void *,const char *);
static int fixture_create(pthread_t *,const pthread_attr_t *,void *(*)(void *),void *);
static int fixture_detach(pthread_t);
#define clock_gettime fixture_clock
#define dlsym fixture_dlsym
#define pthread_create fixture_create
#define pthread_detach fixture_detach
#include "targets/hyos/hyos_control.cpp"
#undef clock_gettime
#undef dlsym
#undef pthread_create
#undef pthread_detach
#define CHECK(x) do{if(!(x))throw std::runtime_error(#x);}while(0)
namespace fixture {
struct Gate {
 std::mutex mutex;std::condition_variable cv;bool enabled=false,entered=false,released=false;
 void arm(){std::lock_guard l(mutex);enabled=true;entered=released=false;}
 void block(){std::unique_lock l(mutex);if(!enabled)return;entered=true;cv.notify_all();cv.wait(l,[&]{return released;});}
 void wait(){std::unique_lock l(mutex);if(!cv.wait_for(l,std::chrono::seconds(5),[&]{return entered;}))throw std::runtime_error("thread gate timeout");}
 void release(){std::lock_guard l(mutex);released=true;enabled=false;cv.notify_all();}
};
Gate query_gate,init_gate,callback_gate,options_gate,execute_gate;
std::atomic_int prepares{},executions{},maintenances{},revocations{},restorations{},post_startups{};
std::atomic_int recents_services{},recents_cancels{},recents_prepares{};
hyos_snapshot::Snapshot recents_frame;bool recents_read=false;
std::atomic_bool fail_prepare_startup{},execute_authorized{},revoke_pending{},force_restore_failure{};
std::atomic<int64_t> sequence{0},server_age{0};
home_startup::Outcome execute_outcome=home_startup::Outcome::Applied;
void (*frame_edit)(hyos_snapshot::Words&)=nullptr;
AIBinder_Class type{"android.view.IWindowManager"},bad_type{"wrong.interface"};AIBinder binder{&type};
std::atomic_int queries{},inits{},workers{},detaches{},allocations{},deletes{},dec_strong{},options_calls{},poll_threads{},watchdogs{},service_refs{},report_retries{};
std::atomic<int64_t> now{1'000'000'000LL};std::atomic_bool bad_clock{},fail_watchdog{};
std::mutex threads_mutex;std::vector<std::thread> threads;
void join_workers(){std::vector<std::thread> copy;{std::lock_guard l(threads_mutex);copy.swap(threads);}for(auto &t:copy)if(t.joinable())t.join();}
std::atomic_bool no_symbol{},no_service{},fail_associate{},fail_prepare{},null_input{},fail_transact{},null_output{},fail_worker{},fail_init{};
std::atomic_int ack{hyos_snapshot::kAck},available{1},flags{1},bytes{188},read_fail{-1};
std::mutex events_mutex;std::vector<std::string> events;std::vector<unsigned> options;
int hook(void *target,void *,void **original){if(original!=nullptr)*original=target;return ZN_SUCCESS;}int unhook(void *){return ZN_SUCCESS;}
ZygiskNextAPI api{};
AParcel *parcel(){++allocations;return new AParcel;}
AIBinder *service(const char *name){CHECK(std::string(name)=="window");if(no_service)return nullptr;++service_refs;return &binder;}
void loaded(const char *name,void *handle){CHECK(handle==nullptr);{std::lock_guard l(events_mutex);events.emplace_back(name);}callback_gate.block();}
void reset(){g_api=nullptr;g_active=false;g_worker_started=false;g_watchdog_started=false;g_watchdog_ready=false;g_flags=0;g_lease_deadline=0;g_poll_completed=false;g_polling.clear();g_pending_libraries=0;g_library_loaded=nullptr;g_initialized=false;g_last_flags=-2;
 g_snapshot.~Cache();new (&g_snapshot) hyos_snapshot::Cache;
 g_received_at=0;g_received_available=false;g_startup_phase=StartupPhase::Pending;
 g_startup_prepared=g_startup_cancelled=g_startup_fault=false;g_startup_reason=0;
 g_startup_requested=g_startup_applied=g_startup_declined=0;g_startup_error=home_startup::Error::None;g_last_startup_phase=StartupPhase::Pending;
 prepares=executions=maintenances=revocations=restorations=post_startups=0;
 recents_services=recents_cancels=recents_prepares=0;recents_frame={};recents_read=false;
 g_last_recents_status={home_recents::Phase::Cold,home_recents::Error::None,false,0};
 fail_prepare_startup=execute_authorized=revoke_pending=force_restore_failure=false;
 sequence=server_age=0;execute_outcome=home_startup::Outcome::Applied;frame_edit=nullptr;
 queries=inits=workers=detaches=allocations=deletes=dec_strong=options_calls=poll_threads=watchdogs=service_refs=report_retries=0;now=1'000'000'000LL;bad_clock=fail_watchdog=false;
 no_symbol=no_service=fail_associate=fail_prepare=null_input=fail_transact=null_output=fail_worker=fail_init=false;
 ack=hyos_snapshot::kAck;available=1;flags=1;bytes=188;read_fail=-1;binder.clazz=&type;api={};api.inlineHook=hook;api.inlineUnhook=unhook;events.clear();options.clear();}
unsigned last_options(){std::lock_guard l(events_mutex);return options.empty()?999:options.back();}
void record(const char *name){hyperceiler_hyos_record_home_library(name);}
void next_poll(){std::thread t([]{poll_flags();});t.join();}
bool boot(const char *name="libapp.so"){record(name);bool ready=hyperceiler_hyos_start_home_control(&api);join_workers();return ready;}
int pass=0,fail=0;
void run(const char *name,std::function<void()> fn){reset();try{fn();join_workers();CHECK(allocations==deletes&&service_refs==0);++pass;std::printf("PASS %s\n",name);}catch(const std::exception &e){++fail;std::printf("FAIL %s: %s\n",name,e.what());}}
}
static void *fixture_dlsym(void *,const char *name){CHECK(std::string(name)=="AServiceManager_checkService");return fixture::no_symbol?nullptr:reinterpret_cast<void *>(fixture::service);}
static int fixture_clock(clockid_t id,timespec *out){CHECK(id==CLOCK_BOOTTIME);if(fixture::bad_clock)return -1;auto n=fixture::now.load();out->tv_sec=n/1'000'000'000LL;out->tv_nsec=n%1'000'000'000LL;return 0;}
static int fixture_create(pthread_t *worker,const pthread_attr_t *,void *(*fn)(void *),void *arg){CHECK(arg==nullptr);++fixture::workers;
 if(fn==lease_watchdog){++fixture::watchdogs;if(fixture::fail_watchdog)return EAGAIN;*worker=1;return 0;}
 CHECK(fn==control_worker);++fixture::poll_threads;if(fixture::fail_worker)return EAGAIN;*worker=2;std::lock_guard l(fixture::threads_mutex);fixture::threads.emplace_back([]{poll_flags();});return 0;}
static int fixture_detach(pthread_t){++fixture::detaches;return 0;}
const AIBinder_Class *AIBinder_getClass(AIBinder *b){return b->clazz;}
const char *AIBinder_Class_getDescriptor(const AIBinder_Class *t){return t->descriptor;}
const AIBinder_Class *AIBinder_Class_define(const char *d,void *(*)(void *),void (*)(void *),binder_status_t (*)(AIBinder *,transaction_code_t,const AParcel *,AParcel *)){CHECK(std::string(d)=="android.view.IWindowManager");return &fixture::type;}
bool AIBinder_associateClass(AIBinder *b,const AIBinder_Class *t){if(fixture::fail_associate)return false;b->clazz=t;return true;}
binder_status_t AIBinder_prepareTransaction(AIBinder *,AParcel **p){if(!fixture::null_input)*p=fixture::parcel();return fixture::fail_prepare?STATUS_BAD_VALUE:STATUS_OK;}
binder_status_t AIBinder_transact(AIBinder *,transaction_code_t code,AParcel **input,AParcel **output,uint32_t opts){
 CHECK(code==hyos_snapshot::kTransaction&&opts==0&&*input!=nullptr);++fixture::queries;
 auto words=hyos_snapshot::unavailable_words();words[0]=fixture::ack;words[1]=fixture::available;
 words[2]=fixture::available==0?0:fixture::flags.load();
 auto put=[&](size_t at,int64_t value){uint64_t bits=static_cast<uint64_t>(value);words[at]=static_cast<int32_t>(bits);words[at+1]=static_cast<int32_t>(bits>>32);};
 put(3,123);put(5,++fixture::sequence);
 if(words[1]==1&&words[2]!=0){
  put(7,fixture::now.load()+5'000'000'000LL-fixture::server_age.load());
  words[9]=1;words[10]=7;words[11]=10;
  const int32_t tweaks[]{5,0,8,5,0,8,5,0,102,0,0,0,0,100,0,100,0};
  for(size_t i=0;i<17;++i)words[30+i]=tweaks[i];
 }
 if(fixture::frame_edit)fixture::frame_edit(words);
 fixture::query_gate.block();
 if(!fixture::null_output){auto *p=fixture::parcel();for(size_t i=0;i<words.size();++i)p->values[i]=words[i];p->bytes=fixture::bytes;p->fail_index=fixture::read_fail;*output=p;}
 AParcel_delete(*input);*input=nullptr;return fixture::fail_transact?STATUS_BAD_VALUE:STATUS_OK;}

void AIBinder_decStrong(AIBinder *p){CHECK(p==&fixture::binder&&fixture::service_refs>0);--fixture::service_refs;++fixture::dec_strong;}
int32_t AParcel_getDataSize(const AParcel *p){return p->bytes;}
binder_status_t AParcel_readInt32(const AParcel *p,int32_t *v){auto *q=const_cast<AParcel *>(p);if(q->pos==q->fail_index)return STATUS_BAD_VALUE;if(q->pos>=47)return STATUS_BAD_VALUE;*v=q->values[q->pos++];return STATUS_OK;}
void AParcel_delete(AParcel *p){++fixture::deletes;delete p;}
extern "C" home_native::LibraryLoaded native_init(const home_native::ApiEntries *e){CHECK(e->version==1&&e->hook_func==fixture::api.inlineHook&&e->unhook_func==fixture::api.inlineUnhook);++fixture::inits;fixture::init_gate.block();return fixture::fail_init?nullptr:fixture::loaded;}
void hyperceiler_home_runtime_options(bool high,bool prestart,bool glass,bool dock,bool report){fixture::options_gate.block();++fixture::options_calls;std::lock_guard l(fixture::events_mutex);fixture::options.push_back((high?2:0)|(prestart?4:0)|(glass?8:0)|(dock?16:0)|(report?32:0));}
void hyperceiler_hyos_retry_home_report(const ZygiskNextAPI *api){CHECK(api==&fixture::api&&hyos_control::feature_enabled(hyos_control::kHideReport));CHECK(g_publish_mutex.try_lock());g_publish_mutex.unlock();++fixture::report_retries;}
void hyperceiler_home_after_startup(){CHECK(startup_finished());++fixture::post_startups;}
namespace home_recents_test {bool atomics();void event(unsigned);}
#define HOME_RECENTS_RUNTIME_TEST 1
#define clock_gettime fixture_clock
#include "targets/home/recents_runtime.cpp"
#undef clock_gettime
#ifndef HC_REAL_STARTUP
namespace home_startup {
 bool prepare_for_child() noexcept {++fixture::prepares;return !fixture::fail_prepare_startup;}
 void request_revoke() noexcept {++fixture::revocations;fixture::revoke_pending=true;}
 Result service_revoke() noexcept {++fixture::restorations;fixture::revoke_pending=false;return {fixture::force_restore_failure?Outcome::TerminalFailure:Outcome::Declined,0,0,0,fixture::force_restore_failure?Error::RollbackFailed:Error::None};}
 Result execute(const Request &request) noexcept {++fixture::executions;fixture::execute_gate.block();
  fixture::execute_authorized=request.still_authorized(request.context);
  return {fixture::execute_authorized?fixture::execute_outcome:Outcome::Declined,PhoneGridDart,fixture::execute_authorized?uint64_t(PhoneGridDart):uint64_t(0),0,fixture::execute_authorized?Error::None:Error::Revoked};}
 Result maintenance(const home_layout::Config *config,bool authorized,AuthorizationCheck check,void *context) noexcept {
  ++fixture::maintenances;bool fresh=authorized&&config!=nullptr&&check(context);
  return {Outcome::Applied,PhoneGridDart,fresh?uint64_t(PhoneGridDart):uint64_t(0),0,fresh?Error::None:Error::Revoked};}
}
#endif
struct Image{
    uint8_t*base;size_t size;
    explicit Image(const char*path){
        std::ifstream f(path,std::ios::binary);std::vector<char> data((std::istreambuf_iterator<char>(f)),{});
        const auto *eh=reinterpret_cast<const Elf64_Ehdr*>(data.data());
        const auto *ph=reinterpret_cast<const Elf64_Phdr*>(data.data()+eh->e_phoff);size=0;
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD)size=std::max(size,size_t(ph[i].p_vaddr+ph[i].p_memsz));
        size=(size+4095)&~size_t(4095);base=static_cast<uint8_t*>(::mmap(nullptr,size,3,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
        if(base==MAP_FAILED)std::abort();
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD)std::memcpy(base+ph[i].p_vaddr,data.data()+ph[i].p_offset,ph[i].p_filesz);
        // Actual checked ELF R_AARCH64_RELATIVE pairs, not an identity bypass.
        for(const auto&p:home_recents_guard::kPointers){uintptr_t value=reinterpret_cast<uintptr_t>(base)+p.target;std::memcpy(base+p.slot,&value,8);}
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD){
            const auto begin=ph[i].p_vaddr&~uintptr_t(4095),end=(ph[i].p_vaddr+ph[i].p_memsz+4095)&~uintptr_t(4095);
            int prot=(ph[i].p_flags&PF_R?PROT_READ:0)|(ph[i].p_flags&PF_W?PROT_WRITE:0)|(ph[i].p_flags&PF_X?PROT_EXEC:0);
            if(::mprotect(base+begin,end-begin,prot))std::abort();
        }

    }
    template<class T>void data(uintptr_t off,T v){std::memcpy(base+off,&v,sizeof(v));}
    void code(uintptr_t off,uint32_t word){
        auto p=(reinterpret_cast<uintptr_t>(base)+off)&~uintptr_t(4095);
        if(::mprotect(reinterpret_cast<void*>(p),4096,7))std::abort();
        std::memcpy(base+off,&word,4);
        if(::mprotect(reinterpret_cast<void*>(p),4096,5))std::abort();
    }
    uint32_t word(){uint32_t v;std::memcpy(&v,base+home_recents_guard::kPatch,4);return v;}
};

static Image*joint_image=nullptr;
static std::string scenario;
static int selected=170,pin_calls=0;
static bool enabled=true;
static bool pin_image(uintptr_t base,uintptr_t entry){++pin_calls;return base==reinterpret_cast<uintptr_t>(joint_image->base)&&entry==base+0x9b544c;}
namespace home_recents_test {
bool atomics(){return true;}
void event(unsigned stage){
    if(stage==0 && scenario=="prephase-entry"){
        bool result=true;
        std::thread loser([&]{result=home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(joint_image->base),hyos_control::read_snapshot,pin_image);});
        loser.join();if(result)std::abort();
    }
    if(stage==4 && scenario=="install-expiry"){
        fixture::now+=5'000'000'000LL;expire_lease();
    }
    if(stage==5 && scenario=="install-revision")fixture::next_poll();
}
}
static void execute(const char*path,const std::string&name){
    scenario=name;fixture::reset();Image image(path);joint_image=&image;
    fixture::frame_edit=[](hyos_snapshot::Words &words){if(words[1]==1 && words[2]!=0){words[44]=enabled?1:0;words[45]=selected;}};
    if(name=="first-query-fails"){
        fixture::no_service=true;fixture::boot();
        CHECK(home_recents::status().phase==home_recents::Phase::Cold);
        fixture::no_service=false;fixture::next_poll();
    }else CHECK(fixture::boot());
    CHECK(fixture::queries==1);
    hyos_snapshot::Snapshot whole;CHECK(hyos_control::read_snapshot(whole));
    CHECK(whole.config.tweaks.animation_recents_enabled && whole.config.tweaks.animation_recents_rate_percent==170);
    CHECK(whole.epoch==123 && whole.revision==1 && whole.valid_until==fixture::now+5'000'000'000LL);
    bool okay=home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(image.base),hyos_control::read_snapshot,pin_image);
    if(name=="prephase-entry"||name=="install-expiry"||name=="install-revision"){
        CHECK(!okay && home_recents::g_lease.read()==1);
        CHECK(image.word()==home_recents_guard::kOriginalWord);
        fixture::now+=1'000'000'000LL;fixture::next_poll();CHECK(home_recents::g_lease.read()==1);
        return;
    }
    CHECK(okay && home_recents::status().phase==home_recents::Phase::Installed);
    CHECK(pin_calls==1 && (home_recents::g_lease.read()&255)==170);
    CHECK(fixture::queries==1); // Preparing does not add a Binder transaction.
    auto before=home_recents::g_lease.read();fixture::now+=1'000'000'000LL;
    if(name=="change")selected=30;
    if(name=="disable")enabled=false;
    if(name=="deny")fixture::flags=0;
    if(name=="watchdog"){
        fixture::now+=5'000'000'000LL;expire_lease();
    }else if(name=="blocked-query-watchdog"){
        fixture::query_gate.arm();std::thread worker([]{poll_flags();});fixture::query_gate.wait();
        fixture::now+=5'000'000'000LL;expire_lease();
        CHECK(home_recents::g_lease.read()==1);
        fixture::query_gate.release();worker.join();
    }else if(name=="repeat-entry"){
        CHECK(!home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(image.base),hyos_control::read_snapshot,pin_image));
    }else fixture::next_poll();
    const bool remains=name=="same-selection"||name=="first-query-fails";
    if(remains){
        CHECK(home_recents::status().phase==home_recents::Phase::Installed);
        CHECK(home_recents::g_lease.read()>before && (home_recents::g_lease.read()&255)==170);
        CHECK(fixture::queries==2);
    }else{
        CHECK(home_recents::g_lease.read()==1 && home_recents::status().phase==home_recents::Phase::Missed);
        enabled=true;selected=170;fixture::flags=1;fixture::next_poll();
        CHECK(home_recents::g_lease.read()==1); // The real service cannot reopen a cached selection.
    }
    CHECK(image.word()==home_recents::g_branch); // No live code rewrite on expiry/disable.
    CHECK(fixture::allocations==fixture::deletes && fixture::service_refs==0);
}
int main(int argc,char**argv){
    if(argc!=2)return 2;
    const std::vector<std::string> cases={"same-selection","first-query-fails","change","disable","deny","watchdog",
        "blocked-query-watchdog","repeat-entry","prephase-entry","install-expiry","install-revision"};
    int bad=0;
    for(const auto&name:cases){
        const auto child=fork();if(child<0)return 3;
        if(child==0){
            try{execute(argv[1],name);fixture::join_workers();std::cout<<"PASS "<<name<<std::endl;_exit(0);}
            catch(const std::exception&e){std::cerr<<"FAIL "<<name<<": "<<e.what()<<std::endl;_exit(1);}
        }
        int status=0;if(waitpid(child,&status,0)!=child||!WIFEXITED(status)||WEXITSTATUS(status))++bad;
    }
    std::cout<<"SCENARIOS="<<cases.size()<<" FAILED="<<bad<<std::endl;return bad?1:0;
}
