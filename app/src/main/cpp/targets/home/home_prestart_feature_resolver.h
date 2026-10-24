/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"

namespace home_layout::features::prestart {
using namespace home_dynamic;
struct Plan {
    uint32_t entry=0,initializer=0,property=0,bridge=0,api=0;
    uint32_t table=0,flag=0;
};

// ARM64 Dart ABI only. Frame slots, static-table fields and all call targets
// come from the current executable. Neither private symbols nor saved RVAs or
// pool displacements identify this feature.
inline bool tuple_api(const Code& code,uint32_t va,unsigned arity,uint32_t* allocator=nullptr) {
    Body b;if(!code.body(va,b)||!frame_size(b))return false;
    unsigned tuples=0,returns=0;uint32_t factory=0;
    for(size_t i=0;i<b.size();++i) {
        uint32_t to=0;
        if(i+2<b.size()&&movz(b[i],1)&&zimm(b[i])==arity&&target(b[i+1],va+uint32_t((i+1)*4),to)
            &&(b[i+2]&0xffffffe0u)==0xaa0003e0u) {
            const auto tuple=rd(b[i+2]);std::vector<int32_t> fields;
            for(size_t j=i+3;j<b.size();++j) {
                // The next receiver decompression ends tuple construction;
                // later task/closure fields cannot be counted as tuple fields.
                if(j+1<b.size()&&(b[j]&0xffe00c00u)==0xb8400000u&&rn(b[j])!=29
                    &&(b[j+1]&0xfffffc00u)==0x8b1c8000u)break;
                if((b[j]&0xffe00fe0u)==(0xb8000000u|(tuple<<5))
                    &&field(imm9(b[j])))fields.push_back(imm9(b[j]));
            }
            std::sort(fields.begin(),fields.end());
            if(fields.size()!=arity||std::adjacent_find(fields.begin(),fields.end())!=fields.end())continue;
            factory=to;++tuples;
        }
        if(b[i]==0xd65f03c0u&&i>=2&&runtime_contract::epilogue(b,i-2))++returns;
    }
    if(tuples!=1||returns!=1)return false;
    if(allocator)*allocator=factory;
    return true;
}

// The original bridge forwards four process arguments, including an unboxed
// integer, to an FRB API with five tuple fields (the API receiver is the fifth).
inline bool process_bridge(const Code& code,uint32_t va,uint32_t& api) {
    Body b;if(!code.body(va,b)||!frame_size(b))return false;
    int32_t locals[4]{};
    const unsigned args[]={1,2,3,5};
    for(unsigned i=0;i<4;++i)if(!receiver_slot(b,args[i],locals[i]))return false;
    for(unsigned i=0;i<4;++i)for(unsigned j=0;j<i;++j)if(locals[i]==locals[j])return false;
    unsigned calls=0;
    for(size_t i=4;i+3<b.size();++i) {
        uint32_t to=0;
        if(!ldx(b[i-4],29,2)||imm9(b[i-4])!=locals[0]
            ||!ldx(b[i-3],29,3)||imm9(b[i-3])!=locals[1]
            ||!ldx(b[i-2],29,5)||imm9(b[i-2])!=locals[2]
            ||!ldx(b[i-1],29,6)||imm9(b[i-1])!=locals[3]
            ||!target(b[i],va+uint32_t(i*4),to)||!runtime_contract::epilogue(b,i+1)
            ||!tuple_api(code,to,5))continue;
        api=to;++calls;
    }
    return calls==1;
}

inline bool property_bridge(const Code& code,uint32_t va,uint32_t* allocator=nullptr) {
    Body b;if(!code.body(va,b)||!frame_size(b))return false;
    int32_t key=0,default_value=0;
    if(!receiver_slot(b,1,default_value)||!receiver_slot(b,2,key)||key==default_value)return false;
    unsigned calls=0;uint32_t factory=0;
    for(size_t i=2;i+3<b.size();++i) {
        uint32_t to=0;
        if(ldx(b[i-2],29,2)&&imm9(b[i-2])==default_value
            &&ldx(b[i-1],29,3)&&imm9(b[i-1])==key
            &&target(b[i],va+uint32_t(i*4),to)&&tuple_api(code,to,3,&factory)
            &&runtime_contract::epilogue(b,i+1))++calls;
    }
    if(calls!=1)return false;
    if(allocator)*allocator=factory;
    return true;
}

inline bool initializer(const Code& code,const Function& f,uint32_t table,uint32_t flag,uint32_t& property) {
    const auto& b=f.words;if(!frame_size(b))return false;
    unsigned producers=0;
    for(size_t i=0;i+8<b.size();++i) {
        uint32_t to=0,await=0;
        // getBool(default:false, key) -> await -> same static flag. The
        // managed await remains entirely original; no Future is fabricated.
        if(!target(b[i],f.va+uint32_t(i*4),to)||!property_bridge(code,to)
            ||b[i+1]!=0xaa0003e1u||!stx(b[i+2],29,1)||!frame_slot(b,imm9(b[i+2]))
            ||!target(b[i+3],f.va+uint32_t((i+3)*4),await)
            ||!ux(b[i+4],26,1)||uoff(b[i+4])!=table
            ||(b[i+5]&0xffc003ffu)!=0xf9000020u||uoff(b[i+5])!=flag
            ||b[i+6]!=0x910082c0u||!ux(b[i+7],26,1)||uoff(b[i+7])!=table
            ||(b[i+8]&0xffc003ffu)!=0xf9000020u||uoff(b[i+8])==flag)continue;
        bool key=false,default_false=false;
        for(size_t j=i>4?i-4:0;j<i;++j) {
            if(b[j]==0x9100c2c1u)default_false=true;
            if(ux(b[j],27,2)||((b[j]&0xffc003ffu)==0x91400362u))key=true;
        }
        if(key&&default_false){property=to;++producers;}
    }
    return producers==1;
}

inline std::optional<Plan> resolve(const Code& code) {
    std::optional<Plan> result;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(!frame_size(b)||b.size()<32||b.size()>0x1000/4)continue;
        int32_t argument=0;if(!receiver_slot(b,1,argument))continue;
        unsigned null_returns=0,foreign_returns=0,bridges=0;Plan p;p.entry=f.va;
        // Only the synchronous, nullable-void process-prelaunch consumer is
        // admitted. Visual pre-start transitions and async launchers are not.
        for(size_t i=0;i<b.size();++i) {
            if(b[i]==0xd65f03c0u) {
                if(i>=3&&b[i-3]==0xaa1603e0u&&runtime_contract::epilogue(b,i-2))++null_returns;
                else ++foreign_returns;
            }
            uint32_t to=0,api=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&process_bridge(code,to,api)){
                p.bridge=to;p.api=api;++bridges;
            }
        }
        if(foreign_returns||null_returns<6||bridges!=1)continue;
        // The first non-null path reads the initializer's cached boolean and
        // branches around an original logged null return. Validate that edge.
        unsigned gates=0;
        for(size_t i=0;i+3<b.size()&&i<64;++i) {
            if(!ux(b[i],26,0)||!ux(b[i+1],0,0)||(b[i+2]&0xfff8001fu)!=0x36200000u)continue;
            const auto next=int64_t(i+2)+(int32_t(b[i+2]<<13)>>18);
            if(next<=int64_t(i+5)||next>=int64_t(b.size())||b[next-4]!=0xaa1603e0u
                ||!runtime_contract::epilogue(b,size_t(next-3)))continue;
            p.table=uoff(b[i]);p.flag=uoff(b[i+1]);++gates;
        }
        if(gates!=1)continue;
        unsigned sources=0;
        for(const auto& source:code.functions()) {
            uint32_t property=0;
            if(initializer(code,source,p.table,p.flag,property)){
                p.initializer=source.va;p.property=property;++sources;
            }
        }
        if(sources!=1)continue;
        uint32_t process_allocator=0,property_allocator=0;
        if(!tuple_api(code,p.api,5,&process_allocator)||!property_bridge(code,p.property,&property_allocator)
            ||process_allocator!=property_allocator)continue;
        if(result)return {}; // Ambiguity retains the OEM path.
        result=p;
    }
    return result;
}
} // namespace home_layout::features::prestart
