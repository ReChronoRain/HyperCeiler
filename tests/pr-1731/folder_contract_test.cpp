/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_geometry.h"
#include "targets/home/home_dynamic_layout.h"
#include <cassert>
#include <iostream>
#include <map>
#include <string>
#include "folder-fixture.inc"
using namespace home_layout;
int main() {
    const auto row=[](const char* name)->const Fixture& {return images.at(name);};
    const char* owners[]={"WidgetPositionUtil.getCellPosition","FolderIconGetxController.calOriginPreviewIconLoc"};
    std::array<home_dynamic::Body,2> bodies{row(owners[0]).words,row(owners[1]).words};
    uint32_t va[]={row(owners[0]).va,row(owners[1]).va};
    GridFieldOffsets fields{0x1b,0x23,0x3b,0x37,0x3f,0x2b,0x33,0x17,0x13,0x0f,0x07};
    FolderGeometryCalls calls{row("GridController.currentConfig").va,row("Inst|find").va,
        row("WidgetPositionUtil.transformPointX").va,row("RxObjectMixin.value").va,
        row("SwipeController.isRtl").va};
    const auto& config=row("GridController.currentConfig");
    calls.table_word=config.words[3];calls.singleton_word=config.words[4];
    const auto& offset=row("Offset.translate");
    for(size_t i=0;i+4<offset.words.size();++i)
        if(offset.words[i+2]==0xfc007000u&&offset.words[i+4]==0xfc00f000u)
            assert(home_dynamic::target(offset.words[i],offset.va+i*4,calls.offset));
    const auto read=[](uint32_t at,size_t count,home_dynamic::Body& out) {
        for(const auto& [name,image]:images)if(image.va==at&&image.words.size()>=count) {
            out.assign(image.words.begin(),image.words.begin()+count);return true;
        }
        return false;
    };
    FolderGeometryContract result;
    const auto accepts=[&](const auto& value){return folder_geometry_contract(value,va,fields,calls,result,read);};
    assert(accepts(bodies));const auto baseline=result;
    auto bad=bodies;bad[0].pop_back();assert(!accepts(bad));
    bad=bodies;bad[0][2]^=0x400;assert(!accepts(bad));
    bad=bodies;bad[0][baseline.site[4]/4]^=0x00010000;assert(!accepts(bad));
    bad=bodies;
    for(auto& word:bad[0])if((word&0xfc000000u)==0x94000000u){word^=1;break;}
    assert(!accepts(bad)); // Correct name cannot admit an unrelated actual callee.
    bad=bodies;
    for(auto& word:bad[0])if((word&0xff00001fu)==0x54000001u){word^=0x20;break;}
    assert(!accepts(bad)); // Every original branch target must still agree.
    auto& init=images.at("anonymous_0x1b59c60").words;const auto original=init[0];
    init[0]=0xd503201fu;assert(!accepts(bodies));init[0]=original;assert(accepts(bodies));
    fields.origin+=16;
    for(auto& body:bodies)for(auto& word:body)if(word==0xb843b001u)
        word=(word&~0x001ff000u)|(uint32_t(fields.origin)<<12);
    assert(accepts(bodies)); // The live field is decoded, not restored from an old table.
    for(auto& [name,image]:images)image.va+=0x10000;
    for(auto& at:va)at+=0x10000;
    calls.config+=0x10000;calls.find+=0x10000;calls.transform+=0x10000;
    calls.rx+=0x10000;calls.rtl+=0x10000;calls.offset+=0x10000;
    assert(accepts(bodies));
    for(unsigned i=0;i<5;++i)assert(result.site[i]==baseline.site[i]);
    std::cout<<"folder semantic contract: real 7751 / changed fields / relocated image; "
        "truncation, frame, arithmetic, calls, branches and helper corruption rejected: PASS\n";
}
