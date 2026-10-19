/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_endpoint_diagnostics.h"
#include "targets/home/home_object_scale.h"
#include <vector>
#include <cassert>
#include <iostream>
#include <array>
#include <atomic>
#include <cstdarg>
#include <cstdio>
using namespace home_layout;
constexpr int ANDROID_LOG_WARN=5;constexpr const char* kTag="fixture";
static std::vector<std::string> events;
int __android_log_print(int,const char*,const char* format,...){
    char text[1024];va_list args;va_start(args,format);std::vsnprintf(text,sizeof(text),format,args);va_end(args);
    events.emplace_back(text);return 0;
}
#include "binding-diagnostic.h"
int main(){
    EndpointDiagnostics d;
    auto decode=[&](std::vector<int32_t> words){size_t at=0;
        return read_endpoint_diagnostics([&](int32_t& w){if(at==words.size())return false;w=words[at++];return true;},d);};
    assert(decode({})==EndpointDiagnosticState::Missing);
    assert(decode({kEndpointDiagnosticsMagic,1,kEndpointRevision,1,1234})==EndpointDiagnosticState::Valid);
    assert(d.revision==kEndpointRevision&&d.capabilities==1&&d.pid==1234);
    assert(decode({kEndpointDiagnosticsMagic,2})==EndpointDiagnosticState::Unsupported);
    assert(decode({kEndpointDiagnosticsMagic,1})==EndpointDiagnosticState::Malformed);
    assert(decode({0,1,1,1,1})==EndpointDiagnosticState::Malformed);
    assert(decode({kEndpointDiagnosticsMagic,1,1,1,0})==EndpointDiagnosticState::Malformed);
    assert(decode({kEndpointDiagnosticsMagic,1,kEndpointRevision-1,0,1})==EndpointDiagnosticState::Valid);
    // Valid old functional snapshots remain usable even without optional diagnostics.
    ObjectScales scales{};bool present=true;
    assert(read_object_scales([](int32_t&){return false;},scales,true,&present)&&!present);
    for(auto s:scales)assert(!s.enabled&&s.factor()==1);
    std::vector<int32_t> block={kObjectScaleMagic,1,75,1,125,0,100};size_t at=0;
    assert(read_object_scales([&](int32_t& w){if(at==block.size())return false;w=block[at++];return true;},scales,true,&present));
    assert(present&&scales[0].factor()==.75&&scales[1].factor()==1.25);
    assert(!scale_bind_failure(0,10,"symbol unavailable"));assert(events.size()==1);
    assert(!scale_bind_failure(0,10,"symbol unavailable"));assert(events.size()==1);
    assert(!scale_bind_failure(0,30,"shape mismatch"));assert(events.size()==2);
    assert(!scale_bind_failure(1,30,"shape mismatch"));assert(events.size()==3);
    g_scale_bind_failure[0].store(0);assert(!scale_bind_failure(0,10,"symbol unavailable"));assert(events.size()==4);
    assert(events[0].find("folder_close_animation")!=std::string::npos);
    assert(events[2].find("widget_scale")!=std::string::npos);
    std::cout<<"endpoint metadata / old snapshot compatibility / production binding-failure dedup: PASS\n";
}
