/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout {
constexpr int32_t kDockHorizontalMagic=0x4843484d; // HCHM
// The settings baseline is 20 dp, just as the workspace horizontal control.
// A disabled/missing/default control is exactly neutral to the original layout.
inline bool valid_dock_horizontal(int delta) {return delta>=-20&&delta<=80;}
template<class Read> bool read_dock_horizontal(Read read,int& delta,bool optional=true,bool* present=nullptr) {
    delta=0;if(present)*present=false;
    int32_t magic=0,count=0,value=0;
    if(!read(magic))return optional;
    if(magic!=kDockHorizontalMagic||!read(count)||count!=1||!read(value)||!valid_dock_horizontal(value))return false;
    delta=value;if(present)*present=true;return true;
}
}
