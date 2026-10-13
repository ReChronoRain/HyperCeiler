/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Fault-reporting live code read and bounded patch write, shared by Dock and Rust targets. */
#pragma once

#include "nhk_base.h"
#include "code_write_lock.h"
#include "memory_read.h"
#include <array>
#include <algorithm>

#include <cerrno>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <fcntl.h>
#include <limits>
#include <span>
#include <sys/mman.h>
#include <sys/uio.h>
#include <unistd.h>

namespace nhk {

// One finite retry budget is shared by syscall, maps and fallback reads.
inline bool readable_memory_ranges(uintptr_t address, size_t size,
    std::vector<ReadRegion>& out, unsigned& interruptions) {
    int fd = open("/proc/self/maps",O_RDONLY|O_CLOEXEC);
    if (fd < 0) return false;
    try {
        std::string text; std::array<char,4096> chunk{}; bool good=true;
        for (;;) {
            const ssize_t n=read(fd,chunk.data(),chunk.size());
            if (n<0 && errno==EINTR && ++interruptions<=32) continue;
            if (n<0) { good=false; break; }
            if (!n) break;
            if (size_t(n)>chunk.size() || size_t(n)>2*1024*1024-text.size()) { good=false; break; }
            text.append(chunk.data(),size_t(n));
        }
        const int closed=close(fd); fd=-1;
        return good && closed==0 && ParseReadableRanges(text,address,size,out);
    } catch (...) {
        if (fd>=0) (void)close(fd);
        return false;
    }
}

inline bool safe_read(uintptr_t address, std::span<std::byte> destination) {
    if (destination.empty() || address>UINTPTR_MAX-destination.size()
        || destination.size()>size_t(std::numeric_limits<ssize_t>::max())) return false;
    // Never expose a successful prefix when a later page/read/close fails. Common
    // instruction/health reads use stack storage; bulk setup reads allocate once.
    std::array<std::byte,256> local; std::vector<std::byte> bulk;
    std::byte* staging=local.data();
    try {
        if (destination.size()>local.size()) { bulk.resize(destination.size()); staging=bulk.data(); }
        size_t completed=0; unsigned interruptions=0;
        while (completed<destination.size()) {
            const size_t request=std::min<size_t>(65536,destination.size()-completed);
            iovec target{staging+completed,request};
            iovec remote{reinterpret_cast<void*>(address+completed),request};
            const ssize_t count=process_vm_readv(getpid(),&target,1,&remote,1,0);
            if (count<0 && errno==EINTR && ++interruptions<=32) continue;
            if (count<=0) {
                // EFAULT/EIO/EOF and failed partial transfers are terminal. Only a
                // wholly unavailable syscall may use the compatibility path.
                if (completed || count==0 || (errno!=ENOSYS && errno!=EPERM && errno!=EACCES)) return false;
                const uintptr_t offMax=static_cast<uintptr_t>(std::numeric_limits<off_t>::max());
                if (address>offMax || destination.size()-1>offMax-address) return false;
                std::vector<ReadRegion> before,after;
                if (!readable_memory_ranges(address,destination.size(),before,interruptions)) return false;
                const int memory=open("/proc/self/mem",O_RDONLY|O_CLOEXEC);
                if (memory<0) return false;
                bool success=true;
                while (completed<destination.size()) {
                    const uintptr_t position=address+completed;
                    const size_t remaining=destination.size()-completed;
                    if (position>static_cast<uintptr_t>(std::numeric_limits<off_t>::max())) { success=false; break; }
                    const size_t request=std::min<size_t>(65536,remaining);
                    const ssize_t copied=pread(memory,staging+completed,request,static_cast<off_t>(position));
                    if (copied<0 && errno==EINTR && ++interruptions<=32) continue;
                    if (copied<=0 || size_t(copied)>request) { success=false; break; }
                    completed+=size_t(copied);
                }
                const int closed=close(memory); // Linux close(EINTR) is not retried.
                if (!success || closed!=0 || !readable_memory_ranges(address,destination.size(),after,interruptions)
                    || before!=after) return false;
                break;
            }
            if (size_t(count)>request) return false;
            completed+=size_t(count);
        }
        std::memcpy(destination.data(),staging,destination.size()); return true;
    } catch (...) {
        return false; // Allocation failure does not publish an incomplete destination.
    }
}

inline bool write_code_bytes(uintptr_t address, std::span<const std::byte> bytes) {
    if (bytes.empty() || address > UINTPTR_MAX - bytes.size()) return false;
    CodeWriteGuard lock;
    if (!lock) return false;
    const uintptr_t page = static_cast<uintptr_t>(page_down(address));
    const size_t length = static_cast<size_t>(address + bytes.size() - page);
    void *base = reinterpret_cast<void *>(page);
    if (mprotect(base, length, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    std::memcpy(reinterpret_cast<void *>(address), bytes.data(), bytes.size());
    __builtin___clear_cache(reinterpret_cast<char *>(address),
        reinterpret_cast<char *>(address + bytes.size()));
    return mprotect(base, length, PROT_READ | PROT_EXEC) == 0;
}

} // namespace nhk
