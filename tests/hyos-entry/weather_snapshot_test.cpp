/* SPDX-License-Identifier: AGPL-3.0-or-later */
#define HC_WEATHER_SNAPSHOT_TEST_ONLY 1
#include "snapshot_test.cpp"
#include <memory>

namespace card_snap {
using namespace hyos_snapshot_guard;
using namespace hyos_startup_reason;
std::unique_ptr<WeatherImage> weather;
void ready(bool pending=true, bool bind_now=true) {
    io_fixture::failure=io_fixture::reads=0;io_fixture::change_at=0;io_fixture::changed=false;
    io_fixture::callback_at=0;io_fixture::read_callback=nullptr;
    snap::images();
    snap::put(snap::dart,0,weather_guard::kDartBytes0);
    weather=std::make_unique<WeatherImage>();
#ifdef HC_ARM64_FIXTURE_READS
    io_fixture::regions.clear();
    io_fixture::region(test::runtime_bytes.data(),test::runtime_bytes.size());
    io_fixture::region(weather->memory,weather->size);
    io_fixture::region(snap::flutter.data(),snap::flutter.size());
    io_fixture::region(snap::linker.data(),snap::linker.size());
    io_fixture::region(snap::dart.data(),snap::dart.size());
#endif
    module_loaded(nullptr,&test::api);test::specialize("com.miui.weather2","com.miui.weather2");
    assert(test::lookup()==test::wrapper());
    test::weather_card_pending=pending;
    hc_hyos_prepare_app_entry(test::base()+hyos_guard::kEntryReturnVa);
    assert(g_startup_armed==pending&&test::weather_starts==1&&test::starts==0&&test::records==0);
    remember_linker_target(reinterpret_cast<void*>(snap::lb()+kLoaderDlsymVa));assert(g_linker_base==snap::lb());
    if(bind_now){snap::bind();assert(g_search_binding==(pending?2u:0u));}
}
void *lookup(const char *name=kSnapshotNames[0],uintptr_t caller=0,void *handle=nullptr) {
    test::symbol_result=reinterpret_cast<void*>(snap::db()+weather_guard::kDartVmDataVa);
    return hooked_dlsym(handle?handle:test::handle,name,reinterpret_cast<void*>(caller?caller:snap::fb()+kSnapshotDlsymReturnVa));
}
void accepted() {
    assert(test::card_first==1&&test::card_missed==0&&test::card_dart==snap::db()&&test::card_flutter==snap::fb());
    assert(test::first_snapshots==0&&test::missed==0&&test::starts==0&&test::records==0);
}
void refused(uint32_t reason) {
    assert(test::card_first==0&&test::card_missed==1&&test::card_reason==reason&&g_snapshot_attempt==2);
    assert(test::first_snapshots==0&&test::missed==0&&test::starts==0&&test::records==0);
}
void reset_attempt() {g_snapshot_attempt=0;test::card_first=test::card_missed=0;test::card_reason=0;}
template<class F>void run(const char *name,F fn) {test::run(name,[&]{ready();fn();});}
}

int main(){using namespace card_snap;
 std::setvbuf(stdout,nullptr,_IONBF,0);
 run("Weather first VMData dispatches exact two images before legacy observer",[]{
    test::card_callback=[](){assert(test::weather_snapshots==0);};
    assert(lookup()==reinterpret_cast<void*>(snap::db()+weather_guard::kDartVmDataVa));accepted();
    assert(test::weather_snapshots==1&&test::last_handle==test::handle&&test::last_caller==reinterpret_cast<void*>(snap::fb()+kSnapshotDlsymReturnVa));
 });
 test::run("unselected card leaves legacy Weather observer active without arming",[]{
    ready(false);lookup();assert(g_snapshot_attempt==0&&test::card_first==0&&test::card_missed==0&&test::weather_snapshots==1);
 });
 run("failed initial bare query leaves the first fallback available",[]{
    test::symbol_result=nullptr;
    assert(hooked_dlsym(test::handle,kSnapshotNames[0]+1,reinterpret_cast<void*>(snap::fb()+kFirstDlsymReturnVa))==nullptr);
    assert(g_snapshot_attempt==0);lookup();accepted();
 });
 run("successful bare names and all other first snapshots are terminal misses",[]{
    for(const char *name:{kSnapshotNames[0]+1,kSnapshotNames[1],kSnapshotNames[2],kSnapshotNames[3]}) {
        reset_attempt();lookup(name);refused(kUnexpectedFirstSnapshot);lookup();refused(kUnexpectedFirstSnapshot);
    }
 });
 run("unknown symbol keeps first window open",[]{lookup("unknown_symbol");assert(g_snapshot_attempt==0);lookup();accepted();});
 run("success never repeats card install for later snapshots",[]{
    lookup();for(const char *name:kSnapshotNames)lookup(name);accepted();assert(test::weather_snapshots==2);
 });
 run("Weather returns unchanged pointer and errno for successful and denied guards",[]{
    errno=EAGAIN;void *p=lookup();assert(errno==EAGAIN&&p==test::symbol_result);accepted();
    reset_attempt();io_fixture::failure=EINTR;errno=EBUSY;p=lookup();assert(errno==EBUSY&&p==test::symbol_result);
    io_fixture::failure=0;refused(kUnknownLinker);assert(test::weather_snapshots==2);
 });
 run("non-child unarmed and ambiguous child states stay inert",[]{
    g_weather_child=false;lookup();assert(g_snapshot_attempt==0);
    g_weather_child=true;g_home_child=true;lookup();assert(g_snapshot_attempt==0);
    g_home_child=false;g_startup_armed=false;lookup();assert(g_snapshot_attempt==0&&test::card_first==0&&test::card_missed==0);
 });
 run("unknown linker refuses only card and still observes legacy Weather",[]{
    g_linker_base=0;lookup();refused(kUnknownLinker);assert(test::weather_snapshots==1);
 });
 run("wrong original dlsym caller and failed-query return cannot install",[]{
    for(uintptr_t pc:{snap::fb()+kSnapshotDlsymReturnVa+4,snap::fb()+kFirstDlsymReturnVa,uintptr_t{1}}) {
        reset_attempt();lookup(kSnapshotNames[0],pc);refused(kFlutterGuardRejected);
    }
 });
 run("opaque handle mismatch is never treated as an ELF pointer",[]{
    lookup(kSnapshotNames[0],0,reinterpret_cast<void*>(7));refused(kUnboundHandleOrThread);
 });
 run("missing or being-published SearchMapping binding never waits",[]{
    for(unsigned value:{0u,1u}) {reset_attempt();g_search_binding=value;lookup();refused(kUnboundHandleOrThread);}
 });
 run("later load cannot replace immutable SearchMapping handle",[]{
    void *old=test::handle;test::handle=reinterpret_cast<void*>(0x777);snap::bind();assert(g_search_handle==old);
    lookup();refused(kUnboundHandleOrThread);test::handle=old;
 });
 run("exact basename successful load and SearchMapping caller required",[]{
    g_search_binding=0;
    hooked_dlopen("otherlibapp.so",1,reinterpret_cast<void*>(snap::fb()+kSearchDlopenReturnVa));assert(g_search_binding==0);
    hooked_dlopen("libapp.so",1,reinterpret_cast<void*>(snap::fb()+kSearchDlopenReturnVa+4));assert(g_search_binding==0);
    test::failed_load=true;hooked_dlopen("libapp.so",1,reinterpret_cast<void*>(snap::fb()+kSearchDlopenReturnVa));assert(g_search_binding==0);
 });
 test::run("Android dlopen extension preserves args and can bind exact Weather search",[]{
    ready(true,false);android_dlextinfo ext{};
    assert(hooked_android_dlopen("libapp.so",17,&ext,reinterpret_cast<void*>(snap::fb()+kSearchDlopenReturnVa))==test::handle);
    assert(test::last_ext==&ext&&test::last_flags==17&&g_search_binding==2);lookup();accepted();
 });
 run("another thread consumes only a conservative miss",[]{
    std::thread t([](){lookup();});t.join();refused(kUnboundHandleOrThread);lookup();refused(kUnboundHandleOrThread);
 });
 run("different exact Flutter mapping cannot borrow original search binding",[]{
    auto copy=snap::flutter;
#ifdef HC_ARM64_FIXTURE_READS
    io_fixture::region(copy.data(),copy.size());
#endif
    lookup(kSnapshotNames[0],reinterpret_cast<uintptr_t>(copy.data())+kSnapshotDlsymReturnVa);refused(kUnboundHandleOrThread);
 });
 run("actual Home Dart identity is rejected for Weather",[]{
    snap::put(snap::dart,kDartIdentityVa,kDartIdentity);lookup();refused(kDartGuardRejected);
 });
 run("Weather ELF phdr note dynstr and dynsym all participate",[]{
    for(size_t at:{size_t{0},size_t{64},size_t{0x1c8},size_t{0x1e8},size_t{0x270},size_t{0x33f}}) {
        reset_attempt();snap::dart[at]^=std::byte{1};lookup();refused(kDartGuardRejected);snap::dart[at]^=std::byte{1};
    }
 });
 run("null and underflow snapshots cannot dispatch",[]{
    test::symbol_result=nullptr;assert(hooked_dlsym(test::handle,kSnapshotNames[0],reinterpret_cast<void*>(snap::fb()+kSnapshotDlsymReturnVa))==nullptr);
    assert(g_snapshot_attempt==0);test::symbol_result=reinterpret_cast<void*>(1);
    hooked_dlsym(test::handle,kSnapshotNames[0],reinterpret_cast<void*>(snap::fb()+kSnapshotDlsymReturnVa));refused(kDartGuardRejected);
 });
 run("all Flutter guard families apply independently to Weather",[]{
    for(auto at:{kFlutterElfVa,kFlutterNoteVa,kSearchMappingVa,kSymbolMappingVa,kVmFromSettingsVa,kIsolateFromSettingsVa}) {
        reset_attempt();snap::flutter[at]^=std::byte{1};lookup();refused(kFlutterGuardRejected);snap::flutter[at]^=std::byte{1};
    }
 });
 run("all linker guard families apply independently to Weather",[]{
    for(auto at:{kLinkerElfVa,kLinkerNoteVa,kMutexLockVa,kMutexUnlockVa,kMutexSlowLockVa,kDlsymImplVa,kDlopenExtVa}) {
        reset_attempt();snap::linker[at]^=std::byte{1};lookup();refused(kUnknownLinker);snap::linker[at]^=std::byte{1};
    }
 });
 run("recursive or foreign loader ownership denies card window",[]{
    for(uint32_t value:{0x4001u,0x4002u,0x4005u,0x4009u,0x6000u,0x10004000u,0u}) {
        reset_attempt();uint32_t fields[2]{value,static_cast<uint32_t>(::syscall(SYS_gettid))};
        std::memcpy(snap::linker.data()+kMutexVa,fields,sizeof(fields));lookup();refused(kLoaderBusyOrUnreadable);
    }
 });
 run("unlocked word with nonzero owner rejected",[]{
    uint32_t fields[2]{0x4000,7};std::memcpy(snap::linker.data()+kMutexVa,fields,sizeof(fields));lookup();refused(kLoaderBusyOrUnreadable);
 });
 run("two actual loader state observations must agree",[]{
    io_fixture::change_at=snap::lb()+kMutexVa;lookup();refused(kLoaderBusyOrUnreadable);assert(io_fixture::changed);
 });
 run("read denial fails immediately and does not block legacy observer",[]{
    for(int error:{EINTR,EPERM,EFAULT}) {
        reset_attempt();io_fixture::failure=error;io_fixture::reads=0;lookup();assert(io_fixture::reads==1);refused(kUnknownLinker);
    }
    io_fixture::failure=0;assert(test::weather_snapshots==3);
 });
 run("checking reentry cancels before card callback dispatch",[]{
    io_fixture::callback_at=snap::lb()+kLinkerElfVa;io_fixture::read_callback=[](){lookup();};
    lookup();refused(kStartupReentered);assert(test::weather_snapshots==2);
 });
 run("dispatch reentry notifies Weather cancel without Home callback",[]{
    test::card_callback=[](){lookup();};lookup();assert(test::card_first==1&&test::card_missed==1&&test::card_reason==kStartupReentered);
    assert(test::first_snapshots==0&&test::missed==0&&test::weather_snapshots==2);
 });
 run("every Weather identity bit is guarded in actual observer",[]{
    size_t bits=0;
    for(size_t offset=0;offset<weather_guard::kDartBytes0.size();++offset) for(unsigned bit=0;bit<8;++bit) {
        reset_attempt();snap::dart[offset]^=std::byte{static_cast<uint8_t>(1u<<bit)};lookup();refused(kDartGuardRejected);
        snap::dart[offset]^=std::byte{static_cast<uint8_t>(1u<<bit)};++bits;
    }
    assert(bits==6656);std::printf("  rejected %zu Weather identity bit mutations\n",bits);
 });
 run("late ready after unarmed snapshot cannot fabricate SearchMapping",[]{
    g_startup_armed=false;g_search_binding=0;lookup();assert(g_snapshot_attempt==0);
    g_startup_armed=true;lookup();refused(kUnboundHandleOrThread);
 });
 std::printf("%d/%d PASS; Weather card actual observer, controlled loader/controller; NOT_TESTED on device\n",test::count,test::count);
 return 0;
}
