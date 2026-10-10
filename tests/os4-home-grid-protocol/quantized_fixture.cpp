// Derived test fixture only from frozen secondary-spacing fixture SHA 7ae5972ce38770b04bed27246592202b90759e4b8eb11437acce051bc24552e3; source remains untouched.
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
 uint8_t*base;size_t size=0,veneer=0;std::vector<std::pair<size_t,std::array<uint8_t,16>>>adapters;
 explicit Image(const char*path){std::ifstream f(path,std::ios::binary);std::vector<uint8_t>b((std::istreambuf_iterator<char>(f)),{});assert(b.size()>64);auto*h=reinterpret_cast<Elf64_Ehdr*>(b.data());assert(h->e_machine==EM_AARCH64);auto*p=reinterpret_cast<Elf64_Phdr*>(b.data()+h->e_phoff);for(int j=0;j<h->e_phnum;j++)if(p[j].p_type==PT_LOAD)size=std::max(size,size_t(p[j].p_vaddr+p[j].p_memsz));size=(size+4095)&~size_t(4095);veneer=size;size+=4096;base=static_cast<uint8_t*>(mmap(nullptr,size,7,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(base!=MAP_FAILED);for(int j=0;j<h->e_phnum;j++)if(p[j].p_type==PT_LOAD)memcpy(base+p[j].p_vaddr,b.data()+p[j].p_offset,p[j].p_filesz);}
 ~Image(){munmap(base,size);}void word(size_t a,uint32_t w){memcpy(base+a,&w,4);}void jump(size_t a,uintptr_t v){word(a,0x58000050);word(a+4,0xd61f0200);memcpy(base+a+8,&v,8);}void sync(){__builtin___clear_cache(reinterpret_cast<char*>(base),reinterpret_cast<char*>(base+size));}
};
extern "C" void geometry_context(),geometry_null(),geometry_add(),geometry_rx(),geometry_rx_set(),geometry_zero(),geometry_true(),geometry_sub(),geometry_async_end();
struct Call {uintptr_t entry,handler,context,stack,pool,heap,null,thread,returned;double result;double input=0;uintptr_t integer_result=0;uintptr_t x3=0,x5=0;double d1=0;};
extern "C" void call_geometry(Call*);
struct Heap {
 uint8_t*mem;std::array<uint8_t,0x35000>pool{};std::array<uint8_t,0x1000>thread{};std::array<uint8_t,0x5000>table{};std::array<uint64_t,2048>stack{};
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
extern "C" void secondary_array(),secondary_view();
extern "C" double secondary_trunc(double x){return std::trunc(x);}
extern "C" double secondary_round(double x){return std::round(x);}
static void close(double got,double want,const char* message){check(std::isfinite(got)&&std::abs(got-want)<1e-8,message);}
extern "C" const unsigned char grid_quantize_begin[],grid_quantize_end[];
int main(int argc,char**argv){
 assert(argc==2||argc==3);Image image(argv[1]);const std::string mutation=argc==3?argv[2]:"";
 const std::array<std::pair<size_t,size_t>,11> bodies{{{0xa49adc,900},{0xa49e60,260},{0xa49f64,1568},{0xa4a890,648},{0xa4a698,288},{0xa4a7b8,132},{0xa4a83c,84},{0xa4a674,36},{0xa4a650,36},{0xa4a584,204},{0xa4418c,560}}};
 std::vector<std::vector<uint8_t>> originals;for(auto [va,size]:bodies)originals.emplace_back(image.base+va,image.base+va+size);
 const std::array<std::pair<size_t,uintptr_t>,10> adapters{{
  {0x1b57c08,uintptr_t(secondary_array)},{0x18aa0e0,uintptr_t(geometry_context)},
  {0xe1d028,uintptr_t(secondary_view)},{0xb19564,uintptr_t(geometry_rx_set)},
  {0x1781f28,uintptr_t(geometry_true)},{0x18a363c,uintptr_t(geometry_sub)},
  {0x18a3aa4,uintptr_t(geometry_add)},{0x868b8c,uintptr_t(geometry_null)},
  {0x86af0c,uintptr_t(geometry_null)},{0xa4ac00,uintptr_t(geometry_zero)}}};
 for(auto [va,to]:adapters){for(auto [begin,size]:bodies)check(va+16<=begin||va>=begin+size,"declared adapter outside complete arithmetic bodies");image.jump(va,to);}
 // Fail at a named final dependency, not a crash/timeout. Original instruction
 // words are checked by the extraction/runner before this private mutation.
 if(mutation=="skip-tail")image.word(0xa49dec,0xd503201f);
 if(mutation=="skip-search-rx")image.word(0xa49f04,0xd503201f);
 if(mutation=="wrong-final-sum")image.word(0xa49f34,0x1e613802); // fsub d2,d0,d1
 // Exact local no-call splice: capture validated DPR returned in d1 by
 // original screenMarginBottom->alignPixel before d1 is reused for padding.
 const size_t sites[]={0xa49aec,0xa49bd4,0xa49c28,0xa49c80,0xa49b74,0xa49b78,0xa49b7c,0xa49c44};
 const uint32_t expected[]={0xaa0203e0,0xf85e83a0,0xf85e83a0,0xf85e83a4,0x4ea01c01,0xfc5d03a0,0x1e613802,0xf85f83a3};
 for(size_t z=0;z<8;z++){uint32_t word;memcpy(&word,image.base+sites[z],4);check(word==expected[z],"exact original local quantization site");}
 check(grid_quantize_end-grid_quantize_begin==24,"linked frameless quantization template has exactly six instructions");
 memcpy(image.base+image.veneer,grid_quantize_begin,24);
 auto branch=[](size_t from,size_t to){const int64_t d=int64_t(to)-int64_t(from);assert(d%4==0&&d>=-0x8000000&&d<0x8000000);return 0x14000000u|(uint32_t(d/4)&0x3ffffff);};
 image.word(image.veneer+20,branch(image.veneer+20,mutation=="lost-first-store"?0xa49c4c:0xa49c48));
 // Assembler-derived words supplied by the separate linked template include.
 uint32_t dpr_store;memcpy(&dpr_store,grid_quantize_end,4);
 uint32_t remaining_load;memcpy(&remaining_load,grid_quantize_end+4,4);
 uint32_t padding_subtract;memcpy(&padding_subtract,grid_quantize_end+8,4);
 uint32_t round_mutant;memcpy(&round_mutant,grid_quantize_end+12,4);
 image.word(0xa49b74,mutation=="missing-dpr-capture"?0xd503201f:dpr_store);image.word(0xa49b78,remaining_load);image.word(0xa49b7c,padding_subtract);image.word(0xa49c44,branch(0xa49c44,image.veneer));
 if(mutation=="round-instead-of-trunc")image.word(image.veneer+8,round_mutant);
 image.sync();
 unsigned negative_margin_cases=0,negative_stride_cases=0;double smallest_margin=0;
 // All 70 configured pairs, three explicit layout values, four controlled DPRs.
 // Two available geometries; selected booleans omit search/navigation padding.
 for(double dpr:std::array<double,8>{1,2,2.75,3,0,-2,INFINITY,NAN})for(auto [width,height]:std::array<std::pair<double,double>,2>{{{120,260},{360,800}}})
 for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++){
  ++cases;
  for(size_t z=0;z<4;z++){const unsigned reg=z==0?2:z==3?4:0;const int value=z==0?cols:(mutation=="old-row-cache"&&z==3?6:rows);image.word(sites[z],0xd2800000u|(uint32_t(value)<<5)|reg);}
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+0xa49aec),reinterpret_cast<char*>(image.base+0xa49c84));
  Heap h;auto thread=uintptr_t(h.thread.data()),table=uintptr_t(h.table.data());
  h.put<double>(h.handler,7,width);h.put<double>(h.handler,0xf,height);
  h.ptr(h.handler,0xdb,h.n+0x30);h.ptr(h.handler,0xdf,h.n+0x30);h.ptr(h.handler,0xe3,h.n+0x30);
  h.put<uintptr_t>(table,0x26f0,h.other);h.ptr(h.other,7,h.other);
  // Actual Rx.value executes with dependency collection inactive; setter is
  // an explicit storage observer, not Dart stream notification execution.
  h.put<uintptr_t>(table,0x1d30,h.n);
  for(unsigned off:std::array<unsigned,4>{0x3f,0x43,0x7f,0x83}){
   const auto rx=h.obj(0x900+off*16),box=h.obj(0x2000+off*8);
   h.ptr(h.handler,off,rx);h.put<double>(box,7,0);h.ptr(rx,0x13,box);
  }
  const auto rowrx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));h.put<uint32_t>(rowrx,0x13,12);
  const auto enumobj=h.obj(0x2900);h.put<int64_t>(enumobj,7,layout);h.put<uintptr_t>(table,0x4ee0,enumobj);
  const auto descriptor=h.obj(0x2a00),provider=h.obj(0x2b00),view=h.obj(0x2c00);
  h.pp(0x418,descriptor);h.put<uint32_t>(descriptor,0x13,2);h.ptr(descriptor,0x1f,h.n);h.pp(0x276b8,h.other+0x10);
  h.put<uintptr_t>(table,0xa08,provider);h.ptr(provider,0x17,h.n);h.put<uint64_t>(provider,size_t(-1),uint64_t(0x2390)<<12);h.ptr(provider,0x13,view);h.put<double>(view,0x13,dpr);
  h.put<uintptr_t>(thread,0x88,provider);h.put<uintptr_t>(thread,0x750,uintptr_t(secondary_trunc));h.put<uintptr_t>(thread,0x758,uintptr_t(secondary_round));
  for(auto [off,bits]:std::array<std::pair<size_t,uint64_t>,3>{{{0x27708,4607569728367971271ULL},{0x27710,4596373779694328216ULL},{0x27718,4590861373750426736ULL}}})h.pp(off,bits);
  const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(image.base+0xa49adc),h.handler,uintptr_t(4),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,uintptr_t(6),uintptr_t(layout),0};
  call_geometry(&c);check(c.returned==top,"complete fixed to tail original calls restore Dart stack");
  check(h.get<uintptr_t>(thread,0x90)==4,"original fixed and tail perform four Rx publication callsites");
  check(h.get<uint32_t>(rowrx,0x13)==uint32_t(rows*2),"row Rx retains original selected Smi publication");
  const double effective_dpr=std::isfinite(dpr)&&dpr>0?dpr:1;
  auto quant=[&](double v){return std::round(v*effective_dpr)/effective_dpr;};
  auto rxd=[&](unsigned off){const auto rx=h.get<uint32_t>(h.handler,off)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));const auto box=h.get<uint32_t>(rx,0x13)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));return h.get<double>(box,7);};
  const double cell=h.get<double>(h.handler,0x47),stride=h.get<double>(h.handler,0x4f);
  close(cell,std::trunc(std::min(width/cols,height/(rows+1))*effective_dpr)/effective_dpr,"candidate cell is pixel-truncated before original publication");
  const double remaining=height-quant(cell)*(rows+1);
  const double margin=(layout<2?remaining-(quant(stride)-quant(cell))*(rows+1):remaining)/2;
  close(h.get<double>(h.handler,0x37),margin,"original top margin consumes pixel rounded remaining height");
  close(h.get<double>(h.handler,0x67),margin,"original bottom margin consumes pixel rounded remaining height");
  close(rxd(0x3f),margin,"top Rx receives calculated margin");close(rxd(0x7f),margin,"bottom Rx receives calculated margin");
  close(rxd(0x83),quant(h.get<double>(h.handler,0x9f))+quant(h.get<double>(h.handler,0x87)),"search Rx receives original rounded operands");
  close(h.get<double>(h.handler,0x57),rxd(0x83)+rxd(0x7f),"final field sum uses both current Rx values");
  if(margin< -1e-8)++negative_margin_cases;
  if(stride+1e-8<cell)++negative_stride_cases;
  smallest_margin=std::min(smallest_margin,margin);
  if(cols==3&&rows==13&&width==120&&dpr==1){
   check(margin>=-1e-8&&stride+1e-8>=cell,"former 3x13 counterexample has bounded final spacing");
   std::cerr<<"SAMPLE layout="<<layout<<" cell="<<cell<<" pixel_cell="<<quant(cell)<<" stride="<<stride<<" remaining="<<remaining<<" margin="<<margin<<" final="<<h.get<double>(h.handler,0x57)<<"\n";
  }
 }
 for(size_t z=0;z<8;z++){image.word(sites[z],expected[z]);}
 image.sync();
 for(size_t k=0;k<bodies.size();k++)check(memcmp(image.base+bodies[k].first,originals[k].data(),bodies[k].second)==0,"all selected full arithmetic bodies still original bytes");
 check(negative_margin_cases==0,"no negative final margin across selected candidate cases");
 check(negative_stride_cases==0,"no raw stride below cell across selected candidate cases");
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"negative_margin_cases\":"<<negative_margin_cases<<",\"negative_raw_stride_cases\":"<<negative_stride_cases<<",\"smallest_margin\":"<<smallest_margin<<",\"device_status\":\"NOT_TESTED\"}\n";
}
