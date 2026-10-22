#include "home_folder_app_return.h"
#include "home_icon_render.h"
#include <cassert>
#include <cstdio>
#include "fixture.h"
using namespace home_layout;
int main() {
    unsigned checks=0;auto ok=[&](bool value){++checks;assert(value);};
    IconRenderSites render;ok(icon_render_sites(body2,va2,current,render));
    IconRenderSites moved;
    ok(icon_render_sites(body2,va2+0x200000,current+0x200000,moved));
    ok(moved.width==render.width&&moved.height==render.height&&moved.grid==render.grid);
    auto moved_icon=body2;
    for(auto& w:moved_icon) {
        if((home_dynamic::ldw(w,0,1)&&home_dynamic::imm9(w)==render.grid)
          ||((home_dynamic::ldd(w,1,0)||home_dynamic::ldd(w,1,1))&&home_dynamic::imm9(w)==render.value))
            w+=8u<<12;
    }
    ok(icon_render_sites(moved_icon,va2,current,moved));
    ok(moved.grid==render.grid+8&&moved.value==render.value+8);
    ok(!icon_render_sites(body2,va2,current+4,moved));
    std::array<uint32_t,2> sites{};
    auto resolve=[&](const home_dynamic::Body& b,const home_dynamic::Body& caller,uint32_t delta=0){
        return folder_app_return_sites(b,va0+delta,current+delta,render.grid,render.value,caller,va1+delta,sites);};
    ok(resolve(body0,body1));ok(sites[0]==0x448&&sites[1]==0x4bc);
    ok(resolve(body0,body1,0x200000));
    // Every producer, frame store/load and register in both replay windows is a guard.
    for(unsigned bank=0;bank<2;++bank) {
        size_t i=sites[bank]/4-(bank?5:4);
        for(size_t j=0;j<(bank?9u:7u);++j){auto bad=body0;bad[i+j]^=1;ok(!resolve(bad,body1));}
    }
    auto bad=body0;bad[0]=0;ok(!resolve(bad,body1));
    auto caller=body1;caller[0]=0;ok(!resolve(body0,caller));
    caller=body1;caller[0xb0/4]=0xd503201f;ok(!resolve(body0,caller));
    caller=body1;caller[0xb0/4+1]=0xd503201f;ok(!resolve(body0,caller));
    std::array<uint32_t,2> untouched{123,456};
    ok(!folder_app_return_sites(body0,va0,current,render.grid+4,render.value,body1,va1,untouched));
    ok(untouched[0]==123&&untouched[1]==456);
    ok(!folder_app_return_sites(body0,va0,current,render.grid,render.value+8,body1,va1,untouched));
    std::printf("folder app return resolver: %u checks passed (real 7751 producers/caller, relocation, changed shapes, fail-closed output)\n",checks);
}
