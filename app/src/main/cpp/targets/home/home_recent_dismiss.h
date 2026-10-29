/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
namespace home_layout::features::dismiss {
using namespace home_dynamic;
struct Plan {
    uint32_t plain=0,stack=0,scale_input=0,opacity_input=0;
    int32_t scale=0,translation=0,opacity=0,height_frame=0,offset_frame=0,scale_frame=0;
};
// Current ARM64 Dart code-generator/CFG contracts. Memory operands, direct
// callees, stack slots and patch addresses are decoded from this image. Local
// branch topology and register roles are explicitly constrained, not RVAs.
namespace shapes {
inline constexpr uint32_t plain[]={
    0xa9bf79fdu,0xaa0f03fdu,0xd100c1efu,0x6e231c63u,0x4ea01c05u,0x4ea11c24u,0xf81f83a1u,0xf81f03a2u,
    0xfc1e03a0u,0xfc1d83a1u,0xfc1d03a2u,0x1e641846u,0xfc1e83a6u,0x1e662060u,0x540000cdu,0x1e6140c0u,
    0x1e6e1001u,0x9400006cu,0x4ea01c02u,0x14000002u,0xfc5e83a2u,0xfc5e83a1u,0x6e201c00u,0x1e612000u,
    0x540003adu,0x1e602040u,0x54000061u,0x6e241c84u,0x14000006u,0x1e622000u,0x5400006du,0x1e614043u,
    0x4ea31c62u,0x4ea21c44u,0x1e6e1003u,0x1e6c1002u,0x1e621885u,0x1e653862u,0xa9460341u,0x91004021u,
    0xeb01001fu,0x540008c9u,0xf9003341u,0xd1003c21u,0xd29c2b80u,0xf2a00060u,0xf81ff020u,0xfc007022u,
    0xf94c7b62u,0xf9623763u,0x941c2f6eu,0xfc407000u,0x14000002u,0x1e6e1000u,0xf85f83a0u,0xfc5e83a2u,
    0xfc007000u,0xfc5d03a0u,0xfc5d83a1u,0x94000042u,0x4ea01c02u,0xfc5e83a1u,0x6e201c00u,0x1e612000u,
    0x5400016du,0xf85f83a0u,0xfc5e03a0u,0x1e601003u,0xfc407004u,0x1e640805u,0x1e653804u,0x1e631880u,
    0x1e602843u,0x4ea31c62u,0x1400000eu,0xf85f83a0u,0xf85f03a1u,0x372000c1u,0xfc5d03a0u,0x1e611002u,
    0x1e621803u,0x4ea31c60u,0x14000005u,0xfc5d03a0u,0x1e631002u,0x1e621803u,0x4ea31c60u,0x4ea01c02u,
    0x1e6e1000u,0xfc00f002u,0x1e602822u,0xa9460b41u,0x91004021u,0xeb01005fu,0x54000309u,0xf9003341u,
    0xd1003c21u,0xd29c2b82u,0xf2a00062u,0xf81ff022u,0xfc007022u,0xf94c7b62u,0xf9623763u,0x941c2f39u,
    0xfc407000u,0xf85f83a1u,0xfc017020u,0xaa1603e0u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0xadbf0de2u,
    0xadbf05e0u,0x94274ec4u,0xaa0003e1u,0xacc105e0u,0xacc10de2u,0x17ffffbau,0x3c9f0de2u,0xf81f8de0u,
    0x94274ebdu,0xaa0003e1u,0xf84085e0u,0x3cc105e2u,0x17ffffe8u,
};
inline constexpr uint32_t stack[]={
    0x6e231c63u,0x1e611844u,0x1e642060u,0x540000cdu,0x1e6c1005u,0x1e6e1001u,0x1e650886u,0x1e6128c5u,
    0x14000003u,0x1e6e1001u,0x1e6e1005u,0xfc007025u,0x1e650806u,0x1e642060u,0x5400010du,0x1e601003u,
    0x1e631804u,0x1e622880u,0x1e6318c4u,0x1e643803u,0x4ea31c60u,0x14000009u,0x372000a2u,0x1e611000u,
    0x1e601843u,0x4ea31c60u,0x14000004u,0x1e631000u,0x1e601843u,0x4ea31c60u,0xfc00f020u,0xfc017021u,
    0xaa1603e0u,0xd65f03c0u,
};
inline constexpr uint32_t choose[]={
    0xa9bf79fdu,0xaa0f03fdu,0xd100c1efu,0xaa0203e0u,0xf81f03a2u,0xaa0303e2u,0xf81f83a1u,0xf81e83a3u,
    0xfc1e03a0u,0xf9403f43u,0xf953b463u,0x6b16007fu,0x540001c1u,0xf9403f40u,0xf94e8000u,0xf9402370u,
    0x6b10001fu,0x54000061u,0xf97bf362u,0x94274c92u,0xf97bf770u,0xf90001f0u,0xf9402b64u,0x97e2baf8u,
    0xaa0003e1u,0x14000002u,0xaa0303e1u,0xf85f03a0u,0xb84e7022u,0x8b1c8042u,0xf8407001u,0xf100083fu,
    0x54000e0cu,0xf100043fu,0x5400084cu,0xf100003fu,0x5400078cu,0x37200102u,0xf85f83a1u,0xfc5e03a0u,
    0xf85e83a2u,0x9400009cu,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0xf85f83a0u,0xb840f001u,0x8b1c8021u,
    0x6b16003fu,0x54000e20u,0x97e392a2u,0xf95b1b70u,0x6b10001fu,0x54000361u,0xf9403f40u,0xf94e8000u,
    0xf9402370u,0x6b10001fu,0x54000061u,0xf97bf362u,0x94274c69u,0x91402b70u,0xf941de10u,0xf90001f0u,
    0xf9402b64u,0x97e2baceu,0xb844b001u,0x8b1c8021u,0xb8407020u,0x8b1c8000u,0xb840b001u,0x93417c20u,
    0xf100041fu,0x540000adu,0xfc5e03a0u,0x1e614001u,0x4ea11c20u,0x14000004u,0xfc5e03a0u,0x14000002u,
    0xfc5e03a0u,0xf85f83a0u,0xb840b001u,0x8b1c8021u,0x6b16003fu,0x540009c0u,0xf8417022u,0x36000062u,
    0x1e614001u,0x4ea11c20u,0xaa0003e1u,0xf85e83a2u,0x9400004bu,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
    0xf85f83a0u,0xfc5e03a0u,0xf85e83a2u,0x14000034u,0xf85f83a0u,0xfc5e03a0u,0x6e211c21u,0xb8427001u,
    0x8b1c8021u,0xf9402370u,0x6b10003fu,0x54000720u,0xfc407022u,0x1e621803u,0x1e632020u,0x540000cdu,
    0x1e6c1002u,0x1e6e1001u,0x1e620864u,0x1e612882u,0x14000003u,0x1e6e1001u,0x1e6e1002u,0xf85e83a2u,
    0xfc1d83a2u,0x941c7516u,0xf81f03a0u,0xf800701fu,0xfc5e03a0u,0xfc00f000u,0x9421e918u,0x1e6e1000u,
    0xfc00f000u,0xf85f03a1u,0xb8017001u,0xfc5d83a0u,0xfc01b000u,0xf802b01fu,0x91402361u,0xf940b021u,
    0xb8033001u,0x9100c2c1u,0xb803f001u,0xf85e83a2u,0xb800b002u,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,
    0xf85f83a0u,0xfc5e03a0u,0xf85e83a2u,0xf100103fu,0x5400010cu,0xf1000c3fu,0x540000ccu,0xaa0003e1u,
    0x9400002du,0xaa1d03efu,0xa8c179fdu,0xd65f03c0u,0xaa0003e1u,0xf9403b64u,0x97ffff32u,0xaa1d03efu,
    0xa8c179fdu,0xd65f03c0u,0x94275595u,0x942755a7u,0x9143ab69u,0xf940fd29u,0x94275647u,
};
inline constexpr uint32_t appearance[]={
    0xa9bf79fdu,0xaa0f03fdu,0xd10121efu,0x1e6e1002u,0x91413771u,0xfd455e21u,0xaa0103e4u,0xaa0203e0u,
    0xf81f83a1u,0xf81f03a2u,0xfc1c03a0u,0xb84a7081u,0x8b1c8021u,0xfc407023u,0xfc417024u,0xfc40f025u,
    0xfc1c83a5u,0xb8427081u,0x8b1c8021u,0xf9402370u,0x6b10003fu,0x54000ea0u,0xfc407026u,0x1e6108c7u,
    0xfc1d03a7u,0x1e603861u,0x1e631826u,0xfc1d83a6u,0x1e663841u,0x1e610882u,0xa9460b41u,0x91004021u,
    0xeb01005fu,0x54000d89u,0xf9003341u,0xd1003c21u,0xd29c2b82u,0xf2a00062u,0xf81ff022u,0xfc007022u,
    0xf94c7b62u,0xf9623763u,0x941c33a5u,0xf81e83a0u,0x941ce8a2u,0xd2800404u,0xf81e03a0u,0x942750efu,
    0xaa0003e1u,0xf85e03a0u,0xb8007001u,0xaa0003e1u,0x97dccf69u,0xfc5d03a0u,0xfc5d83a1u,0x1e610802u,
    0xfc5c83a0u,0x1e623803u,0x1e749000u,0x1e600822u,0xf85e03a1u,0x4ea31c61u,0x6e201c00u,0x97dccc02u,
    0xfc5c03a0u,0xa9460343u,0x91004063u,0xeb03001fu,0x54000a89u,0xf9003343u,0xd1003c63u,0xd29c2b80u,
    0xf2a00060u,0xf81ff060u,0xfc007060u,0xf85e03a1u,0xaa0303e2u,0x97dcd100u,0xf9403f40u,0xf953b400u,
    0x6b16001fu,0x54000181u,0xf9403f40u,0xf94e8000u,0xf9402370u,0x6b10001fu,0x54000061u,0xf97bf362u,
    0x94274b88u,0xf97bf770u,0xf90001f0u,0xf9402b64u,0x97e2b9eeu,0xb84e7001u,0x8b1c8021u,0x37200261u,
    0xf9403f40u,0xf94e8000u,0xf9402370u,0x6b10001fu,0x54000061u,0xf97bf362u,0x94274b7au,0x91402b70u,
    0xf941de10u,0xf90001f0u,0xf9402b64u,0x97e2b9dfu,0xb84ef001u,0x8b1c8021u,0x97f5c1c4u,0xd27c0001u,
    0xaa0103e2u,0x14000002u,0x9100c2c2u,0xf85e83a1u,0xf85e03a0u,0xfc407020u,0xf90001e2u,0xf85f83a1u,
    0xf85f03a2u,0x9141fb64u,0xf944f484u,0x94000022u,0xf81f83a0u,0x941e400fu,0xf85e03a1u,0xb800f001u,
    0x91402361u,0xf940b021u,0xb8017001u,0x910082c1u,0xb801b001u,0xf85f83a1u,0xb800b001u,0xaa1d03efu,
    0xa8c179fdu,0xd65f03c0u,0x9143ab69u,0xf940fd29u,0x9427559cu,0xadbf1de6u,0xadbf15e2u,0x3c9f0de0u,
    0xa9bf11e0u,0x942752d3u,0xaa0003e1u,0xa8c111e0u,0x3cc105e0u,0xacc115e2u,0xacc11de6u,0x17ffff90u,
    0x3c9f0de0u,0x942752cbu,0xaa0003e3u,0x3cc105e0u,0x17ffffaeu,
};
}
template<size_t N> bool matches(const Function& f,const uint32_t (&shape)[N]) {
    if(f.words.size()!=N)return false;
    for(size_t i=0;i<N;++i){
        const auto w=shape[i];
        // Branch offsets express this local CFG, so do not erase them.
        const uint32_t mask=(w&0xffc003ffu)==0xd10001efu?0xffc003ffu:
            (w&0xffc003e0u)==0x91400360u?0xffc003ffu:
            ((w&0xff800000u)==0xd2800000u||(w&0xff800000u)==0xf2800000u)?0xffe0001fu:
            (w&0xfc000000u)==0x94000000u?0xfc000000u:
            (w&0x3b200c00u)==0x38000000u?0xffe00fffu:
            (w&0x3b000000u)==0x39000000u?0xffc003ffu:0xffffffffu;
        if((f.words[i]&mask)!=(w&mask))return false;
        if((w&0x3b200c00u)==0x38000000u){
            const auto off=imm9(f.words[i]);
            if(rn(w)==29&&!frame_slot(f.words,off))return false;
        }
    }
    return true;
}
inline uint32_t call(const Function& f,size_t i){uint32_t to=0;return i<f.words.size()&&target(f.words[i],f.va+uint32_t(i*4),to)?to:0;}
inline std::optional<Plan> resolve(const auto& code){
    const Function *plain=nullptr,*stack=nullptr,*choose=nullptr,*appearance=nullptr;
    for(const auto& f:code.functions()){
        if(matches(f,shapes::appearance)){if(appearance)return {};appearance=&f;}
        if(matches(f,shapes::plain)){if(plain)return {};plain=&f;}
        if(matches(f,shapes::stack)){if(stack)return {};stack=&f;}
        if(matches(f,shapes::choose)){if(choose)return {};choose=&f;}
    }
    if(!plain||!stack||!choose||!appearance)return {};
    const auto& a=plain->words;const auto& s=stack->words;const auto& h=appearance->words;
    Plan p;
    p.scale=imm9(s[11]);p.translation=imm9(s[30]);p.opacity=imm9(s[31]);
    p.height_frame=imm9(a[8]);p.offset_frame=imm9(a[10]);p.scale_frame=imm9(h[10]);
    if(!field(p.scale)||!field(p.translation)||!field(p.opacity)||p.translation!=p.scale+8||p.opacity!=p.translation+8
        ||p.scale!=imm9(a[68])||p.scale!=imm9(a[56])||p.translation!=imm9(a[89])||p.opacity!=imm9(a[106])
        ||imm9(a[54])!=imm9(a[65])||imm9(a[65])!=imm9(a[75])||imm9(a[75])!=imm9(a[105])
        ||p.height_frame!=imm9(a[66])||p.offset_frame!=imm9(a[57])||p.offset_frame!=imm9(a[78])
        ||!call(*plain,17)||call(*plain,17)!=call(*plain,59)||call(*plain,50)!=call(*plain,103)
        ||call(*choose,41)!=appearance->va||call(*choose,152)!=appearance->va||!call(*choose,92)
        ||p.scale_frame!=imm9(h[64])||call(*appearance,42)!=call(*plain,50)
        ||p.scale!=imm9(h[13])||p.opacity!=imm9(h[14])||p.translation!=imm9(h[15]))return {};
    // Both calculators are owned by the SAME task-card drag builder: it calls
    // the normal calculator twice and the guarded stack wrapper once. The
    // wrapper restores the same transform receiver and original offset.
    uint32_t wrapper=0,drag=0;
    for(const auto& f:code.functions()){
        if(f.words.size()<42||!frame_size(f.words)||call(f,37)!=stack->va)continue;
        const auto& b=f.words;
        if(!ldx(b[34],29,1)||!ldd(b[35],29,0)||!ldd(b[36],29,2)||b[38]!=0xaa1603e0u)continue;
        if(wrapper)return {};wrapper=f.va;
    }
    if(!wrapper)return {};
    for(const auto& f:code.functions()){
        unsigned normals=0,stacks=0;
        for(size_t i=0;i<f.words.size();++i){const auto to=call(f,i);normals+=to==plain->va;stacks+=to==wrapper;}
        if(normals==2&&stacks==1){
            bool scale=false,translation=false;
            for(size_t i=2;i<f.words.size();++i){const auto& b=f.words;
                if(!ldw(b[i-2],0,1)||b[i-1]!=0x8b1c8021u)continue;
                if(ldd(b[i],1,0)&&imm9(b[i])==p.scale)scale=true;
                if(ldd(b[i],1,1)&&imm9(b[i])==p.translation)translation=true;
            }
            if(!scale||!translation||call(f,99)!=call(*appearance,63)||call(f,116)!=call(*appearance,77))return {};
            if(drag)return {};drag=f.va;
        }
    }
    if(!drag)return {};
    // Locate replay windows from their actual stores/dispatcher, rather than
    // retaining a function-relative patch offset from either captured build.
    for(size_t i=1;i+4<a.size();++i){
        if(!std_(a[i],1,0)||imm9(a[i])!=p.opacity||a[i+1]!=0xaa1603e0u
            ||a[i+2]!=0xaa1d03efu||a[i+3]!=0xa8c179fdu||a[i+4]!=0xd65f03c0u)continue;
        if(p.plain)return {};p.plain=plain->va+uint32_t(i*4);
    }
    for(size_t i=0;i+3<s.size();++i){
        if(!std_(s[i],1,0)||imm9(s[i])!=p.translation||!std_(s[i+1],1,1)
            ||imm9(s[i+1])!=p.opacity||s[i+2]!=0xaa1603e0u||s[i+3]!=0xd65f03c0u)continue;
        if(p.stack)return {};p.stack=stack->va+uint32_t(i*4);
    }
    // The original helper computes translation/normalised progress FIRST.
    // Only its later scale-only local may be replaced, never the controller,
    // enum or a live progress value used by trajectory/interpolation.
    for(size_t i=0;i+4<h.size();++i){
        if(!ldd(h[i],29,0)||imm9(h[i])!=p.scale_frame||h[i+1]!=0xa9460343u
            ||h[i+2]!=0x91004063u||h[i+3]!=0xeb03001fu)continue;
        for(size_t j=i+1;j<h.size();++j)if(ldd(h[j],29,0)&&imm9(h[j])==p.scale_frame)return {};
        if(p.scale_input)return {};p.scale_input=appearance->va+uint32_t(i*4);
    }
    for(size_t i=1;i+5<h.size();++i){
        if(h[i]!=0xf90001e2u||!ldd(h[i-1],1,0)||!ldx(h[i+1],29,1)||!ldx(h[i+2],29,2)
            ||(h[i+3]&0xffc003ffu)!=0x91400364u||!ux(h[i+4],4,4)||!call(*appearance,i+5))continue;
        if(p.opacity_input)return {};p.opacity_input=appearance->va+uint32_t(i*4);
    }
    if(!p.plain||!p.stack||!p.scale_input||!p.opacity_input)return {};
    return p;
}
}
