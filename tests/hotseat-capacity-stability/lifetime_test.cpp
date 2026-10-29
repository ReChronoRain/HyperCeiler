/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_hotseat_capacity_lifetime.h"
#include "nativehook/page_guard.h"
#include <cstdio>
#include <cstdlib>
using namespace home_layout;
constexpr size_t N=8;
struct Process {
    std::array<nhk::SourceWordPatch,N> bank{};
    std::array<uint32_t,N> live{};
    std::array<int,N> protection{};
    uintptr_t base=0x70000000;
    size_t page=nhk::host_page_size();
    unsigned writes=0,pages=0,registrations=0;
    int rx_fail=-1,read_fail=-1;
    uint64_t generation=91;
    bool protected_pages=false;
    Process(){for(size_t i=0;i<N;++i){auto a=base+i*page;bank[i]={a,uint32_t(0x54000020+i*0x20),uint32_t(0x14000001+i),{1,2,91,i*page}};live[i]=bank[i].original;protection[i]=5;}}
    struct Ops {
        Process& p;size_t i;
        long PageSize(){return p.page;}
        bool Page(uintptr_t a,size_t n,nhk::CodePage& out){++p.pages;if(a!=p.bank[i].address||n!=p.page)return false;out={a,n,i*p.page,p.generation,1,2,p.protection[i],true};return true;}
        bool Accept(const nhk::CodePage& a){const auto& s=p.bank[i].source;return a.inode==s.inode&&a.major==s.device_major&&a.minor==s.device_minor&&a.offset==s.file_offset;}
        bool Read(uintptr_t a,uint32_t& out){if(a!=p.bank[i].address||int(i)==p.read_fail)return false;out=p.live[i];return true;}
        bool Protect(uintptr_t,size_t,int flags){if(flags==5&&int(i)==p.rx_fail)return false;p.protection[i]=flags;return true;}
        long Write(uintptr_t,uint32_t wanted,uint32_t expected){if(p.live[i]!=expected)return 0;p.live[i]=wanted;++p.writes;return 4;}
        void Flush(uintptr_t,size_t){}
    };
    Ops ops(const nhk::SourceWordPatch& s){return {*this,size_t((s.address-base)/page)};}
    auto factory(){return [this](const auto& s){return ops(s);};}
    CapacitySyncResult sync(bool enabled){return maintain_capacity_words(bank,enabled,protected_pages,[this](uintptr_t a,size_t n){++registrations;return n==4&&nhk::add_protected_page(a);},factory());}
    bool matches(bool enabled){for(size_t i=0;i<N;++i)if(live[i]!=(enabled?bank[i].replacement:bank[i].original)||protection[i]!=5)return false;return true;}
};
Process* process;
int discard(void* address,size_t n,int advice){
    if(advice!=4)return 0;
    uintptr_t a=reinterpret_cast<uintptr_t>(address);
    for(size_t i=0;i<N;++i)if(process->bank[i].address<a+n&&process->bank[i].address+process->page>a)process->live[i]=process->bank[i].original;
    return 0;
}
unsigned checks;
void c(bool yes){++checks;if(!yes){std::fprintf(stderr,"FAIL %u\n",checks);std::abort();}}
int main(){
    Process p;process=&p;
    // Reproduce the previous production path: source writes succeed, but none
    // register with the lifetime guard; covered-app discard silently resets all 8.
    c(nhk::apply_source_words(p.bank,true,p.factory()));c(p.matches(true));
    c(nhk::guarded_madvise(reinterpret_cast<void*>(p.base),p.page*N,4,discard)==0);
    c(p.matches(false));
    std::puts("REPRODUCED old capacity path: MADV_DONTNEED resets all 8 unregistered patches");
    auto r=p.sync(true);c(r.ready());c(r.restored==8);c(p.matches(true));c(p.registrations==8);
    p.writes=p.pages=p.registrations=0;
    for(int cycle=0;cycle<1000;++cycle){
        c(nhk::guarded_madvise(reinterpret_cast<void*>(p.base),p.page*N,4,discard)==0);
        r=p.sync(true);c(r.ready()&&r.restored==0);c(p.matches(true));
    }
    c(p.writes==0&&p.pages==0&&p.registrations==0); // healthy maintenance has no mapping scans or writes
    for(size_t i=0;i<N;++i){p.live[i]=p.bank[i].original;r=p.sync(true);c(r.ready()&&r.restored==1);c(p.matches(true));}
    c(p.sync(false).ready());c(p.matches(false));c(p.sync(false).ready());c(p.matches(false));
    c(p.sync(true).ready());c(p.matches(true));
    for(size_t i=0;i<N;++i){auto before=p.live;unsigned writes=p.writes;p.live[i]=0xd503201f;
        c(!p.sync(true).ready());c(p.live[i]==0xd503201f);c(p.writes==writes);p.live=before;}
    p.read_fail=3;unsigned writes=p.writes;c(!p.sync(true).ready());c(p.writes==writes);p.read_fail=-1;
    c(p.sync(false).ready());c(p.matches(false));
    // No code is exposed if page registration is incomplete, even if some earlier
    // metadata registrations succeeded. Retry completes registration then applies.
    p.protected_pages=false;unsigned n=0;writes=p.writes;
    r=maintain_capacity_words(p.bank,true,p.protected_pages,[&](uintptr_t,size_t){return ++n!=4;},p.factory());
    c(r.status==CapacitySyncStatus::page_guard_failed);c(!p.protected_pages);c(p.writes==writes);c(p.matches(false));
    c(p.sync(true).ready());c(p.matches(true));
    c(p.sync(false).ready());
    p.rx_fail=3;r=p.sync(true);c(!r.ready());
    bool pending=false;for(const auto& s:p.bank)pending|=s.pending();c(pending);
    p.rx_fail=-1;p.protected_pages=false;
    r=maintain_capacity_words(p.bank,true,p.protected_pages,[](uintptr_t,size_t){return false;},p.factory());
    c(r.status==CapacitySyncStatus::page_guard_failed);c(p.matches(false));
    for(const auto& s:p.bank)c(!s.pending()); // even failed registration must settle old RX/rollback debt
    c(p.sync(false).ready());c(p.matches(false));
    for(const auto& s:p.bank)c(!s.pending());
    // Layout-generation failure and foreign words must not be "fixed" by an
    // unvalidated write. NHK validates identities on every actual code mutation.
    p.live[3]=p.bank[3].original;p.generation=92;writes=p.writes;
    c(!p.sync(true).ready());c(p.writes==writes);c(p.live[3]==p.bank[3].original);
    p.generation=91;c(p.sync(true).ready());c(p.matches(true));c(p.sync(false).ready());
    auto invalid=p.bank;invalid[3].address=invalid[2].address;bool protected_state=false;writes=p.writes;
    r=maintain_capacity_words(invalid,true,protected_state,[](uintptr_t,size_t){return true;},p.factory());
    c(r.status==CapacitySyncStatus::invalid_bank);c(p.writes==writes);
    // Disabled or expired/denied request restores the full original bank, without
    // requiring another provider query or an animation/layout hook to execute.
    for(int cycle=0;cycle<100;++cycle){c(p.sync(true).ready());c(p.sync(false).ready());c(p.matches(false));}
    std::printf("hotseat lifetime %u checks passed\n",checks);
}
