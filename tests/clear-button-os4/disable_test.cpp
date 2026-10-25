/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_clear_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
static auto identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return clear_disable_feature(*c);}
static auto& body(Functions& f,uint32_t va){auto it=std::find_if(f.begin(),f.end(),[&](const auto& x){return x.va==va;});assert(it!=f.end());return it->words;}
int main(){auto f=fixture();auto p=identify(f);assert(p&&p->overlay==oracle_overlay&&p->fold==oracle_fold&&p->dock_query==oracle_query&&p->calls==oracle_calls);
 std::map<uint32_t,uint32_t> moved;auto copy=relocated(f,moved);auto r=identify(copy);assert(r&&r->overlay==moved.at(p->overlay)&&r->fold==moved.at(p->fold)&&r->empty_widget==p->empty_widget);
 for(const auto& x:f){auto missing=f;std::erase_if(missing,[&](const auto& v){return x.va==v.va;});assert(!identify(missing));}
 for(size_t n=0;n<54;++n){auto broken=f;body(broken,oracle_fold)[n]=0xd503201fu;assert(!identify(broken));}
 const auto owner=std::find_if(f.begin(),f.end(),[&](const auto& x){return oracle_calls[0]>=x.va&&uint64_t(oracle_calls[0])<uint64_t(x.va)+x.words.size()*4;})->va;
 const auto call_index=(oracle_calls[0]-owner)/4;
 {auto broken=f;body(broken,owner)[call_index+1]=0xaa0003e1;assert(!identify(broken));}
 // The same fold-mode field is independently moved in producer and selector.
 {auto broken=f;body(broken,oracle_fold)[32]+=8u<<12;assert(!identify(broken));body(broken,owner)[call_index-11]+=8u<<12;assert(identify(broken));}
 {auto broken=f;body(broken,oracle_fold)[16]+=1u<<10;assert(!identify(broken));}
 {auto broken=f;body(broken,oracle_fold)[38]=0xaa1603e0;assert(!identify(broken));}
 {auto broken=f;body(broken,oracle_fold)[38]+=1u<<10;auto shifted=identify(broken);assert(shifted&&shifted->empty_widget==p->empty_widget+(1u<<10));}
 {auto broken=f;body(broken,oracle_overlay)[3]=0xd503201fu;assert(!identify(broken));}
 const uint32_t va=0x04000000;
 const auto bl=0x94000000u|(uint32_t((int64_t(oracle_overlay)-va-12)/4)&0x3ffffffu);
 // Unrecognized users of the result and branch entries into the replacement
 // window must fail closed, rather than shortening the scan/liveness horizon.
 auto unsafe=f;unsafe.push_back({va,{0xa9bf79fd,0xaa0f03fd,0xd10041ef,bl,0xaa0003e1,0xd65f03c0}});assert(!identify(unsafe));
 unsafe.back().words[4]=0xf85f83a0;auto extra=identify(unsafe);assert(extra&&extra->calls.size()==2);
 auto inbound=f;inbound.push_back({va,{0xa9bf79fd,0xaa0f03fd,0x14000000u|(uint32_t((int64_t(oracle_fold)+8-va-8)/4)&0x3ffffffu)}});assert(!identify(inbound));
 // Keep the sparse executable below the parser's 64 MiB bound so this tests
 // graph ambiguity rather than merely rejecting an oversized synthetic ELF.
 auto clones=copy;
 for(auto& x:clones){const auto previous=x.va;x.va-=0x01000000;
  for(size_t i=0;i<x.words.size();++i){uint32_t to=0;if(!home_dynamic::target(x.words[i],previous+uint32_t(i*4),to))continue;
   if(to>=0x05000000&&to<0x06000000)to-=0x01000000;
   x.words[i]=0x94000000u|(uint32_t((int64_t(to)-x.va-i*4)/4)&0x3ffffffu);}}
 auto ambiguous=f;ambiguous.insert(ambiguous.end(),clones.begin(),clones.end());assert(!identify(ambiguous));
 std::cout<<"clear disable runtime graph PASS: linked fold/controller/overlay, independent relocation/fields, 54 corruptions, missing dependencies, live result/unsafe inbound rejection, ambiguity\n";
}
