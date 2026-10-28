/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdalign.h>
struct Output {uint64_t gp[16];unsigned char vectors[32][16];uint64_t flags,sp_after,sp_before;};
uint64_t observed_arguments[3];
extern void scene_original(void);
void* hc_recent_dart_state_original=scene_original;
extern void scene_abi_call(uint64_t frame,uint64_t thread,uint64_t pool,uint64_t dart_sp,struct Output* out);
int main(void){
 alignas(16) unsigned char dart_stack[8192]={0},thread[256]={0},pool[256]={0};
 unsigned checks=0;
 for(unsigned shift=0;shift<=8;shift+=8){
  struct Output out={0};uint64_t frame=(uintptr_t)(dart_stack+4096+shift),sp=frame-512;
  scene_abi_call(frame,(uintptr_t)thread,(uintptr_t)pool,sp,&out);
  for(unsigned i=0;i<15;++i){++checks;if(out.gp[i]!=0x100u+i){printf("GP fail %u\n",i);return 1;}}
  ++checks;if(out.gp[15]!=sp)return 2;
  for(unsigned i=0;i<32;++i)for(unsigned j=0;j<16;++j){++checks;if(out.vectors[i][j]!=0x40u+i){printf("SIMD fail %u\n",i);return 3;}}
  ++checks;if(out.flags!=0x60000000u)return 4;
  ++checks;if(out.sp_before!=out.sp_after)return 5;
  ++checks;if(observed_arguments[0]!=frame||observed_arguments[1]!=(uintptr_t)thread||observed_arguments[2]!=(uintptr_t)pool)return 6;
 }
 printf("Flutter scene ARM64 ABI %u checks passed, production save/restore, NZCV, 32 SIMD registers, native and Dart stacks, both Dart frame alignments\n",checks);
 return 0;
}
