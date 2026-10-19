#!/usr/bin/env python3
"""Portable regression using real launcher instructions and the production callback."""
import json
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
source = (ROOT / 'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
start = source.index('static thread_local double folder_layout_gap')
end = source.index('extern "C" uint64_t hc_title_custom_label(', start)
fixture = json.loads((HERE / 'launcher-7751.json').read_text())
pre = r'''
#include <atomic>
#include <cstring>
#include <iostream>
#include <vector>
#include <sys/mman.h>
#include "home_folder_layout.h"
uint64_t hc_folder_layout_requested=1;
int g_folder_gap_field=0,g_folder_cell_width_field=0,g_folder_screen_width_field=0,
    g_folder_screen_height_field=0,g_folder_cling_width_field=0,
    g_folder_controller_config_field=0,g_folder_rx_value_field=0,g_folder_enum_index_field=0;
uint32_t g_folder_center_pool=0,g_folder_inner_owner_local=0;
bool g_folder_inner_ready=true;
home_layout::FolderInnerContainer g_folder_inner;
constexpr int ANDROID_LOG_INFO=4;
const char*kTag="test";
int __android_log_print(int,const char*,const char*,...){return 0;}
'''
test = r'''
int main(){
int checks=0,failures=0;
auto check=[&](bool pass,const char*name){++checks;if(!pass){++failures;std::cerr<<"FAIL "<<name<<'\n';}};
using namespace home_layout;
check(folder_inner_container(text,build,padding,text_va,g_folder_inner),"7751 complete title plan");
check(g_folder_inner.inset_slot==0x10,"fresh padding local derived from producer");
const auto plan=g_folder_inner;
for(size_t i=0;i<text.size();++i){
    if(!insn_is_stur_d(text[i])||insn_rn(text[i])!=0)continue;
    auto bad=text;bad[i-1]=0xd503201f;
    FolderInnerContainer rejected;
    check(!folder_inner_container(bad,build,padding,text_va,rejected),"missing allocator spill rejected");
    bad=text;bad[i-3]=0xd503201f;
    check(!folder_inner_container(bad,build,padding,text_va,rejected),"missing allocation call rejected");
    bad=text;bad[i+1]=bad[i-1];
    check(!folder_inner_container(bad,build,padding,text_va,rejected),"foreign initializer rejected");
    bad=text;bad[i+4]=bad[i-1];
    check(!folder_inner_container(bad,build,padding,text_va,rejected),"overwritten padding local rejected");
}
FolderInnerContainer moved;
check(folder_inner_container(text,build,padding,text_va+16,moved)
    &&moved.window==plan.window+16&&moved.inset_slot==plan.inset_slot,"relocation preserves ownership derivation");
void*memory=mmap(nullptr,4096,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
if(memory==MAP_FAILED)return 2;
const uintptr_t owner=reinterpret_cast<uintptr_t>(memory)+9;
const uintptr_t insets=owner+128,shared=owner+256,heap=owner&~uintptr_t(0xffffffff);
alignas(16) unsigned char saved[320]{},stack[256]{};
const uintptr_t save=reinterpret_cast<uintptr_t>(saved),sp=reinterpret_cast<uintptr_t>(stack);
auto set=[&](uintptr_t at,auto value){std::memcpy(reinterpret_cast<void*>(at),&value,sizeof(value));};
auto scalar=[&](uintptr_t at){double value;std::memcpy(&value,reinterpret_cast<void*>(at),8);return value;};
auto word=[&](uintptr_t at){uint32_t value;std::memcpy(&value,reinterpret_cast<void*>(at),4);return value;};
const uint32_t center=0x41,low=0x51,high=0x61;
const auto pool_size=std::max({plan.center_pool,plan.direction_pool[0],plan.direction_pool[1]})/8+2;
std::vector<uint64_t> pool(pool_size);
pool[plan.center_pool/8]=center;pool[plan.direction_pool[0]/8]=low;pool[plan.direction_pool[1]/8]=high;
pool[0x40/8]=0x71;
g_folder_inner_owner_local=32+plan.frame-plan.owner_slot;
auto reset=[&](double left=16){
    std::memset(memory,0,4096);std::memset(saved,0,sizeof(saved));std::memset(stack,0,sizeof(stack));
    hc_folder_layout_requested=1;
    set(save+128,sp);set(save+24,owner);
    set(sp+g_folder_inner_owner_local,uint32_t(owner));
    set(sp+32+plan.frame-plan.inset_slot,uint32_t(insets));
    set(owner+plan.alignment,low);set(owner+plan.padding,uint32_t(insets));
    set(insets+plan.inset,left);
};
auto invoke=[&]{hc_folder_layout_body(save,reinterpret_cast<uintptr_t>(pool.data()),7,heap);};
for(double left:{16.,28.}){
    reset(left);invoke();
    check(word(owner+plan.alignment)==center,"direction root centered");
    check(scalar(insets+plan.inset)==left&&scalar(insets+plan.inset+16)==left,"horizontal gutters symmetric");
    check(scalar(insets+plan.inset+8)==0&&scalar(insets+plan.inset+24)==0,"vertical padding preserved");
    invoke();check(scalar(insets+plan.inset+16)==left,"repeat does not compound padding");
}
reset();set(owner+plan.alignment,center);invoke();
check(scalar(insets+plan.inset+16)==16,"already centered alignment still corrects asymmetric padding");
reset();set(save+24,owner+8);invoke();
check(word(owner+plan.alignment)==low&&scalar(insets+plan.inset+16)==0,"editor path preserves owner and padding");
reset();set(owner+plan.padding,uint32_t(shared));set(shared+plan.inset,28.);invoke();
check(scalar(shared+plan.inset+16)==0&&scalar(insets+plan.inset+16)==0,"shared padding is immutable");
reset();set(sp+32+plan.frame-plan.inset_slot,uint32_t(0));invoke();
check(scalar(insets+plan.inset+16)==0,"missing fresh ownership witness refuses mutation");
reset();set(owner+plan.alignment,uint32_t(0x999));invoke();
check(word(owner+plan.alignment)==0x999&&scalar(insets+plan.inset+16)==0,"foreign alignment preserves all fields");
reset();hc_folder_layout_requested=0;invoke();
check(word(owner+plan.alignment)==low&&scalar(insets+plan.inset+16)==0,"disabled centering is exact no-op");
reset();set(save+128,uintptr_t(0));invoke();
check(scalar(insets+plan.inset+16)==0,"missing Dart stack refuses dereference");
reset();set(insets+plan.inset+8,12.);invoke();
check(scalar(insets+plan.inset+8)==12&&scalar(insets+plan.inset+16)==0,"foreign padding shape preserved");
reset();set(insets+plan.inset+16,8.);invoke();
check(scalar(insets+plan.inset+16)==8,"custom right margin preserved");
munmap(memory,4096);
std::cout<<"FOLDER_TITLE_PADDING="<<checks<<" checks; failed="<<failures<<'\n';
return failures?1:0;
}
'''
arrays = []
for key, name in [('text', 'FolderHeaderWidget._buildText'), ('build', 'Container.build'),
                  ('padding', 'Container._paddingIncludingDecoration')]:
    item = fixture['functions'][name]
    if key == 'text':
        arrays.append(f'const uint32_t text_va={item["va"]};')
    arrays.append(f'const std::vector<uint32_t> {key}={{' + ','.join('0x'+w for w in item['words']) + '};')
with tempfile.TemporaryDirectory(prefix='hc-title-padding-') as temp:
    temp = Path(temp)
    cpp = temp / 'test.cpp'
    cpp.write_text(pre + source[start:end] + '\n'.join(arrays) + test)
    subprocess.run(['c++', '-std=c++20', '-Wall', '-Wextra', '-Werror',
                    '-I'+str(ROOT/'app/src/main/cpp/targets/home'), str(cpp), '-o', str(temp/'test')], check=True)
    subprocess.run([str(temp/'test')], check=True)
