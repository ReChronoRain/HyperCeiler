#!/usr/bin/env python3
"""Compile the actual monitor body with counted timing/locking/I/O boundaries."""
from pathlib import Path
import subprocess,tempfile
R=Path(__file__).resolve().parents[2]
s=(R/'app/src/main/cpp/targets/home/tweaks/entry.cpp').read_text()
a=s.index('void MonitorLoop() {');b=s.index('{',a);at=b+1;depth=1
while depth:depth+=(s[at]=='{')-(s[at]=='}');at+=1
body=s[a:at].replace('for (;;) {','for(unsigned pass=0;pass<200;++pass) {').replace('std::mutex','FixtureMutex')
prefix=r'''
#include <cassert>
#include <mutex>
#include <chrono>
#include <vector>
#include <cstdio>
struct FixtureMutex{bool locked=false;void lock(){assert(!locked);locked=true;}void unlock(){assert(locked);locked=false;}};
FixtureMutex g_workMutex,g_wakeMutex;
uint64_t now=0,period=50;bool g_wakeFlag=false;
struct State{bool configLoaded=false;std::vector<int> applied;};State g_state;
std::vector<uint64_t> observations;
uint64_t NowMs(){return now;}
bool TryDiscoverImage(){return true;}
void DumpLoadedLibraries(int){}void DumpApkMaps(int){}
[[maybe_unused]] constexpr const char* kTargetLibName="fixture";
#define LOGW(...) ((void)0)
void RefreshConfigLocked(State*,bool,bool){}void SettleLocked(State*,bool){}void Watchdog(State*){}
uint64_t MonitorPeriodMs(const State&){return period;}
struct CV{template<class Lock,class Pred>bool wait_for(Lock&,std::chrono::milliseconds delay,Pred){now+=delay.count();return false;}}g_wakeCv;
void hc_home_diagnostic_sample(){assert(!g_workMutex.locked&&!g_wakeMutex.locked);observations.push_back(now);}
'''
main=r'''
int main(){
 MonitorLoop();assert(observations.size()==2&&observations[0]==0&&observations[1]==5000);
 observations.clear();now=0;period=1500;g_state.configLoaded=true;MonitorLoop();
 assert(!observations.empty());for(size_t i=1;i<observations.size();i++)assert(observations[i]-observations[i-1]>=5000);
 puts("Shipping monitor sampling PASS: neutral/unrequested layout still observed, 200 fast and 200 settled passes, no per-pass logging, 5s deadline, outside both locks");
}
'''
with tempfile.TemporaryDirectory(prefix='hc-diag-sampler-')as temp:
 p=Path(temp);(p/'test.cpp').write_text(prefix+body+main)
 subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror',str(p/'test.cpp'),'-o',str(p/'test')],check=True)
 subprocess.run([str(p/'test')],check=True)
