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
// Legacy prefix: small folder, large folder, widget. The optional tail carries
// horizontal/vertical 3x1 folders. Ordinary icons keep their existing protocol.
using ObjectScales=std::array<ObjectScale,5>;
constexpr int32_t kObjectScaleMagic=0x48435331;
template<class Read> bool read_object_scales(Read read,ObjectScales& out,bool optional=true,bool* present=nullptr) {
    out={};if(present)*present=false;int32_t magic=0;
    if(!read(magic))return optional;
    if(magic!=kObjectScaleMagic)return false;
    ObjectScales next{};
    for(size_t i=0;i<3;++i){auto& scale=next[i];
        int32_t enabled=0,percent=0;
        if(!read(enabled)||!read(percent)||enabled<0||enabled>1||percent<60||percent>140)return false;
        scale={enabled!=0,percent};
    }
    out=next;if(present)*present=true;return true;
}
constexpr int32_t kMediumScaleMagic=0x48434d53;
template<class Read> bool read_medium_scales(Read read,ObjectScales& out,bool optional=true) {
    int32_t magic=0;
    if(!read(magic))return optional;
    if(magic!=kMediumScaleMagic)return false;
    auto next=out;
    for(size_t i=3;i<5;++i) {
        int32_t enabled=0,percent=0;
        if(!read(enabled)||!read(percent)||enabled<0||enabled>1||percent<60||percent>140)return false;
        next[i]={enabled!=0,percent};
    }
    out=next;return true;
}
}
