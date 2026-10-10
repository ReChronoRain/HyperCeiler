#include "targets/home/recents_policy.h"
#include <array>
#include <bit>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <sys/mman.h>
#include <time.h>
extern "C" {
uint64_t recents_observed[104]{},recents_expected_sp=0;
void recents_capture();void recents_probe(void*,uint64_t,uint64_t,uint64_t);void recents_expected_lr();
extern const char hc_home_recents_template[],hc_home_recents_template_end[],hc_home_recents_return_branch[],hc_home_recents_lease_literal[];
}
static unsigned passed=0,failed=0;
void check(bool yes,const char *text){if(yes)++passed;else{++failed;std::fprintf(stderr,"FAIL %s\n",text);}}
int64_t now_ns(){timespec t{};if(clock_gettime(CLOCK_BOOTTIME,&t))std::abort();return int64_t(t.tv_sec)*1000000000+t.tv_nsec;}
int main(){
 const auto len=hc_home_recents_template_end-hc_home_recents_template;
 const auto near_page=(reinterpret_cast<uintptr_t>(recents_capture)&~uintptr_t(4095))+0x04000000;
 auto *gate=static_cast<char*>(mmap(reinterpret_cast<void*>(near_page),4096,7,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED_NOREPLACE,-1,0));if(gate==MAP_FAILED||len>4096)std::abort();
 std::memcpy(gate,hc_home_recents_template,len);alignas(8)uint64_t word=0;
 auto *lease=&word;std::memcpy(gate+(hc_home_recents_lease_literal-hc_home_recents_template),&lease,8);
 const auto return_at=gate+(hc_home_recents_return_branch-hc_home_recents_template);
 const int64_t delta=reinterpret_cast<int64_t>(recents_capture)-reinterpret_cast<int64_t>(return_at);
 if(delta%4||delta< -0x8000000LL||delta>0x7fffffcLL)std::abort();
 const uint32_t branch=0x14000000u|(uint32_t(delta>>2)&0x3ffffff);std::memcpy(return_at,&branch,4);
 __builtin___clear_cache(gate,gate+len);
 for(uint64_t flags:{uint64_t{0},uint64_t{0xa0000000},uint64_t{0xf0000000}})
 for(uint64_t fpcr:{uint64_t{0},uint64_t{0x400000},uint64_t{0x800000},uint64_t{0xc00000}})
 for(uint64_t fpsr:{uint64_t{0},uint64_t{0x9f}})
 for(int rate:{0,1,30,31,100,170,199,200,201}){
  const bool valid=rate>=30&&rate<=200;
  word=rate<=1?uint64_t(rate):((uint64_t(now_ns()/1000000)+4000)<<8)|uint64_t(rate);
  std::memset(recents_observed,0,sizeof(recents_observed));recents_probe(gate,flags,fpcr,fpsr);
  for(unsigned r=0;r<29;++r)check(recents_observed[r]==(r==16?reinterpret_cast<uintptr_t>(gate):0x100+r),"all x0..x28 including x16/x17 preserved");
  check(recents_observed[29]==recents_expected_sp+304,"original frame pointer preserved");
  check(recents_observed[30]==reinterpret_cast<uintptr_t>(recents_expected_lr),"original LR preserved");
  check(recents_observed[31]==flags,"NZCV preserved");check(recents_observed[32]==recents_expected_sp,"SP exact");
  check(recents_observed[33]==fpcr,"FPCR preserved");check(recents_observed[34]==fpsr,"FPSR preserved");
  const uint32_t wanted=valid?std::bit_cast<uint32_t>(float(rate)/100.f):0x20202020;
  const auto *simd=reinterpret_cast<const uint8_t*>(recents_observed)+288;
  bool vector_ok=true;for(unsigned q=0;q<32;++q)for(unsigned b=0;b<16;++b){const auto want=q==0&&b<4?uint8_t(wanted>>(b*8)):uint8_t(32+q);vector_ok&=simd[q*16+b]==want;}
  check(vector_ok,"all128bit SIMD lanes preserved except selected s0 low32");
  check(uint32_t(recents_observed[100])==wanted,"displaced native store replayed exactly");
  check(rate!=0||word==1,"pending CASAL becomes terminal");
 }
 munmap(gate,4096);std::printf("RESULT %u passed, %u failed\n",passed,failed);return failed?1:0;
}
