from pathlib import Path
import re,sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W;H=R/'app/src/main/cpp/targets/home';D=W/'handover/icon-scale-boot-20261013';out=D/('host-'+R.name);out.mkdir(exist_ok=True)
assert out.resolve().is_relative_to(D.resolve()) and D.resolve().is_relative_to(W.resolve())
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
(out/'android').mkdir(exist_ok=True);(out/'android/log.h').write_text('#pragma once\n#define ANDROID_LOG_INFO 4\n#define ANDROID_LOG_WARN 5\n#define ANDROID_LOG_ERROR 6\nint __android_log_print(int,const char*,...);\n')
def func(s,name):
 a=s.index(name);b=s.index('{',a);depth=1;i=b+1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 return s[a:i]
s=(H/'home_layout_config.cpp').read_text(encoding='utf8');a=s.index('constexpr char kCacheMagic');b=s.index('void serialize_config(',a);const=s[a:b]
for native,host in [('/data/user_de/0/com.miui.home/files',str(out/'de/files').replace('\\','/')),('/data/user/0/com.miui.home/files',str(out/'ce/files').replace('\\','/'))]:const=const.replace(native,host)
f=s[b:s.index('\n} // namespace',b)].replace('::mkdir(', 'test_mkdir(').replace('std::rename(', 'test_rename(')
cache=r'''#include "home_layout_config.h"
#include <cstring>
#include <cstdio>
#include <cerrno>
#include <fstream>
#include <filesystem>
#include <iostream>
using namespace home_layout;namespace fs=std::filesystem;
// Windows host adapters model Android mkdir and POSIX replacement-rename.
int test_rename(const char*a,const char*b){std::error_code e;fs::remove(b,e);e.clear();fs::rename(a,b,e);return e?-1:0;}
int test_mkdir(const char*p,int){std::error_code e;if(fs::create_directory(p,e))return 0;if(fs::is_directory(p)){errno=EEXIST;return -1;}errno=EACCES;return -1;}
'''+const+f+r'''
int main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v){++failed;std::cout<<"FAIL_CACHE="<<checks<<"\n";}};
 fs::path root=ROOT;fs::remove_all(root/"de");fs::remove_all(root/"ce");fs::remove_all(root/"ce-hidden");fs::create_directories(root/"de");fs::create_directories(root/"ce/files");
 Config cfg;cfg.cell_x=4;cfg.cell_y=6;cfg.tweaks.icon_scale_enabled=true;cfg.tweaks.icon_scale_code=0x63;Config got;
 ok(write_config_cache(cfg));ok(fs::exists(root/"de/files/layout_config_cache.bin"));
 fs::rename(root/"ce",root/"ce-hidden");ok(read_config_cache(got)&&got.tweaks.icon_scale_enabled&&got.tweaks.icon_scale_code==0x63);fs::rename(root/"ce-hidden",root/"ce");
 auto legacy=root/"ce/files/layout_config_cache.bin";std::vector<uint8_t> old;serialize_config(cfg,old);{std::ofstream f(legacy,std::ios::binary);f.write((char*)old.data(),old.size());}
 cfg.tweaks.icon_scale_enabled=false;ok(write_config_cache(cfg));ok(read_config_cache(got)&&!got.tweaks.icon_scale_enabled);
 {std::ofstream f(root/"de/files/layout_config_cache.bin",std::ios::binary);f<<"corrupt";}ok(!read_config_cache(got));
 fs::remove(root/"de/files/layout_config_cache.bin");ok(read_config_cache(got)&&got.tweaks.icon_scale_enabled);
 cfg.tweaks.icon_scale_enabled=true;cfg.tweaks.icon_scale_code=0x73;ok(write_config_cache(cfg));ok(read_config_cache(got)&&got.tweaks.icon_scale_code==0x73);
 fs::rename(root/"ce",root/"ce-hidden");ok(read_config_cache(got)&&got.tweaks.icon_scale_enabled&&got.tweaks.icon_scale_code==0x73);
 std::cout<<"ICON_BOOT_CACHE="<<checks<<" checks; failed="<<failed<<"; real cache I/O, CE unavailable, DE precedence/disable/corruption/legacy\n";return failed?1:0;}
'''.replace('ROOT',json.dumps(str(out).replace('\\','/')))
# Prefix has no escape sequences except quotes in include directives.
cache=cache.replace('\\"','"')
(out/'cache.cpp').write_text(cache,encoding='utf8');total=0
for cmd in [[str(zig),'c++','-std=c++20','-I'+str(out),'-I'+str(H),'-I'+str(W/'app/src/main/cpp/targets/home'),'-I'+str(W/'app/src/main/cpp'),str(out/'cache.cpp'),'-o',str(out/'cache.exe')],[str(out/'cache.exe')]]:
 p=subprocess.run(cmd,capture_output=True,text=True);print(p.stdout+p.stderr,end='')
 if p.returncode and cmd[0]==str(zig):sys.exit(p.returncode)
 total|=p.returncode
hooks=(H/'home_layout_hooks.cpp').read_text(encoding='utf8');entry=(H/'tweaks/entry.cpp').read_text(encoding='utf8');native=(H/'hyperceiler_home.cpp').read_text(encoding='utf8');new='bool PrimeLayoutConfig(' in entry;render=(H/'home_icon_render.h').exists()
prefix=r'''#include "home_layout_config.h"
#include "tweaks/blob.h"
#include <atomic>
#include <mutex>
#include <thread>
#include <chrono>
#include <iostream>
#include <string_view>
#include <vector>
#define __android_log_print(...) 0
constexpr const char*kTag="test";
home_layout::Config cached;bool snapshot=true,patched=false,applied_before_layout=false;unsigned discover_calls=0;
namespace home_layout_engine {
struct Image{};
bool FindImageByName(const char*,Image*){return true;}bool FindImageFromApkMaps(const char*,const char*,Image*){return true;}bool FindImageFromMappedApks(const char*,Image*){return true;}uint64_t ImageIdentity(const Image&){return 1;}uint64_t NowMs(){return 0;}
bool cacheHit=true;struct SymbolIndex{static SymbolIndex&Instance(){static SymbolIndex s;return s;}bool TryLoadCached(const Image&,uint64_t){return cacheHit;}bool EnsureLoaded(const Image&,int){return true;}};
struct State{Image image;uint64_t imageId=0;bool identityTried=false;Config config;bool configLoaded=false,configFromBaked=false,repatchNeeded=false,packTried=false,cacheTried=false,imageFound=true;std::vector<int> applied;};State g_state;std::mutex g_workMutex;std::atomic_bool g_imageKnown{true};
void TryDiscoverImage(){++discover_calls;}bool RefreshConfigLocked(State*s,bool,bool){return s->configLoaded;}void SettleLocked(State*s,bool){patched=s->configLoaded&&s->config.Has(kFeatureIconSize);}
'''
prefix+=func(entry,'void FastPatchOnLibraryLoad()')+'\n'
if new:prefix+=func(entry,'bool PrimeLayoutConfig(')+'\n'
else:prefix+='bool PrimeLayoutConfig(const Config&){return false;}\n'
prefix+='void HomeLayoutEnginePrepareForLauncherChild(){}\n'+func(entry,'bool HomeLayoutEnginePrimeCachedSymbols()')+'\nvoid HomeLayoutEngineOnLibraryLoaded(const char*){FastPatchOnLibraryLoad();}void StartHomeLayoutEngine(){}\n}\n'
if not new:
 prefix=prefix.replace('void HomeLayoutEngineOnLibraryLoaded(const char*){FastPatchOnLibraryLoad();}', 'void HomeLayoutEngineOnLibraryLoaded(const char*){}')
prefix+='namespace home_layout {bool query_bootstrap_config(Config&c){if(!snapshot)return false;c=cached;return true;}}\n'
if new:prefix+=func(hooks,'home_layout_engine::Config make_layout_engine_config(')+'\n'+func(hooks,'void prime_home_layout_engine()')+'\n'
else:prefix+='void prime_home_layout_engine(){}\n'
prefix+='''bool is_launcher_process(){return true;}bool is_hyos_spawner_process(){return false;}bool is_library(std::string_view a,const char*b){return a.ends_with(b);}void install_property_hook(){}void home_layout_prepare_for_launcher_child(){}void* g_hook_function=nullptr;void*g_unhook_function=nullptr;
std::atomic_bool workerReady{false},releaseWorker{false};std::thread simulatedWorker;
void prime_home_layout_probe(void*,void*){}
void prime_home_layout_knobs(void*,void*){simulatedWorker=std::thread([]{std::lock_guard<std::mutex> lock(home_layout_engine::g_workMutex);workerReady=true;while(!releaseWorker.load())std::this_thread::yield();});while(!workerReady.load())std::this_thread::yield();}
void start_home_layout_hooks(const char*,void*,void*){applied_before_layout=patched;}
'''+func(native,'void on_library_loaded(')+r'''
int main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v){++failed;std::cout<<"FAIL_STARTUP="<<checks<<"\n";}};
 cached.tweaks.icon_scale_enabled=true;cached.tweaks.icon_scale_code=0x63;on_library_loaded("libapp.so",nullptr);releaseWorker=true;simulatedWorker.join();ok(applied_before_layout);ok(home_layout_engine::g_state.config.iconScaleCode==0x63);
 home_layout_engine::g_state.configLoaded=true;home_layout_engine::g_state.config.enabled.clear();cached.tweaks.icon_scale_enabled=true;prime_home_layout_engine();ok(!home_layout_engine::g_state.config.Has(home_layout_engine::kFeatureIconSize));
 home_layout_engine::g_state.config.iconScaleCode=0x73;prime_home_layout_engine();ok(home_layout_engine::g_state.config.iconScaleCode==0x73);
 home_layout_engine::g_state.configLoaded=false;snapshot=false;prime_home_layout_engine();ok(!home_layout_engine::g_state.configLoaded);
'''
if new:prefix+=r'''
 snapshot=true;home_layout_engine::g_state.configLoaded=false;cached.tweaks.icon_scale_enabled=false;workerReady=false;releaseWorker=false;on_library_loaded("libapp.so",nullptr);releaseWorker=true;simulatedWorker.join();ok(!applied_before_layout&&home_layout_engine::g_state.configLoaded);
 home_layout_engine::g_workMutex.lock();bool ready=true;auto t0=std::chrono::steady_clock::now();std::thread t([&]{ready=home_layout_engine::PrimeLayoutConfig(home_layout_engine::Config{});home_layout_engine::FastPatchOnLibraryLoad();});t.join();home_layout_engine::g_workMutex.unlock();ok(!ready);ok(std::chrono::steady_clock::now()-t0<std::chrono::milliseconds(250));
 ok(discover_calls==0);
 // Also cover the bounded first-index-load path rather than only disk cache hit.
 home_layout_engine::cacheHit=false;home_layout_engine::g_state.configLoaded=false;cached.tweaks.icon_scale_enabled=true;prime_home_layout_engine();patched=false;ok(home_layout_engine::HomeLayoutEnginePrimeCachedSymbols()&&patched);
'''
else:prefix+='ok(false);ok(false);ok(false);ok(false);ok(false);\n'
if render:
 prefix=prefix.replace('ok(applied_before_layout);','ok(!applied_before_layout);')
 prefix=prefix.replace('HomeLayoutEnginePrimeCachedSymbols()&&patched','HomeLayoutEnginePrimeCachedSymbols()&&!patched')
 print('ICON_BOOT_ROUTE=scalar render banks; legacy calculator planner intentionally disabled; actual rendering covered by icon-render ARM/device tests')
prefix+='std::cout<<"ICON_BOOT_STARTUP="<<checks<<" checks; failed="<<failed<<"; extracted production callback/prime/fast-path, freshness/busy/no-snapshot/disable\\n";return failed?1:0;}\n';prefix=prefix.replace('\\"','"');(out/'startup.cpp').write_text(prefix,encoding='utf8')
for cmd in [[str(zig),'c++','-std=c++20','-I'+str(out),'-I'+str(H),'-I'+str(W/'app/src/main/cpp/targets/home'),'-I'+str(W/'app/src/main/cpp'),str(out/'startup.cpp'),'-o',str(out/'startup.exe')],[str(out/'startup.exe')]]:
 p=subprocess.run(cmd,capture_output=True,text=True);print(p.stdout+p.stderr,end='')
 if p.returncode and cmd[0]==str(zig):sys.exit(p.returncode)
 total|=p.returncode
ht=(H/'tweaks/htcache.cpp').read_text(encoding='utf8')
direct_boot='/data/user_de/%u/com.miui.home/files' in ht
print('ICON_BOOT_INDEX=1 checks; failed='+str(int(not direct_boot))+'; symbol/sites shared private DE directory')
total|=int(not direct_boot)
sys.exit(total)
