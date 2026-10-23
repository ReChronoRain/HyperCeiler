/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_runtime_layout_contracts.h"
#include <optional>

namespace home_layout::features {
struct AnimationRatioFeature { uint32_t source=0,flight=0,api=0; };
struct AnimationMagicFeature { uint32_t magic=0,high=0,low=0; };

// Discover the complete flight -> shared ratio -> typed Rust API chain in the
// current executable image. No function name, saved RVA or old BL displacement
// participates in admission. The existing data-flow contract is the final gate.
inline std::optional<AnimationRatioFeature> animation_ratio_feature(const Code& code) {
    using namespace home_dynamic;
    using namespace runtime_contract;
    std::optional<AnimationRatioFeature> result;
    for(const auto& function:code.functions()) {
        const auto& flight=function.words;
        unsigned direct=0;
        for(size_t i=0;i+3<flight.size();++i)
            if(flight[i]==0xfc407020u&&epilogue(flight,i+1))++direct;
        if(direct!=1||!entry(flight))continue;
        for(size_t i=0;i<flight.size();++i) {
            uint32_t source=0;Body shared;
            if(!target(flight[i],function.va+uint32_t(i*4),source)||!code.body(source,shared)
                ||!entry(shared))continue;
            for(size_t j=1;j+3<shared.size();++j) {
                uint32_t api=0;Body api_body;
                if(shared[j-1]!=0xaa0003e1u||!epilogue(shared,j+1)
                    ||!target(shared[j],source+uint32_t(j*4),api)
                    ||!code.body(api,api_body)||!ratio_api(api_body))continue;
                uint32_t resolved=0;
                if(!animation_ratio(flight,function.va,api,api_body,
                    [&](uint32_t va,Body& body){return code.body(va,body);},resolved)
                    ||resolved!=source)continue;
                // Multiple matching chains are not evidence of the intended
                // animation family, even when they happen to share a scalar.
                if(result)return std::nullopt;
                result=AnimationRatioFeature{source,function.va,api};
            }
        }
    }
    return result;
}

namespace animation_detail {
// Both original gear predicates read the same current settings object/field and
// call the same enum producer. Their Boolean results must reach Dart's canonical
// true/false return. Fields and pool operands may move together with the image.
inline bool gear_predicates(const home_dynamic::Body& high,uint32_t high_va,
    const home_dynamic::Body& low,uint32_t low_va) {
    using namespace home_dynamic;
    using namespace runtime_contract;
    if(high.size()<20||low.size()<20||!entry(high)||!entry(low))return false;
    for(size_t i=0;i<12;++i) {
        if(i==8||i==11)continue; // Resolved BL targets, never relative encodings.
        if(high[i]!=low[i])return false;
    }
    uint32_t high_init=0,low_init=0,high_source=0,low_source=0;
    if(!target(high[8],high_va+32,high_init)||!target(low[8],low_va+32,low_init)
        ||high_init!=low_init
        ||!target(high[11],high_va+44,high_source)||!target(low[11],low_va+44,low_source)
        ||high_source!=low_source||!ldw(high[9],0,1)||!field(imm9(high[9]))
        ||high[10]!=0x8b1c8021u)return false;
    // AOT's current no-frame canonical Boolean return ABI. High tests enum zero;
    // the low comparison is decoded from this image, rather than retaining its
    // old enum value (4). The magic consumer determines which predicate is low.
    if(high[12]!=0x34000060u||high[13]!=0x9100c2c1u||high[14]!=0x14000002u
        ||high[15]!=0x910082c1u||high[16]!=0xaa0103e0u||!epilogue(high,17))return false;
    if((low[12]&0xffc003ffu)!=0x7100001fu)return false;
    const uint32_t low_value=(low[12]>>10)&4095;
    if(!low_value||low_value>16||low[13]!=0x910082d0u||low[14]!=0x9100c2d1u
        ||low[15]!=0x9a910201u||low[16]!=0xaa0103e0u||!epilogue(low,17))return false;
    return true;
}
} // namespace animation_detail

inline std::optional<AnimationMagicFeature> animation_magic_feature(const Code& code) {
    using namespace home_dynamic;
    using namespace runtime_contract;
    std::optional<AnimationMagicFeature> result;
    for(const auto& function:code.functions()) {
        const auto& body=function.words;
        bool multiplier=false;
        for(size_t i=0;i+4<body.size();++i)
            if(body[i+1]==0x1e620820u&&epilogue(body,i+2))multiplier=true;
        if(!multiplier)continue;
        // The low gear call follows high by four instructions in the consumer's
        // verified Boolean diamond. Discover both destinations from actual BLs.
        for(size_t i=1;i+10<body.size();++i) {
            uint32_t high=0,low=0;
            if((body[i+1]&0xfff8001fu)!=0x37200000u||body[i+2]!=0x1e6e1002u
                ||!target(body[i],function.va+uint32_t(i*4),high)
                ||!target(body[i+4],function.va+uint32_t((i+4)*4),low)
                ||high==low||!animation_magic(body,function.va,high,low))continue;
            Body high_body,low_body;
            if(!code.body(high,high_body)||!code.body(low,low_body)
                ||!animation_detail::gear_predicates(high_body,high,low_body,low))continue;
            if(result)return std::nullopt;
            result=AnimationMagicFeature{function.va,high,low};
        }
    }
    return result;
}
} // namespace home_layout::features
