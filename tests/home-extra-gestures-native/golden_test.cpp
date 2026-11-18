/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_extra_gesture_state.h"
#include <cassert>
#include <iostream>
#include <sstream>
#include <string>
using namespace home_extra_gestures;
static float floating(std::istream& input){int64_t bits;input>>bits;return std::bit_cast<float>(static_cast<uint32_t>(bits));}
static std::optional<Geometry> geometry(std::istream& input) {
    int available,handle;Geometry g;input>>available>>g.width>>g.height>>g.rotation;
    g.density=floating(input);g.nav_top=floating(input);g.corner=floating(input);input>>g.owner>>handle;
    Region region;region.left=floating(input);region.top=floating(input);region.right=floating(input);region.bottom=floating(input);
    if(handle)g.handle=region;return available?std::optional<Geometry>{g}:std::nullopt;
}
int main() {
    State state(20,300,400);Shake shake;std::string line;size_t count=0;
    while(std::getline(std::cin,line)) {
        std::istringstream input(line);char command;input>>command;Result actual;
        if(command=='R'){state.reset();shake.reset();}
        else if(command=='S') {
            int action,pointers,mask;int64_t time,down,revision;input>>action>>pointers;
            const float x=floating(input),y=floating(input);input>>time>>down;
            const auto g=geometry(input);input>>revision>>mask;
            actual=state.sample(action,pointers,x,y,time,down,g,revision,mask);
        } else if(command=='L') {
            int64_t time,revision;int mask;input>>time;const auto g=geometry(input);input>>revision>>mask;
            actual=state.long_press(time,g,revision,mask);
        } else if(command=='K') {
            const float x=floating(input),y=floating(input),z=floating(input);int64_t time;input>>time;
            actual={false,false,shake.sample(x,y,z,time)?8:0};
        } else return 2;
        int claim,handled,event,active,pending;int64_t deadline;input>>claim>>handled>>event>>active>>pending>>deadline;
        if(!input||actual!=Result{claim!=0,handled!=0,event}||state.active()!=(active!=0)
            ||state.pending_long()!=(pending!=0)||state.long_deadline()!=deadline) {
            std::cerr<<"Gesture mismatch at "<<count<<": "<<line<<'\n';return 1;
        }
        ++count;
    }
    assert(count>40000);std::cout<<"PASS "<<count<<" original-Java/native gesture and shake results, ownership, cancellation, deadlines and geometry changes\n";
}
