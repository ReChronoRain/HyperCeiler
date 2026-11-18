/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <bit>
#include <cmath>
#include <cstdint>
#include <limits>
#include <optional>

namespace home_dock {
// Pure policy port of DockNativeMotion/DockRecentsMotion. These are the
// established visual constants, not vendor addresses or object offsets.
struct MotionSample {
    int64_t sequence=0,timestamp=0;int scene=0;double scale=1;
    int64_t entries=0,published=0;
    bool operator==(const MotionSample& other)const {
        return sequence==other.sequence&&timestamp==other.timestamp&&scene==other.scene
            &&(std::bit_cast<uint64_t>(scale)==std::bit_cast<uint64_t>(other.scale)
                ||(std::isnan(scale)&&std::isnan(other.scale)))&&entries==other.entries&&published==other.published;
    }
};
inline std::optional<MotionSample> validate_motion(int64_t sequence,int64_t timestamp,uint64_t packed,
    int64_t entries,int64_t published,int64_t previous,int64_t now) {
    const auto scene=int(packed&3);const double scale=std::bit_cast<double>(packed&~uint64_t(3));
    if(sequence<=previous||timestamp<0||timestamp>now||now-timestamp>500000000
        ||entries<0||published<0||published>entries||!std::isfinite(scale)||scale<0||scale>2)return {};
    return MotionSample{sequence,timestamp,scene,scale,entries,published};
}
inline std::optional<double> auto_aim_scale(const std::optional<MotionSample>& sample,int64_t epoch_ms,int64_t now) {
    if(!sample||sample->scene!=3||epoch_ms<0||now<sample->timestamp||now-sample->timestamp>100000000
        ||epoch_ms>std::numeric_limits<int64_t>::max()/1000000||sample->timestamp<epoch_ms*1000000
        ||!std::isfinite(sample->scale)||sample->scale<0||sample->scale>2)return {};
    return sample->scale;
}
inline bool published_progress(const std::optional<MotionSample>& previous,const std::optional<MotionSample>& incoming) {
    if(!incoming)return false;
    if(!previous)return incoming->published>0;
    if(incoming->entries<previous->entries||incoming->published<previous->published)return false;
    return incoming->published>previous->published||incoming->scene!=previous->scene
        ||std::bit_cast<uint64_t>(incoming->scale)!=std::bit_cast<uint64_t>(previous->scale);
}
class NativeMotion {
    int64_t sequence_=0;bool recents_=false,overview_=false;float progress_=0;
    std::optional<MotionSample> last_;
public:
    bool accept(const std::optional<MotionSample>& sample,bool overview=false) {
        if(!sample||sample->sequence<sequence_||sample->scene==3)return false;
        if(sample->sequence==sequence_) {
            if(!last_||*last_!=*sample||overview_||!overview)return false;
        } else {sequence_=sample->sequence;last_=sample;}
        overview_=overview;
        if(sample->scene==1)recents_=true;
        else if(overview&&sample->scale<.999999&&sample->scale>=.90)recents_=true;
        else if(sample->scene==0&&(!recents_||sample->scale>=.999999||sample->scale<.90))recents_=false;
        progress_=recents_?float(std::clamp((1-sample->scale)/.05,0.,1.2)):0;
        if(sample->scene==2&&std::abs(sample->scale-1)<.000001){progress_=0;recents_=false;}
        return true;
    }
    float progress()const{return progress_;}
    float scale()const {
        if(recents_&&last_&&last_->scale>=.8&&last_->scale<=1.05)return float(last_->scale);
        return 1.f-.05f*progress_;
    }
    static float center_shift(float extent,float scale){return extent*(1.f-scale)*.5f;}
    static float relative_y(float density,float progress) {
        if(!std::isfinite(density)||density<=0||!std::isfinite(progress))return 0;
        return 8.f*density*std::clamp(progress,0.f,1.2f);
    }
    float relative_y(float density)const{return relative_y(density,progress_);}
    void reset(){sequence_=0;recents_=false;progress_=0;last_.reset();overview_=false;}
};
class RecentsMotion {
    double from_=0,target_=0,velocity_=0;int64_t started_=0;
    struct Sample {double position,velocity;};
    Sample sample(int64_t now) {
        const auto elapsed=std::max(int64_t(0),now-started_);
        if((from_==target_&&velocity_==0)||elapsed>=1500)return {target_,0};
        const double response=target_==1?.3:.45,damping=target_==1?.9:.95;
        const double omega=2*std::acos(-1.)/response,decay=damping*omega;
        const double frequency=omega*std::sqrt(1-damping*damping),time=elapsed/1000.;
        const double displacement=from_-target_,coefficient=(velocity_+decay*displacement)/frequency;
        const double cosine=std::cos(frequency*time),sine=std::sin(frequency*time),envelope=std::exp(-decay*time);
        const double position=target_+envelope*(displacement*cosine+coefficient*sine);
        const double speed=envelope*((coefficient*frequency-decay*displacement)*cosine
            -(displacement*frequency+decay*coefficient)*sine);
        if(position<0||position>1) {
            const auto bounded=std::clamp(position,0.,1.);if(bounded==target_)finish();return {bounded,0};
        }
        if(std::abs(position-target_)<.001&&std::abs(speed)<.01){finish();return {target_,0};}
        return {position,speed};
    }
public:
    bool set_overview(bool overview,int64_t now) {
        const double next=overview?1:0;if(target_==next)return false;
        const auto current=sample(now);from_=current.position;velocity_=current.velocity;target_=next;started_=now;return true;
    }
    void resume(float progress,bool overview,int64_t now) {
        from_=std::isfinite(progress)?std::clamp(progress,0.f,1.f):0;target_=overview?1:0;velocity_=0;started_=now;
    }
    float progress(int64_t now){return float(sample(now).position);}
    double velocity(int64_t now){return sample(now).velocity;}
    bool running(int64_t now){const auto current=sample(now);return current.position!=target_||current.velocity!=0;}
    float offset(float density,int32_t base_y,int64_t now) {
        if(!std::isfinite(density)||density<=0||base_y<=0)return 0;
        return -std::min(20.f*density,float(base_y))*progress(now);
    }
    void finish(){from_=target_;velocity_=0;}
};
}
