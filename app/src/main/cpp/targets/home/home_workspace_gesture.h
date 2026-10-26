/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include "home_dynamic_layout.h"

// Source-body adapters, not function-entry guesses. All addresses, frame/pool/field
// operands and branch destinations come from this image. No launcher version table.
namespace home_workspace_gesture {
// A tear-off and its actual body may carry the SAME mini-ELF name. Admit this
// forwarding shape only when its unique BL is later proved to target that body;
// arbitrary duplicate framed functions are still ambiguous, never "largest wins".
inline bool tearoff(const home_dynamic::Body& b,uint32_t va,uint32_t& target) {
    using namespace home_dynamic;
    if(b.size()!=10||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu
        ||!ux(b[2],29,0)||!ldw(b[3],0,1)||!field(imm9(b[3]))||b[4]!=0x8b1c8021u
        ||!ux(b[5],29,2)||uoff(b[2])!=uoff(b[5])+8||uoff(b[5])<16
        ||b[7]!=0xaa1d03efu||b[8]!=0xa8c179fdu||b[9]!=0xd65f03c0u)return false;
    return home_dynamic::target(b[6],va+24,target)&&target!=va;
}
struct DoubleTapSites { uint32_t policy=0, policy_resume=0, rx=0, callback=0, resume=0; int32_t frame=0, field=0; };
inline bool call(uint32_t w,uint32_t pc,uint32_t dest) {
    uint32_t target=0;return home_dynamic::target(w,pc,target)&&target==dest;
}
inline bool resolve(const home_dynamic::Body& policy,uint32_t pv,
                    const home_dynamic::Body& down,uint32_t dv,
                    uint32_t elderly,uint32_t registered,uint32_t considered,uint32_t reset,uint32_t rx,
                    DoubleTapSites& out) {
    using namespace home_dynamic;
    if(!pv||!dv||!elderly||!registered||!considered||!reset||!rx||(pv&3)||(dv&3)
        ||policy.size()<16||down.size()<32
        ||uint64_t(pv)+policy.size()*4>UINT32_MAX||uint64_t(dv)+down.size()*4>UINT32_MAX)return false;
    unsigned elders=0,registers=0,considers=0,policy_calls=0;
    size_t first_register=policy.size(),elder=policy.size(),consider=down.size();
    for(size_t i=0;i<policy.size();++i) {
        if(call(policy[i],pv+i*4,elderly)){++elders;elder=i;}
        if(call(policy[i],pv+i*4,registered)){++registers;first_register=std::min(first_register,i);}
    }
    for(size_t i=0;i<down.size();++i) {
        if(call(down[i],dv+i*4,pv))++policy_calls;
        if(call(down[i],dv+i*4,considered)){++considers;consider=i;}
    }
    if(elders!=1||!registers||elder>=first_register||considers!=1||policy_calls!=1)return false;
    DoubleTapSites next;unsigned gates=0,callbacks=0;
    for(size_t i=4;i+6<policy.size();++i) {
        // Only the successful scope path reaches this final Rx preference read.
        // Earlier device/overlay/folder/edit guards remain completely untouched.
        if(policy[i-4]!=0x9100c2c0u||policy[i-3]!=0xaa1d03efu
            ||policy[i-2]!=0xa8c179fdu||policy[i-1]!=0xd65f03c0u
            ||!ldx(policy[i],29,0)||!slot(imm9(policy[i]))
            ||!ldw(policy[i+1],0,1)||!field(imm9(policy[i+1]))||policy[i+2]!=0x8b1c8021u
            ||!call(policy[i+3],pv+(i+3)*4,rx)||policy[i+4]!=0xaa1d03efu
            ||policy[i+5]!=0xa8c179fdu||policy[i+6]!=0xd65f03c0u)continue;
        ++gates;next.policy=pv+uint32_t(i*4);next.policy_resume=next.policy+16;
        next.rx=rx;next.frame=imm9(policy[i]);next.field=imm9(policy[i+1]);
    }
    for(size_t i=consider+1;i+8<down.size();++i) {
        // Original outgoing arguments and closure entry; the BLR stays at its
        // original PC on the stock route (Dart GC stack maps remain valid).
        if(down[i]!=0xa90009e0u||down[i+1]!=0xaa0203e0u||!ux(down[i+2],27,4)
            ||!ldx(down[i+3],0,2)||!field(imm9(down[i+3]))||down[i+4]!=0xd63f0040u
            ||!ldx(down[i+5],29,0)||!slot(imm9(down[i+5])))continue;
        bool cleanup=false;
        for(size_t j=i+5;j<std::min(down.size(),i+16);++j)
            if(call(down[j],dv+j*4,reset))cleanup=true;
        if(!cleanup)return false;
        ++callbacks;next.callback=dv+uint32_t(i*4);next.resume=dv+uint32_t((i+5)*4);
    }
    if(gates!=1||callbacks!=1)return false;
    out=next;return true;
}
// Named original time-to-velocity leaf: no calls, allocations, indirect branches
// or external jumps. All field and pool operands are decoded/validated, not OTA offsets.
inline bool velocity_leaf(const home_dynamic::Body& b) {
    using namespace home_dynamic;
    constexpr uint32_t shape[]={0xf8457023u,0xf8057022u,0xcb030044u,0xf100009fu,0x540000adu,0x9e620081u,0x1e611802u,0x4ea21c41u,0x14000002u,0x6e211c21u,0x6e201c00u,0xfc44f022u,0x1e602040u,0x540000a1u,0xfd74bf63u,0x1e602060u,0x540001adu,0x1400000au,0xfd74bf63u,0x1e622000u,0x540000adu,0x1e614040u,0x1e602060u,0x540000cdu,0x14000003u,0x1e622060u,0x5400006du,0xfc04f021u,0x1400000cu,0x91439771u,0xfd445e23u,0x1e6e1000u,0x9e620084u,0x1e632885u,0x1e651883u,0x1e633804u,0x1e620880u,0x1e610862u,0x1e622801u,0xfc04f021u,0xaa1603e0u,0xd65f03c0u};
    if(b.size()!=std::size(shape))return false;
    for(size_t i=0;i<b.size();++i) {
        uint32_t mask=operand_mask(shape[i]);
        if((shape[i]&0xfc000000u)==0x14000000u)mask=0xfc000000u;
        if((shape[i]&0xffc003ffu)==0x91400371u)mask=0xffc003ffu; // pool ADD X17,X27,imm,LSL12
        if((b[i]&mask)!=(shape[i]&mask))return false;
        int64_t branch=-1;
        if((b[i]&0xfc000000u)==0x14000000u)branch=int64_t(i)+(static_cast<int32_t>(b[i]<<6)>>6);
        else if((b[i]&0xff000010u)==0x54000000u)branch=int64_t(i)+(static_cast<int32_t>(b[i]<<8)>>13);
        if(branch!=-1&&(branch<0||branch>=int64_t(b.size())))return false;
    }
    return field(imm9(b[0]))&&imm9(b[0])==imm9(b[1])&&field(imm9(b[11]))
        &&imm9(b[11])==imm9(b[27])&&imm9(b[11])==imm9(b[39]);
}

}
