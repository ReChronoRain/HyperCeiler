#include "targets/home/home_folder_close_preview.h"
#include <cassert>
#include <iostream>
#include "preview_fixture.h"

int main() {
    using namespace home_layout;
    auto resolve=[](const auto& b,const auto& a,FolderPreviewPlan& p) {
        return folder_preview_plan(b[0],b[1],b[2],b[3],b[4],a[0],a[5],
            b[6],b[7],b[8],b[9],b[11],p,a[3],a[12],a[13],a[11]);
    };
    FolderPreviewPlan p;
    assert(resolve(bodies,addresses,p));
    assert(p.padding_helper&&p.cache_width&&p.cache_height&&p.cache_width!=p.cache_height);
    auto moved=addresses;for(auto& address:moved)address+=0x4000;
    FolderPreviewPlan relocated;
    assert(resolve(bodies,moved,relocated));
    assert(relocated.cache_width==p.cache_width&&relocated.cache_height==p.cache_height);
    for (unsigned helper : {12u,13u}) {
        unsigned consumers=0;
        for(size_t i=0;i+13<bodies[3].size();++i) {
            if(!preview_call_to(bodies[3][i],addresses[3]+uint32_t(i*4),addresses[helper]))continue;
            ++consumers;
            for(unsigned step : {1u,5u,6u,9u,10u,11u,12u,13u}) {
                auto broken=bodies;broken[3][i+step]=0xd503201f;
                assert(!resolve(broken,addresses,relocated));
            }
        }
        assert(consumers==1);
    }
    // The linked helpers, their boxed return consumers, and every geometry
    // owner are required. No partially resolved animation bank may be installed.
    for (int missing : {0,1,2,3,4,6,7,8,9,11}) {
        auto broken=bodies;broken[missing].clear();
        assert(!resolve(broken,addresses,p));
    }
    for (int link : {5,12,13}) {
        auto wrong=addresses;wrong[link]+=4;
        assert(!resolve(bodies,wrong,p));
    }
    auto swapped=addresses;std::swap(swapped[12],swapped[13]);
    // Their semantic identity is proven by the production binder; the parser
    // preserves the two linked consumers rather than guessing field positions.
    FolderPreviewPlan other;
    assert(resolve(bodies,swapped,other));
    assert(other.cache_width==p.cache_height&&other.cache_height==p.cache_width);
    std::cout<<"Rendered folder animation geometry: seven sites resolved; missing owners/links rejected\n";
}
