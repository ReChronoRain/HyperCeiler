#include "targets/home/recents_policy.h"
#include <cstdio>
#include <thread>
#include <atomic>
static unsigned passed=0,failed=0;
void check(bool yes,const char *text){if(yes)++passed;else{++failed;std::fprintf(stderr,"FAIL %s\n",text);}}
hyos_snapshot::Snapshot snap(int64_t now=10'000'000'000LL,int rate=100){
    hyos_snapshot::Snapshot s;s.available=true;s.flags=1;s.epoch=101;s.revision=1;s.valid_until=now+4'000'000'000LL;
    s.config.tweaks.animation_recents_enabled=true;s.config.tweaks.animation_recents_rate_percent=rate;return s;
}
int main(){
    using namespace home_recents_policy;constexpr int64_t now=10'000'000'000LL;
    for(int rate=30;rate<=200;++rate){auto s=snap(now,rate);const auto w=project(s,now);check(w>1&&(w&255)==uint64_t(rate)&&(w>>8)==14000,"all171 UI rates retain exact byte and original deadline");}
    for(int which=0;which<12;++which){auto s=snap();int64_t at=now;
      switch(which){case 0:s.available=false;break;case 1:s.flags=0;break;case 2:s.flags=65;break;case 3:s.epoch=0;break;case 4:s.revision=0;break;case 5:s.valid_until=now;break;case 6:s.valid_until=now+5'000'000'001LL;break;case 7:s.config.tweaks.animation_recents_enabled=false;break;case 8:s.config.tweaks.animation_recents_rate_percent=29;break;case 9:s.config.tweaks.animation_recents_rate_percent=201;break;case 10:at=0;break;case 11:s.flags=9;break;}
      check(project(s,at)==kDenied,"malformed unavailable or unauthorized snapshot denied");
    }
    {auto s=snap();s.valid_until=now+999999;check(project(s,now)==kDenied,"submillisecond deadline truncates early rather than extends");}
    {alignas(8)uint64_t w=0;Lease l(w);auto s=snap(now,150);check(l.arm(s,now),"cold arm");
     auto newer=s;newer.revision=2;newer.valid_until+=1000000000;check(l.refresh(newer,now+1000000000),"same cold percentage epoch may renew newer revision");
     newer.config.tweaks.animation_recents_rate_percent=151;check(!l.refresh(newer,now+1000000000)&&w==1,"percentage change permanently denies requires restart");
     check(!l.refresh(s,now+1000000000)&&!l.arm(s,now),"same-value restoration cannot late reactivate");}
    for(int which=0;which<4;++which){alignas(8)uint64_t w=0;Lease l(w);auto s=snap();check(l.arm(s,now),"selected cold arm for transition");
      auto newer=s;newer.valid_until=now+6'000'000'000LL;int64_t at=now+2'000'000'000LL;
      if(which==0)newer.epoch++;
      if(which==1)newer.flags=0;
      if(which==2)newer.config.tweaks.animation_recents_enabled=false;
      if(which==3)at=now+4'000'000'000LL;
      check(!l.refresh(newer,at)&&w==1,"epoch denial disable expiry are terminal");}
    {alignas(8)uint64_t w=0;Lease l(w);uint64_t expected=0;
      check(__atomic_compare_exchange_n(&w,&expected,uint64_t{1},false,__ATOMIC_ACQ_REL,__ATOMIC_ACQUIRE),"early gate takes pending before publication");
      check(!l.arm(snap(),now)&&w==1,"early call prevents late activation");}
    // Execute the real production arm and gateway-equivalent single-word CAS
    // concurrently. This tests the protocol, not original Android scheduling.
    for(int i=0;i<200;++i){alignas(8)uint64_t w=0;Lease l(w);std::atomic<bool> go{false};bool armed=false,early=false;
      std::thread t([&]{while(!go.load(std::memory_order_acquire)){}uint64_t e=0;early=__atomic_compare_exchange_n(&w,&e,uint64_t{1},false,__ATOMIC_ACQ_REL,__ATOMIC_ACQUIRE);});
      go.store(true,std::memory_order_release);armed=l.arm(snap(),now);t.join();check(armed!=early&&((armed&&w>1)||(early&&w==1)),"first-use and activation have exactly one atomic winner");}
    std::printf("RESULT %u passed, %u failed\n",passed,failed);return failed?1:0;
}
