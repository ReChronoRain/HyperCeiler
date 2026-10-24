from pathlib import Path
import subprocess,sys,struct
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/dock-bottom-margin-20261009'/('host-'+R.name);D.mkdir(parents=True,exist_ok=True)
S=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
a=S.index('size_t publish_hooks() {');a=S.index('    size_t live = 0;',a);b=S.index('    publish_indicator_dot_delta();',a);loop=S[a:b]+'return live;}'
pre=r'''
#include <array>
#include <atomic>
#include <cstring>
#include <cstdint>
#include <iostream>
struct Knob {bool hook_mode=true,hook_armed=true;std::atomic<int> delta_dp{0};std::atomic<bool> requested{false};uint32_t enabled=0;uint64_t value=0;uint32_t* hook_enabled=&enabled;uint64_t* hook_delta=&value;};
using KnobRuntime=Knob;std::array<Knob,9> g_knobs;bool folder_ready=false,drop_ready=false;int kKnobHookSlotBase=0;bool g_slots[9];double kKnobDeltaGain[9]={1,1,1,1,1,1,1,1,1};namespace nhk {bool slot_ready(bool ready){return ready;}}
'''
h=R/'app/src/main/cpp/targets/home/home_hotseat_margin.h'
if h.exists():pre+='#include "home_hotseat_margin.h"\n'
pre+='size_t test_publish(){'+loop
pre+=r'''
int main(){int n=0,failed=0;auto check=[&](bool v){++n;if(!v)++failed;};for(auto &s:g_slots)s=true;
auto read=[](){double d;std::memcpy(&d,&g_knobs[0].value,8);return d;};
for(int v:{0,1,30,69,70,71,110,150}){g_knobs[0].requested=true;g_knobs[0].delta_dp=v-70;test_publish();check(g_knobs[0].enabled==1);check(read()==v);test_publish();check(read()==v);}
for(int v:{-71,81}){g_knobs[0].requested=true;g_knobs[0].delta_dp=v;test_publish();check(g_knobs[0].enabled==0&&read()==0);}
for(int v:{-70,0,80}){g_knobs[0].requested=false;g_knobs[0].delta_dp=v;test_publish();check(g_knobs[0].enabled==0&&read()==0);}
g_knobs[0].requested=true;g_knobs[0].delta_dp=40;g_slots[0]=false;test_publish();check(g_knobs[0].enabled==0&&read()==0);g_slots[0]=true;g_knobs[0].hook_armed=false;test_publish();check(g_knobs[0].enabled==0&&read()==0);
g_knobs[1].delta_dp=20;test_publish();double row;std::memcpy(&row,&g_knobs[1].value,8);check(row==20&&g_knobs[1].enabled==1);
std::cout<<"HOTSEAT_MARGIN="<<n<<" checks; failed="<<failed<<"\n";return failed?1:0;}
'''
(D/'test.cpp').write_text(pre)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';r=subprocess.run([str(zig),'c++','-std=c++20','-O0','-I'+str(R/'app/src/main/cpp/targets/home'),str(D/'test.cpp'),'-o',str(D/'test.exe')],capture_output=True,text=True);print(r.stdout+r.stderr,end='')
if r.returncode:sys.exit(r.returncode)
r=subprocess.run([str(D/'test.exe')],capture_output=True,text=True);print(r.stdout+r.stderr,end='');failed=r.returncode
# Compile actual production ARM64 assembly, not a substituted trampoline.
clang=Path('D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe')
r=subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(W/'app/src/main/cpp'),'-c',str(R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(D/'arm.o')],capture_output=True,text=True);print(r.stdout+r.stderr,end='')
if r.returncode:sys.exit(r.returncode)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
elf=Elf(D/'arm.o');sec=elf.sections[elf.section_names['.symtab']];st=elf.sections[sec[6]];strings=elf.data[st[4]:st[4]+st[5]];symbols={}
for a in range(sec[4],sec[4]+sec[5],24):
 name,_,_,section,value,_=struct.unpack_from('<IBBHQQ',elf.data,a);name=strings[name:strings.find(b'\0',name)].decode();symbols[name]=(section,value)
armfail=0
for name,absolute in [('HotseatMargin',True),('FolderRowSpacing',False)]:
 section,value=symbols['hc_layout_dart_'+name+'_entry'];t=elf.sections[section];code=elf.data[t[4]+value:t[4]+value+256];ins=list(Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(code,value));ins=ins[:next(i for i,x in enumerate(ins) if x.mnemonic=='ret')+1];ok=any(x.mnemonic==('fmov' if absolute else 'fadd') and x.op_str==('d0, d1' if absolute else 'd0, d0, d1') for x in ins);armfail+=not ok
print(f'ARM64_MARGIN=2 checks; failed={armfail}');failed|=armfail>0
# Both early bootstrap and worker must carry enabled independently of delta.
checks=[S.count('g_knobs[index].requested.store(')==2,'knob.requested.store(false' in S]
n=sum(not c for c in checks);print(f'CONFIG_STATE=2 checks; failed={n}');sys.exit(int(bool(failed or n)))
