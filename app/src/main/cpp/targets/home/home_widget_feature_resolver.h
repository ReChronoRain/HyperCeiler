/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_widget_scale.h"
#include <optional>
#include <set>

namespace home_layout::features {
struct WidgetTransformFeature {
    uint32_t owner=0,bank=0,allocator=0,tag=0;
    int32_t scale=0,rotation=0,child=0;
};

// Identify the launcher's own HyperTransform allocation/layout as a witness.
// Normal widgets bypass this renderer; WidgetScaleFeature below must additionally
// prove the common creation/dispatch graph and select its actual closure at runtime.
inline std::optional<WidgetTransformFeature> widget_transform_feature(const Code& code) {
    using namespace home_dynamic;
    std::optional<WidgetTransformFeature> out;
    for(const auto& function:code.functions()) {
        const auto& b=function.words;
        if(!frame_size(b))continue;
        for(size_t i=1;i+10<b.size();++i) {
            // The default outer transform is not the inner model's scale.
            if(b[i]!=0x1e6e1000u||!std_(b[i+1],0,0)||!stx(b[i+2],0,31)
                ||(b[i+3]&0xff8003ffu)!=0x91000361u||!ux(b[i+4],1,1)
                ||!stw(b[i+5],0,1)||b[i+6]!=0x9100c2c1u||!stw(b[i+7],0,1)
                ||!ldx(b[i+8],29,1)||!frame_slot(b,imm9(b[i+8]))||!stw(b[i+9],0,1))continue;
            const int32_t scale=imm9(b[i+1]),rotation=imm9(b[i+2]),child=imm9(b[i+9]);
            uint32_t allocator=0;unsigned allocations=0;size_t fresh=0;
            for(size_t j=i;j>0&&i-j<12;--j) {
                uint32_t to=0;
                if(target(b[j-1],function.va+uint32_t((j-1)*4),to)){allocator=to;fresh=j-1;++allocations;break;}
            }
            Body stub;uint32_t tag=0;
            if(allocations!=1||!code.prefix(allocator,3,stub)||!home_dynamic::tag(stub,tag)
                ||(stub[2]&0xfc000000u)!=0x14000000u)continue;
            const uint32_t size=((tag>>8)&15)*16;
            const int32_t fields[]={scale,rotation,child,imm9(b[i+5]),imm9(b[i+7])};
            bool valid=size>=48&&size<=240;
            for(unsigned a=0;a<std::size(fields);++a) {
                const int bytes=a<2?8:4;
                valid&=field(fields[a])&&uint32_t(fields[a]+1+bytes)<=size;
                for(unsigned c=0;c<a;++c) {
                    const int other=c<2?8:4;
                    if(fields[a]<fields[c]+other&&fields[c]<fields[a]+bytes)valid=false;
                }
            }
            if(!valid)continue;
            // The first use of the same fresh class is the inner model's
            // transform, reading its scale from this frame instead of 1.0.
            // Its opacity/default offset and seven fields corroborate the
            // exact class; the outer child must come from a completed branch.
            unsigned inner=0;int32_t opacity=0;size_t inner_end=0;
            for(size_t j=0;j+18<fresh;++j) {
                uint32_t to=0;
                if(!target(b[j],function.va+uint32_t(j*4),to)||to!=allocator
                    ||b[j+1]!=0x1e6e1000u||!std_(b[j+2],0,0)
                    ||!ux(b[j+3],27,1)||!stw(b[j+4],0,1)
                    ||!ldd(b[j+5],29,1)||!frame_slot(b,imm9(b[j+5]))
                    ||!std_(b[j+6],0,1)||imm9(b[j+6])!=scale
                    ||!ldd(b[j+7],29,1)||!std_(b[j+8],0,1)||imm9(b[j+8])!=rotation)continue;
                if(!ldx(b[j+16],29,3)||!stw(b[j+17],0,3)||imm9(b[j+17])!=child)continue;
                if(!field(imm9(b[j+2]))||uint32_t(imm9(b[j+2])+9)>size)continue;
                opacity=imm9(b[j+2]);inner_end=j+17;++inner;
            }
            if(inner!=1||opacity==scale||opacity==rotation)continue;
            unsigned roots=0,opacities=0;
            const int32_t root=imm9(b[i+8]);
            for(size_t j=inner_end+1;j<fresh;++j)
                if(stx(b[j],29,5)&&imm9(b[j])==root&&frame_slot(b,root))++roots;
            for(size_t j=fresh+1;j+1<i;++j) {
                size_t store=j+1;
                if(stx(b[store],29,0)&&frame_slot(b,imm9(b[store])))++store;
                if(store<i&&ldd(b[j],29,0)&&frame_slot(b,imm9(b[j]))&&std_(b[store],0,0)
                    &&imm9(b[store])==opacity)++opacities;
            }
            if(roots!=1||opacities!=1)continue;
            // No incoming branch may bypass the original FMOV or enter the
            // four displaced instructions. All allocation calls remain before.
            const size_t first=i+1,last=i+4;
            bool incoming=false;
            for(size_t j=0;j<b.size();++j) {
                int64_t destination=-1;
                if((b[j]&0xfc000000u)==0x14000000u)destination=int64_t(j)+(int32_t(b[j]<<6)>>6);
                else if((b[j]&0xff000010u)==0x54000000u||(b[j]&0x7e000000u)==0x34000000u)
                    destination=int64_t(j)+(int32_t(b[j]<<8)>>13);
                else if((b[j]&0x7e000000u)==0x36000000u)destination=int64_t(j)+(int32_t(b[j]<<13)>>18);
                if(destination>=int64_t(first)&&destination<=int64_t(last))incoming=true;
            }
            const std::array<size_t,1> point{first};
            if(incoming||!passes_return_points(b,point))continue;
            if(out)return {};
            out=WidgetTransformFeature{function.va,function.va+uint32_t(first*4),allocator,tag,scale,rotation,child};
        }
    }
    return out;
}
} // namespace home_layout::features

namespace home_layout::features {
// Function/closure/context register roles below are the verified Dart ARM64
// AllocateClosure ABI. Offsets, PCs, nursery fields and class tags are decoded.
struct WidgetBuilderCandidate {
    uint32_t owner=0,return_pc=0,context_allocator=0,observer_allocator=0,closure_allocator=0;
    int32_t parent_frame=0,child=0,capture_widget=0,capture_context=0;
};
inline std::optional<WidgetBuilderCandidate> widget_observer_builder(const Function& f) {
    using namespace home_dynamic;const auto& b=f.words;
    if(b.size()!=25||!frame_size(b)||!stx(b[3],29,1)||!stx(b[4],29,2)||b[5]!=0xd2800041u
        ||b[7]!=0xaa0003e1u||!ldx(b[8],29,0)||imm9(b[8])!=imm9(b[3])
        ||!stx(b[9],29,1)||!stw(b[10],1,0)||!ldx(b[11],29,0)||imm9(b[11])!=imm9(b[4])
        ||!stw(b[12],1,0)||!ldx(b[14],29,2)||imm9(b[14])!=imm9(b[9])
        ||(b[15]&0xff8003ffu)!=0x91000361u||!ux(b[16],1,1)
        ||!stx(b[17],29,0)||b[19]!=0xaa0003e1u||!ldx(b[20],29,0)
        ||imm9(b[20])!=imm9(b[17])||!stw(b[21],0,1)
        ||b[22]!=0xaa1d03efu||b[23]!=0xa8c179fdu||b[24]!=0xd65f03c0u)return {};
    WidgetBuilderCandidate r;r.owner=f.va;r.return_pc=f.va+19*4;r.parent_frame=imm9(b[20]);
    r.child=imm9(b[21]);r.capture_widget=imm9(b[10]);r.capture_context=imm9(b[12]);
    if(!frame_slot(b,r.parent_frame)||!frame_slot(b,imm9(b[3]))||!frame_slot(b,imm9(b[4]))
        ||!frame_slot(b,imm9(b[9]))||imm9(b[3])==imm9(b[4])
        ||!field(r.child)||!field(r.capture_widget)||!field(r.capture_context)
        ||r.capture_widget==r.capture_context
        ||!target(b[6],f.va+24,r.context_allocator)
        ||!target(b[13],f.va+52,r.observer_allocator)
        ||!target(b[18],f.va+72,r.closure_allocator))return {};
    return r;
}
struct WidgetDispatchFeature {uint32_t owner=0,closure_allocator=0,observer_allocator=0,context_allocator=0;int32_t widget=0,context=0,closure_context=0;};
inline std::optional<WidgetDispatchFeature> widget_dispatch_feature(const Code& code,
    const std::vector<WidgetBuilderCandidate>& builders) {
    using namespace home_dynamic;std::optional<WidgetDispatchFeature> out;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(!frame_size(b)||b.size()<40||!ux(b[3],29,0)||uoff(b[3])!=16
            ||!ldw(b[4],0,1)||b[5]!=0x8b1c8021u||!stx(b[6],29,1)||!frame_slot(b,imm9(b[6])))continue;
        struct Switch {size_t at;uint32_t a,c,ca,cc,index;int32_t widget,context;};
        std::vector<Switch> switches;
        for(size_t i=5;i+18<b.size();++i) {
            uint32_t a=0,c=0;
            if(!ldx(b[i],1,0)||imm9(b[i])!=-1||b[i+1]!=0xd34c7c00u
                ||(b[i+2]&0xffe0001fu)!=0xd2800011u||b[i+3]!=0xeb11001fu||b[i+4]!=0x54000061u
                ||!target(b[i+5],f.va+uint32_t((i+5)*4),a)||b[i+6]!=0x1400000bu
                ||(b[i+7]&0xffe0001fu)!=0xd2800011u||b[i+8]!=0xeb11001fu||b[i+9]!=0x54000061u
                ||!target(b[i+10],f.va+uint32_t((i+10)*4),c)||b[i+11]!=0x14000006u
                ||b[i+12]!=b[i]||b[i+13]!=b[i+1]||(b[i+14]&0xffc003ffu)!=0x9100001eu
                ||b[i+15]!=0xf87e7abeu||b[i+16]!=0xd63f03c0u
                ||!ldx(b[i-5],29,0)||imm9(b[i-5])!=imm9(b[6])||!ldw(b[i-4],0,1)
                ||b[i-3]!=0x8b1c8021u||!ldw(b[i-2],0,2)||b[i-1]!=0x8b1c8042u)continue;
            switches.push_back({i,a,c,zimm(b[i+2]),zimm(b[i+7]),(b[i+14]>>10)&4095,imm9(b[i-4]),imm9(b[i-2])});
        }
        if(switches.size()!=2)continue;
        const auto& a=switches[0];const auto& c=switches[1];
        if(a.a!=c.a||a.c!=c.c||a.a==a.c||a.ca!=c.ca||a.cc!=c.cc||a.ca==a.cc||a.index!=c.index
            ||a.widget!=c.widget||a.context!=c.context)continue;
        const auto find=[&](uint32_t va)->const WidgetBuilderCandidate*{
            for(const auto& candidate:builders)if(candidate.owner==va)return &candidate;return nullptr;};
        const auto* one=find(a.a);
        if(!one||one->capture_widget!=a.widget||one->capture_context!=a.context)continue;
        // The second renderer captures the same receiver/context plus one
        // freshly boxed effect scalar. It is not the two-capture wrapper.
        Body other;uint32_t ctx=0;bool linked=false;
        if(!code.body(a.c,other)||other.size()<20||!frame_size(other)
            ||!stx(other[3],29,1)||!stx(other[4],29,2)||other[5]!=0xd2800061u
            ||!target(other[6],a.c+24,ctx)||ctx!=one->context_allocator
            ||other[7]!=0xaa0003e2u||!ldx(other[8],29,0)||imm9(other[8])!=imm9(other[3])
            ||!stx(other[9],29,2)||!stw(other[10],2,0)||imm9(other[10])!=a.widget
            ||!ldx(other[11],29,1)||imm9(other[11])!=imm9(other[4])
            ||!stw(other[12],2,1)||imm9(other[12])!=a.context)continue;
        for(size_t k=13;k+11<other.size();++k) {
            uint32_t alloc=0,cl=0;
            if(!target(other[k],a.c+uint32_t(k*4),alloc)||alloc!=one->observer_allocator
                ||!ldx(other[k+1],29,2)||imm9(other[k+1])!=imm9(other[9])
                ||!stx(other[k+4],29,0)||!target(other[k+5],a.c+uint32_t((k+5)*4),cl)
                ||cl!=one->closure_allocator||other[k+6]!=0xaa0003e1u
                ||!ldx(other[k+7],29,0)||imm9(other[k+7])!=imm9(other[k+4])
                ||!stw(other[k+8],0,1)||imm9(other[k+8])!=one->child
                ||other[k+9]!=0xaa1d03efu||other[k+10]!=0xa8c179fdu||other[k+11]!=0xd65f03c0u)continue;
            if(linked){linked=false;break;}linked=true;
        }
        if(!linked)continue;
        const size_t join=a.at+17,direct=c.at+17;
        uint32_t observer=0;
        if(join+14>=b.size()||!stx(b[join],29,0)||!frame_slot(b,imm9(b[join]))
            ||!target(b[join+1],f.va+uint32_t((join+1)*4),observer)
            ||b[join+2]!=0xaa0003e1u||!ldx(b[join+3],29,0)||imm9(b[join+3])!=imm9(b[join])
            ||!stw(b[join+4],1,0)||!field(imm9(b[join+4]))
            ||b[join+11]!=0xaa0103e0u||b[join+12]!=0xaa1d03efu||b[join+13]!=0xa8c179fdu||b[join+14]!=0xd65f03c0u
            ||direct+2>=b.size()||b[direct]!=0xaa1d03efu||b[direct+1]!=0xa8c179fdu||b[direct+2]!=0xd65f03c0u)continue;
        Body stub;if(!code.prefix(observer,3,stub)||!home_dynamic::tag(stub,observer))continue;
        const std::array<size_t,2> points{a.at,c.at};
        if(!passes_return_points(b,points))continue;
        if(out)return {};
        out=WidgetDispatchFeature{f.va,one->closure_allocator,one->observer_allocator,one->context_allocator,
            a.widget,a.context,imm9(b[4])};
    }
    return out;
}
struct WidgetScaleFeature {
    WidgetTransformFeature transform;
    WidgetDispatchFeature dispatch;
    std::vector<WidgetBuilderCandidate> builders;
    home_layout::WidgetScaleLayout layout;
    uint32_t tail=0;
    int32_t function_entry=0,closure_entry=0,closure_null=0;
};
inline std::optional<WidgetScaleFeature> widget_scale_feature(const Code& code) {
    using namespace home_dynamic;
    auto transform=widget_transform_feature(code);if(!transform)return {};
    std::vector<WidgetBuilderCandidate> candidates;
    for(const auto& f:code.functions())if(auto c=widget_observer_builder(f))candidates.push_back(*c);
    auto dispatch=widget_dispatch_feature(code,candidates);if(!dispatch)return {};
    WidgetScaleFeature out;out.transform=*transform;out.dispatch=*dispatch;
    for(const auto& c:candidates)if(c.context_allocator==dispatch->context_allocator&&c.observer_allocator==dispatch->observer_allocator
        &&c.closure_allocator==dispatch->closure_allocator&&c.capture_widget==dispatch->widget
        &&c.capture_context==dispatch->context)out.builders.push_back(c);
    if(out.builders.empty()||out.builders.size()>64)return {};
    Body closure;
    if(!code.prefix(dispatch->closure_allocator,19,closure)
        ||(closure[0]&0xffc07fffu)!=0xa9401340u||closure[1]!=0x9100c000u||closure[2]!=0xeb00009fu
        ||(closure[3]&0xff00001fu)!=0x54000009u||!ux(closure[4]^0x00400000u,26,0)
        ||closure[5]!=0xd100bc00u||closure[9]!=0xaa1603e4u
        ||!stw(closure[13],0,1)||!stw(closure[14],0,2)||imm9(closure[14])!=dispatch->closure_context
        ||!stw(closure[15],0,4)||!ldx(closure[16],1,4)||!stx(closure[17],0,4)||closure[18]!=0xd65f03c0u)return {};
    const uint32_t closure_tag=zimm(closure[6])|(zimm(closure[7])<<16);
    if((closure[6]&0xffe0001fu)!=0xd2800004u||(closure[7]&0xffe0001fu)!=0xf2a00004u
        ||!stx(closure[8],0,4)||imm9(closure[8])!=-1
        ||!stw(closure[10],0,4)||!stw(closure[11],0,4)||!stw(closure[12],0,4)
        ||((closure_tag>>8)&15)*16!=48)return {};
    const int32_t refs[]={imm9(closure[10]),imm9(closure[11]),imm9(closure[12]),
        imm9(closure[13]),imm9(closure[14]),imm9(closure[15])};
    for(unsigned i=0;i<std::size(refs);++i) {
        if(!field(refs[i])||refs[i]+5>48)return {};
        for(unsigned j=0;j<i;++j)if(std::abs(refs[i]-refs[j])<4)return {};
        if(refs[i]<imm9(closure[17])+8&&imm9(closure[17])<refs[i]+4)return {};
    }
    out.tail=dispatch->closure_allocator+60;out.function_entry=imm9(closure[16]);
    out.closure_entry=imm9(closure[17]);out.closure_null=imm9(closure[15]);
    if(!field(out.function_entry)||!field(out.closure_entry)||!field(out.closure_null))return {};
    Body construction;if(!code.body(transform->owner,construction))return {};
    // The default transform is an allocation/layout witness only. Normal
    // widgets bypass that renderer; their actual Observer closure is selected
    // at runtime from its Function entrypoint AND its validated caller PC.
    Body sample;if(!code.body(out.builders.front().owner,sample)
        ||!home_layout::widget_scale_layout(sample,construction,transform->owner,out.layout))return {};
    Body allocator;if(!code.prefix(out.layout.allocator,3,allocator)||!home_dynamic::tag(allocator,out.layout.tag)
        ||(allocator[2]&0xfc000000u)!=0x14000000u)return {};
    const int64_t common=int64_t(out.layout.allocator)+8+(int32_t(allocator[2]<<6)>>6)*int64_t{4};
    Body nursery;if(common<=0||common>UINT32_MAX||!code.prefix(uint32_t(common),7,nursery)
        ||nursery[0]!=0xd3482c44u||nursery[1]!=0xd37cec84u||(nursery[2]&0xffc07fffu)!=0xa9401740u
        ||nursery[3]!=0x8b040003u||nursery[4]!=0xeb0300bfu||!ux(nursery[6]^0x00400000u,26,3))return {};
    out.layout.size=((out.layout.tag>>8)&15)*16;out.layout.top=((nursery[2]>>15)&127)*8;
    if(uoff(nursery[6])!=out.layout.top||!out.layout.usable())return {};
    Body context;
    if(((closure[0]>>15)&127)*8!=out.layout.top||uoff(closure[4])!=out.layout.top
        ||!code.prefix(dispatch->context_allocator,20,context)
        ||context[0]!=0xd28003e2u||context[1]!=0x8b010842u||context[2]!=0x927cec42u
        ||!ux(context[3],26,0)||uoff(context[3])!=out.layout.top
        ||context[4]!=0x8b000043u||!ux(context[5],26,16)||uoff(context[5])!=out.layout.top+8
        ||context[6]!=0xeb10007fu||!ux(context[8]^0x00400000u,26,3)||uoff(context[8])!=out.layout.top
        ||context[9]!=0x91000400u||!stw(context[18],0,1)||!stw(context[19],0,22))return {};
    // Context capture N immediately follows its original count and parent.
    for(const auto& c:out.builders)
        if(c.capture_widget!=imm9(context[19])+4||c.capture_context!=c.capture_widget+4)return {};
    for(const auto& c:out.builders)if(c.child!=out.layout.child||c.parent_frame!=out.layout.frame)return {};
    return out;
}
} // namespace home_layout::features
