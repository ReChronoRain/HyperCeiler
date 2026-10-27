#!/usr/bin/env python3
"""Count actual startup and auxiliary discovery work, not just policy calls."""
from pathlib import Path
import subprocess,tempfile,re
R=Path(__file__).resolve().parents[2];cpp=R/'app/src/main/cpp';s=(cpp/'targets/home/home_layout_hooks.cpp').read_text()
a=s.index('    if (any_knob || top_probe) {',s.index('const auto all_bound'))
b=s.index('    std::vector<size_t> order;',a);startup=s[a:b]
a=s.index('size_t arm_hooks(');b=s.index('    size_t added = 0;',a);aux=s[a:b]+'return 0;\n}'
functions=sorted(set(re.findall(r'\b(bind_[a-z_]+)\(',aux)))
prefix=r'''
#include <array>
#include <vector>
#include <cstdint>
#include <optional>
#include <cstdio>
#include <cassert>
#if __has_include("targets/home/home_binding_retry.h")
#include "targets/home/home_binding_retry.h"
home_layout::BindingRetry g_aux_bind_retry;
#endif
uint64_t now=0,delays=0;unsigned knobs=0,animations=0,magic=0,binds=0;
uint64_t monotonic_ms(){return now;}
void delay_ms(unsigned n){delays+=n;}
size_t bind_knobs(){++knobs;return 0;}
struct Located{};
bool bind_animation_consumer(Located&){++animations;return false;}
bool bind_magic_consumer(Located&){++magic;return false;}
struct Config{struct Tweaks{bool animation_open_enabled=true,animation_recents_enabled=true;}tweaks;struct Recent{std::array<int,12>values{};}recent;};
namespace home_layout{struct RecentConfig{static constexpr int Animation=3;};}
bool g_grid_autofit_checked=false,g_grid_autofit_bound=false,g_folder_layout_attempted=false,g_folder_layout_checked=false,g_folder_layout_bound=false;
#define ANDROID_LOG_WARN 1
const char*kTag="fixture";
int __android_log_print(int,const char*,const char*,...){return 0;}
'''
for n in functions:prefix+=f'bool {n}(){{++binds;return false;}}\n'
main=r'''
void startup(){bool any_knob=true,top_probe=false;std::array<int,8>g_knobs;auto all_bound=[](){return false;};Config config;std::optional<Located> located{Located{}};
'''+startup+r'''
}
int main(){startup();std::vector<size_t> order;
 for(unsigned i=0;i<1200;++i){now=i*500;arm_hooks(order);}
 fprintf(stderr,"startup unresolved knobs=%u animation=%u magic=%u artificial_wait_ms=%llu; auxiliary binding calls=%u\n",knobs,animations,magic,(unsigned long long)delays,binds);
 assert(knobs==1&&animations==1&&magic==1&&delays==0);
 assert(binds<=25*'''+str(len(functions))+r''');
#if __has_include("targets/home/home_binding_retry.h")
 auto before=binds;g_aux_bind_retry.reset();arm_hooks(order);assert(binds==before+'''+str(len(functions))+r''');
 home_layout::BindingRetry retry;assert(retry.due(0));assert(!retry.due(0));assert(!retry.due(499));assert(retry.due(500));assert(!retry.due(501));
 retry.reset();assert(retry.due(UINT64_MAX-1));assert(retry.due(UINT64_MAX));assert(!retry.due(UINT64_MAX));
#endif
 puts("Binding retry PASS: unsupported startup resolves once without sleep, 1200 worker passes have bounded discovery, explicit reset and monotonic edge cases");}
'''
with tempfile.TemporaryDirectory(prefix='hc-binding-retry-')as tmp:
 p=Path(tmp);(p/'test.cpp').write_text(prefix+aux+main)
 subprocess.run(['c++','-std=c++20','-O2','-I'+str(cpp),str(p/'test.cpp'),'-o',str(p/'test')],check=True)
 subprocess.run([str(p/'test')],check=True)
