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
#include "../../app/src/main/cpp/targets/hyos/hyos_control.cpp"
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
namespace home_recents {
bool prepare_before_entry(uintptr_t,ReadFresh,PinImage) noexcept {++fixture::recents_prepares;return false;}
void service() noexcept {
 // If this aborts, the production integration moved the callback under its lock.
 if(!g_publish_mutex.try_lock())std::abort();
 g_publish_mutex.unlock();
 ++fixture::recents_services;
 fixture::recents_read=hyos_control::read_snapshot(fixture::recents_frame);
}
void cancel() noexcept {++fixture::recents_cancels;}
Status status() noexcept {return {Phase::Cold,Error::None,false,0};}
}
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
int main(){using namespace fixture;
run("Recents service reads whole published HCS without extra Binder queries",[]{
 frame_edit=[](hyos_snapshot::Words &words){words[44]=1;words[45]=175;};
 CHECK(boot()&&recents_services==1&&queries==1&&recents_read);
 CHECK(recents_frame.available&&recents_frame.epoch==123&&recents_frame.revision==1);
 CHECK(recents_frame.config.tweaks.animation_recents_enabled&&recents_frame.config.tweaks.animation_recents_rate_percent==175);
 CHECK(recents_frame.valid_until==now+5'000'000'000LL);
 next_poll();CHECK(recents_services==2&&queries==2&&recents_frame.revision==2);
});
run("Recents cancellation follows disabled and watchdog expiry publications",[]{
 CHECK(boot()&&recents_cancels==0);flags=0;next_poll();CHECK(recents_cancels==1);
 flags=1;next_poll();int calls=recents_services;now+=5'000'000'000LL;expire_lease();
 CHECK(recents_cancels==2&&recents_services==calls);
});
run("Recents unavailable frame cannot retain old authorization",[]{
 CHECK(boot()&&recents_read&&recents_frame.available);no_service=true;next_poll();
 CHECK(recents_cancels==1&&recents_services==2&&(!recents_read||!recents_frame.available));
});
run("report disabled does no image retry",[]{g_startup_phase=StartupPhase::Missed;flags=31;boot();next_poll();CHECK(report_retries==0);});
run("report enabled retries every worker poll without new threads",[]{g_startup_phase=StartupPhase::Missed;flags=33;boot();CHECK(report_retries==1&&inits==1);next_poll();CHECK(report_retries==2&&workers==2);});
run("report poll independent of native_init success or library events",[]{g_startup_phase=StartupPhase::Missed;flags=33;fail_init=true;CHECK(hyperceiler_hyos_start_home_control(&api));join_workers();CHECK(report_retries==1&&events.empty()&&inits==1);});
run("disabled unavailable expired report snapshots never retry",[]{g_startup_phase=StartupPhase::Missed;flags=33;boot();flags=0;next_poll();flags=33;available=0;next_poll();CHECK(report_retries==1);available=1;init_gate.arm();g_initialized=false;std::thread poll([]{poll_flags();});init_gate.wait();now+=5'000'000'001LL;expire_lease();init_gate.release();poll.join();CHECK(report_retries==1);});
run("report retry follows delivery of pending libraries",[]{g_startup_phase=StartupPhase::Missed;flags=33;callback_gate.arm();record("libapp.so");CHECK(!hyperceiler_hyos_start_home_control(&api));callback_gate.wait();bool deferred=report_retries==0;callback_gate.release();join_workers();CHECK(deferred&&report_retries==1);});
run("library recording before start does no Binder, init or threads",[]{record("libapp.so");hyperceiler_hyos_after_home_load(&api,"/path/libapp_launcher.so",nullptr);CHECK(queries==0&&inits==0&&workers==0&&g_pending_libraries==3&&!hyos_control::active());CHECK(boot()&&events.size()==2);});
run("first valid poll runs only on scheduled worker and initializes once",[]{CHECK(boot());CHECK(inits==1&&workers==2&&watchdogs==1&&poll_threads==1&&hyos_control::allowed());CHECK(hyperceiler_hyos_start_home_control(&api));CHECK(inits==1&&workers==2);});
run("first Binder stall bounds application wait and preserves queued images",[]{query_gate.arm();auto start=std::chrono::steady_clock::now();bool ready=hyperceiler_hyos_start_home_control(&api);auto elapsed=std::chrono::steady_clock::now()-start;query_gate.wait();record("libapp_launcher.so");record("libapp.so");bool no_init=inits==0;query_gate.release();join_workers();CHECK(!ready&&elapsed<std::chrono::seconds(1)&&no_init&&events.size()==2);});
run("recording while native initialization is busy preserves both libraries",[]{init_gate.arm();CHECK(!hyperceiler_hyos_start_home_control(&api));init_gate.wait();record("libapp.so");record("libapp_launcher.so");init_gate.release();join_workers();CHECK(inits==1&&events.size()==2);});
run("event arriving after drain remains pending until next worker poll",[]{callback_gate.arm();record("libapp_launcher.so");CHECK(!hyperceiler_hyos_start_home_control(&api));callback_gate.wait();record("libapp.so");callback_gate.release();join_workers();CHECK(events.size()==1&&g_pending_libraries==2);next_poll();CHECK(events.size()==2&&g_pending_libraries==0);});
run("unavailable never initializes; later worker recovery drains events",[]{available=0;CHECK(!boot());CHECK(inits==0&&!hyos_control::allowed()&&g_pending_libraries==2&&last_options()==0);available=1;flags=63;next_poll();CHECK(inits==1&&last_options()==62&&events.size()==1);});
run("explicit disabled is a ready snapshot without activation",[]{flags=0;CHECK(boot());CHECK(inits==0&&!hyos_control::allowed()&&last_options()==0);flags=1;next_poll();CHECK(inits==1&&events.size()==1);});
run("disable outage recovery never reinitializes native state",[]{flags=63;boot();flags=0;next_poll();CHECK(!hyos_control::allowed()&&last_options()==0);record("libapp_launcher.so");available=0;next_poll();CHECK(g_pending_libraries==1&&events.size()==1);available=1;flags=35;next_poll();CHECK(inits==1&&last_options()==34&&events.size()==2);});
run("native initialization failure retains events and retries",[]{fail_init=true;boot();CHECK(inits==1&&!g_initialized&&g_pending_libraries==2);fail_init=false;next_poll();CHECK(inits==2&&events.size()==1);});
run("lease expires while Binder is stuck; getter rejects immediately and watchdog clears raw flags",[]{flags=63;boot();query_gate.arm();std::thread t([]{poll_flags();});query_gate.wait();now+=5'000'000'001LL;bool stale=hyos_control::allowed();expire_lease();unsigned options=last_options();query_gate.release();t.join();CHECK(!stale&&options==0&&!hyos_control::allowed());});
run("late reply beyond five seconds is not granted a new lease",[]{query_gate.arm();CHECK(!hyperceiler_hyos_start_home_control(&api));query_gate.wait();now+=5'000'000'001LL;query_gate.release();join_workers();CHECK(inits==0&&!hyos_control::allowed()&&last_options()==0);next_poll();CHECK(inits==1&&hyos_control::allowed());});
run("clock unavailable fails closed even with valid reply",[]{bad_clock=true;CHECK(!boot());CHECK(inits==0&&!hyos_control::allowed());bad_clock=false;next_poll();CHECK(inits==1);});
run("watchdog rechecks latest publication and cannot revoke refreshed lease",[]{flags=3;boot();now+=4'900'000'000LL;flags=33;next_poll();now+=200'000'000LL;expire_lease();CHECK(hyos_control::feature_enabled(32)&&last_options()==32);});
run("watchdog racing publication keeps refreshed authorization",[]{flags=3;boot();now+=6'000'000'000LL;flags=33;options_gate.arm();std::thread publisher([]{poll_flags();});options_gate.wait();std::thread expiry([]{expire_lease();});options_gate.release();publisher.join();expiry.join();CHECK(hyos_control::feature_enabled(32)&&last_options()==32);});
run("lease expiring during init preserves pending events for recovery",[]{init_gate.arm();record("libapp.so");CHECK(!hyperceiler_hyos_start_home_control(&api));init_gate.wait();now+=5'000'000'001LL;expire_lease();init_gate.release();join_workers();CHECK(events.empty()&&g_pending_libraries==2&&last_options()==0);next_poll();CHECK(inits==1&&events.size()==1);});
run("initial wait remains bounded while options publication is stalled",[]{options_gate.arm();auto start=std::chrono::steady_clock::now();bool ready=hyperceiler_hyos_start_home_control(&api);auto elapsed=std::chrono::steady_clock::now()-start;options_gate.wait();options_gate.release();join_workers();CHECK(!ready&&elapsed<std::chrono::seconds(1));});
run("expired lease clears raw flags once without repeated mutation",[]{flags=63;boot();now+=5'000'000'000LL;expire_lease();int calls=options_calls;expire_lease();CHECK(last_options()==0&&options_calls==calls&&!hyos_control::allowed());});
run("watchdog failure prevents worker launch and authorization",[]{fail_watchdog=true;CHECK(!boot());CHECK(inits==0&&poll_threads==0&&!g_watchdog_ready);fail_watchdog=false;CHECK(hyperceiler_hyos_start_home_control(&api));join_workers();CHECK(inits==1&&watchdogs==2&&poll_threads==1);});
run("poll worker creation can retry without duplicating watchdog",[]{fail_worker=true;CHECK(!boot());CHECK(!g_worker_started&&watchdogs==1);fail_worker=false;CHECK(hyperceiler_hyos_start_home_control(&api));join_workers();CHECK(inits==1&&watchdogs==1&&poll_threads==2);});
run("repeated starts never spawn replacement for stalled worker",[]{query_gate.arm();CHECK(!hyperceiler_hyos_start_home_control(&api));query_gate.wait();CHECK(!hyperceiler_hyos_start_home_control(&api));query_gate.release();join_workers();CHECK(workers==2&&poll_threads==1&&watchdogs==1);});
run("null or incomplete API keeps active companion closed",[]{CHECK(!hyperceiler_hyos_start_home_control(nullptr));CHECK(hyos_control::active()&&!hyos_control::allowed()&&workers==0);api.inlineHook=nullptr;CHECK(!hyperceiler_hyos_start_home_control(&api));CHECK(workers==0);});
run("different API publication rejected",[]{boot();auto other=api;CHECK(!hyperceiler_hyos_start_home_control(&other));CHECK(workers==2);});
run("record accepts only exact known basenames and null is inert",[]{record(nullptr);record("libapp.so.bak");record("otherlibapp.so");CHECK(g_pending_libraries==0&&workers==0);record("/data/a.apk!/lib/libapp.so");CHECK(g_pending_libraries==2);});
run("unknown and zero feature mask cannot pass lease check",[]{flags=63;boot();CHECK(!hyos_control::feature_enabled(0)&&!hyos_control::feature_enabled(64)&&hyos_control::feature_enabled(2|32));});
for(int bits=0;bits<=63;++bits)run(("wire flags "+std::to_string(bits)).c_str(),[bits]{flags=bits;boot();bool valid=bits==0||((bits&1)&&(!(bits&8)||(bits&16)));CHECK((inits==1)==(valid&&(bits&1)));CHECK(hyos_control::allowed()==(valid&&(bits&1)));CHECK(last_options()==(valid&&(bits&1)?static_cast<unsigned>(bits&62):0));});
for(int failure=0;failure<15;++failure)run(("transport or malformed reply "+std::to_string(failure)).c_str(),[failure]{switch(failure){case 0:no_symbol=true;break;case 1:no_service=true;break;case 2:binder.clazz=&bad_type;break;case 3:binder.clazz=nullptr;fail_associate=true;break;case 4:fail_prepare=true;break;case 5:null_input=true;break;case 6:fail_transact=true;break;case 7:null_output=true;break;case 8:bytes=16;break;case 9:bytes=8;break;case 10:ack=0;break;case 11:available=2;break;case 12:read_fail=0;break;case 13:read_fail=1;break;case 14:read_fail=2;break;}CHECK(!boot());CHECK(inits==0&&last_options()==0&&!hyos_control::allowed()&&g_pending_libraries==2);});
run("unassociated remote Binder uses exact descriptor",[]{binder.clazz=nullptr;CHECK(boot());CHECK(inits==1&&events.size()==1);});
run("unknown flag bit refused",[]{flags=65;CHECK(!boot());CHECK(inits==0);});
run("first snapshot alone owns installation and later notifications cannot retry",[]{flags=63;CHECK(boot());CHECK(executions==0&&post_startups==0&&report_retries==0);hyos_control::on_first_home_snapshot(0x100000);CHECK(executions==1&&execute_authorized&&g_startup_phase==StartupPhase::Applied);hyos_control::on_first_home_snapshot(0x100000);next_poll();CHECK(executions==1&&maintenances==1&&post_startups==1&&report_retries==1);});
run("window arriving before preparation is permanently missed",[]{hyos_control::on_first_home_snapshot(0x100000);CHECK(g_startup_phase==StartupPhase::Missed);CHECK(boot());hyos_control::on_first_home_snapshot(0x100000);next_poll();CHECK(executions==0&&maintenances==0&&g_startup_prepared);});
run("window arriving while native initialization blocks never waits or retries",[]{init_gate.arm();CHECK(!hyperceiler_hyos_start_home_control(&api));init_gate.wait();auto start=std::chrono::steady_clock::now();hyos_control::on_first_home_snapshot(0x100000);auto elapsed=std::chrono::steady_clock::now()-start;init_gate.release();join_workers();CHECK(elapsed<std::chrono::milliseconds(100)&&g_startup_phase==StartupPhase::Missed&&executions==0);next_poll();CHECK(executions==0);});
run("disabled first snapshot cannot activate after a later preference enable",[]{flags=0;CHECK(boot());hyos_control::on_first_home_snapshot(0x100000);flags=63;next_poll();CHECK(g_startup_phase==StartupPhase::Missed&&executions==0&&maintenances==0);});
run("expired first snapshot is declined before backend side effects",[]{CHECK(boot());now+=5'000'000'000LL;hyos_control::on_first_home_snapshot(0x100000);CHECK(g_startup_phase==StartupPhase::Missed&&executions==0);});
run("malformed snapshot clears the layout with authorization",[]{CHECK(boot());home_layout::Config value;CHECK(hyos_control::layout_config(value)&&value.grid_enabled&&value.cell_x==7);ack=0;next_poll();CHECK(!hyos_control::layout_config(value)&&!value.grid_enabled&&value.cell_x==4);});
run("preparation failure never exposes startup writer",[]{fail_prepare_startup=true;CHECK(boot());hyos_control::on_first_home_snapshot(0x100000);CHECK(executions==0&&g_startup_error==home_startup::Error::NotPrepared);});
run("reentry during installation cancels authorization and permanently seals phase",[]{CHECK(boot());execute_gate.arm();std::thread t([]{hyos_control::on_first_home_snapshot(0x100000);});execute_gate.wait();hyos_control::on_home_snapshot_window_missed(7);CHECK(g_startup_phase==StartupPhase::Cancelling&&!startup_finished());next_poll();CHECK(post_startups==0&&report_retries==0&&maintenances==0);execute_gate.release();t.join();CHECK(!execute_authorized&&g_startup_cancelled&&g_startup_phase==StartupPhase::TerminalFailure);next_poll();hyos_control::on_first_home_snapshot(0x100000);CHECK(executions==1&&maintenances==0);});
run("publication changing during installation invalidates captured revision",[]{CHECK(boot());execute_gate.arm();std::thread t([]{hyos_control::on_first_home_snapshot(0x100000);});execute_gate.wait();next_poll();execute_gate.release();t.join();CHECK(!execute_authorized&&g_startup_phase==StartupPhase::Declined);});
run("expiry during installation invalidates authorization despite blocked Binder",[]{CHECK(boot());execute_gate.arm();std::thread t([]{hyos_control::on_first_home_snapshot(0x100000);});execute_gate.wait();query_gate.arm();std::thread poll([]{poll_flags();});query_gate.wait();now+=5'000'000'000LL;expire_lease();service_startup_revoke();execute_gate.release();t.join();query_gate.release();poll.join();CHECK(!execute_authorized&&restorations>0&&g_startup_phase==StartupPhase::Declined);});
run("service restore failure remains terminal across later publications",[]{CHECK(boot());hyos_control::on_first_home_snapshot(0x100000);force_restore_failure=true;home_startup::request_revoke();service_startup_revoke();CHECK(g_startup_fault&&g_startup_phase==StartupPhase::TerminalFailure);force_restore_failure=false;next_poll();CHECK(g_startup_phase==StartupPhase::TerminalFailure&&maintenances==0);});
run("late first response retains server deadline rather than receive time",[]{server_age=4'000'000'000LL;CHECK(boot());CHECK(g_lease_deadline==2'000'000'000LL);now+=1'000'000'000LL;CHECK(!hyos_control::allowed());CHECK(!hyperceiler_hyos_start_home_control(&api));});
run("missed guard notification cannot reset a completed phase",[]{CHECK(boot());hyos_control::on_home_snapshot_window_missed(3);hyos_control::on_home_snapshot_window_missed(4);hyos_control::on_first_home_snapshot(0x100000);CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==3&&executions==0);});
run("concurrent first callbacks run one installation",[]{CHECK(boot());std::vector<std::thread> calls;for(int i=0;i<32;++i)calls.emplace_back([]{hyos_control::on_first_home_snapshot(0x100000);});for(auto &t:calls)t.join();CHECK(executions==1&&g_startup_phase==StartupPhase::Applied);});
run("BOOTTIME rollback expires raw options at watchdog",[]{flags=63;CHECK(boot());now=1;expire_lease();CHECK(last_options()==0&&!hyos_control::allowed());});
for(int i=0;i<47;++i)run(("each combined frame read failure revokes "+std::to_string(i)).c_str(),[i]{CHECK(boot());read_fail=i;next_poll();CHECK(!hyos_control::allowed()&&last_options()==0);});
run("busy preparation retries before the first snapshot",[]{fail_prepare_startup=true;CHECK(boot());CHECK(prepares==1&&!g_startup_prepared);fail_prepare_startup=false;next_poll();CHECK(prepares==2&&g_startup_prepared);hyos_control::on_first_home_snapshot(0x100000);CHECK(executions==1&&g_startup_phase==StartupPhase::Applied);});
run("late Applied publication cannot overwrite an already cancelled writer",[]{g_startup_phase=StartupPhase::Installing;hyos_control::on_home_snapshot_window_missed(7);CHECK(g_startup_phase==StartupPhase::Cancelling);finish_startup(StartupPhase::Applied);CHECK(g_startup_phase==StartupPhase::TerminalFailure);});
run("fault during an in-flight writer defers postphase until it exits",[]{g_startup_phase=StartupPhase::Installing;force_restore_failure=true;service_startup_revoke();CHECK(g_startup_fault&&g_startup_phase==StartupPhase::Cancelling&&!startup_finished());finish_startup(StartupPhase::Applied);CHECK(g_startup_phase==StartupPhase::TerminalFailure);});
#ifdef HC_REAL_CONFIG
run("actual Hyos config route reads only combined cache and clears disabled layout",[]{CHECK(boot());home_layout::Config value;int count=queries;CHECK(home_layout::query_config(value)&&value.grid_enabled&&value.cell_x==7&&queries==count);flags=0;next_poll();count=queries;CHECK(home_layout::query_config(value)&&!value.grid_enabled&&value.cell_x==4&&queries==count);});
run("actual Hyos config route never retains stale layout after outage",[]{CHECK(boot());no_service=true;next_poll();home_layout::Config value;value.grid_enabled=true;value.cell_x=9;int count=queries;CHECK(home_layout::query_config(value)&&!value.grid_enabled&&value.cell_x==4&&queries==count);});
#endif
std::printf("RESULT passes=%d failures=%d; actual production control and lease functions; worker scheduling/Binder/BOOTTIME clock and startup patch backend are deterministic fixtures\n",pass,fail);return fail?1:0;}
