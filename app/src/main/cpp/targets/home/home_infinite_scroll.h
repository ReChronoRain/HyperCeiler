/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
#include <cmath>
#include <optional>
#include <set>
namespace home_layout::infinite_scroll {
using namespace home_dynamic;
using features::Code;
struct Plan {
    uint32_t entry=0,spring=0,caller=0,begin=0,drag=0,offset=0,context_allocator=0;
    int32_t metrics_frame=0,metrics_field=0,argument_metrics_frame=0,caller_metrics=0,pixels=0,minimum=0,maximum=0,viewport=0;
    int32_t drag_field=0,drag_owner=0,direction=0;
    uint32_t forward_pool=0,reverse_pool=0;
    std::vector<uint32_t> spring_returns,forward_returns,factory_returns;
};
// AOT embeds native-leaf bridge frames inside arithmetic owners. Their fixed
// AND-Dart-SP/MOV-native-SP ABI is not a new managed owner. Join contiguous
// inventory fragments through those bridges, stopping at the next managed
// prologue; no symbol-derived function length is used.
inline bool owner_body(const Code& code,uint32_t va,Body& out) {
    if(!code.body(va,out)||out.size()<2||out[0]!=0xa9bf79fdu||out[1]!=0xaa0f03fdu)return false;
    auto at=std::upper_bound(code.functions().begin(),code.functions().end(),va,
        [](uint32_t v,const auto& f){return v<f.va;});
    for(;at!=code.functions().end();++at) {
        if(at->va-va>0x4000||at->va!=va+out.size()*4)return false;
        const bool framed=at->words.size()>=2&&at->words[0]==0xa9bf79fdu&&at->words[1]==0xaa0f03fdu;
        const bool leaf=at->words.size()>=4&&at->words[2]==0x927cedefu&&at->words[3]==0x910001ffu;
        if(framed&&!leaf)return true;
        if(out.size()+at->words.size()>0x4000/4)return false;
        out.insert(out.end(),at->words.begin(),at->words.end());
    }return false;
}
inline bool pool_pair(const Body& b,size_t i,unsigned reg,uint32_t& out) {
    if(i+1>=b.size()||(b[i]&0xffc003ffu)!=(0x91400360u|reg)
        ||!ux(b[i+1],reg,reg))return false;
    out=((b[i]>>10)&4095u)*4096+uoff(b[i+1]);return out&&out<0x1000000;
}
// All opcodes/register roles here describe the ARM64 Dart ABI. No symbol,
// section name, saved RVA, CID, heap tag, field or pool displacement is identity.
inline std::optional<Plan> resolve(const Code& code) {
    struct Parent {uint32_t va,spring;int32_t pixels,min,max,viewport;};
    std::vector<Parent> parents;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        int32_t self=0,metrics=0;if(!receiver_slot(b,1,self)||!receiver_slot(b,2,metrics))continue;
        Parent p{};p.va=f.va;unsigned target_calls=0,constructors=0;
        // Page target -> saved D0 -> comparison with the SAME position's pixels.
        for(size_t i=6;i+6<b.size();++i) {
            uint32_t to=0;
            if(!target(b[i],f.va+uint32_t(i*4),to)||!ldx(b[i+1],29,2)||imm9(b[i+1])!=metrics
                ||!std_(b[i+2],29,0)||!frame_slot(b,imm9(b[i+2]))||!ldw(b[i+3],2,0)
                ||b[i+4]!=0x8b1c8000u||b[i+5]!=0x6b16001fu)continue;
            if(!ldx(b[i-6],29,0)||imm9(b[i-6])!=metrics||!ldx(b[i-5],29,1)
                ||imm9(b[i-5])!=self||b[i-4]!=0xaa0003e2u||b[i-3]!=0xaa0403e3u
                ||!ldd(b[i-2],29,0)||!stx(b[i-1],29,4))continue;
            p.pixels=imm9(b[i+3]);++target_calls;
        }
        if(target_calls!=1)continue;
        // The outward-velocity tests pair pixels with min and max respectively.
        std::vector<int32_t> boundaries;
        for(size_t i=0;i+8<b.size();++i)
            if(ldw(b[i],3,0)&&imm9(b[i])==p.pixels&&b[i+1]==0x8b1c8000u
                &&b[i+2]==0x6b16001fu&&ldw(b[i+4],3,1)&&field(imm9(b[i+4]))
                &&b[i+5]==0x8b1c8021u&&b[i+6]==0x6b16003fu)
                boundaries.push_back(imm9(b[i+4]));
        if(boundaries.size()!=2||boundaries[0]==boundaries[1])continue;
        p.min=boundaries[0];p.max=boundaries[1];
        for(size_t i=4;i<b.size();++i) {uint32_t ctor=0;
            if(!target(b[i],f.va+uint32_t(i*4),ctor)||!ldd(b[i-4],29,1)||!ldd(b[i-3],29,2))continue;
            Body c;if(!code.body(ctor,c)||!frame_size(c))continue;
            // Original constructor permutes start/end/velocity, stores its end,
            // and passes start-end to the original managed spring solution.
            const std::array<uint32_t,6> perm{0x4ea11c3fu,0x4ea01c01u,0x4ebf1fe0u,0x4ea21c5fu,0x4ea11c22u,0x4ebf1fe1u};
            if(c.size()<10||!std::equal(perm.begin(),perm.end(),c.begin()+4))continue;
            unsigned solution=0;
            for(size_t j=0;j+4<c.size();++j)if(std_(c[j],0,0)&&field(imm9(c[j]))
                &&c[j+1]==0x1e603843u&&c[j+2]==0x4ea31c60u&&c[j+3]==0xaa1603e1u
                &&(c[j+4]&0xfc000000u)==0x94000000u)++solution;
            if(solution==1){p.spring=ctor;++constructors;}
        }
        if(constructors==1)parents.push_back(p);
    }
    std::optional<Plan> result;
    for(const auto& parent:parents)for(const auto& f:code.functions()) {
        const auto& b=f.words;int32_t metrics=0;
        if(!frame_size(b)||!receiver_slot(b,2,metrics))continue;
        std::vector<uint32_t> direct;unsigned parent_calls=0;
        for(size_t i=0;i<b.size();++i){uint32_t to=0;if(target(b[i],f.va+uint32_t(i*4),to)){
            direct.push_back(to);if(to==parent.va)++parent_calls;}}
        // Workspace's guarded factory has three fallbacks to page physics,
        // plus its own snap/single-page/fling helpers. Generic PageViews do not.
        if(parent_calls!=3)continue;
        Plan p;p.entry=f.va;p.spring=parent.spring;p.argument_metrics_frame=metrics;
        p.minimum=parent.min;p.maximum=parent.max;p.pixels=parent.pixels;
        // The factory also checks viewport dimension before choosing a fling.
        unsigned viewport=0;
        for(size_t i=0;i+5<b.size();++i)if(ldw(b[i],1,2)&&b[i+1]==0x8b1c8042u
            &&b[i+2]==0x6b16005fu&&ldd(b[i+4],2,1)&&imm9(b[i+4])==7
            &&b[i+5]==0x1e602020u&&field(imm9(b[i]))){p.viewport=imm9(b[i]);++viewport;}
        if(viewport!=1)continue;
        std::set<uint32_t> family{p.entry,parent.va};
        for(auto to:direct){Body h;if(owner_body(code,to,h))for(size_t i=0;i<h.size();++i){uint32_t ctor=0;
            if(target(h[i],to+uint32_t(i*4),ctor)&&ctor==p.spring)family.insert(to);}}
        for(auto owner:family){Body h;if(!owner_body(code,owner,h))continue;
            for(size_t i=0;i<h.size();++i){uint32_t ctor=0;
                if(!target(h[i],owner+uint32_t(i*4),ctor))continue;
                const auto ret=owner+uint32_t((i+1)*4);
                if(ctor==p.spring)p.spring_returns.push_back(ret);
                if(owner==p.entry&&(ctor==p.spring||family.contains(ctor)))p.factory_returns.push_back(ret);}}
        if(p.spring_returns.size()<4||p.factory_returns.empty())continue;
        // The factory moves its metrics argument into an allocated Context,
        // then REUSES the argument's original stack slot for configuration.
        // Correlate the original Context producer with its physics consumers.
        int32_t self=0;if(!receiver_slot(b,1,self))continue;
        unsigned contexts=0;
        for(size_t i=5;i+1<b.size();++i){uint32_t allocator=0;
            if(!movz(b[i-5],1)||!target(b[i-4],f.va+uint32_t((i-4)*4),allocator)
                ||!ldx(b[i-3],29,1)||imm9(b[i-3])!=self||!stx(b[i-2],29,0)
                ||!frame_slot(b,imm9(b[i-2]))||imm9(b[i-2])==metrics
                ||!mem(b[i-1],0xb8000000u,0,1)||!field(imm9(b[i-1]))
                ||!ldx(b[i],29,2)||imm9(b[i])!=metrics
                ||!mem(b[i+1],0xb8000000u,0,2)||!field(imm9(b[i+1])))continue;
            Body allocation;
            if(!code.prefix(allocator,20,allocation)||!mem(allocation[18],0xb8000000u,0,1)
                ||!mem(allocation[19],0xb8000000u,0,22))continue;
            const auto context=imm9(b[i-2]),field=imm9(b[i+1]);
            const auto parent_field=imm9(allocation[19]);
            if(field<=parent_field||field>parent_field+int32_t(zimm(b[i-5])*4))continue;
            unsigned consumers=0;
            for(size_t j=i+2;j+7<b.size();++j){uint32_t to=0;
                if(ldx(b[j],29,0)&&imm9(b[j])==context&&ldw(b[j+1],0,2)&&imm9(b[j+1])==field
                    &&b[j+2]==0x8b1c8042u&&ldw(b[j+3],0,1)&&imm9(b[j+3])!=field&&b[j+4]==0x8b1c8021u
                    &&ldd(b[j+5],1,0)&&imm9(b[j+5])==7&&ldx(b[j+6],29,1)&&imm9(b[j+6])==self
                    &&target(b[j+7],f.va+uint32_t((j+7)*4),to)&&family.contains(to))++consumers;
            }
            if(consumers){p.metrics_frame=context;p.metrics_field=field;p.context_allocator=allocator;++contexts;}
        }
        if(contexts!=1)continue;
        // Correlate ScrollPosition's factory caller with its beginActivity,
        // real drag producer and the drag's back-reference to this position.
        unsigned callers=0;uint32_t dispatch_selector=0;
        for(const auto& g:code.functions()) {
            const auto& a=g.words;int32_t root=0;if(!receiver_slot(a,1,root))continue;
            for(size_t i=1;i+3<a.size();++i) {
                if(i<4||!movz(a[i-3],17)||a[i-2]!=0x8b11001eu||a[i-1]!=0xf87e7abeu
                    ||a[i]!=0xd63f03c0u||!std::count(a.begin(),a.end(),0xaa0303e2u)
                    ||!stx(a[i+1],29,0)||a[i+2]!=0x6b16001fu)continue;
                uint32_t begin=0;unsigned transitions=0;
                for(size_t j=i+3;j<a.size();++j) {uint32_t to=0;
                    if(j>=2&&ldx(a[j-2],29,1)&&imm9(a[j-2])==root&&ldx(a[j-1],29,2)
                        &&target(a[j],g.va+uint32_t(j*4),to)){begin=to;++transitions;}}
                Body begin_body;if(transitions!=1||!code.body(begin,begin_body))continue;
                for(const auto& d:code.functions()) {
                    const auto& db=d.words;int32_t owner=0;if(!receiver_slot(db,1,owner))continue;
                    unsigned matches=0;int32_t field=0,backref=0;
                    for(size_t j=3;j+5<db.size();++j){uint32_t to=0;
                        if(!target(db[j],d.va+uint32_t(j*4),to)||to!=begin
                            ||!ldx(db[j+1],29,0)||!ldx(db[j+2],29,1)||imm9(db[j+2])!=owner
                            ||!mem(db[j+3],0xb8000000u,1,0)||!home_dynamic::field(imm9(db[j+3])))continue;
                        const int32_t local=imm9(db[j+1]);
                        for(size_t k=0;k+2<j;++k)
                            if(ldx(db[k],29,2)&&imm9(db[k])==owner&&mem(db[k+1],0xb8000000u,1,2)
                                &&home_dynamic::field(imm9(db[k+1]))){backref=imm9(db[k+1]);}
                        if(backref&&frame_slot(db,local)){field=imm9(db[j+3]);++matches;}
                    }
                    unsigned clears=0;
                    for(auto word:begin_body)if(mem(word,0xb8000000u,2,22)&&imm9(word)==field)++clears;
                    if(matches==1&&clears==1){p.caller=g.va+uint32_t((i+1)*4);p.caller_metrics=root;p.begin=begin;p.drag=d.va;
                        p.drag_field=field;p.drag_owner=backref;dispatch_selector=zimm(a[i-3]);++callers;}
                }
            }
        }
        if(callers!=1||!dispatch_selector)continue;
        // The original parent-physics forwarder changes only the receiver.
        // Prove its SAME virtual selector and that metrics x2/velocity d0 pass
        // through untouched. Register/opcode shape is the ARM64 Dart ABI;
        // parent fields, dispatch selector and return PCs come from this image.
        for(const auto& g:code.functions()){
            const auto& a=g.words;
            if(a.size()!=20||a[0]!=0xa9bf79fdu||a[1]!=0xaa0f03fdu
                ||!ldw(a[2],1,0)||!field(imm9(a[2]))||a[3]!=0x8b1c8000u
                ||a[4]!=0x6b16001fu||a[5]!=0x54000061u||a[6]!=0xaa1603e0u||a[7]!=0x1400000au
                ||a[8]!=0xf85ff001u||a[9]!=0xd34c7c21u||a[10]!=0xaa0003f0u
                ||a[11]!=0xaa0103e0u||a[12]!=0xaa1003e1u||!movz(a[13],17)||zimm(a[13])!=dispatch_selector
                ||a[14]!=0x8b11001eu||a[15]!=0xf87e7abeu||a[16]!=0xd63f03c0u
                ||a[17]!=0xaa1d03efu||a[18]!=0xa8c179fdu||a[19]!=0xd65f03c0u)continue;
            p.forward_returns.push_back(g.va+17*4);
        }
        if(p.forward_returns.size()>8)continue;
        // Same beginActivity -> idle direction setter, and applyUserOffset's
        // sign-selected forward/reverse enum pool values -> that SAME setter.
        Body begin;if(!code.body(p.begin,begin))continue;unsigned directions=0;
        for(size_t i=0;i<begin.size();++i){uint32_t setter=0;
            if(!target(begin[i],p.begin+uint32_t(i*4),setter))continue;
            Body set;if(!code.body(setter,set))continue;
            int32_t direction=0;for(size_t j=0;j+3<set.size();++j)
                if(ldw(set[j],2,0)&&set[j+1]==0x8b1c8000u&&set[j+2]==0x6b01001fu)direction=imm9(set[j]);
            if(!direction)continue;
            unsigned direction_writes=0;
            for(auto word:set)if(mem(word,0xb8000000u,2,0)&&imm9(word)==direction)++direction_writes;
            if(direction_writes!=1)continue;
            for(const auto& o:code.functions()){const auto& ob=o.words;
                for(size_t j=8;j<ob.size();++j){uint32_t to=0,fp=0,rp=0;
                    if(!target(ob[j],o.va+uint32_t(j*4),to)||to!=setter||ob[j-1]!=0xaa0003e1u
                        ||!pool_pair(ob,j-6,2,fp)||!pool_pair(ob,j-3,2,rp)||fp==rp
                        ||ob[j-8]!=0x1e612000u)continue;
                    p.offset=o.va;p.direction=direction;p.forward_pool=fp;p.reverse_pool=rp;++directions;
                }}
        }
        if(directions!=1||result)return {};
        result=std::move(p);
    }
    return result;
}

enum class ReleaseReason {Caller,Stack,Root,Drag,Owner,Scalar,Bounds,Direction,Interior,Armed};
inline const char* release_reason(ReleaseReason reason){switch(reason){
    case ReleaseReason::Caller:return "unrelated caller";case ReleaseReason::Stack:return "invalid Dart frame";
    case ReleaseReason::Root:return "metrics root differs";case ReleaseReason::Drag:return "no live manual drag";
    case ReleaseReason::Owner:return "drag owner differs";case ReleaseReason::Scalar:return "invalid position metrics";
    case ReleaseReason::Bounds:return "position outside verified bounds";case ReleaseReason::Direction:return "no outward direction";
    case ReleaseReason::Interior:return "release inside page range";case ReleaseReason::Armed:return "edge release armed";
}return "unknown";}
// Scalar-only diagnostics, never retains a movable Dart object or stack pointer.
struct ReleaseTrace {ReleaseReason reason=ReleaseReason::Caller;uint32_t caller=0;double minimum=0,maximum=0,pixels=0,viewport=0,velocity=0;};
// Release state is scalar only. Keep the real displacement and velocity so
// the destination can settle with the ORIGINAL spring instead of a zero-length
// start=end simulation. Tuning fractions below are presentation policy, not ABI.
struct Pending {
    uintptr_t frame=0;int direction=0;
    double minimum=0,maximum=0,pixels=0,viewport=0,velocity=0;
    uintptr_t parent=0,return_pc=0;
};
template<class Read> bool position(uintptr_t object,uint64_t heap,uintptr_t null_value,const Plan& p,Read read,
    double& minimum,double& maximum,double& pixels,double& viewport,ReleaseTrace* trace=nullptr) {
    if(object<=4096||!(object&1))return false;
    auto ptr=[&](uintptr_t at,uintptr_t& out){uint32_t word=0;if(!read(at,word))return false;
        out=uint64_t(word)+(heap<<32);return out!=null_value&&out>4096&&(out&1);};
    uintptr_t drag=0,owner=0;if(trace)trace->reason=ReleaseReason::Drag;
    if(!ptr(object+p.drag_field,drag))return false;
    if(trace)trace->reason=ReleaseReason::Owner;
    if(!ptr(drag+p.drag_owner,owner)||owner!=object)return false;
    if(trace)trace->reason=ReleaseReason::Scalar;
    double* values[]={&minimum,&maximum,&pixels,&viewport};const int32_t fields[]={p.minimum,p.maximum,p.pixels,p.viewport};
    for(unsigned i=0;i<4;++i){uintptr_t box=0;if(!ptr(object+fields[i],box)||!read(box+7,*values[i])||!std::isfinite(*values[i]))return false;}
    if(trace){trace->minimum=minimum;trace->maximum=maximum;trace->pixels=pixels;trace->viewport=viewport;trace->reason=ReleaseReason::Bounds;}
    if(viewport<32||viewport>10000||minimum<0||maximum<=minimum||maximum>=1e7)return false;
    const double pages=(maximum-minimum)/viewport;
    return pages>=1&&pages<=128&&std::abs(pages-std::round(pages))<1e-5&&pixels>=minimum-viewport&&pixels<=maximum+viewport;
}
// Walk only instruction-verified transparent physics forwarders, then use
// the original goBallistic frame as the root. No arbitrary stack search.
template<class Registers,class Read> uintptr_t caller_frame(const Registers& r,const Plan& p,uintptr_t base,Read read){
    uintptr_t frame=r.x[29],pc=r.x[30];
    for(unsigned depth=0;depth<=8;++depth){
        if(pc==base+p.caller)return frame;
        if(depth==8)return 0;
        bool forwarded=false;for(auto at:p.forward_returns)forwarded|=pc==base+at;
        uintptr_t parent=0;
        if(!forwarded||frame<4096||(frame&7)||!read(frame,parent)||parent<=frame
            ||parent-frame>0x10000||!read(frame+8,pc))return 0;
        frame=parent;
    }return 0;
}
template<class Registers,class Read> bool enter(Registers& r,Pending& pending,const Plan& p,uintptr_t base,Read read,ReleaseTrace* trace=nullptr) {
    pending={};if(trace){*trace={};trace->velocity=r.d(0);trace->caller=r.x[30]>=base&&r.x[30]-base<=UINT32_MAX?uint32_t(r.x[30]-base):0;}
    const auto caller=caller_frame(r,p,base,read);if(!caller)return false;
    if(trace)trace->reason=ReleaseReason::Stack;
    if((caller&7)||r.x[15]<4096||(r.x[15]&7))return false;
    if(trace)trace->reason=ReleaseReason::Root;
    uintptr_t root=0;if(!read(caller+p.caller_metrics,root)||root!=r.x[2])return false;
    double lo=0,hi=0,x=0,width=0,v=r.d(0);
    if(!std::isfinite(v)||!position(root,r.x[28],r.x[22],p,read,lo,hi,x,width,trace))return false;
    if(trace)trace->reason=ReleaseReason::Direction;
    int outward=v<0?-1:v>0?1:0;
    if(!outward){uint32_t d=0;uintptr_t forward=0,reverse=0;
        if(!read(root+p.direction,d)||!read(r.x[27]+p.forward_pool,forward)||!read(r.x[27]+p.reverse_pool,reverse))return false;
        auto direction=uint64_t(d)+(r.x[28]<<32);outward=direction==forward?-1:direction==reverse?1:0;}
    if(trace)trace->reason=ReleaseReason::Interior;
    // Bouncing physics keeps the released position outside the edge until
    // its spring ticks. Accept that bounded overscroll on the outward side,
    // rather than requiring an exactly settled boundary before release.
    if((outward==-1&&x<=lo+.5)||(outward==1&&x>=hi-.5)){
        if(trace)trace->reason=ReleaseReason::Armed;
        pending={r.x[15]-16,outward,lo,hi,x,width,v};
        pending.parent=r.x[29];pending.return_pc=r.x[30];
        // Let the original factory take its inward non-null simulation path.
        // Its own edit/drag/overlay guards, allocations and GC PCs stay original.
        r.d(0,-outward*std::max(std::abs(v),1000.0));return true;
    }return false;
}
template<class Registers,class Read> bool finish(Registers& r,Pending& pending,const Plan& p,uintptr_t base,Read read) {
    if(!pending.frame)return false;
    bool caller=false;for(auto pc:p.spring_returns)caller|=r.x[30]==base+pc;if(!caller)return false;
    uintptr_t frame=r.x[29],pc=r.x[30];
    for(unsigned i=0;frame&&frame<pending.frame&&i<8;++i){uintptr_t next=0;
        if((frame&7)||!read(frame,next)||next<=frame||next>pending.frame||next-frame>0x10000
            ||!read(frame+8,pc))return false;
        frame=next;
    }
    if(frame!=pending.frame)return false;
    const auto take=pending;pending={};uintptr_t context=0;uint32_t compressed=0;double lo=0,hi=0,x=0,width=0;
    // A guarded factory may return without constructing a spring. Stack
    // addresses are reusable: matching only FP can later mistake PagePhysics
    // (or another helper) for the old factory and hijack an ordinary release.
    // Prove that THIS live frame is waiting at an instruction-verified factory
    // call, and still has the entry's original parent/return PC, before reading
    // any Context. These are native/code scalars, never Dart heap pointers.
    bool active=false;for(auto ret:p.factory_returns)active|=pc==base+ret;
    uintptr_t parent=0,return_pc=0;
    if(!active||!read(frame,parent)||!read(frame+8,return_pc)
        ||parent!=take.parent||return_pc!=take.return_pc)return false;
    if(!read(frame+p.metrics_frame,context)||context<=4096||!(context&1)||context==r.x[22]
        ||!read(context+p.metrics_field,compressed))return false;
    const auto metrics=(r.x[28]<<32)+uint64_t(compressed);
    if(!position(metrics,r.x[28],r.x[22],p,read,lo,hi,x,width)||lo!=take.minimum||hi!=take.maximum
        ||width!=take.viewport)return false;
    // Rebase onto the destination page with the release displacement intact,
    // then settle IN RANGE using the original spring and its damping. Keeping
    // both endpoints equal makes isDone(0) true and causes a visible hard snap.
    // The distance stays within one destination page; no long sweep across
    // intervening pages or negative coordinates that enter the assistant page.
    const double target=take.direction<0?hi:lo;
    const double edge=take.direction<0?lo:hi;
    const double distance=std::clamp(std::abs(take.pixels-edge),width*.05,width*.25);
    const double start=target+take.direction*distance;
    // Cap incoming speed by remaining distance so a fast fling cannot turn
    // the short settle into another immediate boundary hit. Zero stays zero.
    const double velocity=-take.direction*std::min(std::abs(take.velocity),distance*4.0);
    // Original constructor dataflow proves d0=start, d1=end, d2=velocity.
    // No Dart call/allocation is made from this native C frame.
    r.d(0,start);r.d(1,target);r.d(2,velocity);return true;
}
}
