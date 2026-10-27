/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_title_feature_resolver.h"
#include "home_title_custom.h"
#include "home_runtime_layout_contracts.h"
namespace home_layout::features {
struct CustomTitleSite {
    uint32_t owner=0,patch=0,resume=0;
    int32_t source_frame=0,label_field=0,label_frame=0,aux_frame=0,model_frame=0;
    int32_t model_field=0;
};
struct CustomTitleFeature {
    std::array<CustomTitleSite,2> sites{};
    uint32_t package=0,component=0,pin=0,one_allocator=0,two_allocator=0;
    home_dynamic::ModelLayout model;
    home_title::StringLayout strings;
};
inline bool string_allocator(const home_dynamic::Body& b,uint32_t& tag,int32_t& top,int32_t& end,int32_t& length,bool& one) {
    using namespace home_dynamic;
    // Verified ARM64 Dart string allocation ABI, including Smi length and the
    // original overflow/end guards. Runtime operands are decoded below.
    if(b.size()<29||b[0]!=0xf94001e2u||b[1]!=0x93407c42u||b[6]!=0xaa0203e6u)return false;
    one=b[7]==0x93417c42u;size_t i=one?8:7;
    if(i+21>=b.size()||b[i]!=0x91007c42u||b[i+1]!=0x927cec42u||!ux(b[i+2],26,0)
        ||b[i+3]!=0xab020001u||(b[i+4]&0xff00001fu)!=0x54000002u||!ux(b[i+5],26,7)
        ||b[i+6]!=0xeb07003fu||(b[i+7]&0xff00001fu)!=0x54000002u
        ||(b[i+8]&0xffc003ffu)!=0xf9000341u||uoff(b[i+8])!=uoff(b[i+2])
        ||b[i+9]!=0x91000400u||b[i+10]!=0xa93f7c3fu||b[i+11]!=0xf103c05fu
        ||b[i+12]!=0xd37cec42u||b[i+13]!=0x9a9f9042u||!movz(b[i+14],16)
        ||(b[i+15]&0xffe0001fu)!=0xf2a00010u||b[i+16]!=0xaa100042u||b[i+17]!=0xf81ff002u
        ||!stx(b[i+18],0,31)||!mem(b[i+19],0xb8000000u,0,6)
        ||imm9(b[i+18])!=imm9(b[i+19]))return false;
    tag=zimm(b[i+14])|(zimm(b[i+15])<<16);top=uoff(b[i+2]);end=uoff(b[i+5]);length=imm9(b[i+19]);
    return top>0&&end==top+8&&field(length)&&((tag>>8)&15)==0;
}
inline std::optional<CustomTitleFeature> custom_title_feature(const Code& code,const IconFeature& icon,const DockTitleFeature& dock) {
    using namespace home_dynamic;
    Body app;if(!code.body(icon.app,app))return {};
    CustomTitleFeature f;
    std::vector<std::pair<uint32_t,Body>> packages,components,pins;
    // Only package producers consumed by the actual app icon are eligible.
    for(size_t i=0;i<app.size();++i){uint32_t to=0;Body b;
        if(!target(app[i],icon.app+uint32_t(i*4),to)||!code.body(to,b))continue;
        bool dispatch=false,fallback=false;
        for(size_t j=0;j+2<b.size();++j){
            dispatch|=(b[j]&0xffc003ffu)==0xd100001eu&&b[j+1]==0xf87e7abeu&&b[j+2]==0xd63f03c0u;
            fallback|=ldw(b[j],2,3)&&b[j+1]==0x8b1c8063u&&b[j+2]==0xaa0303e0u;
        }
        if(dispatch&&fallback)packages.push_back({to,std::move(b)});
    }
    if(packages.size()!=1)return {};
    for(const auto& fn:code.functions()) {
        const auto& b=fn.words;
        // Component getters are frameless virtual targets and can be inside an
        // inventory fragment. Identify the full Intent/flag return semantics.
        for(size_t i=0;i+14<=b.size();++i)if(movz(b[i],17)&&b[i+1]==0xb8716822u&&b[i+2]==0x8b1c8042u
            &&(b[i+3]&0x7e00001fu)==0x36000002u&&b[i+4]==0xaa1603e0u&&b[i+5]==0xd65f03c0u)
            components.push_back({fn.va+uint32_t(i*4),Body(b.begin()+i,b.begin()+i+14)});
        // A pinned component wrapper calls a managed producer. Its package
        // field must later populate a fresh ComponentName, not a guessed CID.
        if(b.size()>=7&&b[0]==0xa9bf79fdu&&b[1]==0xaa0f03fdu&&b[2]==0xaa0103e0u
            &&runtime_contract::epilogue(b,4)) {uint32_t to=0;Body leaf;
            if(target(b[3],fn.va+12,to)&&code.body(to,leaf))pins.push_back({fn.va,std::move(leaf)});}
    }
    unsigned models=0;
    for(const auto& c:components)for(const auto& pin:pins) {
        ModelLayout fields;if(!model_fields(packages[0].second,c.second,pin.second,fields))continue;
        // The pin producer roots its input package, allocates a ComponentName,
        // and writes that SAME root into the package field read by getPackage.
        unsigned outputs=0;
        for(size_t i=0;i+7<pin.second.size();++i){const auto& b=pin.second;
            if((b[i]&0xfc000000u)==0x94000000u&&ldx(b[i+1],29,1)
                &&mem(b[i+2],0xb8000000u,0,1)&&imm9(b[i+2])==fields.component_package
                &&ldx(b[i+3],29,1)&&mem(b[i+4],0xb8000000u,0,1)
                &&imm9(b[i+4])==fields.component_package+4&&runtime_contract::epilogue(b,i+5))++outputs;
        }
        if(outputs!=1)continue;
        f.model=fields;f.package=packages[0].first;f.component=c.first;f.pin=pin.first;++models;
    }
    if(models!=1)return {};
    unsigned titles=0;
    for(const auto& candidate:dock.builders) {
    const uint32_t shortcut=candidate.va;Body text;
    int32_t root=0;if(!code.body(shortcut,text)||!receiver_slot(text,3,root))continue;
    uint32_t model_field=0;unsigned model_paths=0;
    for(size_t i=1;i+7<text.size();++i){
        if(!movz(text[i],17)||text[i+1]!=0xb8716881u||text[i+2]!=0x8b1c8021u
            ||text[i+3]!=0xf85ff020u||text[i+4]!=0xd34c7c00u
            ||(text[i+5]&0xffc003ffu)!=0xd100001eu||text[i+6]!=0xf87e7abeu||text[i+7]!=0xd63f03c0u)continue;
        bool same_root=false;
        for(size_t j=i>5?i-5:0;j<i;++j)if(ldx(text[j],29,4)&&imm9(text[j])==root)same_root=true;
        if(same_root&&field(zimm(text[i]))){model_field=zimm(text[i]);++model_paths;}
    }
    if(model_paths!=1)continue;
    for(size_t i=0;i+5<text.size();++i){
        if(!ldx(text[i],29,2)||imm9(text[i])!=root||!ldw(text[i+1],2,3)||!field(imm9(text[i+1]))
            ||text[i+2]!=0x8b1c8063u)continue;
        const bool aux=ldx(text[i+3],29,4)&&frame_slot(text,imm9(text[i+3]));
        const auto store=i+(aux?4:3);
        if(!stx(text[store],29,3)||!frame_slot(text,imm9(text[store])))continue;
        unsigned consumers=0;
        for(size_t j=store+1;j+3<text.size();++j)
            if((text[j]&0xfc000000u)==0x94000000u&&ldx(text[j+1],29,2)
                &&imm9(text[j+1])==imm9(text[store])&&stx(text[j+2],29,0)&&mem(text[j+3],0xb8000000u,0,2))++consumers;
        if(consumers!=1)continue;
        f.sites[0]={shortcut,uint32_t(i*4),uint32_t((store+1)*4),root,imm9(text[i+1]),imm9(text[store]),aux?imm9(text[i+3]):0,root,int32_t(model_field)};++titles;
    }
    }
    if(titles!=1)return {};
    // AppIcon already consumes the SAME package producer and stores its model
    // root. Follow that root until the original title string reuses the slot.
    int32_t model=0;
    for(size_t i=1;i<app.size();++i){uint32_t to=0;
        if(!target(app[i],icon.app+uint32_t(i*4),to)||to!=f.package)continue;
        if(ldx(app[i-1],29,1))model=imm9(app[i-1]);
        else if(i>=2&&ldx(app[i-2],29,1)&&stx(app[i-1],29,0))model=imm9(app[i-2]);}
    if(!frame_slot(app,model))return {};
    unsigned drawer=0;
    for(size_t i=0;i+5<app.size();++i)
        if(stx(app[i],29,1)&&ldw(app[i+1],0,2)&&field(imm9(app[i+1]))&&app[i+2]==0x8b1c8042u
            &&stx(app[i+3],29,2)&&imm9(app[i+3])==model&&(app[i+4]&0xfc000000u)==0x94000000u
            &&app[i+5]==0xaa0003e1u){
            f.sites[1]={icon.app,uint32_t(i*4),uint32_t((i+4)*4),0,imm9(app[i+1]),model,imm9(app[i]),model,0};++drawer;
        }
    if(drawer!=1)return {};
    unsigned allocators=0;int32_t length=0;
    for(const auto& fn:code.functions())for(size_t i=0;i+29<=fn.words.size();++i) {
        if(fn.words[i]!=0xf94001e2u||fn.words[i+1]!=0x93407c42u)continue;
        Body b(fn.words.begin()+i,fn.words.end());uint32_t tag=0;int32_t top=0,end=0,len=0;bool one=false;
        if(!string_allocator(b,tag,top,end,len,one))continue;
        if(allocators&&(uint32_t(top)!=f.strings.top||uint32_t(end)!=f.strings.end||len!=length))return {};
        f.strings.top=top;f.strings.end=end;length=len;
        size_t alloc_start=one?8:7;f.strings.bias=(b[alloc_start]>>10)&4095u;f.strings.size_limit=(b[alloc_start+11]>>10)&4095u;
        if(one){if(f.one_allocator)return {};f.one_allocator=fn.va+uint32_t(i*4);f.strings.one_cid=tag>>12;}
        else {if(f.two_allocator)return {};f.two_allocator=fn.va+uint32_t(i*4);f.strings.two_cid=tag>>12;f.strings.tag=tag;}
        ++allocators;
    }
    if(allocators!=2||f.strings.one_cid==f.strings.two_cid)return {};
    f.strings.length=length;
    // Typed halfword read/write consumer of allocateTwoByteString proves the
    // data byte offset and element width, independently of allocator padding.
    std::set<int32_t> data;
    for(const auto& fn:code.functions()){
        const auto& b=fn.words;bool calls=false;
        for(size_t i=0;i<b.size();++i){uint32_t to=0;if(target(b[i],fn.va+uint32_t(i*4),to)&&to==f.two_allocator)calls=true;}
        if(!calls)continue;
        for(size_t i=0;i+3<b.size();++i)
            if(b[i]==0x8b050470u&&mem(b[i+1],0x78400000u,16,6)&&b[i+2]==0x8b050447u
                &&mem(b[i+3],0x78000000u,7,6)&&imm9(b[i+1])==imm9(b[i+3]))data.insert(imm9(b[i+1]));
    }
    if(data.size()!=1)return {};
    f.strings.data=*data.begin();
    if(!f.strings.usable())return {};
    return f;
}
}
