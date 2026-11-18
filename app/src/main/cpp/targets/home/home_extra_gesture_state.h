/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <bit>
#include <cmath>
#include <cstdint>
#include <optional>

namespace home_extra_gestures {
inline uint32_t java_float(float value){return std::isnan(value)?0x7fc00000u:std::bit_cast<uint32_t>(value);}
struct Region {
    float left=0,top=0,right=0,bottom=0;
    bool contains(float x,float y)const{return x>=left&&x<right&&y>=top&&y<bottom;}
    bool valid()const{return std::isfinite(left)&&std::isfinite(top)&&std::isfinite(right)&&std::isfinite(bottom)&&left<right&&top<bottom;}
    bool operator==(const Region& other)const {
        return java_float(left)==java_float(other.left)&&java_float(top)==java_float(other.top)
            &&java_float(right)==java_float(other.right)&&java_float(bottom)==java_float(other.bottom);
    }
};
struct Geometry {
    int width=0,height=0,rotation=0;
    float density=0,nav_top=0,corner=0;
    std::optional<Region> handle;
    // Receiver/window generation identity, never a retained Dart object address.
    uint64_t owner=0;
    bool valid()const {
        return owner&&width>0&&height>0&&std::isfinite(density)&&density>=.5f&&density<=8
            &&std::isfinite(nav_top)&&nav_top>=0&&nav_top<height&&std::isfinite(corner)&&corner>0&&corner<=width/4.f;
    }
    bool operator==(const Geometry& other)const {
        return width==other.width&&height==other.height&&rotation==other.rotation&&owner==other.owner
            &&java_float(density)==java_float(other.density)&&java_float(nav_top)==java_float(other.nav_top)
            &&java_float(corner)==java_float(other.corner)&&handle==other.handle;
    }
};
struct Result {bool claim=false,handled=false;int event=0;bool operator==(const Result&)const=default;};
class State {
    std::optional<Geometry> geometry_;
    int64_t revision_=0,down_=0,latest_=0,tap_=-1;
    float x_=0,y_=0,tap_x_=0,tap_y_=0;
    int kind_=0,mask_=0;
    bool active_=false,owned_=false,second_=false,invalid_=false;
    float slop_;
    int double_timeout_,long_timeout_;
    bool matches(const Geometry& geometry,int64_t revision,int mask)const {
        return geometry_&&*geometry_==geometry&&revision_==revision&&mask_==mask;
    }
    Result result(bool claim,int event)const{return {claim,owned_,event};}
public:
    State(float slop,int double_timeout,int long_timeout):slop_(slop),double_timeout_(double_timeout),long_timeout_(long_timeout){}
    void reset(){active_=owned_=second_=invalid_=false;kind_=0;tap_=-1;geometry_.reset();}
    bool active()const{return active_;}
    bool pending_long()const{return active_&&kind_==3&&!invalid_&&!owned_&&!second_&&(mask_&8);}
    int64_t long_deadline()const {
        // Java long arithmetic wraps. No native signed-overflow assumption.
        return std::bit_cast<int64_t>(uint64_t(down_)+uint64_t(int64_t(long_timeout_)));
    }
    Result sample(int action,int pointers,float px,float py,int64_t time,int64_t down_time,
        const std::optional<Geometry>& geometry,int64_t revision,int mask) {
        if(!geometry||!geometry->valid()||!std::isfinite(px)||!std::isfinite(py)||time<down_time||down_time<0
            ||time-down_time>5000||pointers!=1||!std::isfinite(slop_)||slop_<=0){reset();return {};}
        const auto& g=*geometry;
        if(action==0) {
            if(!matches(g,revision,mask))tap_=-1;
            geometry_=g;revision_=revision;mask_=mask;down_=down_time;latest_=time;x_=px;y_=py;
            active_=true;owned_=invalid_=second_=false;kind_=0;
            if(py>=g.nav_top&&py<g.height&&px>=0&&px<g.width) {
                if(px<g.corner&&(mask&2))kind_=1;
                else if(px>=g.width-g.corner&&(mask&4))kind_=2;
                else if(g.handle&&g.handle->valid()&&g.handle->contains(px,py)&&(mask&24))kind_=3;
            }
            if(!kind_){active_=false;tap_=-1;return {};}
            second_=kind_==3&&(mask&16)&&tap_>=0&&time-tap_>=0&&time-tap_<=double_timeout_
                &&std::hypot(double(px-tap_x_),double(py-tap_y_))<=slop_;
            if(second_){owned_=true;tap_=-1;return result(true,0);}
            return {};
        }
        if(!active_)return {};
        if(!matches(g,revision,mask)||down_!=down_time||time<latest_){reset();return {};}
        latest_=time;
        if(action==3){reset();return {};}
        if(action!=1&&action!=2)return result(false,0);
        const float dx=px-x_,up=y_-py,inward=kind_==2?-dx:dx;
        const double travel=std::hypot(double(dx),double(up));bool claim=false;
        if(kind_==3) {
            if(travel>slop_){invalid_=true;tap_=-1;}
            if(action==1) {
                const int event=second_&&owned_&&!invalid_&&time-down_<=250?12:0;
                if(!owned_&&!invalid_&&time-down_<=250&&(mask&16)){tap_=time;tap_x_=px;tap_y_=py;}
                else tap_=-1;
                active_=false;return result(false,event);
            }
        } else {
            if(travel>slop_&&(inward<=0||up<=0||up>inward*3||inward>up*3))invalid_=true;
            if(action==2&&!invalid_&&!owned_&&travel>slop_){owned_=true;claim=true;tap_=-1;}
            if(action==1) {
                active_=false;return result(claim,owned_&&!invalid_&&inward>=48*g.density&&up>=48*g.density
                    &&time-down_<=1500?(kind_==1?9:10):0);
            }
        }
        return result(claim,0);
    }
    Result long_press(int64_t time,const std::optional<Geometry>& geometry,int64_t revision,int mask) {
        if(!geometry||!pending_long()||!matches(*geometry,revision,mask)||time<long_deadline()||time-down_>5000)return {};
        owned_=true;tap_=-1;invalid_=true;return result(true,11);
    }
};
class Shake {
    float x_=0,y_=0,z_=0;
    int64_t previous_=-1,impulse_=-1,last_=-1;
public:
    void reset(){previous_=impulse_=last_=-1;}
    bool sample(float x,float y,float z,int64_t time) {
        if(!std::isfinite(x)||!std::isfinite(y)||!std::isfinite(z)||time<0){reset();return false;}
        if(previous_<0||time<=previous_||time-previous_>500){x_=x;y_=y;z_=z;previous_=time;impulse_=-1;return false;}
        const float dx=std::fabs(x-x_),dy=std::fabs(y-y_),dz=std::fabs(z-z_);
        x_=x;y_=y;z_=z;previous_=time;
        if(!((dx>4&&dy>4)||(dx>4&&dz>8)||(dy>4&&dz>8)))return false;
        if(impulse_<0||time-impulse_>400){impulse_=time;return false;}
        if(time-impulse_<30)return false;
        impulse_=-1;if(last_>=0&&time-last_<750)return false;
        last_=time;return true;
    }
};
}
