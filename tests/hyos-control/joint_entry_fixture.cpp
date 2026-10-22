/* The actual production entry TU linked to the actual control TU. No fake start/record APIs. */
#include <dlfcn.h>
#include <vector>
#include <thread>
#include <cassert>
#include <cerrno>
#include <cstdarg>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <unistd.h>
#include <atomic>
static void *joint_symbol(void *, const char *);
namespace joint_read {
std::atomic<void (*)()> callback{nullptr};
std::atomic_uintptr_t address{0};
long call(long number, ...) {
 if(number==SYS_gettid)return ::syscall(SYS_gettid);
 assert(number==SYS_process_vm_readv);
 va_list args;va_start(args,number);int pid=va_arg(args,int);auto *local=va_arg(args,iovec*);int lc=va_arg(args,int);
 auto *remote=va_arg(args,iovec*);int rc=va_arg(args,int);int flags=va_arg(args,int);va_end(args);
 if(reinterpret_cast<uintptr_t>(remote->iov_base)==address.load()){
  auto cb=callback.exchange(nullptr);if(cb)cb();
 }
 return ::syscall(number,pid,local,lc,remote,rc,flags);
}
}
#define syscall joint_read::call
#define dlsym joint_symbol
#include "../../app/src/main/cpp/targets/hyos/hyos_entry.cpp"
#undef dlsym
#undef syscall
extern "C" void hc_hyos_app_entry() {} // Assembly ABI is separately executed by hyos-entry/run_arm64.py.
// Home-only integration must never enter Weather's independent controller.
namespace weather_card_lifetime {
void observe_parent_load(const char *) noexcept {}
void after_specialization(bool weather) noexcept {assert(!weather&&"Home fixture entered Weather lifetime installation");}
}
namespace weather_runtime {
bool start_before_entry(uintptr_t){assert(false&&"Home fixture entered Weather control");return false;}
void observe_snapshot(const char*,void*){assert(false&&"Home fixture observed Weather snapshot");}
bool card_startup_pending() noexcept {return false;}
void on_first_card_snapshot(uintptr_t,uintptr_t) noexcept {assert(false&&"Home fixture entered Weather card");}
void on_card_snapshot_window_missed(uint32_t) noexcept {assert(false&&"Home fixture cancelled Weather card");}
}
namespace joint {
std::vector<std::byte> image(0x65000);
auto entry=hyos_guard::kHomeEntryWords;
const ZygiskNextHyosModule *module=nullptr;
std::atomic<const void *> last_caller{nullptr};
std::vector<std::byte> flutter(0xe8b000),linker(0x1bd000),dart(0x1000);
uintptr_t dart_override=0;
void *snapshot_pointer(){return reinterpret_cast<void*>((dart_override?dart_override:reinterpret_cast<uintptr_t>(dart.data()))+hyos_snapshot_guard::kVmDataVa);}
void *handle=reinterpret_cast<void *>(0x6789000);
void *plain(const char *,int,const void *caller){last_caller=caller;return handle;}
void *extended(const char *name,int flags,const android_dlextinfo *,const void *caller){return plain(name,flags,caller);}
void *symbol(void *,const char *name,const void *caller){
 last_caller=caller;
 if(name&&snapshot_index(name)>=0)return snapshot_pointer();
 return entry.data();
}
int register_module(const void *value){module=static_cast<const ZygiskNextHyosModule *>(value);return ZN_SUCCESS;}
ZygiskNextRuntime runtime{ZN_RUNTIME_HYOS,1,register_module};
const ZygiskNextRuntime *get_runtime(){return &runtime;}
uintptr_t base(){return reinterpret_cast<uintptr_t>(image.data());}
template<class T,size_t N>void put(size_t offset,const std::array<T,N> &data){std::memcpy(image.data()+offset,data.data(),sizeof(data));}
}
static void *joint_symbol(void *,const char *name){
 if(std::strcmp(name,"__loader_dlopen")==0)return reinterpret_cast<void *>(joint::plain);
 if(std::strcmp(name,"__loader_android_dlopen_ext")==0)return reinterpret_cast<void *>(joint::extended);
 if(std::strcmp(name,"__loader_dlsym")==0)return reinterpret_cast<void *>(joint::symbol);
 assert(false);return nullptr;
}
void joint_images(){using namespace hyos_snapshot_guard;
 auto put=[](auto &image,uintptr_t va,const auto &bytes){std::memcpy(image.data()+va,bytes.data(),sizeof(bytes));};
 put(joint::flutter,kFlutterElfVa,kFlutterElf);put(joint::flutter,kFlutterNoteVa,kFlutterNote);
 put(joint::flutter,kSearchMappingVa,kSearchMapping);put(joint::flutter,kSymbolMappingVa,kSymbolMapping);
 put(joint::flutter,kVmFromSettingsVa,kVmFromSettings);put(joint::flutter,kIsolateFromSettingsVa,kIsolateFromSettings);
 put(joint::linker,kLinkerElfVa,kLinkerElf);put(joint::linker,kLinkerNoteVa,kLinkerNote);
 put(joint::linker,kMutexLockVa,kMutexLock);put(joint::linker,kMutexUnlockVa,kMutexUnlock);
 put(joint::linker,kMutexSlowLockVa,kMutexSlowLock);put(joint::linker,kDlsymImplVa,kDlsymImpl);put(joint::linker,kDlopenExtVa,kDlopenExt);
 put(joint::dart,kDartIdentityVa,kDartIdentity);
 const uint32_t state[2]{0x4000,0};std::memcpy(joint::linker.data()+kMutexVa,state,sizeof(state));
}
void joint_setup(ZygiskNextAPI *api,bool home){
 g_api=nullptr;g_dlopen=nullptr;g_android_dlopen=nullptr;g_dlsym=nullptr;
 g_registered=false;g_home_child=false;g_weather_child=false;g_runtime_base=0;g_app_entry=nullptr;joint::module=nullptr;
 g_startup_armed=false;g_linker_base=0;g_search_binding=0;g_snapshot_attempt=0;
 g_search_flutter_base=0;g_search_handle=nullptr;g_search_tid=0;
 joint_read::callback=nullptr;joint_read::address=0;joint::dart_override=0;joint_images();
 std::fill(joint::image.begin(),joint::image.end(),std::byte{0});joint::entry=hyos_guard::kHomeEntryWords;
 joint::put(hyos_guard::kRuntimeNoteVa,hyos_guard::kRuntimeBuildIdNote);
 joint::put(hyos_guard::kDlsymWindowVa,hyos_guard::kDlsymWords);
 joint::put(hyos_guard::kEntryWindowVa,hyos_guard::kEntryCallWords);
 api->getRuntime=joint::get_runtime;module_loaded(nullptr,api);assert(joint::module);
 const char *name=home?"com.miui.home":"com.miui.gallery";
 ZnHyosAppSpecializeArgs args{name,name,"platform"};joint::module->onAppSpecialized(&args);
}
bool joint_load(){
 auto *caller=reinterpret_cast<void *>(joint::base()+hyos_guard::kDlsymReturnVa);
 android_dlextinfo ext{0x1234,27};
 return hooked_dlopen("/apk/libapp_launcher.so",17,caller)==joint::handle
  &&hooked_android_dlopen("libapp.so",29,&ext,caller)==joint::handle&&joint::last_caller==caller;
}
bool joint_lookup(){return hooked_dlsym(joint::handle,"app_entry_point",reinterpret_cast<void *>(joint::base()+hyos_guard::kDlsymReturnVa))==reinterpret_cast<void *>(hc_hyos_app_entry);}
bool joint_prepare(bool correct){return hc_hyos_prepare_app_entry(joint::base()+hyos_guard::kEntryReturnVa+(correct?0:4))==joint::entry.data();}
void joint_mutate_runtime(){joint::image[hyos_guard::kEntryWindowVa]^=std::byte{1};}
bool joint_concurrent_binding(bool two_runtime){
 auto other_entry=hyos_guard::kHomeEntryWords;auto other_image=joint::image;
 std::array<void *,64> targets{},results{};std::array<uintptr_t,64> bases{};std::vector<std::thread> threads;
 for(size_t i=0;i<64;++i){targets[i]=i%2?joint::entry.data():other_entry.data();bases[i]=two_runtime&&i%2?reinterpret_cast<uintptr_t>(other_image.data()):joint::base();}
 for(size_t i=0;i<64;++i)threads.emplace_back([&,i]{results[i]=consider_entry(targets[i],reinterpret_cast<void *>(bases[i]+hyos_guard::kDlsymReturnVa));});
 for(auto &t:threads)t.join();
 auto *winner=g_app_entry.load();const auto runtime=g_runtime_base.load();bool wrapped=false;
 for(size_t i=0;i<64;++i){if(results[i]==reinterpret_cast<void *>(hc_hyos_app_entry)){wrapped=true;if(targets[i]!=winner||bases[i]!=runtime)return false;}else if(results[i]!=targets[i])return false;}
 return wrapped;
}

// The exact image bytes are fixtures. The observer still performs real bounded
// process_vm_readv, verifies every production guard, and calls actual control.
void joint_snapshot_bind(){
 using namespace hyos_snapshot_guard;
 remember_linker_target(joint::linker.data()+kLoaderDlsymVa);
 const auto *caller=joint::flutter.data()+kSearchDlopenReturnVa;
 assert(hooked_dlopen("/apk/libapp.so",1,caller)==joint::handle);
}
bool joint_snapshot_lookup(int index,int mode){
 using namespace hyos_snapshot_guard;
 const char *name=kSnapshotNames[static_cast<size_t>(index)];
 void *handle=mode==2?reinterpret_cast<void*>(0x111):joint::handle;
 const void *caller=joint::flutter.data()+kSnapshotDlsymReturnVa+(mode==1?4:0);
 errno=EAGAIN;
 return hooked_dlsym(handle,name,caller)==joint::snapshot_pointer()&&errno==EAGAIN;
}
void joint_snapshot_corrupt(int kind){
 if(kind==0)joint::dart[0]^=std::byte{1};
 if(kind==1)joint::flutter[hyos_snapshot_guard::kSymbolMappingVa]^=std::byte{1};
 if(kind==2){uint32_t value=0x4005;std::memcpy(joint::linker.data()+hyos_snapshot_guard::kMutexVa,&value,4);}
}
void joint_reenter_during_guard(void (*callback)()){
 joint_read::address=reinterpret_cast<uintptr_t>(joint::linker.data());joint_read::callback=callback;
}

bool joint_snapshot_use_dart(uintptr_t base){
 if(!boundary_matches(base,hyos_snapshot_guard::kDartIdentityVa,hyos_snapshot_guard::kDartIdentity))return false;
 joint::dart_override=base;return true;
}
