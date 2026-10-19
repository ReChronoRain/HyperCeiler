// Full original grid arithmetic and vertical spacing tail; explicit copyWith observers.
// External allocation, pixel-alignment and logging are declared adapters.
#include <algorithm>
#include <array>
#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <string>
#include <sys/mman.h>
#include <vector>
static unsigned checks=0,cases=0;
static void check(bool b,const char*s){++checks;if(!b){std::cerr<<"FAILED: "<<s<<" case="<<cases<<"\n";std::exit(3);}}
struct Image {
 uint8_t*base;size_t size=0;std::vector<std::pair<size_t,std::array<uint8_t,16>>>adapters;
 explicit Image(const char*path){std::ifstream f(path,std::ios::binary);std::vector<uint8_t>b((std::istreambuf_iterator<char>(f)),{});assert(b.size()>64);auto*h=reinterpret_cast<Elf64_Ehdr*>(b.data());assert(h->e_machine==EM_AARCH64);auto*p=reinterpret_cast<Elf64_Phdr*>(b.data()+h->e_phoff);for(int j=0;j<h->e_phnum;j++)if(p[j].p_type==PT_LOAD)size=std::max(size,size_t(p[j].p_vaddr+p[j].p_memsz));size=(size+4095)&~size_t(4095);base=static_cast<uint8_t*>(mmap(nullptr,size,7,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(base!=MAP_FAILED);for(int j=0;j<h->e_phnum;j++)if(p[j].p_type==PT_LOAD)memcpy(base+p[j].p_vaddr,b.data()+p[j].p_offset,p[j].p_filesz);}
 ~Image(){munmap(base,size);}void word(size_t a,uint32_t w){memcpy(base+a,&w,4);}void jump(size_t a,uintptr_t v){word(a,0x58000050);word(a+4,0xd61f0200);memcpy(base+a+8,&v,8);}void sync(){__builtin___clear_cache(reinterpret_cast<char*>(base),reinterpret_cast<char*>(base+size));}
};
extern "C" void geometry_context(),geometry_null(),geometry_add(),geometry_rx();
struct Call {uintptr_t entry,handler,context,stack,pool,heap,null,thread,returned;double result;double input=0;uintptr_t integer_result=0;};
extern "C" void call_geometry(Call*);
struct Heap {
 uint8_t*mem;std::array<uint8_t,0x35000>pool{};std::array<uint8_t,0x100>thread{};std::array<uint8_t,0x3000>table{};std::array<uint64_t,2048>stack{};
 uintptr_t handler,context,config,none,layout,screen,other,n;
 Heap(){mem=static_cast<uint8_t*>(mmap(nullptr,65536,3,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(mem!=MAP_FAILED);assert((uintptr_t(mem)>>32)==((uintptr_t(mem)+65535)>>32));n=obj(0);handler=obj(0x100);context=obj(0x300);config=obj(0x400);none=obj(0x500);layout=obj(0x600);screen=obj(0x700);other=obj(0x800);
  ptr(context,7,config);ptr(config,0x27,none);ptr(config,0x2b,other);ptr(config,0x37,layout);ptr(config,0x33,screen);
  put<uintptr_t>(uintptr_t(thread.data()),0x60,uintptr_t(mem+0x3000));put<uintptr_t>(uintptr_t(thread.data()),0x68,uintptr_t(mem+0xf000));put<uintptr_t>(uintptr_t(thread.data()),0x78,uintptr_t(table.data()));put<uintptr_t>(uintptr_t(table.data()),0x2490,other);
  pp(0xf240,none);pp(0x14080,other);pp(0x14078,other+0x10);pp(0x153c8,screen+0x10);pp(0x155c8,screen+0x20);pp(0x15648,layout);pp(0x15650,layout+0x10);pp(0x15658,layout+0x20);pp(0x40,n);
 }
 ~Heap(){munmap(mem,65536);}uintptr_t obj(unsigned o){return uintptr_t(mem+o)+1;}
 template<class T>void put(uintptr_t p,size_t o,T v){memcpy(reinterpret_cast<void*>(p+o),&v,sizeof v);}template<class T>T get(uintptr_t p,size_t o){T v;memcpy(&v,reinterpret_cast<void*>(p+o),sizeof v);return v;}
 void ptr(uintptr_t p,size_t o,uintptr_t v){put<uint32_t>(p,o,uint32_t(v));}void pp(size_t o,uintptr_t v){memcpy(pool.data()+o,&v,8);}
 double run(Image&i,size_t va,int cols,int rows,int layout_kind){put<double>(context,0xb,120.0);put<double>(context,0x13,260.0);put<int64_t>(context,0x1b,cols);put<double>(handler,0x6b,0.0);ptr(config,0x37,layout+16*layout_kind);put<uint32_t>(config,0x3b,rows*2);auto top=uintptr_t(&stack[1800]);Call c{uintptr_t(i.base+va),handler,context,top,uintptr_t(pool.data()),uintptr_t(mem)>>32,n,uintptr_t(thread.data()),0,0};call_geometry(&c);check(c.returned==top,"original Dart function restores x15");return c.result;}
};
extern "C" void spacing_copy_context(), spacing_copy_insets();
struct Result { double cell,stride,top,bottom; int rows; };
static Result sequence(Image &i,int cols,int rows,int layout,double width,double height,bool fixed) {
 ++cases;Heap h;std::array<uintptr_t,9>capture{};
 const auto insets=h.obj(0xc00),rect=h.obj(0xd00),dims=h.obj(0xe00),desc=h.obj(0xf00);
 h.ptr(h.context,0x3b,insets);h.ptr(h.context,0x3f,rect);
 h.put<double>(insets,0xf,2.0);h.put<double>(rect,7,0.0);h.put<double>(rect,0x17,width);
 h.put<uintptr_t>(uintptr_t(h.table.data()),0x2700,dims);
 h.put<double>(dims,7,0.0);h.put<double>(dims,0xf,0.0);h.put<double>(dims,0x27,0.0);
 h.put<uintptr_t>(uintptr_t(h.thread.data()),0x80,uintptr_t(capture.data()));
 h.pp(0x33ec0,desc);h.pp(0x33f60,desc+16);h.pp(0x33eb8,desc+32);h.pp(0x33f58,desc+48);
 h.pp(0x33ea8,h.other+32);
 for(auto entry:std::array<std::pair<size_t,uint64_t>,3>{{{0x27708,4607569728367971271ULL},{0x27710,4596373779694328216ULL},{0x27718,4590861373750426736ULL}}})h.pp(entry.first,entry.second);
 h.put<double>(h.context,0xb,width);h.put<double>(h.context,0x13,height);h.put<int64_t>(h.context,0x1b,cols);
 h.put<uint32_t>(h.config,0x3b,uint32_t(rows*2));h.ptr(h.config,0x37,h.layout+16*layout);
 h.put<int64_t>(h.layout+16*layout,0x13,layout);
 auto top=uintptr_t(&h.stack[1800]);
 Call c{uintptr_t(i.base+(fixed?0x15228e8:0x152228c)),h.handler,h.context,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0};
 call_geometry(&c);check(c.returned==top,"size function restores Dart stack");
 const auto frozen_enum=h.get<uint32_t>(h.config,0x27);
 c.entry=uintptr_t(i.base+0x1520f40);c.input=c.result;c.returned=0;call_geometry(&c);
 check(c.returned==top,"full spacing tail restores Dart stack");
 check(capture[8]==1,"full spacing tail reaches final copyWith observer once");
 check(c.integer_result==h.context,"copyWith observer return flows through original tail");
 check(h.get<uint32_t>(h.config,0x27)==frozen_enum,"original aspect enum remains unchanged");
 auto dbl=[&](int index){return h.get<double>(capture[index],7);};
 const int captured_rows=int(int64_t(capture[5])>>1);
 check(captured_rows==h.get<int64_t>(h.handler,0x7b),"final copyWith receives original handler row as Smi");
 check(std::abs(dbl(7)-h.get<double>(h.handler,0xb))<1e-9,"final copyWith receives calculated cell side");
 check(std::abs(dbl(6)-h.get<double>(h.handler,0x13))<1e-9,"final copyWith receives calculated vertical stride");
 for(int k:{0,2,3,4,6,7})check(std::isfinite(dbl(k)),"final copyWith numeric argument is finite");
 return{dbl(7),dbl(6),h.get<double>(h.handler,0x3b),h.get<double>(h.handler,0x43),captured_rows};
}
int main(int argc,char**argv){
 assert(argc==2||argc==3);const std::string mutation=argc==3?argv[2]:"";Image i(argv[1]);
 const std::array<std::pair<size_t,size_t>,4>tested{{{0x152228c,1244},{0x15228e8,640},{0x1520f40,2392},{0x1521898,276}}};
 std::vector<std::vector<uint8_t>>original;
 for(auto [va,n]:tested)original.emplace_back(i.base+va,i.base+va+n);
 const std::array<std::pair<size_t,uintptr_t>,6>adapters{{
  {0x1b56b78,uintptr_t(geometry_context)},{0x1b56f3c,uintptr_t(geometry_null)},
  {0x8a8810,uintptr_t(geometry_null)},{0x18a3aa4,uintptr_t(geometry_add)},
  {0x151dd28,uintptr_t(spacing_copy_context)},{0x151e6a0,uintptr_t(spacing_copy_insets)}}};
 for(auto [va,target]:adapters){for(auto [start,n]:tested)check(va+16<=start||va>=start+n,"adapter disjoint from all complete original bodies");i.jump(va,target);}
 i.word(0xa4418c,0xd65f03c0); // Pixel alignment identity; display DPR is outside this experiment.
 uint32_t row_sub;memcpy(&row_sub,i.base+0x15224ac,4);check(row_sub==0xcb040005,"old row-only recipe has exact SUB guard");
 if(mutation=="lost-tail-row")i.word(0x1521488,0xd28000c4); // final row becomes stale 6
 if(mutation=="lost-tail-cell")i.word(0x1521478,0x1e601003); // final cell becomes 2.0
 if(mutation=="skip-shrink")i.word(0x15229f4,0x4ea11c22); // mov v2,v1 keeps width-derived side
 i.sync();
 unsigned overlap_witnesses=0;std::array<Result,3>old_examples{};
 for(auto [width,height]:std::array<std::pair<double,double>,3>{{{120,260},{360,800},{440,960}}})
  for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++){
   const auto result=sequence(i,cols,rows,layout,width,height,true);
   check(result.rows==rows,"fixedRows and full tail preserve selected row");
   const double expected=std::min(width/cols,(height-16)/(rows+1));
   check(std::abs(result.cell-expected)<1e-8,"fixed policy retains OEM min width-height shrink through tail");
   check(result.stride+1e-8>=result.cell,"fixed policy vertical stride does not overlap cells");
   check(result.top>=-1e-8&&result.bottom>=-1e-8,"fixed policy keeps nonnegative residual margins");
  }
 // Concrete counterexample against the former row-only patch, with the very
 // same original spacing tail. It is evidence of a defect, not a pass for that recipe.
 i.word(0x15224ac,0xd28001a5);i.sync();
 for(int layout=0;layout<3;layout++){
  const auto old=sequence(i,3,13,layout,120,260,false);
  old_examples[layout]=old;
  if(old.stride+1e-8<old.cell||old.top < -1e-8||old.bottom < -1e-8)++overlap_witnesses;
 }
 check(overlap_witnesses==3,"old row-only candidate exposes overlap or negative margins in all tested layouts");
 i.word(0x15224ac,row_sub);i.sync();
 for(size_t k=0;k<tested.size();k++)check(memcmp(i.base+tested[k].first,original[k].data(),tested[k].second)==0,"complete tested bodies restored byte for byte");
 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"old_recipe_counterexamples\":"<<overlap_witnesses<<",\"old_recipe_values\":[";
 for(int k=0;k<3;k++){const auto&r=old_examples[k];if(k)std::cout<<',';std::cout<<"{\"layout\":"<<k<<",\"cell\":"<<r.cell<<",\"stride\":"<<r.stride<<",\"top\":"<<r.top<<",\"bottom\":"<<r.bottom<<"}";}
 std::cout<<"],\"device_status\":\"NOT_TESTED\"}\n";
}
