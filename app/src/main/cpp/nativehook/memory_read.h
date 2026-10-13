/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <charconv>
#include <cstdint>
#include <sstream>
#include <string>
#include <vector>

namespace nhk {
struct ReadRegion {
    uintptr_t begin = 0, end = 0;
    uint64_t offset = 0, inode = 0;
    uint32_t major = 0, minor = 0;
    std::string permissions;
    bool operator==(const ReadRegion&) const = default;
};
// Used only when the self-read syscall is unavailable/denied, not on the normal
// health-read path. These snapshots are not a kernel mapping lease or proof that
// the bytes were one simultaneous application-level snapshot.
inline bool ParseReadableRanges(const std::string& text, uintptr_t address, size_t size,
    std::vector<ReadRegion>& out) {
    if (!size || address > UINTPTR_MAX-size || text.empty() || text.size()>2*1024*1024
        || text.back()!='\n' || text.find('\0')!=std::string::npos) return false;
    const auto number = [](const std::string& s, int base, uint64_t& n) {
        if (s.empty()) return false;
        const auto r = std::from_chars(s.data(),s.data()+s.size(),n,base);
        return r.ec==std::errc{} && r.ptr==s.data()+s.size();
    };
    std::istringstream lines(text); std::string line; uintptr_t previous=0,cursor=address;
    const uintptr_t limit=address+size; size_t count=0; std::vector<ReadRegion> candidate;
    while (std::getline(lines,line)) {
        if (++count>16384) return false;
        std::istringstream fields(line); std::string range,perms,offset,device,inode;
        if (!(fields>>range>>perms>>offset>>device>>inode)) return false;
        const auto dash=range.find('-'),colon=device.find(':');
        uint64_t begin=0,end=0,off=0,ino=0,major=0,minor=0;
        if (dash==std::string::npos || colon==std::string::npos
            || !number(range.substr(0,dash),16,begin) || !number(range.substr(dash+1),16,end)
            || !number(offset,16,off) || !number(device.substr(0,colon),16,major)
            || !number(device.substr(colon+1),16,minor) || !number(inode,10,ino)
            || begin>=end || end>UINTPTR_MAX || begin<previous
            || major>UINT32_MAX || minor>UINT32_MAX || perms.size()!=4
            || (perms[0]!='r'&&perms[0]!='-') || (perms[1]!='w'&&perms[1]!='-')
            || (perms[2]!='x'&&perms[2]!='-') || (perms[3]!='p'&&perms[3]!='s')) return false;
        previous=uintptr_t(end);
        if (end<=address || begin>=limit) continue;
        if (begin>cursor || end<=cursor || perms[0]!='r'
            || off>UINT64_MAX-(std::min<uint64_t>(end,limit)-begin)) return false;
        candidate.push_back({uintptr_t(begin),uintptr_t(end),off,ino,uint32_t(major),uint32_t(minor),perms});
        cursor=std::min<uintptr_t>(uintptr_t(end),limit);
    }
    if (cursor!=limit || candidate.empty()) return false;
    out=std::move(candidate); return true;
}
} // namespace nhk
