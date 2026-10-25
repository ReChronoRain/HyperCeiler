/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/tweaks/ht_plan.h"
#include "targets/home/tweaks/symtab.h"
#include "targets/home/home_clear_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
#include <filesystem>
#include <cstring>
static unsigned symbol_requests=0;
namespace home_layout_engine {
bool ReadWholeFile(const char* path,std::vector<uint8_t>* out,uint64_t* mtime){std::ifstream f(path,std::ios::binary);if(!f)return false;*out={std::istreambuf_iterator<char>(f),{}};if(mtime)*mtime=0;return true;}
SymbolIndex& SymbolIndex::Instance(){static SymbolIndex i;return i;}
bool SymbolIndex::Find(const char*,uint32_t*,uint32_t*)const{++symbol_requests;return false;}
bool CodeView::ExecutableRangesOk()const{return image_.segmentCount>0;}
bool CodeView::RangeOk(uint32_t va,size_t len)const{for(size_t i=0;i<image_.segmentCount;++i){auto& s=image_.segments[i];if((s.flags&1)&&image_.base+va>=s.begin&&image_.base+va<=s.end&&len<=s.end-image_.base-va)return true;}return false;}
bool CodeView::ReadWords(uint32_t va,uint32_t*out,size_t count)const{if(!RangeOk(va,count*4))return false;memcpy(out,reinterpret_cast<void*>(image_.base+va),count*4);return true;}
bool CodeView::Word(uint32_t va,uint32_t*out)const{return ReadWords(va,out,1);}
bool CodeView::InText(uint32_t va)const{return RangeOk(va,4);}
bool CodeView::FindSignature(const FunctionSignature&,uint32_t*,int*)const{++symbol_requests;return false;}
bool CodeView::MatchSignatureAt(const FunctionSignature&,uint32_t)const{++symbol_requests;return false;}
const FunctionSignature* FindSignatureByName(const char*){++symbol_requests;return nullptr;}
double IconScaleValueFromCode(uint32_t code){const uint32_t b=(code>>6)&1,exp=((1-b)<<10)|((b?255:0)<<2)|((code>>4)&3);const uint64_t bits=(uint64_t((code>>7)&1)<<63)|(uint64_t(exp)<<52)|(uint64_t(code&15)<<48);double v;memcpy(&v,&bits,8);return v;}
}
int main(int argc,char**argv){assert(argc==2||argc==3);auto functions=fixture();
 if(argc==3){std::map<uint32_t,uint32_t> moved;functions=relocated(functions,moved);}
 auto raw=image(functions);auto c=Code::parse(raw);assert(c);auto p=clear_disable_feature(*c);assert(p);
 auto segments=nhk::elf::parse_program_segments(raw);assert(segments);uint64_t extent=0;for(auto&s:*segments)if(s.type==1)extent=std::max(extent,s.vaddr+s.memsz);
 std::vector<std::byte> mapped(extent+4096);home_layout_engine::Image native{};native.base=reinterpret_cast<uintptr_t>(mapped.data());
 for(const auto&s:*segments)if(s.type==1){memcpy(mapped.data()+s.vaddr,raw.data()+s.offset,s.filesz);native.segments[native.segmentCount++]={native.base+s.vaddr,native.base+s.vaddr+s.memsz,s.flags};}
 using namespace home_layout_engine;CodeView live(native);LocatedSites sites;sites.overlayVa=p->overlay;sites.foldVa=p->fold;sites.constWord=p->empty_widget;sites.noClearCount=uint32_t(p->calls.size());std::copy(p->calls.begin(),p->calls.end(),sites.noClearSites);sites.ok9=true;
 Config config;config.flags=1;config.enabled={9};PlanResult plan;DerivePatches(live,config,sites,&plan);assert(plan.ok9&&plan.patches.size()==p->calls.size()+4&&symbol_requests==0);
 std::vector<std::pair<uint32_t,uint32_t>> proof;for(const auto& patch:plan.patches)proof.emplace_back(patch.va,patch.expect);
 auto reader=[&](uint32_t va,uint32_t& word){return live.Word(va,&word);};
 assert(clear_patch_preimages_match(proof,reader));assert(!clear_patch_preimages_match({},reader));
 auto unavailable=proof;unavailable.emplace_back(0xfffffffcu,0);assert(!clear_patch_preimages_match(unavailable,reader));
 auto original=mapped;
 for(const auto& patch:plan.patches){uint32_t word=0;assert(live.Word(patch.va,&word)&&word==patch.expect);memcpy(mapped.data()+patch.va,&patch.patch,4);}
 for(const auto call:p->calls){uint32_t word=0;assert(live.Word(call,&word)&&word==0xd503201fu);}
 assert(!clear_patch_preimages_match(proof,reader));
 home_dynamic::Body fold;assert(c->body(p->fold,fold));
 std::ofstream out(argv[1]);out<<"{\"entry\":"<<p->fold<<",\"words\":[";for(size_t i=0;i<fold.size();++i){if(i)out<<',';uint32_t w=0;assert(live.Word(p->fold+uint32_t(i*4),&w));out<<w;}out<<"]}\n";
 // The normal patch journal restores the exact preimages; replaying its inverse
 // here verifies that enabling/disabling did not manufacture a new baseline.
 for(const auto& patch:plan.patches)memcpy(mapped.data()+patch.va,&patch.expect,4);assert(mapped==original);
 assert(clear_patch_preimages_match(proof,reader));
 uint32_t changed=0;memcpy(mapped.data()+proof.front().first,&changed,4);assert(!clear_patch_preimages_match(proof,reader));mapped=original;
 config.enabled={9,18};PlanResult both;DerivePatches(live,config,sites,&both);
 assert(both.ok9&&!both.ok18&&both.patches.size()==plan.patches.size());
 for(size_t i=0;i<plan.patches.size();++i)assert(both.patches[i].va==plan.patches[i].va&&both.patches[i].patch==plan.patches[i].patch&&both.patches[i].expect==plan.patches[i].expect);
 config.flags=0;PlanResult disabled;DerivePatches(live,config,sites,&disabled);assert(disabled.patches.empty()&&symbol_requests==0);
 config.flags=1;config.enabled.clear();DerivePatches(live,config,sites,&disabled);assert(disabled.patches.empty()&&symbol_requests==0);
 std::cout<<"clear disable production planner PASS: 5 exact preimages, original constant widget, disable/reset restoration, zero symbol/signature requests\n";
}
