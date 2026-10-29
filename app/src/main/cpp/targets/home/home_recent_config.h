/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstdint>
namespace home_layout {
// HCRC v1, optional tail of HCL4. HCS1's fixed authorization reply stays unchanged.
constexpr int32_t kRecentMagic=0x48435243;
constexpr int32_t kRecentWireCount=11;
constexpr int32_t kRecentHeaderMagic=0x48435248; // HCRH v1, optional after infinite-scroll tail.
struct RecentConfig {
    enum Index {Corners,HeaderHeight,HeaderOffset,Animation,TaskHeight,HorizontalPortrait,
        HorizontalLandscape,Vertical,TextSize,TextColor,FreeformColor,HideIcon,HideText,DismissAnimation,CloseHaptic,Count};
    std::array<int32_t,Count> values{20,40,30,100,52,100,100,100,-1,-1,-1,0,0};
    bool operator==(const RecentConfig&)const=default;
    bool requested()const{return *this!=RecentConfig{};}
    bool valid()const {
        constexpr int32_t lo[]={0,0,0,10,30,50,50,50,-1};
        constexpr int32_t hi[]={180,150,150,500,70,150,150,150,30};
        for(unsigned i=0;i<9;++i)if(values[i]<lo[i]||values[i]>hi[i])return false;
        if(values[HideIcon]<0||values[HideIcon]>1||values[HideText]<0||values[HideText]>1
            ||values[DismissAnimation]<0||values[DismissAnimation]>1)return false;
        return true; // Both colors are complete signed ARGB words; -1 is OEM.
    }
};
template<class Read> bool read_recent_config(Read read,RecentConfig& out,bool optional=true,bool* present=nullptr) {
    out={};if(present)*present=false;int32_t magic=0,count=0;
    if(!read(magic))return optional;
    if(magic!=kRecentMagic||!read(count)||count!=kRecentWireCount)return false;
    RecentConfig next;
    for(int i=0;i<kRecentWireCount;++i)if(!read(next.values[i]))return false;
    if(!next.valid())return false;out=next;if(present)*present=true;return true;
}
// Header switches are published together with the core snapshot, never merged
// with a previous query. An older server may omit only this optional extension.
template<class Read> bool read_recent_header(Read read,RecentConfig& out,bool optional=true,bool* present=nullptr) {
    out.values[RecentConfig::HideIcon]=out.values[RecentConfig::HideText]=0;
    if(present)*present=false;int32_t magic=0,flags=0;
    if(!read(magic))return optional;
    if(magic!=kRecentHeaderMagic||!read(flags)||flags<0||(flags&~3))return false;
    out.values[RecentConfig::HideIcon]=flags&1;
    out.values[RecentConfig::HideText]=(flags>>1)&1;
    if(present)*present=true;return true;
}
}
