/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_dock_motion.h"
#include <cassert>
#include <fstream>
#include <iostream>
using namespace home_dock;
static void close(double actual,double expected){assert(std::abs(actual-expected)<=.000003*std::max(1.,std::abs(expected)));}
int main(int argc,char** argv) {
    assert(argc==2);std::ifstream in(argv[1]);in>>std::boolalpha;
    NativeMotion native;RecentsMotion spring;std::optional<MotionSample> previous,last;
    int64_t sequence=0;std::string type;size_t operations=0;
    while(in>>type) {
        if(type=="R"){native.reset();spring=RecentsMotion{};previous.reset();last.reset();sequence=0;continue;}
        if(type=="N") {
            int64_t next,timestamp,entries,published,now,epoch;uint64_t packed;
            bool valid,overview,changed,accepted,has_aim;float density,progress,scale,offset;double aim;
            in>>next>>timestamp>>packed>>entries>>published>>valid>>now>>overview>>changed>>accepted>>density
                >>progress>>scale>>offset>>epoch>>has_aim>>aim;
            last=validate_motion(next,timestamp,packed,entries,published,sequence,now);
            assert(last.has_value()==valid);if(last)sequence=next;
            assert(published_progress(previous,last)==changed);assert(native.accept(last,overview)==accepted);
            close(native.progress(),progress);close(native.scale(),scale);close(native.relative_y(density),offset);
            const auto actual=auto_aim_scale(last,epoch,now);assert(actual.has_value()==has_aim);if(actual)close(*actual,aim);
            if(last)previous=last;
        } else if(type=="S") {
            int action,base;bool target,changed,running;float resume,density,progress,offset;int64_t now;double velocity;
            in>>action>>target>>resume>>now>>changed>>density>>base>>progress>>velocity>>offset>>running;
            if(action==0)assert(spring.set_overview(target,now)==changed);
            if(action==1)spring.resume(resume,target,now);
            if(action==2)spring.finish();
            close(spring.progress(now),progress);close(spring.velocity(now),velocity);
            close(spring.offset(density,base,now),offset);assert(spring.running(now)==running);
        } else if(type=="P") {
            bool accepted;float progress,scale;in>>accepted>>progress>>scale;
            assert(native.accept(last,true)==accepted);close(native.progress(),progress);close(native.scale(),scale);
        } else assert(false);
        assert(in.good()||in.eof());++operations;
    }
    assert(operations>160000);
    std::cout<<"PASS "<<operations<<" operations across 200 actual Java motion-policy sessions, stale/replayed samples, overview handoff, retargeted springs and unlock freshness\n";
}
