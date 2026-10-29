#!/usr/bin/env python3
"""Compile production lookup boundaries with an observable busy worker lock."""
from pathlib import Path
import subprocess,tempfile
R=Path(__file__).resolve().parents[2]
s=(R/'app/src/main/cpp/targets/home/tweaks/entry.cpp').read_text()
def body(name):
 a=s.index(name);i=s.index('{',a);end=i+1;depth=1
 while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
 return s[a:end].replace('std::mutex','FixtureMutex')
prefix=r'''
#include <cassert>
#include <mutex>
#include <cstdio>
struct Busy{};
struct FixtureMutex{bool busy=false;unsigned waits=0;void lock(){++waits;if(busy)throw Busy{};busy=true;}bool try_lock(){if(busy)return false;busy=true;return true;}void unlock(){busy=false;}}g_workMutex;
struct Image{char path[32]="verified.apk";};
struct State{bool imageFound=true,configLoaded=true,repatchNeeded=false,restoreBlocked=false,writeRetryPending=false;Image image;}g_state;
bool loader=true;unsigned discoveries=0,builds=0;
bool InLoaderCallback(){return loader;}
bool TryDiscoverImage(){++discoveries;return g_state.imageFound;}
struct SymbolIndex{bool ready=false,started=true;static SymbolIndex& Instance(){static SymbolIndex value;return value;}bool loaded(){return ready;}bool attempted(){return started;}bool EnsureLoaded(Image){++builds;return ready;}bool Find(const char*,uint32_t* va,uint32_t* size){*va=4;*size=16;return true;}bool SpanFrom(uint32_t,uint32_t* span){*span=32;return true;}};
'''
main=r'''
int main(){uint32_t va=0,size=0;char path[32]{};auto& index=SymbolIndex::Instance();
 for(bool busy:{true,false})for(bool ready:{false,true}){
  g_workMutex.busy=busy;index.ready=ready;discoveries=builds=g_workMutex.waits=0;
  bool found=false,span=false,image=false;try{
   found=HomeLayoutEngineFindSymbol("test",&va,&size);
   span=HomeLayoutEngineSymbolSpan(4,&size);
   image=HomeLayoutEngineTargetImage(path,sizeof(path));
  }catch(Busy&){assert(false&&"loader waited for a busy parser mutex");}
  assert(found==(!busy&&ready)&&span==(!busy&&ready)&&image==!busy);
  assert(discoveries==0&&builds==0&&g_workMutex.waits==0);
 }
 loader=false;g_workMutex.busy=false;index.ready=true;discoveries=builds=0;
 assert(HomeLayoutEngineFindSymbol("test",&va,&size)&&HomeLayoutEngineSymbolSpan(4,&size)&&HomeLayoutEngineTargetImage(path,sizeof(path)));
 assert(discoveries==3&&builds==2); // worker discovery/build behavior retained
 State state;state.imageFound=false;assert(MonitorPeriodMs(state)>=1500);
 state.imageFound=true;state.configLoaded=false;assert(MonitorPeriodMs(state)>=1500);
 state.configLoaded=true;state.repatchNeeded=true;assert(MonitorPeriodMs(state)==50);
 state.restoreBlocked=true;assert(MonitorPeriodMs(state)>=1500);
 puts("Loader lookup PASS: no waiting/discovery/build on busy or cold loader paths, worker parsing retained, missing dependencies use slow cadence");
}
'''
with tempfile.TemporaryDirectory(prefix='hc-loader-perf-')as tmp:
 p=Path(tmp);src=p/'test.cpp';exe=p/'test'
 src.write_text(prefix+'\n'.join(body(n) for n in ['bool HomeLayoutEngineFindSymbol(', 'bool HomeLayoutEngineSymbolSpan(', 'bool HomeLayoutEngineTargetImage(', 'uint64_t MonitorPeriodMs('])+main)
 subprocess.run(['c++','-std=c++20','-O2',str(src),'-o',str(exe)],check=True)
 subprocess.run([str(exe)],check=True)
