/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Actual production process_vm_readv/mprotect/atomic32 writer on host mappings.
// ROM bytes are data only; these tests never execute an AArch64 ROM instruction.
#include "targets/home/home_startup.h"
#include "targets/home/home_startup_plan.generated.h"
#include "nativehook/elf_image.h"
#include <cassert>
#include <cstring>
#include <fstream>
#include <functional>
#include <iterator>
#include <string>
#include <vector>
#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>
using namespace home_startup;
static uintptr_t base;static size_t page_size,extent;static void *originals[15];
static bool authorized=true;static unsigned hooks;
namespace home_startup::detail {
HookBinding binding(unsigned i) noexcept {return {reinterpret_cast<void*>(uintptr_t(0x10000+16*i)),&originals[i]};}
void initialize_consumers(uintptr_t,uintptr_t,uintptr_t,uintptr_t,uintptr_t,const uint32_t*) noexcept {}
void publish_consumers(const home_layout::Config&,uint64_t) noexcept {}
void clear_consumers() noexcept {}
}
static bool auth(void*){return authorized;}
static bool write16(void *target,const void *bytes){
    auto page=reinterpret_cast<uintptr_t>(target)&~(page_size-1);
    if(mprotect(reinterpret_cast<void*>(page),page_size,PROT_READ|PROT_WRITE|PROT_EXEC))return false;
    std::memcpy(target,bytes,16);return mprotect(reinterpret_cast<void*>(page),page_size,PROT_READ|PROT_EXEC)==0;
}
static int hook(void*target,void*,void**original){
    ++hooks;*original=reinterpret_cast<void*>(uintptr_t(0xab000000+hooks*16));
    const uint32_t code[]={0x58000050,0xd61f0200,hooks,0xf001cafe};return write16(target,code)?0:-1;
}
static int unhook(void*target){const uint32_t va=reinterpret_cast<uintptr_t>(target)-base;
    for(const auto&g:plan::kGuards)if(g.va<=va&&va+16<=g.va+g.size)
        return write16(target,plan::kGuardBytes+g.offset+va-g.va)?0:-1;
    return -1;
}
static Result apply(const home_layout::Config&c){return execute({base,hook,unhook,&c,auth,nullptr});}
static bool original_constants(){
    for(const auto&w:plan::kWords)if(*reinterpret_cast<const uint32_t*>(base+w.va)!=w.original)return false;
    for(const auto&w:plan::kCapacity)if(*reinterpret_cast<const uint32_t*>(base+w.va)!=w.original)return false;
    return true;
}
static bool all_code_rx(){
    // Out of the startup observer: independent read-back of kernel VMA flags.
    std::ifstream maps("/proc/self/maps");std::string line;
    while(std::getline(maps,line)) { unsigned long start,end;char permissions[8]{};
        if(std::sscanf(line.c_str(),"%lx-%lx %7s",&start,&end,permissions)!=3)continue;
        for(const auto&s:plan::kSegments)if(s.flags&1)
            if(start<base+s.va+s.memory_size&&end>base+s.va)
                if(permissions[0]!='r'||permissions[1]!='-'||permissions[2]!='x')return false;
    }
    return true;
}
static unsigned tests,failed;
static void test(const char*name,const std::function<void()>& fn){
    ++tests;std::fflush(nullptr);const pid_t pid=fork();assert(pid>=0);
    if(!pid){fn();_exit(0);}int status;assert(waitpid(pid,&status,0)==pid);
    bool ok=WIFEXITED(status)&&WEXITSTATUS(status)==0;if(!ok)++failed;
    std::printf("%s %s\n",ok?"PASS":"FAIL",name);
}
int main(int argc,char**argv){assert(argc==2);page_size=sysconf(_SC_PAGESIZE);
    std::ifstream input(argv[1],std::ios::binary);std::string raw(std::istreambuf_iterator<char>{input},{});
    std::span<const std::byte> bytes(reinterpret_cast<const std::byte*>(raw.data()),raw.size());
    auto segments=nhk::elf::parse_program_segments(bytes);assert(segments);
    for(const auto&s:*segments)if(s.type==1)extent=std::max(extent,size_t(s.vaddr+s.memsz));
    extent=(extent+page_size-1)&~(page_size-1);
    void*image=mmap(nullptr,extent,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);assert(image!=MAP_FAILED);
    base=reinterpret_cast<uintptr_t>(image);
    for(const auto&s:*segments)if(s.type==1)std::memcpy(reinterpret_cast<void*>(base+s.vaddr),raw.data()+s.offset,s.filesz);
    assert(mprotect(image,extent,PROT_READ)==0);
    for(const auto&s:*segments)if(s.type==1&&(s.flags&1)){
        const auto start=(base+s.vaddr)&~(page_size-1),end=(base+s.vaddr+s.memsz+page_size-1)&~(page_size-1);
        assert(mprotect(reinterpret_cast<void*>(start),end-start,PROT_READ|PROT_EXEC)==0);
    }
    test("real readv and atomic32 writes restore RX and constants",[]{assert(prepare_for_child());home_layout::Config c;
        c.tweaks.folder_enabled=true;c.tweaks.folder_cols=5;c.tweaks.hotseat_unlimited=true;
        auto r=apply(c);assert(r.outcome==Outcome::Applied&&!original_constants()&&all_code_rx());
        request_revoke();service_revoke();assert(original_constants()&&all_code_rx());});
    test("real readv inline bank has valid continuation and RX",[]{assert(prepare_for_child());home_layout::Config c;
        c.knobs[0]={true,30};auto r=apply(c);assert(r.applied==HotseatMargin&&hooks==1&&all_code_rx());
        request_revoke();service_revoke();assert(original_constants()&&all_code_rx());});
    test("real readv unmapped image declines without signal",[]{assert(prepare_for_child());base=0x1000;
        home_layout::Config c;assert(apply(c).error==Error::GuardMismatch&&!hooks);});
    test("real readv PROT_NONE guard declines without writes",[]{assert(prepare_for_child());const auto at=(base+plan::kGuards[0].va)&~(page_size-1);
        assert(mprotect(reinterpret_cast<void*>(at),page_size,PROT_NONE)==0);home_layout::Config c;
        assert(apply(c).error==Error::GuardMismatch&&!hooks);});
    test("real readv foreign bytes cannot be overwritten by revoke",[]{assert(prepare_for_child());home_layout::Config c;
        c.tweaks.folder_enabled=true;c.tweaks.folder_cols=5;assert(apply(c).outcome==Outcome::Applied);
        uint32_t va=0;for(const auto&w:plan::kWords)if(*reinterpret_cast<uint32_t*>(base+w.va)!=w.original){va=w.va;break;}assert(va);
        const auto page=(base+va)&~(page_size-1);assert(!mprotect(reinterpret_cast<void*>(page),page_size,PROT_READ|PROT_WRITE|PROT_EXEC));
        __atomic_store_n(reinterpret_cast<uint32_t*>(base+va),0xdeadbeef,__ATOMIC_RELAXED);
        assert(!mprotect(reinterpret_cast<void*>(page),page_size,PROT_READ|PROT_EXEC));
        request_revoke();assert(service_revoke().outcome==Outcome::TerminalFailure);
        assert(*reinterpret_cast<uint32_t*>(base+va)==0xdeadbeef&&all_code_rx());});
    std::printf("{\"tests\":%u,\"passed\":%u,\"failed\":%u}\n",tests,tests-failed,failed);return failed?1:0;
}
