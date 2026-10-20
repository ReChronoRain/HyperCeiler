/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"

namespace home_layout {
// The deterministic app-to-folder destination bypasses the widget/mixin size
// getters. Scale its two canonical lengths before the original centering math.
inline bool folder_app_return_sites(const home_dynamic::Body& body,uint32_t va,
    uint32_t current,int32_t grid,int32_t value,const home_dynamic::Body& caller,
    uint32_t caller_va,std::array<uint32_t,2>& out) {
    using namespace home_dynamic;
    if(body.size()<16||body[0]!=0xa9bf79fdu||body[1]!=0xaa0f03fdu
      ||caller.size()<8||caller[0]!=0xa9bf79fdu||caller[1]!=0xaa0f03fdu)return false;
    unsigned callers=0;
    for(size_t i=1;i+3<caller.size();++i){uint32_t target_va=0;
        if(target(caller[i],caller_va+uint32_t(i*4),target_va)&&target_va==va){
            if(!ldx(caller[i-1],29,2)||!slot(imm9(caller[i-1]))
              ||caller[i+1]!=0xaa1d03efu||caller[i+2]!=0xa8c179fdu||caller[i+3]!=0xd65f03c0u)return false;
            ++callers;
        }
    }
    if(callers!=1||!field(grid)||!field(value))return false;
    std::array<uint32_t,2> sites{};unsigned count=0;int32_t scale_frame=0,width_frame=0;
    for(size_t i=0;i+8<body.size();++i){uint32_t to=0;
        if(!target(body[i],va+uint32_t(i*4),to)||to!=current
          ||!ldw(body[i+1],0,1)||imm9(body[i+1])!=grid||body[i+2]!=0x8b1c8021u
          ||!ldd(body[i+3],1,0)||imm9(body[i+3])!=value)continue;
        if(count>=2||!ldd(body[i+4],29,1)||!slot(imm9(body[i+4]))
          ||body[i+5]!=0x1e610802u)return false; // fmul d2,d0,d1: existing edit/fold scale
        if(count==0){
            scale_frame=imm9(body[i+4]);
            if(!std_(body[i+6],29,2)||!slot(imm9(body[i+6])))return false;
            width_frame=imm9(body[i+6]);
            if(width_frame==scale_frame)return false;
        }else{
            if(imm9(body[i+4])!=scale_frame||!ldd(body[i+6],29,0)
              ||imm9(body[i+6])!=width_frame)return false;
            // Extended frame store for height: MOVN x17,#imm; STR d2,[fp,x17].
            if((body[i+7]&0xffe0001fu)!=0x92800011u||body[i+8]!=0xfc316ba2u)return false;
            const int32_t height_frame=~int32_t((body[i+7]>>5)&65535);
            if(!slot(height_frame)||height_frame==width_frame||height_frame==scale_frame)return false;
        }
        // Height's MOVN x17 and indexed store must both remain inside the
        // replay: the backend uses x17 to jump back after the four words.
        sites[count]=uint32_t((i+(count?5:4))*4);++count;
    }
    if(count!=2||sites[1]<=sites[0]+16)return false;
    out=sites;return true;
}
}
