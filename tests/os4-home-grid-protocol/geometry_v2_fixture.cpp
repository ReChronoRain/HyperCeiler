// Complete original Home Dart variable/fixed grid-size functions under QEMU.
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
extern "C" void geometry_context(),geometry_null(),geometry_add();
struct Call {uintptr_t entry,handler,context,stack,pool,heap,null,thread,returned;double result;};
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
int main(int argc,char**argv){assert(argc==2||argc==3);std::string mutation=argc==3?argv[2]:"";Image i(argv[1]);
 i.jump(0x1b56b78,uintptr_t(geometry_context));i.word(0xa4418c,0xd65f03c0); // controlled already-aligned pixel transform
 i.jump(0x1b56f3c,uintptr_t(geometry_null));i.jump(0x8a8810,uintptr_t(geometry_null));i.jump(0x18a3aa4,uintptr_t(geometry_add));
 uint32_t original;memcpy(&original,i.base+0x15224ac,4);check(original==0xcb040005,"row candidate is exact original SUB instruction");
 if(mutation=="stale-row-remainder")i.word(0x1522560,0xd28000e1); // force original-like 7 only after candidate row publication
 if(mutation=="lost-column-input")i.word(0x15222c8,0xd2800083);
 for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++){
  ++cases;Heap h;const uint32_t mov=0xd2800005u|(uint32_t(rows)<<5);i.word(0x15224ac,mutation=="skip-candidate"?original:mov);i.sync();
  const double result=h.run(i,0x152228c,cols,rows,layout);const double cell=120.0/cols;const double want=260.0-16.0-cell*(rows+1);
  check(h.get<int64_t>(h.handler,0x7b)==rows,"candidate row reaches original handler field");check(std::abs(h.get<double>(h.handler,0xb)-cell)<1e-9,"original cell geometry consumes selected columns");check(std::abs(result-want)<1e-9,"original remainder uses candidate row before return");
 }
 i.word(0x15224ac,original);i.word(0x1522560,0xf847b001);i.word(0x15222c8,0xf841b043);i.sync();
 // The no-patch auto policy remains a distinct control. A positive context row
 // alone cannot select fixedRows: the production caller branches on an enum.
 for(int layout=0;layout<3;layout++){++cases;Heap h;const double v=h.run(i,0x152228c,4,12,layout);const int auto_rows=7-(layout==1);check(h.get<int64_t>(h.handler,0x7b)==auto_rows,"unmodified variable path ignores positive fixedRows field");check(std::abs(v-(214.0-auto_rows*30.0))<1e-9,"unmodified variable geometry remains original");}
 for(int rows=4;rows<=13;rows++){++cases;Heap h;const double v=h.run(i,0x15228e8,4,rows,0);const double cell=std::min(30.0,244.0/(rows+1));check(h.get<int64_t>(h.handler,0x7b)==rows,"original fixedRows consumes typed config Smi");check(std::abs(h.get<double>(h.handler,0xb)-cell)<1e-9&&std::abs(v-(244.0-cell*(rows+1)))<1e-9,"fixedRows has its own original shrink and spacing policy");}

 // Candidate two-word decoder substitution preserves both original Smi and
 // Mint branches, null check and every downstream fixedRows operation.
 uint32_t sbfx,boxed;memcpy(&sbfx,i.base+0x1522918,4);memcpy(&boxed,i.base+0x1522920,4);
 check(sbfx==0x93417c64&&boxed==0xf8407064,"fixedRows two exact decode instructions");
 for(int rows=4;rows<=13;rows++)for(int repr=0;repr<2;repr++){
  ++cases;Heap h;const auto mint=h.obj(0x900);h.put<int64_t>(mint,7,6);
  i.word(0x1522918,0xd2800004u|(uint32_t(rows)<<5));i.word(0x1522920,mutation=="fixed-mint-not-overridden"?boxed:0xd2800004u|(uint32_t(rows)<<5));i.sync();
  // Prepare all ordinary context fields, then substitute original input's
  // fixedRows representation before entering the original method.
  h.put<double>(h.context,0xb,120.0);h.put<double>(h.context,0x13,260.0);h.put<int64_t>(h.context,0x1b,4);h.put<double>(h.handler,0x6b,0.0);
  h.put<uint32_t>(h.config,0x3b,repr?uint32_t(mint):12u);auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+0x15228e8),h.handler,h.context,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0};call_geometry(&c);
  const double cell=std::min(30.0,244.0/(rows+1));check(h.get<int64_t>(h.handler,0x7b)==rows,"fixedRows candidate replaces original six in both representations");check(std::abs(h.get<double>(h.handler,0xb)-cell)<1e-9&&std::abs(c.result-(244.0-cell*(rows+1)))<1e-9,"fixedRows candidate keeps original shrink and remainder");check(c.returned==top,"fixedRows candidate x15 restored");
 }
 i.word(0x1522918,sbfx);i.word(0x1522920,boxed);i.sync();
 {++cases;Heap h;h.run(i,0x15228e8,4,6,0);check(h.get<int64_t>(h.handler,0x7b)==6,"disabled restored instructions use original fixedRows input");}
 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
