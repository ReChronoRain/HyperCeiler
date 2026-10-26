/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_report_feature_resolver.h"
#include "targets/home/home_report_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
static auto identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return report_feature(*c);}
int main(){auto f=fixture();auto p=identify(f);assert(p&&p->entry==oracle_entry&&p->consumers.size()==3);
 std::map<uint32_t,uint32_t> moved;auto copy=relocated(f,moved);auto r=identify(copy);assert(r&&r->entry==moved.at(oracle_entry)&&r->consumers.size()==3);
 for(const auto& x:f){auto missing=f;std::erase_if(missing,[&](const auto& v){return x.va==v.va;});assert(!identify(missing));}
 for(size_t at=0;at<35;++at){auto broken=f;auto& words=std::find_if(broken.begin(),broken.end(),[](const auto& v){return v.va==oracle_entry;})->words;words[at]=0xd503201fu;assert(!identify(broken));}
 auto unknown=f;const uint32_t va=0x04000000;
 unknown.push_back({va,{0xa9bf79fd,0xaa0f03fd,0x94000000u|(uint32_t((int64_t(oracle_entry)-va-8)/4)&0x3ffffffu),0xd65f03c0}});assert(!identify(unknown));
 auto ambiguous=f;for(auto x:copy)ambiguous.push_back(std::move(x));assert(!identify(ambiguous));
 // Fields and pool dispatch descriptors are values derived from current code.
 for(auto& x:copy)if(x.va==moved.at(oracle_entry))for(size_t at:{size_t(3),size_t(14),size_t(16),size_t(18)})x.words[at]+=8u<<12;
 assert(identify(copy));
 unsigned checks=0;
 for(bool ready:{false,true})for(bool hidden:{false,true})for(uintptr_t original:{uintptr_t(0),uintptr_t(0x12345678)}){
  recent_render::Registers s{};for(unsigned i=0;i<32;++i)s.x[i]=0x900000+i*8;
  for(unsigned i=0;i<32;++i){s.q[i][0]=i;s.q[i][1]=~uint64_t(i);}s.fpcr=0x400000;s.fpsr=0x123;
  auto expected=s;const bool applied=ready&&hidden&&original;expected.branch=applied?0:original;if(applied)expected.x[0]=s.x[22]+0x30;
  assert(report_render::apply(s,ready,hidden,original)==applied);assert(std::memcmp(&s,&expected,sizeof(s))==0);++checks;
 }
 std::cout<<"report predicate PASS: relocation, fields, 35 corruptions, missing/unknown/ambiguous consumers; "<<checks<<" full register cases\n";
}
