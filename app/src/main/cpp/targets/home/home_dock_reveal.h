/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
namespace home_dock {
// Our DockUnlockReveal policy, not vendor offsets or a VM ABI. Epochs use
// uptime milliseconds, as does the auxiliary host's original Java 3D sampler.
// This class owns only the native container, opacity and position.
class Reveal {
    static constexpr int64_t duration=821,lead=10,total=duration+lead,expiry=1500;
    bool armed_=false,running_=false,pending_=false;
    int64_t armed_at_=0,started_at_=-1;
    int style_=0;
    float auto_start_=1;
    static float clamp(float x){return std::max(0.f,std::min(1.f,x));}
    static float back(float t,float tension){const float x=clamp(t)-1;return 1+(tension+1)*x*x*x+tension*x*x;}
    static float axis(float a,float b,float t){const float inverse=1-t;return 3*inverse*inverse*t*a+3*inverse*t*t*b+t*t*t;}
    static float decelerate(float x) {
        if(x<=0)return 0;if(x>=1)return 1;
        float low=0,high=1;
        for(int i=0;i<24;++i){const float mid=(low+high)*.5f;if(axis(.05f,.1f,mid)<x)low=mid;else high=mid;}
        return axis(.7f,1,(low+high)*.5f);
    }
    static float raw_spring(float t,float w) {
        const float wd=w*float(std::sqrt(double(1.f-.62f*.62f)));
        const float decay=float(std::exp(double(-.62f*w*t)));
        const float coefficient=(.62f*w-3.4f)/wd;
        return 1-decay*(float(std::cos(double(wd*t)))+coefficient*float(std::sin(double(wd*t))));
    }
    static float spring(float t,float w){return raw_spring(t,w)/raw_spring(1,w);}
    float fraction(int64_t now) {
        if(armed_)return 0;if(!running_)return 1;
        const auto elapsed=std::max(int64_t(0),now-started_at_-lead);
        if(elapsed>=duration){running_=false;return 1;}
        return float(elapsed)/float(duration);
    }
public:
    struct Container {
        float scale_x=1,scale_y=1,crop_width=1,crop_height=1,corner_progress=1;bool active=false;
        static Container projected(double scale) {
            if(!std::isfinite(scale)||scale<0||scale>2)return {};
            const float value=float(scale);if(std::abs(value-1)<.000001f)return {};
            return {value,value,1,1,1,true};
        }
        float radius(float resting,float height)const {
            if(!std::isfinite(resting)||!std::isfinite(height)||height<=0)return std::isnan(resting)?resting:std::max(0.f,resting);
            const float capsule=height*crop_height*.5f;
            return capsule+(resting-capsule)*clamp(corner_progress);
        }
    };
    void style(int value){style_=value;}
    int style()const{return style_;}
    static bool accepts_pending(int64_t event,int64_t now){return event>=0&&now>=event&&now-event<=expiry;}
    static bool accepts_event(int64_t previous,int64_t now){return now>=0&&(previous<0||(now>=previous&&now-previous>=total+400));}
    bool arm(int64_t now){needs_frame(now);if(armed_||running_||!accepts_event(started_at_,now))return false;armed_=true;armed_at_=now;pending_=true;return true;}
    void cancel(){armed_=false;running_=false;}
    bool start(int64_t now){if(!armed_)return false;if(!accepts_pending(armed_at_,now)){cancel();return false;}armed_=false;running_=true;started_at_=armed_at_;return true;}
    bool running()const{return armed_||running_;}
    bool pending()const{return pending_;}
    int64_t epoch()const{return armed_?armed_at_:started_at_;}
    void committed(int64_t now){if(!armed_&&(!running_||now-started_at_>=total))pending_=false;}
    bool stalled(int64_t now)const {if(armed_)return now-armed_at_>expiry;if(running_)return now-started_at_>total+200;return true;}
    bool needs_frame(int64_t now){if(armed_&&now-armed_at_>expiry){cancel();return false;}if(running_&&now>=started_at_&&now-started_at_>=total)running_=false;return running();}
    float progress(int64_t now){const float inverse=1-fraction(now);return 1-inverse*inverse*inverse;}
    float alpha(int64_t now) {
        if(style_==7){const float footprint=auto_start_+(1-auto_start_)*progress(now);const float t=clamp((footprint-.75f)/(1-.75f));return t*t*(3-2*t);}
        const float elapsed=fraction(now)*duration;
        const float t=style_==6?clamp((elapsed-28)/265):std::min(1.f,elapsed/180);
        return t*t*(3-2*t);
    }
    float rise(float density,int64_t now) {
        if(!std::isfinite(density)||density<=0)return 0;
        const float t=fraction(now);if(style_==2||style_==4||style_>=5)return 0;
        if(style_==3)return 44*density*(1-spring(t,8.8f));
        const float remaining=1-t;const float lift=remaining*remaining*(1-(1.25f+1)*t);
        return 96*density*lift;
    }
    float slide(float width,int64_t now) {
        if(!std::isfinite(width)||width<=0)return 0;
        const float t=fraction(now);
        if(style_==2)return -.34f*width*(1-decelerate(t));
        if(style_==3)return -.08f*width*(1-spring(t,12));return 0;
    }
    static Container container(int style,float t) {
        if(t>=1)return {};
        if(style==5||style==6) {
            const float p=style==5?back(t,1.42f):back(clamp(t/.78f),1.78f);
            const float open=clamp(p),overshoot=std::max(0.f,p-1);
            if(style==5)return {1+overshoot*.46f,1+overshoot*.28f,.56f+(1-.56f)*open,.5f+(1-.5f)*open,open,true};
            return {1+overshoot*.40f,1+overshoot*.16f,.16f+(1-.16f)*open,.54f+(1-.54f)*open,open,true};
        }
        return {};
    }
    static Container container(int style,int64_t start,int64_t now) {
        const auto elapsed=now-start-lead;
        return container(style,elapsed<=0?0.f:elapsed>=duration?1.f:float(elapsed)/float(duration));
    }
    Container container(int64_t now){return container(style_,fraction(now));}
    static float projected_start(double radius) {
        if(!std::isfinite(radius)||radius<=0)return 1;
        const double denominator=346.41016151377556-1.71882*radius;
        if(!std::isfinite(denominator)||std::abs(denominator)<1)return 1;
        const double scale=346.41016151377556/denominator;
        if(!std::isfinite(scale))return 1;
        return float(std::clamp(scale,-8.,8.));
    }
    Container auto_pose(int64_t now,float start) {
        auto_start_=std::isfinite(start)?start:1;
        if(fraction(now)>=1)return {};
        const float scale=auto_start_+(1-auto_start_)*progress(now);
        if(!std::isfinite(scale)||std::abs(scale-1)<.000001f)return {};
        return {scale,scale,1,1,1,true};
    }
};
}
