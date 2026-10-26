/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
namespace home_layout::features {
// Recheck after the engine has reverted its journal, including an off -> on
// transition. Discovery once must never authorize overwriting a later patch.
template<class Read> inline bool clear_patch_preimages_match(
    std::span<const std::pair<uint32_t,uint32_t>> originals,Read read) {
    if(originals.empty())return false;
    for(const auto& [va,original]:originals) {
        uint32_t current=0;if(!read(va,current)||current!=original)return false;
    }
    return true;
}
struct ClearVisibilityFeature {
    uint32_t sync=0,init=0,gate=0,gate_target=0,visibility=0,hidden_target=0,initial_call=0,reverse=0;
    int32_t previous=0,visible=0,controller=0;
};
// Fixed register roles/prologue, compressed reference width, X22 true/null and
// bit 4 are the verified Dart ARM64 ABI. Image addresses, fields, pool operands
// and branches below are outputs of the current executable, never saved RVAs.
inline std::optional<ClearVisibilityFeature> clear_visibility_feature(const Code& code) {
    using namespace home_dynamic;using runtime_contract::epilogue;
    std::optional<ClearVisibilityFeature> found;
    const auto branch=[](const Body& b,uint32_t va,size_t i,uint32_t& out){
        const auto w=b[i];int64_t at=-1;
        if((w&0xff000010u)==0x54000000u)at=int64_t(i)+(int32_t(w<<8)>>13);
        else if((w&0x7e000000u)==0x36000000u)at=int64_t(i)+(int32_t(w<<13)>>18);
        else if((w&0xfc000000u)==0x14000000u)at=int64_t(i)+(int32_t(w<<6)>>6);
        if(at<0||at>=int64_t(b.size()))return false;out=va+uint32_t(at*4);return true;
    };
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(b.size()<34||!runtime_contract::entry(b)||!ldw(b[2],1,0)||!field(imm9(b[2]))
            ||b[3]!=0x8b1c8000u||b[4]!=0x6b00005fu
            ||(b[5]&0xff00001fu)!=0x54000001u||b[6]!=0xaa1603e0u||!epilogue(b,7)
            ||!mem(b[10],0xb8000000u,1,2)||imm9(b[10])!=imm9(b[2])
            ||!mem(b[11],0xb8000000u,1,2)||!field(imm9(b[11]))
            ||(b[12]&0xfff8001fu)!=0x37200002u
            ||!ldw(b[13],1,0)||!field(imm9(b[13]))||b[14]!=0x8b1c8000u
            ||!ux(b[15],27,16)||b[16]!=0x6b10001fu||(b[17]&0xff00001fu)!=0x54000000u
            ||b[18]!=0xaa0003e1u||!ux(b[19],27,4)
            ||!ldw(b[22],1,0)||imm9(b[22])!=imm9(b[13])||b[23]!=b[14]
            ||b[24]!=b[15]||b[25]!=b[16]||(b[26]&0xff00001fu)!=0x54000000u
            ||b[27]!=b[18]||b[28]!=b[19]||b[30]!=0xaa1603e0u||!epilogue(b,31))continue;
        ClearVisibilityFeature p;p.sync=f.va;p.gate=f.va+20;p.visibility=f.va+48;
        p.previous=imm9(b[2]);p.visible=imm9(b[11]);p.controller=imm9(b[13]);
        uint32_t forward=0,end=0;Body fb,rb;
        if(p.previous==p.visible||p.previous==p.controller||p.visible==p.controller
            ||!branch(b,f.va,5,p.gate_target)||p.gate_target!=f.va+40
            ||!branch(b,f.va,12,p.hidden_target)||p.hidden_target!=f.va+88
            ||!branch(b,f.va,21,end)||end!=f.va+120
            ||!target(b[20],f.va+80,forward)||!target(b[29],f.va+116,p.reverse)||forward==p.reverse
            ||!code.body(forward,fb)||!runtime_contract::entry(fb)
            ||!code.body(p.reverse,rb)||!runtime_contract::entry(rb))continue;
        unsigned initializers=0;
        for(const auto& init:code.functions()) {
            const auto& v=init.words;if(!frame_size(v))continue;
            for(size_t i=0;i+10<v.size();++i){uint32_t to=0;
                if(!ldx(v[i],29,0)||!frame_slot(v,imm9(v[i]))||v[i+1]!=0x910082c1u
                    ||!mem(v[i+2],0xb8000000u,0,1)||imm9(v[i+2])!=p.visible
                    ||!ldw(v[i+3],0,1)||imm9(v[i+3])!=p.controller||v[i+4]!=0x8b1c8021u
                    ||v[i+5]!=b[19]||!target(v[i+6],init.va+uint32_t((i+6)*4),to)||to!=forward
                    ||v[i+7]!=0xaa1603e0u||!epilogue(v,i+8))continue;
                // The initializer actually publishes that controller field;
                // matching a call to a common animation API alone is insufficient.
                unsigned stores=0;
                for(size_t j=0;j<i;++j)if(mem(v[j],0xb8000000u,3,0)&&imm9(v[j])==p.controller)++stores;
                if(stores!=1)continue;
                p.init=init.va;p.initial_call=init.va+uint32_t((i+6)*4);++initializers;
            }
        }
        if(initializers!=1)continue;
        if(found)return {};found=p;
    }
    return found;
}
} // namespace home_layout::features

namespace home_layout::features {
struct ClearDisableFeature {
    uint32_t overlay=0,fold=0,empty_widget=0,dock_query=0;
    std::vector<uint32_t> calls;
};
// The fold builder's original empty-widget arm and the normal overlay insertion
// are selected by the SAME controller lookup and fold-mode boolean. That link,
// not a function name or an isolated instruction prefix, identifies both arms.
inline std::optional<ClearDisableFeature> clear_disable_feature(const Code& code) {
    using namespace home_dynamic;using runtime_contract::epilogue;
    const auto same_imm=[](uint32_t a,uint32_t b){return (a&0x003ffc00u)==(b&0x003ffc00u);};
    const auto callee=[&](const Body& b,uint32_t va,size_t i,uint32_t& target_va){return target(b[i],va+uint32_t(i*4),target_va);};
    const auto in_body_branch=[](uint32_t w,size_t i,size_t destination){
        if((w&0xff000010u)==0x54000000u)return int64_t(i)+(int32_t(w<<8)>>13)==int64_t(destination);
        if((w&0x7e000000u)==0x36000000u)return int64_t(i)+(int32_t(w<<13)>>18)==int64_t(destination);
        return false;
    };
    std::optional<ClearDisableFeature> found;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(b.size()<54||!runtime_contract::entry(b)||!frame_size(b)
            ||b[3]!=0xaa0103e0u||!stx(b[4],29,1)||!frame_slot(b,imm9(b[4]))
            ||b[5]!=0xaa0203e1u||!stx(b[6],29,2)||!frame_slot(b,imm9(b[6]))
            ||b[7]!=0xd2800041u||b[9]!=0xaa0003e1u
            ||!ldx(b[10],29,0)||imm9(b[10])!=imm9(b[4])
            ||!stx(b[11],29,1)||!frame_slot(b,imm9(b[11]))
            ||!mem(b[12],0xb8000000u,1,0)||!field(imm9(b[12]))
            ||!ldx(b[13],29,0)||imm9(b[13])!=imm9(b[6])
            ||!mem(b[14],0xb8000000u,1,0)||!field(imm9(b[14]))
            ||!ux(b[15],26,2)||!ux(b[16],2,2)||b[17]!=0x6b16005fu
            ||(b[18]&0xff00001fu)!=0x54000001u||!in_body_branch(b[18],18,31)
            ||!ux(b[19],26,0)||!ux(b[20],0,0)||!ux(b[21],27,16)||b[22]!=0x6b10001fu
            ||(b[23]&0xff00001fu)!=0x54000001u||!in_body_branch(b[23],23,26)
            ||!ux(b[24],27,2)||!ux(b[26],27,16)||b[27]!=0xf90001f0u||!ux(b[28],27,4)
            ||b[30]!=0x14000002u||b[31]!=0xaa0203e0u
            ||!ldw(b[32],0,1)||!field(imm9(b[32]))||b[33]!=0x8b1c8021u
            ||(b[34]&0xfff8001fu)!=0x36200001u||!in_body_branch(b[34],34,38)
            ||!ldx(b[35],29,1)||imm9(b[35])!=imm9(b[6])
            ||(b[37]&0xfff8001fu)!=0x36200000u||!in_body_branch(b[37],37,42)
            ||!ux(b[38],27,0)||!epilogue(b,39)
            ||!ldx(b[43],29,2)||imm9(b[43])!=imm9(b[11])
            ||(b[44]&0xffc003ffu)!=0x91400361u||!ux(b[45],1,1)
            ||!stx(b[46],29,0)||imm9(b[46])!=imm9(b[4])||b[48]!=0xaa0003e1u
            ||!ldx(b[49],29,0)||imm9(b[49])!=imm9(b[4])
            ||!mem(b[50],0xb8000000u,0,1)||!field(imm9(b[50]))||!epilogue(b,51))continue;
        uint32_t lookup=0,query=0,context=0,closure=0,widget=0,initialize=0;
        if(!callee(b,f.va,29,lookup)||!callee(b,f.va,36,query)||!callee(b,f.va,8,context)
            ||!callee(b,f.va,25,initialize)||!callee(b,f.va,42,widget)||!callee(b,f.va,47,closure)
            ||lookup==query||context==closure||widget==closure)continue;
        Body q;
        if(!code.body(query,q)||q.size()<74||!runtime_contract::entry(q)||!frame_size(q)
            ||!ux(q[3],26,0)||!ux(q[4],0,0)||!ux(q[5],27,16)||q[6]!=0x6b10001fu
            ||!stx(q[10],29,0)||!frame_slot(q,imm9(q[10]))
            ||!ldw(q[11],0,1)||!field(imm9(q[11]))||q[12]!=0x8b1c8021u
            ||(q[14]&0xfff8001fu)!=0x36200000u||!in_body_branch(q[14],14,19)
            ||q[15]!=0x9100c2c0u||!epilogue(q,16)
            ||!ux(q[19],26,0)||!same_imm(q[19],b[15])||!ux(q[20],0,0)||!same_imm(q[20],b[16])
            ||q[21]!=0x6b16001fu||!ux(q[23],26,0)||q[23]!=b[19]||q[24]!=b[20]
            ||q[25]!=b[21]||q[28]!=b[24]||q[30]!=b[26]||q[31]!=b[27]||q[32]!=b[28]
            ||q[70]!=0x910082c0u||!epilogue(q,71))continue;
        uint32_t qlookup=0,qinitialize=0;
        if(!callee(q,query,33,qlookup)||qlookup!=lookup||!callee(q,query,29,qinitialize)||qinitialize!=initialize)continue;
        ClearDisableFeature plan;plan.fold=f.va;plan.empty_widget=b[38];plan.dock_query=query;
        unsigned selector_count=0;
        for(const auto& c:code.functions()) {
            const auto& v=c.words;
            for(size_t i=26;i+4<v.size();++i) {
                if(!ux(v[i-26],26,0)||!same_imm(v[i-26],b[15])
                    ||!ux(v[i-25],0,0)||!same_imm(v[i-25],b[16])||v[i-24]!=0x6b16001fu
                    ||v[i-22]!=b[19]||v[i-21]!=b[20]||v[i-20]!=b[21]
                    ||v[i-17]!=b[24]||v[i-15]!=b[26]||v[i-14]!=b[27]||v[i-13]!=b[28]
                    ||!ldw(v[i-11],0,1)||imm9(v[i-11])!=imm9(b[32])||v[i-10]!=b[33]
                    ||(v[i-9]&0xfff8001fu)!=0x37200001u||!in_body_branch(v[i-9],i-9,i-3)
                    ||!ldx(v[i-3],29,0)||!frame_slot(v,imm9(v[i-3]))
                    ||!ldw(v[i-2],0,1)||!field(imm9(v[i-2]))||v[i-1]!=0x8b1c8021u
                    ||!ldx(v[i+1],29,0)||imm9(v[i+1])!=imm9(v[i-3])
                    ||v[i+2]!=v[i-2]||v[i+3]!=v[i-1])continue;
                uint32_t caller_lookup=0,insert=0;
                if(!callee(v,c.va,i-12,caller_lookup)||caller_lookup!=lookup||!callee(v,c.va,i,insert))continue;
                Body ov;
                if(!code.body(insert,ov)||ov.size()<40||!runtime_contract::entry(ov)||!frame_size(ov)
                    ||!stx(ov[3],29,1)||!frame_slot(ov,imm9(ov[3])))continue;
                unsigned tails=0;
                for(size_t t=26;t+3<ov.size();++t) {
                    if(ov[t]!=0xaa1603e0u||!epilogue(ov,t+1)
                        ||!ldx(ov[t-3],29,1)||imm9(ov[t-3])!=imm9(ov[3])
                        ||!ux(ov[t-2],26,2)||(ov[t-1]&0xffc003ffu)!=0xf9000041u
                        ||!ux(ov[t-5],27,4)||!ldx(ov[t-6],29,2)||!frame_slot(ov,imm9(ov[t-6]))
                        ||!ldx(ov[t-7],29,1)||!frame_slot(ov,imm9(ov[t-7]))
                        ||!mem(ov[t-8],0xb8000000u,3,0)||!field(imm9(ov[t-8]))
                        ||ov[t-9]!=0x9100c2c0u||!mem(ov[t-16],0xb8000000u,3,0)||!field(imm9(ov[t-16]))
                        ||!ldx(ov[t-17],29,3)||imm9(ov[t-17])!=imm9(ov[3])
                        ||!ldx(ov[t-18],29,0)||imm9(ov[t-18])!=imm9(ov[t-6]))continue;
                    uint32_t api=0;if(!callee(ov,insert,t-4,api)||!code.body(api,q)||!runtime_contract::entry(q))continue;
                    // The same overlay field was checked before insertion, and
                    // the process-wide owner published at the end was read too.
                    bool checked=false,owner_read=false;
                    for(size_t j=4;j+2<t-18;++j) {
                        checked|=ldw(ov[j],0,3)&&imm9(ov[j])==imm9(ov[t-16])
                            &&ov[j+1]==0x8b1c8063u&&ov[j+2]==0x6b16007fu;
                        owner_read|=ux(ov[j],26,1)&&same_imm(ov[j],ov[t-2])
                            &&ux(ov[j+1],1,1)&&same_imm(ov[j+1],ov[t-1]);
                    }
                    if(checked&&owner_read)++tails;
                }
                if(tails!=1)continue;
                if(plan.overlay&&plan.overlay!=insert)return {};
                plan.overlay=insert;++selector_count;
            }
        }
        if(selector_count!=1||!plan.overlay)continue;
        // Every direct call must discard X0 immediately. Never infer liveness
        // from a branch, a timeout or a fixed number of instructions.
        bool unsafe=false;
        for(const auto& c:code.functions())for(size_t i=0;i<c.words.size();++i) {
            uint32_t to=0;if(!callee(c.words,c.va,i,to)||to!=plan.overlay)continue;
            if(i+1>=c.words.size()||!ldx(c.words[i+1],29,0)||!frame_slot(c.words,imm9(c.words[i+1])))unsafe=true;
            plan.calls.push_back(c.va+uint32_t(i*4));
        }
        if(unsafe||plan.calls.empty()||plan.calls.size()>32)continue;
        // No branch or direct call may enter the four words to be replaced.
        for(const auto& c:code.functions())for(size_t i=0;i<c.words.size();++i) {
            const uint32_t w=c.words[i],pc=c.va+uint32_t(i*4);int64_t to=-1;
            if((w&0x7c000000u)==0x14000000u)to=int64_t(pc)+(int32_t(w<<6)>>6)*4ll;
            else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)to=int64_t(pc)+(int32_t(w<<8)>>13)*4ll;
            else if((w&0x7e000000u)==0x36000000u)to=int64_t(pc)+(int32_t(w<<13)>>18)*4ll;
            if(to>=int64_t(f.va)+8&&to<int64_t(f.va)+24)unsafe=true;
        }
        if(unsafe)continue;
        if(found)return {};found=std::move(plan);
    }
    return found;
}
} // namespace home_layout::features
