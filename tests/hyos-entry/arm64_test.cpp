/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <array>
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdio>
struct alignas(16) Probe {
    uint64_t registers[19];
    alignas(16) std::array<unsigned char,16> vectors[8];
    uint64_t nzcv,fpcr,fpsr,sp,lr,sret[3],expected_sp,expected_lr;
};
static_assert(offsetof(Probe,vectors)==160 && offsetof(Probe,nzcv)==288);
static_assert(offsetof(Probe,sret)==328 && offsetof(Probe,expected_lr)==360);
extern "C" { uint64_t hc_test_observed_caller=0; }
extern "C" void hc_test_enter(Probe *);
int main(){
 for(unsigned iteration=0;iteration<32;++iteration){
  Probe p{};hc_test_enter(&p);
  assert(p.registers[0]==reinterpret_cast<uintptr_t>(&p));
  assert(p.registers[8]==reinterpret_cast<uintptr_t>(p.sret));
  for(unsigned i=1;i<19;++i)if(i!=8&&i!=16)assert(p.registers[i]==0x100+i);
  for(unsigned q=0;q<8;++q)for(auto v:p.vectors[q])assert(v==0x30+q);
  assert(p.nzcv==0xa0000000 && p.fpcr==0x04000000 && p.fpsr==0x10);
  assert(p.sp==p.expected_sp && p.lr==p.expected_lr && hc_test_observed_caller==p.expected_lr);
  assert(p.sret[0]==0x111 && p.sret[1]==0x222 && p.sret[2]==0x333);
 }
 std::puts("32/32 ARM64 executions PASS: actual wrapper preserves Runtime x0, sret x8/24 bytes, other arguments, vectors, FP state, NZCV, original SP and LR; helper intentionally clobbers volatile registers.");
}
