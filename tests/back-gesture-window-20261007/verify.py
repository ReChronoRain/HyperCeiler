from pathlib import Path
import os, shlex, subprocess, sys, tempfile
D=Path(__file__).resolve().parent;W=D.parents[1];R=Path(sys.argv[1]) if len(sys.argv)>1 else W
source=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding="utf8")
if 'hc_back_window_body' not in source:print('WINDOW_APPLY=absent; stock GestureStub region persists');sys.exit(1)
a=source.index('extern "C" void hc_back_gesture_body(');b=source.index('extern "C" void hc_back_window_entry();',a);core=source[a:b]
a=source.index('extern "C" void hc_back_window_body(');b=source.index('std::string g_container_path;',a);body=source[a:b]
a=source.index('std::atomic_uint32_t g_back_gesture_config')
b=source.index('std::atomic_int g_dock_horizontal_delta',a)
globals=source[a:b]
c=r"""
#include "home_back_gesture.h"
#include "home_binding_retry.h"
#include <atomic>
#include <fstream>
#include <iostream>
#include <mutex>
using Words=std::array<uint32_t,4>;
struct Slot{uintptr_t address=0;Words original_words{};};
GLOBALS
bool back_regions_enabled(){return g_back_regions_ready.load();}
Slot g_back_window_update{};
bool read_ok=true,words_ok=true;
bool stable_back_read(const Slot &s,Words &out){out=s.original_words;if(!words_ok)out[0]^=1;return read_ok;}
BODY
CORE
unsigned calls=0,checks=0,failed=0;std::byte *saved_for_nested=nullptr;home_layout::BackWindowRect applied{};
void update(uint32_t side,const home_layout::BackWindowRect *r){++calls;applied=*r;if(saved_for_nested)hc_back_window_body(saved_for_nested);}
void ok(bool b){++checks;if(!b){++failed;std::cout<<"FAIL_CHECK="<<checks<<"\\n";}}
int main(int argc,char**argv){
 std::optional<home_layout::BackWindowShape> shape = home_layout::BackWindowShape{0, 0, 0, 16, 32};
 if (argc > 1) {
  std::ifstream f(argv[1],std::ios::binary);std::vector<char> raw((std::istreambuf_iterator<char>(f)),{});
  auto bytes=std::span<const std::byte>(reinterpret_cast<const std::byte*>(raw.data()),raw.size());
  auto image=home_layout::elf_targets::parse(bytes);shape=home_layout::resolve_back_window(bytes);ok(bool(shape));if(!shape)return 1;
  std::cout<<"WINDOW_RESOLVED="<<std::hex<<shape->apply<<" "<<shape->update<<" "<<shape->splice<<" "<<shape->rect_sp<<" "<<shape->side_field<<std::dec<<"\n";
  for(auto site:{shape->splice,shape->splice+4,shape->splice+8,shape->splice+24}){auto mutant=raw;auto at=image->file_offset(site,4);uint32_t nop=0xd503201f;std::memcpy(mutant.data()+*at,&nop,4);ok(!home_layout::resolve_back_window({reinterpret_cast<const std::byte*>(mutant.data()),mutant.size()}));}
 } else {
  std::cout<<"WINDOW_RESOLVER=skipped (no launcher ELF fixture)\n";
 }
 std::array<std::byte,1024> saved{};std::array<std::byte,4096> receiver{};
 auto set=[&](unsigned at,auto value){std::memcpy(saved.data()+at,&value,sizeof(value));};
 auto incoming=[&](home_layout::BackWindowRect r){set(784+shape->rect_sp,r);};
 auto get=[&](){home_layout::BackWindowRect r;std::memcpy(&r,saved.data()+784+shape->rect_sp,16);return r;};
 auto baseline=[&](unsigned side){saved.fill(std::byte{0x5a});set(64,int32_t(280));set(168,int32_t(2576));set(192,int32_t(1200));set(224,int32_t(2670));set(176,int32_t(520));set(384,21.0f);hc_back_gesture_body(saved.data());set(160,reinterpret_cast<uintptr_t>(receiver.data()));set(152,int32_t(122));receiver[shape->side_field]=std::byte(side);incoming(side?home_layout::BackWindowRect{1132,280,1200,2576}:home_layout::BackWindowRect{0,280,68,2576});};
 g_back_regions_ready=true;
 g_back_window_shape=*shape;g_back_window_update.address=reinterpret_cast<uintptr_t>(update);saved_for_nested=saved.data();
 for(unsigned side:{0u,1u})for(int height:{10,25,60,100})for(int width:{100,200,400}){
  g_back_gesture_config=uint32_t(height)|(uint32_t(width)<<16);baseline(side);const auto before=saved;const auto old_calls=calls;const auto old_rect=get();
  hc_back_window_body(saved.data());auto expected=home_layout::back_window_rect({280,2576,1200,2670,520,21.0f},side,{height,width});ok(expected&&get()==*expected);
  ok(calls==old_calls+((expected&&old_rect==*expected)?0:1));
  auto after=saved;auto n=calls;hc_back_window_body(saved.data());ok(saved==after&&calls==n);
  // No multiplication of an already-applied 200%/400% width; default restores stock immediately.
  g_back_gesture_config=60u|(100u<<16);hc_back_window_body(saved.data());auto stock=home_layout::back_window_rect({280,2576,1200,2670,520,21.0f},side,{});ok(get()==*stock);
  for(size_t i=0;i<784;++i)if(i<152||i>=156)ok(saved[i]==before[i]);
 }
 g_back_gesture_config=25u|(200u<<16);baseline(0);auto before=saved;read_ok=false;hc_back_window_body(saved.data());ok(saved==before);read_ok=true;words_ok=false;hc_back_window_body(saved.data());ok(saved==before);words_ok=true;
 baseline(0);incoming({0,2200,68,2576});before=saved;hc_back_window_body(saved.data());ok(saved==before); // app exclusion preserved
 baseline(0);g_back_stock_sequence.fetch_add(1);before=saved;hc_back_window_body(saved.data());ok(saved==before);g_back_stock_sequence.fetch_add(1);
 baseline(0);hc_back_window_body(saved.data());const auto custom=get();
 std::array<std::byte,4096> next_receiver{};
 set(160,reinterpret_cast<uintptr_t>(next_receiver.data()));
 g_back_gesture_config=60u|(100u<<16);const auto before_replacement=calls;
 hc_back_window_body(saved.data());ok(get()==custom&&calls==before_replacement);

 g_back_gesture_config=25u|(200u<<16);baseline(0);hc_back_window_body(saved.data());
 const auto before_generation=calls;
 ++g_back_generation;g_back_gesture_config=60u|(100u<<16);
 hc_back_window_body(saved.data());ok(get()==custom&&calls==before_generation);
 baseline(0);incoming(custom);
 hc_back_window_body(saved.data());ok(get()==custom&&calls==before_generation);

 g_back_gesture_config=25u|(200u<<16);baseline(0);
 auto invalid=saved;int32_t zero=0;std::memcpy(invalid.data()+192,&zero,sizeof(zero));
 hc_back_gesture_body(invalid.data());
 hc_back_window_body(saved.data());ok(calls==before_generation);

 g_back_gesture_config=100u|(200u<<16);baseline(0);hc_back_window_body(saved.data());
 const auto previous_layout_rect=get();
 const auto before_layout_change=calls;
 g_back_gesture_config=60u|(100u<<16);baseline(0);
 set(64,int32_t(300));hc_back_gesture_body(saved.data());incoming(previous_layout_rect);
 hc_back_window_body(saved.data());ok(get()==previous_layout_rect&&calls==before_layout_change);

 std::cout<<"WINDOW_APPLY="<<checks<<" checks; failed="<<failed<<"; nested guard, defaults, exclusions, receiver replacement, stale generation and source checks\n";return failed?1:0;
}
"""
c=c.replace('BODY',body).replace('CORE',core).replace('GLOBALS',globals)
image = Path(sys.argv[2]) if len(sys.argv)>2 else W/'tests/back-gesture-os4/libapp_launcher.so'
with tempfile.TemporaryDirectory(prefix='hc-back-window-') as temp:
    src=Path(temp)/'test.cpp'
    exe=Path(temp)/'test.exe'
    src.write_text(c)
    include=R/'app/src/main/cpp'
    compiler=shlex.split(os.environ.get('CXX','c++'))
    subprocess.run([*compiler,'-std=c++20','-O0','-pthread','-I'+str(include/'targets/home'),
                    '-I'+str(include),str(src),'-o',str(exe)],check=True)
    subprocess.run([str(exe),*([str(image)] if image.is_file() or len(sys.argv)>2 else [])],check=True)
