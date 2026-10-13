/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "code_write_lock.h"
#include <charconv>
#include <cstdint>
#include <cstring>
#include <sstream>
#include <string>

namespace nhk {
// A snapshot is an identity check, not a kernel mapping-generation lease.
struct CodePage {
    uintptr_t begin = 0;
    size_t size = 0;
    uint64_t offset = 0, inode = 0;
    uint32_t major = 0, minor = 0;
    int protection = 0;
    bool privateMap = false;
};
inline bool SameCodePage(const CodePage& a, const CodePage& b) {
    return a.begin == b.begin && a.size == b.size && a.offset == b.offset
        && a.inode == b.inode && a.major == b.major && a.minor == b.minor
        && a.privateMap == b.privateMap;
}
inline bool ParseCodePage(const std::string& text, uintptr_t page, size_t size, CodePage& out) {
    if (!size || page > UINTPTR_MAX - size || text.empty() || text.size() > 2*1024*1024
        || text.back() != '\n' || text.find('\0') != std::string::npos) return false;
    auto number = [](const std::string& s, int base, uint64_t& value) {
        if (s.empty()) return false;
        auto result = std::from_chars(s.data(), s.data()+s.size(), value, base);
        return result.ec == std::errc{} && result.ptr == s.data()+s.size();
    };
    std::istringstream lines(text); std::string line; uintptr_t previous = 0;
    size_t count = 0; bool found = false; CodePage candidate{};
    while (std::getline(lines, line)) {
        if (++count > 16384) return false;
        std::istringstream fields(line); std::string range, perms, offset, device, inode;
        if (!(fields >> range >> perms >> offset >> device >> inode)) return false;
        const auto dash = range.find('-'), colon = device.find(':');
        uint64_t begin = 0, end = 0, off = 0, ino = 0, major = 0, minor = 0;
        if (dash == std::string::npos || colon == std::string::npos
            || !number(range.substr(0,dash),16,begin) || !number(range.substr(dash+1),16,end)
            || !number(offset,16,off) || !number(device.substr(0,colon),16,major)
            || !number(device.substr(colon+1),16,minor) || !number(inode,10,ino)
            || begin >= end || end > UINTPTR_MAX || begin < previous
            || major > UINT32_MAX || minor > UINT32_MAX || perms.size()!=4
            || (perms[0]!='r'&&perms[0]!='-') || (perms[1]!='w'&&perms[1]!='-')
            || (perms[2]!='x'&&perms[2]!='-') || (perms[3]!='p'&&perms[3]!='s')) return false;
        previous = uintptr_t(end);
        if (page < begin || page >= end) continue;
        if (found || page + size > end || off > UINT64_MAX-(page-begin)) return false;
        found = true;
        candidate = {page,size,off+(page-begin),ino,uint32_t(major),uint32_t(minor),
            (perms[0]=='r'?1:0)|(perms[1]=='w'?2:0)|(perms[2]=='x'?4:0),perms[3]=='p'};
    }
    if (!found) return false;
    out = candidate; return true;
}
struct WordWriteJournal {
    CodePage page{};
    uintptr_t address = 0;
    uint32_t restoreWord = 0, ownedWord = 0;
    bool bytesPending = false, protectionPending = false;
    bool pending() const { return bytesPending || protectionPending; }
};
struct WordWriteResult { bool complete = false; WordWriteJournal recovery{}; };

template<class Ops> bool CurrentCodePage(Ops& ops, const WordWriteJournal& j, CodePage& page) {
    return ops.Page(j.page.begin,j.page.size,page) && SameCodePage(page,j.page)
        && (page.protection == 5 || page.protection == 7) && page.privateMap;
}
template<class Ops> bool RestoreCodeProtection(Ops& ops, WordWriteJournal& j) {
    if (!j.protectionPending) return true;
    CodeWriteGuard lock;
    if (!lock) return false;
    CodePage page{};
    if (!CurrentCodePage(ops,j,page)) return false;
    if (page.protection != j.page.protection
        && !ops.Protect(page.begin,page.size,j.page.protection)) return false;
    if (!CurrentCodePage(ops,j,page) || page.protection != j.page.protection) return false;
    j.protectionPending = false; return true;
}
template<class Ops> bool TransferCodeWord(Ops& ops, WordWriteJournal& j, uint32_t wanted) {
    CodeWriteGuard lock;
    if (!lock) return false;
    CodePage page{}; uint32_t current = 0;
    if (!CurrentCodePage(ops,j,page) || !ops.Read(j.address,current) || current!=j.ownedWord)
        return false;
    j.protectionPending = true;
    if (page.protection != 7) {
        if (!ops.Protect(page.begin,page.size,7)) return false;
    }
    // Recheck after changing permissions; never consume a replacement VMA or foreign word.
    if (!CurrentCodePage(ops,j,page) || page.protection != 7
        || !ops.Read(j.address,current) || current != j.ownedWord) return false;
    const long copied = ops.Write(j.address,wanted,j.ownedWord);
    if (copied > 0 && copied <= 4) {
        // A short copy is owned only if readback agrees with precisely this byte prefix.
        uint32_t partial = j.ownedWord;
        std::memcpy(&partial,&wanted,size_t(copied)); j.ownedWord = partial;
        j.bytesPending = true; // Never declare restore complete before full readback.
    }
    if (!CurrentCodePage(ops,j,page)) return false;
    if (copied > 0) ops.Flush(j.address,4);
    return copied == 4 && ops.Read(j.address,current) && current==wanted;
}
template<class Ops> bool RecoverCodeWord(Ops& ops, WordWriteJournal& j) {
    if (!j.pending()) return true;
    CodeWriteGuard lock;
    if (!lock) return false;
    CodePage page{}; uint32_t current = 0;
    if (j.bytesPending && CurrentCodePage(ops,j,page) && ops.Read(j.address,current)) {
        if (current == j.restoreWord) j.bytesPending = false;
        else if (current == j.ownedWord && TransferCodeWord(ops,j,j.restoreWord))
            j.bytesPending = false;
    }
    // Even a foreign instruction must not leave our temporary writable permission behind.
    (void) RestoreCodeProtection(ops,j);
    return !j.pending();
}
template<class Ops> WordWriteResult WriteCodeWord(Ops& ops, uintptr_t address,
    uint32_t expected, uint32_t wanted, uint32_t restore) {
    WordWriteResult result{}; const long size = ops.PageSize();
    if (!address || (address&3u) || address > UINTPTR_MAX-4 || size < 4
        || (uint64_t(size)&(uint64_t(size)-1))) return result;
    const uintptr_t page = address & ~(uintptr_t(size)-1);
    if (page > UINTPTR_MAX-uintptr_t(size)) return result;
    CodeWriteGuard lock;
    if (!lock) return result;
    CodePage mapping{}; uint32_t current = 0;
    if (!ops.Page(page,size_t(size),mapping) || mapping.protection != 5 || !mapping.privateMap
        || !ops.Accept(mapping) || !ops.Read(address,current) || current != expected) return result;
    auto& j = result.recovery;
    j.page = mapping; j.address = address; j.restoreWord = restore; j.ownedWord = expected;
    j.bytesPending = expected != restore;
    const bool transferred = TransferCodeWord(ops,j,wanted);
    const bool protectedAgain = RestoreCodeProtection(ops,j);
    if (transferred && protectedAgain) {
        result.complete = true; j.bytesPending = false;
    } else {
        // Rollback is attempted immediately; any unfinished byte/permission state remains explicit.
        (void) RecoverCodeWord(ops,j);
    }
    return result;
}
} // namespace nhk
