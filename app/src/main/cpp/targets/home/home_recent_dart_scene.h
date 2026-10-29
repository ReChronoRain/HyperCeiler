/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include <set>

namespace home_recent_bar::dart_scene {
using home_dynamic::Body;
using home_layout::features::Code;
using namespace home_dynamic;
struct Plan {
    uint32_t getter=0, wrapper=0, transition=0, observe=0;
    uint32_t static_table=0, overview=0, sentinel_pool=0;
    int32_t receiver_frame=0, state_field=0, below_field=0;
};
inline std::optional<std::array<uint32_t,4>> replay_words(const Code& code,const Plan& plan){
    // Dart AOT has no Rust EH header. Reuse the executable instruction inventory
    // that proved this observation point, including on sectionless snapshots.
    Body words;if(!code.prefix(plan.observe,4,words))return {};
    std::array<uint32_t,4> result{};std::copy(words.begin(),words.end(),result.begin());return result;
}
enum class ReadResult {Ready,Frame,Memory,Uninitialized,State};
struct Observation {ReadResult result=ReadResult::Memory;bool active=false;};
inline const char* read_reason(ReadResult value){switch(value){
    case ReadResult::Ready:return "committed Flutter state read";case ReadResult::Frame:return "Dart frame is not word aligned";
    case ReadResult::Uninitialized:return "overview singleton has not initialized";case ReadResult::State:return "invalid tagged state reference";
    default:return "committed state memory unavailable";
}}
// The observer runs synchronously after the original store/write barrier. Reads
// only current Dart frame roots; no allocation, Dart call or retained heap pointer.
template<class Read> Observation sample(const Plan& p,uint64_t frame,uint64_t thread,uint64_t pool,Read read){
    if(!frame||!thread||!pool||(frame&7)||(thread&7)||!slot(p.receiver_frame)
        ||!field(p.state_field)||!p.static_table||!p.overview||!p.sentinel_pool
        ||frame<uint64_t(-p.receiver_frame)||thread>UINT64_MAX-p.static_table)return {ReadResult::Frame,false};
    uint64_t receiver=0,table=0,overview=0,sentinel=0;uint32_t current=0;
    if(!read(frame+p.receiver_frame,receiver)||!read(thread+p.static_table,table)
        ||!table||(table&7)||table>UINT64_MAX-p.overview||pool>UINT64_MAX-p.sentinel_pool
        ||!read(table+p.overview,overview)||!read(pool+p.sentinel_pool,sentinel))return {};
    if(overview==sentinel)return {ReadResult::Uninitialized,false};
    if(!(receiver&1)||receiver>UINT64_MAX-uint32_t(p.state_field)||!(overview&1))return {ReadResult::State,false};
    if(!read(receiver+uint32_t(p.state_field),current))return {};
    if(!(current&1))return {ReadResult::State,false};
    // Match the getter's compressed CMP W identity, not a copied object or CID.
    return {ReadResult::Ready,current==uint32_t(overview)};
}
// Keep the HCT frame shape and three scene bits. Bit zero is the main
// overview route, now observed from its committed Flutter state in either
// orientation. Bit two remains the dedicated Rust recents-window route.
inline unsigned compose(unsigned legacy,unsigned observation){
    if((observation>>2)!=unsigned(ReadResult::Ready))return 0;
    return 8u+((observation&2)?((legacy&4)|(observation&1)):(legacy&7));
}
inline bool boolean_tail(const Body& b,size_t at) {
    // ARM64 Dart boolean ABI, observed in the current getter itself, not heap addresses.
    return at+7<=b.size()&&b[at]==0x910082d0u&&b[at+1]==0x9100c2d1u
        &&b[at+2]==0x9a910202u&&b[at+3]==0xaa0203e0u
        &&b[at+4]==0xaa1d03efu&&b[at+5]==0xa8c179fdu&&b[at+6]==0xd65f03c0u;
}
inline bool singleton_getter(const Code& code,uint32_t va,Plan& p,uint32_t& initializer) {
    Body b;if(!code.prefix(va,23,b)||frame_size(b)!=8||!ldw(b[3],1,0)||!field(imm9(b[3]))
        ||b[4]!=0x8b1c8000u||!stx(b[5],29,0)||!frame_slot(b,imm9(b[5]))
        ||!ux(b[6],26,0)||!ux(b[7],0,0)||!ux(b[8],27,16)||b[9]!=0x6b10001fu
        ||(b[10]&0xff00001fu)!=0x54000001u||((int32_t(b[10]<<8)>>13)!=4)
        ||(b[11]&0xffc003ffu)!=0x91400362u||!ux(b[12],2,2)
        ||!target(b[13],va+52,initializer)||!ldx(b[14],29,1)||imm9(b[14])!=imm9(b[5])
        ||b[15]!=0x6b00003fu||!boolean_tail(b,16))return false;
    p.getter=va;p.state_field=imm9(b[3]);p.static_table=uoff(b[6]);p.overview=uoff(b[7]);p.sentinel_pool=uoff(b[8]);
    return p.static_table&&p.overview&&p.sentinel_pool;
}
inline bool overlay_predicate(const Code& code,uint32_t va,const Plan& p,uint32_t initializer,std::set<uint32_t>& states) {
    Body b;if(!code.prefix(va,59,b)||frame_size(b)!=8||!stx(b[3],29,2)||!frame_slot(b,imm9(b[3]))
        ||!boolean_tail(b,52))return false;
    std::set<uint32_t> next;
    unsigned calls=0;
    for(size_t i=0;i<b.size();++i){uint32_t to=0;
        if(target(b[i],va+uint32_t(i*4),to)){if(to!=initializer)return false;++calls;}
        if(i+3<b.size()&&ux(b[i],26,0)&&uoff(b[i])==p.static_table&&ux(b[i+1],0,0)
            &&ux(b[i+2],27,16)&&uoff(b[i+2])==p.sentinel_pool&&b[i+3]==0x6b10001fu)
            next.insert(uoff(b[i+1]));
    }
    if(calls!=4||next.size()!=4||next.contains(p.overview))return false;
    states=std::move(next);return true;
}
inline bool window_initialization(const Code& code,const Plan& p,uint32_t initializer,const std::set<uint32_t>& overlays,uint32_t& normal) {
    unsigned hits=0;
    for(const auto& f:code.functions()){
        Body b;if(!code.prefix(f.va,28,b)||frame_size(b)!=8||!stx(b[3],29,2)||!frame_slot(b,imm9(b[3]))
            ||!ux(b[4],26,0)||uoff(b[4])!=p.static_table||!ux(b[5],0,0)
            ||!ux(b[6],27,16)||uoff(b[6])!=p.sentinel_pool||b[7]!=0x6b10001fu
            ||b[8]!=0x54000081u||b[15]!=0x54000120u||b[20]!=0x54000081u
            ||b[12]!=0xaa0003e1u||!ldx(b[13],29,0)||imm9(b[13])!=imm9(b[3])||b[14]!=0x6b01001fu
            ||!ux(b[16],26,0)||uoff(b[16])!=p.static_table||!ux(b[17],0,0)||uoff(b[17])!=p.overview
            ||!ux(b[18],27,16)||uoff(b[18])!=p.sentinel_pool||b[19]!=0x6b10001fu
            ||b[24]!=0xaa1603e0u||b[25]!=0xaa1d03efu||b[26]!=0xa8c179fdu||b[27]!=0xd65f03c0u)continue;
        uint32_t first=0,second=0;
        if(!target(b[11],f.va+44,first)||!target(b[23],f.va+92,second)||first!=initializer||second!=initializer)continue;
        if(uoff(b[5])==p.overview||overlays.contains(uoff(b[5])))continue;
        normal=uoff(b[5]);++hits;
    }
    return hits==1;
}
// Every route reaching the observation site must have committed the state first.
// Same-state early returns and original throw paths may bypass the observation.
inline bool dominates(const Body& b,size_t store,size_t observe) {
    std::vector<std::array<bool,2>> seen(b.size());
    std::vector<std::pair<size_t,bool>> pending{{0,false}};bool reached=false;
    while(!pending.empty()){
        auto [at,committed]=pending.back();pending.pop_back();if(at>=b.size())return false;
        committed=committed||at==store;
        if(at==observe){if(!committed)return false;reached=true;continue;}
        if(seen[at][committed])continue;seen[at][committed]=true;
        const auto w=b[at];
        if((w&0xfffffc1fu)==0xd65f0000u||(w&0xffe0001fu)==0xd4200000u)continue;
        if((w&0xfffffc1fu)==0xd61f0000u)return false;
        int64_t dest=-1;bool branch=false,jump=false;
        if((w&0xfc000000u)==0x14000000u){dest=int64_t(at)+(int32_t(w<<6)>>6);branch=jump=true;}
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u){dest=int64_t(at)+(int32_t(w<<8)>>13);branch=true;}
        else if((w&0x7e000000u)==0x36000000u){dest=int64_t(at)+(int32_t(w<<13)>>18);branch=true;}
        if(branch){if(dest<0||dest>=int64_t(b.size()))return false;pending.emplace_back(size_t(dest),committed);}
        if(!jump)pending.emplace_back(at+1,committed);
    }
    return reached;
}
inline bool transition_writer(const Code& code,Plan& p,uint32_t predicate,uint32_t normal) {
    unsigned matches=0;
    for(const auto& f:code.functions()){
        const auto& b=f.words;int32_t receiver=0,arg=0;
        if(!receiver_slot(b,1,receiver)||!receiver_slot(b,2,arg)||receiver==arg||b.size()>1024)continue;
        bool parent=false,overview=false,default_state=false,below=false,old_compare=false;
        for(size_t i=0;i<b.size();++i){uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&to==predicate)parent=true;
            if(i+3<b.size()&&ldw(b[i],3,2)&&imm9(b[i])==p.state_field&&b[i+1]==0x8b1c8042u
                &&b[i+2]==0x6b00005fu&&(b[i+3]&0xff00001fu)==0x54000000u)old_compare=true;
            if(i+1<b.size()&&ux(b[i],26,0)&&uoff(b[i])==p.static_table&&ux(b[i+1],0,0)){
                overview|=uoff(b[i+1])==p.overview;default_state|=uoff(b[i+1])==normal;
            }
            if(i>=2&&mem(b[i],0xb8000000u,1,0)&&imm9(b[i])==p.below_field
                &&ldx(b[i-2],29,1)&&imm9(b[i-2])==receiver
                &&ldx(b[i-1],29,2)&&imm9(b[i-1])==arg)below=true;
        }
        if(!parent||!overview||!default_state||!below||!old_compare)continue;
        for(size_t i=2;i+10<b.size();++i){uint32_t barrier=0;
            if(!ldx(b[i-2],29,0)||imm9(b[i-2])!=arg||!ldx(b[i-1],29,1)||imm9(b[i-1])!=receiver
                ||!mem(b[i],0xb8000000u,1,0)||imm9(b[i])!=p.state_field
                ||b[i+1]!=0x385ff030u||b[i+2]!=0x385ff011u||b[i+3]!=0x8a500a30u||b[i+4]!=0xea5c821fu
                ||b[i+5]!=0x54000040u||!target(b[i+6],f.va+uint32_t((i+6)*4),barrier)
                ||!ux(b[i+7],26,0)||uoff(b[i+7])!=p.static_table||!ux(b[i+8],0,0)
                ||!ux(b[i+9],27,16)||uoff(b[i+9])!=p.sentinel_pool||b[i+10]!=0x6b10001fu
                ||!dominates(b,i,i+7))continue;
            p.transition=f.va;p.observe=f.va+uint32_t((i+7)*4);p.receiver_frame=receiver;++matches;
        }
    }
    return matches==1;
}
inline std::optional<Plan> resolve(const Code& code) {
    std::optional<Plan> result;
    for(const auto& f:code.functions()){
        Body b;if(!code.prefix(f.va,14,b)||frame_size(b)!=8||b[3]!=0xaa0103e0u||!stx(b[4],29,1)
            ||!frame_slot(b,imm9(b[4]))||!ldx(b[7],29,1)||imm9(b[7])!=imm9(b[4])
            ||(b[6]&0xfff8001fu)!=0x37200000u||(int32_t(b[6]<<13)>>18)!=4||b[9]!=0x14000002u||b[10]!=0x9100c2c0u
            ||b[11]!=0xaa1d03efu||b[12]!=0xa8c179fdu||b[13]!=0xd65f03c0u)continue;
        Plan p;p.wrapper=f.va;uint32_t getter=0,below=0,init=0,predicate=0;
        if(!target(b[5],f.va+20,getter)||!target(b[8],f.va+32,below)||!singleton_getter(code,getter,p,init))continue;
        Body next;if(!code.prefix(below,8,next)||next[0]!=0xa9bf79fdu||next[1]!=0xaa0f03fdu
            ||!ldw(next[2],1,2)||!field(imm9(next[2]))||imm9(next[2])==p.state_field
            ||next[3]!=0x8b1c8042u||!target(next[4],below+16,predicate)
            ||next[5]!=0xaa1d03efu||next[6]!=0xa8c179fdu||next[7]!=0xd65f03c0u)continue;
        p.below_field=imm9(next[2]);std::set<uint32_t> overlays;uint32_t normal=0;
        if(!overlay_predicate(code,predicate,p,init,overlays)||!window_initialization(code,p,init,overlays,normal)
            ||!transition_writer(code,p,predicate,normal))continue;
        if(result)return {};result=p;
    }
    return result;
}
}
