/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_icon_feature_resolver.h"
#include "targets/home/home_animation_feature_resolver.h"
#include "targets/home/home_widget_feature_resolver.h"
#include "targets/home/home_title_feature_resolver.h"
#include "fixture_image.h"
#include "features_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>

static auto identify(const Code& code) {
    std::map<std::string,uint32_t> result;
    const auto icon=icon_feature(code);
    if(icon) {
        result["app"]=icon->app;result["current"]=icon->current;
        const auto desktop=desktop_icon_feature(code,*icon);
        if(desktop) {
            result["desktop"]=desktop->draw;
            result["desktop_width"]=desktop->width;result["desktop_height"]=desktop->height;
            const auto progress=progress_icon_feature(code,*icon,*desktop);
            if(progress) {
                result["progress_width"]=progress->dimensions.width;
                result["progress_height"]=progress->dimensions.height;
            }
        }
        const auto close=folder_close_feature(code,*icon);
        if(close){result["close"]=close->init;result["cache"]=close->cache;}
        const auto landing=folder_return_feature(code,*icon);
        if(landing){result["stable"]=landing->stable;result["caller"]=landing->caller;}
        const auto font=drawer_font_feature(code,*icon);
        if(font){result["font_site"]=font->owner+font->font;result["font_height_site"]=font->owner+font->height;}
    }
    const auto ratio=animation_ratio_feature(code);
    if(ratio){result["ratio"]=ratio->source;result["flight"]=ratio->flight;result["api"]=ratio->api;}
    const auto magic=animation_magic_feature(code);
    if(magic){result["magic"]=magic->magic;result["high"]=magic->high;result["low"]=magic->low;}
    const auto widget=widget_transform_feature(code);
    if(widget){result["widget"]=widget->owner;result["widget_bank"]=widget->bank;result["widget_allocator"]=widget->allocator;}
    const auto title=dock_title_feature(code);
    if(title){result["dock_scope"]=title->scope;result["dock_title0"]=title->builders[0].va;result["dock_title1"]=title->builders[1].va;}
    return result;
}
static auto identify(const Functions& functions) {
    const auto bytes=image(functions);const auto code=Code::parse(bytes);assert(code);
    return identify(*code);
}
static auto duplicate(const Functions& original,uint32_t va) {
    auto functions=original;
    const auto it=std::find_if(functions.begin(),functions.end(),[&](const auto& f){return f.va==va;});
    assert(it!=functions.end());auto clone=*it;clone.va=0x04000000;
    for(size_t i=0;i<clone.words.size();++i) {
        uint32_t to=0;if(!home_dynamic::target(clone.words[i],va+uint32_t(i*4),to))continue;
        const int64_t delta=int64_t(to)-int64_t(clone.va+i*4);
        assert(!(delta&3)&&delta>=-(1ll<<27)&&delta<(1ll<<27));
        clone.words[i]=0x94000000u|(uint32_t(delta/4)&0x03ffffffu);
    }
    functions.push_back(std::move(clone));return functions;
}
int main(int argc,char** argv) {
    const auto fixture=features_fixture();
    const auto found=identify(fixture);
    if(found!=expected_features){for(const auto& [k,v]:found)std::cerr<<k<<"="<<std::hex<<v<<"\n";}
    assert(found==expected_features);
    std::map<uint32_t,uint32_t> moved_addresses;
    auto moved=relocated(fixture,moved_addresses);
    auto expected_moved=expected_features;
    for(auto& [key,va]:expected_moved) {
        const auto owner=std::find_if(fixture.begin(),fixture.end(),[&](const auto& f){return va>=f.va&&uint64_t(va)<uint64_t(f.va)+f.words.size()*4;});
        assert(owner!=fixture.end());va=moved_addresses.at(owner->va)+va-owner->va;
    }
    if(expected_moved.at("dock_title0")>expected_moved.at("dock_title1"))
        std::swap(expected_moved.at("dock_title0"),expected_moved.at("dock_title1"));
    assert(identify(moved)==expected_moved);

    // Ambiguity in one complete relationship must not select its first body or
    // disable unrelated families. All same-shape BLs are re-encoded correctly.
    for(const auto* family:{"app","desktop","close","caller","flight","magic","widget","dock_title0"}) {
        const auto broken=identify(duplicate(fixture,expected_features.at(family)));
        assert(!broken.count(family));
        if(std::string(family)!="magic")assert(broken.count("magic"));
    }
    for(const auto* family:{"app","desktop","close","stable","flight","api","high","low","widget","widget_allocator","dock_scope","dock_title0"}) {
        auto missing=fixture;const auto va=expected_features.at(family);
        std::erase_if(missing,[&](const auto& f){return f.va==va;});
        const auto broken=identify(missing);
        if(std::string(family)=="api")assert(!broken.count("ratio"));
        else if(std::string(family)=="high"||std::string(family)=="low")assert(!broken.count("magic"));
        else assert(!broken.count(family));
    }
    // Relocated fields and frame roles remain data, never signature constants.
    for(auto& f:moved)for(auto& w:f.words) {
        const bool app=f.va==moved_addresses.at(expected_features.at("app"));
        const bool close=f.va==moved_addresses.at(expected_features.at("close"));
        const bool cache=f.va==moved_addresses.at(expected_features.at("cache"));
        const bool stable=f.va==moved_addresses.at(expected_features.at("stable"));
        const bool mixin=f.va==moved_addresses.at(expected_features.at("desktop_width"))
            ||f.va==moved_addresses.at(expected_features.at("desktop_height"))
            ||f.va==moved_addresses.at(expected_features.at("progress_width"))
            ||f.va==moved_addresses.at(expected_features.at("progress_height"));
        if(app||close||cache||stable||mixin) {
            if(home_dynamic::ldw(w,0,1)&&home_dynamic::imm9(w)==107)w+=4u<<12;
            if((home_dynamic::ldd(w,1,0)||home_dynamic::ldd(w,1,1))&&home_dynamic::imm9(w)==23)w+=8u<<12;
        }
    }
    assert(identify(moved)==expected_moved);
    // Widget scalar fields move consistently in the two original transforms.
    auto moved_widget=fixture;
    for(auto& f:moved_widget)if(f.va==expected_features.at("widget"))
        for(auto& w:f.words)if((w&0x3b200c00u)==0x38000000u&&!(w&(1u<<22))&&home_dynamic::rn(w)==0&&home_dynamic::imm9(w)>0)w+=8u<<12;
    assert(identify(moved_widget)==expected_features);
    auto moved_models=fixture;unsigned models=0;
    for(auto& f:moved_models)if(dock_title_hotseat_getter(f.words)) {
        f.words[0]+=4u<<5;f.words[3]+=8u<<12;++models;
    }
    assert(models==2&&identify(moved_models)==expected_features);
    // 8583 places an uncalled frameless leaf immediately after one hotseat
    // getter. Candidate boundaries are not symbol-sized function boundaries.
    auto adjacent_leaf=fixture;
    for(auto& f:adjacent_leaf)if(dock_title_hotseat_getter(f.words))
        f.words.insert(f.words.end(),{0xb844f020u,0x8b1c8000u,0xd65f03c0u});
    assert(identify(adjacent_leaf)==expected_features);
    for(auto& f:adjacent_leaf)if(f.words.size()==23&&dock_title_hotseat_getter(std::span(f.words).first(20)))
        f.words[19]=0xd503201fu; // The actual getter must still end in RET.
    assert(!identify(adjacent_leaf).count("dock_scope"));
    // Neither a branch into the replay nor a returned-widget bypass is safe.
    for(bool into:{false,true}) {
        auto unsafe=fixture;
        for(auto& f:unsafe)if(f.va==expected_features.at("widget")) {
            const size_t target=into?(expected_features.at("widget_bank")-f.va)/4+1:f.words.size()-19;
            // Pick the known final Dart epilogue for the bypass, independently
            // of the slow allocation tails which follow it in this body.
            size_t destination=target;
            if(!into)for(size_t i=0;i+2<f.words.size();++i)
                if(f.words[i]==0xaa1d03efu&&f.words[i+1]==0xa8c179fdu&&f.words[i+2]==0xd65f03c0u){destination=i;break;}
            f.words[4]=0x14000000u|(uint32_t(int64_t(destination)-4)&0x03ffffffu);
        }
        assert(!identify(unsafe).count("widget"));
    }
    // A nominal getter that returns a Dart object instead of an unboxed double
    // cannot be admitted merely because its caller stores D0 in a frame local.
    auto object_return=fixture;uint32_t font_getter=0;
    const auto app=std::find_if(fixture.begin(),fixture.end(),[&](const auto& f){return f.va==expected_features.at("app");});
    const auto call=expected_features.at("font_site")-app->va-4;
    assert(home_dynamic::target(app->words[call/4],app->va+call,font_getter));
    for(auto& f:object_return)if(f.va==font_getter)
        for(size_t i=3;i<f.words.size();++i)if(f.words[i]==0xd65f03c0u)f.words[i-3]=0xaa1603e0u;
    assert(!identify(object_return).count("font_site"));

    const auto bytes=image(fixture);
    for(size_t length:{size_t{0},size_t{63},size_t{100}})assert(!Code::parse(std::span(bytes).first(length)));
    auto corrupt=bytes;
    corrupt[68]=std::byte{7}; // Writable executable image cannot supply immutable evidence.
    assert(!Code::parse(corrupt));
    corrupt=bytes;corrupt[96]=std::byte{0xff};assert(!Code::parse(corrupt));
    std::cout<<"FEATURE_ADDRESSING PASS: executable-only ELF, independent relocation, field movement, missing links, ambiguity and malformed images\n";
    if(argc>1) {
        std::ifstream input(argv[1],std::ios::binary);assert(input);
        std::vector<char> raw{std::istreambuf_iterator<char>(input),{}};
        auto complete=Code::parse({reinterpret_cast<const std::byte*>(raw.data()),raw.size()});assert(complete);
        assert(identify(*complete)==expected_features);
        // Repackage ONLY the executable instructions. There is no original
        // section/symbol/string table or .gnu_debugdata left to consult.
        const auto stripped=image(complete->functions());const auto anonymous=Code::parse(stripped);assert(anonymous);
        assert(identify(*anonymous)==expected_features);
        auto rebased=complete->functions();constexpr uint32_t delta=0x02000000;
        for(auto& f:rebased)f.va+=delta; // All relative BL/B displacements stay valid.
        const auto translated=identify(rebased);auto wanted=expected_features;
        for(auto& [key,va]:wanted)va+=delta;
        assert(translated==wanted);
        std::cout<<"FULL_IMAGE PASS: "<<complete->functions().size()<<" candidates; all symbols/sections/non-code removed; text rebased independently of file offsets\n";
    }
}
