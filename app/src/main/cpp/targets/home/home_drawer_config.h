/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout {
constexpr int32_t kDrawerMagic=0x48434452;
template<class Read> bool read_drawer_config(Read read,bool& hidden,bool optional=true,bool* present=nullptr) {
    hidden=false;if(present)*present=false;int32_t magic=0,value=0;
    if(!read(magic))return optional;
    if(magic!=kDrawerMagic||!read(value)||(value!=0&&value!=1))return false;
    hidden=value==1;if(present)*present=true;return true;
}
}
