/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_clear_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
static auto identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return clear_visibility_feature(*c);}
int main(){auto f=fixture();auto p=identify(f);assert(p&&p->sync==oracle_sync&&p->init==oracle_init&&p->reverse==oracle_reverse);
 std::map<uint32_t,uint32_t> moved;auto copy=relocated(f,moved);auto r=identify(copy);assert(r&&r->sync==moved.at(oracle_sync)&&r->init==moved.at(oracle_init));
 for(auto va:{oracle_sync,oracle_init,oracle_forward,oracle_reverse}){auto broken=f;std::erase_if(broken,[&](const auto& x){return x.va==va;});assert(!identify(broken));}
 const auto& body=std::find_if(f.begin(),f.end(),[](const auto& x){return x.va==oracle_sync;})->words;
 unsigned mutations=0;
 for(size_t n=0;n<body.size();++n){auto broken=f;auto& v=std::find_if(broken.begin(),broken.end(),[](const auto& x){return x.va==oracle_sync;})->words;v[n]=0xd503201fu;
  // Error stubs beyond the validated original return are deliberately untouched.
  if(n<=33){assert(!identify(broken));++mutations;}
 }
 // Move each related reference field, including its initializer publication.
 auto fields=f;
 for(auto& x:fields)if(x.va==oracle_sync||x.va==oracle_init)for(auto& w:x.words){
  const auto field=home_dynamic::imm9(w);
  if(field!=p->controller&&field!=p->visible&&field!=p->previous)continue;
  if((w&0xffe00c00u)==0xb8400000u||(w&0xffe00c00u)==0xb8000000u)w+=16u<<12;
 }
 auto shifted=identify(fields);assert(shifted&&shifted->controller==p->controller+16&&shifted->visible==p->visible+16);
 auto ambiguous=f;std::map<uint32_t,uint32_t> clones{{oracle_sync,oracle_sync+0x2000000},{oracle_init,oracle_init+0x2000000}};
 for(const auto& x:f)if(clones.count(x.va)){auto y=x;y.va=clones.at(x.va);for(size_t i=0;i<y.words.size();++i){uint32_t to=0;if(home_dynamic::target(y.words[i],x.va+uint32_t(i*4),to))y.words[i]=0x94000000u|(uint32_t((int64_t(relocate(to,clones))-y.va-i*4)/4)&0x3ffffffu);}ambiguous.push_back(y);}
 assert(!identify(ambiguous));
 std::cout<<"clear visibility runtime graph PASS: unique graph, independent relocation/fields, missing dependencies, "<<mutations<<" corrupted oracle_sync words, ambiguity\n";
}
