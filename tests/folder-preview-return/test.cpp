/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_folder_preview_return.h"
#include "fixture_image.h"
#include <fstream>
#include <iostream>
#include <iterator>
struct Fixture {const char* version;uint32_t current;Functions functions;std::array<uint32_t,9> roots;};
#include "fixtures.h"
using namespace home_dynamic;
static std::optional<FolderPreviewReturnFeature> resolve(const Functions& input,uint32_t current,uint32_t producer) {
    const auto bytes=image(input);auto code=Code::parse(bytes);assert(code);
    FolderFeatures folders;folders.current=current;folders.preview_builder=producer;
    IconFeature icon;icon.current=current;
    return folder_preview_return_feature(*code,folders,icon);
}
static Function& function(Functions& input,uint32_t va) {
    return *std::find_if(input.begin(),input.end(),[&](const auto& f){return f.va==va;});
}
static uint32_t shifted_field(uint32_t w,int32_t by) {
    return (w&~0x001ff000u)|((uint32_t(imm9(w)+by)&511u)<<12);
}
int main(int argc,char** argv) {
    unsigned corruptions=0;
    for(const auto& fixture:fixtures()) {
        const auto producer=fixture.roots[0],origin=fixture.roots[3],landing=fixture.roots[6];
        auto result=resolve(fixture.functions,fixture.current,producer);assert(result);
        assert(result->origin==origin&&result->landing==landing&&result->caller_return==landing+0x78);
        assert(result->width_site==landing+0xb8&&result->height_site==landing+0xc8);
        assert(result->width_field==7&&result->height_field==15&&result->boxed_value_field==7);
        assert(result->x_frame==-40&&result->y_frame==-32&&result->right_frame==-56&&result->bottom_frame==-48);
        std::map<uint32_t,uint32_t> addresses;auto moved=relocated(fixture.functions,addresses);
        auto relocated_result=resolve(moved,fixture.current,addresses.at(producer));assert(relocated_result);
        assert(relocated_result->origin==addresses.at(origin)&&relocated_result->landing==addresses.at(landing));
        // Frame operands and object byte offsets change independently. The
        // resolver must derive both from their current producer/consumer graph.
        auto changed=moved;
        auto& landing_body=function(changed,addresses.at(landing)).words;
        landing_body[2]+=8u<<10;
        for(auto& w:landing_body)
            if(rn(w)==29&&(ldx(w,29,rd(w))||stx(w,29,rd(w))||ldd(w,29,rd(w))||std_(w,29,rd(w))))
                w=shifted_field(w,-8);
        auto& producer_body=function(changed,addresses.at(producer)).words;
        for(auto& w:producer_body)if(std_(w,0,0)&&imm9(w)>=7&&imm9(w)<=31)w=shifted_field(w,8);
        auto& origin_body=function(changed,addresses.at(origin)).words;
        for(size_t i=0;i<origin_body.size();++i)
            if((origin_body[i]&0xffe00c00u)==0xfc400000u&&rn(origin_body[i])==0
                &&i*4>=0x238&&i*4<=0x26c)origin_body[i]=shifted_field(origin_body[i],8);
        auto& consumer=function(changed,addresses.at(fixture.roots[5])).words;
        for(size_t i=0;i+1<consumer.size();++i)
            if(ldd(consumer[i],0,0)&&std_(consumer[i+1],29,0)&&i*4==0x1f8){
                consumer[i]=shifted_field(consumer[i],8);consumer[i+2]=shifted_field(consumer[i+2],8);}
        auto decoded=resolve(changed,fixture.current,addresses.at(producer));assert(decoded);
        assert(decoded->width_field==15&&decoded->height_field==23&&decoded->x_frame==-48
            &&decoded->right_frame==-64&&decoded->bottom_frame==-56);
        // Every graph endpoint is required; this is not a standalone byte pattern.
        for(uint32_t missing:fixture.roots) {
            auto broken=fixture.functions;std::erase_if(broken,[&](const auto& f){return f.va==missing;});
            assert(!resolve(broken,fixture.current,producer));++corruptions;
        }
        // A second equivalent landing function must decline as ambiguous.
        auto ambiguous=fixture.functions;auto duplicate=function(ambiguous,landing);duplicate.va=0x02000000u;
        for(size_t i=0;i<duplicate.words.size();++i){uint32_t to=0;
            if(target(duplicate.words[i],landing+uint32_t(i*4),to))
                duplicate.words[i]=0x94000000u|(uint32_t((int64_t(to)-duplicate.va-i*4)/4)&0x03ffffffu);}
        ambiguous.push_back(duplicate);assert(!resolve(ambiguous,fixture.current,producer));++corruptions;
        // An unrelated four-double allocator is not the actual painted Rect.
        auto wrong_allocator=fixture.functions;auto& wrong=function(wrong_allocator,landing).words;
        const uint32_t allocation_pc=landing+0xd8;
        wrong[0xd8/4]=0x94000000u|(uint32_t((int64_t(fixture.roots[7])-allocation_pc)/4)&0x03ffffffu);
        assert(!resolve(wrong_allocator,fixture.current,producer));++corruptions;
        // No direct branch may enter the middle of either four-word bank or
        // bypass width while entering the height bank of the landing helper.
        for(uint32_t destination:{landing+0xbc,landing+0xcc}){
            auto unsafe=fixture.functions;
            constexpr uint32_t branch_va=0x03000000;
            const auto branch=0x14000000u|(uint32_t((int64_t(destination)-branch_va)/4)&0x03ffffffu);
            unsafe.push_back({branch_va,{branch,0xd65f03c0u}});
            assert(!resolve(unsafe,fixture.current,producer));++corruptions;
        }
        // Actual data-flow mutations: allocator/type, publication, list source,
        // accessor edge, origin relation, static lengths and both landing banks.
        const std::vector<std::pair<uint32_t,std::vector<uint32_t>>> mutations={
            {fixture.roots[7],{0}},
            {producer,{0x3c8,0x3e0,0x3e8,0x3f0,0x3f8,0x458}},
            {fixture.roots[1],{0x4c,0xf0}},
            {fixture.roots[2],{0x24}},
            {origin,{0x258,0x26c}},
            {fixture.roots[4],{0xbc}},
            {fixture.roots[5],{0x58,0x1f8,0x200,0x290,0x298}},
            {landing,{0x68,0x74,0xb8,0xbc,0xc0,0xc4,0xc8,0xcc,0xd0,0xd4,0xf0}}};
        for(const auto& [va,sites]:mutations)for(uint32_t offset:sites) {
            const uint32_t adjusted=va==fixture.roots[5]&&std::string(fixture.version)=="7751"&&offset>=0x288
                ?offset-8:offset;
            auto broken=fixture.functions;auto& body=function(broken,va).words;assert(adjusted/4<body.size());
            body[adjusted/4]=0xd503201fu;
            if(resolve(broken,fixture.current,producer)){
                std::cerr<<fixture.version<<" mutation unexpectedly admitted va="<<std::hex<<va<<" offset="<<adjusted<<std::endl;
                assert(false);
            }
            ++corruptions;
        }
        std::cout<<"FOLDER_PREVIEW_RETURN "<<fixture.version<<" stripped/relocated/changed fields and frame: PASS\n";
    }
    for(int i=1;i<argc;++i) {
        std::ifstream in(argv[i],std::ios::binary);assert(in);
        std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};Image bytes(raw.size());
        std::memcpy(bytes.data(),raw.data(),raw.size());auto code=Code::parse(bytes);assert(code);
        const auto icon=icon_feature(*code);assert(icon);FolderFeatures folders;
        assert(resolve_folders(*code,icon->current,folders));
        const auto result=folder_preview_return_feature(*code,folders,*icon);assert(result);
        std::cout<<"FOLDER_PREVIEW_RETURN full image "<<argv[i]<<": PASS ("<<code->functions().size()<<" executable candidates)\n";
    }
    std::cout<<"FOLDER_PREVIEW_RETURN missing/ambiguous/corrupt graph cases: "<<corruptions<<" PASS\n";
}
