/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recents_haptic_resolver.h"
#include "home_recents_haptic_state.h"
#include "home_recent_render.h"

namespace home_layout::haptic {
struct Observation {bool valid=false,extent=false;unsigned lane=0;Event event=Event::Update;double value=0;bool programmatic=false;};
template<class Read> Observation observe(const recent_render::Registers& s,const Plan& p,unsigned action,Read read) {
    const auto object=[](uintptr_t v){return v>4096&&(v&7)==1;};
    const auto ref=[&](uintptr_t owner,int32_t field,uintptr_t& out){uint32_t low=0;
        if(!object(owner)||!read(owner+field,&low,4)||!(low&1)||low==uint32_t(s.x[22]))return false;
        out=uint64_t(low)+(s.x[28]<<32);return object(out);
    };
    if(action>=7)return {};
    if(action==4){
        const auto layout=s.x[3];uint32_t axis=0;uint64_t horizontal=0;double spacing=0;
        if(!object(layout)||!read(layout+p.axis,&axis,4)||!read(s.x[27]+p.horizontal_pool,&horizontal,8)
            ||!read(layout+p.spacing,&spacing,8))return {};
        return {true,true,0,Event::Update,(axis==uint32_t(horizontal)?s.d(1):s.d(0))+spacing};
    }
    if(action==5)return {true,true,2,Event::Update,s.d(1)+s.d(2)};
    if(action==6){double extent=0;if(!object(s.x[1])||!read(s.x[1]+p.stack_extent,&extent,8))return {};
        return {true,true,3,Event::Update,extent};}
    const auto notification=s.x[2];uint64_t tags=0;int64_t depth=0;
    if(!object(notification)||!read(notification-1,&tags,8)||!read(notification+p.depth,&depth,8)||depth)return {};
    const uint32_t cid=(tags>>12)&0xfffff;
    if(cid==p.end_cid)return {true,false,action,Event::End,0};
    if(cid!=p.start_cid&&cid!=p.update_cid)return {};
    // Programmatic initial positioning also dispatches ScrollStart. Only a
    // real drag starts a feedback session; fling updates then keep that session.
    if(cid==p.start_cid){uintptr_t details=0;
        if(!ref(notification,p.drag_details,details))return {true,false,action,Event::End,0,true};
    }
    uintptr_t metrics=0,pixels=0;double value=0;
    if(!ref(notification,p.metrics,metrics)||!ref(metrics,p.pixels,pixels)
        ||!read(pixels-1,&tags,8)||((tags>>12)&0xfffff)!=p.double_cid||!read(pixels+p.payload,&value,8))return {};
    return {true,false,action,cid==p.start_cid?Event::Start:Event::Update,value};
}
} // namespace home_layout::haptic
