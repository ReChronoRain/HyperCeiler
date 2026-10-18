/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
namespace home_folder_title {
struct Sites {
    uint32_t color=0, font=0, thread=0, statics=0, nursery=0, allocation=0, end_register=0;
    int32_t color_frame=0, font_frame=0;
};
// Locate the desktop folder's own text-style consumer, including its anonymous
// closure. No instruction address, pool entry or frame operand is a version table.
inline bool resolve(const home_dynamic::Body &b, uint32_t base,
                    uint32_t reactive_value, uint32_t title_size, Sites &out) {
    using namespace home_dynamic;
    Sites s; unsigned colors=0, fonts=0;
    for(size_t i=6;i+4<b.size();++i) {
        uint32_t callee=0;
        if(!target(b[i-6],base+uint32_t((i-6)*4),callee) || callee!=reactive_value
            || (b[i-5]&0xfff8001fu)!=0x37200000u
            || (b[i-4]&0xff80001fu)!=0x91000001u || rn(b[i-4])!=27
            || !ux(b[i-3],1,1) || (b[i-2]&0xfc000000u)!=0x14000000u
            || (b[i-1]&0xff80001fu)!=0x91000001u || rn(b[i-1])!=27) continue;
        // The second branch's pool load is at i, and the two color arms join i+1.
        if(!ux(b[i],1,1) || !stx(b[i+1],29,1) || !ux(b[i+2],26,0)
            || !ux(b[i+3],0,0) || b[i+4]!=0x6b16001fu)continue;
        int32_t tb=static_cast<int32_t>(b[i-5]<<13)>>18;
        int32_t jump=static_cast<int32_t>(b[i-2]<<6)>>6;
        if(tb!=4 || jump!=3 || !slot(imm9(b[i+1])))continue;
        ++colors;s.color=uint32_t((i+1)*4);s.color_frame=imm9(b[i+1]);
        s.thread=uoff(b[i+2]);s.statics=uoff(b[i+3]);
    }
    if(colors!=1)return false;
    for(size_t i=1;i+12<b.size();++i) {
        uint32_t callee=0;
        if(!target(b[i-1],base+uint32_t((i-1)*4),callee) || callee!=title_size
            || !std_(b[i],29,0) || !slot(imm9(b[i])))continue;
        // Preserve the original style allocator call and GC return PC. Splice only
        // the later double load and the original nursery fast-path input window.
        for(size_t j=i+1;j+7<b.size() && j<i+24;++j) {
            if(!ldd(b[j],29,0) || imm9(b[j])!=imm9(b[i])
                || (b[j+1]&0xffc003ffu)!=0xa9400341u
                || (b[j+2]&0xffc003ffu)!=0x91000021u || (b[j+3]&0xfffffc1fu)!=0xeb01001fu
                || (b[j+4]&0xff00001fu)!=0x54000009u)continue;
            // Original font style consumes this color slot; reject other closures.
            unsigned end_register=(b[j+1]>>10)&31;
            if((end_register!=2 && end_register!=4) || rn(b[j+3])!=end_register)continue;
            bool color=false, boxed=false;
            for(size_t k=i+1;k<j;++k)if((b[k]&0xffe00fe0u)==(0xf8400000u|(29<<5)) && imm9(b[k])==s.color_frame
                && k+1<j && (b[k+1]&0xffe0001fu)==(0xb8000000u|rd(b[k])))color=true;
            for(size_t k=j+5;k<b.size() && k<j+12;++k)if(std_(b[k],1,0) && imm9(b[k])==7)boxed=true;
            uint32_t allocation=(b[j+2]>>10)&4095;
            if(!color || !boxed || !allocation || (allocation&15))continue;
            ++fonts;s.font=uint32_t(j*4);s.font_frame=imm9(b[i]);
            s.nursery=((b[j+1]>>15)&127)*8;s.allocation=allocation;s.end_register=end_register;
        }
    }
    if(fonts!=1 || !s.thread || !s.statics || !s.nursery)return false;
    out=s;return true;
}
}
