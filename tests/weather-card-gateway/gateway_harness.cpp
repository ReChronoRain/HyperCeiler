// Actual production template copies plus ROM constructor/tails.
// Descends from frozen v3 fixture; explicit dependency adapters remain fixtures.
#include "targets/weather/weather_card_gateway.h"
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iterator>
#include <vector>
#include <time.h>
#include <atomic>
#include <sys/mman.h>
#include <unistd.h>
#include <algorithm>
extern "C" uint64_t run_card_tail(void*,void*,void*,void*,void*);
extern "C" uint64_t run_card_predicate(void*,void*,void*,uint64_t);
extern "C" void run_card_gateway_probe(void*,void*,void*,void*,void*);
extern "C" uint64_t run_card_constructor(void*,void*,void*,void*,void*);
extern "C" { alignas(8) uint64_t hc_weather_card_lease_candidate=0; }
static unsigned passes=0, failures=0;
static uint64_t now(){timespec ts{};if(clock_gettime(CLOCK_BOOTTIME,&ts))std::abort();return uint64_t(ts.tv_sec)*1000000000+ts.tv_nsec;}

static void check(bool yes,const char*what,int64_t value=0) {if(yes)++passes;else{++failures;if(failures<=32)std::printf("FAIL %s value=%ld\n",what,value);}}
static void put64(uint8_t*p,ptrdiff_t off,uint64_t v){std::memcpy(p+off,&v,8);}
static uint64_t get64(uint8_t*p,ptrdiff_t off){uint64_t v;std::memcpy(&v,p+off,8);return v;}
static void* copied_gateways[4]{};
alignas(8) static uint64_t secondary_lease=0;
static bool prepare_templates(){
 using namespace weather_card_gateway;
 alignas(8) std::array<std::byte,kTemplateSize+16> scratch{};
 scratch.fill(std::byte{0xa5});const auto original=scratch;
 const auto valid=reinterpret_cast<uintptr_t>(&hc_weather_card_lease_candidate);
 for(auto bad:{uintptr_t(0),valid+1}) {
  check(!copy_template(Kind::Recompute,{scratch.data(),kTemplateSize},bad),"reject invalid lease alignment");
  check(scratch==original,"invalid lease leaves output unchanged");
 }
 check(!copy_template(Kind::Recompute,{scratch.data()+1,kTemplateSize},valid),"reject unaligned destination");
 check(scratch==original,"unaligned output unchanged");
 check(!copy_template(Kind::Recompute,{scratch.data(),kTemplateSize-1},valid),"reject short span");
 check(!copy_template(Kind::Recompute,{scratch.data(),kTemplateSize+1},valid),"reject long span");
 check(!copy_template(static_cast<Kind>(99),{scratch.data(),kTemplateSize},valid),"reject unknown kind");
 check(scratch==original,"invalid sizes/kind unchanged");
 for(int page_index=0;page_index<2;++page_index){
  const size_t page=size_t(sysconf(_SC_PAGESIZE));
  auto*mem=static_cast<std::byte*>(mmap(nullptr,page,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
  if(mem==MAP_FAILED)return false;
  const auto lease=reinterpret_cast<uintptr_t>(page_index?&secondary_lease:&hc_weather_card_lease_candidate);
  for(int kind=0;kind<2;++kind){
   const size_t at=page_index?1024+kind*512:128+kind*512;
   check(copy_template(kind?Kind::Constructor:Kind::Recompute,{mem+at,kTemplateSize},lease),"copy actual production template");
   uint64_t literal=0;std::memcpy(&literal,mem+at+kLeaseLiteralOffset,8);
   check(literal==lease,"literal injected to exact aligned slot");
   auto*original_code=kind?hc_weather_card_constructor_template:hc_weather_card_recompute_template;
   check(!std::memcmp(mem+at,original_code,kCodeSize),"copied instructions unchanged");
   uint64_t source_literal=1;std::memcpy(&source_literal,original_code+kLeaseLiteralOffset,8);
   check(source_literal==0,"linked template literal remains zero");
   copied_gateways[page_index*2+kind]=mem+at;
  }
  __builtin___clear_cache(reinterpret_cast<char*>(mem),reinterpret_cast<char*>(mem+page));
  if(mprotect(mem,page,PROT_READ|PROT_EXEC))return false;
 }
 return true;
}
int main(int argc,char**argv){
 if(argc!=2)return 2;
 std::ifstream f(argv[1],std::ios::binary);std::vector<uint8_t> rom{std::istreambuf_iterator<char>(f),{}};
 if(rom.size()<0x5f7e90)return 3;
 constexpr size_t size=0xa00000;
 auto*code=(uint8_t*)mmap(nullptr,size,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
 auto*heap=(uint8_t*)mmap((void*)0x70000000,0x10000,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED_NOREPLACE,-1,0);
 if(code==MAP_FAILED||heap==MAP_FAILED)return 4;
 // All exercised original instructions remain at identical relative addresses.
 std::memcpy(code+0x3a82d4,rom.data()+0x3a82d4,0x3a8418-0x3a82d4);
 std::memcpy(code+0x5bd2bc,rom.data()+0x5bd2bc,0x5bd2dc-0x5bd2bc);
 std::memcpy(code+0x5f7e50,rom.data()+0x5f7e50,0x5f7e8c-0x5f7e50);
 std::memcpy(code+0x3aeb84,rom.data()+0x3aeb84,0x3aecf0-0x3aeb84);
 // Only first original MOV word becomes B in this private fixture mapping.
 if(!prepare_templates())return 7;
 for(int kind=0;kind<2;++kind){
  auto*site=code+(kind?0x3aece0:0x3a8408);
  const intptr_t delta=reinterpret_cast<intptr_t>(copied_gateways[kind])-reinterpret_cast<intptr_t>(site);
  if((delta&3)!=0||delta<-(1LL<<27)||delta>=(1LL<<27))return 8;
  const uint32_t branch=0x14000000U|(uint32_t(delta/4)&0x03ffffffU);
  std::memcpy(site,&branch,4);
  check(!std::memcmp(site+4,rom.data()+(kind?0x3aece0:0x3a8408)+4,12),"original remaining three words unchanged");
 }
 // Exact helper boundary substitutes only: raw color getter and unneeded barriers.
 uint32_t color[]={0xd2800020,0xf2ae0000,0xd65f03c0}; // x0=0x70000001, return
 std::memcpy(code+0x3a84e0,color,sizeof(color));
 for(auto pc:{0x2ead08,0x903314,0x3abc90,0x3a9fa0})std::memcpy(code+pc,color,sizeof(color));
 uint32_t ret=0xd65f03c0;
 for(auto pc:{0x9c39f0,0x9c39b0,0x9c39d0,0x9c3a10,0x3ae31c})std::memcpy(code+pc,&ret,4);
 __builtin___clear_cache((char*)code,(char*)code+size);
 if(mprotect(code,size,PROT_READ|PROT_EXEC))return 5;
 std::vector<uint64_t> pool(0x20000/8),thread(0x900/8),fields(0x4000/8),stack(4096);
 thread[0x78/8]=(uintptr_t)fields.data();fields[0x3130/8]=0x70000001;pool[0x40/8]=0x1021;pool[0x1a608/8]=0x70000001;
 thread[0x48/8]=(uintptr_t)stack.data();
 thread[0x678/8]=thread[0x48/8];
 auto*obj=heap+0x1001;
 for(int branch=0;branch<2;++branch)for(int64_t state=-2;state<=19;++state){
  const int64_t original=branch==0?(state>=4&&state<=9?1:(state>9&&state<=14?2:3)):(state>=4&&state<=14?1:3);
  for(int raw:{0,1,2,3})for(int lifetime:{-1,1})for(int cid:{254,253}){
   std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(cid)<<12);put64(obj,0x73,state);put64(obj,0x8b,777);
   const uint64_t deadline=now()+int64_t(lifetime)*5000000000LL;
   __atomic_store_n(&hc_weather_card_lease_candidate,(deadline<<2)|raw,__ATOMIC_RELEASE);
   auto result=run_card_tail(code+(branch==0?0x3a82d4:0x3a831c),obj,pool.data(),thread.data(),stack.data()+stack.size());
#ifdef EXPECT_CLOCK_FAILURE
   const bool override=false;
#else
   const bool override=raw>=2&&lifetime>0&&cid==254;
#endif
   const int64_t expected=override?raw:original;
   check(result==1&&get64(obj,0x8b)==uint64_t(expected),"candidate final mode",state);
   check(get64(obj,0x73)==uint64_t(state)&&get64(obj,-1)==uint64_t(cid)<<12,"state and header unchanged",state);
   auto dark=run_card_predicate(code+0x5bd2bc,obj,stack.data()+stack.size(),1);
   check(dark==uint64_t(expected==3?33:49),"candidate to actual dark consumer",raw);
  }
 }
 // Full original constructor instruction body, dependency boundaries above are explicit fixtures.
 for(int raw:{0,1,2,3})for(int lifetime:{-1,1})for(int cid:{254,253}){
  std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(cid)<<12);
  const uint64_t deadline=now()+int64_t(lifetime)*5000000000LL;
  __atomic_store_n(&hc_weather_card_lease_candidate,(deadline<<2)|raw,__ATOMIC_RELEASE);
  std::fill(stack.begin(),stack.end(),0); // dead locals cannot inherit a prior synthetic frame
  auto result=run_card_constructor(code+0x3aeb84,obj,pool.data(),thread.data(),stack.data()+stack.size());
#ifdef EXPECT_CLOCK_FAILURE
  const bool override=false;
#else
  const bool override=raw>=2&&lifetime>0&&cid==254;
#endif
  const int expected=override?raw:1;
  check(result==1&&get64(obj,0x8b)==uint64_t(expected),"constructor candidate mode",raw);
  check(get64(obj,0x73)==8&&get64(obj,-1)==uint64_t(cid)<<12,"constructor state and header preserved",raw);
  auto dark=run_card_predicate(code+0x5bd2bc,obj,stack.data()+stack.size(),1);
  check(dark==uint64_t(expected==3?33:49),"constructor to original dark predicate",raw);
 }
 for(void *gate:{copied_gateways[0],copied_gateways[1]})for(int raw:{0,1,2,3})for(int lifetime:{-1,1}){
  std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(254)<<12);put64(obj,0x8b,1);
  __atomic_store_n(&hc_weather_card_lease_candidate,((now()+int64_t(lifetime)*5000000000LL)<<2)|raw,__ATOMIC_RELEASE);
  std::array<uint64_t,18> observed{};
  run_card_gateway_probe(gate,obj,stack.data()+stack.size(),observed.data(),thread.data());
  check(observed[0]==1,"gateway returns Dart null");
  for(unsigned reg=1;reg<=14;++reg)check(observed[reg]==reg*0x1111U,"preserves live register",reg);
  check(observed[15]==(uintptr_t)(stack.data()+stack.size()),"restores Dart stack");
  check(observed[16]==0x2929,"restores original caller FP");
  check(observed[17]==0xa0000000,"preserves NZCV");
 }
 // Both independent pages use their own injected lease, including revocation.
 for(int primary:{0,2,3})for(int secondary:{0,2,3})for(int which=0;which<4;++which){
  __atomic_store_n(&hc_weather_card_lease_candidate,primary?((now()+5000000000ULL)<<2)|primary:0,__ATOMIC_RELEASE);
  __atomic_store_n(&secondary_lease,secondary?((now()+5000000000ULL)<<2)|secondary:0,__ATOMIC_RELEASE);
  std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(254)<<12);put64(obj,0x8b,1);
  std::array<uint64_t,18> observed{};
  run_card_gateway_probe(copied_gateways[which],obj,stack.data()+stack.size(),observed.data(),thread.data());
#ifdef EXPECT_CLOCK_FAILURE
  const int expected=1;
#else
  const int raw=which<2?primary:secondary;const int expected=raw?raw:1;
#endif
  check(get64(obj,0x8b)==uint64_t(expected),"independent copied lease and zero revocation",which);
  check(observed[17]==0xa0000000,"copied independent gateway flags",which);
 }
 // One writable page between two inaccessible pages. The original tail's
 // caller frame (144 bytes) or full constructor frame (24 bytes) is above the
 // chosen gateway-entry x15, so a rejected gateway needs no lower stack bytes.
 const size_t page=size_t(sysconf(_SC_PAGESIZE));
 auto*guard=(uint8_t*)mmap(nullptr,page*3,PROT_NONE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
 if(guard==MAP_FAILED||mprotect(guard+page,page,PROT_READ|PROT_WRITE))return 6;
 auto*low=guard+page;
 unsigned guarded_original_cases=0,guarded_abi_cases=0;
 auto set_limit=[&](int kind,uintptr_t entry_sp){
  uint64_t limit=(uintptr_t)low;
  if(kind==1)limit=0;
  if(kind>=2&&kind<=5)limit=UINT64_MAX-uint64_t(5-kind); // -4,-3,-2,-1
  if(kind==6)limit=entry_sp+8; // subtraction would wrap without ordering check
  thread[0x48/8]=limit;
  thread[0x678/8]=(uintptr_t)low; // deliberately valid saved limit during interrupt
 };
 auto prefix_untouched=[&](size_t gap,bool admitted){
  const size_t prefix=admitted?gap-96:gap;
  return std::all_of(low,low+prefix,[](uint8_t byte){return byte==0xa5;});
 };
 for(int path=0;path<3;++path)for(size_t gap:{0U,8U,88U,96U,104U,128U})
 for(int kind=0;kind<7;++kind)for(int raw:{0,1,2,3})for(int lifetime:{-1,1})for(int cid:{254,253}){
  std::memset(low,0xa5,page);
  auto*top=low+gap+(path==2?24:144);
  set_limit(kind,(uintptr_t)(low+gap));
  const auto thread_before=thread;
  const bool admitted=kind==0&&gap>96;
  std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(cid)<<12);put64(obj,0x73,8);put64(obj,0x8b,777);
  __atomic_store_n(&hc_weather_card_lease_candidate,((now()+int64_t(lifetime)*5000000000LL)<<2)|raw,__ATOMIC_RELEASE);
  auto result=path==2?run_card_constructor(code+0x3aeb84,obj,pool.data(),thread.data(),top):
   run_card_tail(code+(path==0?0x3a82d4:0x3a831c),obj,pool.data(),thread.data(),top);
#ifdef EXPECT_CLOCK_FAILURE
  const bool override=false;
#else
  const bool override=admitted&&raw>=2&&lifetime>0&&cid==254;
#endif
  const int expected=override?raw:1;
  check(result==1&&get64(obj,0x8b)==uint64_t(expected),"guarded original path mode",int64_t(gap));
  check(get64(obj,0x73)==8&&get64(obj,-1)==uint64_t(cid)<<12,"guarded original state and class",kind);
  check(thread==thread_before,"live and saved limits including interrupts unchanged",kind);
  check(prefix_untouched(gap,admitted),"no scratch below admitted 96 bytes or any rejected scratch",int64_t(gap));
  auto dark=run_card_predicate(code+0x5bd2bc,obj,stack.data()+stack.size(),1);
  check(dark==uint64_t(expected==3?33:49),"guarded original dark consumer",raw);
  ++guarded_original_cases;
 }
 // Both frame shapes, admitted/rejected stack, active modes: verify the same
 // non-scratch register/NZCV contract on the integrated gateway itself.
 for(void *gate:{copied_gateways[0],copied_gateways[1]})
 for(size_t gap:{0U,96U,104U})for(int kind=0;kind<7;++kind)for(int raw:{2,3}){
  std::memset(low,0xa5,page);
  auto*top=low+gap+144;
  set_limit(kind,(uintptr_t)(low+gap));
  const auto thread_before=thread;
  const bool admitted=kind==0&&gap>96;
  std::memset(obj-1,0,0x100);put64(obj,-1,uint64_t(254)<<12);put64(obj,0x8b,1);
  __atomic_store_n(&hc_weather_card_lease_candidate,((now()+5000000000ULL)<<2)|raw,__ATOMIC_RELEASE);
  std::array<uint64_t,18> observed{};
  run_card_gateway_probe(gate,obj,top,observed.data(),thread.data());
  check(observed[0]==1,"guarded gateway returns null");
  for(unsigned reg=1;reg<=14;++reg)check(observed[reg]==reg*0x1111U,"guarded gateway preserves register",reg);
  check(observed[15]==(uintptr_t)top,"guarded gateway restores x15");
  check(observed[16]==0x2929,"guarded gateway restores caller FP");
  check(observed[17]==0xa0000000,"guarded gateway preserves NZCV");
#ifdef EXPECT_CLOCK_FAILURE
  const int expected=1;
#else
  const int expected=admitted?raw:1;
#endif
  check(get64(obj,0x8b)==uint64_t(expected),"guarded ABI mode",int64_t(gap));
  check(thread==thread_before,"guarded ABI leaves pending interrupts unchanged",kind);
  check(prefix_untouched(gap,admitted),"guarded ABI scratch footprint",int64_t(gap));
  ++guarded_abi_cases;
 }
 munmap(guard,page*3);
 std::printf("{\"passed\":%u,\"failed\":%u,\"original_cases\":720,\"original_abi_cases\":16,\"guarded_original_cases\":%u,\"guarded_abi_cases\":%u,\"device_tested\":false}\n",passes,failures,guarded_original_cases,guarded_abi_cases);
 return failures?1:0;
}
