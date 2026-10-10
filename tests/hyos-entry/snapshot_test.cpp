/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Compile the real production entry. Controlled loader results are not an
// Android injection test. Normal host reads use actual process_vm_readv.
#include <sys/syscall.h>
#include <sys/uio.h>
#include <sys/mman.h>
#include <unistd.h>
#include <cstdarg>
#include <cerrno>
#include <cstring>
#include <thread>
#include <atomic>
#include <algorithm>
#include <vector>
namespace io_fixture {
int failure=0, reads=0;
#ifdef HC_ARM64_FIXTURE_READS
// qemu-user does not implement the guest self process_vm_readv used here.
// The ARM control-flow fixture exposes ONLY declared readable byte regions.
// Host execution separately tests the real syscall and inaccessible pages.
struct Region { uintptr_t start; size_t size; };
std::vector<Region> regions;
void region(void *p,size_t n){regions.push_back({reinterpret_cast<uintptr_t>(p),n});}
ssize_t vm_read(pid_t,const iovec *local,unsigned long lc,const iovec *remote,unsigned long rc,unsigned long flags){
 if(lc!=1||rc!=1||flags!=0){errno=EINVAL;return -1;}
 const auto at=reinterpret_cast<uintptr_t>(remote->iov_base);
 for(auto r:regions)if(at>=r.start&&at-r.start<r.size){
  const size_t n=std::min({r.size-(at-r.start),remote->iov_len,local->iov_len});
  std::memcpy(local->iov_base,remote->iov_base,n);return static_cast<ssize_t>(n);
 }
 errno=EFAULT;return -1;
}
#endif
uintptr_t change_at=0, callback_at=0;
void (*read_callback)()=nullptr;
bool changed=false;
long invoke(long number, ...) {
 if(number==SYS_gettid)return ::syscall(SYS_gettid);
 if(number!=SYS_process_vm_readv){errno=ENOSYS;return -1;}
 va_list args;va_start(args,number);
 const int pid=va_arg(args,int);auto *local=va_arg(args,iovec*);const int lc=va_arg(args,int);
 auto *remote=va_arg(args,iovec*);const int rc=va_arg(args,int);const int flags=va_arg(args,int);va_end(args);
 ++reads;
 if(read_callback&&callback_at==reinterpret_cast<uintptr_t>(remote->iov_base)){
  auto callback=read_callback;read_callback=nullptr;callback();
 }
 if(failure){errno=failure;return -1;}
#ifdef HC_ARM64_FIXTURE_READS
 const long result=vm_read(pid,local,lc,remote,rc,flags);
#else
 const long result=::syscall(number,pid,local,lc,remote,rc,flags);
#endif
 if(change_at==reinterpret_cast<uintptr_t>(remote->iov_base)&&!changed){
  changed=true;static_cast<uint32_t*>(remote->iov_base)[1]=123;
 }
 return result;
}
}
#ifdef HC_ARM64_FIXTURE_READS
#define process_vm_readv io_fixture::vm_read
#endif
#define syscall io_fixture::invoke
#define main legacy_entry_main
#include "entry_test.cpp"
#undef main
#undef syscall
#ifdef HC_ARM64_FIXTURE_READS
#undef process_vm_readv
#endif

namespace snap {
using namespace hyos_snapshot_guard;
using namespace hyos_startup_reason;
std::vector<std::byte> flutter(0xe8b000),linker(0x1bd000),dart(0x1000);
uintptr_t fb(){return reinterpret_cast<uintptr_t>(flutter.data());}
uintptr_t lb(){return reinterpret_cast<uintptr_t>(linker.data());}
uintptr_t db(){return reinterpret_cast<uintptr_t>(dart.data());}
template<class T,size_t N>void put(std::vector<std::byte>& out,size_t at,const std::array<T,N>& raw){
 assert(at+sizeof(raw)<=out.size());std::memcpy(out.data()+at,raw.data(),sizeof(raw));
}
void images(){
 put(flutter,kFlutterElfVa,kFlutterElf);put(flutter,kFlutterNoteVa,kFlutterNote);
 put(flutter,kSearchMappingVa,kSearchMapping);put(flutter,kSymbolMappingVa,kSymbolMapping);
 put(flutter,kVmFromSettingsVa,kVmFromSettings);put(flutter,kIsolateFromSettingsVa,kIsolateFromSettings);
 put(linker,kLinkerElfVa,kLinkerElf);put(linker,kLinkerNoteVa,kLinkerNote);
 put(linker,kMutexLockVa,kMutexLock);put(linker,kMutexUnlockVa,kMutexUnlock);
 put(linker,kMutexSlowLockVa,kMutexSlowLock);put(linker,kDlsymImplVa,kDlsymImpl);put(linker,kDlopenExtVa,kDlopenExt);
 put(dart,kDartIdentityVa,kDartIdentity);
 const uint32_t mutex[2]{0x4000,0};std::memcpy(linker.data()+kMutexVa,mutex,sizeof(mutex));
}
void base_setup(){
 io_fixture::failure=io_fixture::reads=0;io_fixture::change_at=0;io_fixture::changed=false;io_fixture::callback_at=0;io_fixture::read_callback=nullptr;
 images();
#ifdef HC_ARM64_FIXTURE_READS
 io_fixture::regions.clear();
 io_fixture::region(test::runtime_bytes.data(),test::runtime_bytes.size());
 io_fixture::region(test::entry_words.data(),sizeof(test::entry_words));
 io_fixture::region(flutter.data(),flutter.size());io_fixture::region(linker.data(),linker.size());io_fixture::region(dart.data(),dart.size());
#endif
 test::setup();
#ifdef HC_ARM64_FIXTURE_READS
 // Static executable globals may lie below the ROM's app_entry VA. Put the
 // fixture body in the high-address mapped Flutter buffer, outside its guards.
 std::memcpy(flutter.data()+0x1000,test::entry_words.data(),sizeof(test::entry_words));
 test::symbol_result=flutter.data()+0x1000;
#endif
 assert(test::lookup()==test::wrapper());
 hc_hyos_prepare_app_entry(test::base()+hyos_guard::kEntryReturnVa);
 assert(g_startup_armed&&test::starts==1);
 remember_linker_target(reinterpret_cast<void*>(lb()+kLoaderDlsymVa));assert(g_linker_base==lb());
}
void bind(){
 void *result=hooked_dlopen("/data/app/example/base.apk!/lib/arm64-v8a/libapp.so",1,
                           reinterpret_cast<void*>(fb()+kSearchDlopenReturnVa));
 assert(result==test::handle);
}
void ready(){base_setup();bind();assert(g_search_binding==2);}
void *lookup(const char *name=kSnapshotNames[0],uintptr_t caller=0,void *handle=nullptr){
 test::symbol_result=reinterpret_cast<void*>(db()+kVmDataVa);
 return hooked_dlsym(handle?handle:test::handle,name,reinterpret_cast<void*>(caller?caller:fb()+kSnapshotDlsymReturnVa));
}
void accepted(){assert(test::first_snapshots==1&&test::missed==0&&test::snapshot_base==db());}
void refused(uint32_t reason){assert(test::first_snapshots==0&&test::missed==1&&test::missed_reason==reason&&g_snapshot_attempt==2);}
void reset_attempt(){g_snapshot_attempt=0;test::first_snapshots=test::missed=0;test::missed_reason=0;}
template<class F>void run(const char *name,F fn){test::run(name,[&]{ready();fn();});}
}

#ifndef HC_WEATHER_SNAPSHOT_TEST_ONLY
int main(){using namespace snap;
 run("first VMData fallback returns original pointer and synchronously observes base",[]{
  void *p=lookup();assert(p==reinterpret_cast<void*>(db()+kVmDataVa));accepted();
  assert(test::last_handle==test::handle&&test::last_caller==reinterpret_cast<void*>(fb()+kSnapshotDlsymReturnVa));
 });
 run("failed initial bare query does not consume fallback window",[]{
  test::symbol_result=nullptr;
  assert(hooked_dlsym(test::handle,kSnapshotNames[0]+1,reinterpret_cast<void*>(fb()+kFirstDlsymReturnVa))==nullptr);
  assert(g_snapshot_attempt==0);lookup();accepted();
 });
 run("observer preserves errno on success and read failure",[]{
  errno=EAGAIN;lookup();assert(errno==EAGAIN);reset_attempt();io_fixture::failure=EINTR;
  errno=EBUSY;lookup();assert(errno==EBUSY);io_fixture::failure=0;refused(kUnknownLinker);
 });
 run("terminal success never repeats prime on later snapshots",[]{lookup();for(auto *name:kSnapshotNames)lookup(name);accepted();});
 run("first instructions or isolate snapshot permanently closes startup window",[]{
  for(size_t i=1;i<kSnapshotNames.size();++i){reset_attempt();lookup(kSnapshotNames[i]);refused(kUnexpectedFirstSnapshot);lookup();refused(kUnexpectedFirstSnapshot);}
 });
 run("unexpected successful bare symbol refused",[]{lookup(kSnapshotNames[0]+1);refused(kUnexpectedFirstSnapshot);});
 run("unknown symbols do not consume window",[]{lookup("random_symbol");assert(g_snapshot_attempt==0);lookup();accepted();});
 run("nonHome or unarmed path stays inert",[]{g_home_child=false;lookup();assert(g_snapshot_attempt==0);g_home_child=true;g_startup_armed=false;lookup();assert(g_snapshot_attempt==0);});
 run("unknown linker is declined without changing original pointer",[]{g_linker_base=0;assert(lookup()==test::symbol_result);refused(kUnknownLinker);});
 run("linker base is immutable across later resolver mappings",[]{
  auto copy=linker;
#ifdef HC_ARM64_FIXTURE_READS
  io_fixture::region(copy.data(),copy.size());
#endif
  remember_linker_target(copy.data()+kLoaderDlsymVa);assert(g_linker_base==lb());
 });
 run("invalid linker resolver pointer cannot bind",[]{g_linker_base=0;remember_linker_target(reinterpret_cast<void*>(1));remember_linker_target(reinterpret_cast<void*>(0x200000));assert(g_linker_base==0);});
 run("wrong dlsym caller or first-query return refused",[]{
  for(auto caller:{fb()+kSnapshotDlsymReturnVa+4,fb()+kFirstDlsymReturnVa,uintptr_t{1}}){reset_attempt();lookup(kSnapshotNames[0],caller);refused(kFlutterGuardRejected);}
 });
 run("opaque handle mismatch cannot use real target pointer",[]{lookup(kSnapshotNames[0],0,reinterpret_cast<void*>(7));refused(kUnboundHandleOrThread);});
 run("no SearchMapping binding refuses snapshot",[]{g_search_binding=0;lookup();refused(kUnboundHandleOrThread);});
 run("unpublished SearchMapping binding never waits",[]{g_search_binding=1;lookup();refused(kUnboundHandleOrThread);});
 run("binding cannot be replaced by later other handle",[]{
  void *old=test::handle;test::handle=reinterpret_cast<void*>(0x777);bind();assert(g_search_handle==old);lookup();refused(kUnboundHandleOrThread);test::handle=old;
 });
 run("exact dlopen caller and basename required for binding",[]{
  g_search_binding=0;hooked_dlopen("otherlibapp.so",1,reinterpret_cast<void*>(fb()+kSearchDlopenReturnVa));assert(g_search_binding==0);
  hooked_dlopen("libapp.so",1,reinterpret_cast<void*>(fb()+kSearchDlopenReturnVa+4));assert(g_search_binding==0);
  test::failed_load=true;hooked_dlopen("libapp.so",1,reinterpret_cast<void*>(fb()+kSearchDlopenReturnVa));assert(g_search_binding==0);
 });
 run("another thread cannot consume bound window",[]{std::thread other([]{lookup();});other.join();refused(kUnboundHandleOrThread);});
 run("different verified Flutter mapping cannot borrow binding",[]{
  auto copy=flutter;
#ifdef HC_ARM64_FIXTURE_READS
  io_fixture::region(copy.data(),copy.size());
#endif
  lookup(kSnapshotNames[0],reinterpret_cast<uintptr_t>(copy.data())+kSnapshotDlsymReturnVa);refused(kUnboundHandleOrThread);
 });
 run("Dart header program headers note dynstr and dynsym mutations refused",[]{
  for(size_t at:{size_t{0},size_t{64},size_t{0x1c8},size_t{0x1e8},size_t{0x270},size_t{0x33f}}){
   reset_attempt();dart[at]^=std::byte{1};lookup();refused(kDartGuardRejected);dart[at]^=std::byte{1};
  }
 });
 run("null or underflow snapshot target cannot prime",[]{
  test::symbol_result=nullptr;assert(hooked_dlsym(test::handle,kSnapshotNames[0],reinterpret_cast<void*>(fb()+kSnapshotDlsymReturnVa))==nullptr);assert(g_snapshot_attempt==0);
  test::symbol_result=reinterpret_cast<void*>(1);hooked_dlsym(test::handle,kSnapshotNames[0],reinterpret_cast<void*>(fb()+kSnapshotDlsymReturnVa));refused(kDartGuardRejected);
 });
 run("every guarded Flutter method participates in observer rejection",[]{
  for(auto at:{kFlutterElfVa,kFlutterNoteVa,kSearchMappingVa,kSymbolMappingVa,kVmFromSettingsVa,kIsolateFromSettingsVa}){
   reset_attempt();flutter[at]^=std::byte{1};lookup();refused(kFlutterGuardRejected);flutter[at]^=std::byte{1};
  }
 });
 run("every guarded linker method participates in observer rejection",[]{
  for(auto at:{kLinkerElfVa,kLinkerNoteVa,kMutexLockVa,kMutexUnlockVa,kMutexSlowLockVa,kDlsymImplVa,kDlopenExtVa}){
   reset_attempt();linker[at]^=std::byte{1};lookup();refused(kUnknownLinker);linker[at]^=std::byte{1};
  }
 });
 run("outer recursive own mutex and other owner held mutex refused",[]{
  for(auto state:{0x4001u,0x4002u,0x4005u,0x4009u,0x6000u,0x10004000u,0u}){
   reset_attempt();uint32_t fields[2]{state,static_cast<uint32_t>(::syscall(SYS_gettid))};std::memcpy(linker.data()+kMutexVa,fields,sizeof(fields));lookup();refused(kLoaderBusyOrUnreadable);
  }
 });
 run("free state with nonzero owner refused",[]{uint32_t fields[2]{0x4000,7};std::memcpy(linker.data()+kMutexVa,fields,sizeof(fields));lookup();refused(kLoaderBusyOrUnreadable);});
 run("two mutex reads must be equal",[]{io_fixture::change_at=lb()+kMutexVa;lookup();refused(kLoaderBusyOrUnreadable);assert(io_fixture::changed);});
 run("read denial and EINTR return immediately without proc fallback",[]{
  for(int error:{EINTR,EPERM,EFAULT}){reset_attempt();io_fixture::reads=0;io_fixture::failure=error;lookup();assert(io_fixture::reads==1);refused(kUnknownLinker);}io_fixture::failure=0;
 });
 run("partial reads fail rather than retry or cross inaccessible page",[]{
  const long page=sysconf(_SC_PAGESIZE);void *area=mmap(nullptr,page*2,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);assert(area!=MAP_FAILED);
  assert(mprotect(static_cast<char*>(area)+page,page,PROT_NONE)==0);char out[8]{};io_fixture::reads=0;
#ifdef HC_ARM64_FIXTURE_READS
  io_fixture::region(area,static_cast<size_t>(page));
#endif
  assert(!boundary_read(reinterpret_cast<uintptr_t>(area)+page-4,out,8)&&io_fixture::reads==1);
  assert(munmap(area,page*2)==0);
 });
 run("bounds refuse null overflow or empty read before syscall",[]{
  char out[4]{};io_fixture::reads=0;assert(!boundary_read(0,out,4));assert(!boundary_read(UINTPTR_MAX-1,out,4));assert(!boundary_read(db(),out,0));assert(io_fixture::reads==0);
 });
 run("reentry during identity reads cancels before controller dispatch",[]{
  io_fixture::callback_at=lb()+kLinkerElfVa;io_fixture::read_callback=[](){lookup();};
  lookup();refused(kStartupReentered);
 });
 run("recursive observer only reports reentry without second prime",[]{
  test::snapshot_callback=[](){lookup();};lookup();assert(test::first_snapshots==1&&test::missed==1&&test::missed_reason==kStartupReentered);
 });
 run("all complete guard bytes reject each single bit mutation",[]{
  size_t count=0;
  auto check=[&](auto &image,uintptr_t va,const auto &expected){
   const uintptr_t base=reinterpret_cast<uintptr_t>(image.data());
   for(size_t offset=0;offset<sizeof(expected);++offset)for(unsigned bit=0;bit<8;++bit){
    image[va+offset]^=std::byte{static_cast<unsigned char>(1u<<bit)};
    assert(!boundary_matches(base,va,expected));image[va+offset]^=std::byte{static_cast<unsigned char>(1u<<bit)};++count;
   }
  };
  check(flutter,kFlutterElfVa,kFlutterElf);check(flutter,kFlutterNoteVa,kFlutterNote);
  check(flutter,kSearchMappingVa,kSearchMapping);check(flutter,kSymbolMappingVa,kSymbolMapping);
  check(flutter,kVmFromSettingsVa,kVmFromSettings);check(flutter,kIsolateFromSettingsVa,kIsolateFromSettings);
  check(linker,kLinkerElfVa,kLinkerElf);check(linker,kLinkerNoteVa,kLinkerNote);
  check(linker,kMutexLockVa,kMutexLock);check(linker,kMutexUnlockVa,kMutexUnlock);check(linker,kMutexSlowLockVa,kMutexSlowLock);
  check(linker,kDlsymImplVa,kDlsymImpl);check(linker,kDlopenExtVa,kDlopenExt);check(dart,kDartIdentityVa,kDartIdentity);
  std::printf("  rejected %zu exact guard bit mutations\n",count);
 });
 std::printf("%d/%d PASS; actual production observer with controlled loader results; no runtime/device proof\n",test::count,test::count);
 return 0;
}
#endif
