/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_dock_geometry.h"
#include <bit>
#include <cassert>
#include <iostream>
int main() {
    int32_t width,height,tall,margin,bottom,radius,density_bits;int present;size_t count=0;
    while(std::cin>>width>>height>>density_bits>>tall>>margin>>bottom>>radius>>present) {
        const auto actual=home_dock::layout(width,height,std::bit_cast<float>(density_bits),tall,margin,bottom,radius);
        if(bool(actual)!=(present!=0)){std::cerr<<"Availability mismatch at "<<count<<'\n';return 1;}
        if(present) {
            home_dock::Bounds expected{};int32_t bits;
            std::cin>>expected.x>>expected.y>>expected.width>>expected.height>>bits;
            expected.radius=std::bit_cast<float>(bits);
            if(*actual!=expected){std::cerr<<"Geometry mismatch at "<<count<<'\n';return 2;}
        }
        ++count;
    }
    assert(count>30000);std::cout<<"PASS "<<count<<" actual Java/native Dock layouts, density/rounding/boundaries and invalid geometry\n";
}
