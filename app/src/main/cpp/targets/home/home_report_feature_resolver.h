/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
namespace home_layout::features {
struct ReportFeature {uint32_t entry=0;std::vector<uint32_t> consumers;};
// The shared dialog eligibility predicate accepts an application list, checks
// its single-item arm, dispatches the item's type predicate and returns its
// inverted Dart bool. List/ref fields, pool roles and all callers are discovered.
// The two-bit bool representation and typed dispatch registers are Dart ABI.
inline std::optional<ReportFeature> report_feature(const Code& code) {
    using namespace home_dynamic;std::optional<ReportFeature> found;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;
        if(b.size()<35||!runtime_contract::entry(b)||b[2]!=0xd10041efu
            ||!ldw(b[3],2,0)||!field(imm9(b[3]))||b[4]!=0x93417c01u
            ||b[5]!=0xb4000061u||b[6]!=0xf100043fu||b[7]!=0x5400006du
            ||b[8]!=0x910082c1u||b[9]!=0x14000015u||b[10]!=0xaa0103e0u
            ||b[11]!=0xd2800001u||b[12]!=0xeb00003fu||b[13]!=0x540002a2u
            ||!ldw(b[14],2,0)||!field(imm9(b[14]))||b[15]!=0x8b1c8000u
            ||!ldw(b[16],0,1)||!field(imm9(b[16]))||b[17]!=0x8b1c8021u
            ||!ldw(b[18],1,0)||!field(imm9(b[18]))||b[19]!=0x8b1c8000u
            ||b[20]!=0xf85ff001u||b[21]!=0xd34c7c21u
            ||(b[22]&0xffc003ffu)!=0x91400370u||!ux(b[23],16,16)
            ||b[24]!=0xa90001f0u||b[25]!=0xaa0103e0u||b[26]!=0xaa0003feu
            ||b[27]!=0xf87e7abeu||b[28]!=0xd63f03c0u||b[29]!=0xaa0003e1u
            ||b[30]!=0xd27c0020u||!runtime_contract::epilogue(b,31)
            ||(b[34]&0xfc000000u)!=0x94000000u)continue;
        if(imm9(b[3])==imm9(b[14]))continue;
        ReportFeature p;p.entry=f.va;unsigned saved_dialog_flags=0;
        bool unknown=false;
        for(const auto& caller:code.functions())for(size_t i=0;i+1<caller.words.size();++i) {
            uint32_t to=0;const auto& v=caller.words;
            if(!target(v[i],caller.va+uint32_t(i*4),to)||to!=f.va)continue;
            bool consumed=false;
            if(stx(v[i+1],29,0)&&frame_slot(v,imm9(v[i+1]))) {
                const auto slot=imm9(v[i+1]);
                for(size_t j=i+2;j+1<v.size()&&j<i+100;++j)
                    if(ldx(v[j],29,1)&&imm9(v[j])==slot&&mem(v[j+1],0xb8000000u,0,1)
                        &&field(imm9(v[j+1]))) {consumed=true;++saved_dialog_flags;break;}
            } else if(v[i+1]==0xaa0003e3u&&i+3<v.size()&&ldx(v[i+2],29,0)) {
                consumed=(stx(v[i+3],29,3)&&frame_slot(v,imm9(v[i+3])))
                    ||(i+4<v.size()&&(v[i+3]&0xffe0001fu)==0x92800011u&&v[i+4]==0xf8316ba3u);
            }
            if(!consumed)unknown=true;
            p.consumers.push_back(caller.va+uint32_t((i+1)*4));
        }
        if(unknown||p.consumers.size()<3||!saved_dialog_flags)continue;
        if(found)return {};found=std::move(p);
    }
    return found;
}
} // namespace home_layout::features
