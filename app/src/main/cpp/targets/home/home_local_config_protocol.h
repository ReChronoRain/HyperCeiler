/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <array>
#include <cstdint>
#include <cstddef>
#include <vector>
#include "home_recents_haptic_protocol.h"
#include "home_recent_cards_filter.h"
#include "home_dock_config.h"

namespace home_local_config {
// V7 adds optional in-memory haptic waveforms. V6 adds the capsule gate to the V5 Dock flags, before unchanged HCL4. V4 added
// native-card acknowledgement flags; V2/3/4 decode without Dock availability.
constexpr uint32_t kMagic = 0x48435043;
constexpr uint32_t kAck = 0x48434143;
constexpr uint32_t kVersion = 7;
constexpr uint32_t kAckAccepted=1,kAckNativeCards=2;
constexpr uint32_t kHapticRequest = 0x48434852, kHapticAck = 0x48434841;
constexpr uint32_t kHapticPrepare=0x48434850,kHapticWave=0x48434857;
constexpr size_t kMaxHapticWave=8192;
constexpr size_t kProfileSize = 12;
constexpr size_t kDockSize = 40;
constexpr size_t kHeaderSize = 20;
constexpr size_t kMaxPayload = 256 * 1024;
inline uint32_t word(const uint8_t* p) {
    return uint32_t(p[0])<<24 | uint32_t(p[1])<<16 | uint32_t(p[2])<<8 | uint32_t(p[3]);
}
inline void put_word(uint8_t* p,uint32_t v) {
    for(int i=3;i>=0;--i){p[i]=static_cast<uint8_t>(v);v>>=8;}
}
inline bool read_profile(const uint8_t* p,size_t size,home_layout::haptic::Profile& result) {
    if(size<=kProfileSize)return false;
    const auto flags=word(p),type=word(p+4),strength=word(p+8);
    if(flags>3||type>3||strength<10||strength>100)return false;
    result={true,(flags&1)!=0,(flags&2)!=0,static_cast<int>(type),static_cast<int>(strength)};
    return true;
}
struct HapticWavePack {
    uint32_t profile=0;
    std::array<std::vector<uint8_t>,8> waves;
};
struct Snapshot {
    home_layout::haptic::Profile profile;
    home_layout::cards::Snapshot cards;
    home_dock::Config dock;
    std::shared_ptr<const HapticWavePack> haptic_waves;
    std::span<const uint8_t> layout;
};
inline bool read_snapshot(uint32_t version,std::span<const uint8_t> bytes,Snapshot& out) {
    Snapshot next;
    if((version<2||version>kVersion)||bytes.size()>kMaxPayload
        ||!read_profile(bytes.data(),bytes.size(),next.profile))return false;
    bytes=bytes.subspan(kProfileSize);
    if(version>=3) {
        if(bytes.size()<4)return false;
        const uint32_t count=word(bytes.data());bytes=bytes.subspan(4);
        if(count!=UINT32_MAX) {
            if(count>home_layout::cards::kMaxPackages)return false;
            auto selected=std::make_shared<home_layout::cards::Selection>();
            selected->packages.reserve(count);
            for(uint32_t i=0;i<count;++i) {
                if(bytes.size()<4)return false;
                const uint32_t size=word(bytes.data());bytes=bytes.subspan(4);
                if(size>home_layout::cards::kMaxPackageBytes||size>bytes.size())return false;
                std::string name(reinterpret_cast<const char*>(bytes.data()),size);
                if(!home_layout::cards::valid_package(name)
                    ||(!selected->packages.empty()&&selected->packages.back()>=name))return false;
                selected->packages.push_back(std::move(name));bytes=bytes.subspan(size);
            }
            next.cards=std::move(selected);
        }
    }
    if(version>=5) {
        if(bytes.size()<kDockSize)return false;
        const auto available=word(bytes.data());
        if(available>1)return false;
        if(!available) {
            for(size_t at=4;at<kDockSize;at+=4)if(word(bytes.data()+at))return false;
        } else {
            const auto flags=word(bytes.data()+4),mode=word(bytes.data()+8),height=word(bytes.data()+16),
                margin=word(bytes.data()+20),bottom=word(bytes.data()+24),radius=word(bytes.data()+28),
                night=word(bytes.data()+32),style=word(bytes.data()+36);
            if(flags>(version>=6?3u:1u)||(mode!=1&&mode!=3)||height>300||margin>150||bottom>150||radius>60||night>2||style>7)return false;
            next.dock={true,(flags&1)!=0,static_cast<int32_t>(mode),word(bytes.data()+12),static_cast<int32_t>(height),
                static_cast<int32_t>(margin),static_cast<int32_t>(bottom),static_cast<int32_t>(radius),
                static_cast<int32_t>(night),static_cast<int32_t>(style),version>=6,(flags&2)!=0};
        }
        bytes=bytes.subspan(kDockSize);
    }
    if(version>=7) {
        if(bytes.size()<4)return false;
        const auto count=word(bytes.data());
        bytes=bytes.subspan(4);
        if(count) {
            const unsigned expected=next.profile.enabled?8:next.profile.close?1:0;
            if(count!=expected)return false;
            auto pack=std::make_shared<HapticWavePack>();
            pack->profile=next.profile.encoded();
            for(unsigned i=0;i<count;++i) {
                if(bytes.size()<4)return false;
                const auto size=word(bytes.data());
                bytes=bytes.subspan(4);
                if(!size||size>kMaxHapticWave||(size&3)||size>bytes.size())return false;
                pack->waves[i].assign(bytes.begin(),bytes.begin()+size);
                bytes=bytes.subspan(size);
            }
            next.haptic_waves=std::move(pack);
        }
    }
    if(bytes.empty())return false;
    next.layout=bytes;out=std::move(next);return true;
}
class Decoder {
    std::vector<uint8_t> bytes_;
    uint64_t sequence_ = 0;
public:
    void reset(){bytes_.clear();sequence_=0;}
    template<class Publish> bool feed(const uint8_t* data,size_t size,Publish publish) {
        while(size) {
            size_t wanted=kHeaderSize;
            if(bytes_.size()>=kHeaderSize)wanted+=word(bytes_.data()+8);
            const size_t take=size<wanted-bytes_.size()?size:wanted-bytes_.size();
            bytes_.insert(bytes_.end(),data,data+take);data+=take;size-=take;
            if(bytes_.size()<kHeaderSize)continue;
            const auto* p=bytes_.data();
            const uint32_t length=word(p+8);
            const uint64_t sequence=(uint64_t(word(p+12))<<32)|word(p+16);
            if(word(p)!=kMagic||word(p+4)<2||word(p+4)>kVersion||length==0||length>kMaxPayload
                ||sequence==0||sequence<=sequence_)return false;
            if(bytes_.size()!=kHeaderSize+length)continue;
            if(!publish(p+kHeaderSize,length,sequence,word(p+4)))return false;
            sequence_=sequence;bytes_.clear();
        }
        return true;
    }
};
}
