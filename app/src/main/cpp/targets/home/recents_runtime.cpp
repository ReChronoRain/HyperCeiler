/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "recents_runtime.h"
#include "recents_binding.h"
#include "recents_policy.h"
#include <array>
#include <atomic>
#include <cerrno>
#include <cstring>
#include <fcntl.h>
#include <limits>
#include <span>
#include <sys/auxv.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <time.h>
#include <unistd.h>
#if defined(__aarch64__)
#include <asm/hwcap.h>
#endif

extern "C" {
extern const unsigned char hc_home_recents_template[], hc_home_recents_return_branch[];
extern const unsigned char hc_home_recents_lease_literal[], hc_home_recents_template_end[];
}
namespace home_recents {
namespace {
constexpr size_t kTemplateBytes=980;
constexpr size_t kReturnOffset=284, kLeaseOffset=288;
constexpr int kRx=PROT_READ|PROT_EXEC;
alignas(8) uint64_t g_lease_word=0;
home_recents_policy::Lease g_lease(g_lease_word);
std::atomic<Phase> g_phase{Phase::Cold};
std::atomic<Error> g_error{Error::None};
std::atomic<bool> g_owned{false};
std::atomic<uintptr_t> g_arena{0};
std::atomic_flag g_writer=ATOMIC_FLAG_INIT;
home_recents_binding::Binding g_binding{};
ReadFresh g_read=nullptr;
size_t g_page_size=0;
uint32_t g_branch=0;
bool g_protection_pending=false;
std::array<std::byte,kTemplateBytes> g_template{};
struct Writer {
    const bool owns=!g_writer.test_and_set(std::memory_order_acquire);
    ~Writer(){if(owns)g_writer.clear(std::memory_order_release);}
};
int64_t now() noexcept {
    timespec t{};
    if(clock_gettime(CLOCK_BOOTTIME,&t)!=0 || t.tv_sec<0 || t.tv_nsec<0 || t.tv_nsec>=1000000000
        || t.tv_sec>(std::numeric_limits<int64_t>::max()-999999999)/1000000000) return 0;
    return int64_t(t.tv_sec)*1000000000+t.tv_nsec;
}
bool read_memory(uintptr_t address,std::span<std::byte> output) noexcept {
    if(!address || output.empty() || address>UINTPTR_MAX-output.size())return false;
    iovec local{output.data(),output.size()},remote{reinterpret_cast<void*>(address),output.size()};
    return syscall(SYS_process_vm_readv,getpid(),&local,1,&remote,1,0)==static_cast<ssize_t>(output.size());
}
bool read_word(uintptr_t address,uint32_t &word) noexcept {
    return read_memory(address,std::as_writable_bytes(std::span(&word,1)));
}
// Bounded maps parser shares the already-reviewed format handling with the
// card installer, but this component owns its own state and exact Rust page.
bool hex(const char *line, size_t size, size_t &cursor, uintptr_t &result) noexcept {
    const size_t start = cursor;
    uintptr_t number = 0;
    while (cursor < size) {
        const unsigned char c = line[cursor];
        const unsigned digit = c >= '0' && c <= '9' ? c - '0'
            : c >= 'a' && c <= 'f' ? c - 'a' + 10
            : c >= 'A' && c <= 'F' ? c - 'A' + 10 : 16;
        if (digit == 16) break;
        if (number > (UINTPTR_MAX - digit) / 16) return false;
        number = number * 16 + digit;
        ++cursor;
    }
    if (cursor == start) return false;
    result = number;
    return true;
}

int mapping_line(const char *line, size_t size, uintptr_t address, size_t length) noexcept {
    size_t cursor = 0;
    uintptr_t begin = 0, end = 0;
    if (address == 0 || length == 0 || address > UINTPTR_MAX - length
        || !hex(line, size, cursor, begin) || cursor >= size || line[cursor++] != '-'
        || !hex(line, size, cursor, end) || cursor >= size || line[cursor++] != ' '
        || end <= begin || address < begin || address + length > end || cursor + 4 >= size)
        return -1;
    const char *permission = line + cursor;
    if ((permission[0] != 'r' && permission[0] != '-')
        || (permission[1] != 'w' && permission[1] != '-')
        || (permission[2] != 'x' && permission[2] != '-')
        || permission[3] != 'p' || permission[4] != ' ') return -1;
    return (permission[0] == 'r' ? PROT_READ : 0)
        | (permission[1] == 'w' ? PROT_WRITE : 0)
        | (permission[2] == 'x' ? PROT_EXEC : 0);
}

// A bounded /proc read uses fixed buffers and raw syscalls. No heap, maps scan
// through iostreams, loader lookup, retry loop, Binder or worker creation occurs
// at the snapshot boundary. An incomplete/oversized maps file simply declines.
int protection(uintptr_t address, size_t length) noexcept {
    const int fd = static_cast<int>(syscall(SYS_openat, AT_FDCWD, "/proc/self/maps", O_RDONLY | O_CLOEXEC, 0));
    if (fd < 0) return -1;
    char buffer[4096], prefix[96];
    size_t used = 0;
    int result = -1;
    bool complete = false;
    for (unsigned chunk = 0; chunk < 64 && !complete; ++chunk) {
        const long count = syscall(SYS_read, fd, buffer, sizeof(buffer));
        if (count <= 0) break;
        for (long i = 0; i < count; ++i) {
            if (buffer[i] == '\n') {
                result = mapping_line(prefix, used, address, length);
                used = 0;
                if (result >= 0) { complete = true; break; }
            } else if (used < sizeof(prefix)) prefix[used++] = buffer[i];
        }
    }
    syscall(SYS_close, fd);
    return complete ? result : -1;
}

uintptr_t page(uintptr_t address) noexcept {return address&~(uintptr_t(g_page_size)-1);}

// mlock is retained until process exit. Checking VmFlags confirms the actual
// selected VMA carries VM_LOCKED; the exact Flutter advice4 cannot discard it.
// This is not a promise against arbitrary munmap/mremap or DONTNEED_LOCKED.
bool page_locked(uintptr_t address) noexcept {
    const int fd=static_cast<int>(syscall(SYS_openat,AT_FDCWD,"/proc/self/smaps",O_RDONLY|O_CLOEXEC,0));
    if(fd<0)return false;
    char buffer[4096],line[256];size_t used=0;bool selected=false,found=false,done=false;
    for(unsigned chunk=0;chunk<1024 && !done;++chunk){
        const long count=syscall(SYS_read,fd,buffer,sizeof(buffer));if(count<=0)break;
        for(long i=0;i<count;++i){
            if(buffer[i]!='\n'){if(used<sizeof(line))line[used++]=buffer[i];continue;}
            size_t cursor=0;uintptr_t begin=0,end=0;
            const bool header=hex(line,used,cursor,begin) && cursor<used && line[cursor++]=='-'
                && hex(line,used,cursor,end) && cursor<used && line[cursor]==' ';
            if(header){
                if(selected){done=true;break;}
                selected=mapping_line(line,used,address,g_page_size)==kRx;
            }else if(selected && used>=8 && std::memcmp(line,"VmFlags:",8)==0){
                for(size_t j=8;j+2<used;++j)
                    if(line[j]==' ' && line[j+1]=='l' && line[j+2]=='o' && (j+3==used || line[j+3]==' '))found=true;
                done=true;break;
            }
            used=0;
        }
    }
    syscall(SYS_close,fd);return found;
}

#if defined(HOME_RECENTS_RUNTIME_TEST) || defined(__aarch64__)
bool atomic_isa() noexcept {
#ifdef HOME_RECENTS_RUNTIME_TEST
    return home_recents_test::atomics();
#else
    return (getauxval(AT_HWCAP)&HWCAP_ATOMICS)!=0;
#endif
}
#endif
void event(unsigned stage) noexcept {
#ifdef HOME_RECENTS_RUNTIME_TEST
    home_recents_test::event(stage);
#else
    (void)stage;
#endif
}
bool fresh(hyos_snapshot::Snapshot &s) noexcept {
    return g_lease.load()!=home_recents_policy::kDenied && g_read && g_read(s)
        && home_recents_policy::project(s,now())!=home_recents_policy::kDenied;
}
bool same(const hyos_snapshot::Snapshot &initial) noexcept {
    hyos_snapshot::Snapshot latest;
    return fresh(latest) && latest.epoch==initial.epoch && latest.revision==initial.revision
        && latest.valid_until==initial.valid_until
        && latest.config.tweaks.animation_recents_rate_percent==initial.config.tweaks.animation_recents_rate_percent;
}
bool image(uint32_t patch) noexcept {return home_recents_binding::verify(g_binding.base,read_memory,patch);}
bool arena_intact() noexcept {
    const auto arena=g_arena.load(std::memory_order_acquire);
    if(!arena || protection(arena,g_page_size)!=kRx)return false;
    std::array<std::byte,kTemplateBytes> value{};
    return read_memory(arena,value) && value==g_template;
}
bool copy_template() noexcept {
    const auto first=reinterpret_cast<uintptr_t>(hc_home_recents_template);
    if(reinterpret_cast<uintptr_t>(hc_home_recents_template_end)-first!=kTemplateBytes
        || reinterpret_cast<uintptr_t>(hc_home_recents_return_branch)-first!=kReturnOffset
        || reinterpret_cast<uintptr_t>(hc_home_recents_lease_literal)-first!=kLeaseOffset)return false;
    std::memcpy(g_template.data(),hc_home_recents_template,kTemplateBytes);
    uint32_t instruction=0;uintptr_t literal=0;
    std::memcpy(&instruction,g_template.data()+kReturnOffset,4);
    std::memcpy(&literal,g_template.data()+kLeaseOffset,8);
    if(instruction!=0x14000000 || literal!=0)return false;
    const auto lease=reinterpret_cast<uintptr_t>(&g_lease_word);
    std::memcpy(g_template.data()+kLeaseOffset,&lease,8);return true;
}
bool allocate_arena() noexcept {
    constexpr std::array<int64_t,8> offsets={0x4000000,-0x4000000,0x6000000,-0x6000000,
        0x2000000,-0x2000000,0x7000000,-0x7000000};
    for(const auto offset:offsets){
        uintptr_t candidate=page(g_binding.patch);
        if(offset>0){if(candidate>UINTPTR_MAX-static_cast<uintptr_t>(offset))continue;candidate+=offset;}
        else{if(candidate<=static_cast<uintptr_t>(-offset))continue;candidate-=static_cast<uintptr_t>(-offset);}
        uint32_t outward=0,back=0;
        if(candidate>UINTPTR_MAX-g_page_size
            || !home_recents_binding::branch(g_binding.patch,candidate,outward)
            || !home_recents_binding::branch(candidate+kReturnOffset,g_binding.resume,back))continue;
        void *mapped=mmap(reinterpret_cast<void*>(candidate),g_page_size,PROT_READ|PROT_WRITE,
            MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED_NOREPLACE,-1,0);
        if(mapped==MAP_FAILED)continue;
        if(reinterpret_cast<uintptr_t>(mapped)!=candidate){munmap(mapped,g_page_size);continue;}
        g_arena.store(candidate,std::memory_order_release);g_branch=outward;
        std::memcpy(g_template.data()+kReturnOffset,&back,4);
        std::memcpy(mapped,g_template.data(),g_template.size());
        __builtin___clear_cache(static_cast<char*>(mapped),static_cast<char*>(mapped)+g_template.size());
        return mprotect(mapped,g_page_size,kRx)==0 && arena_intact();
    }
    return false;
}
struct Exchange{bool changed=false,restored=false;};
Exchange exchange(uint32_t expected,uint32_t desired) noexcept {
    const auto at=g_binding.patch,begin=page(at);const int observed=protection(begin,g_page_size);
    if(observed!=kRx && !(g_protection_pending && observed==(kRx|PROT_WRITE)))return {};
    if(mprotect(reinterpret_cast<void*>(begin),g_page_size,kRx|PROT_WRITE)!=0)return {};
    g_protection_pending=true;event(3);
    const bool changed=__atomic_compare_exchange_n(reinterpret_cast<uint32_t*>(at),&expected,desired,
        false,__ATOMIC_ACQ_REL,__ATOMIC_ACQUIRE);
    if(changed)__builtin___clear_cache(reinterpret_cast<char*>(at),reinterpret_cast<char*>(at+4));
    const bool restored=mprotect(reinterpret_cast<void*>(begin),g_page_size,kRx)==0
        && protection(begin,g_page_size)==kRx;
    if(restored)g_protection_pending=false;
    return {changed,restored};
}
bool rollback() noexcept {
    g_lease.cancel();uint32_t word=0;bool okay=read_word(g_binding.patch,word);
    if(okay && word==g_branch && g_owned.load(std::memory_order_acquire)){
        const auto result=exchange(g_branch,home_recents_guard::kOriginalWord);
        okay=result.changed && result.restored && read_word(g_binding.patch,word);
    }
    if(okay && word==home_recents_guard::kOriginalWord)g_owned.store(false,std::memory_order_release);
    else okay=false; // Never replace a foreign instruction.
    if(g_protection_pending){
        const auto begin=page(g_binding.patch);
        if(mprotect(reinterpret_cast<void*>(begin),g_page_size,kRx)==0 && protection(begin,g_page_size)==kRx)
            g_protection_pending=false;
        else okay=false;
    }
    return okay && !g_owned.load(std::memory_order_acquire) && image(home_recents_guard::kOriginalWord);
}
bool fail(Error reason,bool attempted) noexcept {
    g_lease.cancel();const bool restored=!attempted || rollback();
    g_error.store(restored?reason:Error::RollbackFailed,std::memory_order_release);
    g_phase.store(restored?(reason==Error::Cancelled || reason==Error::EarlyCall?Phase::Missed:Phase::Rejected)
        :Phase::Residue,std::memory_order_release);
    // Retain the arena, image reference, and any mlock permanently. A native
    // updater may already have fetched the branch; restoring code cannot prove
    // no in-flight gateway still holds its lease or continuation.
    return false;
}
} // namespace

bool prepare_before_entry(uintptr_t base,ReadFresh read,PinImage pin) noexcept {
    Writer writer;
    // Another exact entry is about to continue without waiting for this writer.
    // It must not observe an original-only first update and then let the first
    // installer activate later. Cancellation and arming share the same word.
    if(!writer.owns){g_lease.cancel();cancel();return false;}
    event(0); // A competing entry must also close the pre-phase-CAS window.
    auto cold=Phase::Cold;
    if(!g_phase.compare_exchange_strong(cold,Phase::Installing,std::memory_order_acq_rel)){cancel();return false;}
    g_read=read;hyos_snapshot::Snapshot initial;
    if(!fresh(initial))return fail(Error::NotAuthorized,false);
#if !defined(HOME_RECENTS_RUNTIME_TEST) && !defined(__aarch64__)
    // The gateway contains A64 LSE instructions. Other architectures cannot
    // install it, independently of any runtime capability observation.
    return fail(Error::UnsupportedAtomicIsa,false);
#else
    if(!atomic_isa())return fail(Error::UnsupportedAtomicIsa,false);
    const long size=sysconf(_SC_PAGESIZE);
    if(size<4096 || size>65536 || (size&(size-1))!=0)return fail(Error::ProtectionFailed,false);
    g_page_size=static_cast<size_t>(size);
    if(!home_recents_binding::resolve(base,read_memory,g_binding))return fail(Error::ImageMismatch,false);
    if(!home_recents_binding::cold(g_binding,read_memory))return fail(Error::NotCold,false);
    if(!pin || !pin(base,g_binding.entry))return fail(Error::ImagePinFailed,false);
    event(1);
    if(!same(initial) || !home_recents_binding::cold(g_binding,read_memory))return fail(Error::Cancelled,false);
    if(!image(home_recents_guard::kOriginalWord))return fail(Error::ImageMismatch,false);
    const auto begin=page(g_binding.patch);
    if(protection(begin,g_page_size)!=kRx)return fail(Error::ProtectionFailed,false);
    if(mlock(reinterpret_cast<void*>(begin),g_page_size)!=0 || !page_locked(begin))
        return fail(Error::PageLockFailed,false);
    if(!copy_template())return fail(Error::TemplateMismatch,false);
    if(!allocate_arena())return fail(Error::NoArena,false);
    event(2);
    if(!same(initial) || !home_recents_binding::cold(g_binding,read_memory))return fail(Error::Cancelled,false);
    if(!image(home_recents_guard::kOriginalWord) || !arena_intact() || !page_locked(begin))
        return fail(Error::ImageMismatch,false);
    const auto changed=exchange(home_recents_guard::kOriginalWord,g_branch);
    if(changed.changed)g_owned.store(true,std::memory_order_release);
    if(!changed.changed || !changed.restored)return fail(Error::WriteFailed,true);
    event(4);
    if(!image(g_branch) || !arena_intact() || !page_locked(begin))return fail(Error::ReadbackFailed,true);
    if(!home_recents_binding::cold(g_binding,read_memory))return fail(Error::EarlyCall,true);
    if(!same(initial))return fail(Error::Cancelled,true);
    event(5);
    if(!g_lease.arm(initial,now()))return fail(Error::EarlyCall,true);
    // Publication may be cancelled between any observations. Check the full
    // captured identity after arm too; the gateway separately checks its lease.
    if(!same(initial))return fail(Error::Cancelled,true);
    auto installing=Phase::Installing;
    if(!g_phase.compare_exchange_strong(installing,Phase::Installed,std::memory_order_acq_rel))
        return fail(Error::Cancelled,true);
    if(g_lease.load()<=home_recents_policy::kDenied)return fail(Error::Cancelled,true);
    return true;
#endif
}
void service() noexcept {
    if(g_phase.load(std::memory_order_acquire)!=Phase::Installed)return;
    Writer writer;if(!writer.owns || g_phase.load(std::memory_order_acquire)!=Phase::Installed)return;
    if(!image(g_branch) || !arena_intact() || protection(page(g_binding.patch),g_page_size)!=kRx
        || !page_locked(page(g_binding.patch))){g_error.store(Error::ForeignCode,std::memory_order_release);cancel();return;}
    hyos_snapshot::Snapshot latest;
    if(!fresh(latest) || !g_lease.refresh(latest,now()))cancel();
}
void cancel() noexcept {
    auto phase=g_phase.load(std::memory_order_acquire);
    // Initial worker failures before this one-shot installer are harmless:
    // prepare reads the authoritative cache after changing Cold->Installing.
    if(phase==Phase::Cold)return;
    g_lease.cancel();
    for(unsigned attempt=0;attempt<4 && (phase==Phase::Installing || phase==Phase::Installed);++attempt)
        if(g_phase.compare_exchange_strong(phase,Phase::Missed,std::memory_order_acq_rel))break;
}
Status status() noexcept {
    return {g_phase.load(std::memory_order_acquire),g_error.load(std::memory_order_acquire),
        g_owned.load(std::memory_order_acquire),g_arena.load(std::memory_order_acquire)};
}
} // namespace home_recents
