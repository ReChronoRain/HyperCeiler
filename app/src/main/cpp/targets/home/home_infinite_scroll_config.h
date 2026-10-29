/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout {
constexpr int32_t kInfiniteScrollMagic=0x48434957; // HCIW, optional boolean tail.
template<class Read> bool read_infinite_scroll(Read read,bool& enabled,bool optional=true,bool* present=nullptr) {
    enabled=false;if(present)*present=false;int32_t magic=0,value=0;
    if(!read(magic))return optional;
    if(magic!=kInfiniteScrollMagic||!read(value)||(value!=0&&value!=1))return false;
    enabled=value==1;if(present)*present=true;return true;
}
}
