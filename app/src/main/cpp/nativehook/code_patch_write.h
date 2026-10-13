/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "code_word_write.h"
#include "native_image.h"
#include <algorithm>
#include <array>
#include <span>

namespace nhk {
// A persistent byte/protection obligation for one already-owned inline slot.
// This protocol does NOT make a 16-byte instruction stream atomic or quiescent.
template<size_t N> struct CodePatchJournal {
    static_assert(N>0 && N<=64);
    uintptr_t address=0;
    CodeSource source{};
    std::array<CodePage,N> pages{};
    std::array<bool,N> protectionPending{};
    size_t pageCount=0;
    std::array<uint32_t,N> restoreWords{},ownedWords{};
    bool bytesPending=false,ownedKnown=false;
    bool pending() const {
        if (bytesPending || pageCount>N) return true;
        for (size_t i=0;i<N;++i) if (protectionPending[i]) return true;
        return false;
    }
};

template<size_t N> bool ValidCodePatchJournal(const CodePatchJournal<N>& j) {
    if (!j.address || (j.address&3u) || j.address>UINTPTR_MAX-N*4 || !j.source.inode
        || j.source.file_offset>UINT64_MAX-N*4 || !j.pageCount || j.pageCount>N) return false;
    for (size_t i=j.pageCount;i<N;++i) if (j.protectionPending[i]) return false;
    uintptr_t cursor=j.address;
    for (size_t i=0;i<j.pageCount;++i) {
        const auto& p=j.pages[i];
        if (!p.size || p.begin>UINTPTR_MAX-p.size || cursor<p.begin || cursor>=p.begin+p.size
            || p.protection!=5 || !p.privateMap || p.inode!=j.source.inode
            || p.major!=j.source.device_major || p.minor!=j.source.device_minor
            || p.offset>UINT64_MAX-(cursor-p.begin)
            || p.offset+(cursor-p.begin)!=j.source.file_offset+(cursor-j.address)) return false;
        cursor=std::min<uintptr_t>(p.begin+p.size,j.address+N*4);
    }
    return cursor==j.address+N*4;
}

template<size_t N,class Ops> bool CurrentCodePatchPages(Ops& ops,const CodePatchJournal<N>& j) {
    if (!ValidCodePatchJournal(j)) return false;
    for (size_t i=0;i<j.pageCount;++i) {
        CodePage page{};
        if (!ops.Page(j.pages[i].begin,j.pages[i].size,page) || !SameCodePage(page,j.pages[i])
            || (page.protection!=5 && page.protection!=7)
            || (page.protection!=5 && !j.protectionPending[i])) return false;
    }
    return true;
}

template<size_t N,class Ops> bool RestoreCodePatchProtection(Ops& ops,CodePatchJournal<N>& j) {
    CodeWriteGuard lock;
    if (!lock || !ValidCodePatchJournal(j)) return false;
    bool complete=true;
    for (size_t k=j.pageCount;k>0;--k) {
        const size_t i=k-1;
        if (!j.protectionPending[i]) continue;
        CodePage page{};
        if (!ops.Page(j.pages[i].begin,j.pages[i].size,page) || !SameCodePage(page,j.pages[i])
            || (page.protection!=5 && page.protection!=7)) { complete=false;continue; }
        if (page.protection!=5) (void)ops.Protect(page.begin,page.size,5);
        CodePage after{};
        if (!ops.Page(page.begin,page.size,after) || !SameCodePage(after,j.pages[i])
            || after.protection!=5) { complete=false;continue; }
        j.protectionPending[i]=false;
    }
    return complete;
}

template<size_t N,class Ops> bool OpenCodePatchPages(Ops& ops,CodePatchJournal<N>& j) {
    CodeWriteGuard lock;
    if (!lock || !CurrentCodePatchPages(ops,j)) return false;
    for (size_t i=0;i<j.pageCount;++i) {
        CodePage page{};
        if (!ops.Page(j.pages[i].begin,j.pages[i].size,page) || !SameCodePage(page,j.pages[i])) return false;
        // Publish BEFORE mprotect: a false syscall result may still leave recovery work.
        j.protectionPending[i]=true;
        if (page.protection!=7 && !ops.Protect(page.begin,page.size,7)) return false;
        CodePage after{};
        if (!ops.Page(page.begin,page.size,after) || !SameCodePage(after,j.pages[i])
            || after.protection!=7) return false;
    }
    return true;
}

template<size_t N,class Ops> long TransferCodePatch(Ops& ops,CodePatchJournal<N>& j,
    const std::array<uint32_t,N>& expected,const std::array<uint32_t,N>& wanted) {
    CodeWriteGuard lock;
    if (!lock) return -1;
    std::array<uint32_t,N> observed{};
    if (!OpenCodePatchPages(ops,j) || !CurrentCodePatchPages(ops,j)
        || !ops.Read(j.address,observed) || observed!=expected) return -1;
    j.bytesPending=true; // Caller-owned record survives a failed result/next worker invocation.
    j.ownedKnown=false;
    const long n=ops.Write(j.address,std::as_bytes(std::span(wanted)));
    if (n>0 && size_t(n)<=N*4) {
        j.ownedWords=expected;
        std::memcpy(j.ownedWords.data(),wanted.data(),size_t(n));
        j.ownedKnown=true;
        ops.Flush(j.address,N*4);
    }
    return n;
}

template<size_t N,class Ops> bool RecoverCodePatch(Ops& ops,CodePatchJournal<N>& j) {
    if (!j.pending()) return true;
    CodeWriteGuard lock;
    if (!lock || !ValidCodePatchJournal(j)) return false;
    if (j.bytesPending && CurrentCodePatchPages(ops,j)) {
        std::array<uint32_t,N> current{};
        if (ops.Read(j.address,current)) {
            if (current==j.restoreWords) j.bytesPending=false;
            else if (j.ownedKnown && current==j.ownedWords) {
                const auto owned=j.ownedWords;
                (void)TransferCodePatch(ops,j,owned,j.restoreWords);
                if (CurrentCodePatchPages(ops,j) && ops.Read(j.address,current)
                    && current==j.restoreWords) j.bytesPending=false;
            }
        }
    }
    // Never overwrite foreign bytes, but still attempt our original RX obligations.
    (void)RestoreCodePatchProtection(ops,j);
    if (j.pending()) return false;
    j={};return true;
}

template<size_t N,class Ops> bool WriteCodePatch(Ops& ops,uintptr_t address,CodeSource source,
    const std::array<uint32_t,N>& expected,const std::array<uint32_t,N>& wanted,
    CodePatchJournal<N>& j) {
    if (!address || (address&3u) || address>UINTPTR_MAX-N*4 || !source.inode
        || source.file_offset>UINT64_MAX-N*4) return false;
    CodeWriteGuard lock;
    if (!lock || !RecoverCodePatch(ops,j)) return false;
    const long size=ops.PageSize();
    if (size<4 || (uint64_t(size)&(uint64_t(size)-1))) return false;
    CodePatchJournal<N> candidate{};candidate.address=address;candidate.source=source;
    candidate.restoreWords=expected;candidate.ownedWords=expected;candidate.ownedKnown=true;
    uintptr_t page=address&~(uintptr_t(size)-1);
    while (page<address+N*4) {
        if (page>UINTPTR_MAX-uintptr_t(size) || candidate.pageCount==N) return false;
        CodePage current{};
        if (!ops.Page(page,size_t(size),current) || current.protection!=5 || !current.privateMap) return false;
        candidate.pages[candidate.pageCount++]=current;
        page+=uintptr_t(size);
    }
    std::array<uint32_t,N> observed{};
    if (!ValidCodePatchJournal(candidate) || !ops.Read(address,observed) || observed!=expected
        || !CurrentCodePatchPages(ops,candidate)) return false;
    if (expected==wanted) return true;
    j=candidate;
    const long copied=TransferCodePatch(ops,j,expected,wanted);
    const bool transferred=copied==long(N*4) && CurrentCodePatchPages(ops,j)
        && ops.Read(address,observed) && observed==wanted;
    const bool restored=RestoreCodePatchProtection(ops,j);
    if (transferred && restored && CurrentCodePatchPages(ops,j)
        && ops.Read(address,observed) && observed==wanted) { j={};return true; }
    (void)RecoverCodePatch(ops,j);return false;
}
} // namespace nhk
