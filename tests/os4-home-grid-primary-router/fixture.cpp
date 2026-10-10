// Fixture object helpers derived from frozen spacing_v3_fixture.cpp; see runner hash binding.
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
 uint8_t*mem;std::array<uint8_t,0x35000>pool{};std::array<uint8_t,0x1000>thread{};std::array<uint8_t,0x3000>table{};std::array<uint64_t,2048>stack{};
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
extern "C" void spacing_copy_context(), spacing_copy_insets(), spacing_provider();
extern "C" double spacing_trunc(double value){ return std::trunc(value); }
extern "C" double spacing_round(double value){ return std::round(value); }

struct Result { double cell,stride,top,bottom; int rows; };

#include <dlfcn.h>
extern "C" void primary_provider(),primary_init_async(),primary_return_async(),primary_original_null_throw();
using Configure=void(*)(const uintptr_t*);
extern "C" void primary_observe_entry(),primary_observe_accept(),primary_observe_original(),primary_observe_variable();
static bool instrument=false;
static Configure configure;
static uint64_t* feature;
alignas(8) static uint64_t epoch;
static uintptr_t router;
static uint8_t* trampoline;
static std::array<uint8_t,16> fixed_head;
static void publish(uint64_t e,uint64_t key){__atomic_store_n(&epoch,e,__ATOMIC_RELEASE);__atomic_store_n(feature,key,__ATOMIC_RELEASE);}
static void config(Image&i,uint64_t key){const uintptr_t a[]={uintptr_t(i.base+0x1abec34),uintptr_t(i.base+0x1abec24),instrument?uintptr_t(primary_observe_original):uintptr_t(trampoline),instrument?uintptr_t(primary_observe_accept):uintptr_t(i.base+0x1522924),instrument?uintptr_t(primary_observe_variable):uintptr_t(i.base+0x152228c),uintptr_t(&epoch),key};configure(a);}
struct Outcome {Result result;uint64_t event_count,saved_lr;};
// route0 uses the real none BL; route1 uses the real natural-fixed BL;
// route2 calls fixed directly from the host-to-Dart wrapper (unknown caller).
static Outcome run_case(Image&i,int cols,int original_rows,int selected_rows,int layout,double dpr,int kind,int route,int change,bool use_router=true){
 ++cases;Heap h;std::array<uintptr_t,9> capture{};std::array<uint64_t,8> event{};std::array<uint64_t,256> boundary{};
 const auto insets=h.obj(0xc00),rect=h.obj(0xd00),dims=h.obj(0xe00),desc=h.obj(0xf00);
 h.ptr(h.context,0x3b,insets);h.ptr(h.context,0x3f,rect);h.put<double>(insets,0xf,2.0);h.put<double>(rect,7,0.0);h.put<double>(rect,0x17,360.0);
 h.put<uintptr_t>(uintptr_t(h.table.data()),0x2700,dims);h.put<double>(dims,7,0.0);h.put<double>(dims,0xf,0.0);h.put<double>(dims,0x27,0.0);
 h.put<uintptr_t>(uintptr_t(h.thread.data()),0x80,uintptr_t(capture.data()));h.put<uintptr_t>(uintptr_t(h.thread.data()),0x90,uintptr_t(event.data()));
 h.put<uintptr_t>(uintptr_t(h.thread.data()),0x98,uintptr_t(boundary.data()));h.put<uintptr_t>(uintptr_t(h.thread.data()),0xa0,router);h.put<uintptr_t>(uintptr_t(h.thread.data()),0xa8,uintptr_t(i.base+0x1522924));h.put<uintptr_t>(uintptr_t(h.thread.data()),0xb0,uintptr_t(trampoline));h.put<uintptr_t>(uintptr_t(h.thread.data()),0xb8,uintptr_t(i.base+0x152228c));
 h.pp(0x33ec0,desc);h.pp(0x33f60,desc+16);h.pp(0x33eb8,desc+32);h.pp(0x33f58,desc+48);h.pp(0x33ea8,h.other+32);
 const auto named=h.obj(0x1100),normal=h.obj(0x1180),arg_name=h.obj(0x1200),provider=h.obj(0x1280),view=h.obj(0x1300),mint=h.obj(0x1400);
 h.pp(0x27790,named);h.pp(0x418,normal);h.pp(0x276b8,arg_name);h.put<uint32_t>(named,0x13,4);h.ptr(named,0x1f,arg_name);h.put<uint32_t>(named,0x23,2);h.put<uint32_t>(normal,0x13,2);h.ptr(normal,0x1f,h.n);
 h.put<uintptr_t>(uintptr_t(h.table.data()),0xa08,provider);h.ptr(provider,0x17,h.n);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x88,provider);
 h.put<uint64_t>(provider,size_t(-1),uint64_t(0x2390)<<12);h.ptr(provider,0x13,view);h.put<double>(view,0x13,dpr);
 h.put<uintptr_t>(uintptr_t(h.thread.data()),0x750,uintptr_t(spacing_trunc));h.put<uintptr_t>(uintptr_t(h.thread.data()),0x758,uintptr_t(spacing_round));
 for(auto p:std::array<std::pair<size_t,uint64_t>,4>{{{0x26fc8,4584304132692975288ULL},{0x27708,4607569728367971271ULL},{0x27710,4596373779694328216ULL},{0x27718,4590861373750426736ULL}}})h.pp(p.first,p.second);
 h.put<double>(h.context,0xb,360.0);h.put<double>(h.context,0x13,800.0);h.put<int64_t>(h.context,0x1b,cols);
 if(kind==0)h.ptr(h.config,0x3b,h.n);else if(kind==1)h.put<uint32_t>(h.config,0x3b,uint32_t(original_rows*2));else{h.put<int64_t>(mint,7,original_rows);h.ptr(h.config,0x3b,mint);}
 h.ptr(h.config,0x37,h.layout+16*layout);h.put<int64_t>(h.layout+16*layout,0x13,std::array<int64_t,3>{1,0,2}[layout]);
 h.ptr(h.config,0x27,route==0?h.none:h.other);
 std::array<uint8_t,128> before{};memcpy(before.data(),reinterpret_cast<void*>(h.config),before.size());
 event[0]=uintptr_t(i.base+0x1522958);event[1]=change==1?uintptr_t(&epoch):change==2?uintptr_t(feature):0;event[2]=change==1?2:0;
 if(!use_router){memcpy(i.base+0x15228e8,fixed_head.data(),16);i.sync();}
 const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+(route==2?0x15228e8:0x1abeb94)),h.handler,h.context,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0};
 call_geometry(&c);check(c.returned==top,"original handle/fixed restores original Dart x15");
 if(route==2){c.entry=uintptr_t(i.base+0x1520f40);c.input=c.result;call_geometry(&c);check(c.returned==top,"unknown fixed tail restores x15");}
 if(!use_router){i.jump(0x15228e8,router);i.sync();}
 check(memcmp(before.data(),reinterpret_cast<void*>(h.config),before.size())==0,"nullable config object and original selected-row field stay unchanged");
 check(capture[8]==1,"original complete handle reaches full-tail copyWith once");check(c.integer_result==h.context,"original continuation returns tail observer result");
 auto dbl=[&](int j){return h.get<double>(capture[j],7);};
 const int actual_rows=int(int64_t(capture[5])>>1);check(actual_rows==h.get<int64_t>(h.handler,0x7b),"same accepted row reaches original tail");
 for(int j:{0,2,3,4,6,7})check(std::isfinite(dbl(j)),"original tail output finite");
 if(change){check(event[3]==1,"revoke occurs in first original fixed alignPixel after admission");check(event[4]==uintptr_t(i.base+(route==0?0x1abec34:0x1abec24)),"saved LR remains exact original caller continuation");check(event[5]==h.handler&&event[6]==h.context&&event[7]==h.config,"original fixed frame saves exact typed entry objects");check(actual_rows==selected_rows,"admitted frame completes selected rows after revoke");}
 if(instrument&&use_router){
  const bool accepted=boundary[128+97]==1;
  for(int reg=0;reg<=30;reg++)if(reg!=16&&reg!=17&&(!accepted||(reg!=0&&reg!=3&&reg!=4&&reg!=15&&reg!=29)))check(boundary[reg]==boundary[128+reg],"router preserves every live GPR including x6 LR heap pool and Thread");
  check(boundary[31]==boundary[159],"router leaves native SP unchanged");check(memcmp(reinterpret_cast<uint8_t*>(boundary.data())+0x100,reinterpret_cast<uint8_t*>(boundary.data())+0x500,512)==0,"router preserves all 32 complete Q registers");
  if(accepted){
   check(boundary[128]==h.config&&boundary[132]==uintptr_t(selected_rows),"accepted prefix exposes original config and selected x4");
   check(boundary[128+15]==boundary[15]-64&&boundary[128+29]==boundary[15]-16,"accepted prefix uses exact original frame extent");
   const uint32_t a=uint32_t(boundary[131]),b=uint32_t(h.n),d=a-b;const uint64_t flags=(uint64_t(d>>31)<<31)|(uint64_t(d==0)<<30)|(uint64_t(a>=b)<<29)|(uint64_t(((a^b)&(a^d))>>31)<<28);
   check(boundary[128+96]==flags,"accepted prefix retains original CMP NZCV");
  }else check(boundary[96]==boundary[128+96],"denied and foreign routing preserves incoming NZCV");
 }
 return{{dbl(7),dbl(6),h.get<double>(h.handler,0x3b),h.get<double>(h.handler,0x43),actual_rows},event[3],event[4]};
}
static void same(const Outcome&a,const Outcome&b,const char*label){check(a.result.rows==b.result.rows&&std::abs(a.result.cell-b.result.cell)<1e-8&&std::abs(a.result.stride-b.result.stride)<1e-8&&std::abs(a.result.top-b.result.top)<1e-8&&std::abs(a.result.bottom-b.result.bottom)<1e-8,label);}
int main(int argc,char**argv){
 assert(argc==3||argc==4);const std::string mode=argc==4?argv[3]:"";Image i(argv[1]);void* so=dlopen(argv[2],RTLD_NOW|RTLD_LOCAL);if(!so){std::cerr<<dlerror()<<'\n';return 2;}
 router=uintptr_t(dlsym(so,"primary_router"));configure=reinterpret_cast<Configure>(dlsym(so,"primary_test_configure"));auto fa=reinterpret_cast<uint64_t*(*)()>(dlsym(so,"primary_test_feature_address"));assert(router&&configure&&fa);feature=fa();
 memcpy(fixed_head.data(),i.base+0x15228e8,16);trampoline=static_cast<uint8_t*>(mmap(nullptr,4096,7,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(trampoline!=MAP_FAILED);memcpy(trampoline,fixed_head.data(),16);uint32_t jump[]={0x58000050,0xd61f0200};memcpy(trampoline+16,jump,8);uintptr_t resume=uintptr_t(i.base+0x15228f8);memcpy(trampoline+24,&resume,8);__builtin___clear_cache(reinterpret_cast<char*>(trampoline),reinterpret_cast<char*>(trampoline+32));
 const std::array<std::pair<size_t,size_t>,6> bodies{{{0x152228c,1244},{0x15228e8,640},{0x1520f40,2392},{0x1521898,276},{0xa4418c,560},{0x1abeb94,176}}};std::vector<std::vector<uint8_t>> originals;for(auto[v,n]:bodies)originals.emplace_back(i.base+v,i.base+v+n);
 const std::array<std::pair<size_t,uintptr_t>,11> adapters{{{0x1b56b78,uintptr_t(geometry_context)},{0x1b56f3c,uintptr_t(geometry_null)},{0x8a8810,uintptr_t(geometry_null)},{0x18a3aa4,uintptr_t(geometry_add)},{0x151dd28,uintptr_t(spacing_copy_context)},{0x151e6a0,uintptr_t(spacing_copy_insets)},{0x18aa0e0,uintptr_t(geometry_context)},{0xe1d028,uintptr_t(primary_provider)},{0x18ac658,uintptr_t(primary_init_async)},{0x18ac364,uintptr_t(primary_return_async)},{0x1b58398,uintptr_t(primary_original_null_throw)}}};
 for(auto[va,p]:adapters){for(auto[v,n]:bodies)check(va+16<=v||va>=v+n,"adapter disjoint from complete tested body");i.jump(va,p);}
 i.jump(0x15228e8,router);i.word(0x1abec30,0x97e98f2e);i.sync(); // exact redirected BL to fixed entry
 const uint64_t k=(5<<8)|9;config(i,k);publish(1,k);
 if(mode=="null-natural-off"||mode=="null-foreign-on"){if(mode=="null-natural-off")publish(2,k);run_case(i,5,6,9,0,2.75,0,mode=="null-natural-off"?1:2,0);check(false,"original null failure must not return normally");}
 if(mode=="abi-only"){
  instrument=true;i.jump(0x15228e8,uintptr_t(primary_observe_entry));config(i,k);i.sync();
  for(int route:{0,1,2})for(bool enabled:{false,true}){publish(enabled?1:2,k);run_case(i,5,6,9,0,2.75,1,route,0);}
  std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"device_status\":\"NOT_TESTED\"}\n";return 0;
 }
 for(int cols:{3,9})for(int rows:{4,8,13})for(int layout:{0,1,2})for(double dpr:{1.0,2.75})for(int kind:{0,1,2})for(int route:{0,1}){
  const uint64_t key=(uint64_t(cols)<<8)|rows;config(i,key);publish(1,key);const auto a=run_case(i,cols,6,rows,layout,dpr,kind,route,0);
  check(a.result.rows==rows,"enabled router selects rows for null Smi and Mint without rewriting object");
  // Reference is the original natural-fixed route with the same aspect and
  // an ordinary Smi row, not a rewritten function or a synthetic arithmetic oracle.
  publish(0,0);const auto b=run_case(i,cols,rows,rows,layout,dpr,1,route==0?0:1,0,false);
  // The none BL is deliberately retained as fixed even for this reference.
  same(a,b,"enabled result equals complete original fixed and full tail with selected row");
 }
 config(i,k);
 for(int route:{0,1,2})for(int kind:{1,2}){
  publish(2,k);auto off=run_case(i,5,6,9,0,2.75,kind,route,0);publish(0,0);
  // Original-none reference restores only its original call target.
  if(route==0){i.word(0x1abec30,0x97e98d97);i.sync();}
  auto ref=run_case(i,5,6,9,0,2.75,kind,route,0,false);
  if(route==0){i.word(0x1abec30,0x97e98f2e);i.sync();}
  same(off,ref,"epoch closed future call selects original variable or natural fixed");
  publish(1,0);auto feature_off=run_case(i,5,6,9,0,2.75,kind,route,0);same(feature_off,ref,"feature-only close works while shared epoch remains odd");
  publish(1,k);auto on=run_case(i,5,6,9,0,2.75,kind,route,0);check(on.result.rows==(route==2?6:9),"only exact two original callers can adopt config");
 }
 for(int change:{1,2})for(int route:{0,1}){
  publish(1,k);auto a=run_case(i,5,6,9,0,2.75,0,route,change);check(a.result.rows==9,"once admitted result survives both revoke kinds");
  if(route==0){auto next=run_case(i,5,6,9,0,2.75,0,0,0);i.word(0x1abec30,0x97e98d97);i.sync();auto ref=run_case(i,5,6,9,0,2.75,0,0,0,false);i.word(0x1abec30,0x97e98f2e);i.sync();same(next,ref,"future none with null config goes back to original variable after revoke");}
 }
 for(uint64_t invalid:{0ULL,3ULL,14ULL,0x204ULL,0xa04ULL,0x303ULL,0x30eULL,0x1000304ULL,~0ULL}){
  publish(1,invalid);auto a=run_case(i,5,6,9,0,2.75,1,1,0);check(a.result.rows==6,"invalid packed key falls back instead of adopting a row");
 }
 // Boundary-only observers preserve all arguments and branch to actual targets.
 instrument=true;i.jump(0x15228e8,uintptr_t(primary_observe_entry));config(i,k);i.sync();
 for(int route:{0,1,2})for(bool enabled:{false,true}){publish(enabled?1:2,k);run_case(i,5,6,9,0,2.75,1,route,0);}
 instrument=false;i.jump(0x15228e8,router);config(i,k);i.sync();
 // Explicit stale publication: nonzero key after even epoch must stay disabled.
 publish(2,k);auto late=run_case(i,5,6,9,0,2.75,1,1,0);check(late.result.rows==6,"late feature publication cannot override global revoked epoch");
 memcpy(i.base+0x15228e8,fixed_head.data(),16);i.word(0x1abec30,0x97e98d97);i.sync();for(size_t n=0;n<bodies.size();n++)check(memcmp(i.base+bodies[n].first,originals[n].data(),bodies[n].second)==0,"all original body bytes preserved except explicit entry and caller journal");
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"device_status\":\"NOT_TESTED\"}\n";return 0;
}
