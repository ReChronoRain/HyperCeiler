/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_gestures_os4.h"
#include "home_workspace_gesture.h"

namespace home_workspace_gesture {
// The native sequence is authorized by the actual workspace Down's successful
// scope path, correlated by the original PointerEvent timestamp (not a global flag).
struct ScopeContract {
    uintptr_t down_return=0, code_begin=0, code_end=0;
    int32_t event_frame=0, timestamp_field=0, duration_field=0;
    uint32_t original_selector=0;
};
inline bool wrapper(const home_dynamic::Body& b,uint32_t &selector,int32_t &field) {
    using namespace home_dynamic;
    if(b.size()<14||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu||b[2]!=0xf85ff020u
        ||b[3]!=0xd34c7c00u||!movz(b[4],17))return false;
    size_t i=5;uint32_t n=zimm(b[4]);
    if((b[i]&0xffe0001fu)==0xf2a00011u){n|=zimm(b[i++])<<16;}
    if(i+8!=b.size()||b[i]!=0x8b11001eu||b[i+1]!=0xf87e7abeu||b[i+2]!=0xd63f03c0u
        ||!ldw(b[i+3],0,1)||!home_dynamic::field(imm9(b[i+3]))||b[i+4]!=0x8b1c8021u
        ||b[i+5]!=0xaa0103e0u||b[i+6]!=0xaa1d03efu||b[i+7]!=0xa8c179fdu)return false;
    // Full all-symbol spans can include the final RET as a fifteenth instruction.
    selector=n;field=imm9(b[i+3]);return n>0&&n<0x200000;
}
inline bool scope_contract(const home_dynamic::Body& down,uint32_t dv,uint32_t policy,
    const home_dynamic::Body& stamp,const home_dynamic::Body& move,uint32_t mv,uint32_t velocity,
    ScopeContract& out) {
    using namespace home_dynamic;
    if(down.size()<8||down[0]!=0xa9bf79fdu||down[1]!=0xaa0f03fdu
        ||!stx(down[3],29,1)||!stx(down[4],29,2)||!slot(imm9(down[4])))return false;
    ScopeContract c;c.event_frame=imm9(down[4]);unsigned calls=0,time=0;
    for(size_t i=0;i<down.size();++i)if(call(down[i],dv+i*4,policy)){++calls;c.down_return=dv+(i+1)*4;}
    auto end=std::find(stamp.begin(),stamp.end(),0xd65f03c0u);if(end==stamp.end())return false;
    home_dynamic::Body prefix(stamp.begin(),end);
    if(!wrapper(prefix,c.original_selector,c.timestamp_field))return false;
    for(size_t i=1;i+7<move.size();++i) {
        if(move[i-1]!=0xd63f03c0u||!ldx(move[i],0,1)||!field(imm9(move[i]))
            ||move[i+1]!=0xd2807d00u||move[i+2]!=0x9ac00c22u
            ||!ldx(move[i+3],29,1)||!ldd(move[i+4],29,0)
            ||!call(move[i+5],mv+(i+5)*4,velocity))continue;
        ++time;c.duration_field=imm9(move[i]);
    }
    if(calls!=1||time!=1)return false;out=c;return true;
}
template<class Read> inline bool event_timestamp(uintptr_t event,uint64_t heap,uintptr_t table,
    const ScopeContract& c,Read read,int64_t& millis) {
    auto ptr=[](uintptr_t p){return p>4096&&(p&1u);};
    uint32_t compressed=0;uint64_t tag=0;if(!ptr(event))return false;
    // Original getter comes from this object's live Dart dispatch table. Decode,
    // never call it from C and never retain Dart heap pointers across callbacks.
    if(!read(event-1,tag))return false;
    const uint32_t cid=(tag>>12)&0xfffff;uintptr_t getter=0;
    if(!cid||!read(table+(uint64_t(cid)+c.original_selector)*8,getter)
        ||getter<c.code_begin||getter+12>c.code_end||(getter&3))return false;
    uint32_t a=0,b=0,d=0;
    if(!read(getter,a)||!read(getter+4,b)||!read(getter+8,d))return false;
    if(a==0xaa0103e0u&&b==0xd65f03c0u) { /* PointerEvent.original == this */ }
    else if(home_dynamic::ldw(a,1,0)&&home_dynamic::field(home_dynamic::imm9(a))
        &&b==0x8b1c8000u&&d==0xd65f03c0u) {
        if(!read(event+home_dynamic::imm9(a),compressed))return false;
        event=uint64_t(compressed)+(heap<<32);if(!ptr(event))return false;
    } else return false;
    if(!read(event+c.timestamp_field,compressed))return false;
    const uintptr_t duration=uint64_t(compressed)+(heap<<32);int64_t micros=0;
    if(!ptr(duration)||!read(duration+c.duration_field,micros)||micros<=0)return false;
    millis=micros/1000;return millis>0;
}
template<class Read> inline bool timestamp(uintptr_t frame,uint64_t heap,uintptr_t table,
    const ScopeContract& c,Read read,int64_t& millis) {
    uintptr_t caller=0,ret=0,event=0;
    return c.down_return&&read(frame,caller)&&read(frame+8,ret)&&ret==c.down_return&&caller
        &&read(caller+c.event_frame,event)&&event_timestamp(event,heap,table,c,read,millis);
}
struct StatusPolicy {
    std::array<uint32_t,3> sites{};int32_t event_frame=0,bool_field=0;
    unsigned value_register=0,base_register=0;
};
inline bool status_policy(const home_dynamic::Body& b,uint32_t va,uint32_t can,StatusPolicy& out) {
    using namespace home_dynamic;
    if(b.size()<32||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu||!stx(b[6],29,2)||!slot(imm9(b[6])))return false;
    StatusPolicy c;c.event_frame=imm9(b[6]);std::array<unsigned,3> counts{};unsigned calls=0;
    for(size_t i=0;i<b.size();++i)if(call(b[i],va+i*4,can))++calls;
    for(size_t i=2;i+3<b.size();++i) {
        if(b[i+1]!=0xaa1d03efu||b[i+2]!=0xa8c179fdu||b[i+3]!=0xd65f03c0u)continue;
        if(b[i]==0x910082c0u){++counts[0];c.sites[0]=va+i*4;}
        else if((b[i]&0xffe0ffffu)==0xaa0003e0u) {
            unsigned reg=(b[i]>>16)&31;
            if(reg>=15||(b[i-2]&0xffffffe0u)!=(0x910082c0u&0xffffffe0u)||rd(b[i-2])!=reg
                ||!mem(b[i-1],0xb8000000u,4,reg)||!field(imm9(b[i-1])))continue;
            ++counts[1];c.sites[1]=va+i*4;c.value_register=reg;c.bool_field=imm9(b[i-1]);
        } else if(b[i]==0x8b1c8000u&&ldw(b[i-1],1,0)&&field(imm9(b[i-1]))) {
            ++counts[2];c.sites[2]=va+(i-1)*4;c.base_register=1;
            if(c.bool_field&&c.bool_field!=imm9(b[i-1]))return false;c.bool_field=imm9(b[i-1]);
        }
    }
    if(calls!=2||counts[0]>1||counts[1]!=1||counts[2]!=1)return false;out=c;return true;
}

struct SwipeSample {
    int action=0,count=0;std::array<int,2> id{};std::array<float,2> x{},y{};
    int64_t time=0;uintptr_t owner=0;
};
class SwipeRecognizer {
    bool active_=false,allowed_=false,rejected_=false,cancelled_=false,lifted_=false;
    int fingers_=1,claim_=0;int64_t start_=0;uintptr_t owner_=0;uint64_t revision_=0;
    unsigned mask_=0;std::array<int,2> ids_{};std::array<float,2> x_{},y_{};
    float span_=0,dx_=0,dy_=0;
public:
    void reset(){*this={};}
    bool tracking()const{return active_;}
    bool authorize(int64_t time,uint64_t revision) {
        if(!active_||rejected_||time!=start_||revision!=revision_)return false;
        allowed_=true;return true;
    }
    home_gestures::Decision accept(const SwipeSample& e,const home_gestures::Settings& s,bool editing) {
        using namespace home_gestures;
        if(e.action==0) {
            reset();if(editing||e.count!=1||e.time<=0||!e.owner||!std::isfinite(e.x[0])||!std::isfinite(e.y[0])
                ||!(s.mask&30u)||!std::isfinite(s.density)||s.density<.5f||s.density>8)return {};
            active_=true;start_=e.time;owner_=e.owner;revision_=s.revision;mask_=s.mask;ids_=e.id;x_=e.x;y_=e.y;return {};
        }
        if(!active_)return {};
        if(e.owner!=owner_){reset();return {};}
        auto finish=[&](bool ending){Decision d{false,cancelled_,0,revision_};if(ending)reset();return d;};
        if(editing||s.revision!=revision_||e.time<start_||e.time-start_>1500)rejected_=true;
        if(e.action==3)return finish(true); // a real cancel NEVER dispatches
        if(e.count<1||e.count>2)rejected_=true;
        for(int i=0;i<std::min(e.count,2);++i)if(!std::isfinite(e.x[i])||!std::isfinite(e.y[i]))rejected_=true;
        if(e.action==5&&!rejected_) {
            if(cancelled_||e.count!=2||fingers_!=1||std::hypot(dx_,dy_)>12*s.density||e.time-start_>250)rejected_=true;
            else {fingers_=2;ids_=e.id;x_=e.x;y_=e.y;span_=std::hypot(x_[1]-x_[0],y_[1]-y_[0]);dx_=dy_=0;}
        }
        if(!rejected_&&!lifted_&&(e.action==2||e.action==6||e.action==1)) {
            if(e.count!=fingers_)rejected_=true;
            else {
                std::array<float,2> dx{},dy{};
                for(int i=0;i<fingers_;++i){int j=0;while(j<e.count&&e.id[j]!=ids_[i])++j;
                    if(j==e.count){rejected_=true;break;}dx[i]=e.x[j]-x_[i];dy[i]=e.y[j]-y_[i];}
                dx_=dx[0];dy_=dy[0];
                if(fingers_==2){dx_=(dx[0]+dx[1])*.5f;dy_=(dy[0]+dy[1])*.5f;
                    float span=std::hypot(e.x[1]-e.x[0],e.y[1]-e.y[0]);
                    if(std::abs(span-span_)>std::max(20*s.density,span_*.18f)
                        ||std::abs(dy[0]-dy[1])>24*s.density||std::abs(dx[0]-dx[1])>24*s.density)rejected_=true;
                }
            }
        }
        if(e.action==6)lifted_=true;
        // Once a horizontal page drag owns the sequence, it cannot turn into a
        // vertical custom gesture halfway through the same pointer stream.
        if(!claim_&&std::abs(dx_)>16*s.density&&std::abs(dx_)>std::abs(dy_)*1.2f)rejected_=true;
        // Reserve the configured vertical direction BEFORE the stock status-bar
        // drag commits. Execution still requires full travel and final release.
        // Waiting for 64dp before CANCEL lets the stock panel open underneath it.
        if(!rejected_&&!claim_&&allowed_&&e.time-start_<=700&&std::abs(dy_)>=4*s.density
            &&std::abs(dy_)>std::abs(dx_)*1.8f) {
            int g=dy_<0?(fingers_==2?Up2:Up):(fingers_==2?Down2:Down);
            if(mask_&(1u<<(g-1)))claim_=g;
        }
        Decision d{false,cancelled_,0,revision_};
        if(claim_&&!rejected_){d.cancel=!cancelled_;d.consume=true;cancelled_=true;
            const bool same_direction=(claim_==Up||claim_==Up2)?dy_<0:dy_>0;
            if(e.action==1&&same_direction&&std::abs(dy_)>=64*s.density)d.gesture=claim_;}
        if(e.action==1)reset();return d;
    }
};
} // namespace home_workspace_gesture
void hc_workspace_scope_bind(const home_workspace_gesture::ScopeContract&);
void hc_workspace_status_bind(const home_workspace_gesture::StatusPolicy&);
