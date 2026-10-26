"""Compile production early columns primer + current_mappings + stable_read with real NHK bank.
Only OS map stream, parser/binder boundary, clock/config and hook backend are substituted.
"""
from pathlib import Path
import subprocess,sys,json,os,shlex
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
O=W/'handover/desktop-columns-20261018'/('host-'+R.name);O.mkdir(parents=True,exist_ok=True)
s=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text('utf8')
def func(name):
 a=s.index(name);i=s.index('{',a);j=i+1;n=1
 while n:
  if s[j]=='{':n+=1
  if s[j]=='}':n-=1
  j+=1
 return s[a:j]
code=r'''
#include "nativehook/hook_bank.h"
#include <atomic>
#include <sstream>
#include <cstring>
#include <iostream>
#include <algorithm>
using Words=std::array<uint32_t,4>;using Slot=nhk::InlineSlot<4>;
using HookFunction=int(*)(void*,void*,void**);using UnhookFunction=int(*)(void*);
std::array<Slot,1>g_slots{};constexpr size_t kNativeColumnsSlot=0;
std::string g_container_path,g_dart_container_path,fixtureMaps;
uint64_t g_view_begin=0,g_view_end=0,g_dart_view_begin=0,g_dart_view_end=0;
std::atomic_flag g_loader_priming;uint64_t g_native_columns_prime_next=0;
std::atomic_bool g_native_columns_prime_attempted{false},g_back_setup_complete{false};
HookFunction g_hook_function=nullptr;UnhookFunction g_unhook_function=nullptr;
void*hc_native_columns_original=nullptr;uint32_t hc_native_columns_request=0,hc_grid_autofit_requested=0;
void hc_native_columns_entry(){};int getpid(){return 1;}int gettid(){return 1;}
uint64_t clockMs=1000;uint64_t monotonic_ms(){return clockMs;}
int __android_log_print(int,const char*,const char*,...){return 0;}
constexpr int ANDROID_LOG_INFO=4,ANDROID_LOG_WARN=5;const char*kTag="fixture";
namespace home_layout {
struct Config{bool grid_enabled=true;int cell_x=5,cell_y=7;};Config cfg;bool configOk=true;
bool query_bootstrap_config(Config&c){c=cfg;return configOk;}
struct NativeColumns{uint64_t splice=4;uint32_t slot=76;};
namespace elf_targets{bool resolveOk=true;std::optional<int> resolve(const std::vector<std::byte>&){return resolveOk?std::optional<int>(1):std::nullopt;}}
std::optional<NativeColumns> resolve_native_columns(const std::vector<std::byte>&,int){return NativeColumns{};}
}
home_layout::NativeColumns g_native_columns;
struct Lib{std::string path="/data/neutral/container.apk";uint64_t view_begin=0x1000,view_end=0x3000;std::vector<std::byte>bytes;};
std::optional<std::string>launcher_apk(const std::vector<nhk::FileMapping>&){return "/data/neutral/container.apk";}
std::optional<Lib>open_library(const std::string&,const char*,const std::vector<nhk::FileMapping>&){return Lib{};}
alignas(16)Words liveWords{0xb9404ff4,0xb9405be8,0x12800019,0x5280003a};
const Words origWords=liveWords;nhk::CodeSource source{0xfe,0x45,789,0x1100};
bool bind_target(const Lib&,uint64_t,uintptr_t&a,nhk::CodeSource&s,Words&w){a=(uintptr_t)liveWords.data();s=source;w=origWords;return true;}
bool fixture_read(uintptr_t a,std::span<std::byte> b){if(a!=(uintptr_t)liveWords.data()||b.size()!=sizeof(liveWords))return false;std::memcpy(b.data(),liveWords.data(),b.size());return true;}
'''
code+=func('std::optional<std::vector<nhk::ExecutableMapping>> current_mappings()').replace('std::ifstream maps("/proc/self/maps")','std::istringstream maps(fixtureMaps)')+'\n'
code+=func('bool stable_read(const Slot &slot, Words &words) {').replace('nhk::safe_read','fixture_read')+'\n'
code+=r'''
unsigned installs=0;
int install(void*t,void*,void**o){++installs;if(t!=liveWords.data())return -1;*o=(void*)0x1234;liveWords={0x58000050,0xd61f0200,0x12340000,0};return 0;}
nhk::InlineHookHost<4>&slot_host(){static nhk::InlineHookHost<4>h;h.read_slot=stable_read;h.write_words=[](uintptr_t,const Words&w){liveWords=w;return true;};h.hook_install=g_hook_function;h.hook_uninstall=g_unhook_function;return h;}
'''
code+=func('void prime_native_columns(').replace('std::ifstream maps("/proc/self/maps")','std::istringstream maps(fixtureMaps)')+'\n'
code+=r'''
unsigned checks=0,failed=0;void ck(bool a,const char*n){checks++;if(!a){failed++;std::cout<<"FAIL="<<n<<"\n";}}
void reset(){g_slots={};liveWords=origWords;g_container_path.clear();g_view_begin=g_view_end=0;g_native_columns_prime_next=0;g_native_columns_prime_attempted=false;g_back_setup_complete=false;g_loader_priming.clear();hc_native_columns_original=nullptr;hc_native_columns_request=0;hc_grid_autofit_requested=0;installs=0;home_layout::configOk=true;home_layout::cfg={true,5};home_layout::elf_targets::resolveOk=true;clockMs+=1000;}
int main(){std::ostringstream m;m<<std::hex<<(uintptr_t)liveWords.data()<<"-"<<(uintptr_t)(liveWords.data()+4)<<" r-xp 00001100 fe:45 789 /data/neutral/container.apk\n";fixtureMaps=m.str();
for(int col:{1,4,5,6,8}){reset();home_layout::cfg.cell_x=col;
 ck(!current_mappings(),"cold inventory has no owner");prime_native_columns(install,nullptr);
 ck(g_slots[0].registered&&g_slots[0].backend_owned&&g_slots[0].patch_known,"early bank ready before worker");
 ck(installs==1,"backend called after validated read");
 ck(g_container_path=="/data/neutral/container.apk"&&g_view_begin==0x1000&&g_view_end==0x3000,"validated owner published");
 ck(hc_grid_autofit_requested==(1u|(7u<<8)),"row boot snapshot published before first pipeline");
 ck(hc_native_columns_request==(1u|(unsigned(col)<<8)),"requested column delivered");
 ck(current_mappings().has_value(),"real NHK owner attribution succeeds");
 auto prior=liveWords;prime_native_columns(install,nullptr);ck(installs==1&&prior==liveWords,"no duplicate backend installation");
 if(g_slots[0].registered){auto old=fixtureMaps;auto pos=fixtureMaps.find("789");fixtureMaps.replace(pos,3,"790");Words out;ck(!stable_read(g_slots[0],out),"foreign generation refused");fixtureMaps=old;ck(stable_read(g_slots[0],out),"original generation retained");}
}
reset();home_layout::cfg.grid_enabled=false;prime_native_columns(install,nullptr);ck(installs==0&&g_container_path.empty(),"disabled has no mutation");
reset();home_layout::configOk=false;prime_native_columns(install,nullptr);ck(installs==0&&g_container_path.empty(),"missing config has no mutation");
reset();home_layout::elf_targets::resolveOk=false;prime_native_columns(install,nullptr);ck(installs==0&&g_container_path.empty(),"unresolved image has no mutation");
std::cout<<"COLUMNS_BOOTSTRAP checks="<<checks<<" failed="<<failed<<"; production primer+inventory+stable_read and real NHK bank; OS/config/parser/backend boundaries substituted\n";return failed?1:0;}
'''
(O/'test.cpp').write_text(code,encoding='utf8')
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
compiler=[str(zig),'c++'] if os.name=='nt' and zig.exists() else shlex.split(os.environ.get('CXX','c++'))
cmd=compiler+['-std=c++20','-I'+str(W/'app/src/main/cpp'),str(O/'test.cpp'),'-o',str(O/'test.exe')]
p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace')
if p.returncode:print(p.stdout+p.stderr);raise SystemExit(p.returncode)
p=subprocess.run([str(O/'test.exe')],capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='')
(O/'result.json').write_text(json.dumps(dict(command=[str(O/'test.exe')],stdout=p.stdout,stderr=p.stderr,exit=p.returncode),indent=2),encoding='utf8');raise SystemExit(p.returncode)
