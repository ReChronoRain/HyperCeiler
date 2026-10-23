/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_native_columns.h"
#include "home_dart_fields.h"
#include <map>
#include <set>
namespace home_layout::features {
struct ColumnsFeature {
    uint32_t owner=0,copy=0,persist=0,save=0,put=0;
    DartColumns argument{};
    int32_t save_field=0;
};
inline bool columns_replay_safe(const Function& f,const DartColumns& shape) {
    using namespace home_dynamic;
    if(!frame_slot(f.words,shape.frame)||uint64_t(shape.splice)+16>f.words.size()*4)return false;
    for(size_t i=0;i<f.words.size();++i) {
        const auto w=f.words[i];int32_t delta=0;bool branch=false;
        if((w&0xfc000000u)==0x14000000u){delta=int32_t(w<<6)>>6;branch=true;}
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u){delta=int32_t(w<<8)>>13;branch=true;}
        else if((w&0x7e000000u)==0x36000000u){delta=int32_t(w<<13)>>18;branch=true;}
        const auto at=i*4;
        if(at>=shape.splice&&at<shape.splice+16&&(branch||(w&0xfc000000u)==0x94000000u))return false;
        const int64_t to=int64_t(at)+int64_t(delta)*4;
        if(branch&&to>shape.splice&&to<shape.splice+16)return false;
    }
    return true;
}
// Correlate the clone's raw counts with the independently proved config bounds.
// Every fresh-object field must have an original receiver read of the same
// width, a bounded stack source and a bounded allocator destination. The two
// integer fallback/unboxing diamonds must reach the very locals published in
// the clone. Tags, class, fields and locals are read from this image.
inline bool columns_context_copy(const Code& code,uint32_t va,int32_t columns,int32_t rows) {
    using namespace home_dynamic;
    Body b;if(!code.body(va,b)||!frame_size(b))return false;
    unsigned clones=0;
    for(size_t i=0;i+5<b.size();++i) {
        uint32_t allocator=0,header=0;Body stub;
        if(!target(b[i],va+uint32_t(i*4),allocator)||!code.prefix(allocator,3,stub)
            ||!tag(stub,header)||(stub[2]&0xfc000000u)!=0x14000000u)continue;
        const uint32_t bytes=((header>>8)&15)*16;if(bytes<32)continue;
        std::map<int32_t,std::pair<int32_t,uint32_t>> fields;
        size_t at=i+1;unsigned doubles=0,refs=0;
        for(;at+1<b.size();at+=2) {
            const auto load=b[at],store=b[at+1];const auto op=store&0xffe00000u;
            const bool dbl=ldd(load,29,0)&&std_(store,0,0);
            const bool raw=ldx(load,29,1)&&stx(store,0,1);
            const bool ref=ldx(load,29,1)&&mem(store,0xb8000000u,0,1);
            if(!dbl&&!raw&&!ref)break;
            const auto field_offset=imm9(store),local=imm9(load);
            const uint32_t width=ref?4:8;
            if(!field(field_offset)||!frame_slot(b,local)||uint32_t(field_offset+1)+width>bytes
                ||!fields.emplace(field_offset,std::pair{local,op}).second)break;
            bool input=false;
            for(size_t j=3;j<i;++j) {
                const auto w=b[j];
                if(rn(w)==1&&imm9(w)==field_offset&&((dbl&&ldd(w,1,rd(w)))
                    ||(raw&&ldx(w,1,rd(w)))||(ref&&ldw(w,1,rd(w)))))input=true;
            }
            if(!input)break;
            doubles+=dbl;refs+=ref;
        }
        if(at+2>=b.size()||b[at]!=0xaa1d03efu||b[at+1]!=0xa8c179fdu||b[at+2]!=0xd65f03c0u
            ||doubles<2||refs<1||!fields.count(columns)||!fields.count(rows))continue;
        unsigned counts=0;
        for(auto field_offset:{columns,rows}) {
            const auto [local,op]=fields.at(field_offset);if(op!=0xf8000000u)continue;
            unsigned diamonds=0;
            for(size_t j=3;j+5<i;++j) {
                const auto reg=rd(b[j]);
                if(!ldx(b[j],1,reg)||imm9(b[j])!=field_offset||(b[j+1]&0xfc000000u)!=0x14000000u
                    ||int32_t(b[j+1]<<6)>>6!=4||!stx(b[j+5],29,reg)||imm9(b[j+5])!=local
                    ||(b[j+2]&0xfffffc1fu)!=(0x93417c00u|reg)
                    ||(b[j+3]&0x7e000000u)!=0x36000000u)continue;
                const unsigned boxed=rn(b[j+2]);
                if((b[j+3]&31)!=boxed||(b[j+3]&0xfff80000u)!=0x36000000u
                    ||(int32_t(b[j+3]<<13)>>18)!=2||!ldx(b[j+4],boxed,reg)||imm9(b[j+4])!=7)continue;
                ++diamonds;
            }
            if(diamonds==1)++counts;
        }
        const std::array<size_t,1> point{i};
        if(counts==2&&passes_return_points(b,point))++clones;
    }
    return clones==1;
}
// Integer key/value arguments survive the async wrapper in distinct frame
// roots and are forwarded in order to its actual setter. No preference name or
// symbol is used to select this callee.
inline bool integer_preference_sink(const Code& code,uint32_t va) {
    using namespace home_dynamic;Body b;
    if(!code.body(va,b)||!frame_size(b))return false;
    int32_t key=0,value=0;
    if(!receiver_slot(b,1,key)||!receiver_slot(b,2,value)||key==value)return false;
    unsigned calls=0;
    for(size_t i=0;i+7<b.size();++i) {
        uint32_t setter=0,finish=0;Body callee;
        if(!ldx(b[i],29,1)||!frame_slot(b,imm9(b[i]))||!ldx(b[i+1],29,2)||imm9(b[i+1])!=key
            ||!ldx(b[i+2],29,3)||imm9(b[i+2])!=value||!target(b[i+3],va+uint32_t((i+3)*4),setter)
            ||!code.body(setter,callee)||!frame_size(callee)||b[i+4]!=0xaa0003e1u
            ||!stx(b[i+5],29,1)||!frame_slot(b,imm9(b[i+5]))
            ||!target(b[i+6],va+uint32_t((i+6)*4),finish)||(b[i+7]&0xfc000000u)!=0x14000000u)continue;
        ++calls;
    }
    return calls==1;
}
inline std::optional<ColumnsFeature> columns_feature(const Code& code,uint32_t current) {
    using namespace home_dynamic;
    Body root;if(!code.body(current,root)||!frame_size(root))return {};
    std::optional<std::pair<int32_t,int32_t>> dimensions;
    for(const auto& f:code.functions()) {
        GridFieldOffsets fields;
        if(!read_grid_counts(f.words,f.va,current,fields))continue;
        const std::pair found{fields.columns,fields.rows};
        if(dimensions&&*dimensions!=found)return {};
        dimensions=found;
    }
    if(!dimensions)return {};
    struct Policy {uint32_t va,put;};std::vector<Policy> policies;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        for(size_t i=4;i<b.size();++i) {
            uint32_t config=0,put=0;
            if(!target(b[i-4],f.va+uint32_t((i-4)*4),config)||config!=current
                ||!ldx(b[i-3],0,2)||imm9(b[i-3])!=dimensions->first
                ||!target(b[i],f.va+uint32_t(i*4),put))continue;
            unsigned rows=0;
            for(size_t j=i+1;j<b.size();++j) {
                uint32_t to=0;
                if(target(b[j],f.va+uint32_t(j*4),to)&&to==current&&j+1<b.size()
                    &&ldx(b[j+1],0,2)&&imm9(b[j+1])==dimensions->second)++rows;
            }
            if(rows==1&&grid_persist_boundary(b,f.va,current,put)&&integer_preference_sink(code,put))
                policies.push_back({f.va,put});
        }
    }
    if(policies.size()!=1)return {};
    const auto policy=policies.front();
    std::optional<std::pair<uint32_t,int32_t>> save;
    for(const auto& f:code.functions()) {
        const auto field=grid_layout_save_field(f.words,f.va,policy.put);if(!field)continue;
        if(!frame_size(f.words)||!frame_slot(f.words,imm9(f.words[3]))||!frame_slot(f.words,imm9(f.words[4])))continue;
        if(save)return {};save=std::pair{f.va,*field};
    }
    if(!save)return {};
    std::optional<ColumnsFeature> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        for(size_t i=4;i+6<b.size();++i) {
            uint32_t copy=0;
            if(!ldw(b[i],0,2)||b[i+1]!=0xd37ff823u||b[i+2]!=0xa90009e3u
                ||!target(b[i+6],f.va+uint32_t((i+6)*4),copy))continue;
            const auto shape=resolve_dart_columns(b,f.va,copy);
            if(!shape||!columns_replay_safe(f,*shape)
                ||!columns_context_copy(code,copy,dimensions->first,dimensions->second))continue;
            if(result)return {};
            result=ColumnsFeature{f.va,copy,policy.va,save->first,policy.put,*shape,save->second};
        }
    }
    return result;
}
} // namespace home_layout::features
