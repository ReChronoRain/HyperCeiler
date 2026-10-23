/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_icon_render.h"
#include <algorithm>
#include <set>

namespace home_layout::features {
struct FolderFeatures {
    uint32_t small=0,base=0,current=0,preview_height=0,big=0,helpers[2]{},preview_builder=0;
    SmallFolderScaleSite small_site{};
    BigFolderScaleSite big_site{};
};

namespace folder_detail {
inline bool frame(const home_dynamic::Body& b) {
    return b.size()>=12&&b[0]==0xa9bf79fdu&&b[1]==0xaa0f03fdu;
}
inline bool tail(const home_dynamic::Body& b,size_t i) {
    return i+4<b.size()&&b[i+1]==0xaa1603e0u&&b[i+2]==0xaa1d03efu
        &&b[i+3]==0xa8c179fdu&&b[i+4]==0xd65f03c0u;
}
inline std::set<int32_t> stores(const home_dynamic::Body& b) {
    std::set<int32_t> out;
    for(uint32_t w:b)if((w&0xffe00c00u)==0xfc000000u
        &&(home_dynamic::rn(w)==1||home_dynamic::rn(w)==2))out.insert(home_dynamic::imm9(w));
    return out;
}
inline bool backgrounds(const home_dynamic::Body& b,const SmallFolderScaleSite& small) {
    using namespace home_dynamic;
    unsigned hits=0;
    for(size_t i=0;i+3<b.size();++i)if(std_(b[i],2,1)&&std_(b[i+1],2,0)
        &&std_(b[i+2],2,1)&&std_(b[i+3],2,0)){
        bool same=true;
        for(unsigned j=0;j<4;++j)same&=imm9(b[i+j])==small.dimensions[j];
        if(same)++hits;
    }
    return hits==1;
}
// The preview owner iterates cells, computes quotient/remainder positions from
// the strategy's lengths, allocates a four-double rectangle and appends it to
// its list before the back edge. Equal field offsets in unrelated renderers
// are not a consumer proof. All fields, allocator destinations and frame roles
// are decoded from this image; only arithmetic/register ABI shapes are fixed.
inline bool preview_owner(const Code& code,const Function& function,const std::set<int32_t>& lengths) {
    using namespace home_dynamic;
    const auto& b=function.words;
    int32_t receiver=0;
    // The current builder moves the receiver before the first call, then roots
    // it in the frame. receiver_slot checks the original frame bound as well.
    if(!receiver_slot(b,1,receiver))return false;
    std::set<int32_t> used;
    unsigned divs=0,remainders=0,multiplies=0,rectangles=0;
    size_t rectangle=0;bool loop=false;
    for(size_t i=0;i<b.size();++i) {
        const auto w=b[i];
        if((w&0xffe0fc00u)==0x9ac00c00u)++divs; // SDIV Xd,Xn,Xm
        if((w&0xffe08000u)==0x9b008000u)++remainders; // MSUB
        if((w&0xffe0fc00u)==0x1e600800u)++multiplies; // FMUL Dd,Dn,Dm
        if((w&0xffe00c00u)==0xfc400000u&&rn(w)!=29&&lengths.count(imm9(w))) {
            const unsigned base=rn(w);
            for(size_t j=i;j>0&&i-j<16;--j) {
                const auto previous=b[j-1];
                if(ldx(previous,29,base)) {
                    if(imm9(previous)==receiver)used.insert(imm9(w));
                    break;
                }
                // Stop when the base register is assigned by another load or
                // arithmetic operation, instead of borrowing an older root.
                const bool store=(previous&0x3b200c00u)==0x38000000u&&!(previous&(1u<<22));
                const bool floating=(previous&0x1e000000u)==0x1e000000u
                    ||((previous&0x3b000000u)==0x38000000u&&(previous&0x04000000u));
                if(rd(previous)==base&&!store&&!floating&&(previous&0xfc000000u)!=0x94000000u
                    &&(previous&0xff000010u)!=0x54000000u)break;
            }
        }
        uint32_t allocator=0;
        if(i+18<b.size()&&target(w,function.va+uint32_t(i*4),allocator)) {
            Body tag;
            if(!code.prefix(allocator,3,tag)||(tag[2]&0xfc000000u)!=0x14000000u)continue;
            uint32_t header=0;if(!home_dynamic::tag(tag,header))continue;
            const uint32_t size=((header>>8)&15)*16;
            std::set<int32_t> fields;unsigned pairs=0;
            for(size_t j=i+1;j+1<b.size()&&j<i+18;++j) {
                size_t store=j+1;
                if(stx(b[store],29,0)&&frame_slot(b,imm9(b[store])))++store;
                if(store<b.size()&&ldd(b[j],29,0)&&frame_slot(b,imm9(b[j]))&&std_(b[store],0,0)) {
                    const auto field=imm9(b[store]);
                    if(home_dynamic::field(field)&&uint32_t(field+9)<=size){fields.insert(field);++pairs;}
                }
            }
            if(pairs==4&&fields.size()==4){++rectangles;rectangle=i;}
        }
    }
    for(size_t i=rectangle+1;rectangles==1&&i<b.size();++i)
        if((b[i]&0xfc000000u)==0x14000000u) {
            const int64_t destination=int64_t(i)+(int32_t(b[i]<<6)>>6);
            if(destination>=3&&destination<int64_t(rectangle))loop=true;
        }
    return used==lengths&&divs>=2&&remainders>=2&&multiplies>=2&&rectangles==1&&loop;
}
}

// Find the actual producers from executable code only. The shared current-config
// root is itself identified by the icon producer/consumer graph. No symbol name,
// stored RVA, instruction offset or first-match fallback is used here. Register
// roles and the framed Dart call/return ABI are the same explicit ABI constraints
// checked by small_folder_scale_site / big_folder_scale_site.
inline bool resolve_folders(const Code& code,uint32_t current,FolderFeatures& out) {
    using namespace home_dynamic;
    using namespace folder_detail;
    if(!current)return false;
    FolderFeatures next;unsigned small_hits=0;
    for(const auto& function:code.functions()){
        const auto& b=function.words;
        if(!frame(b))continue;
        unsigned config_calls=0;
        for(size_t i=0;i+5<b.size();++i){uint32_t to=0;
            if(target(b[i],function.va+uint32_t(i*4),to)&&to==current
                &&ldw(b[i+1],0,1)&&b[i+2]==0x8b1c8021u&&ldd(b[i+3],1,0)
                &&ldx(b[i+4],29,1)&&std_(b[i+5],1,0))++config_calls;
        }
        if(config_calls!=3)continue;
        // Only the completed producer's base-padding tail call can be the bank.
        // The fourth dimension comes from its actual preview-height callee.
        for(size_t i=0;i+4<b.size();++i){uint32_t base=0;
            if(!tail(b,i)||!target(b[i],function.va+uint32_t(i*4),base))continue;
            Body base_body;if(!code.body(base,base_body))continue;
            for(size_t j=0;j+2<b.size();++j){uint32_t preview=0;
                if(!target(b[j],function.va+uint32_t(j*4),preview)||preview==current||preview==base
                    ||!ldx(b[j+1],29,1)||!std_(b[j+2],1,0))continue;
                SmallFolderScaleSite shape;
                if(!small_folder_scale_site(b,function.va,base_body,base,current,preview,shape))continue;
                if(++small_hits!=1)return false;
                next.small=function.va;next.base=base;next.current=current;
                next.preview_height=preview;next.small_site=shape;
            }
        }
    }
    if(small_hits!=1)return false;
    unsigned big_hits=0;
    for(const auto& function:code.functions()){
        const auto& producer=function.words;
        if(!frame(producer)||!backgrounds(producer,next.small_site))continue;
        unsigned base_calls=0;
        for(size_t i=0;i+4<producer.size();++i){uint32_t to=0;
            if(tail(producer,i)&&target(producer[i],function.va+uint32_t(i*4),to)&&to==next.base)++base_calls;
        }
        if(base_calls!=1)continue;
        // Helpers are reached from this producer, never guessed from adjacent
        // functions or equivalent names. Preserve their observed call order.
        std::vector<uint32_t> candidates;
        for(size_t i=0;i<producer.size();++i){uint32_t to=0;Body body;
            if(!target(producer[i],function.va+uint32_t(i*4),to)||to==next.base
                ||to==current||!code.body(to,body)||!frame(body)||stores(body).empty())continue;
            if(std::find(candidates.begin(),candidates.end(),to)==candidates.end())candidates.push_back(to);
        }
        for(size_t a=0;a<candidates.size();++a)for(size_t b=a+1;b<candidates.size();++b){
            const uint32_t helper_va[]={candidates[a],candidates[b]};
            std::array<Body,2> helpers;
            if(!code.body(helper_va[0],helpers[0])||!code.body(helper_va[1],helpers[1]))continue;
            auto fields=stores(producer);
            for(const auto& helper:helpers){const auto f=stores(helper);fields.insert(f.begin(),f.end());}
            if(fields.size()!=13)continue;
            for(int32_t common:next.small_site.dimensions)fields.erase(common);
            if(fields.size()!=9)continue;
            // Each of the nine non-background lengths must feed the same
            // owning preview builder. This rejects unrelated geometry helpers.
            for(const auto& preview:code.functions()){
                if(!frame(preview.words))continue;
                std::set<int32_t> consumed;
                for(uint32_t w:preview.words)if((w&0xffe00c00u)==0xfc400000u&&rn(w)!=29)
                    consumed.insert(imm9(w));
                if(!std::includes(consumed.begin(),consumed.end(),fields.begin(),fields.end()))continue;
                if(!preview_owner(code,preview,fields))continue;
                BigFolderScaleSite shape;
                if(!big_folder_scale_site(producer,function.va,helpers,helper_va,preview.words,
                    next.base,next.small_site,shape))continue;
                if(++big_hits!=1)return false;
                next.big=function.va;next.helpers[0]=helper_va[0];next.helpers[1]=helper_va[1];
                next.preview_builder=preview.va;next.big_site=shape;
            }
        }
    }
    if(big_hits!=1)return false;
    out=next;return true;
}
}
