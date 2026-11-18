/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>
#include <optional>

namespace home_dock {
struct Bounds {int32_t x,y,width,height;float radius;bool operator==(const Bounds&)const=default;};
inline int32_t round_pixels(float value) {
    // Java Math.round(float) saturates on overflow and rounds ties toward
    // positive infinity; C++ round/casts do not share those semantics.
    if(std::isnan(value))return 0;
    if(value>=2147483648.f)return std::numeric_limits<int32_t>::max();
    if(value<=-2147483648.f)return std::numeric_limits<int32_t>::min();
    return static_cast<int32_t>(std::floor(double(value)+0.5));
}
inline std::optional<Bounds> layout(int32_t width,int32_t height,float density,int32_t height_dp,
    int32_t margin_dp,int32_t bottom_dp,int32_t radius_dp) {
    if(width<=0||height<=0||!std::isfinite(density)||density<=0)return {};
    const auto margin=std::min(round_pixels(std::clamp(margin_dp,0,150)*density),width/2);
    const auto bottom=std::min(round_pixels(std::clamp(bottom_dp,0,150)*density),height);
    const auto dock_width=width-margin*2;
    const auto dock_height=std::min(round_pixels(std::clamp(height_dp,0,300)*density),height-bottom);
    if(dock_width<=0||dock_height<=0)return {};
    const auto radius=std::min(std::clamp(radius_dp,0,60)*density,std::min(dock_width,dock_height)/2.f);
    return Bounds{margin,height-bottom-dock_height,dock_width,dock_height,radius};
}
}
