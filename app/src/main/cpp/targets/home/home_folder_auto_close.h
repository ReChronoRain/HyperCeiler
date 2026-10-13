/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <span>
#include <optional>
namespace home_layout {
constexpr const char* kFolderAutoClickSymbol = "AppIconRemoteClick.handleRemoteClick";
constexpr uint32_t kFolderAutoCloseMagic = 0x48434641;
constexpr uint32_t kFolderAutoCloseReplacement = 0xd503201f; // One aligned NOP, no Dart ABI crossing.
inline uint32_t auto_close_call(uint32_t pc, uint32_t w) {
    int32_t imm = int32_t(w << 6) >> 6;
    return uint32_t(int64_t(pc) + int64_t(imm) * 4);
}
// Admit ONLY the mode gate guarding stock close logic. Folder registration/open tests and
// all blocked-launch/recents guards remain intact. No field/pool offset or code VA is a profile.
inline std::optional<size_t> resolve_folder_auto_close(std::span<const uint32_t> body,
    uint32_t va, uint32_t close, uint32_t rx, uint32_t registered) {
    if (body.size() < 8 || body[0] != 0xa9bf79fd || body[1] != 0xaa0f03fd) return {};
    size_t close_at = 0, close_count = 0;
    for (size_t i=0;i<body.size();++i)
        if ((body[i]&0xfc000000u)==0x94000000u && auto_close_call(va+uint32_t(i*4),body[i])==close) {close_at=i;++close_count;}
    if (close_count!=1 || close_at<4 || close_at+1>=body.size()
        || body[close_at-2]!=0x910082c2 || body[close_at-1]!=0xf9403b64) return {};
    const uint32_t end=va+uint32_t((close_at+1)*4);
    size_t gate=0, count=0, registers=0, open=0;
    for (size_t i=2;i<close_at;++i) {
        if ((body[i]&0xfff8001fu)!=0x37200000u) continue; // tbnz w0,#4
        int32_t jump=int32_t(((body[i]>>5)&0x3fff)<<18)>>18;
        if (int64_t(va)+int64_t(i*4)+int64_t(jump)*4!=end) continue;
        const auto call=body[i-1]; if ((call&0xfc000000u)!=0x94000000u) continue;
        const auto target=auto_close_call(va+uint32_t((i-1)*4),call);
        // ldur x1,[fp,#negative spill], not a heap field getter or another launch test.
        if (target==rx && (body[i-2]&0xffe00fffu)==0xf84003a1u
            && ((body[i-2]>>12)&0x100)) {gate=i;++count;}
        else if (target==registered) ++registers;
        else if (target==rx && body[i-2]==0x8b1c8021u) ++open;
    }
    if (count!=1 || registers!=1 || open!=1) return {};
    // The order is mode -> registered -> open -> stock close; later gates are never patched.
    size_t reg_at=0, open_at=0;
    for(size_t i=gate+1;i<close_at;++i) {
        if((body[i]&0xfc000000u)!=0x94000000u)continue;
        auto t=auto_close_call(va+uint32_t(i*4),body[i]);
        if(t==registered)reg_at=i;if(t==rx)open_at=i;
    }
    if(!(gate<reg_at && reg_at<open_at && open_at<close_at))return {};
    return gate*4;
}
template<class Read> bool read_folder_auto_close(Read read,bool& enabled,bool optional=true) {
    int32_t magic=0,flag=0;enabled=false;
    if(!read(magic))return optional;
    if(magic!=int32_t(kFolderAutoCloseMagic)||!read(flag)||(flag!=0&&flag!=1))return false;
    enabled=flag==1;return true;
}
} // namespace home_layout
