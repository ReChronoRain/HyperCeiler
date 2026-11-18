/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_dock_reveal.h"
#include <fstream>
#include <iostream>
#include <string>
#include <cassert>
#include <limits>
using home_dock::Reveal;
void scalar(std::istream& in,float value){float expected;in>>expected;assert(std::abs(value-expected)<=.00002f*std::max(1.f,std::abs(expected)));}
void flag(std::istream& in,bool value){std::string expected;in>>expected;assert(value==(expected=="true"));}
void container(std::istream& in,Reveal::Container p){scalar(in,p.scale_x);scalar(in,p.scale_y);scalar(in,p.crop_width);scalar(in,p.crop_height);scalar(in,p.corner_progress);flag(in,p.active);scalar(in,p.radius(31,319));}
int main(int argc,char** argv) {
    assert(argc==2);std::ifstream in(argv[1]);assert(in);Reveal r;std::string op;int style=0,session=-1;size_t operations=0;
    while(in>>op) {
        if(op=="S"){in>>style;session=(session+1)%24;r=Reveal{};r.style(style);continue;}
        assert(op=="E");int64_t now;int command;in>>now>>command;
        bool result=true;
        switch(command){case 0:result=r.arm(now);break;case 1:result=r.start(now);break;case 2:r.cancel();break;case 3:r.committed(now);break;default:result=r.needs_frame(now);}
        flag(in,result);scalar(in,r.progress(now));
        const float start=Reveal::projected_start(session%4==0?std::numeric_limits<double>::quiet_NaN():session*72.25);
        container(in,r.auto_pose(now,start));scalar(in,r.alpha(now));
        const float density=session%7==0?std::numeric_limits<float>::quiet_NaN():session%7==1?-1:3.2f;
        scalar(in,r.rise(density,now));scalar(in,r.slide(session%5==0?0:1128.f,now));
        container(in,r.container(now));container(in,Reveal::container(style,now-session*100,now));
        int64_t epoch;in>>epoch;assert(r.epoch()==epoch);flag(in,r.running());flag(in,r.pending());flag(in,r.stalled(now));assert(in);++operations;
    }
    assert(operations==8*24*900);std::cout<<"PASS eight reveal policies, native container/opacity/trajectory, projection and lifecycle: "<<operations<<" Java oracle operations\n";
}
