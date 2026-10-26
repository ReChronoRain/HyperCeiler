from pathlib import Path
import subprocess,sys,json
D=Path(__file__).resolve().parents[1]/'back-gesture-os4';W=D.parents[1];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
header=R/'app/src/main/cpp/targets/home/home_back_gesture.h'
if not header.exists():print('BACK_GESTURE=absent; OS4 controls gated; original region unchanged');sys.exit(1)
out=D/('run-'+R.name);out.mkdir(exist_ok=True)
c=r'''
#include "home_back_gesture.h"
#include <fstream>
#include <iostream>
#include <array>
using namespace home_layout;
unsigned checks=0,failed=0;void ok(bool b){++checks;if(!b)++failed;}
int main(int argc,char**argv){
 std::ifstream f(argv[1],std::ios::binary);std::vector<char> raw((std::istreambuf_iterator<char>(f)),{});
 std::span<const std::byte> bytes{reinterpret_cast<const std::byte*>(raw.data()),raw.size()};
 auto shape=resolve_back_gesture(bytes);ok(bool(shape));if(!shape)return 1;
 auto image=elf_targets::parse(bytes);ok(bool(image));
 auto fn=image->bounds(shape->entry);ok(bool(fn));
 std::cout<<"RESOLVED="<<std::hex<<shape->entry<<" left="<<shape->left<<" right="<<shape->right<<" screen="<<shape->screen<<std::dec<<"\n";
 // Independently alter the production instructions: reject a changed receiver or arithmetic.
 for(auto site=fn->begin;site+4<=fn->end;site+=4){auto word=image->word(site);if(word&&(*word==0xaa0003f3u||*word==0x1e2202c0u||*word==0x4b090309u)){
  auto mutant=raw;auto off=image->file_offset(site,4);uint32_t nop=0xd503201f;std::memcpy(mutant.data()+*off,&nop,4);
  ok(!resolve_back_gesture({reinterpret_cast<const std::byte*>(mutant.data()),mutant.size()}));}}
 // One logger is stripped: the second independently locates the same original body.
 for(const char*label:{"update_home_region home_region: ","update_region is_fold_or_pad: "}){
  auto mutant=raw;auto literal=elf_targets::unique_literal(*image,label);ok(bool(literal));
  auto off=image->file_offset(*literal,1);mutant[*off]='X';auto again=resolve_back_gesture({reinterpret_cast<const std::byte*>(mutant.data()),mutant.size()});ok(again&&again->entry==shape->entry);
 }
 // Move the decoded fields together: no field offset is used as a profile constant.
 auto relocated=raw;
 for(auto site=fn->begin;site+4<=fn->end;site+=4){auto w=image->word(site);if(w&&((*w&0xffc003e0u)==0x29000260u)){
  auto p=back_pair(*w,*w&31,(*w>>10)&31);if(p&&(*p==shape->screen||(*p>=shape->left&&*p<=shape->right+8))){uint32_t moved=*w+(4u<<15);auto off=image->file_offset(site,4);std::memcpy(relocated.data()+*off,&moved,4);}}}
 auto moved=resolve_back_gesture({reinterpret_cast<const std::byte*>(relocated.data()),relocated.size()});ok(moved&&moved->left==shape->left+16&&moved->screen==shape->screen+16);

 std::array<std::byte,784> saved{};
 auto set=[&](unsigned at,auto v){std::memcpy(saved.data()+at,&v,sizeof(v));};
 auto integer=[&](unsigned reg){int32_t v;std::memcpy(&v,saved.data()+reg*8,4);return v;};
 auto baseline=[&](int w,int h,int top,int bottom){saved.fill(std::byte{0x5a});set(8*8,top);set(21*8,bottom);set(24*8,w);set(28*8,h);set(22*8,480);set(384,21.0f);};
 for(int rotation=0;rotation<4;++rotation)for(int height:{10,25,60,95,100})for(int width:{100,105,200,400}){
  int w=(rotation&1)?2670:1200,h=(rotation&1)?1200:2670;
  int top=(rotation&1)?240:0,bottom=(rotation&1)?960:2573;
  baseline(w,h,top,bottom);const auto before=saved;
  g_back_gesture_config=height|(width<<16);hc_back_gesture_body(saved.data());
  int t=integer(8),bt=integer(21);float dp;std::memcpy(&dp,saved.data()+384,4);
  ok(std::abs(dp-21*width/100.0f)<0.001f);ok(t>=0&&bt<=h&&t<bt);
  if(height==60)ok(t==top&&bt==bottom);
  else if(rotation&1)ok(bt-t==h*height/100);else ok(bt==bottom&&t==std::max(0,bottom-h*height/100));
  for(unsigned i=0;i<saved.size();++i)if(!(i>=64&&i<68)&&!(i>=168&&i<172)&&!(i>=384&&i<388))ok(saved[i]==before[i]);
 }
 for(int i=0;i<1000;++i){baseline(1200,2670,0,2573);g_back_gesture_config=25|(200<<16);hc_back_gesture_body(saved.data());ok(integer(8)==1906&&integer(21)==2573);}
 for(auto config:{BackGestureConfig{9,100},BackGestureConfig{101,100},BackGestureConfig{60,99},BackGestureConfig{60,401}}){baseline(1200,2670,0,2573);auto before=saved;g_back_gesture_config=config.height|(config.width<<16);hc_back_gesture_body(saved.data());ok(saved==before);}
 baseline(1200,2670,0,0);auto before=saved;g_back_gesture_config=25|(200<<16);hc_back_gesture_body(saved.data());ok(saved==before);
 baseline(1200,2670,0,2573);set(384,float(NAN));before=saved;hc_back_gesture_body(saved.data());ok(saved==before);
 baseline(1200,2670,0,2573);set(22*8,0);before=saved;hc_back_gesture_body(saved.data());ok(saved==before);
 std::vector<int> packet;auto read=[&](int32_t&v){if(packet.empty())return false;v=packet.front();packet.erase(packet.begin());return true;};
 BackGestureConfig config;ok(read_back_gesture(read,config)&&config.height==60&&config.width==100);
 for(auto p:{std::vector<int>{kBackGestureMagic,25,200},std::vector<int>{kBackGestureMagic,100,400}}){packet=p;ok(read_back_gesture(read,config)&&config.height==p[1]&&config.width==p[2]);}
 for(auto p:{std::vector<int>{1,25,200},std::vector<int>{kBackGestureMagic},std::vector<int>{kBackGestureMagic,25},std::vector<int>{kBackGestureMagic,9,200},std::vector<int>{kBackGestureMagic,60,401}}){packet=p;ok(!read_back_gesture(read,config));}
 std::cout<<"BACK_GESTURE="<<checks<<" checks; failed="<<failed<<"\n";return failed?1:0;}
'''
source=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
a=source.index('extern "C" void hc_back_gesture_body(');b=source.index('extern \"C\" void hc_back_window_entry();',a) if 'hc_back_window_entry' in source else source.index('std::string g_container_path;',a)
body=source[a:b]
c=c.replace('using namespace home_layout;', '#include <atomic>\nstd::atomic_uint32_t g_back_gesture_config{60u | (100u<<16)},g_back_stock_width{0},g_back_stock_sequence{0};\nstd::atomic_uint64_t g_back_stock{0};\nstd::atomic_flag g_back_stock_writer=ATOMIC_FLAG_INIT;\n'+body+'\nusing namespace home_layout;')
(out/'test.cpp').write_text(c)
cmd=[str(W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'),'c++','-std=c++20','-O0','-I'+str(R/'app/src/main/cpp/targets/home'),'-I'+str(W/'app/src/main/cpp/targets/home'),'-I'+str(W/'app/src/main/cpp'),str(out/'test.cpp'),'-o',str(out/'test.exe')]
p=subprocess.run(cmd,capture_output=True,text=True);print(p.stdout+p.stderr,end='');
if p.returncode:sys.exit(p.returncode)
p=subprocess.run([str(out/'test.exe'),str(D/'libapp_launcher.so')],capture_output=True,text=True);print(p.stdout+p.stderr,end='');sys.exit(p.returncode)
