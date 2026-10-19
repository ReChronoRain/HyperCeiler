#include <algorithm>
#include <array>
#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <string>
#include <sys/mman.h>
#include <sys/wait.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include "targets/home/recents_runtime.h"
#include "targets/hyos/hyos_snapshot.h"

namespace home_recents_test { bool atomics(); void event(unsigned); }
static bool deny_mlock=false,deny_arena=false,deny_rx_once=false,deny_rx_always=false;
static uintptr_t target_page=0;
static int protect(void *p,size_t n,int mode){
    if(reinterpret_cast<uintptr_t>(p)==target_page && mode==(PROT_READ|PROT_EXEC)
        && (deny_rx_once || deny_rx_always)){deny_rx_once=false;errno=EACCES;return -1;}
    return ::mprotect(p,n,mode);
}
[[maybe_unused]] static int lock_page(const void*p,size_t n){if(deny_mlock){errno=ENOMEM;return -1;}return ::mlock(p,n);}
static void *map_page(void*p,size_t n,int prot,int flags,int fd,off_t off){
    if(deny_arena && (flags&MAP_FIXED_NOREPLACE)){errno=ENOMEM;return MAP_FAILED;}
    return ::mmap(p,n,prot,flags,fd,off);
}
#define HOME_RECENTS_RUNTIME_TEST 1
#define mprotect protect
#define mlock lock_page
#define mmap map_page
#include "targets/home/recents_runtime.cpp"
#undef mmap
#undef mlock
#undef mprotect

static int passed=0,failed=0;
static void check(bool okay,const char*name){if(okay)++passed;else{++failed;std::cerr<<"FAIL "<<name<<'\n';}}
struct Image{
    uint8_t*base;size_t size;
    explicit Image(const char*path){
        std::ifstream f(path,std::ios::binary);std::vector<char> data((std::istreambuf_iterator<char>(f)),{});
        const auto *eh=reinterpret_cast<const Elf64_Ehdr*>(data.data());
        const auto *ph=reinterpret_cast<const Elf64_Phdr*>(data.data()+eh->e_phoff);size=0;
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD)size=std::max(size,size_t(ph[i].p_vaddr+ph[i].p_memsz));
        size=(size+4095)&~size_t(4095);base=static_cast<uint8_t*>(::mmap(nullptr,size,3,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
        if(base==MAP_FAILED)std::abort();
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD)std::memcpy(base+ph[i].p_vaddr,data.data()+ph[i].p_offset,ph[i].p_filesz);
        // Actual checked ELF R_AARCH64_RELATIVE pairs, not an identity bypass.
        for(const auto&p:home_recents_guard::kPointers){uintptr_t value=reinterpret_cast<uintptr_t>(base)+p.target;std::memcpy(base+p.slot,&value,8);}
        for(int i=0;i<eh->e_phnum;++i)if(ph[i].p_type==PT_LOAD){
            const auto begin=ph[i].p_vaddr&~uintptr_t(4095),end=(ph[i].p_vaddr+ph[i].p_memsz+4095)&~uintptr_t(4095);
            int prot=(ph[i].p_flags&PF_R?PROT_READ:0)|(ph[i].p_flags&PF_W?PROT_WRITE:0)|(ph[i].p_flags&PF_X?PROT_EXEC:0);
            if(::mprotect(base+begin,end-begin,prot))std::abort();
        }
        target_page=(reinterpret_cast<uintptr_t>(base)+home_recents_guard::kPatch)&~uintptr_t(4095);
    }
    template<class T>void data(uintptr_t off,T v){std::memcpy(base+off,&v,sizeof(v));}
    void code(uintptr_t off,uint32_t word){
        auto p=(reinterpret_cast<uintptr_t>(base)+off)&~uintptr_t(4095);
        if(::mprotect(reinterpret_cast<void*>(p),4096,7))std::abort();
        std::memcpy(base+off,&word,4);
        if(::mprotect(reinterpret_cast<void*>(p),4096,5))std::abort();
    }
    uint32_t word(){uint32_t v;std::memcpy(&v,base+home_recents_guard::kPatch,4);return v;}
};
static Image*image=nullptr;
static std::string scenario;
static hyos_snapshot::Snapshot snapshot;
static bool valid=true,isa=true;
static int pins=0,reads=0;
static bool read_snapshot(hyos_snapshot::Snapshot &out){++reads;out=snapshot;return valid;}
static bool pin(uintptr_t base,uintptr_t entry){
    ++pins;check(base==reinterpret_cast<uintptr_t>(image->base)&&entry==base+0x9b544c,"exact expected image pin");
    if(scenario=="pin-failure")return false;
    if(scenario=="pin-cold-race")image->data<uint32_t>(0xf19dc0,1);
    if(scenario=="pin-code-race")image->code(0x7a57d8,0xd503201f);
    return true;
}
namespace home_recents_test {
bool atomics(){return isa;}
void event(unsigned stage){
    if(stage==0 && scenario=="prephase-thread"){
        bool result=true;
        std::thread second([&]{result=home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(image->base),read_snapshot,pin);});
        second.join();check(!result,"concurrent entry before phase CAS cancels pending word");
    }
    if(stage==1 && scenario=="recursive-prepare")check(!home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(image->base),read_snapshot,pin),"recursive prepare declines without another writer");
    if(stage==1 && scenario=="thread-prepare"){
        bool result=true;
        std::thread second([&]{result=home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(image->base),read_snapshot,pin);});
        second.join();check(!result,"concurrent entry refuses waiting and cancels installer");
    }
    if(stage==2 && scenario=="service-during-install"){
        home_recents::service();check(home_recents::g_lease.read()==0,"service cannot pre-arm installer");
    }
    if(stage==3 && (scenario=="foreign-cas"||scenario=="foreign-cas-rx")){
        uint32_t foreign=0xd503201f;std::memcpy(image->base+0x7a58a8,&foreign,4);
    }
    if(stage==3 && scenario=="cancel-before-cas")home_recents::cancel();
    if(stage==4 && scenario=="early-update")home_recents::g_lease.cancel(); // Gateway pending CAS is separately real ARM tested.
    if(stage==4 && scenario=="once-starts")image->data<uint32_t>(0xf19dc0,1);
    if(stage==4 && scenario=="foreign-tail")image->code(0x7a58b0,0xd503201f);
    if(stage==5 && scenario=="last-revision")++snapshot.revision;
    if(stage==5 && scenario=="last-cancel")home_recents::cancel();
}
}
static void run(const char*path,const std::string &name){
    scenario=name;Image mapped(path);image=&mapped;
    snapshot.available=true;snapshot.flags=1;snapshot.epoch=10;snapshot.revision=20;
    snapshot.valid_until=home_recents::now()+4'000'000'000LL;
    snapshot.config.tweaks.animation_recents_enabled=true;
    snapshot.config.tweaks.animation_recents_rate_percent=name=="rate30"?30:name=="rate200"?200:170;
    if(name=="disabled")snapshot.config.tweaks.animation_recents_enabled=false;
    if(name=="invalid201")snapshot.config.tweaks.animation_recents_rate_percent=201;
    if(name=="unavailable")valid=false;
    if(name=="expired")snapshot.valid_until=home_recents::now()-1;
    if(name=="no-lse")isa=false;
    if(name=="warm-once")mapped.data<uint32_t>(0xf19dc0,0);
    if(name=="running-once")mapped.data<uint32_t>(0xf19dc0,1);
    if(name=="cold-pointer")mapped.data<uintptr_t>(0xf19db0,0x1234);
    if(name=="bad-body")mapped.code(0x7a57d8,0xd503201f);
    if(name=="bad-relocation")mapped.data<uintptr_t>(0xe13838,0x1234);
    if(name=="lock-fail")deny_mlock=true;
    if(name=="arena-fail")deny_arena=true;
    if((name=="rx-once"||name=="foreign-cas-rx"))deny_rx_once=true;
    if(name=="rx-permanent")deny_rx_always=true;
    if(name=="cold-cancel")home_recents::cancel();
    bool okay=home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(mapped.base),read_snapshot,pin);
    const bool want=name=="rate30"||name=="rate200"||name=="cold-cancel"
        ||name=="service-during-install"||name.starts_with("live-");
    check(okay==want,"prepare outcome");
    if(want){
        check(home_recents::status().phase==home_recents::Phase::Installed && pins==1,"installed and pin exactly once");
        check(mapped.word()==home_recents::g_branch,"actual aligned branch written");
        check(home_recents::page_locked(target_page),"actual selected VMA locked");
        errno=0;check(::madvise(reinterpret_cast<void*>(target_page),4096,MADV_DONTNEED)==-1 && errno==EINVAL,"real kernel advice4 rejects locked patch page");
        check(home_recents::arena_intact(),"actual RX arena complete bytes");
        const uint64_t first=home_recents::g_lease.read();
        check((first&255)==uint64_t(snapshot.config.tweaks.animation_recents_rate_percent),"full selected percentage armed");
        ++snapshot.revision;snapshot.valid_until=home_recents::now()+4'000'000'000LL;
        if(name=="live-change")snapshot.config.tweaks.animation_recents_rate_percent=100;
        if(name=="live-disable")snapshot.config.tweaks.animation_recents_enabled=false;
        if(name=="live-epoch")++snapshot.epoch;
        if(name=="live-revoke")valid=false;
        if(name=="live-expired")__atomic_store_n(&home_recents::g_lease_word,uint64_t(170)|(uint64_t(1)<<8),__ATOMIC_RELEASE);
        if(name=="live-code")mapped.code(0x7a58a8,0xd503201f);
        if(name=="live-unlock")check(::munlock(reinterpret_cast<void*>(target_page),4096)==0,"fixture unlock succeeds");
        home_recents::service();
        const bool revoked=name.starts_with("live-") && name!="live-renew";
        check((home_recents::g_lease.read()==1)==revoked,"same selection refresh or terminal denial");
        if(revoked){
            valid=true;snapshot.config.tweaks.animation_recents_enabled=true;snapshot.config.tweaks.animation_recents_rate_percent=170;
            snapshot.valid_until=home_recents::now()+4'000'000'000LL;home_recents::service();
            check(home_recents::g_lease.read()==1,"later valid snapshot cannot reopen cold-start choice");
        }
    }else{
        const bool residue=name=="foreign-cas"||name=="foreign-cas-rx"||name=="foreign-tail"||name=="rx-permanent";
        check((home_recents::status().phase==home_recents::Phase::Residue)==residue,"precise residue classification");
        check(home_recents::g_lease.read()==1,"failed installation stays denied");
        if(name!="foreign-cas"&&name!="foreign-cas-rx")check(mapped.word()==home_recents_guard::kOriginalWord,"self owned instruction rolled back");
        if(name=="foreign-cas"||name=="foreign-cas-rx")check(mapped.word()==0xd503201f,"foreign instruction retained");
        if(name!="rx-permanent")check(home_recents::protection(target_page,4096)==5,"RX restored even after failed exchange");
        check(!home_recents::prepare_before_entry(reinterpret_cast<uintptr_t>(mapped.base),read_snapshot,pin),"failed attempt cannot late install");
    }
}
int main(int argc,char**argv){
    if(argc!=2)return 2;
    const std::vector<std::string> cases={"rate30","rate200","disabled","invalid201","unavailable","expired","no-lse",
        "warm-once","running-once","cold-pointer","bad-body","bad-relocation","pin-failure","pin-cold-race",
        "pin-code-race","lock-fail","arena-fail","rx-once","rx-permanent","cold-cancel","recursive-prepare","thread-prepare","prephase-thread",
        "service-during-install","foreign-cas","foreign-cas-rx","cancel-before-cas","early-update","once-starts","foreign-tail",
        "last-revision","last-cancel","live-renew","live-change","live-disable","live-epoch","live-revoke",
        "live-expired","live-code","live-unlock"};
    int bad=0;
    for(const auto&name:cases){
        pid_t pid=fork();if(pid<0)return 3;
        if(pid==0){run(argv[1],name);std::cout<<name<<": passed="<<passed<<" failed="<<failed<<std::endl;_exit(failed?1:0);}
        int status=0;if(waitpid(pid,&status,0)!=pid || !WIFEXITED(status) || WEXITSTATUS(status))++bad;
    }
    std::cout<<"SCENARIOS="<<cases.size()<<" FAILED="<<bad<<'\n';return bad?1:0;
}
