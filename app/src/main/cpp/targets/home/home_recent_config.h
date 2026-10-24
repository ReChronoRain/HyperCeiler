/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstdint>
namespace home_layout {
// HCRC v1, optional tail of HCL4. HCS1's fixed authorization reply stays unchanged.
constexpr int32_t kRecentMagic=0x48435243;
struct RecentConfig {
    enum Index {Corners,HeaderHeight,HeaderOffset,Animation,TaskHeight,HorizontalPortrait,
        HorizontalLandscape,Vertical,TextSize,TextColor,FreeformColor,Count};
    std::array<int32_t,Count> values{20,40,30,100,52,100,100,100,-1,-1,-1};
    bool operator==(const RecentConfig&)const=default;
    bool requested()const{return *this!=RecentConfig{};}
    bool valid()const {
        constexpr int32_t lo[]={0,0,0,10,30,50,50,50,-1};
        constexpr int32_t hi[]={180,150,150,500,70,150,150,150,30};
        for(unsigned i=0;i<9;++i)if(values[i]<lo[i]||values[i]>hi[i])return false;
        return true; // Both colors are complete signed ARGB words; -1 is OEM.
    }
};
template<class Read> bool read_recent_config(Read read,RecentConfig& out,bool optional=true,bool* present=nullptr) {
    out={};if(present)*present=false;int32_t magic=0,count=0;
    if(!read(magic))return optional;
    if(magic!=kRecentMagic||!read(count)||count!=RecentConfig::Count)return false;
    RecentConfig next;
    for(auto& value:next.values)if(!read(value))return false;
    if(!next.valid())return false;out=next;if(present)*present=true;return true;
}
}
