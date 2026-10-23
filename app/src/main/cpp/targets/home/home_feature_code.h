/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dynamic_layout.h"
#include "nativehook/elf_image.h"
#include <algorithm>
#include <cstring>
#include <optional>
#include <span>

namespace home_layout::features {
// Reject a normal return reachable without any validated value/patch point.
inline bool passes_return_points(const home_dynamic::Body& b,std::span<const size_t> points) {
    std::vector<bool> visited(b.size());std::vector<size_t> pending{0};bool reached=false;
    while(!pending.empty()) {
        const auto at=pending.back();pending.pop_back();
        if(at>=b.size())return false;
        if(std::find(points.begin(),points.end(),at)!=points.end()){reached=true;continue;}
        if(visited[at])continue;visited[at]=true;
        const auto w=b[at];int64_t destination=-1;bool unconditional=false,branch=false;
        if((w&0xfffffc1fu)==0xd65f0000u||(w&0xfffffc1fu)==0xd61f0000u)return false;
        if((w&0xffe0001fu)==0xd4200000u)continue; // Original throw/trap, no returned widget.
        if((w&0xfc000000u)==0x14000000u){destination=int64_t(at)+(int32_t(w<<6)>>6);unconditional=true;branch=true;}
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)
            {destination=int64_t(at)+(int32_t(w<<8)>>13);branch=true;}
        else if((w&0x7e000000u)==0x36000000u){destination=int64_t(at)+(int32_t(w<<13)>>18);branch=true;}
        if(branch) {
            if(destination<0||destination>=int64_t(b.size()))return false;
            pending.push_back(static_cast<size_t>(destination));
        }
        if(!unconditional)pending.push_back(at+1);
    }
    return reached;
}
struct Function {
    uint32_t va = 0;
    home_dynamic::Body words;
};

// An instruction inventory, not a symbol index. Only file-backed executable
// PT_LOAD ranges are admitted; section headers, names and .gnu_debugdata are
// irrelevant. Dart's frame prologue and the image's own direct BL destinations
// establish candidate boundaries. A boundary is NOT a feature identity: every
// consumer must still prove its complete producer/callee/field relationship.
// The fixed prologue/register roles below are the verified ARM64 Dart ABI.
class Code {
public:
    static std::optional<Code> parse(std::span<const std::byte> image) {
        const auto segments = nhk::elf::parse_program_segments(image);
        if (!segments) return {};
        struct Range { uint32_t va; std::span<const std::byte> bytes; };
        std::vector<Range> ranges;
        size_t total = 0;
        for (const auto& p : *segments) {
            if (p.type != nhk::elf::kProgramTypeLoad || !(p.flags & 1)) continue;
            if ((p.flags & 2) || !p.filesz || (p.vaddr & 3) || (p.filesz & 3)
                || p.vaddr > UINT32_MAX || p.filesz > UINT32_MAX - p.vaddr
                || p.offset > image.size() || p.filesz > image.size() - p.offset
                || p.filesz > 64u * 1024 * 1024 - total) return {};
            total += static_cast<size_t>(p.filesz);
            ranges.push_back({static_cast<uint32_t>(p.vaddr),
                image.subspan(static_cast<size_t>(p.offset),static_cast<size_t>(p.filesz))});
        }
        if (ranges.empty()) return {};
        std::sort(ranges.begin(), ranges.end(), [](const auto& a, const auto& b) {return a.va < b.va;});
        for (size_t i = 1; i < ranges.size(); ++i)
            if (uint64_t(ranges[i-1].va) + ranges[i-1].bytes.size() > ranges[i].va) return {};
        const auto executable = [&](uint32_t va) {
            return std::any_of(ranges.begin(), ranges.end(), [&](const auto& r) {
                return va >= r.va && uint64_t(va) + 4 <= uint64_t(r.va) + r.bytes.size();
            });
        };
        std::vector<uint32_t> entries;
        for (const auto& range : ranges) {
            entries.push_back(range.va);
            for (size_t i = 0; i < range.bytes.size() / 4; ++i) {
                const uint32_t va = range.va + static_cast<uint32_t>(i * 4);
                const uint32_t word=nhk::load_le32(range.bytes.data()+i*4);
                if ((i+1)*4<range.bytes.size() && word == 0xa9bf79fdu
                    && nhk::load_le32(range.bytes.data()+(i+1)*4) == 0xaa0f03fdu) entries.push_back(va);
                uint32_t callee = 0;
                if (home_dynamic::target(word, va, callee) && executable(callee))
                    entries.push_back(callee);
                if(entries.size()>2000000)return {}; // Bound duplicate call evidence before sorting.
            }
        }
        std::sort(entries.begin(), entries.end());
        entries.erase(std::unique(entries.begin(), entries.end()), entries.end());
        if (entries.size() > 400000) return {};
        Code result;
        for (const auto& range : ranges) {
            auto at = std::lower_bound(entries.begin(), entries.end(), range.va);
            const uint64_t end = uint64_t(range.va) + range.bytes.size();
            while (at != entries.end() && *at < end) {
                const uint32_t begin = *at++;
                const uint64_t next = at == entries.end() ? end : std::min<uint64_t>(*at, end);
                Function function;function.va=begin;function.words.resize(static_cast<size_t>((next-begin)/4));
                std::memcpy(function.words.data(),range.bytes.data()+begin-range.va,next-begin);
                result.functions_.push_back(std::move(function));
            }
        }
        return result;
    }

    const std::vector<Function>& functions() const { return functions_; }
    bool body(uint32_t va, home_dynamic::Body& out) const {
        const auto it = std::lower_bound(functions_.begin(), functions_.end(), va,
            [](const auto& f, uint32_t target) { return f.va < target; });
        // Match the existing bounded body contract, without manufacturing a
        // short view that could make an ambiguous parser look unique.
        if (it == functions_.end() || it->va != va || it->words.empty()
            || it->words.size() > 0x4000 / 4) return false;
        out = it->words;
        return true;
    }
    bool prefix(uint32_t va, size_t count, home_dynamic::Body& out) const {
        if (!count || count > 0x4000 / 4 || (va & 3)) return false;
        auto it = std::upper_bound(functions_.begin(), functions_.end(), va,
            [](uint32_t target, const auto& f) { return target < f.va; });
        if (it == functions_.begin()) return false;
        --it;
        const size_t offset = (va - it->va) / 4;
        if (offset > it->words.size() || count > it->words.size() - offset) return false;
        out.assign(it->words.begin() + offset, it->words.begin() + offset + count);
        return true;
    }
private:
    std::vector<Function> functions_;
};
} // namespace home_layout::features
