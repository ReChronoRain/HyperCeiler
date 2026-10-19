/* SPDX-License-Identifier: AGPL-3.0-or-later */
// OFFLINE GENERATOR ONLY. Executes the actual production symbol index/planner
// over the supplied ROM. No executable mapping or device process is modified.
#include "nativehook/elf_image.h"
#include "targets/home/tweaks/ht_plan.h"
#include "targets/home/tweaks/symtab.h"
#include "targets/home/home_hotseat_capacity.h"
#include "targets/home/home_folder_geometry.h"
#include <algorithm>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <iterator>
#include <set>
#include <vector>

static const hometweaks::Image *image;
static std::set<std::pair<uint32_t,uint32_t>> ranges;
namespace hometweaks {
bool HomeTweaksFindSymbol(const char *name,uint32_t *va,uint32_t *size) {
    const bool found=SymbolIndex::Instance().Find(name,va,size);
    if(found) ranges.insert({*va,*size});
    return found;
}
}
bool dart_words(uint32_t va,size_t count,std::vector<uint32_t>& out) {
    ranges.insert({va,uint32_t(count*4)});
    out.resize(count);
    hometweaks::CodeView code(*image);
    for(size_t i=0;i<count;++i) if(!code.Word(va+4*i,&out[i])) return false;
    return true;
}
#include "production_compatibility_guards.inc"

uint32_t find(const char* name) {
    uint32_t va=0,size=0;
    assert(hometweaks::HomeTweaksFindSymbol(name,&va,&size));
    return va;
}
void hook(unsigned index,uint32_t va,uint64_t feature) {
    ranges.insert({va,16});
    std::printf("{\"type\":\"hook\",\"index\":%u,\"va\":%u,\"feature\":%llu}\n",
        index,va,(unsigned long long)feature);
}
void variant(uint32_t family,uint32_t key,const hometweaks::Config& cfg,
             const hometweaks::LocatedSites& sites) {
    hometweaks::PlanResult plan;
    hometweaks::DerivePatches(hometweaks::CodeView(*image),cfg,sites,&plan);
    bool ok=false;
    switch(family) {
        case 0:ok=plan.ok19&&plan.ok19r;break;case 1:ok=plan.ok16;break;
        case 2:ok=plan.ok4;break;case 3:ok=plan.ok20;break;case 4:ok=plan.ok21;break;
        case 5:ok=plan.ok9;break;case 6:ok=plan.ok18;break;
    }
    assert(ok);
    std::printf("{\"type\":\"variant\",\"family\":%u,\"key\":%u,\"words\":[",family,key);
    bool comma=false;
    for(const auto& p:plan.patches) {
        uint32_t actual=0;assert(hometweaks::CodeView(*image).Word(p.va,&actual)&&actual==p.expect);
        if(comma)std::putchar(',');comma=true;
        std::printf("[%u,%u,%u]",p.va,p.expect,p.patch);
        ranges.insert({p.va>=32?p.va-32:p.va,80});
    }
    std::puts("]}");
}
int main(int argc,char** argv) {
    assert(argc==2);
    std::ifstream file(argv[1],std::ios::binary);assert(file);
    std::string raw(std::istreambuf_iterator<char>{file},{});
    std::span<const std::byte> bytes(reinterpret_cast<const std::byte*>(raw.data()),raw.size());
    auto segments=nhk::elf::parse_program_segments(bytes);assert(segments);
    uint64_t extent=0;
    for(const auto&s:*segments)if(s.type==1)extent=std::max(extent,s.vaddr+s.memsz);
    assert(extent&&extent<(1u<<28));
    std::vector<std::byte> mapped(extent);hometweaks::Image img{};
    img.base=reinterpret_cast<uintptr_t>(mapped.data());assert(std::strlen(argv[1])<512);
    std::strcpy(img.path,argv[1]);
    for(const auto&s:*segments)if(s.type==1) {
        assert(nhk::in_image(bytes.size(),s.offset,s.filesz));
        std::memcpy(mapped.data()+s.vaddr,bytes.data()+s.offset,s.filesz);
        img.segments[img.segmentCount++]={img.base+s.vaddr,img.base+s.vaddr+s.memsz,s.flags};
    }
    image=&img;auto& symbols=hometweaks::SymbolIndex::Instance();assert(symbols.EnsureLoaded(img));
    hometweaks::SetForceSymbolOnlyForTest(true);
    // Include complete declared bodies for every production planner target;
    // exact additional windows below cover short Dart entry aliases.
    for(size_t i=0;i<hometweaks::TargetFunctionCount();++i) {
        const auto&t=hometweaks::TargetFunctionAt(i);uint32_t va=0,size=0;
        if(symbols.Find(t.needle,&va,&size))ranges.insert({va,size});
    }
    hometweaks::Config all;all.flags=1;all.enabled={4,9,16,18,19,20,21};
    all.folderCols=5;all.phoneCols=6;all.phoneRows=7;
    hometweaks::LocatedSites sites;
    for(unsigned feature:{4u,9u,16u,18u,19u,20u,21u}) {
        auto only=all;only.enabled={feature};
        hometweaks::LocateSites(hometweaks::CodeView(img),only,&sites);
        hometweaks::LocateSites(hometweaks::CodeView(img),only,&sites);
    }
    if(!sites.AllOk()||!sites.ok19r||!sites.ok16sq) {
        std::fprintf(stderr,"sites: 4=%d 9=%d 16=%d 18=%d 19=%d 19r=%d 20=%d 21=%d sq=%d\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
            sites.ok4,sites.ok9,sites.ok16,sites.ok18,sites.ok19,sites.ok19r,sites.ok20,sites.ok21,sites.ok16sq,
            sites.why4,sites.why9,sites.why16,sites.why18,sites.why19,sites.why19r,sites.why20,sites.why21,sites.why16sq);
        return 3;
    }
    assert(hometweaks::ValidateSitesShape(hometweaks::CodeView(img),sites));
    for(unsigned family=0;family<7;++family) {
        hometweaks::Config cfg=all;
        cfg.enabled={family==0?19u:family==1?16u:family==2?4u:family==3?20u:family==4?21u:family==5?9u:18u};
        if(family==0)for(unsigned x=3;x<=9;++x)for(unsigned y=4;y<=13;++y) {
            cfg.phoneCols=x;cfg.phoneRows=y;variant(family,(x<<8)|y,cfg,sites);
        }
        else if(family==1)for(unsigned x=1;x<=16;++x) {
            cfg.folderCols=x;variant(family,x,cfg,sites);
        }
        else if(family==2||family==3)for(unsigned x=2;x<=16;++x)for(unsigned y=2;y<=16;++y) {
            cfg.padMajor=cfg.foldMajor=x;cfg.padMinor=cfg.foldMinor=y;
            variant(family,(x<<8)|y,cfg,sites);
        }
        else if(family==4)for(unsigned x=0;x<256;++x) {
            cfg.iconScaleCode=x;variant(family,x,cfg,sites);
        }
        else variant(family,1,cfg,sites);
    }
    const char* names[]={"GridController.hotSeatsMarginBottom","FolderGridViewGetxController.folderCellHeight",
        "GridCellDelegate.performLayout","GridOccupiedCellDelegate.performLayout","HotSeatLayoutDelegate.cellLayout",
        "LauncherIndicatorState._wrapWithAnimation","LauncherIndicatorState._buildScreenIndicator","LauncherIndicatorState._showIndicator"};
    uint32_t va[8]{},sz[8]{};
    for(unsigned i=0;i<8;++i)assert(hometweaks::HomeTweaksFindSymbol(names[i],&va[i],&sz[i]));
    uint32_t capsule=0;
    assert(workspace_geometry_code_compatible(va[2],sz[2],false));
    assert(workspace_geometry_code_compatible(va[3],sz[3],true));
    assert(hotseat_geometry_code_compatible(va[4],sz[4]));
    assert(capsule_wrapper_layout_compatible(va[5],sz[5],&capsule));
    assert(indicator_policy_compatible(va[6],sz[6]));
    hook(0,va[0],1ull<<7);hook(1,va[1],1ull<<8);
    hook(2,va[2]+0xd8,1ull<<9);hook(3,va[3]+0x234,1ull<<9);hook(4,va[4]+0x568,1ull<<9);
    hook(5,capsule,1ull<<10);hook(6,va[6]+0x3d8,1ull<<11);hook(7,va[7]+0xe0,1ull<<11);
    hook(8,va[6]+0x3fc,1ull<<11);
    const uint32_t refresh=find("LauncherIndicatorState._refreshIndicator");
    const uint32_t animate=find("LauncherIndicatorState._animateIndicator");
    std::vector<uint32_t> code;uint32_t target=0;
    assert(sz[7]==0x144&&dart_words(refresh+0x13c,1,code)&&bl_target(code[0],refresh+0x13c,&target)&&target==va[7]);
    constexpr uint32_t decision[]={0xf85f83a0,0xf81f03a3,0xf841b002,0xeb03005f};
    constexpr uint32_t fallback[]={0x7100103f,0x54000081,0xb846b061,0x8b1c8021,0x14000003,0xb8463061,0x8b1c8021};
    assert(dart_words(va[7]+0xe0,4,code)&&std::equal(std::begin(decision),std::end(decision),code.begin()));
    assert(dart_words(animate+0xdc,7,code)&&std::equal(std::begin(fallback),std::end(fallback),code.begin()));
    std::printf("{\"type\":\"branches\",\"values\":[%u,%u,%u,%u]}\n",va[6]+0x3e8,va[6]+0x40c,va[6]+0x418,refresh+0x140);
    for(unsigned i=0;i<5;++i) {
        const auto&s=home_layout::kFolderGeometrySites[i];const uint32_t f=find(s.symbol);
        const auto full=i<2||i==4?std::span<const uint32_t>(home_layout::kFolderPositionOriginal):std::span<const uint32_t>(home_layout::kFolderSizeOriginal);
        assert(dart_words(f,full.size(),code)&&home_layout::folder_original_body_matches(code,i<2||i==4));
        assert(dart_words(f+s.offset,4,code)&&std::equal(std::begin(s.words),std::end(s.words),code.begin()));
        hook(9+i,f+s.offset,1ull<<12);
    }
    hook(14,find("Utilities.getDefaultGestureAnimMagicSpeed"),1ull<<14);
    for(const auto&s:home_layout::kCapacitySites) {
        uint32_t f=0,size=0;assert(hometweaks::HomeTweaksFindSymbol(s.symbol,&f,&size)&&size==s.size);
        assert(dart_words(f+s.offset-12,4,code)&&std::equal(std::begin(s.guard),std::end(s.guard),code.begin()));
        std::printf("{\"type\":\"capacity\",\"va\":%u,\"before\":%u,\"after\":%u}\n",f+s.offset,s.guard[3],s.replacement);
    }
    for(auto [a,n]:ranges)std::printf("{\"type\":\"guard\",\"va\":%u,\"size\":%u}\n",a,n);
}
