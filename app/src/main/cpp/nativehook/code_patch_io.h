/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "code_patch_write.h"
#include "source_word_io.h"

namespace nhk {
template<size_t N> struct CodePatchOps {
    uintptr_t target;
    CodeSource source;
    long PageSize() { return sysconf(_SC_PAGESIZE); }
    bool Page(uintptr_t page,size_t size,CodePage& out) {
        CodeSource at=source;
        if (page<=target) {
            if (at.file_offset<target-page) return false;
            at.file_offset-=target-page;
        } else {
            if (at.file_offset>UINT64_MAX-(page-target)) return false;
            at.file_offset+=page-target;
        }
        // Reuse the exact-source, bounded maps parser/exception boundary.
        SourceWordOps metadata{page,at};return metadata.Page(page,size,out);
    }
    bool Read(uintptr_t address,std::array<uint32_t,N>& out) {
        if (address!=target) return false;
        std::array<uint32_t,N> local{};
        if (!safe_read(address,std::as_writable_bytes(std::span(local)))) return false;
        out=local;return true;
    }
    bool Protect(uintptr_t page,size_t size,int protection) {
        return mprotect(reinterpret_cast<void*>(page),size,protection)==0;
    }
    long Write(uintptr_t address,std::span<const std::byte> bytes) {
        if (address!=target || bytes.empty() || bytes.size()>N*4) return 0;
        // Keeps the existing physical transfer semantics. The journal fixes lost
        // ownership/permissions, not atomic publication or unmapping races.
        std::memcpy(reinterpret_cast<void*>(address),bytes.data(),bytes.size());
        return long(bytes.size());
    }
    void Flush(uintptr_t address,size_t size) {
        __builtin___clear_cache(reinterpret_cast<char*>(address),reinterpret_cast<char*>(address+size));
    }
};
} // namespace nhk
