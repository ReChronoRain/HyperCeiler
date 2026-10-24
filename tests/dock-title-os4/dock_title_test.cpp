/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_dock_title.h"
#include "fixture.h"
#include <cassert>
#include <cstdio>
#include <vector>
using namespace home_layout;

// Execute only the real location gate instructions. BLs are stubbed with Dart
// booleans; reaching the stock empty epilogue vs text path is the observable result.
bool renders(std::span<const uint32_t> code,const DockTitlePlan& plan,bool hide,bool hotseat,bool dock) {
    const size_t start=plan.gates[0].offset/4-3,empty=start+7,text=start+11;
    size_t pc=start;uint32_t w0=0,w1=hide?0x20:0x30;
    for(unsigned steps=0;steps<20;++steps){
        if(pc==empty)return false;if(pc==text)return true;
        assert(pc<code.size());const uint32_t w=code[pc];
        if((w&0x7e000000u)==0x36000000u){
            const unsigned bit=((w>>19)&31)|((w>>26)&32);
            const uint32_t value=(w&31)==0?w0:w1;
            const bool taken=bool((value>>bit)&1)==bool((w>>24)&1);
            const int jump=int32_t(w<<13)>>18;pc=taken?size_t(int64_t(pc)+jump):pc+1;
        }else if((w&0xfc000000u)==0x94000000u){
            const bool answer=pc==start+2?hotseat:dock;w0=answer?0x20:0x30;++pc;
        }else if((w&0xfc000000u)==0x14000000u){pc=size_t(int64_t(pc)+(int32_t(w<<6)>>6));}
        else {assert(w==0xaa0003e1u||w==0xd503201fu||home_dynamic::ldx(w,29,1));++pc;}
    }assert(false);return false;
}

int main(){
    const std::span<const uint32_t> builders[]={body0,body1},getters[]={body3,body4};
    const uint32_t vas[]={va0,va1},getter_vas[]={va3,va4};
    unsigned checks=0;auto ok=[&](bool value){++checks;assert(value);};
    for(unsigned b=0;b<2;++b){
        const auto original=builders[b];auto plan=resolve_dock_title(original,vas[b],va2);
        ok(plan.has_value());ok(plan->gates.size()==2);ok(plan->alpha_offset==(b?0x3d4u:0x558u));
        ok(plan->hotseat_getter==getter_vas[b]);ok(dock_title_hotseat_getter(getters[b]));
        ok(plan->gates[0].offset==(b?0x58u:0x64u));ok(plan->gates[1].offset==(b?0x64u:0x70u));
        auto patched=std::vector<uint32_t>(original.begin(),original.end());
        for(auto gate:plan->gates)patched[gate.offset/4]=gate.replacement;
        for(bool hide:{false,true})for(bool hotseat:{false,true})for(bool dock:{false,true}){
            ok(renders(original,*plan,hide,hotseat,dock)==(!hide&&!hotseat&&!dock));
            ok(renders(patched,*plan,hide,hotseat,dock)==!hide);
        }
        for(auto gate:plan->gates)patched[gate.offset/4]=original[gate.offset/4];
        ok(std::equal(patched.begin(),patched.end(),original.begin()));
        for(int n=-4;n<=5;++n){auto bad=patched;bad[plan->alpha_offset/4+n]=0xd503201fu;
            ok(!resolve_dock_title(bad,vas[b],va2));}
        // Every opcode/register/branch in the identified gate is significant.
        const size_t start=plan->gates[0].offset/4-3;
        for(size_t n=0;n<11;++n){auto bad=patched;bad[start+n]=0xd503201fu;ok(!resolve_dock_title(bad,vas[b],va2));}
        ok(!resolve_dock_title(original,vas[b],va2+4));
        auto relocated=patched;const uint32_t shift=0x100000;
        auto moved=resolve_dock_title(relocated,vas[b]+shift,va2+shift);
        ok(moved&&moved->hotseat_getter==getter_vas[b]+shift);
        auto getter=std::vector<uint32_t>(getters[b].begin(),getters[b].end());
        for(size_t n=0;n<getter.size();++n){auto bad=getter;bad[n]=0xd503201fu;ok(!dock_title_hotseat_getter(bad));}
        getter[0]=(getter[0]&0xffe0001fu)|(0x1b3u<<5);getter[3]=0xf8400000u|(0x63u<<12)|(2u<<5)|1;
        ok(dock_title_hotseat_getter(getter)); // derive model offsets, never pin a ROM's fields
    }
    for(auto input:{std::vector<int32_t>{},std::vector<int32_t>{kDockTitleMagic,0},
        std::vector<int32_t>{kDockTitleMagic,1},std::vector<int32_t>{kDockTitleMagic},
        std::vector<int32_t>{kDockTitleMagic,2},std::vector<int32_t>{kDockTitleMagic,-1},
        std::vector<int32_t>{0,1}}){
        bool enabled=true,present=true;size_t at=0;
        const auto read=[&](int32_t& word){if(at==input.size())return false;word=input[at++];return true;};
        const bool valid=input.empty()||(input.size()==2&&input[0]==kDockTitleMagic&&input[1]>=0&&input[1]<=1);
        ok(read_dock_title(read,enabled,true,&present)==valid);
        if(valid){ok(enabled==(!input.empty()&&input[1]==1));ok(present==!input.empty());}
    }
    std::printf("dock title: %u checks passed (7751 ordinary/progress, branches, relocation, guards, protocol)\n",checks);
}
