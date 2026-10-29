/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
#include "home_folder_geometry.h"
#include <optional>
#include <cmath>
#include <cstring>
namespace home_layout::features {
struct DockPositionFeature {
    uint32_t owner=0,width=0,capacity=0;
    std::array<uint32_t,2> sites{};
    int32_t count=0,index=0;
};
// ARM64 Dart unboxed integer/double register roles are ABI constraints. Owner,
// frame slots, callees and configuration fields are discovered from this image.
inline std::optional<DockPositionFeature> dock_position_feature(const Code& code,uint32_t config) {
    using namespace home_dynamic;
    std::optional<DockPositionFeature> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;int32_t receiver=0,count=0,index=0;
        if(!frame_size(b)||!receiver_slot(b,1,receiver)||!receiver_slot(b,2,count)
            ||!receiver_slot(b,3,index)||count==index||!frame_slot(b,count)||!frame_slot(b,index))continue;
        std::vector<size_t> ends;
        for(size_t i=0;i+3<b.size();++i)
            if((b[i]==0x1e622860u||b[i]==0x1e612840u)&&runtime_contract::epilogue(b,i+1))ends.push_back(i);
        if(ends.size()!=2||b[ends[0]]!=0x1e622860u||b[ends[1]]!=0x1e612840u
            ||!passes_return_points(b,ends))continue;
        auto a=ends[0],z=ends[1];uint32_t width=0,capacity=0;
        if(a<8||z<8||!ldx(b[a-6],29,0)||imm9(b[a-6])!=index
            ||b[a-5]!=0x1e601000u||b[a-4]!=0x1e623823u||b[a-3]!=0x1e601862u
            ||b[a-2]!=0x9e620000u||b[a-1]!=0x1e610803u
            ||!ldx(b[z-6],29,1)||!target(b[z-5],f.va+uint32_t((z-5)*4),width)
            ||!ldx(b[z-4],29,0)||imm9(b[z-4])!=index||b[z-3]!=0x9e620001u
            ||b[z-2]!=0x1e600822u||!ldd(b[z-1],29,1))continue;
        unsigned shared_width=0,shared_config=0,mirror=0;
        for(size_t i=0;i<b.size();++i) {
            uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)) {
                if(to==width)++shared_width;
                if(to==config)++shared_config;
                if(i+3<b.size()&&b[i+1]==0xaa0003e1u&&ldx(b[i+2],29,0)
                    &&imm9(b[i+2])==count&&b[i+3]==0xeb01001fu)capacity=to;
            }
            if(i+4<b.size()&&ldx(b[i],29,0)&&imm9(b[i])==count
                &&ldx(b[i+1],29,1)&&imm9(b[i+1])==index&&b[i+2]==0xd1000402u
                &&b[i+3]==0xcb010043u&&b[i+4]==0xaa0303e1u)++mirror;
        }
        if(shared_width!=3||shared_config!=1||mirror!=1||!capacity)continue;
        Body w,c;if(!code.body(width,w)||!code.body(capacity,c)||!frame_size(w)
            ||c.size()<5||c[0]!=0xa9bf79fdu||c[1]!=0xaa0f03fdu
            ||!runtime_contract::epilogue(c,c.size()-3))continue;
        unsigned config_reads=0,capacity_calls=0;
        for(size_t i=0;i<w.size();++i) {uint32_t to=0;
            if(target(w[i],width+uint32_t(i*4),to)&&to==config)++config_reads;}
        for(size_t i=0;i<b.size();++i) {uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&to==capacity)++capacity_calls;}
        // The original cap decodes a boolean and computes its unboxed count.
        const std::array<uint32_t,5> cap_ops{0xf27c001fu,0x9a9f07e1u,0xd1000421u,0x927ff821u,0x93417c20u};
        bool cap=true;for(auto op:cap_ops)cap&=std::count(c.begin(),c.end(),op)==1;
        if(config_reads!=2||capacity_calls!=2||!cap)continue;
        bool safe=true;
        for(size_t i=0;i<b.size();++i) {int32_t d=0;uint32_t mask=0;
            if(folder_relative_branch(b[i],d,mask))for(auto p:ends) {
                const int64_t to=int64_t(i)+d;
                if(to>int64_t(p)&&to<int64_t(p+4))safe=false;
            }}
        if(!safe||result)return {};
        result=DockPositionFeature{f.va,width,capacity,{uint32_t(a*4),uint32_t(z*4)},count,index};
    }
    return result;
}
// No Dart call or allocation. Only a saved FP operand is changed; the original
// final FADD, stack teardown and return still execute. The saved index already
// includes the launcher's own RTL mapping.
inline bool inset_dock_position(uintptr_t frame,void* saved,unsigned arm,int delta,const DockPositionFeature& f) {
    if(!frame||!saved||arm>1||delta==0||delta < -20||delta > 80||f.count>=0||f.index>=0)return false;
    int64_t count=0,index=0;std::memcpy(&count,reinterpret_cast<void*>(frame+f.count),8);
    std::memcpy(&index,reinterpret_cast<void*>(frame+f.index),8);
    if(count<1||count>32||index<0||index>=count)return false;
    auto* operand=static_cast<unsigned char*>(saved)+(arm==0?3:2)*16;
    double value=0;std::memcpy(&value,operand,8);
    const double shift=double(delta)*(1.0-(2.0*double(index)+1.0)/double(count));
    if(!std::isfinite(value)||std::abs(value)>10000||!std::isfinite(value+shift))return false;
    value+=shift;std::memcpy(operand,&value,8);return true;
}
}
