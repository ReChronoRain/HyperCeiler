#include "targets/home/home_folder_title_center.h"
#include "targets/home/home_icon_feature_resolver.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include <cassert>
#include <iostream>
#include "fixture.h"

int main(){
    using namespace home_layout::features;
    auto original=fixture();auto bytes=image(original);auto code=Code::parse(bytes);assert(code);
    auto title=folder_title_center(*code,text,text_end,editor,style,roots());assert(title);
    assert(title->text&&title->editor&&title->inner.window&&title->axes[0]!=title->axes[1]);
    IconFeature icon;icon.current=current;icon.render.grid=grid;icon.render.value=value;
    auto close=folder_close_feature(*code,icon);assert(close);
    // Neither title alignment nor cache scalars may match an unrelated Rx source.
    auto bad=roots();bad[0].clear();assert(!folder_title_center(*code,text,text_end,editor,style,bad));
    assert(!folder_title_center(*code,text,text,editor,style,roots()));
    assert(!folder_title_center(*code,text,text_end,editor,style+4,roots()));
    auto wrong=original;
    for(auto& f:wrong)if(f.va==current)f.words[4]^=1u<<10;
    bytes=image(wrong);code=Code::parse(bytes);assert(code);assert(!folder_close_feature(*code,icon));
    // Preserve the exact image's missing/ambiguous fail-closed behavior.
    for(auto missing:{close->init,close->cache}){
        auto absent=original;std::erase_if(absent,[&](const auto& f){return f.va==missing;});
        bytes=image(absent);code=Code::parse(bytes);assert(code);assert(!folder_close_feature(*code,icon));
    }
    for(auto owner:{close->init,close->cache}){
        auto clobbered=original;bool changed=false;
        for(auto& f:clobbered)if(f.va==owner)for(auto& w:f.words)
            if(w==0xaa0203e1u){w=0xaa0003e1u;changed=true;}
        assert(changed);bytes=image(clobbered);code=Code::parse(bytes);assert(code);
        assert(!folder_close_feature(*code,icon));
    }
    // All direct call relationships and branch-relative windows survive relocation.
    std::map<uint32_t,uint32_t> mapping;auto moved=relocated(original,mapping);bytes=image(moved);code=Code::parse(bytes);assert(code);
    icon.current=mapping.at(current);auto moved_close=folder_close_feature(*code,icon);assert(moved_close);
    assert(moved_close->init==mapping.at(close->init)&&moved_close->cache==mapping.at(close->cache));
    std::cout<<"Folder title/close PASS: independent title graph, phi config dataflow, relocation, wrong roots and missing cache rejected\n";
}
