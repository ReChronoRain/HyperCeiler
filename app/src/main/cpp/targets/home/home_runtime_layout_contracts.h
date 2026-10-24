/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
namespace home_layout::runtime_contract {
using namespace home_dynamic;
inline bool epilogue(const Body& b,size_t i) {
    return i+2<b.size()&&b[i]==0xaa1d03efu&&b[i+1]==0xa8c179fdu&&b[i+2]==0xd65f03c0u;
}
inline bool entry(const Body& b) {
    return b.size()>=5&&b[0]==0xa9bf79fdu&&b[1]==0xaa0f03fdu;
}
struct ScalarSource {uint32_t target=0;int32_t field=0,payload=0,local=0;};
inline bool margin_return(const Body& b,size_t at,uint32_t va,uint32_t screen,int32_t& local) {
    if(at+18>b.size())return false;
    // The original absolute-value diamond must carry this producer's D0 into
    // the saved scalar. No unrelated double or call may intervene.
    if(b[at]!=0x6e211c21u||b[at+1]!=0x1e612000u||b[at+3]!=0x6e201c00u
        ||b[at+5]!=0x1e602020u||b[at+7]!=0x1e614001u||b[at+8]!=0x4ea11c20u
        ||!ldx(b[at+9],29,1)||!frame_slot(b,imm9(b[at+9])))return false;
    if((b[at+2]&0xff00001fu)!=0x54000001u||(int32_t(b[at+2]<<8)>>13)!=3
        ||(b[at+4]&0xfc000000u)!=0x14000000u||(int32_t(b[at+4]<<6)>>6)!=5
        ||(b[at+6]&0xff00001fu)!=0x5400000du||(int32_t(b[at+6]<<8)>>13)!=3)return false;
    uint32_t to=0;const auto end=at+10;
    if(!std_(b[end],29,0)||!frame_slot(b,imm9(b[end]))
        ||!target(b[end+1],va+(end+1)*4,to)||to!=screen
        ||!ldd(b[end+2],29,1)||imm9(b[end+2])!=imm9(b[end])
        ||b[end+3]!=0x1e602822u||b[end+4]!=0x4ea21c40u||!epilogue(b,end+5))return false;
    local=imm9(b[end]);return true;
}
// Follow the actual layout consumer to its scalar source. A symbol is an owner
// hint, never sufficient evidence that an entry returns the value we modify.
template<class Read>
inline bool layout_scalar(const Body& consumer,uint32_t va,uint32_t config,uint32_t screen,
    bool margin,Read read,ScalarSource& out) {
    ScalarSource found;unsigned candidates=0;
    for(size_t i=0;i+4<consumer.size();++i) {
        uint32_t callee=0;
        if(!target(consumer[i],va+i*4,callee)||consumer[i+1]!=0x4ea01c01u
            ||!ldd(consumer[i+2],29,0)||!frame_slot(consumer,imm9(consumer[i+2]))
            ||consumer[i+3]!=(margin?0x1e613802u:0x1e611802u))continue;
        Body b;if(!read(callee,b)||!entry(b))continue;
        ScalarSource s;s.target=callee;unsigned sources=0,returns=0;
        for(size_t j=0;j+4<b.size();++j){uint32_t to=0;
            if(!target(b[j],callee+j*4,to)||to!=config||!ldw(b[j+1],0,1)
                ||b[j+2]!=0x8b1c8021u||!ldd(b[j+3],1,0)
                ||!field(imm9(b[j+1]))||!field(imm9(b[j+3])))continue;
            s.field=imm9(b[j+1]);s.payload=imm9(b[j+3]);++sources;
            if(!margin&&epilogue(b,j+4))++returns;
            if(margin&&margin_return(b,j+4,callee,screen,s.local))++returns;
        }
        if(sources==1&&returns==1){found=s;++candidates;}
    }
    if(candidates!=1)return false;out=found;return true;
}
// The Dart-facing Rust API unboxes the double immediately before its return.
inline bool ratio_api(const Body& b) {
    unsigned returns=0;
    if(!entry(b))return false;
    for(size_t i=1;i+3<b.size();++i)
        if((b[i-1]&0xfc000000u)==0x94000000u&&b[i]==0xfc407000u&&epilogue(b,i+1))++returns;
    return returns==1;
}
inline bool ratio_source(const Body& b,uint32_t va,uint32_t api) {
    if(!entry(b))return false;unsigned calls=0;
    for(size_t i=1;i+3<b.size();++i){uint32_t to=0;
        if(b[i-1]==0xaa0003e1u&&target(b[i],va+i*4,to)&&to==api&&epilogue(b,i+1))++calls;
    }
    return calls==1;
}
template<class Read>
inline bool animation_ratio(const Body& flight,uint32_t va,uint32_t api,const Body& api_body,
    Read read,uint32_t& out) {
    if(!entry(flight)||!ratio_api(api_body))return false;
    unsigned direct=0,candidates=0;uint32_t found=0;
    for(size_t i=0;i+3<flight.size();++i) {
        if(flight[i]==0xfc407020u&&epilogue(flight,i+1))++direct;
        uint32_t to=0;Body b;
        if(target(flight[i],va+i*4,to)&&read(to,b)&&ratio_source(b,to,api)){
            found=to;++candidates;
        }
    }
    if(direct!=1||candidates!=1)return false;out=found;return true;
}
inline bool animation_magic(const Body& b,uint32_t va,uint32_t high,uint32_t low) {
    if(!frame_size(b))return false;
    unsigned tails=0,highs=0,lows=0;size_t tail=0;int32_t local=0;
    for(size_t i=0;i+4<b.size();++i)if(ldd(b[i],29,1)&&frame_slot(b,imm9(b[i]))
        &&b[i+1]==0x1e620820u&&epilogue(b,i+2)){
        local=imm9(b[i]);tail=i;++tails;
    }
    if(tails!=1)return false;
    const auto branch=[&](size_t at,size_t expected){
        if((b[at]&0xfc000000u)!=0x14000000u)return false;
        return int64_t(at)+(int32_t(b[at]<<6)>>6)==int64_t(expected);
    };
    for(size_t i=1;i+10<tail;++i){uint32_t to=0,lo=0;
        if(!target(b[i],va+i*4,to)||to!=high||!std_(b[i-1],29,0)||imm9(b[i-1])!=local
            ||(b[i+1]&0xfff8001fu)!=0x37200000u||b[i+2]!=0x1e6e1002u
            ||!branch(i+3,tail)||!target(b[i+4],va+(i+4)*4,lo)||lo!=low
            ||(b[i+5]&0xfff8001fu)!=0x37200000u||b[i+6]!=0x1e6d1001u
            ||!branch(i+7,tail-1)||b[tail-1]!=0x4ea11c22u)continue;
        const auto tb=[&](size_t at){return int64_t(at)+(int32_t(b[at]<<13)>>18);};
        if(tb(i+1)!=int64_t(i+4)||tb(i+5)!=int64_t(i+8))continue;
        if((b[i+8]&0xff8003ffu)!=0x91000371u||(b[i+9]&0xffc003ffu)!=0xfd400221u)continue;
        ++highs;++lows;
    }
    return highs==1&&lows==1;
}
inline bool new_install_methods(const Body& folder,const Body& light) {
    if(!entry(folder)||!entry(light))return false;
    unsigned booleans=0,dispatch=0,collection=0,voids=0,children=0,gates=0;
    for(size_t i=0;i+3<folder.size();++i) {
        if((folder[i]==0x910082c0u||folder[i]==0x9100c2c0u)&&epilogue(folder,i+1))++booleans;
        if(folder[i]==0xf87e7abeu&&folder[i+1]==0xd63f03c0u)++dispatch;
        if(ldw(folder[i],1,0)&&field(imm9(folder[i]))&&folder[i+1]==0x8b1c8000u
            &&folder[i+2]==0xf85ff001u&&folder[i+3]==0xd34c7c21u)++collection;
        // This method only queries the original model; no object store is admitted.
        if((folder[i]&0x3b200c00u)==0x38000000u&&!(folder[i]&(1u<<22))&&rn(folder[i])!=29)return false;
    }
    if(booleans!=2||dispatch!=4||collection!=1)return false;
    int32_t child=0;
    for(size_t i=0;i+10<light.size();++i) {
        if(light[i]==0xaa1603e0u&&epilogue(light,i+1))++voids;
        if(mem(light[i],0xb8000000u,1,0)&&field(imm9(light[i]))
            &&light[i+1]==0x385ff030u&&light[i+2]==0x385ff011u
            &&light[i+3]==0x8a500a30u&&light[i+4]==0xea5c821fu
            &&light[i+5]==0x54000040u&&(light[i+6]&0xfc000000u)==0x94000000u
            &&light[i+7]==0xaa1603e0u&&epilogue(light,i+8)){
            child=imm9(light[i]);++children;
        }
    }
    for(size_t i=0;i+7<light.size();++i) {
        if(!ldw(light[i],0,2)||!field(imm9(light[i]))||light[i+1]!=0x8b1c8042u
            ||!ux(light[i+2],27,16)||light[i+3]!=0x6b10005fu
            ||(light[i+4]&0xff00001fu)!=0x54000000u||!ldw(light[i+5],2,3)
            ||!field(imm9(light[i+5]))||light[i+6]!=0x8b1c8063u
            ||(light[i+7]&0xfff8001fu)!=0x37200003u)continue;
        const auto to=int64_t(i+7)+(int32_t(light[i+7]<<13)>>18);
        if(to<0||to+2>=int64_t(light.size())||light[to]!=0xaa0003e1u
            ||!ldw(light[to+1],1,2)||imm9(light[to+1])!=child||light[to+2]!=0x8b1c8042u)continue;
        ++gates;
    }
    return voids==1&&children==1&&gates==1;
}
} // namespace home_layout::runtime_contract
