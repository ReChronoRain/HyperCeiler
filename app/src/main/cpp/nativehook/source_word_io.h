/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "code_word_write.h"
#include "memory_io.h"
#include "native_image.h"
#include <array>

namespace nhk {
// One bound instruction, not merely another mapping of the same inode.
// These fresh snapshots are not a kernel lease against concurrent unmapping.
struct SourceWordOps {
    uintptr_t target;
    CodeSource source;
    long PageSize() { return sysconf(_SC_PAGESIZE); }
    bool Accept(const CodePage& page) {
        return source.inode && page.inode == source.inode
            && page.major == source.device_major && page.minor == source.device_minor
            && target >= page.begin && target - page.begin <= page.size
            && page.size - (target - page.begin) >= 4
            && page.offset <= UINT64_MAX - (target - page.begin)
            && page.offset + (target - page.begin) == source.file_offset;
    }
    bool Page(uintptr_t address, size_t size, CodePage& out) {
        int fd = open("/proc/self/maps", O_RDONLY | O_CLOEXEC);
        if (fd < 0) return false;
        try {
            std::string text; std::array<char,4096> chunk{}; bool good = true;
            unsigned interruptions = 0;
            for (;;) {
                const ssize_t n = read(fd,chunk.data(),chunk.size());
                if (n < 0 && errno == EINTR && ++interruptions <= 32) continue;
                if (n < 0) { good = false; break; }
                if (n == 0) break;
                if (size_t(n) > chunk.size() || size_t(n) > 2*1024*1024-text.size()) {
                    good = false; break;
                }
                text.append(chunk.data(),size_t(n));
            }
            const int closed = close(fd); fd = -1;
            if (closed != 0) good = false; // Linux close(EINTR) is never retried.
            CodePage candidate{};
            if (!good || !ParseCodePage(text,address,size,candidate) || !Accept(candidate)) return false;
            out = candidate; return true;
        } catch (...) {
            if (fd >= 0) (void)close(fd);
            return false; // Allocation/parsing faults must retain the caller journal, not escape.
        }
    }
    bool Read(uintptr_t address, uint32_t& out) {
        if (address != target) return false;
        uint32_t word = 0;
        if (!safe_read(address,std::as_writable_bytes(std::span(&word,1)))) return false;
        out = word; return true;
    }
    bool Protect(uintptr_t page, size_t size, int protection) {
        return mprotect(reinterpret_cast<void*>(page),size,protection) == 0;
    }
    long Write(uintptr_t address, uint32_t word, uint32_t expected) {
        if (address != target || (address & 3u)) return 0;
        // Keep aligned instruction CAS; byte-copy syscalls are not atomic instruction writes.
        return __atomic_compare_exchange_n(reinterpret_cast<uint32_t*>(address),&expected,
            word,false,__ATOMIC_RELEASE,__ATOMIC_RELAXED) ? 4 : 0;
    }
    void Flush(uintptr_t address, size_t size) {
        __builtin___clear_cache(reinterpret_cast<char*>(address),reinterpret_cast<char*>(address+size));
    }
};
} // namespace nhk
