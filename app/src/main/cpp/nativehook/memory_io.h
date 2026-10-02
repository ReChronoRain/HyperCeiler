/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Fault-reporting live code read and bounded patch write, shared by Dock and Rust targets. */
#pragma once

#include "nhk_base.h"

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

inline bool safe_read(uintptr_t address, std::span<std::byte> destination) {
    if (destination.empty() || address > UINTPTR_MAX - destination.size()) return false;
    size_t completed = 0;
    while (completed < destination.size()) {
        iovec local{destination.data() + completed, destination.size() - completed};
        iovec remote{reinterpret_cast<void *>(address + completed), destination.size() - completed};
        const ssize_t count = process_vm_readv(getpid(), &local, 1, &remote, 1, 0);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) {
            const int memory = open("/proc/self/mem", O_RDONLY | O_CLOEXEC);
            if (memory < 0) return false;
            bool success = true;
            while (completed < destination.size()) {
                const uintptr_t position = address + completed;
                if (position > static_cast<uintptr_t>(std::numeric_limits<off_t>::max())) {
                    success = false;
                    break;
                }
                const ssize_t copied = pread(memory, destination.data() + completed,
                    destination.size() - completed, static_cast<off_t>(position));
                if (copied < 0 && errno == EINTR) continue;
                if (copied <= 0) { success = false; break; }
                completed += static_cast<size_t>(copied);
            }
            close(memory);
            return success;
        }
        if (static_cast<size_t>(count) > destination.size() - completed) return false;
        completed += static_cast<size_t>(count);
    }
    return true;
}

inline bool write_code_bytes(uintptr_t address, std::span<const std::byte> bytes) {
    if (bytes.empty() || address > UINTPTR_MAX - bytes.size()) return false;
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
