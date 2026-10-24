/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstdint>
namespace home_layout {
struct ObjectScale {
    bool enabled=false;
    int percent=100;
    double factor() const { return enabled ? percent / 100.0 : 1.0; }
    bool operator==(const ObjectScale&) const = default;
};
// Fixed order: small folder, large folder, widget. Ordinary icons keep their existing protocol.
using ObjectScales=std::array<ObjectScale,3>;
constexpr int32_t kObjectScaleMagic=0x48435331;
template<class Read> bool read_object_scales(Read read,ObjectScales& out,bool optional=true,bool* present=nullptr) {
    out={};if(present)*present=false;int32_t magic=0;
    if(!read(magic))return optional;
    if(magic!=kObjectScaleMagic)return false;
    ObjectScales next{};
    for(auto& scale:next){
        int32_t enabled=0,percent=0;
        if(!read(enabled)||!read(percent)||enabled<0||enabled>1||percent<60||percent>140)return false;
        scale={enabled!=0,percent};
    }
    out=next;if(present)*present=true;return true;
}
}
