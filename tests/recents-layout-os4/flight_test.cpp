/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include <iostream>
using namespace home_layout::features::recent;
using namespace home_layout::recent_render;
#include "flight_fixture.h"
static Plan discover(const Functions& fs,Plan p=flight_seed()){
 auto bytes=image(fs);auto code=Code::parse(bytes);assert(code&&corner_animation_graph(*code,p));return p;
}
int main(){(void)&relocated;auto fs=flight_fixture();auto p=discover(fs);assert(p.sites.size()==4);unsigned cases=0;
 alignas(16) std::byte pool[0x200000]{},frame[2048]{};auto pp=reinterpret_cast<uintptr_t>(pool),fp=reinterpret_cast<uintptr_t>(frame)+1024;
 auto target=*std::find_if(p.sites.begin(),p.sites.end(),[](auto&s){return s.action==Action::CornerFrame;});
 assert(target.frame!=target.selector_frame&&target.frame<0&&target.selector_frame<0);
 for(int selected:{0,20,170,180})for(bool stacked:{false,true})for(const auto&site:p.sites){
  Registers r{};r.x[27]=pp;r.x[29]=fp;r.x[30]=0x12345;r.x[15]=fp-64;r.branch=0xbeef;
  for(unsigned i=0;i<32;++i)r.d(i,100+i);
  store<uintptr_t>(pp+target.pool,0x12345679);store<uintptr_t>(fp+target.selector_frame,stacked?0x12345679:0x23456789);
  store<double>(fp+target.frame,39.5);if(site.action==Action::CornerSurface)store<double>(fp+site.frame,39.5);RecentConfig c;c.values[RecentConfig::Corners]=selected;
  bool applied=selected!=20&&!(stacked&&site.action==Action::CornerFrame);
  assert(apply(p,site,r,c,0)==(applied?Result::Applied:Result::Neutral));
  if(site.action==Action::CornerFrame||site.action==Action::CornerSurface){assert(load<double>(fp+site.frame)==(applied?selected:39.5));assert(r.d(0)==100&&r.d(1)==101&&r.branch==0xbeef);}
  if(site.action==Action::Scalar){assert(r.d(3)==(applied?selected:103));assert(r.d(0)==100&&r.d(1)==101&&r.d(2)==102&&r.branch==0xbeef);}
  if(site.action==Action::ScalarReturn){assert(r.d(0)==(applied?selected:100));assert(r.branch==(applied?0:0xbeef));}
  assert(r.x[29]==fp&&r.x[30]==0x12345&&r.x[15]==fp-64);++cases;
 }
 // Move owners and typed allocators independently. Both original direct-call
 // edges and the already-proved producer seed must relocate together.
 std::map<uint32_t,uint32_t> map;auto moved=relocated(fs,map);auto seed=flight_seed();
 seed.radius_factory=relocate(seed.radius_factory,map);seed.border_factory=relocate(seed.border_factory,map);seed.width_selector=relocate(seed.width_selector,map);
 auto shifted=discover(moved,seed);assert(shifted.sites.size()==4);++cases;
 auto frames=fs;
 for(auto&f:frames){auto n=home_dynamic::frame_size(f.words);if(!n)continue;f.words[2]+=8u<<10;
  for(auto&w:f.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<0)
   w=(w&~(511u<<12))|((uint32_t(home_dynamic::imm9(w)-8)&511u)<<12);
 }
 auto shifted_frames=discover(frames);auto t=*std::find_if(shifted_frames.sites.begin(),shifted_frames.sites.end(),[](auto&s){return s.action==Action::CornerFrame;});
 assert(t.frame==target.frame-8&&t.selector_frame==target.selector_frame-8);++cases;
 // A missing/ambiguous owner must publish none of the four additions.
 auto owner=*std::find_if(fs.begin(),fs.end(),[&](auto&f){return target.va>=f.va&&target.va<f.va+f.words.size()*4;});
 auto missing=fs;missing.erase(std::remove_if(missing.begin(),missing.end(),[&](auto&f){return f.va==owner.va;}),missing.end());
 auto bytes=image(missing);auto code=Code::parse(bytes);auto refused=flight_seed();assert(code&&!corner_animation_graph(*code,refused)&&refused.sites.empty());++cases;
 auto dup=fs;auto copy=owner;copy.va=0x03000000;
 for(size_t i=0;i<copy.words.size();++i){uint32_t to=0;if(home_dynamic::target(copy.words[i],owner.va+i*4,to))copy.words[i]=0x94000000u|(uint32_t((int64_t(to)-copy.va-i*4)/4)&0x3ffffffu);}
 dup.push_back(copy);bytes=image(dup);code=Code::parse(bytes);refused=flight_seed();assert(code&&!corner_animation_graph(*code,refused)&&refused.sites.empty());++cases;
 // A changed stock default is a different ABI/semantic contract, not a reason
 // to keep silently interpreting that branch as the old card target.
 auto changed=fs;for(auto&f:changed)if(f.va==owner.va)for(auto&w:f.words)if(w==0x1e669001u)w=0x1e66b001u;
 bytes=image(changed);code=Code::parse(bytes);refused=flight_seed();assert(code&&!corner_animation_graph(*code,refused)&&refused.sites.empty());++cases;
 std::cout<<"recents flight radius PASS "<<cases<<" cases, target and lerp alignment, screen begins preserved, stack scaling retained, neutral/zero/max, relocation, frames and fail-closed families\n";
}
