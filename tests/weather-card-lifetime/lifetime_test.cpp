/* SPDX-License-Identifier: AGPL-3.0-or-later */
#define WEATHER_CARD_LIFETIME_TEST
#ifndef HC_LIFETIME_SOURCE
#define HC_LIFETIME_SOURCE "../../app/src/main/cpp/targets/weather/weather_card_lifetime.cpp"
#endif
#include HC_LIFETIME_SOURCE
#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <functional>
#include <thread>
#include <sstream>
#include <string>
#include <vector>
#include <sys/wait.h>

namespace fixture {
using namespace weather_card_lifetime;
uintptr_t flutter=0, libc=0, dart=0;
size_t page_size=4096;
long process=1000, parent=1000, thread=1000;
int slot_prot=PROT_READ, page_prot=PROT_READ|PROT_EXEC, libc_prot=PROT_READ|PROT_EXEC;
std::string map_override, task_override;
size_t map_at=0, task_at=0;
bool map_fail=false, map_noeof=false, task_fail=false, task_noeof=false, wrong_target=false;
uintptr_t read_fail=0;
unsigned protect_calls=0, fail_protect=0, cas_calls=0, foreign_cas=0;
int forward_fail=0;
std::function<void()> forward_event;
struct Forward {uintptr_t address;size_t length;int advice;};
std::vector<Forward> calls;
std::string line(uintptr_t begin,uintptr_t end,int perms,const char *path="") {
 char s[256];std::snprintf(s,sizeof(s),"%lx-%lx %c%c%cp 00000000 00:00 0 %s\n",begin,end,perms&1?'r':'-',perms&2?'w':'-',perms&4?'x':'-',path);return s;
}
std::string maps() {
 if(!map_override.empty())return map_override;
 auto slot=flutter+weather_card_lifetime_guard::kMadviseSlotVa;
 auto base=slot&~(page_size-1);
 return line(flutter,flutter+page_size,1,"/system_ext/lib64/libhyper_os_flutter.so")
   +line(base,base+page_size,slot_prot)+line(dart,dart+0x400000,page_prot)
   +line(libc+0x5c000,libc+0xf8000,libc_prot);
}
void dir(std::string &s,const std::string &name) {
 size_t n=(19+name.size()+1+7)&~size_t(7),off=s.size();s.resize(off+n,0);
 uint16_t len=n;std::memcpy(s.data()+off+16,&len,2);s[off+18]=4;std::memcpy(s.data()+off+19,name.c_str(),name.size()+1);
}
std::string tasks() {
 if(!task_override.empty())return task_override;
 std::string s;dir(s,".");dir(s,"..");dir(s,std::to_string(thread));return s;
}
uintptr_t alloc(size_t size) {
 auto raw=reinterpret_cast<uintptr_t>(mmap(nullptr,size+16384,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
 if(raw==reinterpret_cast<uintptr_t>(MAP_FAILED))std::abort();return (raw+16383)&~uintptr_t(16383);
}
void writable_slot(uintptr_t value) {
 auto address=flutter+weather_card_lifetime_guard::kMadviseSlotVa;
 ::mprotect(reinterpret_cast<void*>(address&~(page_size-1)),page_size,3);
 *reinterpret_cast<uintptr_t*>(address)=value;
 ::mprotect(reinterpret_cast<void*>(address&~(page_size-1)),page_size,slot_prot);
}
void setup(size_t size=4096) {
 page_size=size;flutter=alloc(0x1200000);libc=alloc(0x100000);dart=alloc(0x400000);
 for(auto g:weather_card_lifetime_guard::kFlutterGuards)std::memcpy(reinterpret_cast<void*>(flutter+g.va),g.data,g.size);
 for(auto g:weather_card_lifetime_guard::kLibcGuards)std::memcpy(reinterpret_cast<void*>(libc+g.va),g.data,g.size);
 for(auto site:weather_card_lifetime_guard::kSites)*reinterpret_cast<uint32_t*>(dart+site)=weather_card_lifetime_guard::kOriginalWord;
 writable_slot(libc+weather_card_lifetime_guard::kMadviseVa);
}
void capture(){observe_parent_load("/system_ext/lib64/libhyper_os_flutter.so");}
void child(bool weather=true){process=1001;thread=1001;after_specialization(weather);}
void start(){capture();child();}
std::array<uintptr_t,2> sites(){return {dart+0x3a8408,dart+0x3aece0};}
bool claim(){return protect(dart,flutter,sites());}
int advise(uintptr_t address,size_t length,int advice=MADV_DONTNEED) {
 auto target=*reinterpret_cast<uintptr_t*>(flutter+weather_card_lifetime_guard::kMadviseSlotVa);
 if(target!=reinterpret_cast<uintptr_t>(&weather_card_lifetime::observed_madvise))return -999;
 return reinterpret_cast<int(*)(void*,size_t,int)>(target)(reinterpret_cast<void*>(address),length,advice);
}
bool avoids() {
 for(auto call:calls)for(auto site:sites()) {
  auto page=site&~(page_size-1);
  if(call.advice==MADV_DONTNEED && call.address<page+page_size && call.address+call.length>page)return false;
 }
 return true;
}
}
namespace weather_card_lifetime_test {
long page_size() noexcept{return fixture::page_size;}
void before_slot_cas() noexcept {
 using namespace fixture;
 if(++cas_calls==foreign_cas)*reinterpret_cast<uintptr_t*>(flutter+weather_card_lifetime_guard::kMadviseSlotVa)=0x12345678;
}
int forward(uintptr_t target,void* address,size_t length,int advice) noexcept {
 using namespace fixture;
 if(target!=libc+weather_card_lifetime_guard::kMadviseVa)wrong_target=true;
 calls.push_back({reinterpret_cast<uintptr_t>(address),length,advice});
 if(forward_event)forward_event();
 if(forward_fail && calls.size()==static_cast<size_t>(forward_fail)){errno=EIO;return -1;}return 0;
}
long call(long op,uintptr_t a,uintptr_t b,uintptr_t c,uintptr_t d,uintptr_t e,uintptr_t f) noexcept {
 using namespace fixture;
 if(op==SYS_getpid)return process;if(op==SYS_gettid)return thread;if(op==SYS_getppid)return parent;
 if(op==SYS_openat){auto s=reinterpret_cast<const char*>(b);if(!std::strcmp(s,"/proc/self/maps")){map_at=0;return map_fail?-1:100;}if(!std::strcmp(s,"/proc/self/task")){task_at=0;return task_fail?-1:101;}return -1;}
 if(op==SYS_close)return 0;
 if(op==SYS_read && a==100){auto s=maps();if(map_at==s.size() && map_noeof)return -1;size_t n=std::min<size_t>(c,s.size()-map_at);std::memcpy(reinterpret_cast<void*>(b),s.data()+map_at,n);map_at+=n;return n;}
 if(op==SYS_getdents64 && a==101){auto s=tasks();if(task_at==s.size() && task_noeof)return -1;size_t n=std::min<size_t>(c,s.size()-task_at);std::memcpy(reinterpret_cast<void*>(b),s.data()+task_at,n);task_at+=n;return n;}
 if(op==SYS_process_vm_readv){auto remote=reinterpret_cast<iovec*>(d);auto at=reinterpret_cast<uintptr_t>(remote->iov_base);if(read_fail>=at && read_fail<at+remote->iov_len)return -1;return ::syscall(op,getpid(),b,c,d,e,f);}
 if(op==SYS_mprotect){++protect_calls;if(protect_calls==fail_protect){errno=EACCES;return -1;}int r=::mprotect(reinterpret_cast<void*>(a),b,c);if(r==0)slot_prot=c;return r;}
 return ::syscall(op,a,b,c,d,e,f);
}
}
using Test=std::pair<const char*,std::function<bool()>>;
int main(){
 using namespace fixture;using namespace weather_card_lifetime;
 std::vector<Test> tests={
 {"positive 4K",[]{setup();start();return ready()&&!intact()&&!has_residue()&&claim()&&intact();}},
 {"positive 16K",[]{setup(16384);start();return ready()&&claim()&&intact();}},
 {"errno metadata",[]{setup();errno=EDOM;start();bool r=ready()&&claim()&&intact()&&release_pages();return r&&errno==EDOM;}},
 {"no capture no late fallback",[]{setup();child();capture();return !ready()&&!claim();}},
 {"wrong path",[]{setup();observe_parent_load("/vendor/lib64/libhyper_os_flutter.so");child();return !ready();}},
 {"non Weather",[]{setup();capture();child(false);return !ready()&&!has_residue();}},
 {"not fork",[]{setup();capture();after_specialization(true);return !ready();}},
 {"wrong parent",[]{setup();capture();parent=999;child();return !ready();}},
 {"wrong TID",[]{setup();capture();process=1001;thread=1002;after_specialization(true);return !ready();}},
 {"multi task",[]{setup();capture();dir(task_override,".");dir(task_override,"..");dir(task_override,"1001");dir(task_override,"1002");child();return !ready();}},
 {"task no EOF",[]{setup();capture();task_noeof=true;child();return !ready();}},
 {"task open fail",[]{setup();capture();task_fail=true;child();return !ready();}},
 {"task bad reclen",[]{setup();capture();task_override=tasks();task_override[16]=1;child();return !ready();}},
 {"task missing NUL",[]{setup();capture();task_override=std::string(24,'x');task_override[16]=24;task_override[17]=0;task_override[18]=4;child();return !ready();}},
 {"task truncated",[]{setup();capture();task_override=tasks();task_override.pop_back();child();return !ready();}},
 {"task overflow",[]{setup();capture();dir(task_override,".");dir(task_override,"..");dir(task_override,"184467440737095516160");child();return !ready();}},
 {"task wrong type",[]{setup();capture();task_override=tasks();task_override[18]=8;child();return !ready();}},
 {"task duplicate dot",[]{setup();capture();dir(task_override,".");dir(task_override,".");dir(task_override,"..");dir(task_override,"1001");child();return !ready();}},
 {"maps missing EOF",[]{setup();map_noeof=true;start();return !ready();}},
 {"maps trailing truncation",[]{setup();map_override=maps()+"truncated-tail";start();return !ready();}},
 {"maps duplicate Flutter",[]{setup();map_override=maps()+line(flutter,flutter+4096,1,"/system_ext/lib64/libhyper_os_flutter.so");start();return !ready();}},
 {"maps malformed",[]{setup();map_override="garbage\n";start();return !ready();}},
 {"maps overlong",[]{setup();map_override=maps()+std::string(1100,'x')+"\n";start();return !ready();}},
 {"wrong page size",[]{setup();page_size=8192;start();return !ready();}},
 {"libc non-executable rejected",[]{setup();libc_prot=1;start();return !ready();}},
 {"child libc NX rejected",[]{setup();capture();libc_prot=1;child();return !ready();}},
 {"late libc executable loss",[]{setup();start();libc_prot=3;return !ready()&&!claim();}},
 {"late Dart page permission loss",[]{setup();start();if(!claim())return false;page_prot=3;return !intact();}},
 {"wrong original target",[]{setup();writable_slot(libc+0xe0344);start();return !ready();}},
 {"Flutter guard changed",[]{setup();*reinterpret_cast<uint8_t*>(flutter+0x10aeeac)^=1;start();return !ready();}},
 {"libc guard changed",[]{setup();*reinterpret_cast<uint8_t*>(libc+0xe0340)^=1;start();return !ready();}},
 {"capture read refusal",[]{setup();read_fail=flutter;start();return !ready();}},
 {"child rereads guards",[]{setup();capture();*reinterpret_cast<uint8_t*>(libc+0x7bcb4)^=1;child();return !ready();}},
 {"child rereads slot",[]{setup();capture();writable_slot(0x1234);child();return !ready();}},
 {"permission rejected",[]{setup();slot_prot=3;start();return !ready();}},
 {"mprotect first refusal",[]{setup();fail_protect=1;start();return !ready()&&!has_residue();}},
 {"restore failure rollback",[]{setup();fail_protect=2;start();return !ready()&&!has_residue()&&slot_prot==1&&*reinterpret_cast<uintptr_t*>(flutter+0x11143b8)==libc+0xe0340;}},
 {"foreign CAS residue",[]{setup();foreign_cas=1;start();return !ready()&&has_residue()&&*reinterpret_cast<uintptr_t*>(flutter+0x11143b8)==0x12345678;}},
 {"late slot loss",[]{setup();start();writable_slot(0x9876);return !ready()&&!claim();}},
 {"late permission loss",[]{setup();start();slot_prot=3;return !ready()&&!claim();}},
 {"wrong sites",[]{setup();start();auto s=sites();s[1]+=4;return !protect(dart,flutter,s)&&!intact();}},
 {"cross image",[]{setup();start();return !protect(dart,flutter+4096,sites())&&!claim();}},
 {"wrong code word",[]{setup();start();*reinterpret_cast<uint32_t*>(sites()[1])^=1;return !claim();}},
 {"wrong Dart protection",[]{setup();start();page_prot=3;return !claim();}},
 {"read failure during claim",[]{setup();start();read_fail=sites()[1];return !claim();}},
 {"in flight reader denies forever",[]{setup();start();bool denied=false;forward_event=[&]{denied=!claim();};int r=advise(dart,4096);forward_event={};return r==0&&denied&&!claim()&&!intact();}},
 {"cross-thread in-flight reader denies",[]{setup();start();std::atomic<bool> entered{false},leave{false};forward_event=[&]{entered.store(true,std::memory_order_release);while(!leave.load(std::memory_order_acquire))std::this_thread::yield();};int result=-99;std::thread other([&]{result=advise(dart,4096);});while(!entered.load(std::memory_order_acquire))std::this_thread::yield();bool denied=!claim();leave.store(true,std::memory_order_release);other.join();forward_event={};return denied&&result==0&&!intact()&&!claim();}},
 {"observer forwards before protect",[]{setup();start();errno=EDOM;int r=advise(dart,0x400000);return r==0&&calls.size()==1&&calls[0].length==0x400000&&errno==EDOM&&!wrong_target;}},
 {"release active reader refused",[]{setup();start();if(!claim())return false;bool denied=false;forward_event=[&]{denied=!release_pages();};int r=advise(dart,4096);forward_event={};return r==0&&denied&&intact()&&release_pages()&&!intact()&&!claim();}},
 {"release patched word refused",[]{setup();start();if(!claim())return false;*reinterpret_cast<uint32_t*>(sites()[0])=0x14000000;return !release_pages()&&intact();}},
 {"release resumes transparent",[]{setup();start();if(!claim()||!release_pages())return false;return advise(dart,0x400000)==0&&calls.size()==1&&calls[0].length==0x400000;}},
 {"three segment 4K",[]{setup();start();if(!claim())return false;errno=EDOM;return advise(dart,0x400000)==0&&calls.size()==3&&avoids()&&errno==EDOM;}},
 {"adjacent pages 16K",[]{setup(16384);start();if(!claim())return false;return advise(dart,0x400000)==0&&calls.size()==2&&avoids();}},
 {"only protected page",[]{setup();start();if(!claim())return false;return advise(sites()[0]&~uintptr_t(4095),4096)==0&&calls.empty();}},
 {"partial protected tail",[]{setup();start();if(!claim())return false;auto p=sites()[0]&~uintptr_t(4095);return advise(p-4096,4097)==0&&calls.size()==1&&calls[0].length==4096&&avoids();}},
 {"partial unprotected tail",[]{setup();start();if(!claim())return false;auto p=sites()[1]&~uintptr_t(4095);return advise(p,4097)==0&&calls.size()==1&&calls[0].address==p+4096&&calls[0].length==1;}},
 {"other advice untouched",[]{setup();start();if(!claim())return false;return advise(dart,0x400000,0)==0&&calls.size()==1&&calls[0].length==0x400000;}},
 {"zero length untouched",[]{setup();start();if(!claim())return false;return advise(dart,0)==0&&calls.size()==1&&calls[0].length==0;}},
 {"unaligned untouched",[]{setup();start();if(!claim())return false;return advise(dart+1,4096)==0&&calls.size()==1&&calls[0].address==dart+1;}},
 {"overflow untouched",[]{setup();start();if(!claim())return false;return advise(UINTPTR_MAX-4095,8192)==0&&calls.size()==1&&calls[0].length==8192;}},
 {"roundup overflow untouched",[]{setup();start();if(!claim())return false;return advise(UINTPTR_MAX-4095,4095)==0&&calls.size()==1;}},
 {"kernel error preserved",[]{setup();start();if(!claim())return false;forward_fail=2;errno=EDOM;return advise(dart,0x400000)==-1&&calls.size()==2&&errno==EIO&&avoids();}},
 {"all exact guard bits reject",[]{setup();uint64_t bits=0;auto exercise=[&](uintptr_t base,const auto &ranges){for(const auto &g:ranges)for(size_t n=0;n<g.size;++n)for(unsigned bit=0;bit<8;++bit){auto *byte=reinterpret_cast<uint8_t*>(base+g.va+n);*byte^=1U<<bit;bool denied=!weather_card_lifetime::guards(base,ranges);*byte^=1U<<bit;if(!denied)return false;++bits;}return true;};bool pass=exercise(flutter,weather_card_lifetime_guard::kFlutterGuards)&&exercise(libc,weather_card_lifetime_guard::kLibcGuards);std::printf("GUARD_BITS checked=%lu rejected=%lu\n",bits,pass?bits:0);return pass;}},
 {"no intersection exact",[]{setup();start();if(!claim())return false;return advise(dart,100)==0&&calls.size()==1&&calls[0].length==100;}},
 };
 unsigned failed=0;for(auto &[name,body]:tests){pid_t childpid=fork();if(childpid==0){bool pass=body();if(!pass)std::fprintf(stderr,"FAIL %s\n",name);std::fflush(nullptr);_exit(pass?0:1);}int status=0;waitpid(childpid,&status,0);if(status!=0){++failed;std::fprintf(stderr,"CASE_FAILED %s status=%d\n",name,status);}}
 std::printf("RESULT cases=%zu failed=%u\n",tests.size(),failed);return failed?1:0;
}
