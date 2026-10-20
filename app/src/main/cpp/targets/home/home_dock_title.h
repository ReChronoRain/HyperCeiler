/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
#include <algorithm>
#include <array>
#include <span>
#include <optional>
namespace home_layout {
constexpr int32_t kDockTitleMagic=0x48434454; // HCDT; optional trailing extension
template<class Read> bool read_dock_title(Read read,bool& out,bool optional=true,bool* present=nullptr) {
    out=false;if(present)*present=false;int32_t magic=0,enabled=0;
    if(!read(magic))return optional;
    if(magic!=kDockTitleMagic||!read(enabled)||enabled<0||enabled>1)return false;
    out=enabled!=0;if(present)*present=true;return true;
}
// The two model implementations have different metadata offsets. Validate the
// actual getter called by this builder, including all five stock hotseat IDs.
inline bool dock_title_hotseat_getter(std::span<const uint32_t> b) {
    if(b.size()!=20||(b[0]&0xffe0001fu)!=0xd2800011u
        ||b[1]!=0xb8716822u||b[2]!=0x8b1c8042u
        ||!home_dynamic::ldx(b[3],2,1)||!home_dynamic::field(home_dynamic::imm9(b[3])))return false;
    constexpr uint32_t tail[]={0xb101943f,0x540000e0,0xb101983f,0x540000a0,
        0xb1019c3f,0x54000060,0xb101a43f,0x54000061,0x910082c0,0x14000006,
        0xb101a83f,0x910082d0,0x9100c2d1,0x9a910202,0xaa0203e0,0xd65f03c0};
    return std::equal(std::begin(tail),std::end(tail),b.begin()+4);
}
struct DockTitleGate {size_t offset;uint32_t replacement;};
struct DockTitlePlan {
    std::vector<DockTitleGate> gates;uint32_t hotseat_getter;
    size_t alpha_offset;std::array<int32_t,6> alpha_fields;
};
// Preserve explicit hide-title/null guards and the empty-widget return.
// Identify the two location exclusions and the title animation wrapper separately.
inline std::optional<DockTitlePlan> resolve_dock_title(std::span<const uint32_t> b,
    uint32_t va,uint32_t dock_scope) {
    using namespace home_dynamic;
    if(b.size()<20||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu)return {};
    std::optional<DockTitlePlan> result;
    auto bit_target=[&](size_t i){int32_t jump=int32_t(((b[i]>>5)&0x3fff)<<18)>>18;return int64_t(i)+jump;};
    for(size_t i=5;i+11<b.size();++i){uint32_t hotseat=0,dock=0;
        if(!ldw(b[i-5],0,1)||!field(imm9(b[i-5]))||b[i-4]!=0x8b1c8021u
            ||!ux(b[i-3],27,16)||b[i-2]!=0x6b10003fu
            ||(b[i-1]&0xff00001fu)!=0x54000000u
            ||(b[i]&0xfff8001fu)!=0x36200001u||b[i+1]!=0xaa0003e1u
            ||!target(b[i+2],va+uint32_t((i+2)*4),hotseat)
            ||(b[i+3]&0xfff8001fu)!=0x36200000u
            ||!ldx(b[i+4],29,1)||!slot(imm9(b[i+4]))
            ||!target(b[i+5],va+uint32_t((i+5)*4),dock)||dock!=dock_scope
            ||(b[i+6]&0xfff8001fu)!=0x37200000u)continue;
        if(bit_target(i)!=int64_t(i+7)||bit_target(i+3)!=int64_t(i+7)
            ||bit_target(i+6)!=int64_t(i+11)
            ||(b[i+7]&0xffc003ffu)!=0xf9400360u // pool-root empty widget
            ||b[i+8]!=0xaa1d03efu||b[i+9]!=0xa8c179fdu||b[i+10]!=0xd65f03c0u)continue;
        if(result)return {};
        std::optional<size_t> alpha;
        // AllocateClosure -> frame local -> allocate builder -> mov -> reload
        // closure -> four-word builder/child initialization -> reload animation.
        for(size_t j=i+11;j+5<b.size();++j){uint32_t allocator=0;
            if(j<5||!stx(b[j],29,1)||!slot(imm9(b[j]))
                ||!mem(b[j+1],0xb8000000u,1,0)||!field(imm9(b[j+1]))
                ||!ldx(b[j+2],29,0)||!slot(imm9(b[j+2]))
                ||!mem(b[j+3],0xb8000000u,1,0)||!field(imm9(b[j+3]))
                ||!ldx(b[j+4],29,0)||!slot(imm9(b[j+4]))
                ||!mem(b[j+5],0xb8000000u,1,0)||!field(imm9(b[j+5]))
                ||b[j-2]!=0xaa0003e1u||!ldx(b[j-1],29,0)
                ||!target(b[j-3],va+uint32_t((j-3)*4),allocator)
                ||!stx(b[j-4],29,0)||imm9(b[j-4])!=imm9(b[j-1]))continue;
            if(alpha)return {};alpha=j*4;
        }
        if(!alpha||i<8||!ldx(b[i-8],29,0)||!slot(imm9(b[i-8])))return {};
        const size_t a=*alpha/4;
        result=DockTitlePlan{{DockTitleGate{(i+3)*4,0xd503201fu},
            DockTitleGate{(i+6)*4,0x14000005u}},hotseat,*alpha,
            {imm9(b[i-8]),imm9(b[a]),imm9(b[a+2]),imm9(b[a+1]),imm9(b[a+3]),imm9(b[a+5])}};
    }
    return result;
}
}
